package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.ArrayList;
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
                VillagerNames.displayNameFor(villager, player),
                villager.getVillagerData().profession().value().name(),
                villager.getVillagerData().level(),
                bodyFor(player, villager, page),
                optionsFor(player, villager, page)));
    }

    /** What the villager says on this page, one entry per line. */
    private static List<Component> bodyFor(ServerPlayer player, Villager villager, DialoguePage page) {
        return switch (page) {
            case GREETING -> List.of(greetingFor(player, villager));
            case REPUTATION -> {
                int reputation = villager.getPlayerReputation(player);
                yield List.of(Component.translatable(
                        "villagerenhanced.dialogue.reputation.body",
                        VillagerNames.displayNameFor(villager, player),
                        ReputationTier.fromReputation(reputation).displayName(),
                        reputation));
            }
            case RUMOURS -> VillagerRumours.gather(player, villager);
        };
    }

    /**
     * How the villager opens, which depends on whether they know the player.
     *
     * <p>Someone who has never introduced themselves is guarded; someone who has been away a
     * long while says so. The gap is measured from the last conversation, which is why
     * {@code noteConversation} runs after the greeting is composed, not before.
     */
    private static Component greetingFor(ServerPlayer player, Villager villager) {
        if (!VillagerMemory.isIntroduced(villager, player)) {
            return Component.translatable("villagerenhanced.dialogue.greeting.stranger");
        }
        if (VillagerMemory.isLongAbsence(villager, player)) {
            return Component.translatable(
                    "villagerenhanced.dialogue.greeting.absent", player.getDisplayName());
        }
        return Component.translatable("villagerenhanced.dialogue.greeting", player.getDisplayName());
    }

    /** The options this villager offers on this page, in display order. */
    public static List<DialogueOptionEntry> optionsFor(
            ServerPlayer player, Villager villager, DialoguePage page) {
        return switch (page) {
            case GREETING -> greetingOptions(player, villager);
            case REPUTATION, RUMOURS -> List.of(
                    new DialogueOptionEntry(DialogueOption.BACK, true),
                    new DialogueOptionEntry(DialogueOption.LEAVE, true));
        };
    }

    /**
     * The greeting page's options, and what standing each requires.
     *
     * <p>What a villager will do for you rises with how they feel about you. Two things are
     * deliberately never gated: <b>gifts</b>, because they are the way back from a bad
     * reputation and locking them would strand a player who made one mistake; and <b>"how do
     * you see me?"</b>, because someone telling you exactly how little they think of you is
     * both in character and the feedback that makes the whole system legible.
     *
     * <p>Unavailable options are greyed out rather than hidden, so they advertise what is
     * possible and what it would take, instead of being doors a player never notices.
     */
    private static List<DialogueOptionEntry> greetingOptions(ServerPlayer player, Villager villager) {
        ReputationTier standing = ReputationTier.fromReputation(villager.getPlayerReputation(player));
        List<DialogueOptionEntry> options = new ArrayList<>();

        // Asking someone's name stops being a sensible thing to say once you know it, so the
        // option disappears rather than sitting there greyed out forever. Refused by anyone who
        // actively dislikes the player -- but freely given by an ordinary stranger, so naming is
        // not hidden behind grinding reputation first.
        if (!VillagerMemory.isIntroduced(villager, player)) {
            options.add(new DialogueOptionEntry(
                    DialogueOption.ASK_NAME, standing.isAtLeast(ReputationTier.STRANGER)));
        }

        // Nitwits, the unemployed and babies have no offers.
        options.add(new DialogueOptionEntry(DialogueOption.TRADE, !villager.getOffers().isEmpty()));
        options.add(new DialogueOptionEntry(DialogueOption.GIFT,
                VillagerGifts.isAcceptable(villager, player.getMainHandItem())));
        options.add(new DialogueOptionEntry(DialogueOption.VIEW_REPUTATION, true));

        // Confiding in someone about the neighbours is something you do for people you like.
        options.add(new DialogueOptionEntry(
                DialogueOption.RUMOURS, standing.isAtLeast(ReputationTier.ACQUAINTANCE)));

        options.add(new DialogueOptionEntry(DialogueOption.LEAVE, true));

        return List.copyOf(options);
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
