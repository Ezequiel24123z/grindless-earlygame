package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import net.minecraft.core.BlockPos;

/**
 * The three Flux Pylon tiers, from the README's <i>System 1</i> table.
 *
 * <p>Supply area, throughput and link range all scale together, so a bigger pylon is
 * unambiguously better. That is deliberate: the decision worth making is <em>where</em> pylons go
 * and <em>how many</em>, which is a layout problem, not whether the expensive one is better.
 */
public enum PylonTier {

    MK1("MK1", 48, 512L, 64, FluxTier.F3),
    MK2("MK2", 80, 4_096L, 112, FluxTier.F5),
    MK3("MK3", 128, 32_768L, 192, FluxTier.F6);

    private final String displayName;
    private final int supplyArea;
    private final long throughput;
    private final int linkRange;
    private final FluxTier maxTier;

    PylonTier(String displayName, int supplyArea, long throughput, int linkRange, FluxTier maxTier) {
        this.displayName = displayName;
        this.supplyArea = supplyArea;
        this.throughput = throughput;
        this.linkRange = linkRange;
        this.maxTier = maxTier;
    }

    public String displayName() {
        return displayName;
    }

    /** Edge length of the supply cube, in blocks. */
    public int supplyArea() {
        return supplyArea;
    }

    /** How much the pylon can move per tick, in FU. */
    public long throughput() {
        return throughput;
    }

    /** How far this pylon will link to another automatically, in blocks. */
    public int linkRange() {
        return linkRange;
    }

    /** The highest Flux tier this pylon can carry. */
    public FluxTier maxFluxTier() {
        return maxTier;
    }

    /** Half the supply cube's edge: the reach from the centre along each axis. */
    public int radius() {
        return supplyArea / 2;
    }

    /**
     * Whether {@code target} is inside the supply cube of a pylon at {@code pylon}.
     *
     * <p>A cube checked per axis, not a sphere. A player cannot see a radius but can see a
     * 16-block cube, and a coverage rule you can predict by eye is worth more than one that is
     * geometrically tidier.
     */
    public boolean covers(BlockPos pylon, BlockPos target) {
        int r = radius();
        return Math.abs(target.getX() - pylon.getX()) <= r
                && Math.abs(target.getY() - pylon.getY()) <= r
                && Math.abs(target.getZ() - pylon.getZ()) <= r;
    }

    /**
     * Whether two pylons link automatically.
     *
     * <p>The README has them link when "within each other's link range", so the <em>shorter</em>
     * of the two ranges governs: an MK3 does not reach out and capture an MK1 that cannot reach
     * back. Distance is Euclidean here rather than per axis, because a link is a line between two
     * points rather than a region.
     */
    public static boolean linksAutomatically(PylonTier tierA, BlockPos a,
                                             PylonTier tierB, BlockPos b) {
        int range = Math.min(tierA.linkRange(), tierB.linkRange());
        return a.distSqr(b) <= (double) range * range;
    }
}
