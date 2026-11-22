package net.tracen.umapyoi.client.renderer.blockentity;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import cn.mcmod_mmf.mmlib.client.model.SimpleBedrockModel;
import cn.mcmod_mmf.mmlib.client.model.bedrock.BedrockPart;
import cn.mcmod_mmf.mmlib.utils.ClientUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.tracen.umapyoi.block.CareerKeepsakeBlock;
import net.tracen.umapyoi.block.entity.CareerKeepsakeBlockEntity;
import net.tracen.umapyoi.utils.ClientUtils;

/**
 * Draws the career keepsake: the Umamusume's own model, half size, sitting on the ground with her
 * legs out in front (the same pose as a player riding, without the horse).
 */
public class CareerKeepsakeRenderer implements BlockEntityRenderer<CareerKeepsakeBlockEntity> {
    /** Figure size against a full-size Umamusume. */
    private static final float SCALE = 0.5F;
    /**
     * Height of the model origin above the floor, in model blocks: 1.5 is standing; the hips (12px up)
     * come down to just above the floor when sitting.
     */
    private static final float SEAT_HEIGHT = 0.875F;
    private static final float LEG_FORWARD = (float) Math.toRadians(-81);
    private static final float LEG_SPREAD = (float) Math.toRadians(18);
    private static final float LEG_TILT = (float) Math.toRadians(4.5);
    private static final float ARM_FORWARD = (float) Math.toRadians(-36);
    private static final float ARM_OUT = (float) Math.toRadians(5);

    private final SimpleBedrockModel model = new SimpleBedrockModel();

    public CareerKeepsakeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CareerKeepsakeBlockEntity keepsake, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        BlockState state = keepsake.getBlockState();
        if (!(state.getBlock() instanceof CareerKeepsakeBlock))
            return;
        Direction facing = state.getValue(CareerKeepsakeBlock.FACING);
        ResourceLocation target = keepsake.getUmaName();

        var pojo = ClientUtil.getModelPOJO(target);
        if (model.needRefresh(pojo))
            model.loadModel(pojo);
        pose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YN.rotationDegrees(facing.toYRot()));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(0.0D, SEAT_HEIGHT, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(ClientUtils.getTexture(target)));
        model.renderToBuffer(poseStack, vertexConsumer, light, overlay);
        if (model.isEmissive()) {
            VertexConsumer emissive = buffer
                    .getBuffer(RenderType.entityTranslucentEmissive(ClientUtils.getEmissiveTexture(target)));
            model.renderEmissiveParts(poseStack, emissive, light, overlay, -1);
        }
        poseStack.popPose();
    }

    /** Sitting: legs forward and a little apart, hands resting toward the knees. */
    private void pose() {
        part("head").ifPresent(p -> {
            p.xRot = 0;
            p.yRot = 0;
            p.zRot = 0;
        });
        part("right_leg").ifPresent(p -> {
            p.xRot = LEG_FORWARD;
            p.yRot = LEG_SPREAD;
            p.zRot = LEG_TILT;
        });
        part("left_leg").ifPresent(p -> {
            p.xRot = LEG_FORWARD;
            p.yRot = -LEG_SPREAD;
            p.zRot = -LEG_TILT;
        });
        part("right_arm").ifPresent(p -> {
            p.xRot = ARM_FORWARD;
            p.yRot = 0;
            p.zRot = ARM_OUT;
        });
        part("left_arm").ifPresent(p -> {
            p.xRot = ARM_FORWARD;
            p.yRot = 0;
            p.zRot = -ARM_OUT;
        });
    }

    private Optional<BedrockPart> part(String name) {
        return Optional.ofNullable(model.getChild(name));
    }
}
