package net.tracen.umapyoi.career;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Which training route a soul is on.
 * A soul without this component is treated as "already decided" (old saves,
 * command-given souls), so no data migration is needed.
 * "Registered" is not a state here: it is the existing "no UMADATA_TRAINING".
 */
public enum CareerState {
    /** Fresh from the Three Goddesses: cannot be worn until a route is chosen. */
    UNDECIDED,
    /** The player chose the existing automatic training. */
    AUTO,
    /** The three-year career is running. */
    IN_CAREER,
    /** The three years are over; the soul can be registered. */
    FINISHED;

    public static final Codec<CareerState> CODEC = Codec.STRING.xmap(
            name -> CareerState.valueOf(name.toUpperCase()), state -> state.name().toLowerCase());

    public static final StreamCodec<ByteBuf, CareerState> STREAM = ByteBufCodecs.VAR_INT
            .map(i -> CareerState.values()[i], CareerState::ordinal);
}
