package com.github.themrpuffin.villagerenhanced.network;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: "I closed the dialogue window."
 *
 * <p>Sent whenever the screen goes away — Leave, Escape, or being replaced. Without it the
 * villager would stay marked busy until the tick check noticed the player had wandered off,
 * which could be a long time if they simply pressed Escape and stood still.
 *
 * <p>Carries no fields deliberately. The server already knows which conversation this player is
 * in, and accepting a villager id here would only invite someone to send a different one.
 */
public record CloseDialoguePayload() implements CustomPacketPayload {

    /** The record has no fields, so every instance is identical. */
    public static final CloseDialoguePayload INSTANCE = new CloseDialoguePayload();

    public static final CustomPacketPayload.Type<CloseDialoguePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(VillagerEnhanced.MODID, "close_dialogue"));

    /** {@code unit} writes nothing and always decodes to the same value. */
    public static final StreamCodec<ByteBuf, CloseDialoguePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
