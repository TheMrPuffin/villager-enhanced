package com.github.themrpuffin.villagerenhanced.dialogue;

/**
 * Which part of a conversation the player is currently looking at.
 *
 * <p>Tracked server-side in the {@link DialogueSession}, because the options a villager offers
 * depend on it — "Back" only exists on the reputation page, "Trade" only on the greeting. The
 * server cannot validate a returning choice without knowing which page produced it.
 *
 * <p>M7 replaces this enum with data-driven nodes loaded from a datapack.
 */
public enum DialoguePage {
    /** The opening page: greeting, trade, ask about standing. */
    GREETING,
    /** How this villager regards the player. */
    REPUTATION,
    /** What the other villagers nearby think of the player. */
    RUMOURS,
    /** What this villager does for a living. */
    WORK,
    /** What this villager is carrying. */
    BELONGINGS
}
