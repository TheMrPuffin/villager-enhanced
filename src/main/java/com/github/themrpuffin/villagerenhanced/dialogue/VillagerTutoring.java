package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.item.VillagerEnhancedItems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;

/**
 * Offering a villager a diamond apple, which makes them better at what they do.
 *
 * <p>Two outcomes from one item, because they are the same story: the apple <i>unlocks what is
 * already in them</i>. A villager with a trade gets better at it. A nitwit — who has no trade and
 * in vanilla never will — is freed to take one up.
 *
 * <p><b>Offered through the ordinary gift option</b>, rather than an option of its own — you are
 * handing a villager something, which is exactly what that button already means, and it keeps the
 * greeting from growing another row.
 *
 * <p><b>Why it needs trust as well as diamonds.</b> An expensive recipe is expensive once; after
 * the first apple it is repeatable on every villager you own, and villager levelling is the main
 * brake on the whole trade economy. Requiring standing turns the apple from a way past this
 * mod's core loop into a reward for playing it — and a villager who thinks little of you refusing
 * your strange fruit needs no explaining.
 *
 * <p><b>The standing requirement is enforced on the outcome, not on the button.</b> Gifts are
 * never gated by design, being the only route back from a bad reputation. So the option stays
 * clickable and a villager who will not take the apple says why — which is better feedback than
 * a greyed button anyway, since there are three different reasons and a greyed button can only
 * carry one tooltip.
 *
 * <p><b>A refused apple is not consumed.</b> Sixteen valuable items quietly becoming a small
 * reputation bump would be a trap, and the apple is food, so the ordinary gift path would happily
 * have swallowed it.
 *
 * <p>Server-side only: reads the villager's data and mutates it.
 */
public final class VillagerTutoring {

    /** Entity event 14 makes a villager emit happy particles on every watching client. */
    private static final byte HAPPY_PARTICLES_EVENT = 14;

    private VillagerTutoring() {}

    /** Is the player holding a diamond apple? */
    public static boolean isHoldingApple(ServerPlayer player) {
        return player.getMainHandItem().is(VillagerEnhancedItems.DIAMOND_APPLE.get());
    }

    /**
     * Would this villager actually get anything from one?
     *
     * <p>Babies have no trade yet and the unemployed have not chosen one — an apple would do
     * nothing for either, and a villager already at the top of their trade has nowhere to go.
     * Nitwits are the exception that makes the item interesting: they are the one profession the
     * apple removes rather than improves.
     */
    public static boolean canBenefit(Villager villager) {
        if (villager.isBaby()) {
            return false;
        }

        VillagerData data = villager.getVillagerData();
        if (data.profession().is(VillagerProfession.NITWIT)) {
            return true;
        }
        if (data.profession().is(VillagerProfession.NONE)) {
            return false;
        }
        return VillagerData.canLevelUp(data.level());
    }

    /** Lowest standing at which a villager will take an apple seriously. */
    private static final ReputationTier REQUIRED_STANDING = ReputationTier.TRUSTED;

    /** Does this villager think enough of the player to accept one? */
    public static boolean trustsEnough(ServerPlayer player, Villager villager) {
        return ReputationTier.fromReputation(villager.getPlayerReputation(player))
                .isAtLeast(REQUIRED_STANDING);
    }

    /**
     * Why this villager will not take the apple.
     *
     * <p>Recomputed rather than passed in from the refusal: nothing it depends on changes between
     * the offer being refused and the page being built, precisely because a refusal consumes
     * nothing and alters nothing.
     *
     * <p>Only called after {@link #offer} has already returned false, so one of these is true.
     */
    public static Component refusalFor(ServerPlayer player, Villager villager) {
        if (canBenefit(villager)) {
            // The only remaining reason: they can use one, they just do not trust you with it.
            return Component.translatable("villagerenhanced.dialogue.apple.not_trusted");
        }

        // A villager with a trade who cannot benefit has simply finished it. Everyone else —
        // babies, the unemployed — has no trade to better in the first place.
        boolean hasTrade = !villager.isBaby()
                && !villager.getVillagerData().profession().is(VillagerProfession.NONE);
        return hasTrade
                ? Component.translatable("villagerenhanced.dialogue.apple.nothing_left_to_learn")
                : Component.translatable("villagerenhanced.dialogue.apple.no_trade_yet");
    }

