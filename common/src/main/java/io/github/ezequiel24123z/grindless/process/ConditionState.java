package io.github.ezequiel24123z.grindless.process;

import net.minecraft.resources.ResourceLocation;

/**
 * What a machine is <em>currently doing</em>, as opposed to {@link ConditionEnvelope}, which is
 * what it could do.
 *
 * <p>Keeping the two apart is what lets the game answer two different questions: the Process Atlas
 * asks "which machines could run this recipe?" of the envelope, and a running machine asks "how
 * well am I running it?" of the state.
 *
 * @param temperature current temperature in degrees Celsius
 * @param pressure    current pressure in MPa
 * @param atmosphere  the gas currently in the chamber, never {@code null}
 * @param field       the field currently applied, never {@code null}
 * @param agitation   the agitation currently applied, never {@code null}
 * @param catalyst    the catalyst currently installed, or {@code null} for an empty or absent slot
 */
public record ConditionState(
        double temperature,
        double pressure,
        Atmosphere atmosphere,
        ProcessField field,
        Agitation agitation,
        ResourceLocation catalyst) {

    /** Room temperature, atmospheric pressure, air, nothing applied — an idle T0 machine. */
    public static final ConditionState AMBIENT = new ConditionState(
            20.0, 0.1, Atmosphere.AIR, ProcessField.NONE, Agitation.STATIC, null);

    public ConditionState {
        if (atmosphere == null || field == null || agitation == null) {
            throw new IllegalArgumentException("atmosphere, field and agitation are always present");
        }
    }

    /** Whether a catalyst is installed. */
    public boolean hasCatalyst() {
        return catalyst != null;
    }

    public ConditionState withTemperature(double value) {
        return new ConditionState(value, pressure, atmosphere, field, agitation, catalyst);
    }

    public ConditionState withPressure(double value) {
        return new ConditionState(temperature, value, atmosphere, field, agitation, catalyst);
    }

    public ConditionState withAtmosphere(Atmosphere value) {
        return new ConditionState(temperature, pressure, value, field, agitation, catalyst);
    }

    public ConditionState withField(ProcessField value) {
        return new ConditionState(temperature, pressure, atmosphere, value, agitation, catalyst);
    }

    public ConditionState withAgitation(Agitation value) {
        return new ConditionState(temperature, pressure, atmosphere, field, value, catalyst);
    }

    public ConditionState withCatalyst(ResourceLocation value) {
        return new ConditionState(temperature, pressure, atmosphere, field, agitation, value);
    }
}
