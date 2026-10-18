package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.MachineMenuLayout;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * One screen for every process machine.
 *
 * <p>Machine I/O, a wrapped status region and player inventory have separate space. Slot
 * backgrounds follow the actual menu slots, including empty machine and player slots.
 */
public final class ProcessMachineScreen extends AbstractContainerScreen<ProcessMachineMenu> {

    private static final ResourceLocation TEXTURE = Grindless.id("textures/gui/process_machine.png");

    public ProcessMachineScreen(ProcessMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MachineMenuLayout.WIDTH;
        this.imageHeight = MachineMenuLayout.HEIGHT;
        this.inventoryLabelY = MachineMenuLayout.INVENTORY_LABEL_Y;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }
        graphics.fill(x + 8, y + 16, x + 20, y + 70, 0xFF373737);
        graphics.fill(x + 9, y + 17, x + 19, y + 69, 0xFF8B8B8B);
        int arrowX = MachineMenuLayout.progressX(menu.kind());
        graphics.fill(x + arrowX, y + 35, x + arrowX + 24, y + 52, 0xFF8B8B8B);
        int energy = menu.energy();
        int capacity = menu.capacity();
        int energyHeight = (int) (50L * energy / capacity);
        if (energyHeight > 0) {
            graphics.fill(x + 10, y + 18 + (50 - energyHeight), x + 18, y + 68, 0xFF00E5FF);
        }
        int progress = menu.progress();
        int cycle = menu.cycle();
        int arrow = (int) (24L * progress / cycle);
        if (arrow > 0) {
            graphics.blit(TEXTURE, x + arrowX, y + 35, 176, 0, arrow, 17);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        int statusY = MachineMenuLayout.STATUS_Y;
        for (var line : font.split(statusLine(), imageWidth - 16)) {
            graphics.drawString(font, line, MachineMenuLayout.STATUS_X, statusY, 0x404040, false);
            statusY += font.lineHeight;
        }
        if (menu.kind() == MachineMenuKind.CLAY_MIXER || menu.kind() == MachineMenuKind.SETTLING_TROUGH
                || menu.kind() == MachineMenuKind.GEOLOGICAL_COLLECTOR || menu.kind() == MachineMenuKind.BIOCULTIVATOR) {
            Component water = menu.fluidOutputAmount() > 0
                    ? Component.translatable("gui.grindless.workshop.water_output",
                            menu.fluidAmount(), menu.fluidOutputAmount())
                    : Component.translatable("gui.grindless.workshop.water", menu.fluidAmount());
            graphics.drawString(font, water, 32, 16, 0x404040, false);
        }
    }

    private Component statusLine() {
        if (menu.siteFault() != SourceSite.Fault.NONE) {
            return Component.translatable(menu.siteFault().translationKey());
        }
        if (menu.structureFault() != WorkshopStructure.Fault.NONE) {
            return Component.translatable(menu.structureFault().translationKey());
        }
        MachineStatus[] statuses = MachineStatus.values();
        int ordinal = menu.statusOrdinal();
        MachineStatus status = ordinal >= 0 && ordinal < statuses.length ? statuses[ordinal] : MachineStatus.IDLE;
        Component line = Component.translatable("gui.grindless.status." + status.getSerializedName());
        if (menu.host() != null && menu.host().menuFault() != null
                && status == MachineStatus.OUT_OF_BAND) {
            line = menu.host().menuFault();
        }
        return line;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + MachineMenuLayout.STATUS_X && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + MachineMenuLayout.STATUS_Y
                && mouseY < topPos + MachineMenuLayout.INVENTORY_LABEL_Y - 4) {
            graphics.renderTooltip(font, statusLine(), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 20
                && mouseY >= topPos + 16 && mouseY < topPos + 70) {
            graphics.renderTooltip(font, Component.translatable("gui.grindless.energy",
                    menu.energy(), menu.capacity()), mouseX, mouseY);
        }
    }
}
