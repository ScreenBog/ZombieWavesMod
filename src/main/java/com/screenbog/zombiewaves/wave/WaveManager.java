package com.screenbog.zombiewaves.wave;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.config.ModConfig;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Глобальный менеджер волн. Работает только на сервере.
 */
public final class WaveManager {
    private static final WaveManager INSTANCE = new WaveManager();

    private MinecraftServer server;
    private WaveState state = WaveState.IDLE;
    private int tickCounter = 0;
    private int currentWave = 0;
    private long waveStartTick = 0L;
    private int cooldownTicks = 0;
    private final Set<UUID> activeWaveZombies = new HashSet<>();

    /** Таймаут волны в тиках (1200 = 60 секунд). */
    private static final long WAVE_TIMEOUT_TICKS = 1200L;
    /** Пауза после волны (100 тиков = 5 секунд). */
    private static final int COOLDOWN_DURATION_TICKS = 100;

    private WaveManager() {
    }

    public static WaveManager get() {
        return INSTANCE;
    }

    /**
     * Исправлено (Prompt 3): загрузка глобального состояния волн из SavedData.
     */
    public void bindServer(MinecraftServer server) {
        try {
            this.server = server;
            this.activeWaveZombies.clear();

            ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
            if (overworld != null) {
                loadWaveData(overworld);
            } else {
                resetToDefaults();
            }

            ZombieWavesMod.LOGGER.info(
                    "WaveManager ready. Wave={}, state={}, tickCounter={}, interval={}s",
                    currentWave, state, tickCounter, ModConfig.SERVER.waveIntervalSeconds.get()
            );
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to bind wave manager", e);
            resetToDefaults();
        }
    }

    /**
     * Исправлено (Prompt 3): сохранение глобального состояния при остановке сервера.
     */
    public void unbindServer() {
        try {
            if (server != null) {
                ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
                if (overworld != null) {
                    persistWaveData(overworld);
                }
            }
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to save wave data on unbind", e);
        } finally {
            this.server = null;
            this.activeWaveZombies.clear();
        }
    }

    private void resetToDefaults() {
        this.state = WaveState.IDLE;
        this.tickCounter = 0;
        this.currentWave = 0;
        this.waveStartTick = 0L;
        this.cooldownTicks = 0;
    }

    private void loadWaveData(ServerLevel level) {
        WaveSavedData saved = WaveSavedData.get(level);
        this.currentWave = saved.getCurrentWave();
        this.tickCounter = saved.getTickCounter();
        this.state = saved.getState();
        this.waveStartTick = saved.getWaveStartTick();
        this.cooldownTicks = saved.getCooldownTicks();
        ZombieWavesMod.LOGGER.info("Restored wave state: wave={}, state={}, ticks={}", currentWave, state, tickCounter);
    }

    private void persistWaveData(ServerLevel level) {
        try {
            WaveSavedData saved = WaveSavedData.get(level);
            saved.update(currentWave, tickCounter, state, waveStartTick, cooldownTicks);
            saved.setDirty();
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to persist wave data", e);
        }
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public WaveState getState() {
        return state;
    }

    public int getTicksUntilNextWave() {
        if (state == WaveState.ACTIVE || state == WaveState.COOLDOWN) {
            return 0;
        }
        return Math.max(0, ModConfig.SERVER.getWaveIntervalTicks() - tickCounter);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) {
            return;
        }

        try {
            tick();
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("WaveManager tick failed", e);
        }
    }

    private void tick() {
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        if (overworld == null || overworld.players().isEmpty()) {
            return;
        }

        // Исправлено (Prompt 7): реальная пауза COOLDOWN 5 секунд
        if (state == WaveState.COOLDOWN) {
            cooldownTicks--;
            if (cooldownTicks <= 0) {
                state = WaveState.IDLE;
                tickCounter = 0;
                ZombieWavesMod.LOGGER.debug("Wave cooldown finished, returning to IDLE");
            }
            persistWaveData(overworld);
            return;
        }

        if (state == WaveState.ACTIVE) {
            handleActiveWave(overworld);
            persistWaveData(overworld);
            return;
        }

        tickCounter++;
        if (tickCounter >= ModConfig.SERVER.getWaveIntervalTicks()) {
            startWave(overworld);
        }

        persistWaveData(overworld);
    }

    private void startWave(ServerLevel level) {
        try {
            currentWave++;
            tickCounter = 0;
            cooldownTicks = 0;
            state = WaveState.ACTIVE;
            activeWaveZombies.clear();
            waveStartTick = server.getTickCount();

            int spawned = WaveSpawner.spawnWaveForPlayers(level, currentWave, activeWaveZombies);
            WaveSpawner.broadcastWaveStart(level, currentWave, spawned);

            ZombieWavesMod.LOGGER.info("Wave {} started. Spawned {} zombies.", currentWave, spawned);
            persistWaveData(level);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to start wave {}", currentWave, e);
            state = WaveState.IDLE;
            persistWaveData(level);
        }
    }

    private void handleActiveWave(ServerLevel level) {
        try {
            activeWaveZombies.retainAll(WaveSpawner.filterAliveWaveZombies(level, activeWaveZombies));

            boolean allPlayersCleared = true;
            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
                int quota = PlayerCoinData.getWaveQuota(player);
                int kills = PlayerCoinData.getWaveKills(player);
                if (quota > 0 && kills < quota) {
                    allPlayersCleared = false;
                }
            }

            long currentTick = server.getTickCount();

            if (activeWaveZombies.isEmpty() && (currentTick - waveStartTick > WAVE_TIMEOUT_TICKS)) {
                ZombieWavesMod.LOGGER.warn("Wave forced end due to timeout");
                endWave(level);
                return;
            }

            if (allPlayersCleared) {
                endWave(level);
            }
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to handle active wave {}", currentWave, e);
        }
    }

    private void endWave(ServerLevel level) {
        try {
            state = WaveState.COOLDOWN;
            cooldownTicks = COOLDOWN_DURATION_TICKS;
            tickCounter = 0;
            waveStartTick = 0L;
            WaveSpawner.broadcastWaveEnd(level, currentWave);

            for (ServerPlayer player : level.players()) {
                PlayerCoinData.resetWaveProgress(player);
            }

            ZombieWavesMod.LOGGER.info("Wave {} ended. Cooldown {} ticks.", currentWave, cooldownTicks);
            persistWaveData(level);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to end wave {}", currentWave, e);
            state = WaveState.IDLE;
            cooldownTicks = 0;
            waveStartTick = 0L;
            persistWaveData(level);
        }
    }

    public void forceStartWave() {
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level != null) {
            startWave(level);
        }
    }

    public void sendStatus(ServerPlayer player) {
        int seconds = getTicksUntilNextWave() / 20;
        player.sendSystemMessage(Component.translatable(
                "message.zombiewaves.wave_status",
                currentWave,
                state.name(),
                seconds
        ));
    }
}