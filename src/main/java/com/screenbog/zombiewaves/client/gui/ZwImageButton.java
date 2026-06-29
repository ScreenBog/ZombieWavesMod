package com.screenbog.zombiewaves.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Кнопка с hover-эффектом в стиле ZombieWaves GUI.
 */
@OnlyIn(Dist.CLIENT)
public class ZwImageButton extends Button {
    private final boolean accentStyle;

    public ZwImageButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean accentStyle) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accentStyle = accentStyle;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bg = accentStyle ? ZwGuiTheme.ACCENT : ZwGuiTheme.PANEL;
        if (isHovered()) {
            bg = ZwGuiTheme.ACCENT_HOVER;
        }
        if (!active) {
            bg = 0xFF444444;
        }

        graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
        ZwGuiTheme.drawBorder(graphics, getX(), getY(), width, height);

        int textColor = active ? ZwGuiTheme.TEXT : ZwGuiTheme.TEXT_DIM;
        graphics.drawCenteredString(
                net.minecraft.client.Minecraft.getInstance().font,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                textColor
        );
    }
}