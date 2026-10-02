package io.github.ezequiel24123z.grindless.container;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Inserts an item into a neighbouring inventory that is not a vanilla {@code Container}.
 *
 * <p>Chests and hoppers are {@code Container}s and are handled in common. Other mods expose a
 * capability instead, and that is loader-specific, so it lives behind this seam the same way
 * energy does.
 */
public final class ItemPlatform {

    private ItemPlatform() {
    }

    /**
     * Inserts {@code stack} into the inventory at {@code pos} from {@code side}.
     *
     * @return what did not fit, possibly empty, never {@code null}
     */
    @ExpectPlatform
    public static ItemStack insert(Level level, BlockPos pos, Direction side, ItemStack stack) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }
}
