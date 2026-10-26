package dev.gyro.spin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class GyroEntity extends VillagerEntity {
    public GyroEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
        setCustomName(Text.translatable("entity.gyrospin.gyro"));
        setCustomNameVisible(true);
        setPersistent();
    }
    @Override public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity sp && hand == Hand.MAIN_HAND) {
            if (SpinMod.rank(sp) == 0) {
                SpinState.get(sp.getServer()).set(sp.getUuid(), 1);
                sp.giveItemStack(new ItemStack(SpinMod.STEEL_BALL));
                sp.giveItemStack(new ItemStack(SpinMod.MANUAL));
                sp.sendMessage(Text.translatable("message.gyrospin.welcome"), false);
            }
            SpinMod.open(sp);
        }
        return ActionResult.success(getWorld().isClient);
    }
}
