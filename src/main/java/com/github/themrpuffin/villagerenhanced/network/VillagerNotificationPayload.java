package com.github.themrpuffin.villagerenhanced.network;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: something happened to a villager nearby that the player may want to know
 * about.
 *
 * <p>Sent as a payload rather than a plain chat message so the player can turn these off for
 * themselves. A chat message from the server cannot be filtered by the receiving client; this
 * can. The server still decides whether to send anything at all, so both a server owner and an
 * individual player can switch it off.
 *
 * <p>The message is composed per recipient, since names depend on who has been introduced to
 * whom — one player may hear "Mira" where another hears "the Librarian".
 *
 * @param message the finished, translated line to show
 */
public record VillagerNotificationPayload(Component message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VillagerNotificationPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(VillagerEnhanced.MODID, "villager_notification"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerNotificationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ComponentSerialization.STREAM_CODEC, VillagerNotificationPayload::message,
                    VillagerNotificationPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
