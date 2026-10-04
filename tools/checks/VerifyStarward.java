package io.github.ezequiel24123z.grindless.star;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.network.PylonTier;
import io.github.ezequiel24123z.grindless.planet.LunarLinkLogic;
import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.structure.GroundArrayLogic;
import io.github.ezequiel24123z.grindless.vein.VeinGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

/** Behaviour checks for the Drift (ADR-0096). Not part of the mod. */
public final class VerifyStarward {

    private static final Path DATA = Path.of("common/src/main/resources/data");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        route();
        charge();
        recipe();
        world();

        System.out.println(failures == 0
                ? "ALL STARWARD CHECKS PASSED"
                : failures + " STARWARD CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void route() {
        List<Material> pack = List.of(
                new Material("iron", EnumSet.of(MaterialForm.ORE, MaterialForm.INGOT), 200));
        yes("the drift has no vein",
                PlanetCatalogue.veins(DriftCatalogue.DRIFT, pack).isEmpty());
        yes("an empty pool derives nothing",
                VeinGenerator.generate(11L, 0, 0, PlanetCatalogue.veins(DriftCatalogue.DRIFT, pack)) == null);
        yes("the overworld keeps the pack list",
                PlanetCatalogue.veins("minecraft:overworld", pack) == pack);
        yes("luna is still helium-3",
                PlanetCatalogue.HELIUM_3.equals(
                        PlanetCatalogue.veins(PlanetCatalogue.LUNA, pack).get(0).name()));
        yes("the drift emits no reagent",
                PlanetCatalogue.extractable(DriftCatalogue.DRIFT, "iron") == null);
        no("the drift is not luna", DriftCatalogue.isDrift(PlanetCatalogue.LUNA));
        eq("the deck is one layer", 1, DriftCatalogue.DECK_LAYERS);
    }

    private static void charge() {
        eq("leaving the star costs the array buffer", GroundArrayLogic.CAPACITY, StarwardLinkLogic.COST);
        eq("that buffer is ten seconds of MK3",
                PylonTier.MK3.throughput() * 200L, StarwardLinkLogic.COST);
        yes("the toll is more than a moon trip", StarwardLinkLogic.COST > LunarLinkLogic.COST);
        eq("nothing drawn stores nothing", 0L, StarwardLinkLogic.accept(0L, 0L));
        eq("a tick of MK3 adds", PylonTier.MK3.throughput(),
                StarwardLinkLogic.accept(0L, PylonTier.MK3.throughput()));
        eq("charge does not pass the cost", StarwardLinkLogic.COST,
                StarwardLinkLogic.accept(StarwardLinkLogic.COST - 10L, 50L));
        yes("the cost is enough", StarwardLinkLogic.ready(StarwardLinkLogic.COST));
        no("one short is not enough", StarwardLinkLogic.ready(StarwardLinkLogic.COST - 1L));
        eq("remaining is the gap", 10L, StarwardLinkLogic.remaining(StarwardLinkLogic.COST - 10L));
        eq("remaining is zero once paid", 0L, StarwardLinkLogic.remaining(StarwardLinkLogic.COST));
        yes("standing beside the pad is in range", StarwardLinkLogic.inRange(1, 0, 0));
        yes("standing on the pad is in range", StarwardLinkLogic.inRange(0, 1, 0));
        no("five blocks away is not", StarwardLinkLogic.inRange(5, 0, 0));
        eq("the pad sits one above the deck", DriftCatalogue.SURFACE_Y + 1, StarwardLinkLogic.ARRIVAL_Y);
    }

