package io.github.ezequiel24123z.grindless.item;

import java.nio.file.Files;
import java.nio.file.Path;

/** Behaviour checks for the Voltaic Harness grid (ADR-0102). Not part of the mod. */
public final class VerifyHarness {

    private static final Path RECIPES = Path.of("common/src/main/resources/data/grindless/recipes");

    private static int failures = 0;

    public static void main(String[] args) {
        eq("one slot", 1, HarnessLogic.SLOTS);
        eq("the cell holds 6400", 6400, HarnessLogic.CELL_CAPACITY);
        eq("the suit cell is not the drill cell", false,
                HarnessLogic.FLUX_CELL.equals("grindless:drill_cell"));

        HarnessLogic.Piece empty = HarnessLogic.empty();
        HarnessLogic.Piece filled = HarnessLogic.install(empty, 100);
        eq("an empty piece takes the cell", true, filled.filled());
        eq("the charge moves in", 100, filled.charge());
        eq("a second cell does not fit", true, HarnessLogic.install(filled, 50) == filled);
        eq("remove returns the charge", 100, HarnessLogic.remove(filled));
        eq("an empty piece removes nothing", true, HarnessLogic.remove(empty) == null);

        eq("charge clamps", 6400, HarnessLogic.afterCharge(6400, 10));
        eq("a partial fill adds", 150, HarnessLogic.afterCharge(100, 50));
        eq("accepted is the room", 50, HarnessLogic.accepted(100, 50));
        eq("a full cell accepts nothing", 0, HarnessLogic.accepted(6400, 20));
        eq("negative charge is empty", 0, HarnessLogic.clamp(-4));

        yes("the helmet is a crafting recipe", Files.isRegularFile(RECIPES.resolve("voltaic_helmet.json")));
        yes("the chest is a crafting recipe", Files.isRegularFile(RECIPES.resolve("voltaic_chestplate.json")));
        yes("the legs are a crafting recipe", Files.isRegularFile(RECIPES.resolve("voltaic_leggings.json")));
        yes("the boots are a crafting recipe", Files.isRegularFile(RECIPES.resolve("voltaic_boots.json")));
        yes("the flux cell is a crafting recipe", Files.isRegularFile(RECIPES.resolve("flux_cell.json")));

        System.out.println(failures == 0
                ? "ALL HARNESS CHECKS PASSED"
                : failures + " HARNESS CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
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

    private static void yes(String what, boolean condition) {
        if (condition) {
            System.out.println("  ok   " + what);
        } else {
            fail(what);
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
