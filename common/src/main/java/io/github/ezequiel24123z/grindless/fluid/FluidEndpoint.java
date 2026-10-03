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

    FluidState insert(Direction from, FluidState state);

    boolean canExtract(Direction from);

    FluidState extract(Direction from, int millibuckets);

    /** Current contents, for Forge's tank view. Temperature and pressure are Grindless-only. */
    FluidState contents();

    int capacity();
}
