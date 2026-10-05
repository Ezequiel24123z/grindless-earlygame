package io.github.ezequiel24123z.grindless.item;

/**
 * The T1 suit grid (ADR-0102). One slot, one module, no generation.
 *
 * <p>No Minecraft types. The Flux Cell is not the drill's fuel. Walking through a pylon does not
 * charge it; that is the Network Tap (slice Y).
 */
public final class HarnessLogic {

    /** Slots on each piece. One, until a second module exists. */
    public static final int SLOTS = 1;

    /** Onboard buffer. Not a generator. */
    public static final int CELL_CAPACITY = 6400;

    public static final String FLUX_CELL = "grindless:flux_cell";

    public static final String MODULE = "Module";

    public static final String CHARGE = "Charge";

    private HarnessLogic() {
    }

    /** What one piece is holding. An empty module is no module. */
    public record Piece(String module, int charge) {

        public Piece {
            module = module == null ? "" : module;
            charge = clamp(charge);
        }

        public boolean filled() {
            return !module.isEmpty();
        }
    }

    public static Piece empty() {
        return new Piece("", 0);
    }

    /**
     * Moves a cell into an empty slot. A filled slot is unchanged, so the caller keeps the cell.
     */
    public static Piece install(Piece piece, int cellCharge) {
        if (piece == null || piece.filled()) {
            return piece == null ? empty() : piece;
        }
        return new Piece(FLUX_CELL, cellCharge);
    }

    /** The cell that comes back out, or {@code null} when the slot was empty. */
    public static Integer remove(Piece piece) {
        if (piece == null || !piece.filled()) {
            return null;
        }
        return piece.charge();
    }

    /** Charge after accepting up to {@code offered} FU. Never above the capacity. */
    public static int afterCharge(int stored, int offered) {
        if (offered <= 0) {
            return clamp(stored);
        }
        long sum = (long) clamp(stored) + offered;
        if (sum > CELL_CAPACITY) {
            return CELL_CAPACITY;
        }
        return (int) sum;
    }

    /** How much of {@code offered} actually fits. */
    public static int accepted(int stored, int offered) {
        return afterCharge(stored, offered) - clamp(stored);
    }

    public static int clamp(int charge) {
        if (charge < 0) {
            return 0;
        }
        return Math.min(CELL_CAPACITY, charge);
    }
}
