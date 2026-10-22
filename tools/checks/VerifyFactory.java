package io.github.ezequiel24123z.grindless.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.T1Recipes;
import io.github.ezequiel24123z.grindless.vein.ChunkVein;
import io.github.ezequiel24123z.grindless.vein.VeinGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Behaviour checks for the construction world and its non-free resource fields. */
public final class VerifyFactory {
    private static final Path DATA = Path.of("common/src/main/resources/data");
    private static final IngredientSpec PORTAL_CONTROL =
            IngredientSpec.minimumControl("grindless:relay_matrix", FluxTier.F1, 1);
    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        resourceFields();
        portalRecipe();
        world();
        System.out.println(failures == 0 ? "ALL FACTORY CHECKS PASSED"
                : failures + " FACTORY CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void resourceFields() {
        List<Material> pack = List.of(
                new Material("iron", EnumSet.of(MaterialForm.RAW, MaterialForm.INGOT), 200),
                new Material("copper", EnumSet.of(MaterialForm.RAW, MaterialForm.INGOT), 100));
        List<Material> factory = PlanetCatalogue.veins(FactoryCatalogue.FACTORY, pack);
        yes("factory keeps the installed pack material pool", factory == pack);
        ChunkVein vein = VeinGenerator.generate(91L, 3, -4, factory);
        yes("a factory chunk has a Grindless field", vein != null);
        yes("the field is one of the installed materials",
                "iron".equals(vein.material()) || "copper".equals(vein.material()));
        yes("a factory field is finite", vein.reserve() > 0L);
    }

    private static void portalRecipe() throws IOException {
        T1Recipes.Shaped portal = T1Recipes.shaped().stream()
                .filter(recipe -> recipe.name().equals("factory_portal"))
                .findFirst().orElse(null);
        yes("the portal is a physical T1 craft", portal != null);
        if (portal != null) {
            yes("the portal retains its exact typed bill with one minimum-F1 Matrix",
                    hasPortalBill(portal.ingredients()));
            JsonObject shipped = read(DATA.resolve("grindless/recipes/factory_portal.json"));
            eq("the shipped portal requires the same typed minimum-F1 control", PORTAL_CONTROL,
                    IngredientSpec.manualControl(shipped.getAsJsonObject("key").getAsJsonObject("M")));
            eq("the portal is one placed block", "grindless:factory_portal", portal.result());
            eq("the portal still produces exactly one ordinary block", 1, portal.resultCount());
            int control = portal.ingredients().indexOf(PORTAL_CONTROL);
            if (control >= 0) {
                for (IngredientSpec changed : List.of(
                        IngredientSpec.item(PORTAL_CONTROL.id(), 1),
                        IngredientSpec.matrix(PORTAL_CONTROL.id(), FluxTier.F1, 1),
                        IngredientSpec.minimumControl(PORTAL_CONTROL.id(), FluxTier.F2, 1),
                        IngredientSpec.minimumControl(PORTAL_CONTROL.id(), FluxTier.F1, 2))) {
                    List<IngredientSpec> inputs = new ArrayList<>(portal.ingredients());
                    inputs.set(control, changed);
                    no("the portal oracle rejects a changed control contract: " + changed, hasPortalBill(inputs));
                }
            }
        }
        no("the portal is not a generated machine recipe", ProcessGraph.generate(List.of()).stream()
                .anyMatch(recipe -> recipe.id().equals("assemble/factory_portal")));
    }

    private static boolean hasPortalBill(List<IngredientSpec> inputs) {
        return inputs.stream().collect(Collectors.groupingBy(input -> input, Collectors.counting()))
                .equals(Map.of(
                        IngredientSpec.item("minecraft:glass", 1), 4L,
                        IngredientSpec.tag("forge:ingots/copper", 1), 2L,
                        IngredientSpec.item("minecraft:redstone", 1), 2L,
                        PORTAL_CONTROL, 1L));
    }

    private static void world() throws IOException {
        JsonObject dimension = read(DATA.resolve("grindless/dimension/factory.json"));
        JsonObject settings = dimension.getAsJsonObject("generator").getAsJsonObject("settings");
        eq("the factory uses its own biome", "grindless:factory", settings.get("biome").getAsString());
        no("the factory has terrain features", settings.get("features").getAsBoolean());
        no("the factory has lakes", settings.get("lakes").getAsBoolean());
        JsonArray layers = settings.getAsJsonArray("layers");
        eq("the factory floor has three layers", 3, layers.size());
        eq("the factory starts with bedrock", "minecraft:bedrock",
                layers.get(0).getAsJsonObject().get("block").getAsString());
        eq("the factory has three dirt layers", FactoryCatalogue.DIRT_LAYERS,
                layers.get(1).getAsJsonObject().get("height").getAsInt());
        eq("the visible floor is grass", "minecraft:grass_block",
                layers.get(2).getAsJsonObject().get("block").getAsString());
        JsonObject type = read(DATA.resolve("grindless/dimension_type/factory.json"));
        eq("the factory stays at midday", FactoryCatalogue.MIDDAY, type.get("fixed_time").getAsLong());
        yes("the factory has a sky", type.get("has_skylight").getAsBoolean());
        eq("the factory uses overworld sky effects", "minecraft:overworld", type.get("effects").getAsString());
        JsonObject biome = read(DATA.resolve("grindless/worldgen/biome/factory.json"));
        no("the factory has rain", biome.get("has_precipitation").getAsBoolean());
        yes("the factory has no hostile spawns",
                biome.getAsJsonObject("spawners").getAsJsonArray("monster").isEmpty());
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected.equals(actual)) { System.out.println("  ok   " + what); }
        else { fail(what + ": expected " + expected + " but was " + actual); }
    }
    private static void yes(String what, boolean actual) {
        if (actual) { System.out.println("  ok   " + what); }
        else { fail(what + ": expected true"); }
    }
    private static void no(String what, boolean actual) { yes(what, !actual); }
    private static void fail(String what) { failures++; System.out.println("  FAIL " + what); }
}
