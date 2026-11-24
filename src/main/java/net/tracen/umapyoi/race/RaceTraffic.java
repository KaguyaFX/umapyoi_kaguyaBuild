package net.tracen.umapyoi.race;

import java.util.List;

import net.minecraft.util.Mth;
import net.tracen.umapyoi.UmapyoiConfig;

/**
 * What runners do to each other. Nobody bumps into anybody: whoever is right behind someone simply can't
 * run any faster than them, and has to go round. It holds for players and computer runners alike, and
 * both can block both.
 * <p>
 * The shape of the rule is the original game's (a wedge behind the runner in front, with a speed a shade
 * under theirs), scaled up to bodies of Minecraft size; and the closer, the stricter, so a fast runner
 * closes up and settles in instead of braking at the edge of it.
 */
final class RaceTraffic {
    /** No limit, in blocks per second. */
    static final double FREE = 1000;
    /** How far to the side someone still blocks: right behind them, and at the far end of the wedge. */
    private static final double WIDTH_NEAR = 0.6;
    private static final double WIDTH_FAR = 0.35;
    /** Nobody comes closer than this, in blocks from one's middle to the other's. */
    private static final double TOUCHING = 1.5;
    /** How much faster than the runner in front they may still be at the far end of the wedge, in blocks per second. */
    private static final double CLOSING = 1.5;
    /** Held to this share of their speed when touching, and to less when closer still. */
    private static final double BEHIND = 0.988;
    private static final double TOO_CLOSE = 0.1;

    /**
     * Someone running.
     *
     * @param who      to tell them apart
     * @param progress race progress, in blocks
     * @param offset   how far off the course's reference line
     * @param speed    blocks per second
     */
    record Body(Object who, double progress, double offset, double speed) {
    }

    private RaceTraffic() {
    }

    /** How fast the runner in front lets {@code self} run, in blocks per second; {@link #FREE} if nobody is there. */
    static double limit(Body self, List<Body> all) {
        double length = UmapyoiConfig.RACE_BLOCK_LENGTH.get();
        double closest = length;
        double limit = FREE;
        for (Body other : all) {
            double gap = other.progress() - self.progress();
            if (other.who() == self.who() || gap <= 0 || gap >= closest)
                continue;
            if (Math.abs(other.offset() - self.offset()) > Mth.lerp(gap / length, WIDTH_NEAR, WIDTH_FAR))
                continue;
            closest = gap;
            double room = Mth.clamp((gap - TOUCHING) / Math.max(0.1, length - TOUCHING), 0, 1);
            double close = Mth.clamp((TOUCHING - gap) / TOUCHING, 0, 1);
            limit = other.speed() * (Mth.lerp(gap / length, BEHIND, 1) - TOO_CLOSE * close) + CLOSING * room;
        }
        return limit;
    }
}
