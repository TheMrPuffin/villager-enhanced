package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * Villagers talking about what they do.
 *
 * <p>Flavour, plus a nudge about what they will offer as their trades improve — the one piece of
 * genuinely useful information a villager can give about their own work.
 *
 * <p>Professions are matched against known keys rather than having a lang key built from the
 * registry name. Building the key would be shorter, but a modded profession would then produce a
 * translation key nothing defines, and the player would see the raw key. Matching explicitly
 * means anything unrecognised falls back to a line that always exists.
 */
public final class VillagerWork {

    /** Lang key suffix per profession. Anything absent uses {@link #UNKNOWN_KEY}. */
    private static final Map<ResourceKey<VillagerProfession>, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(VillagerProfession.NONE, "none"),
            Map.entry(VillagerProfession.NITWIT, "nitwit"),
            Map.entry(VillagerProfession.ARMORER, "armorer"),
            Map.entry(VillagerProfession.BUTCHER, "butcher"),
            Map.entry(VillagerProfession.CARTOGRAPHER, "cartographer"),
            Map.entry(VillagerProfession.CLERIC, "cleric"),
            Map.entry(VillagerProfession.FARMER, "farmer"),
            Map.entry(VillagerProfession.FISHERMAN, "fisherman"),
            Map.entry(VillagerProfession.FLETCHER, "fletcher"),
            Map.entry(VillagerProfession.LEATHERWORKER, "leatherworker"),
            Map.entry(VillagerProfession.LIBRARIAN, "librarian"),
            Map.entry(VillagerProfession.MASON, "mason"),
            Map.entry(VillagerProfession.SHEPHERD, "shepherd"),
            Map.entry(VillagerProfession.TOOLSMITH, "toolsmith"),
            Map.entry(VillagerProfession.WEAPONSMITH, "weaponsmith"));

    private static final String UNKNOWN_KEY = "villagerenhanced.dialogue.work.unknown";

    /** Villagers who never trade have nothing to say about improving. */
    private static final List<ResourceKey<VillagerProfession>> NEVER_TRADES =
            List.of(VillagerProfession.NONE, VillagerProfession.NITWIT);

    private VillagerWork() {}

    /**
     * What this villager says about their trade, and how far along it they are.
     *
     * <p>Two lines for anyone who trades: what they do, and what improving would bring. The
     * unemployed and nitwits get one, since neither will ever have offers.
     */
    public static List<Component> describe(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        Component description = Component.translatable(descriptionKeyFor(profession));

        if (NEVER_TRADES.stream().anyMatch(profession::is)) {
            return List.of(description);
        }

        int level = villager.getVillagerData().level();
        Component progress = level >= VillagerData.MAX_VILLAGER_LEVEL
                ? Component.translatable("villagerenhanced.dialogue.work.mastered")
                : Component.translatable("villagerenhanced.dialogue.work.improving", level);

        return List.of(description, progress);
    }

    private static String descriptionKeyFor(Holder<VillagerProfession> profession) {
        for (Map.Entry<ResourceKey<VillagerProfession>, String> entry : DESCRIPTIONS.entrySet()) {
            if (profession.is(entry.getKey())) {
                return "villagerenhanced.dialogue.work." + entry.getValue();
            }
        }
        return UNKNOWN_KEY;
    }
}
