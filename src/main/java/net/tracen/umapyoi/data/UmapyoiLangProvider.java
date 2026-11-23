package net.tracen.umapyoi.data;

import java.time.Month;
import java.util.function.Supplier;

import cn.mcmod_mmf.mmlib.data.AbstractLangProvider;
import net.minecraft.Util;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.block.BlockRegistry;
import net.tracen.umapyoi.data.builtin.CostumeDataRegistry;
import net.tracen.umapyoi.data.builtin.SupportCardRegistry;
import net.tracen.umapyoi.data.builtin.UmaDataRegistry;
import net.tracen.umapyoi.effect.MobEffectRegistry;
import net.tracen.umapyoi.item.ItemRegistry;
import net.tracen.umapyoi.registry.TrainingSupportRegistry;
import net.tracen.umapyoi.registry.UmaFactorRegistry;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.cosmetics.CosmeticData;
import net.tracen.umapyoi.registry.factors.UmaFactor;
import net.tracen.umapyoi.registry.races.Race;
import net.tracen.umapyoi.registry.races.RaceRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.registry.training.TrainingSupport;
import net.tracen.umapyoi.registry.training.card.SupportCard;
import net.tracen.umapyoi.registry.umadata.UmaData;
import org.apache.commons.lang3.StringUtils;

public class UmapyoiLangProvider extends AbstractLangProvider {

    public UmapyoiLangProvider(PackOutput gen) {
        super(gen, Umapyoi.MODID, "en_us");
    }


