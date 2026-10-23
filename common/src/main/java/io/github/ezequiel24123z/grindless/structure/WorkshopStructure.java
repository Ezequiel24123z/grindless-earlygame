package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.recipe.MachineFamily;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Bounded local-cell contracts for the primitive workshop, not a parametric multiblock kernel.
 *
 * <p>Local X points right, Y up and Z behind a north-facing controller. The reader must return
 * {@code null} for an unloaded cell rather than loading a chunk. No formation result is persisted.
 */
public enum WorkshopStructure {

    NONE(List.of()),
    FIRING_CHAMBER(List.of(
            brick(1, 0, 0), brick(0, 0, 1), brick(1, 0, 1),
            brick(0, 1, 0), brick(1, 1, 0), brick(0, 1, 1), brick(1, 1, 1))),
    SETTLING_TROUGH(List.of(
            new Cell(new Offset(0, 0, 1), "grindless:settling_channel", Fault.MISSING_CHANNEL, true),
            new Cell(new Offset(0, 0, 2), "grindless:settling_screen", Fault.MISSING_SCREEN, true),
            new Cell(new Offset(0, 1, 1), "minecraft:air", Fault.COVERED, false),
            new Cell(new Offset(0, 1, 2), "minecraft:air", Fault.COVERED, false)));

    /** Stable menu values; append future faults instead of reordering them. */
    public enum Fault {
        NONE, MISSING_BRICKS, UNLOADED, MISSING_CHANNEL, MISSING_SCREEN, WRONG_FACING, COVERED,
        INCOMPATIBLE_PORT, EXCESS_PORTS, AMBIGUOUS_OWNER, ORPHANED_PORT, STALE_OWNER;

        /** The localized recovery instruction, never an untranslated server-side sentence. */
        public String translationKey() {
            return "gui.grindless.structure." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** A world-relative offset after rotating the local structure about its controller. */
    public record Offset(int x, int y, int z) {
        /** Rotates clockwise by zero to three quarter turns, with north as zero. */
        public Offset rotate(int turns) {
            return switch (turns) {
                case 0 -> this;
                case 1 -> new Offset(-z, y, x);
                case 2 -> new Offset(-x, y, -z);
                case 3 -> new Offset(z, y, -x);
                default -> throw new IllegalArgumentException("horizontal rotation must be 0..3");
            };
        }
    }

    /** One required block and its actionable failure, in controller-local coordinates. */
    public record Cell(Offset offset, String block, Fault fault, boolean oriented) {
    }

    /** A loaded world's block identity and horizontal rotation, or -1 when it has no facing. */
    public record Present(String block, int turns) {
    }

    /** An observed port position relative to the controller, already rotated into world axes. */
    public record Port(Offset offset, WorkshopPortContract.Kind kind) {
    }

    /** Fresh derived evidence, never a persisted or trusted formed flag. */
    public record Inspection(Fault fault, Offset offset, List<Port> ports) {
        public Inspection {
            ports = List.copyOf(ports);
        }

        public boolean formed() {
            return fault == Fault.NONE;
        }
    }

    private final List<Cell> cells;

    WorkshopStructure(List<Cell> cells) {
        this.cells = List.copyOf(cells);
    }

    /** The immutable cells used by runtime formation and deterministic rotation checks. */
    public List<Cell> cells() {
        return cells;
    }

    /** The shared runtime/inventory binding; a family does not imply an accepted progression tier. */
    public static WorkshopStructure forFamily(MachineFamily family) {
        return switch (family) {
            case FIRING_CHAMBER -> FIRING_CHAMBER;
            case SETTLING_TROUGH -> SETTLING_TROUGH;
            default -> NONE;
        };
    }

    /**
     * Checks every required cell in deterministic order without mutating the world.
     *
     * @param turns clockwise controller rotation from north
     * @param blockAt observed block at an offset, or null when its chunk is not loaded
     * @return the first actionable fault, or {@link Fault#NONE}
     */
    public Fault check(int turns, Function<Offset, Present> blockAt) {
        return inspect(turns, blockAt).fault();
    }

    /** The first fault includes its exact world-relative cell instead of only a broad status. */
    public Inspection inspect(int turns, Function<Offset, Present> blockAt) {
        List<Port> ports = new ArrayList<>();
        Map<WorkshopPortRole, Integer> counts = new EnumMap<>(WorkshopPortRole.class);
        int bricks = 0;
        for (Cell cell : cells) {
            Offset offset = cell.offset().rotate(turns);
            Present found = blockAt.apply(offset);
            if (found == null) {
                return new Inspection(Fault.UNLOADED, offset, ports);
            }
            if (!cell.block().equals(found.block())) {
                Optional<WorkshopPortContract.Kind> kind = WorkshopPortContract.kind(found.block());
                if (kind.isEmpty()) {
                    return new Inspection(cell.fault(), offset, ports);
                }
                Optional<WorkshopPortContract.Slot> slot = WorkshopPortContract.slot(this, cell.offset());
                if (slot.isEmpty() || !slot.get().accepts(kind.get())) {
                    return new Inspection(Fault.INCOMPATIBLE_PORT, offset, ports);
                }
                if (found.turns() != turns) {
                    return new Inspection(Fault.WRONG_FACING, offset, ports);
                }
                WorkshopPortRole role = kind.get().role();
                int count = counts.merge(role, 1, Integer::sum);
                if (count > WorkshopPortContract.maximum(this, role)
                        || ports.size() >= WorkshopPortContract.maximumPorts(this)) {
                    return new Inspection(Fault.EXCESS_PORTS, offset, ports);
                }
                ports.add(new Port(offset, kind.get()));
                continue;
            }
            if (cell.oriented() && found.turns() != turns) {
                return new Inspection(Fault.WRONG_FACING, offset, ports);
            }
            if (cell.block().equals("minecraft:bricks")) {
                bricks++;
            }
        }
        if (bricks < WorkshopPortContract.minimumBricks(this)) {
            return new Inspection(Fault.MISSING_BRICKS, null, ports);
        }
        return new Inspection(Fault.NONE, null, ports);
    }

    private static Cell brick(int x, int y, int z) {
        return new Cell(new Offset(x, y, z), "minecraft:bricks", Fault.MISSING_BRICKS, false);
    }
}
