package com.screenbog.zombiewaves.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Описание товара в магазине.
 */
public final class ShopItem {
    private final String id;
    private final Component displayName;
    private final int price;
    private final Supplier<ItemStack> stackSupplier;

    public ShopItem(String id, Component displayName, int price, Supplier<ItemStack> stackSupplier) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.stackSupplier = stackSupplier;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public int getPrice() {
        return price;
    }

    public ItemStack createStack() {
        ItemStack stack = stackSupplier.get();
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public static ShopItem ofItem(String id, Component name, int price, Item item, int count) {
        return new ShopItem(id, name, price, () -> new ItemStack(item, count));
    }
}