package com.screenbog.zombiewaves.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.screenbog.zombiewaves.common.MenuHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Команда /zwmenu — открывает главное GUI.
 */
public final class MenuCommand {
    private MenuCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zwmenu")
                .executes(MenuCommand::openMenu));
    }

    private static int openMenu(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            MenuHelper.openMainMenu(player);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.zombiewaves.players_only"));
            return 0;
        }
    }
}