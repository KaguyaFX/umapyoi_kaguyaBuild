package net.tracen.umapyoi.client.renderer;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.race.RunnerEntity;
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
