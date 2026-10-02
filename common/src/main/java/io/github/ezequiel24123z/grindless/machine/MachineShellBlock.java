package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A machine with a front and a status but no behaviour yet: the Research Terminal until its
 * block entity exists.
 *
 * <p>It carries the same properties as a working machine so that models, effects and saved worlds
 * are already right when the behaviour arrives; the status stays {@code idle} until something
 * calls {@link MachineProperties#publish}. Nothing sets it today, and that is stated here rather
 * than left to be discovered.
 */
public class MachineShellBlock extends Block {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.CONSUMER);

    private final BlockCatalogue.Geometry geometry;

    public MachineShellBlock(BlockCatalogue.Geometry geometry, Properties properties) {
        super(properties);
        this.geometry = geometry;
        registerDefaultState(stateDefinition.any()
                .setValue(MachineProperties.FACING, Direction.NORTH)
                .setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineProperties.FACING, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(MachineProperties.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(MachineProperties.FACING,
                rotation.rotate(state.getValue(MachineProperties.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(MachineProperties.FACING)));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MachineEffects.animate(geometry, 1, state.getValue(STATUS),
                state.getValue(MachineProperties.FACING), level, pos, random);
    }
}
