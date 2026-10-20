package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.MachineMenuLayout;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;

/**
 * One screen for every process machine.
 *
 * <p>Machine I/O, a wrapped status region and player inventory have separate space. Slot
 * backgrounds follow the actual menu slots, including empty machine and player slots.
 */
public final class ProcessMachineScreen extends AbstractContainerScreen<ProcessMachineMenu> {

    private static final ResourceLocation TEXTURE = Grindless.id("textures/gui/process_machine.png");
    private boolean fluidAppearanceUnavailable;

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
        fluidAppearanceUnavailable = false;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }
        MachineMenuLayout.waterTank(menu.kind()).ifPresent(tank -> {
            drawRecessed(graphics, x + tank.x(), y + tank.y(), tank.width(), tank.height());
            menu.primaryFluidView().ifPresent(fluid -> {
                int fill = fluid.fillHeight(tank.height() - 2);
                if (fill > 0 && !fluid.id().map(id -> FluidClientPlatform.draw(graphics, id,
                        x + tank.x() + 1, y + tank.y() + tank.height() - 1 - fill,
                        tank.width() - 2, fill)).orElse(false)) {
                    fluidAppearanceUnavailable = true;
                    graphics.drawString(font, "?", x + tank.x() + 6, y + tank.y() + 13, 0xAA0000, false);
                }
            });
        });
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
        drawRecessed(graphics, x, y, 18, 18);
    }

    private static void drawRecessed(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        var lines = font.split(statusLine(), imageWidth - 16);
        int visible = MachineMenuLayout.visibleStatusLines(lines.size(), font.lineHeight);
        for (int index = 0; index < visible; index++) {
            int y = MachineMenuLayout.STATUS_Y + index * font.lineHeight;
            if (index == visible - 1 && lines.size() > visible) {
                graphics.drawString(font, Component.translatable("gui.grindless.status_more"),
                        MachineMenuLayout.STATUS_X, y, 0x404040, false);
            } else {
                graphics.drawString(font, lines.get(index), MachineMenuLayout.STATUS_X, y, 0x404040, false);
            }
        }
        menu.primaryFluidView().ifPresent(fluid -> graphics.drawString(font,
                Component.translatable("gui.grindless.fluid_amount", fluid.amount(), fluid.capacity()),
                32, 16, 0x404040, false));
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
        MachineMenuLayout.waterTank(menu.kind()).ifPresent(tank -> {
            if (mouseX >= leftPos + tank.x() && mouseX < leftPos + tank.x() + tank.width()
                    && mouseY >= topPos + tank.y() && mouseY < topPos + tank.y() + tank.height()) {
                menu.primaryFluidView().ifPresent(fluid -> {
                    var tooltip = new ArrayList<Component>();
                    tooltip.add(fluid.id().map(FluidClientPlatform::name).orElseGet(() ->
                            Component.translatable(fluid.amount() == 0
                                    ? "gui.grindless.fluid_empty" : "gui.grindless.fluid_unknown")));
                    tooltip.add(Component.translatable("gui.grindless.fluid_amount",
                            fluid.amount(), fluid.capacity()));
                    if (menu.kind() == MachineMenuKind.SETTLING_TROUGH) {
                        tooltip.add(Component.translatable("gui.grindless.fluid_output_quota", fluid.outputQuota()));
                    }
                    if (fluid.amount() > 0 && fluid.id().isEmpty()) {
                        tooltip.add(Component.translatable("gui.grindless.fluid_unknown_details")
                                .withStyle(ChatFormatting.RED));
                    } else if (fluidAppearanceUnavailable) {
                        fluid.id().ifPresent(id -> tooltip.add(
                                Component.translatable("gui.grindless.fluid_unavailable", id)
                                        .withStyle(ChatFormatting.RED)));
                    }
                    if (fluid.amount() > 0) {
                        ResourceGuidanceClient.append(tooltip, fluid.id()
                                .map(id -> new Resource(Kind.FLUID, id)).orElse(null), true, hasShiftDown());
                    }
                    ResourceGuidanceClient.renderTooltip(graphics, font, tooltip, mouseX, mouseY);
                });
            }
        });
    }
}
