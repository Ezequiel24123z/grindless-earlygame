package io.github.ezequiel24123z.grindless.item;

import java.util.List;
import java.util.Map;

/** Behaviour checks for blueprint capture and stamping (ADR-0085). Not part of the mod. */
public final class VerifyBlueprint {

    private static int failures = 0;

    public static void main(String[] args) {
        bounds();
        pieces();
        System.out.println(failures == 0
                ? "ALL BLUEPRINT CHECKS PASSED"
                : failures + " BLUEPRINT CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void bounds() {
        eq("an edge may be thirty-two", true, BlueprintLogic.edgeOk(0, 31));
        eq("thirty-three is too long", false, BlueprintLogic.edgeOk(0, 32));
        eq("a reversed edge is not a box", false, BlueprintLogic.edgeOk(4, 1));
        eq("one block is a box", true, BlueprintLogic.edgeOk(7, 7));
        eq("empty is not a blueprint", false, BlueprintLogic.countOk(0));
        eq("five hundred twelve is the cap", true, BlueprintLogic.countOk(512));
        eq("five hundred thirteen is too many", false, BlueprintLogic.countOk(513));
    }

    private static void pieces() {
        eq("facing is kept", "north", BlueprintLogic.keptProperty("facing", "north"));
        eq("status is dropped", "", BlueprintLogic.keptProperty("status", "running"));
        eq("a blank facing is dropped", "", BlueprintLogic.keptProperty("facing", " "));
        List<BlueprintLogic.Piece> layout = List.of(
                new BlueprintLogic.Piece(0, 0, 0, "grindless:assembler", "grindless:assembler", "north"),
                new BlueprintLogic.Piece(1, 0, 0, "grindless:assembler", "grindless:assembler", "south"),
                new BlueprintLogic.Piece(0, 1, 0, "grindless:conveyor_belt", "grindless:conveyor_belt", ""));
        Map<String, Integer> cost = BlueprintLogic.cost(layout);
        eq("two assemblers", 2, cost.get("grindless:assembler").intValue());
        eq("one belt", 1, cost.get("grindless:conveyor_belt").intValue());
        Map<String, Integer> have = Map.of("grindless:assembler", 2, "grindless:conveyor_belt", 1);
        yes("a paid stamp has no shortfall", BlueprintLogic.shortfall(cost, have).isEmpty());
        Map<String, Integer> shortOne = BlueprintLogic.shortfall(cost, Map.of("grindless:assembler", 1));
        eq("one assembler short", 1, shortOne.get("grindless:assembler").intValue());
        eq("the belt is also short", 1, shortOne.get("grindless:conveyor_belt").intValue());
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
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

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
