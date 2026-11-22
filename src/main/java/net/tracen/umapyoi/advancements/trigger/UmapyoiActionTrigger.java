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
    /** Wearing any soul. */
    public static final String WEAR_SOUL = "wear_soul";
    /** Wearing any outfit in the outfit slot. */
    public static final String WEAR_COSTUME = "wear_costume";
    /** Tried to mount a horse while an Umamusume (and couldn't). */
    public static final String TRY_RIDE_HORSE = "try_ride_horse";
    /** Riding a horse while an Umamusume anyway: mounted first, put the soul on after. */
    public static final String RIDE_HORSE_AS_UMA = "ride_horse_as_uma";
    /** A helmet lowered motivation. */
    public static final String HELMET_EARS = "helmet_ears";
    /** A liked food raised motivation. */
    public static final String FAVORITE_FOOD = "favorite_food";
    /** Fright from an Uma trait. */
    public static final String FRIGHT = "fright";
    /** Motivation at its worst. */
    public static final String MOOD_BAD = "mood_bad";
    /** Motivation at its best (Hyper Mood doesn't count). */
    public static final String MOOD_PERFECT = "mood_perfect";
    /** Sorrow III kept a player from dying. */
    public static final String SORROW_SAVE = "sorrow_save";
    /** Learned a skill at the skill learning table or through a three-year career (not auto-training). */
    public static final String LEARN_SKILL = "learn_skill";
    /** Finished a three-year career. */
    public static final String FINISH_CAREER = "finish_career";

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
