package gameobjects.cards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fileio.CardInput;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter @Setter
public class GenericCard {
    private int mana;
    private int health;
    private int attackDamage;
    private boolean hasAttacked;
    private boolean isDead;
    private boolean isFrozen;
    private boolean isTank;

    // Constants
    private String name;
    private String description;
    private ArrayList<String> colors;

    // Constructors
    public GenericCard() {
        mana = 0;
        health = 0;
        attackDamage = 0;
        description = "";
        name = "";
        colors = new ArrayList<>();
        hasAttacked = false;
        isFrozen = false;
        isDead = false;
    }

    public GenericCard(final int mana, final int health, final int attackDamage,
                       final String name, final String description,
                       final ArrayList<String> colors) {
        this.mana = mana;
        this.health = health;
        this.attackDamage = attackDamage;

        this.name = name;
        this.description = description;
        this.colors = colors;

        this.hasAttacked = false;
        this.isFrozen = false;
        this.isDead = false;
        this.isTank = false;
    }

    /**
     * CardInput to GenericCard constructor
     * @param input The input from the JSON
     */
    public GenericCard(final CardInput input) {
        this.mana = input.getMana();
        this.health = input.getHealth();
        this.attackDamage = input.getAttackDamage();

        this.name = input.getName();
        this.description = input.getDescription();
        this.colors = input.getColors();

        this.hasAttacked = false;
        this.isFrozen = false;
        this.isDead = false;
    }

    /**
     * Copy constructor
     */
    public GenericCard(final GenericCard genericCard) {
        this.mana = genericCard.getMana();
        this.health = genericCard.getHealth();
        this.attackDamage = genericCard.attackDamage;

        this.name = genericCard.name;
        this.description = genericCard.description;
        this.colors = genericCard.colors;

        this.hasAttacked = genericCard.hasAttacked;
        this.isFrozen = genericCard.isFrozen;
        this.isDead = genericCard.isDead;
        this.isTank = genericCard.isTank;
    }

    // Methods

    /**
     * Receive damage specified
     * @param damage The number of damage points dealt
     * @return Returns the amount of damage taken
     */
    public int receiveDamage(final int damage) {
        if (this.health - damage <= 0) {
            this.health = 0;
            this.isDead = true;
            return this.health;
        }

        this.health -= damage;
        return damage;
    }

    /**
     * Attacks an enemy card
     * @param enemy The enemy that will be attacked
     * @return The attack value dealt
     */
    public int attack(final GenericCard enemy) {
        this.hasAttacked = true;
        return enemy.receiveDamage(this.attackDamage);
    }

    /**
     * Freezes an enemy card
     * @param enemy The enemy minion tha will be frozen
     */
    public void freeze(final GenericCard enemy) {
        enemy.isFrozen = true;
    }

    /**
     * Gives the row placement of the card depending on the player
     * ATTENTION: THIS METHOD SHOULD NEVER BE ACCESSED
     * @param playerIndex The index of the player that places the card
     * @return The row placement of the card defined by the game mechanics
     */
    public int getRowPlacement(final int playerIndex) {
        return 0;
    }

    /**
     * Resets a card after the turn is over
     */
    public void reset() {
        this.hasAttacked = false;
        this.isFrozen = false;
    }

    /**
     * Prints the card content in a specific JSON format
     * @return Returns the ObjectNode with the information requested
     */
    public ObjectNode printCard() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode cardNode = mapper.createObjectNode();
        cardNode.put("mana", mana);
        cardNode.put("attackDamage", attackDamage);
        cardNode.put("health", health);
        cardNode.put("description", description);

        // Create a new mapper for colors
        ObjectMapper colorMapper = new ObjectMapper();
        ArrayNode colorsArray = colorMapper.createArrayNode();
        for (String color : colors) {
            colorsArray.add(color);
        }

        cardNode.set("colors", colorsArray);

        cardNode.put("name", name);

        return cardNode;
    }

}