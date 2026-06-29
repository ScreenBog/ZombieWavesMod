package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.client.ClientMenuHandler;
import com.screenbog.zombiewaves.common.menu.MenuData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Полная синхронизация данных GUI: сервер -> клиент.
 */
public class SyncMenuDataPacket {
    private final MenuData data;

    public SyncMenuDataPacket(MenuData data) {
        this.data = data;
    }

    public MenuData getData() {
        return data;
    }

    public static void encode(SyncMenuDataPacket packet, FriendlyByteBuf buf) {
        packet.data.write(buf);
    }

    public static SyncMenuDataPacket decode(FriendlyByteBuf buf) {
        return new SyncMenuDataPacket(MenuData.read(buf));
    }

    public static void handle(SyncMenuDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientMenuHandler.handleMenuData(packet.getData())));
        context.setPacketHandled(true);
    }
}