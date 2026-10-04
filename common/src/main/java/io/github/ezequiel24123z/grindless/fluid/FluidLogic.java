package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * T1 fluid numbers and rules, independent of a world (ADR-0015, ADR-0062).
 *
 * <p>The Clay Conduit is the first belt of fluids: unpowered, ambient liquids only, downhill
 * or level. Gases need pressure a T1 pipe does not have, so captured CO and SO₂ sit in a tank.
 * CO burns; SO₂ does not (ADR-0065). Heat is conserved on mix; it is not simulated as a solver.
 */
public final class FluidLogic {

    /** One bucket in millibuckets. */
    public static final int BUCKET = 1000;

    /** Reference state in {@code PROCESSES.md}: gases are quoted here unless a process says else. */
    public static final double AMBIENT_C = 20.0;
    public static final double AMBIENT_MPA = 0.1;

    /** Basic Tank and Clay Conduit refuse anything hotter than this. */
    public static final double AMBIENT_MAX_C = 40.0;

    public static final int TANK_CAPACITY = 16 * BUCKET;
    public static final int CONDUIT_CAPACITY = BUCKET;
    public static final int MACHINE_CAPACITY = 2 * BUCKET;

    /** Hand Pump: 20 mB/t is 400 mB/s, a bucket every 2.5 s. Slow, unpowered, enough to fill a mill. */
    public static final int PUMP_MB_PER_TICK = 20;

    /** Clay Conduit: one tick moves 50 mB toward the facing neighbour if that neighbour is not uphill. */
    public static final int CONDUIT_MB_PER_TICK = 50;

    /** Wet mill: 0.5 B water per raw (PROCESSES B2 water, without the washer). */
    public static final int WET_WATER_MB = BUCKET / 2;

    /** R1 names 1 B CO. Captured into a tank when one will take it; otherwise vented. */
    public static final int CO_MB = BUCKET;

    /** 1 B of captured CO burns 400 ticks — 20 s of F1, a quarter of a coal. */
    public static final int CO_BURN_TICKS = 400;

    /** R2 roast names 1 B SO₂. Captured like CO; the Thermal Generator must not burn it. */
    public static final int SO2_MB = BUCKET;

    public static final String WATER = "minecraft:water";
    public static final String CARBON_MONOXIDE = "grindless:carbon_monoxide";
    public static final String SULFUR_DIOXIDE = "grindless:sulfur_dioxide";
    public static final String SULFUR_TRIOXIDE = "grindless:sulfur_trioxide";
    public static final String SULFURIC_ACID = "grindless:sulfuric_acid";

    private FluidLogic() {
    }

    public static boolean isAmbient(FluidState state) {
        return state.isEmpty()
                || (state.temperatureC() <= AMBIENT_MAX_C + 1e-9
                && state.pressureMPa() <= AMBIENT_MPA + 1e-9);
    }

    /**
     * Gases at the reference state. Water is liquid below 100 °C at ambient pressure.
     * Carbon monoxide and sulfur dioxide are gases at ambient. Unknown ids default to liquid
     * so a T1 pipe will still move a foreign fluid that arrived through Forge interop.
     */
    public static boolean isGas(FluidState state) {
        if (state.isEmpty()) {
            return false;
        }
        if (WATER.equals(state.id())) {
            return state.temperatureC() >= 100.0 - 1e-9;
        }
        return CARBON_MONOXIDE.equals(state.id()) || SULFUR_DIOXIDE.equals(state.id())
                || SULFUR_TRIOXIDE.equals(state.id());
    }

    public static boolean isLiquid(FluidState state) {
        return !state.isEmpty() && !isGas(state);
    }

    /** Clay Conduit: ambient liquid, and the destination is not uphill. */
    public static boolean canGravityFlow(FluidState state, int fromY, int toY) {
        return isLiquid(state) && isAmbient(state) && toY <= fromY;
    }

    /**
     * How much of {@code incoming} a buffer holding {@code current} can take.
     *
     * @return millibuckets accepted, 0 if the substance, heat or pressure is refused
     */
    public static int accepted(FluidState current, FluidState incoming, int capacity, double maxC,
                               double maxP) {
        if (incoming == null || incoming.isEmpty() || incoming.temperatureC() > maxC + 1e-9
                || incoming.pressureMPa() > maxP + 1e-9) {
            return 0;
        }
        if (current.isEmpty()) {
            return Math.min(incoming.millibuckets(), capacity);
        }
        if (!current.id().equals(incoming.id())) {
            return 0;
        }
        return Math.min(incoming.millibuckets(), Math.max(0, capacity - current.millibuckets()));
    }

    public static FluidState insert(FluidState current, FluidState incoming, int capacity,
                                    double maxC, double maxP) {
        int take = accepted(current, incoming, capacity, maxC, maxP);
        if (take <= 0) {
            return current;
        }
        return current.merge(incoming.withAmount(take));
    }

    public static FluidState leftover(FluidState current, FluidState incoming, int capacity,
                                      double maxC, double maxP) {
        int take = accepted(current, incoming, capacity, maxC, maxP);
        if (incoming == null || incoming.isEmpty()) {
            return EMPTY_OR(incoming);
        }
        return incoming.withAmount(incoming.millibuckets() - take);
    }

    private static FluidState EMPTY_OR(FluidState incoming) {
        return incoming == null ? FluidState.EMPTY : incoming;
    }

    public static int extractable(FluidState current, String want, int max) {
        if (current.isEmpty() || max <= 0) {
            return 0;
        }
        if (want != null && !want.isEmpty() && !current.id().equals(want)) {
            return 0;
        }
        return Math.min(max, current.millibuckets());
    }

    public static MachineStatus conduitStatus(boolean moving) {
        return moving ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }

    public static MachineStatus pumpStatus(boolean hasSource, boolean moving) {
        if (!hasSource) {
            return MachineStatus.STARVED;
        }
        return moving ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }

    public static MachineStatus tankStatus(boolean holding) {
        return holding ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }

    /** How many millibuckets of {@code id} sit in {@code neighbours}. */
    public static int available(Iterable<FluidState> neighbours, String id) {
        if (neighbours == null || id == null || id.isBlank()) {
            return 0;
        }
        int total = 0;
        for (FluidState state : neighbours) {
            if (state != null && state.is(id)) {
                total += state.millibuckets();
            }
        }
        return total;
    }

    public static boolean hasAtLeast(Iterable<FluidState> neighbours, String id, int millibuckets) {
        return available(neighbours, id) >= millibuckets;
    }
}
