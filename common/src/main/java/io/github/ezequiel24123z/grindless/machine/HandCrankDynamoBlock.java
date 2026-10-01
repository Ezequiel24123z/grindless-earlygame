package io.github.ezequiel24123z.grindless.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;

/**
 * The block half of the Hand Crank Dynamo. Right-click to crank.
 */
public class HandCrankDynamoBlock extends BaseEntityBlock {

    public HandCrankDynamoBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HandCrankDynamoBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE, which is right for a block drawn entirely by a
        // renderer and wrong for one with an ordinary model. Forgetting this is the usual cause
        // of "my block entity is invisible".
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HandCrankDynamoBlockEntity dynamo) {
            dynamo.crank();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof HandCrankDynamoBlockEntity dynamo) {
            // Required for a neighbour that appears: capability invalidation cannot signal it,
            // because there was nothing there to invalidate (ADR-0044).
            dynamo.onNeighbourChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // Server only. Machines have no client-side logic beyond rendering, which is driven by
        // synced state, so a client ticker would be pure overhead on every machine in view.
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.HAND_CRANK_DYNAMO.get(),
                        MachineBlockEntity::tick);
    }
}
