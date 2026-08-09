package com.github.themrpuffin.villagerenhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings that decide what players can actually do.
 *
 * <p>Server config lives with the world and is <b>synced to every client on join</b>, so both
 * sides agree without the client being able to change anything. Everything with a consequence
 * belongs here rather than in {@link ClientConfig} — a client-side conversation range would let
 * anyone widen it at will.
 *
 * <p>The sync is why the client can read these too: {@code ClientVillagerInteractionHandler}
 * needs to know whether dialogue is enabled in order to decide whether to suppress its local
 * prediction.
 */
public final class ServerConfig {

    public static final ModConfigSpec SPEC;

    /** Master switch. False restores vanilla right-click trading entirely. */
    public static final ModConfigSpec.BooleanValue DIALOGUE_ENABLED;

    /** Whether sneak + right-click skips the conversation and trades directly. */
    public static final ModConfigSpec.BooleanValue SNEAK_TO_TRADE;

    /** Whether a villager is occupied for the whole conversation, not just while trading. */
    public static final ModConfigSpec.BooleanValue HOLD_VILLAGER_DURING_DIALOGUE;

    /** How far a player may be from a villager before the conversation ends. */
    public static final ModConfigSpec.DoubleValue CONVERSATION_RANGE;

    /** Gossip added per gift. Vanilla caps the underlying value at 25 regardless. */
    public static final ModConfigSpec.IntValue GIFT_GOSSIP;

    /** How many neighbours' opinions a villager will relay at once. */
    public static final ModConfigSpec.IntValue RUMOUR_COUNT;

    /** How far a villager's acquaintances extend, in blocks. */
    public static final ModConfigSpec.IntValue RUMOUR_RADIUS;

    /** Whether the reputation page shows the raw gossip score beside the tier. */
    public static final ModConfigSpec.BooleanValue SHOW_RAW_REPUTATION;

    /** Whether nearby players are told when a villager is killed. */
    public static final ModConfigSpec.BooleanValue NOTIFY_VILLAGER_DEATHS;

    /** Whether nearby players are told when a villager is born. */
    public static final ModConfigSpec.BooleanValue NOTIFY_VILLAGER_BIRTHS;

    /** How close a player must be to be told, in blocks. */
    public static final ModConfigSpec.IntValue NOTIFICATION_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("What villagers will do, and how the dialogue behaves for everyone on this world.")
                .push("dialogue");

        DIALOGUE_ENABLED = builder
                .comment(
                        "Whether right-clicking a villager opens the dialogue.",
                        "Set to false to restore vanilla trading entirely, leaving the mod installed",
                        "but inactive. Useful alongside other mods that hook villager interaction.")
                .translation("villagerenhanced.config.dialogue_enabled")
                .define("dialogueEnabled", true);

        SNEAK_TO_TRADE = builder
                .comment("Whether sneak + right-click skips the conversation and opens trading directly.")
                .translation("villagerenhanced.config.sneak_to_trade")
                .define("sneakToTrade", true);

        HOLD_VILLAGER_DURING_DIALOGUE = builder
                .comment(
                        "Whether a villager is occupied for the whole conversation, refusing other players.",
                        "Vanilla only occupies a villager while it is actually being traded with. On a busy",
                        "server you may prefer false, so several players can talk to the same villager.")
                .translation("villagerenhanced.config.hold_villager")
                .define("holdVillagerDuringDialogue", true);

        CONVERSATION_RANGE = builder
                .comment(
                        "How far a player may be from a villager before the conversation ends, in blocks.",
                        "Also the reach the server validates every dialogue action against, so raising it",
                        "raises how far away players may act on a villager.")
                .translation("villagerenhanced.config.conversation_range")
                .defineInRange("conversationRange", 4.0D, 2.0D, 16.0D);

        builder.pop();

        builder.comment("Reputation, gifts and gossip.").push("reputation");

        GIFT_GOSSIP = builder
                .comment(
                        "How much a single gift improves a villager's opinion of you.",
                        "Vanilla caps the underlying gossip at 25 however high this is set, so gifting",
                        "alone can never reach the Honoured tier.")
                .translation("villagerenhanced.config.gift_gossip")
                .defineInRange("giftGossip", 2, 1, 25);

        RUMOUR_COUNT = builder
                .comment("How many neighbours' opinions a villager will pass on at once.")
                .translation("villagerenhanced.config.rumour_count")
                .defineInRange("rumourCount", 4, 1, 8);

        RUMOUR_RADIUS = builder
                .comment("How far a villager's acquaintances extend when gathering rumours, in blocks.")
                .translation("villagerenhanced.config.rumour_radius")
                .defineInRange("rumourRadius", 48, 8, 128);

        SHOW_RAW_REPUTATION = builder
                .comment(
                        "Whether the reputation page shows the raw gossip score beside the named tier.",
                        "This is a server setting rather than a personal one because the dialogue text is",
                        "written on the server: how much detail a villager gives away is content.")
                .translation("villagerenhanced.config.show_raw_reputation")
                .define("showRawReputation", true);

        builder.pop();

        builder.comment("Telling players when something happens to a villager nearby.")
                .push("notifications");

        NOTIFY_VILLAGER_DEATHS = builder
                .comment("Whether nearby players are told when a villager is killed.")
                .translation("villagerenhanced.config.notify_deaths")
                .define("notifyVillagerDeaths", true);

        NOTIFY_VILLAGER_BIRTHS = builder
                .comment("Whether nearby players are told when a villager is born.")
                .translation("villagerenhanced.config.notify_births")
                .define("notifyVillagerBirths", true);

        NOTIFICATION_RADIUS = builder
                .comment(
                        "How close a player must be to hear about it, in blocks.",
                        "Generous by default, since the point is hearing about a raid you are not",
                        "standing in. A busy server with many villages will want this smaller.")
                .translation("villagerenhanced.config.notification_radius")
                .defineInRange("notificationRadius", 128, 16, 512);

        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {}
}
