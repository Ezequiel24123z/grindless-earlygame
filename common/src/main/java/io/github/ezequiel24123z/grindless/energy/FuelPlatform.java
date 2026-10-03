package io.github.ezequiel24123z.grindless.energy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.ItemStack;

/**
 * Furnace-fuel burn times, behind {@code @ExpectPlatform} because vanilla keeps the map
 * private and Forge is the one that other mods register against.
 */
public final class FuelPlatform {

    private FuelPlatform() {
    }

    /**
     * Ticks {@code stack} would burn in a furnace, or {@code 0} if it is not fuel.
     */
    @ExpectPlatform
    public static int burnTicks(ItemStack stack) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }
}
