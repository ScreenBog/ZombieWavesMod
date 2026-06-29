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
    private final Set<UUID> activeWaveZombies = new HashSet<>();

    private WaveManager() {
    }

    public static WaveManager get() {
        return INSTANCE;
    }

    public void bindServer(MinecraftServer server) {
        this.server = server;
        this.state = WaveState.IDLE;
        this.tickCounter = 0;
        this.currentWave = 0;
        this.activeWaveZombies.clear();
        ZombieWavesMod.LOGGER.info("WaveManager ready. Interval: {}s", ModConfig.SERVER.waveIntervalSeconds.get());
    }

    public void unbindServer() {
        this.server = null;
        this.activeWaveZombies.clear();
        this.state = WaveState.IDLE;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public WaveState getState() {
        return state;
    }

    public int getTicksUntilNextWave() {
        if (state == WaveState.ACTIVE) {
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

        if (state == WaveState.ACTIVE) {
            handleActiveWave(overworld);
            return;
        }

        tickCounter++;
        if (tickCounter >= ModConfig.SERVER.getWaveIntervalTicks()) {
            startWave(overworld);
        }
    }

    private void startWave(ServerLevel level) {
        currentWave++;
        tickCounter = 0;
        state = WaveState.ACTIVE;
        activeWaveZombies.clear();

        int spawned = WaveSpawner.spawnWaveForPlayers(level, currentWave, activeWaveZombies);
        WaveSpawner.broadcastWaveStart(level, currentWave, spawned);

        ZombieWavesMod.LOGGER.info("Wave {} started. Spawned {} zombies.", currentWave, spawned);
    }

    private void handleActiveWave(ServerLevel level) {
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

        if (activeWaveZombies.isEmpty() && allPlayersCleared) {
            endWave(level);
        }
    }

    private void endWave(ServerLevel level) {
        state = WaveState.COOLDOWN;
        tickCounter = 0;
        WaveSpawner.broadcastWaveEnd(level, currentWave);

        for (ServerPlayer player : level.players()) {
            PlayerCoinData.resetWaveProgress(player);
        }

        state = WaveState.IDLE;
        ZombieWavesMod.LOGGER.info("Wave {} ended.", currentWave);
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