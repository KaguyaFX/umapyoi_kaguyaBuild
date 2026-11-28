package net.tracen.umapyoi.client.race;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.client.SkillTiers;
import net.tracen.umapyoi.race.RaceMeeting;
import net.tracen.umapyoi.race.RaceMeetingClient;
import net.tracen.umapyoi.race.RaceMeetingPayload;
import net.tracen.umapyoi.race.RacePlayerSkillPayload;
import net.tracen.umapyoi.race.RaceWorld;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.registry.umadata.Motivations;

/**
 * What shows over runners' heads in the race world, players' and computer runners' alike: before the
 * race, the stall's number on its bracket's colour and the runner's mood, for those looking at them in
 * the paddock; during it, a skill just used, as on the HUD.
 */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RaceHeads {
    /** Over the head, above the name. */
    public static final float TAG_ABOVE = 0.8F, SKILL_ABOVE = 0.95F, PLAYER_SKILL_ABOVE = 1.1F;
    private static final float SCALE = 0.025F;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    private RaceHeads() {
    }

    /** Whether the numbers and moods show: in the race world, before the race. */
    public static boolean tagsShown() {
        RaceMeetingPayload held = RaceMeetingClient.current();
        Minecraft minecraft = Minecraft.getInstance();
        return held != null && minecraft.level != null && RaceWorld.isRaceWorld(minecraft.level)
                && held.stageNow().ordinal() < RaceMeeting.Stage.RUNNING.ordinal();
    }

    /** How many run, for the brackets' colours. */
    public static int field() {
        RaceMeetingPayload held = RaceMeetingClient.current();
        return held == null ? 0 : held.rows().size();
    }

    /** A stall's number on its bracket's colour and a mood's arrow, side by side, facing the camera. */
    public static void tag(PoseStack poseStack, MultiBufferSource buffer, Font font, Quaternionf camera, float height, int gate, int field,
            Motivations mood) {
        poseStack.pushPose();
        poseStack.translate(0, height, 0);
        poseStack.mulPose(camera);
        poseStack.scale(SCALE, -SCALE, SCALE);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer to = buffer.getBuffer(RenderType.textBackground());
        String number = String.valueOf(gate);
        int chipW = font.width(number) + 5, total = chipW + 2 + 9;
        int x = -total / 2, y = -5;
        rect(to, pose, x, y, chipW, 9, RaceArt.bracketColor(gate, field));
        int mx = x + chipW + 2;
        int moodColor = RaceArt.moodColor(mood);
        for (int dy = 0; dy < 9; dy++)
            for (int dx = 0; dx < 9; dx++) {
                boolean corner = (dx == 0 || dx == 8) && (dy == 0 || dy == 8);
                if (corner)
                    continue;
                boolean arrow = dx >= 2 && dx < 7 && dy >= 2 && dy < 7 && RaceArt.moodArrow(mood, dx - 2, dy - 2);
                rect(to, pose, mx + dx, y + dy, 1, 1, arrow ? 0xFFFFFFFF : moodColor);
            }
        font.drawInBatch(number, x + 3, y + 1, RaceArt.bracketTextColor(gate, field), false, pose, buffer, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
        poseStack.popPose();
    }

    private static void rect(VertexConsumer to, Matrix4f pose, float x, float y, float w, float h, int argb) {
        to.addVertex(pose, x, y, 0).setColor(argb).setLight(LIGHT);
        to.addVertex(pose, x, y + h, 0).setColor(argb).setLight(LIGHT);
        to.addVertex(pose, x + w, y + h, 0).setColor(argb).setLight(LIGHT);
        to.addVertex(pose, x + w, y, 0).setColor(argb).setLight(LIGHT);
    }

    /** A skill just used, as on the HUD: its frame, icon and name, facing the camera, fading at the end. */
    public static void skill(PoseStack poseStack, MultiBufferSource buffer, Font font, Quaternionf camera, float height, UmaSkill skill,
            int ticksLeft) {
        float alpha = Math.min(1F, ticksLeft / 10F);
        if (alpha <= 0.05F)
            return;
        poseStack.pushPose();
        poseStack.translate(0, height, 0);
        poseStack.mulPose(camera);
        poseStack.scale(SCALE, -SCALE, SCALE);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer sheet = buffer.getBuffer(RenderType.text(SkillTiers.SHEET));
        quad(sheet, pose, -48, -10, 0, 96, 20, SkillTiers.plateU(skill.getRarity(), false), SkillTiers.plateV(skill.getRarity(), false), alpha);
        // A hair nearer than the plate: the quads are sorted by distance, and the plate mustn't cover it.
        quad(sheet, pose, -45, -8, 0.1F, 16, 16, SkillTiers.iconU(skill, false), SkillTiers.iconV(skill), alpha);
        int color = (Math.max(4, (int) (alpha * 255)) << 24) | 0x794016;
        font.drawInBatch(skill.getDescription(), -26, -4, color, false, pose, buffer, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
        poseStack.popPose();
    }

    /** A piece of the skill sheet, in the order the font draws its glyphs, so it faces the same way. */
    private static void quad(VertexConsumer to, Matrix4f pose, float x, float y, float z, float w, float h, float u, float v, float alpha) {
        float size = SkillTiers.SHEET_SIZE;
        float u0 = u / size, v0 = v / size, u1 = (u + w) / size, v1 = (v + h) / size;
        to.addVertex(pose, x, y, z).setColor(1F, 1F, 1F, alpha).setUv(u0, v0).setLight(LIGHT);
        to.addVertex(pose, x, y + h, z).setColor(1F, 1F, 1F, alpha).setUv(u0, v1).setLight(LIGHT);
        to.addVertex(pose, x + w, y + h, z).setColor(1F, 1F, 1F, alpha).setUv(u1, v1).setLight(LIGHT);
        to.addVertex(pose, x + w, y, z).setColor(1F, 1F, 1F, alpha).setUv(u1, v0).setLight(LIGHT);
    }

    /** Players who run: their number and mood before the race, and the skills they use to everyone else. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !RaceWorld.isRaceWorld(minecraft.level))
            return;
        Font font = minecraft.font;
        Quaternionf camera = minecraft.getEntityRenderDispatcher().cameraOrientation();
        PoseStack pose = event.getPoseStack();
        if (tagsShown()) {
            RaceMeetingPayload.Row row = row(player);
            if (row != null)
                tag(pose, event.getMultiBufferSource(), font, camera, player.getBbHeight() + TAG_ABOVE, row.gate(), field(), row.form().motivation());
        }
        if (player == minecraft.player)
            return;
        ResourceLocation used = RacePlayerSkillPayload.shown(player.getUUID());
        UmaSkill skill = used == null ? null : UmaSkillRegistry.REGISTRY.get(used);
        if (skill != null)
            skill(pose, event.getMultiBufferSource(), font, camera, player.getBbHeight() + PLAYER_SKILL_ABOVE, skill,
                    RacePlayerSkillPayload.ticksLeft(player.getUUID()));
    }

    /** The race's row for a player who runs in it. */
    @Nullable
    private static RaceMeetingPayload.Row row(Player player) {
        RaceMeetingPayload held = RaceMeetingClient.current();
        if (held == null)
            return null;
        String name = player.getScoreboardName();
        for (RaceMeetingPayload.Row row : held.rows())
            if (!row.scratched() && name.equals(row.player()))
                return row;
        return null;
    }
}
