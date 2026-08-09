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

    /** Volume of the villager's mumble as each line appears. 0 mutes it. */
    public static final ModConfigSpec.DoubleValue VOICE_VOLUME;

    /** Ticks between lines appearing. 0 shows the whole answer at once. */
    public static final ModConfigSpec.IntValue REVEAL_TICKS_PER_LINE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("How the dialogue window looks and sounds. These affect only you.")
                .push("presentation");

        VOICE_VOLUME = builder
                .comment(
                        "Volume of the villager's mumble as each line of dialogue appears.",
                        "Set to 0 to silence it.")
                .translation("villagerenhanced.config.voice_volume")
                .defineInRange("voiceVolume", 0.45D, 0.0D, 1.0D);

        REVEAL_TICKS_PER_LINE = builder
                .comment(
                        "Ticks to wait between each line of dialogue appearing; 20 ticks is one second.",
                        "Set to 0 to show the whole answer immediately. The villager still speaks,",
                        "once rather than once per line; use voiceVolume to silence them.")
                .translation("villagerenhanced.config.reveal_ticks_per_line")
                .defineInRange("revealTicksPerLine", 5, 0, 20);

        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
