package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.client.ClientMenuHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Результат покупки для анимации и звука: сервер -> клиент.
 */
public class PurchaseResultPacket {
    private final boolean success;
    private final String itemId;

    public PurchaseResultPacket(boolean success, String itemId) {
        this.success = success;
        this.itemId = itemId == null ? "" : itemId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getItemId() {
        return itemId;
    }

    public static void encode(PurchaseResultPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeUtf(packet.itemId);
    }

    public static PurchaseResultPacket decode(FriendlyByteBuf buf) {
        return new PurchaseResultPacket(buf.readBoolean(), buf.readUtf());
    }

    public static void handle(PurchaseResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientMenuHandler.handlePurchaseResult(packet.isSuccess(), packet.getItemId())));
        context.setPacketHandled(true);
    }
}