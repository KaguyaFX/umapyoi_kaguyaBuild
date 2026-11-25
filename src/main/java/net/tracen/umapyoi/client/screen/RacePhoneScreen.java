package net.tracen.umapyoi.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tracen.umapyoi.client.race.RaceArt;
import net.tracen.umapyoi.client.race.RaceMap;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.race.RaceBetClient;
import net.tracen.umapyoi.race.RaceBetPayload;
import net.tracen.umapyoi.race.RaceEntities;
import net.tracen.umapyoi.race.RaceLivePayload;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.RaceMeetingPayload.Form;
import net.tracen.umapyoi.race.RaceMeetingPayload.Row;
import net.tracen.umapyoi.race.RaceOddsPayload;
import net.tracen.umapyoi.race.RaceStyle;
import net.tracen.umapyoi.race.RunnerEntity;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.ClientUtils;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/** The race phone: who runs, what's known of each of them, the course, and how the race goes. */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RacePhoneScreen extends Screen {
    private static final int WIDTH = 146, HEIGHT = 236, SCREEN_X = 7, SCREEN_Y = 24, W = 132, H = 202;
    private static final int HEADER_Y = 11, HEADER_H = 13, BODY_Y = 24, TABS_H = 18;
    private static final int TABLE_Y = 37, ROW_H = 11, ROWS = 12;
    private static final int MAP_Y = BODY_Y, ORDER_X = 65;
    private static final String[] STATS = { "speed", "stamina", "strength", "guts", "wisdom" };

    private static final int TAB_ENTRIES = 0, TAB_COURSE = 1, TAB_TICKETS = 2, TABS = 3;
    /** Kept while it's in the pocket. */
    private static int tab = TAB_ENTRIES;
    private static boolean byGate;
    /** The betting slips instead of the odds. */
    private static boolean showSlips;

    /** The stall of the runner being looked at; 0 = the table. */
    private int selected;
    private int scroll;
    /** The stall of the runner being bet on; 0 = none, and what kind of bet, and how much. */
    private int betOn;
    private boolean betPlace;
    private long stake;
    private int left, top;
    @Nullable
    private RunnerEntity figure;
    @Nullable
    private ResourceLocation figureOf;
    /** What the mouse is over, to be shown on top of everything. */
    private List<Component> tooltip = List.of();

    public RacePhoneScreen() {
        super(Component.translatable("item.umapyoi.race_phone"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RacePhoneScreen());
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2 + SCREEN_X;
        this.top = Math.max(0, (this.height - HEIGHT) / 2) + SCREEN_Y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** One can still see the race around it. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void tick() {
        if (this.minecraft == null || this.minecraft.player == null)
            return;
        // Handed in at the stalls.
        if (!this.minecraft.player.getMainHandItem().is(ItemRegistry.RACE_PHONE.get()) && !this.minecraft.player.getOffhandItem().is(ItemRegistry.RACE_PHONE.get()))
            this.onClose();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.tooltip = List.of();
        graphics.blit(RaceArt.PHONE, this.left - SCREEN_X, this.top - SCREEN_Y, 0, 0, WIDTH, HEIGHT, 256, 256);
        graphics.enableScissor(this.left, this.top, this.left + W, this.top + H);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
        RaceMeetingPayload held = RaceMeetingClient.current();
        RaceArt.panel(graphics, this.left, this.top, W, H);
        if (held == null) {
            this.status(graphics, null);
            this.header(graphics, this.title, null, false);
            this.centered(graphics, Component.translatable("umapyoi.meeting.problem.none"), 100);
        } else {
            Row row = this.selected == 0 ? null : held.row(this.selected);
            if (row == null)
                this.selected = 0;
            this.status(graphics, held);
            if (tab == TAB_COURSE)
                this.course(graphics, held, mouseX, mouseY);
            else if (tab == TAB_TICKETS)
                this.tickets(graphics, held, mouseX, mouseY);
            else if (row != null)
                this.runner(graphics, held, row, mouseX, mouseY);
            else if (held.rows().isEmpty())
                this.preparing(graphics, held);
            else
                this.table(graphics, held, mouseX, mouseY);
            this.tabs(graphics, held);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        if (!this.tooltip.isEmpty())
            graphics.renderComponentTooltip(this.font, this.tooltip, mouseX, mouseY);
    }

    private void centered(GuiGraphics graphics, Component text, int y) {
        for (var line : this.font.split(text, W - 12)) {
            graphics.drawString(this.font, line, this.left + (W - this.font.width(line)) / 2, this.top + y, RaceArt.GRAY, false);
            y += 11;
        }
    }

    private void preparing(GuiGraphics graphics, RaceMeetingPayload held) {
        this.raceHeader(graphics, held);
        this.centered(graphics, Component.translatable(held.stageNow().key()), 100);
    }

    /** The phone's own top line: what's going on, and the weather. */
    private void status(GuiGraphics graphics, @Nullable RaceMeetingPayload held) {
        int x = this.left, y = this.top;
        graphics.fill(x, y, x + W, y + HEADER_Y, RaceArt.WHITE);
        graphics.blit(RaceArt.PHONE, x + W - 15, y + 2, 42, 238, 13, 7, 256, 256);
        if (held == null)
            return;
        RaceMeeting.Stage stage = held.stageNow();
        Component what = Component.translatable(stage.key());
        graphics.drawString(this.font, what, x + 3, y + 2, stage == RaceMeeting.Stage.OPEN ? RaceArt.GREEN : RaceArt.RED, false);
        String clock = switch (stage) {
            case OPEN, CLOSED, LEAVING -> RaceMeetingClient.clock();
            case RUNNING -> RaceMeetingClient.home() ? "" : RaceMeetingClient.raceClockText();
            default -> "";
        };
        int used = 6 + this.font.width(what);
        graphics.drawString(this.font, clock, x + used, y + 2, RaceArt.DARK, false);
        used += this.font.width(clock);
        Component weather = Component.translatable(held.weatherNow().key()).append("·").append(Component.translatable(held.weatherNow().ground().key()));
        if (used + 4 + this.font.width(weather) < W - 17)
            RaceArt.right(graphics, weather, x + W - 18, y + 2, RaceArt.GRAY);
    }

    private void header(GuiGraphics graphics, Component text, @Nullable Race tier, boolean back) {
        int x = this.left, y = this.top + HEADER_Y;
        RaceArt.bar(graphics, x, y, W, HEADER_H);
        int room = W - (back ? 28 : 6);
        int badge = 0;
        Component tierName = tier == null ? Component.empty() : RaceArt.tier(tier.ranking);
        if (tier != null)
            badge = this.font.width(tierName) + 8;
        int w = Math.min(room - badge, this.font.width(text));
        int at = x + (W - w - badge) / 2;
        if (tier != null) {
            RaceArt.roundBox(graphics, at, y + 2, badge - 3, 10, RaceArt.tierColor(tier.ranking), RaceArt.WHITE);
            graphics.drawString(this.font, tierName, at + 3, y + 3, RaceArt.WHITE, false);
        }
        RaceArt.line(graphics, text, at + badge, y + 3, room - badge, RaceArt.WHITE, true);
        if (back)
            graphics.blit(RaceArt.PANEL, x + 2, y + 2, 200, 0, 10, 10, 256, 128);
    }

    private void raceHeader(GuiGraphics graphics, RaceMeetingPayload held) {
        this.header(graphics, RaceArt.raceName(held.race()), ClientUtils.getRaceRegistry().get(held.race()), false);
    }

    private void tabs(GuiGraphics graphics, RaceMeetingPayload held) {
        int y = this.top + H - TABS_H;
        graphics.fill(this.left, y, this.left + W, y + TABS_H, RaceArt.WHITE);
        graphics.fill(this.left, y, this.left + W, y + 1, RaceArt.BOX_EDGE);
        for (int i = 0; i < TABS; i++) {
            boolean on = tab == i;
            int x = this.left + i * W / TABS, w = W / TABS;
            if (on)
                graphics.fill(x, y, x + w, y + 2, 0xFF7DC10E);
            int color = on ? RaceArt.GREEN : 0xFF9A9A9A;
            Component name = Component.translatable(i == TAB_COURSE ? "gui.umapyoi.phone.course" : i == TAB_TICKETS ? "gui.umapyoi.phone.tickets"
                    : held.announced() ? "gui.umapyoi.phone.results" : "gui.umapyoi.phone.entries");
            int icon = 11 + i;
            boolean room = this.font.width(name) + icon <= w;
            int at = x + (w - this.font.width(name) - (room ? icon : 0)) / 2;
            if (room && i == TAB_ENTRIES) {
                for (int k = 0; k < 3; k++) {
                    graphics.fill(at, y + 6 + k * 3, at + 2, y + 8 + k * 3, color);
                    graphics.fill(at + 3, y + 6 + k * 3, at + 9, y + 8 + k * 3, color);
                }
            } else if (room && i == TAB_COURSE) {
                RaceArt.roundBox(graphics, at, y + 6, 10, 8, RaceArt.WHITE, color);
                graphics.fill(at + 3, y + 9, at + 7, y + 11, color);
            } else if (room) {
                RaceArt.roundBox(graphics, at, y + 6, 11, 8, RaceArt.WHITE, color);
                graphics.fill(at + 3, y + 7, at + 4, y + 13, color);
                graphics.fill(at + 5, y + 8, at + 9, y + 9, color);
                graphics.fill(at + 5, y + 11, at + 8, y + 12, color);
            }
            graphics.drawString(this.font, name, at + (room ? icon : 0), y + 6, color, false);
        }
    }

    // ---------------------------------------------------------------- entries and results

    private List<Row> sorted(RaceMeetingPayload held) {
        List<Row> rows = new ArrayList<>(held.rows());
        if (held.announced())
            rows.sort(Comparator.comparingInt(row -> row.place() > 0 ? row.place() : 100 + row.gate()));
        else if (!byGate)
            rows.sort(Comparator.comparingInt(row -> popularity(row) > 0 ? popularity(row) : 100 + row.gate()));
        return rows;
    }

    /** By the odds as they are now, if they are known. */
    private static int popularity(Row row) {
        int now = RaceBetClient.favourite(row.gate());
        return row.popularity() == 0 || now == 0 ? row.popularity() : now;
    }

    private void table(GuiGraphics graphics, RaceMeetingPayload held, int mouseX, int mouseY) {
        this.raceHeader(graphics, held);
        Race race = ClientUtils.getRaceRegistry().get(held.race());
        int x = this.left, y = this.top;
        MutableComponent what = Component.empty();
        if (race != null)
            what.append(RaceArt.field(race)).append(" ");
        what.append(RaceArt.surface(held)).append(" " + RaceArt.meters(held.meters()) + " ").append(RaceArt.direction(held));
        RaceArt.line(graphics, what, x + 4, y + BODY_Y + 3, W - 8, RaceArt.DARK);

        boolean results = held.announced();
        int head = y + TABLE_Y;
        graphics.fill(x, head, x + W, head + ROW_H, 0xFFDCECC4);
        graphics.drawString(this.font, Component.translatable(results ? "gui.umapyoi.phone.place" : "gui.umapyoi.phone.gate"), x + 2, head + 2, RaceArt.GREEN, false);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.phone.runner"), x + (results ? 30 : 17), head + 2, RaceArt.GREEN, false);
        if (results) {
            RaceArt.right(graphics, Component.translatable("gui.umapyoi.phone.time"), x + 108, head + 2, RaceArt.GREEN);
            RaceArt.right(graphics, Component.translatable("gui.umapyoi.phone.popularity"), x + W - 2, head + 2, RaceArt.GREEN);
        } else {
            RaceArt.center(graphics, Component.translatable("gui.umapyoi.phone.mood"), x + 63, 9, head + 2, RaceArt.GREEN, false);
            RaceArt.center(graphics, Component.translatable("gui.umapyoi.phone.tips"), x + 75, 29, head + 2, RaceArt.GREEN, false);
            RaceArt.center(graphics, Component.translatable("gui.umapyoi.phone.popularity"), x + 106, 19, head + 2, RaceArt.GREEN, false);
            int at = x + (byGate ? 12 : 126);
            for (int i = 0; i < 3; i++)
                graphics.fill(at + i, head + 4 + i, at + 5 - i, head + 5 + i, RaceArt.GREEN);
        }

        List<Row> rows = this.sorted(held);
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, rows.size() - ROWS));
        float winner = rows.stream().filter(row -> row.place() == 1).map(Row::time).findFirst().orElse(0F);
        for (int n = 0; n < Math.min(ROWS, rows.size()); n++) {
            Row row = rows.get(n + this.scroll);
            int at = head + ROW_H + n * ROW_H;
            boolean over = mouseX >= x && mouseX < x + W && mouseY >= at && mouseY < at + ROW_H;
            int back = RaceArt.rowColor(row);
            graphics.fill(x, at, x + W, at + ROW_H, over ? 0xFFDDF0C0 : back != 0 ? back : n % 2 == 0 ? RaceArt.WHITE : 0xFFF1F1F1);
            MutableComponent name = RaceArt.name(row);
            if (row.scratched())
                name.withStyle(ChatFormatting.STRIKETHROUGH);
            if (over)
                this.tooltip = this.fullName(row, results ? 45 : 46);
            int field = held.rows().size();
            if (results) {
                RaceArt.place(graphics, x + 1, at + 1, 9, row.place(), RaceArt.DARK);
                RaceArt.chip(graphics, x + 15, at + 1, 13, 9, row.gate(), field, row.scratched());
                RaceArt.line(graphics, name, x + 30, at + 2, 45, RaceArt.nameColor(row));
                RaceArt.right(graphics, Component.literal(RaceArt.time(row, winner)), x + 113, at + 2, row.place() == 1 ? RaceArt.DARK : RaceArt.GRAY);
                if (row.popularity() > 0)
                    RaceArt.center(graphics, Component.literal(String.valueOf(row.popularity())), x + 115, 17, at + 2, RaceArt.GRAY, false);
                continue;
            }
            RaceArt.chip(graphics, x + 2, at + 1, 13, 9, row.gate(), field, row.scratched());
            RaceArt.line(graphics, name, x + 17, at + 2, 46, RaceArt.nameColor(row));
            int popularity = popularity(row);
            if (popularity == 0)
                continue;
            RaceArt.mood(graphics, x + 63, at + 1, row.form().motivation());
            for (int k = 0; k < row.tips().length; k++)
                RaceArt.tip(graphics, x + 75 + k * 10, at + 1, row.tips()[k]);
            RaceArt.place(graphics, x + 110, at + 1, 9, popularity, RaceArt.DARK);
        }
        this.scrollbar(graphics, rows.size(), head + ROW_H);
    }

    private void scrollbar(GuiGraphics graphics, int rows, int y) {
        if (rows <= ROWS)
            return;
        int h = ROWS * ROW_H;
        int bar = h * ROWS / rows;
        int at = y + (h - bar) * this.scroll / (rows - ROWS);
        graphics.fill(this.left + W - 2, at, this.left + W, at + bar, 0x80406020);
    }

    /** What a row has no room for; nothing if it's all there. */
    private List<Component> fullName(Row row, int room) {
        Component uma = UmaSoulUtils.getTranslatedUmaName(row.uma());
        if (!row.player().isEmpty())
            return List.of(Component.literal(row.player()), uma.copy().withStyle(ChatFormatting.GRAY));
        return uma.getString().equals(RaceArt.umaName(row.uma()).getString()) && this.font.width(uma) <= room ? List.of() : List.of(uma);
    }

    // ---------------------------------------------------------------- one runner

    private void runner(GuiGraphics graphics, RaceMeetingPayload held, Row row, int mouseX, int mouseY) {
        this.header(graphics, RaceArt.umaName(row.uma()), null, true);
        int x = this.left, y = this.top;
        RaceArt.box(graphics, x + 4, y + 28, 48, 74, 0xFFDFECCF, RaceArt.BOX_EDGE);
        graphics.fill(x + 5, y + 88, x + 51, y + 101, 0xFFB9D98A);
        RunnerEntity figure = this.figure(row.uma());
        if (figure != null)
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 5, y + 29, x + 51, y + 101, 31, 0.0F, mouseX, mouseY, figure);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        RaceArt.chip(graphics, x + 6, y + 30, 13, 10, row.gate(), held.rows().size(), row.scratched());
        graphics.pose().popPose();

        Form form = row.form();
        int tx = x + 57;
        RaceArt.line(graphics, row.player().isEmpty() ? Component.translatable("gui.umapyoi.phone.computer") : Component.literal(row.player()), tx, y + 28, 72,
                row.player().isEmpty() ? RaceArt.GRAY : RaceArt.nameColor(row));
        RaceArt.moodBar(graphics, tx, y + 38, form.motivation());
        int popularity = popularity(row);
        if (popularity > 0) {
            Component label = Component.translatable("gui.umapyoi.phone.popularity");
            graphics.drawString(this.font, label, tx, y + 56, RaceArt.GRAY, false);
            graphics.drawString(this.font, String.valueOf(popularity), tx + this.font.width(label) + 4, y + 56, RaceArt.DARK, false);
            double odds = RaceBetClient.odds(row.gate(), false, 0);
            if (odds > 0) {
                Component pays = Component.literal(RaceArt.odds(odds));
                Component win = Component.translatable("gui.umapyoi.ticket.win");
                RaceArt.right(graphics, pays, x + W - 4, y + 56, RaceArt.DARK);
                RaceArt.right(graphics, win, x + W - 7 - this.font.width(pays), y + 56, RaceArt.GRAY);
            }
            Component tips = Component.translatable("gui.umapyoi.phone.tips");
            graphics.drawString(this.font, tips, tx, y + 67, RaceArt.GRAY, false);
            for (int k = 0; k < row.tips().length; k++)
                RaceArt.tip(graphics, tx + this.font.width(tips) + 4 + k * 12, y + 66, row.tips()[k]);
        }
        int used = this.aptitude(graphics, RaceArt.surface(held), form.aptitude(Form.SURFACE), tx, y + 78, RaceArt.GRAY);
        this.aptitude(graphics, RaceArt.distance(Math.round(held.meters())), form.aptitude(Form.DISTANCE), tx + Math.max(36, used + 4), y + 78, RaceArt.GRAY);
        Aptitude best = Aptitude.G;
        for (int i = 0; i < 4; i++)
            if (form.style(Position.values()[i]).compareTo(best) > 0)
                best = form.style(Position.values()[i]);
        int second = 36;
        for (int i = 0; i < 4; i++) {
            Position style = Position.values()[i];
            int w = this.aptitude(graphics, Component.translatable(RaceStyle.key(style)), form.style(style), tx + i % 2 * second, y + 87 + i / 2 * 9,
                    form.style(style) == best ? RaceArt.DARK : RaceArt.GRAY);
            if (i == 0)
                second = Math.max(second, Math.max(w, this.font.width(Component.translatable(RaceStyle.key(Position.LATE_SURGER))) + 9) + 3);
        }

        RaceArt.box(graphics, x + 4, y + 106, W - 8, 28, RaceArt.BOX, RaceArt.BOX_EDGE);
        for (int i = 0; i < Form.STATS; i++) {
            int sx = x + 5 + i * 49 / 2;
            graphics.blit(RaceArt.SNAP, sx + 6, y + 108, 3 + i * 23, 219, 12, 12, 256, 256);
            Component grade = RaceArt.grade(form.stat(i));
            RaceArt.center(graphics, grade, sx, 25, y + 123, RaceArt.gradeColor(grade), false);
            if (mouseX >= sx && mouseX < sx + 24 && mouseY >= y + 106 && mouseY < y + 134)
                this.tooltip = List.of(Component.translatable("gui.umapyoi.phone.stat." + STATS[i]));
        }

        Component skills = Component.translatable("gui.umapyoi.phone.skills");
        graphics.drawString(this.font, skills, x + 4, y + 138, RaceArt.GREEN, false);
        graphics.fill(x + 7 + this.font.width(skills), y + 142, x + W - 4, y + 143, 0xFFC8DCAE);
        List<UmaSkill> known = form.skills().stream().map(UmaSkillRegistry.REGISTRY::get).filter(java.util.Objects::nonNull).toList();
        if (known.isEmpty()) {
            RaceArt.line(graphics, Component.translatable(row.player().isEmpty() ? "gui.umapyoi.phone.skills.computer" : "gui.umapyoi.phone.skills.none"),
                    x + 4, y + 149, W - 8, 0xFF9A9A9A);
            return;
        }
        int shown = known.size() > 6 ? 5 : known.size();
        for (int i = 0; i < shown; i++)
            this.skill(graphics, known.get(i), x + 4 + i % 2 * 63, y + 148 + i / 2 * 12, mouseX, mouseY);
        if (shown < known.size()) {
            int sx = x + 4 + 63, sy = y + 148 + 24;
            graphics.drawString(this.font, "+" + (known.size() - shown), sx + 2, sy + 2, RaceArt.GRAY, false);
            if (mouseX >= sx && mouseX < sx + 61 && mouseY >= sy && mouseY < sy + 11)
                this.tooltip = known.subList(shown, known.size()).stream().map(UmaSkill::getDescription).toList();
        }
    }

    /** @return the width used */
    private int aptitude(GuiGraphics graphics, Component label, Aptitude aptitude, int x, int y, int color) {
        graphics.drawString(this.font, label, x, y, color, false);
        int w = this.font.width(label) + 3;
        graphics.drawString(this.font, aptitude.name(), x + w, y, RaceArt.aptitudeColor(aptitude), false);
        return w + 6;
    }

    private void skill(GuiGraphics graphics, UmaSkill skill, int x, int y, int mouseX, int mouseY) {
        RaceArt.roundBox(graphics, x, y, 61, 11, RaceArt.MINE, 0xFFE6B04A);
        int v = switch (skill.getType()) {
            case BUFF -> 21;
            case HINDER -> 37;
            case HEAL -> 53;
            case PASSIVE -> 69;
        };
        graphics.blit(RaceArt.SKILL, x + 2, y + 1, 9, 9, 176, v, 16, 16, 256, 256);
        RaceArt.line(graphics, skill.getDescription(), x + 13, y + 2, 47, RaceArt.DARK);
        if (mouseX >= x && mouseX < x + 61 && mouseY >= y && mouseY < y + 11)
            this.tooltip = List.of(skill.getDescription(), skill.getDescriptionDetail().copy().withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    private RunnerEntity figure(ResourceLocation uma) {
        if (this.minecraft == null || this.minecraft.level == null)
            return null;
        if (this.figure == null)
            this.figure = RaceEntities.RUNNER.get().create(this.minecraft.level);
        if (this.figure != null && !uma.equals(this.figureOf)) {
            this.figureOf = uma;
            ClientUtils.getClientUmaDataRegistry().getOptional(uma).ifPresent(data -> this.figure.showSoul(
                    UmaSoulUtils.initUmaSoul(ItemRegistry.UMA_SOUL.get().getDefaultInstance(), uma, data)));
        }
        return this.figure;
    }

    // ---------------------------------------------------------------- the course

    private void course(GuiGraphics graphics, RaceMeetingPayload held, int mouseX, int mouseY) {
        RaceMeeting.Stage stage = held.stageNow();
        boolean live = RaceMeetingClient.watching() && stage.ordinal() >= RaceMeeting.Stage.RUNNING.ordinal();
        if (!live)
            this.raceHeader(graphics, held);
        else if (held.announced())
            this.raceHeader(graphics, held);
        else if (RaceMeetingClient.home())
            this.header(graphics, Component.translatable(RaceMeeting.Stage.RESULTS.key()), null, false);
        else
            this.header(graphics, Component.translatable("gui.umapyoi.phone.left", RaceMeetingClient.metersLeft()), null, false);
        int x = this.left, y = this.top + MAP_Y;
        graphics.fill(x, y, x + W, this.top + H - TABS_H, RaceMap.GROUND);
        if (held.track() == null) {
            this.centered(graphics, Component.translatable(stage.key()), 100);
            return;
        }
        if (live)
            this.courseLive(graphics, held, x, y, mouseX, mouseY);
        else
            this.courseBefore(graphics, held, x, y);
    }

    private void courseBefore(GuiGraphics graphics, RaceMeetingPayload held, int x, int y) {
        RaceMap map = RaceMap.of(held, x, y, 82, H - BODY_Y - TABS_H);
        if (map == null)
            return;
        map.render(graphics);
        map.arrows(graphics);
        // The signs stand on the infield.
        double finish = held.course().distance();
        List<int[]> taken = new ArrayList<>();
        this.sign(graphics, map, Component.translatable("gui.umapyoi.phone.final"), held.course().finalFrom(), 0xFFD98F10, taken);
        this.sign(graphics, map, Component.translatable("gui.umapyoi.phone.finish"), finish, 0xFFD8362E, taken);
        this.sign(graphics, map, Component.translatable("gui.umapyoi.phone.start"), 0, 0xFF2F66C8, taken);

        int bx = x + 83, bw = W - 85;
        RaceArt.box(graphics, bx, y + 3, bw, H - BODY_Y - TABS_H - 6, RaceArt.BOX, RaceArt.BOX_EDGE);
        int at = y + 7;
        double perBlock = held.meters() / held.course().distance();
        long runners = held.rows().stream().filter(row -> !row.scratched()).count();
        at = this.fact(graphics, "gui.umapyoi.phone.length", Component.literal(RaceArt.meters(held.meters())), true, bx, bw, at);
        at = this.fact(graphics, "gui.umapyoi.phone.straight", Component.literal(RaceArt.meters(held.course().straight() * perBlock)), true, bx, bw, at);
        at = this.fact(graphics, "gui.umapyoi.phone.direction", Component.translatable(held.leftHanded() ? "gui.umapyoi.phone.direction.left" : "gui.umapyoi.phone.direction.right"), false, bx, bw, at);
        at = this.fact(graphics, "gui.umapyoi.phone.surface", RaceArt.surface(held), false, bx, bw, at);
        at = this.fact(graphics, "gui.umapyoi.phone.weather", Component.translatable(held.weatherNow().key()), false, bx, bw, at);
        at = this.fact(graphics, "gui.umapyoi.phone.ground", Component.translatable(held.weatherNow().ground().key()), false, bx, bw, at);
        at = this.fact(graphics, "gui.umapyoi.phone.runners", Component.literal(String.valueOf(runners)), true, bx, bw, at);
        graphics.fill(bx + 4, at, bx + bw - 4, at + 1, RaceArt.BOX_EDGE);
        this.fact(graphics, "gui.umapyoi.phone.final", Component.translatable("gui.umapyoi.phone.final.left", Math.round(held.meters() - held.course().finalFrom() * perBlock)), true, bx, bw, at + 4);
    }

    /**
     * A label and its value. If they don't fit on one line they take two, or the label is left out.
     *
     * @param needed whether the value says little without its label
     * @return where the next one goes
     */
    private int fact(GuiGraphics graphics, String key, Component value, boolean needed, int x, int width, int y) {
        Component label = Component.translatable(key);
        boolean one = this.font.width(label) + this.font.width(value) + 11 <= width;
        boolean two = !one && needed;
        if (one || two)
            RaceArt.line(graphics, label, x + 4, y, width - 8, RaceArt.GRAY);
        RaceArt.line(graphics, value, Math.max(x + 4, x + width - 4 - this.font.width(value)), two ? y + 10 : y, width - 8, RaceArt.DARK);
        return y + (two ? 22 : 13);
    }

    private void sign(GuiGraphics graphics, RaceMap map, Component text, double progress, int color, List<int[]> taken) {
        int w = this.font.width(text) + 4;
        double px = map.screenX(progress, map.track().inner() - 2), py = map.screenY(progress, map.track().inner() - 2);
        double dx = map.centerX() - px, dy = map.centerY() - py;
        double far = Math.max(1, Math.hypot(dx, dy));
        int sx = (int) Math.round(px + dx / far * (2 + w / 2.0 * Math.abs(dx) / far + 5 * Math.abs(dy) / far) - w / 2.0);
        int sy = (int) Math.round(py + dy / far * (2 + w / 2.0 * Math.abs(dx) / far + 5 * Math.abs(dy) / far) - 5);
        for (int[] other : taken)
            if (sx < other[0] + other[1] && other[0] < sx + w && Math.abs(sy - other[2]) < 11)
                sy = other[2] - 11;
        taken.add(new int[] { sx, w, sy });
        RaceArt.roundBox(graphics, sx, sy, w, 10, color, color);
        graphics.drawString(this.font, text, sx + 2, sy + 1, RaceArt.WHITE, false);
    }

    private void courseLive(GuiGraphics graphics, RaceMeetingPayload held, int x, int y, int mouseX, int mouseY) {
        List<RaceLivePayload.Spot> spots = RaceMeetingClient.spots();
        RaceMap map = RaceMap.of(held, x, y, ORDER_X, 136);
        if (map == null)
            return;
        map.render(graphics);
        map.runners(graphics, spots);

        List<Integer> gates = held.announced() ? this.sorted(held).stream().filter(row -> row.place() > 0).map(Row::gate).toList() : RaceMeetingClient.order();
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, gates.size() - ROWS));
        RaceArt.box(graphics, x + ORDER_X, y + 1, W - ORDER_X - 1, ROWS * ROW_H + 2, RaceArt.BOX, RaceArt.BOX_EDGE);
        int field = held.rows().size();
        for (int n = 0; n < Math.min(ROWS, gates.size()); n++) {
            Row row = held.row(gates.get(n + this.scroll));
            if (row == null)
                continue;
            int at = y + 2 + n * ROW_H;
            int back = RaceArt.rowColor(row);
            if (back != 0)
                graphics.fill(x + ORDER_X + 1, at, x + W - 2, at + ROW_H, back);
            RaceArt.right(graphics, Component.literal(String.valueOf(n + this.scroll + 1)), x + ORDER_X + 14, at + 2, RaceArt.GRAY);
            RaceArt.chip(graphics, x + ORDER_X + 14, at + 1, 12, 9, row.gate(), field, false);
            RaceArt.cut(graphics, RaceArt.name(row), x + ORDER_X + 28, at + 2, W - ORDER_X - 30, RaceArt.nameColor(row));
            if (mouseX >= x + ORDER_X && mouseX < x + W && mouseY >= at && mouseY < at + ROW_H)
                this.tooltip = this.fullName(row, W - ORDER_X - 30);
        }
        this.pack(graphics, spots, x, y + 136, field);
    }

    /** The front of the field close up: the leader on the right, the inside at the bottom. */
    private void pack(GuiGraphics graphics, List<RaceLivePayload.Spot> spots, int x, int y, int field) {
        int h = H - BODY_Y - TABS_H - 136;
        graphics.fill(x, y, x + W, y + h, 0xFF7FB83A);
        for (int i = 0; i < W; i += 24)
            graphics.fill(x + i, y, x + i + 12, y + h, 0xFF8CC447);
        graphics.fill(x, y, x + W, y + 1, 0xFFD9C9A0);
        graphics.fill(x, y + h - 1, x + W, y + h, RaceArt.WHITE);
        List<RaceLivePayload.Spot> running = spots.stream().filter(spot -> spot.state() != RaceLivePayload.OUT)
                .sorted(Comparator.comparingDouble(RaceLivePayload.Spot::progress)).toList();
        if (running.isEmpty())
            return;
        double lead = running.getLast().progress();
        double wide = 3, spread = 0, inside = Double.MAX_VALUE;
        for (RaceLivePayload.Spot spot : running) {
            if (lead - spot.progress() > 40)
                continue;
            spread = Math.max(spread, lead - spot.progress());
            inside = Math.min(inside, spot.offset());
            wide = Math.max(wide, spot.offset());
        }
        double perBlock = Mth.clamp((W - 16) / Math.max(1, spread), 2.6, 5.7);
        double perLane = Math.min(4.6, (h - 12) / Math.max(1, wide + 0.5));
        for (RaceLivePayload.Spot spot : running) {
            double px = x + W - 14 - (lead - spot.progress()) * perBlock;
            double py = y + h - 10.5 - Math.max(0, spot.offset() + 0.5) * perLane;
            if (px < x - 14)
                continue;
            graphics.pose().pushPose();
            graphics.pose().translate(px, py, 0);
            RaceArt.chip(graphics, 0, 0, 12, 9, spot.gate(), field, false);
            graphics.pose().popPose();
        }
    }

    // ---------------------------------------------------------------- the tickets

    private static final int ODDS_Y = 39, SLIPS_Y = 42, SLIP_H = 28, SLIP_STEP = 31, SLIPS = 4;
    /** The stake's buttons: where they are and how wide. */
    private static final int[][] STAKE = { { 4, 28 }, { 33, 21 }, { 55, 21 }, { 77, 28 }, { 106, 22 } };

    private void tickets(GuiGraphics graphics, RaceMeetingPayload held, int mouseX, int mouseY) {
        Row row = this.betOn == 0 ? null : held.row(this.betOn);
        if (row == null || !RaceBetClient.odds().open() || !RaceBetClient.running(row.gate()))
            this.betOn = 0;
        boolean bet = this.betOn != 0 && row != null;
        int x = this.left, y = this.top;
        RaceArt.bar(graphics, x, y + HEADER_Y, W, HEADER_H);
        Component balance = Component.literal(RaceArt.tickets(RaceBetClient.account().balance()));
        RaceArt.line(graphics, Component.translatable(bet ? "gui.umapyoi.ticket.bet" : "gui.umapyoi.phone.tickets"), x + (bet ? 16 : 5), y + HEADER_Y + 3, W - 24 - this.font.width(balance), RaceArt.WHITE, true);
        graphics.drawString(this.font, balance, x + W - 4 - this.font.width(balance), y + HEADER_Y + 3, 0xFFFFF3B0);
        if (bet) {
            graphics.blit(RaceArt.PANEL, x + 2, y + HEADER_Y + 2, 200, 0, 10, 10, 256, 128);
            this.betting(graphics, row, mouseX, mouseY);
            return;
        }
        int at = x + 3;
        for (int i = 0; i < 2; i++) {
            Component name = this.pill(i);
            int w = this.font.width(name) + 6;
            boolean on = (i == 1) == showSlips;
            RaceArt.roundBox(graphics, at, y + 26, w, 11, on ? 0xFF7DC10E : 0xFFD4D4D4, on ? 0xFF3F7A06 : 0xFFB0B0B0);
            graphics.drawString(this.font, name, at + 3, y + 28, on ? RaceArt.WHITE : RaceArt.GRAY, on);
            at += w + 2;
        }
        RaceOddsPayload odds = RaceBetClient.odds();
        Component state = !odds.ready() ? Component.translatable(odds.open() ? "gui.umapyoi.ticket.counting" : "gui.umapyoi.ticket.closed")
                : odds.open() ? Component.translatable("gui.umapyoi.ticket.closes", RaceBetClient.clock())
                : held.announced() ? Component.empty() : Component.translatable("gui.umapyoi.ticket.closed");
        if (at + this.font.width(state) > x + W - 3 && odds.open() && odds.ready())
            state = Component.literal(RaceBetClient.clock());
        if (at + this.font.width(state) <= x + W - 3)
            RaceArt.right(graphics, state, x + W - 4, y + 28, RaceArt.RED);
        if (showSlips)
            this.slips(graphics, held);
        else
            this.odds(graphics, held, mouseX, mouseY);
    }

    private Component pill(int index) {
        return index == 0 ? Component.translatable("gui.umapyoi.ticket.odds") : Component.translatable("gui.umapyoi.ticket.mine", RaceBetClient.odds().mine().size());
    }

    private void odds(GuiGraphics graphics, RaceMeetingPayload held, int mouseX, int mouseY) {
        int x = this.left, head = this.top + ODDS_Y;
        graphics.fill(x, head, x + W, head + ROW_H, 0xFFDCECC4);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.phone.gate"), x + 2, head + 2, RaceArt.GREEN, false);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.phone.runner"), x + 17, head + 2, RaceArt.GREEN, false);
        RaceArt.right(graphics, Component.translatable("gui.umapyoi.ticket.win"), x + 98, head + 2, RaceArt.GREEN);
        RaceArt.right(graphics, Component.translatable("gui.umapyoi.ticket.place"), x + 127, head + 2, RaceArt.GREEN);
        List<Row> rows = this.byOdds(held);
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, rows.size() - ROWS));
        for (int n = 0; n < Math.min(ROWS, rows.size()); n++) {
            Row row = rows.get(n + this.scroll);
            int at = head + ROW_H + n * ROW_H;
            boolean open = RaceBetClient.odds().open() && RaceBetClient.running(row.gate());
            boolean over = mouseX >= x && mouseX < x + W && mouseY >= at && mouseY < at + ROW_H;
            int back = RaceArt.rowColor(row);
            graphics.fill(x, at, x + W, at + ROW_H, over && open ? 0xFFDDF0C0 : back != 0 ? back : n % 2 == 0 ? RaceArt.WHITE : 0xFFF1F1F1);
            MutableComponent name = RaceArt.name(row);
            boolean off = row.scratched() || RaceBetClient.odds().ready() && !RaceBetClient.running(row.gate());
            if (off)
                name.withStyle(ChatFormatting.STRIKETHROUGH);
            if (over)
                this.tooltip = this.fullName(row, 46);
            RaceArt.chip(graphics, x + 2, at + 1, 13, 9, row.gate(), held.rows().size(), off);
            RaceArt.line(graphics, name, x + 17, at + 2, 46, off ? 0xFFB0B0B0 : RaceArt.nameColor(row));
            if (off)
                continue;
            RaceArt.mood(graphics, x + 64, at + 1, row.form().motivation());
            double win = RaceBetClient.odds(row.gate(), false, 0), place = RaceBetClient.odds(row.gate(), true, 0);
            RaceArt.right(graphics, Component.literal(RaceArt.odds(win)), x + 98, at + 2, win > 0 && win < 5 ? RaceArt.RED : RaceArt.DARK);
            RaceArt.right(graphics, Component.literal(RaceArt.odds(place)), x + 124, at + 2, place > 0 && place < 2 ? RaceArt.RED : RaceArt.DARK);
            if (RaceBetClient.staked(row.gate()) > 0)
                RaceArt.roundBox(graphics, x + 126, at + 3, 5, 5, 0xFFE0B020, 0xFFA87A10);
        }
        this.scrollbar(graphics, rows.size(), head + ROW_H);
    }

    private List<Row> byOdds(RaceMeetingPayload held) {
        List<Row> rows = new ArrayList<>(held.rows());
        rows.sort(Comparator.comparingInt(row -> popularity(row) > 0 ? popularity(row) : 100 + row.gate()));
        return rows;
    }

    private void betting(GuiGraphics graphics, Row row, int mouseX, int mouseY) {
        int x = this.left, y = this.top;
        RaceArt.box(graphics, x + 4, y + 27, W - 8, 22, RaceArt.WHITE, RaceArt.BOX_EDGE);
        RaceMeetingPayload held = RaceMeetingClient.current();
        RaceArt.chip(graphics, x + 8, y + 33, 13, 10, row.gate(), held == null ? 12 : held.rows().size(), false);
        Component name = RaceArt.name(row);
        Component popular = Component.translatable("gui.umapyoi.phone.popularity").append(" " + popularity(row));
        int room = W - 52 - this.font.width(popular);
        RaceArt.line(graphics, name, x + 25, y + 34, room, RaceArt.nameColor(row));
        RaceArt.mood(graphics, x + 28 + Math.min(room, this.font.width(name)), y + 34, row.form().motivation());
        RaceArt.right(graphics, popular, x + W - 8, y + 35, RaceArt.GRAY);

        int places = RaceBetClient.odds().places();
        if (places == 0)
            this.betPlace = false;
        for (int k = 0; k < 2; k++) {
            boolean place = k == 1, on = place == this.betPlace;
            int bx = x + 4 + k * 63;
            RaceArt.roundBox(graphics, bx, y + 53, 61, 30, on ? 0xFFF2FBE0 : 0xFFECECEC, on ? 0xFF5FAE0A : RaceArt.BOX_EDGE);
            graphics.drawString(this.font, Component.translatable(place ? "gui.umapyoi.ticket.place" : "gui.umapyoi.ticket.win"), bx + 5, y + 57, on ? RaceArt.GREEN : RaceArt.GRAY, false);
            RaceArt.right(graphics, place ? Component.translatable("gui.umapyoi.ticket.place.what", places) : Component.translatable("gui.umapyoi.ticket.win.what"), bx + 57, y + 57, RaceArt.GRAY);
            Component pays = place && places == 0 ? Component.translatable("gui.umapyoi.ticket.no_place")
                    : Component.translatable("gui.umapyoi.ticket.times", RaceArt.odds(RaceBetClient.odds(row.gate(), place, 0)));
            RaceArt.center(graphics, pays, bx, 61, y + 70, on ? RaceArt.DARK : RaceArt.GRAY, false);
        }

        this.stake = Mth.clamp(this.stake, 0, this.most());
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.ticket.amount"), x + 4, y + 88, RaceArt.GRAY, false);
        RaceArt.box(graphics, x + 30, y + 86, 76, 12, RaceArt.WHITE, RaceArt.BOX_EDGE);
        RaceArt.right(graphics, Component.literal(String.format("%,d", this.stake)), x + 88, y + 88, RaceArt.DARK);
        graphics.drawString(this.font, "HT", x + 91, y + 88, RaceArt.GRAY, false);
        RaceArt.roundBox(graphics, x + 109, y + 86, 19, 12, 0xFFE8E8E8, 0xFFB0B0B0);
        RaceArt.center(graphics, Component.translatable("gui.umapyoi.ticket.clear"), x + 109, 20, y + 88, RaceArt.GRAY, false);
        for (int k = 0; k < STAKE.length; k++) {
            int bx = x + STAKE[k][0];
            boolean over = mouseX >= bx && mouseX < bx + STAKE[k][1] && mouseY >= y + 101 && mouseY < y + 114;
            Component text = k == 4 ? Component.translatable("gui.umapyoi.ticket.all") : Component.literal((k < 2 ? "-" : "+") + Math.abs(this.step(k)));
            RaceArt.smallButton(graphics, bx, y + 101, STAKE[k][1], 13, text, true, over);
        }

        RaceArt.box(graphics, x + 4, y + 118, W - 8, 38, RaceArt.BOX, RaceArt.BOX_EDGE);
        double odds = RaceBetClient.odds(row.gate(), this.betPlace, this.stake);
        String[] labels = { "gui.umapyoi.ticket.after", "gui.umapyoi.ticket.would_pay", "gui.umapyoi.ticket.left" };
        Component[] values = { Component.translatable("gui.umapyoi.ticket.about", Component.translatable("gui.umapyoi.ticket.times", RaceArt.odds(odds))),
                Component.translatable("gui.umapyoi.ticket.about", RaceArt.tickets((long) Math.floor(odds * this.stake + 1e-6))),
                Component.literal(RaceArt.tickets(RaceBetClient.account().balance() - this.stake)) };
        for (int k = 0; k < 3; k++) {
            RaceArt.line(graphics, Component.translatable(labels[k]), x + 8, y + 122 + k * 11, W - 22 - this.font.width(values[k]), RaceArt.GRAY);
            RaceArt.right(graphics, values[k], x + W - 8, y + 122 + k * 11, k == 1 ? RaceArt.GREEN : RaceArt.DARK);
        }
        boolean over = mouseX >= x + 32 && mouseX < x + 100 && mouseY >= y + 160 && mouseY < y + 177;
        RaceArt.button(graphics, x + 32, y + 160, 68, Component.translatable("gui.umapyoi.ticket.bet"), this.stake > 0, over);
    }

    /** The most one can stake: what's on the account, in whole units. */
    private long most() {
        int unit = Math.max(1, RaceBetClient.odds().unit());
        return RaceBetClient.account().balance() / unit * unit;
    }

    /** What one of the stake's buttons adds. */
    private long step(int button) {
        long unit = Math.max(1, RaceBetClient.odds().unit());
        return switch (button) {
            case 0 -> -unit * 10;
            case 1 -> -unit;
            case 2 -> unit;
            default -> unit * 10;
        };
    }

    private void slips(GuiGraphics graphics, RaceMeetingPayload held) {
        List<RaceOddsPayload.Slip> mine = RaceBetClient.odds().mine();
        int x = this.left, y = this.top + SLIPS_Y;
        if (mine.isEmpty()) {
            this.centered(graphics, Component.translatable("gui.umapyoi.ticket.none"), 100);
            return;
        }
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, mine.size() - SLIPS));
        long staked = 0, paid = 0;
        boolean done = true;
        for (RaceOddsPayload.Slip slip : mine) {
            staked += slip.amount();
            paid += slip.paid();
            done &= slip.state() != RaceOddsPayload.OPEN;
        }
        for (int n = 0; n < Math.min(SLIPS, mine.size()); n++)
            this.slip(graphics, held, mine.get(n + this.scroll), x, y + n * SLIP_STEP);
        if (mine.size() < SLIPS) {
            int below = y + mine.size() * SLIP_STEP + 3;
            graphics.drawString(this.font, done ? Component.translatable("gui.umapyoi.ticket.sum.result", String.format("%,d", staked), String.format("%,d", paid))
                    : Component.translatable("gui.umapyoi.ticket.sum", String.format("%,d", staked)), x + 5, below, RaceArt.GRAY, false);
        }
    }

    /** A betting slip, with a stamp on it if it was right. */
    private void slip(GuiGraphics graphics, RaceMeetingPayload held, RaceOddsPayload.Slip slip, int x, int y) {
        boolean lost = slip.state() == RaceOddsPayload.LOST;
        graphics.fill(x + 4, y, x + W - 4, y + SLIP_H, lost ? RaceArt.BOX_EDGE : 0xFFD8B860);
        graphics.fill(x + 5, y + 1, x + W - 5, y + SLIP_H - 1, lost ? 0xFFE6E6E6 : 0xFFFFFBE8);
        for (int k = 0; k < SLIP_H; k += 4) {
            graphics.fill(x + 3, y + k + 1, x + 5, y + k + 3, 0xFFE4E4E4);
            graphics.fill(x + W - 5, y + k + 1, x + W - 3, y + k + 3, 0xFFE4E4E4);
        }
        Row row = held.row(slip.gate());
        int at = x + 9 + RaceArt.tag(graphics, Component.translatable(slip.place() ? "gui.umapyoi.ticket.place" : "gui.umapyoi.ticket.win"), x + 9, y + 3,
                lost ? 0xFF9A9A9A : slip.place() ? 0xFF2F66C8 : 0xFFC8402A);
        RaceArt.chip(graphics, at + 1, y + 3, 13, 10, slip.gate(), held.rows().size(), lost);
        // After a change somebody else may be in that stall.
        if (row != null && slip.state() != RaceOddsPayload.BACK)
            RaceArt.line(graphics, RaceArt.name(row), at + 17, y + 5, x + W - 26 - at - 17, lost ? 0xFF9A9A9A : RaceArt.nameColor(row));
        Component amount = Component.literal(RaceArt.tickets(slip.amount()));
        graphics.drawString(this.font, amount, x + 9, y + 17, RaceArt.GRAY, false);
        double odds = RaceBetClient.odds(slip.gate(), slip.place(), 0);
        Component text = switch (slip.state()) {
            case RaceOddsPayload.WON -> Component.literal("+" + String.format("%,d", slip.paid()));
            case RaceOddsPayload.LOST -> Component.translatable("gui.umapyoi.ticket.lost");
            case RaceOddsPayload.BACK -> Component.translatable("gui.umapyoi.ticket.back");
            default -> Component.translatable("gui.umapyoi.ticket.times", RaceArt.odds(odds)).append(" = " + String.format("%,d", (long) Math.floor(odds * slip.amount() + 1e-6)));
        };
        RaceArt.line(graphics, text, Math.max(x + 12 + this.font.width(amount), x + W - 10 - this.font.width(text)), y + 17, W - 22 - this.font.width(amount),
                slip.state() == RaceOddsPayload.WON ? RaceArt.GREEN : slip.state() == RaceOddsPayload.OPEN ? RaceArt.DARK : RaceArt.GRAY);
        if (slip.state() == RaceOddsPayload.WON) {
            graphics.blit(RaceArt.PHONE, x + W - 26, y + 1, 96, 238, 15, 15, 256, 256);
            RaceArt.center(graphics, Component.translatable("gui.umapyoi.ticket.won"), x + W - 26, 16, y + 5, 0xFFD8362E, false);
        }
    }

    private void touchTickets(RaceMeetingPayload held, double x, double y) {
        if (this.betOn != 0) {
            this.touchBetting(x, y);
            return;
        }
        if (y >= 26 && y < 37) {
            int at = 3;
            for (int i = 0; i < 2; i++) {
                int w = this.font.width(this.pill(i)) + 6;
                if (x >= at && x < at + w) {
                    showSlips = i == 1;
                    this.scroll = 0;
                    this.click();
                }
                at += w + 2;
            }
            return;
        }
        if (showSlips || y < ODDS_Y + ROW_H || !RaceBetClient.odds().open())
            return;
        int index = (int) ((y - ODDS_Y - ROW_H) / ROW_H) + this.scroll;
        List<Row> rows = this.byOdds(held);
        if (index < Math.min(rows.size(), ROWS + this.scroll) && RaceBetClient.running(rows.get(index).gate())) {
            this.betOn = rows.get(index).gate();
            this.betPlace = false;
            this.stake = Math.min(this.most(), 10L * RaceBetClient.odds().unit());
            this.click();
        }
    }

    private void touchBetting(double x, double y) {
        if (y >= HEADER_Y && y < HEADER_Y + HEADER_H && x < 16) {
            this.betOn = 0;
            this.click();
        } else if (y >= 53 && y < 83 && x >= 4 && x < 128) {
            this.betPlace = x >= 66 && RaceBetClient.odds().places() > 0;
            this.click();
        } else if (y >= 86 && y < 98 && x >= 109 && x < 128) {
            this.stake = 0;
            this.click();
        } else if (y >= 101 && y < 114) {
            for (int k = 0; k < STAKE.length; k++)
                if (x >= STAKE[k][0] && x < STAKE[k][0] + STAKE[k][1]) {
                    this.stake = k == 4 ? this.most() : Mth.clamp(this.stake + this.step(k), 0, this.most());
                    this.click();
                }
        } else if (y >= 160 && y < 177 && x >= 32 && x < 100 && this.stake > 0) {
            PacketDistributor.sendToServer(new RaceBetPayload(this.betOn, this.betPlace, this.stake));
            this.betOn = 0;
            showSlips = true;
            this.scroll = 0;
            this.click();
        }
    }

    // ---------------------------------------------------------------- touching it

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        RaceMeetingPayload held = RaceMeetingClient.current();
        double x = mouseX - this.left, y = mouseY - this.top;
        if (button != 0 || held == null || x < 0 || x >= W || y < 0 || y >= H)
            return super.mouseClicked(mouseX, mouseY, button);
        if (y >= H - TABS_H) {
            tab = Mth.clamp((int) (x * TABS / W), 0, TABS - 1);
            this.selected = 0;
            this.betOn = 0;
            this.scroll = 0;
            this.click();
            return true;
        }
        if (tab == TAB_COURSE)
            return true;
        if (tab == TAB_TICKETS) {
            this.touchTickets(held, x, y);
            return true;
        }
        if (this.selected != 0) {
            if (y >= HEADER_Y && y < HEADER_Y + HEADER_H && x < 16) {
                this.selected = 0;
                this.click();
            }
            return true;
        }
        if (y >= TABLE_Y && y < TABLE_Y + ROW_H && !held.announced()) {
            byGate = x < 72;
            this.click();
            return true;
        }
        int row = (int) Math.floor((y - TABLE_Y - ROW_H) / ROW_H) + this.scroll;
        List<Row> rows = this.sorted(held);
        if (y >= TABLE_Y + ROW_H && row < Math.min(rows.size(), ROWS + this.scroll)) {
            this.selected = rows.get(row).gate();
            this.click();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == TAB_TICKETS && this.betOn != 0)
            this.stake = Mth.clamp(this.stake + (long) Math.signum(scrollY) * RaceBetClient.odds().unit() * (hasShiftDown() ? 10 : 1), 0, this.most());
        else
            this.scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scan)) {
            if (this.selected != 0)
                this.selected = 0;
            else if (this.betOn != 0)
                this.betOn = 0;
            else
                this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
