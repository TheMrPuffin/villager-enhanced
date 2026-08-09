package com.github.themrpuffin.villagerenhanced.network;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's network messages.
 *
 * <p>A payload cannot be sent until registered, and both sides must agree on the registration,
 * so this runs on the client and the dedicated server alike.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class VillagerEnhancedNetwork {

    /**
     * Bump whenever a payload's fields change, so an old client meeting a new server is
     * rejected at connect time rather than misreading bytes later.
     */
    // 2: OpenDialoguePayload replaced its raw reputation int with a server-composed body
    //    Component, and DialogueOption gained VIEW_REPUTATION and BACK.
    // 3: that body became a list of lines, and DialogueOption gained GIFT and RUMOURS.
    // 4: DialogueOption gained ASK_NAME.
    // 5: DialogueOption gained ASK_ABOUT_WORK and SHOW_BELONGINGS.
    private static final String NETWORK_VERSION = "5";

    private VillagerEnhancedNetwork() {}

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

        // Declaring the direction means NeoForge rejects a payload arriving the wrong way
        // round, which is one less thing to validate by hand.
        registrar.playToServer(
                ChooseDialogueOptionPayload.TYPE,
                ChooseDialogueOptionPayload.STREAM_CODEC,
                ServerPayloadHandlers::handleChooseOption);

        registrar.playToServer(
                CloseDialoguePayload.TYPE,
                CloseDialoguePayload.STREAM_CODEC,
                ServerPayloadHandlers::handleCloseDialogue);

        // Clientbound payloads are registered here WITHOUT handlers, deliberately: their
        // handlers open Screens, so they live in a client-only class. Naming them here would
        // drag client code onto the dedicated server and crash it on load. The client attaches
        // its own via RegisterClientPayloadHandlersEvent -- see client/ClientPayloadHandlers.
        // Both sides still register type and codec, which keeps the protocol in agreement.
        registrar.playToClient(
                OpenDialoguePayload.TYPE,
                OpenDialoguePayload.STREAM_CODEC);

        registrar.playToClient(
                DialogueClosedPayload.TYPE,
                DialogueClosedPayload.STREAM_CODEC);
    }
}
