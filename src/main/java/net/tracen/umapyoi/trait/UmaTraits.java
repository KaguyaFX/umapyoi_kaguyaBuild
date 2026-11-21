package net.tracen.umapyoi.trait;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;

/**
 * Traits stored on a soul. Every field may be empty (souls from commands, the creative menu, old
 * saves, or made before this update). Unknown or repeated ids are skipped when read, never fatal.
 *
 * @param uma      Uma traits, e.g. "claustrophobia", "fear:minecraft:skeleton"
 * @param negative negative personality traits (normally one, rolled by the Three Goddesses)
 * @param positive positive personality traits (normally one, chosen in the self-training terminal)
 * @param likes    favourite foods (item ids)
 * @param dislikes disliked foods (item ids)
 */
public record UmaTraits(List<String> uma, List<String> negative, List<String> positive, List<ResourceLocation> likes,
        List<ResourceLocation> dislikes) {

    public static final UmaTraits EMPTY = new UmaTraits(List.of(), List.of(), List.of(), List.of(), List.of());

    public static final Codec<UmaTraits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("uma", List.of()).forGetter(UmaTraits::uma),
            Codec.STRING.listOf().optionalFieldOf("negative", List.of()).forGetter(UmaTraits::negative),
            Codec.STRING.listOf().optionalFieldOf("positive", List.of()).forGetter(UmaTraits::positive),
            ResourceLocation.CODEC.listOf().optionalFieldOf("likes", List.of()).forGetter(UmaTraits::likes),
            ResourceLocation.CODEC.listOf().optionalFieldOf("dislikes", List.of()).forGetter(UmaTraits::dislikes))
            .apply(instance, UmaTraits::new));

    public static final StreamCodec<ByteBuf, UmaTraits> STREAM = ByteBufCodecs.fromCodec(CODEC);

    public UmaTraits {
        uma = List.copyOf(uma);
        negative = List.copyOf(negative);
        positive = List.copyOf(positive);
        likes = List.copyOf(likes);
        dislikes = List.copyOf(dislikes);
    }

    public static UmaTraits of(ItemStack soul) {
        return soul.getOrDefault(DataComponentsTypeRegistry.UMA_TRAITS, EMPTY);
    }

    public UmaTraits withPositive(PersonalityTrait trait) {
        return new UmaTraits(uma, negative, List.of(trait.id()), likes, dislikes);
    }

    // ---------------------------------------------------------------- tolerant readers

    private static final Set<String> warned = new java.util.HashSet<>();

    private static void warnOnce(String what) {
        if (warned.add(what))
            Umapyoi.getLogger().warn("Ignoring unknown trait or food on a soul: {}", what);
    }

    public List<UmaTrait.Entry> umaEntries() {
        List<UmaTrait.Entry> list = new ArrayList<>();
        for (String id : new LinkedHashSet<>(uma)) {
            UmaTrait.parse(id).ifPresentOrElse(list::add, () -> warnOnce(id));
        }
        return list;
    }

    public List<PersonalityTrait> negatives() {
        return personality(negative);
    }

    public List<PersonalityTrait> positives() {
        return personality(positive);
    }

    private static List<PersonalityTrait> personality(List<String> ids) {
        List<PersonalityTrait> list = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            PersonalityTrait.byId(id).ifPresentOrElse(list::add, () -> warnOnce(id));
        }
        return list;
    }

    public List<Item> likedItems() {
        return items(likes);
    }

    public List<Item> dislikedItems() {
        return items(dislikes);
    }

    private static List<Item> items(List<ResourceLocation> ids) {
        List<Item> list = new ArrayList<>();
        for (ResourceLocation id : new LinkedHashSet<>(ids)) {
            if (BuiltInRegistries.ITEM.containsKey(id))
                list.add(BuiltInRegistries.ITEM.get(id));
            else
                warnOnce(id.toString());
        }
        return list;
    }

    /** True when a positive trait is excluded by one of the negative ones (玻璃心/開朗, 懶散/踏實). */
    public boolean isExcluded(PersonalityTrait positiveTrait) {
        return negatives().stream().anyMatch(n -> n.excludes() == positiveTrait);
    }
}
