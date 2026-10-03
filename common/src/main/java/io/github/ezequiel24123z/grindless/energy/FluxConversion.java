package io.github.ezequiel24123z.grindless.energy;

/**
 * Conversion between Flux Units and the energy systems of other mods.
 *
 * <p>FU converts to FE and RF at exactly <strong>1:1, losslessly</strong>. That is a considered
 * decision rather than laziness: any other ratio forces rounding on every transfer, in both
 * directions, on thousands of machines, twenty times a second. Those fractions accumulate into
 * energy quietly being created or destroyed, and the resulting reports are unreproducible. See
 * ADR-0006.
 *
 * <p>The only lossy operation here is {@link #toFe(long)}, which must clamp because FE is
 * {@code int}-sized while FU is {@code long}-sized. Clamping saturates rather than overflowing, so
 * a transfer can be smaller than requested but is never negative and never wraps. See ADR-0037.
 */
public final class FluxConversion {

    /** EU is four times the size of an FU, the long-standing IC2 convention. Config-overridable. */
    public static final int DEFAULT_FU_PER_EU = 4;

    private FluxConversion() {
    }

    /**
     * Converts FU to FE for a transfer across the bridge, saturating at {@link Integer#MAX_VALUE}.
     *
     * <p>Callers must treat the result as a <em>request</em> and trust the amount the other side
     * reports as moved. Never assume the full amount crossed.
     */
    public static int toFe(long fu) {
        if (fu <= 0L) {
            return 0;
        }
        return fu > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) fu;
    }

    /** Converts FE back to FU. Always exact, since every {@code int} fits in a {@code long}. */
    public static long fromFe(int fe) {
        return Math.max(0, fe);
    }

    /** Converts EU to FU at {@code fuPerEu}. Exact. */
    public static long fromEu(long eu, int fuPerEu) {
        return eu <= 0L ? 0L : eu * fuPerEu;
    }

    /**
     * Converts FU to EU at {@code fuPerEu}, rounding down.
     *
     * <p>Rounding down is mandatory: rounding up or to nearest would let a chain of conversions
     * create energy, which is the exact failure the 1:1 FE rate exists to avoid.
     */
    public static long toEu(long fu, int fuPerEu) {
        return fu <= 0L || fuPerEu <= 0 ? 0L : fu / fuPerEu;
    }
}
