package com.screenbog.zombiewaves.shop;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import com.screenbog.zombiewaves.integration.ModIntegrations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Базовый магазин через чат. Покупки списывают монеты у конкретного игрока.
 */
public final class ShopManager {
    private static final Map<String, ShopItem> ITEMS = new LinkedHashMap<>();

    static {
        registerDefaults();
        ModIntegrations.registerShopItems(ShopManager::register);
    }

    private ShopManager() {
    }

    private static void registerDefaults() {
        register(ShopItem.ofItem("bread", Component.translatable("item.zombiewaves.shop.bread"), 5, Items.BREAD, 8));
        register(ShopItem.ofItem("arrow", Component.translatable("item.zombiewaves.shop.arrows"), 8, Items.ARROW, 16));
        register(ShopItem.ofItem("iron_sword", Component.translatable("item.zombiewaves.shop.iron_sword"), 35, Items.IRON_SWORD, 1));
        register(ShopItem.ofItem("golden_apple", Component.translatable("item.zombiewaves.shop.golden_apple"), 50, Items.GOLDEN_APPLE, 1));
        register(ShopItem.ofItem("shield", Component.translatable("item.zombiewaves.shop.shield"), 40, Items.SHIELD, 1));
        register(ShopItem.ofItem("torch_bundle", Component.translatable("item.zombiewaves.shop.torch_bundle"), 6, Items.TORCH, 32));
    }

    public static void register(ShopItem item) {
        if (item == null || item.getId() == null || item.getId().isBlank()) {
            ZombieWavesMod.LOGGER.warn("Skipped invalid shop item registration");
            return;
        }
        ITEMS.put(item.getId().toLowerCase(), item);
    }

    public static List<ShopItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(ITEMS.values()));
    }

    public static Optional<ShopItem> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ITEMS.get(id.toLowerCase()));
    }

    public static void sendCatalog(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.zombiewaves.shop_header"));
        for (ShopItem item : ITEMS.values()) {
            player.sendSystemMessage(Component.translatable(
                    "message.zombiewaves.shop_entry",
                    item.getId(),
                    item.getDisplayName(),
                    item.getPrice()
            ));
        }
        player.sendSystemMessage(Component.translatable("message.zombiewaves.shop_help"));
        player.sendSystemMessage(Component.translatable(
                "message.zombiewaves.shop_balance",
                PlayerCoinData.getCoins(player)
        ));
    }

    public static boolean tryPurchase(ServerPlayer player, String itemId) {
        Optional<ShopItem> optional = find(itemId);
        if (optional.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.zombiewaves.shop_unknown", itemId));
            return false;
        }

        ShopItem shopItem = optional.get();
        int price = shopItem.getPrice();
        int balance = PlayerCoinData.getCoins(player);

        if (balance < price) {
            player.sendSystemMessage(Component.translatable(
                    "message.zombiewaves.shop_not_enough",
                    price,
                    balance
            ));
            return false;
        }

        ItemStack stack = shopItem.createStack();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.zombiewaves.shop_invalid_item"));
            ZombieWavesMod.LOGGER.warn("Shop item {} produced empty stack", shopItem.getId());
            return false;
        }

        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }

        if (!PlayerCoinData.trySpend(player, price)) {
            player.sendSystemMessage(Component.translatable("message.zombiewaves.shop_error"));
            return false;
        }

        player.sendSystemMessage(Component.translatable(
                "message.zombiewaves.shop_success",
                shopItem.getDisplayName(),
                price,
                PlayerCoinData.getCoins(player)
        ));
        return true;
    }
}