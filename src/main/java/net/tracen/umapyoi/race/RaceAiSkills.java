package net.tracen.umapyoi.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.Surface;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * A computer runner's skills: the action points she has, when each skill may be used again, and whether
 * now is the time. The rules are the players' (when a skill may be used, what it costs, how long it rests);
 * what is hers is the judgement:
 * <ul>
 * <li>A good moment: a speed skill when nobody holds her up and no speed skill is running yet, one that
 * gives stamina back when she is below half, a hindering one in the final leg when someone is within reach
 * (earlier the field is bunched up and it would go off at every turn).</li>
 * <li>Wisdom decides how quickly she takes a good moment, from a second and a half or so (G) to about a
 * quarter of a second (S), and how often she wastes a skill at a bad one.</li>
 * </ul>
 */
final class RaceAiSkills {
    private enum Kind {
        SPEED, REST, HINDER
    }

    private record Usable(ResourceLocation id, RaceSkills.Active active, int cost, Kind kind) {
    }

    private static final Set<String> REST = Set.of("deep_breaths", "cooldown", "nutritional_supplements", "big_eater");
    private static final Set<String> HINDER = Set.of("tether", "dominator");
    /** Wisdom levels: G and S. */
    private static final int DULLEST = 1, SHARPEST = 15;

    private final List<Usable> skills = new ArrayList<>();
    /** 0 for wisdom G, 1 for S and above. */
    private final double sharp;
    private final double max;
    /** Action points back per tick. */
    private final double regen;
    private double points;
    private final Map<ResourceLocation, Integer> readyAt = new HashMap<>();
    private int ticks;

    RaceAiSkills(ItemStack soul, RaceStats stats) {
        for (ResourceLocation id : UmaSoulUtils.getSkills(soul)) {
            RaceSkills.Active active = RaceSkills.active(id);
            UmaSkill skill = UmaSkillRegistry.REGISTRY.get(id);
            if (active == null || skill == null)
                continue;
            String name = id.getPath();
            skills.add(new Usable(id, active, skill.getActionPoint(), REST.contains(name) ? Kind.REST : HINDER.contains(name) ? Kind.HINDER : Kind.SPEED));
        }
        int wisdom = UmaSoulUtils.getProperty(soul).wisdom();
        this.sharp = Mth.clamp((wisdom - DULLEST) / (double) (SHARPEST - DULLEST), 0, 1);
        this.max = UmaSoulUtils.getMaxActionPoint(soul);
        this.points = max;
        this.regen = stats.apRegen();
    }

    /**
     * One tick of the race.
     *
     * @return the skill she used, or null
     */
    @Nullable
    ResourceLocation tick(RaceSkills.User user, RandomSource random) {
        ticks++;
        points = Math.min(max, points + regen);
        if (skills.isEmpty())
            return null;
        double take = 0.03 + 0.17 * sharp;
        double waste = 0.004 * (1 - sharp);
        for (Usable skill : skills) {
            if (points < skill.cost() || readyAt.getOrDefault(skill.id(), 0) > ticks || !skill.active().when().holds(user))
                continue;
            if (random.nextDouble() >= (good(skill.kind(), user) ? take : waste))
                continue;
            points -= skill.cost();
            readyAt.put(skill.id(), ticks + Math.max(20, skill.active().effect().apply(user)));
            return skill.id();
        }
        return null;
    }

    private static boolean good(Kind kind, RaceSkills.User user) {
        RaceRunner runner = user.runner();
        return switch (kind) {
            case SPEED -> runner.heldTicks() == 0 && runner.boosts().speedLevel() == 0 && !runner.exhausted();
            case REST -> runner.staminaFraction() < 0.5;
            case HINDER -> (runner.phase() == RacePhase.FINAL || runner.phase() == RacePhase.LAST_SPURT) && user.near(RaceSkills.REACH) > 0;
        };
    }

    // ---- what they get

    /** Ordinary skills a computer runner may have, and which ones they become at the upper level. */
    private static final List<String> COMMON = List.of("last_leg", "rapid", "low_health_buff", "deep_breaths", "nutritional_supplements",
            "low_health_heal", "tether", "turf_runner", "dirt_runner", "serene");
    private static final Map<String, String> UPPER = Map.of("last_leg", "heart_and_soul", "rapid", "divine_speed", "low_health_buff", "adv_low_health_buff",
            "deep_breaths", "cooldown", "nutritional_supplements", "big_eater", "low_health_heal", "adv_low_health_heal", "tether", "dominator",
            "serene", "steel_will");

    /**
     * Gives a computer runner skills besides her own, by how good the field is: none or one in the easiest
     * races, up to two or three in GI, where more of them are the upper level. Ground skills only for their ground.
     *
     * @param level what the field's stats are around
     */
    static void handOut(ItemStack soul, int level, Surface surface, RandomSource random) {
        int count = level <= 4 ? random.nextInt(2) : level <= 9 ? 1 : level <= 11 ? 2 : 2 + random.nextInt(2);
        double upper = Mth.clamp((level - 6) / 10.0, 0, 0.8);
        List<String> pool = new ArrayList<>(COMMON);
        pool.removeIf(name -> name.equals("turf_runner") && surface != Surface.TURF || name.equals("dirt_runner") && surface != Surface.DIRT);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            String name = pool.remove(random.nextInt(pool.size()));
            if (UPPER.containsKey(name) && random.nextDouble() < upper)
                name = UPPER.get(name);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, name);
            if (!UmaSoulUtils.hasSkill(soul, id))
                UmaSoulUtils.addSkill(soul, id);
        }
    }
}
