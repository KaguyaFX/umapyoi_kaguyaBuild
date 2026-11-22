package net.tracen.umapyoi.trait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.blessing.BlessingManager;
import net.tracen.umapyoi.blessing.BlessingType;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import net.tracen.umapyoi.advancements.trigger.UmapyoiActionTrigger;
import net.tracen.umapyoi.registry.umadata.Motivations;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * What eating does to an Umamusume (design doc 8.6, 12.2): the soul's liked and disliked foods,
 * carrots, raw meat and a sensitive stomach. The fixed mood foods (Hachimi drinks, cupcakes, golden
 * carrots, bitter things) use the motivation tags in CommonEvents. 生存 turns food aversions off:
 * disliked foods and raw meat.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class FoodEvents {

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null)
            return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (soul.isEmpty())
            return;
        RandomSource random = player.getRandom();
        boolean aversionsOff = BlessingManager.level(player, BlessingType.SURVIVAL) >= 0;
        UmaTraits traits = UmaTraits.of(soul);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (traits.likes().contains(id)) {
            Motivations before = UmaSoulUtils.getMotivation(soul);
            UmaStatusUtils.changeMotivation(player, 1);
            if (UmaSoulUtils.getMotivation(soul).ordinal() < before.ordinal())
                UmapyoiActionTrigger.fire(player, UmapyoiActionTrigger.FAVORITE_FOOD);
        } else if (traits.dislikes().contains(id) && !aversionsOff) {
            if (PersonalityEffects.has(soul, PersonalityTrait.PICKY_EATER)
                    || random.nextDouble() < UmapyoiConfig.DISLIKED_FOOD_MOOD_CHANCE.get())
                UmaStatusUtils.changeMotivation(player, -1);
        }

        if (stack.is(UmapyoiItemTags.CARROT_MOOD) && random.nextDouble() < UmapyoiConfig.CARROT_MOOD_CHANCE.get())
            UmaStatusUtils.changeMotivation(player, 1);

        if (stack.is(Tags.Items.FOODS_RAW_MEAT) && !aversionsOff
                && random.nextDouble() < UmapyoiConfig.RAW_MEAT_MOOD_CHANCE.get())
            UmaStatusUtils.changeMotivation(player, -1);

        sensitiveStomach(player, food);
    }

    /** The harmful effects this food just gave last longer. */
    private static void sensitiveStomach(ServerPlayer player, FoodProperties food) {
        double multiplier = UmapyoiConfig.SENSITIVE_STOMACH_MULTIPLIER.get();
        for (FoodProperties.PossibleEffect possible : food.effects()) {
            MobEffectInstance fromFood = possible.effect();
            if (fromFood.getEffect().value().getCategory() != MobEffectCategory.HARMFUL)
                continue;
            MobEffectInstance current = player.getEffect(fromFood.getEffect());
            if (current == null || current.isInfiniteDuration() || current.getAmplifier() != fromFood.getAmplifier())
                continue;
            int longer = (int) (fromFood.getDuration() * multiplier);
            if (current.getDuration() < longer)
                player.addEffect(new MobEffectInstance(fromFood.getEffect(), longer, fromFood.getAmplifier(),
                        fromFood.isAmbient(), fromFood.isVisible(), fromFood.showIcon()));
        }
    }
}
