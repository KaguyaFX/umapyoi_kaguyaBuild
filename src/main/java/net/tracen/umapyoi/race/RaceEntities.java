package net.tracen.umapyoi.race;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tracen.umapyoi.Umapyoi;

/** The entity types of races. */
@EventBusSubscriber(modid = Umapyoi.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RaceEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Umapyoi.MODID);

    /** Never saved or summoned: only a race makes them, and takes them away again. */
    public static final DeferredHolder<EntityType<?>, EntityType<RunnerEntity>> RUNNER = ENTITY_TYPES.register("race_runner",
            () -> EntityType.Builder.<RunnerEntity>of(RunnerEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).eyeHeight(1.62F)
                    .noSave().noSummon().fireImmune().clientTrackingRange(20).updateInterval(20).setShouldReceiveVelocityUpdates(false)
                    .build("race_runner"));

    private RaceEntities() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(RUNNER.get(), LivingEntity.createLivingAttributes().build());
    }
}
