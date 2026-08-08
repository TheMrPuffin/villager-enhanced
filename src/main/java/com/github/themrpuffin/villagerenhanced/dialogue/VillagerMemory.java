package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.themrpuffin.villagerenhanced.attachment.VillagerEnhancedAttachments;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * What villagers remember about the people they have met.
 *
 * <p>Reads are safe anywhere; writes are server-side, since the attachment is not synced.
 */
public final class VillagerMemory {

    /**
     * How long before a villager treats you as someone they have not seen for a while.
     * Three in-game days, at 24000 ticks per day.
     */
    private static final long LONG_ABSENCE_TICKS = 24000L * 3L;

    private VillagerMemory() {}

    /** What this villager remembers about this player, or {@link PlayerRelationship#NEVER_MET}. */
    public static PlayerRelationship get(Villager villager, Player player) {
        // hasData is checked first because getData attaches the default on its first call.
        if (!villager.hasData(VillagerEnhancedAttachments.VILLAGER_RELATIONSHIPS)) {
            return PlayerRelationship.NEVER_MET;
        }
        return villager.getData(VillagerEnhancedAttachments.VILLAGER_RELATIONSHIPS)
                .getOrDefault(player.getUUID(), PlayerRelationship.NEVER_MET);
    }

    /** Has this villager told this player their name? */
    public static boolean isIntroduced(Villager villager, Player player) {
        return get(villager, player).introduced();
    }

    /** Records that this villager has given this player their name. */
    public static void introduce(Villager villager, Player player) {
        put(villager, player, get(villager, player).withIntroduced());
    }

    /**
     * Records that a conversation just happened.
     *
     * <p>Call <b>after</b> composing the greeting, since the greeting depends on how long it had
     * been since the previous one.
     */
    public static void noteConversation(Villager villager, Player player) {
        put(villager, player, get(villager, player).withLastSpoken(villager.level().getGameTime()));
    }

    /**
     * Has it been long enough that the villager would remark on the gap?
     *
     * <p>False for a player they have never spoken to — that is a first meeting, not a return.
     */
    public static boolean isLongAbsence(Villager villager, Player player) {
        PlayerRelationship relationship = get(villager, player);
        if (relationship.lastSpokenGameTime() == 0L) {
            return false;
        }
        return villager.level().getGameTime() - relationship.lastSpokenGameTime() > LONG_ABSENCE_TICKS;
    }

    /**
     * Stores one player's entry, leaving the rest untouched.
     *
     * <p>The map is replaced rather than mutated in place: attachments hand back the stored
     * instance, and quietly editing it would skip the write that marks the entity as changed.
     */
    private static void put(Villager villager, Player player, PlayerRelationship relationship) {
        Map<UUID, PlayerRelationship> current =
                villager.hasData(VillagerEnhancedAttachments.VILLAGER_RELATIONSHIPS)
                        ? villager.getData(VillagerEnhancedAttachments.VILLAGER_RELATIONSHIPS)
                        : Map.of();

        Map<UUID, PlayerRelationship> updated = new HashMap<>(current);
        updated.put(player.getUUID(), relationship);

        villager.setData(VillagerEnhancedAttachments.VILLAGER_RELATIONSHIPS, Map.copyOf(updated));
    }
}
