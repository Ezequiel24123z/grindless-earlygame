package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.item.Relocation;
import io.github.ezequiel24123z.grindless.machine.FacingLogisticsBlock;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** T2 belt. 16 items/s while it spends LV. An empty tile spends nothing. */
public class FluxBeltBlock extends FacingLogisticsBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.CONSUMER);

    public FluxBeltBlock(Properties properties) {
        super(properties, STATUS, BlockCatalogue.Geometry.FLUX_BELT,
                Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxBeltBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.FLUX_BELT.get(), MachineBlockEntity::tick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moving) {
        if (!Relocation.active() && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FluxBeltBlockEntity belt) {
            dropLane(level, pos, belt.left());
            dropLane(level, pos, belt.right());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    private static void dropLane(Level level, BlockPos pos, Lane lane) {
        for (LaneItem item : lane.items()) {
            ItemStack stack = BeltStacks.toStack(item);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}
