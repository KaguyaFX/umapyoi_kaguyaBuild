package net.tracen.umapyoi.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.blessing.BlessingManager;
import net.tracen.umapyoi.blessing.BlessingType;
import net.tracen.umapyoi.item.ItemRegistry;

import com.mojang.blaze3d.platform.Lighting;

import cn.mcmod_mmf.mmlib.client.model.SimpleBedrockModel;
import cn.mcmod_mmf.mmlib.utils.ClientUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.container.ThreeGoddessContainer;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.item.data.DataLocation;
import net.tracen.umapyoi.registry.umadata.UmaData;
import net.tracen.umapyoi.utils.ClientUtils;

public class ThreeGoddessScreen extends AbstractContainerScreen<ThreeGoddessContainer> {

	private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
			"textures/gui/three_goddess.png");
	/** Blessing page background (0,0 176x122) and the page arrows (180,0 four 10x10 sprites). */
	private static final ResourceLocation BLESSING_TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
			"textures/gui/three_goddess_blessing.png");

	private static final int DARK = 0x404040, GRAY = 0x8A8A8A;
	private static final int LIST_X = 8, LIST_Y = 27, LIST_W = 151, ROW_H = 20, ROWS = 3;

	public ThreeGoddessScreen(ThreeGoddessContainer screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		this.leftPos = 0;
		this.topPos = 0;
		this.imageWidth = 176;
		this.imageHeight = 220;
	}

	// ---------------------------------------------------------------- blessing page state and widgets

	private boolean choosing;
	private int scroll;
	@Nullable
	private BlessingType selected;

	private PageButton arrowButton, prayButton, chooseButton, backButton, acceptButton;
	private final PageButton[] extendButtons = new PageButton[2];

	@Override
	protected void init() {
		super.init();
		int x = this.leftPos, y = this.topPos;
		this.arrowButton = this.addRenderableWidget(new PageButton(x + 163, y + 1, 10, 10, Style.ARROW, Component::empty,
				() -> this.showPage(this.menu.getPage() == ThreeGoddessContainer.PAGE_INHERIT
						? ThreeGoddessContainer.PAGE_BLESSING : ThreeGoddessContainer.PAGE_INHERIT)));
		this.prayButton = this.addRenderableWidget(new PageButton(x + 98, y + 76, 68, 17, Style.BIG, this::prayLabel,
				() -> this.send(ThreeGoddessContainer.BUTTON_PRAY)));
		this.chooseButton = this.addRenderableWidget(new PageButton(x + 98, y + 96, 68, 12, Style.SMALL,
				() -> Component.translatable("gui.umapyoi.blessing.choose"), this::openChooser));
		for (int i = 0; i < 2; i++) {
			final int row = i;
			this.extendButtons[i] = this.addRenderableWidget(new PageButton(x + 63, y + 39 + i * 27, 22, 12, Style.SMALL,
					() -> Component.translatable("gui.umapyoi.blessing.extend"), () -> this.extendRow(row)));
		}
		this.backButton = this.addRenderableWidget(new PageButton(x + 62, y + 95, 30, 12, Style.SMALL,
				() -> Component.translatable("gui.umapyoi.blessing.back"), () -> this.setChoosing(false)));
		this.acceptButton = this.addRenderableWidget(new PageButton(x + 98, y + 92, 68, 17, Style.BIG, this::acceptLabel,
				this::acceptChoice));
		this.updateWidgets();
	}

	private void send(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null)
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
	}

	private void showPage(int page) {
		this.menu.setPage(page);
		this.send(page);
		this.setChoosing(false);
	}

	private void setChoosing(boolean choosing) {
		this.choosing = choosing;
		this.menu.hideOffering = choosing;
		this.updateWidgets();
	}

	private void openChooser() {
		this.scroll = 0;
		this.selected = null;
		this.setChoosing(true);
	}

	private void acceptChoice() {
		if (this.selected == null)
			return;
		this.send(ThreeGoddessContainer.BUTTON_CHOOSE + this.selected.ordinal());
		this.setChoosing(false);
	}

	private void extendRow(int row) {
		List<BlessingType> held = this.held();
		if (row < held.size())
			this.send(ThreeGoddessContainer.BUTTON_EXTEND + held.get(row).ordinal());
	}

	private boolean onBlessingPage() {
		return this.menu.getPage() == ThreeGoddessContainer.PAGE_BLESSING;
	}

	private List<BlessingType> held() {
		return this.minecraft == null || this.minecraft.player == null ? List.of()
				: BlessingManager.held(this.minecraft.player);
	}

	private boolean wearingSoul() {
		return this.minecraft != null && this.minecraft.player != null
				&& !UmapyoiAPI.getUmaSoul(this.minecraft.player).isEmpty();
	}

	private Component prayLabel() {
		int points = this.menu.getBlessingPoints();
		if (!this.wearingSoul())
			return Component.translatable("gui.umapyoi.blessing.need_soul");
		if (this.held().size() >= BlessingManager.MAX_HELD)
			return Component.translatable("gui.umapyoi.blessing.full");
		if (points < BlessingManager.MIN_POINTS)
			return Component.translatable("gui.umapyoi.blessing.need_points", BlessingManager.MIN_POINTS);
		return Component.translatable("gui.umapyoi.blessing.pray");
	}

	private Component acceptLabel() {
		return this.selected == null ? Component.translatable("gui.umapyoi.blessing.pick_one")
				: Component.translatable("gui.umapyoi.blessing.accept", this.selected.displayName(0));
	}

	private void updateWidgets() {
		if (this.arrowButton == null)
			return;
		boolean blessing = this.onBlessingPage();
		boolean normal = blessing && !this.choosing;
		int points = this.menu.getBlessingPoints();
		List<BlessingType> held = this.held();
		boolean canReceive = this.wearingSoul() && held.size() < BlessingManager.MAX_HELD;

		this.arrowButton.left = blessing;
		this.prayButton.visible = normal;
		this.prayButton.active = canReceive && points >= BlessingManager.MIN_POINTS;
		this.chooseButton.visible = normal && points >= BlessingManager.MAX_SPEND;
		this.chooseButton.active = canReceive;
		for (int i = 0; i < 2; i++) {
			this.extendButtons[i].visible = normal && i < held.size();
			if (i < held.size()) {
				int level = BlessingManager.level(this.minecraft.player, held.get(i));
				this.extendButtons[i].active = this.wearingSoul() && points >= BlessingType.EXTEND_COST[Math.max(0, level)];
			}
		}
		this.backButton.visible = blessing && this.choosing;
		this.acceptButton.visible = blessing && this.choosing;
		this.acceptButton.active = this.selected != null && canReceive && points >= BlessingManager.MAX_SPEND;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.updateWidgets();
	}

	// ---------------------------------------------------------------- drawing

	@Override
	protected void renderBg(GuiGraphics graphic, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphic.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
		if (!this.onBlessingPage()) {
			// Render progress bar
			int l = this.menu.getProgressionScaled();
			graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 9, this.topPos + 112, 0, 220, l + 1, 5);
			return;
		}
		int x = this.leftPos, y = this.topPos;
		graphic.blit(BLESSING_TEXTURE, x, y, 0, 0, 176, 122, 256, 128);
		int fill = Math.min(this.menu.getBlessingPoints(), BlessingManager.MAX_SPEND) * 157 / BlessingManager.MAX_SPEND;
		if (fill > 0)
			graphic.blit(BACKGROUND_TEXTURE, x + 9, y + 112, 0, 220, fill, 5);

		if (this.choosing) {
			box(graphic, x + LIST_X, y + LIST_Y, LIST_W, ROWS * ROW_H + 2, 0xFFF4F4F4, 0xFFB8B8B8);
			BlessingType[] all = BlessingType.values();
			for (int row = 0; row < ROWS; row++) {
				BlessingType type = all[this.scroll + row];
				int ry = y + LIST_Y + 1 + row * ROW_H;
				if (type == this.selected)
					box(graphic, x + LIST_X + 1, ry, LIST_W - 2, ROW_H, 0xFFFFF3C4, 0xFFE6B04A);
				graphic.blit(type.icon(), x + 11, ry + 1, 0, 0, 18, 18, 18, 18);
			}
			// scrollbar
			box(graphic, x + 160, y + LIST_Y, 7, ROWS * ROW_H + 2, 0xFFD0D0D0, 0xFFA0A0A0);
			int track = ROWS * ROW_H + 2;
			int thumb = track * ROWS / all.length;
			int thumbY = y + LIST_Y + (track - thumb) * this.scroll / (all.length - ROWS);
			box(graphic, x + 160, thumbY, 7, thumb, 0xFF8A8A8A, 0xFF5A5A5A);
			return;
		}

		graphic.blit(BACKGROUND_TEXTURE, x + 143, y + 14, 223, 45, 26, 52);
		slotFrame(graphic, x + 99, y + 29);
		List<BlessingType> held = this.held();
		for (int i = 0; i < 2; i++) {
			int ry = y + 27 + i * 27;
			box(graphic, x + 8, ry, 80, 25, 0xFFF4F4F4, 0xFFB8B8B8);
			if (i < held.size())
				graphic.blit(held.get(i).icon(), x + 11, ry + 3, 0, 0, 18, 18, 18, 18);
		}
	}

	private static void box(GuiGraphics graphic, int x, int y, int w, int h, int fill, int border) {
		graphic.fill(x, y, x + w, y + h, border);
		graphic.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
	}

	private static void slotFrame(GuiGraphics graphic, int x, int y) {
		graphic.fill(x, y, x + 18, y + 18, 0xFF373737);
		graphic.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
		graphic.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
	}

	protected void renderModels(GuiGraphics graphic) {
		ItemStack fatherFactor = this.menu.inventory.getStackInSlot(1);
		ItemStack motherFactor = this.menu.inventory.getStackInSlot(2);
        Lighting.setupForEntityInInventory();
		if (!fatherFactor.isEmpty()) {
			ResourceLocation name = fatherFactor
					.getOrDefault(DataComponentsTypeRegistry.DATA_LOCATION, new DataLocation(UmaData.DEFAULT_UMA_ID))
					.name();
			this.renderModel(graphic, this.leftPos + 33, this.topPos + 55, 25,
					new Quaternionf().rotateXYZ((float) (Math.PI / 6), (float) (-Math.PI / 4F), 0), name);
		}
		if (!motherFactor.isEmpty()) {
			ResourceLocation name = motherFactor
					.getOrDefault(DataComponentsTypeRegistry.DATA_LOCATION, new DataLocation(UmaData.DEFAULT_UMA_ID))
					.name();
			this.renderModel(graphic, this.leftPos + 142, this.topPos + 55, 25,
					new Quaternionf().rotateXYZ((float) (Math.PI / 6), (float) (Math.PI / 4F), 0), name);
		}
        Lighting.setupFor3DItems();
	}

	protected void renderModel(GuiGraphics graphic, int pPosX, int pPosY, int pScale, Quaternionf pQuaternion,
			ResourceLocation name) {
		if (!ClientUtils.getClientUmaDataRegistry().containsKey(name))
			name = UmaData.DEFAULT_UMA_ID;
		SimpleBedrockModel model = new SimpleBedrockModel(ClientUtil.getModelPOJO(name));
		ClientUtils.renderModelInInventory(graphic, pPosX, pPosY, pScale, pQuaternion, model, name);
	}

	@Override
	public void render(GuiGraphics graphic, final int mouseX, final int mouseY, float partialTicks) {
		super.render(graphic, mouseX, mouseY, partialTicks);
		if (this.onBlessingPage()) {
			this.renderBlessingTooltip(graphic, mouseX, mouseY);
		} else {
			this.renderTooltip(graphic, mouseX, mouseY);
			this.renderModels(graphic);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphic, int mouseX, int mouseY) {
		Component title = this.onBlessingPage() ? Component.translatable("gui.umapyoi.blessing.title") : this.title;
		graphic.drawString(this.font, title,
				(this.imageWidth / 2) - (this.font.width(title.getVisualOrderText()) / 2), this.titleLabelY - 3,
				0xFFFFFF);
		graphic.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

		if (this.onBlessingPage()) {
			this.renderBlessingLabels(graphic);
			return;
		}
		// Statue is idle because only one wish is inside: say so on the free right side of the inventory label row.
		Component idle = ThreeGoddessContainer.getIdleProblem(this.menu.inventory);
		if (idle != null) {
			graphic.drawString(this.font, idle, this.imageWidth - 8 - this.font.width(idle),
					this.imageHeight - 96 + 2, 0xB02020, false);
		}
	}

	private void renderBlessingLabels(GuiGraphics graphic) {
		int points = this.menu.getBlessingPoints();
		if (this.choosing) {
			graphic.drawString(this.font, Component.translatable("gui.umapyoi.blessing.choose_title"), 9, 16, DARK, false);
			BlessingType[] all = BlessingType.values();
			for (int row = 0; row < ROWS; row++) {
				BlessingType type = all[this.scroll + row];
				int ry = LIST_Y + 1 + row * ROW_H;
				graphic.drawString(this.font, type.displayName(0), 32, ry + 1, DARK, false);
				graphic.drawString(this.font, type.shortText(), 32, ry + 11, GRAY, false);
			}
			graphic.drawString(this.font, Component.translatable("gui.umapyoi.blessing.points_line", points), 9, 97,
					DARK, false);
			return;
		}
		List<BlessingType> held = this.held();
		graphic.drawString(this.font, Component.translatable("gui.umapyoi.blessing.held", held.size()), 9, 16, DARK, false);
		for (int i = 0; i < 2; i++) {
			int ry = 27 + i * 27;
			if (i < held.size()) {
				BlessingType type = held.get(i);
				MobEffectInstance instance = this.minecraft.player.getEffect(type.effect());
				graphic.drawString(this.font, type.displayName(BlessingManager.level(this.minecraft.player, type)),
						32, ry + 3, DARK, false);
				if (instance != null)
					graphic.drawString(this.font, time(instance.getDuration()), 32, ry + 14, GRAY, false);
			} else {
				Component empty = Component.translatable("gui.umapyoi.blessing.empty");
				graphic.drawString(this.font, empty, 48 - this.font.width(empty) / 2, ry + 8, GRAY, false);
			}
		}
		graphic.drawString(this.font, Component.translatable("gui.umapyoi.blessing.offering"), 99, 18, DARK, false);
		graphic.drawString(this.font, Component.translatable("gui.umapyoi.blessing.points"), 99, 52, DARK, false);
		graphic.drawString(this.font, points + " / " + BlessingManager.MAX_SPEND, 99, 62, DARK, false);
	}

	private static String time(int ticks) {
		int seconds = ticks / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}

	private List<FormattedCharSequence> blessingTooltip(BlessingType type, int level, boolean held) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(type.displayName(level).copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
		lines.addAll(this.font.split(type.flavorText().copy().withStyle(ChatFormatting.GRAY), 160));
		lines.addAll(this.font.split(type.effectText(level), 160));
		if (held)
			lines.add(Component.translatable("gui.umapyoi.blessing.extend_cost", BlessingType.EXTEND_COST[level])
					.withStyle(ChatFormatting.AQUA).getVisualOrderText());
		else
			lines.add(Component.translatable("gui.umapyoi.blessing.duration", type.durationText(level))
					.withStyle(ChatFormatting.AQUA).getVisualOrderText());
		return lines;
	}

	private void renderBlessingTooltip(GuiGraphics graphic, int mouseX, int mouseY) {
		int mx = mouseX - this.leftPos, my = mouseY - this.topPos;
		if (this.choosing) {
			int row = this.listRowAt(mx, my);
			if (row >= 0)
				graphic.renderTooltip(this.font, this.blessingTooltip(BlessingType.values()[this.scroll + row], 0, false),
						mouseX, mouseY);
			return;
		}
		List<BlessingType> held = this.held();
		for (int i = 0; i < held.size(); i++) {
			int ry = 27 + i * 27;
			if (mx >= 8 && mx < 62 && my >= ry && my < ry + 25) {
				BlessingType type = held.get(i);
				graphic.renderTooltip(this.font,
						this.blessingTooltip(type, BlessingManager.level(this.minecraft.player, type), true), mouseX, mouseY);
				return;
			}
		}
		if (this.hoveredSlot != null && this.hoveredSlot.index == ThreeGoddessContainer.OFFERING_SLOT
				&& !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
			graphic.renderTooltip(this.font, this.font.split(Component.translatable("gui.umapyoi.blessing.offering_hint"), 170),
					mouseX, mouseY);
			return;
		}
		this.renderTooltip(graphic, mouseX, mouseY);
	}

	/** Row of the chooser list under the mouse (GUI coordinates), or -1. */
	private int listRowAt(int mx, int my) {
		if (mx < LIST_X + 1 || mx >= LIST_X + LIST_W - 1 || my < LIST_Y + 1 || my >= LIST_Y + 1 + ROWS * ROW_H)
			return -1;
		return (my - LIST_Y - 1) / ROW_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.onBlessingPage() && this.choosing && button == 0) {
			int mx = (int) mouseX - this.leftPos, my = (int) mouseY - this.topPos;
			int row = this.listRowAt(mx, my);
			if (row >= 0) {
				this.selected = BlessingType.values()[this.scroll + row];
				this.updateWidgets();
				return true;
			}
			if (mx >= 160 && mx < 167 && my >= LIST_Y && my < LIST_Y + ROWS * ROW_H + 2) {
				this.scrollBy(my < LIST_Y + (ROWS * ROW_H + 2) / 2 ? -ROWS : ROWS);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.onBlessingPage() && this.choosing && scrollY != 0) {
			this.scrollBy(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void scrollBy(int rows) {
		this.scroll = Math.max(0, Math.min(BlessingType.values().length - ROWS, this.scroll + rows));
	}

	// ---- Feedback: say why an item is rejected ----

	private static final int STATUE_INPUT_SLOTS = 3;

	private static boolean isStatueInputSlot(@Nullable Slot slot) {
		return slot != null && slot.index < STATUE_INPUT_SLOTS;
	}

	@Override
	protected void renderTooltip(GuiGraphics graphic, int mouseX, int mouseY) {
		ItemStack carried = this.menu.getCarried();
		if (!carried.isEmpty() && isStatueInputSlot(this.hoveredSlot)) {
			Component problem = ThreeGoddessContainer.getPlaceProblem(this.menu.inventory,
					this.hoveredSlot.getContainerSlot(), carried);
			if (problem != null) {
				graphic.renderTooltip(this.font, problem.copy().withStyle(ChatFormatting.RED), mouseX, mouseY);
				return;
			}
		}
		super.renderTooltip(graphic, mouseX, mouseY);
	}

	/** Items in the player's inventory get a red line when shift-clicking them into the statue would fail. */
	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if (this.hoveredSlot == null || this.hoveredSlot.index <= 3 || this.onBlessingPage())
			return tooltip;
		int target = -1;
		if (stack.is(ItemRegistry.BLANK_UMA_SOUL.get())) {
			if (this.menu.inventory.getStackInSlot(0).isEmpty())
				target = 0;
		} else if (stack.is(ItemRegistry.UMA_FACTOR_ITEM.get())) {
			if (this.menu.inventory.getStackInSlot(1).isEmpty())
				target = 1;
			else if (this.menu.inventory.getStackInSlot(2).isEmpty())
				target = 2;
		}
		if (target >= 0) {
			Component problem = ThreeGoddessContainer.getPlaceProblem(this.menu.inventory, target, stack);
			if (problem != null)
				tooltip.add(problem.copy().withStyle(ChatFormatting.RED));
		}
		return tooltip;
	}

	// ---------------------------------------------------------------- buttons drawn in the statue's style

	private enum Style {
		ARROW, BIG, SMALL
	}

	private class PageButton extends AbstractButton {
		private final Style style;
		private final Supplier<Component> label;
		private final Runnable onPress;
		boolean left;

		PageButton(int x, int y, int w, int h, Style style, Supplier<Component> label, Runnable onPress) {
			super(x, y, w, h, Component.empty());
			this.style = style;
			this.label = label;
			this.onPress = onPress;
		}

		@Override
		public void onPress() {
			this.onPress.run();
		}

		@Override
		protected void renderWidget(GuiGraphics graphic, int mouseX, int mouseY, float partialTick) {
			int x = this.getX(), y = this.getY();
			boolean hover = this.isHoveredOrFocused() && this.active;
			Component text = this.label.get();
			switch (this.style) {
				case ARROW -> graphic.blit(BLESSING_TEXTURE, x, y, 180 + (this.left ? 20 : 0) + (hover ? 10 : 0), 0, 10, 10,
						256, 128);
				case BIG -> {
					graphic.blit(BACKGROUND_TEXTURE, x, y, 181, this.active ? 4 : 23, this.width, this.height);
					int color = this.active ? (hover ? 0xFFFFA0 : 0xFFFFFF) : 0xB8C8A0;
					graphic.drawString(ThreeGoddessScreen.this.font, text,
							x + (this.width - ThreeGoddessScreen.this.font.width(text)) / 2, y + 4, color, this.active);
				}
				case SMALL -> {
					int body = !this.active ? 0xFF5D7F2A : hover ? 0xFFA6E61E : 0xFF8FD40A;
					box(graphic, x, y, this.width, this.height, body, 0xFF3F7A06);
					graphic.drawString(ThreeGoddessScreen.this.font, text,
							x + (this.width - ThreeGoddessScreen.this.font.width(text)) / 2, y + 2,
							this.active ? 0xFFFFFF : 0xB8C8A0, false);
				}
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}

		@Override
		public Component getMessage() {
			return this.label.get();
		}
	}
}
