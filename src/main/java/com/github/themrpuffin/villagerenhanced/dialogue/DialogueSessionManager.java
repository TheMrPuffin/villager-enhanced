package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.config.ServerConfig;
import com.github.themrpuffin.villagerenhanced.network.DialogueClosedPayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

/**
 * Tracks who is talking to whom, and cleans up after them.
 *
 * <p>Two reasons this exists. The server must know a player really has a dialogue open with a
 * villager before acting on anything they send. More urgently, an open dialogue marks the
 * villager <b>busy</b> and refuses every other player — so a session that is opened and never
 * closed is a villager permanently out of service. Every way a conversation can end is
 * therefore handled, with {@link #tick} as the catch-all.
 *
 * <p><b>Threading:</b> everything here runs on the main server thread — payload handlers are
 * dispatched there by default, as are tick and lifecycle events — so a plain {@code HashMap} is
 * safe. Moving any caller off-thread would require revisiting this.
 */
public final class DialogueSessionManager {

    private static final Map<UUID, DialogueSession> SESSIONS = new HashMap<>();

    private DialogueSessionManager() {}

    /**
     * Is this villager already occupied by somebody other than this player?
     *
     * <p><b>Our own sessions are the authority, not vanilla's trading-player flag.</b> Vanilla
     * clears that flag every tick for villagers with no profession —
     * {@code customServerAiStep} calls {@code stopTrading()} whenever an unemployed villager is
     * marked as trading — so relying on it alone would leave the unemployed unprotected, and two
     * players could hold a conversation with the same one. Nitwits are unaffected, since the
     * check is against {@code NONE} specifically.
     *
     * <p>The vanilla flag is still consulted afterwards, because someone may be mid-trade
     * through the merchant screen rather than in a dialogue.
     */
    public static boolean isBusyWithSomeoneElse(Villager villager, ServerPlayer player) {
        for (Map.Entry<UUID, DialogueSession> entry : SESSIONS.entrySet()) {
            if (entry.getKey().equals(player.getUUID())) {
                continue;
            }
            DialogueSession session = entry.getValue();
            if (session.villagerId() == villager.getId()
                    && session.dimension().equals(villager.level().dimension())) {
                return true;
            }
        }

        return villager.isTrading() && villager.getTradingPlayer() != player;
    }

    /**
     * Starts a conversation and marks the villager busy.
     *
     * <p>Closes any conversation the player already had, so right-clicking a second villager
     * cannot leave the first one locked.
     */
    public static void open(ServerPlayer player, Villager villager) {
        close(player, null);

        // Occupying the villager is what stops a second player talking to them. Servers that
        // would rather several players share a villager can switch it off, in which case the
        // villager is only ever occupied while actually trading, as in vanilla.
        if (ServerConfig.HOLD_VILLAGER_DURING_DIALOGUE.get()) {
            villager.setTradingPlayer(player);
        }
        SESSIONS.put(player.getUUID(), new DialogueSession(
                player.level().dimension(), villager.getId(), DialoguePage.GREETING));
    }

