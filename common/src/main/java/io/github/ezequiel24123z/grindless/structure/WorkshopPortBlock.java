package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.item.MultitoolItem;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Geometry-preserving workshop access: no process, local inventory, tank, status or active sound. */
public final class WorkshopPortBlock extends BaseEntityBlock {

    private final WorkshopPortContract.Kind kind;

    public WorkshopPortBlock(WorkshopPortContract.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(MachineProperties.FACING, Direction.NORTH));
    }

    public WorkshopPortContract.Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(MachineProperties.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(MachineProperties.FACING, rotation.rotate(state.getValue(MachineProperties.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(MachineProperties.FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.family() == WorkshopPortContract.Family.BRICK ? Block.box(0, 0, 0, 16, 16, 16)
                : WorkshopPartBlock.shape(kind.geometry(), state.getValue(MachineProperties.FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof MultitoolItem) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WorkshopPortBlockEntity port) {
            WorkshopPortBlockEntity.Inspection inspection = port.inspection();
            Component status = inspection.formed()
                    ? Component.translatable("message.grindless.port.owner",
                            inspection.ownerPos().toShortString())
                    : Component.translatable(inspection.fault().translationKey());
            player.displayClientMessage(Component.translatable("message.grindless.port.status",
                    Component.translatable(port.role().translationKey()), status), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorkshopPortBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.WORKSHOP_PORT.get(), WorkshopPortBlockEntity::tick);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour,
                                BlockPos neighbourPos, boolean moving) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WorkshopPortBlockEntity port) {
            port.revalidate();
        }
        super.neighborChanged(state, level, pos, neighbour, neighbourPos, moving);
    }
}
