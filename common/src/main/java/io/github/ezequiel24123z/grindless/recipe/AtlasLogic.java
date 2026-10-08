package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.ezequiel24123z.grindless.research.ResearchLogic;

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
     * @param station        which workstation performs the route
     * @param family         generated-process family, or {@code null} for a physical supplemental route
     * @param inputs         qualified item and fluid inputs
     * @param outputs        qualified item and fluid outputs, including vented fluids
     * @param catalysts      qualified unconsumed extras
     * @param temperatureC   required temperature, or {@code NaN} when unnamed
     * @param atmosphere     required atmosphere name, or {@code null} when unnamed
     * @param agitation      required agitation name, or {@code null} when unnamed
     * @param durationTicks  cycle length at full power
     * @param fuPerTick      draw while working
     */
    public record Entry(
            String id,
            String station,
            MachineFamily family,
            List<String> inputs,
            List<String> outputs,
            List<String> catalysts,
            double temperatureC,
            String atmosphere,
            String agitation,
            int durationTicks,
            long fuPerTick) {
    }

    /** Generated process recipes as sorted viewer rows: station, then id. */
    public static List<Entry> entries(List<ProcessRecipe> recipes) {
        List<Entry> rows = new ArrayList<>(recipes.size());
        for (ProcessRecipe recipe : recipes) {
            rows.add(entry(recipe));
        }
        return sorted(rows);
    }

    /**
     * Every live Grindless-owned route the handheld can explain.
     *
     * <p>Machine processes remain generated graph rows. The terminal cycle and authored shaped
     * recipes are supplemental physical routes, deliberately bounded to data the mod owns.
     */
    public static List<Entry> allEntries(List<ProcessRecipe> recipes) {
        List<Entry> rows = new ArrayList<>(entries(recipes));
        rows.add(calibration());
        for (T1Recipes.Shaped recipe : T1Recipes.shaped()) {
            rows.add(crafting(recipe));
        }
        return sorted(rows);
    }

    public static Entry entry(ProcessRecipe recipe) {
        List<String> inputs = new ArrayList<>();
        for (IngredientSpec spec : recipe.itemInputs()) {
            addUnits(inputs, spec.qualified(), spec.count());
        }
        for (IngredientSpec spec : recipe.fluidInputs()) {
            addUnits(inputs, spec.qualified(), spec.count());
        }
        List<String> outputs = new ArrayList<>();
        for (OutputSpec spec : recipe.itemOutputs()) {
            addUnits(outputs, spec.qualified(), spec.count());
        }
        for (OutputSpec spec : recipe.fluidOutputs()) {
            addUnits(outputs, spec.qualified(), spec.count());
        }
        List<String> catalysts = new ArrayList<>();
        for (IngredientSpec spec : recipe.catalysts()) {
            addUnits(catalysts, spec.qualified(), spec.count());
        }
        return new Entry(
                recipe.id(),
                recipe.family().name(),
                recipe.family(),
                List.copyOf(inputs),
                List.copyOf(outputs),
                List.copyOf(catalysts),
                recipe.temperatureC(),
                recipe.atmosphere(),
                recipe.agitation(),
                recipe.durationTicks(),
                recipe.fuPerTick());
    }

    /** Preserve recipe quantities in a viewer row so a four-unit batch never looks singular. */
    private static void addUnits(List<String> values, String qualified, int count) {
        for (int unit = 0; unit < count; unit++) {
            values.add(qualified);
        }
    }

    private static Entry calibration() {
        return new Entry(
                "calibrate/data_core",
                "RESEARCH_TERMINAL",
                null,
                List.of("item:" + ResearchLogic.DATA_CORE),
                List.of("item:" + ResearchLogic.CALIBRATED_DATA_CORE),
                List.of(),
                Double.NaN,
                null,
                null,
                ResearchLogic.CYCLE_TICKS,
                ResearchLogic.FU_PER_TICK);
    }

    private static Entry crafting(T1Recipes.Shaped recipe) {
        List<String> outputs = new ArrayList<>(recipe.resultCount());
        for (int count = 0; count < recipe.resultCount(); count++) {
            outputs.add("item:" + recipe.result());
        }
        return new Entry(
                "craft/" + recipe.name(),
                "CRAFTING_TABLE",
                null,
                craftingInputs(recipe),
                List.copyOf(outputs),
                List.of(),
                Double.NaN,
                null,
                null,
                0,
                0L);
    }

    private static List<String> craftingInputs(T1Recipes.Shaped recipe) {
        List<String> inputs = new ArrayList<>();
        for (String row : recipe.pattern()) {
            for (int index = 0; index < row.length(); index++) {
                char symbol = row.charAt(index);
                if (symbol == ' ') {
                    continue;
                }
                String ingredient = recipe.key().get(String.valueOf(symbol));
                if (ingredient == null) {
                    throw new IllegalArgumentException("missing crafting key " + symbol + " in " + recipe.name());
                }
                inputs.add(ingredient);
            }
        }
        return List.copyOf(inputs);
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
        text.append(row.station());
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
        if (row.agitation() != null && !row.agitation().isBlank()) {
            text.append("  ").append(row.agitation());
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
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String value : qualified) {
            counts.merge(value, 1, Integer::sum);
        }
        StringBuilder text = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (index++ > 0) {
                text.append(" + ");
            }
            if (entry.getValue() > 1) {
                text.append(entry.getValue()).append("x ");
            }
            text.append(shorten(entry.getKey()));
        }
        return text.toString();
    }

    /** Compact, count-aware text for the handheld's detail pane. */
    public static String describe(List<String> qualified) {
        return join(qualified);
    }

    private static List<Entry> sorted(List<Entry> rows) {
        rows.sort(Comparator.comparing(Entry::station).thenComparing(Entry::id));
        return List.copyOf(rows);
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
