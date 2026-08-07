package com.github.themrpuffin.villagerenhanced.attachment;

import java.util.function.Supplier;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.mojang.serialization.Codec;

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

    private VillagerEnhancedAttachments() {}
}
