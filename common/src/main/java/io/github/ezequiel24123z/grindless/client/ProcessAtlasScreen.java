package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.menu.ProcessAtlasMenu;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Scrollable list of live process recipes (ADR-0066).
 *
 * <p>Drawn with fills, not a GUI texture: the stub is the lookup, not the chrome.
 */
public final class ProcessAtlasScreen extends AbstractContainerScreen<ProcessAtlasMenu> {

    private static final int PANEL = 0xFF1A1A22;
    private static final int PANEL_INNER = 0xFF252532;
    private static final int HEADER = 0xFF18FFFF;
    private static final int ROW = 0xFFECEFF1;
    private static final int SELECTED = 0xFF80CBC4;
    private static final int MUTED = 0xFF90A4AE;
    private static final int ROW_HEIGHT = 10;
    private static final int LIST_TOP = 18;
    private static final int DETAIL_HEIGHT = 48;

    private List<AtlasLogic.Entry> rows = List.of();
    private int scroll;
    private int selected = -1;

    public ProcessAtlasScreen(ProcessAtlasMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        rows = AtlasLogic.allEntries(ProcessLookup.recipes());
        scroll = 0;
        selected = rows.isEmpty() ? -1 : 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL_INNER);
        int detailTop = y + imageHeight - DETAIL_HEIGHT;
        graphics.fill(x + 4, detailTop, x + imageWidth - 4, y + imageHeight - 4, 0xFF1A1A22);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, HEADER, false);
        if (rows.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.grindless.atlas.empty"),
                    8, LIST_TOP, MUTED, false);
            return;
        }
        int visible = visibleRows();
        int maxScroll = Math.max(0, rows.size() - visible);
        scroll = Math.min(scroll, maxScroll);
        int listBottom = imageHeight - DETAIL_HEIGHT - 4;
        for (int i = 0; i < visible; i++) {
            int index = scroll + i;
            if (index >= rows.size()) {
                break;
            }
            int rowY = LIST_TOP + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT > listBottom) {
                break;
            }
            AtlasLogic.Entry row = rows.get(index);
            boolean highlight = index == selected;
            String text = font.plainSubstrByWidth(AtlasLogic.line(row), imageWidth - 16);
            graphics.drawString(font, text, 8, rowY, highlight ? SELECTED : ROW, false);
        }
        drawDetail(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, rows.size() - visibleRows());
        scroll = (int) Math.max(0, Math.min(max, scroll - Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !rows.isEmpty()) {
            int relY = (int) mouseY - topPos;
            int relX = (int) mouseX - leftPos;
            int listBottom = imageHeight - DETAIL_HEIGHT - 4;
            if (relX >= 8 && relX < imageWidth - 8 && relY >= LIST_TOP && relY < listBottom) {
                int index = scroll + (relY - LIST_TOP) / ROW_HEIGHT;
                if (index >= 0 && index < rows.size()) {
                    selected = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawDetail(GuiGraphics graphics) {
        if (selected < 0 || selected >= rows.size()) {
            return;
        }
        AtlasLogic.Entry row = rows.get(selected);
        int y = imageHeight - DETAIL_HEIGHT + 6;
        graphics.drawString(font, row.station() + "  " + row.id(), 8, y, HEADER, false);
        graphics.drawString(font, font.plainSubstrByWidth(
                "in  " + AtlasLogic.describe(row.inputs()), imageWidth - 16), 8, y + 10, ROW, false);
        if (!row.catalysts().isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(
                    "cat " + AtlasLogic.describe(row.catalysts()), imageWidth - 16), 8, y + 20, MUTED, false);
            graphics.drawString(font, font.plainSubstrByWidth(
                    "out " + AtlasLogic.describe(row.outputs()), imageWidth - 16), 8, y + 30, ROW, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(
                    "out " + AtlasLogic.describe(row.outputs()), imageWidth - 16), 8, y + 20, ROW, false);
        }
    }

    private int visibleRows() {
        int listHeight = imageHeight - DETAIL_HEIGHT - LIST_TOP - 4;
        return Math.max(1, listHeight / ROW_HEIGHT);
    }
}