    /**
     * Hands the apple over.
     *
     * <p>Re-checks everything rather than trusting the caller: the player may have swapped items,
     * and the villager may have changed job, between the page being sent and the button being
     * clicked.
     *
     * <p>Returning false leaves the apple in the player's hand, untouched — see the class note on
     * why a refusal must not consume it.
     *
     * @return false if nothing happened, so the caller can explain why instead
     */
    public static boolean offer(ServerPlayer player, Villager villager) {
        if (!isHoldingApple(player) || !canBenefit(villager) || !trustsEnough(player, villager)) {
            return false;
        }
        if (!(villager.level() instanceof ServerLevel level)) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        // Creative players keep their stack, matching how vanilla treats item consumption and
        // how gifts already behave here.
        if (!player.hasInfiniteMaterials()) {
            held.shrink(1);
        }

        boolean wasNitwit = villager.getVillagerData().profession().is(VillagerProfession.NITWIT);
        if (wasNitwit) {
            freeFromNitwittery(villager, level);
        } else {
            advanceTrade(villager, level);
        }

        // Feedback the player can see behind the open dialogue, as gifts already do.
        villager.makeSound(SoundEvents.VILLAGER_YES);
        level.broadcastEntityEvent(villager, HAPPY_PARTICLES_EVENT);

        VillagerEnhanced.LOGGER.debug(
                "Diamond apple accepted: player={} villager={} wasNitwit={} profession={} level={}",
                player.getName().getString(),
                villager.getUUID(),
                wasNitwit,
                villager.getVillagerData().profession().getRegisteredName(),
                villager.getVillagerData().level());
        return true;
    }

    /**
     * Did the apple just free a nitwit, rather than raise a trade level?
     *
     * <p>Only meaningful on the page shown straight after a successful offer, which is the only
     * place it is called. There, an unemployed villager can only be a nitwit from a moment ago:
     * {@link #canBenefit} refuses an already-unemployed villager, so the option that leads here
     * was never offered for one.
     */
    public static boolean wasFreedFromNitwittery(Villager villager) {
        return villager.getVillagerData().profession().is(VillagerProfession.NONE);
    }

    /**
     * Turns a nitwit into an ordinary unemployed villager.
     *
     * <p>The same call vanilla makes when a villager loses its job, so nothing here is novel.
     * {@code refreshBrain} is not optional: nitwits run a different daily schedule from everyone
     * else, and without it the villager keeps standing around on the old one.
     *
     * <p>Note the result is <b>unemployed, not employed</b>. They will claim the next free
     * workstation like any other jobless villager, so with no job site nearby it looks as though
     * nothing happened — which is why the dialogue says what to expect.
     */
    private static void freeFromNitwittery(Villager villager, ServerLevel level) {
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), VillagerProfession.NONE));
        villager.refreshBrain(level);
    }

    /**
     * Raises the villager one trade level.
     *
     * <p>{@code increaseMerchantCareer} is private in vanilla and widened by an access
     * transformer. It is used rather than reproduced because it does two things — raises the
     * level and regenerates the offers — and doing only the first gives a higher number with the
     * old trades behind it.
     *
     * <p>The XP is then set to the floor of the new level. Without this the villager carries the
     * XP it had at the old level, so its <i>next</i> level would take longer to earn than it
     * should. Setting it leaves a villager taught by apple indistinguishable from one that earned
     * its way there, which is what makes the two routes interchangeable rather than one being
     * quietly worse.
     */
    private static void advanceTrade(Villager villager, ServerLevel level) {
        villager.increaseMerchantCareer(level);
        villager.setVillagerXp(VillagerData.getMinXpPerLevel(villager.getVillagerData().level()));
    }
}
