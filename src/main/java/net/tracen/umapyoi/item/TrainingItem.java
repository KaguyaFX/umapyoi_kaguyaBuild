package net.tracen.umapyoi.item;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.registry.training.SupportContainer;
import net.tracen.umapyoi.registry.training.SupportStack;
import net.tracen.umapyoi.registry.training.SupportType;
import net.tracen.umapyoi.registry.training.TrainingSupport;
import net.tracen.umapyoi.utils.GachaRanking;
import net.tracen.umapyoi.utils.UmaSoulUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.registry.umadata.UmaDataBasicStatus;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.minecraft.util.RandomSource;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TrainingItem extends Item implements SupportContainer {
    private final SupportType type;
    private final Supplier<TrainingSupport> support;
    private final int level;

    public TrainingItem(SupportType type, Supplier<TrainingSupport> support, int level) {
        super(Umapyoi.defaultItemProperties());
        this.type = type;
        this.support = support;
        this.level = level;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide()) return super.use(level, player, usedHand);
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        ItemStack itemInHand = player.getItemInHand(usedHand);
        return learning(level, player, soul, itemInHand);
    }

    private InteractionResultHolder<ItemStack> learning(Level pLevel, Player pPlayer, ItemStack soul, ItemStack tranining) {
        if (soul.isEmpty()) {
            pPlayer.displayClientMessage(Component.translatable("umapyoi.no_umasoul_equiped"), true);
            return InteractionResultHolder.fail(tranining);
        }
        
        if (UmaSoulUtils.getLearningTimes(soul) <= 0) {
            pPlayer.displayClientMessage(Component.translatable("umapyoi.learning.no_learning_time"), true);
            return InteractionResultHolder.fail(tranining);
        }
        
        // During a three-year career a notebook may also raise its stat's cap (design doc 7.5).
        if (CareerManager.isInCareer(soul))
            raiseCapInCareer(pPlayer, soul, pLevel.getRandom());
        if (this.getSupport().applySupport(soul, pLevel.getRandom())) {
            UmaSoulUtils.downLearningTimes(soul);
            pPlayer.getCooldowns().addCooldown(tranining.getItem(), 30);
            tranining.shrink(1);
            return InteractionResultHolder.success(tranining);
        } else {
            pPlayer.displayClientMessage(Component.translatable("umapyoi.learning.can_not_learn"), true);
            return InteractionResultHolder.fail(tranining);
        }
    }

    /** Low: a chance of +1; mid: +1; high: +2 (all configurable), up to the absolute max of 39. */
    private void raiseCapInCareer(Player player, ItemStack soul, RandomSource random) {
        int amount = switch (this.level) {
            case 1 -> random.nextDouble() < UmapyoiConfig.NOTEBOOK_CAP_LOW_CHANCE.get() ? 1 : 0;
            case 2 -> UmapyoiConfig.NOTEBOOK_CAP_MID.get();
            default -> UmapyoiConfig.NOTEBOOK_CAP_HIGH.get();
        };
        int stat = this.type.ordinal(); // SPEED..WISDOM in the stat order
        if (amount <= 0 || stat > 4)
            return;
        int[] max = UmaSoulUtils.getMaxProperty(soul).toArray();
        int raised = Math.min(39, max[stat] + amount) - max[stat];
        if (raised <= 0)
            return;
        max[stat] += raised;
        soul.set(DataComponentsTypeRegistry.UMADATA_MAX_BASIC_STATUS, UmaDataBasicStatus.init(max));
        player.displayClientMessage(Component.translatable("umapyoi.career.notebook_cap",
                Component.translatable("gui.umapyoi.self_training." + STAT_KEYS[stat]), raised), true);
    }

    private static final String[] STAT_KEYS = { "speed", "stamina", "strength", "guts", "wisdom" };

    @Override
    public boolean overrideStackedOnOther(ItemStack pStack, Slot pSlot, ClickAction pAction, Player pPlayer) {
        if (pPlayer.level().isClientSide())
            return super.overrideStackedOnOther(pStack, pSlot, pAction, pPlayer);
        if(pAction != ClickAction.SECONDARY || pPlayer.getCooldowns().isOnCooldown(pStack.getItem())) return false;

        ItemStack soul = pSlot.getItem();
        return this.learning(pPlayer.level(), pPlayer, soul, pStack).getResult() == InteractionResult.SUCCESS;
    }

    protected SupportStack getSupport() {
        return Suppliers.memoize(() -> new SupportStack(this.support.get(), this.level)).get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(this.getSupport().getDescription());
    }

    @Override
    public boolean isConsumable(Level level, ItemStack stack) {
        return true;
    }

    @Override
    public GachaRanking getSupportLevel(Level level, ItemStack stack) {
        return GachaRanking.values()[this.level - 1];
    }

    @Override
    public SupportType getSupportType(Level level, ItemStack stack) {
        return this.type;
    }

    @Override
    public List<SupportStack> getSupports(Level level, ItemStack stack) {
        return ImmutableList.of(this.getSupport());
    }

    @Override
    public Predicate<ItemStack> canSupport(Level level, ItemStack stack) {
        return soul -> true;
    }

}
