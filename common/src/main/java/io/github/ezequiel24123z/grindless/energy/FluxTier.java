package io.github.ezequiel24123z.grindless.energy;

/**
 * The Flux tier ladder: how much power a machine or a source can handle, in FU/t.
 *
 * <p>There is exactly one ladder. The README's voltage names (LV, MV, HV, EV, IV) are
 * <em>aliases</em> on F1 through F5, not a second scale — their nominal rates are identical, and
 * modelling them separately would guarantee the two drift apart. See ADR-0038.
 *
 * <p>Research tiers (T0–T6) are a different axis again: they gate what the player may
 * <em>build</em>, while this gates how much power a built thing may <em>accept</em>.
 */
public enum FluxTier {

    F0("Manual", 8L, null),
    F1("Voltaic", 32L, "LV"),
    F2("Industrial", 128L, "MV"),
    F3("Arc", 512L, "HV"),
    F4("Plasma", 2_048L, "EV"),
    F5("Quantum", 8_192L, "IV"),
    F6("Singular", 32_768L, null),
    F7("Stellar", 131_072L, null),
    F8("Exotic", 524_288L, null),
    F9("Transcendent", 2_097_152L, null);

    /** Nominal throughput of the highest tier, in FU/t. */
    public static final long MAX_NOMINAL = 2_097_152L;

    private static final FluxTier[] VALUES = values();

    private final String tierName;
    private final long nominal;
    private final String voltageAlias;

    FluxTier(String tierName, long nominal, String voltageAlias) {
        this.tierName = tierName;
        this.nominal = nominal;
        this.voltageAlias = voltageAlias;
    }

    /** Display name of the tier, such as {@code "Industrial"}. */
    public String tierName() {
        return tierName;
    }

    /** Throughput in FU per tick. */
    public long nominal() {
        return nominal;
    }

    /**
     * The legacy voltage name for this tier, or {@code null} for tiers outside the LV–IV range.
     * Present so existing design text and tooltips keep working; never a separate scale.
     */
    public String voltageAlias() {
        return voltageAlias;
    }

    /** The next tier up, or this one if already at the top. */
    public FluxTier next() {
        return this == F9 ? this : VALUES[ordinal() + 1];
    }

    /**
     * How fast a machine rated at this tier runs when fed {@code supplied}.
     *
     * <p>Under-volting degrades smoothly rather than stalling: roughly half throughput per tier
     * below requirement. This is the opposite of the usual convention and it is deliberate — a hard
     * voltage gate turns a power shortfall into a wall, and walls are the progress-stopping tedium
     * the mod exists to remove.
     *
     * <p>Over-volting is safe and confers no bonus; nothing explodes.
     *
     * @return a multiplier in {@code (0, 1]}
     */
    public double throughputFactor(FluxTier supplied) {
        int deficit = ordinal() - supplied.ordinal();
        return deficit <= 0 ? 1.0 : Math.pow(0.5, deficit);
    }

    /** The lowest tier whose nominal throughput covers {@code fuPerTick}, or {@link #F9}. */
    public static FluxTier forThroughput(long fuPerTick) {
        for (FluxTier tier : VALUES) {
            if (tier.nominal >= fuPerTick) {
                return tier;
            }
        }
        return F9;
    }
}
