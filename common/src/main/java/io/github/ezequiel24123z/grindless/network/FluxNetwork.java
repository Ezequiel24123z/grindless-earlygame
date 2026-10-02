package io.github.ezequiel24123z.grindless.network;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One Flux Network: a set of linked pylons sharing a single pooled energy buffer.
 *
 * <p>Generation anywhere on the network powers consumption anywhere else on it, with no wiring
 * and no per-face connections. A network is a <em>pool</em>, not a graph to be walked — which is
 * what makes distribution cost nothing per tick.
 *
 * <h2>Brownouts</h2>
 *
 * <p>When demand exceeds supply the network <b>browns out</b>: every machine on it slows
 * <em>proportionally</em>, together. Most mods instead let machines starve individually, which
 * produces the worst possible failure — a random subset of the base stops with no indication why,
 * and the player goes block-hunting. A proportional brownout is immediately legible: everything
 * is visibly sluggish, the readout shows a deficit, and the fix is obviously "add generation or
 * remove load".
 *
 * <p>Legible failure is a feature, and it is the same principle as the soft voltage curve: never
 * leave the player stuck without knowing why.
 */
public final class FluxNetwork {

    private final int id;
    private final Set<BlockPos> pylons = new LinkedHashSet<>();

    private long stored;
    private long capacity;
    private long throughput;

    /** Demand registered this tick, reset each tick once satisfaction is computed. */
    private long demand;
    private double satisfaction = 1.0;

    public FluxNetwork(int id) {
        this.id = id;
    }

    /** Stable identifier, used to reference this network from saved data. */
    public int id() {
        return id;
    }

    /** The pylons making up this network. */
    public Set<BlockPos> pylons() {
        return Collections.unmodifiableSet(pylons);
    }

    public boolean isEmpty() {
        return pylons.isEmpty();
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }

    /** Combined throughput of every pylon on the network, in FU per tick. */
    public long throughput() {
        return throughput;
    }

    /**
     * Adds a pylon and recomputes the pooled limits.
     *
     * <p>Capacity is derived from the members rather than stored, so it can never drift out of
     * step with them — the failure where a network remembers capacity for a pylon that was broken
     * three sessions ago.
     */
    public void addPylon(BlockPos pos, PylonTier tier) {
        if (pylons.add(pos.immutable())) {
            capacity += tier.throughput() * 20L;
            throughput += tier.throughput();
        }
    }

    /** Removes a pylon, recomputing the pooled limits and clamping the contents to fit. */
    public void removePylon(BlockPos pos, PylonTier tier) {
        if (pylons.remove(pos)) {
            capacity -= tier.throughput() * 20L;
            throughput -= tier.throughput();
            // A shrinking network must not keep energy it can no longer hold, or breaking and
            // replacing a pylon would be a way to manufacture power.
            stored = Math.min(stored, capacity);
        }
    }

    /** Inserts up to {@code amount} FU, returning what was accepted. */
    public long receive(long amount, boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        long accepted = Math.min(amount, Math.max(0L, capacity - stored));
        if (!simulate) {
            stored += accepted;
        }
        return accepted;
    }

    /** Removes up to {@code amount} FU, returning what was removed. */
    public long extract(long amount, boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        long removed = Math.min(amount, stored);
        if (!simulate) {
            stored -= removed;
        }
        return removed;
    }

    /** Sets the contents directly, clamped. For loading and creative tools. */
    public void setStored(long amount) {
        stored = Math.max(0L, Math.min(amount, capacity));
    }

    /**
     * Registers a machine's intended draw for this tick.
     *
     * <p>Machines declare what they want <em>before</em> any of them is served, so the deficit is
     * known when the first one is. Serving machines first-come-first-served instead is exactly the
     * individual starvation this design rejects: the machines that happened to tick early would
     * run at full speed while the rest stopped dead.
     */
    public void registerDemand(long amount) {
        if (amount > 0L) {
            demand += amount;
        }
    }

    /**
     * Works out how much of this tick's demand can be met, and resets the accumulator.
     *
     * <p>Called once per tick, after demand is registered and before it is drawn.
     */
    public void resolveTick() {
        long available = Math.min(stored, throughput);
        satisfaction = demand <= 0L ? 1.0 : Math.min(1.0, (double) available / (double) demand);
        demand = 0L;
    }

    /**
     * The fraction of its requested power each machine gets this tick, in {@code [0, 1]}.
     *
     * <p>The same value for every machine on the network, which is the whole point.
     */
    public double satisfaction() {
        return satisfaction;
    }

    /** Whether the network is failing to meet demand. */
    public boolean isBrownedOut() {
        return satisfaction < 1.0;
    }

    /** Fill fraction, for gauges and status readouts. */
    public double fillFraction() {
        return capacity <= 0L ? 0.0 : (double) stored / (double) capacity;
    }
}
