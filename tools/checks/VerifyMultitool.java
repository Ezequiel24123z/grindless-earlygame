package io.github.ezequiel24123z.grindless.item;

/**
 * Behaviour checks for Multitool rotate and relocate policy (ADR-0069). Not part of the mod.
 */
public final class VerifyMultitool {

    private static int failures = 0;

    public static void main(String[] args) {
        eq("north turns east", "east", MultitoolLogic.rotateClockwise("north"));
        eq("east turns south", "south", MultitoolLogic.rotateClockwise("east"));
        eq("south turns west", "west", MultitoolLogic.rotateClockwise("south"));
        eq("west turns north", "north", MultitoolLogic.rotateClockwise("west"));
        eq("a full turn is identity", "north",
                MultitoolLogic.rotateClockwise(MultitoolLogic.rotateClockwise(
                        MultitoolLogic.rotateClockwise(MultitoolLogic.rotateClockwise("north")))));
        eq("unknown facing is a no-op", "up", MultitoolLogic.rotateClockwise("up"));
        eq("null facing stays null", null, MultitoolLogic.rotateClockwise(null));
        yes("sneak means relocate", MultitoolLogic.sneakMeansRelocate());
        no("relocation is off until a pickup wraps it", Relocation.active());
        Relocation.run(() -> yes("relocation is on inside the wrap", Relocation.active()));
        no("relocation does not leak after the wrap", Relocation.active());

        System.out.println(failures == 0
                ? "ALL MULTITOOL CHECKS PASSED"
                : failures + " MULTITOOL CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void eq(String what, String expected, String actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
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
