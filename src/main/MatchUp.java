package main;

import gameobjects.cards.GenericCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fileio.ActionsInput;
import fileio.GameInput;
import fileio.Input;

import gameobjects.GameField;
import gameobjects.Player;

import gameobjects.cards.GenericHero;
import lombok.Getter;
import lombok.Setter;
import utils.GameConstants;
import utils.Point;

import java.util.ArrayList;

@Getter @Setter
public final class MatchUp {
    private static MatchUp instance = null;
    private GameField field;
    private Player player1;
    private Player player2;
    private int shuffleSeed;

    private int playerTurn;
    private int turnCounter;

    // TODO: REMOVE THIS
    private int debugBreakpointCounter = 0;

    // Constructors

    // The match-up is a singleton
    private MatchUp(final Input input) {
        field = new GameField();
        player1 = new Player(input.getPlayerOneDecks());
        player2 = new Player(input.getPlayerTwoDecks());
        turnCounter = 1;
    }

    // Methods

    /**
     * Singleton method for class MatchUp
     * @return Returns the MatchUp instance
     */
    public static MatchUp getInstance(final Input input) {
        if (instance == null) {
            return new MatchUp(input);
        }
        return instance;
    }

    /**
     * Resets the entire field
     */
    public void resetMatchUp() {
        field.resetField();
        player1.resetPlayer();
        player2.resetPlayer();
        turnCounter = 1;
    }

    /**
     * This method starts a new game for this MatchUp
     * @param input The game played
     */
    public void startNewGame(final GameInput input) {
        // Choose the deck for the players
        player1.chooseDeck(input.getStartGame().getPlayerOneDeckIdx());
        player2.chooseDeck(input.getStartGame().getPlayerTwoDeckIdx());

        // Choose the Heroes for the players
        player1.setGenericHero(input.getStartGame().getPlayerOneHero());
        player2.setGenericHero(input.getStartGame().getPlayerTwoHero());

        // Set the starting player and shuffle seed
        playerTurn = input.getStartGame().getStartingPlayer();
        shuffleSeed = input.getStartGame().getShuffleSeed();

        // Set the hero instances for the game field
        field.setPlayer1Hero(player1.getGenericHero());
        field.setPlayer2Hero(player2.getGenericHero());

        // Shuffle the player decks
        player1.shufflePlayerDeck(shuffleSeed);
        player2.shufflePlayerDeck(shuffleSeed);

        // Add the first card from the deck to the shuffle
        player1.drawCardFromDeck();
        player2.drawCardFromDeck();

        // Add mana to the players
        player1.receiveMana(turnCounter);
        player2.receiveMana(turnCounter);
    }

    /**
     * Handles the start of a new round
     */
    public void startRound() {
        // Add mana to the players
        player1.receiveMana(turnCounter);
        player2.receiveMana(turnCounter);

        // Draw cards from stockpile
        player1.drawCardFromDeck();
        player2.drawCardFromDeck();

        // Reset the field statuses
        field.resetAttackForCards();
    }

