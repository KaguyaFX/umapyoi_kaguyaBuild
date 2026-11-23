package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

/**
 * The local player's race state as the server last sent it, plus the client's own
 * {@link RaceMotion} (started fresh each race) and where the player was a tick ago. Plain data, so
 * it can live outside the client package; only the client ever fills it.
 */
public final class RaceClientState {
    @Nullable
    private static RaceStatePayload state;
    @Nullable
    private static RaceMotion motion;
    private static double lastX = Double.NaN;
    private static double lastZ;

    private RaceClientState() {
    }

    public static void set(RaceStatePayload payload) {
        if (!payload.active()) {
            reset();
            return;
        }
        if (state == null || motion == null) {
            motion = new RaceMotion(payload.startSpeed());
            lastX = Double.NaN;
        }
        state = payload;
    }

    public static void reset() {
        state = null;
        motion = null;
        lastX = Double.NaN;
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

    /**
     * How far the player moved since this was last called (blocks), or -1 the first time. Call it
     * once per tick, before the player moves: the game resets its own "previous position" to the
     * current one right before each entity's tick, so that can't be used to measure anything there.
     */
    static double movedSinceLastTick(Player player) {
        double moved = Double.isNaN(lastX) ? -1 : Math.hypot(player.getX() - lastX, player.getZ() - lastZ);
        lastX = player.getX();
        lastZ = player.getZ();
        return moved;
    }

    public static float stamina() {
        return state == null ? 0 : state.stamina();
    }

    public static float maxStamina() {
        return state == null ? 0 : state.maxStamina();
    }
}
