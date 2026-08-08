package com.github.themrpuffin.villagerenhanced.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What one villager remembers about one player.
 *
 * <p>Both fields default when absent, so adding a field later will not invalidate villagers
 * saved by an earlier version — old entries simply come back with the new field at its default.
 *
 * @param introduced          whether this villager has told this player their name
 * @param lastSpokenGameTime  world game time of their last conversation, for "it has been a
 *                            while" greetings. 0 means never.
 */
public record PlayerRelationship(boolean introduced, long lastSpokenGameTime) {

    /** The state of every villager-player pair that has no history. */
    public static final PlayerRelationship NEVER_MET = new PlayerRelationship(false, 0L);

    public static final Codec<PlayerRelationship> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerRelationship::introduced),
            Codec.LONG.optionalFieldOf("last_spoken", 0L).forGetter(PlayerRelationship::lastSpokenGameTime)
    ).apply(i, PlayerRelationship::new));

    public PlayerRelationship withIntroduced() {
        return new PlayerRelationship(true, this.lastSpokenGameTime);
    }

    public PlayerRelationship withLastSpoken(long gameTime) {
        return new PlayerRelationship(this.introduced, gameTime);
    }
}
