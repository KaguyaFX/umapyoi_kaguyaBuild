package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tracen.umapyoi.UmapyoiConfig;

/**
 * The local player's race state as the server last sent it, plus the client's own
 * {@link RaceMotion} (started fresh each race), where the player was a tick ago, and how long
 * they've been letting go of the movement keys. Plain data, so it can live outside the client
 * package; only the client ever fills it.
 */
public final class RaceClientState {
    /**
     * How much forward input the coast injects: below 0.8, the amount vanilla needs to start
     * sprinting, so rolling along never wakes a sprint up.
     */
    public static final float COAST_INPUT = 0.79F;

    @Nullable
    private static RaceStatePayload state;
    @Nullable
    private static RaceMotion motion;
    private static double lastX = Double.NaN;
    private static double lastZ;
    /** Whether the player has pressed a movement key since the gates opened. */
    private static boolean movedOnce;
    /** Ticks in a row without any movement input. */
    private static int idleTicks;
    /** The way the player was going when they last pressed something (a unit vector). */
    private static double dirX;
    private static double dirZ;

    private RaceClientState() {
    }

    public static void set(RaceStatePayload payload) {
        if (!payload.active()) {
            reset();
            return;
        }
        if (state == null || motion == null) {
            motion = new RaceMotion(payload.startSpeed());
            resetCoast();
            lastX = Double.NaN;
        }
        state = payload;
    }

    public static void reset() {
        state = null;
        motion = null;
        lastX = Double.NaN;
        resetCoast();
    }

    private static void resetCoast() {
        movedOnce = false;
        idleTicks = 0;
        dirX = 0;
        dirZ = 0;
    }

    /** A movement key is down: remember which way the player is going. */
    public static void moving(Player player) {
        movedOnce = true;
        idleTicks = 0;
        Vec3 velocity = player.getDeltaMovement();
        double length = Math.hypot(velocity.x, velocity.z);
        if (length > 0.02) {
            dirX = velocity.x / length;
            dirZ = velocity.z / length;
        }
    }

    /** No movement key down this tick. */
    public static void idleTick() {
        idleTicks++;
    }

    /** Nothing pressed, after having moved since the start. */
    public static boolean idle() {
        return movedOnce && idleTicks > 0;
    }

    /** Still rolling: nothing pressed, but not for longer than race_coast_seconds. */
    public static boolean coasting() {
        return idle() && idleTicks <= (int) Math.round(UmapyoiConfig.RACE_COAST_SECONDS.get() * 20)
                && (dirX != 0 || dirZ != 0);
    }

    public static double dirX() {
        return dirX;
    }

    public static double dirZ() {
        return dirZ;
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
