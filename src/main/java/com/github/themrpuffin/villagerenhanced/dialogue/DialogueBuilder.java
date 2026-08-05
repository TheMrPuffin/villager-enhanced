package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Decides which options a villager offers a given player.
 *
 * <p>The server calls this twice per interaction: once to build the dialogue, and again when a
 * choice comes back, to confirm the chosen option was really on offer. Recomputing rather than
 * remembering avoids storing per-conversation state, and is more correct — if the villager lost
 * its job mid-conversation, recomputation gives the honest current answer where a remembered
 * list would be stale.
 *
 * <p>M7's branching dialogue will depend on conversation history and will need real session
 * state. Until then, this is both simpler and safer.
 *
 * <p>Server-side only: reads {@code getOffers()}, which is not populated on the client.
 */
public final class DialogueBuilder {

    private DialogueBuilder() {}

    /** The options this villager offers this player, in display order. */
    public static List<DialogueOptionEntry> optionsFor(ServerPlayer player, Villager villager) {
        // Nitwits, the unemployed and babies have no offers.
        boolean canTrade = !villager.getOffers().isEmpty();

        return List.of(
                new DialogueOptionEntry(DialogueOption.TRADE, canTrade),
                new DialogueOptionEntry(DialogueOption.LEAVE, true));
    }

    /**
     * Would this villager currently offer this player this option, enabled?
     *
     * <p>A modified client can name any option in the enum, including one that was greyed out
     * or never sent.
     */
    public static boolean isOptionAllowed(ServerPlayer player, Villager villager, DialogueOption option) {
        for (DialogueOptionEntry entry : optionsFor(player, villager)) {
            if (entry.option() == option) {
                return entry.enabled();
            }
        }
        return false;
    }
}
