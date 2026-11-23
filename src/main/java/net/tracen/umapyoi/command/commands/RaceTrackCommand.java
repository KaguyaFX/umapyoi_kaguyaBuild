package net.tracen.umapyoi.command.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tracen.umapyoi.UmapyoiConfig;
import net.tracen.umapyoi.race.track.TrackBuilder;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * /umapyoi racetrack build [straight_m radius_m width]: builds a prototype racecourse around the
 * caller (the infield's middle where they stand), a slice per tick, and then puts them at the
 * start of the home straight. Without arguments the sizes come from the config. It overwrites the
 * area; use a test world.
 */
public class RaceTrackCommand {
    /** Distance poles stand every this many race meters before the finish. */
    private static final double POLE_METERS = 200;

    public static LiteralArgumentBuilder<CommandSourceStack> registry(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.then(Commands.literal("racetrack").requires(source -> source.hasPermission(2))
                .then(Commands.literal("build")
                        .executes(ctx -> build(ctx.getSource(), UmapyoiConfig.TRACK_STRAIGHT_METERS.get(),
                                UmapyoiConfig.TRACK_CURVE_RADIUS_METERS.get(), UmapyoiConfig.TRACK_WIDTH.get()))
                        .then(Commands.argument("straight_m", DoubleArgumentType.doubleArg(0, 5000))
                                .then(Commands.argument("radius_m", DoubleArgumentType.doubleArg(10, 5000))
                                        .then(Commands.argument("width", IntegerArgumentType.integer(4, 64))
                                                .executes(ctx -> build(ctx.getSource(),
                                                        DoubleArgumentType.getDouble(ctx, "straight_m"),
                                                        DoubleArgumentType.getDouble(ctx, "radius_m"),
                                                        IntegerArgumentType.getInteger(ctx, "width"))))))));
    }

    private static int build(CommandSourceStack source, double straightMeters, double radiusMeters, int width) {
        if (TrackBuilder.busy()) {
            source.sendFailure(Component.translatable("umapyoi.command.racetrack.busy"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        double metersPerBlock = UmapyoiConfig.RACE_METERS_PER_BLOCK.get();
        TrackGeometry track = TrackGeometry.fromMeters(straightMeters, radiusMeters, width,
                UmapyoiConfig.TRACK_FINISH_METERS.get(), metersPerBlock);
        BlockPos ground = BlockPos.containing(source.getPosition()).below();
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;

        TrackBuilder.start(level, ground, track, POLE_METERS / metersPerBlock,
                percent -> {
                    if (player != null)
                        player.displayClientMessage(Component.translatable("umapyoi.command.racetrack.progress", percent), true);
                },
                result -> {
                    double lap = track.lap();
                    source.sendSuccess(() -> Component.translatable("umapyoi.command.racetrack.built",
                            String.format("%.0f", lap), String.format("%.0f", lap * metersPerBlock),
                            result.sizeX(), result.sizeZ(), result.millis()), true);
                    if (player != null && !player.isRemoved() && player.level() == level) {
                        TrackGeometry.Point view = result.view();
                        player.teleportTo(level, ground.getX() + view.x(), ground.getY() + 1, ground.getZ() + view.z(),
                                view.yaw(), 0);
                    }
                });
        return 1;
    }
}
