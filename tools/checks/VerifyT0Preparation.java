package io.github.ezequiel24123z.grindless.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.T1Recipes;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks the T0 audit projection and original assets against the live catalogues and images. */
public final class VerifyT0Preparation {

    private static final Path ASSETS = Path.of("common", "src", "main", "resources", "assets", "grindless");
    private static final Set<Path> CHECKED_IMAGES = new HashSet<>();
    private static int failures;

    public static void main(String[] args) throws IOException {
        graph();
        assets();
        System.out.println("Checked " + CHECKED_IMAGES.size() + " distinct T0 PNGs.");
        System.out.println(failures == 0 ? "ALL T0 PREPARATION CHECKS PASSED"
                : failures + " T0 PREPARATION CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void graph() throws IOException {
        JsonObject graph = read(Path.of("docs", "tiers", "t0-production-graph.json"));
        check("the graph projects the active workshop rather than supplying runtime recipes",
                graph.get("status").getAsString().equals("active_runtime_audit"));
        check("the exact three new material roles are inventoried",
                new HashSet<>(strings(graph.getAsJsonArray("new_runtime_items"))).equals(
                        Set.of("ceramic_grog", "unfired_ceramic", "ceramic_insulator")));
        check("the exact four process machines are inventoried",
                new HashSet<>(strings(graph.getAsJsonArray("new_runtime_machines"))).equals(
                        Set.of("quern", "clay_mixer", "firing_chamber", "settling_trough")));
        check("the two static parts are inventoried", strings(graph.getAsJsonArray("new_runtime_parts"))
                .equals(List.of("settling_channel", "settling_screen")));
        check("both bounded structures are inventoried",
                strings(graph.getAsJsonArray("new_runtime_multiblocks"))
                        .equals(List.of("firing_chamber", "settling_trough")));
        for (String machine : strings(graph.getAsJsonArray("new_runtime_machines"))) {
            check(machine + " is registered", BlockCatalogue.block(machine) != null);
        }
        Set<String> sources = new HashSet<>(List.of("ExtractorLogic", "ResearchLogic"));
        BootstrapRecipes.shaped().forEach(recipe -> sources.add("BootstrapRecipes.shaped:" + recipe.name()));
        BootstrapRecipes.shapeless().forEach(recipe -> sources.add("BootstrapRecipes.shapeless:" + recipe.name()));
        T1Recipes.shaped().forEach(recipe -> sources.add("T1Recipes.shaped:" + recipe.name()));
        BootstrapProcesses.recipes().forEach(recipe -> sources.add("BootstrapProcesses:" + recipe.id()));
        Map<String, List<String>> edges = new HashMap<>();
        for (JsonElement element : graph.getAsJsonArray("nodes")) {
            JsonObject node = element.getAsJsonObject();
            String id = node.get("id").getAsString();
            check("graph id is unique: " + id, !edges.containsKey(id));
            edges.put(id, node.has("requires") ? strings(node.getAsJsonArray("requires")) : List.of());
            if (node.has("source")) {
                check("graph source exists: " + id, sources.contains(node.get("source").getAsString()));
            }
        }
        Set<String> visited = new HashSet<>();
        for (String id : edges.keySet()) {
            visit(id, edges, new HashSet<>(), visited);
        }
        check("all audit nodes resolve and the exit exists",
                visited.size() == edges.size() && visited.contains("bootstrap_exit"));
        try {
            visit("a", Map.of("a", List.of("b"), "b", List.of("a")), new HashSet<>(), new HashSet<>());
            check("cycle detection rejects a circular fixture", false);
        } catch (IllegalArgumentException expected) {
            check("cycle detection rejects a circular fixture", true);
        }
        try {
            visit("a", Map.of("a", List.of("missing")), new HashSet<>(), new HashSet<>());
            check("reachability rejects a missing prerequisite fixture", false);
        } catch (IllegalArgumentException expected) {
            check("reachability rejects a missing prerequisite fixture", true);
        }
    }

    private static void visit(String id, Map<String, List<String>> edges, Set<String> active, Set<String> visited) {
        if (visited.contains(id)) {
            return;
        }
        if (!edges.containsKey(id)) {
            throw new IllegalArgumentException("Missing graph prerequisite: " + id);
        }
        if (!active.add(id)) {
            throw new IllegalArgumentException("Circular graph prerequisite: " + id);
        }
        for (String required : edges.get(id)) {
            visit(required, edges, active, visited);
        }
        active.remove(id);
        visited.add(id);
    }

    private static void assets() throws IOException {
        JsonObject manifest = read(Path.of("docs", "assets", "t0-asset-manifest.json"));
        check("the asset manifest identifies the intentional runtime additions",
                manifest.get("runtime_changes").getAsBoolean());
        check("the fixed chamber has a visual composition contract",
                manifest.getAsJsonArray("multiblocks").size() == 2
                        && manifest.getAsJsonArray("multiblocks").get(0).getAsJsonObject()
                        .get("id").getAsString().equals("firing_chamber"));
        check("all seven T0 machines have asset contracts", manifest.getAsJsonArray("machines").size() == 7);
        JsonObject animation = manifest.getAsJsonObject("defaults").getAsJsonObject("animation");
        check("current placeholders are static", animation.get("frame_count").getAsInt() == 1
                && !animation.get("interpolate").getAsBoolean() && animation.get("mcmeta").isJsonNull());
        Set<String> ids = new HashSet<>();
        for (JsonElement element : manifest.getAsJsonArray("machines")) {
            JsonObject machine = element.getAsJsonObject();
            String id = machine.get("id").getAsString();
            String prefix = machine.get("prefix").getAsString();
            check("machine identifier is legal and unique: " + id, id.matches("[a-z0-9_]+") && ids.add(id));
            BlockCatalogue.Entry block = BlockCatalogue.block(id);
            if (block == null) {
                throw new IllegalArgumentException("Asset manifest names an unregistered machine: " + id);
            }
            check("manifest states match the catalogue: " + id,
                    strings(machine.getAsJsonArray("states")).equals(block.statuses()));
            JsonObject idle = read(ASSETS.resolve("models/block/" + block.modelName("idle") + ".json"))
                    .getAsJsonObject("textures");
            for (String state : block.statuses()) {
                JsonObject model = read(ASSETS.resolve("models/block/" + block.modelName(state) + ".json"));
                JsonObject textures = model.getAsJsonObject("textures");
                check(id + " has the declared " + state + " front",
                        textures.get("front").getAsString().equals("grindless:block/" + prefix + "_front_" + state));
                for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
                    if (!texture.getKey().equals("front")) {
                        check(id + " keeps stable " + texture.getKey() + " in " + state,
                                texture.getValue().equals(idle.get(texture.getKey())));
                    }
                    String reference = texture.getValue().getAsString();
                    if (reference.startsWith("grindless:")) {
                        png(ASSETS.resolve("textures/" + reference.substring("grindless:".length()) + ".png"));
                    }
                }
            }
            check(id + " has a block-item model", Files.isRegularFile(ASSETS.resolve("models/item/" + id + ".json")));
            BufferedImage resting = ImageIO.read(ASSETS.resolve("textures/block/" + prefix + "_front_idle.png").toFile());
            BufferedImage running = ImageIO.read(ASSETS.resolve("textures/block/" + prefix + "_front_running.png").toFile());
            check(id + " active pixels differ from idle", !Arrays.equals(
                    resting.getRGB(0, 0, 16, 16, null, 0, 16), running.getRGB(0, 0, 16, 16, null, 0, 16)));
        }
        for (JsonElement element : manifest.getAsJsonArray("items")) {
            String id = element.getAsJsonObject().get("id").getAsString();
            check("item identifier is legal and unique: " + id, id.matches("[a-z0-9_]+") && ids.add(id));
            check(id + " is in the sprite catalogue", BlockCatalogue.placeholderSprites().contains(id));
            png(ASSETS.resolve("textures/item/" + id + ".png"));
            check(id + " has an item model", Files.isRegularFile(ASSETS.resolve("models/item/" + id + ".json")));
        }
        check("both stateless parts have asset contracts", manifest.getAsJsonArray("parts").size() == 2);
        for (JsonElement element : manifest.getAsJsonArray("parts")) {
            JsonObject part = element.getAsJsonObject();
            String id = part.get("id").getAsString();
            String prefix = part.get("prefix").getAsString();
            BlockCatalogue.Entry block = BlockCatalogue.block(id);
            check(id + " is a unique, stateless authored part", ids.add(id) && !block.hasStatus()
                    && !part.get("block_entity").getAsBoolean());
            check(id + " has one visual variant", strings(part.getAsJsonArray("states"))
                    .equals(block.statuses()) && block.statuses().equals(List.of("idle")));
            png(ASSETS.resolve("textures/block/" + prefix + "_front_idle.png"));
            png(ASSETS.resolve("textures/block/" + prefix + "_top.png"));
        }
        BufferedImage channel = ImageIO.read(ASSETS.resolve("textures/block/channel_top.png").toFile());
        int[] edge = channel.getRGB(0, 0, 16, 1, null, 0, 16);
        for (String prefix : List.of("settler", "channel", "screen")) {
            BufferedImage top = ImageIO.read(ASSETS.resolve("textures/block/" + prefix + "_top.png").toFile());
            check(prefix + " has continuous entry/exit pixels",
                    Arrays.equals(edge, top.getRGB(0, 0, 16, 1, null, 0, 16))
                            && Arrays.equals(edge, top.getRGB(0, 15, 16, 1, null, 0, 16)));
        }
        BufferedImage preview = ImageIO.read(Path.of("docs", "assets", "t0-settling-preview.png").toFile());
        check("the assembled preview has the exact declared dimensions",
                preview != null && preview.getWidth() == 720 && preview.getHeight() == 256);
        if (preview != null && preview.getWidth() == 720 && preview.getHeight() == 256) {
            check("the preview distinguishes inactive and active controllers", !Arrays.equals(
                    preview.getRGB(0, 0, 360, 256, null, 0, 360),
                    preview.getRGB(360, 0, 360, 256, null, 0, 360)));
        }
    }

    private static void png(Path path) throws IOException {
        path = path.normalize();
        if (!path.startsWith(ASSETS)) {
            throw new IllegalArgumentException("Asset reference escapes the resource root: " + path);
        }
        if (!CHECKED_IMAGES.add(path)) {
            return;
        }
        byte[] bytes = Files.readAllBytes(path);
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        check(path.getFileName() + " has a PNG signature",
                bytes.length >= signature.length && Arrays.equals(Arrays.copyOf(bytes, 8), signature));
        BufferedImage image = ImageIO.read(path.toFile());
        check(path.getFileName() + " is 16x16 8-bit RGBA", bytes.length > 25
                && bytes[24] == 8 && bytes[25] == 6 && image != null && image.getWidth() == 16
                && image.getHeight() == 16 && image.getColorModel().hasAlpha());
        check(path.getFileName() + " has no undeclared animation",
                !Files.exists(path.resolveSibling(path.getFileName() + ".mcmeta")));
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static List<String> strings(JsonArray array) {
        List<String> result = new ArrayList<>();
        array.forEach(value -> result.add(value.getAsString()));
        return result;
    }

    private static void check(String label, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + label);
        if (!passed) {
            failures++;
        }
    }
}
