package io.github.ezequiel24123z.grindless.recipe;

/**
 * The machine a {@link ProcessRecipe} runs in.
 *
 * <p>A recipe names a family, not a block. Any block of that family whose envelope covers the
 * recipe's conditions can host it (ADR-0019, ADR-0020). Slice A shipped two families; Slice D
 * adds Press and Assembler; the Kiln adds roast without forking the type (ADR-0065). The
 * Wire Mill adds drawing as its own family (ADR-0074). The Chemical Reactor adds
 * the contact process (ADR-0075). The Washer adds B2 (ADR-0076). Electrolysis and
 * the Intake add the gas line (ADR-0077).
 */
public enum MachineFamily {

    /** Dry mill. B1: 1 u raw → 2 u crushed. */
    PULVERIZER,

    /** Carbothermic reduction. R1: graded feed + carbon → ingot + slag; CO vents.
     * R2 reduce: oxide + carbon → ingot + slag; no CO. */
    ARC_FURNACE,

    /** Forming. One ingot and a die; the die is not consumed. */
    PRESS,

    /** Multi-ingredient fabrication. The last crafting-table machine; T2+ is manufactured here. */
    ASSEMBLER,

    /** Roast. 1 u feed → 1 u oxide; 1 B SO₂ vents or captures. */
    KILN,

    /** Drawing. 1 ingot → 2 wire; 2 copper wire → 1 coil. T2. */
    WIRE_MILL,

    /** Stirred contact process. SO₂ → SO₃ → sulfuric acid. T2. */
    CHEMICAL_REACTOR,

    /** Wet line. 8 crushed + 2 B water → 8 washed crushed + one secondary crushed. T2. */
    CHEMICAL_WASHER,

    /** Current through a fluid. 2 B water → 2 B hydrogen + 1 B oxygen. T2. */
    ELECTROLYSIS_CELL,

    /** Ambient air to stored oxygen. Nitrogen and argon wait for a sink. T2. */
    ATMOSPHERIC_INTAKE,

    /** Clean re-melt. 1 ingot → 144 mB molten, no slag. T2. */
    INDUCTION_FURNACE,

    /** 144 mB molten and a mould → one solid form. T2. */
    CASTER,

    /** Sulfide concentrate. 20 crushed + 500 mB surfactant → 24 concentrate + 3 tailings. T2. */
    FLOTATION
}
