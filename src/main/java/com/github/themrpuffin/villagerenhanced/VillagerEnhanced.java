package com.github.themrpuffin.villagerenhanced;

import com.github.themrpuffin.villagerenhanced.attachment.VillagerEnhancedAttachments;
import com.github.themrpuffin.villagerenhanced.config.ClientConfig;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;
import com.github.themrpuffin.villagerenhanced.item.VillagerEnhancedItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import org.slf4j.Logger;

/**
 * Mod entry point.
 *
 * <p>Villager Enhanced replaces the vanilla trade screen with a dialogue window. Right-clicking
 * a villager opens a conversation offering Trade and Leave; sneak + right-click skips straight
 * to trading.
 *
 * <p>Event handling registers itself through {@code @EventBusSubscriber}. The only thing that
 * cannot is a {@code DeferredRegister}, which has to be attached to the mod event bus by hand —
 * hence the constructor.
 */
@Mod(VillagerEnhanced.MODID)
public class VillagerEnhanced {

    /** Must match the modId in neoforge.mods.toml and the mod_id in gradle.properties. */
    public static final String MODID = "villagerenhanced";

    public static final Logger LOGGER = LogUtils.getLogger();

    /** FML supplies these; it recognises the parameter types and passes them in. */
    public VillagerEnhanced(IEventBus modEventBus, ModContainer modContainer) {
        VillagerEnhancedAttachments.ATTACHMENT_TYPES.register(modEventBus);
        VillagerEnhancedItems.ITEMS.register(modEventBus);

        // CLIENT lives in each player's own files and is never consulted by the server; SERVER
        // lives with the world and is synced to clients on join. Which of the two a value goes
        // in is a correctness decision, not a preference -- see the config classes.
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
