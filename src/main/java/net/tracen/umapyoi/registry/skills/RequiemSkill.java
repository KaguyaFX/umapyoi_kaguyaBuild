package net.tracen.umapyoi.registry.skills;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.api.TargetSelector;

/** Dominator's evolution, outside a race: everything living within 35 blocks slowed hard (Slowness III) for a minute. */
public class RequiemSkill extends UmaSkill {
    private static final double RANGE = 35;
    private static final int TICKS = 60 * 20;

    public RequiemSkill(Builder builder) {
        super(builder);
    }

    @Override
    public void applySkill(Level level, LivingEntity user) {
        List<Entity> found = TargetSelector.getTargettableEntitiesWithinAABB(level, user, user.getBoundingBox().inflate(RANGE),
                TargetSelector.getResolvedReach(user) + RANGE + 7);
        for (Entity entity : found)
            if (entity instanceof LivingEntity living)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TICKS, 2));
    }
}