    /** Is this player currently in a dialogue with this specific villager? */
    public static boolean isTalkingTo(ServerPlayer player, Villager villager) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        return session != null
                && session.villagerId() == villager.getId()
                && session.dimension().equals(player.level().dimension());
    }

    /** Which page this player is on, or null if they are not in a conversation. */
    public static @Nullable DialoguePage currentPage(ServerPlayer player) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        return session == null ? null : session.page();
    }

    /**
     * Records that the player has navigated to a different page.
     *
     * <p>A no-op without a session, so sending a page to somebody who is not in a conversation
     * cannot conjure one.
     */
    public static void setPage(ServerPlayer player, DialoguePage page) {
        SESSIONS.computeIfPresent(player.getUUID(), (uuid, session) -> session.withPage(page));
    }

    /**
     * Ends the conversation and frees the villager.
     *
     * @param reason if given, the client is told to close its screen and shown this message.
     *               Null when the client already knows — it clicked Leave, or the server is
     *               replacing the screen with something else.
     */
    public static void close(ServerPlayer player, @Nullable Component reason) {
        DialogueSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }

        releaseVillager(player.level().getServer(), session, player);

        if (reason != null) {
            PacketDistributor.sendToPlayer(player, new DialogueClosedPayload(reason));
        }
    }

    /**
     * Ends the conversation <b>without</b> freeing the villager.
     *
     * <p>Only for the trade handoff. {@code MerchantMenu} takes ownership of the trading-player
     * flag and clears it when closed, so releasing here would break the trade the instant it
     * opened.
     */
    public static void endForTradeHandoff(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    /**
     * Re-checks every open conversation and ends those no longer valid.
     *
     * <p>Called every server tick. This is the catch-all that covers what individual event
     * hooks would miss: the villager died, despawned or unloaded; the player walked away, went
     * through a portal, died or vanished. Rather than hooking each case, we ask once per tick
     * whether the conversation still makes sense.
     */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, DialogueSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DialogueSession> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            DialogueSession session = entry.getValue();

            Villager villager = resolveVillager(server, session);
            Component reason = invalidReason(player, villager, session);
            if (reason == null) {
                // Vanilla clears the trading-player flag every tick on villagers with no
                // profession, so re-assert it. Without this, an unemployed villager wanders off
                // mid-conversation while every other villager politely stands and faces you --
                // the flag is what drives LookAndFollowTradingPlayerSink, the brain behaviour
                // that walks a villager to whoever it is dealing with.
                if (villager != null && player != null
                        && ServerConfig.HOLD_VILLAGER_DURING_DIALOGUE.get()
                        && villager.getTradingPlayer() != player) {
                    villager.setTradingPlayer(player);
                }
                continue;
            }

            // Drop the session before releasing, so releaseVillager cannot recurse into close().
            iterator.remove();
            releaseVillager(server, session, player);

            if (player != null) {
                PacketDistributor.sendToPlayer(player, new DialogueClosedPayload(reason));
                VillagerEnhanced.LOGGER.debug(
                        "Closed dialogue for {}: {}", player.getName().getString(), reason.getString());
            }
        }
    }

    /** Why this conversation should end, or null if it is still valid. */
    private static @Nullable Component invalidReason(
            @Nullable ServerPlayer player, @Nullable Villager villager, DialogueSession session) {
        if (player == null || player.hasDisconnected() || !player.isAlive()) {
            return Component.translatable("villagerenhanced.dialogue.closed.gone");
        }
        if (villager == null || !villager.isAlive()) {
            return Component.translatable("villagerenhanced.dialogue.closed.villager_gone");
        }
        if (!player.level().dimension().equals(session.dimension())) {
            return Component.translatable("villagerenhanced.dialogue.closed.too_far");
        }
        if (!player.isWithinEntityInteractionRange(villager, ServerConfig.CONVERSATION_RANGE.get())) {
            return Component.translatable("villagerenhanced.dialogue.closed.too_far");
        }
        return null;
    }

    private static @Nullable Villager resolveVillager(MinecraftServer server, DialogueSession session) {
        ServerLevel level = server.getLevel(session.dimension());
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(session.villagerId());
        return entity instanceof Villager villager ? villager : null;
    }

    /**
     * Clears the villager's busy flag, but only if it is still busy with <i>this</i> player.
     *
     * <p>The guard matters: if the player has since opened the trade screen, the merchant menu
     * owns that flag and clearing it would break the trade.
     */
    private static void releaseVillager(
            @Nullable MinecraftServer server, DialogueSession session, @Nullable ServerPlayer player) {
        if (server == null) {
            return;
        }
        Villager villager = resolveVillager(server, session);
        if (villager != null && villager.getTradingPlayer() == player) {
            villager.setTradingPlayer(null);
        }
    }

    /** Called when the server stops, so single-player sessions do not leak between worlds. */
    public static void clearAll() {
        SESSIONS.clear();
    }
}
