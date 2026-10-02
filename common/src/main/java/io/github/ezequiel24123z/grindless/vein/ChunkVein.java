package io.github.ezequiel24123z.grindless.vein;

/**
 * The Resource Vein one chunk owns.
 *
 * <p>Derived rather than stored (ADR-0009), so this is a description of what a chunk <em>would</em>
 * produce, independent of what has actually been taken out of it. Extraction is tracked separately
 * by {@link VeinData}, which is the only part that ever reaches disk.
 *
 * <h2>Depletion toward a nonzero floor</h2>
 *
 * <p>A worked vein slows down but <b>never dies</b>, exactly like a Factorio infinite ore patch.
 * That single rule defines the mod's whole rhythm:
 *
 * <ul>
 *   <li>a vein is never dead, so no player ever loses an investment outright;</li>
 *   <li>but a worked vein is slow, so the efficient move is always to <em>expand outward</em> —
 *       survey a new chunk, run a conduit, build another outpost;</li>
 *   <li>so the pressure is horizontal, not vertical. You explore and lay out infrastructure,
 *       which is interesting, instead of digging another 3×3 tunnel, which is not.</li>
 * </ul>
 *
 * @param material  the material's name, as discovered from the pack's tags
 * @param richness  rate multiplier, between {@link VeinGenerator#MIN_RICHNESS} and
 *                  {@link VeinGenerator#MAX_RICHNESS}
 * @param reserve   how much can be taken before the vein is down to its floor rate
 * @param version   the derivation version this vein was produced under (ADR-0009)
 */
public record ChunkVein(String material, double richness, long reserve, int version) {

    /**
     * The fraction of its original rate a fully worked vein still produces.
     *
     * <p>Thirty per cent, not a token amount. A worked outpost has to stay genuinely worth running
     * or the design collapses into a treadmill: abandon, relocate, rebuild the same layout
     * somewhere else, repeat. Rebuilding a solved layout is grind by this project's own
     * definition, so the floor is set where an old outpost remains a real contributor rather than
     * a monument.
     *
     * <p>A Deep Bore upgrade raises it further still — see {@link #rateAfter(long, double)}.
     */
    public static final double FLOOR_FRACTION = 0.30;

    /**
     * How fast this vein yields after {@code extracted} units have been taken, at the default
     * floor.
     */
    public double rateAfter(long extracted) {
        return rateAfter(extracted, FLOOR_FRACTION);
    }

    /**
     * How fast this vein yields, given an extractor able to hold the vein at {@code floor}.
     *
     * <p>The curve is deliberately <b>gentle early and steep late</b> rather than linear: a
     * quarter of the reserve gone is still about 96 % of the original rate, half is about 83 %,
     * and the slowdown only becomes obvious past three quarters. A linear decay is noticeable
     * from the first hour, which makes a
     * player feel they are always on a clock — the opposite of the intended rhythm, where a
     * working outpost is something you build and then stop thinking about.
     *
     * @param floor the lowest fraction this extractor can hold the vein to, normally
     *              {@link #FLOOR_FRACTION} and higher with a Deep Bore upgrade
     * @return a multiplier on the extractor's base rate
     */
    public double rateAfter(long extracted, double floor) {
        double effectiveFloor = Math.max(FLOOR_FRACTION, Math.min(1.0, floor));
        double depletion = depletion(extracted);
        // Quadratic: flat for most of the vein's life, then tapering to the floor.
        double remaining = 1.0 - depletion * depletion;
        return richness * (effectiveFloor + (1.0 - effectiveFloor) * remaining);
    }

    /** How worked out this vein is, in {@code [0, 1]}. */
    public double depletion(long extracted) {
        return reserve <= 0L ? 1.0 : Math.min(1.0, Math.max(0L, extracted) / (double) reserve);
    }

    /** Whether the vein has been worked down to its floor rate. */
    public boolean isExhausted(long extracted) {
        return depletion(extracted) >= 1.0;
    }
}
