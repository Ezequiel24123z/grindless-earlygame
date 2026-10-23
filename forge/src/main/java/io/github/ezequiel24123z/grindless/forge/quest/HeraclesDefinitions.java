package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.api.quests.QuestDisplayStatus;
import earth.terrarium.heracles.api.quests.QuestSettings;
import earth.terrarium.heracles.api.quests.defaults.ItemQuestIcon;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProjection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.joml.Vector2i;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Catalogue-generated native definitions; a hash manifest protects existing user configuration. */
public final class HeraclesDefinitions {

    public static final String GROUP = "Grindless T0";
    public static final String T1_GROUP = "Grindless T1";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String MANIFEST = "grindless-manifest";
    private static final int MAX_MEMBER_BYTES = 1024 * 1024;

    private HeraclesDefinitions() {
    }

    public static Map<String, Quest> create() {
        return create(HeraclesDefinitions::icon);
    }

    static Item icon(QuestCatalogue.Task task) {
        if (task.evidence() == QuestCatalogue.Evidence.FREE
                || task.evidence() == QuestCatalogue.Evidence.DIMENSION) {
            return Items.BOOK;
        }
        ResourceLocation id = ResourceLocation.tryParse(task.subject());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.get(id) == Items.AIR) {
            throw new IllegalStateException("Quest subject is not registered: " + task.subject());
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    static Map<String, Quest> create(Function<QuestCatalogue.Task, Item> icons) {
        Map<String, Quest> result = new LinkedHashMap<>();
        Map<String, Integer> depths = new LinkedHashMap<>();
        List<QuestProjection.Entry> entries = QuestProjection.entries();
        for (QuestProjection.Entry entry : entries) {
            depths.put(entry.task().id(), entry.task().requires().stream()
                    .filter(id -> QuestCatalogue.chapter(QuestCatalogue.byId(id)) == QuestCatalogue.chapter(entry.task()))
                    .mapToInt(id -> depths.get(id) + 1).max().orElse(0));
        }
        for (QuestCatalogue.Chapter chapter : QuestCatalogue.Chapter.values()) {
            int lineTop = 0;
            for (String line : QuestCatalogue.lines(chapter)) {
                Map<Integer, Integer> rows = new LinkedHashMap<>();
                for (QuestProjection.Entry entry : entries) {
                    QuestCatalogue.Task task = entry.task();
                    if (!line.equals(task.line()) || QuestCatalogue.chapter(task) != chapter) {
                        continue;
                    }
                    int depth = depths.get(task.id());
                    int row = rows.getOrDefault(depth, 0);
                    rows.put(depth, row + 1);
                    QuestDisplay display = QuestDisplay.createDefault(
                            new GroupDisplay(chapter == QuestCatalogue.Chapter.T0 ? GROUP : T1_GROUP,
                                    new Vector2i(depth * 96, lineTop + row * 56)));
                    display.setIcon(new ItemQuestIcon(icons.apply(task)));
                    display.setTitle(Component.translatable("gui.grindless.quest.task." + task.id()));
                    Component lineTitle = Component.translatable("gui.grindless.quest.line." + task.line());
                    display.setSubtitle(chapter == QuestCatalogue.Chapter.T0 ? lineTitle
                            : Component.translatable("gui.grindless.quest.t1_line", lineTitle));
                    display.setDescription(List.of());
                    if (task.id().equals(QuestCatalogue.T0_COMPLETION)) {
                        // Heracles visibility assumes absent cross-group dependencies are complete.
                        display.groups().put(T1_GROUP, new GroupDisplay(T1_GROUP, new Vector2i(-96, 0)));
                    }
                    QuestSettings settings = QuestSettings.createDefault();
                    settings.setIndividualProgress(true);
                    settings.setRepeatable(false);
                    settings.setAutoClaimRewards(false);
                    settings.setUnlockNotification(false);
                    settings.setShowDependencyArrow(true);
                    settings.setHiddenUntil(chapter == QuestCatalogue.Chapter.T0
                            ? QuestDisplayStatus.LOCKED : QuestDisplayStatus.IN_PROGRESS);
                    result.put(entry.id(), new Quest(display, settings, new LinkedHashSet<>(entry.dependencies()),
                            new LinkedHashMap<>(Map.of(QuestProjection.CLAIM_TASK,
                                    new GrindlessClaimTask(QuestProjection.CLAIM_TASK, task.id()))),
                            new LinkedHashMap<>()));
                }
                lineTop += rows.values().stream().mapToInt(Integer::intValue).max().orElse(1) * 56 + 40;
            }
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public static Map<String, String> encode(Map<String, Quest> quests) {
        Map<String, String> result = new LinkedHashMap<>();
        quests.forEach((id, quest) -> result.put(id, JSON.toJson(Quest.CODEC.encodeStart(JsonOps.INSTANCE, quest)
                .getOrThrow(false, message -> {
                    throw new IllegalStateException("Cannot encode " + id + ": " + message);
                })) + "\n"));
        return result;
    }

    public static boolean matches(Quest expected, Quest actual) {
        return actual != null && expected.tasks().equals(actual.tasks())
                && expected.dependencies().equals(actual.dependencies())
                && expected.display().groups().equals(actual.display().groups())
                && expected.settings().hiddenUntil() == actual.settings().hiddenUntil()
                && actual.rewards().isEmpty() && actual.settings().individualProgress()
                && !actual.settings().autoClaimRewards() && !actual.settings().repeatable();
    }

    static Path bundle(Path config) {
        return config.resolve("heracles").resolve("quests").resolve("grindless.zip");
    }

    @FunctionalInterface
    interface StagingOutput {
        OutputStream open(Path path) throws IOException;
    }

    public static void synchronize(Path config, Map<String, String> definitions) throws IOException {
        synchronize(config, definitions, Files::newOutputStream);
    }

    static void synchronize(Path config, Map<String, String> definitions, StagingOutput output) throws IOException {
        checkDirectories(config);
        Path destination = bundle(config);
        Map<String, String> previous = readBundle(destination);
        if (!definitions.keySet().containsAll(previous.keySet())) {
            throw new IOException("Removed owned quests require an explicit migration; no files were deleted");
        }
        checkCollisions(destination.getParent(), destination, definitions.keySet());
        if (hashes(definitions).equals(previous)) {
            return;
        }
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".grindless-", ".tmp");
        try {
            writeBundle(output.open(temporary), definitions);
            try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                file.force(true);
            }
            // The archive and its ownership proof become visible in the same atomic replacement.
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void verifyFiles(Path config, Map<String, String> definitions) throws IOException {
        checkDirectories(config);
        Path archive = bundle(config);
        if (!hashes(definitions).equals(readBundle(archive))) {
            throw new IOException("Grindless quest bundle does not match the current catalogue");
        }
        checkCollisions(archive.getParent(), archive, definitions.keySet());
    }

    private static Map<String, String> readBundle(Path archive) throws IOException {
        if (!Files.exists(archive, LinkOption.NOFOLLOW_LINKS)) {
            return Map.of();
        }
        if (!Files.isRegularFile(archive) || Files.isSymbolicLink(archive)) {
            throw new IOException("Grindless quest bundle must be a regular owned file: " + archive);
        }
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry manifest = zip.getEntry(MANIFEST);
            if (manifest == null) {
                throw new IOException("Unowned quest archive was preserved: " + archive);
            }
            Map<String, String> hashes = readManifest(new String(read(zip, manifest), StandardCharsets.UTF_8));
            Set<String> expected = new LinkedHashSet<>();
            expected.add(MANIFEST);
            hashes.keySet().forEach(id -> expected.add(id + ".json"));
            Set<String> seen = new LinkedHashSet<>();
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!expected.contains(name) || !seen.add(name) || entry.isDirectory()) {
                    throw new IOException("Unexpected or duplicated owned archive member was preserved: " + name);
                }
                if (!name.equals(MANIFEST) && !hash(read(zip, entry)).equals(hashes.get(nativeId(name)))) {
                    throw new IOException("User-modified owned quest was preserved: " + name);
                }
            }
            if (!seen.equals(expected)) {
                throw new IOException("Incomplete owned quest archive was preserved: " + archive);
            }
            return hashes;
        }
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws IOException {
        try (var input = zip.getInputStream(entry)) {
            byte[] bytes = input.readNBytes(MAX_MEMBER_BYTES + 1);
            if (bytes.length > MAX_MEMBER_BYTES) {
                throw new IOException("Owned quest archive member exceeds its bound: " + entry.getName());
            }
            return bytes;
        }
    }

    private static Map<String, String> readManifest(String contents) throws IOException {
        com.google.gson.JsonElement parsed;
        try {
            parsed = JsonParser.parseString(contents);
        } catch (JsonParseException exception) {
            throw new IOException("Invalid Grindless quest bundle manifest", exception);
        }
        if (!parsed.isJsonObject()) {
            throw new IOException("Grindless quest manifest must be an object");
        }
        JsonObject document = parsed.getAsJsonObject();
        if (!document.keySet().equals(Set.of("owner", "schema", "files"))
                || !document.get("owner").isJsonPrimitive() || !document.get("owner").getAsJsonPrimitive().isString()
                || !"grindless".equals(document.get("owner").getAsString())
                || !document.get("schema").isJsonPrimitive() || !document.get("schema").getAsJsonPrimitive().isNumber()
                || !"2".equals(document.get("schema").getAsString()) || !document.get("files").isJsonObject()) {
            throw new IOException("Unrecognized Grindless quest bundle ownership manifest");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (var entry : document.getAsJsonObject("files").entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IOException("Invalid Grindless quest ownership hash: " + entry.getKey());
            }
            String value = entry.getValue().getAsString();
            if (!entry.getKey().matches("grindless_[a-z0-9_]+") || !value.matches("[a-f0-9]{64}")) {
                throw new IOException("Invalid Grindless quest ownership entry: " + entry.getKey());
            }
            result.put(entry.getKey(), value);
        }
        if (result.isEmpty()) {
            throw new IOException("Owned Grindless quest archive cannot be empty");
        }
        return result;
    }

    private static void checkDirectories(Path config) throws IOException {
        for (Path path : List.of(config, config.resolve("heracles"),
                config.resolve("heracles").resolve("quests"), bundle(config))) {
            if (Files.isSymbolicLink(path)) {
                throw new IOException("Grindless quest output cannot use symbolic links: " + path);
            }
        }
    }

    private static void checkCollisions(Path quests, Path owned, Set<String> ids)
            throws IOException {
        if (!Files.exists(quests)) {
            return;
        }
        try (var paths = Files.walk(quests)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (path.equals(owned)) {
                    continue;
                }
                String name = path.getFileName().toString();
                String id = nativeId(name);
                if (name.endsWith(".json") && ids.contains(id)) {
                    throw new IOException("Quest id collision, including legacy loose definitions; existing file was preserved: " + path);
                } else if (name.endsWith(".zip")) {
                    try (ZipFile zip = new ZipFile(path.toFile())) {
                        var entries = zip.entries();
                        while (entries.hasMoreElements()) {
                            String member = entries.nextElement().getName().replace('\\', '/');
                            if (member.endsWith(".json") && ids.contains(nativeId(member.substring(member.lastIndexOf('/') + 1)))) {
                                throw new IOException("Quest id collision in preserved archive: " + path + "!" + member);
                            }
                        }
                    }
                }
            }
        }
    }

