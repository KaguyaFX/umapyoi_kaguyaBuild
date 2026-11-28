package net.tracen.umapyoi.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.registry.skills.SkillRarity;
import net.tracen.umapyoi.registry.skills.UmaSkill;

/**
 * A skill's frame by its rarity: the plate, the icon with its border, the fire while it runs, and the smaller
 * plate on the phone. A unique skill's plate has light flowing through it, in step with its fire.
 */
public final class SkillTiers {
    /**
     * 256 x 256: plates of 96 x 20 at (0, row * 20), the four rarities then the same greyed; icons of 16 x 16
     * at (column * 16, 160 + rarity * 16), buff, hinder, heal, passive, unique, then the same greyed; the unique
     * plate's light, 12 frames at (160, frame * 20).
     */
    public static final ResourceLocation SHEET = texture("skill_tiers");
    public static final int SHEET_SIZE = 256;
    /** The fire alone, 12 frames of 112 x 36; the plate sits at (8, 12) in each. Gold for plain and upper skills. */
    private static final ResourceLocation[] FIRE = { texture("skill_flame_gold"), texture("skill_flame_gold"), texture("skill_flame_pink"),
            texture("skill_flame_rainbow") };
    private static final int FIRE_FRAMES = 12, FIRE_W = 112, FIRE_H = 36;
    private static final int FLOW_FRAMES = 12;
    /** 128 x 192: plates of 61 x 11 at (0, rarity * 11); the unique plate's light, 16 frames at (64, frame * 11). */
    private static final ResourceLocation PHONE = texture("phone_skill_tiers");
    private static final int PHONE_FLOW_FRAMES = 16;

    private SkillTiers() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "textures/gui/" + name + ".png");
    }

    /** Light and fire move on a frame every tenth of a second, together. */
    private static int frame(int frames) {
        return (int) (Util.getMillis() / 100 % frames);
    }

    public static int plateU(SkillRarity rarity, boolean dim) {
        return rarity == SkillRarity.UNIQUE && !dim ? 160 : 0;
    }

    public static int plateV(SkillRarity rarity, boolean dim) {
        if (rarity == SkillRarity.UNIQUE && !dim)
            return frame(FLOW_FRAMES) * 20;
        return (rarity.ordinal() + (dim ? 4 : 0)) * 20;
    }

    /** A unique skill has the unique icon whatever it does (only a buff one is drawn so far). */
    public static int iconU(UmaSkill skill, boolean dim) {
        int column = skill.getRarity() == SkillRarity.UNIQUE ? 4 : switch (skill.getType()) {
            case BUFF -> 0;
            case HINDER -> 1;
            case HEAL -> 2;
            case PASSIVE -> 3;
        };
        return (column + (dim ? 5 : 0)) * 16;
    }

    public static int iconV(UmaSkill skill) {
        return 160 + skill.getRarity().ordinal() * 16;
    }

    /** The 96 x 20 plate. */
    public static void plate(GuiGraphics graphics, SkillRarity rarity, int x, int y, boolean dim) {
        graphics.blit(SHEET, x, y, plateU(rarity, dim), plateV(rarity, dim), 96, 20, SHEET_SIZE, SHEET_SIZE);
    }

    /** The icon, drawn {@code size} pixels square. */
    public static void icon(GuiGraphics graphics, UmaSkill skill, int x, int y, int size, boolean dim) {
        graphics.blit(SHEET, x, y, size, size, iconU(skill, dim), iconV(skill), 16, 16, SHEET_SIZE, SHEET_SIZE);
    }

    /** The fire round a plate at (x, y), dying down with {@code strength} (1 = full). */
    public static void fire(GuiGraphics graphics, SkillRarity rarity, int x, int y, float strength) {
        RenderSystem.enableBlend();
        graphics.setColor(1F, 1F, 1F, strength);
        graphics.blit(FIRE[rarity.ordinal()], x - 8, y - 12, 0, frame(FIRE_FRAMES) * FIRE_H, FIRE_W, FIRE_H, FIRE_W, FIRE_H * FIRE_FRAMES);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    /** The phone's 61 x 11 plate. */
    public static void phonePlate(GuiGraphics graphics, SkillRarity rarity, int x, int y) {
        boolean flowing = rarity == SkillRarity.UNIQUE;
        graphics.blit(PHONE, x, y, flowing ? 64 : 0, flowing ? frame(PHONE_FLOW_FRAMES) * 11 : rarity.ordinal() * 11, 61, 11, 128, 192);
    }
}
