package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Giving a villager something, which improves how they think of you.
 *
 * <p>This is the only thing a player can do in a conversation other than trade, and the point
 * of it is to make reputation a mechanic rather than a readout.
 *
 * <p><b>What counts as a gift.</b> Anything the villager would <i>buy</i> — that is, any item
 * appearing as a cost in their current offers — plus any food. Deriving it from their trades
 * means the answer varies by profession and trade level for free, with no per-profession table
 * to maintain, and it reads naturally: you are handing over something they wanted anyway. Food
 * is accepted universally so that nitwits, the unemployed and babies, who have no offers at
 * all, can still be befriended.
 *
 * <p><b>Why this cannot be farmed.</b> Gifts add {@code MINOR_POSITIVE} gossip, which vanilla
 * caps at 25 — so gifting tops out at +25 reputation however long you keep going, the same
 * ceiling trading has, and cannot reach the Honoured tier on its own. Curing a zombie villager
 * remains the only route to that. Raising the per-gift amount in the config only changes how
 * quickly the ceiling is reached, never the ceiling itself.
 *
 * <p>MINOR_POSITIVE decays at 1/day against trading's 2/day, so goodwill from gifts outlasts
 * goodwill from business.
 */
public final class VillagerGifts {

    /** Entity event 14 makes a villager emit happy particles on every watching client. */
    private static final byte HAPPY_PARTICLES_EVENT = 14;

    private VillagerGifts() {}

    /** Would this villager accept this item? */
    public static boolean isAcceptable(Villager villager, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Everyone appreciates food, including villagers with no trades to derive wants from.
        if (stack.has(DataComponents.FOOD)) {
            return true;
        }

        for (MerchantOffer offer : villager.getOffers()) {
            if (!offer.getCostA().isEmpty() && stack.is(offer.getCostA().getItem())) {
                return true;
            }
            if (!offer.getCostB().isEmpty() && stack.is(offer.getCostB().getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the villager one of whatever the player is holding.
     *
     * <p>Re-checks acceptability rather than trusting the caller: the player may have swapped
     * items between the dialogue being sent and the button being clicked.
     *
     * @return false if the villager would not accept what is being held
     */
    public static boolean give(ServerPlayer player, Villager villager) {
        ItemStack held = player.getMainHandItem();
        if (!isAcceptable(villager, held)) {
            return false;
        }

        // Creative players keep their stack, matching how vanilla treats item consumption.
        if (!player.hasInfiniteMaterials()) {
            held.shrink(1);
        }

        villager.gossips.add(player.getUUID(), GossipType.MINOR_POSITIVE, ServerConfig.GIFT_GOSSIP.get());

        // Feedback the player can actually see: the dialogue does not pause the game or hide
        // the world, so the villager visibly reacts behind the open screen.
        villager.makeSound(SoundEvents.VILLAGER_YES);
        villager.level().broadcastEntityEvent(villager, HAPPY_PARTICLES_EVENT);

        VillagerEnhanced.LOGGER.debug(
                "Gift accepted: player={} villager={} item={} reputation now {}",
                player.getName().getString(),
                villager.getUUID(),
                held.getItem(),
                villager.getPlayerReputation(player));
        return true;
    }
}
