package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A facing logistics block. The status property is the subclass's static one. {@link Block}
 * builds that definition inside {@code super}, so the property is stashed before the call.
 */
public abstract class FacingLogisticsBlock extends BaseEntityBlock {

    /**
     * {@link Block}'s constructor builds the state definition before this constructor can assign
     * fields. The arguments to {@code super} run first, so the property is stashed there.
     */
    private static final ThreadLocal<EnumProperty<MachineStatus>> PENDING_STATUS = new ThreadLocal<>();

    private final EnumProperty<MachineStatus> statusProperty;
    private final BlockCatalogue.Geometry geometry;
    private final VoxelShape shape;

    protected FacingLogisticsBlock(Properties properties, EnumProperty<MachineStatus> status,
                                   BlockCatalogue.Geometry geometry, VoxelShape shape) {
        super(stash(properties, status));
        this.statusProperty = status;
        this.geometry = geometry;
        this.shape = shape;
        PENDING_STATUS.remove();
        registerDefaultState(stateDefinition.any()
                .setValue(MachineProperties.FACING, Direction.NORTH)
                .setValue(statusProperty, MachineStatus.IDLE));
    }

    private static Properties stash(Properties properties, EnumProperty<MachineStatus> status) {
        PENDING_STATUS.set(status);
        return properties;
    }

    protected EnumProperty<MachineStatus> statusProperty() {
        return statusProperty;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineProperties.FACING, PENDING_STATUS.get());
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return shape;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MachineEffects.animate(geometry, 1, state.getValue(statusProperty),
                state.getValue(MachineProperties.FACING), level, pos, random);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public abstract <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type);
}
