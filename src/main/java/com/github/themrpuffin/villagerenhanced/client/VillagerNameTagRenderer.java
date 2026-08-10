package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ClientConfig;
import com.github.themrpuffin.villagerenhanced.dialogue.VillagerNames;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Shows a villager's name above their head while you are looking at them.
 *
 * <p>Only villagers who have actually told you their name get one, which makes this a reminder
 * of who you know rather than a way around asking. The gate is not enforced here: the name
 * attachment is only synced to players who have been introduced, so a client that was never told
 * simply has nothing to draw.
 *
 * <p><b>Vanilla already does most of this.</b> {@code EntityRenderer#shouldShowName} renders a
 * name tag for any mob that both has a custom name and is the entity under the crosshair. All
 * that is missing for a villager is the first half, since this mod deliberately leaves
 * {@code CustomName} alone. So rather than invent a rule for when a name should appear, this
 * reuses vanilla's: same crosshair, same feel as looking at a name-tagged mob.
 *
 * <p>{@code Dist.CLIENT} means NeoForge only registers this on the physical client, so a
 * dedicated server never loads it.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID, value = Dist.CLIENT)
public final class VillagerNameTagRenderer {

    private VillagerNameTagRenderer() {}

    /**
     * Decides whether this villager's name plate is drawn, and what it says.
     *
     * <p>This fires for every visible entity as its render state is extracted, so the checks are
     * ordered cheapest-first and the crosshair test comes early — it rejects everything except at
     * most one entity per frame before anything touches attachment storage.
     */
    @SubscribeEvent
    public static void onCanRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!ClientConfig.SHOW_OVERHEAD_NAMES.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        // The one under the crosshair, and no others. Note this also does the distance limiting:
        // picking only reaches as far as the player can interact, so a name appears exactly when
        // the villager is close enough to talk to. Setting TriState.TRUE below skips vanilla's
        // own distance check, so leaning on the pick range rather than adding one back matters.
        if (villager != Minecraft.getInstance().crosshairPickEntity) {
            return;
        }

        // A player who name-tagged this villager has said what it is called, and vanilla renders
        // that already -- on look, by the same rule. Leave it alone.
        if (villager.hasCustomName()) {
            return;
        }

        String name = VillagerNames.storedName(villager);
        if (name.isEmpty()) {
            // Never introduced, so the name was never synced. Still a stranger.
            return;
        }

        event.setContent(Component.literal(name));
        // TRUE rather than DEFAULT: the default path would consult shouldShowName, which is false
        // here precisely because there is no CustomName.
        event.setCanRender(TriState.TRUE);
    }
}
