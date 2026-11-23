package net.tracen.umapyoi.race;

import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.Surface;

/**
 * What a race is: how far, on what, which way round, and the weather. The distance category (and
 * with it the distance aptitude) is worked out from the distance.
 *
 * @param leftHanded the course is run the other way round (mirrored)
 */
public record RaceSetup(double distanceMeters, Surface surface, boolean leftHanded, RaceWeather weather) {
    public static final RaceSetup DEFAULT = new RaceSetup(2000, Surface.TURF, false, RaceWeather.SUNNY);

    public Distance category() {
        return Distance.match((int) Math.round(distanceMeters));
    }

    public RaceGround ground() {
        return weather.ground();
    }
}
