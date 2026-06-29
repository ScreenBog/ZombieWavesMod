package com.screenbog.zombiewaves.wave;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Помечает зомби тегами NBT, чтобы отличать волновых от обычных.
 */
public final class WaveZombieMarker {
    private static final String WAVE_TAG = ZombieWavesMod.MOD_ID + "_wave";
    private static final String WAVE_NUMBER = "wave_number";

    private WaveZombieMarker() {
    }

    public static void mark(Zombie zombie, int waveNumber) {
        CompoundTag data = zombie.getPersistentData();
        data.putBoolean(WAVE_TAG, true);
        data.putInt(WAVE_NUMBER, waveNumber);
    }

    public static boolean isWaveZombie(Zombie zombie) {
        return zombie.getPersistentData().getBoolean(WAVE_TAG);
    }

    public static int getWaveNumber(Zombie zombie) {
        return zombie.getPersistentData().getInt(WAVE_NUMBER);
    }
}