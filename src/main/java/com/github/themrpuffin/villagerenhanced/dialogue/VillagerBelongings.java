package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

/**
 * What a villager is carrying, told rather than displayed.
 *
 * <p>Vanilla villagers have a real, saved eight-slot inventory. They pick up only what the
 * {@code #minecraft:villager_picks_up} tag allows — bread, wheat, beetroot, plantable seeds —
 * or what their profession requests, and they use it: eating raises their food level, surplus
 * food is handed to villagers who want to breed, and farmers plant seeds from it. Players have
 * never been able to see any of that.
 *
 * <p>Rendered as spoken lines rather than a grid of slots. A grid would say "rifle through their
 * pockets"; a list of what they mention having says "ask someone what they are carrying", which
 * is what the option actually is. It also needs no new payload field, since the dialogue body is
 * already a list of lines.
 *
 * <p>Expect most villagers to be carrying nothing most of the time. That is informative rather
 * than disappointing: an empty-handed villager beside one with twenty bread tells you who is
 * about to breed.
 */
public final class VillagerBelongings {

    private VillagerBelongings() {}

    /** One line per distinct stack, or a single line saying they have nothing. */
    public static List<Component> describe(Villager villager) {
        SimpleContainer inventory = villager.getInventory();
        List<Component> lines = new ArrayList<>();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            lines.add(Component.translatable(
                    "villagerenhanced.dialogue.belongings.item",
                    stack.getCount(),
                    stack.getHoverName()));
        }

        if (lines.isEmpty()) {
            return List.of(Component.translatable("villagerenhanced.dialogue.belongings.empty"));
        }

        lines.addFirst(Component.translatable("villagerenhanced.dialogue.belongings.intro"));
        return List.copyOf(lines);
    }
}
