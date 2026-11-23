package net.tracen.umapyoi.race;

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
import net.tracen.umapyoi.UmapyoiConfig;

/**
 * Sets a runner's movement speed during a race from their {@link RaceMotion}: the attribute lands on
 * the plain player's sprint times the motion's speed. One modifier is kept so the final value lands
 * there whatever else is on the attribute (the survival soul bonus, vanilla's sprint boost, the
 * Speed effect).
 * <p>
 * It runs on both sides. Players move on their own client, which steps its own motion here; the
 * server steps its copy in the time trial and keeps the same modifier, because whenever it re-sends
 * the player's attributes the client replaces its own modifiers with the server's (a client-only
 * modifier would vanish until the next tick and the view would twitch). The modifier is only
 * touched when its amount changes.
 */
@EventBusSubscriber(modid = Umapyoi.MODID)
public final class RaceSpeed {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_speed");
    /** Vanilla's sprint boost. */
    private static final double SPRINT = 1.3;
    /** Blocks per second on the ground per point of the movement speed attribute. */
    public static final double BLOCKS_PER_SECOND = 43.17;
    private static final double EPSILON = 1.0E-6;

    private RaceSpeed() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        double speed;
        if (player.level().isClientSide()) {
            if (!player.isLocalPlayer())
                return;
            RaceStatePayload state = RaceClientState.current();
            RaceMotion motion = RaceClientState.motion();
            if (state == null || motion == null) {
                apply(player, Double.NaN);
                return;
            }
            AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            double plain = attribute == null ? 0.13 : attribute.getBaseValue() * SPRINT;
            double moved = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);
            double actual = plain <= 0 ? -1 : moved / (plain * BLOCKS_PER_SECOND / 20);
            motion.step(state, player.isSprinting(), actual, UmapyoiConfig.RACE_MOMENTUM_MARGIN.get());
            speed = motion.speed();
        } else {
            if (!(player instanceof ServerPlayer serverPlayer))
                return;
            speed = TimeTrial.speed(serverPlayer);
        }
        apply(player, speed);
    }

    /** @param speed a fraction of the plain sprint, or NaN when not racing */
    private static void apply(Player player, double speed) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null)
            return;
        AttributeModifier old = attribute.getModifier(ID);
        if (Double.isNaN(speed)) {
            if (old != null)
                attribute.removeModifier(ID);
            return;
        }
        // Ours is a final multiplier, so the value without it is the value with it divided back out.
        double withoutOurs = old == null ? attribute.getValue() : attribute.getValue() / (1 + old.amount());
        if (withoutOurs <= 0)
            return;
        double target = attribute.getBaseValue() * SPRINT * speed;
        double amount = target / withoutOurs - 1;
        if (old != null && Math.abs(old.amount() - amount) < EPSILON)
            return;
        if (old != null)
            attribute.removeModifier(ID);
        attribute.addTransientModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
