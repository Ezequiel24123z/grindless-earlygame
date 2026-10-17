package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * One screen for every process machine.
 *
 * <p>Energy on the left, progress in the middle, named status on the title line. Slots are
 * placed by the menu.
 */
public final class ProcessMachineScreen extends AbstractContainerScreen<ProcessMachineMenu> {

    private static final ResourceLocation TEXTURE = Grindless.id("textures/gui/process_machine.png");

    public ProcessMachineScreen(ProcessMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
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
            graphics.blit(TEXTURE, x + 79, y + 35, 176, 0, arrow, 17);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        Component line = statusLine();
        graphics.drawString(font, font.plainSubstrByWidth(line.getString(), imageWidth - 40),
                32, 70, 0x404040, false);
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
        if (mouseX >= leftPos + 32 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + 69 && mouseY < topPos + 80) {
            graphics.renderTooltip(font, statusLine(), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 20
                && mouseY >= topPos + 16 && mouseY < topPos + 70) {
            graphics.renderTooltip(font, Component.translatable("gui.grindless.energy",
                    menu.energy(), menu.capacity()), mouseX, mouseY);
        }
    }
}
