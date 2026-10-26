package dev.gyro.spin;

import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.*;
import net.minecraft.world.World;

public final class SteelBallEntity extends ThrownItemEntity {
    private int rank = 1;
    public boolean recoverable;
    private boolean returning;
    public SteelBallEntity(EntityType<? extends SteelBallEntity> type, World world) { super(type, world); }
    public SteelBallEntity(World world, LivingEntity owner, int rank) { super(SpinMod.BALL, owner, world); this.rank = rank; }
    @Override protected Item getDefaultItem() { return SpinMod.STEEL_BALL; }
    @Override protected void onEntityHit(EntityHitResult hit) {
        if (!getWorld().isClient && !returning && hit.getEntity() != getOwner()) {
            Entity target = hit.getEntity();
            target.damage(getDamageSources().thrown(this, getOwner()), rank >= 5 ? 14 : rank >= 3 ? 9 : 5);
            if (target instanceof LivingEntity living && rank >= 3) living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            if (rank >= 5 && target instanceof LivingEntity living) living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
            finishFlight();
        }
    }
    @Override protected void onBlockHit(BlockHitResult hit) { if (!getWorld().isClient && !returning) finishFlight(); }
    private void finishFlight() { if (rank >= 2 && getOwner() != null) { returning = true; setNoGravity(true); } else release(); }
    private void release() {
        if (recoverable) { recoverable = false; dropStack(new ItemStack(SpinMod.STEEL_BALL)); }
        discard();
    }
    @Override public void tick() {
        super.tick();
        if (getWorld().isClient || isRemoved()) return;
        ((ServerWorld)getWorld()).spawnParticles(rank >= 5 ? ParticleTypes.END_ROD : ParticleTypes.CRIT, getX(), getY(), getZ(), 2, .05, .05, .05, .01);
        if (age > 200) { release(); return; }
        if (age > 25 && rank >= 2) { returning = true; setNoGravity(true); }
        if (returning) {
            Entity owner = getOwner();
            if (!(owner instanceof PlayerEntity player) || !owner.isAlive() || owner.getWorld() != getWorld()) { release(); return; }
            var delta = player.getEyePos().subtract(getPos());
            if (delta.lengthSquared() < 2.25) {
                if (recoverable) { recoverable = false; if (!player.getInventory().insertStack(new ItemStack(SpinMod.STEEL_BALL))) dropStack(new ItemStack(SpinMod.STEEL_BALL)); }
                discard();
            } else setVelocity(delta.normalize().multiply(1.4));
        }
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) { super.writeCustomDataToNbt(nbt); nbt.putInt("SpinRank", rank); nbt.putBoolean("Recoverable", recoverable); nbt.putBoolean("Returning", returning); }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) { super.readCustomDataFromNbt(nbt); rank = Math.max(1, Math.min(5, nbt.getInt("SpinRank"))); recoverable = nbt.getBoolean("Recoverable"); returning = nbt.getBoolean("Returning"); }
}
