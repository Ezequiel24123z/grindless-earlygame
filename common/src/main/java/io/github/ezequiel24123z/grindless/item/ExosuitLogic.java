package io.github.ezequiel24123z.grindless.item;

/**
 * The T2 suit grid (ADR-0090). Two slots, a pylon tap, and speed that spends charge.
 *
 * <p>No Minecraft types. The suit does not generate. The harness stays at one slot.
 */
public final class ExosuitLogic {

    public static final int SLOTS = 2;

    /** FU pulled from pylon coverage in one tick, shared across the suit's cells. */
    public static final int TAP_FU = 32;

    /** FU a tick of extra speed costs. */
    public static final int LEGS_FU = 1;

    /** Added to movement speed while the legs are installed and a cell has charge. */
    public static final double SPEED = 0.04;

    public static final String NETWORK_TAP = "grindless:network_tap";

    public static final String EXOSKELETON = "grindless:exoskeleton_legs";

    public static final String FIRST = "ModuleA";

    public static final String FIRST_CHARGE = "ChargeA";

    public static final String SECOND = "ModuleB";

    public static final String SECOND_CHARGE = "ChargeB";

    private ExosuitLogic() {
    }

    /** Two slots. An empty id is an empty slot. Charge is only meaningful on a Flux Cell. */
    public record Piece(String first, int firstCharge, String second, int secondCharge) {

        public Piece {
            first = first == null ? "" : first;
            second = second == null ? "" : second;
            firstCharge = cellCharge(first, firstCharge);
            secondCharge = cellCharge(second, secondCharge);
        }

        public boolean has(String module) {
            return module != null && (module.equals(first) || module.equals(second));
        }

        public int freeSlots() {
            return (first.isEmpty() ? 1 : 0) + (second.isEmpty() ? 1 : 0);
        }
    }

    public static Piece empty() {
        return new Piece("", 0, "", 0);
    }

    /** Fills the first empty slot. A full piece is returned unchanged. */
    public static Piece install(Piece piece, String module, int charge) {
        if (piece == null || module == null || module.isBlank()) {
            return piece == null ? empty() : piece;
        }
        if (piece.first().isEmpty()) {
            return new Piece(module, charge, piece.second(), piece.secondCharge());
        }
        if (piece.second().isEmpty()) {
            return new Piece(piece.first(), piece.firstCharge(), module, charge);
        }
        return piece;
    }

    /** Drops the last filled slot. {@code null} when both are empty. */
    public static Removed removeLast(Piece piece) {
        if (piece == null) {
            return null;
        }
        if (!piece.second().isEmpty()) {
            return new Removed(piece.second(), piece.secondCharge(),
                    new Piece(piece.first(), piece.firstCharge(), "", 0));
        }
        if (!piece.first().isEmpty()) {
            return new Removed(piece.first(), piece.firstCharge(), empty());
        }
        return null;
    }

    /** How much more FU the cells on this piece can hold. */
    public static int room(Piece piece) {
        if (piece == null) {
            return 0;
        }
        return roomOf(piece.first(), piece.firstCharge()) + roomOf(piece.second(), piece.secondCharge());
    }

    /** Puts {@code fu} into cells, first slot then second. Modules that are not cells are skipped. */
    public static Piece addCharge(Piece piece, int fu) {
        if (piece == null || fu <= 0) {
            return piece == null ? empty() : piece;
        }
        int first = piece.firstCharge();
        int second = piece.secondCharge();
        int intoFirst = Math.min(fu, roomOf(piece.first(), first));
        first += intoFirst;
        int intoSecond = Math.min(fu - intoFirst, roomOf(piece.second(), second));
        second += intoSecond;
        return new Piece(piece.first(), first, piece.second(), second);
    }

    /** Spends up to {@code fu} from cells. Returns the piece and what was actually spent. */
    public static Spent spend(Piece piece, int fu) {
        if (piece == null || fu <= 0) {
            return new Spent(piece == null ? empty() : piece, 0);
        }
        int left = fu;
        int first = piece.firstCharge();
        int second = piece.secondCharge();
        int fromFirst = Math.min(left, cellCharge(piece.first(), first));
        first -= fromFirst;
        left -= fromFirst;
        int fromSecond = Math.min(left, cellCharge(piece.second(), second));
        second -= fromSecond;
        left -= fromSecond;
        return new Spent(new Piece(piece.first(), first, piece.second(), second), fu - left);
    }

    public static int cellCharge(String module, int charge) {
        if (!HarnessLogic.FLUX_CELL.equals(module)) {
            return 0;
        }
        return HarnessLogic.clamp(charge);
    }

    private static int roomOf(String module, int charge) {
        if (!HarnessLogic.FLUX_CELL.equals(module)) {
            return 0;
        }
        return HarnessLogic.CELL_CAPACITY - cellCharge(module, charge);
    }

    public record Removed(String module, int charge, Piece piece) {
    }

    public record Spent(Piece piece, int spent) {
    }
}
