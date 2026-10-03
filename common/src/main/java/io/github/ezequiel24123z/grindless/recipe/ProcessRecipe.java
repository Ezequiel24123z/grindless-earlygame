package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;

/**
 * One generated process: inputs + conditions + time → outputs (ADR-0020).
 *
 * <p>Not a vanilla {@code Recipe}. The graph is rebuilt from tags with the material registry
 * (ADR-0005, ADR-0043) so lookup stays indexed and fluid slots can fill later without forking
 * the type (ADR-0058). Conditions are stored as primitives so this record has no Minecraft
 * imports and {@code VerifyRecipes} can check generation against the design.
 *
 * @param id             stable id, such as {@code b0_r1/iron}
 * @param family         which machine runs it
 * @param inputs         item and (later) fluid inputs; materials are tags
 * @param outputs        item outputs plus vented fluids
 * @param temperatureC   required temperature, or {@code NaN} when unnamed
 * @param atmosphere     required atmosphere name, or {@code null} when unnamed
 * @param durationTicks  cycle length at full power and optimal conditions
 * @param fuPerTick      draw while working
 * @param catalysts      dies and other unconsumed extras; empty when the recipe has none
 */
public record ProcessRecipe(
        String id,
        MachineFamily family,
        List<IngredientSpec> inputs,
        List<OutputSpec> outputs,
        double temperatureC,
        String atmosphere,
        int durationTicks,
        long fuPerTick,
        List<IngredientSpec> catalysts) {

    public ProcessRecipe(String id, MachineFamily family, List<IngredientSpec> inputs,
                         List<OutputSpec> outputs, double temperatureC, String atmosphere,
                         int durationTicks, long fuPerTick) {
        this(id, family, inputs, outputs, temperatureC, atmosphere, durationTicks, fuPerTick,
                List.of());
    }

    public ProcessRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        catalysts = catalysts == null ? List.of() : List.copyOf(catalysts);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("recipe id is required");
        }
        if (family == null) {
            throw new IllegalArgumentException("family is required");
        }
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (fuPerTick < 0L) {
            throw new IllegalArgumentException("draw cannot be negative");
        }
    }

    public boolean namesTemperature() {
        return !Double.isNaN(temperatureC);
    }

    public boolean namesAtmosphere() {
        return atmosphere != null && !atmosphere.isBlank();
    }

    public List<IngredientSpec> itemInputs() {
        return inputs.stream().filter(spec -> !spec.isFluid()).toList();
    }

    public List<IngredientSpec> fluidInputs() {
        return inputs.stream().filter(IngredientSpec::isFluid).toList();
    }

    public List<OutputSpec> itemOutputs() {
        return outputs.stream().filter(OutputSpec::isItem).toList();
    }

    public List<OutputSpec> fluidOutputs() {
        return outputs.stream().filter(OutputSpec::isFluid).toList();
    }

    public List<OutputSpec> ventedOutputs() {
        return outputs.stream().filter(OutputSpec::vented).toList();
    }
}
