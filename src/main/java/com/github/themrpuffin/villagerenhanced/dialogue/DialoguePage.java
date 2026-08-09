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
    BELONGINGS;

    /**
     * Where "Back" leads from here.
     *
     * <p>Written as a switch rather than a constructor field because an enum constant cannot be
     * referenced from another constant's constructor arguments.
     */
    public DialoguePage parent() {
        return switch (this) {
            // Already the root; Back is never offered on either.
            case GREETING, INTRODUCTION -> GREETING;
            case TOPICS -> GREETING;
            case REPUTATION, RUMOURS, WORK, BELONGINGS -> TOPICS;
        };
    }
}
