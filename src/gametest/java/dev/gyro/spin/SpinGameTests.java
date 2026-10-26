package dev.gyro.spin;

import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.hit.EntityHitResult;

public class SpinGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void customEntitiesSurviveTicking(TestContext context) {
        var gyro = context.spawnEntity(SpinMod.GYRO, 2, 2, 2);
        var chair = context.spawnEntity(SpinMod.CHAIR, 5, 2, 5);
        context.waitAndRun(20, () -> {
            context.assertTrue(gyro.isAlive(), "Gyro failed to spawn or tick");
            context.assertTrue(chair.isAlive(), "Wheelchair failed to spawn or tick");
            context.assertTrue(gyro.hasCustomName(), "Mentor has no name");
            context.complete();
        });
    }
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void goldenBallDamagesAndDebuffs(TestContext context) {
        var owner = context.spawnEntity(EntityType.VILLAGER, 1, 2, 1);
        var target = context.spawnEntity(EntityType.IRON_GOLEM, 5, 2, 5);
        var ball = new SteelBallEntity(context.getWorld(), owner, 5);
        ball.setPosition(owner.getPos());
        context.getWorld().spawnEntity(ball);
        ball.onEntityHit(new EntityHitResult(target));
        context.assertTrue(target.getHealth() < target.getMaxHealth(), "Ball did no damage");
        context.assertTrue(target.hasStatusEffect(StatusEffects.SLOWNESS), "Missing slowing effect");
        context.assertTrue(target.hasStatusEffect(StatusEffects.WEAKNESS), "Missing golden weakness effect");
        context.complete();
    }
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void progressionRoundTripsSavedNbt(TestContext context) {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        SpinState original = new SpinState();
        original.set(first, 5); original.set(second, 2);
        SpinState restored = SpinState.read(original.writeNbt(new NbtCompound()));
        context.assertTrue(restored.rank(first) == 5, "Lost master rank");
        context.assertTrue(restored.rank(second) == 2, "Mixed player progress");
        context.assertTrue(restored.rank(UUID.randomUUID()) == 0, "New player already has Spin");
        context.complete();
    }
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void projectileOwnershipAndRecoverySurviveNbt(TestContext context) {
        var owner = context.spawnEntity(EntityType.VILLAGER, 1, 2, 1);
        var original = new SteelBallEntity(context.getWorld(), owner, 3);
        original.recoverable = true;
        NbtCompound data = new NbtCompound(); original.writeNbt(data);
        var restored = SpinMod.BALL.create(context.getWorld()); restored.readNbt(data);
        context.assertTrue(restored.recoverable, "Lost recoverable steel ball");
        context.assertTrue(restored.getOwner() == owner, "Lost projectile ownership");
        context.assertTrue(restored.getStack().isOf(SpinMod.STEEL_BALL), "Wrong projectile item");
        context.complete();
    }
}
