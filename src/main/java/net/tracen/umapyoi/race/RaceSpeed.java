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
import net.tracen.umapyoi.UmapyoiConfig;

/**
 * Sets the local player's movement speed during a race from their {@link RaceMotion}: the attribute
 * lands on the plain player's sprint times the motion's speed, whatever else is on it (the survival
 * soul bonus, vanilla's sprint boost, the Speed effect). Players move on their own client, so this
 * only runs there.
 * <p>
 * Whenever the server re-sends the player's attributes (it does every time the soul's action points
 * change, for one), the client replaces its modifiers with the server's, which would drop this one
 * until the next tick and make the view twitch. ClientPacketListenerMixin calls
 * {@link #reapply} right after such a packet. The modifier is only touched when its amount changes.
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
        if (!player.level().isClientSide() || !player.isLocalPlayer())
            return;
        RaceStatePayload state = RaceClientState.current();
        RaceMotion motion = RaceClientState.motion();
        if (state == null || motion == null) {
            apply(player, Double.NaN);
            return;
        }
        double moved = RaceClientState.movedSinceLastTick(player);
        double plain = plainSprint(player);
        double perTick = plain <= 0 ? 0 : 1 / (plain * BLOCKS_PER_SECOND * 20);
        if (RaceClientState.idle()) {
            // Nothing pressed: rolling for a moment, then braking.
            boolean coasting = RaceClientState.coasting();
            motion.slow((coasting ? UmapyoiConfig.RACE_COAST_DECEL.get() : UmapyoiConfig.RACE_STOP_DECEL.get()) * perTick);
            // While rolling, the input the client makes up is under the full push, so the attribute makes up for it.
            apply(player, coasting ? motion.speed() / RaceClientState.COAST_INPUT : motion.speed());
            return;
        }
        double actual = moved < 0 || plain <= 0 ? -1 : moved / (plain * BLOCKS_PER_SECOND / 20);
        motion.step(state, player.isSprinting(), actual, UmapyoiConfig.RACE_MOMENTUM_MARGIN.get());
        apply(player, motion.speed());
    }

    /** Puts the local player's race speed back after the server replaced their attribute modifiers. */
    public static void reapply(Player player) {
        RaceMotion motion = RaceClientState.motion();
        if (motion != null)
            apply(player, RaceClientState.coasting() ? motion.speed() / RaceClientState.COAST_INPUT : motion.speed());
    }

    /** The plain player's sprint as an attribute value (without race speed or the Umamusume bonus). */
    private static double plainSprint(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return attribute == null ? 0 : attribute.getBaseValue() * SPRINT;
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
        double amount = plainSprint(player) * speed / withoutOurs - 1;
        if (old != null && Math.abs(old.amount() - amount) < EPSILON)
            return;
        if (old != null)
            attribute.removeModifier(ID);
        attribute.addTransientModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
