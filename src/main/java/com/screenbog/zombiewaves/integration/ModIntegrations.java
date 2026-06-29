package com.screenbog.zombiewaves.integration;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.shop.ShopItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

/**
 * Точки интеграции с опциональными модами.
 * Все проверки безопасны — мод работает и без них.
 */
public final class ModIntegrations {
    public static final String GECKOLIB = "geckolib";
    public static final String AI_IMPROVEMENTS = "aiimprovements";
    public static final String ZOMBIES_BREAK_BUILD = "zombiesbreakbuild";
    public static final String MARBLED_ARSENAL = "marbledsarsenal";

    private static boolean geckoLibLoaded;
    private static boolean aiImprovementsLoaded;
    private static boolean zombiesBreakBuildLoaded;
    private static boolean marbledArsenalLoaded;

    private ModIntegrations() {
    }

    public static void init() {
        geckoLibLoaded = isLoaded(GECKOLIB);
        aiImprovementsLoaded = isLoaded(AI_IMPROVEMENTS);
        zombiesBreakBuildLoaded = isLoaded(ZOMBIES_BREAK_BUILD);
        marbledArsenalLoaded = isLoaded(MARBLED_ARSENAL);

        ZombieWavesMod.LOGGER.info("Integrations: GeckoLib={}, AI-Improvements={}, ZBB={}, MarbledArsenal={}",
                geckoLibLoaded, aiImprovementsLoaded, zombiesBreakBuildLoaded, marbledArsenalLoaded);
    }

    private static boolean isLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.warn("Failed to check mod presence: {}", modId, e);
            return false;
        }
    }

    /**
     * Хук для будущих баффов зомби от сторонних модов.
     */
    public static void applyToZombie(Zombie zombie, int waveNumber) {
        if (zombiesBreakBuildLoaded) {
            zombie.getPersistentData().putBoolean("zombiewaves_allow_break", true);
        }
        if (aiImprovementsLoaded) {
            zombie.getPersistentData().putBoolean("zombiewaves_ai_boost", waveNumber >= 4);
        }
        if (geckoLibLoaded) {
            zombie.getPersistentData().putString("zombiewaves_anim_profile", "wave_" + waveNumber);
        }
        if (marbledArsenalLoaded) {
            zombie.getPersistentData().putBoolean("zombiewaves_arsenal_eligible", waveNumber >= 7);
        }
    }

    /**
     * Регистрация товаров магазина от опциональных модов.
     */
    public static void registerShopItems(Consumer<ShopItem> registrar) {
        if (marbledArsenalLoaded) {
            // Placeholder: реальные предметы Marbled's Arsenal можно подключить позже по registry name
            registrar.accept(ShopItem.ofItem(
                    "arsenal_token",
                    Component.translatable("item.zombiewaves.shop.arsenal_token"),
                    120,
                    Items.NETHER_STAR,
                    1
            ));
        }
    }

    public static boolean isGeckoLibLoaded() {
        return geckoLibLoaded;
    }

    public static boolean isAiImprovementsLoaded() {
        return aiImprovementsLoaded;
    }

    public static boolean isZombiesBreakBuildLoaded() {
        return zombiesBreakBuildLoaded;
    }

    public static boolean isMarbledArsenalLoaded() {
        return marbledArsenalLoaded;
    }
}