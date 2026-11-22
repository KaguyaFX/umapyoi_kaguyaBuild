package net.tracen.umapyoi.career.event;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A career's events, stored on the soul so they pause with the career when it's taken off.
 * Turns are career periods counted from 0 (the first half of Junior June).
 *
 * @param table     what can still come up, with how many times each is left
 * @param scheduled events fixed to a turn (filled in a later step: New Year and the like)
 * @param active    events the player has right now
 */
public record CareerEventData(List<Entry> table, List<Scheduled> scheduled, List<Active> active) {

    public record Entry(String id, int left) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                Codec.INT.fieldOf("left").forGetter(Entry::left)).apply(i, Entry::new));
    }

    public record Scheduled(int turn, String id) {
        public static final Codec<Scheduled> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("turn").forGetter(Scheduled::turn),
                Codec.STRING.fieldOf("id").forGetter(Scheduled::id)).apply(i, Scheduled::new));
    }

    /** An event in progress: it came up at {@code start} and is settled at {@code deadline}. */
    public record Active(String id, int start, int deadline) {
        public static final Codec<Active> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Active::id),
                Codec.INT.fieldOf("start").forGetter(Active::start),
                Codec.INT.fieldOf("deadline").forGetter(Active::deadline)).apply(i, Active::new));
    }

    public static final CareerEventData EMPTY = new CareerEventData(List.of(), List.of(), List.of());

    public static final Codec<CareerEventData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("table", List.of()).forGetter(CareerEventData::table),
            Scheduled.CODEC.listOf().optionalFieldOf("scheduled", List.of()).forGetter(CareerEventData::scheduled),
            Active.CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(CareerEventData::active))
            .apply(i, CareerEventData::new));

    public static final StreamCodec<ByteBuf, CareerEventData> STREAM = ByteBufCodecs.fromCodec(CODEC);

    public CareerEventData {
        table = List.copyOf(table);
        scheduled = List.copyOf(scheduled);
        active = List.copyOf(active);
    }
}
