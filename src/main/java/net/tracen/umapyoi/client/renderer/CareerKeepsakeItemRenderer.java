package net.tracen.umapyoi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.item.data.DataLocation;
import net.tracen.umapyoi.registry.umadata.UmaData;

/**
 * The career keepsake item: shows the same seated figure as the placed block, of whichever
 * Umamusume the item remembers. The view angles per context are in the item model's display.
 */
public class CareerKeepsakeItemRenderer extends BlockEntityWithoutLevelRenderer {

    public CareerKeepsakeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        DataLocation location = stack.get(DataComponentsTypeRegistry.DATA_LOCATION);
        ResourceLocation target = location == null ? UmaData.DEFAULT_UMA_ID : location.name();
        KeepsakeFigure.render(target, 0F, poseStack, buffer, light, overlay);
    }
}
