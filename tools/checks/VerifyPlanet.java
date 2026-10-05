package io.github.ezequiel24123z.grindless.planet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.network.CapacitorLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.vein.ChunkVein;
import io.github.ezequiel24123z.grindless.vein.VeinGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

/** Behaviour checks for Luna (ADR-0095). Not part of the mod. */
public final class VerifyPlanet {

    private static final Path DATA = Path.of("common/src/main/resources/data");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        pool();
        charge();
        recipe();
        world();

        System.out.println(failures == 0
                ? "ALL PLANET CHECKS PASSED"
                : failures + " PLANET CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void pool() {
        List<Material> pack = List.of(
                new Material("iron", EnumSet.of(MaterialForm.ORE, MaterialForm.INGOT), 200),
                new Material("copper", EnumSet.of(MaterialForm.RAW, MaterialForm.INGOT), 200));
        List<Material> luna = PlanetCatalogue.veins(PlanetCatalogue.LUNA, pack);
        eq("luna has one material", 1, luna.size());
        eq("that material is helium-3", PlanetCatalogue.HELIUM_3, luna.get(0).name());
        yes("the overworld keeps the pack list",
                PlanetCatalogue.veins("minecraft:overworld", pack) == pack);
        yes("the nether is not luna",
                PlanetCatalogue.veins("minecraft:the_nether", pack) == pack);
        eq("helium-3 extracts as the reagent", PlanetCatalogue.HELIUM_3_ITEM,
                PlanetCatalogue.extractable(PlanetCatalogue.LUNA, PlanetCatalogue.HELIUM_3));
        yes("the overworld does not emit that reagent",
                PlanetCatalogue.extractable("minecraft:overworld", "iron") == null);
        no("helium-3 is not a supply-catalogue item",
                SupplyCatalogue.byItemName("helium_3").isPresent());
        no("raw helium-3 is not a supply-catalogue item",
                SupplyCatalogue.byItemName("raw_helium_3").isPresent());

        boolean allHelium = true;
        boolean inBand = true;
        boolean richnessVaries = false;
        ChunkVein first = null;
        for (int x = 0; x < 64; x++) {
            ChunkVein vein = VeinGenerator.generate(11L, x, 4, luna);
            allHelium &= PlanetCatalogue.HELIUM_3.equals(vein.material());
            inBand &= vein.richness() >= VeinGenerator.MIN_RICHNESS
                    && vein.richness() <= VeinGenerator.MAX_RICHNESS;
            if (first == null) {
                first = vein;
            } else {
                richnessVaries |= vein.richness() != first.richness();
            }
        }
        yes("every luna chunk is helium-3", allHelium);
        yes("luna richness stays inside the vein band", inBand);
        yes("luna richness still varies between chunks", richnessVaries);
        yes("an overworld chunk can be iron",
                "iron".equals(VeinGenerator.generate(11L, 0, 4, pack).material())
                        || "copper".equals(VeinGenerator.generate(11L, 0, 4, pack).material()));
    }

    private static void charge() {
        eq("a departure costs one capacitor bank", CapacitorLogic.CAPACITY, LunarLinkLogic.COST);
        eq("nothing drawn stores nothing", 0L, LunarLinkLogic.accept(0L, 0L));
        eq("a tick of MK1 adds", 512L, LunarLinkLogic.accept(0L, 512L));
        eq("charge does not pass the cost", LunarLinkLogic.COST,
                LunarLinkLogic.accept(LunarLinkLogic.COST - 10L, 50L));
        yes("the cost is enough", LunarLinkLogic.ready(LunarLinkLogic.COST));
        no("one short is not enough", LunarLinkLogic.ready(LunarLinkLogic.COST - 1L));
        eq("remaining is the gap", 10L, LunarLinkLogic.remaining(LunarLinkLogic.COST - 10L));
        eq("remaining is zero once paid", 0L, LunarLinkLogic.remaining(LunarLinkLogic.COST));
        yes("standing beside the pad is in range", LunarLinkLogic.inRange(1, 0, 0));
        yes("standing on the pad is in range", LunarLinkLogic.inRange(0, 1, 0));
        no("five blocks away is not", LunarLinkLogic.inRange(5, 0, 0));
        eq("the pad sits one above the regolith", PlanetCatalogue.SURFACE_Y + 1, LunarLinkLogic.ARRIVAL_Y);
    }

