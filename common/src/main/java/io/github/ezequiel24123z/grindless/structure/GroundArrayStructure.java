package io.github.ezequiel24123z.grindless.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Whether a Ground Array's ring is standing (ADR-0094).
 *
 * <p>This is one fixed footprint. It is not a multiblock kernel: no hatches, no variable
 * size, no sided containers.
 */
public final class GroundArrayStructure {

    private GroundArrayStructure() {
    }

    /** How many of the eight neighbours are array casings. */
    public static int casings(BlockGetter level, BlockPos controller) {
        int count = 0;
        for (BlockPos pos : GroundArrayLogic.ring(controller)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ArrayCasingBlock) {
                count++;
            }
        }
        return count;
    }

    /** The ring around {@code controller} is eight casings. */
    public static boolean formed(BlockGetter level, BlockPos controller) {
        return GroundArrayLogic.formed(casings(level, controller));
    }
}
