package com.screenbog.zombiewaves.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.screenbog.zombiewaves.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Команда /shop — каталог и покупка предметов за монеты.
 */
public final class ShopCommand {
    private static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    ShopManager.getItems().stream().map(item -> item.getId()),
                    builder
            );

    private ShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                .executes(ShopCommand::openShop)
                .then(Commands.literal("buy")
                        .then(Commands.argument("item", StringArgumentType.word())
                                .suggests(SHOP_SUGGESTIONS)
                                .executes(ShopCommand::buyItem))));
    }

    private static int openShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManager.sendCatalog(player);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.zombiewaves.players_only"));
            return 0;
        }
    }

    private static int buyItem(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            String itemId = StringArgumentType.getString(context, "item");
            return ShopManager.tryPurchase(player, itemId) ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.zombiewaves.players_only"));
            return 0;
        }
    }
}