    @Override
    protected void addTranslations() {
        add("attribute.umapyoi.generic.sprint_speed", "Sprint Speed");
        add("attribute.umapyoi.generic.exhaustion_penalty", "Exhaustion Penalty");
        add("key.umapyoi.configure", "Configure HUD");
        add("setting.umapyoi.save", "Save");
        add("setting.umapyoi.discard", "Discard");

        add(ItemRegistry.HACHIMI_MID.get(), "Hachimi Drink");
        add(ItemRegistry.HACHIMI_BIG.get(), "Hachimi Extreme");
        add(ItemRegistry.ROYAL_BITTER.get(), "Royal BitterJuice");
        add(ItemRegistry.JEWEL.get(), "Carat");

        add(ItemRegistry.CUPCAKE.get(), "Plain Cupcake");
        add(ItemRegistry.SWEET_CUPCAKE.get(), "Sweet Cupcake");
        add(ItemRegistry.BLANK_UMA_SOUL.get(), "Faded Umamusume Soul");
        add(ItemRegistry.UMA_SOUL.get(), "Umamusume Soul");
        add(ItemRegistry.UMA_FACTOR_ITEM.get(), "Umamusume Spirit");

        add(ItemRegistry.TRAINNING_SUIT.get(), "Tracen Training Uniform");
        add(ItemRegistry.SUMMER_UNIFORM.get(), "Tracen Summer Uniform");
        add(ItemRegistry.WINTER_UNIFORM.get(), "Tracen Winter Uniform");

        addCostume(CostumeDataRegistry.COMMON_COSTUME, "White T-Shirt");
        addCostume(CostumeDataRegistry.STARTING_FUTURE, "Starting Future Costume");
        addCostume(CostumeDataRegistry.KINDERGARTEN_UNIFORM, "Kindergarten Uniform");
        addCostume(CostumeDataRegistry.KASAMATSU_TRAINING_UNIFORM, "Kasamatsu Tracen Training Uniform");

        add(ItemRegistry.SPEED_LOW_ITEM.get(), "Speed Notepad");
        add(ItemRegistry.SPEED_MID_ITEM.get(), "Speed Writings");
        add(ItemRegistry.SPEED_HIGH_ITEM.get(), "Speed Scroll");

        add(ItemRegistry.STAMINA_LOW_ITEM.get(), "Stamina Notepad");
        add(ItemRegistry.STAMINA_MID_ITEM.get(), "Stamina Writings");
        add(ItemRegistry.STAMINA_HIGH_ITEM.get(), "Stamina Scroll");

        add(ItemRegistry.STRENGTH_LOW_ITEM.get(), "Power Notepad");
        add(ItemRegistry.STRENGTH_MID_ITEM.get(), "Power Writings");
        add(ItemRegistry.STRENGTH_HIGH_ITEM.get(), "Power Scroll");

        add(ItemRegistry.MENTALITY_LOW_ITEM.get(), "Guts Notepad");
        add(ItemRegistry.MENTALITY_MID_ITEM.get(), "Guts Writings");
        add(ItemRegistry.MENTALITY_HIGH_ITEM.get(), "Guts Scroll");

        add(ItemRegistry.WISDOM_LOW_ITEM.get(), "Wit Notepad");
        add(ItemRegistry.WISDOM_MID_ITEM.get(), "Wit Writings");
        add(ItemRegistry.WISDOM_HIGH_ITEM.get(), "Wit Scroll");

        add(ItemRegistry.SMALL_ENERGY_DRINK.get(), "Vital 20");
        add(ItemRegistry.MEDIUM_ENERGY_DRINK.get(), "Vital 40");
        add(ItemRegistry.LARGE_ENERGY_DRINK.get(), "Vital 65");

        add(ItemRegistry.SKILL_BOOK.get(), "Skill Book");

        add(ItemRegistry.BLANK_TICKET.get(), "Blank Ticket");

        add(ItemRegistry.UMA_TICKET.get(), "Umamusume Ticket");
        add(ItemRegistry.SR_UMA_TICKET.get(), "Golden Umamusume Ticket");
        add(ItemRegistry.SSR_UMA_TICKET.get(), "Rainbow Umamusume Ticket");

        add(ItemRegistry.CARD_TICKET.get(), "Support Card Ticket");
        add(ItemRegistry.SR_CARD_TICKET.get(), "Golden Support Card Ticket");
        add(ItemRegistry.SSR_CARD_TICKET.get(), "Rainbow Support Card Ticket");

        add(ItemRegistry.CRYSTAL_SILVER.get(), "Silver Umamusume Crystal");
        add(ItemRegistry.CRYSTAL_GOLD.get(), "Golden Umamusume Crystal");
        add(ItemRegistry.CRYSTAL_RAINBOW.get(), "Rainbow Umamusume Crystal");

        add(ItemRegistry.HORSESHOE_SILVER.get(), "Silver Cleats");
        add(ItemRegistry.HORSESHOE_GOLD.get(), "Golden Cleats");
        add(ItemRegistry.HORSESHOE_RAINBOW.get(), "Rainbow Cleats");

        add(ItemRegistry.NAGINATA.get(), "Naginata");
        add(ItemRegistry.BASEBALL_BAT.get(), "Metal Baseball Bat");

        add(ItemRegistry.FACTOR_SHARD.get(), "Factor Shard");
        add(ItemRegistry.UMA_RACE_TICKET.get(), "Race Ticket");

        add(BlockRegistry.THREE_GODDESS.get(), "Three Goddesses Statue");
        add(BlockRegistry.THREE_GODDESS_UPPER.get(), "Three Goddesses Statue");
        add(BlockRegistry.TRAINING_FACILITY.get(), "Training Terminal");
        add(BlockRegistry.SKILL_LEARNING_TABLE.get(), "Skill Learning Table");
        add(BlockRegistry.REGISTER_LECTERN.get(), "Retire Register Lectern");

        add(BlockRegistry.SILVER_UMA_PEDESTAL.get(), "Silver Umamusume Pedestal");
        add(BlockRegistry.SILVER_SUPPORT_ALBUM_PEDESTAL.get(), "Silver Support Album Pedestal");

        add(BlockRegistry.UMA_PEDESTAL.get(), "Golden Umamusume Pedestal");
        add(BlockRegistry.SUPPORT_ALBUM_PEDESTAL.get(), "Golden Support Album Pedestal");

        add(BlockRegistry.DISASSEMBLY_BLOCK.get(), "Transfer Register Lectern");
        add(BlockRegistry.UMA_SELECT_BLOCK.get(), "Selection Lectern");

        add(BlockRegistry.UMA_STATUES.get(), "Umamusume Statue");

        add(BlockRegistry.RACE_REGISTER_BLOCK.get(), "Race Registration Terminal");
        add(BlockRegistry.FACTOR_DECOMPOSE_TABLE.get(), "Factor Decomposition Table");
        add(BlockRegistry.FACTOR_RESEARCH_TABLE.get(), "Factor Research Table");
        add(BlockRegistry.GATE_DOOR.get(), "Gate Door");
        add(BlockRegistry.GATE.get(), "Gate");

        add(ItemRegistry.SWIMSUIT.get(), "Tracen Swimsuit");

        add(MobEffectRegistry.PANICKING.get(), "Panicking");
        add(MobEffectRegistry.FRIGHT.get(), "Fright");
        add(MobEffectRegistry.MOOD_BONUS.get(), "Hyper Mood");
        add(MobEffectRegistry.NIGHT_OWL.get(), "Night Owl");
        add(MobEffectRegistry.SLOW_METABOLISM.get(), "Slow Metabolism");

        add("curios.modifiers.uma_soul", "When being umamusume:");
        add("curios.modifiers.uma_suit", "When wearing apparel:");

        add("curios.identifier.uma_soul", "Umamusume Soul");
        add("curios.identifier.uma_suit", "Umamusume Apparel");
        add("itemGroup.umapyoi", "Umapyoi");
        add("itemGroup.umapyoi.souls", "Umamusume Souls");
        add("itemGroup.umapyoi.blank_souls", "Faded Umamusume Souls");
        add("itemGroup.umapyoi.cards", "Support Card");
        add("itemGroup.umapyoi.race_tickets", "Race Tickets");
        add("itemGroup.umapyoi.factor_shards", "Factor Shards");

        addTooltip(".uma_soul.should_retire", "Couldn't continue training anymore, maybe it's time to retire.");

        // Three-year career
        addTooltip(".career.undecided", "Training route not chosen. Put it in a Training Terminal to choose.");
        addTooltip(".career.in_career", "Three-year career: %s");
        addTooltip(".career.finished", "Three-year career complete. Ready to retire.");
        addTooltip(".training.choose_route", "Press Start on the right for auto-training. For a three-year career, put it in a Self-Training Terminal.");
        addTooltip(".training.career_soul", "A soul in or after its three-year career can't be auto-trained.");
        add("gui.umapyoi.training.auto_accept", "Auto-accept: %s");
        add("gui.umapyoi.training.auto_accept.tooltip", "When on, new souls put in (e.g. by a hopper) go straight to Auto Training.");
        add("umapyoi.career.calendar.header", "Only");
        add("umapyoi.career.calendar.footer", "more");
        add("key.umapyoi.career_events", "Show Career Events");
        add("umapyoi.career.events.header", "Events (%s)");
        add("umapyoi.career.events.close", "[%s] Close");
        add("umapyoi.career.events.empty", "Nothing right now");
        add("umapyoi.career.events.turns", "%s turns");
        add("umapyoi.career.event.kind.optional", "Optional");
        add("umapyoi.career.event.kind.mandatory", "Must");
        add("umapyoi.career.event.kind.state", "State");
        add("umapyoi.career.event.kind.notice", "Notice");
        add("umapyoi.career.event.want_to_run", "Want to run");
        add("umapyoi.career.event.want_to_run.hint", "Sprint for 30 seconds in a row");
        add("umapyoi.career.event.hungry", "Hungry");
        add("umapyoi.career.event.hungry.hint", "Eat anything");
        add("umapyoi.career.event.want_carrot", "Want a carrot");
        add("umapyoi.career.event.want_carrot.hint", "Eat a carrot or a carrot dish");
        add("umapyoi.career.event.completed", "Done: %s");
        add("umapyoi.career.event.failed", "Couldn't do \"%s\"... motivation and stats dropped");
        add("umapyoi.career.event.want_hachimi", "Want a Hachimi drink");
        add("umapyoi.career.event.want_hachimi.hint", "Drink a Hachimi Drink or Hachimi Extreme");
        add("umapyoi.career.event.change_outfit", "Want to change clothes");
        add("umapyoi.career.event.change_outfit.hint", "Put on a different outfit in the outfit slot");
        add("umapyoi.career.event.other_biome", "Want to see somewhere else");
        add("umapyoi.career.event.other_biome.hint", "Walk into another biome");
        add("umapyoi.career.event.watch_sunrise", "Want to see the sunrise");
        add("umapyoi.career.event.watch_sunrise.hint", "Be under the open sky at dawn");
        add("umapyoi.career.event.watch_sunset", "Want to see the sunset");
        add("umapyoi.career.event.watch_sunset.hint", "Be under the open sky at dusk");
        add("umapyoi.career.event.cancelled", "Too late for \"%s\" now");
        add("umapyoi.career.event.want_see_horse", "Want to see horses");
        add("umapyoi.career.event.want_see_horse.hint", "Stay near a horse for 30 seconds");
        add("umapyoi.career.event.want_go_home", "Want to go home");
        add("umapyoi.career.event.want_go_home.hint", "Get back near your respawn point");
        add("umapyoi.career.event.want_company", "Want company");
        add("umapyoi.career.event.want_company.hint", "Stay with a companion for 1 minute");
        add("umapyoi.career.event.want_light", "Want somewhere bright");
        add("umapyoi.career.event.want_light.hint", "Stay where the light is 12 or more for 1 minute");
        add("umapyoi.career.event.want_food", "Craving %s");
        add("umapyoi.career.event.want_food.hint", "Eat %s");
        add("umapyoi.career.event.want_golden_carrot", "Craving a golden carrot");
        add("umapyoi.career.event.want_golden_carrot.hint", "Eat a golden carrot");
        add("umapyoi.career.event.want_soup", "Want soup or stew");
        add("umapyoi.career.event.want_soup.hint", "Eat any soup or stew");
        add("umapyoi.career.event.want_sleep", "Sleepy");
        add("umapyoi.career.event.want_sleep.hint", "Sleep in a bed");
        add("umapyoi.career.event.want_indoors", "Want to be indoors");
        add("umapyoi.career.event.want_indoors.hint", "Stay under a roof for 1 minute while it rains");
        add("umapyoi.career.event.lazy_period", "Lazy period");
        add("umapyoi.career.event.lazy_period.hint", "Career points halved; a liked food or a mood food ends it");
        add("umapyoi.career.event.want_fight", "Want a fight");
        add("umapyoi.career.event.want_fight.hint", "Defeat a hostile mob");
        add("umapyoi.career.event.want_learn", "Want to learn something");
        add("umapyoi.career.event.want_learn.hint", "Enchant, use an anvil, or learn a skill");
        add("umapyoi.career.event.ended", "\"%s\" is over");
        add("umapyoi.career.event.new_year_first", "A New Year Begins");
        add("umapyoi.career.event.new_year_first.hint", "Seems like a very special day in a career, doesn't it? Relax and enjoy the feeling.");
        add("umapyoi.career.event.new_year_last", "New Year... Once More");
        add("umapyoi.career.event.new_year_last.hint", "In the blink of an eye, it's already the last one... Hard to let go.");
        add("umapyoi.career.event.acupuncture", "Acupuncturist's visit");
        add("umapyoi.career.event.acupuncture.hint.success", "Every stat cap +1");
        add("umapyoi.career.event.acupuncture.hint.fail", "Didn't seem to help... motivation dropped");
        add("umapyoi.career.event.random_stat", "A lucky find");
        add("umapyoi.career.event.random_stat.hint", "%s +1");
        add("umapyoi.career.event.skill_learned", "Learned %s");
        add("umapyoi.career.event.skill_learned.hint", "A skill from a support card");
        add("umapyoi.career.event.notice_message", "[%s] %s");
        add("umapyoi.uma.no_riding", "An Umamusume doesn't ride horses");
        add("umapyoi.career.notebook_cap", "%s cap +%s");
        add("umapyoi.command.racetrack.built", "Racecourse built: one lap is %s blocks (%s m), %s × %s blocks in all, took %s ms.");
        add("umapyoi.command.racetrack.busy", "A racecourse is already being built.");
        add("umapyoi.command.racetrack.progress", "Building the racecourse... %s%%");
        add("umapyoi.command.race.already_there", "You're already at the racecourse.");
        add("umapyoi.command.race.missing", "The racecourse world isn't loaded.");
        add("umapyoi.command.race.not_there", "You're not at the racecourse.");
        add("umapyoi.command.race.preparing", "Building the racecourse; you'll be taken there when it's ready.");
        add("umapyoi.command.race.no_course", "The racecourse isn't built yet; use /umapyoi race enter first.");
        add("umapyoi.command.race.no_trial", "You have no time trial running.");
        add("umapyoi.command.race.trial_busy", "A time trial is already running.");
        add("umapyoi.race.trial.cancelled", "Time trial cancelled.");
        add("umapyoi.race.trial.finish_subtitle", "%s m time trial");
        add("umapyoi.race.trial.go", "Go!");
        add("umapyoi.race.trial.hud", "%s m to go   %s");
        add("umapyoi.race.trial.result", "Time trial, %s m: %s");
        add("item.umapyoi.career_keepsake", "Memories");
        add("item.umapyoi.career_keepsake.named", "%s's Memories");
        add("item.umapyoi.career_keepsake.desc", "What an unforgettable time it was.");
        add("block.umapyoi.career_keepsake", "Memories");
        add("umapyoi.career.started", "Her three years begin. Time only passes while you wear her soul.");
        add("umapyoi.career.finished", "Three years complete! Take her soul to the Retire Register Lectern.");
        add("umapyoi.career.death", "A setback in her career: stats lowered, motivation at its lowest.");

        // Self-training terminal and traits
        add("gui.umapyoi.training.start", "Start");
        add("gui.umapyoi.training.start.tooltip", "Auto-train this soul with the cards inside. For a three-year career, use the Self-Training Terminal.");
        add("block.umapyoi.self_training_terminal", "Self-Training Terminal");
        add("container.umapyoi.self_training_terminal", "Self-Training Terminal");
        add("tooltip.umapyoi.self_training.need_undecided", "Only a soul whose training route isn't chosen yet can go here.");
        add("gui.umapyoi.self_training.personality", "Traits");
        add("gui.umapyoi.self_training.skill", "Skill");
        add("gui.umapyoi.self_training.start", "Start");
        add("gui.umapyoi.self_training.put_soul", "Put in an undecided soul");
        add("gui.umapyoi.self_training.status", "Undecided - Physique %s");
        add("gui.umapyoi.self_training.stat", "Stat");
        add("gui.umapyoi.self_training.current", "Now (cap)");
        add("gui.umapyoi.self_training.guaranteed", "Min.");
        add("gui.umapyoi.self_training.guaranteed.tip1", "The grade you will reach at least when the three years end.");
        add("gui.umapyoi.self_training.guaranteed.tip2", "It changes with the cards and the soul's physique.");
        add("gui.umapyoi.self_training.guaranteed.tip3", "The more you do day to day, the higher you end up.");
        add("gui.umapyoi.self_training.need_positive", "Choose a positive trait first.");
        add("gui.umapyoi.self_training.speed", "Speed");
        add("gui.umapyoi.self_training.stamina", "Stamina");
        add("gui.umapyoi.self_training.strength", "Power");
        add("gui.umapyoi.self_training.guts", "Guts");
        add("gui.umapyoi.self_training.wisdom", "Wit");
        add("gui.umapyoi.self_training.personality_line", "Traits: ");
        add("gui.umapyoi.self_training.skill_line", "Skill: ");
        add("gui.umapyoi.self_training.not_chosen", "not chosen");
        add("gui.umapyoi.self_training.none", "none");
        add("gui.umapyoi.self_training.negative", "Negative");
        add("gui.umapyoi.self_training.positive", "Positive");
        add("gui.umapyoi.self_training.hover_for_more", "Hover to see what they do");
        add("gui.umapyoi.self_training.uma_traits", "Uma traits: ");
        add("gui.umapyoi.self_training.likes", "Likes: ");
        add("gui.umapyoi.self_training.dislikes", "Dislikes: ");
        add("gui.umapyoi.self_training.separator", ", ");
        add("gui.umapyoi.self_training.offered_skills", "Basic skills on offer");
        add("gui.umapyoi.self_training.chosen", "Chosen: %s");
        add("gui.umapyoi.self_training.conflict", "Can't be picked together with %s");
        add("umapyoi.trait.personality.timid", "Timid");
        add("umapyoi.trait.personality.timid.desc", "When hurt, may get Slowness I for 3 s");
        add("umapyoi.trait.personality.cowardly", "Cowardly");
        add("umapyoi.trait.personality.cowardly.desc", "When hurt with no companion within 8 blocks, get Weakness I for 5 s");
        add("umapyoi.trait.personality.dull", "Dull");
        add("umapyoi.trait.personality.dull.desc", "Whims come half as often; motivation is harder to raise");
        add("umapyoi.trait.personality.afraid_of_dark", "Afraid of the Dark");
        add("umapyoi.trait.personality.afraid_of_dark.desc", "Darkness in places with light level 3 or less");
        add("umapyoi.trait.personality.glutton", "Glutton");
        add("umapyoi.trait.personality.glutton.desc", "Hunger drains x1.3");
        add("umapyoi.trait.personality.picky_eater", "Picky Eater");
        add("umapyoi.trait.personality.picky_eater.desc", "Motivation drops on disliked food, or the same food 3 times in a row");
        add("umapyoi.trait.personality.sleepy", "Sleepy");
        add("umapyoi.trait.personality.sleepy.desc", "Night Owl sets in twice as fast");
        add("umapyoi.trait.personality.hates_rain", "Hates Rain");
        add("umapyoi.trait.personality.hates_rain.desc", "Motivation slowly drops while out in the rain");
        add("umapyoi.trait.personality.lazy", "Lazy");
        add("umapyoi.trait.personality.lazy.desc", "Lazy spells happen more often");
        add("umapyoi.trait.personality.impatient", "Impatient");
        add("umapyoi.trait.personality.impatient.desc", "Required whims have less time; failing costs more motivation");
        add("umapyoi.trait.personality.fragile", "Fragile");
        add("umapyoi.trait.personality.fragile.desc", "Twice the chance to lose motivation when hurt");
        add("umapyoi.trait.personality.light_averse", "Light-Averse");
        add("umapyoi.trait.personality.light_averse.desc", "Training points x0.9 in daylight, x1.15 at night");
        add("umapyoi.trait.personality.tenacious", "Tenacious");
        add("umapyoi.trait.personality.tenacious.desc", "Resistance I at 25%% health or less");
        add("umapyoi.trait.personality.cheerful", "Cheerful");
        add("umapyoi.trait.personality.cheerful.desc", "Half the chance to lose motivation");
        add("umapyoi.trait.personality.studious", "Studious");
        add("umapyoi.trait.personality.studious.desc", "+15%% experience");
        add("umapyoi.trait.personality.steady", "Steady");
        add("umapyoi.trait.personality.steady.desc", "+10%% training points while motivation is Normal or better");
        add("umapyoi.trait.personality.excitable", "Excitable");
        add("umapyoi.trait.personality.excitable.desc", "Combat training points x1.2; no action point recovery for 10 s after a fight");
        add("umapyoi.trait.uma.fear", "Afraid of %s");
        add("umapyoi.trait.uma.fear.desc", "Panics when this creature is nearby");
        add("umapyoi.trait.uma.claustrophobia", "Claustrophobic");
        add("umapyoi.trait.uma.claustrophobia.desc", "Panics after a while in cramped places without sky");
        add("umapyoi.trait.uma.fear_of_fire", "Afraid of Fire");
        add("umapyoi.trait.uma.fear_of_fire.desc", "Panics near fire or lava");
        add("umapyoi.trait.uma.restless", "Restless");
        add("umapyoi.trait.uma.restless.desc", "Motivation slowly drops after a long time without sprinting");
        add("umapyoi.trait.uma.curious", "Curious");
        add("umapyoi.trait.uma.curious.desc", "Motivation +1 on entering a new biome (with cooldown)");
        add("umapyoi.trait.uma.night_vision", "Night Vision");
        add("umapyoi.trait.uma.night_vision.desc", "Night Vision outdoors at night");
        add("umapyoi.trait.uma.horse_lover", "Loves Horses");
        add("umapyoi.trait.uma.horse_lover.desc", "Motivation slowly rises near horses");

        // Three Goddesses blessings
        add("effect.umapyoi.blessing_renewal", "Renewal");
        add("umapyoi.blessing.renewal.short", "Refills absorption");
        add("umapyoi.blessing.renewal.flavor", "Your life will go on forever, always protected.");
        add("umapyoi.blessing.renewal.effect.1", "4 absorption, refilled every 30 s");
        add("umapyoi.blessing.renewal.effect.2", "6 absorption, refilled every 25 s");
        add("umapyoi.blessing.renewal.effect.3", "8 absorption, refilled every 20 s");
        add("effect.umapyoi.blessing_happiness", "Happiness");
        add("umapyoi.blessing.happiness.short", "Stay unhurt, mood up");
        add("umapyoi.blessing.happiness.flavor", "A strange gentleness and comfort wash over you, and you accept them.");
        add("umapyoi.blessing.happiness.effect.1", "Take no damage for 60 s: motivation +1");
        add("umapyoi.blessing.happiness.effect.2", "Take no damage for 45 s: motivation +1");
        add("umapyoi.blessing.happiness.effect.3", "Take no damage for 30 s: motivation +1");
        add("effect.umapyoi.blessing_sorrow", "Sorrow");
        add("umapyoi.blessing.sorrow.short", "Guards you on death");
        add("umapyoi.blessing.sorrow.flavor", "The goddess from the underworld weeps, yet in her weeping you sense a feeling that is hard to notice.");
        add("umapyoi.blessing.sorrow.effect.1", "Dying with it: no stat loss. Each hit takes 60 s off");
        add("umapyoi.blessing.sorrow.effect.2", "Dying with it: no stat or motivation loss. Each hit takes 60 s off");
        add("umapyoi.blessing.sorrow.effect.3", "Stops one death (back to 1 health). Each hit takes 60 s off");
        add("effect.umapyoi.blessing_survival", "Survival");
        add("umapyoi.blessing.survival.short", "More saturation");
        add("umapyoi.blessing.survival.flavor", "A roar from the jungle makes you tremble all over, but the feeling does not seem to come from fear.");
        add("umapyoi.blessing.survival.effect.1", "+3 saturation from all food");
        add("umapyoi.blessing.survival.effect.2", "+3 saturation from all food");
        add("umapyoi.blessing.survival.effect.3", "+3 saturation from all food");
        add("effect.umapyoi.blessing_gale", "Gale");
        add("umapyoi.blessing.gale.short", "Faster sprinting");
        add("umapyoi.blessing.gale.flavor", "Today's storm is wilder than usual, but perhaps it is only the roar of the chase.");
        add("umapyoi.blessing.gale.effect.1", "Sprint speed +5%%, sprinting hunger -20%%");
        add("umapyoi.blessing.gale.effect.2", "Sprint speed +10%%, sprinting hunger -30%%");
        add("umapyoi.blessing.gale.effect.3", "Sprint speed +15%%, sprinting hunger -40%%");
        add("effect.umapyoi.blessing_slumber", "Slumber");
        add("umapyoi.blessing.slumber.short", "Sleep lifts mood");
        add("umapyoi.blessing.slumber.flavor", "peace.");
        add("umapyoi.blessing.slumber.effect.1", "No Night Owl. Sleeping gives +1 more motivation");
        add("umapyoi.blessing.slumber.effect.2", "No Night Owl. Sleeping gives +1 more motivation");
        add("umapyoi.blessing.slumber.effect.3", "No Night Owl. Sleeping gives +2 more motivation");
        add("effect.umapyoi.blessing_calm", "Calm");
        add("umapyoi.blessing.calm.short", "Steadier when hurt");
        add("umapyoi.blessing.calm.flavor", "For a moment it seemed you could hear nothing at all, but that was all.");
        add("umapyoi.blessing.calm.effect.1", "Chance to lose motivation when hurt -30%%. No panicking");
        add("umapyoi.blessing.calm.effect.2", "Chance to lose motivation when hurt -60%%. No panicking");
        add("umapyoi.blessing.calm.effect.3", "Getting hurt never lowers motivation. No panicking");
        add("effect.umapyoi.blessing_focus", "Focus");
        add("umapyoi.blessing.focus.short", "More career growth");
        add("umapyoi.blessing.focus.flavor", "History pushes history forward, so the Umamusume of a new era may once again reach the summit of their time. That urge seems to be felt right now.");
        add("umapyoi.blessing.focus.effect.1", "Career training points x1.05 (three-year career only)");
        add("umapyoi.blessing.focus.effect.2", "Career training points x1.10 (three-year career only)");
        add("umapyoi.blessing.focus.effect.3", "Career training points x1.15 (three-year career only)");
        add("gui.umapyoi.blessing.title", "Three Goddesses' Blessing");
        add("gui.umapyoi.blessing.held", "Blessings %s/2");
        add("gui.umapyoi.blessing.empty", "(empty)");
        add("gui.umapyoi.blessing.offering", "Offering");
        add("gui.umapyoi.blessing.points", "Grace");
        add("gui.umapyoi.blessing.points_line", "Grace %s / 64");
        add("gui.umapyoi.blessing.pray", "Pray");
        add("gui.umapyoi.blessing.full", "Max 2 held");
        add("gui.umapyoi.blessing.need_points", "Need %s pts");
        add("gui.umapyoi.blessing.need_soul", "Wear a soul");
        add("gui.umapyoi.blessing.choose", "Choose (I)");
        add("gui.umapyoi.blessing.choose_title", "Choose a blessing (level I only)");
        add("gui.umapyoi.blessing.pick_one", "Pick one");
        add("gui.umapyoi.blessing.accept", "Accept");
        add("gui.umapyoi.blessing.back", "Back");
        add("gui.umapyoi.blessing.extend", "Ext");
        add("gui.umapyoi.blessing.extend_cost", "Extending costs %s grace");
        add("gui.umapyoi.blessing.duration", "Lasts %s");
        add("gui.umapyoi.blessing.minutes", "%s min");
        add("gui.umapyoi.blessing.minutes_range", "%s-%s min");
        add("gui.umapyoi.blessing.seconds", "%s s");
        add("gui.umapyoi.blessing.offering_hint", "Offer items for grace: iron ingot 1, gold ingot 2, Carat 4, diamond 8, netherite ingot 32; blocks are worth 9x. Offerings are not returned.");
        add("umapyoi.blessing.message.need_soul", "Only a player wearing an Umamusume soul can receive blessings.");
        add("umapyoi.blessing.message.too_many", "You already have 2 blessings. You can still extend them.");
        add("umapyoi.blessing.message.not_enough", "Not enough grace: %s needed.");
        add("umapyoi.blessing.message.received", "The Three Goddesses bless you: %s");
        add("umapyoi.career.death_blessed", "The blessing of Sorrow protected you: no stats were lost.");

        // Why the training terminal / Three Goddesses / race terminal rejects something
        addTooltip(".training.card_conflict", "Conflicts with %s already inside (same card, or a shared supporter).");
        addTooltip(".training.card_has_soul", "This card's supporters include %s, so they can't train together.");
        addTooltip(".training.invalid_support", "A support slot holds something that isn't a support card: %s");
        addTooltip(".training.need_uma_soul", "Only an Umamusume Soul can go here.");
        addTooltip(".training.soul_is_supporter", "This Umamusume is a supporter of %s, so they can't train together.");
        addTooltip(".training.soul_retired", "This soul has already retired and can't be trained.");
        addTooltip(".three_goddess.factor_same_as_factor", "The two spirits must be different Umamusume (%s is already inside).");
        addTooltip(".three_goddess.factor_same_as_soul", "Same Umamusume as the soul %s; a soul can't inherit from itself.");
        addTooltip(".three_goddess.need_factor", "Only an Umamusume Spirit can go here.");
        addTooltip(".three_goddess.need_faded_soul", "Only a Faded Umamusume Soul can go here.");
        addTooltip(".three_goddess.need_two_factors", "Needs two spirits");
        addTooltip(".three_goddess.soul_same_as_factor", "Same Umamusume as the spirit %s; a soul can't inherit from itself.");
        addTooltip(".race.unavailable", "Can't enter this race");
        addTooltip(".race.unavailable.already_debuted", "Already debuted, so debut races are closed.");
        addTooltip(".race.unavailable.invalid", "This race ticket is not valid.");
        addTooltip(".race.unavailable.need_debut", "Hasn't debuted yet. Run a debut race first.");
        addTooltip(".race.unavailable.need_previous_race", "Must have run one of these first: %s");
        addTooltip(".race.unavailable.time_passed", "This race's date has passed (schedule is at %s).");
        addTooltip(".race.unavailable.too_early", "Too early in the schedule; open after %s.");
        addTooltip(".race.unavailable.wrong_status", "Soul is %s; this race only takes: %s");
        addTooltip(".race.status.retired", "retired");
        addTooltip(".race.status.trained", "trained");
        addTooltip(".race.status.untrained", "untrained");
        addTooltip(".race.time.none", "no races yet");

        addTooltip(".umadata.name", "Umamusume's Name:%s");
        addTooltip(".support_card.name", "Support Card:%s");
        addTooltip(".umafactor.data", "Has %s and %d more factors.");

        addTooltip(".supports", "Supports:");
        addTooltip(".supporters", "Supporters:");

        addTooltip(".support_card.press_shift_for_supports", "Press Shift Button for support details.");
        addTooltip(".support_card.press_ctrl_for_supporters", "Press Ctrl Button for supporter details.");
        addTooltip(".uma_soul.soul_details", "Umamusume Soul Status:");
        addTooltip(".press_shift_for_details", "Press Shift Button for details.");
        addTooltip(".factors.factors_details", "Factors:");

        addTooltip(".uma_soul.ranking", "Total Ranking: %s");

        addTooltip(".uma_soul.speed_details", "Speed: %s / %s");
        addTooltip(".uma_soul.stamina_details", "Stamina: %s / %s");
        addTooltip(".uma_soul.strength_details", "Power: %s / %s");
        addTooltip(".uma_soul.guts_details", "Guts: %s / %s");
        addTooltip(".uma_soul.wisdom_details", "Wit: %s / %s");

        addTooltip(".uma_soul.aptitude.details", "Aptitudes:");
        addTooltip(".uma_soul.aptitude.turf", "Turf %s");
        addTooltip(".uma_soul.aptitude.dirt", "Dirt %s");
        addTooltip(".uma_soul.aptitude.synthetic", "Synthetic %s");
        addTooltip(".uma_soul.aptitude.short", "Short %s");
        addTooltip(".uma_soul.aptitude.miles", "Mile %s");
        addTooltip(".uma_soul.aptitude.medium", "Medium %s");
        addTooltip(".uma_soul.aptitude.long", "Long %s");
        addTooltip(".uma_soul.aptitude.strategy", "Strategy: %s");
        addTooltip(".uma_soul.traits", "Traits:");

        addTooltip(".uma_soul.aptitude.strategy.front_runner", "Front");
        addTooltip(".uma_soul.aptitude.strategy.pace_chaser", "Pace");
        addTooltip(".uma_soul.aptitude.strategy.late_surger", "Late");
        addTooltip(".uma_soul.aptitude.strategy.end_closer", "End");
        addTooltip(".uma_soul.aptitude.strategy.runaway", "Runaway");
        
        add(BlockRegistry.RACE_SELECT_BLOCK.get(), "Race Selection Lectern");
        add("container.umapyoi.raceselect", "Race Selection Register");

        addSupport(TrainingSupportRegistry.SPEED_SUPPORT, "Speed Increase");
        addSupport(TrainingSupportRegistry.STAMINA_SUPPORT, "Stamina Increase");
        addSupport(TrainingSupportRegistry.STRENGTH_SUPPORT, "Power Increase");
        addSupport(TrainingSupportRegistry.GUTS_SUPPORT, "Guts Increase");
        addSupport(TrainingSupportRegistry.WISDOM_SUPPORT, "Wit Increase");
        addSupport(TrainingSupportRegistry.SKILL_SUPPORT, "Learning Skill");
        addSupport(TrainingSupportRegistry.AP_SUPPORT, "Action Pt Increase");

        addSupport(TrainingSupportRegistry.RANDOM_STATUS_SUPPORT, "Random Status Increase");
        addSupport(TrainingSupportRegistry.ACUPUNCTUIST_SUPPORT, "Acupunctuist Support");
        addSupport(TrainingSupportRegistry.MEMORY_SUPPORT, "Memory Increase");

        addFactor(UmaFactorRegistry.SPEED_FACTOR, "Speed Factor");
        addFactor(UmaFactorRegistry.STAMINA_FACTOR, "Stamina Factor");
        addFactor(UmaFactorRegistry.STRENGTH_FACTOR, "Power Factor");
        addFactor(UmaFactorRegistry.GUTS_FACTOR, "Guts Factor");
        addFactor(UmaFactorRegistry.WISDOM_FACTOR, "Wit Factor");

        addFactor(UmaFactorRegistry.PHYSIQUE_FACTOR, "Physique Factor");
        addFactor(UmaFactorRegistry.TELENT_FACTOR, "Talent Factor");
        addFactor(UmaFactorRegistry.MEMORY_FACTOR, "Memory Factor");
        addFactor(UmaFactorRegistry.ACTIONS_FACTOR, "Action Pt Factor");

        addFactor(UmaFactorRegistry.WHITE_SPEED_FACTOR, "Speed Experiences");
        addFactor(UmaFactorRegistry.WHITE_STAMINA_FACTOR, "Stamina Experiences");
        addFactor(UmaFactorRegistry.WHITE_STRENGTH_FACTOR, "Power Experiences");
        addFactor(UmaFactorRegistry.WHITE_GUTS_FACTOR, "Guts Experiences");
        addFactor(UmaFactorRegistry.WHITE_WISDOM_FACTOR, "Wit Experiences");
        addFactor(UmaFactorRegistry.WHITE_TELENT_FACTOR, "Effective Learning");
        addFactor(UmaFactorRegistry.WHITE_ACTIONS_FACTOR, "Action Planning");

        addFactorDetail(UmaFactorRegistry.SPEED_FACTOR, "Increase starting speed & max speed.");
        addFactorDetail(UmaFactorRegistry.STAMINA_FACTOR, "Increase starting stamina & max stamina.");
        addFactorDetail(UmaFactorRegistry.STRENGTH_FACTOR, "Increase starting power & max power.");
        addFactorDetail(UmaFactorRegistry.GUTS_FACTOR, "Increase starting guts & max guts.");
        addFactorDetail(UmaFactorRegistry.WISDOM_FACTOR, "Increase starting wit & max wit.");

        addFactorDetail(UmaFactorRegistry.PHYSIQUE_FACTOR, "Add more training times.");
        addFactorDetail(UmaFactorRegistry.TELENT_FACTOR, "Add more times for using training books.");
        addFactorDetail(UmaFactorRegistry.MEMORY_FACTOR, "Add more skill slots.");
        addFactorDetail(UmaFactorRegistry.ACTIONS_FACTOR, "Increase Max Action Pt");

        addFactorDetail(UmaFactorRegistry.WHITE_SPEED_FACTOR, "Increase starting speed & max speed.");
        addFactorDetail(UmaFactorRegistry.WHITE_STAMINA_FACTOR, "Increase starting stamina & max stamina.");
        addFactorDetail(UmaFactorRegistry.WHITE_STRENGTH_FACTOR, "Increase starting power & max power.");
        addFactorDetail(UmaFactorRegistry.WHITE_GUTS_FACTOR, "Increase starting guts & max guts.");
        addFactorDetail(UmaFactorRegistry.WHITE_WISDOM_FACTOR, "Increase starting wit & max wit.");
        addFactorDetail(UmaFactorRegistry.WHITE_TELENT_FACTOR, "Add more skill slots.");
        addFactorDetail(UmaFactorRegistry.WHITE_ACTIONS_FACTOR, "Slightly increase Max Action Pt");

        addSkill(UmaSkillRegistry.BASIC_PACE, "Basic Pace");
        addSkill(UmaSkillRegistry.LAST_LEG, "Homestretch Haste");
        addSkill(UmaSkillRegistry.HEART_AND_SOUL, "In Body and Mind");
        addSkill(UmaSkillRegistry.DEEP_BREATHS, "Deep Breaths");
        addSkill(UmaSkillRegistry.COOLDOWN, "Cooldown");
        addSkill(UmaSkillRegistry.SERENE, "Serene");

        addSkill(UmaSkillRegistry.STEEL_WILL, "Iron Will");
        addSkill(UmaSkillRegistry.BIG_EATER, "Big Eater");
        addSkill(UmaSkillRegistry.NUTRITIONAL_SUPPLEMENTS, "Hydrate");

        addSkill(UmaSkillRegistry.LOW_HEALTH_BUFF, "All I Have");
        addSkill(UmaSkillRegistry.ADV_LOWHEALTH_BUFF, "Prepared to Die");
        addSkill(UmaSkillRegistry.LOW_HEALTH_HEAL, "One Chance");
        addSkill(UmaSkillRegistry.ADV_LOWHEALTH_HEAL, "From the Brink");

        addSkill(UmaSkillRegistry.MOUNTAIN_CLIMBER, "Highlander");
        addSkill(UmaSkillRegistry.DIG_SPEED, "Mining Skills");
        addSkill(UmaSkillRegistry.TURF_RUNNER, "Turf Runner");
        addSkill(UmaSkillRegistry.DIRT_RUNNER, "Dirt Runner");
        addSkill(UmaSkillRegistry.SNOW_RUNNER, "Snow Runner");

        addSkill(UmaSkillRegistry.RAPID, "Rapid");
        addSkill(UmaSkillRegistry.DIVINE_SPEED, "Divine Speed");
        addSkill(UmaSkillRegistry.TOP_UMAMUSUME, "Japan's #1 Umamusume");

        addSkill(UmaSkillRegistry.INQUISITIVE_MIND, "Inquisitive Mind");

        addSkill(UmaSkillRegistry.TETHER, "Tether");
        addSkill(UmaSkillRegistry.DOMINATOR, "Dominator");

        addSkillDetail(UmaSkillRegistry.BASIC_PACE, "Slightly increase speed.");
        addSkillDetail(UmaSkillRegistry.LAST_LEG, "Slightly increase speed and power.");
        addSkillDetail(UmaSkillRegistry.HEART_AND_SOUL, "Increase speed and power.");
        addSkillDetail(UmaSkillRegistry.DEEP_BREATHS, "Slightly recover health.");
        addSkillDetail(UmaSkillRegistry.COOLDOWN, "Recover health.");
        addSkillDetail(UmaSkillRegistry.SERENE, "Slightly recover health and remove panicking.");

        addSkillDetail(UmaSkillRegistry.STEEL_WILL, "Increase resistance, boost mood and remove panicking.");
        addSkillDetail(UmaSkillRegistry.BIG_EATER, "Recover endurance and increase health");
        addSkillDetail(UmaSkillRegistry.NUTRITIONAL_SUPPLEMENTS, "Slightly recover endurance and increase health");

        addSkillDetail(UmaSkillRegistry.LOW_HEALTH_BUFF, "Slightly increase speed and power, increase more during low health.");
        addSkillDetail(UmaSkillRegistry.ADV_LOWHEALTH_BUFF, "Increase speed and power, increase more during low health.");
        addSkillDetail(UmaSkillRegistry.LOW_HEALTH_HEAL, "Slightly recover health, recover more during low health.");
        addSkillDetail(UmaSkillRegistry.ADV_LOWHEALTH_HEAL, "Recover health, recover more during low health.");

        addSkillDetail(UmaSkillRegistry.MOUNTAIN_CLIMBER, "Increase the step height.");
        addSkillDetail(UmaSkillRegistry.DIG_SPEED, "Increase the digging speed.");
        addSkillDetail(UmaSkillRegistry.TURF_RUNNER, "Increase speed on turf blocks.");
        addSkillDetail(UmaSkillRegistry.DIRT_RUNNER, "Increase speed on dirt blocks.");
        addSkillDetail(UmaSkillRegistry.SNOW_RUNNER, "Increase speed on snow blocks.");

        addSkillDetail(UmaSkillRegistry.RAPID, "Slightly increase speed and recover endurance.");
        addSkillDetail(UmaSkillRegistry.DIVINE_SPEED, "Increase speed and recover endurance.");
        addSkillDetail(UmaSkillRegistry.TOP_UMAMUSUME, "Increase speed and power, boost mood.");

        addSkillDetail(UmaSkillRegistry.INQUISITIVE_MIND, "Increase the attack speed, increase more when has higher wit.");

        addSkillDetail(UmaSkillRegistry.TETHER, "Slightly lower the speed of other entities.");
        addSkillDetail(UmaSkillRegistry.DOMINATOR, "Lower the speed of other entities.");

        addTooltip(".race.time.junior", "Junior");
        addTooltip(".race.time.classic", "Classic");
        addTooltip(".race.time.senior", "Senior");
        addTooltip(".race.time.after_regular", "After Twinkle Series");

        for (int i = 0; i < 12; i++) {
            String monthName = StringUtils.capitalize(Month.of(i + 1).name().toLowerCase());
            addTooltip(".race.time." + (i * 2 + 1), "Early " + monthName);
            addTooltip(".race.time." + ((i + 1) * 2), "Late " + monthName);
        }

        addTooltip(".race.tier.hint", "Tier: ");

        add("race.umapyoi.tier.debut", "Debut");
        add("race.umapyoi.tier.preop", "Pre-OP");
        add("race.umapyoi.tier.op", "Open");
        add("race.umapyoi.tier.giii", "GIII");
        add("race.umapyoi.tier.gii", "GII");
        add("race.umapyoi.tier.gi", "GI");

        addTooltip(".race.surface", "Surface: ");

        add("race.umapyoi.surface.turf", "Turf");
        add("race.umapyoi.surface.dirt", "Dirt");
        add("race.umapyoi.surface.synthetic", "Synthetic");
        add("race.umapyoi.surface.adaptive", "Adaptive");

        addTooltip(".race.distance", "Distance: ");

        addTooltip(".race.distance.sprint", "Sprint");
        addTooltip(".race.distance.mile", "Mile");
        addTooltip(".race.distance.medium", "Medium");
        addTooltip(".race.distance.long", "Long");
        addTooltip(".race.distance.adaptive", "Adaptive");

        addTooltip(".race.unit", " Meters");

        addTooltip(".race.field", "Field: ");
        add("race.umapyoi.field.hakodate", "Hakodate");
        add("race.umapyoi.field.chukyo", "Chukyo");
        add("race.umapyoi.field.niigata", "Niigata");
        add("race.umapyoi.field.sapporo", "Sapporo");
        add("race.umapyoi.field.kokura", "Kokura");
        add("race.umapyoi.field.hanshin", "Hanshin");
        add("race.umapyoi.field.nakayama", "Nakayama");
        add("race.umapyoi.field.tokyo", "Tokyo");
        add("race.umapyoi.field.kyoto", "Kyoto");
        add("race.umapyoi.field.fukushima", "Fukushima");
        add("race.umapyoi.field.kawasaki", "Kawasaki");
        add("race.umapyoi.field.funabashi", "Funabashi");
        add("race.umapyoi.field.morioka", "Morioka");
        add("race.umapyoi.field.ohi", "Ohi");
        add("race.umapyoi.field.adaptive", "Adaptive");

        addTooltip(".race.gainable_tags", "Race Tags: ");

        add("race.umapyoi.tags.triple_tiara", "Triple Tiara");
        add("race.umapyoi.tags.triple_crown", "Triple Crown");
        add("race.umapyoi.tags.eight_great_races", "Eight Great Races");

        add("race.umapyoi.tags.senior_spring_triple_crown", "Senior Spring Triple Crown");
        add("race.umapyoi.tags.senior_autumn_triple_crown", "Senior Autumn Triple Crown");
        add("race.umapyoi.tags.grand_prix", "Spring-Autumn Grand Prix");

        add("race.umapyoi.tags.mile", "Spring-Autumn Mile");
        add("race.umapyoi.tags.sprint", "Spring-Autumn Sprint");
        add("race.umapyoi.tags.dirt", "Spring-Autumn Dirt");

        add("container.umapyoi.three_goddess", "Three Goddesses Statue");
        add("container.umapyoi.training_facility", "Training Terminal");
        add("container.umapyoi.skill_learning", "Skill Learning Table");
        add("container.umapyoi.retire_register", "Retire Register");
        add("container.umapyoi.disassembly_block", "Transfer Register");
        add("container.umapyoi.umaselect", "Selection Register");
        add("container.umapyoi.race", "Race Registration Terminal");
        add("container.umapyoi.factor_decompose", "Factor Decomposition Table");
        add("container.umapyoi.factor_research", "Factor Research Table");


        add("key.category.umapyoi", "Umapyoi");
        add("key.umapyoi.use_skill", "Use Skill");
        add("key.umapyoi.select_former_skill", "Select Former Skill");
        add("key.umapyoi.select_latter_skill", "Select Latter Skill");

        add("umapyoi.skill.no_require", "Nothing required.");
        add("umapyoi.skill.require_wisdom", "%s wisdom required.");
        add("umapyoi.skill.slot_needed", "Has reached the learning limit.");
        add("umapyoi.skill.has_retired", "This Umamusume is retired.");
        add("umapyoi.skill.has_learned_skill", "This skill has learned.");
        add("umapyoi.skill.passive", "This is a passive skill.");
        add("umapyoi.not_enough_ap", "Not enough action points.");
        add("umapyoi.uma_pedestal.cannot_add_item", "Can not add item anymore.");

        add("umapyoi.motivation.bad", "Awful");
        add("umapyoi.motivation.down", "Bad");
        add("umapyoi.motivation.normal", "Normal");
        add("umapyoi.motivation.good", "Good");
        add("umapyoi.motivation.perfect", "Great");

        add("book.umapyoi.trainers_manual.title", "Tracen Trainers' Manual");
        add("book.umapyoi.trainers_manual.subtitle", "Tracen Academy");
        add("book.umapyoi.trainers_manual.landing_text", "This is the manual of Umapyoi mod.");

        add("umapyoi.no_umasoul_equiped", "Please equip Umamusume Soul first.");
        add("umapyoi.learning.no_learning_time", "No longer able to learn more.");
        add("umapyoi.learning.can_not_learn", "No longer possible to continue with this part of the learning process.");

        add("umapyoi.jei.disassembly", "Transfer Register");
        add("umapyoi.jei.gacha", "Pedestal Summon");
        add("umapyoi.jei.gacha.need_book", "Need a book on pedestal");

        add("entity.minecraft.villager.umapyoi.trainer", "Trainer");

        add("umastatus.level.0", "§7G-");
        add("umastatus.level.1", "§7G");
        add("umastatus.level.2", "§7G+");
        add("umastatus.level.3", "§5F");
        add("umastatus.level.4", "§5F+");
        add("umastatus.level.5", "§dE");
        add("umastatus.level.6", "§dE+");
        add("umastatus.level.7", "§bD");
        add("umastatus.level.8", "§bD+");
        add("umastatus.level.9", "§aC");
        add("umastatus.level.10", "§aC+");
        add("umastatus.level.11", "§4B");
        add("umastatus.level.12", "§4B+");
        add("umastatus.level.13", "§cA");
        add("umastatus.level.14", "§cA+");
        add("umastatus.level.15", "§eS");
        add("umastatus.level.16", "§eS+");
        add("umastatus.level.17", "§eSS");
        add("umastatus.level.18", "§eSS+");

        add("umastatus.level.19", "§9U§7G-");
        add("umastatus.level.20", "§9U§7G");
        add("umastatus.level.21", "§9U§7G+");
        add("umastatus.level.22", "§9U§5F-");
        add("umastatus.level.23", "§9U§5F");
        add("umastatus.level.24", "§9U§5F+");
        add("umastatus.level.25", "§9U§dE");
        add("umastatus.level.26", "§9U§dE+");
        add("umastatus.level.27", "§9U§bD");
        add("umastatus.level.28", "§9U§bD+");
        add("umastatus.level.29", "§9U§aC");
        add("umastatus.level.30", "§9U§aC+");
        add("umastatus.level.31", "§9U§4B");
        add("umastatus.level.32", "§9U§4B+");
        add("umastatus.level.33", "§9U§cA");
        add("umastatus.level.34", "§9U§cA+");
        add("umastatus.level.35", "§9U§eS");
        add("umastatus.level.36", "§9U§eS+");
        add("umastatus.level.37", "§9U§eSS");
        add("umastatus.level.38", "§9U§eSS+");
        add("umastatus.level.39", "§eMAX");

        add("gui.umapyoi.multiple_factor", "Multiple Factor");
        add("gui.umapyoi.hover_for_tooltip", "Hover for details");

        add("sound.umapyoi.gate_close", "Gate Closed");
        add("sound.umapyoi.gate_open", "Gate Opened");

        add("umapyoi.command.modify.emptyhand", "Player is not holding anything.");
        add("umapyoi.command.modify.notumasoul", "The item player is currently holding is not a uma soul.");
        add("umapyoi.command.modify.emptyequipment", "Player does not have any uma soul equipped.");
        add("umapyoi.command.modify.emptytarget", "Player is not holding or equipping any uma soul.");
        add("umapyoi.command.modify.novalidsoulexist", "Uma soul is not presenting.");
        add("umapyoi.command.modify.success", "Successfully modified the uma soul which %s is %s.");
        add("umapyoi.command.part.hand", "holding");
        add("umapyoi.command.part.equipped", "equipping");
        add("umapyoi.command.parse.unknown.growth", "Unknown value for parameter Growth: %s, expected untrained, trained, retired");
        add("umapyoi.command.parse.unknown.motivation", "Unknown value for parameter Motivation: %s, expected perfect, good, normal, down, bad");
        add("umapyoi.command.parse.unknown.skill", "Unknown skill: %s");
        add("umapyoi.command.skill.remove.notexist", "The skill %s does not exists on the target Uma Soul");
        add("umapyoi.command.parse.unknown.umadata", "Unknown UmaData: %s");
        add("umapyoi.command.give.success", "Successfully give %2$s to %1$s");

        addUma(UmaDataRegistry.COMMON_UMA, "Nameless Bay Umamusume");
        addUma(UmaDataRegistry.COMMON_UMA_A, "Nameless Perlino Umamusume");
        addUma(UmaDataRegistry.COMMON_UMA_B, "Nameless Gray Umamusume");
        addUma(UmaDataRegistry.COMMON_UMA_C, "Nameless Aqua Umamusume");

        addUma(UmaDataRegistry.MEJIRO_MCQUEEN, "Mejiro McQueen");
        addUma(UmaDataRegistry.GOLD_SHIP, "Gold Ship");
        addUma(UmaDataRegistry.SAKURA_CHIYONO_O, "Sakura Chiyono O");
        addUma(UmaDataRegistry.SPECIAL_WEEK, "Special Week");
        addUma(UmaDataRegistry.TOKAI_TEIO, "Tokai Teio");
        addUma(UmaDataRegistry.OGURI_CAP, "Oguri Cap");
        addUma(UmaDataRegistry.AGNUS_TACHYON, "Agnes Tachyon");
        addUma(UmaDataRegistry.HARU_URARA, "Haru Urara");
        addUma(UmaDataRegistry.TAMAMO_CROSS, "Tamamo Cross");
        addUma(UmaDataRegistry.OGURI_CAP_XMAS, "[Miraculous White Star] Oguri Cap");
        addUma(UmaDataRegistry.GOLD_SHIP_WATER, "[Run! Fun! Watergun!!] Gold Ship");
        addUma(UmaDataRegistry.SAKURA_BAKUSHIN_O, "Sakura Bakushin O");
        addUma(UmaDataRegistry.MATIKANEFUKUKITARU, "Matikane Fukukitaru");
        addUma(UmaDataRegistry.RICE_SHOWER, "Rice Shower");
        addUma(UmaDataRegistry.SEIUN_SKY, "Seiun Sky");
        addUma(UmaDataRegistry.VODKA, "Vodka");
        addUma(UmaDataRegistry.MANHATTAN_CAFE, "Manhattan cafe");
        addUma(UmaDataRegistry.MEJIRO_ARDAN, "Mejiro Ardan");
        addUma(UmaDataRegistry.DAITAKU_HELIOS, "Daitaku Helios");
        addUma(UmaDataRegistry.SWEEP_TOSHO, "Sweep Tosho");
        addUma(UmaDataRegistry.GOLD_CITY, "Gold City");
        addUma(UmaDataRegistry.CURREN_CHAN, "Curren Chan");
        addUma(UmaDataRegistry.ASTON_MACHAN, "Aston Machan");
        addUma(UmaDataRegistry.SILENCE_SUZUKA, "Silence Suzuka");
        addUma(UmaDataRegistry.MR_CB, "Mr.CB");
        addUma(UmaDataRegistry.TAMAMO_CROSS_FESTIVAL, "[Ferocious Thunder] Tamamo Cross");
        addUma(UmaDataRegistry.GRASS_WONDER, "Grass Wonder");
        addUma(UmaDataRegistry.NEO_UNIVERSE, "Neo Universe");
        addUma(UmaDataRegistry.NICE_NATURE, "Nice Nature");
        addUma(UmaDataRegistry.MAYANO_TOP_GUN, "Mayano Topgun");
        addUma(UmaDataRegistry.TAIKI_SHUTTLE, "Taiki Shuttle");
        addUma(UmaDataRegistry.MEISHO_DOTOU, "Meisho Doto");
        addUma(UmaDataRegistry.KITASAN_BLACK, "Kitasan Black");
        addUma(UmaDataRegistry.SATONO_DIAMOND, "Satono Diamond");
        addUma(UmaDataRegistry.COPANO_RICKEY, "Copano Rickey");
        addUma(UmaDataRegistry.CURREN_CHAN_DRESS, "[Calend's Ma Chérie] Curren Chan");
        addUma(UmaDataRegistry.SYMBOLI_RUDOLF, "Symboli Rudolf");
        addUma(UmaDataRegistry.NARITA_TOP_ROAD, "Narita Top Road");
        addUma(UmaDataRegistry.VENUS_PARK, "Venus Park");
        addUma(UmaDataRegistry.AGNUS_TACHYON_SWIM, "[Lunatic Lab] Agnus Tachyon");
        addUma(UmaDataRegistry.MIHONO_BOURBON, "Mihono Bourbon");
        addUma(UmaDataRegistry.MATIKANETANNHAUSER, "Matikanetannhauser");
        addUma(UmaDataRegistry.KAWAKAMI_PRINCESS, "Kawakami Princess");
        addUma(UmaDataRegistry.TWIN_TURBO, "Twin Turbo");
        addUma(UmaDataRegistry.LITTLE_COCON, "Little Cocon");
        addUma(UmaDataRegistry.SAKURA_LAUREL, "Sakura Laurel");
        addUma(UmaDataRegistry.NARITA_TAISHIN, "Narita Taishin");
        addUma(UmaDataRegistry.TM_OPERA_O, "TM Opera O");
        addUma(UmaDataRegistry.ADMIRE_VEGA, "Admire Vega");
        addUma(UmaDataRegistry.JUNGLE_POCKET, "Jungle Pocket");
        addUma(UmaDataRegistry.SYAMEIMARU_ZHENG, "Syameimaru Zheng");
        addUma(UmaDataRegistry.DUMNHEINT, "Dumnheint");
        addUma(UmaDataRegistry.DARLEY_ARABIAN, "Darley Arabian");
        addUma(UmaDataRegistry.GODOLPHIN_BARB, "Godolphin Barb");
        addUma(UmaDataRegistry.BYERLEY_TURK, "Byerley Turk");
        addUma(UmaDataRegistry.FINE_MOTION, "Fine Motion");
        addUma(UmaDataRegistry.SMART_FALCON, "Smart Falcon");
        addUma(UmaDataRegistry.HISHI_MIRACLE, "Hishi Miracle");
        addUma(UmaDataRegistry.GOLD_CITY_AUTUMN, "[Akizakura Danzatrice] Gold City");
        addUma(UmaDataRegistry.GRASS_WONDER_UMANET, "[Saint Jade Healer] Grass Wonder");
        addUma(UmaDataRegistry.SATONO_DIAMOND_FRENCH, "[Chevalier Blue] Satono Diamond");
        addUma(UmaDataRegistry.MANHATTAN_CAFE_VALENTINE, "[Willow Night] Manhattan cafe");
        addUma(UmaDataRegistry.DAIWA_SCARLET, "Daiwa Scarlet");
        addUma(UmaDataRegistry.WIN_VARIATION, "Win Variation");
        addUma(UmaDataRegistry.EL_CONDOR_PASA, "El Condor Pasa");
        addUma(UmaDataRegistry.KING_HALO, "King Halo");
        addUma(UmaDataRegistry.KING_HALO_WEDDING, "[Evergreen Identity] King Halo");
        addUma(UmaDataRegistry.HOKKO_TARUMAE, "Hokko Tarumae");
        addUma(UmaDataRegistry.MATIKANETANNHAUSER_SPORTS, "[Blue Turbulence] Makikanetannhauser");
        addUma(UmaDataRegistry.CHEVAL_GRAND, "Cheval Grand");
        addUma(UmaDataRegistry.VERXINA, "Verxina");
        addUma(UmaDataRegistry.VIVLOS, "Vivlos");
        addUma(UmaDataRegistry.FUJI_KISEKI, "Fuji Kiseki");
        addUma(UmaDataRegistry.FUJIMASA_MARCH, "Fujimasa March");
        addUma(UmaDataRegistry.HOKKO_TARUMAE_SWIM, "[Pastel Marine Locodol] Hokko Tarumae");
        addUma(UmaDataRegistry.DURANDAL, "Durandal");
        addUma(UmaDataRegistry.TRANSCEND, "Transcend");
        addUma(UmaDataRegistry.CALSTONE_LIGHT_O, "Calstone Light O");
        addUma(UmaDataRegistry.MEJIRO_PALMER, "Mejiro Palmer");
        addUma(UmaDataRegistry.DAIICHI_RUBY, "Daiichi Ruby");

        addUma(UmaDataRegistry.STILL_IN_LOVE, "Still In Love");
        addUma(UmaDataRegistry.HAPPY_MEEK, "Happy Meek");
        addUma(UmaDataRegistry.KATSURAGI_ACE, "Katsuragi Ace");
        addUma(UmaDataRegistry.RHEIN_KRAFT, "Rhein Kraft");

        addUma(UmaDataRegistry.BUENA_VISTA, "Buena Vista");

        addUma(UmaDataRegistry.KS_MIRACLE, "K.S. Miracle");
        addUma(UmaDataRegistry.EISHIN_FLASH, "Eishin Flash");

        addUma(UmaDataRegistry.AGNES_DIGITAL, "Agnes Digital");

        addUma(UmaDataRegistry.SATONO_CROWN, "Satono Crown");
        addUma(UmaDataRegistry.YAMANIN_ZEPHYR, "Yamanin Zephyr");

        addUma(UmaDataRegistry.MIYA_YOMOGI, "Miya Yomogi");
        addUma(UmaDataRegistry.ALMOND_EYE, "Almond Eye");

        addUma(UmaDataRegistry.FUSAICHI_PANDORA, "Fusaichi Pandora");

        addUma(UmaDataRegistry.MEJIRO_RYAN, "Mejiro Ryan");
        addUma(UmaDataRegistry.TYCHE, "Tyche");

        addUma(UmaDataRegistry.NICE_NATURE_CHEER, "[RUN & WIN] Nice Nature");

        addUma(UmaDataRegistry.HISHI_AKEBONO, "Hishi Akebono");
        addUma(UmaDataRegistry.SHENONE_SUZUNA, "Shenone Suzuna");

        addUma(UmaDataRegistry.VIVLOS_SWIM, "[Éclat d'été] Vivlos");

        addUma(UmaDataRegistry.MARUZENSKY, "Maruzensky");

        addUma(UmaDataRegistry.AGNES_DIGITAL_KYOSHI, "[Oirai♡Kyonshii] Agnes Digital");
        addUma(UmaDataRegistry.DANTSU_FLAME, "Dantsu Flame");

        addUma(UmaDataRegistry.NARITA_BRIAN, "Narita Brian");
        addUma(UmaDataRegistry.CESARIO, "Cesario");
        addUma(UmaDataRegistry.NISHINO_FLOWER, "Nishino Flower");
        addUma(UmaDataRegistry.HISHI_AMAZON, "Hishi Amazon");
        addUma(UmaDataRegistry.KISEKI, "Kiseki");
        addUma(UmaDataRegistry.MEJIRO_RAMONU, "Mejiro Ramonu");
        addUma(UmaDataRegistry.CHRONO_GENESIS, "Chrono Genesis");
        addUma(UmaDataRegistry.VICTOIRE_PISA, "Victoire Pisa");
        addUma(UmaDataRegistry.STARDUST, "Stardust");
        addUma(UmaDataRegistry.KING_HALO_CHEER, "[Cheerleader in Nobel White] King Halo");
        addUma(UmaDataRegistry.SAKURA_BAKUSHIN_O_SPORTS, "[Red Hot☆Leader] Sakura Bakushin O");

        addUma(UmaDataRegistry.INES_FUJIN, "Ines Fujin");

        add(Util.makeDescriptionId("umadata", ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "super_creek")), "Super Creek");

