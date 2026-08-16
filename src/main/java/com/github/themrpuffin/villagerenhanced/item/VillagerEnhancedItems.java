package com.github.themrpuffin.villagerenhanced.item;

import java.util.List;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The items this mod adds.
 *
 * <p>A {@code DeferredRegister} is the safe way to register anything: registries are frozen for
 * most of the game's life, so you hand NeoForge a description of what you want and it creates it
 * during the one window when that is allowed. It is the only thing in this mod that cannot
 * register itself through {@code @EventBusSubscriber} — see the constructor in
 * {@link VillagerEnhanced}.
 *
 * <p>The {@code @EventBusSubscriber} here is for the creative tab below. It needs no {@code bus}
 * argument despite what most guides say: FML routes by event type, sending anything implementing
 * {@code IModBusEvent} to the mod bus and everything else to the game bus.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class VillagerEnhancedItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VillagerEnhanced.MODID);

    /**
     * Hunger and saturation, at exactly twice a golden apple's {@code Foods.GOLDEN_APPLE}
     * (nutrition 4, saturation 1.2). {@code alwaysEdible} matches too, so a full player can
     * still eat one for the effects.
     */
    private static final FoodProperties DIAMOND_APPLE_FOOD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(2.4F)
            .alwaysEdible()
            .build();

    /**
     * What eating one does, at twice a golden apple's {@code Consumables.GOLDEN_APPLE}
     * (Regeneration II for 100 ticks, Absorption I for 2400).
     *
     * <p><b>Durations are doubled, amplifiers are not</b> — the only coherent reading of
     * "double". Absorption's amplifier is already 0, and twice nothing is nothing; doubling
     * Regeneration's would jump II straight to IV, which is a step change rather than a doubling.
     * Twice as long for twice the price is the version that holds together.
     *
     * <p>Built on vanilla's own {@code defaultFood()}, so the eating time, animation, sound and
     * particles are the ones every other food uses.
     */
    private static final Consumable DIAMOND_APPLE_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.REGENERATION, 200, 1),
                    new MobEffectInstance(MobEffects.ABSORPTION, 4800, 0))))
            .build();

    /**
     * A diamond apple. Offered to a villager in conversation, it makes them better at their
     * trade — one level per apple — or frees a nitwit to take up a trade at all.
     *
     * <p>It is also edible, at twice a golden apple. Note the two uses do not collide: eating
     * happens on right-click in the air or at a block, while offering one to a villager goes
     * through the dialogue, and {@code VillagerInteractionHandler} cancels the interaction before
     * the held item is ever consulted.
     *
     * <p>{@code Rarity.RARE} for the aqua name, which reads as diamond and marks it as something
     * you do not find lying around.
     */
    public static final DeferredItem<Item> DIAMOND_APPLE = ITEMS.registerSimpleItem(
            "diamond_apple",
            properties -> properties
                    .rarity(Rarity.RARE)
                    .food(DIAMOND_APPLE_FOOD, DIAMOND_APPLE_CONSUMABLE));

    private VillagerEnhancedItems() {}

    /**
     * Puts the apple in the creative inventory.
     *
     * <p>Food and Drinks rather than Ingredients, even though it is neither: it is an apple, and
     * that is the tab a player will look in. It lands beside the golden apples it is modelled on.
     *
     * <p>{@code ResourceKey}s are interned, so {@code ==} is the correct comparison here.
     */
    @SubscribeEvent
    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(DIAMOND_APPLE);
        }
    }
}
