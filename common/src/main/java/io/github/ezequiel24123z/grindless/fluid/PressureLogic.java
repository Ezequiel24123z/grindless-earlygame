package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;

/**
 * T2 fluid numbers, independent of a world (ADR-0082).
 *
 * <p>The ceiling is hot enough for molten metal and pressurised enough for the boiler's
 * steam. Superheated steam is 6 MPa and does not fit. Clay and the Basic Tank stay ambient.
 */
public final class PressureLogic {

    /** Melt's buffer ceiling. Steam at 150 °C fits; a later cryo line will not reuse this. */
    public static final double MAX_C = ProcessLogic.MOLTEN_MAX_C;

    /** The boiler's pressure ceiling. Steam at 0.5 MPa fits; 6 MPa does not. */
    public static final double MAX_MPA = ProcessLogic.STEAM_MAX_MPA;

    /** One pressure-pipe tile. A bucket of steam fits, and so does a second. */
    public static final int PIPE_CAPACITY = 2 * FluidLogic.BUCKET;

    /** Four times the clay conduit. The pipe is the workhorse, not a second gravity trough. */
    public static final int PIPE_MB_PER_TICK = 200;

    /** Five times the Hand Pump. The pump moves fluid; it does not invent it. */
    public static final int PUMP_MB_PER_TICK = 100;

    /** Four times the Basic Tank. One block, not a formed structure. */
    public static final int TANK_CAPACITY = 64 * FluidLogic.BUCKET;

    /** One bucket a second. The fluid inserter. */
    public static final int MANIPULATOR_MB = FluidLogic.BUCKET;

    public static final int MANIPULATOR_TICKS = 20;

    private PressureLogic() {
    }

    /** Whether {@code state} is inside the T2 rating. Empty is not a fluid to move. */
    public static boolean accepts(FluidState state) {
        return state != null && !state.isEmpty()
                && state.temperatureC() <= MAX_C + 1e-9
                && state.pressureMPa() <= MAX_MPA + 1e-9;
    }

    /** A rated buffer of {@code capacity}. Callers do not pass the ambient cap. */
    public static FluidBuffer buffer(int capacity) {
        return new FluidBuffer(capacity, MAX_C, MAX_MPA);
    }
}
