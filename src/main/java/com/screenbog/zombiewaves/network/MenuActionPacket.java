package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.common.MenuHelper;
import com.screenbog.zombiewaves.common.menu.MenuAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Пакет действия GUI: клиент -> сервер.
 */
public class MenuActionPacket {
    private final int actionId;
    private final String itemId;
    private final int intervalSeconds;

    public MenuActionPacket(MenuAction action, String itemId, int intervalSeconds) {
        this.actionId = action.ordinal();
        this.itemId = itemId == null ? "" : itemId;
        this.intervalSeconds = intervalSeconds;
    }

    public static void encode(MenuActionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.actionId);
        buf.writeUtf(packet.itemId);
        buf.writeInt(packet.intervalSeconds);
    }

    public static MenuActionPacket decode(FriendlyByteBuf buf) {
        return new MenuActionPacket(
                MenuAction.fromId(buf.readVarInt()),
                buf.readUtf(),
                buf.readInt()
        );
    }

    public static void handle(MenuActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            MenuAction action = MenuAction.fromId(packet.actionId);
            if (action == null) {
                return;
            }
            MenuHelper.handleAction(player, action, packet.itemId, packet.intervalSeconds);
        });
        context.setPacketHandled(true);
    }
}