package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.flight.RocketFlight;
import io.github.ezequiel24123z.grindless.menu.LandingMapMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Two buttons and a title. The map is the choice of a body, drawn with fills (ADR-0097).
 */
public final class LandingMapScreen extends AbstractContainerScreen<LandingMapMenu> {

    private static final int PANEL = 0xFF101820;
    private static final int FRAME = 0xFF1A3A4A;

    public LandingMapScreen(LandingMapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 86;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        List<String> sites = RocketFlight.sites();
        for (int i = 0; i < sites.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(
                    Component.translatable(RocketFlight.siteKey(sites.get(i))),
                    button -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                        }
                    }).bounds(leftPos + 8, topPos + 28 + i * 24, 160, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, FRAME);
    }
}
