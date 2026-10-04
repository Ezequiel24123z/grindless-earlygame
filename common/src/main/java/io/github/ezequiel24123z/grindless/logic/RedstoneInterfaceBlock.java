package io.github.ezequiel24123z.grindless.logic;

import io.github.ezequiel24123z.grindless.machine.FacingLogisticsBlock;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Bridges one cable and vanilla redstone, on opposite faces. */
public class RedstoneInterfaceBlock extends FacingLogisticsBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.BELT);

    public RedstoneInterfaceBlock(Properties properties) {
        super(properties, STATUS, BlockCatalogue.Geometry.INTERFACE,
                Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneInterfaceBlockEntity(pos, state);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return emittedToward(state, level, pos, direction);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return emittedToward(state, level, pos, direction);
    }

    private static int emittedToward(BlockState state, BlockGetter level, BlockPos pos,
                                     Direction direction) {
        if (direction != state.getValue(MachineProperties.FACING).getOpposite()) {
            return 0;
        }
        if (level.getBlockEntity(pos) instanceof RedstoneInterfaceBlockEntity face) {
            return face.emitted();
        }
        return 0;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.REDSTONE_INTERFACE.get(),
                RedstoneInterfaceBlockEntity::tick);
    }
}
