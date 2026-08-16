package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Whether a villager would start a family, and what is stopping them.
 *
 * <p><b>The point of this is the second half.</b> Vanilla exposes {@code Villager#canBreed()},
 * which is about food and age — but a willing pair still will not breed without a spare bed, and
 * that is checked somewhere else entirely, in {@code VillagerMakeLove#tryToGiveBirth}. A player
 * whose village has stopped growing is usually looking straight at the reason and cannot see it,
 * because nothing in the game ever says "there is nowhere to put a child".
 *
 * <p>Both halves are reported, so the answer is a diagnosis rather than a yes or no.
 *
 * <p>Server-side only: reads the villager's inventory and the point-of-interest manager, neither
 * of which exists on the client.
 */
public final class VillagerFamily {

    /**
     * How far {@code VillagerMakeLove#takeVacantBed} looks for the child's bed. Matched exactly,
     * because an answer measured differently from the rule it describes would be a lie.
     */
    private static final int BED_SEARCH_RADIUS = 48;

    private VillagerFamily() {}

    /** What this villager says about starting a family. */
    public static List<Component> describe(Villager villager) {
        if (villager.isBaby()) {
            return List.of(Component.translatable("villagerenhanced.dialogue.family.child"));
        }

        // VillagerMakeLove sets both parents to age 6000 when a child is born, so a positive age
        // on an adult means they have bred recently and are still on cooldown.
        if (villager.getAge() > 0) {
            return List.of(Component.translatable("villagerenhanced.dialogue.family.recent"));
        }

        // Checked before canBreed() so a sleeping villager is not reported as underfed --
        // canBreed() folds sleep in with everything else and would give the wrong reason.
        if (villager.isSleeping()) {
            return List.of(Component.translatable("villagerenhanced.dialogue.family.asleep"));
        }

        boolean wellFed = villager.canBreed();
        boolean hasBed = hasVacantBed(villager);

        if (wellFed && hasBed) {
            return List.of(Component.translatable("villagerenhanced.dialogue.family.ready"));
        }
        if (wellFed) {
            // The interesting case, and the one vanilla never explains. "But" earns its place
            // here: it turns on the willing line before it.
            return List.of(
                    Component.translatable("villagerenhanced.dialogue.family.willing"),
                    Component.translatable("villagerenhanced.dialogue.family.no_bed"));
        }
        if (hasBed) {
            return List.of(Component.translatable("villagerenhanced.dialogue.family.hungry"));
        }
        // Both problems at once, which is the usual state of a village that has stopped growing.
        //
        // A SEPARATE bed line, not the one above. That one opens with "But", which contrasts with
        // the willing line it follows -- after "Not yet", contrast is wrong and the two read as
        // unrelated statements shoved together. This one continues a negative instead. Lines are
        // revealed one at a time, so each has to land as a reply to the one before it.
        return List.of(
                Component.translatable("villagerenhanced.dialogue.family.hungry"),
                Component.translatable("villagerenhanced.dialogue.family.no_bed_either"));
    }

    /**
     * Is there a bed a newborn could claim?
     *
     * <p>Mirrors {@code VillagerMakeLove#takeVacantBed} — same point-of-interest type, same
     * radius, same reachability test — <b>minus the {@code acquireTicket()}</b>. Vanilla's version
     * is {@code PoiManager#take}, which claims the bed it finds; answering a question must not
     * quietly reserve one, so this uses the read-only {@code getInRange} that {@code take} is
     * itself built on.
     *
     * <p>{@code Occupancy.HAS_SPACE} is what makes this mean "spare": a bed a villager has already
     * made their home has no free tickets and is skipped, so their own bed never counts.
     *
     * <p>Pathfinding to candidates is not cheap, but {@code anyMatch} stops at the first reachable
     * bed and this runs once when a question is asked, where vanilla does the same work on every
     * attempted birth.
     */
    private static boolean hasVacantBed(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return false;
        }

        return level.getPoiManager()
                .getInRange(
                        poiType -> poiType.is(PoiTypes.HOME),
                        villager.blockPosition(),
                        BED_SEARCH_RADIUS,
                        PoiManager.Occupancy.HAS_SPACE)
                .anyMatch(record -> canReach(villager, record.getPos(), record.getPoiType()));
    }

    /** A bed behind a wall is not a bed, which is why vanilla paths to it before counting it. */
    private static boolean canReach(Villager villager, BlockPos pos, Holder<PoiType> poiType) {
        Path path = villager.getNavigation().createPath(pos, poiType.value().validRange());
        return path != null && path.canReach();
    }
}
