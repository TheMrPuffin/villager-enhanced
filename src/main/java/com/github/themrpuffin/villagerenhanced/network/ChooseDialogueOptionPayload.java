package com.github.themrpuffin.villagerenhanced.network;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOption;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: "I clicked this option while talking to this villager."
 *
 * <p>Carries the villager's entity id rather than its UUID: far smaller on the wire, and the
 * server can resolve it directly. It is not a secret and it is not trusted — see
 * {@link ServerPayloadHandlers} for the checks applied before anything is acted on.
 */
public record ChooseDialogueOptionPayload(int villagerId, DialogueOption option) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChooseDialogueOptionPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(VillagerEnhanced.MODID, "choose_dialogue_option"));

    /** Field order must match the record's, since fields are read back in write order. */
    public static final StreamCodec<ByteBuf, ChooseDialogueOptionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ChooseDialogueOptionPayload::villagerId,
                    DialogueOption.STREAM_CODEC, ChooseDialogueOptionPayload::option,
                    ChooseDialogueOptionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
