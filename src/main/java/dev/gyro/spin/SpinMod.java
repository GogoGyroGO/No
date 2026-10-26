package dev.gyro.spin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.Heightmap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;

public final class SpinMod implements ModInitializer {
    public static Identifier id(String path) { return new Identifier("gyrospin", path); }
    public static final Identifier OPEN = id("open"), LEARN = id("learn");
    public static final EntityType<SteelBallEntity> BALL = Registry.register(Registries.ENTITY_TYPE, id("steel_ball"),
        FabricEntityTypeBuilder.<SteelBallEntity>create(SpawnGroup.MISC, SteelBallEntity::new).dimensions(EntityDimensions.fixed(.25f,.25f)).trackRangeBlocks(64).trackedUpdateRate(1).build());
    public static final EntityType<GyroEntity> GYRO = Registry.register(Registries.ENTITY_TYPE, id("gyro"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GyroEntity::new).dimensions(EntityDimensions.fixed(.6f,1.95f)).build());
    public static final EntityType<WheelchairEntity> CHAIR = Registry.register(Registries.ENTITY_TYPE, id("wheelchair"),
        FabricEntityTypeBuilder.<WheelchairEntity>create(SpawnGroup.MISC, WheelchairEntity::new).dimensions(EntityDimensions.fixed(.9f,1.1f)).trackRangeBlocks(64).build());
    public static final Item STEEL_BALL = Registry.register(Registries.ITEM, id("steel_ball"), new SteelBallItem(new Item.Settings().maxCount(1)));
    public static final Item MANUAL = Registry.register(Registries.ITEM, id("spin_manual"), new Item(new Item.Settings().maxCount(1)) {
        @Override public TypedActionResult<ItemStack> use(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, Hand hand) {
            if (player instanceof ServerPlayerEntity sp) open(sp);
            return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
        }
    });
    public static final Item CHAIR_ITEM = Registry.register(Registries.ITEM, id("wheelchair"), new Item(new Item.Settings().maxCount(1)) {
        @Override public ActionResult useOnBlock(ItemUsageContext ctx) {
            if (!ctx.getWorld().isClient) {
                var pos = ctx.getBlockPos().offset(ctx.getSide());
                WheelchairEntity chair = CHAIR.create(ctx.getWorld());
                if (chair == null) return ActionResult.FAIL;
                chair.refreshPositionAndAngles(pos.getX()+.5, pos.getY(), pos.getZ()+.5, ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYaw(), 0);
                if (!ctx.getWorld().isSpaceEmpty(chair)) return ActionResult.FAIL;
                ctx.getWorld().spawnEntity(chair);
                if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) ctx.getStack().decrement(1);
            }
            return ActionResult.success(ctx.getWorld().isClient);
        }
    });
    public static final Item GYRO_EGG = Registry.register(Registries.ITEM, id("gyro_spawn_egg"), new SpawnEggItem(GYRO, 0x56854a, 0xd9c079, new Item.Settings()));
    public static int rank(ServerPlayerEntity player) { return SpinState.get(player.getServer()).rank(player.getUuid()); }
    public static void open(ServerPlayerEntity player) {
        var buf = PacketByteBufs.create(); buf.writeVarInt(rank(player)); buf.writeVarInt(player.experienceLevel);
        ServerPlayNetworking.send(player, OPEN, buf);
    }
    @Override public void onInitialize() {
        FabricDefaultAttributeRegistry.register(GYRO, VillagerEntity.createVillagerAttributes());
        SpawnRestriction.register(GYRO, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, net.minecraft.entity.mob.MobEntity::canMobSpawn);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS), SpawnGroup.CREATURE, GYRO, 2, 1, 1);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> { entries.add(STEEL_BALL); entries.add(MANUAL); entries.add(GYRO_EGG); entries.add(CHAIR_ITEM); });
        ServerPlayNetworking.registerGlobalReceiver(LEARN, (server, player, handler, buf, sender) -> {
            // Client never chooses cost, player identity, or resulting rank.
            server.execute(() -> {
                int rank = rank(player);
                if (rank == 0) player.sendMessage(Text.translatable("message.gyrospin.find_gyro"), false);
                else if (rank < 5) {
                    int cost = SpinSkills.cost(rank + 1);
                    if (SpinSkills.canLearn(rank, player.experienceLevel, player.isCreative())) {
                        if (!player.isCreative()) player.addExperienceLevels(-cost);
                        SpinState.get(server).set(player.getUuid(), rank + 1);
                        player.sendMessage(Text.translatable("message.gyrospin.learned"), false);
                    } else player.sendMessage(Text.translatable("message.gyrospin.need_xp", cost), false);
                }
                open(player);
            });
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(CommandManager.literal("spin").executes(ctx -> { open(ctx.getSource().getPlayerOrThrow()); return 1; })));
    }
}
