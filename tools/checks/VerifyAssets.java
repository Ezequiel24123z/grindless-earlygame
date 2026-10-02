package io.github.ezequiel24123z.grindless.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Throwaway check that every registered block and item can be drawn, dropped and named. */
public final class VerifyAssets {

    private static final Path RESOURCES = Path.of("common/src/main/resources");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
    private static final Path ASSETS = RESOURCES.resolve("assets/grindless");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        if (!Files.isDirectory(ASSETS)) {
            fail("resources not found at " + ASSETS.toAbsolutePath() + " - run from the project root");
            finish();
            return;
        }
        catalogueMatchesRegistry();
        blocks();
        everyModelResolves();
        itemsAreNamedAndModelled();
        finish();
    }

    private static void finish() {
        System.out.println(failures == 0 ? "ALL ASSET CHECKS PASSED" : failures + " ASSET CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<String> registered(String file) throws IOException {
        String source = Files.readString(SOURCES.resolve("registry/" + file));
        Matcher matcher = Pattern.compile("register\\(\"([a-z0-9_]+)\"").matcher(source);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static void catalogueMatchesRegistry() throws IOException {
        List<String> inCode = registered("ModBlocks.java");
        List<String> inCatalogue = BlockCatalogue.blocks().stream().map(BlockCatalogue.Entry::name).toList();
        yes("BlockCatalogue lists exactly the blocks ModBlocks registers " + inCode,
                inCode.size() == inCatalogue.size() && inCode.containsAll(inCatalogue));
    }

    private static void blocks() throws IOException {
        JsonObject pickaxe = read(RESOURCES.resolve("data/minecraft/tags/blocks/mineable/pickaxe.json"));
        yes("the pickaxe tag is additive", pickaxe != null && !pickaxe.get("replace").getAsBoolean());
        String tagText = pickaxe == null ? "" : pickaxe.getAsJsonArray("values").toString();

        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            String n = block.name();
            yes(n + " has a blockstate pointing at its model",
                    exists(ASSETS.resolve("blockstates/" + n + ".json"))
                            && Files.readString(ASSETS.resolve("blockstates/" + n + ".json"))
                            .contains("grindless:block/" + n));
            yes(n + " has a block model", exists(ASSETS.resolve("models/block/" + n + ".json")));
            yes(n + " has an item model", exists(ASSETS.resolve("models/item/" + n + ".json")));
            Path loot = RESOURCES.resolve("data/grindless/loot_tables/blocks/" + n + ".json");
            yes(n + " drops itself", exists(loot) && Files.readString(loot).contains("\"grindless:" + n + "\""));
            yes(n + " is mineable with a pickaxe", tagText.contains("\"grindless:" + n + "\""));
        }
    }

    /** Every texture and parent a Grindless model names must exist, or the game draws a purple cube. */
    private static void everyModelResolves() throws IOException {
        List<String> missing = new ArrayList<>();
        int models = 0;
        try (Stream<Path> files = Files.walk(ASSETS.resolve("models"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                models++;
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.has("parent")) {
                    checkParent(json.get("parent").getAsString(), file, missing);
                }
                if (json.has("textures")) {
                    for (var texture : json.getAsJsonObject("textures").entrySet()) {
                        String id = texture.getValue().getAsString();
                        if (id.startsWith("grindless:")
                                && !exists(ASSETS.resolve("textures/" + id.substring(10) + ".png"))) {
                            missing.add(file.getFileName() + " -> " + id);
                        }
                    }
                }
            }
        }
        yes("scanned " + models + " models", models > 100);
        yes("every texture and parent a model names exists" + (missing.isEmpty() ? "" : " " + missing),
                missing.isEmpty());
    }

    private static void checkParent(String parent, Path file, List<String> missing) {
        if (parent.startsWith("grindless:") && !exists(ASSETS.resolve("models/" + parent.substring(10) + ".json"))) {
            missing.add(file.getFileName() + " -> " + parent);
        }
    }

    private static void itemsAreNamedAndModelled() throws IOException {
        JsonObject lang = read(ASSETS.resolve("lang/en_us.json"));
        List<String> unnamed = new ArrayList<>();
        List<String> unmodelled = new ArrayList<>();
        for (String item : registered("ModItems.java")) {
            if (!lang.has("item.grindless." + item)) {
                unnamed.add(item);
            }
            if (!exists(ASSETS.resolve("models/item/" + item + ".json"))) {
                unmodelled.add(item);
            }
        }
        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            if (!lang.has("block.grindless." + block.name())) {
                unnamed.add(block.name());
            }
        }
        yes("every hand-registered item and block has a display name" + (unnamed.isEmpty() ? "" : " " + unnamed),
                unnamed.isEmpty());
        yes("and every hand-registered item has a model" + (unmodelled.isEmpty() ? "" : " " + unmodelled),
                unmodelled.isEmpty());
        for (String sprite : BlockCatalogue.placeholderSprites()) {
            yes(sprite + " has a sprite", exists(ASSETS.resolve("textures/item/" + sprite + ".png")));
        }
    }

    private static JsonObject read(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        JsonElement json = JsonParser.parseString(Files.readString(file));
        return json.getAsJsonObject();
    }

    private static boolean exists(Path file) {
        return Files.exists(file);
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
