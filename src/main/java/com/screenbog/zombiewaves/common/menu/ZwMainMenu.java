package com.screenbog.zombiewaves.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Контейнерное меню главного GUI (без слотов инвентаря).
 */
public class ZwMainMenu extends AbstractContainerMenu {
    private MenuData data = new MenuData();
    private int initialTab;

    /** Серверная фабрика (без данных в буфере). */
    public ZwMainMenu(int containerId, Inventory inventory) {
        super(ModMenus.ZW_MAIN_MENU.get(), containerId);
    }

    /** Клиентская фабрика — читает синхронизированные данные. */
    public ZwMainMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        super(ModMenus.ZW_MAIN_MENU.get(), containerId);
        if (buf != null && buf.readableBytes() > 0) {
            this.data = MenuData.read(buf);
            if (buf.isReadable()) {
                this.initialTab = buf.readVarInt();
            }
        }
    }

    public int getInitialTab() {
        return initialTab;
    }

    public MenuData getData() {
        return data;
    }

    public void setData(MenuData data) {
        this.data = data;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}