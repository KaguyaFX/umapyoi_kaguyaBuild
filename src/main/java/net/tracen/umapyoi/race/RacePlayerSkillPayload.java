package net.tracen.umapyoi.race;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tracen.umapyoi.Umapyoi;

/**
 * Server to clients in the race world: a player running in the race used a skill, so that everyone
 * else sees it over their head for a moment, as they do the computer runners'.
 */
public record RacePlayerSkillPayload(UUID player, ResourceLocation skill) implements CustomPacketPayload {
    public static final Type<RacePlayerSkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID, "race_player_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RacePlayerSkillPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RacePlayerSkillPayload::player,
            ResourceLocation.STREAM_CODEC, RacePlayerSkillPayload::skill,
            RacePlayerSkillPayload::new);
    /** As long as a computer runner's shows, in milliseconds. */
    private static final long SHOWN_MILLIS = RunnerEntity.SKILL_SHOWN * 50L;

    private record Used(ResourceLocation skill, long at) {
    }

    private static final Map<UUID, Used> USED = new HashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RacePlayerSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> USED.put(payload.player(), new Used(payload.skill(), Util.getMillis())));
    }

    /** The skill this player used just now, if it still shows. */
    @Nullable
    public static ResourceLocation shown(UUID player) {
        Used used = USED.get(player);
        if (used == null)
            return null;
        if (Util.getMillis() - used.at() >= SHOWN_MILLIS) {
            USED.remove(player);
            return null;
        }
        return used.skill();
    }

    /** Ticks left of what shows over this player's head. */
    public static int ticksLeft(UUID player) {
        Used used = USED.get(player);
        return used == null ? 0 : (int) ((SHOWN_MILLIS - (Util.getMillis() - used.at())) / 50);
    }
}
