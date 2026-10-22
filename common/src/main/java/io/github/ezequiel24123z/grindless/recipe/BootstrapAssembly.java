package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Automatic components/infrastructure project existing crafts, never final normal machines. */
public final class BootstrapAssembly {
    private BootstrapAssembly() {
    }

    public static List<ProcessRecipe> recipes(MachineFamily family, int ticks, long power) {
        List<ProcessRecipe> result = new ArrayList<>();
        for (BootstrapRecipes.Shaped craft : BootstrapRecipes.shaped()) {
            if (ManufacturingPolicy.isManualFinalMachine(craft.result())) {
                continue;
            }
            Map<String, Integer> inputs = new LinkedHashMap<>();
            for (String row : craft.pattern()) {
                for (char symbol : row.toCharArray()) {
                    if (symbol != ' ') {
                        inputs.merge(craft.key().get(String.valueOf(symbol)), 1, Integer::sum);
                    }
                }
            }
            result.add(assemble(family, craft.name(), inputs, craft.result(), craft.resultCount(), ticks, power));
        }
        for (BootstrapRecipes.Shapeless craft : BootstrapRecipes.shapeless()) {
            if (ManufacturingPolicy.isManualFinalMachine(craft.result())) {
                continue;
            }
            Map<String, Integer> inputs = new LinkedHashMap<>();
            craft.ingredients().forEach(input -> inputs.merge(input, 1, Integer::sum));
            result.add(assemble(family, craft.name(), inputs, craft.result(), 1, ticks, power));
        }
        result.add(assemble(family, "chest", Map.of(BootstrapRecipes.PLANKS, 8), "minecraft:chest", 1, ticks, power));
        result.add(assemble(family, "crafting_table", Map.of(BootstrapRecipes.PLANKS, 4), "minecraft:crafting_table", 1, ticks, power));
        result.add(assemble(family, "brick_block", Map.of("item:minecraft:brick", 4), "minecraft:bricks", 1, ticks, power));
        return List.copyOf(result);
    }

    private static ProcessRecipe assemble(MachineFamily family, String id, Map<String, Integer> grouped,
                                          String output, int count, int ticks, long power) {
        if (grouped.size() > 4) {
            throw new IllegalArgumentException("Primitive assembly exceeds four distinct inputs: " + id);
        }
        List<IngredientSpec> inputs = new ArrayList<>();
        grouped.forEach((qualified, amount) -> {
            if (qualified.startsWith("tag:")) {
                inputs.add(IngredientSpec.tag(qualified.substring(4), amount));
            } else if (qualified.startsWith("item:")) {
                inputs.add(IngredientSpec.item(qualified.substring(5), amount));
            } else {
                throw new IllegalArgumentException("Unknown crafting ingredient: " + qualified);
            }
        });
        String prefix = family == MachineFamily.ASSEMBLY_BENCH ? "bench/" : "assemble/bootstrap/";
        return new ProcessRecipe(prefix + id, family, inputs, List.of(OutputSpec.item(output, count)),
                Double.NaN, null, ticks, power, List.of(), family == MachineFamily.ASSEMBLY_BENCH ? "bootstrap" : null);
    }
}
