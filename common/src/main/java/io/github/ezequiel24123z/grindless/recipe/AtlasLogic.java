package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Queries the generated process graph for the Process Atlas (ADR-0066).
 *
 * <p>A lookup, not a solver. Given recipes, it answers which routes produce or consume a
 * thing and how to print a row. No Minecraft imports: {@code VerifyAtlas} dumps this without
 * booting the game. Target rates, machine counts and reachability stay ADR-0023.
 */
public final class AtlasLogic {

    private AtlasLogic() {
    }

    /**
     * One viewer row.
     *
     * @param id             recipe id, such as {@code roast/iron}
     * @param family         which machine runs it
     * @param inputs         qualified item and fluid inputs
     * @param outputs        qualified item and fluid outputs, including vented fluids
     * @param catalysts      qualified unconsumed extras
     * @param temperatureC   required temperature, or {@code NaN} when unnamed
     * @param atmosphere     required atmosphere name, or {@code null} when unnamed
     * @param durationTicks  cycle length at full power
     * @param fuPerTick      draw while working
     */
    public record Entry(
            String id,
            MachineFamily family,
            List<String> inputs,
            List<String> outputs,
            List<String> catalysts,
            double temperatureC,
            String atmosphere,
            int durationTicks,
            long fuPerTick) {
    }

    /** Every recipe as a sorted viewer row: family name, then id. */
    public static List<Entry> entries(List<ProcessRecipe> recipes) {
        List<Entry> rows = new ArrayList<>(recipes.size());
        for (ProcessRecipe recipe : recipes) {
            rows.add(entry(recipe));
        }
        rows.sort(Comparator.comparing((Entry row) -> row.family().name()).thenComparing(Entry::id));
        return List.copyOf(rows);
    }

    public static Entry entry(ProcessRecipe recipe) {
        List<String> inputs = new ArrayList<>();
        for (IngredientSpec spec : recipe.itemInputs()) {
            inputs.add(spec.qualified());
        }
        for (IngredientSpec spec : recipe.fluidInputs()) {
            inputs.add(spec.qualified());
        }
        List<String> outputs = new ArrayList<>();
        for (OutputSpec spec : recipe.itemOutputs()) {
            outputs.add(spec.qualified());
        }
        for (OutputSpec spec : recipe.fluidOutputs()) {
            outputs.add(spec.qualified());
        }
        List<String> catalysts = new ArrayList<>();
        for (IngredientSpec spec : recipe.catalysts()) {
            catalysts.add(spec.qualified());
        }
        return new Entry(
                recipe.id(),
                recipe.family(),
                List.copyOf(inputs),
                List.copyOf(outputs),
                List.copyOf(catalysts),
                recipe.temperatureC(),
                recipe.atmosphere(),
                recipe.durationTicks(),
                recipe.fuPerTick());
    }

    /** Routes whose outputs name {@code query} as a qualified id or as the id after the kind. */
    public static List<Entry> producing(List<ProcessRecipe> recipes, String query) {
        List<Entry> found = new ArrayList<>();
        for (Entry row : entries(recipes)) {
            if (namesAny(row.outputs(), query)) {
                found.add(row);
            }
        }
        return List.copyOf(found);
    }

    /** Routes whose inputs or catalysts name {@code query}. */
    public static List<Entry> consuming(List<ProcessRecipe> recipes, String query) {
        List<Entry> found = new ArrayList<>();
        for (Entry row : entries(recipes)) {
            if (namesAny(row.inputs(), query) || namesAny(row.catalysts(), query)) {
                found.add(row);
            }
        }
        return List.copyOf(found);
    }

    public static List<Entry> family(List<ProcessRecipe> recipes, MachineFamily family) {
        List<Entry> found = new ArrayList<>();
        for (Entry row : entries(recipes)) {
            if (row.family() == family) {
                found.add(row);
            }
        }
        return List.copyOf(found);
    }

    /** One compact line the stub screen can print. */
    public static String line(Entry row) {
        StringBuilder text = new StringBuilder();
        text.append(row.family().name());
        text.append("  ").append(row.id());
        text.append("  ").append(join(row.inputs()));
        if (!row.catalysts().isEmpty()) {
            text.append(" + ").append(join(row.catalysts()));
        }
        text.append(" -> ").append(join(row.outputs()));
        text.append("  ").append(duration(row.durationTicks()));
        text.append("  ").append(row.fuPerTick()).append(" FU/t");
        if (!Double.isNaN(row.temperatureC())) {
            text.append("  ").append((int) row.temperatureC()).append(" C");
        }
        if (row.atmosphere() != null && !row.atmosphere().isBlank()) {
            text.append("  ").append(row.atmosphere());
        }
        return text.toString();
    }

    public static boolean names(String qualified, String query) {
        if (query == null || query.isBlank() || qualified == null) {
            return false;
        }
        if (qualified.equals(query)) {
            return true;
        }
        int colon = qualified.indexOf(':');
        String id = colon < 0 ? qualified : qualified.substring(colon + 1);
        return id.equals(query);
    }

    private static boolean namesAny(List<String> qualified, String query) {
        for (String value : qualified) {
            if (names(value, query)) {
                return true;
            }
        }
        return false;
    }

    private static String join(List<String> qualified) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < qualified.size(); i++) {
            if (i > 0) {
                text.append(" + ");
            }
            text.append(shorten(qualified.get(i)));
        }
        return text.toString();
    }

    /** {@code tag:forge:ingots/iron} → {@code ingots/iron}. */
    static String shorten(String qualified) {
        int last = qualified.lastIndexOf(':');
        return last < 0 ? qualified : qualified.substring(last + 1);
    }

    private static String duration(int ticks) {
        if (ticks % 20 == 0) {
            return (ticks / 20) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
    }
}
