package io.github.ezequiel24123z.grindless.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.List;

/** The block state properties every machine shares, and the one way a status is written. */
public final class MachineProperties {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private MachineProperties() {
    }

    /** The {@code status} property, limited to the values a kind of machine can actually show. */
    public static EnumProperty<MachineStatus> status(List<String> names) {
        return EnumProperty.create(MachineStatus.PROPERTY, MachineStatus.class,
                names.stream().map(MachineStatus::parse).toList());
    }

    /**
     * Shows {@code status} on the block at {@code pos}, if it differs from what is shown.
     *
     * <p>Sent to clients only (flag 2). A status change alters no neighbour's behaviour, so
     * notifying neighbours would make every status flip ripple outward for nothing.
     *
     * @return whether the world was written
     */
    public static boolean publish(Level level, BlockPos pos, MachineStatus status) {
        BlockState current = level.getBlockState(pos);
        BlockState next = MachineStatus.with(current, status);
        if (next == current) {
            return false;
        }
        return level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }
}
