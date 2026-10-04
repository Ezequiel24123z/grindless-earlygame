package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.belt.BeltEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Pulls one stack out of the block on one face: belts first, then vanilla containers.
 */
public final class ItemExtract {

    private ItemExtract() {
    }

    public static ItemStack fromNeighbour(Level level, BlockPos from, Direction side, int max) {
        if (max <= 0) {
            return ItemStack.EMPTY;
        }
        BlockPos pos = from.relative(side);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BeltEndpoint belt) {
            return belt.extract(side.getOpposite(), max);
        }
        if (blockEntity instanceof WorldlyContainer worldly) {
            Direction incoming = side.getOpposite();
            for (int slot : worldly.getSlotsForFace(incoming)) {
                ItemStack stack = worldly.getItem(slot);
                if (stack.isEmpty() || !worldly.canTakeItemThroughFace(slot, stack, incoming)) {
                    continue;
                }
                return worldly.removeItem(slot, max);
            }
            return ItemStack.EMPTY;
        }
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !container.canTakeItem(container, slot, stack)) {
                    continue;
                }
                return container.removeItem(slot, max);
            }
        }
        return ItemStack.EMPTY;
    }

    /** A copy of the item a pull would take, or empty when the neighbour cannot say. */
    public static ItemStack preview(Level level, BlockPos from, Direction side) {
        BlockPos pos = from.relative(side);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BeltEndpoint belt) {
            return belt.preview(side.getOpposite());
        }
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    return copy;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasExtractable(Level level, BlockPos from, Direction side) {
        BlockPos pos = from.relative(side);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BeltEndpoint belt) {
            return belt.canExtract(side.getOpposite());
        }
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
