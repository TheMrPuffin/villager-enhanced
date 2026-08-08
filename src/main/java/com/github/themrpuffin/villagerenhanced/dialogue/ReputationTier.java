package com.github.themrpuffin.villagerenhanced.dialogue;

import net.minecraft.network.chat.Component;

/**
 * A player's standing with a villager, as a named tier rather than a bare number.
 *
 * <p><b>Where the thresholds come from.</b> Vanilla reputation is the weighted sum of a
 * villager's gossip about you, and its realistic range is far narrower than its theoretical one
 * (roughly -700 to +150). Every source, weighted:
 *
 * <table border="1">
 * <caption>Reputation sources</caption>
 * <tr><th>Action</th><th>Gossip</th><th>Reputation</th></tr>
 * <tr><td>Kill a villager</td><td>MAJOR_NEGATIVE 25 x -5</td><td>-125</td></tr>
 * <tr><td>Hurt a villager</td><td>MINOR_NEGATIVE 25 x -1</td><td>-25</td></tr>
 * <tr><td>Trade once</td><td>TRADING +2 x 1</td><td>+2, capped at +25</td></tr>
 * <tr><td>Cure a zombie villager</td><td>MAJOR_POSITIVE 20x5 + MINOR_POSITIVE 25x1</td><td>+125</td></tr>
 * </table>
 *
 * <p>Trading alone therefore tops out at +25 however long you do it, while one cured zombie
 * villager is worth +125 and never decays. Thresholds spread evenly across the theoretical
 * range would leave every ordinary player stuck in the bottom tier forever, so these are tuned
 * to what is actually reachable — each tier corresponds to something a player has really done.
 *
 * <p>Gossip decays over time (except MAJOR_POSITIVE), so a tier is a living value, not a score
 * that only goes up.
 */
public enum ReputationTier {
    /** Killed a villager (-125). The floor, so it catches anything worse too. */
    REVILED(Integer.MIN_VALUE),
    /** Hurt a villager (-25). */
    DISLIKED(-99),
    /** The default: no history either way. */
    STRANGER(-14),
    /** Traded a handful of times. */
    ACQUAINTANCE(5),
    /** A regular customer. Trading alone maxes out inside this tier, at +25. */
    TRUSTED(20),
    /** Cured a zombie villager — the one thing vanilla treats as genuinely exceptional. */
    HONOURED(75);

    /**
     * Lowest reputation still in this tier. Values must ascend in declaration order, since
     * {@link #fromReputation} relies on that to find the highest match.
     */
    private final int minimum;

    ReputationTier(int minimum) {
        this.minimum = minimum;
    }

    /**
     * The tier a given raw reputation falls into.
     *
     * <p>Walks downwards so the highest matching tier wins.
     */
    public static ReputationTier fromReputation(int reputation) {
        ReputationTier[] tiers = values();
        for (int i = tiers.length - 1; i >= 0; i--) {
            if (reputation >= tiers[i].minimum) {
                return tiers[i];
            }
        }
        return REVILED;
    }

    /**
     * Is this standing at least as good as the given one?
     *
     * <p>Relies on the tiers being declared worst to best, which {@link #fromReputation} already
     * requires.
     *
     * <p>Used to decide what a villager is willing to do for a player. From 0.5.0 this becomes a
     * declared minimum on each {@code DialogueOption} rather than scattered checks.
     */
    public boolean isAtLeast(ReputationTier other) {
        return this.ordinal() >= other.ordinal();
    }

    /** Display name, e.g. "Trusted". */
    public Component displayName() {
        return Component.translatable("villagerenhanced.reputation.tier." + name().toLowerCase());
    }
}
