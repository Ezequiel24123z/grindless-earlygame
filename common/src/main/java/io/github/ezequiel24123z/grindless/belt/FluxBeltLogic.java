package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/**
 * Flux Belt numbers, independent of a world (ADR-0082).
 *
 * <p>Twice the conveyor, same packing, so the belt is visibly twice as fast. LV is F1.
 * The tunnel range of 9 is the README figure and is not a block in this slice.
 */
public final class FluxBeltLogic {

    public static final int ITEMS_PER_SECOND = 16;

    /** Tiles per second. Throughput per lane divided by packing: {@code 8 / 4 = 2}. */
    public static final double SPEED =
            (ITEMS_PER_SECOND / (double) BeltLogic.LANES) / BeltLogic.SLOTS_PER_LANE;

    /** What a T2 tunnel would span. The T1 tunnel stays at {@link TunnelLogic#RANGE}. */
    public static final int TUNNEL_RANGE = 9;

    public static final long FU_PER_TICK = FluxTier.F1.nominal();

    private FluxBeltLogic() {
    }

    /** How far an item travels in {@code ticks} of game time while the belt is powered. */
    public static double travel(int ticks) {
        return SPEED * (ticks / 20.0);
    }
}
