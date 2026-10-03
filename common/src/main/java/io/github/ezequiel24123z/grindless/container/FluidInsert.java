package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Pushes a fluid through one face into a neighbour that implements {@link FluidEndpoint}.
 */
public final class FluidInsert {

    private FluidInsert() {
    }

    public static FluidState intoNeighbour(Level level, BlockPos from, Direction side, FluidState state) {
        if (state == null || state.isEmpty()) {
            return FluidState.EMPTY;
        }
        BlockEntity blockEntity = level.getBlockEntity(from.relative(side));
        if (blockEntity instanceof FluidEndpoint endpoint) {
            return endpoint.insert(side.getOpposite(), state);
        }
        return state;
    }
}
