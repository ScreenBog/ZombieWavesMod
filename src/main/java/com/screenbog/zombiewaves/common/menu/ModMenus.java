package com.screenbog.zombiewaves.common.menu;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Регистрация MenuType через DeferredRegister.
 */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ZombieWavesMod.MOD_ID);

    public static final RegistryObject<MenuType<ZwMainMenu>> ZW_MAIN_MENU = MENUS.register(
            "zw_main",
            () -> IForgeMenuType.create(ZwMainMenu::new)
    );

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}