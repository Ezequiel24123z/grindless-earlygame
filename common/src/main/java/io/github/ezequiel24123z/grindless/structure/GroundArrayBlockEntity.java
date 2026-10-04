package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Centre of a Ground Array. A complete ring adds storage to the covering network.
 * An open ring, or a controller no pylon covers, adds nothing (ADR-0094).
 */
public final class GroundArrayBlockEntity extends MachineBlockEntity {

    private TickSubscription working;

    public GroundArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROUND_ARRAY.get(), pos, state);
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
        // The energy that matters is pooled on the covering network. A local buffer would
        // look like a second battery that other mods could fill instead of the grid.
        return new SimpleFluxStorage(0L, 0L, 0L, this::setChanged);
    }

    /**
     * Publishes the ring's extra, or withdraws it.
     *
     * <p>Called when the controller or a casing is placed or broken. A chunk load does not
     * call this: the extra was saved with the world, the same rule as a capacitor bank.
     */
    public void refresh() {
        if (!(getLevel() instanceof ServerLevel server)) {
            return;
        }
        FluxNetworkData data = FluxNetworkData.get(server);
        if (GroundArrayStructure.formed(server, getBlockPos())) {
            data.addArray(getBlockPos(), GroundArrayLogic.CAPACITY);
        } else {
            data.removeArray(getBlockPos());
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
        if (!(getLevel() instanceof ServerLevel server)) {
            return;
        }
        boolean formed = GroundArrayStructure.formed(server, getBlockPos());
        FluxNetwork network = network();
        MachineStatus status = !formed || network == null
                ? MachineStatus.STARVED
                : (network.stored() > 0L ? MachineStatus.RUNNING : MachineStatus.IDLE);
        MachineProperties.publish(getLevel(), getBlockPos(), status);
    }
}
