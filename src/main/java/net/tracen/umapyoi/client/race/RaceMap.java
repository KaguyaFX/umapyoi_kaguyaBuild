package net.tracen.umapyoi.client.race;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.RaceLivePayload.Spot;
import net.tracen.umapyoi.race.RaceLivePayload;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.track.TrackGeometry;
import net.tracen.umapyoi.utils.Surface;

/**
 * The course seen from above, standing upright: the home straight on the right. The picture is made once for
 * each race and size, the runners are drawn on top.
 */
public final class RaceMap {
    /** Picture pixels per GUI pixel. */
    private static final int DETAIL = 4;
    private static final int KEEP = 4;
    private static final double RAIL = 1.6;
    public static final int GROUND = 0xFFCFE3B0;
    private static final int RAILS = 0xFFD9C9A0, WATER = 0xFF6AA6DC, SHORE = 0xFFE9DFAE;
    private static final int FINISH = 0xFFD8362E, START = 0xFF2F66C8, FINAL = 0xFFF2B81E;
    /** Where the runners are across the course is shown this much wider than it is, and this far out. */
    private static final double SPREAD = 1.8;

    private record Key(RaceMeetingPayload.Course course, boolean leftHanded, Surface surface, int width, int height) {
    }

    private static final Map<Key, ResourceLocation> PICTURES = new LinkedHashMap<>();
    private static int made;

    private final TrackGeometry track;
    private final RaceMeetingPayload held;
    private final int x, y, width, height;
    private final double scale;

    private RaceMap(TrackGeometry track, RaceMeetingPayload held, int x, int y, int width, int height) {
        this.track = track;
        this.held = held;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale(track, width, height);
    }

    private static double scale(TrackGeometry track, int width, int height) {
        return Math.min((height - 4) / (2 * track.halfLengthX(RAIL)), (width - 4) / (2 * track.halfLengthZ(RAIL)));
    }

    /** The map of the race being held, in this part of the screen; null while its course isn't laid out. */
    @Nullable
    public static RaceMap of(RaceMeetingPayload held, int x, int y, int width, int height) {
        TrackGeometry track = held.track();
        return track == null ? null : new RaceMap(track, held, x, y, width, height);
    }

    // ---- where things are

    /** On the screen. */
    public double screenX(double progress, double offset) {
        return x + width / 2.0 + track.pointAt(track.wrap(progress), offset).z() * scale;
    }

    public double screenY(double progress, double offset) {
        return y + height / 2.0 - track.pointAt(track.wrap(progress), offset).x() * scale;
    }

    public TrackGeometry track() {
        return track;
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }

    // ---- drawing

    public void render(GuiGraphics graphics) {
        Key key = new Key(held.course(), held.leftHanded(), held.surfaceNow(), width, height);
        ResourceLocation picture = PICTURES.get(key);
        if (picture == null) {
            picture = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_map/" + made++ % KEEP);
            ResourceLocation reused = picture;
            PICTURES.values().removeIf(reused::equals);
            Minecraft.getInstance().getTextureManager().register(picture, new DynamicTexture(paint()));
            PICTURES.put(key, picture);
        }
        graphics.blit(picture, x, y, width, height, 0, 0, width * DETAIL, height * DETAIL, width * DETAIL, height * DETAIL);
    }

