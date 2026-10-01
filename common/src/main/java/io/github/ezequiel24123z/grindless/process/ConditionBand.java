package io.github.ezequiel24123z.grindless.process;

/**
 * A numeric condition requirement: an optimum with a band around it.
 *
 * <p>A written condition is the centre of a band, not an exact value. There are three zones:
 *
 * <ul>
 *   <li><b>Optimal</b> — within {@code optimalHalfWidth} of the optimum. Full speed.</li>
 *   <li><b>Tolerance</b> — within {@code toleranceHalfWidth}. The process still runs, but slower,
 *       tapering linearly down to {@link #MINIMUM_EFFICIENCY} at the outer edge.</li>
 *   <li><b>Outside</b> — the process does not run, and says which way it was wrong.</li>
 * </ul>
 *
 * <p>Degradation costs <em>time</em>, never yield (ADR-0040): a recipe run at the edge of its band
 * produces exactly what it always produces, just more slowly. That keeps the ratios quoted in
 * {@code PROCESSES.md} true regardless of how well a machine is tuned.
 *
 * <p>Bounds are precomputed because this is evaluated per machine per tick.
 */
public final class ConditionBand {

    /** Default optimal half-width, as a fraction of the optimum: ±15 %. */
    public static final double DEFAULT_OPTIMAL_FRACTION = 0.15;

    /** The tolerance zone extends this many times further out than the optimal zone. */
    public static final double TOLERANCE_MULTIPLIER = 2.0;

    /** Speed multiplier at the very edge of the tolerance zone. */
    public static final double MINIMUM_EFFICIENCY = 0.25;

    private final double optimum;
    private final double optimalMin;
    private final double optimalMax;
    private final double toleranceMin;
    private final double toleranceMax;

    private ConditionBand(double optimum, double optimalHalfWidth, double toleranceHalfWidth) {
        this.optimum = optimum;
        this.optimalMin = optimum - optimalHalfWidth;
        this.optimalMax = optimum + optimalHalfWidth;
        this.toleranceMin = optimum - toleranceHalfWidth;
        this.toleranceMax = optimum + toleranceHalfWidth;
    }

    /**
     * A band of ±{@value #DEFAULT_OPTIMAL_FRACTION} of the optimum, which is what an unqualified
     * condition such as {@code T 1500} means.
     *
     * <p>The width is taken from the magnitude of the optimum, so a cryogenic −196 °C process gets
     * the same proportional band as a 1500 °C one rather than an inverted one.
     *
     * @throws IllegalArgumentException if {@code optimum} is zero, where a relative band would have
     *                                  zero width and the process could never run. Use
     *                                  {@link #absolute} instead and state the width.
     */
    public static ConditionBand relative(double optimum) {
        return relative(optimum, DEFAULT_OPTIMAL_FRACTION);
    }

    /** A band of ±{@code fraction} of the optimum's magnitude. */
    public static ConditionBand relative(double optimum, double fraction) {
        if (fraction <= 0.0) {
            throw new IllegalArgumentException("band fraction must be positive, was " + fraction);
        }
        double halfWidth = Math.abs(optimum) * fraction;
        if (halfWidth <= 0.0) {
            throw new IllegalArgumentException(
                    "a relative band around zero has no width; use ConditionBand.absolute(0, width)");
        }
        return absolute(optimum, halfWidth);
    }

    /**
     * A band with an explicitly stated half-width, for processes whose sensitivity is not
     * proportional — zone refining at {@code T 1420 ±5} being the standard example.
     */
    public static ConditionBand absolute(double optimum, double halfWidth) {
        if (halfWidth <= 0.0) {
            throw new IllegalArgumentException("band half-width must be positive, was " + halfWidth);
        }
        return new ConditionBand(optimum, halfWidth, halfWidth * TOLERANCE_MULTIPLIER);
    }

    /** The value the process is happiest at. */
    public double optimum() {
        return optimum;
    }

    /** Lowest value that still runs at all. */
    public double toleranceMin() {
        return toleranceMin;
    }

    /** Highest value that still runs at all. */
    public double toleranceMax() {
        return toleranceMax;
    }

    /** Lowest value that runs at full speed. */
    public double optimalMin() {
        return optimalMin;
    }

    /** Highest value that runs at full speed. */
    public double optimalMax() {
        return optimalMax;
    }

    /** Whether {@code actual} runs the process at all, however slowly. */
    public boolean admits(double actual) {
        return actual >= toleranceMin && actual <= toleranceMax;
    }

    /** Whether {@code actual} runs the process at full speed. */
    public boolean isOptimal(double actual) {
        return actual >= optimalMin && actual <= optimalMax;
    }

    /** Whether {@code actual} is below the band rather than above it; only meaningful when
     * {@link #admits} is false, and it is what turns a failure into "too cold" rather than
     * "wrong" (ADR-0041). */
    public boolean isBelow(double actual) {
        return actual < toleranceMin;
    }

    /**
     * Speed multiplier at {@code actual}.
     *
     * @return {@code 1.0} inside the optimal zone, tapering linearly to
     *         {@link #MINIMUM_EFFICIENCY} at the edge of tolerance, and {@code 0.0} outside it
     */
    public double efficiencyAt(double actual) {
        if (!admits(actual)) {
            return 0.0;
        }
        if (isOptimal(actual)) {
            return 1.0;
        }
        double overshoot = actual > optimalMax ? actual - optimalMax : optimalMin - actual;
        double span = actual > optimalMax ? toleranceMax - optimalMax : optimalMin - toleranceMin;
        if (span <= 0.0) {
            return MINIMUM_EFFICIENCY;
        }
        return 1.0 - (1.0 - MINIMUM_EFFICIENCY) * (overshoot / span);
    }

    /**
     * Whether a machine able to hold anything in {@code [min, max]} can run this process at all.
     *
     * <p>This is the static question the Process Atlas asks — "which machines could do this?" — as
     * opposed to {@link #efficiencyAt}, which asks how well one is doing it right now.
     */
    public boolean reachableWithin(double min, double max) {
        return min <= toleranceMax && max >= toleranceMin;
    }

    /** Whether a machine spanning {@code [min, max]} can run this process at full speed. */
    public boolean optimallyReachableWithin(double min, double max) {
        return min <= optimalMax && max >= optimalMin;
    }

    /**
     * The best speed a machine able to hold anything in {@code [min, max]} could achieve.
     *
     * <p>Efficiency peaks at the optimum and falls away monotonically on both sides, so the best
     * reachable setting is simply the optimum clamped into the machine's range. Reporting the
     * best case rather than the worst is what makes this useful to the Atlas: a machine that
     * <em>just</em> misses the optimal band is nearly as good as one that reaches it, and saying
     * otherwise would push players toward upgrades they do not need.
     *
     * @return a multiplier in {@code [0, 1]}, zero if the process is unreachable in that range
     */
    public double bestEfficiencyWithin(double min, double max) {
        double closest = Math.max(min, Math.min(optimum, max));
        return efficiencyAt(closest);
    }

    @Override
    public String toString() {
        return String.format("%.1f [%.1f..%.1f optimal, %.1f..%.1f tolerated]",
                optimum, optimalMin, optimalMax, toleranceMin, toleranceMax);
    }
}
