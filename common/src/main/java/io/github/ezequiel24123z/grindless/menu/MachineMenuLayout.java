package io.github.ezequiel24123z.grindless.menu;

import java.util.List;

/** Read-only menu geometry shared by the live menu and recipe-viewer projections. */
public final class MachineMenuLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 204;
    public static final int STATUS_X = 8;
    public static final int STATUS_Y = 70;
    public static final int INVENTORY_LABEL_Y = 110;
    public static final int INVENTORY_Y = 122;
    public static final int HOTBAR_Y = 180;

    private MachineMenuLayout() {
    }

    /** Index and role refer to the existing machine inventory, not a compacted recipe list. */
    public record ItemSlot(int index, int x, int y, boolean output) {
    }

    public static int progressX(MachineMenuKind kind) {
        return kind == MachineMenuKind.GENERATOR ? 112 : 79;
    }

    public static List<ItemSlot> itemSlots(MachineMenuKind kind) {
        return switch (kind) {
            case GENERATOR -> List.of(input(0, 80, 36));
            case PULVERIZER, KILN, WIRE_MILL, CHEMICAL_REACTOR, BIOCULTIVATOR, CASTER ->
                    List.of(input(0, 52, 35), output(1, 116, 35));
            case CHEMICAL_WASHER, FLOTATION, QUERN ->
                    List.of(input(0, 44, 35), output(1, 116, 26), output(2, 116, 44));
            case GEOLOGICAL_COLLECTOR -> List.of(output(0, 116, 26), output(1, 116, 44));
            case SETTLING_TROUGH -> List.of(input(0, 44, 35),
                    output(1, 107, 26), output(2, 125, 26), output(3, 143, 26),
                    output(4, 107, 44), output(5, 125, 44));
            case ELECTROLYSIS_CELL, ATMOSPHERIC_INTAKE, BOILER, CONDENSER -> List.of();
            case INDUCTION_FURNACE -> List.of(input(0, 52, 35));
            case ARC_FURNACE, FIRING_CHAMBER -> List.of(input(0, 44, 26), input(1, 44, 44),
                    output(2, 116, 26), output(3, 116, 44));
            case PRESS, CLAY_MIXER ->
                    List.of(input(0, 44, 26), input(1, 44, 44), output(2, 116, 35));
            case ASSEMBLER, ASSEMBLY_BENCH -> List.of(input(0, 35, 26), input(1, 53, 26),
                    input(2, 35, 44), input(3, 53, 44), output(4, 116, 35));
        };
    }

    private static ItemSlot input(int index, int x, int y) {
        return new ItemSlot(index, x, y, false);
    }

    private static ItemSlot output(int index, int x, int y) {
        return new ItemSlot(index, x, y, true);
    }
}
