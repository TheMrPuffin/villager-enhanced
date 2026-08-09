package com.github.themrpuffin.villagerenhanced.dialogue;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;
import com.github.themrpuffin.villagerenhanced.network.VillagerNotificationPayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells nearby players when a villager is born or killed.
 *
 * <p>Losing a villager to a zombie while you are off mining is the sort of thing you want to
 * find out about at the time, not three days later when you notice the beds are empty.
 *
 * <p>Messages are composed <b>per recipient</b>, because names depend on introductions: one
 * player may hear "Mira was killed" where another, who has never spoken to her, hears "the
 * Librarian was killed".
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class VillagerNotifications {

    private VillagerNotifications() {}

    /**
     * A villager died.
     *
     * <p>The attacker is named when there is one, because "killed by a Zombie" and "died" call
     * for very different responses from the player. Vanilla's own death message is not reused:
     * it would say "Villager", losing the name this mod exists to give them.
     */
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!ServerConfig.NOTIFY_VILLAGER_DEATHS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }

        Entity killer = event.getSource().getEntity();
        notifyNearby(level, villager, player -> {
            Component name = subjectOf(villager, player);
            return killer == null
                    ? Component.translatable("villagerenhanced.notification.died", name)
                    : Component.translatable(
                            "villagerenhanced.notification.killed", name, killer.getDisplayName());
        });
    }

    /**
     * A villager was born.
     *
     * <p>Named after the parents rather than the child: the event fires before the baby is added
     * to the world, so naming it here would assign a name from a position it does not have yet.
     */
    @SubscribeEvent
    public static void onVillagerBorn(BabyEntitySpawnEvent event) {
        if (!ServerConfig.NOTIFY_VILLAGER_BIRTHS.get()) {
            return;
        }
        if (!(event.getParentA() instanceof Villager parentA)
                || !(event.getParentB() instanceof Villager parentB)) {
            return;
        }
        if (!(parentA.level() instanceof ServerLevel level)) {
            return;
        }

        notifyNearby(level, parentA, player -> Component.translatable(
                "villagerenhanced.notification.born",
                subjectOf(parentA, player),
                subjectOf(parentB, player)));
    }

    /**
     * How this player would refer to this villager: by name if introduced, by trade otherwise.
     */
    private static Component subjectOf(Villager villager, ServerPlayer player) {
        return Component.translatable(
                "villagerenhanced.notification.subject",
                VillagerNames.displayNameFor(villager, player),
                villager.getVillagerData().profession().value().name());
    }

    /**
     * Sends a message to every player close enough to care.
     *
     * <p>The radius is generous by default, since the point is hearing about a raid you are not
     * standing in — but a busy server with many villages will want it smaller.
     */
    private static void notifyNearby(ServerLevel level, Mob about, MessageFor message) {
        double radius = ServerConfig.NOTIFICATION_RADIUS.get();
        double radiusSqr = radius * radius;

        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(about) <= radiusSqr)) {
            PacketDistributor.sendToPlayer(player, new VillagerNotificationPayload(message.forPlayer(player)));
        }
    }

    /** Builds the line one recipient should see. */
    @FunctionalInterface
    private interface MessageFor {
        Component forPlayer(ServerPlayer player);
    }
}
