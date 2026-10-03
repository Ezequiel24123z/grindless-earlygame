package io.github.ezequiel24123z.grindless.recipe;

/**
 * The machine a {@link ProcessRecipe} runs in.
 *
 * <p>A recipe names a family, not a block. Any block of that family whose envelope covers the
 * recipe's conditions can host it (ADR-0019, ADR-0020). Slice A shipped two families; Slice D
 * adds Press and Assembler without forking the type.
 */
public enum MachineFamily {

    /** Dry mill. B1: 1 u raw → 2 u crushed. */
    PULVERIZER,

    /** Carbothermic reduction. R1: graded feed + carbon → ingot + slag; CO vents. */
    ARC_FURNACE,

    /** Forming. One ingot and a die; the die is not consumed. */
    PRESS,

    /** Multi-ingredient fabrication. The last crafting-table machine; T2+ is manufactured here. */
    ASSEMBLER
}
