package net.tracen.umapyoi.registry.skills;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * A skill that only does something in a race (its race version is in RaceSkills); outside one it can't be
 * used at all, so it costs nothing there.
 */
public class RaceOnlySkill extends UmaSkill {
    public RaceOnlySkill(Builder builder) {
        super(builder);
    }

    @Override
    public void applySkill(Level level, LivingEntity user) {
    }
}
