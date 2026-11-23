package net.tracen.umapyoi.command.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.RaceWorld;
import net.tracen.umapyoi.race.RacecourseData;
import net.tracen.umapyoi.race.TimeTrial;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * Test commands for the race world (operators only, for now):
 * <ul>
 * <li>/umapyoi race enter: go to the course (built the first time) and remember where you were.</li>
 * <li>/umapyoi race leave: go back.</li>
 * <li>/umapyoi race trial [distance_m]: a solo time trial from the stalls on the finish line (2000 m by default).</li>
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
                .then(Commands.literal("trial")
                        .executes(ctx -> trial(ctx.getSource(), 2000))
                        .then(Commands.argument("distance_m", DoubleArgumentType.doubleArg(200, 10000))
                                .executes(ctx -> trial(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "distance_m")))))
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

    private static int trial(CommandSourceStack source, double distanceMeters) throws CommandSyntaxException {
        String key = switch (TimeTrial.start(source.getPlayerOrException(), distanceMeters)) {
            case STARTED -> null;
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
