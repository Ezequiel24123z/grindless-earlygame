package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.menu.FieldGuideMenu;
import io.github.ezequiel24123z.grindless.quest.GuideCatalogue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * One page of the route at a time, drawn with fills (ADR-0100).
 *
 * <p>Turning a page stays on the client. The guide has nothing to claim.
 */
public final class FieldGuideScreen extends AbstractContainerScreen<FieldGuideMenu> {

    private static final int PANEL = 0xFF141820;
    private static final int PANEL_INNER = 0xFF1C2830;
    private static final int HEADER = 0xFF18FFFF;
    private static final int BODY = 0xFFECEFF1;
    private static final int TEXT_TOP = 32;
    private static final int TEXT_BOTTOM = 168;

    private int page;

    public FieldGuideScreen(FieldGuideMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.grindless.guide.prev"),
                button -> turn(-1)).bounds(leftPos + 8, topPos + imageHeight - 22, 72, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.grindless.guide.next"),
                button -> turn(1)).bounds(leftPos + imageWidth - 80, topPos + imageHeight - 22, 72, 16).build());
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
        List<String> pages = GuideCatalogue.pages();
        if (pages.isEmpty()) {
            return;
        }
        page = Math.max(0, Math.min(pages.size() - 1, page));
        String id = pages.get(page);
        graphics.drawString(font, title, 8, 6, HEADER, false);
        graphics.drawString(font, Component.translatable(GuideCatalogue.titleKey(id)), 8, 18, HEADER, false);
        List<FormattedCharSequence> lines = font.split(
                Component.translatable(GuideCatalogue.bodyKey(id)), imageWidth - 16);
        int y = TEXT_TOP;
        for (FormattedCharSequence line : lines) {
            if (y + font.lineHeight > TEXT_BOTTOM) {
                break;
            }
            graphics.drawString(font, line, 8, y, BODY, false);
            y += font.lineHeight;
        }
        graphics.drawString(font, Component.translatable("gui.grindless.guide.page", page + 1, pages.size()),
                imageWidth / 2 - 12, imageHeight - 18, HEADER, false);
    }

    private void turn(int delta) {
        int size = GuideCatalogue.pages().size();
        if (size == 0) {
            return;
        }
        page = Math.max(0, Math.min(size - 1, page + delta));
    }
}
