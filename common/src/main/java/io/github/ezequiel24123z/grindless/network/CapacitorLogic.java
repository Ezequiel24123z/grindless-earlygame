package io.github.ezequiel24123z.grindless.network;

/**
 * Numbers for the T1 Capacitor Bank (ADR-0064).
 *
 * <p>A bank adds capacity to the covering Flux Network. It does not project a supply cube, so
 * it cannot replace a pylon. Uncovered, it adds nothing.
 */
public final class CapacitorLogic {

    /**
     * Extra FU a bank contributes to the covering network.
     *
     * <p>200 ticks of MK1 throughput: ten seconds of a full MK1 pylon, or about fifty seconds
     * of a Thermal Generator. Enough to ride a fuel gap; not enough to be a second grid.
     */
    public static final long CAPACITY = PylonTier.MK1.throughput() * 200L;

    private CapacitorLogic() {
    }
}
