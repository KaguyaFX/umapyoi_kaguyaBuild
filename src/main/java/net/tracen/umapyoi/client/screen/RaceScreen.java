package net.tracen.umapyoi.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.client.race.RaceArt;
import net.tracen.umapyoi.container.RaceContainer;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.UmaRaceTicketItem;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.RaceStyle;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.ClientUtils;
import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import net.tracen.umapyoi.utils.UmaStatusUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static net.tracen.umapyoi.item.UmaRaceTicketItem.getRaceNameInRawComponent;
import static net.tracen.umapyoi.item.UmaRaceTicketItem.getRaceNameInStyledComponent;

public class RaceScreen extends AbstractContainerScreen<RaceContainer> {

    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/race_screen.png");

    public RaceScreen(RaceContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.leftPos = 0;
        this.topPos = 0;
        this.imageWidth = 175;
        this.imageHeight = 201;
    }

    private static double bezier(double t, double v0, double v1, double v2, double v3) {
        double u = 1 - t;
        return u*u*u*v0 + 3*u*u*t*v1 + 3*u*t*t*v2 + t*t*t*v3;
    }

    public static final int SAMPLES = 10000;
    public static final HashMap<Position, Map.Entry<double[], double[]>> CONST_PROGRESS_MAP = new HashMap<>();

