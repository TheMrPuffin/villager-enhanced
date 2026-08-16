package com.github.themrpuffin.villagerenhanced.dialogue;

/**
 * Which part of a conversation a player is looking at.
 *
 * <p>Stored in the {@link DialogueSession}, because the options a villager offers depend on it
 * and the server cannot work out from the world alone which page somebody navigated to.
 *
 * <p>Hardcoded for now; 1.0 replaces these with data-driven dialogue nodes.
 */
public enum DialoguePage {
    /** The opening page: what a villager says when you first speak to them. */
    GREETING,
    /**
     * The greeting, with the villager actually giving their name.
     *
     * <p>Shown once, immediately after being asked. Otherwise the only sign anything happened is
     * the window title quietly changing, which makes asking feel like it did nothing. Offers the
     * same options as the greeting, so it functions as one.
     */
    INTRODUCTION,
    /** The list of things you can ask about, which keeps the greeting from filling with them. */
    TOPICS,
    /** How this villager regards the player. */
    REPUTATION,
    /** What the other villagers nearby think of the player. */
    RUMOURS,
    /** What this villager does for a living. */
    WORK,
    /** What this villager is carrying. */
    BELONGINGS,
    /**
     * What a villager says just after accepting a diamond apple.
     *
     * <p>Exists for the same reason {@link #INTRODUCTION} does. Levelling someone up otherwise
     * changes a number in the window subtitle and nothing else, which reads as though the apple
     * was swallowed for no effect. Having them say what changed makes it land.
     *
     * <p>Adding a page is <b>not</b> a protocol change — pages never cross the wire; the payload
     * carries composed lines and options, not which page produced them.
     */
    TUTORED,
    /**
     * Why a villager would not take the diamond apple you offered.
     *
     * <p>A separate page because the apple is <b>not consumed</b> when refused — sixteen valuable
     * items quietly becoming a small reputation bump would be a trap, so the player is told
     * instead. The reason is recomputed from the villager and the player's standing rather than
     * passed in, since everything it depends on is still true when the page is built.
     */
    APPLE_REFUSED;

    /**
     * Where "Back" leads from here.
     *
     * <p>Written as a switch rather than a constructor field because an enum constant cannot be
     * referenced from another constant's constructor arguments.
     */
    public DialoguePage parent() {
        return switch (this) {
            // Already the root; Back is never offered on any of them. TUTORED is reached by
            // acting rather than navigating, so its Back goes to where the action was taken.
            case GREETING, INTRODUCTION, TUTORED, APPLE_REFUSED -> GREETING;
            case TOPICS -> GREETING;
            case REPUTATION, RUMOURS, WORK, BELONGINGS -> TOPICS;
        };
    }
}
