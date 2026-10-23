package io.github.ezequiel24123z.grindless.fluid;

import net.minecraft.core.Direction;

/**
 * How a tank, conduit, pump or machine talks fluid to a neighbour (ADR-0062).
 *
 * <p>Inventories stay on {@code ItemInsert}. Belts stay on {@code BeltEndpoint}. This is the
 * fluid equivalent: one stack, pushed from the source's tick.
 */
public interface FluidEndpoint {

    boolean canInsert(Direction from, FluidState state);

    /** Non-mutating acceptance used by capability simulation as well as transfer planning. */
    default int acceptedAmount(Direction from, FluidState state) {
        if (state == null || state.isEmpty() || !canInsert(from, state)) {
            return 0;
        }
        FluidState current = contents();
        if (!current.isEmpty() && !current.is(state.id())) {
            return 0;
        }
        return Math.min(state.millibuckets(), Math.max(0, capacity() - current.millibuckets()));
    }

    /**
     * Returns the unaccepted remainder. An extractable face need not accept insertion, so this
     * operation is not a guaranteed inverse of {@link #extract(Direction, int)}.
     */
    FluidState insert(Direction from, FluidState state);

    boolean canExtract(Direction from);

    /**
     * For a positive request on an extractable face, returns exactly the lesser of the requested
     * amount and the current contents, retaining their fluid identity, temperature and pressure.
     * Returns empty when extraction is disallowed or the request is non-positive. In-mod
     * endpoints do not apply a hidden rate limit to this operation (ADR-0138).
     */
    FluidState extract(Direction from, int millibuckets);

    /**
     * The private native backing owner, or {@code null} for an unsupported handler. Recipe
     * batches use its reversible, notification-free withdrawals, not external extract/insert.
     */
    default NativeFluidStore nativeStore() {
        return null;
    }

    /** Transient access generation for a proxy whose formation/owner/face policy can be rebound. */
    default long nativeAccessRevision() {
        return 0L;
    }

    /** Current contents, for Forge's tank view. Temperature and pressure are Grindless-only. */
    FluidState contents();

    int capacity();
}
