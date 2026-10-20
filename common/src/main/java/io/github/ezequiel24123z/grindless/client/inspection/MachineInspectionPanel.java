package io.github.ezequiel24123z.grindless.client.inspection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** A small native-font panel; long details are paged rather than drawn beyond the viewport. */
public final class MachineInspectionPanel {

    public static final int MAX_WRAPPED_LINES = 256;
    private static final int MARGIN = 8;
    private static final int PADDING = 6;

    private MachineInspectionPanel() {
    }

    public static void render(GuiGraphics graphics, Component title, List<Component> text,
                              int requestedPage, Component pageKey) {
        Font font = Minecraft.getInstance().font;
        int width = Math.max(40, Math.min(280, graphics.guiWidth() - 2 * (MARGIN + PADDING)));
        List<FormattedCharSequence> lines = new ArrayList<>();
        boolean bounded = false;
        for (Component line : text) {
            for (FormattedCharSequence wrapped : font.split(line, width)) {
                if (lines.size() == MAX_WRAPPED_LINES - 1) {
                    bounded = true;
                    break;
                }
                lines.add(wrapped);
            }
            if (bounded) {
                break;
            }
        }
        if (bounded) {
            lines.add(Component.translatable("inspection.grindless.bounded").getVisualOrderText());
        }
        if (lines.isEmpty()) {
            return;
        }
        int available = Math.max(1, (graphics.guiHeight() - 2 * (MARGIN + PADDING) - 36) / font.lineHeight - 3);
        int pages = (lines.size() + available - 1) / available;
        int page = Math.floorMod(requestedPage, pages);
        int start = page * available;
        int count = Math.min(available, lines.size() - start);
        List<FormattedCharSequence> heading = font.split(title, width);
        FormattedCharSequence header = heading.isEmpty() ? Component.literal("[Native]").getVisualOrderText()
                : heading.get(0);
        int height = PADDING * 2 + (count + 2) * font.lineHeight + 4;
        int left = graphics.guiWidth() - width - 2 * PADDING - MARGIN;
        int top = MARGIN;
        graphics.fill(left, top, left + width + 2 * PADDING, top + height, 0xE0101010);
        graphics.fill(left, top, left + width + 2 * PADDING, top + 1, 0xFFC6C6C6);
        graphics.fill(left, top + height - 1, left + width + 2 * PADDING, top + height, 0xFFC6C6C6);
        graphics.fill(left, top, left + 1, top + height, 0xFFC6C6C6);
        graphics.fill(left + width + 2 * PADDING - 1, top, left + width + 2 * PADDING, top + height, 0xFFC6C6C6);
        int x = left + PADDING;
        int y = top + PADDING;
        graphics.drawString(font, header, x, y, 0xFFFFFFFF, false);
        y += font.lineHeight + 3;
        for (int index = 0; index < count; index++) {
            graphics.drawString(font, lines.get(start + index), x, y, 0xFFF0F0F0, false);
            y += font.lineHeight;
        }
        Component footer = pages > 1
                ? Component.translatable("inspection.grindless.page", page + 1, pages, pageKey)
                : Component.translatable("inspection.grindless.native_footer", pageKey);
        graphics.drawString(font, font.plainSubstrByWidth(footer.getString(), width),
                x, y + 1, 0xFFD0D0D0, false);
    }
}
