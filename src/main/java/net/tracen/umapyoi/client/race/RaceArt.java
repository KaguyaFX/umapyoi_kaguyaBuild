package net.tracen.umapyoi.client.race;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.ClientUtils;
import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.RaceRanking;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/** The bits and pieces that the race terminal and the phone draw races with. */
public final class RaceArt {
    public static final ResourceLocation PHONE = texture("race_phone");
    public static final ResourceLocation PANEL = texture("three_goddess_blessing");
    public static final ResourceLocation SNAP = texture("training_snap");
    public static final ResourceLocation SKILL = texture("skill_learning");
    private static final ResourceLocation MOODS = texture("motivations");
    private static final ResourceLocation GODDESS = texture("three_goddess");

    public static final int WHITE = 0xFFFFFFFF, DARK = 0xFF404040, GRAY = 0xFF7A7A7A, FAINT = 0xFFC8C8C8;
    public static final int GREEN = 0xFF2F7A12, RED = 0xFFB02020, BLUE = 0xFF2A5DB0;
    public static final int BOX = 0xFFF4F4F4, BOX_EDGE = 0xFFB8B8B8;
    /** Behind the row of this client's player, and of another player. */
    public static final int MINE = 0xFFFFF3C4, THEIRS = 0xFFE4EFFB;
    public static final int TAG_PLAIN = 0xFF6D7680, TAG_TURF = 0xFF4F9A2A, TAG_DIRT = 0xFFA87A3A, TAG_SYNTHETIC = 0xFF9A5AA0, TAG_SKY = 0xFF3F8FC8, TAG_LEVEL = 0xFFC0602A;

    /** The caps of the brackets, from the inside. */
    private static final int[] BRACKET = { 0xFFFFFFFF, 0xFF2C2C2C, 0xFFD8362E, 0xFF2F66C8, 0xFFF2CF2E, 0xFF3A9A3A, 0xFFEF8A2A, 0xFFF08BB0 };
    private static final int[] BRACKET_TEXT = { DARK, WHITE, WHITE, WHITE, DARK, WHITE, WHITE, DARK };
    private static final int[] MEDAL = { 0xFFE0B020, 0xFFA8B0B8, 0xFFC8864A };
    private static final int[] MOOD = { 0xFFEE5B8F, 0xFFF08A42, 0xFFF5B82E, 0xFF1890D0, 0xFF8D6CB8 };
    private static final String[][] MOOD_ARROW = {
            { "..#..", ".###.", "#####", "..#..", "..#.." }, { ".####", "...##", "..#.#", ".#..#", "#...." }, { "..#..", "...#.", "#####", "...#.", "..#.." },
            { "#....", ".#..#", "..#.#", "...##", ".####" }, { "..#..", "..#..", "#####", ".###.", "..#.." } };

    private RaceArt() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/" + name + ".png");
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    // ---- shapes

    public static void box(GuiGraphics graphics, int x, int y, int w, int h, int fill, int edge) {
        graphics.fill(x, y, x + w, y + h, edge);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** With its corner pixels cut off. */
    public static void roundBox(GuiGraphics graphics, int x, int y, int w, int h, int fill, int edge) {
        graphics.fill(x + 1, y, x + w - 1, y + h, edge);
        graphics.fill(x, y + 1, x + w, y + h - 1, edge);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** The mod's dotted panel. */
    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        for (int dy = 0; dy < h; dy += 94)
            for (int dx = 0; dx < w; dx += 160)
                graphics.blit(PANEL, x + dx, y + dy, 6, 14, Math.min(160, w - dx), Math.min(94, h - dy), 256, 128);
    }

    /** The green of the mod's title bars. */
    public static void bar(GuiGraphics graphics, int x, int y, int w, int h) {
        for (int dy = 0; dy < h; dy++)
            graphics.blit(PANEL, x, y + dy, 6, Math.min(11, 1 + dy * 10 / Math.max(1, h - 1)), w, 1, 256, 128);
    }

    // ---- text

    /** Cut short if it's too long. */
    public static void line(GuiGraphics graphics, Component text, int x, int y, int width, int color, boolean shadow) {
        Font font = font();
        FormattedCharSequence shown = text.getVisualOrderText();
        if (font.width(text) > width)
            shown = Language.getInstance().getVisualOrder(FormattedText.composite(
                    font.substrByWidth(text, Math.max(0, width - font.width("..."))), Component.literal("...")));
        graphics.drawString(font, shown, x, y, color, shadow);
    }

    public static void line(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        line(graphics, text, x, y, width, color, false);
    }

    /** Cut off without further ado, for the narrowest lists. */
    public static void cut(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font(), Language.getInstance().getVisualOrder(font().substrByWidth(text, width)), x, y, color, false);
    }

