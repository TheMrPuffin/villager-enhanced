package com.github.themrpuffin.villagerenhanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings that change only how the dialogue looks and sounds to one player.
 *
 * <p><b>Nothing here may affect gameplay.</b> Client config lives in each player's own files and
 * is never checked by the server, so a value that changed what a player could do would simply be
 * a cheat switch. Anything with consequences belongs in {@link ServerConfig}.
 */
public final class ClientConfig {

    public static final ModConfigSpec SPEC;

    /**
     * Volume of the villager's mumble as each line appears, as a percentage. 0 mutes it.
     *
     * <p><b>Deliberately an int, not a double.</b> NeoForge's config screen renders a ranged
     * integer as a slider, but any double as a text box — and that box will not accept a decimal
     * point, so a 0.0–1.0 double is only settable to 0 or 1 through the UI. A percentage gets a
     * usable slider and reads more naturally besides.
     */
    public static final ModConfigSpec.IntValue VOICE_VOLUME_PERCENT;

    /** Ticks between lines appearing. 0 shows the whole answer at once. */
    public static final ModConfigSpec.IntValue REVEAL_TICKS_PER_LINE;

    /** Whether to show messages about villagers being born or killed nearby. */
    public static final ModConfigSpec.BooleanValue SHOW_NOTIFICATIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("How the dialogue window looks and sounds. These affect only you.")
                .push("presentation");

        VOICE_VOLUME_PERCENT = builder
                .comment(
                        "Volume of the villager's mumble as each line of dialogue appears, in percent.",
                        "Set to 0 to silence it.")
                .translation("villagerenhanced.config.voice_volume")
                .defineInRange("voiceVolumePercent", 45, 0, 100);

        REVEAL_TICKS_PER_LINE = builder
                .comment(
                        "Ticks to wait between each line of dialogue appearing; 20 ticks is one second.",
                        "Set to 0 to show the whole answer immediately. The villager still speaks,",
                        "once rather than once per line; use voiceVolume to silence them.")
                .translation("villagerenhanced.config.reveal_ticks_per_line")
                .defineInRange("revealTicksPerLine", 15, 0, 20);

        SHOW_NOTIFICATIONS = builder
                .comment(
                        "Whether to show messages when a villager nearby is born or killed.",
                        "The world decides whether these are sent at all, and how far they carry;",
                        "this is your own switch for whether to see them.")
                .translation("villagerenhanced.config.show_notifications")
                .define("showNotifications", true);

        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
