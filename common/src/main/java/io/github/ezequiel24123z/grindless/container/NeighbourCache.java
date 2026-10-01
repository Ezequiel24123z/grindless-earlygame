package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.energy.FluxPlatform;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

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

    private final Level level;
    private final BlockPos pos;
    private final Direction side;

    private FluxStorage cached;
    private boolean resolved;

    /**
     * @param pos  the <em>machine's</em> position, not the neighbour's
     * @param side the face to look through
     */
    public NeighbourCache(Level level, BlockPos pos, Direction side) {
        this.level = level;
        this.pos = pos;
        this.side = side;
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
            cached = FluxPlatform.findEnergy(level, pos.relative(side), side.getOpposite());
            resolved = true;
            if (cached != null) {
                FluxPlatform.onInvalidated(level, pos.relative(side), side.getOpposite(),
                        this::invalidate);
            }
        }
        return cached;
    }

    /**
     * Drops the cached value so the next {@link #get()} looks it up again.
     *
     * <p>Called by the platform's invalidation listener, and by the machine on a neighbour-changed
     * event. Both paths matter: invalidation covers a capability being revoked, and the neighbour
     * event covers a block appearing where there was none, which invalidation cannot signal
     * because there was nothing there to invalidate.
     */
    public void invalidate() {
        cached = null;
        resolved = false;
    }

    /** Whether a neighbour with an energy buffer is currently known to be present. */
    public boolean isPresent() {
        return get() != null;
    }
}