    public static void center(GuiGraphics graphics, Component text, int x, int width, int y, int color, boolean shadow) {
        graphics.drawString(font(), text, x + (width - font().width(text)) / 2, y, color, shadow);
    }

    public static void right(GuiGraphics graphics, Component text, int right, int y, int color) {
        graphics.drawString(font(), text, right - font().width(text), y, color, false);
    }

    /** A label in a coloured box. @return its width and the gap after it */
    public static int tag(GuiGraphics graphics, Component text, int x, int y, int fill) {
        return tag(graphics, text, x, y, fill, 3, 2);
    }

    /** The same where there are many of them in a row. */
    public static int smallTag(GuiGraphics graphics, Component text, int x, int y, int fill) {
        return tag(graphics, text, x, y, fill, 2, 1);
    }

    private static int tag(GuiGraphics graphics, Component text, int x, int y, int fill, int padding, int gap) {
        int w = font().width(text) + padding * 2 - 1;
        roundBox(graphics, x, y, w, 11, fill, fill);
        graphics.drawString(font(), text, x + padding, y + 2, WHITE, false);
        return w + gap;
    }

    /** The mod's green button. */
    public static void button(GuiGraphics graphics, int x, int y, int w, Component text, boolean on, boolean over) {
        int v = on ? 4 : 23;
        graphics.blit(GODDESS, x, y, 181, v, w - 6, 17);
        graphics.blit(GODDESS, x + w - 6, y, 243, v, 6, 17);
        center(graphics, text, x, w, y + 4, !on ? 0xFFB8C8A0 : over ? 0xFFFFFFA0 : WHITE, on);
    }

    /** A little one. */
    public static void smallButton(GuiGraphics graphics, int x, int y, int w, int h, Component text, boolean on, boolean over) {
        roundBox(graphics, x, y, w, h, !on ? 0xFF5D7F2A : over ? 0xFFA6E61E : 0xFF8FD40A, 0xFF3F7A06);
        center(graphics, text, x, w + 1, y + (h - 7) / 2, on ? WHITE : 0xFFB8C8A0, false);
    }

    /** A mood's arrow. */
    public static void mood(GuiGraphics graphics, int x, int y, Motivations mood) {
        int at = mood.ordinal();
        roundBox(graphics, x, y, 9, 9, MOOD[at], MOOD[at]);
        for (int dy = 0; dy < 5; dy++)
            for (int dx = 0; dx < 5; dx++)
                if (MOOD_ARROW[at][dy].charAt(dx) == '#')
                    graphics.fill(x + 2 + dx, y + 2 + dy, x + 3 + dx, y + 3 + dy, WHITE);
    }

    /** The same as the mod shows one's own. */
    public static void moodBar(GuiGraphics graphics, int x, int y, Motivations mood) {
        graphics.blit(MOODS, x, y, 0, mood.ordinal() * 15, 64, 14, 64, 96);
        graphics.drawString(font(), moodName(mood), x + 16, y + 3, WHITE);
    }

    public static Component moodName(Motivations mood) {
        return Component.translatable("umapyoi.motivation." + mood.name().toLowerCase(Locale.ROOT));
    }

    /** Horse tickets. */
    public static String tickets(long amount) {
        return String.format("%,d HT", amount);
    }

    /** What a ticket pays, or a dash. */
    public static String odds(double odds) {
        return odds <= 0 ? "-" : odds < 100 ? String.format("%.1f", odds) : String.format("%.0f", odds);
    }

