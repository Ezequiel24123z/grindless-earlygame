package io.github.ezequiel24123z.grindless.structure;

import java.util.List;
import java.util.Locale;
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
            brick(0, 1, 0), brick(1, 1, 0), brick(0, 1, 1), brick(1, 1, 1)));

    /** Stable menu values; append future faults instead of reordering them. */
    public enum Fault {
        NONE, MISSING_BRICKS, UNLOADED;

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
    public record Cell(Offset offset, String block, Fault fault) {
    }

    private final List<Cell> cells;

    WorkshopStructure(List<Cell> cells) {
        this.cells = List.copyOf(cells);
    }

    /** The immutable cells used by runtime formation and deterministic rotation checks. */
    public List<Cell> cells() {
        return cells;
    }

    /**
     * Checks every required cell in deterministic order without mutating the world.
     *
     * @param turns clockwise controller rotation from north
     * @param blockAt block identifier at an offset, or null when its chunk is not loaded
     * @return the first actionable fault, or {@link Fault#NONE}
     */
    public Fault check(int turns, Function<Offset, String> blockAt) {
        for (Cell cell : cells) {
            String found = blockAt.apply(cell.offset().rotate(turns));
            if (found == null) {
                return Fault.UNLOADED;
            }
            if (!cell.block().equals(found)) {
                return cell.fault();
            }
        }
        return Fault.NONE;
    }

    private static Cell brick(int x, int y, int z) {
        return new Cell(new Offset(x, y, z), "minecraft:bricks", Fault.MISSING_BRICKS);
    }
}
