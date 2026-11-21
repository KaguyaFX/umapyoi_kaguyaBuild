package net.tracen.umapyoi.trait;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Per-player timers for the Uma traits, counted in seconds. Lives only in memory. */
public class TraitState {
    int crampedSeconds;
    int restlessSeconds;
    int horseSeconds;
    int frightSeconds;
    int curiousCooldown;
    int rainSeconds;
    /** Studious: the fraction of a bonus experience point not given yet. */
    double xpFraction;
    /** Excitable: game time of the last hit given or taken. */
    long lastCombatTick = Long.MIN_VALUE;
    /** Picky eater: the last food eaten and how many times in a row. */
    net.minecraft.world.item.Item lastFood;
    int sameFoodCount;
    /** Fright modifiers currently on the player, so they can be taken off again. */
    final Map<Holder<Attribute>, Map<AttributeModifier.Operation, AttributeModifier>> frightModifiers = new HashMap<>();
}
