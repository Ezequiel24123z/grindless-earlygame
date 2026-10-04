package io.github.ezequiel24123z.grindless.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Pulls fluid from the one neighbour on {@code side}. Pipes push; pumps and arms pull. */
public final class FluidPull {

    private FluidPull() {
    }

    public static boolean hasExtractable(Level level, BlockPos from, Direction side) {
        return accepts(endpoint(level, from, side), side);
    }

    public static FluidState fromNeighbour(Level level, BlockPos from, Direction side, int millibuckets) {
        FluidEndpoint endpoint = endpoint(level, from, side);
        if (!accepts(endpoint, side) || millibuckets <= 0) {
            return FluidState.EMPTY;
        }
        return endpoint.extract(side.getOpposite(), millibuckets);
    }

    private static boolean accepts(FluidEndpoint endpoint, Direction side) {
        return endpoint != null
                && endpoint.canExtract(side.getOpposite())
                && PressureLogic.accepts(endpoint.contents());
    }

    private static FluidEndpoint endpoint(Level level, BlockPos from, Direction side) {
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(from.relative(side));
        return blockEntity instanceof FluidEndpoint endpoint ? endpoint : null;
    }
}
