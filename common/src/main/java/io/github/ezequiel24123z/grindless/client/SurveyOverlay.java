package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.vein.ClientSurvey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

/**
 * While the scanner is held, the standing chunk's last survey is drawn on the HUD.
 */
public final class SurveyOverlay {

    private SurveyOverlay() {
    }

    public static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        if (!player.getMainHandItem().is(ModItems.PROSPECTORS_SCANNER.get())
                && !player.getOffhandItem().is(ModItems.PROSPECTORS_SCANNER.get())) {
            return;
        }
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        ClientSurvey.Entry entry = ClientSurvey.at(chunk.x, chunk.z);
        Component line;
        if (entry == null || entry.material().isEmpty()) {
            line = Component.translatable("overlay.grindless.scanner.none");
        } else {
            line = Component.translatable("overlay.grindless.scanner.vein",
                    Component.translatable("material.grindless." + entry.material()),
                    String.format(java.util.Locale.ROOT, "%.2f", entry.richness()));
        }
        int width = minecraft.font.width(line);
        graphics.drawString(minecraft.font, line,
                (graphics.guiWidth() - width) / 2, graphics.guiHeight() - 56, 0xFFE8F4FF, true);
    }
}
