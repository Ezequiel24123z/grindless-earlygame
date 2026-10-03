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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Adds capacity to the covering Flux Network. Uncovered, it is inert. Never a supply cube.
 */
public final class CapacitorBankBlockEntity extends MachineBlockEntity {

    private TickSubscription working;

    public CapacitorBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPACITOR_BANK.get(), pos, state);
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
    protected SimpleFluxStorage createEnergyBuffer() {
        // The energy that matters is pooled on the covering network. A local buffer would look
        // like a second battery that other mods could fill instead of the grid.
        return new SimpleFluxStorage(0L, 0L, 0L, this::setChanged);
    }

    /**
     * Joins the covering network's extra capacity.
     *
     * <p>Placement only: a chunk load does not call {@code onPlace}, and the extra was saved
     * with the world. An earlier pylon bug kept a transient flag that reload cleared; banks
     * follow the same rule and do not re-register on tick.
     */
    public void register() {
        if (getLevel() instanceof ServerLevel server) {
            FluxNetworkData.get(server).addBank(getBlockPos(), CapacitorLogic.CAPACITY);
        }
    }

    public void deregister() {
        if (getLevel() instanceof ServerLevel server) {
            FluxNetworkData.get(server).removeBank(getBlockPos());
        }
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::show);
    }

    private void show() {
        if (!(getLevel() instanceof ServerLevel)) {
            return;
        }
        FluxNetwork network = network();
        MachineStatus status = network == null
                ? MachineStatus.STARVED
                : (network.stored() > 0L ? MachineStatus.RUNNING : MachineStatus.IDLE);
        MachineProperties.publish(getLevel(), getBlockPos(), status);
    }
}
