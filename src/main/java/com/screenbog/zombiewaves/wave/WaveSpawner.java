package com.screenbog.zombiewaves.wave;

import com.screenbog.zombiewaves.ZombieWavesMod;
import com.screenbog.zombiewaves.config.ModConfig;
import com.screenbog.zombiewaves.data.PlayerCoinData;
import com.screenbog.zombiewaves.integration.ModIntegrations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Спавнит усиленных зомби вокруг игроков и помечает их как волновых.
 */
public final class WaveSpawner {
    private WaveSpawner() {
    }

    public static int spawnWaveForPlayers(ServerLevel level, int waveNumber, Set<UUID> waveZombieIds) {
        int spawned = 0;
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            int quota = calculateQuota(waveNumber);
            PlayerCoinData.setWaveQuota(player, quota);
            PlayerCoinData.setWaveKills(player, 0);

            int playerSpawned = spawnAroundPlayer(level, player, waveNumber, quota, waveZombieIds);
            spawned += playerSpawned;
        }
        return spawned;
    }

    public static int calculateQuota(int waveNumber) {
        return ModConfig.SERVER.baseZombieCount.get()
                + (waveNumber - 1) * ModConfig.SERVER.zombiesPerWave.get();
    }

    private static int spawnAroundPlayer(ServerLevel level, ServerPlayer player, int waveNumber,
                                         int count, Set<UUID> waveZombieIds) {
        int spawned = 0;
        int radius = ModConfig.SERVER.spawnRadius.get();
        BlockPos origin = player.blockPosition();

        for (int attempt = 0; attempt < count * 6 && spawned < count; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 8.0D + level.random.nextDouble() * (radius - 8.0D);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            int y = findSpawnY(level, x, z, origin.getY());

            if (y < level.getMinBuildHeight()) {
                continue;
            }

            Zombie zombie = createEnhancedZombie(level, new BlockPos(x, y, z), waveNumber);
            if (zombie == null) {
                continue;
            }

            if (level.addFreshEntity(zombie)) {
                waveZombieIds.add(zombie.getUUID());
                WaveZombieMarker.mark(zombie, waveNumber);
                spawned++;
            }
        }

        if (spawned < count) {
            ZombieWavesMod.LOGGER.warn("Spawned only {}/{} zombies for player {}", spawned, count, player.getName().getString());
        }
        return spawned;
    }

    private static int findSpawnY(ServerLevel level, int x, int z, int preferredY) {
        for (int offset = 0; offset < 8; offset++) {
            for (int sign : new int[]{1, -1}) {
                int y = preferredY + offset * sign;
                BlockPos pos = new BlockPos(x, y, z);
                BlockState below = level.getBlockState(pos.below());
                BlockState at = level.getBlockState(pos);
                BlockState above = level.getBlockState(pos.above());
                if (!below.isAir() && at.isAir() && above.isAir()) {
                    return y;
                }
            }
        }
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static Zombie createEnhancedZombie(ServerLevel level, BlockPos pos, int waveNumber) {
        try {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) {
                return null;
            }

            zombie.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);

            if (level.random.nextDouble() < ModConfig.SERVER.babyZombieChance.get()) {
                zombie.setBaby(true);
            }

            applyWaveBuffs(zombie, waveNumber);
            ModIntegrations.applyToZombie(zombie, waveNumber);

            return zombie;
        } catch (Exception e) {
            ZombieWavesMod.LOGGER.error("Failed to create wave zombie at {}", pos, e);
            return null;
        }
    }

    private static void applyWaveBuffs(Zombie zombie, int waveNumber) {
        double speedBonus = 1.0D + (waveNumber - 1) * ModConfig.SERVER.speedBonusPerWave.get();
        AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * speedBonus);
        }

        // Небольшое усиление здоровья на высоких волнах
        if (waveNumber >= 5) {
            AttributeInstance health = zombie.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(health.getBaseValue() + (waveNumber - 4) * 2.0D);
                zombie.setHealth(zombie.getMaxHealth());
            }
        }

        // Экипировка растёт с волной
        if (waveNumber >= 3 && zombie.getRandom().nextFloat() < 0.35F) {
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
        if (waveNumber >= 6 && zombie.getRandom().nextFloat() < 0.25F) {
            zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        }
    }

    public static void broadcastWaveStart(ServerLevel level, int waveNumber, int totalZombies) {
        if (!ModConfig.SERVER.announceWaves.get()) {
            return;
        }
        Component message = Component.translatable("message.zombiewaves.wave_start", waveNumber, totalZombies);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }

    public static void broadcastWaveEnd(ServerLevel level, int waveNumber) {
        if (!ModConfig.SERVER.announceWaves.get()) {
            return;
        }
        Component message = Component.translatable("message.zombiewaves.wave_end", waveNumber);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }

    public static Set<UUID> filterAliveWaveZombies(ServerLevel level, Set<UUID> ids) {
        Set<UUID> alive = new HashSet<>();
        for (UUID id : ids) {
            if (level.getEntity(id) instanceof Zombie zombie && zombie.isAlive()) {
                alive.add(id);
            }
        }
        return alive;
    }

    public static boolean isOverworld(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    public static Vec3 randomOffset(Vec3 center, int radius, net.minecraft.util.RandomSource random) {
        double dx = (random.nextDouble() - 0.5D) * radius * 2.0D;
        double dz = (random.nextDouble() - 0.5D) * radius * 2.0D;
        return center.add(dx, 0.0D, dz);
    }
}