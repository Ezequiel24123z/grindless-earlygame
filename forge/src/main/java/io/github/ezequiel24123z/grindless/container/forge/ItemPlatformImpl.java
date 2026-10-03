package io.github.ezequiel24123z.grindless.container.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Optional;

/**
 * Forge implementation of {@code ItemPlatform}, resolved by Architectury's {@code @ExpectPlatform}.
 */
public final class ItemPlatformImpl {

    private ItemPlatformImpl() {
    }

    public static ItemStack insert(Level level, BlockPos pos, Direction side, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return stack;
        }
        Optional<IItemHandler> handler =
                blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve();
        return handler.map(inventory -> ItemHandlerHelper.insertItemStacked(inventory, stack, false))
                .orElse(stack);
    }
}
