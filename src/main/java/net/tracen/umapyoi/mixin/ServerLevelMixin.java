package net.tracen.umapyoi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /**
     * The race world is flat, whatever the overworld is. Players are told so when they arrive; otherwise,
     * standing on ground near the bottom of the world, they see the sky and fog darken towards the
     * horizon as if they were by the void.
     */
    @Inject(method = "isFlat", at = @At("HEAD"), cancellable = true)
    private void umapyoi$raceWorldIsFlat(CallbackInfoReturnable<Boolean> cir) {
        if (RaceWorld.isRaceWorld((ServerLevel) (Object) this))
            cir.setReturnValue(true);
    }
}