    private static void recipe() throws IOException {
        List<ProcessRecipe> recipes = ProcessGraph.generate(List.of());
        ProcessRecipe link = null;
        for (ProcessRecipe recipe : recipes) {
            if ("assemble/starward_link".equals(recipe.id())) {
                link = recipe;
            }
        }
        yes("the link is an assembler recipe", link != null);
        if (link == null) {
            return;
        }
        eq("the link is the assembler", MachineFamily.ASSEMBLER, link.family());
        eq("the link takes one lunar link", StarwardLinkLogic.LINKS, link.itemInputs().get(0).count());
        eq("that input is the lunar link", "item:grindless:lunar_link",
                link.itemInputs().get(0).qualified());
        eq("the link takes four array casings", StarwardLinkLogic.CASINGS,
                link.itemInputs().get(1).count());
        eq("those casings are the block", "item:grindless:array_casing",
                link.itemInputs().get(1).qualified());
        eq("the link makes one block", 1, link.itemOutputs().get(0).count());
        eq("the output is the starward link", "item:grindless:starward_link",
                link.itemOutputs().get(0).qualified());
        eq("the link is twenty seconds", 20 * 20, link.durationTicks());
        eq("the link draws F1", 32L, link.fuPerTick());
        eq("the link needs Industrial", "industrial", link.blueprint());
        yes("the link names no temperature", Double.isNaN(link.temperatureC()));
        Path recipesDir = DATA.resolve("grindless/recipes");
        no("the link has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("starward_link.json")));
        no("the deck has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("drift_deck.json")));
    }

    private static void world() throws IOException {
        JsonObject dimension = read(DATA.resolve("grindless/dimension/drift.json"));
        JsonObject settings = dimension.getAsJsonObject("generator").getAsJsonObject("settings");
        eq("the dimension uses the drift biome", "grindless:drift", settings.get("biome").getAsString());
        no("the flat world generates features", settings.get("features").getAsBoolean());
        no("the flat world generates lakes", settings.get("lakes").getAsBoolean());
        yes("structure overrides are empty",
                settings.getAsJsonArray("structure_overrides").isEmpty());
        JsonArray layers = settings.getAsJsonArray("layers");
        eq("two layers", 2, layers.size());
        eq("bedrock is one layer", DriftCatalogue.BEDROCK_LAYERS,
                layers.get(0).getAsJsonObject().get("height").getAsInt());
        eq("the bottom is bedrock", "minecraft:bedrock",
                layers.get(0).getAsJsonObject().get("block").getAsString());
        eq("the deck is one layer", DriftCatalogue.DECK_LAYERS,
                layers.get(1).getAsJsonObject().get("height").getAsInt());
        eq("the surface block is the deck", "grindless:drift_deck",
                layers.get(1).getAsJsonObject().get("block").getAsString());
        String dimensionText = Files.readString(DATA.resolve("grindless/dimension/drift.json"));
        no("the dimension does not name an ore", dimensionText.contains("ore"));
        no("the dimension does not name stone", dimensionText.contains("stone"));
        no("the dimension does not name regolith", dimensionText.contains("regolith"));
        no("the dimension does not name helium", dimensionText.contains("helium"));

        JsonObject type = read(DATA.resolve("grindless/dimension_type/drift.json"));
        eq("the sky is the vanilla end sky", "minecraft:the_end", type.get("effects").getAsString());
        no("the drift is not ultrawarm", type.get("ultrawarm").getAsBoolean());
        eq("the column starts at y 0", 0, type.get("min_y").getAsInt());
        eq("the surface constant matches that column",
                DriftCatalogue.BEDROCK_LAYERS + DriftCatalogue.DECK_LAYERS - 1,
                DriftCatalogue.SURFACE_Y);

        JsonObject biome = read(DATA.resolve("grindless/worldgen/biome/drift.json"));
        no("the drift has no rain", biome.get("has_precipitation").getAsBoolean());
        yes("monster spawns are empty",
                biome.getAsJsonObject("spawners").getAsJsonArray("monster").isEmpty());
        boolean featuresEmpty = true;
        for (var step : biome.getAsJsonArray("features")) {
            featuresEmpty &= step.getAsJsonArray().isEmpty();
        }
        yes("the biome adds no features", featuresEmpty);
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        yes(what, !actual);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
