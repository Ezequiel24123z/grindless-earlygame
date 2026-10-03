package io.github.ezequiel24123z.grindless.fluid;

/**
 * One amount of one substance at a temperature and a pressure (ADR-0015, ADR-0062).
 *
 * <p>Volume is millibuckets. 1000 mB is one bucket {@code B} in {@code PROCESSES.md}. Empty is
 * zero millibuckets, not a null. No Minecraft imports: {@code VerifyFluid} checks the arithmetic
 * without a world.
 *
 * @param id            {@code namespace:path}, such as {@code minecraft:water}
 * @param millibuckets  amount at the stack's own temperature and pressure
 * @param temperatureC  degrees Celsius
 * @param pressureMPa   megapascals
 */
public record FluidState(String id, int millibuckets, double temperatureC, double pressureMPa) {

    public static final FluidState EMPTY = new FluidState("", 0, FluidLogic.AMBIENT_C,
            FluidLogic.AMBIENT_MPA);

    public FluidState {
        if (millibuckets < 0) {
            throw new IllegalArgumentException("volume cannot be negative");
        }
        if (millibuckets == 0) {
            id = "";
            temperatureC = FluidLogic.AMBIENT_C;
            pressureMPa = FluidLogic.AMBIENT_MPA;
        } else if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a non-empty stack needs an id");
        }
    }

    public static FluidState of(String id, int millibuckets) {
        return millibuckets <= 0 ? EMPTY
                : new FluidState(id, millibuckets, FluidLogic.AMBIENT_C, FluidLogic.AMBIENT_MPA);
    }

    public static FluidState of(String id, int millibuckets, double temperatureC, double pressureMPa) {
        return millibuckets <= 0 ? EMPTY : new FluidState(id, millibuckets, temperatureC, pressureMPa);
    }

    public boolean isEmpty() {
        return millibuckets == 0;
    }

    public boolean is(String substance) {
        return !isEmpty() && id.equals(substance);
    }

    /** A copy holding {@code amount} millibuckets of the same substance and state. */
    public FluidState withAmount(int amount) {
        return amount <= 0 ? EMPTY : new FluidState(id, amount, temperatureC, pressureMPa);
    }

    /** Mixes two stacks of the same substance; heat and pressure weight by volume. */
    public FluidState merge(FluidState other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        if (!id.equals(other.id)) {
            throw new IllegalArgumentException("cannot mix " + id + " with " + other.id);
        }
        int total = millibuckets + other.millibuckets;
        double t = (temperatureC * millibuckets + other.temperatureC * other.millibuckets) / total;
        double p = (pressureMPa * millibuckets + other.pressureMPa * other.millibuckets) / total;
        return new FluidState(id, total, t, p);
    }
}
