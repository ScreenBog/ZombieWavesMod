package com.screenbog.zombiewaves.wave;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Персистентное глобальное состояние волн (переживает рестарт сервера).
 */
public class WaveSavedData extends SavedData {
    public static final String DATA_ID = "zombiewaves_waves";

    private int currentWave;
    private int tickCounter;
    private WaveState state = WaveState.IDLE;
    private long waveStartTick;
    private int cooldownTicks;
    private int intervalOverrideSeconds = -1;

    public WaveSavedData() {
    }

    public static WaveSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WaveSavedData::load,
                WaveSavedData::new,
                DATA_ID
        );
    }

    public static WaveSavedData load(CompoundTag tag) {
        WaveSavedData data = new WaveSavedData();
        try {
            data.currentWave = tag.getInt("currentWave");
            data.tickCounter = tag.getInt("tickCounter");
            data.waveStartTick = tag.getLong("waveStartTick");
            data.cooldownTicks = tag.getInt("cooldownTicks");
            data.intervalOverrideSeconds = tag.contains("intervalOverride") ? tag.getInt("intervalOverride") : -1;
            String stateName = tag.getString("state");
            data.state = stateName.isEmpty() ? WaveState.IDLE : WaveState.valueOf(stateName);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to load wave saved data, using defaults", e);
            data.currentWave = 0;
            data.tickCounter = 0;
            data.state = WaveState.IDLE;
            data.waveStartTick = 0L;
            data.cooldownTicks = 0;
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("currentWave", currentWave);
        tag.putInt("tickCounter", tickCounter);
        tag.putString("state", state.name());
        tag.putLong("waveStartTick", waveStartTick);
        tag.putInt("cooldownTicks", cooldownTicks);
        tag.putInt("intervalOverride", intervalOverrideSeconds);
        return tag;
    }

    public void update(int currentWave, int tickCounter, WaveState state, long waveStartTick, int cooldownTicks, int intervalOverrideSeconds) {
        this.currentWave = currentWave;
        this.tickCounter = tickCounter;
        this.state = state;
        this.waveStartTick = waveStartTick;
        this.cooldownTicks = cooldownTicks;
        this.intervalOverrideSeconds = intervalOverrideSeconds;
    }

    public void setIntervalOverride(int seconds) {
        this.intervalOverrideSeconds = seconds;
        setDirty();
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public WaveState getState() {
        return state;
    }

    public long getWaveStartTick() {
        return waveStartTick;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getIntervalOverrideSeconds() {
        return intervalOverrideSeconds;
    }
}