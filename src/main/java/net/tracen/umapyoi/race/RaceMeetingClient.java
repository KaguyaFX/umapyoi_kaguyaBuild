package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;

/**
 * What this client was last told about the race being held. Plain data, so it can live outside the client
 * package; only the client ever fills it.
 */
public final class RaceMeetingClient {
    @Nullable
    private static RaceMeetingPayload current;
    private static long since;

    private RaceMeetingClient() {
    }

    static void set(RaceMeetingPayload payload) {
        current = payload.active() ? payload : null;
        since = Util.getMillis();
    }

    public static void reset() {
        current = null;
    }

    /** The race being held, or null when there's none. */
    @Nullable
    public static RaceMeetingPayload current() {
        return current;
    }

    /** Ticks left of what {@link RaceMeetingPayload#left()} counts, by now. */
    public static int left() {
        return current == null ? 0 : (int) Math.max(0, current.left() - (Util.getMillis() - since) / 50);
    }

    /** The same as minutes and seconds. */
    public static String clock() {
        return RaceMeeting.clock(left());
    }
}
