package com.screenbog.zombiewaves.wave;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.Zombie;

import java.util.UUID;

/**
 * Помечает зомби тегами NBT, чтобы отличать волновых от обычных.
 */
public final class WaveZombieMarker {
    private static final String WAVE_TAG = ZombieWavesMod.MOD_ID + "_wave";
    private static final String WAVE_NUMBER = "wave_number";
    private static final String OWNER_PLAYER_UUID = "owner_player_uuid";

    private WaveZombieMarker() {
    }

    /**
     * Исправлено (Prompt 4): сохраняем UUID владельца зомби для multiplayer.
     */
    public static void mark(Zombie zombie, int waveNumber, UUID ownerPlayerUuid) {
        CompoundTag data = zombie.getPersistentData();
        data.putBoolean(WAVE_TAG, true);
        data.putInt(WAVE_NUMBER, waveNumber);
        if (ownerPlayerUuid != null) {
            data.putUUID(OWNER_PLAYER_UUID, ownerPlayerUuid);
        }
    }

    public static boolean isWaveZombie(Zombie zombie) {
        return zombie.getPersistentData().getBoolean(WAVE_TAG);
    }

    public static int getWaveNumber(Zombie zombie) {
        return zombie.getPersistentData().getInt(WAVE_NUMBER);
    }

    public static UUID getOwnerUuid(Zombie zombie) {
        CompoundTag data = zombie.getPersistentData();
        if (data.hasUUID(OWNER_PLAYER_UUID)) {
            return data.getUUID(OWNER_PLAYER_UUID);
        }
        return null;
    }
}