package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Flux Pylon: the block that projects a supply area and links into a Flux Network.
 *
 * <p>The pylon holds almost nothing itself. Its energy, its membership and its topology all live
 * in the level's {@link FluxNetworkData} (ADR-0007), so an unloaded chunk cannot fragment a
 * network and there is no per-tick graph walk. This class is the bridge between a block existing
 * in the world and an entry existing in that data.
 *
 * <h2>Registration is the whole job</h2>
 *
 * <p>The pylon must register when it is placed or loaded and deregister when it is broken, and
 * getting either wrong is a quiet bug: a network that remembers a pylon that no longer exists
 * keeps its capacity and its topology, so a player can break a pylon and watch nothing change.
 * Both paths are handled here rather than in the block, so no future pylon variant can forget one.
 */
public final class PylonBlockEntity extends MachineBlockEntity {

    public PylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_PYLON.get(), pos, state);
    }

    /**
     * This pylon's tier, read from its block.
     *
     * <p>Keeping the tier on the block rather than on the block entity lets one block entity type
     * serve all three tiers, and it cannot drift: the block <em>is</em> the tier. A saved field
     * would be a second source of truth, able to disagree with the block a player can see.
     */
    public PylonTier tier() {
        return getBlockState().getBlock() instanceof PylonBlock pylon
                ? pylon.tier()
                : PylonTier.MK1;
    }

    /** The network this pylon belongs to, or {@code null} before it has registered. */
    public FluxNetwork network() {
        return getLevel() instanceof ServerLevel server
                ? FluxNetworkData.get(server).networkAt(getBlockPos())
                : null;
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        // A pylon runs no processes, so it holds no conditions and has nothing to widen.
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return narrowEnvelope();
    }

    @Override
    protected FluxTier ratedTier() {
        // Called from the base constructor, before the block state can be read reliably, so this
        // must not route through tier(). The pylon's own buffer is incidental anyway: the energy
        // that matters is pooled on the network.
        return FluxTier.F3;
    }

    /**
     * Joins the network, merging with anything in link range.
     *
     * <p>Reached from placement only: a chunk load does not call {@code onPlace}, and the network
     * data was saved with the world, so a reloaded pylon is already a member. Membership lives in
     * {@link FluxNetworkData} and nowhere else. An earlier version kept a transient
     * {@code registered} flag here, restored by a first-tick hook that pylons never reach because
     * they have no ticker; after a reload the flag read false, {@link #deregister()} returned
     * early, and a broken pylon stayed in its network as a ghost.
     */
    public void register() {
        if (getLevel() instanceof ServerLevel server) {
            FluxNetworkData.get(server).addPylon(getBlockPos(), tier());
        }
    }

    /**
     * Leaves the network, splitting it if that disconnects what remains.
     *
     * <p>Called on breakage, not on chunk unload. That distinction is the point of ADR-0007: an
     * unloaded pylon is still part of its network, and deregistering it would make network
     * topology depend on which chunks a player happens to be standing near.
     */
    public void deregister() {
        if (getLevel() instanceof ServerLevel server) {
            FluxNetworkData.get(server).removePylon(getBlockPos());
        }
    }

    @Override
    protected void updateSubscriptions() {
        // A pylon subscribes to nothing. Distribution is a property of the network pool, not work
        // the pylon performs, so a pylon standing in a finished base costs exactly nothing.
    }
}
