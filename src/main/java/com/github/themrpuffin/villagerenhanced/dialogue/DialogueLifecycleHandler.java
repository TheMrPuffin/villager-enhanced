package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Drives {@link DialogueSessionManager} from the server lifecycle.
 *
 * <p>Because an open dialogue marks its villager busy, a leaked session is a villager nobody
 * can use. These three hooks make that impossible.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class DialogueLifecycleHandler {

    private DialogueLifecycleHandler() {}

    /**
     * Re-validates every open conversation once per tick.
     *
     * <p>{@code Post} so the world is judged after everything has moved this tick. The manager
     * returns immediately when nothing is open, so the idle cost is one map check.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DialogueSessionManager.tick(event.getServer());
    }

    /**
     * Frees the villager when a player disconnects.
     *
     * <p>The tick check would catch this a tick later, but a client that crashed mid-dialogue
     * is precisely the case that would otherwise strand a villager, so it is handled directly.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DialogueSessionManager.close(player, null);
        }
    }

    /**
     * Drops all sessions when the server stops.
     *
     * <p>Matters in single-player, where the game keeps running after leaving a world; without
     * this, stale entries would survive into the next world loaded.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DialogueSessionManager.clearAll();
    }
}
