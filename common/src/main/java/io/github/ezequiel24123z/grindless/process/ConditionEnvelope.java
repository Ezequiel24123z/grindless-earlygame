package io.github.ezequiel24123z.grindless.process;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a machine <em>can</em> hold: the range of conditions it is able to maintain.
 *
 * <p>This is the whole definition of a machine in Grindless. A machine does not own recipes; a
 * recipe runs in any machine whose envelope contains the conditions it names. That is what lets
 * roughly thirty machines host the entire recipe graph, because the recipe space is the product of
 * machines and conditions rather than a list.
 *
 * <p>An envelope belongs to a (machine, chassis mark) pair, not to a machine alone: a mark's
 * defining property is that it <em>widens</em> this envelope, so an MK I Arc Furnace reaching
 * 1800 °C and an MK V reaching 3500 °C are the same machine with two envelopes (ADR-0027). The
 * marks themselves are declared by the machine definitions in a later step; this class only
 * describes one.
 */
public final class ConditionEnvelope {

    private final double minTemperature;
    private final double maxTemperature;
    private final double minPressure;
    private final double maxPressure;
    private final Set<Atmosphere> atmospheres;
    private final Set<ProcessField> fields;
    private final Set<Agitation> agitations;
    private final boolean catalystSlot;

    private ConditionEnvelope(Builder builder) {
        this.minTemperature = builder.minTemperature;
        this.maxTemperature = builder.maxTemperature;
        this.minPressure = builder.minPressure;
        this.maxPressure = builder.maxPressure;
        this.atmospheres = Collections.unmodifiableSet(EnumSet.copyOf(builder.atmospheres));
        this.fields = Collections.unmodifiableSet(EnumSet.copyOf(builder.fields));
        this.agitations = Collections.unmodifiableSet(EnumSet.copyOf(builder.agitations));
        this.catalystSlot = builder.catalystSlot;
    }

    public static Builder builder() {
        return new Builder();
    }

    public double minTemperature() {
        return minTemperature;
    }

    public double maxTemperature() {
        return maxTemperature;
    }

    public double minPressure() {
        return minPressure;
    }

    public double maxPressure() {
        return maxPressure;
    }

    public Set<Atmosphere> atmospheres() {
        return atmospheres;
    }

    public Set<ProcessField> fields() {
        return fields;
    }

    public Set<Agitation> agitations() {
        return agitations;
    }

    /** Whether this machine has a catalyst slot at all. */
    public boolean hasCatalystSlot() {
        return catalystSlot;
    }

    public boolean supportsAtmosphere(Atmosphere atmosphere) {
        return atmospheres.contains(atmosphere);
    }

    public boolean supportsField(ProcessField field) {
        return fields.contains(field);
    }

    public boolean supportsAgitation(Agitation agitation) {
        return agitations.contains(agitation);
    }

    /**
     * Builds an envelope. Defaults describe the least capable machine that could exist — ambient
     * temperature and pressure, air only, no field, no stirring, no catalyst slot — so every
     * capability a machine has must be stated rather than inherited by accident.
     */
    public static final class Builder {

        private double minTemperature = 20.0;
        private double maxTemperature = 20.0;
        private double minPressure = 0.1;
        private double maxPressure = 0.1;
        private final Set<Atmosphere> atmospheres = EnumSet.of(Atmosphere.AIR);
        private final Set<ProcessField> fields = EnumSet.of(ProcessField.NONE);
        private final Set<Agitation> agitations = EnumSet.of(Agitation.STATIC);
        private boolean catalystSlot;

        private Builder() {
        }

        /** Temperature range in degrees Celsius. */
        public Builder temperature(double min, double max) {
            require(min <= max, "temperature range is inverted");
            this.minTemperature = min;
            this.maxTemperature = max;
            return this;
        }

        /** Pressure range in MPa. */
        public Builder pressure(double min, double max) {
            require(min <= max, "pressure range is inverted");
            this.minPressure = min;
            this.maxPressure = max;
            return this;
        }

        /** Adds atmospheres this machine can establish. {@link Atmosphere#AIR} is always present. */
        public Builder atmospheres(Atmosphere... values) {
            Collections.addAll(this.atmospheres, values);
            return this;
        }

        /** Adds fields this machine can apply. {@link ProcessField#NONE} is always present. */
        public Builder fields(ProcessField... values) {
            Collections.addAll(this.fields, values);
            return this;
        }

        /** Adds agitation modes. {@link Agitation#STATIC} is always present. */
        public Builder agitation(Agitation... values) {
            Collections.addAll(this.agitations, values);
            return this;
        }

        /** Gives the machine a catalyst slot. */
        public Builder catalystSlot() {
            this.catalystSlot = true;
            return this;
        }

        public ConditionEnvelope build() {
            return new ConditionEnvelope(this);
        }

        private static void require(boolean condition, String message) {
            if (!condition) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
