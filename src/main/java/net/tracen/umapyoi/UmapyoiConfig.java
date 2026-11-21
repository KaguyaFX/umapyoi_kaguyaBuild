package net.tracen.umapyoi;

import net.neoforged.neoforge.common.ModConfigSpec;

public class UmapyoiConfig {
    
    public static final int DEFAULT_GACHA_PROBABILITY_SUM = 100;
    public static final int DEFAULT_GACHA_PROBABILITY_R = 70;
    public static final int DEFAULT_GACHA_PROBABILITY_SR = 20;
    public static final int DEFAULT_GACHA_PROBABILITY_SSR = 10;
    
    public static ModConfigSpec COMMON_CONFIG;
    
    public static ModConfigSpec.IntValue STAT_LIMIT_VALUE;
    public static ModConfigSpec.DoubleValue STAT_LIMIT_REDUCTION_RATE;
    
    public static ModConfigSpec.DoubleValue CHANCE_MOTIVATION_EFFECT;
    public static ModConfigSpec.DoubleValue DAMAGE_MOTIVATION_EFFECT;
    
    public static ModConfigSpec.IntValue GACHA_PROBABILITY_SUM;
    public static ModConfigSpec.IntValue GACHA_PROBABILITY_R;
    public static ModConfigSpec.IntValue GACHA_PROBABILITY_SR;
    public static ModConfigSpec.IntValue GACHA_PROBABILITY_SSR;

    public static ModConfigSpec CLIENT_CONFIG;
    public static ModConfigSpec.BooleanValue VANILLA_ARMOR_RENDER;
    public static ModConfigSpec.BooleanValue HIDE_PARTS_RENDER;
    public static ModConfigSpec.BooleanValue ELYTRA_RENDER;

    public static ModConfigSpec.IntValue EAR_ANIMATION_INTERVAL;
    public static ModConfigSpec.IntValue TAIL_ANIMATION_INTERVAL;

    public static ModConfigSpec.BooleanValue OVERLAY_SWITCH;
    public static ModConfigSpec.BooleanValue TOOLTIP_SWITCH;

    public static ModConfigSpec.BooleanValue DISPLAY_DETAIL;

    public static ModConfigSpec.DoubleValue ACUPUNCTUIST_SUPPORT_CHANCE;
    
    public static ModConfigSpec.DoubleValue UMASOUL_MAX_SPEED;
    public static ModConfigSpec.DoubleValue UMASOUL_MAX_STRENGTH_ATTACK;
    public static ModConfigSpec.DoubleValue UMASOUL_MAX_STAMINA_HEALTH;
    public static ModConfigSpec.DoubleValue UMASOUL_MAX_GUTS_ARMOR;
    public static ModConfigSpec.DoubleValue UMASOUL_MAX_GUTS_ARMOR_TOUGHNESS;
    
    public static ModConfigSpec.BooleanValue UMASOUL_SPEED_PRECENT_ENABLE;
    public static ModConfigSpec.BooleanValue UMASOUL_STRENGTH_PRECENT_ENABLE;
    public static ModConfigSpec.BooleanValue UMASOUL_STAMINA_PRECENT_ENABLE;
    public static ModConfigSpec.BooleanValue UMASOUL_GUTS_PRECENT_ENABLE;

    public static ModConfigSpec.DoubleValue SLOW_METABOLISM_PROBABILITY;
    public static ModConfigSpec.LongValue NIGHT_OWL_THRESHOLD;
    public static ModConfigSpec.DoubleValue NIGHT_OWL_PROBABILITY_DOWN_MOTIVATION;

    public static ModConfigSpec.BooleanValue GRANT_GUIDE_ON_FIRST_JOIN;

