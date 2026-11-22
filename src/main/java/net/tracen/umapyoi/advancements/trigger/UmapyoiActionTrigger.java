package net.tracen.umapyoi.advancements.trigger;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * One trigger for things a player does with the mod that no vanilla trigger can see, told apart by
 * name. In an advancement: {"trigger": "umapyoi:action", "conditions": {"action": "blessing"}}.
 */
public class UmapyoiActionTrigger extends SimpleCriterionTrigger<UmapyoiActionTrigger.Instance> {
    /** Chose auto-training for a soul at the training terminal. */
    public static final String AUTO_TRAINING = "auto_training";
    /** Started a three-year career at the self-training terminal. */
    public static final String SELF_TRAINING = "self_training";
    /** Received any blessing of the Three Goddesses. */
    public static final String BLESSING = "blessing";
    /** Turned a summoning pedestal into a support one with a book. */
    public static final String SUPPORT_PEDESTAL = "support_pedestal";

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String action) {
        this.trigger(player, instance -> instance.action().equals(action));
    }

    public record Instance(Optional<ContextAwarePredicate> player, String action)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("action").forGetter(Instance::action)).apply(i, Instance::new));
    }

    /** Fires the action trigger for a player; does nothing for anyone else. */
    public static void fire(Object player, String action) {
        if (player instanceof ServerPlayer serverPlayer)
            TriggerRegistry.ACTION.get().trigger(serverPlayer, action);
    }
}