    public static Map.Entry<double[], double[]> calculateForPosition(Position pos) {
        double[] xs = new double[SAMPLES];
        double[] ys = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            double t = (double) i / (SAMPLES - 1);
            xs[i] = bezier(t, 0, pos.x1, pos.x2, 1);
            ys[i] = bezier(t, 0, pos.y1, pos.y2, 1);
        }
        return new AbstractMap.SimpleEntry<>(xs, ys);
    }

    static {
        for (Position position: Position.values()) {
            CONST_PROGRESS_MAP.put(position, calculateForPosition(position));
        }
    }

    public double mapProgress(double progress, Position position) {
        Map.Entry<double[], double[]> tables = CONST_PROGRESS_MAP.computeIfAbsent(position, RaceScreen::calculateForPosition);
        double[] xs = tables.getKey(), ys = tables.getValue();
        int idx = Arrays.binarySearch(xs, progress);
        if (idx >= 0) return ys[idx];
        int insertPoint = -idx - 1;
        if (insertPoint == 0) return ys[0];
        if (insertPoint == xs.length) return ys[xs.length - 1];
        double x0 = xs[insertPoint - 1], x1 = xs[insertPoint];
        double y0 = ys[insertPoint - 1], y1 = ys[insertPoint];
        return y0 + (y1 - y0) * (progress - x0) / (x1 - x0);
    }

    // ---------------------------------------------------------------- pages and their buttons

    private StyledButton toMeeting, toQuick, createButton, createRunButton, enterButton, watchButton, withdrawButton;

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos, y = this.topPos;
        this.toMeeting = this.addRenderableWidget(new StyledButton(x + 162, y + 1, 10, 10, StyledButton.Style.ARROW_RIGHT, Component::empty,
                () -> this.showPage(RaceContainer.PAGE_MEETING)));
        this.toQuick = this.addRenderableWidget(new StyledButton(x + 162, y + 1, 10, 10, StyledButton.Style.ARROW_LEFT, Component::empty,
                () -> this.showPage(RaceContainer.PAGE_QUICK)));
        this.createButton = this.addRenderableWidget(this.bigButton(x + 18, y + 87, "gui.umapyoi.meeting.create", RaceContainer.BUTTON_CREATE)
                .enabledWhen(() -> this.ticketRace() != null && !RaceMeeting.adaptive(this.ticketRace())));
        this.createRunButton = this.addRenderableWidget(this.bigButton(x + 92, y + 87, "gui.umapyoi.meeting.create_run", RaceContainer.BUTTON_CREATE_RUN)
                .enabledWhen(() -> this.ticketRace() != null && this.runProblem(this.ticketRace()) == null));
        this.enterButton = this.addRenderableWidget(new StyledButton(x + 18, y + 179, 68, 17, StyledButton.Style.BIG,
                () -> Component.translatable(this.entered() ? "gui.umapyoi.meeting.return" : "gui.umapyoi.meeting.enter"),
                () -> this.send(RaceContainer.BUTTON_ENTER))
                .enabledWhen(() -> this.stage() == RaceMeeting.Stage.OPEN && (this.entered() || this.heldProblem() == null)));
        this.watchButton = this.addRenderableWidget(this.bigButton(x + 92, y + 179, "gui.umapyoi.meeting.watch", RaceContainer.BUTTON_WATCH)
                .enabledWhen(() -> this.stage() != null && this.stage() != RaceMeeting.Stage.PREPARING && this.stage() != RaceMeeting.Stage.LEAVING));
        this.withdrawButton = this.addRenderableWidget(this.bigButton(x + 92, y + 179, "gui.umapyoi.meeting.withdraw", RaceContainer.BUTTON_WITHDRAW));
        this.refreshButtons();
    }

    private StyledButton bigButton(int x, int y, String key, int id) {
        return new StyledButton(x, y, 68, 17, StyledButton.Style.BIG, () -> Component.translatable(key), () -> this.send(id));
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null)
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void showPage(int page) {
        this.menu.setPage(page);
        this.send(page);
        this.refreshButtons();
    }

    private boolean onMeetingPage() {
        return this.menu.getPage() == RaceContainer.PAGE_MEETING;
    }

    private void refreshButtons() {
        boolean page = this.onMeetingPage();
        boolean held = page && RaceMeetingClient.current() != null;
        this.toMeeting.visible = !page;
        this.toQuick.visible = page;
        this.createButton.visible = page && !held;
        this.createRunButton.visible = page && !held;
        boolean withdraw = held && this.entered() && this.stage() == RaceMeeting.Stage.OPEN;
        this.enterButton.visible = held;
        this.watchButton.visible = held && !withdraw;
        this.withdrawButton.visible = withdraw;
        for (StyledButton button : List.of(this.createButton, this.createRunButton, this.enterButton, this.watchButton, this.withdrawButton))
            button.refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.refreshButtons();
    }

    @Nullable
    private RaceMeeting.Stage stage() {
        RaceMeetingPayload held = RaceMeetingClient.current();
        return held == null ? null : held.stageNow();
    }

    /** Whether this player runs in the race being held. */
    private boolean entered() {
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (held == null || this.minecraft == null || this.minecraft.player == null)
            return false;
        String name = this.minecraft.player.getScoreboardName();
        return held.rows().stream().anyMatch(row -> !row.scratched() && row.player().equals(name));
    }

    /** The race on the ticket in the meeting page's slot. */
    @Nullable
    private Race ticketRace() {
        ItemStack ticket = this.menu.getMeetingTicket();
        return ticket.is(ItemRegistry.UMA_RACE_TICKET.get()) ? UmaRaceTicketItem.getRace(ticket) : null;
    }

    @Nullable
    private Component runProblem(Race race) {
        return this.minecraft == null || this.minecraft.player == null ? null : RaceMeeting.problem(race, UmapyoiAPI.getUmaSoul(this.minecraft.player));
    }

    /** Why this player can't run in the race being held, or null. */
    @Nullable
    private Component heldProblem() {
        RaceMeetingPayload held = RaceMeetingClient.current();
        Race race = held == null ? null : ClientUtils.getRaceRegistry().get(held.race());
        return race == null ? null : this.runProblem(race);
    }

    // ---------------------------------------------------------------- the meeting page

    private static final int ROSTER_Y = 78, ROSTER_X = 7, ROSTER_W = 161;
    /** The three pictures of a course: the stand, the finish, the far side. */
    private static final int PICTURE_STAND = 1, PICTURE_FINISH = 39, PICTURE_FAR = 77;
    private static final int INFO = 0xB0FFFFFF;

    /** Plain panel over what belongs to the quick page, and the crowd at the bottom of it. */
    private void meetingPanel(GuiGraphics graphic, int bottom, int crowd) {
        RaceArt.panel(graphic, this.leftPos + 3, this.topPos + 13, 170, bottom - 13);
        graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 4, this.topPos + crowd, 4, 204, 168, 52, 420, 256);
    }

    private void picture(GuiGraphics graphic, int v) {
        graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 6, this.topPos + 24, 257, v, 162, 37, 420, 256);
    }

    private void label(GuiGraphics graphic, Component text) {
        int w = Math.min(157, this.font.width(text));
        this.renderLabelImage(graphic, Math.max(66, w + 8));
        RaceArt.line(graphic, text, this.leftPos + (this.imageWidth - w) / 2, this.topPos + 18, 157, RaceArt.WHITE, true);
    }

    private void renderMeetingBg(GuiGraphics graphic) {
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (held == null)
            this.renderApplication(graphic);
        else
            this.renderHeld(graphic, held);
    }

    private static Component levelTag(int level) {
        return Component.translatable("gui.umapyoi.meeting.level", RaceArt.grade(level));
    }

    private void renderApplication(GuiGraphics graphic) {
        this.meetingPanel(graphic, 117, 64);
        this.picture(graphic, PICTURE_STAND);
        int x = this.leftPos, y = this.topPos;
        graphic.fill(x + 7, y + 63, x + 168, y + 86, INFO);
        graphic.blit(RaceArt.PHONE, x + 9, y + 66, 76, 238, 18, 18, 256, 256);
        Race race = this.ticketRace();
        if (race == null) {
            this.label(graphic, Component.translatable("gui.umapyoi.meeting.apply"));
            List<FormattedCharSequence> hint = this.font.split(Component.translatable("gui.umapyoi.meeting.hint"), 136);
            for (int i = 0; i < Math.min(2, hint.size()); i++)
                graphic.drawString(this.font, hint.get(i), x + 31, y + 66 + i * 10, RaceArt.DARK, false);
            return;
        }
        this.label(graphic, getRaceNameInRawComponent(this.menu.getMeetingTicket()));
        int at = x + 31;
        at += RaceArt.tag(graphic, RaceArt.tier(race.ranking), at, y + 64, RaceArt.tierColor(race.ranking));
        at += RaceArt.tag(graphic, RaceArt.field(race), at, y + 64, RaceArt.TAG_PLAIN);
        RaceArt.tag(graphic, Component.translatable("race.umapyoi.surface." + race.surface.name().toLowerCase(Locale.ROOT)), at, y + 64, RaceArt.surfaceColor(race.surface));
        at = x + 31;
        MutableComponent distance = Component.empty();
        if (race.distance != Distance.ADAPTIVE)
            distance.append(RaceArt.meters(race.length) + " ");
        distance.append(Component.translatable("tooltip.umapyoi.race.distance." + race.distance.name().toLowerCase(Locale.ROOT)));
        at += RaceArt.tag(graphic, distance, at, y + 75, RaceArt.surfaceColor(race.surface));
        RaceArt.tag(graphic, levelTag(RaceMeeting.runnerLevel(race)), at, y + 75, RaceArt.TAG_LEVEL);
        // Why this player can't run in it, over the picture.
        Component problem = this.runProblem(race);
        if (problem != null) {
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("gui.umapyoi.meeting.cannot_run", problem), 153);
            int shown = Math.min(2, lines.size());
            graphic.fill(x + 7, y + 58 - shown * 10, x + 167, y + 60, 0xB0000000);
            for (int i = 0; i < shown; i++)
                graphic.drawString(this.font, lines.get(i), x + 11, y + 60 - (shown - i) * 10, 0xFFFF9A80, false);
        }
    }

    private void renderHeld(GuiGraphics graphic, RaceMeetingPayload held) {
        RaceMeeting.Stage stage = held.stageNow();
        Race race = ClientUtils.getRaceRegistry().get(held.race());
        int x = this.leftPos, y = this.topPos;
        this.meetingPanel(graphic, 198, 146);
        this.picture(graphic, stage == RaceMeeting.Stage.RUNNING ? PICTURE_FAR : stage.ordinal() > RaceMeeting.Stage.RUNNING.ordinal() ? PICTURE_FINISH : PICTURE_STAND);
        this.renderScene(graphic, held, stage);
        this.label(graphic, RaceArt.raceName(held.race()));

        // What's going on.
        graphic.fill(x + 7, y + 49, x + 167, y + 60, 0x98000000);
        Component clock = switch (stage) {
            case OPEN, CLOSED -> Component.translatable("gui.umapyoi.meeting.starts", RaceMeetingClient.clock());
            case RUNNING -> Component.literal(RaceMeetingClient.watching() && RaceMeetingClient.home() ? "" : RaceMeetingClient.raceClockText());
            case LEAVING -> Component.translatable("gui.umapyoi.meeting.leaves", RaceMeetingClient.clock());
            default -> Component.empty();
        };
        int color = stage == RaceMeeting.Stage.OPEN ? 0xFFB8F060 : stage == RaceMeeting.Stage.LEAVING ? 0xFFFFE08A : 0xFFFF9A80;
        RaceArt.line(graphic, Component.translatable(stage.key()), x + 10, y + 51, 152 - this.font.width(clock), color);
        RaceArt.right(graphic, clock, x + 165, y + 51, RaceArt.WHITE);

        // What race it is.
        int at = x + 7;
        if (race != null) {
            at += RaceArt.smallTag(graphic, RaceArt.tier(race.ranking), at, y + 64, RaceArt.tierColor(race.ranking));
            at += RaceArt.smallTag(graphic, RaceArt.field(race), at, y + 64, RaceArt.TAG_PLAIN);
        }
        at += RaceArt.smallTag(graphic, Component.empty().append(RaceArt.surface(held)).append(" " + RaceArt.meters(held.meters())), at, y + 64, RaceArt.surfaceColor(held.surfaceNow()));
        List<Component> rest = List.of(RaceArt.direction(held), Component.translatable(held.weatherNow().key()), Component.translatable(held.weatherNow().ground().key()));
        for (int i = 0; i < rest.size(); i++)
            if (at + this.font.width(rest.get(i)) + 3 <= x + 168)
                at += RaceArt.smallTag(graphic, rest.get(i), at, y + 64, i == 0 ? RaceArt.TAG_PLAIN : RaceArt.TAG_SKY);

        // Who runs.
        List<RaceMeetingPayload.Row> rows = this.roster(held);
        int half = this.rosterColumn(rows), rowH = this.rosterRow(rows), h = half * rowH + 5;
        RaceArt.box(graphic, x + ROSTER_X, y + ROSTER_Y, ROSTER_W, h, RaceArt.BOX, RaceArt.BOX_EDGE);
        graphic.fill(x + 87, y + ROSTER_Y + 2, x + 88, y + ROSTER_Y + h - 2, 0xFFD8D8D8);
        for (int i = 0; i < rows.size(); i++)
            this.renderRow(graphic, held, rows.get(i), x + 9 + i / half * 80, y + ROSTER_Y + 3 + i % half * rowH, Math.min(10, rowH));
        if (rows.isEmpty())
            RaceArt.center(graphic, Component.translatable(stage.key()), x + ROSTER_X, ROSTER_W, y + ROSTER_Y + 34, RaceArt.GRAY, false);

        // And what else there is to say.
        int info = y + ROSTER_Y + h + 3;
        int lines = info + 18 <= y + 177 ? 2 : 1;
        graphic.fill(x + 7, info, x + 168, info + lines * 9 + 1, INFO);
        Component level = levelTag(held.runnerLevel());
        RaceArt.line(graphic, Component.translatable("gui.umapyoi.meeting.applicant", held.applicant()), x + 10, info + 1, 150 - this.font.width(level), RaceArt.GRAY);
        RaceArt.right(graphic, level, x + 165, info + 1, RaceArt.GRAY);
        if (lines > 1 && !rows.isEmpty())
            RaceArt.line(graphic, Component.translatable(stage == RaceMeeting.Stage.LEAVING ? "gui.umapyoi.meeting.hint.leaving"
                    : stage.ordinal() >= RaceMeeting.Stage.RUNNING.ordinal() ? "gui.umapyoi.meeting.hint.running" : "gui.umapyoi.meeting.hint.entries"), x + 10, info + 10, 156, RaceArt.GRAY);
    }

    /** What is seen in the picture. */
    private void renderScene(GuiGraphics graphic, RaceMeetingPayload held, RaceMeeting.Stage stage) {
        int x = this.leftPos, y = this.topPos;
        long time = Util.getMillis() / 180;
        DummyUmaDefinition[] umas = DummyUmaDefinition.values();
        if (stage == RaceMeeting.Stage.RUNNING) {
            for (int i = 0; i < umas.length; i++)
                this.renderFigure(graphic, umas[(i + 2) % umas.length], x + 32 + i * 19, y + 50 - (int) ((time + i) % 2));
            graphic.blit(BACKGROUND_TEXTURE, x + 112, y + 31, 208, 81 + 24 * (int) (time % 4), 17, 19, 420, 256);
            return;
        }
        RaceMeetingPayload.Row winner = held.rows().stream().filter(row -> row.place() == 1).findFirst().orElse(null);
        if (winner != null) {
            graphic.blit(BACKGROUND_TEXTURE, x + 147, y + 27, 232, 82 + 26 * 4, 19, 23, 420, 256);
            graphic.blit(BACKGROUND_TEXTURE, x + 8, y + 30, 182, 81 + 24 * (int) (time % 4), 17, 19, 420, 256);
            graphic.blit(BACKGROUND_TEXTURE, x + 24, y + 26, 262, 159, 128, 22, 420, 256);
            RaceArt.line(graphic, Component.translatable("gui.umapyoi.meeting.winner", RaceArt.name(winner)),
                    x + 88 - Math.min(66, this.font.width(Component.translatable("gui.umapyoi.meeting.winner", RaceArt.name(winner)))) / 2, y + 35, 66, RaceArt.WHITE, true);
            return;
        }
        if (stage == RaceMeeting.Stage.RESULTS) {
            graphic.blit(BACKGROUND_TEXTURE, x + 147, y + 27, 232, 82, 19, 23, 420, 256);
            return;
        }
        // Waiting for the start.
        graphic.blit(BACKGROUND_TEXTURE, x + 13, y + 31, 346, 225, 22, 24, 420, 256);
        for (int i = 0; i < 3; i++)
            this.renderFigure(graphic, umas[i * 3 % umas.length], x + 46 + i * 25, y + 50);
    }

    private void renderFigure(GuiGraphics graphic, DummyUmaDefinition figure, int x, int bottom) {
        graphic.blit(figure.texture, x, bottom - figure.height, figure.uOffset, figure.vOffset, figure.width, figure.height, figure.textureWidth, figure.textureHeight);
    }

    /** The field by stall, or as they came in. */
    private List<RaceMeetingPayload.Row> roster(RaceMeetingPayload held) {
        if (!held.announced())
            return held.rows();
        List<RaceMeetingPayload.Row> rows = new ArrayList<>(held.rows());
        rows.sort(Comparator.comparingInt(row -> row.place() > 0 ? row.place() : 100 + row.gate()));
        return rows;
    }

    /** Twelve runners in two columns of six; with more, the rows get narrower. */
    private int rosterColumn(List<RaceMeetingPayload.Row> rows) {
        return Math.max(6, (rows.size() + 1) / 2);
    }

    private int rosterRow(List<RaceMeetingPayload.Row> rows) {
        int half = this.rosterColumn(rows);
        return half <= 6 ? 12 : half <= 7 ? 11 : 9;
    }

    private void renderRow(GuiGraphics graphic, RaceMeetingPayload held, RaceMeetingPayload.Row row, int x, int y, int h) {
        int back = RaceArt.rowColor(row);
        if (back != 0)
            graphic.fill(x, y - 1, x + 77, y + h + 1, back);
        MutableComponent name = RaceArt.name(row);
        if (row.scratched())
            name.withStyle(ChatFormatting.STRIKETHROUGH);
        int field = held.rows().size();
        int text = y + (h - 7) / 2;
        if (held.announced()) {
            RaceArt.place(graphic, x, y, h, row.place(), RaceArt.DARK);
            RaceArt.chip(graphic, x + 13, y, 12, h, row.gate(), field, row.scratched());
            RaceArt.line(graphic, name, x + 28, text, 49, RaceArt.nameColor(row));
            return;
        }
        RaceArt.chip(graphic, x + 1, y, 13, h, row.gate(), field, row.scratched());
        RaceArt.line(graphic, name, x + 17, text, 48, RaceArt.nameColor(row));
        if (row.popularity() > 0)
            RaceArt.place(graphic, x + 65, y, h, row.popularity(), RaceArt.GRAY);
    }

    /** The runner under the mouse. */
    @Nullable
    private RaceMeetingPayload.Row hoveredRow(RaceMeetingPayload held, int mouseX, int mouseY) {
        List<RaceMeetingPayload.Row> rows = this.roster(held);
        int half = this.rosterColumn(rows), rowH = this.rosterRow(rows);
        for (int i = 0; i < rows.size(); i++)
            if (this.isHovering(9 + i / half * 80, ROSTER_Y + 2 + i % half * rowH, 77, rowH, mouseX, mouseY))
                return rows.get(i);
        return null;
    }

    /** All there is to know about her. */
    private List<Component> rowTooltip(RaceMeetingPayload held, RaceMeetingPayload.Row row) {
        RaceMeetingPayload.Form form = row.form();
        List<Component> lines = new ArrayList<>();
        MutableComponent name = UmaSoulUtils.getTranslatedUmaName(row.uma()).copy();
        if (!row.player().isEmpty())
            name.append(Component.literal("  " + row.player()).withStyle(RaceArt.mine(row) ? ChatFormatting.GREEN : ChatFormatting.AQUA));
        lines.add(name);
        if (row.popularity() > 0)
            lines.add(Component.translatable("gui.umapyoi.phone.popularity").append(" ").append(Component.translatable("gui.umapyoi.phone.popularity.place", row.popularity())).withStyle(ChatFormatting.GRAY));
        MutableComponent stats = Component.empty();
        String[] names = { "speed", "stamina", "strength", "guts", "wisdom" };
        for (int i = 0; i < names.length; i++) {
            if (i == 3) {
                lines.add(stats);
                stats = Component.empty();
            }
            stats.append(Component.translatable("gui.umapyoi.phone.stat." + names[i]).withStyle(ChatFormatting.GRAY)).append(" ")
                    .append(UmaStatusUtils.getStatusLevel(form.stat(i))).append("  ");
        }
        lines.add(stats);
        lines.add(aptitude(RaceArt.surface(held), form.aptitude(RaceMeetingPayload.Form.SURFACE))
                .append(aptitude(RaceArt.distance(Math.round(held.meters())), form.aptitude(RaceMeetingPayload.Form.DISTANCE))));
        MutableComponent styles = Component.empty();
        for (int i = 0; i < 4; i++)
            styles.append(aptitude(Component.translatable(RaceStyle.key(Position.values()[i])), form.style(Position.values()[i])));
        lines.add(styles);
        return lines;
    }

    private static MutableComponent aptitude(Component label, Aptitude aptitude) {
        return Component.empty().append(label.copy().withStyle(ChatFormatting.GRAY)).append(" ").append(aptitude.styledComponent()).append("  ");
    }

    @Override
    public void render(@Nonnull GuiGraphics graphic, final int mouseX, final int mouseY, float partialTicks) {
        this.refreshButtons();
        super.render(graphic, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphic, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphic, int mouseX, int mouseY) {
        Component heading = this.onMeetingPage() ? Component.translatable("gui.umapyoi.meeting.title") : this.title;
        graphic.drawString(this.font, heading,
                (this.imageWidth / 2) - (this.font.width(heading.getVisualOrderText()) / 2),
                this.titleLabelY - 3, 0xFFFFFF);
        if (this.onMeetingPage() && RaceMeetingClient.current() != null)
            return;
        graphic.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphic, float partialTicks, int mouseX, int mouseY) {
        if (this.minecraft == null) {
            return;
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        // Background image (with slot)
        graphic.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth,
                this.imageHeight, 420, 256);
        if (this.onMeetingPage()) {
            this.raceLabel = null;
            this.unavailableReason = null;
            this.renderMeetingBg(graphic);
            return;
        }
        this.renderDetail(graphic);
    }

    private Component raceLabel = null;

    // Track picture area (GUI coordinates), used for the "why can't this race start" overlay.
    private static final int TRACK_X = 7, TRACK_Y = 25, TRACK_W = 161, TRACK_H = 35;
    private static final int REASON_MAX_LINES = 2;

    @Nullable
    private Component unavailableReason = null;

    private void renderUnavailableReason(GuiGraphics graphic) {
        this.unavailableReason = this.menu.tileEntity.getUnavailableReason();
        if (this.unavailableReason == null)
            return;
        int x0 = this.leftPos + TRACK_X, y0 = this.topPos + TRACK_Y;
        graphic.fill(x0, y0, x0 + TRACK_W, y0 + TRACK_H, 0xB0000000);

        Component header = Component.translatable("tooltip.umapyoi.race.unavailable");
        List<FormattedCharSequence> lines = this.font.split(this.unavailableReason, TRACK_W - 8);
        boolean cut = lines.size() > REASON_MAX_LINES;
        if (cut)
            lines = lines.subList(0, REASON_MAX_LINES);
        int lineHeight = this.font.lineHeight + 1;
        int y = y0 + (TRACK_H - lineHeight * (lines.size() + 1)) / 2 + 1;
        graphic.drawString(this.font, header, x0 + (TRACK_W - this.font.width(header)) / 2, y, 0xFFD24A);
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            if (cut && i == lines.size() - 1)
                line = FormattedCharSequence.composite(line, Component.literal("...").getVisualOrderText());
            y += lineHeight;
            graphic.drawString(this.font, line, x0 + (TRACK_W - this.font.width(line)) / 2, y, 0xFF8080);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (this.onMeetingPage() && held != null) {
            RaceMeetingPayload.Row row = this.hoveredRow(held, pX, pY);
            if (row != null)
                pGuiGraphics.renderComponentTooltip(this.font, this.rowTooltip(held, row), pX, pY);
            return;
        }
        // Full text of a long reason when hovering the overlay.
        if (this.unavailableReason != null && this.menu.getCarried().isEmpty()
                && this.isHovering(TRACK_X, TRACK_Y, TRACK_W, TRACK_H, pX, pY)) {
            pGuiGraphics.renderTooltip(this.font, this.font.split(this.unavailableReason, 200), pX, pY);
        }
        if (raceLabel == null) return;
        if (this.isHovering(55, 16, 66, 12, pX, pY)) {
            pGuiGraphics.renderComponentTooltip(this.font, List.of(this.raceLabel), pX, pY);
        }
    }

    private void renderMainUma(GuiGraphics pGuiGraphics, double progress, Position tactic) {
        int variance = this.menu.getAnimationTickMod(4);
        double mapped = mapProgress(progress, tactic);
        int startPixel = (int) Mth.lerp(mapped, 13d, 143d);
        int uOffset = mapped >= 0.75d ? 182 : 208;
        int vOffset = 81 + 24 * variance;
        pGuiGraphics.blit(BACKGROUND_TEXTURE, this.leftPos + startPixel, this.topPos + 35, uOffset, vOffset,
                17, 19, 420, 256);
    }

    private void renderDummyUma(GuiGraphics pGuiGrapahics, double progress, DummyUmaDefinition umaDefinition) {
        int startPixel = (int) Mth.lerp(mapProgress(progress, umaDefinition.tactic), umaDefinition.start, umaDefinition.end);
        pGuiGrapahics.blit(umaDefinition.texture, this.leftPos + startPixel,
                this.topPos + 54 - umaDefinition.height, umaDefinition.uOffset, umaDefinition.vOffset,
                umaDefinition.width, umaDefinition.height, umaDefinition.textureWidth, umaDefinition.textureHeight);
    }

    private void renderLabelImage(GuiGraphics graphic, int backgroundWidth) {
        if ((backgroundWidth & 1) == 1) backgroundWidth += 1;
        int repeatZone = backgroundWidth - 46;
        int startPos = this.leftPos + (this.imageWidth / 2) - (backgroundWidth / 2);
        graphic.blit(BACKGROUND_TEXTURE, startPos, this.topPos + 16, 184, 4, 16, 12, 420, 256);
        startPos += 16;
        while (repeatZone > 0) {
            int renderChunkWidth = Math.min(repeatZone, 20);
            graphic.blit(BACKGROUND_TEXTURE, startPos, this.topPos + 16, 200, 4, renderChunkWidth, 12, 420, 256);
            repeatZone -= renderChunkWidth;
            startPos += renderChunkWidth;
        }
        graphic.blit(BACKGROUND_TEXTURE, startPos, this.topPos + 16, 220, 4, 30, 12, 420, 256);
    }

    private void renderDetail(GuiGraphics graphic) {
        this.unavailableReason = null;
        if (this.menu.inventory.getStackInSlot(0).isEmpty() || this.menu.inventory.getStackInSlot(1).isEmpty()) {
            raceLabel = null;
            return;
        };
        if (!this.menu.tileEntity.fulfill()) {
            // Condition doesn't satisfy: the track is empty anyway, so explain why on top of it.
            raceLabel = null;
            this.renderUnavailableReason(graphic);
            return;
        }
        // Goal (Most underlay)
        graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 143, this.topPos + 31,
                232, 82 + this.menu.getGoalType() * 26, 19, 23,
                420, 256);

        // Render Uma's here (farther from camera, underlay)
        float progress = this.menu.getProgression();
        if (Float.isNaN(progress)) progress = 0;
        RandomSource random = RandomSource.create(this.menu.getWinnerReplaceSeed());
        double[] factors = new double[3];
        int whoShallWin = this.menu.shallSoulWin() ? 0 : random.nextIntBetweenInclusive(1, 2);
        for (int i = 0; i < 3; i++) {
            if (i == whoShallWin) {
                factors[i] = 1d;
            } else {
                if (i == 0) {
                    factors[i] = this.menu.getBaseScaleFactor();
                } else {
                    factors[i] = Mth.lerp(random.nextDouble(), 0.75d, 1d);
                }
            }
        }
        for (int i = 1; i <= 2; i++) {
            renderDummyUma(graphic, progress * factors[i],
                    DummyUmaDefinition.values()[random.nextInt(0, DummyUmaDefinition.values().length)]
            );
        }
        renderMainUma(graphic, progress * factors[0], this.menu.getSoulTactic());
        // Render anything here that is closer to camera (overlay)
        // Starting gates
        graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 13, this.topPos + 31, 346, 225,
                22, 24, 420, 256);

        // Race label
        ItemStack stackOfTicket = this.menu.inventory.getStackInSlot(1);
        if (stackOfTicket.is(ItemRegistry.UMA_RACE_TICKET.get())) {
            MutableComponent component = getRaceNameInRawComponent(stackOfTicket);
            raceLabel = getRaceNameInStyledComponent(stackOfTicket);
            int fullWidth = this.font.width(component.getVisualOrderText());
            if (fullWidth <= 157) {
                if (fullWidth <= 61) {
                    graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 55, this.topPos + 16, 184, 4,
                            66, 12, 420, 256);
                } else {
                    int backgroundWidth = fullWidth + 5;
                    renderLabelImage(graphic, backgroundWidth);
                }
                graphic.drawString(this.font, component,
                        this.leftPos + (this.imageWidth / 2) - fullWidth / 2, this.topPos + 18, 0xFFFFFF);
            } else {
                int ellipseWidth = this.font.width("...");
                int maxWidth = 157 - ellipseWidth;
                List<FormattedCharSequence> split = this.font.split(component, maxWidth);
                FormattedCharSequence combinedSequence = FormattedCharSequence.composite(
                        split.get(0), Component.literal("...").getVisualOrderText()
                );
                int textWidth = this.font.width(combinedSequence);
                renderLabelImage(graphic, textWidth + 5);
                graphic.drawString(this.font, combinedSequence,
                        this.leftPos + (this.imageWidth / 2) - textWidth / 2,
                        this.topPos + 18, 0xffffff);
            }
        } else {
            raceLabel = null;
        }
    }

    public enum DummyUmaDefinition {
        U1(21, 19, 181, 176, 13, 140, Position.PACE_CHASER),
        U2(18, 19, 208, 176, 13, 143, Position.PACE_CHASER),
        U3(18, 19, 183, 198, 13, 143, Position.END_CLOSER),
        U4(18, 19, 208, 198, 13, 143, Position.LATE_SURGER);

        public final int width;
        public final int height;
        public final int uOffset;
        public final int vOffset;
        public final int start;
        public final int end;
        public final Position tactic;
        public final ResourceLocation texture;
        public final int textureHeight;
        public final int textureWidth;

        DummyUmaDefinition(int w, int h, int u, int v, int s, int e, Position tactic, ResourceLocation rl, int textureWidth, int textureHeight) {
            this.width = w;
            this.height = h;
            this.uOffset = u;
            this.vOffset = v;
            this.start = s;
            this.end = e;
            this.tactic = tactic;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.texture = rl;
        }

        DummyUmaDefinition(int w, int h, int u, int v, int s, int e, Position tactic) {
            this(w, h, u, v, s, e, tactic, BACKGROUND_TEXTURE, 420, 256);
        }
    }
}
