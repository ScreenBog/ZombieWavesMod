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

/**
 * Обработчики игровых событий: убийства зомби, клонирование данных игрока.
 */
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
            int reward = ModConfig.SERVER.coinPerKill.get();
            PlayerCoinData.addCoins(killer, reward);
            PlayerCoinData.incrementWaveKills(killer);
            PlayerCoinData.incrementTotalKills(killer);

            int quota = PlayerCoinData.getWaveQuota(killer);
            int kills = PlayerCoinData.getWaveKills(killer);

            if (quota > 0 && kills >= quota) {
                int bonus = ModConfig.SERVER.waveClearBonus.get();
                PlayerCoinData.addCoins(killer, bonus);
                PlayerCoinData.incrementWavesCleared(killer);
                PlayerCoinData.setWaveQuota(killer, 0);

                killer.sendSystemMessage(Component.translatable(
                        "message.zombiewaves.wave_clear_bonus",
                        bonus,
                        PlayerCoinData.getCoins(killer)
                ));
            } else if (killer instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.zombiewaves.kill_reward",
                        reward,
                        kills,
                        Math.max(quota, 1)
                ), true);
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
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(
                    "message.zombiewaves.welcome",
                    ModConfig.SERVER.waveIntervalSeconds.get() / 60
            ));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WaveManager.get().sendStatus(player);
        }
    }
}