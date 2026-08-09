package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.ArrayList;
import java.util.List;

import com.github.themrpuffin.villagerenhanced.config.ServerConfig;
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
                Component name = VillagerNames.displayNameFor(villager, player);
                Component tier = ReputationTier.fromReputation(reputation).displayName();

                // Whether the raw score is shown is a server setting rather than a personal one,
                // because the dialogue text is written on the server: how much a villager gives
                // away is content, not presentation.
                yield List.of(ServerConfig.SHOW_RAW_REPUTATION.get()
                        ? Component.translatable(
                                "villagerenhanced.dialogue.reputation.body", name, tier, reputation)
                        : Component.translatable(
                                "villagerenhanced.dialogue.reputation.body_plain", name, tier));
            }
            case TOPICS -> List.of(Component.translatable("villagerenhanced.dialogue.topics.body"));
            case RUMOURS -> VillagerRumours.gather(player, villager);
            case WORK -> VillagerWork.describe(villager);
            case BELONGINGS -> VillagerBelongings.describe(villager);
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
            case TOPICS -> topicOptions(player, villager);
            case REPUTATION, RUMOURS, WORK, BELONGINGS -> List.of(
                    new DialogueOptionEntry(DialogueOption.BACK, true),
                    new DialogueOptionEntry(DialogueOption.LEAVE, true));
        };
    }

    /**
     * The questions branch.
     *
     * <p>These four used to sit on the greeting, which pushed it to eight buttons. They are the
     * things you <i>ask</i> rather than <i>do</i>, so they group naturally — and it keeps the
     * greeting to the actions worth reaching in one click.
     */
    private static List<DialogueOptionEntry> topicOptions(ServerPlayer player, Villager villager) {
        ReputationTier standing = ReputationTier.fromReputation(villager.getPlayerReputation(player));

        return List.of(
                entry(DialogueOption.ASK_ABOUT_WORK, standing, true),
                entry(DialogueOption.VIEW_REPUTATION, standing, true),
                entry(DialogueOption.RUMOURS, standing, true),
                entry(DialogueOption.SHOW_BELONGINGS, standing, true),
                entry(DialogueOption.BACK, standing, true));
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

        // Asking someone's name stops being a sensible thing to say once you know it, so this
        // option disappears rather than sitting greyed out forever asking an answered question.
        if (!VillagerMemory.isIntroduced(villager, player)) {
            options.add(entry(DialogueOption.ASK_NAME, standing, true));
        }

        // Nitwits, the unemployed and babies have no offers.
        options.add(entry(DialogueOption.TRADE, standing, !villager.getOffers().isEmpty()));
        options.add(entry(DialogueOption.GIFT, standing,
                VillagerGifts.isAcceptable(villager, player.getMainHandItem())));
        // Everything you ask rather than do lives behind this, which keeps the greeting to the
        // handful of actions worth reaching in a single click.
        options.add(entry(DialogueOption.TOPICS, standing, true));
        options.add(entry(DialogueOption.LEAVE, standing, true));

        return List.copyOf(options);
    }

    /**
     * Combines the two reasons an option can be unavailable: the player's standing, declared on
     * {@link DialogueOption}, and whatever the situation demands — offers to trade, something in
     * hand to give.
     */
    private static DialogueOptionEntry entry(
            DialogueOption option, ReputationTier standing, boolean situationAllows) {
        return new DialogueOptionEntry(option, situationAllows && option.isUnlockedAt(standing));
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
