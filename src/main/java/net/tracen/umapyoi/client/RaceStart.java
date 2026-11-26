package net.tracen.umapyoi.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.block.GateDoor;
import net.tracen.umapyoi.race.RaceClientState;
import net.tracen.umapyoi.race.RaceGatePayload;
import net.tracen.umapyoi.race.RaceWorld;

/**
 * Leaving the starting stall. Nobody is told when the gates open: once everyone is in, the player is
 * told to keep still, and they go when they see their door open.
 * <ul>
 * <li>Until then movement keys do nothing. Each fresh press is a fidget; a few are let off with a
 * warning, after that she's late out of the stall: held in it a moment after the gates open.</li>
 * <li>A key held down when the gates open does nothing until every movement key has been let go, so
 * holding W through the start is no good.</li>
 * <li>How long the player took from the door opening to going is shown for a moment just above the
 * action bar, which the race keeps busy by then.</li>
 * </ul>
 * All of it is judged here, by what this player saw, so a slow connection doesn't count against them.
 */
@EventBusSubscriber(modid = Umapyoi.MODID, value = Dist.CLIENT)
public final class RaceStart {
    private enum Phase {
        NONE, STILL, OPEN
    }

    /** Ticks after which a start nobody finished is forgotten (a race called off, say). */
    private static final int GIVE_UP = 20 * 60;

    private static Phase phase = Phase.NONE;
    private static BlockPos door = BlockPos.ZERO;
    private static int allowed;
    private static int late;
    private static int fidgets;
    private static int ticks;
    /** Which movement keys were down last tick. */
    private static final boolean[] was = new boolean[5];
    /** Keys have been held since before the gates opened, and not all let go yet. */
    private static boolean held;
    private static boolean lateOut;
    private static long openNanos;
    private static long pressNanos;
    /** How long it took to get out, shown for a moment. */
    private static Component readout = Component.empty();
    private static long readoutAt = -1;
    private static final long READOUT_MILLIS = 3000;
    private static final long FADE_MILLIS = 500;

    /** Draws the time out of the stall just above the action bar. */
    public static final LayeredDraw.Layer READOUT = (graphics, tracker) -> {
        if (readoutAt < 0)
            return;
        long age = Util.getMillis() - readoutAt;
        if (age > READOUT_MILLIS) {
            readoutAt = -1;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui)
            return;
        int alpha = (int) Math.min(255, 255 * (READOUT_MILLIS - age) / (double) FADE_MILLIS);
        if (alpha < 8)
            return;
        Font font = minecraft.font;
        int x = (graphics.guiWidth() - font.width(readout)) / 2;
        int y = graphics.guiHeight() - 72 - font.lineHeight - 3;
        graphics.drawString(font, readout, x, y, 0xFFFFFF | alpha << 24);
    };

    private RaceStart() {
    }

    public static void reset() {
        phase = Phase.NONE;
    }

    /** Forgets everything, the readout too: when leaving a world. */
    public static void forget() {
        reset();
        readoutAt = -1;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        RaceGatePayload call = RaceGatePayload.take();
        if (call != null)
            still(minecraft, call);
        if (phase == Phase.NONE)
            return;
        if (minecraft.player == null || minecraft.level == null || !RaceWorld.isRaceWorld(minecraft.level) || ++ticks > GIVE_UP) {
            reset();
            return;
        }
        if (phase == Phase.STILL && opened(minecraft.level)) {
            phase = Phase.OPEN;
            ticks = 0;
            openNanos = Util.getNanos();
            lateOut = fidgets > allowed;
            held = any(keys(minecraft.options));
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (phase == Phase.NONE || event.getAction() != GLFW.GLFW_PRESS)
            return;
        for (KeyMapping key : movement(Minecraft.getInstance().options))
            if (key.matches(event.getKey(), event.getScanCode()))
                pressNanos = Util.getNanos();
    }

    /**
     * Called with the player's movement input every tick, before anything else looks at it.
     *
     * @return true if the player may not move now (the input has been emptied)
     */
    static boolean filter(Input input) {
        switch (phase) {
            case STILL -> {
                boolean[] now = keys(input);
                boolean fresh = false;
                for (int i = 0; i < now.length; i++) {
                    fresh |= now[i] && !was[i];
                    was[i] = now[i];
                }
                if (fresh)
                    fidget();
                stop(input);
                return true;
            }
            case OPEN -> {
                boolean[] now = keys(input);
                if (held && !any(now))
                    held = false;
                if (held || lateOut && ticks < late) {
                    stop(input);
                    return true;
                }
                if (any(now))
                    off();
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private static void still(Minecraft minecraft, RaceGatePayload call) {
        phase = Phase.STILL;
        door = call.door();
        allowed = call.allowed();
        late = call.late();
        fidgets = 0;
        ticks = 0;
        lateOut = false;
        // Keys already down don't count until they're pressed again.
        boolean[] now = keys(minecraft.options);
        System.arraycopy(now, 0, was, 0, was.length);
    }

    /** Whether this player can see their door open. */
    private static boolean opened(Level level) {
        BlockState state = level.getBlockState(door);
        if (state.getBlock() instanceof GateDoor)
            return state.getValue(GateDoor.OPEN);
        // Not there (any more): go by the race having started.
        return RaceClientState.active();
    }

    private static void fidget() {
        fidgets++;
        int left = allowed - fidgets;
        if (left > 0)
            say(Component.translatable("umapyoi.race.start.fidget", left).withStyle(ChatFormatting.YELLOW));
        else if (left == 0)
            say(Component.translatable("umapyoi.race.start.fidget_last").withStyle(ChatFormatting.GOLD));
        else
            say(Component.translatable("umapyoi.race.start.fidget_late").withStyle(ChatFormatting.RED));
    }

    /** Off and away: how long did it take? */
    private static void off() {
        long now = Util.getNanos();
        long went = pressNanos > openNanos && pressNanos <= now ? pressNanos : now;
        if (lateOut)
            went = Math.max(went, openNanos + late * 50_000_000L);
        String seconds = String.format("%.2f", (went - openNanos) / 1e9);
        readout = lateOut ? Component.translatable("umapyoi.race.start.late_out", seconds).withStyle(ChatFormatting.RED)
                : Component.translatable("umapyoi.race.start.out", seconds).withStyle(ChatFormatting.WHITE);
        readoutAt = Util.getMillis();
        reset();
    }

    private static void say(Component message) {
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    private static void stop(Input input) {
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
    }

    private static KeyMapping[] movement(Options options) {
        return new KeyMapping[] {options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump};
    }

    private static boolean[] keys(Options options) {
        KeyMapping[] keys = movement(options);
        boolean[] down = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++)
            down[i] = keys[i].isDown();
        return down;
    }

    private static boolean[] keys(Input input) {
        return new boolean[] {input.up, input.down, input.left, input.right, input.jumping};
    }

    private static boolean any(boolean[] keys) {
        for (boolean key : keys)
            if (key)
                return true;
        return false;
    }
}