    private NativeImage paint() {
        NativeImage image = new NativeImage(width * DETAIL, height * DETAIL, false);
        double lap = track.lap();
        double distance = held.course().distance();
        double half = 0.6 / scale;
        boolean turf = held.surfaceNow() == Surface.TURF;
        double finish = track.wrap(distance);
        double last = track.wrap(held.course().finalFrom());
        boolean apart = Math.min(finish, lap - finish) > 1;
        for (int j = 0; j < image.getHeight(); j++)
            for (int i = 0; i < image.getWidth(); i++) {
                double z = ((i + 0.5) / DETAIL - width / 2.0) / scale;
                double cx = -((j + 0.5) / DETAIL - height / 2.0) / scale;
                TrackGeometry.Spot spot = track.locate(cx, z);
                int color = GROUND;
                if (track.onSurface(spot.offset())) {
                    boolean stripe = (int) (spot.progress() / lap * 40) % 2 == 0;
                    color = turf ? stripe ? 0xFF8CC447 : 0xFF7FB83A : held.surfaceNow() == Surface.DIRT ? stripe ? 0xFFC9A878 : 0xFFBF9C6A : stripe ? 0xFFB8A0C0 : 0xFFAE94B8;
                    if (distance < lap - 1 && spot.progress() > distance)
                        color = mix(color, GROUND, 0.62);
                    if (near(spot.progress(), last, half, lap))
                        color = FINAL;
                    if (apart && near(spot.progress(), 0, half, lap))
                        color = START;
                    if (near(spot.progress(), finish, half, lap))
                        color = FINISH;
                } else if (track.beyondSurface(spot.offset()) < RAIL) {
                    color = RAILS;
                } else if (spot.offset() < 0) {
                    double pond = Math.hypot(cx / (track.straight() * 0.36), z / (track.radius() * 0.33));
                    if (pond < 0.93)
                        color = WATER;
                    else if (pond < 1.06)
                        color = SHORE;
                }
                image.setPixelRGBA(i, j, (color & 0xFF00FF00) | (color & 0xFF) << 16 | (color >> 16 & 0xFF));
            }
        return image;
    }

    private static boolean near(double progress, double line, double within, double lap) {
        double apart = Math.abs(progress - line);
        return Math.min(apart, lap - apart) < within;
    }

    private static int mix(int a, int b, double part) {
        int color = 0xFF000000;
        for (int shift = 0; shift < 24; shift += 8)
            color |= (int) Math.round((a >> shift & 0xFF) * (1 - part) + (b >> shift & 0xFF) * part) << shift;
        return color;
    }

    /** Chevrons pointing the way round, on the two straights. */
    public void arrows(GuiGraphics graphics) {
        for (TrackGeometry.Section section : new TrackGeometry.Section[] { TrackGeometry.Section.BACK_STRAIGHT, TrackGeometry.Section.HOME_STRAIGHT }) {
            double from = track.sectionStart(section) + track.sectionLength(section) * (section == TrackGeometry.Section.HOME_STRAIGHT ? 0.2 : 0.4);
            if (held.course().distance() < track.lap() - 1 && track.wrap(from) > held.course().distance())
                continue;
            boolean up = screenY(from + 1, 0) < screenY(from, 0);
            for (int i = 0; i < 3; i++) {
                double at = from + i * 5 / scale;
                graphics.blit(RaceArt.PHONE, (int) Math.round(screenX(at, track.middle())) - 3, (int) Math.round(screenY(at, track.middle())) - 2, up ? 58 : 66, 238, 7, 4, 256, 256);
            }
        }
    }

    /** Everyone where they are; those in front on top. */
    public void runners(GuiGraphics graphics, List<Spot> spots) {
        int field = held.rows().size();
        for (Spot spot : spots.stream().sorted((a, b) -> Float.compare(a.progress(), b.progress())).toList()) {
            if (spot.state() == RaceLivePayload.OUT)
                continue;
            RaceMeetingPayload.Row row = held.row(spot.gate());
            boolean player = row != null && !row.player().isEmpty();
            double offset = Math.min(track.outer() - 1, Math.max(track.inner() + 1, spot.offset() * SPREAD + 1));
            graphics.pose().pushPose();
            graphics.pose().translate(screenX(spot.progress(), offset), screenY(spot.progress(), offset), 0);
            graphics.fill(-2, -3, 2, 3, player ? 0xFFFFFFFF : 0xFF303030);
            graphics.fill(-3, -2, 3, 2, player ? 0xFFFFFFFF : 0xFF303030);
            graphics.fill(-2, -2, 2, 2, RaceArt.bracketColor(spot.gate(), field));
            graphics.pose().popPose();
        }
    }
}
