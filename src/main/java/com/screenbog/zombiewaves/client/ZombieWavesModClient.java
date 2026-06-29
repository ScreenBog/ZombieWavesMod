package com.screenbog.zombiewaves.client;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.client.gui.ZwMainScreen;
import com.screenbog.zombiewaves.common.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Клиентская инициализация: регистрация Screen для MenuType.
 */
@Mod.EventBusSubscriber(modid = ZombieWavesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ZombieWavesModClient {
    private ZombieWavesModClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ZW_MAIN_MENU.get(), ZwMainScreen::new));
    }
}