package io.github.ezequiel24123z.grindless.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Throwaway check of the compatibility rules (ADR-0050). Not part of the mod. */
public final class VerifyMaterial {

    private static final Path RESOURCES = Path.of("common/src/main/resources");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        catalogue();
        vanillaAndGrindlessOnly();
        anotherModProvidesIt();
        unknownMaterials();
        unifier();
        shippedData();
        recipeLint();

        System.out.println(failures == 0
                ? "ALL MATERIAL CHECKS PASSED"
                : failures + " MATERIAL CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ---- what Grindless registers ----------------------------------------------------------

    private static void catalogue() {
        no("never registers vanilla's iron ingot", SupplyCatalogue.isSupplied("iron", MaterialForm.INGOT));
        no("nor its iron nugget", SupplyCatalogue.isSupplied("iron", MaterialForm.NUGGET));
        no("nor its raw iron", SupplyCatalogue.isSupplied("iron", MaterialForm.RAW));
        no("nor vanilla's copper ingot", SupplyCatalogue.isSupplied("copper", MaterialForm.INGOT));
        no("nor its gold ingot", SupplyCatalogue.isSupplied("gold", MaterialForm.INGOT));
        no("nor its gold nugget", SupplyCatalogue.isSupplied("gold", MaterialForm.NUGGET));
        yes("but vanilla has no copper nugget, so Grindless supplies one",
                SupplyCatalogue.isSupplied("copper", MaterialForm.NUGGET));
        yes("vanilla has no iron plate either", SupplyCatalogue.isSupplied("iron", MaterialForm.PLATE));
        yes("or iron gear", SupplyCatalogue.isSupplied("iron", MaterialForm.GEAR));
        yes("or iron wire", SupplyCatalogue.isSupplied("iron", MaterialForm.WIRE));
        yes("vanilla has no copper wire either", SupplyCatalogue.isSupplied("copper", MaterialForm.WIRE));
        yes("a material no vanilla item covers is fully supplied",
                SupplyCatalogue.isSupplied("platinum", MaterialForm.INGOT)
                        && SupplyCatalogue.isSupplied("platinum", MaterialForm.RAW));
        no("an alloy has no raw form", SupplyCatalogue.isSupplied("steel", MaterialForm.RAW));
        no("nor a crushed form", SupplyCatalogue.isSupplied("steel", MaterialForm.CRUSHED));
        no("nor an oxide", SupplyCatalogue.isSupplied("steel", MaterialForm.OXIDE));
        no("nor a washed form", SupplyCatalogue.isSupplied("steel", MaterialForm.WASHED));
        no("nor a concentrate", SupplyCatalogue.isSupplied("steel", MaterialForm.CONCENTRATE));
        no("nor tailings", SupplyCatalogue.isSupplied("steel", MaterialForm.TAILINGS));
        yes("iron washed is supplied", SupplyCatalogue.isSupplied("iron", MaterialForm.WASHED));
        yes("iron concentrate is supplied", SupplyCatalogue.isSupplied("iron", MaterialForm.CONCENTRATE));
        yes("iron tailings are supplied", SupplyCatalogue.isSupplied("iron", MaterialForm.TAILINGS));
        yes("but it has an ingot", SupplyCatalogue.isSupplied("steel", MaterialForm.INGOT));
        yes("vanilla has no iron oxide, so Grindless supplies one",
                SupplyCatalogue.isSupplied("iron", MaterialForm.OXIDE));
        yes("and a platinum oxide", SupplyCatalogue.isSupplied("platinum", MaterialForm.OXIDE));

        Set<String> names = new HashSet<>();
        boolean unique = true;
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            unique &= names.add(entry.itemName());
        }
        yes("every item name is unique", unique);

        boolean noVanillaClash = true;
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            noVanillaClash &= !Set.of("iron_ingot", "gold_ingot", "copper_ingot", "iron_nugget",
                    "gold_nugget", "raw_iron", "raw_gold", "raw_copper").contains(entry.itemName());
        }
        yes("no registered name equals a vanilla item's name", noVanillaClash);
    }

    // ---- scan results ------------------------------------------------------------------------

    private static void vanillaAndGrindlessOnly() {
        FakeTags tags = new FakeTags();
        tags.add(MaterialForm.INGOT, "iron", "minecraft:iron_ingot");
        tags.add(MaterialForm.PLATE, "iron", "grindless:iron_plate");
        tags.add(MaterialForm.INGOT, "platinum", "grindless:platinum_ingot");
        tags.add(MaterialForm.RAW, "platinum", "grindless:raw_platinum");
        tags.add(MaterialForm.NUGGET, "copper", "grindless:copper_nugget");
        MaterialSnapshot snapshot = MaterialSnapshot.scan(tags, new Unifier(List.of()));

        yes("an iron plate is Grindless's when nobody else makes one",
                snapshot.fallbackActive("iron", MaterialForm.PLATE));
        yes("platinum is Grindless's when no mod provides it",
                snapshot.fallbackActive("platinum", MaterialForm.INGOT));
        yes("so is a copper nugget, which vanilla lacks",
                snapshot.fallbackActive("copper", MaterialForm.NUGGET));
        yes("vanilla iron is found", snapshot.material("iron").isPresent());
        eq("the iron ingot is vanilla's", "minecraft:iron_ingot",
                snapshot.preferred("iron", MaterialForm.INGOT).orElseThrow().toString());
        yes("platinum is found though only Grindless provides it",
                snapshot.material("platinum").isPresent());
        yes("and can be mined, because Grindless supplies its raw form",
                snapshot.material("platinum").orElseThrow().isMineable());
        eq("Grindless outputs its own platinum when it is the only one", "grindless:platinum_ingot",
                snapshot.preferred("platinum", MaterialForm.INGOT).orElseThrow().toString());
    }

    private static void anotherModProvidesIt() {
        FakeTags tags = new FakeTags();
        tags.add(MaterialForm.INGOT, "tin", "grindless:tin_ingot", "thermal:tin_ingot");
        tags.add(MaterialForm.DUST, "tin", "grindless:tin_dust");
        tags.add(MaterialForm.RAW, "platinum", "grindless:raw_platinum", "othermod:raw_platinum");
        MaterialSnapshot snapshot = MaterialSnapshot.scan(tags, new Unifier(List.of()));

        no("Grindless's tin ingot steps aside for another mod's",
                snapshot.fallbackActive("tin", MaterialForm.INGOT));
        eq("and the other mod's ingot is the one output", "thermal:tin_ingot",
                snapshot.preferred("tin", MaterialForm.INGOT).orElseThrow().toString());
        eq("both remain accepted as inputs", 2,
                snapshot.providers("tin", MaterialForm.INGOT).size());
        yes("another mod providing the ingot does not retire the dust",
                snapshot.fallbackActive("tin", MaterialForm.DUST));
        no("a foreign raw platinum retires Grindless's",
                snapshot.fallbackActive("platinum", MaterialForm.RAW));

        MaterialRegistry.rebuild(tags);
        yes("the creative tab hides the redundant ingot",
                MaterialRegistry.isRedundantFallback(new ResourceLocation("grindless", "tin_ingot")));
        no("but keeps the dust nobody else makes",
                MaterialRegistry.isRedundantFallback(new ResourceLocation("grindless", "tin_dust")));
        no("and never hides something that is not a supply item",
                MaterialRegistry.isRedundantFallback(new ResourceLocation("grindless", "multitool")));
        no("or another mod's item",
                MaterialRegistry.isRedundantFallback(new ResourceLocation("thermal", "tin_ingot")));

        MaterialRegistry.rebuild(new FakeTags());
        no("an empty pack hides nothing", MaterialRegistry.isRedundantFallback(
                new ResourceLocation("grindless", "tin_ingot")));
    }

    private static void unknownMaterials() {
        FakeTags tags = new FakeTags();
        tags.add(MaterialForm.INGOT, "mythril", "newmod:mythril_ingot");
        tags.add(MaterialForm.ORE, "mythril", "newmod:mythril_ore");
        tags.add(MaterialForm.INGOT, "bronze", "newmod:bronze_ingot");
        MaterialSnapshot snapshot = MaterialSnapshot.scan(tags, new Unifier(List.of()));

        yes("a material Grindless has never heard of is discovered",
                snapshot.material("mythril").isPresent());
        yes("with an ore it can be mined", snapshot.material("mythril").orElseThrow().isMineable());
        no("with only an ingot it is made, not mined",
                snapshot.material("bronze").orElseThrow().isMineable());
        yes("an unknown material needs no Grindless fallback", SupplyCatalogue.find("mythril", MaterialForm.INGOT).isEmpty());
    }

    private static void unifier() {
        List<ResourceLocation> mixed = ids("grindless:tin_ingot", "zeta:tin_ingot",
                "alpha:tin_ingot", "minecraft:tin_ingot");
        Unifier plain = new Unifier(List.of());
        eq("vanilla beats every mod", "minecraft:tin_ingot", plain.pick(mixed).orElseThrow().toString());

        List<ResourceLocation> mods = ids("grindless:tin_ingot", "zeta:tin_ingot", "alpha:tin_ingot");
        eq("with no preference, mods are ordered alphabetically", "alpha:tin_ingot",
                plain.pick(mods).orElseThrow().toString());
        eq("and Grindless is always last", "grindless:tin_ingot",
                plain.sorted(mods).get(2).toString());

        Unifier prefersZeta = new Unifier(List.of("zeta"));
        eq("a named namespace is preferred over the rest", "zeta:tin_ingot",
                prefersZeta.pick(mods).orElseThrow().toString());
        eq("but not over vanilla", "minecraft:tin_ingot",
                prefersZeta.pick(mixed).orElseThrow().toString());

        List<ResourceLocation> reversed = new ArrayList<>(mods);
        java.util.Collections.reverse(reversed);
        eq("the choice does not depend on input order", plain.pick(mods).orElseThrow().toString(),
                plain.pick(reversed).orElseThrow().toString());
        yes("nothing to pick from gives nothing", plain.pick(List.of()).isEmpty());
    }

    // ---- what the jar ships ------------------------------------------------------------------

    private static void shippedData() throws IOException {
        if (!Files.isDirectory(RESOURCES)) {
            fail("resources not found at " + RESOURCES.toAbsolutePath() + " - run from the project root");
            return;
        }
        boolean complete = true;
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            String name = entry.itemName();
            complete &= Files.exists(RESOURCES.resolve("assets/grindless/textures/item/" + name + ".png"));
            complete &= Files.exists(RESOURCES.resolve("assets/grindless/models/item/" + name + ".json"));
            Path tag = RESOURCES.resolve("data/" + entry.form().tagNamespace() + "/tags/items/"
                    + entry.form().tagPath(entry.material()) + ".json");
            if (Files.exists(tag)) {
                complete &= JsonParser.parseString(Files.readString(tag)).getAsJsonObject()
                        .getAsJsonArray("values").toString().contains("grindless:" + name);
            } else {
                complete = false;
            }
        }
        yes("every supplied item has a texture, a model and a tag entry", complete);

        Set<String> supplied = new HashSet<>();
        SupplyCatalogue.entries().forEach(e -> supplied.add(e.itemName()));
        supplied.addAll(io.github.ezequiel24123z.grindless.registry.BlockCatalogue.placeholderSprites());
        boolean noStray = true;
        try (Stream<Path> files = Files.list(RESOURCES.resolve("assets/grindless/textures/item"))) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString().replace(".png", "");
                noStray &= supplied.contains(name);
            }
        }
        yes("no texture exists for an item that is not registered (e.g. vanilla's)", noStray);

        boolean onlyMiningTags = true;
        Path vanilla = RESOURCES.resolve("data/minecraft");
        if (Files.exists(vanilla)) {
            try (Stream<Path> files = Files.walk(vanilla)) {
                for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                    // Windows relativizes with backslashes, so compare on a normalised separator.
                    // Without this the assertion is false on every Windows run of run-checks.ps1.
                    onlyMiningTags &= vanilla.relativize(file).toString().replace('\\', '/')
                            .startsWith("tags/blocks/mineable/");
                }
            }
        }
        yes("the only thing shipped into minecraft's namespace is a mining tag (ADR-0051)",
                onlyMiningTags);

        boolean additive = true;
        boolean foreignFree = true;
        int tags = 0;
        for (String namespace : List.of("forge", "grindless")) {
            Path root = RESOURCES.resolve("data/" + namespace + "/tags");
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                    tags++;
                    JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    additive &= json.has("replace") && !json.get("replace").getAsBoolean();
                    for (JsonElement value : json.getAsJsonArray("values")) {
                        String id = value.getAsString();
                        // A vanilla tag include (#minecraft:coals in grindless:carbon) is not a
                        // rival item. Sand and nether quartz are item ids because #minecraft:sand
                        // also contains red sand, and quartz has no vanilla item tag (ADR-0092).
                        // Hard-wiring another mod's item id still fails this check.
                        foreignFree &= id.startsWith("grindless:")
                                || id.startsWith("#" + namespace + ":")
                                || id.startsWith("#minecraft:")
                                || id.equals("minecraft:sand")
                                || id.equals("minecraft:quartz");
                    }
                }
            }
        }
        yes("scanned a meaningful number of tag files (" + tags + ")", tags > 100);
        yes("every shipped tag is additive and so can never remove another mod's entry", additive);
        yes("and lists only Grindless items or sibling tags, never another mod's items", foreignFree);
    }

    // ---- recipes -----------------------------------------------------------------------------

    private static void recipeLint() throws IOException {
        String hardWired = "{\"type\":\"minecraft:crafting_shaped\",\"key\":{\"A\":{\"item\":\"othermod:tin_ingot\"}}}";
        yes("the linter flags a recipe that hard-wires another mod's tin",
                !lintRecipe(JsonParser.parseString(hardWired)).isEmpty());
        yes("and one that names vanilla's iron ingot, which must be the tag",
                !lintRecipe(JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:iron_ingot\"}}")).isEmpty());
        String tagged = "{\"key\":{\"A\":{\"tag\":\"forge:ingots/tin\"},\"B\":{\"item\":\"grindless:machine_casing\"}}}";
        yes("but accepts a tag and a non-material Grindless item",
                lintRecipe(JsonParser.parseString(tagged)).isEmpty());

        Path data = RESOURCES.resolve("data");
        List<String> violations = new ArrayList<>();
        int recipes = 0;
        if (Files.isDirectory(data)) {
            try (Stream<Path> files = Files.walk(data)) {
                for (Path file : (Iterable<Path>) files
                        .filter(p -> p.toString().contains("/recipes/") && p.toString().endsWith(".json"))::iterator) {
                    recipes++;
                    for (String violation : lintRecipe(JsonParser.parseString(Files.readString(file)))) {
                        violations.add(file.getFileName() + ": " + violation);
                    }
                }
            }
        }
        yes("no shipped recipe hard-wires a material item (" + recipes + " recipes scanned)"
                + (violations.isEmpty() ? "" : " " + violations), violations.isEmpty());
    }

    /**
     * Finds ingredients that name a material item by ID instead of by tag.
     *
     * <p>Any {@code "item"} whose path is a material in a form — {@code tin_ingot},
     * {@code raw_iron} — is a violation whatever the namespace, because the ID belongs to one mod
     * and the recipe should accept all of them.
     */
    static List<String> lintRecipe(JsonElement json) {
        Set<String> materialItems = new HashSet<>();
        for (SupplyCatalogue.Supplied material : SupplyCatalogue.materials()) {
            for (MaterialForm form : SupplyCatalogue.forms()) {
                materialItems.add(SupplyCatalogue.itemName(material.name(), form));
            }
        }
        List<String> found = new ArrayList<>();
        walk(json, materialItems, found);
        return found;
    }

    private static void walk(JsonElement node, Set<String> materialItems, List<String> found) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            for (Map.Entry<String, JsonElement> member : object.entrySet()) {
                if (member.getKey().equals("item") && member.getValue().isJsonPrimitive()) {
                    String id = member.getValue().getAsString();
                    String path = id.substring(id.indexOf(':') + 1);
                    if (materialItems.contains(path)) {
                        found.add("ingredient names " + id + " instead of a tag");
                    }
                }
                walk(member.getValue(), materialItems, found);
            }
        } else if (node.isJsonArray()) {
            JsonArray array = node.getAsJsonArray();
            array.forEach(element -> walk(element, materialItems, found));
        }
    }

    // ---- helpers -----------------------------------------------------------------------------

    private static List<ResourceLocation> ids(String... ids) {
        List<ResourceLocation> list = new ArrayList<>();
        for (String id : ids) {
            list.add(new ResourceLocation(id));
        }
        return list;
    }

    /** A hand-built pack: which items sit in which tag. */
    private static final class FakeTags implements TagView {
        private final Map<MaterialForm, Map<String, Set<ResourceLocation>>> tags = new HashMap<>();

        void add(MaterialForm form, String material, String... items) {
            Set<ResourceLocation> set = tags.computeIfAbsent(form, f -> new HashMap<>())
                    .computeIfAbsent(material, m -> new HashSet<>());
            for (String item : items) {
                set.add(new ResourceLocation(item));
            }
        }

        @Override
        public Set<String> materials(MaterialForm form) {
            return tags.getOrDefault(form, Map.of()).keySet();
        }

        @Override
        public Set<ResourceLocation> items(MaterialForm form, String material) {
            return tags.getOrDefault(form, Map.of()).getOrDefault(material, Set.of());
        }
    }

    private static void eq(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            pass(what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            pass(what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void pass(String what) {
        System.out.println("  ok   " + what);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
