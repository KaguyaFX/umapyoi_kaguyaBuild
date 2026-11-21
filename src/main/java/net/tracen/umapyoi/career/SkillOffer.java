package net.tracen.umapyoi.career;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/** Basic skills a player may pick in the self-training terminal; learned when the career starts. */
public final class SkillOffer {
    /** Placeholder list, to be tuned later. */
    public static final List<ResourceLocation> SKILLS = List.of(id("last_leg"), id("deep_breaths"),
            id("nutritional_supplements"), id("turf_runner"), id("mountain_climber"), id("dig_speed"));

    /**
     * The usual wisdom requirement and free-slot check are kept here but switched off: every offered
     * skill is basic. Turn this on if harder skills are ever offered.
     */
    public static final boolean CHECK_REQUIREMENTS = false;

    private SkillOffer() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, path);
    }

    public static boolean isAvailable(ItemStack soul, ResourceLocation skill) {
        if (!UmaSkillRegistry.REGISTRY.containsKey(skill))
            return false;
        if (!CHECK_REQUIREMENTS)
            return true;
        UmaSkill umaSkill = UmaSkillRegistry.REGISTRY.get(skill);
        return UmaSoulUtils.getProperty(soul).wisdom() >= umaSkill.getRequiredWisdom()
                && UmaSoulUtils.hasEmptySkillSlot(soul);
    }

    /** Adds the skill if the soul doesn't have it yet (no slot limit while CHECK_REQUIREMENTS is off). */
    public static void grant(ItemStack soul, ResourceLocation skill) {
        if (isAvailable(soul, skill) && !UmaSoulUtils.hasSkill(soul, skill))
            UmaSoulUtils.addSkill(soul, skill);
    }
}
