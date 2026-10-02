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
        sounds();
        everyModelResolves();
        itemsAreNamedAndModelled();
        finish();
    }

    private static void finish() {
        System.out.println(failures == 0 ? "ALL ASSET CHECKS PASSED" : failures + " ASSET CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<String> registered(String file) throws IOException {
        return registered(file, "register");
    }

    private static List<String> registered(String file, String call) throws IOException {
        String source = Files.readString(SOURCES.resolve("registry/" + file));
        Matcher matcher = Pattern.compile(call + "\\(\"([a-z0-9_]+)\"").matcher(source);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static void catalogueMatchesRegistry() throws IOException {
        List<String> inCode = registered("ModBlocks.java");
        inCode.removeAll(BlockCatalogue.technical());
        List<String> inCatalogue = BlockCatalogue.blocks().stream().map(BlockCatalogue.Entry::name).toList();
        yes("BlockCatalogue lists exactly the player-facing blocks ModBlocks registers " + inCode,
                inCode.size() == inCatalogue.size() && inCode.containsAll(inCatalogue));
        yes("the pylon shaft has a blockstate",
                exists(ASSETS.resolve("blockstates/flux_pylon_shaft.json")));
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
            checkStates(block);
            yes(n + " has an item model", exists(ASSETS.resolve("models/item/" + n + ".json")));
            Path loot = RESOURCES.resolve("data/grindless/loot_tables/blocks/" + n + ".json");
            yes(n + " drops itself", exists(loot) && Files.readString(loot).contains("\"grindless:" + n + "\""));
            yes(n + " is mineable with a pickaxe", tagText.contains("\"grindless:" + n + "\""));
        }
    }

    /**
     * Every state of a block must have a model, and the variants must be exactly the cross product
     * of its properties: a missing one renders as the purple cube only in that state, which is the
     * hardest kind of bug to find by looking.
     */
    private static void checkStates(BlockCatalogue.Entry block) throws IOException {
        JsonObject variants = read(ASSETS.resolve("blockstates/" + block.name() + ".json")).getAsJsonObject("variants");
        int expected = 0;
        boolean complete = true;
        boolean rotated = true;
        List<String> facings = block.facing() ? BlockCatalogue.FACINGS : List.of("");
        for (String status : block.statuses()) {
            Path model = ASSETS.resolve("models/block/" + block.modelName(status) + ".json");
            complete &= exists(model);
            for (int i = 0; i < facings.size(); i++) {
                expected++;
                JsonObject variant = variants.has(block.variantKey(facings.get(i), status))
                        ? variants.getAsJsonObject(block.variantKey(facings.get(i), status)) : null;
                if (variant == null) {
                    complete = false;
                    continue;
                }
                int y = variant.has("y") ? variant.get("y").getAsInt() : 0;
                rotated &= y == (block.facing() ? BlockCatalogue.FACING_ROTATIONS.get(i) : 0);
                complete &= variant.get("model").getAsString().equals("grindless:block/" + block.modelName(status));
            }
        }
        yes(block.name() + " has a model for each of its " + expected + " states", complete);
        yes(block.name() + " has exactly those states and no others", variants.size() == expected);
        yes(block.name() + " turns its model to match its facing", rotated);
    }

    /** Every sound event must exist in code, in sounds.json and on disk, and be mono. */
    private static void sounds() throws IOException {
        List<String> inCode = registered("ModSounds.java", "sound");
        JsonObject events = read(ASSETS.resolve("sounds.json"));
        yes("ModSounds registers something", !inCode.isEmpty());
        yes("sounds.json defines exactly the events ModSounds registers " + inCode,
                events.size() == inCode.size() && inCode.stream().allMatch(events::has));
        JsonObject lang = read(ASSETS.resolve("lang/en_us.json"));
        for (String name : inCode) {
            Path file = ASSETS.resolve("sounds/" + name + ".ogg");
            yes(name + " has an ogg file", exists(file));
            yes(name + " is mono, or Minecraft will not place it in 3D", exists(file) && vorbisChannels(file) == 1);
            yes(name + " has a subtitle", lang.has("subtitles.grindless." + name));
        }
    }

    /** Reads the channel count from the Vorbis identification header. */
    private static int vorbisChannels(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        byte[] marker = {0x01, 'v', 'o', 'r', 'b', 'i', 's'};
        for (int i = 0; i + marker.length + 5 < Math.min(bytes.length, 200); i++) {
            boolean match = true;
            for (int j = 0; j < marker.length; j++) {
                match &= bytes[i + j] == marker[j];
            }
            if (match) {
                return bytes[i + marker.length + 4] & 0xFF;
            }
        }
        return -1;
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
        yes("the server-side state scenario covers every state",
                Files.readAllLines(Path.of("tools/smoke/states.expect")).size() == totalStates());
        yes("every texture and parent a model names exists" + (missing.isEmpty() ? "" : " " + missing),
                missing.isEmpty());
    }

    private static int totalStates() {
        int total = 0;
        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            total += block.statuses().size() * (block.facing() ? BlockCatalogue.FACINGS.size() : 1);
        }
        return total;
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
