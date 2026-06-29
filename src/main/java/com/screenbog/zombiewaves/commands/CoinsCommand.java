package com.screenbog.zombiewaves.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import com.screenbog.zombiewaves.wave.WaveManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Команда /coins — показывает баланс и статистику игрока.
 */
public final class CoinsCommand {
    private CoinsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("coins")
                .executes(CoinsCommand::showOwnCoins)
                .then(Commands.literal("stats")
                        .executes(CoinsCommand::showStats)));
    }

    private static int showOwnCoins(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            PlayerCoinData.notifyCoins(player);
            WaveManager.get().sendStatus(player);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.zombiewaves.players_only"));
            return 0;
        }
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            player.sendSystemMessage(Component.translatable(
                    "message.zombiewaves.stats",
                    PlayerCoinData.getCoins(player),
                    PlayerCoinData.getTotalKills(player),
                    PlayerCoinData.getWavesCleared(player),
                    WaveManager.get().getCurrentWave()
            ));
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.zombiewaves.players_only"));
            return 0;
        }
    }
}