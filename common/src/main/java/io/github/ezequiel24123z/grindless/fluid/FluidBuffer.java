package io.github.ezequiel24123z.grindless.fluid;

/**
 * A single-substance buffer with a heat and pressure rating (ADR-0062).
 *
 * <p>The Clay Conduit, Basic Tank, Hand Pump and process machines all use this. Ratings are
 * the caller's: a Basic Tank refuses hot fluid; a later Industrial Tank will not.
 */
public final class FluidBuffer {

    private final int capacity;
    private final double maxC;
    private final double maxP;
    private FluidState state = FluidState.EMPTY;

    public FluidBuffer(int capacity, double maxC, double maxP) {
        this.capacity = capacity;
        this.maxC = maxC;
        this.maxP = maxP;
    }

    public static FluidBuffer ambient(int capacity) {
        return new FluidBuffer(capacity, FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA);
    }

    public FluidState state() {
        return state;
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return state.isEmpty();
    }

    public int space() {
        return Math.max(0, capacity - state.millibuckets());
    }

    public int accepted(FluidState incoming) {
        return FluidLogic.accepted(state, incoming, capacity, maxC, maxP);
    }

    /** Adds what fits. Returns what did not. */
    public FluidState offer(FluidState incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return FluidState.EMPTY;
        }
        int take = FluidLogic.accepted(state, incoming, capacity, maxC, maxP);
        if (take <= 0) {
            return incoming;
        }
        state = state.merge(incoming.withAmount(take));
        return incoming.withAmount(incoming.millibuckets() - take);
    }

    public FluidState extract(String want, int millibuckets) {
        int take = FluidLogic.extractable(state, want, millibuckets);
        if (take <= 0) {
            return FluidState.EMPTY;
        }
        FluidState out = state.withAmount(take);
        state = state.withAmount(state.millibuckets() - take);
        return out;
    }

    public void set(FluidState next) {
        this.state = next == null ? FluidState.EMPTY : next;
    }
}
