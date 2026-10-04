package io.github.ezequiel24123z.grindless.item;

/** Behaviour checks for the exosuit grid (ADR-0090). Not part of the mod. */
public final class VerifyExosuit {

    private static int failures = 0;

    public static void main(String[] args) {
        eq("two slots", 2, ExosuitLogic.SLOTS);
        eq("the harness stays at one", 1, HarnessLogic.SLOTS);
        eq("a tap pulls thirty-two", 32, ExosuitLogic.TAP_FU);
        eq("legs spend one", 1, ExosuitLogic.LEGS_FU);

        ExosuitLogic.Piece empty = ExosuitLogic.empty();
        ExosuitLogic.Piece tap = ExosuitLogic.install(empty, ExosuitLogic.NETWORK_TAP, 0);
        eq("the first slot takes the tap", true, tap.has(ExosuitLogic.NETWORK_TAP));
        eq("one slot remains", 1, tap.freeSlots());
        ExosuitLogic.Piece both = ExosuitLogic.install(tap, HarnessLogic.FLUX_CELL, 100);
        eq("the second slot takes the cell", true, both.has(HarnessLogic.FLUX_CELL));
        eq("the piece is full", 0, both.freeSlots());
        eq("a third module does not fit", true,
                ExosuitLogic.install(both, ExosuitLogic.EXOSKELETON, 0) == both);

        eq("only the cell has room", HarnessLogic.CELL_CAPACITY - 100, ExosuitLogic.room(both));
        ExosuitLogic.Piece filled = ExosuitLogic.addCharge(both, 50);
        eq("charge lands on the cell", 150, filled.secondCharge());
        eq("the tap is not a battery", 0, filled.firstCharge());

        ExosuitLogic.Spent spent = ExosuitLogic.spend(filled, 1);
        eq("legs spend from the cell", 1, spent.spent());
        eq("the cell keeps the rest", 149, spent.piece().secondCharge());
        ExosuitLogic.Spent dry = ExosuitLogic.spend(ExosuitLogic.install(empty, ExosuitLogic.EXOSKELETON, 0), 1);
        eq("legs with no cell spend nothing", 0, dry.spent());

        ExosuitLogic.Removed removed = ExosuitLogic.removeLast(filled);
        eq("remove drops the cell", HarnessLogic.FLUX_CELL, removed.module());
        eq("the tap stays", true, removed.piece().has(ExosuitLogic.NETWORK_TAP));

        System.out.println(failures == 0
                ? "ALL EXOSUIT CHECKS PASSED"
                : failures + " EXOSUIT CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, boolean expected, boolean actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
