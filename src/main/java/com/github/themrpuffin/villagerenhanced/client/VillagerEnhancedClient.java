package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only mod entry point, whose sole job is to make the settings reachable from the Mods
 * menu rather than only by editing a file.
 *
 * <p>Server settings appear here too when a world is open, since they are synced. They show as
 * read-only unless the player has the authority to change them.
 */
@Mod(value = VillagerEnhanced.MODID, dist = Dist.CLIENT)
public class VillagerEnhancedClient {

    public VillagerEnhancedClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
