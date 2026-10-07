package io.github.ezequiel24123z.grindless.energy;

/**
 * The Flux tier ladder: how much power a machine or a source can handle, in FU/t.
 *
 * <p>There is exactly one ladder. The README's voltage names (LV, MV, HV, EV, IV) are
 * <em>aliases</em> on F1 through F5, not a second scale — their nominal rates are identical, and
 * modelling them separately would guarantee the two drift apart. See ADR-0038.
 *
 * <p>Technology Tn, Control Matrix rating Tn and Flux tier Fn share one aligned index from
 * Bootstrap through Event Horizon. The matrix controls what may be built; this enum controls
 * how much power the built thing may accept (ADR-0107).
 */
public enum FluxTier {

    F0("Bootstrap", 8L, null),
    F1("Voltaic", 32L, "LV"),
    F2("Industrial", 128L, "MV"),
    F3("Arc", 512L, "HV"),
    F4("Precision", 2_048L, "EV"),
    F5("Nuclear", 8_192L, "IV"),
    F6("Cryogenic", 32_768L, null),
    F7("Fusion", 131_072L, null),
    F8("Particle", 524_288L, null),
    F9("Quantum", 2_097_152L, null),
    F10("Orbital", 8_388_608L, null),
    F11("Planetary", 33_554_432L, null),
    F12("Stellar", 134_217_728L, null),
    F13("Interstellar", 536_870_912L, null),
    F14("Galactic", 2_147_483_648L, null),
    F15("Event Horizon", 8_589_934_592L, null);

    /** Nominal throughput of the highest tier, in FU/t. */
    public static final long MAX_NOMINAL = 8_589_934_592L;

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
        return this == F15 ? this : VALUES[ordinal() + 1];
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

    /** The lowest tier whose nominal throughput covers {@code fuPerTick}, or {@link #F15}. */
    public static FluxTier forThroughput(long fuPerTick) {
        for (FluxTier tier : VALUES) {
            if (tier.nominal >= fuPerTick) {
                return tier;
            }
        }
        return F15;
    }
}
