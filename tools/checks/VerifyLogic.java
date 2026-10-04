package io.github.ezequiel24123z.grindless.logic;

/** Behaviour checks for the logic numbers (ADR-0083). Not part of the mod. */
public final class VerifyLogic {

    private static int failures = 0;

    public static void main(String[] args) {
        numbers();
        System.out.println(failures == 0
                ? "ALL LOGIC CHECKS PASSED"
                : failures + " LOGIC CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void numbers() {
        eq("the example threshold is five hundred", 500, SignalLogic.THRESHOLD);
        eq("run writes one", 1, SignalLogic.RUN);
        eq("hold writes zero", 0, SignalLogic.HOLD);
        yes("four hundred ninety-nine still runs", SignalLogic.shouldRun(499, SignalLogic.THRESHOLD));
        no("five hundred holds", SignalLogic.shouldRun(500, SignalLogic.THRESHOLD));
        no("above the threshold holds", SignalLogic.shouldRun(501, SignalLogic.THRESHOLD));
        eq("a quiet cable emits nothing", 0, SignalLogic.toRedstone(0));
        eq("a negative cable emits nothing", 0, SignalLogic.toRedstone(-3));
        eq("one on the cable is one on the wire", 1, SignalLogic.toRedstone(1));
        eq("a large cable is still a full redstone line", 15, SignalLogic.toRedstone(20));
        eq("no redstone writes zero", 0, SignalLogic.fromRedstone(0));
        eq("seven stays seven", 7, SignalLogic.fromRedstone(7));
        eq("redstone above fifteen is clipped", 15, SignalLogic.fromRedstone(20));
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
