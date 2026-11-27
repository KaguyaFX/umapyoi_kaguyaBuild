package net.tracen.umapyoi.registry.races;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/**
 * How hard a race meeting's computer runners are, beyond what its grade gives.
 * <p>
 * In the race's data, {@code "difficulty": 2} means two grades stronger than usual for its grade (the
 * famous races). A Major Match (URA Finals, the zodiac cups) is set by an object instead:
 * <pre>
 * "difficulty": { "major": { "floor": 25, "above": [2, 3], "rise": [2, 6], "moods": "no_bad", "announce": true } }
 * </pre>
 * Its field is built around the stats of the player who holds it, {@code above} grades higher (drawn
 * once per race); every computer runner then has each stat at least {@code floor} and {@code rise}
 * grades more on top (drawn per stat), never past 39 (MAX). Major Matches are only run for real, and
 * only a win in one's own meeting counts as having passed it.
 *
 * @param strength grades added to the usual ones for the race's grade (not used by a Major Match)
 */
public record RaceDifficulty(int strength, Optional<Major> major) {
    public static final RaceDifficulty NONE = new RaceDifficulty(0, Optional.empty());

    /** What the computer runners' moods may be. */
    public enum Moods implements StringRepresentable {
        /** As usual. */
        USUAL,
        /** All of them at their best. */
        PERFECT,
        /** As usual, but never at their worst. */
        NO_BAD;

        public static final Codec<Moods> CODEC = StringRepresentable.fromEnum(Moods::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    /** A range of whole grades, both ends included. */
    public record Range(int min, int max) {
        public static final Range ZERO = new Range(0, 0);
        public static final Codec<Range> CODEC = Codec.INT.listOf().comapFlatMap(
                list -> list.size() == 2 && list.get(0) <= list.get(1) ? DataResult.success(new Range(list.get(0), list.get(1)))
                        : DataResult.error(() -> "a range is two numbers, the lower first: " + list),
                range -> List.of(range.min, range.max));

        public int draw(RandomSource random) {
            return min + random.nextInt(max - min + 1);
        }
    }

    public record Major(int floor, Range above, Range rise, Moods moods, boolean announce) {
        public static final Codec<Major> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 39).optionalFieldOf("floor", 1).forGetter(Major::floor),
                Range.CODEC.optionalFieldOf("above", Range.ZERO).forGetter(Major::above),
                Range.CODEC.optionalFieldOf("rise", Range.ZERO).forGetter(Major::rise),
                Moods.CODEC.optionalFieldOf("moods", Moods.USUAL).forGetter(Major::moods),
                Codec.BOOL.optionalFieldOf("announce", false).forGetter(Major::announce)
        ).apply(instance, Major::new));
    }

    private static final Codec<RaceDifficulty> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("strength", 0).forGetter(RaceDifficulty::strength),
            Major.CODEC.optionalFieldOf("major").forGetter(RaceDifficulty::major)
    ).apply(instance, RaceDifficulty::new));

    /** A plain number for the usual case, an object for a Major Match. */
    public static final Codec<RaceDifficulty> CODEC = Codec.either(Codec.INT, FULL).xmap(
            either -> either.map(strength -> new RaceDifficulty(strength, Optional.empty()), full -> full),
            difficulty -> difficulty.major.isEmpty() ? Either.left(difficulty.strength) : Either.right(difficulty));

    public static RaceDifficulty major(int floor, Range above, Range rise, Moods moods, boolean announce) {
        return new RaceDifficulty(0, Optional.of(new Major(floor, above, rise, moods, announce)));
    }

    public boolean isMajor() {
        return major.isPresent();
    }
}