    // Three-year career. Point values are per action.
    public static ModConfigSpec.IntValue CAREER_LENGTH_MINUTES;
    public static ModConfigSpec.IntValue CAREER_POINTS_PER_LEVEL;
    public static ModConfigSpec.IntValue CAREER_BASE_REWARD_PERCENT;
    public static ModConfigSpec.IntValue CAREER_DEATH_KEEP_PERMILLE;
    public static ModConfigSpec.IntValue CAREER_SPEED_WALK_PER_SECOND;
    public static ModConfigSpec.IntValue CAREER_SPEED_SPRINT_PER_SECOND;
    public static ModConfigSpec.IntValue CAREER_STAMINA_PER_FOOD;
    public static ModConfigSpec.IntValue CAREER_STAMINA_PER_HEAL;
    public static ModConfigSpec.IntValue CAREER_STRENGTH_PER_HARDNESS;
    public static ModConfigSpec.IntValue CAREER_STRENGTH_PER_DAMAGE;
    public static ModConfigSpec.IntValue CAREER_GUTS_PER_DAMAGE_TAKEN;
    public static ModConfigSpec.IntValue CAREER_GUTS_PER_DAMAGE_BLOCKED;
    public static ModConfigSpec.IntValue CAREER_GUTS_PER_SECOND_HARMFUL;
    public static ModConfigSpec.IntValue CAREER_WISDOM_PER_LEVEL_SPENT;
    public static ModConfigSpec.IntValue CAREER_WISDOM_PER_POTION;
    public static ModConfigSpec.IntValue CAREER_WISDOM_PER_XP;

    // Uma traits and fright (design doc 8.4, 8.5). Checked once per second.
    public static ModConfigSpec.DoubleValue FRIGHT_BONUS_CUT;
    public static ModConfigSpec.IntValue FRIGHT_SECONDS;
    public static ModConfigSpec.IntValue FRIGHT_MOOD_INTERVAL_SECONDS;
    public static ModConfigSpec.DoubleValue FRIGHT_MOOD_CHANCE;
    public static ModConfigSpec.IntValue TRAIT_FEAR_RANGE;
    public static ModConfigSpec.IntValue TRAIT_FIRE_RANGE;
    public static ModConfigSpec.IntValue TRAIT_CRAMPED_MAX_OPEN_CELLS;
    public static ModConfigSpec.IntValue TRAIT_CRAMPED_SECONDS;
    public static ModConfigSpec.IntValue TRAIT_RESTLESS_SECONDS;
    public static ModConfigSpec.IntValue TRAIT_CURIOUS_COOLDOWN_SECONDS;
    public static ModConfigSpec.IntValue TRAIT_HORSE_RANGE;
    public static ModConfigSpec.IntValue TRAIT_HORSE_SECONDS;

    public static ModConfigSpec.IntValue TOPLEFT_COORD_SKILL_X;
    public static ModConfigSpec.IntValue TOPLEFT_COORD_SKILL_Y;
    public static ModConfigSpec.IntValue TOPLEFT_COORD_MOTIVATION_Y;
    public static ModConfigSpec.IntValue TOPLEFT_COORD_MOTIVATION_X;
    public static ModConfigSpec.IntValue TOPLEFT_COORD_CAREER_X;
    public static ModConfigSpec.IntValue TOPLEFT_COORD_CAREER_Y;

