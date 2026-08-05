package com.github.themrpuffin.villagerenhanced.dialogue;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The choices a player can make in a dialogue.
 *
 * <p>Hardcoded for now; M7 replaces this with data-driven dialogue nodes.
 */
public enum DialogueOption {
    /** Hand off to the vanilla merchant screen. */
    TRADE,
    /** Close the dialogue. */
    LEAVE;

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
