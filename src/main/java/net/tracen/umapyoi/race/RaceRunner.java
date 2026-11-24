package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.tracen.umapyoi.UmapyoiConfig;

/**
 * One runner in a race: how far they are, how fast they go and what stamina they have left. Players
 * and computer runners follow the same rules; only who decides when to sprint differs, and that a
 * player moves themself (the client gets {@link #state}) while a computer runner goes where its speed
 * takes it.
 */
final class RaceRunner {
    /** Blocks per tick: anything faster isn't running. */
    static final double MAX_STEP = 4;

    private final RaceSetup setup;
    private final RaceStats stats;
    private final RaceLayout layout;
    /** The plain player's sprint, in blocks per second: converts accelerations to RaceMotion's unit. */
    private final double plainSprint;
    /** Null until the gates open. */
    @Nullable
    private RaceMotion motion;
    private double progress;
    private double stamina;
    private boolean exhausted;
    private int stillTicks;

    RaceRunner(RaceSetup setup, RaceStats stats, RaceLayout layout, double plainSprint) {
        this.setup = setup;
        this.stats = stats;
        this.layout = layout;
        this.plainSprint = plainSprint;
        this.stamina = stats.stamina();
    }

    /** The gates open: off from just behind (or on) the line. */
    void start(double progress) {
        this.motion = new RaceMotion(UmapyoiConfig.RACE_START_SPEED.get());
        this.progress = progress;
    }

    boolean started() {
        return motion != null;
    }

    RaceStats stats() {
        return stats;
    }

    RaceLayout layout() {
        return layout;
    }

    /** Race progress: blocks run along the course since the start. */
    double progress() {
        return progress;
    }

    void advance(double along) {
        progress += along;
    }

    RacePhase phase() {
        return layout.phase(progress);
    }

    boolean exhausted() {
        return exhausted;
    }

    double staminaFraction() {
        return stats.stamina() <= 0 ? 0 : stamina / stats.stamina();
    }

    /** Ground speed in blocks per second. */
    double speed() {
        return motion == null ? 0 : motion.speed() * plainSprint;
    }

    boolean accelerating(RaceStatePayload state, boolean sprinting) {
        return motion != null && motion.accelerating(state, sprinting);
    }

    boolean decelerating(RaceStatePayload state, boolean sprinting) {
        return motion != null && motion.decelerating(state, sprinting);
    }

    /** One tick of speeding up or slowing down. */
    void move(RaceStatePayload state, boolean sprinting) {
        if (motion != null)
            motion.step(state, sprinting, -1, 0);
    }

    /**
     * Full speed = the running style's coefficient for this phase x (1 + the Umamusume bonus left
     * after stamina, times the skill boost); cruise is a fraction of it. Exhausted, both drop to
     * about the same, slower speed.
     *
     * @param skill what speed skills multiply the Umamusume bonus by (1 = none)
     */
    RaceStatePayload state(double skill) {
        RacePhase phase = phase();
        double left = RaceStats.bonusLeft(staminaFraction(), stats.emptyFloor());
        double sprint = RaceStyle.speed(stats.style(), phase) * (1 + stats.bonusRatio() * left * skill);
        double cruise = sprint * UmapyoiConfig.RACE_CRUISE_RATIO.get();
        double accel = stats.accel() * RaceStyle.accel(stats.style(), phase) * setup.ground().accel();
        if (exhausted) {
            cruise *= UmapyoiConfig.RACE_EXHAUSTED_SPEED.get();
            sprint = cruise * UmapyoiConfig.RACE_EXHAUSTED_SPRINT.get();
            accel *= UmapyoiConfig.RACE_EXHAUSTED_ACCEL.get();
        }
        double perTick = 1 / (plainSprint * 20);
        return new RaceStatePayload(true, (float) stamina, (float) stats.stamina(), (float) sprint, (float) cruise,
                (float) (accel * perTick),
                (float) (UmapyoiConfig.RACE_DECEL.get() * perTick),
                (float) (UmapyoiConfig.RACE_START_DASH.get() * perTick),
                UmapyoiConfig.RACE_SPRINT_KICK.get().floatValue(),
                UmapyoiConfig.RACE_START_SPEED.get().floatValue(),
                exhausted);
    }

    /**
     * Speed above cruising costs stamina: one point per block at full speed, less while still
     * accelerating, and a little while slowing back down. Running wide costs more (more blocks), and
     * so does soft ground. Resting brings a little back.
     *
     * @param moved blocks moved on the ground this tick
     * @return true if this is the moment they ran out
     */
    boolean useStamina(RaceStatePayload state, double moved) {
        if (motion == null)
            return false;
        double span = state.sprintSpeed() - state.cruiseSpeed();
        double effort = span <= 0 ? 0 : Math.max(0, Math.min(1, (motion.speed() - state.cruiseSpeed()) / span));
        if (moved <= MAX_STEP)
            stamina -= moved * effort * setup.ground().drain();
        stillTicks = moved < 0.01 ? stillTicks + 1 : 0;
        if (stillTicks >= UmapyoiConfig.RACE_REST_SECONDS.get() * 20)
            stamina += stats.stamina() * UmapyoiConfig.RACE_STAMINA_REGEN_PERCENT.get() / 100 / 20;
        stamina = Math.max(0, Math.min(stats.stamina(), stamina));
        return updateExhaustion();
    }

    /** @return true if this is the moment they ran out */
    boolean jump() {
        if (motion == null)
            return false;
        stamina = Math.max(0, stamina - UmapyoiConfig.RACE_JUMP_STAMINA.get());
        return updateExhaustion();
    }

    private boolean updateExhaustion() {
        boolean before = exhausted;
        if (stamina <= 0)
            exhausted = true;
        else if (stamina >= stats.stamina() * UmapyoiConfig.RACE_EXHAUSTED_UNTIL.get())
            exhausted = false;
        return exhausted && !before;
    }
}
