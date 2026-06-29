package com.screenbog.zombiewaves.common.menu;

/**
 * Действия GUI, отправляемые клиентом на сервер.
 */
public enum MenuAction {
    BUY,
    START_WAVE,
    SKIP_WAVE,
    END_WAVE,
    SET_INTERVAL;

    public static MenuAction fromId(int id) {
        MenuAction[] values = values();
        if (id < 0 || id >= values.length) {
            return null;
        }
        return values[id];
    }
}