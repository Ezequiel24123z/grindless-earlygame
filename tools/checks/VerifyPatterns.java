package io.github.ezequiel24123z.grindless.pattern;

import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.recipe.ReplicationCost;

import java.util.List;

/** Behaviour checks for replication cost and the Matter yield (ADR-0088). Not part of the mod. */
public final class VerifyPatterns {

    private static int failures = 0;

    public static void main(String[] args) {
        eq("a blank id is refused", true, PatternLogic.refused(""));
        eq("matter is refused", true, PatternLogic.refused(PatternLogic.MATTER));
        eq("an ingot is scannable", false, PatternLogic.refused("minecraft:iron_ingot"));
        eq("a smash yields one", 1, PatternLogic.YIELD);

        eq("no line costs the floor", 1, ReplicationCost.of("minecraft:dirt", List.of()));
        eq("a blank walk costs the floor", 1, ReplicationCost.of("  ", List.of()));

        ReplicationCost.Line plate = new ReplicationCost.Line("grindless:plate", 1, List.of(
                new ReplicationCost.ItemPart("minecraft:iron_ingot", 2)), 0);
        eq("two primitives cost two", 2, ReplicationCost.of("grindless:plate", List.of(plate)));

        ReplicationCost.Line shared = new ReplicationCost.Line("grindless:plate", 2, List.of(
                new ReplicationCost.ItemPart("minecraft:iron_ingot", 5)), 0);
        eq("five inputs over two outputs cost three", 3,
                ReplicationCost.of("grindless:plate", List.of(shared)));

        ReplicationCost.Line dear = new ReplicationCost.Line("grindless:plate", 1, List.of(
                new ReplicationCost.ItemPart("minecraft:iron_ingot", 8)), 0);
        ReplicationCost.Line cheap = new ReplicationCost.Line("grindless:plate", 1, List.of(
                new ReplicationCost.ItemPart("minecraft:iron_ingot", 1)), 0);
        eq("the cheaper route wins", 1,
                ReplicationCost.of("grindless:plate", List.of(dear, cheap)));

        ReplicationCost.Line loopA = new ReplicationCost.Line("a", 1, List.of(
                new ReplicationCost.ItemPart("b", 1)), 0);
        ReplicationCost.Line loopB = new ReplicationCost.Line("b", 1, List.of(
                new ReplicationCost.ItemPart("a", 1)), 0);
        eq("a cycle costs the floor", 1, ReplicationCost.of("a", List.of(loopA, loopB)));

        eq("one millibucket is one bucket", 1, ReplicationCost.fluidCost(1));
        eq("a full bucket is one", 1, ReplicationCost.fluidCost(1000));
        eq("a bucket and a drop is two", 2, ReplicationCost.fluidCost(1001));
        ReplicationCost.Line wet = new ReplicationCost.Line("grindless:wet", 1, List.of(), 1500);
        eq("fifteen hundred millibuckets cost two", 2,
                ReplicationCost.of("grindless:wet", List.of(wet)));

        ProcessRecipe tagged = new ProcessRecipe(
                "test/widget",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.tag("forge:plates/iron", 4),
                        IngredientSpec.item("grindless:motor", 1)),
                List.of(
                        OutputSpec.tag("forge:ingots/iron", 1),
                        OutputSpec.item("grindless:widget", 1)),
                Double.NaN,
                null,
                20,
                1L);
        eq("a tag input is not expanded", 1,
                ReplicationCost.ofRecipes("grindless:widget", List.of(tagged)));
        eq("a tag output is not a member item", 1,
                ReplicationCost.ofRecipes("forge:ingots/iron", List.of(tagged)));

        System.out.println(failures == 0
                ? "ALL PATTERN CHECKS PASSED"
                : failures + " PATTERN CHECK(S) FAILED");
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

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
