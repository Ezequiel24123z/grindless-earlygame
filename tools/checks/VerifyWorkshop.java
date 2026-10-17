package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact quantities, non-amplifying ceramic loops and every rotated chamber cell (ADR-0129). */
public final class VerifyWorkshop {

    private static int failures;

    public static void main(String[] args) {
        recipes();
        conservation();
        structures();
        System.out.println(failures == 0 ? "ALL WORKSHOP CHECKS PASSED"
                : failures + " WORKSHOP CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void recipes() {
        List<ProcessRecipe> recipes = BootstrapProcesses.recipes();
        check("eight complete workshop processes", recipes.size() == 8);
        Set<String> ids = new HashSet<>();
        for (ProcessRecipe recipe : recipes) {
            check(recipe.id() + " is unique", ids.add(recipe.id()));
            check(recipe.id() + " is bounded F0 work", recipe.fuPerTick() > 0 && recipe.fuPerTick() <= 8);
            check(recipe.id() + " consumes real stock", !recipe.itemInputs().isEmpty());
            check(recipe.id() + " needs no Matrix or future material",
                    recipe.inputs().stream().noneMatch(input -> input.matrixRating() > 0
                            || input.id().contains("relay_matrix") || input.id().contains("silicon")));
        }
        ProcessRecipe sand = recipe("sand_body");
        ProcessRecipe grog = recipe("grog_body");
        check("the body spends two clay and one aggregate", sand.itemInputs().get(0).count() == 2
                && sand.itemInputs().get(1).count() == 1);
        check("the body spends exactly 250 mB water", sand.fluidInputs().size() == 1
                && sand.fluidInputs().get(0).id().equals("minecraft:water")
                && sand.fluidInputs().get(0).count() == 250);
        check("grog improves time without inventing yield", grog.durationTicks() < sand.durationTicks()
                && grog.outputs().equals(sand.outputs()) && grog.fluidInputs().equals(sand.fluidInputs()));
        ProcessRecipe firing = recipe("insulators");
        check("firing spends three blanks and one fuel", firing.itemInputs().get(0).count() == 3
                && firing.itemInputs().get(1).equals(IngredientSpec.tag("grindless:carbon", 1)));
        check("firing yields two insulators and one reusable grog",
                firing.outputs().equals(List.of(OutputSpec.item(BootstrapProcesses.INSULATOR, 2),
                        OutputSpec.item(BootstrapProcesses.GROG, 1))));
        check("fuel-backed heat does not exceed the kiln ceiling",
                firing.temperatureC() == 850 && firing.atmosphere().equals("OXIDISING"));
        check("the Data Core can no longer skip ceramics",
                BootstrapRecipes.shapeless().stream().filter(craft -> craft.name().equals("data_core"))
                        .allMatch(craft -> craft.ingredients().equals(List.of(
                                "item:" + BootstrapProcesses.INSULATOR, BootstrapRecipes.REDSTONE))));
        for (String name : List.of("quern", "clay_mixer", "firing_chamber", "hand_pump",
                "basic_tank", "clay_conduit", "conveyor_belt", "crude_manipulator", "process_atlas")) {
            BootstrapRecipes.Shaped craft = BootstrapRecipes.shaped().stream()
                    .filter(row -> row.name().equals(name)).findFirst().orElseThrow();
            check(name + " is reachable without iron or a Matrix",
                    craft.iron() == 0 && !craft.key().containsValue(T1Recipes.RELAY_MATRIX));
        }
    }

    /**
     * A positive potential on every ceramic solid proves any composed loop cannot create
     * additional solid units. Water and consumed fuel still charge every firing/mixing cycle.
     */
    private static void conservation() {
        Set<String> solids = Set.of("minecraft:gravel", "minecraft:sand", "minecraft:brick",
                "minecraft:clay_ball", BootstrapProcesses.GROG, BootstrapProcesses.UNFIRED,
                BootstrapProcesses.INSULATOR);
        for (ProcessRecipe recipe : BootstrapProcesses.recipes()) {
            int input = recipe.itemInputs().stream().filter(item -> solids.contains(item.id()))
                    .mapToInt(IngredientSpec::count).sum();
            int output = recipe.itemOutputs().stream().filter(item -> solids.contains(item.id()))
                    .mapToInt(OutputSpec::count).sum();
            check(recipe.id() + " has no undeclared output", recipe.itemOutputs().stream()
                    .allMatch(item -> solids.contains(item.id())));
            check(recipe.id() + " cannot amplify ceramic stock", input > 0 && input >= output);
        }
        check("the grog sink is a real live process", recipe("grog_body").itemInputs().stream()
                .anyMatch(input -> input.id().equals(BootstrapProcesses.GROG)));
        check("unfired recovery does not return spent water", recipe("unfired_recovery").fluidOutputs().isEmpty());
        check("the workshop adds no ore multiplier", BootstrapProcesses.recipes().stream()
                .flatMap(recipe -> recipe.inputs().stream())
                .noneMatch(input -> input.id().contains("ores/") || input.id().contains("raw_materials/")));
    }

    private static void structures() {
        WorkshopStructure structure = WorkshopStructure.FIRING_CHAMBER;
        check("the chamber needs exactly seven surrounding bricks", structure.cells().size() == 7);
        for (int turn = 0; turn < 4; turn++) {
            Map<WorkshopStructure.Offset, String> world = new HashMap<>();
            for (WorkshopStructure.Cell cell : structure.cells()) {
                world.put(cell.offset().rotate(turn), cell.block());
            }
            check("rotation " + turn + " keeps seven unique occupied cells", world.size() == 7);
            check("rotation " + turn + " never occupies its controller",
                    !world.containsKey(new WorkshopStructure.Offset(0, 0, 0)));
            check("rotation " + turn + " forms", structure.check(turn, world::get) == WorkshopStructure.Fault.NONE);
            for (WorkshopStructure.Cell cell : structure.cells()) {
                WorkshopStructure.Offset position = cell.offset().rotate(turn);
                world.put(position, "minecraft:air");
                check("every broken cell invalidates rotation " + turn,
                        structure.check(turn, world::get) == WorkshopStructure.Fault.MISSING_BRICKS);
                world.put(position, cell.block());
                check("repair restores rotation " + turn,
                        structure.check(turn, world::get) == WorkshopStructure.Fault.NONE);
            }
            world.remove(structure.cells().get(0).offset().rotate(turn));
            check("unloaded cells never count as formed", structure.check(turn, world::get)
                    == WorkshopStructure.Fault.UNLOADED);
        }
        check("ordinary machines never inspect structure cells",
                WorkshopStructure.NONE.check(0, cell -> { throw new AssertionError("unexpected world read"); })
                        == WorkshopStructure.Fault.NONE);
    }

    private static ProcessRecipe recipe(String id) {
        return BootstrapProcesses.recipes().stream().filter(row -> row.id().equals("bootstrap/" + id))
                .findFirst().orElseThrow();
    }

    private static void check(String label, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + label);
        if (!passed) {
            failures++;
        }
    }
}
