package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Decides what right-clicking a villager does, and stops vanilla opening its trade screen when
 * we are taking the interaction over.
 *
 * <p>Common code: loaded on the client and the dedicated server alike, so it must never
 * reference client-only types. The client half lives in
 * {@code com.github.themrpuffin.villagerenhanced.client.ClientVillagerInteractionHandler}.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class VillagerInteractionHandler {

    private VillagerInteractionHandler() {}

    /**
     * Is this a click we care about — main hand, on a live villager?
     *
     * <p>Says nothing about sneaking, because sneaking changes <i>what</i> we do rather than
     * <i>whether</i> we act.
     */
    private static boolean isDialogueTarget(PlayerInteractEvent.EntityInteract event) {
        // The master switch. False leaves the mod installed but dormant, so vanilla trading
        // works exactly as it always did.
        if (!ServerConfig.DIALOGUE_ENABLED.get()) {
            return false;
        }

        // A single right-click fires this event once per hand.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        // Villager rather than AbstractVillager, so Wandering Traders stay out of scope.
        if (!(event.getTarget() instanceof Villager villager)) {
            return false;
        }

        return villager.isAlive();
    }

    /**
     * Should this click open the dialogue screen?
     *
     * <p>Called by both the server handler below and the client handler, so the two sides can
     * never disagree about which clicks we take over.
     */
    public static boolean shouldOpenDialogue(PlayerInteractEvent.EntityInteract event) {
        // Sneaking means "skip the conversation and just trade", handled on the server.
        return isDialogueTarget(event) && !event.getEntity().isSecondaryUseActive();
    }

    /**
     * Server-side half.
     *
     * <p>Cancelling stops {@code Villager#mobInteract}, which is what would otherwise open the
     * vanilla trade screen.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Fires on both the client and server copies of the world; only the server acts.
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!isDialogueTarget(event)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Villager villager = (Villager) event.getTarget();

        // Somebody else is already talking to or trading with this villager. Vanilla's response
        // to a busy villager is to do nothing; we cancel and explain rather than stealing them.
        if (DialogueSessionManager.isBusyWithSomeoneElse(villager, player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            // No screen opens for this player, so the action bar is visible.
            player.sendOverlayMessage(Component.translatable("villagerenhanced.dialogue.busy"));
            return;
        }

        event.setCanceled(true);
        // Cancelled events report this to vanilla instead. SUCCESS means "handled, stop looking
        // for something else to do"; the default PASS would let the game try the held item.
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (player.isSecondaryUseActive()) {
            // Sneak shortcut: straight to trading. Note this is us opening the trade screen
            // deliberately -- vanilla's own response to a sneaking player is to do nothing,
            // which is also what happens when the shortcut is switched off.
            if (ServerConfig.SNEAK_TO_TRADE.get()) {
                VillagerTrading.openTradeScreen(player, villager);
            }
            return;
        }

        openDialogue(player, villager);
    }

    /** Starts a conversation and sends the opening page. */
    private static void openDialogue(ServerPlayer player, Villager villager) {
        // Register before announcing: this marks the villager busy, so a second player cannot
        // slip in between the session opening and the payload being sent.
        DialogueSessionManager.open(player, villager);
        DialogueBuilder.send(player, villager, DialoguePage.GREETING);

        // After sending, because the greeting depends on how long it had been since the last
        // conversation. Noting it first would mean nobody was ever greeted as long-absent.
        VillagerMemory.noteConversation(villager, player);

        VillagerEnhanced.LOGGER.debug(
                "Opened dialogue: player={} villager={} profession={} level={} reputation={}",
                player.getName().getString(),
                villager.getUUID(),
                villager.getVillagerData().profession().getRegisteredName(),
                villager.getVillagerData().level(),
                villager.getPlayerReputation(player));
    }
}