    static {
        ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
        COMMON_BUILDER.comment("General settings").push("general");

        ACUPUNCTUIST_SUPPORT_CHANCE = COMMON_BUILDER
                .comment("Determining the success chance of acupunctuist support.",
                        "Set to 0 will only fail in trainning.").defineInRange("umasoul_max_speed", 0.4, 0.0, 1.0);
        
        UMASOUL_SPEED_PRECENT_ENABLE = COMMON_BUILDER.comment("Determines whether to add speed bouns as a percentage.",
                "If enable, the bonus will be calculated as a percentage.").define("umasoul_speed_percent_enable", true);
        
        UMASOUL_MAX_SPEED = COMMON_BUILDER
                .comment("Determining the max speed of umamusume soul.",
                        "Set to 0 will not add any speed.").defineInRange("umasoul_max_speed", 1.20, 0.0, Double.MAX_VALUE);
        
        UMASOUL_STRENGTH_PRECENT_ENABLE = COMMON_BUILDER.comment("Determines whether to add strength bouns as a percentage.",
                "If enable, the bonus will be calculated as a percentage.").define("umasoul_strength_percent_enable", true);
        
        UMASOUL_MAX_STRENGTH_ATTACK = COMMON_BUILDER
                .comment("Determining the max attack of umamusume soul.",
                        "Set to 0 will not add any attack.").defineInRange("umasoul_max_strength_attack", 2.0, 0.0, Double.MAX_VALUE);
        
        UMASOUL_STAMINA_PRECENT_ENABLE = COMMON_BUILDER.comment("Determines whether to add stamina bouns as a percentage.",
                "If enable, the bonus will be calculated as a percentage.").define("umasoul_stamina_percent_enable", false);
        
        UMASOUL_MAX_STAMINA_HEALTH = COMMON_BUILDER
                .comment("Determining the max health of umamusume soul.",
                        "Set to 0 will not add any health.").defineInRange("umasoul_max_stamina_health", 20, 0.0, Double.MAX_VALUE);
        
        UMASOUL_GUTS_PRECENT_ENABLE = COMMON_BUILDER.comment("Determines whether to add guts bouns as a percentage.",
                "If enable, the bonus will be calculated as a percentage.").define("umasoul_guts_percent_enable", false);
        
        UMASOUL_MAX_GUTS_ARMOR = COMMON_BUILDER
                .comment("Determining the max armor of umamusume soul.",
                        "Set to 0 will not add any health.").defineInRange("umasoul_max_guts_armor", 5.0, 0.0, Double.MAX_VALUE);
        
        UMASOUL_MAX_GUTS_ARMOR_TOUGHNESS = COMMON_BUILDER
                .comment("Determining the max armor toughness of umamusume soul.",
                        "Set to 0 will not add any health.").defineInRange("umasoul_max_guts_armor_toughness", 4.0, 0.0, Double.MAX_VALUE);

        CHANCE_MOTIVATION_EFFECT = COMMON_BUILDER.comment("Determining the chance that damage will effect motivation.",
                "Set to 0 to turn off.").defineInRange("chance_motivation_effect", 0.5, 0.0, 1.0);

        DAMAGE_MOTIVATION_EFFECT = COMMON_BUILDER
                .comment("Determining the damage that will effect motivation.",
                        "Set to 0 to turn off.").defineInRange("damage_motivation_effect", 4.0, 0.0, Double.MAX_VALUE);

        GACHA_PROBABILITY_SUM = COMMON_BUILDER
                .comment("Determining the sum of gacha probabilities.",
                        "If the sum of the three gacha probabilities does not equal the expected sum, follow the default configuration.")
                .defineInRange("gacha_probability_sum", DEFAULT_GACHA_PROBABILITY_SUM, 3, Integer.MAX_VALUE);
        
        GACHA_PROBABILITY_R = COMMON_BUILDER
                .comment("Determining the R ranking probability of gacha probabilities.",
                        "If the sum of the three gacha probabilities does not equal the expected sum, follow the default configuration.")
                .defineInRange("gacha_probability_r", DEFAULT_GACHA_PROBABILITY_R, 1, Integer.MAX_VALUE);
        
        GACHA_PROBABILITY_SR = COMMON_BUILDER
                .comment("Determining the SR ranking probability of gacha probabilities.",
                        "If the sum of the three gacha probabilities does not equal the expected sum, follow the default configuration.")
                .defineInRange("gacha_probability_sr", DEFAULT_GACHA_PROBABILITY_SR, 1, Integer.MAX_VALUE);
        
        GACHA_PROBABILITY_SSR = COMMON_BUILDER
                .comment("Determining the SSR ranking probability of gacha probabilities.",
                        "If the sum of the three gacha probabilities does not equal the expected sum, follow the default configuration.")
                .defineInRange("gacha_probability_ssr", DEFAULT_GACHA_PROBABILITY_SSR, 1, Integer.MAX_VALUE);
        
        STAT_LIMIT_VALUE = COMMON_BUILDER.comment("Determines the threshold for all base stat values.",
                "If the threshold value is exceeded, the attribute effect will be reduced according to stat_limit_reduction.",
                "Some attribute effects no longer increase after exceeding the threshold")
                .defineInRange("stat_limit_value", 18, 18, Integer.MAX_VALUE);
        STAT_LIMIT_REDUCTION_RATE = COMMON_BUILDER.comment("Determines the reduction for all base stat values.",
                "If the threshold value is exceeded, the attribute effect will be reduced according to this value.")
                .defineInRange("stat_limit_reduction", 0.6D, 0D, 1D);

        SLOW_METABOLISM_PROBABILITY = COMMON_BUILDER.comment("Determines the probability of getting Slow Metabolism effect after player eaten certain food.")
                .defineInRange("slow_metabolism_probability", 0.05d, 0d, 1d);

        NIGHT_OWL_THRESHOLD = COMMON_BUILDER.comment("Determines how long of sleepless time before Night Owl effect may occur, unit: tick")
                .defineInRange("night_owl_threshold", 72000L, 0L, Long.MAX_VALUE);

        NIGHT_OWL_PROBABILITY_DOWN_MOTIVATION = COMMON_BUILDER.comment("Determines the probability of motivation down per second")
                .defineInRange("night_owl_probability_down_motivation", 0.01d, 0d, 1d);

        GRANT_GUIDE_ON_FIRST_JOIN = COMMON_BUILDER.comment("Determines if the guide would be given to player on their first join to the world or not.")
                .define("grant_guide_on_first_join", true);

        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Three-year career settings").push("career");
        CAREER_LENGTH_MINUTES = COMMON_BUILDER.comment("Real-time length of a three-year career, in minutes.",
                "Time only passes while the soul is worn. Only affects careers started after the change.")
                .defineInRange("career_length_minutes", 45, 1, 6000);
        CAREER_POINTS_PER_LEVEL = COMMON_BUILDER.comment("Training points needed for one stat level.")
                .defineInRange("career_points_per_level", 1000, 1, Integer.MAX_VALUE);
        CAREER_BASE_REWARD_PERCENT = COMMON_BUILDER.comment("Base reward at the end of a career, as a percentage of",
                "the worst-case auto-training growth (rounded up). 110 = +10%.")
                .defineInRange("career_base_reward_percent", 110, 0, 1000);
        CAREER_DEATH_KEEP_PERMILLE = COMMON_BUILDER.comment("Share of stats kept after dying during a career, per mille. 900 = lose 10%.")
                .defineInRange("career_death_keep_permille", 900, 0, 1000);
        CAREER_SPEED_WALK_PER_SECOND = COMMON_BUILDER.comment("Speed points per second of moving on foot.")
                .defineInRange("career_speed_walk_per_second", 2, 0, 100000);
        CAREER_SPEED_SPRINT_PER_SECOND = COMMON_BUILDER.comment("Speed points per second of sprinting on foot.")
                .defineInRange("career_speed_sprint_per_second", 4, 0, 100000);
        CAREER_STAMINA_PER_FOOD = COMMON_BUILDER.comment("Stamina points per hunger point eaten.")
                .defineInRange("career_stamina_per_food", 35, 0, 100000);
        CAREER_STAMINA_PER_HEAL = COMMON_BUILDER.comment("Stamina points per health point healed (not while under Regeneration).")
                .defineInRange("career_stamina_per_heal", 50, 0, 100000);
        CAREER_STRENGTH_PER_HARDNESS = COMMON_BUILDER.comment("Strength points per 1.0 hardness of a mined block.")
                .defineInRange("career_strength_per_hardness", 3, 0, 100000);
        CAREER_STRENGTH_PER_DAMAGE = COMMON_BUILDER.comment("Strength points per point of melee damage dealt.")
                .defineInRange("career_strength_per_damage", 7, 0, 100000);
        CAREER_GUTS_PER_DAMAGE_TAKEN = COMMON_BUILDER.comment("Guts points per point of damage taken.")
                .defineInRange("career_guts_per_damage_taken", 20, 0, 100000);
        CAREER_GUTS_PER_DAMAGE_BLOCKED = COMMON_BUILDER.comment("Guts points per point of damage blocked with a shield.")
                .defineInRange("career_guts_per_damage_blocked", 13, 0, 100000);
        CAREER_GUTS_PER_SECOND_HARMFUL = COMMON_BUILDER.comment("Guts points per second under any harmful effect.")
                .defineInRange("career_guts_per_second_harmful", 7, 0, 100000);
        CAREER_WISDOM_PER_LEVEL_SPENT = COMMON_BUILDER.comment("Wisdom points per experience level spent (enchanting, anvil).")
                .defineInRange("career_wisdom_per_level_spent", 100, 0, 100000);
        CAREER_WISDOM_PER_POTION = COMMON_BUILDER.comment("Wisdom points per potion taken out of a brewing stand.")
                .defineInRange("career_wisdom_per_potion", 200, 0, 100000);
        CAREER_WISDOM_PER_XP = COMMON_BUILDER.comment("Wisdom points per experience point picked up (orbs from mobs, ores, smelting...).")
                .defineInRange("career_wisdom_per_xp", 8, 0, 100000);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Uma traits and the fright status. Checked once per second.").push("traits");
        FRIGHT_BONUS_CUT = COMMON_BUILDER.comment("Share of the soul's own bonuses that fright takes away. 0.2 = 20%.",
                "It is a share of the soul's bonus, so a frightened player never drops below a player without a soul.")
                .defineInRange("fright_bonus_cut", 0.2, 0.0, 1.0);
        FRIGHT_SECONDS = COMMON_BUILDER.comment("Fright is refreshed to this many seconds while its cause is there.")
                .defineInRange("fright_seconds", 10, 1, 600);
        FRIGHT_MOOD_INTERVAL_SECONDS = COMMON_BUILDER.comment("While frightened, motivation may drop once every this many seconds.")
                .defineInRange("fright_mood_interval_seconds", 60, 1, 3600);
        FRIGHT_MOOD_CHANCE = COMMON_BUILDER.comment("Chance that motivation drops one step at each of those checks.")
                .defineInRange("fright_mood_chance", 0.5, 0.0, 1.0);
        TRAIT_FEAR_RANGE = COMMON_BUILDER.comment("Afraid of a creature: frightened when one is seen within this many blocks.")
                .defineInRange("trait_fear_range", 8, 1, 64);
        TRAIT_FIRE_RANGE = COMMON_BUILDER.comment("Afraid of fire: frightened when fire, lava or a lit campfire is this close, or when burning.")
                .defineInRange("trait_fire_range", 3, 1, 16);
        TRAIT_CRAMPED_MAX_OPEN_CELLS = COMMON_BUILDER.comment("Claustrophobia: cramped when, in a 5x3x5 box at the feet (75 cells), the player can reach",
                "at most this many cells that are not full blocks, and the sky can't be seen. A 1x2 tunnel is about 10, a 2x2 one 20.")
                .defineInRange("trait_cramped_max_open_cells", 20, 0, 75);
        TRAIT_CRAMPED_SECONDS = COMMON_BUILDER.comment("Claustrophobia: seconds in a row in a cramped place before fright. Not checked while sleeping.")
                .defineInRange("trait_cramped_seconds", 30, 1, 3600);
        TRAIT_RESTLESS_SECONDS = COMMON_BUILDER.comment("Restless: motivation drops one step after this many seconds without sprinting, then again after as many more.")
                .defineInRange("trait_restless_seconds", 300, 10, 36000);
        TRAIT_CURIOUS_COOLDOWN_SECONDS = COMMON_BUILDER.comment("Curious: seconds before another new biome can raise motivation.")
                .defineInRange("trait_curious_cooldown_seconds", 300, 0, 36000);
        TRAIT_HORSE_RANGE = COMMON_BUILDER.comment("Horse lover: a horse, donkey or mule within this many blocks counts as near.")
                .defineInRange("trait_horse_range", 8, 1, 64);
        TRAIT_HORSE_SECONDS = COMMON_BUILDER.comment("Horse lover: motivation rises one step per this many seconds in a row near one.")
                .defineInRange("trait_horse_seconds", 60, 1, 3600);
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }
    static {
        ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
        CLIENT_BUILDER.comment("Client settings").push("client");
        VANILLA_ARMOR_RENDER = CLIENT_BUILDER.comment("Determines whether to render vanilla armor slot's model.",
                "Player's armor won't render after closing this.").define("vanilla_armor_render", false);

        OVERLAY_SWITCH = CLIENT_BUILDER.comment("Decide Overlays' switch")
                .comment("Uma's selected skill and motivation won't render after closing this.")
                .define("overlay_switch", true);

        TOOLTIP_SWITCH = CLIENT_BUILDER.comment("Deciding whether to omit details.")
                .comment("After enabling, some details need to be pressed to display.").define("tooltip_switch", true);

        DISPLAY_DETAIL = CLIENT_BUILDER.comment("Deciding whether to always display skill or factor's details.")
                .define("always_display_details", false);

        HIDE_PARTS_RENDER = CLIENT_BUILDER
                .comment("Determines whether to render model's hiden parts.",
                        "Hiden parts like breasts won't render after closing this.")
                .define("hiden_parts_render", false);
        ELYTRA_RENDER = CLIENT_BUILDER
                .comment("Determines whether to render player's elytra after closed vanilla armor render.",
                        "Player's elytra won't render after closing this.")
                .define("elytra_render", true);

        EAR_ANIMATION_INTERVAL = CLIENT_BUILDER.comment("Determining the interval tick between twice ear animation.")
                .defineInRange("ear_animation_interval", 100, 10, Integer.MAX_VALUE);
        TAIL_ANIMATION_INTERVAL = CLIENT_BUILDER.comment("Determining the interval tick between twice tail animation.")
                .defineInRange("tail_animation_interval", 200, 10, Integer.MAX_VALUE);

        TOPLEFT_COORD_SKILL_X = CLIENT_BUILDER.comment("The x-coordinate of the top-left coordinate of skill overlay. Default: 102")
                .defineInRange("top_left_skill_x", 102, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TOPLEFT_COORD_SKILL_Y = CLIENT_BUILDER.comment("The y-coordinate of the top-left coordinate of skill overlay. Default: -21")
                .defineInRange("top_left_skill_y", -21, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TOPLEFT_COORD_MOTIVATION_X = CLIENT_BUILDER.comment("The x-coordinate of the top-left coordinate of motivation overlay. Default: 102")
                .defineInRange("top_left_motivation_x", 118, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TOPLEFT_COORD_MOTIVATION_Y = CLIENT_BUILDER.comment("The y-coordinate of the top-left coordinate of motivation overlay. Default: -21")
                .defineInRange("top_left_motivation_y", -37, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TOPLEFT_COORD_CAREER_X = CLIENT_BUILDER.comment("The x-coordinate of the career calendar, from the top-left corner of the screen. Default: 4")
                .defineInRange("top_left_career_x", 4, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TOPLEFT_COORD_CAREER_Y = CLIENT_BUILDER.comment("The y-coordinate of the career calendar, from the top-left corner of the screen. Default: 4")
                .defineInRange("top_left_career_y", 4, Integer.MIN_VALUE, Integer.MAX_VALUE);

        CLIENT_BUILDER.pop();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}
