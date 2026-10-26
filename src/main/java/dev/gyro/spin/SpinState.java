package dev.gyro.spin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Overworld saved data is authoritative and survives death and dimension changes. */
public final class SpinState extends PersistentState {
    private final Map<UUID, Integer> ranks = new HashMap<>();
    public static SpinState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(SpinState::read, SpinState::new, "gyrospin_players");
    }
    public int rank(UUID id) { return ranks.getOrDefault(id, 0); }
    public void set(UUID id, int rank) { ranks.put(id, rank); markDirty(); }
    public static SpinState read(NbtCompound nbt) {
        SpinState state = new SpinState();
        for (String key : nbt.getKeys()) {
            try { state.ranks.put(UUID.fromString(key), Math.max(0, Math.min(5, nbt.getInt(key)))); }
            catch (IllegalArgumentException ignored) { }
        }
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt) {
        ranks.forEach((id, rank) -> nbt.putInt(id.toString(), rank)); return nbt;
    }
}
