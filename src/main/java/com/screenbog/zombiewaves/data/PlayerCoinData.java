package com.screenbog.zombiewaves.data;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Хранит монеты и статистику волн для каждого игрока отдельно.
 * Данные сохраняются в persistent NBT игрока (работает в multiplayer).
 */
public final class PlayerCoinData {
    private static final String ROOT = ZombieWavesMod.MOD_ID;
    private static final String COINS = "coins";
    private static final String KILLS = "wave_kills";
    private static final String WAVE_QUOTA = "wave_quota";
    private static final String TOTAL_KILLS = "total_kills";
    private static final String WAVES_CLEARED = "waves_cleared";

    private PlayerCoinData() {
    }

    public static CompoundTag getRoot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    public static int getCoins(Player player) {
        return getRoot(player).getInt(COINS);
    }

    public static void setCoins(Player player, int amount) {
        getRoot(player).putInt(COINS, Math.max(0, amount));
    }

    public static void addCoins(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        setCoins(player, getCoins(player) + amount);
    }

    public static boolean trySpend(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        int current = getCoins(player);
        if (current < amount) {
            return false;
        }
        setCoins(player, current - amount);
        return true;
    }

    public static int getWaveKills(Player player) {
        return getRoot(player).getInt(KILLS);
    }

    public static void setWaveKills(Player player, int kills) {
        getRoot(player).putInt(KILLS, Math.max(0, kills));
    }

    public static void incrementWaveKills(Player player) {
        setWaveKills(player, getWaveKills(player) + 1);
    }

    public static int getWaveQuota(Player player) {
        return getRoot(player).getInt(WAVE_QUOTA);
    }

    public static void setWaveQuota(Player player, int quota) {
        getRoot(player).putInt(WAVE_QUOTA, Math.max(0, quota));
    }

    public static int getTotalKills(Player player) {
        return getRoot(player).getInt(TOTAL_KILLS);
    }

    public static void incrementTotalKills(Player player) {
        getRoot(player).putInt(TOTAL_KILLS, getTotalKills(player) + 1);
    }

    public static int getWavesCleared(Player player) {
        return getRoot(player).getInt(WAVES_CLEARED);
    }

    public static void incrementWavesCleared(Player player) {
        getRoot(player).putInt(WAVES_CLEARED, getWavesCleared(player) + 1);
    }

    public static void resetWaveProgress(Player player) {
        setWaveKills(player, 0);
        setWaveQuota(player, 0);
    }

    public static void copy(Player original, Player clone) {
        CompoundTag source = original.getPersistentData().getCompound(ROOT);
        if (!source.isEmpty()) {
            clone.getPersistentData().put(ROOT, source.copy());
        }
    }

    public static void notifyCoins(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable(
                "message.zombiewaves.coins_balance",
                getCoins(player)
        ));
    }
}