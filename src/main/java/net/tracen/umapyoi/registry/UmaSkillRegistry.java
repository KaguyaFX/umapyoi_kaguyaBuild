package net.tracen.umapyoi.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.registry.skills.*;
import net.tracen.umapyoi.registry.skills.RaceOnlySkill;
import net.tracen.umapyoi.registry.skills.RequiemSkill;
import net.tracen.umapyoi.registry.skills.SkillRarity;
import net.tracen.umapyoi.registry.skills.passive.PassiveSkill;

public class UmaSkillRegistry {
    public static final DeferredRegister<UmaSkill> SKILLS = DeferredRegister.create(UmaSkill.REGISTRY_KEY,
            Umapyoi.MODID);

    public static final Registry<UmaSkill> REGISTRY = SKILLS.makeRegistry(builder->{
    	builder.defaultKey(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "basic_pace"));
    });

    public static final DeferredHolder<UmaSkill, UmaSkill> BASIC_PACE = SKILLS.register("basic_pace",
            () -> new SpeedSkill(new UmaSkill.Builder().level(1).type(SkillType.BUFF).actionPoint(500), 400));

    public static final DeferredHolder<UmaSkill, UmaSkill> LAST_LEG = SKILLS.register("last_leg",
            () -> new LastLegSkill(new UmaSkill.Builder().level(1).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "heart_and_soul")).type(SkillType.BUFF).actionPoint(350).requiredWisdom(2),
                    100));
    public static final DeferredHolder<UmaSkill, UmaSkill> HEART_AND_SOUL = SKILLS.register("heart_and_soul",
            () -> new LastLegSkill(new UmaSkill.Builder().level(2).type(SkillType.BUFF).actionPoint(650).requiredWisdom(4),
                    100));

    public static final DeferredHolder<UmaSkill, UmaSkill> DEEP_BREATHS = SKILLS.register("deep_breaths",
            () -> new HealSkill(new UmaSkill.Builder().level(1).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "cooldown")).type(SkillType.HEAL).actionPoint(400).requiredWisdom(2)));
    public static final DeferredHolder<UmaSkill, UmaSkill> COOLDOWN = SKILLS.register("cooldown",
            () -> new HealSkill(new UmaSkill.Builder().level(2).type(SkillType.HEAL).actionPoint(700).requiredWisdom(4)));

    public static final DeferredHolder<UmaSkill, UmaSkill> LOW_HEALTH_HEAL = SKILLS.register("low_health_heal",
            () -> new LowHealthHealSkill(new UmaSkill.Builder().level(1).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "adv_low_health_heal")).type(SkillType.HEAL).actionPoint(400).requiredWisdom(2)));
    public static final DeferredHolder<UmaSkill, UmaSkill> ADV_LOWHEALTH_HEAL = SKILLS.register("adv_low_health_heal",
            () -> new LowHealthHealSkill(new UmaSkill.Builder().level(2).type(SkillType.HEAL).actionPoint(650).requiredWisdom(4)));

    public static final DeferredHolder<UmaSkill, UmaSkill> LOW_HEALTH_BUFF = SKILLS.register("low_health_buff",
            () -> new LowHealthBuffSkill(new UmaSkill.Builder().level(1).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "adv_low_health_buff")).type(SkillType.BUFF).actionPoint(400).requiredWisdom(2)));
    public static final DeferredHolder<UmaSkill, UmaSkill> ADV_LOWHEALTH_BUFF = SKILLS.register("adv_low_health_buff",
            () -> new LowHealthBuffSkill(new UmaSkill.Builder().level(2).type(SkillType.BUFF).actionPoint(700).requiredWisdom(4)));


    public static final DeferredHolder<UmaSkill, UmaSkill> NUTRITIONAL_SUPPLEMENTS = SKILLS.register("nutritional_supplements",
            () -> new NutritionalSupplementsSkill(new UmaSkill.Builder().level(1).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "big_eater")).type(SkillType.HEAL).actionPoint(350).requiredWisdom(3)));
    public static final DeferredHolder<UmaSkill, UmaSkill> BIG_EATER = SKILLS.register("big_eater",
            () -> new NutritionalSupplementsSkill(new UmaSkill.Builder().level(2).type(SkillType.HEAL).actionPoint(600).requiredWisdom(5)));

    public static final DeferredHolder<UmaSkill, UmaSkill> SERENE = SKILLS.register("serene",
            () -> new SereneSkill(new UmaSkill.Builder().level(1).type(SkillType.HEAL).actionPoint(300).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> STEEL_WILL = SKILLS.register("steel_will",
            () -> new SteelWillSkill(new UmaSkill.Builder().level(2).type(SkillType.HEAL).actionPoint(1200).requiredWisdom(8).nonInheritable()));

    public static final DeferredHolder<UmaSkill, UmaSkill> MOUNTAIN_CLIMBER = SKILLS.register("mountain_climber",
            () -> new PassiveSkill(new UmaSkill.Builder().level(1).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> DIG_SPEED = SKILLS.register("dig_speed",
            () -> new PassiveSkill(new UmaSkill.Builder().level(1).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> TURF_RUNNER = SKILLS.register("turf_runner",
            () -> new PassiveSkill(new UmaSkill.Builder().level(1).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> DIRT_RUNNER = SKILLS.register("dirt_runner",
            () -> new PassiveSkill(new UmaSkill.Builder().level(1).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> SNOW_RUNNER = SKILLS.register("snow_runner",
            () -> new PassiveSkill(new UmaSkill.Builder().level(1).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> RAPID = SKILLS.register("rapid",
            () -> new DivineSpeedSkill(new UmaSkill.Builder().level(1).requiredWisdom(2).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "divine_speed")).type(SkillType.BUFF).actionPoint(450), 200));

    public static final DeferredHolder<UmaSkill, UmaSkill> DIVINE_SPEED = SKILLS.register("divine_speed",
            () -> new DivineSpeedSkill(new UmaSkill.Builder().level(2).requiredWisdom(5).actionPoint(1000).type(SkillType.BUFF), 400));

    public static final DeferredHolder<UmaSkill, UmaSkill> TOP_UMAMUSUME = SKILLS.register("top_umamusme",
            () -> new TopUmamusumeSkill(new UmaSkill.Builder().level(3).type(SkillType.BUFF).actionPoint(1000).requiredWisdom(8).nonInheritable()));

    public static final DeferredHolder<UmaSkill, UmaSkill> INQUISITIVE_MIND = SKILLS.register("inquisitive_mind",
            () -> new PassiveSkill(new UmaSkill.Builder().level(1).requiredWisdom(2)));

    public static final DeferredHolder<UmaSkill, UmaSkill> TETHER = SKILLS.register("tether",
            () -> new TetherSkill(new UmaSkill.Builder().level(1).upperSkill(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "dominator")).type(SkillType.HINDER).actionPoint(350).requiredWisdom(2)));
    public static final DeferredHolder<UmaSkill, UmaSkill> DOMINATOR = SKILLS.register("dominator",
            () -> new TetherSkill(new UmaSkill.Builder().level(2).type(SkillType.HINDER).actionPoint(550).requiredWisdom(4)));

    /** The zodiac cups' computer runners' skills, for races only; players get them by command or in creative. */
    public static final DeferredHolder<UmaSkill, UmaSkill> CONQUEROR_OF_STARS = SKILLS.register("conqueror_of_stars",
            () -> new RaceOnlySkill(new UmaSkill.Builder().level(3).type(SkillType.BUFF).actionPoint(2500).requiredWisdom(15).nonInheritable().notSold()));
    public static final DeferredHolder<UmaSkill, UmaSkill> GALAXY_DESTROYER = SKILLS.register("galaxy_destroyer",
            () -> new RaceOnlySkill(new UmaSkill.Builder().level(3).type(SkillType.BUFF).actionPoint(3000).requiredWisdom(15).nonInheritable().notSold()));
    /** Dominator evolved. How it evolves is still open: by command for now. */
    public static final DeferredHolder<UmaSkill, UmaSkill> REQUIEM = SKILLS.register("requiem",
            () -> new RequiemSkill(new UmaSkill.Builder().level(2).rarity(SkillRarity.EVOLVED).type(SkillType.HINDER).actionPoint(700).requiredWisdom(13)
                    .nonInheritable().notSold()));
}
