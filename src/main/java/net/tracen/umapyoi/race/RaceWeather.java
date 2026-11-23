package net.tracen.umapyoi.race;

import java.util.Locale;

/**
 * The weather of a race. It sets the ground condition, and what the sky shows to people in the race
 * world ({@link RaceSky}).
 */
public enum RaceWeather {
    SUNNY(RaceGround.FIRM, false, false), RAIN(RaceGround.YIELDING, true, false), THUNDER(RaceGround.HEAVY, true, true);

    private final RaceGround ground;
    private final boolean rain;
    private final boolean thunder;

    RaceWeather(RaceGround ground, boolean rain, boolean thunder) {
        this.ground = ground;
        this.rain = rain;
        this.thunder = thunder;
    }

    public RaceGround ground() {
        return ground;
    }

    public boolean raining() {
        return rain;
    }

    public boolean thundering() {
        return thunder;
    }

    public String key() {
        return "umapyoi.race.weather." + name().toLowerCase(Locale.ROOT);
    }
}
