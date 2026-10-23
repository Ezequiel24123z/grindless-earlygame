package io.github.ezequiel24123z.grindless.shop;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.recipe.RenewableProcesses;
import io.github.ezequiel24123z.grindless.recipe.T1Recipes;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.sustainability.AuthorityAdapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Read-only T0 conversions. Recipes, prices and quest batches are never authored a second time. */
public final class EconomyGraph {
    public enum Boundary { CONVERSION, ENVIRONMENTAL_SOURCE, FINITE_QUEST }

    public record Row(String id, Map<String, Long> inputs, Map<String, Long> outputs,
                      Boundary boundary, Set<String> requires, String source,
                      long ticks, long fuPerTick, String family) {
        public Row {
            if (id == null || id.isBlank() || inputs == null || outputs == null
                    || outputs.isEmpty() && boundary != Boundary.FINITE_QUEST
                    || boundary == null || requires == null || source == null || source.isBlank()
                    || ticks < 0 || fuPerTick < 0) {
                throw new IllegalArgumentException("Invalid economy conversion");
            }
            inputs.values().forEach(EconomyGraph::requirePositive);
            outputs.values().forEach(EconomyGraph::requirePositive);
            inputs = Collections.unmodifiableMap(new TreeMap<>(inputs));
            outputs = Collections.unmodifiableMap(new TreeMap<>(outputs));
            requires = Set.copyOf(requires);
        }
    }

    private EconomyGraph() {
    }

