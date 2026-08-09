package com.github.themrpuffin.villagerenhanced.dialogue;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import org.jspecify.annotations.Nullable;

/**
 * The choices a player can make in a dialogue, and how much a villager must like them first.
 *
 * <p><b>Declaration order is part of the network protocol</b> — options travel as ordinals, so
 * new entries go at the end and existing ones are never reordered.
 *
 * <p>Hardcoded for now; 1.0 replaces this with data-driven dialogue nodes, where the standing
 * requirement becomes a datapack condition rather than a field here.
 */
public enum DialogueOption {
    /** Hand off to the vanilla merchant screen. */
    TRADE(null),
    /** Close the dialogue. */
    LEAVE(null),
    /**
     * Ask how this villager regards you.
     *
     * <p>Never gated. Someone telling you exactly how little they think of you is in character,
     * and it is the feedback that makes the rest of the system legible — hiding it when a player
     * most needs it would be perverse.
     */
    VIEW_REPUTATION(null),
    /** Return to the previous page. */
    BACK(null),
    /**
     * Give the villager whatever is in your main hand.
     *
     * <p>Never gated. Gifts are the route back from a bad reputation; locking them would strand
     * a player who made one mistake with no way to make amends.
     */
    GIFT(null),
    /** Ask what the other villagers nearby think of you — confided only to people they like. */
    RUMOURS(ReputationTier.ACQUAINTANCE),
    /** Ask the villager their name. Refused by anyone who actively resents you. */
    ASK_NAME(ReputationTier.STRANGER),
    /**
     * Ask what they do for a living.
     *
     * <p>Never gated. Asking someone about their work is the most basic small talk there is, and
     * it doubles as how a new player discovers what the mod does.
     */
    ASK_ABOUT_WORK(null),
    /** Ask what they are carrying — personal enough to need real trust. */
    SHOW_BELONGINGS(ReputationTier.TRUSTED),
    /**
     * Open the list of things you can ask about.
     *
     * <p>Never gated: the questions behind it carry their own requirements, and a branch that
     * refuses to open tells the player nothing about why.
     */
    TOPICS(null);

    /** Lowest standing at which this option is available, or null if it is never gated. */
    private final @Nullable ReputationTier minimumStanding;

    DialogueOption(@Nullable ReputationTier minimumStanding) {
        this.minimumStanding = minimumStanding;
    }

    /**
     * Does the player's standing with this villager unlock this option?
     *
     * <p>Only answers the standing question. Options may have their own reasons to be
     * unavailable — no trades to make, nothing in hand to give — which the caller applies
     * alongside this.
     *
     * <p>Note each option currently has at most one reason to be disabled, which is why a single
     * fixed tooltip per option is accurate. An option gated on both standing and context would
     * need its tooltip chosen at build time instead.
     */
    public boolean isUnlockedAt(ReputationTier standing) {
        return this.minimumStanding == null || standing.isAtLeast(this.minimumStanding);
    }

    /** {@code values()} allocates a new array per call, and this is read for every packet. */
    private static final DialogueOption[] VALUES = values();

    /**
     * Travels over the wire as an ordinal.
     *
     * <p>The bounds check is not optional: a modified client can send any integer, and without
     * it an out-of-range value throws {@code ArrayIndexOutOfBoundsException} deep inside the
     * network layer instead of failing cleanly here.
     */
    public static final StreamCodec<ByteBuf, DialogueOption> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DialogueOption::byId, DialogueOption::ordinal);

    private static DialogueOption byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            throw new IllegalArgumentException("Unknown dialogue option id: " + id);
        }
        return VALUES[id];
    }
}
