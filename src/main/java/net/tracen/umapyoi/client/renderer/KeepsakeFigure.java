package net.tracen.umapyoi.client.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import cn.mcmod_mmf.mmlib.client.model.SimpleBedrockModel;
import cn.mcmod_mmf.mmlib.client.model.bedrock.BedrockPart;
import cn.mcmod_mmf.mmlib.utils.ClientUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.utils.ClientUtils;

/**
 * The career keepsake figure: an Umamusume's own model, half size, sitting on the ground with her
 * legs out in front (a player's riding pose, without the horse). Drawn by the placed block and by
 * the item, in a 1x1x1 block space. One model per Umamusume is kept, so many different keepsakes
 * in view don't reload models every frame.
 */
public final class KeepsakeFigure {
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

    private static final Map<ResourceLocation, SimpleBedrockModel> MODELS = new HashMap<>();

    private KeepsakeFigure() {
    }

    /**
     * Draws the figure in the block space at the pose stack's origin, turned {@code yRotDegrees}
     * (0 = facing south).
     */
    public static void render(ResourceLocation target, float yRotDegrees, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        var pojo = ClientUtil.getModelPOJO(target);
        SimpleBedrockModel model = MODELS.computeIfAbsent(target, key -> new SimpleBedrockModel());
        if (model.needRefresh(pojo))
            model.loadModel(pojo);
        pose(model);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YN.rotationDegrees(yRotDegrees));
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
    private static void pose(SimpleBedrockModel model) {
        part(model, "head").ifPresent(p -> {
            p.xRot = 0;
            p.yRot = 0;
            p.zRot = 0;
        });
        part(model, "right_leg").ifPresent(p -> {
            p.xRot = LEG_FORWARD;
            p.yRot = LEG_SPREAD;
            p.zRot = LEG_TILT;
        });
        part(model, "left_leg").ifPresent(p -> {
            p.xRot = LEG_FORWARD;
            p.yRot = -LEG_SPREAD;
            p.zRot = -LEG_TILT;
        });
        part(model, "right_arm").ifPresent(p -> {
            p.xRot = ARM_FORWARD;
            p.yRot = 0;
            p.zRot = ARM_OUT;
        });
        part(model, "left_arm").ifPresent(p -> {
            p.xRot = ARM_FORWARD;
            p.yRot = 0;
            p.zRot = -ARM_OUT;
        });
    }

    private static Optional<BedrockPart> part(SimpleBedrockModel model, String name) {
        return Optional.ofNullable(model.getChild(name));
    }
}
