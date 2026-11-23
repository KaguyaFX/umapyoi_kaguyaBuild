package net.tracen.umapyoi.command.commands;

import java.util.Locale;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.RaceSetup;
import net.tracen.umapyoi.race.RaceWeather;
import net.tracen.umapyoi.race.RaceWorld;
import net.tracen.umapyoi.race.RacecourseData;
import net.tracen.umapyoi.race.TimeTrial;
import net.tracen.umapyoi.utils.Surface;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * Test commands for the race world (operators only, for now):
 * <ul>
 * <li>/umapyoi race enter: go to the course (built the first time) and remember where you were.</li>
 * <li>/umapyoi race leave: go back.</li>
 * <li>/umapyoi race trial [distance_m] [turf|dirt|synthetic] [right|left] [sunny|rain|thunder]: a solo
 * time trial from the stalls on the finish line (2000 m on turf, right-handed, sunny, by default).</li>
 * <li>/umapyoi race cancel: stop your time trial.</li>
 * <li>/umapyoi racetrack rebuild [lap_m straight_m width]: rebuild the course from the config or
 * the given sizes, clearing the old one.</li>
 * </ul>
 */
public class RaceCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> registry(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("race").requires(source -> source.hasPermission(2))
                .then(Commands.literal("enter").executes(ctx -> enter(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(trialArguments())
                .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource()))));
        return builder.then(Commands.literal("racetrack").requires(source -> source.hasPermission(2))
                .then(Commands.literal("rebuild")
                        .executes(ctx -> rebuild(ctx.getSource(), RaceWorld.configuredTrack()))
                        .then(Commands.argument("lap_m", DoubleArgumentType.doubleArg(400, 20000))
                                .then(Commands.argument("straight_m", DoubleArgumentType.doubleArg(0, 5000))
                                        .then(Commands.argument("width", IntegerArgumentType.integer(4, 64))
                                                .executes(ctx -> rebuild(ctx.getSource(), RaceWorld.track(
                                                        DoubleArgumentType.getDouble(ctx, "lap_m"),
                                                        DoubleArgumentType.getDouble(ctx, "straight_m"),
                                                        IntegerArgumentType.getInteger(ctx, "width")))))))));
    }

    /** Each argument may be left off, from the right. */
    private static LiteralArgumentBuilder<CommandSourceStack> trialArguments() {
        RaceSetup standard = RaceSetup.DEFAULT;
        var distance = Commands.argument("distance_m", DoubleArgumentType.doubleArg(200, 10000))
                .executes(ctx -> trial(ctx, distance(ctx), standard.surface(), standard.leftHanded(), standard.weather()));
        for (Surface surface : Surface.values()) {
            if (surface == Surface.ADAPTIVE)
                continue;
            var surfaceNode = Commands.literal(lower(surface))
                    .executes(ctx -> trial(ctx, distance(ctx), surface, standard.leftHanded(), standard.weather()));
            for (boolean left : new boolean[] { false, true }) {
                var direction = Commands.literal(left ? "left" : "right")
                        .executes(ctx -> trial(ctx, distance(ctx), surface, left, standard.weather()));
                for (RaceWeather weather : RaceWeather.values())
                    direction.then(Commands.literal(lower(weather))
                            .executes(ctx -> trial(ctx, distance(ctx), surface, left, weather)));
                surfaceNode.then(direction);
            }
            distance.then(surfaceNode);
        }
        return Commands.literal("trial").executes(ctx -> trial(ctx.getSource(), standard)).then(distance);
    }

    private static double distance(CommandContext<CommandSourceStack> ctx) {
        return DoubleArgumentType.getDouble(ctx, "distance_m");
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static int trial(CommandContext<CommandSourceStack> ctx, double distance, Surface surface, boolean left,
            RaceWeather weather) throws CommandSyntaxException {
        return trial(ctx.getSource(), new RaceSetup(distance, surface, left, weather));
    }

    private static int enter(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String key = switch (RaceWorld.enter(player)) {
            case SENT -> null;
            case PREPARING -> "umapyoi.command.race.preparing";
            case BUSY -> "umapyoi.command.racetrack.busy";
            case ALREADY_THERE -> "umapyoi.command.race.already_there";
            case MISSING -> "umapyoi.command.race.missing";
        };
        if (key != null)
            source.sendSuccess(() -> Component.translatable(key), false);
        return 1;
    }

    private static int leave(CommandSourceStack source) throws CommandSyntaxException {
        if (!RaceWorld.leave(source.getPlayerOrException())) {
            source.sendFailure(Component.translatable("umapyoi.command.race.not_there"));
            return 0;
        }
        return 1;
    }

    private static int trial(CommandSourceStack source, RaceSetup setup) throws CommandSyntaxException {
        String key = switch (TimeTrial.start(source.getPlayerOrException(), setup)) {
            case STARTED, PREPARING -> null;
            case BUSY -> "umapyoi.command.race.trial_busy";
            case NOT_THERE -> "umapyoi.command.race.not_there";
            case NO_COURSE -> "umapyoi.command.race.no_course";
        };
        if (key == null)
            return 1;
        source.sendFailure(Component.translatable(key));
        return 0;
    }

    private static int cancel(CommandSourceStack source) throws CommandSyntaxException {
        if (!TimeTrial.cancel(source.getPlayerOrException())) {
            source.sendFailure(Component.translatable("umapyoi.command.race.no_trial"));
            return 0;
        }
        return 1;
    }

    private static int rebuild(CommandSourceStack source, TrackGeometry track) {
        if (TimeTrial.running()) {
            source.sendFailure(Component.translatable("umapyoi.command.race.trial_busy"));
            return 0;
        }
        if (TrackBuilder.busy()) {
            source.sendFailure(Component.translatable("umapyoi.command.racetrack.busy"));
            return 0;
        }
        ServerLevel race = RaceWorld.level(source.getServer());
        if (race == null) {
            source.sendFailure(Component.translatable("umapyoi.command.race.missing"));
            return 0;
        }
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
        double metersPerBlock = UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
        RaceWorld.build(race, track,
                percent -> {
                    if (player != null)
                        player.displayClientMessage(Component.translatable("umapyoi.command.racetrack.progress", percent), true);
                },
                result -> {
                    double lap = track.lap();
                    source.sendSuccess(() -> Component.translatable("umapyoi.command.racetrack.built",
                            String.format("%.0f", lap), String.format("%.0f", lap * metersPerBlock),
                            result.sizeX(), result.sizeZ(), result.millis()), true);
                    if (player != null && !player.hasDisconnected() && RaceWorld.isRaceWorld(player.level()))
                        RaceWorld.sendToCourse(player, race, RacecourseData.get(race));
                });
        return 1;
    }
}
