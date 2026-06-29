package com.screenbog.zombiewaves;

import com.screenbog.zombiewaves.commands.CoinsCommand;
import com.screenbog.zombiewaves.commands.ShopCommand;
import com.screenbog.zombiewaves.config.ModConfig;
import com.screenbog.zombiewaves.integration.ModIntegrations;
import com.screenbog.zombiewaves.network.ModNetwork;
import com.screenbog.zombiewaves.registry.ModItems;
import com.screenbog.zombiewaves.wave.WaveManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Главный класс мода ZombieWavesMod.
 * Регистрирует предметы, конфиг, сеть и серверные системы волн.
 */
@Mod(ZombieWavesMod.MOD_ID)
public class ZombieWavesMod {
    public static final String MOD_ID = "zombiewaves";
    public static final Logger LOGGER = LogManager.getLogger();

    public ZombieWavesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModConfig.register();

        modEventBus.addListener(this::commonSetup);

        // ModEvents регистрируется автоматически через @Mod.EventBusSubscriber
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                ModNetwork.register();
                ModIntegrations.init();
                LOGGER.info("ZombieWavesMod common setup complete.");
            } catch (Exception e) {
                LOGGER.error("Failed during common setup", e);
            }
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        try {
            MinecraftForge.EVENT_BUS.register(WaveManager.get());
            WaveManager.get().bindServer(event.getServer());
            LOGGER.info("Wave manager bound to server.");
        } catch (Exception e) {
            LOGGER.error("Failed to start wave manager", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        try {
            WaveManager.get().unbindServer();
            MinecraftForge.EVENT_BUS.unregister(WaveManager.get());
        } catch (Exception e) {
            LOGGER.error("Failed to stop wave manager cleanly", e);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CoinsCommand.register(event.getDispatcher());
        ShopCommand.register(event.getDispatcher());
    }
}