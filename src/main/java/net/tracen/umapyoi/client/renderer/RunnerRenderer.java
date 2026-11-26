package net.tracen.umapyoi.client.renderer;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.client.SkillOverlay;
import net.tracen.umapyoi.race.RunnerEntity;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Draws a computer runner as her Umamusume, the way a player wearing the soul is drawn: the player
 * model only supplies the pose and isn't drawn itself.
 */
public class RunnerRenderer extends LivingEntityRenderer<RunnerEntity, PlayerModel<RunnerEntity>> {
    public RunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new UmaLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RunnerEntity entity) {
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Nullable
    @Override
    protected RenderType getRenderType(RunnerEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return null;
    }

    @Override
    public void render(RunnerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, light);
        ResourceLocation used = entity.skillShown();
        UmaSkill skill = used == null ? null : UmaSkillRegistry.REGISTRY.get(used);
        if (skill != null)
            renderSkill(entity, skill, poseStack, buffer);
    }

    /** The skill she just used, over her head as on the HUD: its frame, icon and name, facing the camera, fading at the end. */
    private void renderSkill(RunnerEntity entity, UmaSkill skill, PoseStack poseStack, MultiBufferSource buffer) {
        float alpha = Math.min(1F, entity.skillTicksLeft() / 10F);
        if (alpha <= 0.05F)
            return;
        int light = LightTexture.FULL_BRIGHT;
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() + 0.95, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer sheet = buffer.getBuffer(RenderType.text(SkillOverlay.HUD));
        quad(sheet, pose, -48, -10, 96, 20, 0, 0, alpha, light);
        int icon = switch (skill.getType()) {
            case BUFF -> 0;
            case HINDER -> 16;
            case HEAL -> 32;
            case PASSIVE -> 48;
        };
        quad(sheet, pose, -45, -8, 16, 16, icon, 48, alpha, light);
        int color = (Math.max(4, (int) (alpha * 255)) << 24) | 0x794016;
        getFont().drawInBatch(skill.getDescription(), -26, -4, color, false, pose, buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
    }

    /** A piece of the skill sheet (128 x 64), in the order the font draws its glyphs, so it faces the same way. */
    private static void quad(VertexConsumer to, Matrix4f pose, float x, float y, float w, float h, float u, float v, float alpha, int light) {
        float u0 = u / 128F, v0 = v / 64F, u1 = (u + w) / 128F, v1 = (v + h) / 64F;
        to.addVertex(pose, x, y, 0).setColor(1F, 1F, 1F, alpha).setUv(u0, v0).setLight(light);
        to.addVertex(pose, x, y + h, 0).setColor(1F, 1F, 1F, alpha).setUv(u0, v1).setLight(light);
        to.addVertex(pose, x + w, y + h, 0).setColor(1F, 1F, 1F, alpha).setUv(u1, v1).setLight(light);
        to.addVertex(pose, x + w, y, 0).setColor(1F, 1F, 1F, alpha).setUv(u1, v0).setLight(light);
    }

    /** Not on a figure that is only looked at in a menu. */
    @Override
    protected boolean shouldShowName(RunnerEntity entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }

    private static class UmaLayer extends RenderLayer<RunnerEntity, PlayerModel<RunnerEntity>> {
        /** One per Umamusume: a renderer reloads its model whenever it draws someone else. */
        private final Map<ResourceLocation, UmaSoulRenderer> renderers = new HashMap<>();

        private final RenderLayerParent<RunnerEntity, PlayerModel<RunnerEntity>> parent;

        UmaLayer(RenderLayerParent<RunnerEntity, PlayerModel<RunnerEntity>> parent) {
            super(parent);
            this.parent = parent;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, RunnerEntity entity, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ItemStack soul = entity.soul();
            if (!UmaSoulUtils.hasSoulData(soul))
                return;
            renderers.computeIfAbsent(UmaSoulUtils.getName(soul), name -> new UmaSoulRenderer())
                    .render(soul, new SlotContext("uma_soul", entity, 0, false, true), poseStack, parent, buffer, light,
                            limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }
}
