package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Сетевой канал для синхронизации монет и будущего GUI-магазина.
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

            ZombieWavesMod.LOGGER.info("Network channel registered with SyncCoinsPacket stub");
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to register network packets", e);
        }
    }

    /** Синхронизирует баланс монет на клиент (заготовка под GUI). */
    public static void sendCoinsToClient(ServerPlayer player, int coins) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCoinsPacket(coins));
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to send coin sync to player {}", player.getUUID(), e);
        }
    }
}