    // ---- runners

    private static int bracket(int gate, int field) {
        if (field <= 8)
            return Math.floorMod(gate - 1, 8);
        int each = field / 8;
        int plain = 8 - field % 8; // the outer brackets take the rest
        int inPlain = plain * each;
        int at = gate <= inPlain ? (gate - 1) / each : plain + (gate - inPlain - 1) / (each + 1);
        return Math.min(7, Math.max(0, at));
    }

    public static int bracketColor(int gate, int field) {
        return BRACKET[bracket(gate, field)];
    }

    /** The colour of a number on its bracket's colour. */
    public static int bracketTextColor(int gate, int field) {
        return BRACKET_TEXT[bracket(gate, field)];
    }

    /** Margins as races give them: a nose, a head, a neck, lengths to the quarter, then a distance. */
    private static final double[] LENGTHS = { 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3, 3.5, 4, 5, 6, 7, 8, 9, 10 };

    /**
     * How far behind the one in front, in lengths of a runner.
     *
     * @param lengths how many
     */
    public static Component margin(double lengths) {
        if (lengths < 0.1)
            return Component.translatable("umapyoi.race.margin.nose");
        if (lengths < 0.25)
            return Component.translatable("umapyoi.race.margin.head");
        if (lengths < 0.4)
            return Component.translatable("umapyoi.race.margin.neck");
        if (lengths > 10.5)
            return Component.translatable("umapyoi.race.margin.distance");
        double best = LENGTHS[0];
        for (double step : LENGTHS)
            if (Math.abs(step - lengths) < Math.abs(best - lengths))
                best = step;
        int whole = (int) best;
        double part = best - whole;
        String fraction = part == 0.25 ? "¼" : part == 0.5 ? "½" : part == 0.75 ? "¾" : "";
        return Component.literal((whole > 0 ? String.valueOf(whole) : "") + fraction);
    }

    /**
     * Seconds to a length, as real races go: a length is about 2.4 m, and they're timed at about 6
     * lengths a second. Our runners are faster than real ones, but margins read as they do there.
     */
    private static final double SECONDS_PER_LENGTH = 1 / 6.0;

    /** How many lengths this much time behind is. */
    public static double lengths(double seconds) {
        return seconds / SECONDS_PER_LENGTH;
    }

    /** The stall's number on its bracket's colour. */
    public static void chip(GuiGraphics graphics, int x, int y, int w, int h, int gate, int field, boolean off) {
        roundBox(graphics, x, y, w, h, off ? 0xFFD8D8D8 : bracketColor(gate, field), off ? 0xFFB0B0B0 : 0xFF5A5A5A);
        center(graphics, Component.literal(String.valueOf(gate)), x, w + 1, y + (h - 7) / 2, off ? 0xFF9A9A9A : BRACKET_TEXT[bracket(gate, field)], false);
    }

    /** A place: the first three get a medal. */
    public static void place(GuiGraphics graphics, int x, int y, int h, int place, int color) {
        boolean medal = place >= 1 && place <= MEDAL.length;
        if (medal)
            roundBox(graphics, x, y, 11, h, MEDAL[place - 1], MEDAL[place - 1]);
        center(graphics, Component.literal(place > 0 ? String.valueOf(place) : "-"), x, 12, y + (h - 7) / 2, medal ? WHITE : color, false);
    }

    /** A tip, 1 (the best) to 4. */
    public static void tip(GuiGraphics graphics, int x, int y, int tip) {
        if (tip >= 1 && tip <= 4)
            graphics.blit(PHONE, x, y, (tip - 1) * 10, 238, 9, 9, 256, 256);
        else
            graphics.fill(x + 3, y + 4, x + 6, y + 5, FAINT);
    }

    /** A player's name, or her own: there's no room for the outfit's. */
    public static MutableComponent name(RaceMeetingPayload.Row row) {
        return row.player().isEmpty() ? umaName(row.uma()) : Component.literal(row.player());
    }

    public static MutableComponent umaName(ResourceLocation uma) {
        ResourceLocation name = ClientUtils.getClientUmaDataRegistry().getOptional(uma).map(UmaData::identifier).orElse(uma);
        return UmaSoulUtils.getTranslatedUmaName(name).copy();
    }

