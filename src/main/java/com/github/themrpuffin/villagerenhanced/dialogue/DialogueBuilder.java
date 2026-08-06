package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.List;

import com.github.themrpuffin.villagerenhanced.network.OpenDialoguePayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Builds what a villager says and offers, and sends it to the player.
 *
 * <p>The server calls {@link #optionsFor} twice per interaction: once when sending a page, and
 * again when a choice comes back, to confirm the chosen option was really on offer. Recomputing
 * rather than remembering the list is more correct — if the villager lost its job mid-
 * conversation, recomputation gives the honest current answer where a stored list would be
 * stale.
 *
 * <p>What cannot be recomputed is <i>which page</i> the player is on, since that depends on
 * where they navigated. That lives in the {@link DialogueSession}.
 *
 * <p>Server-side only: reads {@code getOffers()} and the gossip container, neither of which is
 * populated on the client.
 */
public final class DialogueBuilder {

    private DialogueBuilder() {}

    /**
     * Sends a page of dialogue to the player and records that they are now on it.
     *
     * <p>The session must already exist — {@link DialogueSessionManager#open} creates it when
     * the conversation starts.
     */
    public static void send(ServerPlayer player, Villager villager, DialoguePage page) {
        DialogueSessionManager.setPage(player, page);

        PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(
                villager.getId(),
                villager.getDisplayName(),
                villager.getVillagerData().profession().value().name(),
                villager.getVillagerData().level(),
                bodyFor(player, villager, page),
                optionsFor(player, villager, page)));
    }

    /** What the villager says on this page. */
    private static Component bodyFor(ServerPlayer player, Villager villager, DialoguePage page) {
        return switch (page) {
            case GREETING -> Component.translatable(
                    "villagerenhanced.dialogue.greeting", player.getDisplayName());
            case REPUTATION -> {
                int reputation = villager.getPlayerReputation(player);
                yield Component.translatable(
                        "villagerenhanced.dialogue.reputation.body",
                        villager.getDisplayName(),
                        ReputationTier.fromReputation(reputation).displayName(),
                        reputation);
            }
        };
    }

    /** The options this villager offers on this page, in display order. */
    public static List<DialogueOptionEntry> optionsFor(
            ServerPlayer player, Villager villager, DialoguePage page) {
        return switch (page) {
            case GREETING -> List.of(
                    // Nitwits, the unemployed and babies have no offers.
                    new DialogueOptionEntry(DialogueOption.TRADE, !villager.getOffers().isEmpty()),
                    new DialogueOptionEntry(DialogueOption.GIFT,
                            VillagerGifts.isAcceptable(villager, player.getMainHandItem())),
                    new DialogueOptionEntry(DialogueOption.VIEW_REPUTATION, true),
                    new DialogueOptionEntry(DialogueOption.LEAVE, true));
            case REPUTATION -> List.of(
                    new DialogueOptionEntry(DialogueOption.BACK, true),
                    new DialogueOptionEntry(DialogueOption.LEAVE, true));
        };
    }

    /**
     * Would this villager currently offer this player this option, on this page, enabled?
     *
     * <p>A modified client can name any option in the enum — including one greyed out, one
     * belonging to a different page, or one never sent at all.
     */
    public static boolean isOptionAllowed(
            ServerPlayer player, Villager villager, DialoguePage page, DialogueOption option) {
        for (DialogueOptionEntry entry : optionsFor(player, villager, page)) {
            if (entry.option() == option) {
                return entry.enabled();
            }
        }
        return false;
    }
}
