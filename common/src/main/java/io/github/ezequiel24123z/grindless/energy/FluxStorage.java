package io.github.ezequiel24123z.grindless.energy;

/**
 * A buffer of Flux Units.
 *
 * <p>This is Grindless's own energy contract. It is deliberately close in shape to Forge's
 * {@code IEnergyStorage} so the bridge stays trivial, with one difference that matters: amounts are
 * {@code long}, not {@code int}. The Flux ladder reaches {@link FluxTier#MAX_NOMINAL} FU/t at F15
 * and buffers are sized in seconds of throughput, which overflows a signed 32-bit integer well
 * before the endgame. See ADR-0037 for why the clamping happens at the FE boundary instead.
 *
 * <p>Implementations must never report more than was actually moved, and a {@code simulate} call
 * must have no side effects whatsoever — the network planner relies on being able to ask.
 */
public interface FluxStorage {

    /**
     * Inserts up to {@code maxReceive} FU.
     *
     * @param simulate when true, report what would happen and change nothing
     * @return the amount actually accepted, never negative and never more than {@code maxReceive}
     */
    long receive(long maxReceive, boolean simulate);

    /**
     * Removes up to {@code maxExtract} FU.
     *
     * @param simulate when true, report what would happen and change nothing
     * @return the amount actually removed, never negative and never more than {@code maxExtract}
     */
    long extract(long maxExtract, boolean simulate);

    /** Current contents, in FU. */
    long getStored();

    /** Maximum contents, in FU. */
    long getCapacity();

    /** Whether this buffer accepts energy from outside at all. */
    boolean canReceive();

    /** Whether this buffer gives energy up to the outside at all. */
    boolean canExtract();

    /** Remaining room, in FU. Never negative, even if an implementation over-fills. */
    default long getSpace() {
        return Math.max(0L, getCapacity() - getStored());
    }

    /** Fill fraction in {@code [0, 1]}, for gauges and status readouts. Zero capacity reads empty. */
    default double getFillFraction() {
        long capacity = getCapacity();
        return capacity <= 0L ? 0.0 : (double) getStored() / (double) capacity;
    }
}
