package io.github.ezequiel24123z.grindless.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Behaviour checks for the T0 bootstrap recipes and the Research Terminal. Not part of the mod. */
public final class VerifyBootstrap {

    private static final Path RECIPES = Path.of("common/src/main/resources/data/grindless/recipes");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        budget();
        recipes();
        research();

        System.out.println(failures == 0
                ? "ALL BOOTSTRAP CHECKS PASSED"
                : failures + " BOOTSTRAP CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void budget() {
        eq("the T0 loop spends two iron", 2, BootstrapRecipes.ironBudget());
        eq("the multitool spends none", 0, shaped("multitool").iron());
        eq("the dynamo spends one", 1, shaped("hand_crank_dynamo").iron());
        eq("the extractor spends one", 1, shaped("crude_extractor").iron());
        eq("the terminal spends none", 0, shaped("research_terminal").iron());
    }

    private static void recipes() throws IOException {
        for (BootstrapRecipes.Shaped recipe : BootstrapRecipes.shaped()) {
            JsonObject json = read(recipe.name());
            eq(recipe.name() + " is shaped", "minecraft:crafting_shaped",
                    json.get("type").getAsString());
            eq(recipe.name() + " result", recipe.result(),
                    json.getAsJsonObject("result").get("item").getAsString());
            List<String> pattern = strings(json.getAsJsonArray("pattern"));
            yes(recipe.name() + " pattern matches the catalogue", pattern.equals(recipe.pattern()));
            JsonObject key = json.getAsJsonObject("key");
            for (Map.Entry<String, String> entry : recipe.key().entrySet()) {
                yes(recipe.name() + " key " + entry.getKey(),
                        ingredientEquals(key.getAsJsonObject(entry.getKey()), entry.getValue()));
            }
            no(recipe.name() + " names no material item", namesMaterialItem(json));
        }
        for (BootstrapRecipes.Shapeless recipe : BootstrapRecipes.shapeless()) {
            JsonObject json = read(recipe.name());
            eq(recipe.name() + " is shapeless", "minecraft:crafting_shapeless",
                    json.get("type").getAsString());
            eq(recipe.name() + " result", recipe.result(),
                    json.getAsJsonObject("result").get("item").getAsString());
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            eq(recipe.name() + " ingredient count", recipe.ingredients().size(), ingredients.size());
            for (int i = 0; i < recipe.ingredients().size(); i++) {
                yes(recipe.name() + " ingredient " + i,
                        ingredientEquals(ingredients.get(i).getAsJsonObject(),
                                recipe.ingredients().get(i)));
            }
            no(recipe.name() + " names no material item", namesMaterialItem(json));
        }
    }

    private static void research() {
        eq("calibration takes thirty seconds", 20 * 30, ResearchLogic.CYCLE_TICKS);
        eq("it draws F0", 8L, ResearchLogic.FU_PER_TICK);
        eq("full power is one tick of work", 1.0, ResearchLogic.work(8L, 8L));
        eq("a brownout at half power is half work", 0.5, ResearchLogic.work(4L, 8L));
        eq("zero draw is zero work", 0.0, ResearchLogic.work(0L, 8L));
        eq("no core is idle", MachineStatus.IDLE,
                ResearchLogic.status(false, false, false, false));
        eq("a core and no power is starved", MachineStatus.STARVED,
                ResearchLogic.status(true, false, false, false));
        eq("powered and working is running", MachineStatus.RUNNING,
                ResearchLogic.status(true, false, true, true));
        eq("completed core is blocked until extracted", MachineStatus.BLOCKED,
                ResearchLogic.status(false, true, true, true));
        eq("calibration consumes a data core", "grindless:data_core", ResearchLogic.DATA_CORE);
        eq("calibration produces a physical core", "grindless:calibrated_data_core",
                ResearchLogic.CALIBRATED_DATA_CORE);
    }

    private static BootstrapRecipes.Shaped shaped(String name) {
        return BootstrapRecipes.shaped().stream()
                .filter(recipe -> recipe.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static JsonObject read(String name) throws IOException {
        Path file = RECIPES.resolve(name + ".json");
        yes(name + ".json exists", Files.isRegularFile(file));
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static boolean ingredientEquals(JsonObject json, String spec) {
        int split = spec.indexOf(':');
        String kind = spec.substring(0, split);
        String id = spec.substring(split + 1);
        return json.has(kind) && id.equals(json.get(kind).getAsString());
    }

    /**
     * True when a recipe names {@code minecraft:iron_ingot} (or another material form) by item
     * ID instead of by tag — the thing ADR-0050 forbids and VerifyMaterial already lints.
     */
    private static boolean namesMaterialItem(JsonObject json) {
        String text = json.toString();
        return text.contains("\"item\":\"minecraft:iron_ingot\"")
                || text.contains("\"item\":\"grindless:iron_ingot\"");
    }

    private static List<String> strings(JsonArray array) {
        List<String> list = new ArrayList<>();
        array.forEach(element -> list.add(element.getAsString()));
        return list;
    }

    private static void eq(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, MachineStatus expected, MachineStatus actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
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
        if (!actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
