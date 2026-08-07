package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/**
 * What the other villagers around here think of you.
 *
 * <p>Minecraft has always run a gossip network: each villager forms its own opinion of you, and
 * villagers pass those opinions to one another when they meet. Players have never been able to
 * see any of it. This asks the villager in front of you to relay what the neighbours are saying.
 *
 * <p>The emergent part is worth understanding: a villager you have <b>never interacted with</b>
 * can still have an opinion, because they heard it from someone who has. Trade with one farmer
 * and, in time, the whole village knows you are good for it. Hurt one and word gets round the
 * same way.
 *
 * <p>Needs no stored state of its own — every opinion here is vanilla gossip, read live.
 */
public final class VillagerRumours {

    /** How far the speaker's acquaintances extend. Matches the naming radius: roughly a village. */
    private static final double EARSHOT = 48.0;

    /** Kept short so the page stays readable; the strongest opinions are the interesting ones. */
    private static final int MAX_RUMOURS = 4;

    private VillagerRumours() {}

    /**
     * One line per neighbour with an opinion worth repeating, strongest feelings first.
     *
     * <p>Villagers who feel nothing in particular are left out — a list of people with no view
     * on you is not a rumour, and it would bury the ones that matter. The speaker is excluded
     * too, since you can ask them directly.
     *
     * @return the lines to display, or a single line saying nobody has much to say
     */
    public static List<Component> gather(ServerPlayer player, Villager speaker) {
        AABB area = speaker.getBoundingBox().inflate(EARSHOT);
        List<Villager> neighbours = speaker.level().getEntitiesOfClass(Villager.class, area);

        record Opinion(Villager villager, int reputation, ReputationTier tier) {}

        List<Opinion> opinions = new ArrayList<>();
        for (Villager neighbour : neighbours) {
            if (neighbour == speaker || !neighbour.isAlive()) {
                continue;
            }

            int reputation = neighbour.getPlayerReputation(player);
            ReputationTier tier = ReputationTier.fromReputation(reputation);

            // STRANGER means "no strong feelings", which is not worth repeating.
            if (tier != ReputationTier.STRANGER) {
                opinions.add(new Opinion(neighbour, reputation, tier));
            }
        }

        if (opinions.isEmpty()) {
            return List.of(Component.translatable("villagerenhanced.dialogue.rumours.none"));
        }

        // Strongest opinions first, in either direction -- being hated is as interesting as
        // being liked.
        opinions.sort(Comparator.comparingInt((Opinion o) -> Math.abs(o.reputation())).reversed());

        List<Component> lines = new ArrayList<>();
        for (Opinion opinion : opinions.subList(0, Math.min(MAX_RUMOURS, opinions.size()))) {
            lines.add(lineFor(opinion.villager(), opinion.tier()));
        }
        return List.copyOf(lines);
    }

    /**
     * Renders one neighbour's view, naming them so the player knows who to thank or apologise to.
     */
    private static Component lineFor(Villager villager, ReputationTier tier) {
        Component subject = Component.translatable(
                "villagerenhanced.dialogue.rumours.subject",
                VillagerNames.nameFor(villager),
                villager.getVillagerData().profession().value().name());

        return Component.translatable(translationKeyFor(tier), subject);
    }

    private static String translationKeyFor(ReputationTier tier) {
        return switch (tier) {
            case HONOURED -> "villagerenhanced.dialogue.rumours.honoured";
            case TRUSTED -> "villagerenhanced.dialogue.rumours.trusted";
            case ACQUAINTANCE -> "villagerenhanced.dialogue.rumours.acquaintance";
            case DISLIKED -> "villagerenhanced.dialogue.rumours.disliked";
            case REVILED -> "villagerenhanced.dialogue.rumours.reviled";
            // Filtered out before we get here, but the switch must be exhaustive.
            case STRANGER -> "villagerenhanced.dialogue.rumours.none";
        };
    }
}
