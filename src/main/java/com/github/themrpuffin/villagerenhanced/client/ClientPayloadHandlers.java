package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ClientConfig;
import com.github.themrpuffin.villagerenhanced.network.DialogueClosedPayload;
import com.github.themrpuffin.villagerenhanced.network.OpenDialoguePayload;
import com.github.themrpuffin.villagerenhanced.network.VillagerNotificationPayload;

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
        event.register(VillagerNotificationPayload.TYPE, ClientPayloadHandlers::handleNotification);
    }

    /**
     * Something happened to a villager nearby.
     *
     * <p>Shown in chat rather than the action bar: these arrive unprompted and are worth being
     * able to scroll back to, where an action-bar line vanishes in seconds.
     *
     * <p>This is the point of sending a payload rather than a chat message from the server —
     * the player gets to decide whether they want these at all.
     */
    private static void handleNotification(VillagerNotificationPayload payload, IPayloadContext context) {
        if (!ClientConfig.SHOW_NOTIFICATIONS.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(payload.message());
        }
    }

    /**
     * The server has sent a page of dialogue: show it.
     *
     * <p>If a dialogue with this villager is already open, the page is swapped **in place**.
     * Opening a replacement screen would make {@code Gui#setScreen} call {@code removed()} on
     * the outgoing one, which sends a {@code CloseDialoguePayload} and ends the session the new
     * page belongs to — leaving every subsequent click rejected.
     *
     * <p>Nothing to validate — unlike the serverbound direction, this comes from the authority.
     */
    private static void handleOpenDialogue(OpenDialoguePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (minecraft.gui.screen() instanceof VillagerDialogueScreen open
                && open.isFor(payload.villagerId())) {
            open.update(payload);
        } else {
            minecraft.gui.setScreen(new VillagerDialogueScreen(payload));
        }
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
