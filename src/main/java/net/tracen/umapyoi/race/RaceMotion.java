package net.tracen.umapyoi.race;

/**
 * A runner's current race speed, climbing toward the target instead of jumping to it. Speeds are
 * fractions of the plain player's sprint (1 = a vanilla sprint), per tick.
 * <ul>
 * <li>The target is the full sprint while sprinting, the cruise otherwise.</li>
 * <li>Below the target it accelerates (power); out of the gates there's an extra start dash until
 * cruising speed is first reached.</li>
 * <li>Above the target it slows at a fixed rate.</li>
 * <li>Starting to sprint while already about at cruising speed puts the speed at once a little way
 * above the cruise (a floor, so tapping sprint again and again doesn't stack). Not from a
 * standstill: there the start dash and the power acceleration do the work.</li>
 * </ul>
 * The server and the client each keep one and step it with the same numbers, so pressing sprint
 * responds at once on the client; the server's copy decides stamina.
 */
public final class RaceMotion {
    /** Sprinting has to start from at least this much of the cruising speed for the instant bump. */
    private static final double KICK_FROM = 0.9;

    private double speed;
    private boolean wasSprinting;
    private boolean dashing = true;

    public RaceMotion(double startSpeed) {
        this.speed = startSpeed;
    }

    public double speed() {
        return speed;
    }

    /** True while still below the target. */
    public boolean accelerating(RaceStatePayload state, boolean sprinting) {
        return speed < target(state, sprinting) - 1.0E-4;
    }

    public boolean decelerating(RaceStatePayload state, boolean sprinting) {
        return speed > target(state, sprinting) + 1.0E-4;
    }

    private static double target(RaceStatePayload state, boolean sprinting) {
        return sprinting ? state.sprintSpeed() : state.cruiseSpeed();
    }

    /** One tick of slowing down without any target: rolling to a stop, or braking. */
    public void slow(double decelPerTick) {
        speed = Math.max(0, speed - decelPerTick);
        // Pressing sprint again counts as a new press.
        wasSprinting = false;
    }

    /**
     * One tick.
     *
     * @param actual how fast the runner really moved last tick, or a negative number if unknown; the speed
     *               can't run ahead of it by more than {@code margin}, so after a wall or a stop you
     *               accelerate again
     */
    public void step(RaceStatePayload state, boolean sprinting, double actual, double margin) {
        double cruise = state.cruiseSpeed();
        double sprint = state.sprintSpeed();
        if (sprinting && !wasSprinting && sprint > cruise && speed >= cruise * KICK_FROM)
            speed = Math.max(speed, cruise + state.kick() * (sprint - cruise));
        wasSprinting = sprinting;

        double target = sprinting ? sprint : cruise;
        if (speed < target) {
            double accel = state.accel() + (dashing ? state.dash() : 0);
            speed = Math.min(target, speed + accel);
        } else {
            speed = Math.max(target, speed - state.decel());
        }
        if (dashing && speed >= cruise)
            dashing = false;
        if (actual >= 0)
            speed = Math.min(speed, Math.max(actual + margin, state.startSpeed()));
    }
}
