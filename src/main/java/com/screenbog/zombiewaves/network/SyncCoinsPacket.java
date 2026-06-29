package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.client.ClientMenuHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Синхронизация баланса монет: сервер -> клиент.
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
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientMenuHandler.setClientCoins(packet.getCoins())));
        context.setPacketHandled(true);
    }
}