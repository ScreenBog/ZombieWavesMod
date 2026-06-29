package com.screenbog.zombiewaves.client.gui;

import com.screenbog.zombiewaves.common.menu.MenuAction;
import com.screenbog.zombiewaves.common.menu.MenuData;
import com.screenbog.zombiewaves.common.menu.ZwMainMenu;
import com.screenbog.zombiewaves.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный экран ZombieWaves с вкладками: Статистика, Магазин, Волны, Настройки.
 */
@OnlyIn(Dist.CLIENT)
public class ZwMainScreen extends AbstractContainerScreen<ZwMainMenu> {
    public static final int PANEL_WIDTH = 280;
    public static final int PANEL_HEIGHT = 230;

    private static final int TAB_STATS = 0;
    private static final int TAB_SHOP = 1;
    private static final int TAB_WAVES = 2;
    private static final int TAB_SETTINGS = 3;

    private MenuData data;
    private int activeTab;
    private int purchaseFlashTicks;
    private String lastPurchasedItem = "";
    private EditBox intervalBox;
    private final List<ZwImageButton> shopBuyButtons = new ArrayList<>();

    public ZwMainScreen(ZwMainMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.data = menu.getData();
        this.activeTab = menu.getInitialTab();
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        refreshTabWidgets();
    }

    private void refreshTabWidgets() {
        clearWidgets();
        shopBuyButtons.clear();

        int tabY = topPos + 6;
        int tabX = leftPos + 8;
        int tabW = 60;
        int tabH = 18;

        addRenderableWidget(new ZwImageButton(tabX, tabY, tabW, tabH,
                Component.translatable("gui.zombiewaves.tab.stats"),
                b -> switchTab(TAB_STATS), activeTab == TAB_STATS));

        addRenderableWidget(new ZwImageButton(tabX + tabW + 4, tabY, tabW, tabH,
                Component.translatable("gui.zombiewaves.tab.shop"),
                b -> switchTab(TAB_SHOP), activeTab == TAB_SHOP));

        if (data.isOp) {
            addRenderableWidget(new ZwImageButton(tabX + (tabW + 4) * 2, tabY, tabW, tabH,
                    Component.translatable("gui.zombiewaves.tab.waves"),
                    b -> switchTab(TAB_WAVES), activeTab == TAB_WAVES));

            addRenderableWidget(new ZwImageButton(tabX + (tabW + 4) * 3, tabY, tabW, tabH,
                    Component.translatable("gui.zombiewaves.tab.settings"),
                    b -> switchTab(TAB_SETTINGS), activeTab == TAB_SETTINGS));
        }

        if (activeTab == TAB_SHOP) {
            initShopButtons();
        } else if (activeTab == TAB_WAVES && data.isOp) {
            initWaveButtons();
        } else if (activeTab == TAB_SETTINGS && data.isOp) {
            initSettingsWidgets();
        }
    }

    private void switchTab(int tab) {
        activeTab = tab;
        refreshTabWidgets();
    }

    private void initShopButtons() {
        int startX = leftPos + 16;
        int startY = topPos + 58;
        int cellW = 80;
        int cellH = 52;
        int cols = 3;

        for (int i = 0; i < data.shopEntries.size(); i++) {
            MenuData.ShopEntry entry = data.shopEntries.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * cellW;
            int y = startY + row * cellH;

            ZwImageButton buyBtn = new ZwImageButton(
                    x + 8, y + 34, 64, 14,
                    Component.translatable("gui.zombiewaves.buy"),
                    b -> ModNetwork.sendMenuAction(MenuAction.BUY, entry.id, 0),
                    true
            );
            shopBuyButtons.add(buyBtn);
            addRenderableWidget(buyBtn);
        }
    }

    private void initWaveButtons() {
        int bx = leftPos + 20;
        int by = topPos + 80;
        int bw = 115;
        int bh = 20;
        int gap = 6;

        addRenderableWidget(new ZwImageButton(bx, by, bw, bh,
                Component.translatable("gui.zombiewaves.start_wave"),
                b -> ModNetwork.sendMenuAction(MenuAction.START_WAVE, "", 0), true));

        addRenderableWidget(new ZwImageButton(bx, by + bh + gap, bw, bh,
                Component.translatable("gui.zombiewaves.skip_wave"),
                b -> ModNetwork.sendMenuAction(MenuAction.SKIP_WAVE, "", 0), true));

        addRenderableWidget(new ZwImageButton(bx, by + (bh + gap) * 2, bw, bh,
                Component.translatable("gui.zombiewaves.end_wave"),
                b -> ModNetwork.sendMenuAction(MenuAction.END_WAVE, "", 0), true));
    }

    private void initSettingsWidgets() {
        intervalBox = new EditBox(font, leftPos + 20, topPos + 90, 80, 18,
                Component.translatable("gui.zombiewaves.interval"));
        intervalBox.setValue(String.valueOf(data.waveIntervalSeconds));
        intervalBox.setMaxLength(5);
        intervalBox.setTextColor(ZwGuiTheme.TEXT);
        intervalBox.setBordered(true);
        addRenderableWidget(intervalBox);

        addRenderableWidget(new ZwImageButton(leftPos + 110, topPos + 88, 80, 20,
                Component.translatable("gui.zombiewaves.apply"),
                b -> applyInterval(), true));
    }

