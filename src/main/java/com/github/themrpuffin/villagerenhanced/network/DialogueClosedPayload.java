package com.github.themrpuffin.villagerenhanced.network;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: "the conversation is over, and here is why."
 *
 * <p>The server ends conversations the player did not ask to end — the villager died, they
 * walked out of range, they changed dimension. Without this the screen would sit there looking
 * functional while every button was silently refused.
 *
 * <p>The reason shows on the action bar as the screen closes: invisible <i>behind</i> a screen,
 * perfectly readable once it has gone.
 *
 * @param reason player-facing explanation, translated server-side into a Component
 */
public record DialogueClosedPayload(Component reason) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DialogueClosedPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(VillagerEnhanced.MODID, "dialogue_closed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueClosedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ComponentSerialization.STREAM_CODEC, DialogueClosedPayload::reason,
                    DialogueClosedPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
