package net.tracen.umapyoi.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.world.inventory.Slot;
import net.tracen.umapyoi.item.ItemRegistry;

import com.mojang.blaze3d.platform.Lighting;

import cn.mcmod_mmf.mmlib.client.model.SimpleBedrockModel;
import cn.mcmod_mmf.mmlib.utils.ClientUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

	public ThreeGoddessScreen(ThreeGoddessContainer screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		this.leftPos = 0;
		this.topPos = 0;
		this.imageWidth = 176;
		this.imageHeight = 220;
	}
	
	@Override
	protected void renderBg(GuiGraphics graphic, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphic.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

		// Render progress bar
		int l = this.menu.getProgressionScaled();
		graphic.blit(BACKGROUND_TEXTURE, this.leftPos + 9, this.topPos + 112, 0, 220, l + 1, 5);

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
		this.renderTooltip(graphic, mouseX, mouseY);
		this.renderModels(graphic);
	}
	
	@Override
	protected void renderLabels(GuiGraphics graphic, int mouseX, int mouseY) {
		graphic.drawString(this.font, this.title,
				(this.imageWidth / 2) - (this.font.width(this.title.getVisualOrderText()) / 2), this.titleLabelY - 3,
				0xFFFFFF);
		graphic.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

		// Statue is idle because only one wish is inside: say so on the free right side of the inventory label row.
		Component idle = ThreeGoddessContainer.getIdleProblem(this.menu.inventory);
		if (idle != null) {
			graphic.drawString(this.font, idle, this.imageWidth - 8 - this.font.width(idle),
					this.imageHeight - 96 + 2, 0xB02020, false);
		}
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
		if (this.hoveredSlot == null || this.hoveredSlot.index <= 3)
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

}
