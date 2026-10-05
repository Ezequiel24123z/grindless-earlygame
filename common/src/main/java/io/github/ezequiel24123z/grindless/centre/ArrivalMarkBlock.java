package io.github.ezequiel24123z.grindless.centre;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The object in the chamber. Arriving is the victory, and this is what stands there
 * (ADR-0099).
 *
 * <p>The status property has the three grid values because an enum property with one
 * value is illegal. The mark never leaves idle: it does not tick, and it is not an item.
 */
public class ArrivalMarkBlock extends Block {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.GRID);

    public ArrivalMarkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }
}
