package io.github.ezequiel24123z.grindless.process;

import net.minecraft.nbt.CompoundTag;
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

    private static final String KEY_TEMPERATURE = "Temperature";
    private static final String KEY_PRESSURE = "Pressure";
    private static final String KEY_ATMOSPHERE = "Atmosphere";
    private static final String KEY_FIELD = "Field";
    private static final String KEY_AGITATION = "Agitation";
    private static final String KEY_CATALYST = "Catalyst";

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

    /**
     * Writes this state into {@code tag}. Enums go by name, not ordinal, so reordering them
     * cannot change what a saved machine believes it is doing.
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble(KEY_TEMPERATURE, temperature);
        tag.putDouble(KEY_PRESSURE, pressure);
        tag.putString(KEY_ATMOSPHERE, atmosphere.name());
        tag.putString(KEY_FIELD, field.name());
        tag.putString(KEY_AGITATION, agitation.name());
        if (catalyst != null) {
            tag.putString(KEY_CATALYST, catalyst.toString());
        }
        return tag;
    }

    /**
     * Reads a state written by {@link #save}, falling back to {@link #AMBIENT} for anything
     * missing, unknown or not a finite number. A throw during world load reads to the player as a
     * corrupt save, so a value from a different version degrades to idle instead.
     */
    public static ConditionState load(CompoundTag tag) {
        return new ConditionState(
                finite(tag, KEY_TEMPERATURE, AMBIENT.temperature),
                finite(tag, KEY_PRESSURE, AMBIENT.pressure),
                named(Atmosphere.values(), tag.getString(KEY_ATMOSPHERE), AMBIENT.atmosphere),
                named(ProcessField.values(), tag.getString(KEY_FIELD), AMBIENT.field),
                named(Agitation.values(), tag.getString(KEY_AGITATION), AMBIENT.agitation),
                tag.contains(KEY_CATALYST) ? ResourceLocation.tryParse(tag.getString(KEY_CATALYST)) : null);
    }

    private static double finite(CompoundTag tag, String key, double fallback) {
        if (!tag.contains(key)) {
            return fallback;
        }
        double value = tag.getDouble(key);
        return Double.isFinite(value) ? value : fallback;
    }

    private static <E extends Enum<E>> E named(E[] values, String name, E fallback) {
        for (E value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return fallback;
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
