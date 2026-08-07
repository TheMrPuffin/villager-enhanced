package com.github.themrpuffin.villagerenhanced;

import com.github.themrpuffin.villagerenhanced.attachment.VillagerEnhancedAttachments;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

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

    /** FML supplies the mod event bus; it recognises the parameter type and passes it in. */
    public VillagerEnhanced(IEventBus modEventBus) {
        VillagerEnhancedAttachments.ATTACHMENT_TYPES.register(modEventBus);
    }
}
