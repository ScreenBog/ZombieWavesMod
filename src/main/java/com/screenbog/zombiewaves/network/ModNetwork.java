package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.common.menu.MenuAction;
import com.screenbog.zombiewaves.common.menu.MenuData;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Сетевой канал: синхронизация монет, GUI-данных и действий игрока.
 */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZombieWavesMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {
    }

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        try {
            CHANNEL.messageBuilder(SyncCoinsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(SyncCoinsPacket::encode)
                    .decoder(SyncCoinsPacket::decode)
                    .consumerMainThread(SyncCoinsPacket::handle)
                    .add();

            CHANNEL.messageBuilder(SyncMenuDataPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(SyncMenuDataPacket::encode)
                    .decoder(SyncMenuDataPacket::decode)
                    .consumerMainThread(SyncMenuDataPacket::handle)
                    .add();

            CHANNEL.messageBuilder(PurchaseResultPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(PurchaseResultPacket::encode)
                    .decoder(PurchaseResultPacket::decode)
                    .consumerMainThread(PurchaseResultPacket::handle)
                    .add();

            CHANNEL.messageBuilder(MenuActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                    .encoder(MenuActionPacket::encode)
                    .decoder(MenuActionPacket::decode)
                    .consumerMainThread(MenuActionPacket::handle)
                    .add();

            ZombieWavesMod.LOGGER.info("Network channel registered with GUI packets");
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to register network packets", e);
        }
    }

    public static void sendCoinsToClient(ServerPlayer player, int coins) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCoinsPacket(coins));
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to send coin sync to player {}", player.getUUID(), e);
        }
    }

    public static void sendMenuData(ServerPlayer player) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMenuDataPacket(MenuData.fromPlayer(player)));
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to send menu data to player {}", player.getUUID(), e);
        }
    }

    public static void sendPurchaseResult(ServerPlayer player, boolean success, String itemId) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PurchaseResultPacket(success, itemId));
            sendCoinsToClient(player, PlayerCoinData.getCoins(player));
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to send purchase result to player {}", player.getUUID(), e);
        }
    }

    public static void sendMenuAction(MenuAction action, String itemId, int intervalSeconds) {
        try {
            CHANNEL.sendToServer(new MenuActionPacket(action, itemId, intervalSeconds));
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to send menu action {}", action, e);
        }
    }
}