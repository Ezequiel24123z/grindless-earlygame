package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.process.Agitation;
import io.github.ezequiel24123z.grindless.process.Atmosphere;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ProcessField;

/**
 * A machine's capability ceiling, MK I through MK V.
 *
 * <p>A mark is not a different block. A Chassis Upgrade Kit applies it in place, keeping the
 * machine's position, contents, configuration and connections, so nothing is ever rebuilt or
 * re-piped.
 *
 * <p>The defining property of a mark is that it <b>widens the condition envelope</b> (ADR-0027).
 * Because recipes are selected by conditions rather than held by machines, a wider envelope unlocks
 * <em>recipes</em> rather than adding a speed number — which is what makes upgrading a goal instead
 * of a tax. An MK I Arc Furnace reaches 1800 °C and an MK V reaches 3500 °C; they are the same
 * machine with two envelopes.
 */
public enum ChassisMark {

    MK_I("MK I", 1, FluxTier.F1, 0.0),
    MK_II("MK II", 2, FluxTier.F3, 0.30),
    MK_III("MK III", 3, FluxTier.F5, 0.60),
    MK_IV("MK IV", 4, FluxTier.F7, 0.825),
    MK_V("MK V", 6, FluxTier.F9, 1.0);

    private static final ChassisMark[] VALUES = values();

    private final String displayName;
    private final int upgradeSlots;
    private final FluxTier maxFluxTier;
    private final double width;

    ChassisMark(String displayName, int upgradeSlots, FluxTier maxFluxTier, double width) {
        this.displayName = displayName;
        this.upgradeSlots = upgradeSlots;
        this.maxFluxTier = maxFluxTier;
        this.width = width;
    }

    public String displayName() {
        return displayName;
    }

    /** How many upgrades this chassis can hold. */
    public int upgradeSlots() {
        return upgradeSlots;
    }

    /** The highest Flux tier this chassis can accept power from. */
    public FluxTier maxFluxTier() {
        return maxFluxTier;
    }

    /**
     * How far along the machine's narrow-to-full range this mark sits, in {@code [0, 1]}.
     *
     * <p>The values are not evenly spaced. They were chosen to reproduce the Arc Furnace ladder
     * documented in {@code MACHINES.md} — 1800, 2300, 2800, 3200, 3500 °C — whose steps shorten as
     * they approach the machine's physical ceiling, because the last few hundred degrees are the
     * expensive ones.
     */
    public double width() {
        return width;
    }

    /** The next mark up, or this one if already at the top. */
    public ChassisMark next() {
        return this == MK_V ? this : VALUES[ordinal() + 1];
    }

    /**
     * Interpolates between the machine's narrowest and widest envelopes.
     *
     * <p>Giving every mark a sensible envelope for free matters: roughly thirty machines at five
     * marks each would otherwise be a hundred and fifty envelopes to write and keep consistent. A
     * machine whose ladder is not a smooth interpolation is free to declare its marks explicitly
     * instead; this is the default, not a constraint.
     *
     * <p>Discrete capabilities — which atmospheres, fields, agitation modes and whether there is a
     * catalyst slot — are taken from the <em>full</em> envelope at MK V and from the narrow one
     * below it, because a chamber either can hold a vacuum or it cannot; there is no half of one.
     */
    public ConditionEnvelope widen(ConditionEnvelope narrow, ConditionEnvelope full) {
        ConditionEnvelope discrete = this == MK_V ? full : narrow;
        ConditionEnvelope.Builder builder = ConditionEnvelope.builder()
                .temperature(
                        lerp(narrow.minTemperature(), full.minTemperature()),
                        lerp(narrow.maxTemperature(), full.maxTemperature()))
                .pressure(
                        lerp(narrow.minPressure(), full.minPressure()),
                        lerp(narrow.maxPressure(), full.maxPressure()))
                .atmospheres(discrete.atmospheres().toArray(new Atmosphere[0]))
                .fields(discrete.fields().toArray(new ProcessField[0]))
                .agitation(discrete.agitations().toArray(new Agitation[0]));
        if (discrete.hasCatalystSlot()) {
            builder.catalystSlot();
        }
        return builder.build();
    }

    private double lerp(double from, double to) {
        return from + (to - from) * width;
    }
}
