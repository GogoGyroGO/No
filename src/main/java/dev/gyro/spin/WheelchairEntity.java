package dev.gyro.spin;

import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class WheelchairEntity extends Entity {
    private int shoutCooldown;
    public WheelchairEntity(EntityType<?> type, World world) { super(type, world); }
    @Override protected void initDataTracker() { }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { }
    @Override public boolean isCollidable() { return true; }
    @Override public double getMountedHeightOffset() { return .55; }
    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            if (player.isSneaking() && !hasPassengers()) {
                dropItem(SpinMod.CHAIR_ITEM); discard();
            } else player.startRiding(this);
        }
        return ActionResult.success(getWorld().isClient);
    }
    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (shoutCooldown > 0) shoutCooldown--;
        Vec3d velocity = getVelocity();
        if (getFirstPassenger() instanceof PlayerEntity player) {
            setYaw(player.getYaw());
            double speed = Math.max(-1, Math.min(1, player.forwardSpeed)) * .12;
            double angle = Math.toRadians(getYaw());
            velocity = new Vec3d(-Math.sin(angle)*speed, velocity.y, Math.cos(angle)*speed);
            if (shoutCooldown == 0) {
                var mentors = getWorld().getEntitiesByClass(GyroEntity.class, getBoundingBox().expand(6), gyro -> gyro.isAlive() && squaredDistanceTo(gyro) <= 36);
                if (!mentors.isEmpty()) {
                    Text line = Text.literal("БЕГИ, ДЖОННИ, БЕГИ!");
                    for (PlayerEntity nearby : getWorld().getPlayers()) if (nearby.squaredDistanceTo(this) <= 256) nearby.sendMessage(Text.empty().append(mentors.get(0).getDisplayName()).append(": ").append(line), false);
                    shoutCooldown = 200;
                }
            }
        } else velocity = velocity.multiply(.7, 1, .7);
        setVelocity(velocity.add(0, -.08, 0));
        move(MovementType.SELF, getVelocity());
        if (isOnGround()) setVelocity(getVelocity().multiply(1, 0, 1));
    }
}
