package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipFile;

/** Catalogue-generated native definitions; a hash manifest protects existing user configuration. */
public final class HeraclesDefinitions {

    public static final String GROUP = "Grindless";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private HeraclesDefinitions() {
    }

    public static Map<String, Quest> create() {
        return create(task -> {
            ResourceLocation id = ResourceLocation.tryParse(task.subject());
            if (task.evidence() == QuestCatalogue.Evidence.DIMENSION) {
                return Items.BOOK;
            }
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.get(id) == Items.AIR) {
                throw new IllegalStateException("Quest subject is not registered: " + task.subject());
            }
            return BuiltInRegistries.ITEM.get(id);
        });
    }

    static Map<String, Quest> create(Function<QuestCatalogue.Task, Item> icons) {
        Map<String, Quest> result = new LinkedHashMap<>();
        Map<String, Integer> depths = new LinkedHashMap<>();
        List<QuestProjection.Entry> entries = QuestProjection.entries();
        for (QuestProjection.Entry entry : entries) {
            depths.put(entry.task().id(),
                    entry.task().requires().stream().mapToInt(id -> depths.get(id) + 1).max().orElse(0));
        }
        int lineTop = 0;
        for (String line : QuestCatalogue.lines()) {
            Map<Integer, Integer> rows = new LinkedHashMap<>();
            for (QuestProjection.Entry entry : entries) {
                QuestCatalogue.Task task = entry.task();
                if (!line.equals(task.line())) {
                    continue;
                }
                int depth = depths.get(task.id());
                int row = rows.getOrDefault(depth, 0);
                rows.put(depth, row + 1);
                QuestDisplay display = QuestDisplay.createDefault(
                        new GroupDisplay(GROUP, new Vector2i(depth * 96, lineTop + row * 56)));
                display.setIcon(new ItemQuestIcon(icons.apply(task)));
                display.setTitle(Component.translatable("gui.grindless.quest.task." + task.id()));
                display.setSubtitle(Component.translatable("gui.grindless.quest.line." + task.line()));
                display.setDescription(List.of());
                QuestSettings settings = QuestSettings.createDefault();
                settings.setIndividualProgress(true);
                settings.setRepeatable(false);
                settings.setAutoClaimRewards(false);
                settings.setUnlockNotification(false);
                settings.setShowDependencyArrow(true);
                settings.setHiddenUntil(QuestDisplayStatus.LOCKED);
                result.put(entry.id(), new Quest(display, settings, new LinkedHashSet<>(entry.dependencies()),
                        new LinkedHashMap<>(Map.of(QuestProjection.CLAIM_TASK,
                                new GrindlessClaimTask(QuestProjection.CLAIM_TASK, task.id()))),
                        new LinkedHashMap<>()));
            }
            lineTop += rows.values().stream().mapToInt(Integer::intValue).max().orElse(1) * 56 + 40;
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
                && actual.rewards().isEmpty() && actual.settings().individualProgress()
                && !actual.settings().autoClaimRewards() && !actual.settings().repeatable();
    }

    public static void synchronize(Path config, Map<String, String> definitions) throws IOException {
        checkDirectories(config);
        Path quests = config.resolve("heracles").resolve("quests");
        Path owned = quests.resolve("grindless");
        Path manifest = config.resolve("grindless").resolve("heracles-manifest.json");
        Map<String, String> previous = readManifest(manifest);
        checkFiles(quests, owned, previous, definitions.keySet());
        Files.createDirectories(owned);
        for (var entry : definitions.entrySet()) {
            write(owned.resolve(entry.getKey() + ".json"), entry.getValue());
        }
        JsonObject hashes = new JsonObject();
        definitions.forEach((id, contents) -> hashes.addProperty(id, hash(contents)));
        JsonObject document = new JsonObject();
        document.addProperty("owner", "grindless");
        document.addProperty("schema", 1);
        document.add("files", hashes);
        Files.createDirectories(manifest.getParent());
        write(manifest, JSON.toJson(document) + "\n");
    }

    public static void verifyFiles(Path config, Map<String, String> definitions) throws IOException {
        checkDirectories(config);
        Path quests = config.resolve("heracles").resolve("quests");
        Path owned = quests.resolve("grindless");
        Map<String, String> manifest = readManifest(config.resolve("grindless").resolve("heracles-manifest.json"));
        if (!manifest.keySet().equals(definitions.keySet())) {
            throw new IOException("Grindless quest manifest does not match the current catalogue");
        }
        checkFiles(quests, owned, manifest, definitions.keySet());
        for (var entry : definitions.entrySet()) {
            if (!hash(entry.getValue()).equals(manifest.get(entry.getKey()))) {
                throw new IOException("Grindless quest definition changed since generation: " + entry.getKey());
            }
        }
    }

    private static Map<String, String> readManifest(Path manifest) throws IOException {
        if (!Files.exists(manifest)) {
            return Map.of();
        }
        var parsed = JsonParser.parseString(Files.readString(manifest));
        if (!parsed.isJsonObject()) {
            throw new IOException("Grindless quest manifest must be an object: " + manifest);
        }
        JsonObject document = parsed.getAsJsonObject();
        if (!document.has("owner") || !document.get("owner").isJsonPrimitive()
                || !"grindless".equals(document.get("owner").getAsString())
                || !document.has("schema") || !document.get("schema").isJsonPrimitive()
                || !"1".equals(document.get("schema").getAsString())
                || !document.has("files") || !document.get("files").isJsonObject()) {
            throw new IOException("Unrecognized Grindless quest ownership manifest: " + manifest);
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
        return result;
    }

    private static void checkDirectories(Path config) throws IOException {
        for (Path path : List.of(config.resolve("heracles"), config.resolve("heracles").resolve("quests"),
                config.resolve("heracles").resolve("quests").resolve("grindless"),
                config.resolve("grindless"), config.resolve("grindless").resolve("heracles-manifest.json"))) {
            if (Files.isSymbolicLink(path)) {
                throw new IOException("Grindless quest output cannot use symbolic links: " + path);
            }
        }
    }

    private static void checkFiles(Path quests, Path owned, Map<String, String> previous, Set<String> ids)
            throws IOException {
        if (!ids.containsAll(previous.keySet())) {
            throw new IOException("Removed owned quests require an explicit migration; no files were deleted");
        }
        for (String id : ids) {
            Path destination = owned.resolve(id + ".json");
            if (Files.exists(destination, java.nio.file.LinkOption.NOFOLLOW_LINKS) && !previous.containsKey(id)) {
                throw new IOException("Unowned quest output already exists; refusing to overwrite: " + destination);
            }
        }
        for (var entry : previous.entrySet()) {
            Path file = owned.resolve(entry.getKey() + ".json");
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)
                    || !entry.getValue().equals(hash(Files.readString(file)))) {
                throw new IOException("Owned quest is missing or user-modified; refusing to overwrite: " + file);
            }
        }
        if (!Files.exists(quests)) {
            return;
        }
        if (Files.isSymbolicLink(quests) || Files.isSymbolicLink(owned)) {
            throw new IOException("Grindless quest output cannot use a symbolic-link directory");
        }
        try (var paths = Files.walk(quests)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString();
                String id = nativeId(name);
                if (name.endsWith(".json") && ids.contains(id)) {
                    if (!path.equals(owned.resolve(id + ".json")) || !previous.containsKey(id)) {
                        throw new IOException("Quest id collision; existing file was preserved: " + path);
                    }
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
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contents.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static void write(Path destination, String contents) throws IOException {
        if (Files.exists(destination) && contents.equals(Files.readString(destination))) {
            return;
        }
        Path temporary = Files.createTempFile(destination.getParent(), ".grindless-", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
