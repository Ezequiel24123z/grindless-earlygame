package io.github.ezequiel24123z.grindless.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Throwaway check that every registered block and item can be drawn, dropped and named. */
public final class VerifyAssets {

    private static final Path RESOURCES = Path.of("common/src/main/resources");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
    private static final Path ASSETS = RESOURCES.resolve("assets/grindless");
    // Minecraft 1.20.1: display/gui_light only, not a second source of geometry or textures.
    private static final String NATIVE_BLOCK_PRESENTATION = "minecraft:block/block";

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        if (!Files.isDirectory(ASSETS)) {
            fail("resources not found at " + ASSETS.toAbsolutePath() + " - run from the project root");
            finish();
            return;
        }
        if (args.length == 1 && "--sounds-only".equals(args[0])) {
            sounds();
            finish();
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("usage: VerifyAssets [--sounds-only]");
        }
        catalogueMatchesRegistry();
        blocks();
        blockItemPresentation();
        presentationRegressionCases();
        sounds();
        everyModelResolves();
        textureReferenceRegressionCases();
        itemsAreNamedAndModelled();
        thermalGeneratorCore();
        physicalItemSprites();
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
        Map<BlockCatalogue.MiningTool, Set<String>> mining = new EnumMap<>(BlockCatalogue.MiningTool.class);
        for (BlockCatalogue.MiningTool tool : BlockCatalogue.MiningTool.values()) {
            JsonObject tag = read(RESOURCES.resolve("data/minecraft/tags/blocks/" + tool.tagPath() + ".json"));
            yes("the " + tool.tagPath() + " tag is additive",
                    tag != null && tag.has("replace") && !tag.get("replace").getAsBoolean());
            Set<String> values = new HashSet<>();
            if (tag != null && tag.has("values")) {
                tag.getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
            }
            mining.put(tool, values);
        }

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
            yes(n + " is mineable only with its declared " + block.miningTool().tagPath(),
                    mining.get(block.miningTool()).contains("grindless:" + n)
                            && mining.entrySet().stream()
                            .filter(entry -> entry.getKey() != block.miningTool())
                            .noneMatch(entry -> entry.getValue().contains("grindless:" + n)));
        }
        for (String technical : BlockCatalogue.technical()) {
            // The shaft uses the separately assembled pylon parts, not BlockModels.model().
            if (technical.equals("flux_pylon_shaft")) {
                continue;
            }
            boolean presentation = true;
            for (String status : BlockCatalogue.GRID) {
                presentation &= isSharedBlockModel(
                        read(ASSETS.resolve("models/block/" + technical + "_" + status + ".json")));
            }
            yes(technical + " shares native presentation without replacing its mass geometry", presentation);
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
        boolean presentation = true;
        List<String> facings = block.facing() ? BlockCatalogue.FACINGS : List.of("");
        for (String status : block.statuses()) {
            Path model = ASSETS.resolve("models/block/" + block.modelName(status) + ".json");
            complete &= exists(model);
            presentation &= isSharedBlockModel(read(model));
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
        yes(block.name() + " keeps local geometry and inherits native block presentation", presentation);
    }

    private static boolean isSharedBlockModel(JsonObject model) {
        return model != null && model.has("parent")
                && NATIVE_BLOCK_PRESENTATION.equals(model.get("parent").getAsString())
                && model.has("textures") && model.has("elements")
                && model.getAsJsonArray("elements").size() > 0
                && !hasPresentationOverrides(model);
    }

    private static boolean hasPresentationOverrides(JsonObject model) {
        return model.has("display") || model.has("gui_light")
                || model.has("transform") || model.has("loader");
    }

    /** Item wrappers keep the exact idle model; the shared block root supplies native transforms. */
    private static void blockItemPresentation() throws IOException {
        Path modelRoot = ASSETS.resolve("models").normalize();
        ModelLookup lookup = modelId -> {
            Path file = modelRoot.resolve(modelId + ".json").normalize();
            return file.startsWith(modelRoot) ? read(file) : null;
        };
        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            // pylonItem()/assembled() are intentionally outside the shared presentation repair.
            if (block.geometry() == BlockCatalogue.Geometry.PYLON) {
                continue;
            }
            String modelId = "item/" + block.name();
            JsonObject item = lookup.load(modelId);
            yes(block.name() + " item only inherits its existing idle block model",
                    item != null && item.size() == 1 && item.has("parent")
                            && ("grindless:block/" + block.modelName("idle"))
                            .equals(item.get("parent").getAsString()));
            yes(block.name() + " item reaches native block presentation through its model hierarchy",
                    inheritsNativeBlockPresentation(modelId, item, lookup, new HashSet<>()));
        }
    }

    private static boolean inheritsNativeBlockPresentation(String modelId, JsonObject model,
                                                           ModelLookup lookup, Set<String> visiting)
            throws IOException {
        if (model == null || !visiting.add(modelId)) {
            return false;
        }
        try {
            if (!model.has("parent") || hasPresentationOverrides(model)) {
                return false;
            }
            String parent = model.get("parent").getAsString();
            if (NATIVE_BLOCK_PRESENTATION.equals(parent)) {
                return true;
            }
            if (!parent.startsWith("grindless:")) {
                return false;
            }
            String parentId = parent.substring("grindless:".length());
            return inheritsNativeBlockPresentation(parentId, lookup.load(parentId), lookup, visiting);
        } finally {
            visiting.remove(modelId);
        }
    }

    private static JsonObject presentationModel(String parent) {
        JsonObject model = new JsonObject();
        if (parent != null) {
            model.addProperty("parent", parent);
        }
        return model;
    }

    private static void presentationRegressionCases() throws IOException {
        JsonObject root = presentationModel(NATIVE_BLOCK_PRESENTATION);
        root.add("textures", JsonParser.parseString("{\"front\":\"grindless:block/casing_side\"}"));
        root.add("elements", JsonParser.parseString("[{\"from\":[2,1,3],\"to\":[14,9,12],"
                + "\"faces\":{\"north\":{\"texture\":\"#front\",\"uv\":[1,2,15,14]}}}]"));
        JsonObject before = root.deepCopy();
        JsonObject item = presentationModel("grindless:block/middle");
        Map<String, JsonObject> hierarchy = Map.of(
                "block/root", root,
                "block/middle", presentationModel("grindless:block/root"),
                "item/part", item);
        yes("an item can inherit native display metadata through intermediate block models",
                inheritsNativeBlockPresentation("item/part", item, hierarchy::get, new HashSet<>()));
        yes("native presentation keeps local elements, UVs and textures unchanged",
                isSharedBlockModel(root) && root.equals(before));
        Map<String, String> bindings = effectiveTextureBindings("block/root", root, hierarchy::get,
                new HashSet<>(), new ArrayList<>());
        yes("a native metadata parent leaves local face texture bindings authoritative",
                resolvesTextureKey("front", bindings, new HashSet<>()));
        yes("a legacy parentless model does not acquire block display transforms automatically",
                !inheritsNativeBlockPresentation("block/legacy", presentationModel(null),
                        hierarchy::get, new HashSet<>()));
        yes("a generated sprite is not mistaken for native block presentation",
                !inheritsNativeBlockPresentation("item/sprite", presentationModel("minecraft:item/generated"),
                        hierarchy::get, new HashSet<>()));
        yes("a missing local presentation parent fails closed",
                !inheritsNativeBlockPresentation("item/missing", presentationModel("grindless:block/missing"),
                        hierarchy::get, new HashSet<>()));
        Map<String, JsonObject> cycle = Map.of(
                "block/first", presentationModel("grindless:block/second"),
                "block/second", presentationModel("grindless:block/first"));
        yes("cyclic presentation inheritance is rejected",
                !inheritsNativeBlockPresentation("block/first", cycle.get("block/first"),
                        cycle::get, new HashSet<>()));
        for (String override : List.of("display", "gui_light", "transform", "loader")) {
            JsonObject overridden = root.deepCopy();
            if (override.equals("gui_light") || override.equals("loader")) {
                overridden.addProperty(override, override.equals("gui_light") ? "front" : "forge:obj");
            } else {
                overridden.add(override, new JsonObject());
            }
            yes("local " + override + " cannot silently replace shared native presentation",
                    !isSharedBlockModel(overridden)
                            && !inheritsNativeBlockPresentation("block/overridden", overridden,
                            hierarchy::get, new HashSet<>()));
        }
    }

    /** Every event is a checked mono import, an explicit native alias or intentional silence. */
    private static void sounds() throws IOException {
        List<String> inCode = registered("ModSounds.java", "sound");
        JsonObject events = read(ASSETS.resolve("sounds.json"));
        yes("ModSounds registers something", !inCode.isEmpty());
        yes("sounds.json defines exactly the events ModSounds registers " + inCode,
                events.size() == inCode.size() && inCode.stream().allMatch(events::has));
        JsonObject lang = read(ASSETS.resolve("lang/en_us.json"));
        Map<String, String> nativeEvents = Map.of(
                "pylon_link", "minecraft:block.tripwire.attach",
                "relay_click", "minecraft:block.lever.click",
                "brownout_alarm", "minecraft:block.iron_trapdoor.close",
                "t0_fire_draft", "minecraft:block.furnace.fire_crackle");
        for (String name : inCode) {
            Path file = ASSETS.resolve("sounds/" + name + ".ogg");
            yes(name + " has a subtitle", lang.has("subtitles.grindless." + name));
            JsonArray sounds = events.getAsJsonObject(name).getAsJsonArray("sounds");
            if (Set.of("machine_hum", "machine_hum_heavy").contains(name)) {
                yes(name + " retains its identity but is intentionally silent",
                        sounds.isEmpty() && !exists(file));
                continue;
            }
            JsonElement sound = sounds.size() == 1 ? sounds.get(0) : null;
            if (nativeEvents.containsKey(name)) {
                yes(name + " references its exact installed native event without a copied recording",
                        sound != null && sound.isJsonObject()
                                && sound.getAsJsonObject().keySet().equals(Set.of("name", "type"))
                                && sound.getAsJsonObject().get("type").getAsString().equals("event")
                                && sound.getAsJsonObject().get("name").getAsString().equals(nativeEvents.get(name))
                                && !exists(file));
                continue;
            }
            yes(name + " has an ogg file", exists(file));
            yes(name + " is mono, or Minecraft will not place it in 3D", exists(file) && vorbisChannels(file) == 1);
            yes(name + " binds exactly its checked Ogg resource",
                    sound != null && ("grindless:" + name).equals(sound.isJsonObject()
                            ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString()));
        }
        importedQuernAudio(events);
        importedMixerAudio(events);
        physicalAudio(events);
        retiredSyntheticAudio();
    }

    private static void importedQuernAudio(JsonObject events) throws IOException {
        String expectedHash = "811b29471e3fa6a51ce76db2241259d02adfc64e244eadc096ef376d1d1970d5";
        Path ogg = ASSETS.resolve("sounds/t0_grinding.ogg");
        yes("Quern retains exactly the approved candidate A bytes",
                exists(ogg) && Files.size(ogg) == 34364 && expectedHash.equals(sha256(Files.readAllBytes(ogg))));
        JsonObject sound = events.getAsJsonObject("t0_grinding")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        yes("Quern uses one non-streamed native buffer and the checked attenuation radius",
                sound.has("stream") && !sound.get("stream").getAsBoolean()
                        && sound.get("attenuation_distance").getAsInt() == 16
                        && !sound.has("volume") && !sound.has("pitch"));
        Path notice = ASSETS.resolve("AUDIO-NOTICE.txt");
        String noticeText = exists(notice) ? Files.readString(notice) : "";
        yes("the public audio notice ships beside the resource with its exact provenance",
                noticeText.contains(expectedHash) && noticeText.contains("eleven_text_to_sound_v2")
                        && noticeText.contains("https://elevenlabs.io/creative-terms")
                        && noticeText.contains("not a standalone sample or sound library"));
        try (Stream<Path> files = Files.walk(ASSETS.resolve("sounds"))) {
            yes("runtime sounds contain no raw MP3/WAV or preview bundle",
                    files.filter(Files::isRegularFile).allMatch(file -> file.toString().endsWith(".ogg")));
        }
        String generator = Files.readString(Path.of(
                "tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/GenerateAssets.java"));
        Set<String> legacy = Set.of("machine_hum", "machine_hum_heavy", "pylon_link",
                "relay_click", "brownout_alarm");
        Set<String> generated = new HashSet<>();
        Matcher writes = Pattern.compile("SoundSynth\\.write\\(new File\\(sounds, \"([a-z0-9_]+)\\.wav\"\\)")
                .matcher(generator);
        while (writes.find()) {
            generated.add(writes.group(1));
        }
        yes("the generator preserves only the five historical development WAV writers", generated.equals(legacy));
        yes("regeneration cannot reset the imported event, file or public provenance",
                generator.contains("Generation does not write imported Ogg files, `sounds.json`, `AUDIO-NOTICE.txt`")
                        && !generator.contains("deleteTree(sounds)")
                        && !generator.contains("resourceDir(root, \"sounds\")"));
        yes("generated audio prose distinguishes all imports from historical development synthesis",
                generator.contains("`sounds/t0_grinding.ogg` and")
                        && generator.contains("`sounds/t0_wet_work.ogg`")
                        && generator.contains("Quern A import remains byte-identical")
                        && generator.contains("separately recorded technical normalization")
                        && generator.contains("`sounds/t0_water_flow.ogg`")
                        && generator.contains("`sounds/t0_mineral_extraction.ogg`")
                        && generator.contains("converters reject runtime synthesis"));
        String shell = Files.readString(Path.of("tools/convert-audio.sh"));
        yes("the retired shell converter cannot restore synthetic runtime audio",
                shell.contains("Synthetic runtime conversion is retired.") && shell.contains("exit 1")
                        && !shell.contains("ffmpeg -") && !shell.contains("mkdir"));
        String powershell = Files.readString(Path.of("tools/convert-audio.ps1"));
        yes("the retired Windows converter has the same fail-closed boundary",
                powershell.contains("throw 'Synthetic runtime conversion is retired.")
                        && !powershell.contains("& ffmpeg") && !powershell.contains("New-Item"));
    }

    private static void importedMixerAudio(JsonObject events) throws IOException {
        String expectedHash = "2c052cd678775f237fbf1a939037006ed26d05588e99a4ff29bbffcd66599f45";
        Path ogg = ASSETS.resolve("sounds/t0_wet_work.ogg");
        yes("Mixer ships only the technically selected, normalized A Ogg with its exact identity",
                exists(ogg) && Files.size(ogg) == 33374 && expectedHash.equals(sha256(Files.readAllBytes(ogg))));
        JsonObject event = events.getAsJsonObject("t0_wet_work");
        JsonObject sound = event.getAsJsonArray("sounds").get(0).getAsJsonObject();
        yes("Mixer uses exactly one non-streamed positional buffer with range sixteen and no mix overrides",
                event.keySet().equals(Set.of("subtitle", "sounds"))
                        && event.get("subtitle").getAsString().equals("subtitles.grindless.t0_wet_work")
                        && event.getAsJsonArray("sounds").size() == 1
                        && sound.keySet().equals(Set.of("name", "stream", "attenuation_distance"))
                        && sound.get("name").getAsString().equals("grindless:t0_wet_work")
                        && !sound.get("stream").getAsBoolean()
                        && sound.get("attenuation_distance").getAsInt() == 16);
        yes("Mixer subtitle describes the working loop, not a new start/stop/error palette",
                read(ASSETS.resolve("lang/en_us.json")).get("subtitles.grindless.t0_wet_work")
                        .getAsString().equals("Clay mixer stirs"));
        String notice = Files.readString(ASSETS.resolve("AUDIO-NOTICE.txt"));
        String provenance = Files.readString(Path.of("THIRD_PARTY_NOTICES.md"));
        int mixerNoticeStart = notice.indexOf("Grindless Clay Mixer wet-work audio");
        String mixerNotice = mixerNoticeStart < 0 ? "" : notice.substring(mixerNoticeStart).replaceAll("\\s+", " ");
        yes("Mixer's shipped notice and public provenance bind the same final game-audio import",
                Stream.of(mixerNotice, provenance).allMatch(text -> text.contains("t0_wet_work.ogg")
                        && text.contains(expectedHash) && text.contains("eleven_text_to_sound_v2")
                        && text.contains("https://elevenlabs.io/creative-terms"))
                        && mixerNotice.contains("not a standalone sample or sound library"));
        JsonObject record = read(Path.of("docs/assets/mixer-runtime-import.json"));
        JsonObject resource = record.getAsJsonObject("resource");
        JsonObject runtime = record.getAsJsonObject("runtime_contract");
        yes("Mixer's source-bound provenance declares the actual normalized import and runtime mix",
                resource.get("sha256").getAsString().equals(expectedHash)
                        && resource.get("bytes").getAsInt() == 33374
                        && resource.get("event").getAsString().equals("grindless:t0_wet_work")
                        && resource.get("path").getAsString().equals(ogg.toString().replace('\\', '/'))
                        && resource.get("imported").getAsBoolean() && resource.get("import_authorized").getAsBoolean()
                        && resource.get("notice_recorded_before_import").getAsBoolean()
                        && !resource.get("cc0_or_mit_relicensing").getAsBoolean()
                        && resource.get("standalone_audio_distribution").getAsString().equals("not_permitted")
                        && record.getAsJsonObject("selection").get("selected_candidate").getAsString().equals("wet-work-a")
                        && record.getAsJsonObject("normalization").get("source_preview").getAsString().equals("wet-work-a.preview.wav")
                        && runtime.get("volume").getAsDouble() == 0.35 && runtime.get("pitch").getAsDouble() == 1.0
                        && runtime.get("radius_blocks").getAsInt() == 16
                        && runtime.get("category").getAsString().equals("BLOCKS")
                        && runtime.get("attenuation").getAsString().equals("LINEAR")
                        && !runtime.get("ordinary_error_sounds").getAsBoolean());
    }

    private static void physicalAudio(JsonObject events) throws IOException {
        Map<String, String> hashes = Map.of(
                "t0_water_flow", "5fbfa5c1906fcc98ac310f3feaa9959fd7838ef5f4f6143dc293d37eadc6aeb1",
                "t0_mineral_extraction", "a634f9a17c5f5c71e7fd20da8f79ec5453715d125b1084c557de54bb380ba9fb");
        JsonObject record = read(Path.of("docs/assets/t0-experience-audio.json"));
        yes("the physical wave records six real requests without claiming human approval",
                record.getAsJsonObject("generation").get("performed_posts").getAsInt() == 6
                        && record.getAsJsonObject("generation").get("successful_http_200").getAsInt() == 6
                        && !record.get("human_listening_approved").getAsBoolean()
                        && !record.get("release_ready").getAsBoolean());
        String notice = Files.readString(ASSETS.resolve("AUDIO-NOTICE.txt"));
        JsonArray selected = record.getAsJsonArray("selected_resources");
        yes("only two new technically selected files are imported", selected.size() == 2);
        for (var entry : hashes.entrySet()) {
            String name = entry.getKey();
            Path ogg = ASSETS.resolve("sounds/" + name + ".ogg");
            JsonObject event = events.getAsJsonObject(name);
            JsonArray sounds = event.getAsJsonArray("sounds");
            int radius = name.equals("t0_water_flow") ? 10 : 12;
            long size = name.equals("t0_water_flow") ? 33490 : 29241;
            yes(name + " has the exact technically selected normalized payload",
                    exists(ogg) && entry.getValue().equals(sha256(Files.readAllBytes(ogg)))
                            && Files.size(ogg) == size && notice.contains(entry.getValue()));
            yes(name + " has a non-streamed mono positional binding with its physical radius",
                    sounds.size() == 1 && sounds.get(0).isJsonObject()
                            && sounds.get(0).getAsJsonObject().get("name").getAsString().equals("grindless:" + name)
                            && !sounds.get(0).getAsJsonObject().get("stream").getAsBoolean()
                            && sounds.get(0).getAsJsonObject().get("attenuation_distance").getAsInt() == radius);
            JsonObject imported = StreamSupport.stream(selected.spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .filter(resource -> resource.get("event").getAsString().equals("grindless:" + name))
                    .findFirst().orElseThrow();
            yes(name + " binds actual imported/QC metadata without inventing listening",
                    imported.get("sha256").getAsString().equals(entry.getValue())
                            && imported.get("bytes").getAsLong() == size
                            && imported.get("import_authorized").getAsBoolean()
                            && imported.get("imported").getAsBoolean()
                            && imported.get("human_approval").getAsString().equals("pending")
                            && imported.getAsJsonObject("decoded_qc").getAsJsonArray("flags").isEmpty()
                            && imported.getAsJsonObject("decoded_qc").get("clipped_samples").getAsLong() == 0);
        }
        try (Stream<Path> files = Files.list(ASSETS.resolve("sounds"))) {
            yes("runtime ships exactly the two preserved and two new selected Oggs, no candidates",
                    files.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet())
                            .equals(Set.of("t0_grinding.ogg", "t0_wet_work.ogg",
                                    "t0_water_flow.ogg", "t0_mineral_extraction.ogg")));
        }
    }

    private static void retiredSyntheticAudio() throws IOException {
        for (String name : List.of("machine_hum", "machine_hum_heavy", "pylon_link", "relay_click", "brownout_alarm")) {
            yes(name + " synthetic Ogg is absent from runtime distribution",
                    Files.notExists(ASSETS.resolve("sounds/" + name + ".ogg")));
        }
        String effects = Files.readString(SOURCES.resolve("machine/MachineEffects.java"));
        yes("MachineEffects cannot duplicate the managed physical beds with generic hum bursts",
                !effects.contains("ModSounds.MACHINE_HUM") && !effects.contains("ModSounds.MACHINE_HUM_HEAVY"));
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
        List<String> invalidFaceReferences = new ArrayList<>();
        int models = 0;
        Path blockModels = ASSETS.resolve("models/block");
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
                if (blockModels.equals(file.getParent())) {
                    checkLocalBlockModelFaceReferences(file, json, invalidFaceReferences);
                }
            }
        }
        yes("scanned " + models + " models", models > 100);
        int states = totalStates();
        yes("the server-side state scenario covers every state",
                Files.readAllLines(Path.of("tools/smoke/states.expect")).size() == states);
        long functionSays = Files.readAllLines(
                        Path.of("tools/smoke/states-pack/data/grindless/functions/states.mcfunction"))
                .stream().filter(line -> line.contains("say STATE-OK")).count();
        yes("the state scenario runs as one function so a ticker cannot rewrite status first",
                functionSays == states
                        && Files.readString(Path.of("tools/smoke/states.commands"))
                        .contains("function grindless:states"));
        yes("every texture and parent a model names exists" + (missing.isEmpty() ? "" : " " + missing),
                missing.isEmpty());
        String faceExamples = invalidFaceReferences.isEmpty() ? ""
                : " (" + invalidFaceReferences.size() + "): "
                + invalidFaceReferences.stream().limit(8).toList()
                + (invalidFaceReferences.size() > 8 ? " ..." : "");
        yes("generated block model local faces use one # and a declared or inherited texture key" + faceExamples,
                invalidFaceReferences.isEmpty());
    }

    private static void checkLocalBlockModelFaceReferences(Path file, JsonObject model, List<String> invalid)
            throws IOException {
        // Minecraft resolves inherited elements; this audit covers only faces declared here.
        if (!model.has("elements")) {
            return;
        }
        JsonArray elements = model.getAsJsonArray("elements");
        if (elements.size() == 0) {
            return;
        }
        Path modelRoot = ASSETS.resolve("models").normalize();
        String modelId = modelRoot.relativize(file.normalize()).toString().replace('\\', '/');
        modelId = modelId.substring(0, modelId.length() - ".json".length());
        Map<String, String> textures = effectiveTextureBindings(modelId, model, parentId -> {
            Path parentFile = modelRoot.resolve(parentId + ".json").normalize();
            return parentFile.startsWith(modelRoot) ? read(parentFile) : null;
        }, new HashSet<>(), invalid);
        for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
            JsonObject element = elements.get(elementIndex).getAsJsonObject();
            if (!element.has("faces")) {
                invalid.add(file.getFileName() + " element " + elementIndex + " has no faces");
                continue;
            }
            for (Map.Entry<String, JsonElement> faceEntry
                    : element.getAsJsonObject("faces").entrySet()) {
                JsonObject face = faceEntry.getValue().getAsJsonObject();
                if (!face.has("texture")) {
                    invalid.add(file.getFileName() + " element " + elementIndex + " "
                            + faceEntry.getKey() + " face has no texture");
                    continue;
                }
                String reference = face.get("texture").getAsString();
                if (!isSingleTextureKey(reference)) {
                    invalid.add(file.getFileName() + " element " + elementIndex + " "
                            + faceEntry.getKey() + " face uses " + reference);
                } else if (!resolvesTextureKey(reference.substring(1), textures, new HashSet<>())) {
                    invalid.add(file.getFileName() + " element " + elementIndex + " "
                            + faceEntry.getKey() + " face has no binding for " + reference);
                }
            }
        }
    }

    @FunctionalInterface
    private interface ModelLookup {
        JsonObject load(String modelId) throws IOException;
    }

    private static Map<String, String> effectiveTextureBindings(String modelId, JsonObject model,
                                                                 ModelLookup lookup, Set<String> visiting,
                                                                 List<String> invalid) throws IOException {
        if (!visiting.add(modelId)) {
            invalid.add(modelId + " has a cyclic model parent chain");
            return Map.of();
        }
        Map<String, String> textures = new HashMap<>();
        try {
            if (model.has("parent")) {
                String parent = model.get("parent").getAsString();
                if (parent.startsWith("grindless:")) {
                    String parentId = parent.substring("grindless:".length());
                    JsonObject parentModel = lookup.load(parentId);
                    if (parentModel != null) {
                        textures.putAll(effectiveTextureBindings(parentId, parentModel, lookup, visiting, invalid));
                    }
                }
            }
            if (model.has("textures")) {
                for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                    textures.put(texture.getKey(), texture.getValue().getAsString());
                }
            }
            return textures;
        } finally {
            visiting.remove(modelId);
        }
    }

    private static JsonObject textureModel(String parentId, Map<String, String> bindings) {
        JsonObject model = new JsonObject();
        if (parentId != null) {
            model.addProperty("parent", "grindless:" + parentId);
        }
        JsonObject textures = new JsonObject();
        for (Map.Entry<String, String> binding : bindings.entrySet()) {
            textures.addProperty(binding.getKey(), binding.getValue());
        }
        model.add("textures", textures);
        return model;
    }

    private static void textureReferenceRegressionCases() throws IOException {
        List<String> inheritedElementsErrors = new ArrayList<>();
        checkLocalBlockModelFaceReferences(ASSETS.resolve("models/block/child.json"),
                textureModel("block/parent", Map.of()), inheritedElementsErrors);
        yes("a model without local elements is left to inherit its parent's faces",
                inheritedElementsErrors.isEmpty());
        yes("double-hash face references are rejected", !isSingleTextureKey("##cap"));
        yes("unbound face texture keys are rejected",
                !resolvesTextureKey("missing", Map.of(), new HashSet<>()));
        yes("texture alias cycles are rejected",
                !resolvesTextureKey("first", Map.of("first", "#second", "second", "#first"), new HashSet<>()));

        Map<String, JsonObject> inheritedModels = Map.of(
                "block/parent", textureModel(null, Map.of("base", "grindless:block/casing_side")),
                "block/child", textureModel("block/parent", Map.of("front", "#base")));
        List<String> inheritedErrors = new ArrayList<>();
        Map<String, String> inheritedBindings = effectiveTextureBindings("block/child",
                inheritedModels.get("block/child"), inheritedModels::get, new HashSet<>(), inheritedErrors);
        yes("a face texture alias resolves through an inherited parent binding",
                inheritedErrors.isEmpty()
                        && resolvesTextureKey("front", inheritedBindings, new HashSet<>()));

        Map<String, JsonObject> cyclicModels = Map.of(
                "block/first", textureModel("block/second",
                        Map.of("cap", "grindless:block/casing_side")),
                "block/second", textureModel("block/first", Map.of()));
        List<String> cycleErrors = new ArrayList<>();
        Map<String, String> cyclicBindings = effectiveTextureBindings("block/first",
                cyclicModels.get("block/first"), cyclicModels::get, new HashSet<>(), cycleErrors);
        yes("a parent cycle is reported even when a local face key resolves",
                cycleErrors.stream().anyMatch(error -> error.contains("cyclic model parent chain"))
                        && isSingleTextureKey("#cap")
                        && resolvesTextureKey("cap", cyclicBindings, new HashSet<>()));
    }

    private static boolean isSingleTextureKey(String reference) {
        return reference != null && reference.length() > 1 && reference.charAt(0) == '#'
                && reference.indexOf('#', 1) < 0;
    }

    private static boolean resolvesTextureKey(String key, Map<String, String> textures, Set<String> visited) {
        if (!visited.add(key)) {
            return false;
        }
        String reference = textures.get(key);
        if (reference == null || reference.isBlank()) {
            return false;
        }
        if (reference.startsWith("#")) {
            return isSingleTextureKey(reference)
                    && resolvesTextureKey(reference.substring(1), textures, visited);
        }
        return reference.indexOf('#') < 0;
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
        for (io.github.ezequiel24123z.grindless.source.SourceSite.Fault fault
                : io.github.ezequiel24123z.grindless.source.SourceSite.Fault.values()) {
            yes(fault.name() + " source fault has a display message", lang.has(fault.translationKey()));
        }
        yes("the Crank Handle has an instruction tooltip", lang.has("tooltip.grindless.crank_handle"));
        yes("JEI process titles inherit the localized catalyst name", lang.has("jei.grindless.process"));
        yes("every hand-registered item and block has a display name" + (unnamed.isEmpty() ? "" : " " + unnamed),
                unnamed.isEmpty());
        yes("and every hand-registered item has a model" + (unmodelled.isEmpty() ? "" : " " + unmodelled),
                unmodelled.isEmpty());
        for (String sprite : BlockCatalogue.placeholderSprites()) {
            yes(sprite + " has a sprite", exists(ASSETS.resolve("textures/item/" + sprite + ".png")));
        }
    }

    private static void thermalGeneratorCore() throws IOException {
        String name = BlockCatalogue.THERMAL_GENERATOR_CORE_ITEM;
        yes("the Thermal core has exactly one ordinary item registration",
                registered("ModItems.java").stream().filter(name::equals).count() == 1
                        && Pattern.compile("THERMAL_GENERATOR_CORE = register\\(\"thermal_generator_core\",\\s*"
                                + "\\(\\) -> new Item\\(new Item.Properties\\(\\)\\)\\);")
                                .matcher(Files.readString(SOURCES.resolve("registry/ModItems.java"))).find());
        yes("the Thermal core is discoverable in the existing creative tab",
                Files.readString(SOURCES.resolve("registry/ModCreativeTabs.java"))
                        .contains("lazy(ModItems.THERMAL_GENERATOR_CORE)"));
        yes("the Thermal core is not a new block, stateful item or missing standalone sprite",
                !registered("ModBlocks.java").contains(name) && !BlockCatalogue.placeholderSprites().contains(name));
        JsonObject model = read(ASSETS.resolve("models/item/" + name + ".json"));
        yes("the Thermal core uses the existing native item-model mechanism without custom loaders",
                model != null && model.keySet().equals(Set.of("parent", "textures"))
                        && "minecraft:block/cube_bottom_top".equals(model.get("parent").getAsString()));
        JsonObject expectedTextures = new JsonObject();
        expectedTextures.addProperty("top", "grindless:block/generator_top");
        expectedTextures.addProperty("bottom", "grindless:block/casing_top");
        expectedTextures.addProperty("side", "grindless:block/generator_front_idle");
        yes("the core composes only the existing owned generator and casing textures",
                model != null && expectedTextures.equals(model.getAsJsonObject("textures")));
        yes("normal asset generation retains the core model rather than needing a bespoke asset pipeline",
                Files.readString(Path.of("tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/GenerateAssets.java"))
                        .contains("BlockCatalogue.THERMAL_GENERATOR_CORE_ITEM + \".json\""));
    }

    /** Structural sprite guards, not an artistic or in-game rendering acceptance claim. */
    private static void physicalItemSprites() throws IOException {
        Map<String, BufferedImage> sprites = new HashMap<>();
        for (String name : List.of("data_core", "calibrated_data_core", "relay_matrix",
                "mineral_aggregate", "biomass", "slag",
                "ceramic_grog", "unfired_ceramic", "ceramic_insulator")) {
            Path file = ASSETS.resolve("textures/item/" + name + ".png");
            BufferedImage image = exists(file) ? ImageIO.read(file.toFile()) : null;
            yes(name + " remains a native 16x16 sprite",
                    image != null && image.getWidth() == 16 && image.getHeight() == 16);
            sprites.put(name, image);
        }
        yes("raw and calibrated cores have different physical silhouettes, not only different hues",
                differentSilhouettes(sprites.get("data_core"), sprites.get("calibrated_data_core")));
        yes("the relay matrix has a different physical silhouette from the raw core",
                differentSilhouettes(sprites.get("relay_matrix"), sprites.get("data_core")));
        yes("the relay matrix has a different physical silhouette from the calibrated core",
                differentSilhouettes(sprites.get("relay_matrix"), sprites.get("calibrated_data_core")));

        // Alpha-only baselines from 102d68f: surfaces may change, these approved shapes may not.
        Map<String, String> preservedSilhouettes = Map.of(
                "ceramic_grog", "7df62b2e4d70a470a70b0df6d7489420afed6bb258c049dec76636294512af8f",
                "unfired_ceramic", "4e9016f43140bfad11af57a941b7a7d199e82c909ed1580470ee10f6eadb1e0c",
                "ceramic_insulator", "182f2c8a9cdedb56cde3f71f8a3f6ed871b7639603533848e4e580dbf61d6a86",
                "biomass", "c32cd59207c6e970dfeec105890428e171eab15d551a888463f8af2c1261bdb1");
        for (Map.Entry<String, String> entry : preservedSilhouettes.entrySet()) {
            yes(entry.getKey() + " retains its pre-refinement silhouette",
                    entry.getValue().equals(alphaFingerprint(sprites.get(entry.getKey()))));
        }
        Path advanced = ASSETS.resolve("textures/item/advanced_data_core.png");
        yes("advanced_data_core retains its exact 102d68f PNG bytes outside the T0 batch",
                exists(advanced)
                        && "95d3c92c3f348b473bf062be37ebcb08d8a1d18901a6efdec3c85189892bb1be"
                        .equals(sha256(Files.readAllBytes(advanced))));
    }

    private static boolean differentSilhouettes(BufferedImage first, BufferedImage second) {
        return first != null && second != null
                && first.getWidth() == second.getWidth() && first.getHeight() == second.getHeight()
                && !alphaFingerprint(first).equals(alphaFingerprint(second));
    }

    private static String alphaFingerprint(BufferedImage image) {
        if (image == null) {
            return "";
        }
        byte[] alpha = new byte[image.getWidth() * image.getHeight()];
        int index = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                alpha[index++] = (byte) (image.getRGB(x, y) >>> 24);
            }
        }
        return sha256(alpha);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("A Java 17 runtime must provide SHA-256", impossible);
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