    private static void recipe() throws IOException {
        List<ProcessRecipe> recipes = ProcessGraph.generate(List.of());
        ProcessRecipe link = null;
        for (ProcessRecipe recipe : recipes) {
            if ("assemble/lunar_link".equals(recipe.id())) {
                link = recipe;
            }
        }
        yes("the link is an assembler recipe", link != null);
        if (link == null) {
            return;
        }
        eq("the link is the assembler", MachineFamily.ASSEMBLER, link.family());
        eq("the link takes two array casings", LunarLinkLogic.CASINGS, link.itemInputs().get(0).count());
        eq("those casings are the block", "item:grindless:array_casing",
                link.itemInputs().get(0).qualified());
        eq("the link takes one machine casing", LunarLinkLogic.MACHINE_CASINGS,
                link.itemInputs().get(1).count());
        eq("that casing is the reagent", "item:grindless:machine_casing",
                link.itemInputs().get(1).qualified());
        eq("the link makes one block", 1, link.itemOutputs().get(0).count());
        eq("the output is the link", "item:grindless:lunar_link",
                link.itemOutputs().get(0).qualified());
        eq("the link is twenty seconds", 20 * 20, link.durationTicks());
        eq("the link draws F1", 32L, link.fuPerTick());
        eq("the link needs Industrial", "industrial", link.blueprint());
        yes("the link names no temperature", Double.isNaN(link.temperatureC()));
        no("helium-3 is not a process output",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("helium")));
        Path recipesDir = DATA.resolve("grindless/recipes");
        no("the link has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("lunar_link.json")));
        no("regolith has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("lunar_regolith.json")));
        no("helium-3 has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("helium_3.json")));
    }

    private static void world() throws IOException {
        JsonObject dimension = read(DATA.resolve("grindless/dimension/luna.json"));
        JsonObject settings = dimension.getAsJsonObject("generator").getAsJsonObject("settings");
        eq("the dimension uses the luna biome", "grindless:luna", settings.get("biome").getAsString());
        no("the flat world generates features", settings.get("features").getAsBoolean());
        no("the flat world generates lakes", settings.get("lakes").getAsBoolean());
        yes("structure overrides are empty",
                settings.getAsJsonArray("structure_overrides").isEmpty());
        JsonArray layers = settings.getAsJsonArray("layers");
        eq("two layers", 2, layers.size());
        eq("bedrock is one layer", PlanetCatalogue.BEDROCK_LAYERS,
                layers.get(0).getAsJsonObject().get("height").getAsInt());
        eq("the bottom is bedrock", "minecraft:bedrock",
                layers.get(0).getAsJsonObject().get("block").getAsString());
        eq("regolith is the rest", PlanetCatalogue.REGOLITH_LAYERS,
                layers.get(1).getAsJsonObject().get("height").getAsInt());
        eq("the surface block is regolith", "grindless:lunar_regolith",
                layers.get(1).getAsJsonObject().get("block").getAsString());
        String dimensionText = Files.readString(DATA.resolve("grindless/dimension/luna.json"));
        no("the dimension does not name an ore", dimensionText.contains("ore"));
        no("the dimension does not name stone", dimensionText.contains("stone"));

        JsonObject type = read(DATA.resolve("grindless/dimension_type/luna.json"));
        eq("the sky is the vanilla end sky", "minecraft:the_end", type.get("effects").getAsString());
        no("luna is not ultrawarm", type.get("ultrawarm").getAsBoolean());
        eq("the column starts at y 0", 0, type.get("min_y").getAsInt());
        eq("the surface constant matches that column",
                PlanetCatalogue.BEDROCK_LAYERS + PlanetCatalogue.REGOLITH_LAYERS - 1,
                PlanetCatalogue.SURFACE_Y);

        JsonObject biome = read(DATA.resolve("grindless/worldgen/biome/luna.json"));
        no("luna has no rain", biome.get("has_precipitation").getAsBoolean());
        yes("monster spawns are empty",
                biome.getAsJsonObject("spawners").getAsJsonArray("monster").isEmpty());
        boolean featuresEmpty = true;
        for (var step : biome.getAsJsonArray("features")) {
            featuresEmpty &= step.getAsJsonArray().isEmpty();
        }
        yes("the biome adds no features", featuresEmpty);

        String tag = Files.readString(DATA.resolve("grindless/tags/items/helium_3.json"));
        yes("helium-3 has a grindless item tag", tag.contains("\"grindless:helium_3\""));
        no("the tag is not a forge raw material",
                Files.isRegularFile(DATA.resolve("forge/tags/items/raw_materials/helium_3.json")));
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
