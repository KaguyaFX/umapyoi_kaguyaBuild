package net.tracen.umapyoi.career;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;

/**
 * Server-side, per-player cache of the worn soul's career.
 * Training points go here every tick and are written back to the soul every second, when an
 * integer stat changes, at each career period, on death, at the end and on logout.
 * Not saved: taking the soul off loses at most the last second.
 */
public class CareerCache {
    /** Career of the soul this cache belongs to; null = no career cached. */
    UUID careerId;
    long elapsed;
    /**
     * The elapsed time this cache last read from or wrote to the soul. A worn soul with the same
     * career id but another elapsed time is a different stack (a copy, or edited by a command),
     * so the cache reloads instead of writing its own progress over it.
     */
    long writtenElapsed;
    long total;
    int period;
    /** Precise stats in millionths. */
    final long[] precise = new long[5];
    /** Integer stats this cache believes are on the soul; used to merge outside changes. */
    final int[] baseline = new int[5];
    final int[] max = new int[5];
    final int[] cardMultiplier = new int[5];
    /** player.tickCount of the last career tick; a gap means the soul was taken off. */
    int lastActiveTick = Integer.MIN_VALUE;

    boolean hasLastPos;
    double lastX;
    double lastZ;
    int lastXpLevel = -1;

    boolean isActive(Player player) {
        return careerId != null && lastActiveTick >= player.tickCount - 1;
    }

    void clear() {
        careerId = null;
        hasLastPos = false;
        lastXpLevel = -1;
        lastActiveTick = Integer.MIN_VALUE;
    }
}
