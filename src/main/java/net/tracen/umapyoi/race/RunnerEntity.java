package net.tracen.umapyoi.race;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.race.track.TrackGeometry;

/**
 * The figure of a computer runner. It decides nothing: the race tells it how far along the course it is,
 * how far off the inside and how fast, and it stands where that is. Nothing moves it; a player running
 * beside it can't get into it ({@link #shoulder}).
 * <p>
 * Clients are told those numbers instead of positions and work the place out themselves, running on
 * at the last speed between updates; so it follows the curves and moves as smoothly as the frame rate
 * allows. Position packets are ignored. What a client hears is a little old, and its own player is a
 * little ahead of what the server knows, so it shows the runner that much further on: then who is in
 * front of whom looks the way the server sees it.
 */
public class RunnerEntity extends LivingEntity {
    private static final EntityDataAccessor<ItemStack> SOUL = SynchedEntityData.defineId(RunnerEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<CompoundTag> COURSE = SynchedEntityData.defineId(RunnerEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> PROGRESS = SynchedEntityData.defineId(RunnerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET = SynchedEntityData.defineId(RunnerEntity.class, EntityDataSerializers.FLOAT);
    /** Blocks per second. */
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(RunnerEntity.class, EntityDataSerializers.FLOAT);

    /** How much of the difference to the server's numbers a client makes up per tick. */
    private static final double CATCH_UP = 0.06;
    /** How many ticks ahead of the server's numbers a client shows it. */
    private static final double LEAD_TICKS = 2;
    /** Further off than this, it jumps there. */
    private static final double SNAP = 8;
    /** A player within this along the course and this to the side is pushed off sideways, this much per block too close. */
    private static final double BESIDE = 0.5;
    private static final double SHOULDER = 0.65;
    private static final double PUSH = 0.5;

    @Nullable
    private TrackGeometry track;
    private BlockPos origin = BlockPos.ZERO;
    // What this client shows.
    private double shownProgress = Double.NaN;
    private double shownOffset;

    public RunnerEntity(EntityType<? extends RunnerEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOUL, ItemStack.EMPTY);
        builder.define(COURSE, new CompoundTag());
        builder.define(PROGRESS, 0F);
        builder.define(OFFSET, 0F);
        builder.define(SPEED, 0F);
    }

    // ---- told by the race (server)

    void place(ItemStack soul, TrackGeometry track, BlockPos origin) {
        this.track = track;
        this.origin = origin;
        CompoundTag course = new CompoundTag();
        course.putDouble("straight", track.straight());
        course.putDouble("radius", track.radius());
        course.putDouble("width", track.width());
        course.putDouble("finish", track.finish());
        course.putBoolean("mirrored", track.mirrored());
        course.putLong("origin", origin.asLong());
        entityData.set(SOUL, soul.copy());
        entityData.set(COURSE, course);
    }

    void show(double progress, double offset, double speed) {
        entityData.set(PROGRESS, (float) progress);
        entityData.set(OFFSET, (float) offset);
        entityData.set(SPEED, (float) speed);
        stand(progress, offset);
    }

    /** Only to look at: an Umamusume who isn't anywhere. */
    public void showSoul(ItemStack soul) {
        entityData.set(SOUL, soul.copy());
    }

    /** The soul of the Umamusume this is. */
    public ItemStack soul() {
        return entityData.get(SOUL);
    }

    // ---- moving

    private void stand(double progress, double offset) {
        if (track == null)
            return;
        TrackGeometry.Point point = track.pointAt(track.wrap(progress), offset);
        setPos(origin.getX() + point.x(), origin.getY() + 1, origin.getZ() + point.z());
        setYRot(point.yaw());
        setYHeadRot(point.yaw());
        setYBodyRot(point.yaw());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (COURSE.equals(key) && level().isClientSide()) {
            CompoundTag course = entityData.get(COURSE);
            if (course.contains("straight")) {
                track = new TrackGeometry(course.getDouble("straight"), course.getDouble("radius"), course.getDouble("width"),
                        course.getDouble("finish"), course.getBoolean("mirrored"));
                origin = BlockPos.of(course.getLong("origin"));
            }
        }
    }

    /** No physics, no AI: a client runs it along the course, the server puts it there ({@link #show}). */
    @Override
    public void aiStep() {
        if (!level().isClientSide() || track == null)
            return;
        double perTick = entityData.get(SPEED) / 20.0;
        double offset = entityData.get(OFFSET);
        double progress = entityData.get(PROGRESS);
        progress += perTick * LEAD_TICKS * track.along(progress, offset);
        if (Double.isNaN(shownProgress) || Math.abs(progress - shownProgress) > SNAP) {
            shownProgress = progress;
            shownOffset = offset;
        } else {
            shownProgress += perTick * track.along(shownProgress, shownOffset);
            shownProgress += (progress - shownProgress) * CATCH_UP;
            shownOffset += (offset - shownOffset) * CATCH_UP;
        }
        stand(shownProgress, shownOffset);
        setOnGround(true);
        calculateEntityAnimation(false);
        for (Player player : level().players())
            if (player.isLocalPlayer() && RaceClientState.active())
                shoulder(player);
    }

    /** Two runners side by side don't fit into each other: pushes the player off to the side, softly. */
    private void shoulder(Player player) {
        TrackGeometry.Spot spot = track.locate(player.getX() - origin.getX(), player.getZ() - origin.getZ());
        double lap = track.lap();
        double gap = track.wrap(spot.progress() - shownProgress + lap / 2) - lap / 2;
        double across = spot.offset() - shownOffset;
        if (Math.abs(gap) >= BESIDE || Math.abs(across) >= SHOULDER)
            return;
        // Straight off the inside of the course there, as a direction.
        TrackGeometry.Point in = track.pointAt(spot.progress(), spot.offset());
        TrackGeometry.Point out = track.pointAt(spot.progress(), spot.offset() + 1);
        double push = (SHOULDER - Math.abs(across)) * PUSH * (across < 0 ? -1 : 1);
        player.push((out.x() - in.x()) * push, 0, (out.z() - in.z()) * push);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    }

    @Override
    public void lerpHeadTo(float yaw, int pitch) {
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
    }

    // ---- nothing touches it

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ---- it wears nothing

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
