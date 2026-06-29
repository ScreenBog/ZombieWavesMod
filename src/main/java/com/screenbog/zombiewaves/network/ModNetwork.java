package com.screenbog.zombiewaves.network;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Заготовка сетевого канала для будущей синхронизации монет и GUI магазина.
 */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZombieWavesMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        // Пакеты будут добавлены при переходе на GUI-магазин и клиентский HUD.
        ZombieWavesMod.LOGGER.debug("Network channel registered: {}", ZombieWavesMod.MOD_ID);
    }
}