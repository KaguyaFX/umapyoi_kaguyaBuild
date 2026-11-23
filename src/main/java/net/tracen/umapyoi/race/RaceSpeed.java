package net.tracen.umapyoi.race;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tracen.umapyoi.Umapyoi;

/**
 * Sets the local player's movement speed during a race. Players move on their own client, so this
 * runs there, from the state the server sends ({@link RaceClientState}).
 * <p>
 * The target, as an attribute value: sprinting is the plain player's sprint times
 * (1 + the bonus stamina leaves); walking is that times the cruise ratio, so cruising is never faster
 * than sprinting. One modifier is recomputed each tick so the final value lands on the target
 * whatever else is on the attribute (the survival soul bonus, vanilla's sprint boost).
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceSpeed {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_speed");
    /** Vanilla's sprint boost. */
    private static final double SPRINT = 1.3;

    private RaceSpeed() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() || !player.isLocalPlayer())
            return;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
            return;
        speed.removeModifier(ID);
        if (!RaceClientState.active())
            return;
        double sprint = speed.getBaseValue() * SPRINT * (1 + RaceClientState.bonusRatio());
        double target = player.isSprinting() ? sprint : sprint * RaceClientState.cruiseRatio();
        double current = speed.getValue();
        if (current <= 0)
            return;
        speed.addTransientModifier(new AttributeModifier(ID, target / current - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
