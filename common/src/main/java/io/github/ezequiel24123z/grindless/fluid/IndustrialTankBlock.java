package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.FacingLogisticsBlock;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** T2 tank. One block, rated for steam and melt. Does not merge with its neighbours. */
public class IndustrialTankBlock extends FacingLogisticsBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.BELT);

    public IndustrialTankBlock(Properties properties) {
        super(properties, STATUS, BlockCatalogue.Geometry.INDUSTRIAL,
                Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.INDUSTRIAL_TANK.get(),
                IndustrialTankBlockEntity::tick);
    }
}
