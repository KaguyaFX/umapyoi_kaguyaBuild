package net.tracen.umapyoi.trait;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.career.CareerAttachments;
import net.tracen.umapyoi.effect.MobEffectRegistry;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;

/**
 * The 7 Uma traits (design doc 8.4) and the fright status (8.5). Everything is checked once per
 * second on the server, only while a soul is worn.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public class UmaTraitEvents {

    private static final Set<EntityType<?>> HORSES = Set.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE);
    /** Night vision is topped up before it gets below 10 seconds, where the screen starts to flicker. */
    private static final int NIGHT_VISION_TICKS = 300;
    private static final int NIGHT_VISION_REFRESH_BELOW = 240;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
            return;
        TraitState state = player.getData(CareerAttachments.TRAIT_STATE);
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (soul.isEmpty() || !player.isAlive()) {
            state.crampedSeconds = 0;
            state.restlessSeconds = 0;
            state.horseSeconds = 0;
            state.frightSeconds = 0;
            updateFrightModifiers(player, ItemStack.EMPTY, state);
            return;
        }

        boolean frightened = false;
        for (UmaTrait.Entry entry : UmaTraits.of(soul).umaEntries()) {
            switch (entry.trait()) {
                case FEAR -> frightened |= seesCreature(player, entry.target());
                case FEAR_OF_FIRE -> frightened |= nearFire(player);
                case CLAUSTROPHOBIA -> frightened |= crampedTooLong(player, state);
                case RESTLESS -> restless(player, state);
                case CURIOUS -> curious(player, soul, state);
                case NIGHT_VISION -> nightVision(player);
                case HORSE_LOVER -> horseLover(player, state);
            }
        }
        if (frightened)
            player.addEffect(new MobEffectInstance(MobEffectRegistry.FRIGHT,
                    UmapyoiConfig.FRIGHT_SECONDS.get() * 20, 0, false, false, true));

        frightMood(player, state);
        updateFrightModifiers(player, soul, state);
    }

    /** A respawned or End-returned player starts with fresh timers; drop any fright modifier copied over. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null)
                for (AttributeModifier.Operation operation : AttributeModifier.Operation.values())
                    instance.removeModifier(frightId(operation));
        });
    }

    // ---------------------------------------------------------------- fright

    private static ResourceLocation frightId(AttributeModifier.Operation operation) {
        return ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "fright_" + operation.getSerializedName());
    }

    private static void frightMood(ServerPlayer player, TraitState state) {
        if (!player.hasEffect(MobEffectRegistry.FRIGHT)) {
            state.frightSeconds = 0;
            return;
        }
        if (++state.frightSeconds < UmapyoiConfig.FRIGHT_MOOD_INTERVAL_SECONDS.get())
            return;
        state.frightSeconds = 0;
        if (player.getRandom().nextDouble() < UmapyoiConfig.FRIGHT_MOOD_CHANCE.get())
            UmaStatusUtils.changeMotivation(player, -1);
    }

    /**
     * Fright takes a share of what the soul gives right now, with modifiers of its own. Curios only
     * recalculates a soul's modifiers when the item changes, so changing those would not follow the
     * effect coming and going. Being a share of the soul's bonus, it never takes a player below
     * one without a soul.
     */
    private static void updateFrightModifiers(ServerPlayer player, ItemStack soul, TraitState state) {
        Map<Holder<Attribute>, Map<AttributeModifier.Operation, AttributeModifier>> wanted = new HashMap<>();
        if (!soul.isEmpty() && player.hasEffect(MobEffectRegistry.FRIGHT)) {
            double cut = UmapyoiConfig.FRIGHT_BONUS_CUT.get();
            Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> sums = new HashMap<>();
            CuriosApi.getCurio(soul).ifPresent(curio -> curio
                    .getAttributeModifiers(new SlotContext("uma_soul", player, 0, false, true), frightId(AttributeModifier.Operation.ADD_VALUE))
                    .forEach((attribute, modifier) -> {
                        if (attribute.value() instanceof SlotAttribute)
                            return;
                        sums.computeIfAbsent(attribute, a -> new EnumMap<>(AttributeModifier.Operation.class))
                                .merge(modifier.operation(), modifier.amount(), Double::sum);
                    }));
            sums.forEach((attribute, byOperation) -> byOperation.forEach((operation, amount) -> {
                double value = -amount * cut;
                if (Math.abs(value) > 1.0E-9)
                    wanted.computeIfAbsent(attribute, a -> new EnumMap<>(AttributeModifier.Operation.class))
                            .put(operation, new AttributeModifier(frightId(operation), value, operation));
            }));
        }

        state.frightModifiers.forEach((attribute, byOperation) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null)
                return;
            Map<AttributeModifier.Operation, AttributeModifier> keep = wanted.getOrDefault(attribute, Map.of());
            byOperation.keySet().stream().filter(operation -> !keep.containsKey(operation))
                    .forEach(operation -> instance.removeModifier(frightId(operation)));
        });
        wanted.forEach((attribute, byOperation) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null)
                return;
            byOperation.values().forEach(modifier -> {
                AttributeModifier current = instance.getModifier(modifier.id());
                if (current == null || Math.abs(current.amount() - modifier.amount()) > 1.0E-9)
                    instance.addOrUpdateTransientModifier(modifier);
            });
        });
        state.frightModifiers.clear();
        state.frightModifiers.putAll(wanted);
    }

    // ---------------------------------------------------------------- the traits

    /** 怕〔某種生物〕: one in sight and in range. */
    private static boolean seesCreature(ServerPlayer player, @Nullable EntityType<?> target) {
        if (target == null)
            return false;
        int range = UmapyoiConfig.TRAIT_FEAR_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);
        return !player.level().getEntities(player, box, e -> e.getType() == target && e.isAlive()
                && player.distanceToSqr(e) <= (double) range * range && !e.isInvisibleTo(player)
                && player.hasLineOfSight(e)).isEmpty();
    }

    /** 怕火: burning, in lava, or fire, lava or a lit campfire close by. */
    private static boolean nearFire(ServerPlayer player) {
        if (player.isOnFire() || player.isInLava())
            return true;
        int range = UmapyoiConfig.TRAIT_FIRE_RANGE.get();
        Level level = player.level();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            BlockState block = level.getBlockState(pos);
            if (block.is(BlockTags.FIRE) || block.getFluidState().is(FluidTags.LAVA)
                    || (block.is(BlockTags.CAMPFIRES) && block.getOptionalValue(BlockStateProperties.LIT).orElse(false)))
                return true;
        }
        return false;
    }

    /** 幽閉恐懼: cramped for long enough (design doc 8.4, last part). Not while sleeping. */
    private static boolean crampedTooLong(ServerPlayer player, TraitState state) {
        if (player.isSleeping() || !isCramped(player)) {
            state.crampedSeconds = 0;
            return false;
        }
        state.crampedSeconds = Math.min(state.crampedSeconds + 1, 1_000_000);
        return state.crampedSeconds >= UmapyoiConfig.TRAIT_CRAMPED_SECONDS.get();
    }

    /**
     * Counts the open cells of a 5x3x5 box at the feet that the player can reach from where they
     * stand, moving through cells that are not full blocks (torches, grass, doors, slabs and water
     * count as open). Air behind a wall doesn't count, so thin walls or a cave on the other side of
     * the rock don't make a tunnel feel roomy. Cramped when few are reachable and the sky can't be seen.
     */
    private static boolean isCramped(ServerPlayer player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        if (level.canSeeSky(feet.above()))
            return false;
        int limit = UmapyoiConfig.TRAIT_CRAMPED_MAX_OPEN_CELLS.get();
        BlockPos min = feet.offset(-2, 0, -2);
        BlockPos max = feet.offset(2, 2, 2);
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(feet);
        queue.add(feet);
        int open = 0;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (level.getBlockState(pos).isCollisionShapeFullBlock(level, pos))
                continue;
            if (++open > limit)
                return false;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (next.getX() >= min.getX() && next.getX() <= max.getX() && next.getY() >= min.getY()
                        && next.getY() <= max.getY() && next.getZ() >= min.getZ() && next.getZ() <= max.getZ()
                        && seen.add(next))
                    queue.add(next);
            }
        }
        return true;
    }

    /** 坐不住: too long without sprinting lowers motivation one step, again and again. */
    private static void restless(ServerPlayer player, TraitState state) {
        if (player.isSleeping())
            return;
        if (player.isSprinting()) {
            state.restlessSeconds = 0;
            return;
        }
        if (++state.restlessSeconds < UmapyoiConfig.TRAIT_RESTLESS_SECONDS.get())
            return;
        state.restlessSeconds = 0;
        UmaStatusUtils.changeMotivation(player, -1);
    }

    /**
     * 好奇心: the first visit of this soul to a biome raises motivation one step. During the cooldown
     * new biomes are not marked, so they still count later.
     */
    private static void curious(ServerPlayer player, ItemStack soul, TraitState state) {
        if (state.curiousCooldown > 0) {
            state.curiousCooldown--;
            return;
        }
        Optional<ResourceKey<Biome>> biome = player.level().getBiome(player.blockPosition()).unwrapKey();
        if (biome.isEmpty())
            return;
        ResourceLocation id = biome.get().location();
        List<ResourceLocation> visited = soul.getOrDefault(DataComponentsTypeRegistry.VISITED_BIOMES, List.of());
        if (visited.contains(id))
            return;
        List<ResourceLocation> updated = new ArrayList<>(visited);
        updated.add(id);
        soul.set(DataComponentsTypeRegistry.VISITED_BIOMES, updated);
        UmaStatusUtils.changeMotivation(player, 1);
        state.curiousCooldown = UmapyoiConfig.TRAIT_CURIOUS_COOLDOWN_SECONDS.get();
    }

    /** 夜視: night vision at night under the open sky (not in the Nether or the End). */
    private static void nightVision(ServerPlayer player) {
        Level level = player.level();
        if (level.dimensionType().hasFixedTime() || !level.isNight()
                || !level.canSeeSky(BlockPos.containing(player.getEyePosition())))
            return;
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || (!current.isInfiniteDuration() && current.getDuration() < NIGHT_VISION_REFRESH_BELOW))
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false, true));
    }

    /** 戀同族: time near a horse, donkey or mule raises motivation one step at a time. */
    private static void horseLover(ServerPlayer player, TraitState state) {
        int range = UmapyoiConfig.TRAIT_HORSE_RANGE.get();
        boolean near = !player.level().getEntitiesOfClass(AbstractHorse.class, player.getBoundingBox().inflate(range),
                horse -> HORSES.contains(horse.getType()) && horse.isAlive()
                        && player.distanceToSqr(horse) <= (double) range * range).isEmpty();
        if (!near) {
            state.horseSeconds = 0;
            return;
        }
        if (++state.horseSeconds < UmapyoiConfig.TRAIT_HORSE_SECONDS.get())
            return;
        state.horseSeconds = 0;
        UmaStatusUtils.changeMotivation(player, 1);
    }
}
