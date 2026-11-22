package net.tracen.umapyoi.career.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerAttachments;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.data.tag.UmapyoiItemTags;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.trait.PersonalityEffects;
import net.tracen.umapyoi.trait.UmaTraits;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/** Watches for what completes (or rules out) an event in progress. */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class CareerEventHooks {
    private static final Set<EntityType<?>> HORSES = Set.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE);

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        if (stack.getItem().getFoodProperties(stack, player) == null)
            return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        if (!CareerManager.isInCareer(soul) || data == null)
            return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<CareerEventType> done = new ArrayList<>();
        for (CareerEventData.Active active : data.active()) {
            CareerEventType type = CareerEventType.byId(active.id()).orElse(null);
            if (type == null)
                continue;
            boolean ate = switch (type) {
                case HUNGRY -> true;
                case WANT_CARROT -> stack.is(UmapyoiItemTags.CARROT_FOODS);
                case WANT_HACHIMI -> stack.is(ItemRegistry.HACHIMI_MID.get()) || stack.is(ItemRegistry.HACHIMI_BIG.get());
                case WANT_FOOD -> stack.is(CareerEventType.foodOf(active.data()));
                case WANT_GOLDEN_CARROT -> stack.is(Items.GOLDEN_CARROT);
                case WANT_SOUP -> stack.is(Tags.Items.FOODS_SOUP);
                // a liked food or a mood food ends the lazy period early
                case LAZY_PERIOD -> UmaTraits.of(soul).likes().contains(id)
                        || stack.is(UmapyoiItemTags.getMotivationFoodTag(1))
                        || stack.is(UmapyoiItemTags.getMotivationFoodTag(2));
                default -> false;
            };
            if (ate)
                done.add(type);
        }
        done.forEach(type -> CareerEventManager.complete(player, type));
    }

    /** 想打一架: defeating any hostile mob. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() instanceof Enemy
                && CareerEventManager.isActive(player, CareerEventType.WANT_FIGHT))
            CareerEventManager.complete(player, CareerEventType.WANT_FIGHT);
    }

    /** Once a second, every event in progress that depends on where the player is or what they do. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        EventProgress progress = player.getData(CareerAttachments.EVENT_PROGRESS);
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        CareerEventData data = soul.get(DataComponentsTypeRegistry.CAREER_EVENTS);
        int xpLevel = player.experienceLevel;
        boolean spentLevels = progress.lastXpLevel >= 0 && xpLevel < progress.lastXpLevel;
        progress.lastXpLevel = xpLevel;
        if (!CareerManager.isInCareer(soul) || data == null) {
            resetStreaks(progress, Set.of());
            return;
        }

        Set<CareerEventType> present = new HashSet<>();
        List<CareerEventType> done = new ArrayList<>();
        List<CareerEventType> missed = new ArrayList<>();
        Level level = player.level();
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        int stay = UmapyoiConfig.EVENT_STAY_SECONDS.get();
        for (CareerEventData.Active active : data.active()) {
            CareerEventType type = CareerEventType.byId(active.id()).orElse(null);
            if (type == null)
                continue;
            present.add(type);
            switch (type) {
                case WANT_TO_RUN -> {
                    progress.sprintSeconds = player.isSprinting() ? progress.sprintSeconds + 1 : 0;
                    if (progress.sprintSeconds >= UmapyoiConfig.EVENT_RUN_SECONDS.get())
                        done.add(type);
                }
                case CHANGE_OUTFIT -> {
                    String outfit = CareerEventType.outfitOf(player);
                    if (!outfit.isEmpty() && !outfit.equals(active.data()))
                        done.add(type);
                }
                case OTHER_BIOME -> {
                    String biome = CareerEventType.biomeOf(player);
                    if (!biome.isEmpty() && !biome.equals(active.data()))
                        done.add(type);
                }
                case WATCH_SUNRISE, WATCH_SUNSET -> {
                    CareerEventType.SkyWindow window = type == CareerEventType.WATCH_SUNRISE
                            ? CareerEventType.SkyWindow.SUNRISE : CareerEventType.SkyWindow.SUNSET;
                    if (level.getDayTime() >= parseLong(active.data()))
                        missed.add(type);
                    else if (window.isNow(level) && level.canSeeSky(eyes))
                        done.add(type);
                }
                case WANT_SEE_HORSE -> {
                    progress.horseSeconds = nearHorse(player) ? progress.horseSeconds + 1 : 0;
                    if (progress.horseSeconds >= UmapyoiConfig.EVENT_HORSE_SECONDS.get())
                        done.add(type);
                }
                case WANT_GO_HOME -> {
                    if (CareerEventType.isHome(player))
                        done.add(type);
                }
                case WANT_COMPANY -> {
                    progress.companySeconds = PersonalityEffects.hasCompanion(player) ? progress.companySeconds + 1 : 0;
                    if (progress.companySeconds >= stay)
                        done.add(type);
                }
                case WANT_LIGHT -> {
                    boolean bright = CareerEventType.lightAtEyes(player) >= UmapyoiConfig.EVENT_BRIGHT_LIGHT.get();
                    progress.lightSeconds = bright ? progress.lightSeconds + 1 : 0;
                    if (progress.lightSeconds >= stay)
                        done.add(type);
                }
                case WANT_SLEEP -> {
                    long time = CareerEventType.SkyWindow.timeOfDay(level);
                    boolean nightLeft = level.dimensionType().bedWorks() && CareerEventType.SkyWindow.hasDayCycle(level)
                            && time >= CareerEventType.SkyWindow.NIGHT.start - CareerEventType.SkyWindow.LEAD
                            && time < CareerEventType.SkyWindow.NIGHT.end;
                    if (player.isSleeping())
                        done.add(type);
                    else if (!nightLeft)
                        missed.add(type); // morning came, or no beds here: can't be done, no penalty
                }
                case WANT_INDOORS -> {
                    if (!level.isRaining()) {
                        missed.add(type);
                    } else {
                        progress.indoorSeconds = level.canSeeSky(eyes) ? 0 : progress.indoorSeconds + 1;
                        if (progress.indoorSeconds >= stay)
                            done.add(type);
                    }
                }
                case WANT_LEARN -> {
                    Set<String> before = new HashSet<>(Arrays.asList(active.data().split(";")));
                    boolean newSkill = UmaSoulUtils.getSkills(soul).stream()
                            .anyMatch(skill -> !before.contains(skill.toString()));
                    if (spentLevels || newSkill)
                        done.add(type);
                }
                default -> {
                }
            }
        }
        resetStreaks(progress, present);
        done.forEach(type -> CareerEventManager.complete(player, type));
        missed.forEach(type -> CareerEventManager.cancel(player, type));
    }

    /** Streaks only count while their event is in progress. */
    private static void resetStreaks(EventProgress progress, Set<CareerEventType> present) {
        if (!present.contains(CareerEventType.WANT_TO_RUN))
            progress.sprintSeconds = 0;
        if (!present.contains(CareerEventType.WANT_SEE_HORSE))
            progress.horseSeconds = 0;
        if (!present.contains(CareerEventType.WANT_COMPANY))
            progress.companySeconds = 0;
        if (!present.contains(CareerEventType.WANT_LIGHT))
            progress.lightSeconds = 0;
        if (!present.contains(CareerEventType.WANT_INDOORS))
            progress.indoorSeconds = 0;
    }

    private static boolean nearHorse(ServerPlayer player) {
        int range = UmapyoiConfig.TRAIT_HORSE_RANGE.get();
        double rangeSqr = (double) range * range;
        return !player.level().getEntitiesOfClass(AbstractHorse.class, player.getBoundingBox().inflate(range),
                horse -> HORSES.contains(horse.getType()) && horse.isAlive() && player.distanceToSqr(horse) <= rangeSqr)
                .isEmpty();
    }

    private static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE; // no note: treat the window as gone
        }
    }
}
