package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * One part of a Ground Array's ring. It does not tick and it stores nothing.
 * Placing or breaking it tells any controller in its own neighbourhood to look again,
 * including a controller on the diagonal, which a neighbour notification does not reach
 * (ADR-0094).
 *
 * <p>The status property has the three grid values because an enum property with one
 * value is illegal. The casing never leaves idle: it does not tick.
 */
public class ArrayCasingBlock extends Block {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.GRID);

    public ArrayCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            GroundArrayBlock.refreshNear(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // The casing is already gone, so a controller that looks now sees the gap.
            GroundArrayBlock.refreshNear(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
