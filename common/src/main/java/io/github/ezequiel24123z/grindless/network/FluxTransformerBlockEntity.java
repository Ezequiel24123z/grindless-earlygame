package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moves FU between the covering network and a local buffer at F1.
 *
 * <p>Grindless machines do not read this buffer. They ask {@code networkCovering}. Standing next
 * to a transformer is not coverage (ADR-0064). Other mods see the buffer as FE.
 */
public final class FluxTransformerBlockEntity extends MachineBlockEntity {

    private TickSubscription working;

    public FluxTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_TRANSFORMER.get(), pos, state);
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return narrowEnvelope();
    }

    @Override
    protected FluxTier ratedTier() {
        return FluxTier.F1;
    }

    @Override
    protected int bufferSeconds() {
        return 10;
    }

    @Override
    protected SimpleFluxStorage createEnergyBuffer() {
        FluxTier tier = ratedTier();
        return new SimpleFluxStorage(bufferCapacity(), tier.nominal(), tier.nominal(), this::setChanged);
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::exchange);
    }

    private void exchange() {
        FluxNetwork network = network();
        if (network == null) {
            MachineProperties.publish(getLevel(), getBlockPos(), MachineStatus.STARVED);
            return;
        }
        long target = energy().getCapacity() / 2L;
        long delta = TransformerLogic.exchange(
                energy().getStored(), energy().getCapacity(),
                network.stored(), network.capacity(),
                target, TransformerLogic.RATE);
        boolean moved = false;
        if (delta > 0L) {
            long got = network.extract(delta, false);
            if (got > 0L) {
                energy().receive(got, false);
                moved = true;
            }
        } else if (delta < 0L) {
            long pushed = energy().extract(-delta, false);
            long accepted = network.receive(pushed, false);
            if (accepted < pushed) {
                energy().receive(pushed - accepted, false);
            }
            moved = accepted > 0L;
        }
        MachineProperties.publish(getLevel(), getBlockPos(),
                moved ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }
}
