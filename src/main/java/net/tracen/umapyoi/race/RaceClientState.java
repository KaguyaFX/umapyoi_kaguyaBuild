package net.tracen.umapyoi.race;

/**
 * The local player's race state as the server last sent it: whether they're running, their
 * stamina, and the speed bonus left after stamina. Plain data, so it can live outside the client
 * package; only the client ever fills it.
 */
public final class RaceClientState {
    private static boolean active;
    private static float stamina;
    private static float maxStamina;
    private static float bonusRatio;
    private static float cruiseRatio = 1;

    private RaceClientState() {
    }

    public static void set(RaceStatePayload payload) {
        active = payload.active();
        stamina = payload.stamina();
        maxStamina = payload.maxStamina();
        bonusRatio = payload.bonusRatio();
        cruiseRatio = payload.cruiseRatio();
    }

    public static void reset() {
        active = false;
    }

    /** The last state as a payload, or null when not racing. */
    public static RaceStatePayload current() {
        return active ? new RaceStatePayload(true, stamina, maxStamina, bonusRatio, cruiseRatio) : null;
    }

    public static boolean active() {
        return active;
    }

    public static float stamina() {
        return stamina;
    }

    public static float maxStamina() {
        return maxStamina;
    }

    public static float bonusRatio() {
        return bonusRatio;
    }

    public static float cruiseRatio() {
        return cruiseRatio;
    }
}
