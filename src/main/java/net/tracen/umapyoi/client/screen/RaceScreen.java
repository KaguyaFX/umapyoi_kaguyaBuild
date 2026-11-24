package net.tracen.umapyoi.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
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
import net.tracen.umapyoi.container.RaceContainer;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.UmaRaceTicketItem;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.registry.umadata.UmaData;
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

    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/three_goddess_blessing.png");
    private static final int DARK = 0x404040, GRAY = 0x7A7A7A, RED = 0xB02020, GREEN = 0x2F7A12;
    private static final int ROSTER_Y = 76, ROSTER_ROW = 11, ROSTER_ROWS = 9;

    /** Plain panel over the picture and the slots behind it (the dotted pattern repeats every two pixels). */
    private void panel(GuiGraphics graphic, int top, int bottom) {
        for (int y = top; y < bottom; y += 80)
            graphic.blit(PANEL_TEXTURE, this.leftPos + 3, this.topPos + y, 3, 14 + (top & 1), 170, Math.min(80, bottom - y), 256, 128);
    }

    private void renderMeetingBg(GuiGraphics graphic) {
        if (RaceMeetingClient.current() == null) {
            this.panel(graphic, 14, 117);
            int x = this.leftPos + 10, y = this.topPos + 26;
            graphic.fill(x, y, x + 18, y + 18, 0xFF373737);
            graphic.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
            graphic.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
            return;
        }
        this.panel(graphic, 14, 198);
        int x = this.leftPos + 7, y = this.topPos + ROSTER_Y - 3;
        graphic.fill(x, y, x + 161, y + ROSTER_ROWS * ROSTER_ROW + 5, 0xFFB8B8B8);
        graphic.fill(x + 1, y + 1, x + 160, y + ROSTER_ROWS * ROSTER_ROW + 4, 0xFFF4F4F4);
    }

    private void line(GuiGraphics graphic, Component text, int x, int y, int width, int color) {
        FormattedCharSequence shown = text.getVisualOrderText();
        if (this.font.width(text) > width)
            shown = Language.getInstance().getVisualOrder(FormattedText.composite(
                    this.font.substrByWidth(text, width - this.font.width("...")), Component.literal("...")));
        graphic.drawString(this.font, shown, x, y, color, false);
    }

    private Component grade(int level) {
        return Component.translatable("gui.umapyoi.meeting.level", UmaStatusUtils.getStatusLevel(level));
    }

    private void renderMeetingLabels(GuiGraphics graphic) {
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (held == null) {
            this.renderApplication(graphic);
            return;
        }
        Race race = ClientUtils.getRaceRegistry().get(held.race());
        MutableComponent name = Component.translatable(Util.makeDescriptionId("race", held.race()) + ".name");
        if (race != null)
            name.withStyle(race.ranking.color == ChatFormatting.YELLOW ? ChatFormatting.GOLD : race.ranking.color);
        this.line(graphic, name, 9, 16, 108, DARK);
        MutableComponent what = Component.empty();
        if (race != null)
            what.append(Component.translatable("race.umapyoi.tier." + race.ranking.name().toLowerCase(Locale.ROOT))).append(" · ")
                    .append(Component.translatable("race." + race.field.getNamespace() + ".field." + race.field.getPath())).append(" · ");
        what.append(Component.translatable(held.leftHanded() ? "umapyoi.race.direction.left" : "umapyoi.race.direction.right"));
        this.line(graphic, what, 9, 27, 158, DARK);
        Distance distance = Distance.match(Math.round(held.meters()));
        MutableComponent how = Component.empty();
        how.append(Component.translatable("race.umapyoi.surface." + held.surfaceNow().name().toLowerCase(Locale.ROOT))).append(" " + Math.round(held.meters()) + "m (")
                .append(Component.translatable("tooltip.umapyoi.race.distance." + (distance == null ? Distance.ADAPTIVE : distance).name().toLowerCase(Locale.ROOT))).append(")");
        this.line(graphic, how, 9, 38, 158, DARK);
        this.line(graphic, Component.translatable("gui.umapyoi.meeting.conditions", Component.translatable(held.weatherNow().key()),
                Component.translatable(held.weatherNow().ground().key()), this.grade(held.runnerLevel())), 9, 49, 158, GRAY);
        RaceMeeting.Stage stage = held.stageNow();
        Component state = stage == RaceMeeting.Stage.OPEN || stage == RaceMeeting.Stage.CLOSED
                ? Component.translatable("gui.umapyoi.meeting.state", Component.translatable(stage.key()), RaceMeetingClient.clock())
                : Component.translatable(stage.key());
        this.line(graphic, state, 9, 60, 158, stage == RaceMeeting.Stage.OPEN ? GREEN : RED);
        Component applicant = Component.literal(held.applicant());
        this.line(graphic, applicant, 167 - Math.min(46, this.font.width(applicant)), 16, 46, GRAY);
        String me = this.minecraft == null || this.minecraft.player == null ? "" : this.minecraft.player.getScoreboardName();
        int half = (held.rows().size() + 1) / 2;
        for (int i = 0; i < held.rows().size() && i < ROSTER_ROWS * 2; i++) {
            RaceMeetingPayload.Row row = held.rows().get(i);
            int x = 9 + i / half * 80, y = ROSTER_Y + i % half * ROSTER_ROW;
            boolean player = !row.player().isEmpty();
            int color = row.scratched() ? 0xB0B0B0 : row.player().equals(me) && player ? GREEN : player ? 0x2A5DB0 : DARK;
            graphic.drawString(this.font, String.valueOf(row.gate()), x + 10 - this.font.width(String.valueOf(row.gate())), y, GRAY, false);
            MutableComponent who = player ? Component.literal(row.player()) : this.umaName(row.uma());
            if (row.scratched())
                who.withStyle(ChatFormatting.STRIKETHROUGH);
            this.line(graphic, who, x + 13, y, 65, color);
        }
    }

    /** Her own name: there's no room for the outfit's. */
    private MutableComponent umaName(ResourceLocation uma) {
        ResourceLocation name = ClientUtils.getClientUmaDataRegistry().getOptional(uma).map(UmaData::identifier).orElse(uma);
        return UmaSoulUtils.getTranslatedUmaName(name).copy();
    }

    private void renderApplication(GuiGraphics graphic) {
        Race race = this.ticketRace();
        if (race == null) {
            int y = 27;
            for (FormattedCharSequence hint : this.font.split(Component.translatable("gui.umapyoi.meeting.hint"), 130)) {
                graphic.drawString(this.font, hint, 34, y, GRAY, false);
                y += 10;
            }
            return;
        }
        MutableComponent name = getRaceNameInRawComponent(this.menu.getMeetingTicket()).withStyle(race.ranking.color == ChatFormatting.YELLOW ? ChatFormatting.GOLD : race.ranking.color);
        this.line(graphic, name, 34, 22, 132, DARK);
        MutableComponent what = Component.empty().append(Component.translatable("race.umapyoi.tier." + race.ranking.name().toLowerCase(Locale.ROOT))).append(" ")
                .append(Component.translatable("race." + race.field.getNamespace() + ".field." + race.field.getPath()));
        this.line(graphic, what, 34, 33, 132, DARK);
        MutableComponent how = Component.empty().append(Component.translatable("race.umapyoi.surface." + race.surface.name().toLowerCase(Locale.ROOT))).append(" ");
        if (race.distance != Distance.ADAPTIVE)
            how.append(race.length + "m (").append(Component.translatable("tooltip.umapyoi.race.distance." + race.distance.name().toLowerCase(Locale.ROOT))).append(")");
        else
            how.append(Component.translatable("tooltip.umapyoi.race.distance.adaptive"));
        this.line(graphic, how, 34, 44, 132, DARK);
        this.line(graphic, this.grade(RaceMeeting.runnerLevel(race)), 34, 55, 132, GRAY);
        Component problem = this.runProblem(race);
        if (problem != null) {
            int y = 68;
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("gui.umapyoi.meeting.cannot_run", problem), 156);
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                graphic.drawString(this.font, lines.get(i), 10, y + i * 10, RED, false);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphic, final int mouseX, final int mouseY, float partialTicks) {
        super.render(graphic, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphic, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphic, int mouseX, int mouseY) {
        Component heading = this.onMeetingPage() ? Component.translatable("gui.umapyoi.meeting.title") : this.title;
        graphic.drawString(this.font, heading,
                (this.imageWidth / 2) - (this.font.width(heading.getVisualOrderText()) / 2),
                this.titleLabelY - 3, 0xFFFFFF);
        if (this.onMeetingPage()) {
            this.renderMeetingLabels(graphic);
            if (RaceMeetingClient.current() != null)
                return;
        }
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
