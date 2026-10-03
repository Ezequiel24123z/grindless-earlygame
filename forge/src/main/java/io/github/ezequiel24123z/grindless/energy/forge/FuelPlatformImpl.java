package io.github.ezequiel24123z.grindless.energy.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

/**
 * Forge implementation of {@code FuelPlatform}. Other mods' furnace fuels count.
 */
public final class FuelPlatformImpl {

    private FuelPlatformImpl() {
    }

    public static int burnTicks(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }
}
