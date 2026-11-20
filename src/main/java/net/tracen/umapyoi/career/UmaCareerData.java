package net.tracen.umapyoi.career;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Career progress stored on the soul.
 * All five-stat lists are in the order speed, stamina, strength, guts, wisdom.
 *
 * @param careerId       unique per career; lets the player-side cache tell souls apart
 * @param elapsedTicks   career time already passed (only advances while worn)
 * @param totalTicks     career length, fixed when the career starts
 * @param precise        precise stats in millionths (12.6 = 12_600_000)
 * @param lastWritten    the integer stats written to the soul last time, to merge outside changes
 * @param worstGrowth    worst-case auto-training growth, for the base reward at the end
 * @param cardMultiplier support card growth multiplier per stat, per mille (1000 = x1.0)
 * @param deaths         deaths during the career
 */
public record UmaCareerData(UUID careerId, long elapsedTicks, long totalTicks, List<Integer> precise,
        List<Integer> lastWritten, List<Integer> worstGrowth, List<Integer> cardMultiplier, int deaths) {

    public static final Codec<UmaCareerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("career_id").forGetter(UmaCareerData::careerId),
            Codec.LONG.fieldOf("elapsed_ticks").forGetter(UmaCareerData::elapsedTicks),
            Codec.LONG.fieldOf("total_ticks").forGetter(UmaCareerData::totalTicks),
            Codec.INT.listOf().fieldOf("precise").forGetter(UmaCareerData::precise),
            Codec.INT.listOf().fieldOf("last_written").forGetter(UmaCareerData::lastWritten),
            Codec.INT.listOf().fieldOf("worst_growth").forGetter(UmaCareerData::worstGrowth),
            Codec.INT.listOf().fieldOf("card_multiplier").forGetter(UmaCareerData::cardMultiplier),
            Codec.INT.fieldOf("deaths").forGetter(UmaCareerData::deaths))
            .apply(instance, UmaCareerData::new));

    public static final StreamCodec<ByteBuf, UmaCareerData> STREAM = ByteBufCodecs.fromCodec(CODEC);

    public UmaCareerData {
        precise = List.copyOf(precise);
        lastWritten = List.copyOf(lastWritten);
        worstGrowth = List.copyOf(worstGrowth);
        cardMultiplier = List.copyOf(cardMultiplier);
    }

    public UmaCareerData withProgress(long elapsed, int[] precise, int[] lastWritten) {
        return new UmaCareerData(careerId, elapsed, totalTicks, toList(precise), toList(lastWritten), worstGrowth,
                cardMultiplier, deaths);
    }

    public UmaCareerData withDeaths(int deaths) {
        return new UmaCareerData(careerId, elapsedTicks, totalTicks, precise, lastWritten, worstGrowth,
                cardMultiplier, deaths);
    }

    /** Career period index on the race calendar: 11 (Junior, early June) to 72 (Senior, late December). */
    public int periodIndex() {
        long elapsed = Math.min(elapsedTicks, totalTicks);
        int period = totalTicks <= 0 ? 0 : (int) (elapsed * CareerManager.PERIODS / totalTicks);
        return CareerManager.FIRST_PERIOD + Math.min(period, CareerManager.PERIODS - 1);
    }

    public static List<Integer> toList(int[] values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }

    public static int[] toArray(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
}
