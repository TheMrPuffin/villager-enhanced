package com.github.themrpuffin.villagerenhanced.dialogue;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * One player's open conversation with one villager.
 *
 * <p>Stores where to find the villager rather than a reference to it. A direct reference would
 * keep a dead villager in memory and go stale when its chunk unloaded; a dimension and an
 * entity id can always be re-resolved, and failing to resolve is itself a useful signal that
 * the conversation is over.
 *
 * @param dimension  which world the villager is in
 * @param villagerId the villager's entity id within that world
 */
public record DialogueSession(ResourceKey<Level> dimension, int villagerId) {}
