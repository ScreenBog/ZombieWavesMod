package com.screenbog.zombiewaves.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Цветовая схема GUI — тёмный фон с красными акцентами (зомби-тематика).
 */
@OnlyIn(Dist.CLIENT)
public final class ZwGuiTheme {
    public static final int BACKGROUND = 0xF0180808;
    public static final int PANEL = 0xF0281010;
    public static final int BORDER = 0xFF8B1A1A;
    public static final int ACCENT = 0xFFCC2222;
    public static final int ACCENT_HOVER = 0xFFE83333;
    public static final int TEXT = 0xFFE8E8E8;
    public static final int TEXT_DIM = 0xFFAAAAAA;
    public static final int SUCCESS_FLASH = 0x8844FF44;
    public static final int COIN_GOLD = 0xFFFFD700;

    private ZwGuiTheme() {
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL);
        drawBorder(graphics, x, y, width, height);
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 2, BORDER);
        graphics.fill(x, y + height - 2, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 2, y + height, BORDER);
        graphics.fill(x + width - 2, y, x + width, y + height, BORDER);
    }

    public static void drawAccentLine(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, ACCENT);
    }
}