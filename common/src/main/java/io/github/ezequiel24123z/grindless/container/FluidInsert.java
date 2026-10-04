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

    /** Pulls {@code millibuckets} of {@code id} from neighbouring endpoints. Returns how much was taken. */
    public static int takeFromNeighbours(Level level, BlockPos from, String id, int millibuckets) {
        if (level == null || id == null || id.isBlank() || millibuckets <= 0) {
            return 0;
        }
        int remaining = millibuckets;
        for (Direction side : Direction.values()) {
            if (remaining <= 0) {
                break;
            }
            BlockEntity blockEntity = level.getBlockEntity(from.relative(side));
            if (!(blockEntity instanceof FluidEndpoint endpoint)
                    || !endpoint.canExtract(side.getOpposite())
                    || !endpoint.contents().is(id)) {
                continue;
            }
            FluidState taken = endpoint.extract(side.getOpposite(), remaining);
            remaining -= taken.millibuckets();
        }
        return millibuckets - remaining;
    }
}
