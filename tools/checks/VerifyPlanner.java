package io.github.ezequiel24123z.grindless.item;

/** Behaviour checks for the deconstruction mark (ADR-0086). Not part of the mod. */
public final class VerifyPlanner {

    private static int failures = 0;

    public static void main(String[] args) {
        eq("the edge matches a blueprint", BlueprintLogic.MAX_EDGE, PlannerLogic.MAX_EDGE);
        eq("a 2×3×4 box is twenty-four", 24L, PlannerLogic.volume(0, 1, 0, 2, 0, 3));
        eq("one block is one", 1L, PlannerLogic.volume(4, 4, 4, 4, 4, 4));
        eq("thirty-two on an edge is the cap", 32L * 32L * 32L,
                PlannerLogic.volume(0, 31, 0, 31, 0, 31));
        eq("thirty-three is refused", 0L, PlannerLogic.volume(0, 32, 0, 0, 0, 0));
        eq("a reversed edge is refused", 0L, PlannerLogic.volume(5, 1, 0, 0, 0, 0));
        System.out.println(failures == 0
                ? "ALL PLANNER CHECKS PASSED"
                : failures + " PLANNER CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, int expected, int actual) {
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
