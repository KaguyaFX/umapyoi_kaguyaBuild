package net.tracen.umapyoi.race;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.events.SkillEvent;
import net.tracen.umapyoi.utils.Aptitude;
import net.tracen.umapyoi.utils.Distance;
import net.tracen.umapyoi.utils.Position;
import net.tracen.umapyoi.utils.Surface;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * What the Umamusume skills do in a race. A runner's skills give no Minecraft effects there: each has a
 * race version that changes the race's own numbers ({@link RaceBoosts}, stamina, saving up), and an active
 * one can only be used at certain points of the race. The action points it costs are the skill's own, the
 * same as anywhere. After use a skill rests for as long as it lasts (at least a second). Passive skills
 * work by themselves. Computer runners don't use skills.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceSkills {
    /** Blocks around the user that a hindering skill reaches. */
    static final double REACH = 2.5;
    /** Stat levels: C+, B and A. */
    private static final int C_PLUS = 10, B = 11, A = 13;
    private static final int SPEED = 0, POWER = 2, WISDOM = 4;

    private RaceSkills() {
    }

    /** A runner using a skill: what the skill needs to know and do. */
    interface User {
        RaceRunner runner();

        ItemStack soul();

        RaceSetup setup();

        /** On a corner (the final straight isn't one). */
        boolean onCorner();

        /** Slows everyone else within reach; @return how many */
        int hinder(int level, int ticks, Component skill);

        /** How many others are within this many blocks. */
        int near(double reach);
    }

    /** When an active skill may be used. */
    enum When {
        ANY, MIDDLE, FINAL, FINAL_MIDDLE_RANKS, FINAL_CORNER, FINAL_MIDDLE_DISTANCE;

        boolean holds(User user) {
            RaceRunner runner = user.runner();
            RacePhase phase = runner.phase();
            boolean last = phase == RacePhase.FINAL || phase == RacePhase.LAST_SPURT;
            Distance category = user.setup().category();
            return switch (this) {
                case ANY -> true;
                case MIDDLE -> phase == RacePhase.MIDDLE;
                case FINAL -> last;
                case FINAL_MIDDLE_RANKS -> last && runner.rank() >= order(runner.field(), 20) && runner.rank() <= order(runner.field(), 60);
                case FINAL_CORNER -> last && user.onCorner() && (category == Distance.MEDIUM || category == Distance.LONG);
                case FINAL_MIDDLE_DISTANCE -> last && category == Distance.MEDIUM;
            };
        }

        /** Why it can't be used now. */
        MutableComponent why(Component skill, User user) {
            String key = "umapyoi.race.skill.when." + name().toLowerCase(java.util.Locale.ROOT);
            if (this == FINAL_MIDDLE_RANKS) {
                int field = user.runner().field();
                return Component.translatable(key, skill, order(field, 20), order(field, 60));
            }
            return Component.translatable(key, skill);
        }
    }

    /** An active skill's race version. */
    record Active(When when, Effect effect) {
    }

    /** @return how long it lasts, in ticks (0 = all at once) */
    interface Effect {
        int apply(User user);
    }

    private static final Map<String, Active> ACTIVE = new HashMap<>();
    /** Work by themselves in a race. */
    private static final Set<String> PASSIVE = Set.of("serene", "steel_will", "turf_runner", "dirt_runner", "inquisitive_mind");

    static {
        active("basic_pace", When.MIDDLE, user -> speed(user, stat(user, POWER) >= C_PLUS ? 2 : 1, stat(user, WISDOM) >= C_PLUS ? 12 : 10));
        active("last_leg", When.FINAL, user -> speed(user, 1 + byPower(user), 5));
        active("heart_and_soul", When.FINAL, user -> speed(user, 2 + byPower(user), 5));
        active("rapid", When.ANY, user -> {
            lateRest(user);
            return speed(user, 1, 6);
        });
        active("divine_speed", When.ANY, user -> {
            lateRest(user);
            return speed(user, 2, 8);
        });
        active("low_health_buff", When.FINAL_MIDDLE_RANKS, user -> speed(user, 1, 10));
        active("adv_low_health_buff", When.FINAL_MIDDLE_RANKS, user -> speed(user, 2, 10));
        active("top_umamusme", When.FINAL_CORNER, user -> {
            user.runner().boosts().accel(1.5, 5 * 20);
            return speed(user, 3, 5);
        });
        active("deep_breaths", When.MIDDLE, user -> rest(user, 0.05));
        active("cooldown", When.MIDDLE, user -> rest(user, 0.10));
        active("low_health_heal", When.FINAL_MIDDLE_DISTANCE, user -> {
            rest(user, 0.03);
            return speed(user, 1, 5);
        });
        active("adv_low_health_heal", When.FINAL_MIDDLE_DISTANCE, user -> {
            rest(user, 0.05);
            return speed(user, 1, 6);
        });
        active("nutritional_supplements", When.MIDDLE, user -> {
            rest(user, 0.07);
            return slow(user, 1, 5);
        });
        active("big_eater", When.MIDDLE, user -> {
            rest(user, 0.12);
            return slow(user, 1, 6);
        });
        active("tether", When.ANY, user -> hinder(user, "tether", 1, 5));
        active("dominator", When.ANY, user -> hinder(user, "dominator", 1, 8));
    }

    private static void active(String name, When when, Effect effect) {
        ACTIVE.put(name, new Active(when, effect));
    }

    /** The race version of an active skill, or null if it has none (it may be passive, or do nothing here). */
    @Nullable
    static Active active(ResourceLocation skill) {
        return Umapyoi.MODID.equals(skill.getNamespace()) ? ACTIVE.get(skill.getPath()) : null;
    }

    static boolean passive(ResourceLocation skill) {
        return Umapyoi.MODID.equals(skill.getNamespace()) && PASSIVE.contains(skill.getPath());
    }

    /**
     * A position condition in the original game's way: a percentage of the field, turned into a place
     * (rounded) before comparing. 1 = first.
     */
    static int order(int field, int percent) {
        return (int) Math.round(field * percent / 100.0);
    }

    // ---- what they do

    private static int stat(User user, int stat) {
        return UmaSoulUtils.getProperty(user.soul()).toArray()[stat];
    }

    /** Last-leg skills grow with power: one level from B, two from A. */
    private static int byPower(User user) {
        int power = stat(user, POWER);
        return power >= A ? 2 : power >= B ? 1 : 0;
    }

    private static int speed(User user, int level, int seconds) {
        user.runner().boosts().speed(level, seconds * 20);
        return seconds * 20;
    }

    private static int slow(User user, int level, int seconds) {
        user.runner().boosts().slow(level, seconds * 20);
        return seconds * 20;
    }

    private static int rest(User user, double fraction) {
        user.runner().restore(fraction);
        return 0;
    }

    /** A little stamina back too, when used in the final leg. */
    private static void lateRest(User user) {
        RacePhase phase = user.runner().phase();
        if (phase == RacePhase.FINAL || phase == RacePhase.LAST_SPURT)
            user.runner().restore(0.03);
    }

    private static int hinder(User user, String skill, int level, int seconds) {
        user.hinder(level, seconds * 20, Component.translatable("uma_skill.umapyoi." + skill));
        return seconds * 20;
    }

    // ---- passive skills

    /** At the start: what passive skills give for the whole race. */
    static void start(RaceRunner runner, ItemStack soul, RaceSetup setup) {
        int[] stats = UmaSoulUtils.getProperty(soul).toArray();
        for (ResourceLocation skill : UmaSoulUtils.getSkills(soul)) {
            if (!Umapyoi.MODID.equals(skill.getNamespace()))
                continue;
            switch (skill.getPath()) {
                case "turf_runner" -> {
                    if (setup.surface() == Surface.TURF)
                        runner.passive(0, 1.05);
                }
                case "dirt_runner" -> {
                    if (setup.surface() == Surface.DIRT)
                        runner.passive(0, 1.05);
                }
                case "inquisitive_mind" -> {
                    if (stats[SPEED] >= A)
                        runner.passive(stats[WISDOM] >= A ? 0.35 : 0.25, 1);
                }
                default -> {
                }
            }
        }
    }

    /**
     * Every tick: an end closer (aptitude A or better) held up at the back of the field for five seconds in a
     * row saves up faster while that lasts, with Serene or Iron Will.
     */
    static void charge(RaceRunner runner, ItemStack soul) {
        double times = 1;
        if (runner.heldTicks() >= 5 * 20 && runner.rank() >= order(runner.field(), 70)
                && runner.styles()[Position.END_CLOSER.ordinal()].ordinal() >= Aptitude.A.ordinal())
            for (ResourceLocation skill : UmaSoulUtils.getSkills(soul))
                if (Umapyoi.MODID.equals(skill.getNamespace()))
                    times = Math.max(times, switch (skill.getPath()) {
                        case "serene" -> 1.05;
                        case "steel_will" -> 1.10;
                        default -> 1;
                    });
        runner.chargeTimes(times);
    }

    // ---- using one

    /**
     * A runner's skills work the race way. Runs after everything else that may change the price (a
     * blessing halves it), then takes the use over.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseSkill(SkillEvent.UseSkillEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !RaceWorld.isRaceWorld(player.level()))
            return;
        RaceSession session = RaceSession.current();
        if (session == null || !session.running(player))
            return;
        event.setCanceled(true);
        session.useSkill(player, event.getSkillResourceLocation(), event.getAp());
    }
}
