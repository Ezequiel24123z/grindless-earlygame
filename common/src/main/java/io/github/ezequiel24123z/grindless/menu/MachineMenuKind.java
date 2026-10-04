package io.github.ezequiel24123z.grindless.menu;

/**
 * Layout of the shared machine menu.
 *
 * <p>One screen, shared slot maps. Energy, progress and the named fault are the same on every
 * machine (ADR-0058).
 */
public enum MachineMenuKind {

    GENERATOR(1, 0),
    PULVERIZER(1, 1),
    ARC_FURNACE(2, 2),
    PRESS(2, 1),
    ASSEMBLER(3, 1),
    KILN(1, 1),
    WIRE_MILL(1, 1),
    CHEMICAL_REACTOR(1, 1),
    CHEMICAL_WASHER(1, 2),
    /** Fluid-only. Ordinals are saved; new kinds are appended (ADR-0077). */
    ELECTROLYSIS_CELL(0, 0),
    ATMOSPHERIC_INTAKE(0, 0),
    INDUCTION_FURNACE(1, 0),
    CASTER(1, 1),
    FLOTATION(1, 2),
    BOILER(0, 0),
    CONDENSER(0, 0);

    private final int inputs;
    private final int outputs;

    MachineMenuKind(int inputs, int outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public int inputs() {
        return inputs;
    }

    public int outputs() {
        return outputs;
    }

    public int size() {
        return inputs + outputs;
    }
}
