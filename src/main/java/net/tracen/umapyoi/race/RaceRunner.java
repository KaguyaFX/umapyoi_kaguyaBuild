package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.Position;

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
    /** How well they run in front, with the pace, from behind and from the very back. */
    private final Aptitude[] styles;
    /** Saved up for the last leg, 0 to 1, and how far that has been counted. */
    private double charge;
    private double chargedTo;
    /** The style they run in right now: where they are in the field. */
    private Position zone = Position.FRONT_RUNNER;
    /** Too far behind the one just in front to save anything up. */
    private boolean lostTouch;
    /** The full speed without what was saved up, as a fraction of the plain sprint. */
    private double baseSprint;
    /** Skill effects running for a few seconds. */
    private final RaceBoosts boosts = new RaceBoosts();
    /** From skills held for the whole race: full speed added (a fraction of the plain sprint), acceleration times. */
    private double sprintAdded;
    private double accelTimes = 1;
    /** What saving up for the last leg is multiplied by right now (a skill may speed it up while it holds). */
    private double chargeTimes = 1;
    /** Where they were in the field when last counted (1 = first), out of how many. */
    private int rank = 1;
    private int field = 1;
    /** Ticks in a row they have been held up by someone in front. */
    private int heldTicks;
    /** Sprinting (rather than cruising) on the last tick. */
    private boolean sprinting;

    RaceRunner(RaceSetup setup, RaceStats stats, RaceLayout layout, double plainSprint, Aptitude[] styles) {
        this.setup = setup;
        this.stats = stats;
        this.layout = layout;
        this.plainSprint = plainSprint;
        this.stamina = stats.stamina();
        this.styles = styles;
    }

    Aptitude[] styles() {
        return styles;
    }

    RaceBoosts boosts() {
        return boosts;
    }

    /** For the whole race: full speed plus this many blocks per second, acceleration times {@code accel}. */
    void passive(double blocksPerSecond, double accel) {
        sprintAdded += blocksPerSecond / plainSprint;
        accelTimes *= accel;
    }

    void chargeTimes(double times) {
        chargeTimes = times;
    }

    int rank() {
        return rank;
    }

    int field() {
        return field;
    }

    /** Whether they are being held up by someone in front this tick; counts how long that has lasted. */
    void held(boolean held) {
        heldTicks = held ? heldTicks + 1 : 0;
    }

    int heldTicks() {
        return heldTicks;
    }

    /** Takes this fraction of their full stamina at once. */
    void spend(double fraction) {
        stamina = Math.max(0, stamina - fraction * stats.stamina());
        updateExhaustion();
    }

    boolean sprinting() {
        return sprinting;
    }

    /** Gives back this fraction of their full stamina at once. */
    void restore(double fraction) {
        stamina = Math.min(stats.stamina(), stamina + fraction * stats.stamina());
        updateExhaustion();
    }

    double charge() {
        return charge;
    }

    boolean lostTouch() {
        return lostTouch;
    }

    Position zone() {
        return zone;
    }

    /** The aptitude that counts right now. */
    Aptitude zoneAptitude() {
        return styles[zone.ordinal()];
    }

    /** On to the last leg: what was saved up is used while sprinting. */
    boolean releasing() {
        RacePhase phase = phase();
        return charge > 0 && !exhausted && (phase == RacePhase.FINAL || phase == RacePhase.LAST_SPURT);
    }

    /**
     * Until the final leg, whoever isn't in front saves something up for it: the further back in the field the
     * more, all of it for the last one all the way, times their aptitude for running there. There's only so much
     * of it in a field, since somebody has to be in front. Not for someone who has lost touch.
     *
     * @param rank  1 = first
     * @param field how many are running
     * @param gap   blocks to the one just in front
     */
    void chargeUp(int rank, int field, double gap) {
        this.rank = rank;
        this.field = field;
        double done = Math.max(0, progress - chargedTo);
        chargedTo = Math.max(chargedTo, progress);
        zone = RaceStyle.zone(rank, field);
        RacePhase phase = phase();
        boolean over = phase == RacePhase.FINAL || phase == RacePhase.LAST_SPURT;
        lostTouch = !over && (gap > UmapyoiConfig.RACE_CHARGE_GAP.get() || boosts.touchLost());
        if (field < 2 || over || lostTouch || UmapyoiConfig.RACE_CHARGE_BLOCKS.get() <= 0)
            return;
        charge = Math.min(1, charge + done / layout.finalFrom() * (rank - 1) / (field - 1) * zoneAptitude().distanceFactor * chargeTimes * boosts.chargeFactor());
    }

    /**
     * Until the final leg, whoever runs in front uses less stamina: what saving up is for the others. All of
     * it for the first, half for the one behind (two neck and neck don't swap all or nothing), times their
     * aptitude for running in front. Not alone (a time trial, or a computer runner working out its plan), and
     * not for a runaway: going off in front at once is her way anyway, and she'd only run away with it.
     */
    double frontSave() {
        RacePhase phase = phase();
        if (field < 2 || zone != Position.FRONT_RUNNER || stats.style() == Position.RUNAWAY || phase == RacePhase.FINAL
                || phase == RacePhase.LAST_SPURT)
            return 0;
        double share = rank == 1 ? 1 : 0.5;
        return Math.min(1, UmapyoiConfig.RACE_FRONT_STAMINA_SAVE.get() * share * zoneAptitude().distanceFactor);
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

    RaceSetup setup() {
        return setup;
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
        this.sprinting = sprinting;
        if (motion != null)
            motion.step(state, sprinting, -1, 0);
    }

    /**
     * Full speed = the running style's coefficient for this phase x (1 + the Umamusume bonus left
     * after stamina, times what speed or slowness skills make of it), plus what skills held all race
     * add; cruise is a fraction of it. Exhausted, both drop to about the same, slower speed.
     *
     * @param limit the most the runner in front allows, in blocks per second ({@link RaceTraffic#limit})
     */
    RaceStatePayload state(double limit) {
        RacePhase phase = phase();
        double left = RaceStats.bonusLeft(staminaFraction(), stats.emptyFloor());
        double sprint = RaceStyle.speed(stats.style(), phase) * (1 + stats.bonusRatio() * left * boosts.bonusFactor()) + sprintAdded
                + boosts.flatSpeed() / plainSprint;
        double cruise = sprint * UmapyoiConfig.RACE_CRUISE_RATIO.get();
        double accel = stats.accel() * RaceStyle.accel(stats.style(), phase) * setup.ground().accel() * accelTimes * boosts.accelFactor();
        if (exhausted) {
            cruise *= UmapyoiConfig.RACE_EXHAUSTED_SPEED.get();
            sprint = cruise * UmapyoiConfig.RACE_EXHAUSTED_SPRINT.get();
            accel *= UmapyoiConfig.RACE_EXHAUSTED_ACCEL.get();
        }
        baseSprint = sprint;
        if (releasing())
            sprint = sprint * (1 + UmapyoiConfig.RACE_CHARGE_SPEED.get()) + boosts.releaseSpeed() / plainSprint;
        double perTick = 1 / (plainSprint * 20);
        return new RaceStatePayload(true, (float) stamina, (float) stats.stamina(), (float) sprint, (float) cruise,
                (float) (accel * perTick),
                (float) (UmapyoiConfig.RACE_DECEL.get() * perTick),
                (float) (stats.startDash() * perTick),
                UmapyoiConfig.RACE_SPRINT_KICK.get().floatValue(),
                UmapyoiConfig.RACE_START_SPEED.get().floatValue(),
                exhausted,
                (float) (limit / plainSprint),
                (float) (UmapyoiConfig.RACE_BLOCK_DECEL.get() * perTick),
                (float) charge, releasing(), lostTouch);
    }

    /** How fast they would like to go, in blocks per second, if nobody were in the way. */
    double wanted(RaceStatePayload state, boolean sprinting) {
        return (sprinting ? state.sprintSpeed() : state.cruiseSpeed()) * plainSprint;
    }

    /**
     * Speed above cruising costs stamina: one point per block at full speed, less while still
     * accelerating, and a little while slowing back down. Running wide costs more (more blocks), and
     * so does soft ground. Resting brings a little back. What goes beyond full speed comes out of what was
     * saved up instead.
     *
     * @param moved blocks moved on the ground this tick
     * @return true if this is the moment they ran out
     */
    boolean useStamina(RaceStatePayload state, double moved) {
        if (motion == null)
            return false;
        double span = baseSprint - state.cruiseSpeed();
        double effort = span <= 0 ? 0 : Math.max(0, Math.min(1, (motion.speed() - state.cruiseSpeed()) / span));
        if (moved <= MAX_STEP && !boosts.free())
            stamina -= moved * effort * setup.ground().drain() * (1 - frontSave()) * boosts.drainFactor();
        if (releasing() && motion.speed() > baseSprint)
            charge = Math.max(0, charge - Math.min(moved, (motion.speed() - baseSprint) * plainSprint / 20) / UmapyoiConfig.RACE_CHARGE_BLOCKS.get());
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
