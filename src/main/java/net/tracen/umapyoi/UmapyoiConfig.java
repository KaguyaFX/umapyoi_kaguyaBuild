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
    public static ModConfigSpec.IntValue CAREER_CARD_PERCENT_PER_LEVEL;
    public static ModConfigSpec.DoubleValue NOTEBOOK_CAP_LOW_CHANCE;
    public static ModConfigSpec.IntValue NOTEBOOK_CAP_MID;
    public static ModConfigSpec.IntValue NOTEBOOK_CAP_HIGH;

    // Uma traits and fright. Checked once per second.
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

    // Personality traits. Active whenever a soul that has them is worn.
    public static ModConfigSpec.DoubleValue TIMID_CHANCE;
    public static ModConfigSpec.IntValue TIMID_SLOWNESS_SECONDS;
    public static ModConfigSpec.IntValue COWARDLY_RANGE;
    public static ModConfigSpec.IntValue COWARDLY_WEAKNESS_SECONDS;
    public static ModConfigSpec.DoubleValue DULL_MOOD_GAIN_FAIL_CHANCE;
    public static ModConfigSpec.IntValue AFRAID_OF_DARK_LIGHT;
    public static ModConfigSpec.DoubleValue GLUTTON_HUNGER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue SLEEPY_NIGHT_OWL_FACTOR;
    public static ModConfigSpec.IntValue PICKY_EATER_SAME_FOOD;
    public static ModConfigSpec.IntValue HATES_RAIN_SECONDS;
    public static ModConfigSpec.DoubleValue FRAGILE_HURT_MOOD_MULTIPLIER;
    public static ModConfigSpec.IntValue LIGHT_AVERSE_DAY_PERMILLE;
    public static ModConfigSpec.IntValue LIGHT_AVERSE_NIGHT_PERMILLE;
    public static ModConfigSpec.DoubleValue TENACIOUS_HEALTH;
    public static ModConfigSpec.DoubleValue CHEERFUL_MOOD_DROP_MULTIPLIER;
    public static ModConfigSpec.DoubleValue STUDIOUS_XP_BONUS;
    public static ModConfigSpec.IntValue STEADY_PERMILLE;
    public static ModConfigSpec.IntValue EXCITABLE_COMBAT_PERMILLE;
    public static ModConfigSpec.IntValue EXCITABLE_AP_PAUSE_SECONDS;

    // Food likes and the Umamusume's own food habits.
    public static ModConfigSpec.DoubleValue DISLIKED_FOOD_MOOD_CHANCE;
    public static ModConfigSpec.DoubleValue CARROT_MOOD_CHANCE;
    public static ModConfigSpec.DoubleValue RAW_MEAT_MOOD_CHANCE;
    public static ModConfigSpec.DoubleValue SENSITIVE_STOMACH_MULTIPLIER;

    // Career events.
    public static ModConfigSpec.DoubleValue EVENT_CHANCE_PER_TURN;
    public static ModConfigSpec.IntValue EVENT_OPTIONAL_TURNS;
    public static ModConfigSpec.IntValue EVENT_MANDATORY_TURNS;
    public static ModConfigSpec.IntValue EVENT_FAIL_KEEP_PERMILLE;
    public static ModConfigSpec.IntValue EVENT_HUNGRY_MAX_FOOD;
    public static ModConfigSpec.IntValue EVENT_RUN_SECONDS;
    public static ModConfigSpec.IntValue EVENT_STAY_SECONDS;
    public static ModConfigSpec.IntValue EVENT_HORSE_SECONDS;
    public static ModConfigSpec.IntValue EVENT_HOME_DISTANCE;
    public static ModConfigSpec.IntValue EVENT_BRIGHT_LIGHT;
    public static ModConfigSpec.IntValue EVENT_LAZY_POINTS_PERMILLE;
    public static ModConfigSpec.DoubleValue DULL_EVENT_CHANCE_MULTIPLIER;
    public static ModConfigSpec.IntValue IMPATIENT_FEWER_TURNS;
    public static ModConfigSpec.IntValue IMPATIENT_FAIL_MOOD_STEPS;
    public static ModConfigSpec.DoubleValue EVENT_SKILL_CHANCE;

    // What any Umamusume player is like.
    public static ModConfigSpec.DoubleValue UMA_BASE_HEALTH;
    public static ModConfigSpec.DoubleValue UMA_BASE_SPEED;
    public static ModConfigSpec.DoubleValue UMA_SPRINT_HUNGER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue UMA_EXTRA_FALL_DISTANCE;
    public static ModConfigSpec.IntValue UMA_TREAT_EXTRA_FOOD;
    public static ModConfigSpec.IntValue EAR_EXPLOSION_RANGE;
    public static ModConfigSpec.DoubleValue EAR_EXPLOSION_CHANCE;
    public static ModConfigSpec.IntValue EAR_THUNDER_RANGE;
    public static ModConfigSpec.DoubleValue EAR_THUNDER_CHANCE;
    public static ModConfigSpec.IntValue EAR_COOLDOWN_SECONDS;
    public static ModConfigSpec.IntValue HELMET_MOOD_SECONDS;

    public static ModConfigSpec.DoubleValue RACE_METERS_PER_BLOCK;
    public static ModConfigSpec.DoubleValue TRACK_STRAIGHT_METERS;
    public static ModConfigSpec.DoubleValue TRACK_LAP_METERS;
    public static ModConfigSpec.IntValue TRACK_WIDTH;
    public static ModConfigSpec.DoubleValue TRACK_FINISH_METERS;
    public static ModConfigSpec.IntValue TRACK_BUILD_COLUMNS_PER_TICK;
    public static ModConfigSpec.IntValue RACE_COUNTDOWN_SECONDS;
    public static ModConfigSpec.IntValue RACE_GATE_CLEAR_SECONDS;
    public static ModConfigSpec.DoubleValue RACE_SPEED_MAX_BONUS;
    public static ModConfigSpec.DoubleValue RACE_CRUISE_RATIO;
    public static ModConfigSpec.DoubleValue RACE_STAMINA_BASE;
    public static ModConfigSpec.DoubleValue RACE_STAMINA_SCALE;
    public static ModConfigSpec.DoubleValue RACE_LOW_STAMINA;
    public static ModConfigSpec.DoubleValue RACE_LOW_BONUS_START;
    public static ModConfigSpec.DoubleValue RACE_LOW_BONUS_END;
    public static ModConfigSpec.DoubleValue RACE_EXHAUSTED_SPEED;
    public static ModConfigSpec.DoubleValue RACE_AI_ERROR;
    public static ModConfigSpec.DoubleValue RACE_AI_BOOST;
    public static ModConfigSpec.DoubleValue RACE_EXHAUSTED_SPRINT;
    public static ModConfigSpec.DoubleValue RACE_EXHAUSTED_ACCEL;
    public static ModConfigSpec.DoubleValue RACE_EXHAUSTED_UNTIL;
    public static ModConfigSpec.DoubleValue RACE_JUMP_STAMINA;
    public static ModConfigSpec.IntValue RACE_REST_SECONDS;
    public static ModConfigSpec.DoubleValue RACE_STAMINA_REGEN_PERCENT;
    public static ModConfigSpec.DoubleValue RACE_ACTION_POINT_REGEN;
    public static ModConfigSpec.DoubleValue RACE_SKILL_SPEED_PER_LEVEL;
    public static ModConfigSpec.DoubleValue RACE_PHASE_MIDDLE_FROM;
    public static ModConfigSpec.DoubleValue RACE_PHASE_FINAL_FROM;
    public static ModConfigSpec.DoubleValue RACE_PHASE_SPURT_FROM;
    public static ModConfigSpec.DoubleValue RACE_ACCEL_BASE;
    public static ModConfigSpec.DoubleValue RACE_ACCEL_SCALE;
    public static ModConfigSpec.DoubleValue RACE_DECEL;
    public static ModConfigSpec.DoubleValue RACE_START_DASH;
    public static ModConfigSpec.DoubleValue RACE_START_SPEED;
    public static ModConfigSpec.DoubleValue RACE_SPRINT_KICK;
    public static ModConfigSpec.DoubleValue RACE_MOMENTUM_MARGIN;
    public static ModConfigSpec.DoubleValue RACE_COAST_SECONDS;
    public static ModConfigSpec.DoubleValue RACE_COAST_DECEL;
    public static ModConfigSpec.DoubleValue RACE_STOP_DECEL;
    public static ModConfigSpec.DoubleValue RACE_STYLE_STRENGTH;
    public static ModConfigSpec.ConfigValue<java.util.List<? extends Double>> RACE_MOOD_MULTIPLIERS;
    public static ModConfigSpec.DoubleValue RACE_GUTS_FLOOR_MAX;
    public static ModConfigSpec.ConfigValue<java.util.List<? extends Double>> RACE_GROUND_ACCEL;
    public static ModConfigSpec.ConfigValue<java.util.List<? extends Double>> RACE_GROUND_DRAIN;
    public static ModConfigSpec.DoubleValue RACE_ACTION_POINT_REGEN_MAX;
    public static ModConfigSpec.DoubleValue EVENT_ACUPUNCTURE_CHANCE;
    public static ModConfigSpec.IntValue EVENT_NEW_YEAR_BUFF_SECONDS;

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
                        "Set to 0 will only fail in trainning.").defineInRange("acupuncturist_support_chance", 0.4, 0.0, 1.0);
        
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
        CAREER_CARD_PERCENT_PER_LEVEL = COMMON_BUILDER.comment("Career growth bonus per level of stat support on the cards, in percent, added up across cards.",
                "5 = a card with Speed 3 makes speed grow 15% faster. Only for careers started after the change.")
                .defineInRange("career_card_percent_per_level", 5, 0, 1000);
        NOTEBOOK_CAP_LOW_CHANCE = COMMON_BUILDER.comment("During a career, chance that a low notebook also raises its stat's cap by 1.")
                .defineInRange("notebook_cap_low_chance", 0.3, 0.0, 1.0);
        NOTEBOOK_CAP_MID = COMMON_BUILDER.comment("During a career, how much a mid notebook raises its stat's cap.")
                .defineInRange("notebook_cap_mid", 1, 0, 39);
        NOTEBOOK_CAP_HIGH = COMMON_BUILDER.comment("During a career, how much a high notebook raises its stat's cap.")
                .defineInRange("notebook_cap_high", 2, 0, 39);
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

        COMMON_BUILDER.comment("Personality traits. Points multipliers are per mille: 1100 = x1.1.").push("personality");
        TIMID_CHANCE = COMMON_BUILDER.comment("Timid: chance to get Slowness I when hurt.")
                .defineInRange("timid_chance", 0.3, 0.0, 1.0);
        TIMID_SLOWNESS_SECONDS = COMMON_BUILDER.comment("Timid: how long that Slowness lasts.")
                .defineInRange("timid_slowness_seconds", 3, 1, 600);
        COWARDLY_RANGE = COMMON_BUILDER.comment("Cowardly: hurt with no companion this close (another player, a tamed animal, an iron golem) gives Weakness I.")
                .defineInRange("cowardly_range", 8, 1, 64);
        COWARDLY_WEAKNESS_SECONDS = COMMON_BUILDER.comment("Cowardly: how long that Weakness lasts.")
                .defineInRange("cowardly_weakness_seconds", 5, 1, 600);
        DULL_MOOD_GAIN_FAIL_CHANCE = COMMON_BUILDER.comment("Dull: chance that a motivation gain doesn't happen.")
                .defineInRange("dull_mood_gain_fail_chance", 0.5, 0.0, 1.0);
        AFRAID_OF_DARK_LIGHT = COMMON_BUILDER.comment("Afraid of the dark: Darkness while the light at the eyes is at most this.")
                .defineInRange("afraid_of_dark_light", 3, 0, 15);
        GLUTTON_HUNGER_MULTIPLIER = COMMON_BUILDER.comment("Glutton: hunger use multiplier.")
                .defineInRange("glutton_hunger_multiplier", 1.3, 1.0, 10.0);
        SLEEPY_NIGHT_OWL_FACTOR = COMMON_BUILDER.comment("Sleepy: Night Owl comes after this share of the usual sleepless time. 0.5 = twice as soon.")
                .defineInRange("sleepy_night_owl_factor", 0.5, 0.01, 1.0);
        PICKY_EATER_SAME_FOOD = COMMON_BUILDER.comment("Picky eater: eating the same food this many times in a row lowers motivation one step.")
                .defineInRange("picky_eater_same_food", 3, 2, 64);
        HATES_RAIN_SECONDS = COMMON_BUILDER.comment("Hates rain: motivation drops one step per this many seconds in a row out in the rain.")
                .defineInRange("hates_rain_seconds", 60, 1, 3600);
        FRAGILE_HURT_MOOD_MULTIPLIER = COMMON_BUILDER.comment("Fragile: multiplier on the chance to lose motivation when hurt.")
                .defineInRange("fragile_hurt_mood_multiplier", 2.0, 1.0, 100.0);
        LIGHT_AVERSE_DAY_PERMILLE = COMMON_BUILDER.comment("Light-averse: career points in direct daylight, per mille.")
                .defineInRange("light_averse_day_permille", 900, 0, 10000);
        LIGHT_AVERSE_NIGHT_PERMILLE = COMMON_BUILDER.comment("Light-averse: career points at night, per mille.")
                .defineInRange("light_averse_night_permille", 1150, 0, 10000);
        TENACIOUS_HEALTH = COMMON_BUILDER.comment("Tenacious: Resistance I at or below this share of max health.")
                .defineInRange("tenacious_health", 0.25, 0.0, 1.0);
        CHEERFUL_MOOD_DROP_MULTIPLIER = COMMON_BUILDER.comment("Cheerful: multiplier on the chance of every motivation drop. 0.5 = half as likely.")
                .defineInRange("cheerful_mood_drop_multiplier", 0.5, 0.0, 1.0);
        STUDIOUS_XP_BONUS = COMMON_BUILDER.comment("Studious: extra experience from picked-up orbs. 0.15 = +15%.")
                .defineInRange("studious_xp_bonus", 0.15, 0.0, 10.0);
        STEADY_PERMILLE = COMMON_BUILDER.comment("Steady: career points while motivation is Normal or better, per mille.")
                .defineInRange("steady_permille", 1100, 0, 10000);
        EXCITABLE_COMBAT_PERMILLE = COMMON_BUILDER.comment("Excitable: career points from combat (hitting, getting hurt, blocking), per mille.")
                .defineInRange("excitable_combat_permille", 1200, 0, 10000);
        EXCITABLE_AP_PAUSE_SECONDS = COMMON_BUILDER.comment("Excitable: action points don't recover for this long after hitting or getting hurt.")
                .defineInRange("excitable_ap_pause_seconds", 10, 0, 600);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Food. A soul's liked food always raises motivation one step.").push("food");
        DISLIKED_FOOD_MOOD_CHANCE = COMMON_BUILDER.comment("Chance that a disliked food lowers motivation one step. Picky eaters: always.")
                .defineInRange("disliked_food_mood_chance", 0.5, 0.0, 1.0);
        CARROT_MOOD_CHANCE = COMMON_BUILDER.comment("Chance that a carrot (item tag umapyoi:carrot_mood) raises motivation one step.")
                .defineInRange("carrot_mood_chance", 0.25, 0.0, 1.0);
        RAW_MEAT_MOOD_CHANCE = COMMON_BUILDER.comment("Chance that raw meat (item tag c:foods/raw_meat) lowers motivation one step.")
                .defineInRange("raw_meat_mood_chance", 0.5, 0.0, 1.0);
        SENSITIVE_STOMACH_MULTIPLIER = COMMON_BUILDER.comment("Harmful effects a food gives last this many times as long.")
                .defineInRange("sensitive_stomach_multiplier", 2.0, 1.0, 10.0);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Career events. A career has 62 turns; at most one new event per turn.").push("events");
        EVENT_CHANCE_PER_TURN = COMMON_BUILDER.comment("Chance that a turn without a fixed event draws one from the career's event table.")
                .defineInRange("event_chance_per_turn", 0.15, 0.0, 1.0);
        EVENT_OPTIONAL_TURNS = COMMON_BUILDER.comment("Turns an optional event stays before it goes away.")
                .defineInRange("event_optional_turns", 7, 1, 62);
        EVENT_MANDATORY_TURNS = COMMON_BUILDER.comment("Turns a mandatory event gives before it fails.")
                .defineInRange("event_mandatory_turns", 4, 1, 62);
        EVENT_FAIL_KEEP_PERMILLE = COMMON_BUILDER.comment("Share of every stat kept when a mandatory event fails, per mille. 980 = lose 2%.")
                .defineInRange("event_fail_keep_permille", 980, 0, 1000);
        EVENT_HUNGRY_MAX_FOOD = COMMON_BUILDER.comment("Hungry: only comes up at this food level or lower (full is 20).")
                .defineInRange("event_hungry_max_food", 14, 0, 20);
        EVENT_RUN_SECONDS = COMMON_BUILDER.comment("Want to run: seconds of sprinting in a row to complete it.")
                .defineInRange("event_run_seconds", 30, 1, 600);
        EVENT_STAY_SECONDS = COMMON_BUILDER.comment("Want company / somewhere bright / to stay indoors: seconds in a row to complete them.")
                .defineInRange("event_stay_seconds", 60, 1, 600);
        EVENT_HORSE_SECONDS = COMMON_BUILDER.comment("Want to see horses: seconds in a row near a horse, donkey or mule.")
                .defineInRange("event_horse_seconds", 30, 1, 600);
        EVENT_HOME_DISTANCE = COMMON_BUILDER.comment("Want to go home: how close to the respawn point counts as home.")
                .defineInRange("event_home_distance", 10, 1, 256);
        EVENT_BRIGHT_LIGHT = COMMON_BUILDER.comment("Want somewhere bright: the light level that counts as bright.")
                .defineInRange("event_bright_light", 12, 0, 15);
        EVENT_LAZY_POINTS_PERMILLE = COMMON_BUILDER.comment("Lazy period: career points during it, per mille. 500 = half.")
                .defineInRange("event_lazy_points_permille", 500, 0, 1000);
        DULL_EVENT_CHANCE_MULTIPLIER = COMMON_BUILDER.comment("Dull: multiplier on the chance that a turn draws an event.")
                .defineInRange("dull_event_chance_multiplier", 0.5, 0.0, 1.0);
        IMPATIENT_FEWER_TURNS = COMMON_BUILDER.comment("Impatient: mandatory events give this many turns fewer (at least 1 is left).")
                .defineInRange("impatient_fewer_turns", 1, 0, 62);
        IMPATIENT_FAIL_MOOD_STEPS = COMMON_BUILDER.comment("Impatient: motivation steps lost when a mandatory event fails (normally 1).")
                .defineInRange("impatient_fail_mood_steps", 2, 1, 4);
        EVENT_SKILL_CHANCE = COMMON_BUILDER.comment("Chance to learn each support card skill over a whole career; rolled every turn.")
                .defineInRange("event_skill_chance", 0.9, 0.0, 1.0);
        EVENT_ACUPUNCTURE_CHANCE = COMMON_BUILDER.comment("Acupuncturist card event: chance that every stat cap goes up by 1 (otherwise motivation drops).")
                .defineInRange("event_acupuncture_chance", 0.4, 0.0, 1.0);
        EVENT_NEW_YEAR_BUFF_SECONDS = COMMON_BUILDER.comment("New Year: seconds of Hyper Mood it gives.")
                .defineInRange("event_new_year_buff_seconds", 180, 1, 3600);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("What any player wearing a soul is like, trained or not.").push("umamusume");
        UMA_BASE_HEALTH = COMMON_BUILDER.comment("Extra max health from any worn soul.")
                .defineInRange("uma_base_health", 2.0, 0.0, 100.0);
        UMA_BASE_SPEED = COMMON_BUILDER.comment("Extra movement speed from any worn soul. 0.05 = +5%.")
                .defineInRange("uma_base_speed", 0.05, 0.0, 10.0);
        UMA_SPRINT_HUNGER_MULTIPLIER = COMMON_BUILDER.comment("Hunger use while sprinting is multiplied by this.")
                .defineInRange("uma_sprint_hunger_multiplier", 1.2, 1.0, 10.0);
        UMA_EXTRA_FALL_DISTANCE = COMMON_BUILDER.comment("Falls count as this many blocks higher (an Umamusume's legs are precious).")
                .defineInRange("uma_extra_fall_distance", 1.0, 0.0, 64.0);
        UMA_TREAT_EXTRA_FOOD = COMMON_BUILDER.comment("Carrots and sweets (item tag umapyoi:uma_treats) fill this much more.")
                .defineInRange("uma_treat_extra_food", 2, 0, 20);
        EAR_EXPLOSION_RANGE = COMMON_BUILDER.comment("Sensitive ears: an explosion this close may lower motivation.")
                .defineInRange("ear_explosion_range", 12, 1, 128);
        EAR_EXPLOSION_CHANCE = COMMON_BUILDER.comment("Sensitive ears: chance for that.")
                .defineInRange("ear_explosion_chance", 0.5, 0.0, 1.0);
        EAR_THUNDER_RANGE = COMMON_BUILDER.comment("Sensitive ears: lightning this close may lower motivation.")
                .defineInRange("ear_thunder_range", 32, 1, 256);
        EAR_THUNDER_CHANCE = COMMON_BUILDER.comment("Sensitive ears: chance for that.")
                .defineInRange("ear_thunder_chance", 0.3, 0.0, 1.0);
        EAR_COOLDOWN_SECONDS = COMMON_BUILDER.comment("Sensitive ears: at most one drop per this many seconds.")
                .defineInRange("ear_cooldown_seconds", 30, 0, 3600);
        HELMET_MOOD_SECONDS = COMMON_BUILDER.comment("A helmet squashes the ears: motivation drops one step per this many seconds wearing one.",
                "Headwear in the item tag umapyoi:ear_friendly_headwear doesn't count.")
                .defineInRange("helmet_mood_seconds", 300, 1, 36000);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Races run in person (prototype).").push("race");
        RACE_METERS_PER_BLOCK = COMMON_BUILDER.comment("How many race meters one block stands for.")
                .defineInRange("race_meters_per_block", 3.2, 0.5, 20.0);
        TRACK_STRAIGHT_METERS = COMMON_BUILDER.comment("Racecourse: length of each straight, in meters.")
                .defineInRange("track_straight_meters", 480.0, 0.0, 5000.0);
        TRACK_LAP_METERS = COMMON_BUILDER.comment("Racecourse: one lap, in meters, measured one block outside the inner rail (where race distance is measured).",
                "The curves take what the two straights leave. 2000 m makes a 2000 m race exactly one lap, starting and finishing in front of the stand.")
                .defineInRange("track_lap_meters", 2000.0, 400.0, 20000.0);
        TRACK_WIDTH = COMMON_BUILDER.comment("Racecourse: width of the running surface, in blocks (one starting stall is one block).")
                .defineInRange("track_width", 20, 4, 64);
        TRACK_FINISH_METERS = COMMON_BUILDER.comment("Racecourse: the finish line is this many meters before the home straight ends in the curve.")
                .defineInRange("track_finish_meters", 100.0, 0.0, 5000.0);
        TRACK_BUILD_COLUMNS_PER_TICK = COMMON_BUILDER.comment("Building a racecourse: block columns placed per tick. Lower it if building lags.")
                .defineInRange("track_build_columns_per_tick", 1500, 16, 100000);
        RACE_COUNTDOWN_SECONDS = COMMON_BUILDER.comment("Seconds counted down in the starting stalls before the gates open.")
                .defineInRange("race_countdown_seconds", 3, 1, 10);
        RACE_GATE_CLEAR_SECONDS = COMMON_BUILDER.comment("The starting gates are taken off the course this many seconds after they open.")
                .defineInRange("race_gate_clear_seconds", 8, 1, 60);
        RACE_SPEED_MAX_BONUS = COMMON_BUILDER.comment("Race speed: the most the speed stat can add to an Umamusume's sprint, as a fraction (survival uses 0.84).",
                "A race sprint is the plain player's sprint plus the Umamusume bonus (body + speed stat, mood and growth rate included).",
                "Graduated and active souls both count at full strength in races for now.")
                .defineInRange("race_speed_max_bonus", 0.5, 0.0, 10.0);
        RACE_CRUISE_RATIO = COMMON_BUILDER.comment("Race speed: walking (cruising) is this fraction of the current sprint speed, and costs no stamina.")
                .defineInRange("race_cruise_ratio", 0.85, 0.1, 1.0);
        RACE_STAMINA_BASE = COMMON_BUILDER.comment("Race stamina: blocks of sprinting any runner has (one point per block sprinted).")
                .defineInRange("race_stamina_base", 250.0, 1.0, 100000.0);
        RACE_STAMINA_SCALE = COMMON_BUILDER.comment("Race stamina: blocks the stamina stat adds at most (same curve as speed).")
                .defineInRange("race_stamina_scale", 500.0, 0.0, 100000.0);
        RACE_LOW_STAMINA = COMMON_BUILDER.comment("Race stamina: below this fraction of the bar, the Umamusume speed bonus drops.")
                .defineInRange("race_low_stamina", 0.5, 0.0, 1.0);
        RACE_LOW_BONUS_START = COMMON_BUILDER.comment("Race stamina: the bonus drops at once to this fraction when stamina falls below the line above...")
                .defineInRange("race_low_bonus_start", 0.8, 0.0, 1.0);
        RACE_LOW_BONUS_END = COMMON_BUILDER.comment("...and keeps falling to this fraction at empty.")
                .defineInRange("race_low_bonus_end", 0.1, 0.0, 1.0);
        RACE_EXHAUSTED_SPEED = COMMON_BUILDER.comment("Race exhaustion: a runner whose stamina runs out is exhausted. They cruise at this fraction of what cruising would be",
                "without it. They also can't jump and get no action points back.")
                .defineInRange("race_exhausted_speed", 0.8, 0.1, 1.0);
        RACE_EXHAUSTED_SPRINT = COMMON_BUILDER.comment("Race exhaustion: sprinting is this many times the exhausted cruise (close to 1: no strength left to sprint).")
                .defineInRange("race_exhausted_sprint", 1.03, 1.0, 2.0);
        RACE_EXHAUSTED_ACCEL = COMMON_BUILDER.comment("Race exhaustion: what acceleration is multiplied by.")
                .defineInRange("race_exhausted_accel", 0.5, 0.0, 1.0);
        RACE_EXHAUSTED_UNTIL = COMMON_BUILDER.comment("Race exhaustion: it ends when stamina is back to this fraction of the bar, so a short rest doesn't clear it.")
                .defineInRange("race_exhausted_until", 0.1, 0.0, 1.0);
        RACE_AI_BOOST = COMMON_BUILDER.comment("Computer runners: what every one of their stats is multiplied by (like a mood). They don't plan like a player",
                "and have no skills, so the same grade is a bit stronger in their hands.")
                .defineInRange("race_ai_boost", 1.1, 0.1, 10.0);
        RACE_AI_ERROR = COMMON_BUILDER.comment("Computer runners: how badly they time their last sprint, as a fraction of the race's distance (standard deviation;",
                "0 = always perfect).")
                .defineInRange("race_ai_error", 0.04, 0.0, 0.5);
        RACE_JUMP_STAMINA = COMMON_BUILDER.comment("Race stamina: points each jump costs (the same as sprinting this many blocks).")
                .defineInRange("race_jump_stamina", 10.0, 0.0, 10000.0);
        RACE_REST_SECONDS = COMMON_BUILDER.comment("Race stamina: standing still this long starts a slow recovery.")
                .defineInRange("race_rest_seconds", 2, 0, 60);
        RACE_STAMINA_REGEN_PERCENT = COMMON_BUILDER.comment("Race stamina: percent of the bar recovered per second while resting.")
                .defineInRange("race_stamina_regen_percent", 0.5, 0.0, 100.0);
        RACE_ACTION_POINT_REGEN = COMMON_BUILDER.comment("Skill action points in the race world recover at this fraction of the usual rate at the lowest wisdom;",
                "higher wisdom raises it along the stat curve, up to race_action_point_regen_max.")
                .defineInRange("race_action_point_regen", 0.5, 0.0, 1.0);
        RACE_SKILL_SPEED_PER_LEVEL = COMMON_BUILDER.comment("Race speed: each level of the Speed effect (what speed skills give) multiplies the Umamusume bonus by 1 + this.",
                "It only touches the bonus, never the plain player's speed; vanilla Speed is 0.2 per level of the whole speed.")
                .defineInRange("race_skill_speed_per_level", 0.2, 0.0, 10.0);
        RACE_PHASE_MIDDLE_FROM = COMMON_BUILDER.comment("Race phases: the middle part starts at this fraction of the distance (the opening is before it).")
                .defineInRange("race_phase_middle_from", 1.0 / 6, 0.0, 1.0);
        RACE_PHASE_FINAL_FROM = COMMON_BUILDER.comment("Race phases: the late part (final leg) starts at this fraction of the distance.")
                .defineInRange("race_phase_final_from", 2.0 / 3, 0.0, 1.0);
        RACE_PHASE_SPURT_FROM = COMMON_BUILDER.comment("Race phases: the last spurt starts at this fraction of the distance (the original game splits a race in sixths: 1, 3, 1, 1).")
                .defineInRange("race_phase_spurt_from", 5.0 / 6, 0.0, 1.0);
        RACE_ACTION_POINT_REGEN_MAX = COMMON_BUILDER.comment("Skill action points in the race world never recover faster than this fraction of the usual rate.")
                .defineInRange("race_action_point_regen_max", 0.9, 0.0, 1.0);
        RACE_ACCEL_BASE = COMMON_BUILDER.comment("Race acceleration, in blocks per second per second, at the lowest power. Sprinting doesn't reach full speed at once:",
                "it climbs at this rate (plus the power part below).")
                .defineInRange("race_accel_base", 0.25, 0.0, 100.0);
        RACE_ACCEL_SCALE = COMMON_BUILDER.comment("Race acceleration: what the power stat adds at most (same curve as speed).")
                .defineInRange("race_accel_scale", 0.55, 0.0, 100.0);
        RACE_DECEL = COMMON_BUILDER.comment("Race deceleration, in blocks per second per second, when the target drops (stopped sprinting, stamina fell below the line).")
                .defineInRange("race_decel", 0.8, 0.01, 100.0);
        RACE_START_DASH = COMMON_BUILDER.comment("Race start: extra acceleration out of the gates, until cruising speed is first reached.")
                .defineInRange("race_start_dash", 4.0, 0.0, 100.0);
        RACE_START_SPEED = COMMON_BUILDER.comment("Race start: speed when the gates open, as a fraction of the plain player's sprint.")
                .defineInRange("race_start_speed", 0.15, 0.0, 2.0);
        RACE_SPRINT_KICK = COMMON_BUILDER.comment("Race: starting to sprint puts you at once this fraction of the way from cruising to full speed (it never stacks).")
                .defineInRange("race_sprint_kick", 0.2, 0.0, 1.0);
        RACE_MOMENTUM_MARGIN = COMMON_BUILDER.comment("Race: after being slowed down (a wall, a stop), speed can't be more than this far above how fast you're really moving,",
                "as a fraction of the plain player's sprint, so you have to accelerate again.")
                .defineInRange("race_momentum_margin", 0.25, 0.0, 10.0);
        RACE_STYLE_STRENGTH = COMMON_BUILDER.comment("Running styles: how strongly the original game's per-phase speed, acceleration and stamina coefficients apply (1 = as in the original, 0 = off).")
                .defineInRange("race_style_strength", 1.0, 0.0, 10.0);
        RACE_COAST_SECONDS = COMMON_BUILDER.comment("Race: after letting go of all movement keys you keep rolling for this long, slowing gently,",
                "so you can settle into cruising without losing your speed; after that you stop quickly.")
                .defineInRange("race_coast_seconds", 1.0, 0.0, 10.0);
        RACE_COAST_DECEL = COMMON_BUILDER.comment("Race coasting: deceleration while rolling, in blocks per second per second.")
                .defineInRange("race_coast_decel", 0.8, 0.0, 100.0);
        RACE_STOP_DECEL = COMMON_BUILDER.comment("Race coasting: deceleration once the rolling is over, in blocks per second per second.")
                .defineInRange("race_stop_decel", 8.0, 0.0, 1000.0);
        RACE_MOOD_MULTIPLIERS = COMMON_BUILDER.comment("Race: what the motivation multiplies every stat by, from perfect to bad (the original game's values).",
                "Survival's multipliers are separate.")
                .defineList("race_mood_multipliers", java.util.List.of(1.04, 1.02, 1.0, 0.98, 0.96), () -> 1.0,
                        value -> value instanceof Number number && number.doubleValue() >= 0 && number.doubleValue() <= 10);
        RACE_GUTS_FLOOR_MAX = COMMON_BUILDER.comment("Race stamina: how much of the Umamusume speed bonus is still there at empty for the highest guts (lowest guts: race_low_bonus_end).")
                .defineInRange("race_guts_floor_max", 0.4, 0.0, 1.0);
        RACE_GROUND_ACCEL = COMMON_BUILDER.comment("Race ground: what acceleration is multiplied by on firm, yielding and heavy ground (from the weather: sunny, rain, thunderstorm).")
                .defineList("race_ground_accel", java.util.List.of(1.0, 0.95, 0.88), () -> 1.0,
                        value -> value instanceof Number number && number.doubleValue() >= 0 && number.doubleValue() <= 10);
        RACE_GROUND_DRAIN = COMMON_BUILDER.comment("Race ground: what the stamina drain is multiplied by on firm, yielding and heavy ground.")
                .defineList("race_ground_drain", java.util.List.of(1.0, 1.03, 1.08), () -> 1.0,
                        value -> value instanceof Number number && number.doubleValue() >= 0 && number.doubleValue() <= 10);
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
