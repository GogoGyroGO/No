package dev.gyro.spin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class SteelBallItem extends Item {
    public SteelBallItem(Settings settings) { super(settings); }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player instanceof ServerPlayerEntity sp) {
            int rank = SpinMod.rank(sp);
            if (rank == 0) {
                player.sendMessage(Text.translatable("message.gyrospin.find_gyro"), true);
                return TypedActionResult.fail(stack);
            }
            if (player.isSneaking() && rank >= 4) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0));
                player.getItemCooldownManager().set(this, 200);
            } else {
                SteelBallEntity ball = new SteelBallEntity(world, player, rank);
                ball.setVelocity(player, player.getPitch(), player.getYaw(), 0, 1.8f, .4f);
                ball.setItem(stack.copy());
                ball.recoverable = !player.isCreative();
                world.spawnEntity(ball);
                if (!player.isCreative()) stack.decrement(1);
                player.getItemCooldownManager().set(this, 15);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