    public static boolean mine(RaceMeetingPayload.Row row) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !row.player().isEmpty() && row.player().equals(minecraft.player.getScoreboardName());
    }

    public static int nameColor(RaceMeetingPayload.Row row) {
        return row.scratched() ? 0xFFB0B0B0 : mine(row) ? GREEN : row.player().isEmpty() ? DARK : BLUE;
    }

    /** Behind a player's row; 0 for a computer runner. */
    public static int rowColor(RaceMeetingPayload.Row row) {
        return row.scratched() || row.player().isEmpty() ? 0 : mine(row) ? MINE : THEIRS;
    }

    /** The grade of a stat; its own colours are meant for dark tooltips. */
    public static Component grade(int level) {
        String letters = ChatFormatting.stripFormatting(Component.translatable("umastatus.level." + level).getString());
        return Component.literal(letters == null ? "" : letters);
    }

    public static int gradeColor(Component grade) {
        String letters = grade.getString();
        return letters.isEmpty() ? DARK : letterColor(letters.charAt(0));
    }

    public static int aptitudeColor(Aptitude aptitude) {
        return letterColor(aptitude.name().charAt(0));
    }

    private static int letterColor(char letter) {
        return switch (letter) {
            case 'G' -> 0xFF8A8A8A;
            case 'F' -> 0xFF8A3FB0;
            case 'E' -> 0xFFD05AA8;
            case 'D' -> 0xFF1F93B8;
            case 'C' -> 0xFF3C9A14;
            case 'B' -> 0xFFC03030;
            case 'A' -> 0xFFE8603C;
            case 'S', 'M' -> 0xFFD98F10;
            case 'U' -> 0xFF2F66C8;
            default -> DARK;
        };
    }

    // ---- races

    public static MutableComponent raceName(ResourceLocation race) {
        return Component.translatable(Util.makeDescriptionId("race", race) + ".name");
    }

    public static Component tier(RaceRanking ranking) {
        return Component.translatable("race.umapyoi.tier." + ranking.name().toLowerCase(Locale.ROOT));
    }

    public static int tierColor(RaceRanking ranking) {
        return switch (ranking) {
            case GI -> 0xFF2F66C8;
            case GII -> 0xFFD84A8A;
            case GIII -> 0xFF2F8A2F;
            case OP -> 0xFFC88414;
            case PREOP -> 0xFFB09A20;
            case DEBUT -> 0xFF4A9AB8;
        };
    }

    public static Component field(Race race) {
        return Component.translatable("race." + race.field.getNamespace() + ".field." + race.field.getPath());
    }

    public static Component surface(RaceMeetingPayload held) {
        return Component.translatable("race.umapyoi.surface." + held.surfaceNow().name().toLowerCase(Locale.ROOT));
    }

    public static int surfaceColor(net.tracen.umapyoi.utils.Surface surface) {
        return switch (surface) {
            case TURF -> TAG_TURF;
            case DIRT -> TAG_DIRT;
            case SYNTHETIC -> TAG_SYNTHETIC;
            case ADAPTIVE -> TAG_PLAIN;
        };
    }

    public static Component distance(int meters) {
        Distance distance = Distance.match(meters);
        return Component.translatable("tooltip.umapyoi.race.distance." + (distance == null ? Distance.ADAPTIVE : distance).name().toLowerCase(Locale.ROOT));
    }

    public static Component direction(RaceMeetingPayload held) {
        return Component.translatable(held.leftHanded() ? "umapyoi.race.direction.left" : "umapyoi.race.direction.right");
    }

    public static String meters(double meters) {
        return Math.round(meters) + "m";
    }

    /** A time in the result: the winner's, or how much later. */
    public static String time(RaceMeetingPayload.Row row, float winner) {
        if (row.place() == 1) {
            int minutes = (int) (row.time() / 60);
            return String.format("%d:%05.2f", minutes, row.time() - minutes * 60);
        }
        return row.place() > 1 ? String.format("+%.2f", row.time() - winner) : "-";
    }
}
