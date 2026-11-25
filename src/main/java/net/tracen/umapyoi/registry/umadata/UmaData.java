package net.tracen.umapyoi.registry.umadata;

import java.util.Arrays;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.GachaRanking;
import net.tracen.umapyoi.utils.Position;

public record UmaData(
		ResourceLocation identifier, 
		GachaRanking ranking, 
		int[] property, 
		int[] maxProperty,
        int[] propertyRate, 
        ResourceLocation uniqueSkill,
        Aptitude[] surfaceAptitude,
        Aptitude[] distanceAptitude,
        Position position,
        Aptitude[] styleAptitude
        ) {
    private static final int[] EMPTY_PROPERTY_RATE = new int[] { 0, 0, 0, 0, 0 };
    private static final int[] DEFAULT_MAX_PROPERTY = new int[] { 18, 18, 18, 18, 18 };
    private static final int[] DEFAULT_PROPERTY = new int[] { 1, 1, 1, 1, 1 };
    public static final Aptitude[] DEFAULT_SURFACE_APTITUDE = new Aptitude[]{Aptitude.C, Aptitude.C, Aptitude.B};
    public static final Aptitude[] DEFAULT_DISTANCE_APTITUDE = new Aptitude[]{Aptitude.C, Aptitude.C, Aptitude.C, Aptitude.C};
    private static final Position DEFAULT_POSITION = Position.FRONT_RUNNER;
    /** Not given: worked out from the position ({@link #styleAptitude(Position)}). */
    private static final Aptitude[] NO_STYLE_APTITUDE = new Aptitude[0];
    public static final ResourceLocation DEFAULT_UMA_ID = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "common_uma");
    public static final UmaData DEFAULT_UMA = UmaData.createNewUmamusume("common_uma", GachaRanking.R, new Aptitude[]{Aptitude.C, Aptitude.C, Aptitude.C, Aptitude.C, Aptitude.C, Aptitude.C}, Position.FRONT_RUNNER);

    public static final Codec<UmaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("identifier").forGetter(UmaData::identifier),
                    GachaRanking.CODEC.optionalFieldOf("ranking", GachaRanking.EASTER_EGG).forGetter(UmaData::ranking),
                    Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream).optionalFieldOf("property", DEFAULT_PROPERTY).forGetter(UmaData::property),
                    Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream).optionalFieldOf("maxProperty", DEFAULT_MAX_PROPERTY)
                            .forGetter(UmaData::maxProperty),
                    Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream).optionalFieldOf("propertyRate", EMPTY_PROPERTY_RATE)
                            .forGetter(UmaData::propertyRate),
                    ResourceLocation.CODEC.optionalFieldOf("uniqueSkill", UmaSkillRegistry.BASIC_PACE.getId()).forGetter(UmaData::uniqueSkill),
                    Aptitude.CODEC.listOf().xmap((lst) -> lst.toArray(Aptitude[]::new), Arrays::asList).optionalFieldOf("surface_aptitude", DEFAULT_SURFACE_APTITUDE).forGetter(UmaData::surfaceAptitude),
                    Aptitude.CODEC.listOf().xmap((lst) -> lst.toArray(Aptitude[]::new), Arrays::asList).optionalFieldOf("distance_aptitude", DEFAULT_DISTANCE_APTITUDE).forGetter(UmaData::distanceAptitude),
                    Position.CODEC.optionalFieldOf("position", DEFAULT_POSITION).forGetter(UmaData::position),
                    Aptitude.CODEC.listOf().xmap((lst) -> lst.toArray(Aptitude[]::new), Arrays::asList).optionalFieldOf("style_aptitude", NO_STYLE_APTITUDE).forGetter(UmaData::styleAptitude))
            .apply(instance, UmaData::new));

    public static final ResourceKey<Registry<UmaData>> REGISTRY_KEY = ResourceKey
            .createRegistryKey(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "umadata"));

    /** How well she runs in a style. Front runners (runaways included), pace chasers, late surgers and end closers, in that order. */
    public Aptitude styleAptitude(Position style) {
        int index = styleIndex(style);
        if (index < styleAptitude.length)
            return styleAptitude[index];
        return switch (Math.abs(index - styleIndex(position))) {
            case 0 -> Aptitude.A;
            case 1 -> Aptitude.C;
            default -> Aptitude.F;
        };
    }

    private static int styleIndex(Position style) {
        return switch (style) {
            case FRONT_RUNNER, RUNAWAY -> 0;
            case PACE_CHASER -> 1;
            case LATE_SURGER -> 2;
            case END_CLOSER -> 3;
        };
    }

    public static UmaData createNewUmamusume(String name, GachaRanking ranking,
                                             Aptitude[] aptitudes, Position pos) {
        return createNewUmamusume(name, ranking, EMPTY_PROPERTY_RATE, aptitudes, NO_STYLE_APTITUDE, pos);
    }

    /** @param styles front runner, pace chaser, late surger, end closer */
    public static UmaData createNewUmamusume(String name, GachaRanking ranking,
                                             Aptitude[] aptitudes, Aptitude[] styles, Position pos) {
        return createNewUmamusume(name, ranking, EMPTY_PROPERTY_RATE, aptitudes, styles, pos);
    }

    /**
     * @param aptitudes turf, dirt, then sprint, mile, medium, long
     * @param styles    front runner, pace chaser, late surger, end closer
     */
    public static UmaData createNewUmamusume(String name, GachaRanking ranking, int[] rate, Aptitude[] aptitudes, Aptitude[] styles, Position pos) {
        Aptitude[] surfaceAptitude = DEFAULT_SURFACE_APTITUDE.clone();
        System.arraycopy(aptitudes, 0, surfaceAptitude, 0, aptitudes.length - 4);
        Aptitude[] distanceAptitude = new Aptitude[4];
        System.arraycopy(aptitudes, aptitudes.length - 4, distanceAptitude, 0, 4);
        return new UmaData(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, name), ranking, DEFAULT_PROPERTY,
                DEFAULT_MAX_PROPERTY, rate, ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "basic_pace"), surfaceAptitude, distanceAptitude, pos, styles);
    }
}
