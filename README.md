# ZombieWavesMod

Minecraft Forge 1.20.1 mod with automatic zombie waves, per-player coin economy, and a chat-based shop.

## Features

- Zombie waves every 10 minutes (configurable in `config/zombiewaves-server.toml`)
- Scaling difficulty: more zombies, faster movement, occasional baby zombies
- Coins for zombie kills and wave-clear bonus
- Commands: `/coins`, `/coins stats`, `/shop`, `/shop buy <id>`
- Per-player progress in multiplayer
- Integration hooks for GeckoLib, AI-Improvements, Zombies Break & Build, Marbled's Arsenal

## Build

```bash
./gradlew build
```

The compiled jar is in `build/libs/`.

## Run client

```bash
./gradlew runClient
```

## Configuration

Server config path: `config/zombiewaves-server.toml`

Key options:
- `waveIntervalSeconds` — time between waves (default 600)
- `baseZombieCount` / `zombiesPerWave` — spawn scaling
- `coinPerKill` / `waveClearBonus` — economy tuning