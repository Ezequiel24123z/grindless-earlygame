package io.github.ezequiel24123z.grindless.star;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * A link that no longer moves a player (ADR-0098). Right-click drops any saved charge
 * and says so. The station is the ride. The block stays so a placed link does not vanish.
 */
public final class StarwardLinkBlockEntity extends MachineBlockEntity {

    private static final String KEY_CHARGE = "Charge";
    private static final String KEY_TRAVELLER = "Traveller";

    private TickSubscription working;
    private long stored;
    private UUID traveller;

    public StarwardLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STARWARD_LINK.get(), pos, state);
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
        // The charge is counted here as progress, not as a battery other mods can fill.
        return new SimpleFluxStorage(0L, 0L, 0L, this::setChanged);
    }

    /**
     * Tells the player the station replaced this hop. The block does not move anyone
     * (ADR-0098). A charge that was already saved is dropped here.
     */
    public void begin(ServerPlayer player) {
        stored = 0L;
        traveller = null;
        setChanged();
        if (getLevel() instanceof ServerLevel server) {
            MachineProperties.publish(server, getBlockPos(), MachineStatus.IDLE);
            updateSubscriptions();
        }
        player.displayClientMessage(Component.translatable("chat.grindless.drift.replaced"), true);
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        boolean charging = traveller != null && !StarwardLinkLogic.ready(stored) && !onDrift();
        if (charging) {
            working = subscriptions().subscribe(working, this::charge);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
    }

    private void charge() {
        if (!(getLevel() instanceof ServerLevel server) || onDrift() || traveller == null) {
            updateSubscriptions();
            return;
        }
        if (StarwardLinkLogic.ready(stored)) {
            updateSubscriptions();
            return;
        }
        FluxNetwork network = network();
        if (network == null) {
            MachineProperties.publish(server, getBlockPos(), MachineStatus.STARVED);
            return;
        }
        long ask = StarwardLinkLogic.remaining(stored);
        requestPower(ask);
        long drawn = drawPower(ask);
        if (drawn > 0L) {
            stored = StarwardLinkLogic.accept(stored, drawn);
            setChanged();
        }
        if (!StarwardLinkLogic.ready(stored)) {
            MachineProperties.publish(server, getBlockPos(),
                    drawn > 0L ? MachineStatus.RUNNING : MachineStatus.STARVED);
            return;
        }
        ServerPlayer player = server.getServer().getPlayerList().getPlayer(traveller);
        finish(server, player);
    }

    private void finish(ServerLevel server, ServerPlayer player) {
        stored = 0L;
        traveller = null;
        setChanged();
        MachineProperties.publish(server, getBlockPos(), MachineStatus.IDLE);
        updateSubscriptions();
        if (player != null) {
            player.displayClientMessage(Component.translatable("chat.grindless.drift.replaced"), true);
        }
    }

    private boolean onDrift() {
        return getLevel() instanceof ServerLevel server
                && DriftCatalogue.isDrift(server.dimension().location().toString());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(KEY_CHARGE, stored);
        if (traveller != null) {
            tag.putUUID(KEY_TRAVELLER, traveller);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // A link that was mid-charge must not finish the old hop (ADR-0098).
        stored = 0L;
        traveller = null;
    }
}
