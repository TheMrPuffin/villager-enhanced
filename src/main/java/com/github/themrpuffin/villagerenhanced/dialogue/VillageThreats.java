package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import org.jspecify.annotations.Nullable;

/**
 * Tells players when a raid is coming, and how it ends.
 *
 * <p>Being off mining while a raid takes the village apart is the same problem 0.8.0's death
 * notifications solved, one scale up.
 *
 * <p><b>Raids give real warning, which is the surprise here.</b> A raid does not simply start.
 * Carrying Bad Omen into a village converts it to Raid Omen with a fixed 600-tick duration —
 * exactly thirty seconds — and the raid is only created when that effect <i>expires</i>
 * ({@code RaidOmenMobEffect}). So there is a half minute in which the raid is loaded and has not
 * broken, which is worth telling people about.
 *
 * <p><b>Three messages, not one per wave.</b> Waves were tried and dropped: a raid runs for
 * minutes and a line per wave turns a warning into a running commentary. Wave numbers are left
 * out of the opening message too, since promising "wave 1 of 5" and then going quiet is worse
 * than never having raised the subject.
 *
 * <p><b>Zombie sieges are not reported.</b> {@code VillageSiegeEvent} exists and fires the moment
 * a siege picks its spot, but that is only a couple of seconds before the zombies arrive — a
 * message that lands at the same time as the thing it describes is not a warning.
 *
 * <p>Nothing here changes what happens; it only reports.
 *
 * <p>Server-side only.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class VillageThreats {

    /**
     * How often raids are polled. Raids move in waves over minutes, so once a second is ample —
     * and it keeps the cost off the tick loop, since there is no raid event to subscribe to.
     */
    private static final int POLL_INTERVAL_TICKS = 20;

    /** Identifies a raid across dimensions; raid ids are only unique within one. */
    private record RaidKey(ResourceKey<Level> dimension, int raidId) {}

    /**
     * Raids already announced, and whether their outcome has been reported too.
     *
     * <p>Presence means the raid's start has been announced, so the value only has to carry the
     * second half.
     */
    private static final Map<RaidKey, Boolean> RAIDS = new HashMap<>();

    /** Players already told about the omen they are carrying. */
    private static final Set<UUID> OMEN_WARNED = new HashSet<>();

    private VillageThreats() {}

    /**
     * Polls for raids.
     *
     * <p>There is no raid event in NeoForge, so this watches instead — which turns out to give
     * more than an event would have: start, every wave, and the outcome, rather than a single
     * "a raid began".
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % POLL_INTERVAL_TICKS != 0) {
            return;
        }
        if (!ServerConfig.NOTIFY_RAIDS.get()) {
            return;
        }

        warnOfCarriedOmens(level);
        followRaids(level);
        forgetFinishedRaids(level);
    }

    /**
     * A player is carrying an omen that is about to break.
     *
     * <p>Announced to everyone nearby rather than only the carrier: it is the village that is
     * about to be attacked, and on a server the people who did not bring it are the ones most in
     * need of telling.
     */
    private static void warnOfCarriedOmens(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            MobEffectInstance omen = player.getEffect(MobEffects.RAID_OMEN);

            if (omen == null) {
                // Forgotten as soon as the effect goes, so a second omen warns again rather than
                // the player being permanently marked as told.
                OMEN_WARNED.remove(player.getUUID());
                continue;
            }

            if (!OMEN_WARNED.add(player.getUUID())) {
                continue;
            }

            // Rounded up, because "0 seconds" on a countdown that has not finished reads wrong.
            int seconds = Math.max(1, omen.getDuration() / 20);
            VillagerNotifications.announceNear(level, player.blockPosition(),
                    Component.translatable("villagerenhanced.notification.raid_omen", seconds));
        }
    }

    /**
     * Reports any raid a player is standing in.
     *
     * <p>Driven from players rather than from the raid list, because {@code Raids} exposes no way
     * to enumerate every raid — and a raid nobody is near is one nobody needs telling about.
     * Several players in one raid is harmless: the first poll records the change and the rest
     * find nothing new to say.
     */
    private static void followRaids(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            Raid raid = level.getRaidAt(player.blockPosition());
            if (raid != null) {
                report(level, raid);
            }
        }
    }

    private static void report(ServerLevel level, Raid raid) {
        OptionalInt id = level.getRaids().getId(raid);
        if (id.isEmpty()) {
            return;
        }

        RaidKey key = new RaidKey(level.dimension(), id.getAsInt());
        Boolean outcomeAnnounced = RAIDS.get(key);

        if (raid.isOver()) {
            // A won raid celebrates for 600 ticks and a lost one lingers, so a one-second poll
            // has plenty of time to catch either before the raid is dropped.
            if (outcomeAnnounced != null && !outcomeAnnounced) {
                VillagerNotifications.announceNear(level, raid.getCenter(),
                        Component.translatable(raid.isVictory()
                                ? "villagerenhanced.notification.raid_won"
                                : "villagerenhanced.notification.raid_lost"));
                RAIDS.put(key, true);
            }
            return;
        }

        if (!raid.hasFirstWaveSpawned()) {
            // Created but not yet broken -- the omen warning above covers this window.
            return;
        }

        if (outcomeAnnounced == null) {
            VillagerNotifications.announceNear(level, raid.getCenter(),
                    Component.translatable("villagerenhanced.notification.raid_begins"));
            RAIDS.put(key, false);
        }
    }

    /** Drops raids that no longer exist, so the map does not grow for the life of the server. */
    private static void forgetFinishedRaids(ServerLevel level) {
        RAIDS.keySet().removeIf(key -> key.dimension().equals(level.dimension())
                && level.getRaids().get(key.raidId()) == null);
    }

    /**
     * Ringing a bell during a raid lights up every raider in it.
     *
     * <p><b>Vanilla already does this and it does not help.</b> {@code BellBlockEntity} glows
     * raiders for 60 ticks, within 48 blocks, and only resonates at all if one is already within
     * 32 — so it lights up the raider you were looking at anyway and does nothing about the one
     * you cannot find. The failure it needs to solve is the last raider spawning badly and
     * getting stuck somewhere out of sight, which is precisely the case it excludes.
     *
     * <p>This uses {@code Raid#getAllRaiders}, which has no distance limit, so the whole raid
     * lights up however far out it has wandered. The count is sent too, because glowing only
     * renders on entities the client is tracking — beyond that, knowing three are still alive is
     * the only thing that helps.
     *
     * <p>Vanilla's own ring is left alone; this runs alongside it rather than replacing it.
     */
    @SubscribeEvent
    public static void onBellRung(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // A single right-click fires this event once per hand.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int seconds = ServerConfig.RAID_GLOW_SECONDS.get();
        if (seconds <= 0) {
            return;
        }

        BlockPos bell = event.getPos();
        BlockState state = level.getBlockState(bell);
        if (!state.is(Blocks.BELL)) {
            return;
        }
        if (!wouldRing(state, event.getFace(), event.getHitVec().getLocation().y - bell.getY())) {
            return;
        }

        Raid raid = level.getRaidAt(bell);
        if (raid == null) {
            return;
        }

        int alive = 0;
        for (Raider raider : raid.getAllRaiders()) {
            if (raider.isAlive()) {
                // Not ambient, and not visible: glowing particles on a pillager would read as a
                // status effect rather than a search light.
                raider.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, seconds * 20, 0, false, false));
                alive++;
            }
        }

        // The action bar rather than a notification: this answers something the player just did,
        // so it is not theirs to switch off, and it should not go to everyone else in the village.
        player.sendOverlayMessage(
                Component.translatable("villagerenhanced.raid.raiders_lit", alive));
    }

    /**
     * Will this click actually ring the bell?
     *
     * <p>A copy of the private {@code BellBlock#isProperHit}. Copied rather than widened by
     * access transformer because it is fifteen lines of geometry over two <b>public</b> block
     * state properties and holds no state — an access transformer would be permanent production
     * surface for something that can simply be restated.
     *
     * <p>Without this, clicking the top of a bell or the wrong face lights the whole raid without
     * a sound, which reads as the mod firing at random. The one case still not modelled is
     * sneaking with a placeable item, where vanilla places the block instead of ringing.
     *
     * <p><b>If bells change in a future version, this is the line that silently goes stale.</b>
     */
    private static boolean wouldRing(BlockState state, Direction face, double clickY) {
        // The top of the bell, and its own axis, do not swing it.
        if (face.getAxis() == Direction.Axis.Y || clickY > 0.8124F) {
            return false;
        }

        Direction facing = state.getValue(BellBlock.FACING);
        return switch (state.getValue(BellBlock.ATTACHMENT)) {
            case FLOOR -> facing.getAxis() == face.getAxis();
            case SINGLE_WALL, DOUBLE_WALL -> facing.getAxis() != face.getAxis();
            case CEILING -> true;
        };
    }

    /**
     * What a villager says instead of hello, when their village is under threat.
     *
     * <p>Deliberately <b>not</b> gated on the notification settings: turning off chat messages is
     * a request for less noise, not for villagers to behave as though nothing is happening.
     *
     * @return null when there is nothing wrong, so the ordinary greeting is used
     */
    public static @Nullable Component greetingUnderThreat(ServerPlayer player, Villager villager) {
        // A player carrying an omen is a raid that has not broken yet. Bad Omen is visible on the
        // player, so a villager noticing it is fair.
        if (player.hasEffect(MobEffects.RAID_OMEN)) {
            return Component.translatable("villagerenhanced.dialogue.greeting.omen");
        }

        if (!(villager.level() instanceof ServerLevel level)) {
            return null;
        }

        Raid raid = level.getRaidAt(villager.blockPosition());
        if (raid == null) {
            return null;
        }

        if (raid.isVictory()) {
            return Component.translatable("villagerenhanced.dialogue.greeting.raid_won");
        }
        if (raid.isLoss()) {
            // The village is already gone; there is nothing left to warn about, so the ordinary
            // greeting is the more honest one.
            return null;
        }
        if (!raid.hasFirstWaveSpawned()) {
            return Component.translatable("villagerenhanced.dialogue.greeting.raid_coming");
        }
        return Component.translatable("villagerenhanced.dialogue.greeting.raid");
    }

    /** Nothing here survives a world, and raid ids restart with one. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RAIDS.clear();
        OMEN_WARNED.clear();
    }
}
