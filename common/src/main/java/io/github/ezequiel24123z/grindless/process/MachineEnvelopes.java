package io.github.ezequiel24123z.grindless.process;

/**
 * The envelope table from {@code docs/MACHINES.md}, encoded.
 *
 * <p>Every range here is transcribed from the <i>Machines are condition envelopes</i> table rather
 * than invented, so this class is also the check that the condition model can actually express the
 * design. Where the document says "ambient" the range is a point at room temperature or
 * atmospheric pressure; where it says "any", the range spans the machine's whole working span.
 *
 * <p>These are <b>MK I</b> envelopes. A chassis mark widens them (ADR-0027), and the per-mark
 * values belong to the machine definitions that arrive with the block entity framework. This class
 * is the floor those build on, not the whole story.
 */
public final class MachineEnvelopes {

    /** Drying, calcining, roasting. The cheapest heat in the game. */
    public static final ConditionEnvelope KILN = ConditionEnvelope.builder()
            .temperature(100.0, 900.0)
            .atmospheres(Atmosphere.OXIDISING, Atmosphere.REDUCING, Atmosphere.INERT)
            .agitation(Agitation.FLUIDISED)
            .build();

    /** The metallurgy workhorse: 1200–3500 °C in any atmosphere. */
    public static final ConditionEnvelope ARC_FURNACE = ConditionEnvelope.builder()
            .temperature(1200.0, 3500.0)
            .atmospheres(Atmosphere.OXIDISING, Atmosphere.REDUCING, Atmosphere.INERT)
            .fields(ProcessField.ELECTRIC)
            .build();

    /** Clean, precise and electrically efficient; the machine zone refining needs. */
    public static final ConditionEnvelope INDUCTION_FURNACE = ConditionEnvelope.builder()
            .temperature(200.0, 2000.0)
            .atmospheres(Atmosphere.INERT, Atmosphere.REDUCING)
            .build();

    /** Stirred liquid-phase reactions with a catalyst slot. MK I also hosts contact oxidation. */
    public static final ConditionEnvelope CHEMICAL_REACTOR = ConditionEnvelope.builder()
            .temperature(-20.0, 500.0)
            .pressure(0.1, 2.0)
            .atmospheres(Atmosphere.INERT, Atmosphere.OXIDISING, Atmosphere.REDUCING)
            .agitation(Agitation.STIRRED)
            .catalystSlot()
            .build();

    /** Wet beneficiation. Room temperature today; the range still contains a 90 °C leach. */
    public static final ConditionEnvelope CHEMICAL_WASHER = ConditionEnvelope.builder()
            .temperature(10.0, 100.0)
            .agitation(Agitation.STIRRED)
            .build();

    /** Aqueous electrolysis. Chlor-alkali at 60 °C fits; molten salt does not (ADR-0077). */
    public static final ConditionEnvelope ELECTROLYSIS_CELL = ConditionEnvelope.builder()
            .temperature(10.0, 80.0)
            .fields(ProcessField.ELECTRIC)
            .build();

    /**
     * Air separation. The documented cryogenic cut still fits; MK I holds ambient and
     * skims oxygen only until nitrogen has a sink (ADR-0077).
     */
    public static final ConditionEnvelope ATMOSPHERIC_INTAKE = ConditionEnvelope.builder()
            .temperature(-200.0, 40.0)
            .pressure(0.1, 1.0)
            .build();

    /** High-pressure hydrothermal chemistry, and the only machine that can hold the 20 MPa the
     * Haber synthesis needs. */
    public static final ConditionEnvelope AUTOCLAVE = ConditionEnvelope.builder()
            .temperature(100.0, 400.0)
            .pressure(0.1, 25.0)
            .atmospheres(Atmosphere.INERT, Atmosphere.REDUCING)
            .agitation(Agitation.STIRRED)
            .catalystSlot()
            .build();

    /** The cold end: liquefaction and separation by boiling point. */
    public static final ConditionEnvelope CRYO_CHAMBER = ConditionEnvelope.builder()
            .temperature(-270.0, 0.0)
            .pressure(0.1, 25.0)
            .atmospheres(Atmosphere.INERT, Atmosphere.VACUUM)
            .build();

    /** No oxidation, thin films, and higher purity than any process run in air. */
    public static final ConditionEnvelope VACUUM_CHAMBER = ConditionEnvelope.builder()
            .temperature(20.0, 1500.0)
            .pressure(0.0, 0.1)
            .atmospheres(Atmosphere.VACUUM, Atmosphere.INERT)
            .build();

    /** Dissociation into elements, held together by a magnetic field. */
    public static final ConditionEnvelope PLASMA_CHAMBER = ConditionEnvelope.builder()
            .temperature(5000.0, 100_000.0)
            .atmospheres(Atmosphere.VACUUM, Atmosphere.INERT)
            .fields(ProcessField.MAGNETIC)
            .build();

    /**
     * Transmutation by particle energy rather than by heat.
     *
     * <p>The design document gives this machine no temperature or pressure, which is not an
     * omission: an accelerator's capability is its ring circumference, and that belongs to the
     * multiblock layer. Its envelope is therefore ambient, and the recipes it hosts name a field
     * rather than a temperature.
     */
    public static final ConditionEnvelope ACCELERATOR = ConditionEnvelope.builder()
            .atmospheres(Atmosphere.VACUUM)
            .fields(ProcessField.MAGNETIC, ProcessField.ELECTRIC)
            .build();

    private MachineEnvelopes() {
    }
}
