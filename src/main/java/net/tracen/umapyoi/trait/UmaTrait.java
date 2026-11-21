package net.tracen.umapyoi.trait;

import java.util.Arrays;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Uma traits: what the body is like. Rolled by the Three Goddesses (2 per soul), active whenever
 * the soul is worn. "Afraid of a creature" carries its target, stored as "fear:minecraft:skeleton".
 */
public enum UmaTrait {
    FEAR("fear", false),
    CLAUSTROPHOBIA("claustrophobia", false),
    FEAR_OF_FIRE("fear_of_fire", false),
    RESTLESS("restless", false),
    CURIOUS("curious", true),
    NIGHT_VISION("night_vision", true),
    HORSE_LOVER("horse_lover", true);

    private final String id;
    private final boolean positive;

    UmaTrait(String id, boolean positive) {
        this.id = id;
        this.positive = positive;
    }

    public String id() {
        return id;
    }

    public boolean positive() {
        return positive;
    }

    /** A stored entry: the trait plus its target creature for FEAR. */
    public record Entry(UmaTrait trait, @Nullable EntityType<?> target) {
        public Component displayName() {
            if (trait == FEAR)
                return Component.translatable("umapyoi.trait.uma.fear",
                        target == null ? Component.literal("?") : target.getDescription());
            return Component.translatable("umapyoi.trait.uma." + trait.id);
        }

        public Component description() {
            return Component.translatable("umapyoi.trait.uma." + trait.id + ".desc");
        }
    }

    /** Parses "claustrophobia" or "fear:minecraft:skeleton"; empty for anything unknown. */
    public static Optional<Entry> parse(String stored) {
        String id = stored;
        String argument = null;
        int colon = stored.indexOf(':');
        if (colon >= 0) {
            id = stored.substring(0, colon);
            argument = stored.substring(colon + 1);
        }
        final String key = id;
        Optional<UmaTrait> trait = Arrays.stream(values()).filter(t -> t.id.equals(key)).findFirst();
        if (trait.isEmpty())
            return Optional.empty();
        if (trait.get() != FEAR)
            return Optional.of(new Entry(trait.get(), null));
        ResourceLocation target = argument == null ? null : ResourceLocation.tryParse(argument);
        if (target == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(target))
            return Optional.empty();
        return Optional.of(new Entry(FEAR, BuiltInRegistries.ENTITY_TYPE.get(target)));
    }
}
