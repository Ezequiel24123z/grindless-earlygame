package io.github.ezequiel24123z.grindless.centre;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The mass of the galactic centre. A full cube, not a floor you can walk off (ADR-0099).
 *
 * <p>The status property has the three grid values because an enum property with one
 * value is illegal. The shell never leaves idle: it does not tick, and it is not an item.
 */
public class HorizonShellBlock extends Block {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.GRID);

    public HorizonShellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }
}
