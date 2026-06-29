package com.screenbog.zombiewaves.common;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.common.menu.MenuAction;
import com.screenbog.zombiewaves.common.menu.MenuData;
import com.screenbog.zombiewaves.common.menu.ZwMainMenu;
import com.screenbog.zombiewaves.network.ModNetwork;
import com.screenbog.zombiewaves.shop.ShopManager;
import com.screenbog.zombiewaves.wave.WaveManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

/**
 * Серверная логика открытия GUI и обработки действий игрока.
 */
public final class MenuHelper {
    private MenuHelper() {
    }

    public static void openMainMenu(ServerPlayer player) {
        openMainMenu(player, 0);
    }

    public static void openMainMenu(ServerPlayer player, int tab) {
        try {
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (windowId, inventory, p) -> new ZwMainMenu(windowId, inventory),
                    Component.translatable("gui.zombiewaves.main.title")
            ), buf -> {
                MenuData.fromPlayer(player).write(buf);
                buf.writeVarInt(tab);
            });
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to open main menu for {}", player.getUUID(), e);
        }
    }

    public static void handleAction(ServerPlayer player, MenuAction action, String itemId, int intervalSeconds) {
        try {
            switch (action) {
                case BUY -> {
                    boolean success = ShopManager.tryPurchase(player, itemId);
                    ModNetwork.sendPurchaseResult(player, success, itemId);
                }
                case START_WAVE -> {
                    if (!player.hasPermissions(2)) {
                        player.sendSystemMessage(Component.translatable("gui.zombiewaves.op_only"));
                        return;
                    }
                    WaveManager.get().forceStartWave();
                    player.sendSystemMessage(Component.translatable("gui.zombiewaves.wave_started"));
                }
                case SKIP_WAVE -> {
                    if (!player.hasPermissions(2)) {
                        player.sendSystemMessage(Component.translatable("gui.zombiewaves.op_only"));
                        return;
                    }
                    // skipWave() уведомляет всех игроков и обновляет GUI
                    WaveManager.get().skipWave();
                }
                case END_WAVE -> {
                    if (!player.hasPermissions(2)) {
                        player.sendSystemMessage(Component.translatable("gui.zombiewaves.op_only"));
                        return;
                    }
                    WaveManager.get().forceEndCurrentWave();
                    player.sendSystemMessage(Component.translatable("gui.zombiewaves.wave_ended"));
                }
                case SET_INTERVAL -> {
                    if (!player.hasPermissions(2)) {
                        player.sendSystemMessage(Component.translatable("gui.zombiewaves.op_only"));
                        return;
                    }
                    if (WaveManager.get().setRuntimeInterval(intervalSeconds)) {
                        player.sendSystemMessage(Component.translatable(
                                "gui.zombiewaves.interval_set",
                                intervalSeconds
                        ));
                    } else {
                        player.sendSystemMessage(Component.translatable("gui.zombiewaves.interval_invalid"));
                    }
                }
            }
            ModNetwork.sendMenuData(player);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Menu action {} failed for {}", action, player.getUUID(), e);
        }
    }
}