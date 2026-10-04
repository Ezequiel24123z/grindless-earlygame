package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A T2 logistics block that spends F1 and holds no process envelope (ADR-0082).
 *
 * <p>The draw matches the Fluid Well: network first, then the local buffer, and only a tick
 * that actually spent FU counts as powered.
 */
public abstract class PoweredLogisticsBlockEntity extends MachineBlockEntity {

    protected PoweredLogisticsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected FluxTier ratedTier() {
        return FluxTier.F1;
    }

    /** {@code true} only when this tick spent FU. */
    protected final boolean draw() {
        requestPower(ProcessLogic.FU_PER_TICK);
        long drawn = drawPower(ProcessLogic.FU_PER_TICK);
        if (drawn <= 0L && energy().getStored() >= ProcessLogic.FU_PER_TICK) {
            energy().setStored(energy().getStored() - ProcessLogic.FU_PER_TICK);
            drawn = ProcessLogic.FU_PER_TICK;
        }
        return drawn > 0L;
    }

    protected final Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }
}
