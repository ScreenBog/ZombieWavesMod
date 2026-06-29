package com.screenbog.zombiewaves.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Заготовка пакета синхронизации монет для будущего GUI/HUD.
 */
public class SyncCoinsPacket {
    private final int coins;

    public SyncCoinsPacket(int coins) {
        this.coins = coins;
    }

    public int getCoins() {
        return coins;
    }

    public static void encode(SyncCoinsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.coins);
    }

    public static SyncCoinsPacket decode(FriendlyByteBuf buf) {
        return new SyncCoinsPacket(buf.readInt());
    }

    public static void handle(SyncCoinsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Клиентская обработка будет добавлена при реализации GUI-магазина.
        });
        context.setPacketHandled(true);
    }
}