package net.tracen.umapyoi.trait;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;

/**
 * Rolls what the Three Goddesses give a new soul: 2 Uma traits, 1 negative personality trait,
 * 2 liked foods and 1 disliked food. The positive personality trait is not rolled; the player
 * picks it in the self-training terminal.
 */
public final class TraitRoller {
    public static final int UMA_TRAITS = 2;
    public static final int NEGATIVE_TRAITS = 1;
    public static final int LIKES = 2;
    public static final int DISLIKES = 1;

    private TraitRoller() {
    }

    public static UmaTraits roll(RandomSource random) {
        List<String> uma = new ArrayList<>();
        for (UmaTrait trait : pick(List.of(UmaTrait.values()), UMA_TRAITS, random))
            uma.add(trait == UmaTrait.FEAR ? fearId(random) : trait.id());

        List<String> negative = pick(PersonalityTrait.negatives(), NEGATIVE_TRAITS, random).stream()
                .map(PersonalityTrait::id).toList();

        // Likes and dislikes come from one shuffled pick, so a food is never both.
        List<ResourceLocation> foods = pick(foodCandidates(), LIKES + DISLIKES, random);
        int likeCount = Math.min(LIKES, foods.size());
        List<ResourceLocation> likes = foods.subList(0, likeCount);
        List<ResourceLocation> dislikes = foods.subList(likeCount, foods.size());

        return new UmaTraits(uma, negative, List.of(), likes, dislikes);
    }

    private static String fearId(RandomSource random) {
        EntityType<?> target = UmaTrait.FEAR_TARGETS.get(random.nextInt(UmaTrait.FEAR_TARGETS.size()));
        return UmaTrait.FEAR.id() + ":" + BuiltInRegistries.ENTITY_TYPE.getKey(target);
    }

    /** Foods in the {@code umapyoi:favorite_food_candidates} tag. Empty if a pack emptied the tag. */
    private static List<ResourceLocation> foodCandidates() {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        BuiltInRegistries.ITEM.getTag(UmapyoiItemTags.FAVORITE_FOOD_CANDIDATES).ifPresent(set -> {
            for (Holder<Item> holder : set)
                ids.add(BuiltInRegistries.ITEM.getKey(holder.value()));
        });
        return new ArrayList<>(ids);
    }

    /** Up to {@code count} distinct entries in random order. */
    private static <T> List<T> pick(List<T> from, int count, RandomSource random) {
        List<T> pool = new ArrayList<>(from);
        List<T> picked = new ArrayList<>();
        while (picked.size() < count && !pool.isEmpty())
            picked.add(pool.remove(random.nextInt(pool.size())));
        return picked;
    }
}