    public static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        BootstrapProcesses.recipes().forEach(recipe -> rows.add(process(recipe, "BootstrapProcesses")));
        RenewableProcesses.recipes().forEach(recipe -> rows.add(process(recipe, "RenewableProcesses")));
        ProcessGraph.generate(AuthorityAdapters.materialViews()).stream()
                .filter(recipe -> recipe.family() == MachineFamily.ASSEMBLY_BENCH)
                .forEach(recipe -> rows.add(process(recipe, "BootstrapAssembly")));
        for (BootstrapRecipes.Shaped craft : BootstrapRecipes.shaped()) {
            rows.add(craft("craft/" + craft.name(), craft.pattern(), craft.key(), craft.result(),
                    craft.resultCount(), "BootstrapRecipes"));
        }
        for (BootstrapRecipes.Shapeless craft : BootstrapRecipes.shapeless()) {
            Map<String, Long> inputs = new TreeMap<>();
            craft.ingredients().forEach(input -> inputs.merge(input, 1L, Math::addExact));
            rows.add(new Row("craft/" + craft.name(), inputs, Map.of(item(craft.result()), 1L),
                    Boundary.CONVERSION, Set.of(), "BootstrapRecipes.shapeless", 0, 0, "CRAFTING_TABLE"));
        }
        T1Recipes.Shaped relay = relay();
        rows.add(craft("craft/" + relay.name(), relay.pattern(), relay.key(), relay.result(),
                relay.resultCount(), "T1Recipes"));
        rows.add(new Row("calibrate/data_core", Map.of(item(ResearchLogic.DATA_CORE), 1L),
                Map.of(item(ResearchLogic.CALIBRATED_DATA_CORE), 1L), Boundary.CONVERSION,
                Set.of(), "ResearchLogic", ResearchLogic.CYCLE_TICKS, ResearchLogic.FU_PER_TICK, "RESEARCH_TERMINAL"));
        for (ShopCatalogue.Offer offer : ShopCatalogue.offers()) {
            Map<String, Long> payment = new TreeMap<>();
            offer.payment().forEach((id, count) -> payment.put(item(id), (long) count));
            rows.add(new Row("shop/" + offer.id(), payment,
                    Map.of(item(offer.goods().item()), (long) offer.goods().count()), Boundary.CONVERSION,
                    offer.requires(), "ShopCatalogue", 0, 0, "RECOVERY_SHOP"));
        }
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            Map<String, Long> rewards = new TreeMap<>();
            task.rewards().forEach(reward -> rewards.merge(item(reward.itemId()), (long) reward.count(), Math::addExact));
            rows.add(new Row("quest/" + task.id(), Map.of("finite_claim:" + task.id(), 1L), rewards,
                    Boundary.FINITE_QUEST, Set.copyOf(task.requires()), "QuestCatalogue.rewards",
                    0, 0, "ONE_CLAIM_PER_PLAYER_WORLD"));
        }
        compatibility(rows, "oak_planks", Map.of(item("minecraft:oak_log"), 1L),
                Map.of(item("minecraft:oak_planks"), 4L));
        compatibility(rows, "sticks", Map.of(item("minecraft:oak_planks"), 2L),
                Map.of(item("minecraft:stick"), 4L));
        compatibility(rows, "crafting_table", Map.of(item("minecraft:oak_planks"), 4L),
                Map.of(item("minecraft:crafting_table"), 1L));
        compatibility(rows, "furnace", Map.of(item("minecraft:cobblestone"), 8L),
                Map.of(item("minecraft:furnace"), 1L));
        compatibility(rows, "bricks", Map.of(item("minecraft:brick"), 4L),
                Map.of(item("minecraft:bricks"), 1L));
        compatibility(rows, "clay", Map.of(item("minecraft:clay_ball"), 4L),
                Map.of(item("minecraft:clay"), 1L));
        compatibility(rows, "break_clay", Map.of(item("minecraft:clay"), 1L),
                Map.of(item("minecraft:clay_ball"), 4L));
        compatibility(rows, "bucket", Map.of(BootstrapRecipes.IRON, 3L),
                Map.of(item("minecraft:bucket"), 1L));
        compatibility(rows, "empty_bucket", Map.of(item("minecraft:water_bucket"), 1L),
                Map.of(item("minecraft:bucket"), 1L, "fluid:minecraft:water", 1000L));
        compatibility(rows, "grass_to_dirt", Map.of(item("minecraft:grass_block"), 1L),
                Map.of(item("minecraft:dirt"), 1L));
        compatibility(rows, "smelt_bricks", Map.of(item("minecraft:clay_ball"), 8L,
                item("minecraft:charcoal"), 1L), Map.of(item("minecraft:brick"), 8L));
        compatibility(rows, "smelt_glass", Map.of(item("minecraft:sand"), 8L,
                item("minecraft:charcoal"), 1L), Map.of(item("minecraft:glass"), 8L));
        compatibility(rows, "smelt_charcoal", Map.of(item("minecraft:oak_log"), 8L,
                item("minecraft:charcoal"), 1L), Map.of(item("minecraft:charcoal"), 8L));
        return List.copyOf(rows);
    }

    public static T1Recipes.Shaped relay() {
        return T1Recipes.shaped().stream().filter(craft -> craft.name().equals("relay_matrix"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing physical T0 exit recipe"));
    }

    public static Map<String, Long> shapedInputs(List<String> pattern, Map<String, String> key) {
        Map<String, Long> inputs = new TreeMap<>();
        for (String row : pattern) {
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ') {
                    String ingredient = key.get(String.valueOf(symbol));
                    if (ingredient == null) {
                        throw new IllegalArgumentException("Craft symbol has no ingredient");
                    }
                    inputs.merge(ingredient, 1L, Math::addExact);
                }
            }
        }
        return Collections.unmodifiableMap(inputs);
    }

    private static Row craft(String id, List<String> pattern, Map<String, String> key,
                             String output, int count, String source) {
        return new Row(id, shapedInputs(pattern, key), Map.of(item(output), (long) count),
                Boundary.CONVERSION, Set.of(), source, 0, 0, "CRAFTING_TABLE");
    }

    private static Row process(ProcessRecipe recipe, String source) {
        Map<String, Long> inputs = new TreeMap<>();
        Map<String, Long> outputs = new TreeMap<>();
        recipe.inputs().forEach(input -> inputs.merge(input.qualified(), (long) input.count(), Math::addExact));
        recipe.outputs().stream().filter(output -> !output.vented())
                .forEach(output -> outputs.merge(output.qualified(), (long) output.count(), Math::addExact));
        Boundary boundary = Boundary.CONVERSION;
        if (recipe.id().equals("source/geological_aggregate")) {
            inputs.put("environment:eligible_rock_bed", 1L);
            boundary = Boundary.ENVIRONMENTAL_SOURCE;
        } else if (recipe.id().equals("source/biomass")) {
            inputs.put("environment:eligible_sunlight", 1L);
            boundary = Boundary.ENVIRONMENTAL_SOURCE;
        }
        return new Row(recipe.id(), inputs, outputs, boundary, Set.of(), source,
                recipe.durationTicks(), recipe.fuPerTick(), recipe.family().name());
    }

    private static void compatibility(List<Row> rows, String id, Map<String, Long> inputs, Map<String, Long> outputs) {
        rows.add(new Row("vanilla/" + id, inputs, outputs, Boundary.CONVERSION, Set.of(),
                "Minecraft 1.20.1 compatibility conversion; not a renewable source",
                0, 0, "VANILLA_COMPATIBILITY"));
    }

    public static String item(String id) {
        return "item:" + id;
    }

    private static void requirePositive(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Economy quantities must be positive");
        }
    }
}
