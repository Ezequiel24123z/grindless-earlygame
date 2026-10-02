package io.github.ezequiel24123z.grindless.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Pushes an item into the block on one face: vanilla containers first, then any other-mod
 * inventory the loader can see.
 */
public final class ItemInsert {

    private ItemInsert() {
    }

    /**
     * Tries to move {@code stack} through {@code side} of {@code from}.
     *
     * @return what did not fit
     */
    public static ItemStack intoNeighbour(Level level, BlockPos from, Direction side, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        BlockPos pos = from.relative(side);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            return HopperBlockEntity.addItem(null, container, stack, side.getOpposite());
        }
        return ItemPlatform.insert(level, pos, side.getOpposite(), stack);
    }
}
