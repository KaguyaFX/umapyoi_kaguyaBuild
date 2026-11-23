package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

/**
 * The local player's race state as the server last sent it, plus the client's own
 * {@link RaceMotion} (started fresh each race). Plain data, so it can live outside the client
 * package; only the client ever fills it.
 */
public final class RaceClientState {
    @Nullable
    private static RaceStatePayload state;
    @Nullable
    private static RaceMotion motion;

    private RaceClientState() {
    }

    public static void set(RaceStatePayload payload) {
        if (!payload.active()) {
            reset();
            return;
        }
        if (state == null || motion == null)
            motion = new RaceMotion(payload.startSpeed());
        state = payload;
    }

    public static void reset() {
        state = null;
        motion = null;
    }

    public static boolean active() {
        return state != null;
    }

    /** The last state, or null when not racing. */
    @Nullable
    public static RaceStatePayload current() {
        return state;
    }

    @Nullable
    public static RaceMotion motion() {
        return motion;
    }

    public static float stamina() {
        return state == null ? 0 : state.stamina();
    }

    public static float maxStamina() {
        return state == null ? 0 : state.maxStamina();
    }
}
