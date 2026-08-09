package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.Comparator;
import java.util.List;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;
import com.github.themrpuffin.villagerenhanced.network.VillagerNotificationPayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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

    /** How far from a newborn to look for the adults that produced it. */
    private static final double PARENT_SEARCH_RADIUS = 8.0;

    /**
     * A villager was born.
     *
     * <p><b>Not {@code BabyEntitySpawnEvent}</b>, which despite the name is fired only by
     * {@code Animal} and {@code Fox}. Villagers descend from {@code AgeableMob}, not
     * {@code Animal}, and breed through the {@code VillagerMakeLove} brain behaviour, which adds
     * the child with {@code addFreshEntityWithPassengers} and raises no event of its own. Hooking
     * the baby joining the level is the only signal there is.
     *
     * <p>That signal is broader than a birth, though: it also fires for baby villagers in a
     * village being generated as a player explores into it, and for spawn eggs. They are told
     * apart by the parents. {@code VillagerMakeLove} sets both parents to age 6000 — their
     * breeding cooldown — <i>before</i> adding the child, so at this moment a real birth has two
     * adults beside it on cooldown, where a freshly generated village has adults at age 0.
     * Finding them also supplies the names for the message.
     */
    @SubscribeEvent
    public static void onVillagerBorn(EntityJoinLevelEvent event) {
        if (!ServerConfig.NOTIFY_VILLAGER_BIRTHS.get()) {
            return;
        }
        if (event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof Villager child) || !child.isBaby()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        List<Villager> parents = parentsOf(level, child);
        if (parents.size() < 2) {
            return;
        }

        Villager parentA = parents.get(0);
        Villager parentB = parents.get(1);
        notifyNearby(level, child, player -> Component.translatable(
                "villagerenhanced.notification.born",
                subjectOf(parentA, player),
                subjectOf(parentB, player)));
    }

    /** The two nearest adults on breeding cooldown, which is what a just-bred pair looks like. */
    private static List<Villager> parentsOf(ServerLevel level, Villager child) {
        AABB area = child.getBoundingBox().inflate(PARENT_SEARCH_RADIUS);

        return level.getEntitiesOfClass(Villager.class, area,
                        other -> other != child && !other.isBaby() && other.getAge() > 0)
                .stream()
                .sorted(Comparator.comparingDouble(other -> other.distanceToSqr(child)))
                .limit(2)
                .toList();
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
