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
 * Charges a departure from the covering pylon, then sends the player who clicked.
 * A link standing on the Drift does not charge: the trip out already paid for the return.
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
     * Starts a departure, or walks home when this block is already on the Drift.
     */
    public void begin(ServerPlayer player) {
        if (!(getLevel() instanceof ServerLevel server)) {
            return;
        }
        if (DriftCatalogue.isDrift(server.dimension().location().toString())) {
            if (StarwardTravel.home(player)) {
                player.displayClientMessage(Component.translatable("chat.grindless.drift.returned"), true);
            }
            return;
        }
        if (network() == null && !StarwardLinkLogic.ready(stored)) {
            MachineProperties.publish(server, getBlockPos(), MachineStatus.STARVED);
            player.displayClientMessage(Component.translatable("chat.grindless.drift.unpowered"), true);
            return;
        }
        traveller = player.getUUID();
        setChanged();
        if (StarwardLinkLogic.ready(stored)) {
            finish(server, player);
            return;
        }
        updateSubscriptions();
        player.displayClientMessage(Component.translatable("chat.grindless.drift.charging"), true);
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
        if (player != null && player.level() == server && inRange(player)) {
            if (StarwardTravel.depart(player, getBlockPos())) {
                stored = 0L;
                traveller = null;
                setChanged();
                MachineProperties.publish(server, getBlockPos(), MachineStatus.IDLE);
                player.displayClientMessage(Component.translatable("chat.grindless.drift.departed"), true);
                updateSubscriptions();
                return;
            }
            player.displayClientMessage(Component.translatable("chat.grindless.drift.missing"), true);
        } else if (player != null && player.level() == server) {
            player.displayClientMessage(Component.translatable("chat.grindless.drift.away"), true);
        }
        MachineProperties.publish(server, getBlockPos(), MachineStatus.RUNNING);
        updateSubscriptions();
    }

    private boolean onDrift() {
        return getLevel() instanceof ServerLevel server
                && DriftCatalogue.isDrift(server.dimension().location().toString());
    }

    private boolean inRange(ServerPlayer player) {
        BlockPos at = player.blockPosition();
        return StarwardLinkLogic.inRange(
                at.getX() - getBlockPos().getX(),
                at.getY() - getBlockPos().getY(),
                at.getZ() - getBlockPos().getZ());
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
        stored = tag.getLong(KEY_CHARGE);
        traveller = tag.hasUUID(KEY_TRAVELLER) ? tag.getUUID(KEY_TRAVELLER) : null;
    }
}
