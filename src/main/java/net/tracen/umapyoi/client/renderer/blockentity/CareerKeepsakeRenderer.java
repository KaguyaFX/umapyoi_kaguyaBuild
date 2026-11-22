package net.tracen.umapyoi.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.tracen.umapyoi.block.CareerKeepsakeBlock;
import net.tracen.umapyoi.block.entity.CareerKeepsakeBlockEntity;
import net.tracen.umapyoi.client.renderer.KeepsakeFigure;

/** The placed career keepsake: the seated figure, facing whoever placed it. */
public class CareerKeepsakeRenderer implements BlockEntityRenderer<CareerKeepsakeBlockEntity> {

    public CareerKeepsakeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CareerKeepsakeBlockEntity keepsake, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        BlockState state = keepsake.getBlockState();
        if (!(state.getBlock() instanceof CareerKeepsakeBlock))
            return;
        float yRot = state.getValue(CareerKeepsakeBlock.FACING).toYRot();
        KeepsakeFigure.render(keepsake.getUmaName(), yRot, poseStack, buffer, light, overlay);
    }
}