    private void applyInterval() {
        try {
            int seconds = Integer.parseInt(intervalBox.getValue().trim());
            ModNetwork.sendMenuAction(MenuAction.SET_INTERVAL, "", seconds);
        } catch (NumberFormatException ignored) {
            // Сервер вернёт сообщение об ошибке
            ModNetwork.sendMenuAction(MenuAction.SET_INTERVAL, "", -1);
        }
    }

    public void updateData(MenuData newData) {
        this.data = newData;
        if (intervalBox != null) {
            intervalBox.setValue(String.valueOf(data.waveIntervalSeconds));
        }
        refreshTabWidgets();
    }

    public void refreshCoins(int coins) {
        this.data.coins = coins;
    }

    public void triggerPurchaseAnimation(String itemId) {
        this.purchaseFlashTicks = 20;
        this.lastPurchasedItem = itemId;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ZwGuiTheme.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        ZwGuiTheme.drawAccentLine(graphics, leftPos + 6, topPos + 28, imageWidth - 12);

        if (purchaseFlashTicks > 0) {
            purchaseFlashTicks--;
            graphics.fill(leftPos + 4, topPos + 30, leftPos + imageWidth - 4, topPos + imageHeight - 4, ZwGuiTheme.SUCCESS_FLASH);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Заголовок и баланс рисуем вручную
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 8, ZwGuiTheme.ACCENT);
        graphics.drawString(font,
                Component.translatable("gui.zombiewaves.balance", data.coins),
                leftPos + 10, topPos + 34, ZwGuiTheme.COIN_GOLD);

        int contentY = topPos + 50;
        switch (activeTab) {
            case TAB_STATS -> renderStatsTab(graphics, contentY);
            case TAB_SHOP -> renderShopTab(graphics, mouseX, mouseY, contentY);
            case TAB_WAVES -> renderWavesTab(graphics, contentY);
            case TAB_SETTINGS -> renderSettingsTab(graphics, contentY);
        }

        if (purchaseFlashTicks > 0 && !lastPurchasedItem.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("gui.zombiewaves.purchased", lastPurchasedItem),
                    leftPos + imageWidth / 2, topPos + imageHeight - 16, ZwGuiTheme.TEXT);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderStatsTab(GuiGraphics graphics, int y) {
        int x = leftPos + 14;
        int line = 0;
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.wave", data.currentWave);
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.state", data.waveState);
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.timer", data.getSecondsUntilNextWave());
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.kills", data.waveKills, data.waveQuota);
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.total_kills", data.totalKills);
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.waves_cleared", data.wavesCleared);
        drawLine(graphics, x, y + line++ * 14, "gui.zombiewaves.stats.coins", data.coins);
    }

    private void renderShopTab(GuiGraphics graphics, int mouseX, int mouseY, int contentY) {
        int startX = leftPos + 16;
        int startY = contentY + 8;
        int cellW = 80;
        int cellH = 52;
        int cols = 3;

        for (int i = 0; i < data.shopEntries.size(); i++) {
            MenuData.ShopEntry entry = data.shopEntries.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * cellW;
            int y = startY + row * cellH;

            graphics.fill(x, y, x + cellW - 4, y + cellH - 4, ZwGuiTheme.PANEL);
            ZwGuiTheme.drawBorder(graphics, x, y, cellW - 4, cellH - 4);

            ItemStack stack = entry.createPreviewStack();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 28, y + 4);
                graphics.renderItemDecorations(font, stack, x + 28, y + 4);
            }

            graphics.drawCenteredString(font,
                    Component.translatable("gui.zombiewaves.price", entry.price),
                    x + (cellW - 4) / 2, y + 24, ZwGuiTheme.COIN_GOLD);

            if (mouseX >= x && mouseX < x + cellW - 4 && mouseY >= y && mouseY < y + cellH - 4) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }

    private void renderWavesTab(GuiGraphics graphics, int y) {
        int x = leftPos + 150;
        drawLine(graphics, x, y, "gui.zombiewaves.stats.wave", data.currentWave);
        drawLine(graphics, x, y + 14, "gui.zombiewaves.stats.state", data.waveState);
        drawLine(graphics, x, y + 28, "gui.zombiewaves.stats.timer", data.getSecondsUntilNextWave());
        drawLine(graphics, x, y + 42, "gui.zombiewaves.interval_current", data.waveIntervalSeconds);
    }

    private void renderSettingsTab(GuiGraphics graphics, int y) {
        graphics.drawString(font,
                Component.translatable("gui.zombiewaves.interval_hint"),
                leftPos + 20, y, ZwGuiTheme.TEXT_DIM);
        graphics.drawString(font,
                Component.translatable("gui.zombiewaves.interval_range"),
                leftPos + 20, y + 60, ZwGuiTheme.TEXT_DIM);
    }

    private void drawLine(GuiGraphics graphics, int x, int y, String key, Object... args) {
        graphics.drawString(font, Component.translatable(key, args), x, y, ZwGuiTheme.TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}