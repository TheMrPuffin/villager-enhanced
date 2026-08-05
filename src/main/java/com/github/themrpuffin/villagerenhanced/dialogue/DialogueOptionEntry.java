package com.github.themrpuffin.villagerenhanced.dialogue;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One option as the server offers it: which choice, and whether it can be used.
 *
 * <p>Disabled options are sent rather than omitted so the player sees a greyed-out button and
 * understands why. Silently dropping the button would look like a bug.
 *
 * @param option  which choice this is
 * @param enabled false to show it greyed out and refuse it server-side
 */
public record DialogueOptionEntry(DialogueOption option, boolean enabled) {

    public static final StreamCodec<ByteBuf, DialogueOptionEntry> STREAM_CODEC =
            StreamCodec.composite(
                    DialogueOption.STREAM_CODEC, DialogueOptionEntry::option,
                    ByteBufCodecs.BOOL, DialogueOptionEntry::enabled,
                    DialogueOptionEntry::new);
}
