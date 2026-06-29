package com.screenbog.zombiewaves.config;

import com.screenbog.zombiewaves.ZombieWavesMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**
 * Серверный конфиг мода. Все значения настраиваются в config/zombiewaves-server.toml.
 */
public final class ModConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    private ModConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(Type.SERVER, SERVER_SPEC);
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue waveIntervalSeconds;
        public final ForgeConfigSpec.IntValue baseZombieCount;
        public final ForgeConfigSpec.IntValue zombiesPerWave;
        public final ForgeConfigSpec.DoubleValue speedBonusPerWave;
        public final ForgeConfigSpec.DoubleValue babyZombieChance;
        public final ForgeConfigSpec.IntValue coinPerKill;
        public final ForgeConfigSpec.IntValue waveClearBonus;
        public final ForgeConfigSpec.IntValue spawnRadius;
        public final ForgeConfigSpec.BooleanValue announceWaves;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Zombie Waves Mod server settings").push("waves");

            waveIntervalSeconds = builder
                    .comment("Interval between waves in seconds (default 600 = 10 minutes)")
                    .defineInRange("waveIntervalSeconds", 600, 60, 7200);

            baseZombieCount = builder
                    .comment("Base number of zombies in wave 1")
                    .defineInRange("baseZombieCount", 5, 1, 200);

            zombiesPerWave = builder
                    .comment("Additional zombies added per wave number")
                    .defineInRange("zombiesPerWave", 2, 0, 50);

            speedBonusPerWave = builder
                    .comment("Movement speed multiplier bonus per wave (0.05 = +5% per wave)")
                    .defineInRange("speedBonusPerWave", 0.05D, 0.0D, 1.0D);

            babyZombieChance = builder
                    .comment("Chance for a spawned zombie to be a baby (0.0 - 1.0)")
                    .defineInRange("babyZombieChance", 0.15D, 0.0D, 1.0D);

            spawnRadius = builder
                    .comment("Spawn radius around each player during a wave")
                    .defineInRange("spawnRadius", 24, 8, 64);

            announceWaves = builder
                    .comment("Broadcast wave start/end messages to all players")
                    .define("announceWaves", true);

            builder.pop();
            builder.push("economy");

            coinPerKill = builder
                    .comment("Coins awarded per zombie kill during an active wave")
                    .defineInRange("coinPerKill", 2, 0, 1000);

            waveClearBonus = builder
                    .comment("Bonus coins awarded when a player clears their wave quota")
                    .defineInRange("waveClearBonus", 25, 0, 10000);

            builder.pop();
        }

        public int getWaveIntervalTicks() {
            return waveIntervalSeconds.get() * 20;
        }
    }
}