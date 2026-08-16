package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
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
 *
 * <p><b>Gifts a villager can use are kept, not consumed.</b> Bread handed to someone who has just
 * said they have no food to raise a child on now actually feeds them. See {@link #tryKeep} for
 * why only some items are kept — the inventory is eight slots and filling it does harm.
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
     * <p>Anything the villager has a real use for is <b>put in their inventory</b> rather than
     * vanishing — see {@link #tryKeep}. Everything else is still accepted as a gesture and
     * consumed. Reputation rises either way; where the gift goes changes what it <i>does</i>, not
     * whether it counts.
     *
     * @return false if the villager would not accept what is being held
     */
    public static boolean give(ServerPlayer player, Villager villager) {
        ItemStack held = player.getMainHandItem();
        if (!isAcceptable(villager, held)) {
            return false;
        }

        // Separated before the hand is emptied below, since that is what decides the outcome.
        boolean kept = tryKeep(villager, held.copyWithCount(1));

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
                "Gift accepted: player={} villager={} item={} kept={} reputation now {}",
                player.getName().getString(),
                villager.getUUID(),
                held.getItem(),
                kept,
                villager.getPlayerReputation(player));
        return true;
    }

    /**
     * Puts the gift in the villager's inventory, if it is something they will actually use.
     *
     * <p>This is what turns a gift from a gesture into an effect. Villagers really do live off
     * that inventory — they eat from it, plant from it, hand food to villagers who want to breed,
     * and throw the surplus to their neighbours once carrying more than they need. Three bread is
     * enough food to start a family, so handing bread to a villager who has just said they have
     * none is now something that works.
     *
     * <p><b>Only what they would use, because the inventory is eight slots.</b> Filling it with
     * emeralds would be actively harmful: {@code Villager#wantsToPickUp} requires room, so a
     * villager with a full inventory stops collecting — a farmer would stand in a ripe field and
     * gather nothing. Anything not kept is consumed exactly as gifts always were.
     *
     * @param offered a single item, already separated from the player's stack
     * @return whether it went into the inventory
     */
    private static boolean tryKeep(Villager villager, ItemStack offered) {
        if (!wouldUse(villager, offered)) {
            return false;
        }

        SimpleContainer inventory = villager.getInventory();
        if (!inventory.canAddItem(offered)) {
            return false;
        }

        // addItem returns whatever would not fit; empty means all of it went in. Checked rather
        // than assumed, so a gift is never quietly destroyed by a full inventory.
        return inventory.addItem(offered).isEmpty();
    }

    /**
     * Would this villager get anything out of holding this?
     *
     * <p>Mostly vanilla's own rule: {@code Villager#wantsToPickUp} is the game's answer to "would
     * a villager take this off the ground?", being the {@code villager_picks_up} tag plus
     * whatever the profession asks for.
     *
     * <p><b>One deliberate addition.</b> {@code Villager.FOOD_POINTS} counts bread, potatoes,
     * carrots and beetroot, but the pick-up tag omits potatoes and carrots — so vanilla will
     * happily <i>eat</i> a carrot out of the inventory while never picking one off the floor.
     * Something handed over in conversation is not something found in the dirt, and a villager
     * refusing to hold a carrot they would then eat makes no sense to a player.
     */
    private static boolean wouldUse(Villager villager, ItemStack stack) {
        return stack.is(ItemTags.VILLAGER_PICKS_UP)
                || villager.getVillagerData().profession().value().requestedItems().contains(stack.getItem())
                || Villager.FOOD_POINTS.containsKey(stack.getItem());
    }
}
