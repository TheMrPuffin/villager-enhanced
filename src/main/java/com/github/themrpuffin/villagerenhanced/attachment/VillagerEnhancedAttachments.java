package com.github.themrpuffin.villagerenhanced.attachment;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.dialogue.PlayerRelationship;
import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Extra data the mod stores on vanilla objects.
 *
 * <p>Data Attachments are NeoForge's replacement for the old capability system: a typed value
 * bolted onto any vanilla object, saved and loaded with it, without touching vanilla classes.
 */
public final class VillagerEnhancedAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, VillagerEnhanced.MODID);

    /**
     * The villager's given name, stored rather than derived.
     *
     * <p>Storing it is what makes names reliably distinct. A name derived from the villager's
     * UUID would be an independent roll per villager, so avoiding coincidental clashes across a
     * village would need thousands of names — 8 villagers drawing from 20 names collide 80% of
     * the time. Assigning a name and remembering it turns that into a much easier problem:
     * look at who is nearby, pick something unused. The pool only has to exceed the size of a
     * village.
     *
     * <p>An empty string means "not yet named". {@code getData} attaches the default on first
     * call, so callers must check {@code hasData} first if they need to distinguish the two.
     *
     * <p>Not synced. The client learns names from {@code OpenDialoguePayload}. If names are ever
     * rendered above villagers' heads, add {@code .sync(...)} here rather than writing a packet.
     */
    public static final Supplier<AttachmentType<String>> VILLAGER_NAME = ATTACHMENT_TYPES.register(
            "villager_name",
            () -> AttachmentType.<String>builder(() -> "")
                    .serialize(Codec.STRING.fieldOf("name"))
                    .build());

    /**
     * What this villager remembers about each player who has spoken to it.
     *
     * <p>Keyed by player UUID. Absent entries mean "never met", so the map only grows for
     * players who have actually held a conversation — a villager nobody has talked to stores
     * nothing.
     *
     * <p>Kept deliberately small, because this rides along with every villager in the world. If
     * it ever needs to hold much more per player, consider pruning entries for players who have
     * not been seen in a long time.
     */
    public static final Supplier<AttachmentType<Map<UUID, PlayerRelationship>>> VILLAGER_RELATIONSHIPS =
            ATTACHMENT_TYPES.register(
                    "villager_relationships",
                    () -> AttachmentType.<Map<UUID, PlayerRelationship>>builder(Map::of)
                            .serialize(Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerRelationship.CODEC)
                                    .fieldOf("relationships"))
                            .build());

    private VillagerEnhancedAttachments() {}
}
