package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
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
        MachineStatus[] statuses = MachineStatus.values();
        int ordinal = menu.statusOrdinal();
        MachineStatus status = ordinal >= 0 && ordinal < statuses.length ? statuses[ordinal] : MachineStatus.IDLE;
        Component line = Component.translatable("gui.grindless.status." + status.getSerializedName());
        if (menu.host() != null && menu.host().menuFault() != null
                && status == MachineStatus.OUT_OF_BAND) {
            line = menu.host().menuFault();
        }
        graphics.drawString(font, line, 32, 70, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 20
                && mouseY >= topPos + 16 && mouseY < topPos + 70) {
            graphics.renderTooltip(font, Component.translatable("gui.grindless.energy",
                    menu.energy(), menu.capacity()), mouseX, mouseY);
        }
    }
}
