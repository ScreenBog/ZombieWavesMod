package com.screenbog.zombiewaves.client;

import com.screenbog.zombiewaves.client.gui.ZwMainScreen;
import com.screenbog.zombiewaves.common.menu.MenuData;
import com.screenbog.zombiewaves.common.menu.ZwMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентская обработка сетевых пакетов GUI.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMenuHandler {
    private static int clientCoins;

    private ClientMenuHandler() {
    }

    public static int getClientCoins() {
        return clientCoins;
    }

    public static void setClientCoins(int coins) {
        clientCoins = coins;
        refreshOpenScreen();
    }

    public static void handleMenuData(MenuData data) {
        clientCoins = data.coins;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ZwMainScreen screen) {
            screen.updateData(data);
        } else if (mc.player != null && mc.player.containerMenu instanceof ZwMainMenu menu) {
            menu.setData(data);
        }
    }

    public static void handlePurchaseResult(boolean success, String itemId) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof ZwMainScreen screen)) {
            return;
        }
        if (success) {
            screen.triggerPurchaseAnimation(itemId);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
        }
    }

    private static void refreshOpenScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ZwMainScreen screen) {
            screen.refreshCoins(clientCoins);
        }
    }
}