        add(Util.makeDescriptionId("umadata", ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "winning_ticket")), "Winning Ticket");
        add(Util.makeDescriptionId("umadata", ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "orfevre")), "Orfevre");

        add(Util.makeDescriptionId("umadata", ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "kiryuuin_aoi")), "Kiryuuin Aoi");
        add(Util.makeDescriptionId("umadata", ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "anshinzawa_sasami")), "Anshinzawa Sasami");

        addSupportCard(SupportCard.EMPTY_ID, "Blank Support Card");

        addSupportCard(SupportCardRegistry.R_TM_OPERA, "[Tracen Academy] TM Opera O");
        addSupportCard(SupportCardRegistry.R_ACUPUNCTUIST, "[Acupunctuist] Anshinzawa Sasami");
        addSupportCard(SupportCardRegistry.SSR_ACUPUNCTUIST, "[Want a Stab?] Anshinzawa Sasami");
        addSupportCard(SupportCardRegistry.SSR_THREE_GODDESSES, "[In Pursuit of Eternal Glory] The Progenitors & Guides");
        addSupportCard(SupportCardRegistry.SSR_TEAM_SIRIUS, "[Accumulating Feelings] Team Sirius");
        addSupportCard(SupportCardRegistry.SSR_ORFEVRE, "[Unconditional Subjugation] Orfevre");

        addSupportCard(SupportCardRegistry.SSR_ANIME_MAIN, "[Into the future!] Timeless Icons");
        addSupportCard(SupportCardRegistry.SSR_NEO_UNIVERSE_WIDSOM, "[My dear V.E.R.2285] Neo Universe");
        addSupportCard(SupportCardRegistry.SSR_MEJIRO_MCQUEEN_STAMINA, "[My heart on a night breeze] Mejiro McQueen");
        addSupportCard(SupportCardRegistry.SSR_SATONO_DIAMOND_STAMINA, "[Surpassing That Back] Satono Diamond");
        addSupportCard(SupportCardRegistry.SSR_KIRYUUIN_AOI, "[Together on the Same Path!] Kiryuuin Aoi");

        addSupportCard(SupportCardRegistry.BASIC_SPEED_CARD, "[Basic Training] Speed Training");
        addSupportCard(SupportCardRegistry.BASIC_STAMINA_CARD, "[Basic Training] Stamina Training");
        addSupportCard(SupportCardRegistry.BASIC_STRENGTH_CARD, "[Basic Training] Power Training");
        addSupportCard(SupportCardRegistry.BASIC_GUTS_CARD, "[Basic Training] Guts Training");
        addSupportCard(SupportCardRegistry.BASIC_WISDOM_CARD, "[Basic Training] Wit Training");

        addSupportCard(SupportCardRegistry.ADV_SPEED_CARD, "[Advanced Training] Speed Training");
        addSupportCard(SupportCardRegistry.ADV_STAMINA_CARD, "[Advanced Training] Stamina Training");
        addSupportCard(SupportCardRegistry.ADV_STRENGTH_CARD, "[Advanced Training] Power Training");
        addSupportCard(SupportCardRegistry.ADV_GUTS_CARD, "[Advanced Training] Guts Training");
        addSupportCard(SupportCardRegistry.ADV_WISDOM_CARD, "[Advanced Training] Wit Training");

        addSupportCard(SupportCardRegistry.SPEED_MASTER_CARD, "[Master Training] Speed Training");
        addSupportCard(SupportCardRegistry.STAMINA_MASTER_CARD, "[Master Training] Stamina Training");
        addSupportCard(SupportCardRegistry.STRENGTH_MASTER_CARD, "[Master Training] Power Training");
        addSupportCard(SupportCardRegistry.GUTS_MASTER_CARD, "[Master Training] Guts Training");
        addSupportCard(SupportCardRegistry.WISDOM_MASTER_CARD, "[Master Training] Wit Training");

        addSupportCard(SupportCardRegistry.R_AGNUS_TACHYON, "[Tracen Academy] Agnes Tachyon");
        addSupportCard(SupportCardRegistry.R_KITASANBLACK, "[Tracen Academy] Kitasan Black");
        addSupportCard(SupportCardRegistry.R_KS_MIRACLE, "[Tracen Academy] K.S. Miracle");
        addSupportCard(SupportCardRegistry.R_OGURICAP, "[Tracen Academy] Oguri Cap");
        addSupportCard(SupportCardRegistry.R_SUPERCREEK, "[Tracen Academy] Super Creek");

        addSupportCard(SupportCardRegistry.R_TURF_TRAINING, "[Tracen Academy] Standard Turf Training");
        addSupportCard(SupportCardRegistry.R_DIRT_TRAINING, "[Tracen Academy] Standard Dirt Training");
        addSupportCard(SupportCardRegistry.R_SNOW_TRAINING, "[Tracen Academy] Standard Snow Training");

        addSupportCard(SupportCardRegistry.SR_AGNUS_TACHYON, "[Experimental Study of Lifeform A] Agnes Tachyon");
        addSupportCard(SupportCardRegistry.SSR_AGNUS_TACHYON, "[Q!=0] Agnes Tachyon");
        addSupportCard(SupportCardRegistry.SSR_OGURICAP, "['You'll Be Dearly Beloved'] Oguri Cap");
        addSupportCard(SupportCardRegistry.SSR_KS_MIRACLE, "[To you] K.S. Miracle");
        addSupportCard(SupportCardRegistry.SSR_KITASANBLACK, "[Pushed by the Approaching Passion] Kitasan Black");
        addSupportCard(SupportCardRegistry.SSR_SUPERCREEK, "[A Grain of Peace] Super Creek");

        addSupportCard(SupportCardRegistry.SSR_FINE_MOTION, "[Gratitude Up to One's Fingertips] Fine Motion");

        addSupportCard(SupportCardRegistry.SSR_RUDOLF_G, "[Unmistakable Emperor] Symboli Rudolf");
        addSupportCard(SupportCardRegistry.SSR_MEJIRO_RAMONU_W, "[Radiant] Mejiro Ramonu");

        addAdvTitle("umapyoi.root", "Welcome to... Minecraft!?");
        addAdvDesc("umapyoi.root", "Join the game and take this book");
        addAdvTitle("umapyoi.silver_pedestal", "Evil Summoning Altar!");
        addAdvDesc("umapyoi.silver_pedestal", "Get a Silver Umamusume Pedestal");
        addAdvTitle("umapyoi.gold_pedestal", "Pricier, but Better");
        addAdvDesc("umapyoi.gold_pedestal", "Get a Golden Umamusume Pedestal");
        addAdvTitle("umapyoi.support_pedestal", "Bookmark of Mysterious Power!");
        addAdvDesc("umapyoi.support_pedestal", "Turn a summoning pedestal into a support one");
        addAdvTitle("umapyoi.carrot_jewel", "A Tastier Carrot");
        addAdvDesc("umapyoi.carrot_jewel", "Get a Carat");
        addAdvTitle("umapyoi.blank_soul", "Power Beyond Time and Space");
        addAdvDesc("umapyoi.blank_soul", "Get your first Faded Umamusume Soul");
        addAdvTitle("umapyoi.uma_soul", "Could It Be a Stand?");
        addAdvDesc("umapyoi.uma_soul", "Get your first Umamusume Soul");
        addAdvTitle("umapyoi.three_goddesses", "A Mysterious Statue");
        addAdvDesc("umapyoi.three_goddesses", "Get the Three Goddesses Statue");
        addAdvTitle("umapyoi.training_terminal", "Let's Work Hard Starting Today!");
        addAdvDesc("umapyoi.training_terminal", "Get a Training Terminal");
        addAdvTitle("umapyoi.self_training_terminal", "This Will Be a Long Time...");
        addAdvDesc("umapyoi.self_training_terminal", "Get a Self-Training Terminal");
        addAdvTitle("umapyoi.auto_training", "My Phone Plays Uma Musume by Itself");
        addAdvDesc("umapyoi.auto_training", "Send a soul to auto-training");
        addAdvTitle("umapyoi.self_training", "Every Season of That Year");
        addAdvDesc("umapyoi.self_training", "Start a three-year career");
        addAdvTitle("umapyoi.inheritance", "Her Legacy, Her Thanks");
        addAdvDesc("umapyoi.inheritance", "Get a factor");
        addAdvTitle("umapyoi.transfer", "Off to Tracen?");
        addAdvDesc("umapyoi.transfer", "Get a Transfer Register Lectern");
        addAdvTitle("umapyoi.skill_table", "Great, I'm Starting to Understand Everything");
        addAdvDesc("umapyoi.skill_table", "Get a Skill Learning Table");
        addAdvTitle("umapyoi.notebook", "Eat It and Get Stronger? Trainer, You're Lying!");
        addAdvDesc("umapyoi.notebook", "Get any notepad, writings or scroll");
        addAdvTitle("umapyoi.blessing", "Blessing");
        addAdvDesc("umapyoi.blessing", "Receive any blessing of the Three Goddesses");
        addAdvTitle("umapyoi.wear_soul", "This Is Me?!");
        addAdvDesc("umapyoi.wear_soul", "Wear any Umamusume Soul for the first time");
        addAdvTitle("umapyoi.wear_costume", "Warm in Winter, Cool in Summer, and Stylish");
        addAdvDesc("umapyoi.wear_costume", "Wear any Umamusume outfit for the first time");
        addAdvTitle("umapyoi.try_ride_horse", "What Is This Creature?!");
        addAdvDesc("umapyoi.try_ride_horse", "Try to ride a horse as an Umamusume");
        addAdvTitle("umapyoi.ride_horse_as_uma", "So I Can Get a Ride?");
        addAdvDesc("umapyoi.ride_horse_as_uma", "Sit on a horse as an Umamusume, in a special way");
        addAdvTitle("umapyoi.helmet_ears", "My Ears Hurt!");
        addAdvDesc("umapyoi.helmet_ears", "A helmet worn too long gives an Umamusume a headache");
        addAdvTitle("umapyoi.favorite_food", "Looks Delicious!");
        addAdvDesc("umapyoi.favorite_food", "Eat a food you like");
        addAdvTitle("umapyoi.fright", "Waaaaah");
        addAdvDesc("umapyoi.fright", "You seem really scared!");
        addAdvTitle("umapyoi.mood_bad", "Not Happy.");
        addAdvDesc("umapyoi.mood_bad", "Feeling as bad as it gets.");
        addAdvTitle("umapyoi.mood_perfect", "Super Happy!");
        addAdvDesc("umapyoi.mood_perfect", "Feeling as good as it gets!");
        addAdvTitle("umapyoi.sorrow_save", "I Am the One Chosen by the Era!");
        addAdvDesc("umapyoi.sorrow_save", "Protected by the power of the Three Goddesses... or were you?");
        addAdvTitle("umapyoi.learn_skill", "A Handy Little Trick");
        addAdvDesc("umapyoi.learn_skill", "Learn any skill");
        addAdvTitle("umapyoi.finish_career", "We'll Meet Again... Right?");
        addAdvDesc("umapyoi.finish_career", "Finish a three-year career");

        addRace(RaceRegistry.MAKE_DEBUT, "Twinkle Series Debut");
        addRace(RaceRegistry.URA_FINALS, "URA Finale");

        addRace(RaceRegistry.HAKODATE_JUNIOR_STAKES, "Hakodate Junior Stakes");
        addRace(RaceRegistry.CHUKYO_JUNIOR_STAKES, "Chukyo Junior Stakes");
        addRace(RaceRegistry.NIIGATA_JUNIOR_STAKES, "Niigata Junior Stakes");
        addRace(RaceRegistry.CLOVER_SHO, "Clover Sho");
        addRace(RaceRegistry.DAHLIA_SHO, "Dahlia Sho");
        addRace(RaceRegistry.PHOENIX_SHO, "Phoenix Sho");
        addRace(RaceRegistry.COSMOS_SHO, "Cosmos Sho");
        addRace(RaceRegistry.KIKYO_STAKES, "Kikyo Stakes");
        addRace(RaceRegistry.FUYO_STAKES, "Fuyo Stakes");
        addRace(RaceRegistry.CANNA_STAKES, "Canna Stakes");
        addRace(RaceRegistry.SAFFRON_SHO, "Saffron Sho");
        addRace(RaceRegistry.SAPPORO_JUNIOR_STAKES, "Sapporo Junior Stakes");
        addRace(RaceRegistry.KOKURA_JUNIOR_STAKES, "Kokura Junior Stakes");
        addRace(RaceRegistry.SUZURAN_SHO, "Suzuran Sho");
        addRace(RaceRegistry.NOJIGIKU_STAKES, "Nojigiku Stakes");
        addRace(RaceRegistry.ASTER_SHO, "Aster Sho");
        addRace(RaceRegistry.ARTEMIS_STAKES, "Artemis Stakes");
        addRace(RaceRegistry.IVY_STAKES, "Ivy Stakes");
        addRace(RaceRegistry.HAGI_STAKES, "Hagi Stakes");
        addRace(RaceRegistry.NADESHIKO_SHO, "Nadeshiko Sho");
        addRace(RaceRegistry.SAUDI_ARABIA_ROYAL_CUP, "Saudi Arabia Royal Cup");
        addRace(RaceRegistry.MOMIJI_STAKES, "Momiji Stakes");
        addRace(RaceRegistry.RINDO_SHO, "Rindo Sho");
        addRace(RaceRegistry.SHIGIKU_SHO, "Shigiku Sho");
        addRace(RaceRegistry.PLATANUS_SHO, "Platanus Sho");
        addRace(RaceRegistry.TOKYO_SPORTS_HAI_JUNIOR_STAKES, "Tokyo Sports Hai Junior Stakes");
        addRace(RaceRegistry.KYOTO_JUNIOR_STAKES, "Kyoto Junior Stakes");
        addRace(RaceRegistry.MOCHINOKI_SHO, "Mochinoki Sho");
        addRace(RaceRegistry.AKAMATSU_SHO, "Akamatsu Sho");
        addRace(RaceRegistry.SHUMEIGIKU_SHO, "Shumeigiku Sho");
        addRace(RaceRegistry.CATTLEYA_SHO, "Cattleya Sho");
        addRace(RaceRegistry.BEGONIA_SHO, "Begonia Sho");
        addRace(RaceRegistry.SHIRAGIKU_SHO, "Shiragiku Sho");
        addRace(RaceRegistry.HABOTAN_SHO, "Habotan Sho");
        addRace(RaceRegistry.KOYAMAKI_SHO, "Koyamaki Sho");
        addRace(RaceRegistry.KEIO_HAI_JUNIOR_STAKES, "Keio Hai Junior Stakes");
        addRace(RaceRegistry.DAILY_HAI_JUNIOR_STAKES, "Daily Hai Junior Stakes");
        addRace(RaceRegistry.FANTASY_STAKES, "Fantasy Stakes");
        addRace(RaceRegistry.FUKUSHIMA_JUNIOR_STAKES, "Fukushima Junior Stakes");
        addRace(RaceRegistry.HYAKUNICHISO_TOKUBETSU, "Hyakunichiso Tokubetsu");
        addRace(RaceRegistry.KIMMOKUSEI_TOKUBETSU, "Kimmokusei Tokubetsu");
        addRace(RaceRegistry.OXALIS_SHO, "Oxalis Sho");
        addRace(RaceRegistry.KIGIKU_SHO, "Kigiku Sho");
        addRace(RaceRegistry.HOPE_STAKES, "Hope Stakes");
        addRace(RaceRegistry.ZEN_NIPPON_JUNIOR_YUSHUN, "Zen Nippon Junior Yushun");
        addRace(RaceRegistry.CHRISTMAS_ROSE_STAKES, "Christmas Rose Stakes");
        addRace(RaceRegistry.SENRYO_SHO, "Senryo Sho");
        addRace(RaceRegistry.HANSHIN_JUVENILE_FILLIES, "Hanshin Juvenile Fillies");
        addRace(RaceRegistry.ASAHI_HAI_FUTURITY_STAKES, "Asahi Hai Futurity Stakes");
        addRace(RaceRegistry.MANRYO_SHO, "Manryo Sho");
        addRace(RaceRegistry.KUROMATSU_SHO, "Kuromatsu Sho");
        addRace(RaceRegistry.ERICA_SHO, "Erica Sho");
        addRace(RaceRegistry.TSUWABUKI_SHO, "Tsuwabuki Sho");
        addRace(RaceRegistry.HIIRAGI_SHO, "Hiiragi Sho");
        addRace(RaceRegistry.SAZANKA_SHO, "Sazanka Sho");
        addRace(RaceRegistry.KANTSUBAKI_SHO, "Kantsubaki Sho");
        addRace(RaceRegistry.HIMAWARI_SHO, "Himawari Sho");
        addRace(RaceRegistry.WAKAGOMA_STAKES, "Wakagoma Stakes");
        addRace(RaceRegistry.CROCUS_STAKES, "Crocus Stakes");
        addRace(RaceRegistry.SHINZAN_KINEN, "Shinzan Kinen");
        addRace(RaceRegistry.FAIRY_STAKES, "Fairy Stakes");
        addRace(RaceRegistry.KEISEI_HAI, "Keisei Hai");
        addRace(RaceRegistry.JUNIOR_CUP, "Junior Cup");
        addRace(RaceRegistry.KOBAI_STAKES, "Kobai Stakes");
        addRace(RaceRegistry.HYACINTH_STAKES, "Hyacinth Stakes");
        addRace(RaceRegistry.SUMIRE_STAKES, "Sumire Stakes");
        addRace(RaceRegistry.MARGUERITE_STAKES, "Marguerite Stakes");
        addRace(RaceRegistry.KISARAGI_SHO, "Kisaragi Sho");
        addRace(RaceRegistry.QUEEN_CUP, "Queen Cup");
        addRace(RaceRegistry.KYODO_TSUSHIN_HAI, "Kyodo Tsushin Hai");
        addRace(RaceRegistry.ELFIN_STAKES, "Elfin Stakes");
        addRace(RaceRegistry.SPRING_STAKES, "Spring Stakes");
        addRace(RaceRegistry.FALCON_STAKES, "Falcon Stakes");
        addRace(RaceRegistry.FLOWER_CUP, "Flower Cup");
        addRace(RaceRegistry.MAINICHI_HAI, "Mainichi Hai");
        addRace(RaceRegistry.WAKABA_STAKES, "Wakaba Stakes");
        addRace(RaceRegistry.YAYOI_SHO, "Yayoi Sho");
        addRace(RaceRegistry.FILLIES_REVIEW, "Fillies Review");
        addRace(RaceRegistry.TURNIP_STAKES, "Tulip Stakes");
        addRace(RaceRegistry.ANEMONE_STAKES, "Anemone Stakes");
        addRace(RaceRegistry.SHORYU_STAKES, "Shoryu Stakes");
        addRace(RaceRegistry.FLORA_STAKES, "Flora Stakes");
        addRace(RaceRegistry.AOBA_SHO, "Aoba Sho");
        addRace(RaceRegistry.TACHIBANA_STAKES, "Tachibana Stakes");
        addRace(RaceRegistry.TANGO_STAKES, "Tango Stakes");
        addRace(RaceRegistry.SWEET_PEA_STAKES, "Sweet Pea Stakes");
        addRace(RaceRegistry.OKA_SHO, "Oka Sho");
        addRace(RaceRegistry.SATSUKI_SHO, "Satsuki Sho");
        addRace(RaceRegistry.NEW_ZEALAND_TROPHY, "New Zealand Trophy");
        addRace(RaceRegistry.ARLINGTON_CUP, "Arlington Cup");
        addRace(RaceRegistry.FUKURYU_STAKES, "Fukuryu Stakes");
        addRace(RaceRegistry.WASURENAGUSA_SHO, "Wasurenagusa Sho");
        addRace(RaceRegistry.MARINE_CUP, "Marine Cup");
        addRace(RaceRegistry.OAKS, "Oaks");
        addRace(RaceRegistry.TOKYO_YUSHUN_JAPANESE_DERBY, "Tokyo Yushun (Japanese Derby)");
        addRace(RaceRegistry.AOI_STAKES, "Aoi Stakes");
        addRace(RaceRegistry.HOSU_STAKES, "Hosu Stakes");
        addRace(RaceRegistry.SHIRAYURI_STAKES, "Shirayuri Stakes");
        addRace(RaceRegistry.NHK_MILE_CUP, "Nhk Mile Cup");
        addRace(RaceRegistry.KYOTO_SHIMBUN_HAI, "Kyoto Shimbun Hai");
        addRace(RaceRegistry.PRINCIPAL_STAKES, "Principal Stakes");
        addRace(RaceRegistry.SEIRYU_STAKES, "Seiryu Stakes");
        addRace(RaceRegistry.UNICORN_STAKES, "Unicorn Stakes");
        addRace(RaceRegistry.TAKARAZUKA_KINEN, "Takarazuka Kinen");
        addRace(RaceRegistry.HAKODATE_SPRINT_STAKES, "Hakodate Sprint Stakes");
        addRace(RaceRegistry.AKHAL_TEKE_STAKES, "Akhal Teke Stakes");
        addRace(RaceRegistry.YONAGO_STAKES, "Yonago Stakes");
        addRace(RaceRegistry.ONUMA_STAKES, "Onuma Stakes");
        addRace(RaceRegistry.PARADISE_STAKES, "Paradise Stakes");
        addRace(RaceRegistry.SANNOMIYA_STAKES, "Sannomiya Stakes");
        addRace(RaceRegistry.KANTO_OAKS, "Kanto Oaks");
        addRace(RaceRegistry.YASUDA_KINEN, "Yasuda Kinen");
        addRace(RaceRegistry.NARUO_KINEN, "Naruo Kinen");
        addRace(RaceRegistry.MERMAID_STAKES, "Mermaid Stakes");
        addRace(RaceRegistry.EPSOM_CUP, "Epsom Cup");
        addRace(RaceRegistry.TEMPOZAN_STAKES, "Tempozan Stakes");
        addRace(RaceRegistry.SLEIPNIR_STAKES, "Sleipnir Stakes");
        addRace(RaceRegistry.CHUKYO_KINEN, "Chukyo Kinen");
        addRace(RaceRegistry.IBIS_SUMMER_DASH, "Ibis Summer Dash");
        addRace(RaceRegistry.QUEEN_STAKES, "Queen Stakes");
        addRace(RaceRegistry.MERCURY_CUP, "Mercury Cup");
        addRace(RaceRegistry.FUKUSHIMA_TV_OPEN, "Fukushima Tv Open");
        addRace(RaceRegistry.JAPAN_DIRT_DERBY, "Japan Dirt Derby");
        addRace(RaceRegistry.RADIO_NIKKEI_SHO, "Radio Nikkei Sho");
        addRace(RaceRegistry.CBC_SHO, "Cbc Sho");
        addRace(RaceRegistry.PROCYON_STAKES, "Procyon Stakes");
        addRace(RaceRegistry.TANABATA_SHO, "Tanabata Sho");
        addRace(RaceRegistry.HAKODATE_KINEN, "Hakodate Kinen");
        addRace(RaceRegistry.SPARKING_LADY_CUP, "Sparking Lady Cup");
        addRace(RaceRegistry.TOMOE_SHO, "Tomoe Sho");
        addRace(RaceRegistry.MARINE_STAKES, "Marine Stakes");
        addRace(RaceRegistry.MEITETSU_HAI, "Meitetsu Hai");
        addRace(RaceRegistry.SAPPORO_KINEN, "Sapporo Kinen");
        addRace(RaceRegistry.KITAKYUSHU_KINEN, "Kitakyushu Kinen");
        addRace(RaceRegistry.KEENELAND_CUP, "Keeneland Cup");
        addRace(RaceRegistry.CLUSTER_CUP, "Cluster Cup");
        addRace(RaceRegistry.NST_SHO, "Nst Sho");
        addRace(RaceRegistry.BSN_SHO, "Bsn Sho");
        addRace(RaceRegistry.KOKURA_NIKKEI_OPEN, "Kokura Nikkei Open");
        addRace(RaceRegistry.TOKI_STAKES, "Toki Stakes");
        addRace(RaceRegistry.LEOPARD_STAKES, "Leopard Stakes");
        addRace(RaceRegistry.KOKURA_KINEN, "Kokura Kinen");
        addRace(RaceRegistry.SEKIYA_KINEN, "Sekiya Kinen");
        addRace(RaceRegistry.ELM_STAKES, "Elm Stakes");
        addRace(RaceRegistry.SAPPORO_NIKKEI_STAKES, "Sapporo Nikkei Stakes");
        addRace(RaceRegistry.UHB_SHO, "Uhb Sho");
        addRace(RaceRegistry.ASO_STAKES, "Aso Stakes");
        addRace(RaceRegistry.KANETSU_STAKES, "Kanetsu Stakes");
        addRace(RaceRegistry.ST_LITE_KINEN, "St Lite Kinen");
        addRace(RaceRegistry.KOBE_SHIMBUN_HAI, "Kobe Shimbun Hai");
        addRace(RaceRegistry.SPRINTERS_STAKES, "Sprinters Stakes");
        addRace(RaceRegistry.ALL_COMERS, "All Comers");
        addRace(RaceRegistry.SAZANKA_TV_CUP, "Sazanka Tv Cup");
        addRace(RaceRegistry.SIRIUS_STAKES, "Sirius Stakes");
        addRace(RaceRegistry.PORT_ISLAND_STAKES, "Port Island Stakes");
        addRace(RaceRegistry.NAGATSUKI_STAKES, "Nagatsuki Stakes");
        addRace(RaceRegistry.ROSE_STAKES, "Rose Stakes");
        addRace(RaceRegistry.SHION_STAKES, "Shion Stakes");
        addRace(RaceRegistry.CENTAUR_STAKES, "Centaur Stakes");
        addRace(RaceRegistry.NIIGATA_KINEN, "Niigata Kinen");
        addRace(RaceRegistry.KEISEI_HAI_AUTUMN_HANDICAP, "Keisei Hai Autumn Handicap");
        addRace(RaceRegistry.TANCHO_STAKES, "Tancho Stakes");
        addRace(RaceRegistry.ENIF_STAKES, "Enif Stakes");
        addRace(RaceRegistry.RADIO_NIPPON_SHO, "Radio Nippon Sho");
        addRace(RaceRegistry.SHUKA_SHO, "Shuka Sho");
        addRace(RaceRegistry.KIKUKA_SHO, "Kikuka Sho");
        addRace(RaceRegistry.TENNO_SHO_AUTUMN, "Tenno Sho Autumn");
        addRace(RaceRegistry.SWAN_STAKES, "Swan Stakes");
        addRace(RaceRegistry.FUJI_STAKES, "Fuji Stakes");
        addRace(RaceRegistry.MUROMAJI_STAKES, "Muromachi Stakes");
        addRace(RaceRegistry.BRAZIL_CUP, "Brazil Cup");
        addRace(RaceRegistry.CASSIOPEIA_STAKES, "Cassiopeia Stakes");
        addRace(RaceRegistry.LUMIERE_AUTUMN_DASH, "Lumiere Autumn Dash");
        addRace(RaceRegistry.MILE_CHAMPIONSHIP_NANBU_HAI, "Mile Championship Nanbu Hai");
        addRace(RaceRegistry.MAINICHI_OKAN, "Mainichi Okan");
        addRace(RaceRegistry.KYOTO_DAISHOTEN, "Kyoto Daishoten");
        addRace(RaceRegistry.FUCHU_UMAMUSUME_STAKES, "Fuchu Umamusume Stakes");
        addRace(RaceRegistry.LADIES_PRELUDE, "Ladies Prelude");
        addRace(RaceRegistry.TOKYO_HAI, "Tokyo Hai");
        addRace(RaceRegistry.OPAL_STAKES, "Opal Stakes");
        addRace(RaceRegistry.GREEN_CHANNEL_CUP, "Green Channel Cup");
        addRace(RaceRegistry.OCTOBER_STAKES, "October Stakes");
        addRace(RaceRegistry.SHINETSU_STAKES, "Shinetsu Stakes");
        addRace(RaceRegistry.UZUMASA_STAKES, "Uzumasa Stakes");
        addRace(RaceRegistry.MILE_CHAMPIONSHIP, "Mile Championship");
        addRace(RaceRegistry.JAPAN_CUP, "Japan Cup");
        addRace(RaceRegistry.KEIHAN_CUP, "Keihan Cup");
        addRace(RaceRegistry.ANDROMEDA_STAKES, "Andromeda Stakes");
        addRace(RaceRegistry.SHIMOTSUKI_STAKES, "Shimotsuki Stakes");
        addRace(RaceRegistry.FUKUSHIMA_MINYU_CUP, "Fukushima Minyu Cup");
        addRace(RaceRegistry.CAPITAL_STAKES, "Capital Stakes");
        addRace(RaceRegistry.AUTUMN_LEAF_STAKES, "Autumn Leaf Stakes");
        addRace(RaceRegistry.QUEEN_ELIZABETH_II_CUP, "Queen Elizabeth Ii Cup");
        addRace(RaceRegistry.JBC_LADIES_CLASSIC, "Jbc Ladies Classic");
        addRace(RaceRegistry.JBC_SPRINT, "Jbc Sprint");
        addRace(RaceRegistry.JBC_CLASSIC, "Jbc Classic");
        addRace(RaceRegistry.COPA_REPUBLICA_ARGENTINA, "Copa Republica Argentina");
        addRace(RaceRegistry.MIYAKO_STAKES, "Miyako Stakes");
        addRace(RaceRegistry.MUSASHINO_STAKES, "Musashino Stakes");
        addRace(RaceRegistry.FUKUSHIMA_KINEN, "Fukushima Kinen");
        addRace(RaceRegistry.ORO_CUP, "Oro Cup");
        addRace(RaceRegistry.ARIMA_KINEN, "Arima Kinen");
        addRace(RaceRegistry.TOKYO_DAISHOTEN, "Tokyo Daishoten");
        addRace(RaceRegistry.HANSHIN_CUP, "Hanshin Cup");
        addRace(RaceRegistry.GALAXY_STAKES, "Galaxy Stakes");
        addRace(RaceRegistry.BETELGEUSE_STAKES, "Betelgeuse Stakes");
        addRace(RaceRegistry.CHAMPIONS_CUP, "Champions Cup");
        addRace(RaceRegistry.STAYERS_STAKES, "Stayers Stakes");
        addRace(RaceRegistry.CHALLENGER_CUP, "Challenger Cup");
        addRace(RaceRegistry.CHUNICHI_SHIMBUN_HAI, "Chunichi Shimbun Hai");
        addRace(RaceRegistry.CAPELLA_STAKES, "Capella Stakes");
        addRace(RaceRegistry.TURQUOISE_STAKES, "Turquoise Stakes");
        addRace(RaceRegistry.QUEEN_SHO, "Queen Sho");
        addRace(RaceRegistry.LAPIS_LAZULI_STAKES, "Lapis Lazuli Stakes");
        addRace(RaceRegistry.SHIWASU_STAKES, "Shiwasu Stakes");
        addRace(RaceRegistry.RIGEL_STAKES, "Rigel Stakes");
        addRace(RaceRegistry.TANZANITE_STAKES, "Tanzanite Stakes");
        addRace(RaceRegistry.DECEMBER_STAKES, "December Stakes");
        addRace(RaceRegistry.TOKAI_STAKES, "Tokai Stakes");
        addRace(RaceRegistry.AMERICAN_JOCKEY_CLUB_CUP, "American Jockey Club Cup");
        addRace(RaceRegistry.SLIK_ROAD_STAKES, "Slik Road Stakes");
        addRace(RaceRegistry.NEGISHI_STAKES, "Negishi Stakes");
        addRace(RaceRegistry.TCK_JO_O_HAI, "Tck Jo O Hai");
        addRace(RaceRegistry.SUBARU_STAKES, "Subaru Stakes");
        addRace(RaceRegistry.SHIRAFUJI_STAKES, "Shirafuji Stakes");
        addRace(RaceRegistry.NIKKEI_SHINSHUN_HAI, "Nikkei Shinshun Hai");
        addRace(RaceRegistry.KYOTO_KIMPAI, "Kyoto Kimpai");
        addRace(RaceRegistry.NAKAYAMA_KIMPAI, "Nakayama Kimpai");
        addRace(RaceRegistry.AICHI_HAI, "Aichi Hai");
        addRace(RaceRegistry.MANYO_STAKES, "Manyo Stakes");
        addRace(RaceRegistry.YODO_TANKYORI_STAKES, "Yodo Tankyori Stakes");
        addRace(RaceRegistry.POLLUX_STAKES, "Pollux Stakes");
        addRace(RaceRegistry.JANRUARY_STAKES, "Janruary Stakes");
        addRace(RaceRegistry.NEW_YEAR_STAKES, "New Year Stakes");
        addRace(RaceRegistry.CARBUNCLE_STAKES, "Carbuncle Stakes");
        addRace(RaceRegistry.FEBRUARY_STAKES, "February Stakes");
        addRace(RaceRegistry.NAKAYAMA_KINEN, "Nakayama Kinen");
        addRace(RaceRegistry.KYOTO_UMAMUSUME_STAKES, "Kyoto Umamusume Stakes");
        addRace(RaceRegistry.DIAMOND_STAKES, "Diamond Stakes");
        addRace(RaceRegistry.KOKURA_DAISHOTEN, "Kokura Daishoten");
        addRace(RaceRegistry.HANKYU_HAI, "Hankyu Hai");
        addRace(RaceRegistry.SOBU_STAKES, "Sobu Stakes");
        addRace(RaceRegistry.KITAKYUSHU_TANKYORI_STAKES, "Kitakyushu Tankyori Stakes");
        addRace(RaceRegistry.KAWASAKI_KINEN, "Kawasaki Kinen");
        addRace(RaceRegistry.KYOTO_KINEN, "Kyoto Kinen");
        addRace(RaceRegistry.TOKYO_SHIMBUN_HAI, "Tokyo Shimbun Hai");
        addRace(RaceRegistry.DAIWA_STAKES, "Yamato Stakes");
        addRace(RaceRegistry.RAKUYO_STAKES, "Rakuyo Stakes");
        addRace(RaceRegistry.ALDEBARAN_STAKES, "Aldebaran Stakes");
        addRace(RaceRegistry.VALENTINE_STAKES, "Valentine Stakes");
        addRace(RaceRegistry.TAKAMATSUNOMIYA_KINEN, "Takamatsunomiya Kinen");
        addRace(RaceRegistry.OSAKA_HAI, "Osaka Hai");
        addRace(RaceRegistry.HANSHIN_DAISHOTEN, "Hanshin Daishoten");
        addRace(RaceRegistry.NIKKEI_SHO, "Nikkei Sho");
        addRace(RaceRegistry.DIOLITE_KINEN, "Diolite Kinen");
        addRace(RaceRegistry.MARCH_STAKES, "March Stakes");
        addRace(RaceRegistry.CHIBA_STAKES, "Chiba Stakes");
        addRace(RaceRegistry.ROKKO_STAKES, "Rokko Stakes");
        addRace(RaceRegistry.KINKO_SHO, "Kinko Sho");
        addRace(RaceRegistry.EMPRESS_HAI, "Empress Hai");
        addRace(RaceRegistry.OCEAN_STAKES, "Ocean Stakes");
        addRace(RaceRegistry.NAKAYAMA_UMAMUSUME_STAKES, "Nakayama Umamusume Stakes");
        addRace(RaceRegistry.OSAKAJO_STAKES, "Osakajo Stakes");
        addRace(RaceRegistry.POLARIS_STAKES, "Polaris Stakes");
        addRace(RaceRegistry.NIGAWA_STAKES, "Nigawa Stakes");
        addRace(RaceRegistry.KOCHI_STAKES, "Kochi Stakes");
        addRace(RaceRegistry.TENNO_SHO_SPRING, "Tenno Sho Spring");
        addRace(RaceRegistry.MILERS_CUP, "Milers Cup");
        addRace(RaceRegistry.FUKUSHIMA_UMAMUSUME_STAKES, "Fukushima Umamusume Stakes");
        addRace(RaceRegistry.TOKYO_SPRINT, "Tokyo Sprint");
        addRace(RaceRegistry.OASIS_STAKES, "Oasis Stakes");
        addRace(RaceRegistry.TENNOZAN_STAKES, "Tennozan Stakes");
        addRace(RaceRegistry.HANSHIN_UMAMUSUME_STAKES, "Hanshin Umamusume Stakes");
        addRace(RaceRegistry.LORD_DERBY_CHALLENGE_TROPHY, "Lord Derby Challenge Trophy");
        addRace(RaceRegistry.ANTERES_STAKES, "Anteres Stakes");
        addRace(RaceRegistry.CORAL_STAKES, "Coral Stakes");
        addRace(RaceRegistry.KEIYO_STAKES, "Keiyo Stakes");
        addRace(RaceRegistry.SHUNRAI_STAKES, "Shunrai Stakes");
        addRace(RaceRegistry.FUKUSHIMA_MIMPO_HAI, "Fukushima Mimpo Hai");
        addRace(RaceRegistry.AZUMAKOFUJI_STAKES, "Azumakofuji Stakes");
        addRace(RaceRegistry.MEGURO_KINEN, "Meguro Kinen");
        addRace(RaceRegistry.HEIAN_STAKES, "Heian Stakes");
        addRace(RaceRegistry.MAY_STAKES, "May Stakes");
        addRace(RaceRegistry.IDATEN_STAKES, "Idaten Stakes");
        addRace(RaceRegistry.KEYAKI_STAKES, "Keyaki Stakes");
        addRace(RaceRegistry.AZUCHIJO_STAKES, "Azuchijo Stakes");
        addRace(RaceRegistry.VICTORIA_MILE, "Victoria Mile");
        addRace(RaceRegistry.KASHIWA_KINEN, "Kashiwa Kinen");
        addRace(RaceRegistry.KEIO_SPRING_CUP, "Keio Spring Cup");
        addRace(RaceRegistry.NIIGATA_DAISHOTEN, "Niigata Daishoten");
        addRace(RaceRegistry.TANIGAWADAKE_STAKES, "Tanigawadake Stakes");
        addRace(RaceRegistry.METROPOLITAN_STAKES, "Metropolitan Stakes");
        addRace(RaceRegistry.KURAMA_STAKES, "Kurama Stakes");
        addRace(RaceRegistry.BRILLIANT_STAKES, "Brilliant Stakes");
        addRace(RaceRegistry.MIYAKOOJI_STAKES, "Miyakooji Stakes");
        addRace(RaceRegistry.RITTO_STAKES, "Ritto Stakes");
        addRace(RaceRegistry.TEIO_SHO, "Teio Sho");
        addRace(RaceRegistry.TAURUS_CUP, "Taurus Cup");
        addRace(RaceRegistry.GEMINI_CUP, "Gemini Cup");
        addRace(RaceRegistry.CANCER_CUP, "Cancer Cup");
        addRace(RaceRegistry.LEO_CUP, "Leo Cup");
        addRace(RaceRegistry.VIRGO_CUP, "Virgo Cup");
        addRace(RaceRegistry.LIBRA_CUP, "Libra Cup");
        addRace(RaceRegistry.SCORPIO_CUP, "Scorpio Cup");
        addRace(RaceRegistry.SAGITTARIUS_CUP, "Sagittarius Cup");
        addRace(RaceRegistry.CAPRICORNUS_CUP, "Capricornus Cup");
        addRace(RaceRegistry.AQUARIUS_CUP, "Aquarius Cup");
        addRace(RaceRegistry.PISCES_CUP, "Pisces Cup");
        addRace(RaceRegistry.ARIES_CUP, "Aries Cup");

    }

    private void addSupportCard(ResourceKey<SupportCard> data, String name) {
        this.addSupportCard(data.location(), name);
    }

    private void addSupportCard(ResourceLocation key, String name) {
        add(Util.makeDescriptionId("support_card", key) + ".name", name);
    }

    private void addRace(ResourceKey<Race> race, String name) {
        add(Util.makeDescriptionId("race", race.location()) + ".name", name);
    }

    private void addUma(ResourceKey<UmaData> data, String name) {
        this.addUma(data.location(), name);
    }

    private void addUma(ResourceLocation key, String name) {
        add(Util.makeDescriptionId("umadata", key), name);
    }

    private void addCostume(ResourceKey<CosmeticData> data, String name) {
        this.addCostume(data.location(), name);
    }

    private void addCostume(ResourceLocation key, String name) {
        add(Util.makeDescriptionId("item", key) + ".name", name);
    }

    private void addSupport(Supplier<TrainingSupport> key, String name) {
        addSupport(key.get(), name);
    }

    private void addSkill(Supplier<UmaSkill> key, String name) {
        addSkill(key.get(), name);
    }

    private void addSkillDetail(Supplier<UmaSkill> key, String name) {
        addSkillDetail(key.get(), name);
    }

    private void addFactor(Supplier<? extends UmaFactor> key, String name) {
        addFactor(key.get(), name);
    }

    private void addSupport(TrainingSupport key, String name) {
        add(key.getDescriptionId(), name);
    }

    private void addSkill(UmaSkill key, String name) {
        add(key.getDescriptionId(), name);
    }

    private void addSkillDetail(UmaSkill key, String name) {
        add(key.getDetailDescriptionId(), name);
    }

    private void addFactor(UmaFactor key, String name) {
        add(key.getDescriptionId(), name);
    }

    private void addFactorDetail(Supplier<? extends UmaFactor> key, String name) {
        addFactorDetail(key.get(), name);
    }

    private void addFactorDetail(UmaFactor key, String name) {
        add(key.getDetailDescriptionId(), name);
    }
}
