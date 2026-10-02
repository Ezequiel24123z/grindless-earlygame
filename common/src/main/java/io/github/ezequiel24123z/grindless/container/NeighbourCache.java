package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.energy.FluxPlatform;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Caches the energy buffer of the block on one face, so a machine pushing power is not doing a
 * block-entity lookup every tick.
 *
 * <h2>Why bother</h2>
 *
 * <p>{@code Level.getBlockEntity(BlockPos)} is a chunk lookup plus a map lookup. On one machine
 * that is nothing; on a machine that does it six times a tick, times thousands of machines, it is
 * a measurable slice of the tick budget spent rediscovering something that changes when a player
 * breaks a block — which is to say, almost never.
 *
 * <h2>Why this is not just a field</h2>
 *
 * <p>The obvious version caches the result and never notices the neighbour being broken, leaving
 * the machine pushing power into a block that no longer exists. Forge's answer is that a
 * capability is handed out as a {@code LazyOptional} which is <em>invalidated</em> when it stops
 * being valid, and holders can register a listener for that. The platform implementation uses
 * exactly that, so an invalidated neighbour clears this cache rather than going stale.
 *
 * <p>This is the single most common cause of "my machine stopped working until I broke and
 * replaced it", and it is a correctness bug rather than a performance one.
 *
 * <p>Not thread-safe; server-thread state.
 */
public final class NeighbourCache {

    private final Supplier<FluxStorage> lookup;
    private final Function<Runnable, Runnable> subscribe;

    private FluxStorage cached;
    private boolean resolved;
    private Runnable unsubscribe;

    /**
     * @param pos  the <em>machine's</em> position, not the neighbour's
     * @param side the face to look through
     */
    public NeighbourCache(Level level, BlockPos pos, Direction side) {
        this(() -> FluxPlatform.findEnergy(level, pos.relative(side), side.getOpposite()),
                onInvalidated -> FluxPlatform.onInvalidated(
                        level, pos.relative(side), side.getOpposite(), onInvalidated));
    }

    /**
     * The seam that lets the registration behaviour be checked without a loader.
     *
     * @param lookup    resolves the neighbour's buffer, or {@code null} if it has none
     * @param subscribe registers a callback for the neighbour's capability being invalidated and
     *                  returns a handle that unregisters it
     */
    NeighbourCache(Supplier<FluxStorage> lookup, Function<Runnable, Runnable> subscribe) {
        this.lookup = lookup;
        this.subscribe = subscribe;
    }

    /**
     * The neighbour's energy buffer, or {@code null} if it has none.
     *
     * <p>Resolved on first use and kept until invalidated. A neighbour that genuinely has no
     * buffer is cached as absent too, so a machine next to a wall is not re-asking the wall
     * forever.
     */
    public FluxStorage get() {
        if (!resolved) {
            cached = lookup.get();
            resolved = true;
            if (cached != null) {
                unsubscribe = subscribe.apply(this::onCapabilityInvalidated);
            }
        }
        return cached;
    }

    /**
     * Drops the cached value so the next {@link #get()} looks it up again.
     *
     * <p>Called by the machine on a neighbour-changed event, and when the machine itself is
     * removed. The neighbour event covers a block appearing where there was none, which the
     * capability's own invalidation cannot signal because there was nothing there to invalidate.
     *
     * <p>It also unregisters the listener from the previous resolve. The neighbour's capability
     * lives as long as the neighbour, so a listener left behind stays there, keeps this cache
     * and the machine behind it reachable, and is joined by one more on every re-resolve.
     */
    public void invalidate() {
        release();
        cached = null;
        resolved = false;
    }

    /**
     * The capability told us it is gone, so there is nothing left to unregister from.
     */
    private void onCapabilityInvalidated() {
        unsubscribe = null;
        cached = null;
        resolved = false;
    }

    private void release() {
        if (unsubscribe != null) {
            unsubscribe.run();
            unsubscribe = null;
        }
    }

    /** Whether a neighbour with an energy buffer is currently known to be present. */
    public boolean isPresent() {
        return get() != null;
    }
}
