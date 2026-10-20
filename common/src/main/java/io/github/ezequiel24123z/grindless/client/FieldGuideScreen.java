package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.menu.FieldGuideMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compatibility screen for the retained menu identity, not a second gameplay manual. */
public final class FieldGuideScreen extends AbstractContainerScreen<FieldGuideMenu> {

    private static final int PANEL = 0xFF141820;
    private static final int PANEL_INNER = 0xFF1C2830;
    private static final int HEADER = 0xFF18FFFF;
    private static final int BODY = 0xFFECEFF1;

    public FieldGuideScreen(FieldGuideMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.open"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                }).bounds(leftPos + 8, topPos + imageHeight - 24, imageWidth - 16, 18).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, PANEL_INNER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.grindless.quest.guide_retired"), 8, 8, HEADER, false);
        int y = 32;
        for (var line : font.split(Component.translatable("gui.grindless.quest.guide_migration"), imageWidth - 16)) {
            graphics.drawString(font, line, 8, y, BODY, false);
            y += font.lineHeight;
        }
    }
}
