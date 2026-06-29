package com.screenbog.zombiewaves.wave;

/**
 * Состояние глобальной волны на сервере.
 */
public enum WaveState {
    /** Ожидание следующей волны. */
    IDLE,
    /** Волна активна, зомби спавнятся. */
    ACTIVE,
    /** Волна завершена, ожидание следующего интервала. */
    COOLDOWN
}