    private static String nativeId(String name) {
        int dot = name.indexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String hash(String contents) {
        return hash(contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(byte[] contents) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contents));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static Map<String, String> hashes(Map<String, String> definitions) {
        Map<String, String> hashes = new LinkedHashMap<>();
        definitions.forEach((id, contents) -> hashes.put(id, hash(contents)));
        return hashes;
    }

    static void writeBundle(OutputStream destination, Map<String, String> definitions) throws IOException {
        JsonObject files = new JsonObject();
        hashes(definitions).forEach(files::addProperty);
        JsonObject manifest = new JsonObject();
        manifest.addProperty("owner", "grindless");
        manifest.addProperty("schema", 2);
        manifest.add("files", files);
        try (ZipOutputStream zip = new ZipOutputStream(destination, StandardCharsets.UTF_8)) {
            writeMember(zip, MANIFEST, JSON.toJson(manifest) + "\n");
            for (var entry : definitions.entrySet()) {
                writeMember(zip, entry.getKey() + ".json", entry.getValue());
            }
        }
    }

    private static void writeMember(ZipOutputStream zip, String name, String contents) throws IOException {
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_MEMBER_BYTES) {
            throw new IOException("Generated quest archive member exceeds its bound: " + name);
        }
        ZipEntry member = new ZipEntry(name);
        member.setTime(0);
        zip.putNextEntry(member);
        zip.write(bytes);
        zip.closeEntry();
    }
}
