package com.screenbog.zombiewaves.wave;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.config.ModConfig;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import com.screenbog.zombiewaves.network.ModNetwork;
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
    private int intervalOverrideSeconds = -1;
    private final Set<UUID> activeWaveZombies = new HashSet<>();

    /** Минимальная длительность волны (600 тиков = 30 секунд). */
    private static final long WAVE_MIN_DURATION_TICKS = 600L;
    /** Таймаут волны в тиках (1200 = 60 секунд). */
    private static final long WAVE_TIMEOUT_TICKS = 1200L;
    /** Пауза после волны (100 тиков = 5 секунд). */
    private static final int COOLDOWN_DURATION_TICKS = 100;

    private WaveManager() {
    }

    public static WaveManager get() {
        return INSTANCE;
    }

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
                    currentWave, state, tickCounter, getEffectiveIntervalSeconds()
            );
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to bind wave manager", e);
            resetToDefaults();
        }
    }

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
        this.intervalOverrideSeconds = -1;
    }

    private void loadWaveData(ServerLevel level) {
        WaveSavedData saved = WaveSavedData.get(level);
        this.currentWave = saved.getCurrentWave();
        this.tickCounter = saved.getTickCounter();
        this.state = saved.getState();
        this.waveStartTick = saved.getWaveStartTick();
        this.cooldownTicks = saved.getCooldownTicks();
        this.intervalOverrideSeconds = saved.getIntervalOverrideSeconds();
        ZombieWavesMod.LOGGER.info(
                "Restored wave state: wave={}, state={}, ticks={}, interval={}s",
                currentWave, state, tickCounter, getEffectiveIntervalSeconds()
        );
    }

    private void persistWaveData(ServerLevel level) {
        try {
            WaveSavedData saved = WaveSavedData.get(level);
            saved.update(currentWave, tickCounter, state, waveStartTick, cooldownTicks, intervalOverrideSeconds);
            saved.setDirty();
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to persist wave data", e);
        }
    }

    public int getEffectiveIntervalSeconds() {
        if (intervalOverrideSeconds > 0) {
            return intervalOverrideSeconds;
        }
        return ModConfig.SERVER.waveIntervalSeconds.get();
    }

    public int getEffectiveIntervalTicks() {
        return getEffectiveIntervalSeconds() * 20;
    }

    public boolean setRuntimeInterval(int seconds) {
        if (seconds < 60 || seconds > 7200) {
            return false;
        }
        try {
            intervalOverrideSeconds = seconds;
            if (server != null) {
                ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
                if (level != null) {
                    WaveSavedData.get(level).setIntervalOverride(seconds);
                    persistWaveData(level);
                }
            }
            ZombieWavesMod.LOGGER.info("Wave interval override set to {}s", seconds);
            return true;
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to set runtime interval", e);
            return false;
        }
    }

    /** OP: принудительно завершить текущую волну (с бонусами за зачистку). */
    public void forceEndCurrentWave() {
        if (state != WaveState.ACTIVE || server == null) {
            return;
        }
        try {
            ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
            if (level != null) {
                endWave(level, false);
            }
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to force end wave", e);
        }
    }

    /**
     * OP: пропустить волну — завершить без бонусов, сбросить таймер, уведомить всех.
     */
    public void skipWave() {
        if (server == null) {
            return;
        }
        try {
            ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
            if (level == null) {
                return;
            }

            if (state == WaveState.ACTIVE) {
                // Завершаем без бонусов, без COOLDOWN
                activeWaveZombies.clear();
                waveStartTick = 0L;
                for (ServerPlayer player : level.players()) {
                    PlayerCoinData.resetWaveProgress(player);
                }
                ZombieWavesMod.LOGGER.info("Active wave {} skipped by operator", currentWave);
            }

            state = WaveState.IDLE;
            cooldownTicks = 0;
            tickCounter = 0;
            waveStartTick = 0L;
            activeWaveZombies.clear();
            persistWaveData(level);

            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(Component.translatable("message.zombiewaves.wave_skipped_admin"));
            }

            ModNetwork.broadcastMenuData(level);
            ZombieWavesMod.LOGGER.info("Wave skipped by operator, timer reset");
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to skip wave", e);
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
        return Math.max(0, getEffectiveIntervalTicks() - tickCounter);
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
        if (tickCounter >= getEffectiveIntervalTicks()) {
            startWave(overworld);
        }

        persistWaveData(overworld);
    }

    private void startWave(ServerLevel level) {
        try {
            currentWave++;
            tickCounter = 0;
            cooldownTicks = 0;
            activeWaveZombies.clear();
            waveStartTick = 0L;

            int spawned = WaveSpawner.spawnWaveForPlayers(level, currentWave, activeWaveZombies);

            // Волна активна только если реально заспавнились зомби
            if (spawned <= 0) {
                currentWave--;
                state = WaveState.IDLE;
                ZombieWavesMod.LOGGER.warn("Wave aborted: no zombies spawned for wave {}", currentWave + 1);
                persistWaveData(level);
                return;
            }

            state = WaveState.ACTIVE;
            waveStartTick = server.getTickCount();

            WaveSpawner.broadcastWaveStart(level, currentWave, spawned);
            ZombieWavesMod.LOGGER.info("Wave {} started. Spawned {} zombies.", currentWave, spawned);
            persistWaveData(level);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to start wave {}", currentWave, e);
            state = WaveState.IDLE;
            waveStartTick = 0L;
            persistWaveData(level);
        }
    }

    private void handleActiveWave(ServerLevel level) {
        try {
            activeWaveZombies.retainAll(WaveSpawner.filterAliveWaveZombies(level, activeWaveZombies));

            boolean hasEligiblePlayers = false;
            boolean allPlayersCleared = true;

            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
                int quota = PlayerCoinData.getWaveQuota(player);
                int kills = PlayerCoinData.getWaveKills(player);

                if (quota > 0) {
                    hasEligiblePlayers = true;
                    if (kills < quota) {
                        allPlayersCleared = false;
                    }
                }
            }

            long currentTick = server.getTickCount();
            long elapsed = currentTick - waveStartTick;
            boolean zombiesEmpty = activeWaveZombies.isEmpty();
            boolean minDurationPassed = elapsed >= WAVE_MIN_DURATION_TICKS;

            // Нет игроков с квотой — ждём минимум 30 сек, затем отменяем волну
            if (!hasEligiblePlayers) {
                if (minDurationPassed) {
                    ZombieWavesMod.LOGGER.warn("Wave {} ended: no players with spawn quota", currentWave);
                    endWave(level, false);
                }
                return;
            }

            // Таймаут: зомби исчезли, прошло 60+ сек
            if (zombiesEmpty && elapsed >= WAVE_TIMEOUT_TICKS) {
                ZombieWavesMod.LOGGER.warn("Wave forced end due to timeout");
                endWave(level, false);
                return;
            }

            // Штатное завершение: все выполнили квоту, зомби мертвы, минимум 30 сек прошло
            if (allPlayersCleared && zombiesEmpty && minDurationPassed) {
                endWave(level, false);
            }
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to handle active wave {}", currentWave, e);
        }
    }

    /**
     * Завершает волну. При skipped=false выдаёт бонус за зачистку всем, кто выполнил квоту.
     */
    private void endWave(ServerLevel level, boolean skipped) {
        try {
            if (!skipped) {
                distributeWaveClearBonuses(level);
            }

            state = WaveState.COOLDOWN;
            cooldownTicks = COOLDOWN_DURATION_TICKS;
            tickCounter = 0;
            waveStartTick = 0L;
            activeWaveZombies.clear();

            if (!skipped) {
                WaveSpawner.broadcastWaveEnd(level, currentWave);
            }

            for (ServerPlayer player : level.players()) {
                PlayerCoinData.resetWaveProgress(player);
            }

            ZombieWavesMod.LOGGER.info("Wave {} ended. Cooldown {} ticks. skipped={}", currentWave, cooldownTicks, skipped);
            persistWaveData(level);
            ModNetwork.broadcastMenuData(level);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to end wave {}", currentWave, e);
            state = WaveState.IDLE;
            cooldownTicks = 0;
            waveStartTick = 0L;
            persistWaveData(level);
        }
    }

    /**
     * Бонус за полную зачистку волны — выдаётся при штатном завершении.
     */
    private void distributeWaveClearBonuses(ServerLevel level) {
        try {
            int bonus = ModConfig.SERVER.waveClearBonus.get();
            if (bonus <= 0) {
                return;
            }

            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }

                int quota = PlayerCoinData.getWaveQuota(player);
                int kills = PlayerCoinData.getWaveKills(player);

                if (quota > 0 && kills >= quota) {
                    PlayerCoinData.addCoins(player, bonus);
                    PlayerCoinData.incrementWavesCleared(player);
                    ModNetwork.sendCoinsToClient(player, PlayerCoinData.getCoins(player));

                    player.sendSystemMessage(Component.translatable(
                            "message.zombiewaves.wave_clear_bonus_gold",
                            bonus
                    ));

                    ZombieWavesMod.LOGGER.info(
                            "Wave clear bonus {} given to player {} ({}/{})",
                            bonus, player.getUUID(), kills, quota
                    );
                }
            }
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to distribute wave clear bonuses", e);
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