    /**
     * Plays a game and performs debug actions
     * @param actions The actions performed by players and/or debuggers
     */
    public ArrayNode playGame(final ArrayList<ActionsInput> actions) {
        // Create the mapper and the output array
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode output = mapper.createArrayNode();

        // Iterate over the given actions
        for (ActionsInput action : actions) {
            // Create a new object node and add the command to it
            ObjectNode objectNode = mapper.createObjectNode();
            ArrayNode retArrayNode;
            ObjectNode retObjectNode;
            int retInt;
            String retStringError;

            // Execute the correct action
            switch (action.getCommand()) {
                // Gameplay commands
                case "placeCard":
                    retStringError = handlePlaceCard(action);
                    if (retStringError == null) {
                        break;
                    }
                    objectNode.put("command", action.getCommand());
                    objectNode.put("error", retStringError);
                    objectNode.put("handIdx", action.getHandIdx());
                    break;

                case "cardUsesAttack":
                    Point attackerAttackCoords = new Point(action.getCardAttacker().getX(), action.getCardAttacker().getY());
                    Point defenderAttackCoords = new Point(action.getCardAttacked().getX(), action.getCardAttacked().getY());
                    retStringError = handleAttackCard(attackerAttackCoords, defenderAttackCoords);
                    if (retStringError == null) {
                        break;
                    }
                    objectNode.put("command", action.getCommand());
                    objectNode.set("cardAttacker", attackerAttackCoords.toJson());
                    objectNode.set("cardAttacked", defenderAttackCoords.toJson());
                    objectNode.put("error", retStringError);
                    break;

                case "cardUsesAbility":
                    Point attackerAbilityCoords = new Point(action.getCardAttacker().getX(), action.getCardAttacker().getY());
                    Point defenderAbilityCoords = new Point(action.getCardAttacked().getX(), action.getCardAttacked().getY());
                    retStringError =
                            handleUsesAbilityCard(attackerAbilityCoords, defenderAbilityCoords);
                    if (retStringError == null) {
                        break;
                    }
                    objectNode.put("command", action.getCommand());
                    objectNode.set("cardAttacker", attackerAbilityCoords.toJson());
                    objectNode.set("cardAttacked", defenderAbilityCoords.toJson());
                    objectNode.put("error", retStringError);
                    break;

                case "useAttackHero":
                    Point attackerHeroCoords = new Point(action.getCardAttacker().getX(), action.getCardAttacker().getY());
                    retStringError = handleAttackHero(attackerHeroCoords);
                    if (retStringError == null) {
                        break;
                    }
                    if (retStringError.equals("Player one killed the enemy hero.")
                        || retStringError.equals("Player two killed the enemy hero.")) {
                        objectNode.put("gameEnded", retStringError);
                        break;
                    }
                    objectNode.put("command", action.getCommand());
                    objectNode.set("cardAttacker", attackerHeroCoords.toJson());
                    objectNode.put("error", retStringError);
                    break;

                case "useHeroAbility":
                    retStringError = handleHeroAbility(action.getAffectedRow());
                    if (retStringError == null) {
                        break;
                    }
                    objectNode.put("command", action.getCommand());
                    objectNode.put("affectedRow", action.getAffectedRow());
                    objectNode.put("error", retStringError);
                    break;

                case "endPlayerTurn":
                    // TODO: REMOVE THIS LINE
                    System.out.println("ended player turn: " + playerTurn);
                    field.unfreezePlayerCards(playerTurn);
                    System.out.println("Reset freeze: " + playerTurn + "\n");
                    playerTurn = playerTurn == 1 ? 2 : 1;
                    turnCounter++;
                    if (turnCounter % 2 == 1) {
                        //TODO: AND THIS ONE
                        startRound();
                    }
                    break;

                // Statistics
                case "getPlayerOneWins":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("output", player1.getWonGames());
                    break;

                case "getPlayerTwoWins":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("output", player2.getWonGames());
                    break;

                case "getTotalGamesPlayed":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("output", player1.getTotalGames());
                    break;

                // Debug commands

                // Directed at the player
                case "getPlayerDeck":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("playerIdx", action.getPlayerIdx());
                    retArrayNode = action.getPlayerIdx() == 1 ? player1.toJsonPlayerDeck() : player2.toJsonPlayerDeck();
                    objectNode.set("output", retArrayNode);
                    break;

                case "getPlayerHero":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("playerIdx", action.getPlayerIdx());
                    retObjectNode = action.getPlayerIdx() == 1 ? player1.toJsonPlayerHero() : player2.toJsonPlayerHero();
                    objectNode.set("output", retObjectNode);
                    break;

                case "getCardsInHand":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("playerIdx", action.getPlayerIdx());
                    retArrayNode = action.getPlayerIdx() == 1 ? player1.toJsonPlayerHand() : player2.toJsonPlayerHand();
                    objectNode.set("output", retArrayNode);
                    break;

                case "getPlayerMana":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("playerIdx", action.getPlayerIdx());
                    retInt = action.getPlayerIdx() == 1 ? player1.getMana() : player2.getMana();
                    objectNode.put("output", retInt);
                    break;

                case "getPlayerTurn":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("output", playerTurn);
                    break;

                // Directed at the field

                case "getCardAtPosition":
                    objectNode.put("command", action.getCommand());
                    objectNode.put("x", action.getX());
                    objectNode.put("y", action.getY());
                    Point point = new Point(action.getX(), action.getY());
                    retObjectNode = handleGetCardAtPosition(point);
                    if (retObjectNode == null) {
                        objectNode.put("output", "No card available at that position.");
                        break;
                    }
                    objectNode.set("output", retObjectNode);
                    break;

                case "getCardsOnTable":
                    objectNode.put("command", action.getCommand());
                    retArrayNode = field.printAllCardsOnTable();
                    objectNode.set("output", retArrayNode);
                    break;

                case "getFrozenCardsOnTable":
                    objectNode.put("command", action.getCommand());
                    retArrayNode = field.printAllFrozenCardsOnTable();
                    objectNode.set("output", retArrayNode);
                    break;

                case "breakpoint":
                    debugBreakpointCounter++;
                    break;

                default:
                    System.out.println("Error: Command not recognised.");
                    System.out.println("Command: " + action.getCommand());
            }

            if (!objectNode.isEmpty()) {
                output.add(objectNode);
            }
        }

        return output;
    }

