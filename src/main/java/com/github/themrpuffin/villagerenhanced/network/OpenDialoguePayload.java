package com.github.themrpuffin.villagerenhanced.network;

import java.util.List;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOptionEntry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: "open a dialogue with this villager, and here is what to draw."
 *
 * <p>The server decides a conversation has started; the client renders what it is told rather
 * than deciding for itself. Everything the screen displays arrives here.
 *
 * <p>The buffer is a {@code RegistryFriendlyByteBuf} rather than a plain {@code ByteBuf}
 * because sending a {@code Component} needs registry access — text can embed item references.
 *
 * <p>Also used to <b>update</b> an already-open dialogue when the player navigates to another
 * page. The client must update its screen in place rather than opening a new one — replacing a
 * screen fires {@code removed()} on the old one, which would send a {@code CloseDialoguePayload}
 * and end the very session the new page belongs to.
 *
 * @param villagerId     entity id, so the client can name the villager when replying
 * @param villagerName   display name, resolved server-side
 * @param professionName profession display name, e.g. "Farmer"
 * @param villagerLevel  trade level, 1–5
 * @param bodyLines      what the villager is saying, composed server-side, one entry per line.
 *                       A list rather than one Component because pages like rumours are
 *                       naturally several separate statements, and {@code MultiLineLabel}
 *                       renders each entry as its own line while still wrapping long ones.
 * @param options        what this villager offers here, and which options are usable
 */
public record OpenDialoguePayload(
        int villagerId,
        Component villagerName,
        Component professionName,
        int villagerLevel,
        List<Component> bodyLines,
        List<DialogueOptionEntry> options) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenDialoguePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(VillagerEnhanced.MODID, "open_dialogue"));

    /**
     * {@code .apply(ByteBufCodecs.list())} turns a single-entry codec into a list codec,
     * writing the length first. Field order must match the record's.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenDialoguePayload::villagerId,
                    ComponentSerialization.STREAM_CODEC, OpenDialoguePayload::villagerName,
                    ComponentSerialization.STREAM_CODEC, OpenDialoguePayload::professionName,
                    ByteBufCodecs.VAR_INT, OpenDialoguePayload::villagerLevel,
                    ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenDialoguePayload::bodyLines,
                    DialogueOptionEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenDialoguePayload::options,
                    OpenDialoguePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
