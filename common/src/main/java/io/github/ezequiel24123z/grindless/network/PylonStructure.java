package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pylon occupies three blocks of height: a base the player places, and two shaft blocks above
 * it (ADR-0054).
 *
 * <p>Minecraft's JSON models cannot extend past 32 units on an axis, so a three-block tower cannot
 * be one model hanging off the base. The shaft blocks are the middle and top thirds, and they
 * carry the same status and tier as the base so the whole tower reads as one machine.
 *
 * <p>Only the base is a member of the Flux Network. The shafts are occupancy and visuals.
 */
public final class PylonStructure {

    /** Blocks of height, including the base. */
    public static final int HEIGHT = 3;

    private PylonStructure() {
    }

    /** The shaft positions above {@code base}, index 1 then 2. */
    public static BlockPos[] shafts(BlockPos base) {
        BlockPos[] positions = new BlockPos[HEIGHT - 1];
        for (int i = 1; i < HEIGHT; i++) {
            positions[i - 1] = base.above(i);
        }
        return positions;
    }

    /** How many blocks above the base this shaft sits, or 0 if it is not a shaft of {@code base}. */
    public static int indexOf(BlockPos base, BlockPos shaft) {
        return shaft.getX() == base.getX() && shaft.getZ() == base.getZ()
                ? shaft.getY() - base.getY()
                : 0;
    }

    /** The base beneath a shaft of {@code index}. */
    public static BlockPos baseOf(BlockPos shaft, int index) {
        return shaft.below(index);
    }

    /** Whether the two blocks above {@code base} can be replaced by shafts. */
    public static boolean hasRoom(Level level, BlockPos base, BlockPlaceContext context) {
        for (int i = 1; i < HEIGHT; i++) {
            BlockPos above = base.above(i);
            if (above.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            BlockState state = level.getBlockState(above);
            boolean replaceable = context != null ? state.canBeReplaced(context) : state.canBeReplaced();
            if (!replaceable) {
                return false;
            }
        }
        return true;
    }

    /** Places the shafts, copying tier and status from {@code baseState}. */
    public static void placeShafts(Level level, BlockPos base, BlockState baseState) {
        if (!(baseState.getBlock() instanceof PylonBlock pylon)) {
            return;
        }
        int tier = pylon.tier().ordinal() + 1;
        MachineStatus status = baseState.hasProperty(PylonBlock.STATUS)
                ? baseState.getValue(PylonBlock.STATUS)
                : MachineStatus.IDLE;
        for (int i = 1; i < HEIGHT; i++) {
            BlockPos above = base.above(i);
            if (!level.getBlockState(above).canBeReplaced()) {
                continue;
            }
            level.setBlock(above, ModBlocks.FLUX_PYLON_SHAFT.get().defaultBlockState()
                    .setValue(PylonShaftBlock.INDEX, i)
                    .setValue(PylonShaftBlock.TIER, tier)
                    .setValue(PylonShaftBlock.STATUS, status), Block.UPDATE_ALL);
        }
    }

    /** Removes the shafts above {@code base} without dropping them. */
    public static void removeShafts(Level level, BlockPos base) {
        for (BlockPos shaft : shafts(base)) {
            if (level.getBlockState(shaft).is(ModBlocks.FLUX_PYLON_SHAFT.get())) {
                level.removeBlock(shaft, false);
            }
        }
    }

    /** Copies {@code status} onto the shafts so the tower's lights stay in step with the base. */
    public static void syncShafts(Level level, BlockPos base, MachineStatus status) {
        for (BlockPos shaft : shafts(base)) {
            if (level.isLoaded(shaft)) {
                MachineProperties.publish(level, shaft, status);
            }
        }
    }
}
