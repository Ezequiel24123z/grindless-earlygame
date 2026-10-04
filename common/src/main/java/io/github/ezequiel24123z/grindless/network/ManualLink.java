package io.github.ezequiel24123z.grindless.network;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * An undirected manual link between two pylons, created by the Flux Conduit (ADR-0064).
 *
 * <p>Automatic links are a property of range and live in {@link PylonIndex}. Manual links are
 * extra edges the player draws, with no length limit and an upkeep proportional to distance.
 * They are stored canonicalised — the lower {@code asLong()} end first — so A→B and B→A are
 * the same edge and a set of them cannot hold a duplicate.
 */
public final class ManualLink {

    /**
     * Blocks of Euclidean distance that cost one FU per tick.
     *
     * <p>MK1 auto-range is 64, so a link at the edge of what pylons already do for free costs
     * 8 FU/t. A 200-block outpost costs 25. Sprawling is viable, not free.
     */
    public static final int BLOCKS_PER_FU = 8;

    private final BlockPos a;
    private final BlockPos b;

    private ManualLink(BlockPos a, BlockPos b) {
        this.a = a;
        this.b = b;
    }

    /** The unique edge between {@code x} and {@code y}. Ends are copied immutable. */
    public static ManualLink of(BlockPos x, BlockPos y) {
        BlockPos p = x.immutable();
        BlockPos q = y.immutable();
        if (p.asLong() > q.asLong()) {
            BlockPos swap = p;
            p = q;
            q = swap;
        }
        return new ManualLink(p, q);
    }

    public BlockPos a() {
        return a;
    }

    public BlockPos b() {
        return b;
    }

    /** The other end from {@code pos}, or {@code null} if {@code pos} is not on this edge. */
    public BlockPos other(BlockPos pos) {
        if (a.equals(pos)) {
            return b;
        }
        if (b.equals(pos)) {
            return a;
        }
        return null;
    }

    public boolean touches(BlockPos pos) {
        return a.equals(pos) || b.equals(pos);
    }

    /**
     * FU this link draws every tick.
     *
     * <p>Distance is Euclidean: a link is a line between two pylons, not a cube. Minimum 1 so a
     * degenerate zero-length pair (which placement already refuses) cannot be free if it leaks in.
     */
    public long upkeep() {
        double distance = Math.sqrt(a.distSqr(b));
        return Math.max(1L, (long) Math.ceil(distance / (double) BLOCKS_PER_FU));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ManualLink link)) {
            return false;
        }
        return a.equals(link.a) && b.equals(link.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
