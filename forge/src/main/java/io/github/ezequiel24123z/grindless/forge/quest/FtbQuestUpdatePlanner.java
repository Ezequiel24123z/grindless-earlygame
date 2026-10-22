package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.ezequiel24123z.grindless.forge.quest.FtbQuestDefinitionPublisher.*;

/** Native-SNBT preflight only. The stopped-target Prism transaction is the sole mutation authority. */
public final class FtbQuestUpdatePlanner {
    static final String LEGACY_SOURCE = "dd0b4bc93187fe172fd336bfa330987220bd7be4";
    static final String LEGACY_JAR = "29bcce43372016719fe1202b8859a18c93147562c04a2ef1daf163fddf7cc54f";
    private static final Set<String> ADDED_LESSONS = Set.of("first_grid", "forming_dies", "copper_coil", "flux_cell");
    private static final String OWNER = ".grindless-dev-owner.json";
    private static final String DEPLOYMENT = ".grindless-dev-deployment.json";
    private static final String RECEIPT = ".grindless-definitions.json";

    private FtbQuestUpdatePlanner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 9) {
            throw new IllegalArgumentException("Expected ROOT SOURCE RUNTIME JAR JAR_SHA256 INSTANCE PREVIOUS PREVIOUS_SHA256 OUTPUT");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        verifySource(root, args[1], args[2]);
        requireClassOrigin(root, FtbQuestDefinitions.class);
        requireClassOrigin(root, QuestCatalogue.class);
        if (!hash(readRegular(Path.of(args[3]), 64 * 1024 * 1024)).equals(args[4])) {
            throw new IOException("Planner runtime JAR differs from its verified SHA256");
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        JsonObject current = definitionSet(FtbQuestDefinitions.chapters(), args[1], args[2], args[4]);
        byte[] previous = readRegular(Path.of(args[6]), 4 * 1024 * 1024);
        requireHash(args[7]);
        if (!hash(previous).equals(args[7])) {
            throw new IOException("Previous definition publication differs from the explicitly verified SHA256");
        }
        JsonObject plan = prepare(Path.of(args[5]), previous, current);
        Path output = Path.of(args[8]).toAbsolutePath().normalize();
        if (!output.startsWith(root.resolve("build")) && !output.startsWith(root.resolve("forge").resolve("build"))) {
            throw new IOException("Update plans belong only in the execution checkout's build output");
        }
        writeNew(output, encode(plan));
        System.out.println("FTB_DEFINITION_PLAN_PUBLISHED source=" + args[1] + " sha256=" + hash(encode(plan))
                + " target_writes=0 native_progress_writes=0");
    }

    static JsonObject prepare(Path instance, byte[] previousPublication, JsonObject currentPublication) throws IOException {
        instance = instance.toAbsolutePath().normalize();
        checkPath(instance);
        String instanceId = instance.getFileName().toString();
        if (!instanceId.matches("Grindless-[A-Za-z0-9._+-]+-Dev")) {
            throw new IOException("Managed definitions require a dedicated Grindless engineering instance");
        }
        Path config = instance.resolve("minecraft").resolve("config");
        Path receiptPath = config.resolve("ftbquests").resolve(RECEIPT);
        if (Files.exists(instance.resolve(".grindless-dev-pending.json"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Incomplete runtime deployment must be recovered first");
        }
        FtbQuestFiles.refusePendingUpdate(config);
        byte[] ownerBytes = readRegular(instance.resolve(OWNER), 2 * 1024 * 1024);
        byte[] deploymentBytes = readRegular(instance.resolve(DEPLOYMENT), 2 * 1024 * 1024);
        JsonObject owner = object(ownerBytes);
        keys(owner, "schemaVersion", "tool", "purpose", "instanceId", "ownershipId");
        String ownership = string(owner, "ownershipId");
        if (integer(owner, "schemaVersion") != 1 || !string(owner, "tool").equals("grindless-prism-dev")
                || !string(owner, "purpose").equals("engineering-development")
                || !string(owner, "instanceId").equals(instanceId) || !ownership.matches("[0-9a-f]{32}")) {
            throw new IOException("Missing or unknown managed instance ownership");
        }
        JsonObject deployment = object(deploymentBytes);
        if (integer(deployment, "schemaVersion") != 1 || !string(deployment, "tool").equals("grindless-prism-dev")
                || !string(deployment, "state").equals("deployed") || !string(deployment, "instanceId").equals(instanceId)
                || !string(deployment, "ownershipId").equals(ownership)) {
            throw new IOException("Unknown deployment provenance");
        }
        DefinitionSet previous = readSet(object(previousPublication));
        DefinitionSet current = readSet(currentPublication);
        if (current.questCount() != 50 || !current.documents().equals(FtbQuestDefinitions.chapters())) {
            throw new IOException("Update destination must be the actual current 50-quest generator");
        }
        JsonElement installedJar = deployment.get("grindless");
        if (installedJar == null || !installedJar.isJsonObject()
                || !string(deployment, "sourceRevision").equals(current.source())
                || !string(deployment, "runtimeSourceRevision").equals(current.runtime())
                || !string(installedJar.getAsJsonObject(), "sha256").equals(current.jarHash())) {
            throw new IOException("Deploy the verified current package while stopped before planning its definition update");
        }
        validateTransition(previous, current);
        JsonObject newReceipt = receipt(current, instanceId, ownership);
        byte[] receiptBytes = null;
        DefinitionSet before = previous;
        if (Files.exists(receiptPath, LinkOption.NOFOLLOW_LINKS)) {
            receiptBytes = readRegular(receiptPath, 2 * 1024 * 1024);
            JsonObject existing = object(receiptBytes);
            if (existing.equals(newReceipt)) {
                before = current;
            } else if (!existing.equals(receipt(previous, instanceId, ownership))) {
                throw new IOException("Unknown or modified native definition ownership receipt");
            }
        } else {
            // Names and matching IDs do not establish ownership. The reference must be an actual
            // publication of the known old source/runtime, with caller-verified artifact bytes.
            if (previous.questCount() != 46 || !previous.source().equals(LEGACY_SOURCE)
                    || !previous.jarHash().equals(LEGACY_JAR)) {
                throw new IOException("Unreceipted definitions require the authentic pinned dd0/46 source/runtime publication");
            }
        }
        Map<String, byte[]> snapshot = FtbQuestFiles.managedSnapshot(config, before.documents(), current.documents());
        for (String name : FILENAMES) {
            if (!Arrays.equals(snapshot.get(name), before.contents().get(name))) {
                throw new IOException("User-modified or unknown owned chapter was preserved: " + name);
            }
        }
        // Re-read metadata after the whole native parse, just as the apply tool rechecks file bytes.
        if (!Arrays.equals(ownerBytes, readRegular(instance.resolve(OWNER), 2 * 1024 * 1024))
                || !Arrays.equals(deploymentBytes, readRegular(instance.resolve(DEPLOYMENT), 2 * 1024 * 1024))
                || (receiptBytes == null ? Files.exists(receiptPath, LinkOption.NOFOLLOW_LINKS)
                : !Arrays.equals(receiptBytes, readRegular(receiptPath, 2 * 1024 * 1024)))) {
            throw new IOException("Definition ownership/deployment drifted during native preflight");
        }
        JsonObject plan = new JsonObject();
        plan.addProperty("schemaVersion", 1);
        plan.addProperty("tool", "grindless-ftb-definition-update");
        plan.addProperty("sourceRevision", current.source());
        plan.addProperty("runtimeSourceRevision", current.runtime());
        plan.addProperty("grindlessSha256", current.jarHash());
        plan.addProperty("instanceId", instanceId);
        plan.addProperty("ownershipId", ownership);
        plan.addProperty("target", instance.toString());
        plan.addProperty("ownerSha256", hash(ownerBytes));
        plan.addProperty("deploymentSha256", hash(deploymentBytes));
        plan.add("previousReceiptSha256", receiptBytes == null ? JsonNull.INSTANCE : new JsonPrimitive(hash(receiptBytes)));
        plan.addProperty("previousDefinitionsSha256", hash(previousPublication));
        JsonArray chapters = new JsonArray();
        snapshot.forEach((name, contents) -> chapters.add(file(name, contents)));
        plan.add("chapters", chapters);
        JsonArray files = new JsonArray();
        for (String name : FILENAMES) {
            byte[] oldBytes = before.contents().get(name);
            byte[] newBytes = current.contents().get(name);
            JsonObject replacement = new JsonObject();
            replacement.addProperty("fileName", name);
            replacement.addProperty("beforeSha256", hash(oldBytes));
            replacement.addProperty("beforeBytes", oldBytes.length);
            replacement.addProperty("afterSha256", hash(newBytes));
            replacement.addProperty("afterBytes", newBytes.length);
            replacement.addProperty("contentsBase64", Base64.getEncoder().encodeToString(newBytes));
            files.add(replacement);
        }
        plan.add("files", files);
        plan.add("receipt", newReceipt);
        return plan;
    }

    private static DefinitionSet readSet(JsonObject published) throws IOException {
        keys(published, "schemaVersion", "tool", "sourceRevision", "runtimeSourceRevision", "grindlessSha256", "questCount", "files");
        if (integer(published, "schemaVersion") != 1 || !string(published, "tool").equals("grindless-ftb-definition-set")) {
            throw new IOException("Unknown native definition publication version");
        }
        String source = string(published, "sourceRevision");
        String runtime = string(published, "runtimeSourceRevision");
        String jar = string(published, "grindlessSha256");
        requireRevision(source);
        requireRevision(runtime);
        requireHash(jar);
        int questCount = integer(published, "questCount");
        if ((questCount != 46 && questCount != 50) || !published.get("files").isJsonArray()
                || published.getAsJsonArray("files").size() != 2) {
            throw new IOException("Unknown native definition catalogue version");
        }
        Map<String, byte[]> contents = new LinkedHashMap<>();
        Map<String, CompoundTag> documents = new LinkedHashMap<>();
        for (JsonElement element : published.getAsJsonArray("files")) {
            if (!element.isJsonObject()) {
                throw new IOException("Malformed published native chapter");
            }
            JsonObject file = element.getAsJsonObject();
            keys(file, "fileName", "bytes", "sha256", "contentsBase64");
            String name = string(file, "fileName");
            byte[] bytes;
            CompoundTag document;
            try {
                bytes = Base64.getDecoder().decode(string(file, "contentsBase64"));
                if (bytes.length <= 0 || bytes.length > 1024 * 1024 || bytes.length != integer(file, "bytes")
                        || !hash(bytes).equals(string(file, "sha256"))) {
                    throw new IOException("Published native chapter bytes/hash mismatch");
                }
                String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                document = TagParser.parseTag(text);
            } catch (IllegalArgumentException | com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new IOException("Malformed published native SNBT", exception);
            }
            if (!FILENAMES.contains(name) || contents.putIfAbsent(name, bytes) != null
                    || !document.getString("filename").equals(name.substring(0, name.length() - 5))) {
                throw new IOException("Unknown or duplicate published native chapter filename");
            }
            documents.put(name, document);
        }
        FtbQuestFiles.generated(documents);
        if (documents.values().stream().mapToInt(document -> document.getList("quests", Tag.TAG_COMPOUND).size()).sum() != questCount) {
            throw new IOException("Published native quest count does not match its documents");
        }
        return new DefinitionSet(source, runtime, jar, questCount, contents, documents);
    }

    private static void validateTransition(DefinitionSet previous, DefinitionSet current) throws IOException {
        Map<String, CompoundTag> oldQuests = quests(previous.documents());
        Map<String, CompoundTag> newQuests = quests(current.documents());
        Set<String> expectedOld = QuestCatalogue.tasks().stream()
                .filter(task -> previous.questCount() == 50 || !ADDED_LESSONS.contains(task.id()))
                .map(task -> FtbQuestDefinitions.code(FtbQuestDefinitions.questId(task.id()))).collect(Collectors.toSet());
        if (!oldQuests.keySet().equals(expectedOld)) {
            throw new IOException("Previous definition identities do not match the known 46/50 contract");
        }
        for (String name : FILENAMES) {
            if (!previous.documents().get(name).getString("id").equals(current.documents().get(name).getString("id"))) {
                throw new IOException("Native chapter identity changed");
            }
        }
        for (var entry : oldQuests.entrySet()) {
            CompoundTag oldQuest = entry.getValue();
            CompoundTag newQuest = newQuests.get(entry.getKey());
            if (!oldQuest.getList("tasks", Tag.TAG_COMPOUND).equals(newQuest.getList("tasks", Tag.TAG_COMPOUND))
                    || !oldQuest.getList("rewards", Tag.TAG_COMPOUND).equals(newQuest.getList("rewards", Tag.TAG_COMPOUND))
                    || !oldQuest.getList("tags", Tag.TAG_STRING).equals(newQuest.getList("tags", Tag.TAG_STRING))) {
                throw new IOException("A retained logical/task/reward identity or paid reward contract changed");
            }
        }
        for (String lesson : ADDED_LESSONS) {
            CompoundTag quest = newQuests.get(FtbQuestDefinitions.code(FtbQuestDefinitions.questId(lesson)));
            if (!quest.getList("rewards", Tag.TAG_COMPOUND).isEmpty()) {
                throw new IOException("New teaching must remain reward-free");
            }
        }
    }

    private static Map<String, CompoundTag> quests(Map<String, CompoundTag> documents) throws IOException {
        Map<String, CompoundTag> result = new LinkedHashMap<>();
        for (CompoundTag document : documents.values()) {
            for (Tag tag : document.getList("quests", Tag.TAG_COMPOUND)) {
                if (!(tag instanceof CompoundTag quest) || result.putIfAbsent(quest.getString("id"), quest) != null) {
                    throw new IOException("Malformed or colliding native quest identity");
                }
            }
        }
        return result;
    }

    private static JsonObject receipt(DefinitionSet set, String instance, String ownership) {
        JsonObject receipt = new JsonObject();
        receipt.addProperty("schemaVersion", 1);
        receipt.addProperty("tool", "grindless-ftb-definitions");
        receipt.addProperty("instanceId", instance);
        receipt.addProperty("ownershipId", ownership);
        receipt.addProperty("sourceRevision", set.source());
        receipt.addProperty("runtimeSourceRevision", set.runtime());
        receipt.addProperty("grindlessSha256", set.jarHash());
        receipt.addProperty("questCount", set.questCount());
        JsonArray files = new JsonArray();
        FILENAMES.forEach(name -> files.add(file(name, set.contents().get(name))));
        receipt.add("files", files);
        return receipt;
    }

    private static void keys(JsonObject object, String... names) throws IOException {
        if (object == null || !object.keySet().equals(Set.of(names))) {
            throw new IOException("Unknown or missing native definition metadata fields");
        }
    }

    private static String string(JsonObject object, String name) throws IOException {
        JsonElement value = object == null ? null : object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IOException("Missing or invalid native definition string: " + name);
        }
        return value.getAsString();
    }

    private static int integer(JsonObject object, String name) throws IOException {
        JsonElement value = object == null ? null : object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                || !value.getAsString().matches("[0-9]{1,9}")) {
            throw new IOException("Missing or invalid native definition integer: " + name);
        }
        return value.getAsInt();
    }

    private static JsonObject object(byte[] bytes) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()))) {
            reader.setLenient(false);
            JsonElement value = json(reader, 0);
            if (!value.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Native definition metadata must be one complete JSON object");
            }
            return value.getAsJsonObject();
        } catch (IllegalStateException | NumberFormatException exception) {
            throw new IOException("Malformed native definition JSON", exception);
        }
    }

    private static JsonElement json(JsonReader reader, int depth) throws IOException {
        if (depth > 40) {
            throw new IOException("Native definition metadata exceeds its nesting bound");
        }
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject();
                JsonObject value = new JsonObject();
                Set<String> names = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!names.add(name)) {
                        throw new IOException("Duplicate native definition metadata field");
                    }
                    value.add(name, json(reader, depth + 1));
                }
                reader.endObject();
                yield value;
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                JsonArray value = new JsonArray();
                while (reader.hasNext()) {
                    value.add(json(reader, depth + 1));
                }
                reader.endArray();
                yield value;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> throw new IOException("Malformed native definition JSON token");
        };
    }

    private record DefinitionSet(String source, String runtime, String jarHash, int questCount,
                                 Map<String, byte[]> contents, Map<String, CompoundTag> documents) {
    }
}
