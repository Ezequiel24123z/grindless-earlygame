package io.github.ezequiel24123z.grindless.assetgen;

import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;

/**
 * Writes every generated asset into the mod's resource tree.
 *
 * <pre>
 *     powershell -ExecutionPolicy Bypass -File .\tools\generate-assets.ps1 -Root .
 * </pre>
 *
 * <p>Deterministic: running it twice produces byte-identical files, so regenerating never shows up
 * as a spurious diff and a review only ever sees real changes.
 *
 * <p>This covers the matrix and the casings — the bulk by count, and the part where consistency is
 * the quality. It deliberately does <b>not</b> produce hero item sprites, complex models, entity
 * animation or music; ADR-0048 lists those as known gaps rather than leaving a future session to
 * assume art is handled.
 */
public final class GenerateAssets {

    /**
     * Starter palettes.
     *
     * <p>Only the colour is declared per material — the point of the matrix is that adding one is
     * a single value, not an art task. The real set comes from the runtime tag scan (ADR-0004);
     * these cover the bootstrap materials and prove the approach across warm, cool and neutral
     * metals, which is where a shared template is most likely to fall apart.
     */
    private static final List<Palette> PALETTES = List.of(
            Palette.of("copper", 0xB87333),
            Palette.of("iron", 0xD8D8D8),
            Palette.of("gold", 0xFFD700),
            Palette.of("tin", 0xDCE4E8),
            Palette.of("lead", 0x6E7A8A),
            Palette.of("silver", 0xE8E8F0),
            Palette.of("nickel", 0xC9C9A8),
            Palette.of("zinc", 0xB8C4C8),
            Palette.of("steel", 0x4A6E8A),
            Palette.of("aluminium", 0xCFD8DC),
            Palette.of("titanium", 0x9FA8AE),
            Palette.of("tungsten", 0x5A5A66),
            Palette.of("platinum", 0xBFC9D9));

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: GenerateAssets <repository-root>");
            System.exit(2);
        }
        File root = new File(args[0]);
        File items = resourceDir(root, "textures/item");
        File blocks = resourceDir(root, "textures/block");
        File sounds = new File(root, "tools/audio");

        int written = 0;

        // ---- the form x material matrix ----
        // Driven by SupplyCatalogue, the same list the mod registers its items from, so a texture
        // can never exist for an item that is not registered or the other way round. Vanilla's own
        // items (iron ingot, gold nugget, copper ingot...) are absent from it by design: Grindless
        // never registers a rival to something Minecraft already provides (ADR-0050).
        Set<String> expected = new HashSet<>();
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            Palette palette = paletteFor(entry.material());
            written += write(items, entry.itemName(), texture(entry.form(), palette));
            written += writeText(new File(resourceDir(root, "models/item"),
                    entry.itemName() + ".json"), itemModel(entry.itemName()));
            expected.add(entry.itemName());
        }
        removeStale(items, ".png", expected);
        removeStale(resourceDir(root, "models/item"), ".json", expected);
        written += writeTags(root);
        written += writeReagentTags(root);

        // ---- placeholder sprites, blocks, loot and mining tags ----
        written += write(items, "data_core", FormTextures.dataCore(Palette.of("data_core", MachineTextures.ACCENT)));
        written += write(items, "multitool", FormTextures.multitool(Palette.of("multitool", 0xC9A227)));
        written += write(items, "slag", FormTextures.slag(Palette.of("slag", 0x6B5B4B)));
        written += write(items, "prospectors_scanner",
                FormTextures.scanner(Palette.of("prospectors_scanner", 0x40C4FF)));
        written += write(items, "process_atlas",
                FormTextures.atlas(Palette.of("process_atlas", 0x26C6DA)));
        written += write(items, "flux_conduit",
                FormTextures.conduit(Palette.of("flux_conduit", 0x18FFFF)));
        written += write(items, "plate_die", FormTextures.die(Palette.of("plate_die", 0xB0BEC5)));
        written += write(items, "rod_die", FormTextures.die(Palette.of("rod_die", 0x90A4AE)));
        written += write(items, "gear_die", FormTextures.die(Palette.of("gear_die", 0xFFB74D)));
        written += write(items, "coil_die", FormTextures.die(Palette.of("coil_die", 0xB87333)));
        written += write(items, "copper_coil", FormTextures.coil(Palette.of("copper_coil", 0xB87333)));
        for (String sprite : BlockCatalogue.placeholderSprites()) {
            written += writeText(new File(resourceDir(root, "models/item"), sprite + ".json"),
                    itemModel(sprite));
        }
        written += writeBlocks(root);

        // ---- machine textures ----
        // Names are kept deliberately short. Minecraft's asset layout is already deep
        // (assets/grindless/textures/block/...), and on Windows a long checkout path plus a long
        // asset path exceeds MAX_PATH — the hazard ADR-0024 documents for source files applies
        // just as much here. "dynamo_front_idle" rather than "hand_crank_dynamo_front_idle" buys a
        // dozen characters for nothing.
        deleteTree(blocks);
        blocks = resourceDir(root, "textures/block");
        written += write(blocks, "casing_side", MachineTextures.side(MachineTextures.CASING));
        written += write(blocks, "casing_top", MachineTextures.top(MachineTextures.CASING));
        java.util.Set<io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry> tops =
                new java.util.HashSet<>();
        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            if (block.geometry() == BlockCatalogue.Geometry.PYLON) {
                continue;
            }
            if (tops.add(block.geometry())) {
                written += write(blocks, block.lower() + "_top", MachineArt.top(block.geometry()));
                for (String status : block.statuses()) {
                    written += write(blocks, block.lower() + "_front_" + status,
                            MachineArt.front(block.geometry(), status));
                }
            }
        }
        for (String status : List.of("idle", "running", "blocked", "starved")) {
            for (int tier = 1; tier <= 3; tier++) {
                written += write(blocks, "pylon" + tier + "_side_" + status, MachineArt.pylonSide(tier, status));
                written += write(blocks, "pylon" + tier + "_top_" + status, MachineArt.pylonTop(tier, status));
            }
        }
        written += writeGui(root);

        // ---- sounds ----
        // Loops are built from harmonics of their own loop frequency and must not be faded, or
        // the fade reintroduces the discontinuity the construction exists to avoid.
        SoundSynth.write(new File(sounds, "machine_hum.wav"),
                SoundSynth.hum(1.0, 60, 0.4), false);
        SoundSynth.write(new File(sounds, "machine_hum_heavy.wav"),
                SoundSynth.hum(1.0, 42, 0.6), false);
        SoundSynth.write(new File(sounds, "pylon_link.wav"),
                SoundSynth.chirp(0.45, 420, 1320), true);
        SoundSynth.write(new File(sounds, "relay_click.wav"),
                SoundSynth.click(0.08, 0.5), true);
        SoundSynth.write(new File(sounds, "brownout_alarm.wav"),
                SoundSynth.alarm(0.6, 330, 247), true);
        written += 5;

        writeProvenance(root, written);
        System.out.println("Generated " + written + " assets under "
                + root.toPath().resolve("common/src/main/resources/assets/grindless"));
    }

    private static Palette paletteFor(String material) {
        for (Palette palette : PALETTES) {
            if (palette.name().equals(material)) {
                return palette;
            }
        }
        throw new IllegalStateException("SupplyCatalogue lists '" + material
                + "' but PALETTES has no colour for it");
    }

    private static BufferedImage texture(MaterialForm form, Palette palette) {
        return switch (form) {
            case RAW -> FormTextures.raw(palette);
            case CRUSHED -> FormTextures.crushed(palette);
            case OXIDE -> FormTextures.oxide(palette);
            case DUST -> FormTextures.dust(palette);
            case NUGGET -> FormTextures.nugget(palette);
            case INGOT -> FormTextures.ingot(palette);
            case PLATE -> FormTextures.plate(palette);
            case ROD -> FormTextures.rod(palette);
            case BOLT -> FormTextures.bolt(palette);
            case GEAR -> FormTextures.gear(palette);
            case RING -> FormTextures.ring(palette);
            default -> throw new IllegalStateException("no texture for form " + form);
        };
    }

    private static String itemModel(String name) {
        return "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n"
                + "    \"layer0\": \"grindless:item/" + name + "\"\n  }\n}\n";
    }

    /**
     * Writes the tag files that put Grindless's items into the shared conventions.
     *
     * <p>Every file is {@code "replace": false}, so Grindless only ever <em>adds</em> to a tag and
     * can never remove another mod's entries. Conventional forms go under {@code forge:}; forms
     * with no agreed convention go under {@code grindless:} (ADR-0050).
     */
    private static int writeTags(File root) throws IOException {
        File data = new File(root, "common/src/main/resources/data");
        deleteTree(new File(data, "forge/tags/items"));
        deleteTree(new File(data, "grindless/tags/items"));

        Map<String, List<String>> parents = new TreeMap<>();
        int written = 0;
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            MaterialForm form = entry.form();
            String namespace = form.tagNamespace();
            String path = form.tagPath(entry.material());
            File file = new File(data, namespace + "/tags/items/" + path + ".json");
            written += writeText(file, tagJson("grindless:" + entry.itemName()));
            parents.computeIfAbsent(namespace + "/" + form.tagPath(), k -> new ArrayList<>())
                    .add("#" + namespace + ":" + path);
        }
        for (Map.Entry<String, List<String>> parent : parents.entrySet()) {
            File file = new File(data, parent.getKey().replaceFirst("/", "/tags/items/") + ".json");
            written += writeText(file, tagJson(parent.getValue().toArray(new String[0])));
        }
        return written;
    }

    /**
     * Reagents that are not material forms. Written after {@link #writeTags} wipes the
     * {@code grindless} item-tag tree, so they survive regeneration.
     */
    private static int writeReagentTags(File root) throws IOException {
        File file = new File(root, "common/src/main/resources/data/grindless/tags/items/carbon.json");
        return writeText(file, tagJson("#minecraft:coals"));
    }

    /** The shared machine menu background and a progress arrow strip. */
    private static int writeGui(File root) throws IOException {
        BufferedImage gui = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        fill(gui, 0, 0, 176, 166, 0xC6C6C6);
        fill(gui, 3, 3, 170, 160, 0x8B8B8B);
        fill(gui, 7, 15, 14, 54, 0x373737);
        fill(gui, 79, 35, 24, 17, 0x373737);
        // Arrow source at u=176.
        for (int x = 176; x < 200; x++) {
            for (int y = 0; y < 17; y++) {
                int mid = Math.abs(y - 8);
                int rgb = mid <= (x - 176) / 3 ? 0xFFFFFF : 0x00000000;
                if (rgb != 0) {
                    gui.setRGB(x, y, 0xFF00E5FF);
                }
            }
        }
        File dir = resourceDir(root, "textures/gui");
        return write(dir, "process_machine", gui);
    }

    private static void fill(BufferedImage img, int x, int y, int w, int h, int rgb) {
        int argb = 0xFF000000 | rgb;
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                img.setRGB(xx, yy, argb);
            }
        }
    }

    private static String tagJson(String... values) {
        StringBuilder out = new StringBuilder("{\n  \"replace\": false,\n  \"values\": [\n");
        for (int i = 0; i < values.length; i++) {
            out.append("    \"").append(values[i]).append('"');
            out.append(i + 1 < values.length ? ",\n" : "\n");
        }
        return out.append("  ]\n}\n").toString();
    }

    private static int writeText(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("could not create " + parent);
        }
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        return 1;
    }

    /**
     * Removes generated files that no longer correspond to a supplied item.
     *
     * <p>Only names shaped like a supply item are touched, so a hand-written model or texture in
     * the same directory is never deleted by a regeneration.
     */
    private static void removeStale(File dir, String extension, Set<String> expected) {
        Set<String> shaped = new HashSet<>();
        for (SupplyCatalogue.Supplied material : SupplyCatalogue.materials()) {
            for (MaterialForm form : SupplyCatalogue.forms()) {
                shaped.add(SupplyCatalogue.itemName(material.name(), form));
            }
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(extension));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - extension.length());
                if (shaped.contains(name) && !expected.contains(name) && file.delete()) {
                    System.out.println("removed stale " + file.getName());
                }
            }
        }
    }

    private static void deleteTree(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteTree(child);
                }
                child.delete();
            }
        }
        dir.delete();
    }

    /**
     * Blockstates, models, loot tables and the pickaxe tag for every block in the catalogue, and
     * the server-side scenario that proves each state is one the game accepts.
     *
     * <p>Without these a block renders as the missing-texture cube and, because machines require
     * the correct tool, breaks into nothing. The loot table is a plain self-drop; the pickaxe tag
     * is additive and is the one place Grindless writes into {@code minecraft:} (ADR-0051).
     */
    private static int writeBlocks(File root) throws IOException {
        File assets = assetRoot(root);
        File data = new File(root, "common/src/main/resources/data");
        deleteTree(new File(assets, "models/block"));
        int written = 0;
        List<String> names = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        List<String> expect = new ArrayList<>();
        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            String n = block.name();
            names.add("grindless:" + n);
            StringBuilder variants = new StringBuilder();
            for (String status : block.statuses()) {
                written += writeText(new File(assets, "models/block/" + block.modelName(status) + ".json"),
                        BlockModels.model(block, status));
                List<String> facings = block.facing() ? BlockCatalogue.FACINGS : List.of("");
                for (int i = 0; i < facings.size(); i++) {
                    if (variants.length() > 0) {
                        variants.append(",\n");
                    }
                    int rotation = block.facing() ? BlockCatalogue.FACING_ROTATIONS.get(i) : 0;
                    variants.append("    \"").append(block.variantKey(facings.get(i), status))
                            .append("\": { \"model\": \"grindless:block/").append(block.modelName(status)).append('"');
                    if (rotation != 0) {
                        variants.append(", \"y\": ").append(rotation);
                    }
                    variants.append(" }");
                    String state = block.facing()
                            ? "[facing=" + facings.get(i) + ",status=" + status + "]"
                            : "[status=" + status + "]";
                    commands.add("setblock 8 100 8 grindless:" + n + state);
                    commands.add("execute if block 8 100 8 grindless:" + n + state
                            + " run say STATE-OK " + n + state);
                    expect.add(("STATE-OK " + n + state).replace("[", "\\[").replace("]", "\\]"));
                }
            }
            written += writeText(new File(assets, "blockstates/" + n + ".json"),
                    "{\n  \"variants\": {\n" + variants + "\n  }\n}\n");
            if (block.geometry() == BlockCatalogue.Geometry.PYLON) {
                written += writeText(new File(assets, "models/item/" + n + ".json"),
                        BlockModels.pylonItem(block.tier()));
                for (String status : block.statuses()) {
                    written += writeText(new File(assets, "models/block/" + n + "_part1_" + status + ".json"),
                            BlockModels.pylonPart(block.tier(), 1, status));
                    written += writeText(new File(assets, "models/block/" + n + "_part2_" + status + ".json"),
                            BlockModels.pylonPart(block.tier(), 2, status));
                }
            } else {
                written += writeText(new File(assets, "models/item/" + n + ".json"),
                        "{\n  \"parent\": \"grindless:block/" + block.modelName("idle") + "\"\n}\n");
            }
            written += writeText(new File(data, "grindless/loot_tables/blocks/" + n + ".json"), lootTable(n));
        }
        written += writeShaft(assets, data, names);
        written += writeText(new File(assets, "models/item/" + BlockCatalogue.CASING_ITEM + ".json"),
                "{\n  \"parent\": \"minecraft:block/cube_bottom_top\",\n  \"textures\": {\n"
                        + "    \"top\": \"grindless:block/casing_top\",\n"
                        + "    \"bottom\": \"grindless:block/casing_top\",\n"
                        + "    \"side\": \"grindless:block/casing_side\"\n  }\n}\n");
        written += writeText(new File(data, "minecraft/tags/blocks/mineable/pickaxe.json"),
                tagJson(names.toArray(new String[0])));
        File smoke = new File(root, "tools/smoke");
        // One function, one tick: a belt that is empty publishes idle, and an extractor over a
        // vein publishes starved, so a 1s gap between setblock and execute-if misses STATE-OK
        // (ADR-0061). Hopper smokes still send console lines a second apart; they need the ticks.
        File function = new File(smoke, "states-pack/data/grindless/functions/states.mcfunction");
        written += writeText(function, String.join("\n", commands) + "\n");
        written += writeText(new File(smoke, "states-pack/pack.mcmeta"),
                "{\n  \"pack\": {\n    \"description\": \"Places every Grindless machine state in one tick\",\n"
                        + "    \"pack_format\": 15\n  }\n}\n");
        written += writeText(new File(smoke, "states.commands"),
                "forceload add 0 0\nfunction grindless:states\n");
        written += writeText(new File(smoke, "states.expect"), String.join("\n", expect) + "\n");
        return written;
    }

    /**
     * The two blocks above a pylon: occupancy and the middle/top models. Empty loot, pickaxe
     * mineable, no item.
     */
    private static int writeShaft(File assets, File data, List<String> pickaxe) throws IOException {
        StringBuilder variants = new StringBuilder();
        List<String> grid = BlockCatalogue.GRID;
        for (int tier = 1; tier <= 3; tier++) {
            for (int index = 1; index <= 2; index++) {
                for (String status : grid) {
                    if (variants.length() > 0) {
                        variants.append(",\n");
                    }
                    String model = "grindless:block/flux_pylon_mk" + tier + "_part" + index + "_" + status;
                    variants.append("    \"index=").append(index)
                            .append(",status=").append(status)
                            .append(",tier=").append(tier)
                            .append("\": { \"model\": \"").append(model).append("\" }");
                }
            }
        }
        int written = writeText(new File(assets, "blockstates/flux_pylon_shaft.json"),
                "{\n  \"variants\": {\n" + variants + "\n  }\n}\n");
        written += writeText(new File(data, "grindless/loot_tables/blocks/flux_pylon_shaft.json"),
                emptyLoot());
        pickaxe.add("grindless:flux_pylon_shaft");
        return written;
    }

    private static String emptyLoot() {
        return """
                {
                  "type": "minecraft:block",
                  "pools": []
                }
                """;
    }

    private static String lootTable(String block) {
        return """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "bonus_rolls": 0.0,
                      "entries": [
                        { "type": "minecraft:item", "name": "grindless:%s" }
                      ],
                      "conditions": [
                        { "condition": "minecraft:survives_explosion" }
                      ]
                    }
                  ]
                }
                """.formatted(block);
    }

    /**
     * Records that everything here is generated, and by what.
     *
     * <p>The README's provenance claim — that no Minecraft asset is redistributed and no
     * third-party art is vendored — is a promise, and a promise is worth more with a marker beside
     * the files it covers.
     */
    private static void writeProvenance(File root, int count) throws IOException {
        File readme = new File(assetRoot(root), "GENERATED.md");
        String text = """
                # Generated assets

                Every file under this directory is produced by `tools/assetgen` and written by
                `tools/generate-assets.ps1`. **Do not edit them by hand** — the next run overwrites
                them. Change the generator instead.

                Generation is deterministic: the same generator always produces byte-identical
                output, so regenerating never shows up as a spurious diff.

                Nothing here is copied from Minecraft or from any third-party mod, which is what
                lets the README state its provenance without qualification.

                What is **not** generated, and is therefore missing rather than merely plain, is
                listed in ADR-0048: hero item sprites, complex models, entity animation and music.
                A future session should treat those as known gaps with no owner, not as oversights.

                Current output: %d files.
                """.formatted(count);
        Files.writeString(readme.toPath(), text, StandardCharsets.UTF_8);
    }

    private static int write(File dir, String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "PNG", new File(dir, name + ".png"));
        return 1;
    }

    private static File assetRoot(File root) {
        return new File(root, "common/src/main/resources/assets/grindless");
    }

    private static File resourceDir(File root, String path) {
        File dir = new File(assetRoot(root), path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("could not create " + dir);
        }
        return dir;
    }

    private GenerateAssets() {
    }
}
