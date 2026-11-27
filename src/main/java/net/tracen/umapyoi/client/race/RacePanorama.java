package net.tracen.umapyoi.client.race;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.race.RaceWorld;

/**
 * The far scenery around the racecourse: mountains, Fuji, the city, painted on one picture that goes
 * all the way round, as if infinitely far away. It is drawn just after the sky and before anything
 * else, so every block stands in front of it, and it moves with the camera like the sky does: Fuji
 * is always in the same direction, wherever one stands.
 * <p>
 * The picture ({@link #TEXTURE}) is 8 times as wide as it is tall. Its width is the whole way round,
 * starting from north on the left, then east, south and west. Its top is 33.75 degrees above the
 * horizon, its bottom 11.25 below: the horizon (eye level) is three quarters of the way down. What is
 * below the horizon is what one sees between the end of the ground and the mountains from up high.
 * Transparent where it's sky. It darkens with the sky and takes on the fog's colour a little. Its
 * lights, if drawn on a second picture of the same size ({@link #LIGHTS}), come on at dusk.
 */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RacePanorama {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/environment/race_panorama.png");
    /** Degrees above and below the horizon the picture's top and bottom are. */
    private static final float TOP = 33.75F, BOTTOM = 11.25F;
    private static final int AROUND = 96, BANDS = 12;
    /** How far the picture is: any distance, as nothing is in front of or behind it in depth. */
    private static final float RADIUS = 10;
    /** How much it takes the fog's colour. */
    private static final float TINT = 0.3F;
    /**
     * The same picture's lights, if there is one: transparent but for windows, lamps and the like,
     * drawn on top from dusk, at full strength once it is dark (by the sky's brightness, 1 by day).
     */
    public static final ResourceLocation LIGHTS = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/environment/race_panorama_lights.png");
    private static final float LIGHTS_FROM = 0.85F, LIGHTS_FULL = 0.4F;
    @Nullable
    private static Boolean hasLights;

    private RacePanorama() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !RaceWorld.isRaceWorld(minecraft.level) || event.getCamera().getFluidInCamera() != FogType.NONE)
            return;
        float light = minecraft.level.getSkyDarken(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        float[] fog = RenderSystem.getShaderFogColor();
        float most = Math.max(1e-3F, Math.max(fog[0], Math.max(fog[1], fog[2])));
        float r = light * (1 - TINT + TINT * fog[0] / most);
        float g = light * (1 - TINT + TINT * fog[1] / most);
        float b = light * (1 - TINT + TINT * fog[2] / most);

        // Only the camera's turn: the picture is centred on the eye.
        Matrix4f m = new Matrix4f(event.getModelViewMatrix());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        draw(m, TEXTURE, r, g, b, 1);
        // The lights come on from dusk, as bright as they are drawn, added on top.
        float dark = Math.clamp((LIGHTS_FROM - light) / (LIGHTS_FROM - LIGHTS_FULL), 0, 1);
        if (dark > 0 && hasLights()) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            draw(m, LIGHTS, 1, 1, 1, dark);
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void draw(Matrix4f m, ResourceLocation texture, float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder to = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < AROUND; i++) {
            float u0 = (float) i / AROUND, u1 = (float) (i + 1) / AROUND;
            for (int j = 0; j < BANDS; j++) {
                float v0 = (float) j / BANDS, v1 = (float) (j + 1) / BANDS;
                vertex(to, m, u0, v0, r, g, b, a);
                vertex(to, m, u0, v1, r, g, b, a);
                vertex(to, m, u1, v1, r, g, b, a);
                vertex(to, m, u1, v0, r, g, b, a);
            }
        }
        BufferUploader.drawWithShader(to.buildOrThrow());
    }

    /** Whether there's a picture of the lights; looked up again when resources are reloaded. */
    private static boolean hasLights() {
        if (hasLights == null)
            hasLights = Minecraft.getInstance().getResourceManager().getResource(LIGHTS).isPresent();
        return hasLights;
    }

    @EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        private Reload() {
        }

        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> hasLights = null);
        }
    }

    /**
     * A point of the picture: across from north round by east, down from its top. Rows are evenly
     * spaced in angle, so a mountain is as tall in the game as it's drawn.
     */
    private static void vertex(BufferBuilder to, Matrix4f m, float u, float v, float r, float g, float b, float a) {
        double bearing = u * Math.PI * 2;
        double elevation = Math.toRadians(TOP - v * (TOP + BOTTOM));
        float x = (float) (Math.sin(bearing) * RADIUS);
        float z = (float) (-Math.cos(bearing) * RADIUS);
        float y = (float) (Math.tan(elevation) * RADIUS);
        to.addVertex(m, x, y, z).setUv(u, v).setColor(r, g, b, a);
    }
}
