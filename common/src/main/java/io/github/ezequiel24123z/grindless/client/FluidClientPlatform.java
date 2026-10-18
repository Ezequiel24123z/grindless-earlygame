package io.github.ezequiel24123z.grindless.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Loader-owned fluid appearance, without leaking typed Forge fluids into common. */
public final class FluidClientPlatform {

    private FluidClientPlatform() {
    }

    /** Returns false for an unregistered state fluid, never a fabricated water texture. */
    @ExpectPlatform
    public static boolean draw(GuiGraphics graphics, String id, int x, int y, int width, int height) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }

    @ExpectPlatform
    public static Component name(String id) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }
}
