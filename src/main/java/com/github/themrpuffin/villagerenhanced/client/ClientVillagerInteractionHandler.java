package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.dialogue.VillagerInteractionHandler;

import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Stops the client predicting a vanilla trade when we are taking the interaction over.
 *
 * <p>This class opens nothing. The server sends an {@code OpenDialoguePayload} and
 * {@link ClientPayloadHandlers} opens the screen when it arrives; all that happens here is
 * suppressing the client's local guess, so nothing flickers before the server replies.
 *
 * <p>{@code Dist.CLIENT} means NeoForge only registers this on the physical client, so a
 * dedicated server never loads it.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID, value = Dist.CLIENT)
public final class ClientVillagerInteractionHandler {

    private ClientVillagerInteractionHandler() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Even on the physical client this fires twice in single-player: once for the client
        // world, once for the built-in server's.
        if (!event.getLevel().isClientSide()) {
            return;
        }

        // The same gate the server uses, so the two sides always agree.
        if (!VillagerInteractionHandler.shouldOpenDialogue(event)) {
            return;
        }

        // Vanilla sends the interact packet before running this logic, so the server still gets
        // its own copy of the event. Cancelling here only stops the client predicting a trade.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
