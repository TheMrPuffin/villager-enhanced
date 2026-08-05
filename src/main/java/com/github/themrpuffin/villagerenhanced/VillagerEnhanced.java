package com.github.themrpuffin.villagerenhanced;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

import org.slf4j.Logger;

/**
 * Mod entry point.
 *
 * <p>Villager Enhanced replaces the vanilla trade screen with a dialogue window. Right-clicking
 * a villager opens a conversation offering Trade and Leave; sneak + right-click skips straight
 * to trading.
 *
 * <p>There is deliberately no setup code here. Everything registers itself through
 * {@code @EventBusSubscriber}, so this class only exists to anchor the {@code @Mod} annotation
 * and hold the two constants everything else references.
 */
@Mod(VillagerEnhanced.MODID)
public class VillagerEnhanced {

    /** Must match the modId in neoforge.mods.toml and the mod_id in gradle.properties. */
    public static final String MODID = "villagerenhanced";

    public static final Logger LOGGER = LogUtils.getLogger();
}
