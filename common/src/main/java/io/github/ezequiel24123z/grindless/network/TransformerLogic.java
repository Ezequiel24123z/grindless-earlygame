package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/**
 * Numbers for the T1 Flux Transformer (ADR-0064).
 *
 * <p>A transformer is a covered tap. It moves FU between the covering network and a local
 * buffer at F1. It does not project coverage: Grindless machines still ask
 * {@code networkCovering}, never "the transformer next door".
 */
public final class TransformerLogic {

    /** How much crosses between the network and the buffer each tick. */
    public static final long RATE = FluxTier.F1.nominal();

    /**
     * How much the front face would allow if per-face energy gating existed.
     *
     * <p>The Forge capability is still one buffer on every face (ADR-0045). This is the T1
     * step in the numbers, so a check can prove F0 is slower than F1 without waiting on that.
     */
    public static final long LOW_RATE = FluxTier.F0.nominal();

    private TransformerLogic() {
    }

    /**
     * Moves up to {@code rate} FU toward {@code targetStored} in a buffer of {@code capacity},
     * pulling from or pushing into a network that currently holds {@code networkStored} of
     * {@code networkCapacity}.
     *
     * @return signed delta applied to the buffer (positive = pulled from the network)
     */
    public static long exchange(long stored, long capacity, long networkStored, long networkCapacity,
                                long targetStored, long rate) {
        if (rate <= 0L || capacity <= 0L) {
            return 0L;
        }
        long want = Math.max(0L, Math.min(capacity, targetStored)) - stored;
        if (want > 0L) {
            long pulled = Math.min(rate, Math.min(want, Math.max(0L, networkStored)));
            return Math.max(0L, pulled);
        }
        if (want < 0L) {
            long dumped = Math.min(rate, Math.min(-want, Math.max(0L, networkCapacity - networkStored)));
            return -Math.max(0L, dumped);
        }
        return 0L;
    }

    /** Transfer cap from {@code from} into {@code to}: the slower of the two tiers. */
    public static long throughput(FluxTier from, FluxTier to) {
        return Math.min(from.nominal(), to.nominal());
    }
}
