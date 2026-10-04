package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** T2 daylight panel. 32 FU/t while the block above can see the sky (ADR-0081). */
public class SolarArrayBlock extends BaseEntityBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.GENERATOR);

    public SolarArrayBlock(Properties properties) {
        super(properties);
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
                context.getHorizontalDirection());
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
        MachineEffects.animate(BlockCatalogue.Geometry.SOLAR, 1, state.getValue(STATUS),
                state.getValue(MachineProperties.FACING), level, pos, random);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarArrayBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof SolarArrayBlockEntity solar) {
            solar.onNeighbourChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.SOLAR_ARRAY.get(), MachineBlockEntity::tick);
    }
}
