package io.github.ezequiel24123z.grindless.process;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The conditions a process requires: the {@code conditions} half of
 * {@code inputs + conditions + time -> outputs} (ADR-0020).
 *
 * <h2>The omission rule</h2>
 *
 * <p><b>An unwritten condition is not a condition.</b> A dimension this object does not name
 * carries no requirement at all, and a machine does not have to be able to supply it for the
 * process to run. The sulfide roast names a temperature and an atmosphere and nothing else, so it
 * runs in a Kiln, an Arc Furnace or a Vacuum Furnace alike.
 *
 * <p>That is why unnamed dimensions are <em>absent</em> here rather than stored as a full range.
 * The two behave identically when matching, but only absence can answer "how many conditions does
 * this recipe actually name?", which is what keeps a six-dimensional condition space from showing
 * the player six dials per recipe. In practice no recipe names more than three.
 *
 * <h2>Two questions, two methods</h2>
 *
 * <p>{@link #checkEnvelope(ConditionEnvelope)} asks whether a machine <em>could ever</em> run this
 * process — the question the Process Atlas asks. {@link #evaluate(ConditionState)} asks how well a
 * machine is running it <em>right now</em>. Both return a {@link ConditionReport} that names what
 * is wrong rather than a boolean (ADR-0041).
 */
public final class ProcessConditions {

    /** A process with no requirements at all; runs anywhere, always at full speed. */
    public static final ProcessConditions NONE = builder().build();

    /**
     * Speed of a process whose <em>optional</em> catalyst is absent.
     *
     * <p>Half speed is a real incentive to build the catalyst line without ever gating the process
     * behind it, which is the point of an optional catalyst: a process you already run gets better
     * when you can finally make its catalyst, with no new machine and no new research.
     */
    public static final double UNCATALYSED_EFFICIENCY = 0.5;

    private final ConditionBand temperature;
    private final ConditionBand pressure;
    private final Atmosphere atmosphere;
    private final ProcessField field;
    private final Agitation agitation;
    private final ResourceLocation catalyst;
    private final boolean catalystRequired;

    private ProcessConditions(Builder builder) {
        this.temperature = builder.temperature;
        this.pressure = builder.pressure;
        this.atmosphere = builder.atmosphere;
        this.field = builder.field;
        this.agitation = builder.agitation;
        this.catalyst = builder.catalyst;
        this.catalystRequired = builder.catalystRequired;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConditionBand temperature() {
        return temperature;
    }

    public ConditionBand pressure() {
        return pressure;
    }

    public Atmosphere atmosphere() {
        return atmosphere;
    }

    public ProcessField field() {
        return field;
    }

    public Agitation agitation() {
        return agitation;
    }

    /** The catalyst this process uses, or {@code null} if it uses none. */
    public ResourceLocation catalyst() {
        return catalyst;
    }

    /**
     * Whether the catalyst is mandatory.
     *
     * <p>Some processes require a catalyst and some are merely faster with one, so a named catalyst
     * is not automatically a requirement. An optional catalyst that is absent costs speed; a
     * required one that is absent stops the process.
     */
    public boolean isCatalystRequired() {
        return catalystRequired;
    }

    /** The dimensions this process actually names, in display order. */
    public List<ConditionDimension> namedDimensions() {
        List<ConditionDimension> named = new ArrayList<>(3);
        if (temperature != null) {
            named.add(ConditionDimension.TEMPERATURE);
        }
        if (pressure != null) {
            named.add(ConditionDimension.PRESSURE);
        }
        if (atmosphere != null) {
            named.add(ConditionDimension.ATMOSPHERE);
        }
        if (catalyst != null) {
            named.add(ConditionDimension.CATALYST);
        }
        if (field != null) {
            named.add(ConditionDimension.FIELD);
        }
        if (agitation != null) {
            named.add(ConditionDimension.AGITATION);
        }
        return named;
    }

    /**
     * Whether a machine with this envelope could ever run the process, at any setting.
     *
     * <p>Unnamed dimensions are skipped entirely, so a machine is never rejected for being unable
     * to pull a vacuum that the recipe never asked for.
     *
     * @return {@link ConditionReport#OPTIMAL} when the machine can reach the optimum,
     *         a degraded report when it can only reach the tolerance zone, or
     *         {@link ConditionFault#OUTSIDE_ENVELOPE} naming the dimension it cannot reach
     */
    public ConditionReport checkEnvelope(ConditionEnvelope envelope) {
        if (temperature != null) {
            if (!temperature.reachableWithin(envelope.minTemperature(), envelope.maxTemperature())) {
                return ConditionReport.outsideEnvelope(ConditionDimension.TEMPERATURE);
            }
        }
        if (pressure != null) {
            if (!pressure.reachableWithin(envelope.minPressure(), envelope.maxPressure())) {
                return ConditionReport.outsideEnvelope(ConditionDimension.PRESSURE);
            }
        }
        if (atmosphere != null && !envelope.supportsAtmosphere(atmosphere)) {
            return ConditionReport.outsideEnvelope(ConditionDimension.ATMOSPHERE);
        }
        if (field != null && !envelope.supportsField(field)) {
            return ConditionReport.outsideEnvelope(ConditionDimension.FIELD);
        }
        if (agitation != null && !envelope.supportsAgitation(agitation)) {
            return ConditionReport.outsideEnvelope(ConditionDimension.AGITATION);
        }
        if (catalystRequired && !envelope.hasCatalystSlot()) {
            return ConditionReport.outsideEnvelope(ConditionDimension.CATALYST);
        }

        // Reachable, but possibly only into the tolerance zone — a machine that can run the
        // process but never at full speed, which the Atlas should say out loud rather than
        // present as equivalent to a machine that can.
        double best = 1.0;
        ConditionDimension limiting = null;
        if (temperature != null) {
            double value = temperature.bestEfficiencyWithin(
                    envelope.minTemperature(), envelope.maxTemperature());
            if (value < best) {
                best = value;
                limiting = ConditionDimension.TEMPERATURE;
            }
        }
        if (pressure != null) {
            double value = pressure.bestEfficiencyWithin(
                    envelope.minPressure(), envelope.maxPressure());
            if (value < best) {
                best = value;
                limiting = ConditionDimension.PRESSURE;
            }
        }
        // An optional catalyst in a machine with nowhere to put one: still runs, just never
        // at full speed. Only a *required* catalyst puts the machine outside the envelope.
        if (catalyst != null && !catalystRequired && !envelope.hasCatalystSlot()
                && UNCATALYSED_EFFICIENCY < best) {
            best = UNCATALYSED_EFFICIENCY;
            limiting = ConditionDimension.CATALYST;
        }
        return limiting == null
                ? ConditionReport.OPTIMAL
                : ConditionReport.degraded(limiting, best);
    }

    /**
     * How well a machine currently holding {@code state} is running the process.
     *
     * <p>The speed multiplier is the <em>minimum</em> across the named dimensions rather than their
     * product. Two dimensions slightly off should not compound into a crawl, and the minimum has a
     * single identifiable cause the machine can report — which is the difference between "running
     * at 62 %, limited by Temperature" and an unexplained number.
     *
     * <p>Degradation only ever costs speed, never yield (ADR-0040).
     */
    public ConditionReport evaluate(ConditionState state) {
        double efficiency = 1.0;
        ConditionDimension limiting = null;

        if (temperature != null) {
            if (!temperature.admits(state.temperature())) {
                return ConditionReport.failed(temperature.isBelow(state.temperature())
                        ? ConditionFault.TOO_COLD
                        : ConditionFault.TOO_HOT);
            }
            double value = temperature.efficiencyAt(state.temperature());
            if (value < efficiency) {
                efficiency = value;
                limiting = ConditionDimension.TEMPERATURE;
            }
        }

        if (pressure != null) {
            if (!pressure.admits(state.pressure())) {
                return ConditionReport.failed(pressure.isBelow(state.pressure())
                        ? ConditionFault.PRESSURE_TOO_LOW
                        : ConditionFault.PRESSURE_TOO_HIGH);
            }
            double value = pressure.efficiencyAt(state.pressure());
            if (value < efficiency) {
                efficiency = value;
                limiting = ConditionDimension.PRESSURE;
            }
        }

        if (atmosphere != null && state.atmosphere() != atmosphere) {
            return ConditionReport.failed(ConditionFault.WRONG_ATMOSPHERE);
        }
        if (field != null && state.field() != field) {
            return ConditionReport.failed(ConditionFault.WRONG_FIELD);
        }
        if (agitation != null && state.agitation() != agitation) {
            return ConditionReport.failed(ConditionFault.WRONG_AGITATION);
        }

        if (catalyst != null && !catalyst.equals(state.catalyst())) {
            if (catalystRequired) {
                return ConditionReport.failed(ConditionFault.MISSING_CATALYST);
            }
            // Optional catalyst, absent: the process runs, at the price the catalyst would
            // have saved. This is what makes producing a catalyst an upgrade to a process you
            // already run, rather than a gate on running it at all.
            if (UNCATALYSED_EFFICIENCY < efficiency) {
                efficiency = UNCATALYSED_EFFICIENCY;
                limiting = ConditionDimension.CATALYST;
            }
        }

        return limiting == null
                ? ConditionReport.OPTIMAL
                : ConditionReport.degraded(limiting, efficiency);
    }

    @Override
    public String toString() {
        List<ConditionDimension> named = namedDimensions();
        return named.isEmpty() ? "no conditions" : named.toString();
    }

    /** Builds a condition set. Every dimension left unset is left <em>unrequired</em>. */
    public static final class Builder {

        private ConditionBand temperature;
        private ConditionBand pressure;
        private Atmosphere atmosphere;
        private ProcessField field;
        private Agitation agitation;
        private ResourceLocation catalyst;
        private boolean catalystRequired;

        private Builder() {
        }

        /** Temperature in degrees Celsius, with the default ±15 % band. */
        public Builder temperature(double celsius) {
            this.temperature = ConditionBand.relative(celsius);
            return this;
        }

        /** Temperature with an explicit half-width, as in {@code T 1420 ±5} for zone refining. */
        public Builder temperature(double celsius, double halfWidth) {
            this.temperature = ConditionBand.absolute(celsius, halfWidth);
            return this;
        }

        /** Pressure in MPa, with the default ±15 % band. */
        public Builder pressure(double megaPascals) {
            this.pressure = ConditionBand.relative(megaPascals);
            return this;
        }

        /** Pressure with an explicit half-width. */
        public Builder pressure(double megaPascals, double halfWidth) {
            this.pressure = ConditionBand.absolute(megaPascals, halfWidth);
            return this;
        }

        public Builder atmosphere(Atmosphere value) {
            this.atmosphere = value;
            return this;
        }

        public Builder field(ProcessField value) {
            this.field = value;
            return this;
        }

        public Builder agitation(Agitation value) {
            this.agitation = value;
            return this;
        }

        /** A catalyst the process cannot run without. */
        public Builder requiredCatalyst(ResourceLocation value) {
            this.catalyst = value;
            this.catalystRequired = true;
            return this;
        }

        /** A catalyst that makes the process faster but is not needed to run it. */
        public Builder optionalCatalyst(ResourceLocation value) {
            this.catalyst = value;
            this.catalystRequired = false;
            return this;
        }

        public ProcessConditions build() {
            return new ProcessConditions(this);
        }
    }
}