    /**
     * Handles the placement of cards on the field
     * @param action The command that was given
     * @return Null on success or an error string on failure
     */
    public String handlePlaceCard(final ActionsInput action) {
        // Get the card from hand
        int handIndex = action.getHandIdx();
        GenericCard card = playerTurn == 1 ? player1.placeCardFromHand(handIndex) : player2.placeCardFromHand(handIndex);
        if (card == null) {
            return "Not enough mana to place card on table.";
        }

        //   Check if we can place the card and return the card to the hand of the player if we cant
        int rowAffected = card.getRowPlacement(playerTurn);
        if (field.getRowOccupancy(rowAffected) == GameConstants.TABLE_COLUMNS) {
            if (playerTurn == 1) {
                player1.returnCardToHand(card, handIndex);
            } else {
                player2.returnCardToHand(card, handIndex);
            }
            return "Cannot place card on table since row is full.";
        }
        field.addCard(card, rowAffected);
        return null;
    }

    /**
     * Handles the attack command for a card
     * @param attackerCoords The coordinates of the attacking card
     * @param defenderCoords The coordinates of the defender card
     * @return Null on success or an error string on failure
     */
    public String handleAttackCard(final Point attackerCoords, final Point defenderCoords) {
        GenericCard attackerCard = field.getCard(attackerCoords);
        GenericCard defenderCard = field.getCard(defenderCoords);
        if (attackerCard == null || defenderCard == null) {
            return "Card not found.";
        }
// TODO: REMOVE THIS
//        System.out.println("attacker - x: " + attackerCoords.getRow() +" y: " + attackerCoords.getColumn());
//        System.out.println("defender - x: " + defenderCoords.getRow() +" y: " + defenderCoords.getColumn());
//        System.out.println("Card name: " + attackerCard.getName());

        // Required checks
        if (!field.isEnemy(defenderCoords, playerTurn)) {
            return "Attacked card does not belong to the enemy.";
        }

        if (attackerCard.isHasAttacked()) {
            return "Attacker card has already attacked this turn.";
        }

        if (attackerCard.isFrozen()) {
            return "Attacker card is frozen.";
        }

        if (field.getTanksOnRow(playerTurn) != 0 && !defenderCard.isTank()) {
            return "Attacked card is not of type 'Tank'.";
        }

        // Main logic of the function
        int attackDealt = attackerCard.attack(defenderCard);
        // If we dealt less damage then normal, then we killed the card
        if (attackDealt < attackerCard.getAttackDamage()) {
            field.removeCard(defenderCoords);
        }

        return null;
    }

    /**
     * Handles the ability command for a card
     * @param attackerCoords The coordinates of the attacking card
     * @param defenderCoords The coordinates of the defender card
     * @return Null on success or an error string on failure
     */
    public String handleUsesAbilityCard(final Point attackerCoords, final Point defenderCoords) {
        GenericCard attackerCard = field.getCard(attackerCoords);
        GenericCard defenderCard = field.getCard(defenderCoords);
        if (attackerCard == null || defenderCard == null) {
            return "Card not found.";
        }

        // TODO: Remove this
//        System.out.println("attacker - x: " + attackerCoords.getRow() +" y: " + attackerCoords.getColumn());
//        System.out.println("defender - x: " + defenderCoords.getRow() +" y: " + defenderCoords.getColumn());
//        System.out.println("ATT card: " + attackerCard.getAttackDamage());
//        System.out.println("hasAttacked: " + attackerCard.isHasAttacked());
//        System.out.println("HP card: " + defenderCard.getHealth());
//        System.out.println("Card name: " + attackerCard.getName());

        // Required checks
        if (attackerCard.isFrozen()) {
            return "Attacker card is frozen.";
        }

        if (attackerCard.isHasAttacked()) {
            return "Attacker card has already attacked this turn.";
        }

        // Check if the card is an ally for the ability of Disciple
        if (attackerCard.getName().equals("Disciple")
            && field.isEnemy(defenderCoords, playerTurn)) {
            return "Attacked card does not belong to the current player.";
        }

        // Check if the card is an enemy for the abilities of The Ripper/Miraj/Cursed One
        if (attackerCard.getName().equals("The Ripper")
            || attackerCard.getName().equals("Miraj")
            || attackerCard.getName().equals("The Cursed One")) {
            if (!field.isEnemy(defenderCoords, playerTurn)) {
                return "Attacked card does not belong to the enemy.";
            }

            if (attackerCard.getName().equals("The Ripper")
                || attackerCard.getName().equals("Miraj")) {
                if (field.getTanksOnRow(playerTurn) != 0 && !defenderCard.isTank()) {
                    return "Attacked card is not of type 'Tank'.";
                }
            }
        }

        // Add the card to the list and pass it to the useAbility command
        ArrayList<GenericCard> defenderCards = new ArrayList<>(1);
        defenderCards.add(defenderCard);

        attackerCard.useAbility(defenderCards);
        // Check if The Cursed One has killed the card
        if (attackerCard.getName().equals("The Cursed One") && defenderCard.getHealth() == 0) {
            field.removeCard(defenderCoords);
        }

        return null;
    }

