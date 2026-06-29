package com.screenbog.zombiewaves.common.menu;

import com.screenbog.zombiewaves.data.PlayerCoinData;
import com.screenbog.zombiewaves.shop.ShopManager;
import com.screenbog.zombiewaves.wave.WaveManager;
import com.screenbog.zombiewaves.wave.WaveState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Снимок данных для GUI (сервер -> клиент).
 */
public class MenuData {
    public int coins;
    public int currentWave;
    public int waveKills;
    public int waveQuota;
    public int totalKills;
    public int wavesCleared;
    public int ticksUntilNextWave;
    public String waveState = WaveState.IDLE.name();
    public boolean isOp;
    public int waveIntervalSeconds;
    public final List<ShopEntry> shopEntries = new ArrayList<>();

    public static MenuData fromPlayer(ServerPlayer player) {
        MenuData data = new MenuData();
        data.coins = PlayerCoinData.getCoins(player);
        data.currentWave = WaveManager.get().getCurrentWave();
        data.waveKills = PlayerCoinData.getWaveKills(player);
        data.waveQuota = PlayerCoinData.getWaveQuota(player);
        data.totalKills = PlayerCoinData.getTotalKills(player);
        data.wavesCleared = PlayerCoinData.getWavesCleared(player);
        data.ticksUntilNextWave = WaveManager.get().getTicksUntilNextWave();
        data.waveState = WaveManager.get().getState().name();
        data.isOp = player.hasPermissions(2);
        data.waveIntervalSeconds = WaveManager.get().getEffectiveIntervalSeconds();
        data.shopEntries.addAll(ShopManager.buildShopEntries());
        return data;
    }

    public static MenuData read(FriendlyByteBuf buf) {
        MenuData data = new MenuData();
        data.coins = buf.readInt();
        data.currentWave = buf.readInt();
        data.waveKills = buf.readInt();
        data.waveQuota = buf.readInt();
        data.totalKills = buf.readInt();
        data.wavesCleared = buf.readInt();
        data.ticksUntilNextWave = buf.readInt();
        data.waveState = buf.readUtf();
        data.isOp = buf.readBoolean();
        data.waveIntervalSeconds = buf.readInt();

        int shopSize = buf.readVarInt();
        for (int i = 0; i < shopSize; i++) {
            data.shopEntries.add(new ShopEntry(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
        }
        return data;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(coins);
        buf.writeInt(currentWave);
        buf.writeInt(waveKills);
        buf.writeInt(waveQuota);
        buf.writeInt(totalKills);
        buf.writeInt(wavesCleared);
        buf.writeInt(ticksUntilNextWave);
        buf.writeUtf(waveState);
        buf.writeBoolean(isOp);
        buf.writeInt(waveIntervalSeconds);

        buf.writeVarInt(shopEntries.size());
        for (ShopEntry entry : shopEntries) {
            buf.writeUtf(entry.id);
            buf.writeUtf(entry.itemId);
            buf.writeVarInt(entry.price);
            buf.writeVarInt(entry.count);
        }
    }

    public int getSecondsUntilNextWave() {
        return ticksUntilNextWave / 20;
    }

    public static final class ShopEntry {
        public final String id;
        public final String itemId;
        public final int price;
        public final int count;

        public ShopEntry(String id, String itemId, int price, int count) {
            this.id = id;
            this.itemId = itemId;
            this.price = price;
            this.count = count;
        }

        public ItemStack createPreviewStack() {
            return ShopManager.createPreviewStack(itemId, count);
        }
    }
}