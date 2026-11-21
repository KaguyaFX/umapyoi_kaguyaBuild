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
    /** Fright modifiers currently on the player, so they can be taken off again. */
    final Map<Holder<Attribute>, Map<AttributeModifier.Operation, AttributeModifier>> frightModifiers = new HashMap<>();
}