    /**
     * Handles the attack of cards on enemy hero
     * @param attackerCoords The coords of the attacking card
     * @return Null on success, victory string on game end, error string on failure
     */
    public String handleAttackHero(final Point attackerCoords) {
        GenericCard attackerCard = field.getCard(attackerCoords);
        if (attackerCard == null) {
            return "Card not found.";
        }

        // Required checks for game mechanics
        if (attackerCard.isFrozen()) {
            return "Attacker card is frozen.";
        }
        if (attackerCard.isHasAttacked()) {
            return "Attacker card has already attacked this turn.";
        }
        if (field.getTanksOnRow(playerTurn) != 0) {
            return "Attacked card is not of type 'Tank'.";
        }

        // Main logic of the function
        GenericHero genericHero = playerTurn == 1 ? field.getPlayer2Hero() : field.getPlayer1Hero();

        int attackDealt = attackerCard.attack(genericHero);
        // If we dealt less damage then normal, then we killed the card
        if (attackDealt < attackerCard.getAttackDamage()) {
            if (playerTurn == 1) {
                player1.winGame();
                player2.loseGame();
                return "Player one killed the enemy hero.";
            } else {
                player1.loseGame();
                player2.winGame();
                return "Player two killed the enemy hero.";
            }
        }

        return null;
    }

    /**
     * Handles the usage of abilities by heroes
     * @param affectedRow The row affected by the hero ability
     * @return Null on success, error string on failure
     */
    public String handleHeroAbility(int affectedRow) {
        GenericHero genericHero = playerTurn == 1 ? field.getPlayer1Hero() : field.getPlayer2Hero();
        Player currentPlayer = playerTurn == 1 ? player1 : player2;

        // Required checks for game mechanics
        if (genericHero.getMana() > currentPlayer.getMana()) {
            return "Not enough mana to use hero's ability.";
        }
        if (genericHero.isHasAttacked()) {
            return "Hero has already attacked this turn.";
        }

        Point rowCoordinates = new Point(affectedRow, 0);
        if ((genericHero.getName().equals("Lord Royce")
                || genericHero.getName().equals("Empress Thorina"))
                && !field.isEnemy(rowCoordinates, playerTurn)) {
            return "Selected row does not belong to the enemy.";
        }
        if ((genericHero.getName().equals("General Kocioraw")
                || genericHero.getName().equals("King Mudface"))
                && field.isEnemy(rowCoordinates, playerTurn)) {
            return "Selected row does not belong to the current player.";
        }

        // Main logic of the function
        currentPlayer.expendMana(genericHero.getMana());
        ArrayList<GenericCard> modifiedCards = field.getRowCards(affectedRow);
        GenericCard destroyedCard = genericHero.useAbility(modifiedCards);
        if (destroyedCard == null) {
            return null;
        }

        // Remove the destroyed card from the field
        // TODO: REMOVE THIS FROM FIELD
//        System.out.println("HP card: " + destroyedCard.getHealth());
//        System.out.println("Mana Card: " + destroyedCard.getMana());
//        System.out.println("ATT card: " + destroyedCard.getAttackDamage());
//        System.out.println("Card name: " + destroyedCard.getName());

        // We have to search the opposite player's half of
        // the table to be sure we get the right card
        int startingRow = playerTurn == 1 ? 0 : 2;
        Point destroyedCoords = field.getCardPosition(destroyedCard, startingRow);
        if (destroyedCoords == null) {
            return "Something has gone wrong.";
        }
        field.removeCard(destroyedCoords);
        return null;
    }

    /**
     * Finds the card at the coordinates given
     * @param point The coordinates of the card
     * @return Returns the card that need to be displayed or null on failure
     */
    public ObjectNode handleGetCardAtPosition(final Point point) {
        GenericCard card = field.getCard(point);
        if (card == null) {
            return null;
        }
        return card.printCard();
    }
}
