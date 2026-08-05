package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Reproduces vanilla's hand-off into the merchant screen.
 *
 * <p>Vanilla does this in {@code Villager#startTrading}, which is private, so its three steps
 * are copied here. Keeping them in one place stops the Trade button and the sneak shortcut
 * drifting apart.
 */
public final class VillagerTrading {

    private VillagerTrading() {}

    /**
     * Opens the vanilla trade screen, as right-clicking a villager does in vanilla.
     *
     * <p>Server-side only. The merchant screen cannot be opened from the client: the offers
     * live on the server and {@code openTradingScreen} builds the menu and sends them down.
     * That is the reason the dialogue needs a network round trip at all.
     *
     * @return true if the trade screen opened; false if this villager has nothing to trade
     */
    public static boolean openTradeScreen(ServerPlayer player, Villager villager) {
        // Vanilla refuses to open an empty merchant screen -- it shakes the villager's head
        // instead -- and so do we. An empty trade window looks like a bug.
        if (villager.getOffers().isEmpty()) {
            // Only actually visible on the sneak-to-trade path, where no screen is open. From
            // the dialogue's Trade button the screen background covers the action bar, which is
            // why that button is greyed out instead.
            player.sendOverlayMessage(Component.translatable("villagerenhanced.dialogue.no_trades"));
            return false;
        }

        // Applies the reputation discount. Private in vanilla; widened by our access transformer.
        villager.updateSpecialPrices(player);

        // Marks the villager busy, so other players cannot trade with it simultaneously.
        villager.setTradingPlayer(player);

        // Replaces whatever screen the player had open, which is how the dialogue gives way.
        villager.openTradingScreen(player, villager.getDisplayName(), villager.getVillagerData().level());

        VillagerEnhanced.LOGGER.debug(
                "Opened trade screen: player={} villager={}",
                player.getName().getString(), villager.getUUID());
        return true;
    }
}
