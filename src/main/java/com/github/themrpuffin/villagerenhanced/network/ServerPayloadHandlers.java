package com.github.themrpuffin.villagerenhanced.network;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueBuilder;
import com.github.themrpuffin.villagerenhanced.dialogue.DialoguePage;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueSessionManager;
import com.github.themrpuffin.villagerenhanced.dialogue.VillagerGifts;
import com.github.themrpuffin.villagerenhanced.dialogue.VillagerMemory;
import com.github.themrpuffin.villagerenhanced.dialogue.VillagerTrading;
import com.github.themrpuffin.villagerenhanced.dialogue.VillagerTutoring;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side handling of messages sent by the client.
 *
 * <p><b>Everything here is hostile input.</b> A modified client can send any payload at any
 * time, with any values — it need not have a dialogue open, be near a villager, or even be
 * looking at one. Every field is therefore re-checked against the server's own view of the
 * world before anything happens.
 *
 * <p>NeoForge dispatches these on the main server thread, so touching the world is safe.
 */
public final class ServerPayloadHandlers {

    private ServerPayloadHandlers() {}

    /** Handles a player choosing a dialogue option. */
    public static void handleChooseOption(ChooseDialogueOptionPayload payload, IPayloadContext context) {
        // 1. A real server-side player.
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        // 2. The entity id must exist in this player's world.
        Entity entity = player.level().getEntity(payload.villagerId());

        // 3. And be a living Villager -- not a cow, a dropped item, or another player.
        if (!(entity instanceof Villager villager) || !villager.isAlive()) {
            VillagerEnhanced.LOGGER.debug(
                    "Rejected dialogue option from {}: entity id {} is not a live villager",
                    player.getName().getString(), payload.villagerId());
            return;
        }

        // 4. Close enough to have plausibly clicked it. Without this a modified client could
        //    trade with a villager across the map.
        if (!player.isWithinEntityInteractionRange(villager, ServerConfig.CONVERSATION_RANGE.get())) {
            VillagerEnhanced.LOGGER.debug(
                    "Rejected dialogue option from {}: villager {} out of reach",
                    player.getName().getString(), villager.getUUID());
            return;
        }

        // 5. Actually in a dialogue with THIS villager. Without this, anyone could send an
        //    option for any villager they stood near, with no dialogue open at all.
        if (!DialogueSessionManager.isTalkingTo(player, villager)) {
            VillagerEnhanced.LOGGER.debug(
                    "Rejected dialogue option from {}: no open dialogue with villager {}",
                    player.getName().getString(), villager.getUUID());
            return;
        }

        // 6. The option must be on offer on the page they are actually looking at, and enabled.
        //    The codec guaranteed the value is a real enum member, but not that it was on this
        //    menu -- "Back" exists only on the reputation page, "Trade" only on the greeting.
        DialoguePage page = DialogueSessionManager.currentPage(player);
        if (page == null || !DialogueBuilder.isOptionAllowed(player, villager, page, payload.option())) {
            VillagerEnhanced.LOGGER.debug(
                    "Rejected dialogue option from {}: {} is not offered on page {} by villager {}",
                    player.getName().getString(), payload.option(), page, villager.getUUID());
            return;
        }

        switch (payload.option()) {
            case TRADE -> {
                // End the session BEFORE opening the trade screen: MerchantMenu takes over the
                // villager's trading-player flag and clears it itself when closed, so the
                // session must let go without releasing the villager.
                DialogueSessionManager.endForTradeHandoff(player);
                if (!VillagerTrading.openTradeScreen(player, villager)) {
                    // Refused after all (offers emptied between opening and clicking). The
                    // session is gone, so just make sure the villager is not left busy.
                    villager.setTradingPlayer(null);
                }
            }
            case GIFT -> {
                // A diamond apple is food, so the ordinary gift path would accept it and turn
                // sixteen valuable items into a small reputation bump. It is intercepted here
                // instead: taken and acted on if the villager can use it and trusts the player,
                // and otherwise left in hand with the villager explaining why.
                if (VillagerTutoring.isHoldingApple(player)) {
                    DialogueBuilder.send(player, villager, VillagerTutoring.offer(player, villager)
                            ? DialoguePage.TUTORED
                            : DialoguePage.APPLE_REFUSED);
                } else {
                    // Re-checked inside give(), since the player may have swapped items between
                    // the page being sent and the button being clicked.
                    VillagerGifts.give(player, villager);
                    // Resend either way: on success the Gift option may now be disabled because
                    // the last of the stack was handed over, and on failure the page corrects
                    // itself.
                    DialogueBuilder.send(player, villager, DialoguePage.GREETING);
                }
            }
            case ASK_NAME -> {
                // The introduction page rather than the plain greeting, so the villager actually
                // says their name. Sending the greeting alone changed only the window title,
                // which made asking feel like it had done nothing.
                VillagerMemory.introduce(villager, player);
                DialogueBuilder.send(player, villager, DialoguePage.INTRODUCTION);
            }
            case TOPICS -> DialogueBuilder.send(player, villager, DialoguePage.TOPICS);
            case VIEW_REPUTATION -> DialogueBuilder.send(player, villager, DialoguePage.REPUTATION);
            case RUMOURS -> DialogueBuilder.send(player, villager, DialoguePage.RUMOURS);
            case ASK_ABOUT_WORK -> DialogueBuilder.send(player, villager, DialoguePage.WORK);
            case SHOW_BELONGINGS -> DialogueBuilder.send(player, villager, DialoguePage.BELONGINGS);
            // Back climbs one level rather than jumping to the greeting, so a question returns
            // to the list it was asked from.
            case BACK -> DialogueBuilder.send(player, villager, page.parent());
            // The client closes its own screen for Leave; the server just tidies up. Null
            // reason means "no need to tell the client" -- it already knows.
            case LEAVE -> DialogueSessionManager.close(player, null);
        }
    }

    /**
     * The player's dialogue window went away — Leave, Escape, or being replaced.
     *
     * <p>Stops a villager being left busy by someone who pressed Escape and stood still.
     *
     * <p>Nothing to validate: the payload carries no data, and the worst a forged one can do is
     * end the sender's own conversation.
     */
    public static void handleCloseDialogue(CloseDialoguePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // A no-op when there is no session, which is the normal case right after a trade
        // handoff: the server has already dropped it and the client's screen is closing.
        DialogueSessionManager.close(player, null);
    }
}
