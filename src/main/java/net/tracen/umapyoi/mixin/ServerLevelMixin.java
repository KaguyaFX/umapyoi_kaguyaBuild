package net.tracen.umapyoi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.tracen.umapyoi.race.RaceWorld;

/** The race world doesn't follow the overworld's weather; what its players see is up to RaceSky. */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void umapyoi$noWeatherInRaceWorld(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!RaceWorld.isRaceWorld(level))
            return;
        // A world loaded while it rains starts every dimension wet.
        if (level.getRainLevel(1) > 0)
            level.setRainLevel(0);
        level.setThunderLevel(0);
        ci.cancel();
    }
}
