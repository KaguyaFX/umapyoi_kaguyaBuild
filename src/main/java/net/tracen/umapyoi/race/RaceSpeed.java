package net.tracen.umapyoi.race;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;

/**
 * Sets a runner's movement speed during a race. The target, as an attribute value: sprinting is
 * the plain player's sprint times (1 + the bonus stamina leaves); walking is that times the cruise
 * ratio, so cruising is never faster than sprinting. One modifier is kept so the final value lands
 * on the target whatever else is on the attribute (the survival soul bonus, vanilla's sprint boost).
 * <p>
 * It runs on both sides with the same numbers. Players move on their own client, which is what
 * matters for the run; the server keeps the same modifier because whenever it re-sends the
 * player's attributes (it does every time the soul's action points change, for one), the client
 * replaces its own modifiers with the server's. A client-only modifier would vanish until the next
 * tick each time, and the field of view would twitch. The modifier is only touched when its
 * amount actually changes, so it doesn't make the server re-send attributes on its own.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceSpeed {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_speed");
    /** Vanilla's sprint boost. */
    private static final double SPRINT = 1.3;
    private static final double EPSILON = 1.0E-6;

    private RaceSpeed() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        RaceStatePayload state;
        if (player.level().isClientSide()) {
            if (!player.isLocalPlayer())
                return;
            state = RaceClientState.current();
        } else {
            if (!(player instanceof ServerPlayer serverPlayer))
                return;
            state = TimeTrial.state(serverPlayer);
        }
        apply(player, state);
    }

    private static void apply(Player player, @Nullable RaceStatePayload state) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
            return;
        AttributeModifier old = speed.getModifier(ID);
        if (state == null || !state.active()) {
            if (old != null)
                speed.removeModifier(ID);
            return;
        }
        // Ours is a final multiplier, so the value without it is the value with it divided back out.
        double withoutOurs = old == null ? speed.getValue() : speed.getValue() / (1 + old.amount());
        if (withoutOurs <= 0)
            return;
        double sprint = speed.getBaseValue() * SPRINT * (1 + state.bonusRatio());
        double target = player.isSprinting() ? sprint : sprint * state.cruiseRatio();
        double amount = target / withoutOurs - 1;
        if (old != null && Math.abs(old.amount() - amount) < EPSILON)
            return;
        if (old != null)
            speed.removeModifier(ID);
        speed.addTransientModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
