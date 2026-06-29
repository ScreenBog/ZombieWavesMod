package com.screenbog.zombiewaves.events;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.config.ModConfig;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import com.screenbog.zombiewaves.wave.WaveManager;
import com.screenbog.zombiewaves.wave.WaveZombieMarker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Обработчики игровых событий: убийства зомби, клонирование данных игрока.
 * Исправлено (Prompt 6): автоматическая регистрация через @Mod.EventBusSubscriber.
 */
@Mod.EventBusSubscriber(modid = ZombieWavesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModEvents {
    private ModEvents() {
    }

    @SubscribeEvent
    public static void onZombieDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }
        if (!WaveZombieMarker.isWaveZombie(zombie)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }

        try {
            // Исправлено (Prompt 4): награда только владельцу зомби
            UUID ownerUuid = WaveZombieMarker.getOwnerUuid(zombie);
            if (ownerUuid == null || !killer.getUUID().equals(ownerUuid)) {
                ZombieWavesMod.LOGGER.debug(
                        "Ignored wave zombie kill: killer {} is not owner {}",
                        killer.getUUID(),
                        ownerUuid
                );
                return;
            }

            int reward = ModConfig.SERVER.coinPerKill.get();
            PlayerCoinData.addCoins(killer, reward);
            PlayerCoinData.incrementWaveKills(killer);
            PlayerCoinData.incrementTotalKills(killer);

            int quota = PlayerCoinData.getWaveQuota(killer);
            int kills = PlayerCoinData.getWaveKills(killer);

            // Бонус за зачистку выдаётся в WaveManager.endWave(), здесь только награда за килл
            if (killer instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.zombiewaves.kill_reward",
                        reward,
                        kills,
                        Math.max(quota, 1)
                ), true);

                if (quota > 0 && kills >= quota) {
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "message.zombiewaves.quota_complete",
                            kills,
                            quota
                    ));
                }
            }
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to process zombie kill reward", e);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            PlayerCoinData.copy(event.getOriginal(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            // Исправлено (Prompt 5): валидация и загрузка данных при входе
            PlayerCoinData.validateAndLoad(player);
            player.sendSystemMessage(Component.translatable(
                    "message.zombiewaves.welcome",
                    ModConfig.SERVER.waveIntervalSeconds.get() / 60
            ));
            PlayerCoinData.notifyCoins(player);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to handle player login for {}", player.getUUID(), e);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZombieWavesMod.LOGGER.debug(
                    "Player {} logged out with {} coins",
                    player.getUUID(),
                    PlayerCoinData.getCoins(player)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WaveManager.get().sendStatus(player);
        }
    }
}