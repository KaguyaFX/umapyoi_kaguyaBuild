package net.tracen.umapyoi.career;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;

/**
 * Server-side, per-player cache of the worn soul's career.
 * Training points go here every tick and are only written back to the soul when an
 * integer stat changes, at each career period, on death, at the end and on logout.
 * Not saved: anything not yet written back is at most one period of progress.
 */
public class CareerCache {
    /** Career of the soul this cache belongs to; null = no career cached. */
    UUID careerId;
    long elapsed;
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
