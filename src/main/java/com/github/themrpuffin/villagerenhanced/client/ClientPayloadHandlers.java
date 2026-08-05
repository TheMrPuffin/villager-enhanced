package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.network.DialogueClosedPayload;
import com.github.themrpuffin.villagerenhanced.network.OpenDialoguePayload;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side handling of messages sent by the server.
 *
 * <p>Registered through {@code RegisterClientPayloadHandlersEvent} rather than alongside the
 * serverbound handlers, so nothing here is ever referenced from common code. The dedicated
 * server never loads this class and so never tries to load {@code Screen}.
 *
 * <p>NeoForge dispatches these on the main client thread, so touching the screen is safe.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID, value = Dist.CLIENT)
public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {}

    @SubscribeEvent
    public static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenDialoguePayload.TYPE, ClientPayloadHandlers::handleOpenDialogue);
        event.register(DialogueClosedPayload.TYPE, ClientPayloadHandlers::handleDialogueClosed);
    }

    /**
     * The server has started a dialogue: open the window and draw what it sent.
     *
     * <p>Nothing to validate — unlike the serverbound direction, this comes from the authority.
     */
    private static void handleOpenDialogue(OpenDialoguePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.gui.setScreen(new VillagerDialogueScreen(payload, minecraft.player.getName()));
    }

    /**
     * The server ended the conversation: close the window and say why.
     *
     * <p>The check that the current screen is ours matters — by now the player may have moved
     * on to their inventory or the trade screen, and closing whatever happens to be open would
     * be its own bug.
     */
    private static void handleDialogueClosed(DialogueClosedPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.gui.screen() instanceof VillagerDialogueScreen dialogue) {
            dialogue.closeFromServer();
        }

        // Shown once the screen has gone, where the action bar is readable.
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(payload.reason());
        }
    }
}
