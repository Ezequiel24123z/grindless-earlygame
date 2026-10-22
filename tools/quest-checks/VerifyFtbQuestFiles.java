package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.ezequiel24123z.grindless.forge.quest.FtbQuestDefinitionPublisher.*;

/** Real native-parser/filesystem contracts, not a real dd0 export or native team/world execution. */
public final class VerifyFtbQuestFiles {
    private static final Set<String> ADDED = Set.of(
            "first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation");
    private static final String CURRENT_SOURCE = "a".repeat(40);
    private static int assertions;

    private VerifyFtbQuestFiles() {
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Files.createDirectories(Path.of("build"));
        Path fixture = Files.createTempDirectory(Path.of("build"), "ftb-managed-files-");
        staleCompilation(fixture);
        jarPayloadContract(fixture);
        Map<String, CompoundTag> current = FtbQuestDefinitions.chapters();
        equal(51, currentQuestCount(), "current content follows the complete bound catalogue");
        equal(46, knownLogicalIds(46).size(), "known46 identity set");
        equal(50, knownLogicalIds(50).size(), "known50 identity set");
        equal(51, knownLogicalIds(51).size(), "known51 identity set");
        Set<String> delta50 = new java.util.HashSet<>(knownLogicalIds(50));
        delta50.removeAll(knownLogicalIds(46));
        equal(ADDED_50, delta50, "only the four reviewed lessons extend46");
        Set<String> delta51 = new java.util.HashSet<>(knownLogicalIds(51));
        delta51.removeAll(knownLogicalIds(50));
        equal(ADDED_51, delta51, "only wet beneficiation extends50");
        refuses(() -> knownLogicalIds(47), "47 is not a content version");
        refuses(() -> knownLogicalIds(52), "52 is not a content version");
        Map<String, FtbQuestFiles.Chapter> encodedCurrent = FtbQuestFiles.generated(current);
        Map<String, CompoundTag> previous50 = new LinkedHashMap<>();
        current.forEach((name, document) -> {
            CompoundTag copy = document.copy();
            ListTag quests = copy.getList("quests", Tag.TAG_COMPOUND);
            for (int index = quests.size() - 1; index >= 0; index--) {
                if (quests.getCompound(index).getString("id")
                        .equals(FtbQuestDefinitions.code(FtbQuestDefinitions.questId("wet_beneficiation")))) {
                    quests.remove(index);
                }
            }
            previous50.put(name, copy);
        });
        Path config50 = fixture.resolve("previous50-config");
        FtbQuestFiles.ensure(config50, previous50);
        Map<Path, String> before50 = snapshot(config50);
        refuses(() -> FtbQuestFiles.ensure(config50, current),
                "startup preserves its fail-closed50-to51 boundary for a missing optional lesson");
        equal(before50, snapshot(config50), "missing wet definitions cannot silently rewrite an installed50 chapter");
        // Synthetic old-ID input: never publish this as an authentic historical source/hash receipt.
        Map<String, CompoundTag> previous = new LinkedHashMap<>();
        current.forEach((name, document) -> {
            CompoundTag copy = document.copy();
            ListTag quests = copy.getList("quests", Tag.TAG_COMPOUND);
            for (int index = quests.size() - 1; index >= 0; index--) {
                String id = quests.getCompound(index).getString("id");
                if (ADDED.stream().anyMatch(lesson -> FtbQuestDefinitions.code(FtbQuestDefinitions.questId(lesson)).equals(id))) {
                    quests.remove(index);
                }
            }
            previous.put(name, copy);
        });
        refuses(() -> definitionSet(previous, CURRENT_SOURCE, CURRENT_SOURCE, "b".repeat(64), "f".repeat(64)),
                "a reduced46 fixture is not an authentic publication by current51 classes");
        refuses(() -> definitionSet(previous50, CURRENT_SOURCE, CURRENT_SOURCE, "b".repeat(64), "f".repeat(64)),
                "a reduced50 fixture is not an authentic publication by current51 classes");
        JsonObject reference = syntheticPublication(previous, FtbQuestUpdatePlanner.LEGACY_SOURCE,
                FtbQuestUpdatePlanner.LEGACY_SOURCE, FtbQuestUpdatePlanner.LEGACY_JAR);
        JsonObject reference50 = syntheticPublication(previous50, "d".repeat(40), "d".repeat(40), "e".repeat(64));
        JsonObject destination = definitionSet(current, CURRENT_SOURCE, CURRENT_SOURCE, "b".repeat(64), "f".repeat(64));
        Path instance = fixture.resolve("Grindless-File-Dev");
        Path config = instance.resolve("minecraft").resolve("config");
        Path chapters = FtbQuestFiles.chapterDirectory(config);
        Files.createDirectories(instance);
        JsonObject owner = new JsonObject();
        owner.addProperty("schemaVersion", 1);
        owner.addProperty("tool", "grindless-prism-dev");
        owner.addProperty("purpose", "engineering-development");
        owner.addProperty("instanceId", instance.getFileName().toString());
        owner.addProperty("ownershipId", "c".repeat(32));
        Files.write(instance.resolve(".grindless-dev-owner.json"), encode(owner));
        JsonObject deployment = owner.deepCopy();
        deployment.remove("purpose");
        deployment.addProperty("state", "deployed");
        deployment.addProperty("sourceRevision", CURRENT_SOURCE);
        deployment.addProperty("runtimeSourceRevision", CURRENT_SOURCE);
        JsonObject jar = new JsonObject();
        jar.addProperty("sha256", "b".repeat(64));
        deployment.add("grindless", jar);
        Files.write(instance.resolve(".grindless-dev-deployment.json"), encode(deployment));
        FtbQuestFiles.ensure(config, previous);
        Path team = instance.resolve("minecraft").resolve("saves").resolve("reference").resolve("ftbquests").resolve("team.snbt");
        Files.createDirectories(team.getParent());
        Files.writeString(team, "SYNTHETIC OPAQUE PAID/TEAM STATE: NEVER PARSE OR REWRITE");
        Map<Path, String> before = snapshot(instance);
        refuses(() -> FtbQuestFiles.ensure(config, current), "startup preserves its fail-closed46-to51 boundary");
        equal(before, snapshot(instance), "startup missing-ID refusal never changes either chapter");
        JsonObject plan = FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination);
        equal(before, snapshot(instance), "whole native preflight writes no target byte");
        equal(2, plan.getAsJsonArray("files").size(), "only the two known definition filenames are replacements");
        equal(51, plan.getAsJsonObject("receipt").get("questCount").getAsInt(), "new receipt identifies51 definitions");
        for (var element : plan.getAsJsonArray("files")) {
            JsonObject replacement = element.getAsJsonObject();
            String name = replacement.get("fileName").getAsString();
            equal(hash(Files.readAllBytes(chapters.resolve(name))), replacement.get("beforeSha256").getAsString(),
                    "before hashes bind actual old bytes " + name);
            equal(hash(encodedCurrent.get(name).contents()), replacement.get("afterSha256").getAsString(),
                    "after hashes bind the append-only publisher's actual bytes " + name);
        }
        equal(hash(encode(reference)), plan.get("previousDefinitionsSha256").getAsString(), "plan binds the selected reference artifact");
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference50), destination),
                "unreceipted50 never acquires ownership from names/counts", "authentic pinned dd0/46");
        lineageRefusals(instance, reference50, previous50, destination);
        managed50(fixture, previous50, reference50, destination, owner, deployment, plan.getAsJsonObject("receipt"));

        Path t1 = chapters.resolve("grindless_t1.snbt");
        byte[] originalT1 = Files.readAllBytes(t1);
        Files.writeString(t1, "{malformed: [");
        Map<Path, String> malformed = snapshot(instance);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination), "malformed second chapter refuses the entire preflight");
        equal(malformed, snapshot(instance), "malformed second chapter cannot cause a first-file write");
        Files.write(t1, originalT1);
        CompoundTag edited = previous.get("grindless_t1.snbt").copy();
        edited.putString("title", "USER-EDITED COMPLETE CHAPTER");
        Files.writeString(t1, edited + "\n");
        FtbQuestFiles.ensure(config, previous);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination), "complete compatible edits are not silently adopted");
        equal(edited + "\n", Files.readString(t1), "compatible user chapter stays byte-identical");
        Files.write(t1, originalT1);

        Path foreign = chapters.resolve("foreign.snbt");
        Files.writeString(foreign, "{id:\"" + FtbQuestDefinitions.code(FtbQuestDefinitions.questId("first_grid")) + "\",quests:[]}");
        Map<Path, String> collided = snapshot(instance);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination), "foreign collision with a new quest ID refuses");
        equal(collided, snapshot(instance), "foreign collision is never renamed or overwritten");
        Files.writeString(foreign, "{id:\"0000000000007777\",item:{id:\""
                + FtbQuestDefinitions.code(FtbQuestDefinitions.questId("first_grid")) + "\"},quests:[]}");
        FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination);
        equal(true, Files.exists(foreign), "an arbitrary nested item id is not a native object collision");

        JsonObject unknownVersion = reference.deepCopy();
        unknownVersion.addProperty("schemaVersion", 3);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(unknownVersion), destination), "unknown publication schema refuses");
        JsonObject unqualifiedV1 = reference.deepCopy();
        unqualifiedV1.addProperty("schemaVersion", 1);
        unqualifiedV1.remove("compilationSha256");
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(unqualifiedV1), destination),
                "old path-only publication cannot stand in for compilation provenance");
        JsonObject unknownSource = reference.deepCopy();
        unknownSource.addProperty("sourceRevision", "d".repeat(40));
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(unknownSource), destination), "unreceipted unknown source refuses despite matching names and IDs");
        JsonObject badHash = reference.deepCopy();
        badHash.getAsJsonArray("files").get(1).getAsJsonObject().addProperty("sha256", "e".repeat(64));
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(badHash), destination), "modified reference payload refuses");
        String duplicate = new String(encode(reference), StandardCharsets.UTF_8).replaceFirst("\\{", "{\"schemaVersion\":1,");
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, duplicate.getBytes(StandardCharsets.UTF_8), destination), "duplicate provenance keys refuse");
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, new byte[0], destination), "missing reference is never inferred from chapter names");
        Map<String, CompoundTag> changedContract = new LinkedHashMap<>();
        previous.forEach((name, document) -> changedContract.put(name, document.copy()));
        String thermal = FtbQuestDefinitions.code(FtbQuestDefinitions.questId("thermal"));
        String multitool = FtbQuestDefinitions.code(FtbQuestDefinitions.questId("multitool"));
        for (CompoundTag document : changedContract.values()) {
            for (Tag value : document.getList("quests", Tag.TAG_COMPOUND)) {
                CompoundTag quest = (CompoundTag) value;
                if (quest.getString("id").equals(thermal)) {
                    quest.getList("tasks", Tag.TAG_COMPOUND).getCompound(0).putLong("count", 99);
                }
            }
        }
        JsonObject changedTask = syntheticPublication(changedContract, FtbQuestUpdatePlanner.LEGACY_SOURCE,
                FtbQuestUpdatePlanner.LEGACY_SOURCE, FtbQuestUpdatePlanner.LEGACY_JAR);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(changedTask), destination),
                "retained task contract cannot change even under a selected reference", "retained logical/task/reward");
        changedContract.clear();
        previous.forEach((name, document) -> changedContract.put(name, document.copy()));
        for (CompoundTag document : changedContract.values()) {
            for (Tag value : document.getList("quests", Tag.TAG_COMPOUND)) {
                CompoundTag quest = (CompoundTag) value;
                if (quest.getString("id").equals(multitool)) {
                    quest.getList("rewards", Tag.TAG_COMPOUND).getCompound(0).putInt("count", 99);
                }
            }
        }
        JsonObject changedReward = syntheticPublication(changedContract, FtbQuestUpdatePlanner.LEGACY_SOURCE,
                FtbQuestUpdatePlanner.LEGACY_SOURCE, FtbQuestUpdatePlanner.LEGACY_JAR);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(changedReward), destination),
                "paid reward contract cannot change even under a selected reference", "retained logical/task/reward");
        Path ownerPath = instance.resolve(".grindless-dev-owner.json");
        byte[] originalOwner = Files.readAllBytes(ownerPath);
        Files.delete(ownerPath);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination), "missing instance ownership refuses");
        Files.write(ownerPath, originalOwner);

        Path pending = config.resolve("ftbquests").resolve(".grindless-definition-update.json");
        Files.writeString(pending, "PARTIAL TRANSACTION SENTINEL");
        Map<Path, String> interrupted = snapshot(instance);
        refuses(() -> FtbQuestFiles.ensure(config, previous), "startup refuses a durable interrupted definition update");
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination), "new planning refuses an interrupted definition update");
        equal(interrupted, snapshot(instance), "interruption evidence is never removed by startup/planning");
        Files.delete(pending);

        // Only simulate publication here. The real Windows replacement/backup/recovery transaction
        // is covered by Test-PrismDev.ps1, not by this direct fixture setup.
        for (var element : plan.getAsJsonArray("files")) {
            JsonObject replacement = element.getAsJsonObject();
            Files.write(chapters.resolve(replacement.get("fileName").getAsString()),
                    Base64.getDecoder().decode(replacement.get("contentsBase64").getAsString()));
        }
        Files.write(config.resolve("ftbquests").resolve(".grindless-definitions.json"), encode(plan.getAsJsonObject("receipt")));
        Map<Path, String> published = snapshot(instance);
        JsonObject repeat = FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination);
        for (var element : repeat.getAsJsonArray("files")) {
            JsonObject replacement = element.getAsJsonObject();
            equal(replacement.get("beforeSha256"), replacement.get("afterSha256"), "already managed current definitions plan a no-op");
        }
        equal(published, snapshot(instance), "repeated native preflight remains read-only");
        JsonObject completed51 = FtbQuestUpdatePlanner.prepare(instance, encode(destination), destination);
        equal(51, completed51.getAsJsonObject("receipt").get("questCount").getAsInt(), "managed51 reference remains51");
        equal(published, snapshot(instance), "completed51 reference/current plan is a read-only no-op");
        equal("SYNTHETIC OPAQUE PAID/TEAM STATE: NEVER PARSE OR REWRITE", Files.readString(team), "native progress and paid history stay opaque and unchanged");

        Path compatibleConfig = fixture.resolve("compatible").resolve("config");
        FtbQuestFiles.ensure(compatibleConfig, current);
        Path compatibleT1 = FtbQuestFiles.chapterDirectory(compatibleConfig).resolve("grindless_t1.snbt");
        CompoundTag compatible = current.get("grindless_t1.snbt").copy();
        compatible.putString("title", "USER-COMPATIBLE COMPLETE51 CHAPTER");
        Files.writeString(compatibleT1, compatible + "\n");
        byte[] compatibleBytes = Files.readAllBytes(compatibleT1);
        FtbQuestFiles.ensure(compatibleConfig, current);
        equal(true, Arrays.equals(compatibleBytes, Files.readAllBytes(compatibleT1)), "ensure remains identity-complete, never unconditional exact-byte management");
        System.out.println("FTB_MANAGED_FILE_CONTRACT_PASS assertions=" + assertions + " fixture=" + fixture.toAbsolutePath());
        System.out.println("SYNTHETIC_REFERENCE_ONLY: this is not a genuine dd0 publication, registered native execution, paid-receipt reopen or owner-world pass.");
    }

    /** Deliberately doctored fixture metadata, never an authentic output of the publication CLI. */
    private static JsonObject syntheticPublication(Map<String, CompoundTag> documents, String source, String runtime,
                                                   String jar) throws IOException {
        JsonObject result = definitionSet(FtbQuestDefinitions.chapters(), source, runtime, jar, "f".repeat(64));
        result.addProperty("questCount", documents.values().stream()
                .mapToInt(document -> document.getList("quests", Tag.TAG_COMPOUND).size()).sum());
        JsonArray files = new JsonArray();
        for (String name : FILENAMES) {
            byte[] contents = (documents.get(name).copy() + "\n").getBytes(StandardCharsets.UTF_8);
            JsonObject file = file(name, contents);
            file.addProperty("contentsBase64", Base64.getEncoder().encodeToString(contents));
            files.add(file);
        }
        result.add("files", files);
        return result;
    }

    private static void lineageRefusals(Path instance, JsonObject previous, Map<String, CompoundTag> documents,
                                        JsonObject destination) throws Exception {
        Map<Path, String> before = snapshot(instance);
        for (int unknown : List.of(47, 52)) {
            JsonObject bad = previous.deepCopy();
            bad.addProperty("questCount", unknown);
            refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(bad), destination),
                    "unknown reference version must refuse", "Unknown native catalogue content version");
        }
        for (String mutation : List.of("quest", "missing", "task", "reward", "tag", "chapter")) {
            Map<String, CompoundTag> altered = new LinkedHashMap<>();
            documents.forEach((name, document) -> altered.put(name, document.copy()));
            CompoundTag t0 = altered.get("grindless_t0.snbt");
            CompoundTag first = t0.getList("quests", Tag.TAG_COMPOUND).getCompound(0);
            String expected = "";
            switch (mutation) {
                case "quest" -> {
                    first.putString("id", FtbQuestDefinitions.code(FtbQuestDefinitions.id("quest", "unknown")));
                    expected = "replaced native quest identity";
                }
                case "missing" -> t0.getList("quests", Tag.TAG_COMPOUND).remove(0);
                case "task" -> {
                    first.getList("tasks", Tag.TAG_COMPOUND).getCompound(0).putString("id",
                            FtbQuestDefinitions.code(FtbQuestDefinitions.id("task", "unknown")));
                    expected = "task identity drifted";
                }
                case "reward" -> {
                    first.getList("rewards", Tag.TAG_COMPOUND).getCompound(0).putString("id",
                            FtbQuestDefinitions.code(FtbQuestDefinitions.id("reward", "unknown")));
                    expected = "reward identity drifted";
                }
                case "tag" -> {
                    first.getList("tags", Tag.TAG_STRING).set(0, net.minecraft.nbt.StringTag.valueOf("grindless_unknown"));
                    expected = "logical tag or chapter placement drifted";
                }
                case "chapter" -> {
                    t0.putString("id", FtbQuestDefinitions.code(FtbQuestDefinitions.id("chapter", "unknown")));
                    expected = "chapter identity";
                }
                default -> throw new AssertionError(mutation);
            }
            JsonObject reference = syntheticPublication(altered, previous.get("sourceRevision").getAsString(),
                    previous.get("runtimeSourceRevision").getAsString(), previous.get("grindlessSha256").getAsString());
            if (mutation.equals("missing")) { reference.addProperty("questCount", 50); }
            String fragment = expected;
            refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination),
                    "known-count identities cannot drift: " + mutation, fragment);
        }
        JsonObject partialDestination = previous.deepCopy();
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(previous), partialDestination),
                "known50 is not the complete current51 destination", "complete actual source-bound generator");
        Map<String, CompoundTag> paidAddition = new LinkedHashMap<>();
        FtbQuestDefinitions.chapters().forEach((name, document) -> paidAddition.put(name, document.copy()));
        String wet = FtbQuestDefinitions.code(FtbQuestDefinitions.id("quest", "wet_beneficiation"));
        for (Tag value : paidAddition.get("grindless_t1.snbt").getList("quests", Tag.TAG_COMPOUND)) {
            CompoundTag quest = (CompoundTag) value;
            if (quest.getString("id").equals(wet)) {
                CompoundTag reward = new CompoundTag();
                reward.putString("id", FtbQuestDefinitions.code(FtbQuestDefinitions.id("reward", "wet_beneficiation/0")));
                quest.getList("rewards", Tag.TAG_COMPOUND).add(reward);
            }
        }
        JsonObject paid = syntheticPublication(paidAddition, CURRENT_SOURCE, CURRENT_SOURCE, "b".repeat(64));
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(previous), paid),
                "a new content lesson cannot gain native rewards");
        equal(before, snapshot(instance), "all lineage refusals happen before target mutation");
    }

    private static void managed50(Path fixture, Map<String, CompoundTag> documents, JsonObject reference,
                                   JsonObject destination, JsonObject owner, JsonObject deployment,
                                   JsonObject currentReceipt) throws Exception {
        Path instance = fixture.resolve("Grindless-Managed50-Dev");
        Path config = instance.resolve("minecraft").resolve("config");
        Files.createDirectories(instance);
        JsonObject ownership = owner.deepCopy();
        ownership.addProperty("instanceId", instance.getFileName().toString());
        JsonObject installed = deployment.deepCopy();
        installed.addProperty("instanceId", instance.getFileName().toString());
        Files.write(instance.resolve(".grindless-dev-owner.json"), encode(ownership));
        Files.write(instance.resolve(".grindless-dev-deployment.json"), encode(installed));
        FtbQuestFiles.ensure(config, documents);
        JsonObject receipt = currentReceipt.deepCopy();
        receipt.addProperty("instanceId", instance.getFileName().toString());
        for (String key : List.of("sourceRevision", "runtimeSourceRevision", "grindlessSha256", "questCount")) {
            receipt.add(key, reference.get(key).deepCopy());
        }
        JsonArray files = reference.getAsJsonArray("files").deepCopy();
        files.forEach(value -> value.getAsJsonObject().remove("contentsBase64"));
        receipt.add("files", files);
        Path receiptPath = config.resolve("ftbquests").resolve(".grindless-definitions.json");
        Files.write(receiptPath, encode(receipt));
        Map<Path, String> before = snapshot(instance);
        JsonObject plan = FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination);
        equal(51, plan.getAsJsonObject("receipt").get("questCount").getAsInt(), "managed50-to51 plan succeeds");
        equal(hash(encode(receipt)), plan.get("previousReceiptSha256").getAsString(), "managed50 receipt is hash-bound");
        equal(before, snapshot(instance), "managed50 planning writes nothing");
        JsonObject changed = receipt.deepCopy();
        changed.addProperty("sourceRevision", "e".repeat(40));
        Files.write(receiptPath, encode(changed));
        Map<Path, String> changedBefore = snapshot(instance);
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination),
                "unknown managed50 source receipt refuses", "Unknown or modified native definition ownership receipt");
        equal(changedBefore, snapshot(instance), "refused source receipt is preserved exactly");
        Files.write(receiptPath, encode(receipt));
        Map<Path, String> restored = snapshot(instance);
        JsonObject badReference = reference.deepCopy();
        badReference.getAsJsonArray("files").get(0).getAsJsonObject().addProperty("sha256", "0".repeat(64));
        refuses(() -> FtbQuestUpdatePlanner.prepare(instance, encode(badReference), destination), "managed50 publication hash drift refuses");
        equal(restored, snapshot(instance), "managed50 refused attempts preserve all source/receipt files");
    }

    private static void staleCompilation(Path fixture) throws Exception {
        Path root = fixture.resolve("compiler-proof").toAbsolutePath();
        Path expected = root.resolve("build").resolve("ftb-compilation-" + "1".repeat(32))
                .resolve("forge").resolve("classes").resolve("java").resolve("main");
        Path stale = root.resolve("forge").resolve("build").resolve("classes").resolve("java").resolve("main");
        Path source = root.resolve("src").resolve("fixture").resolve("Definitions.java");
        Path otherSource = fixture.resolve("other-source").resolve("fixture").resolve("Definitions.java").toAbsolutePath();
        Files.createDirectories(source.getParent());
        Files.createDirectories(otherSource.getParent());
        Files.createDirectories(expected);
        Files.createDirectories(stale);
        String prefix = "package fixture; public class Definitions { public static int chapters() { return 46; } "
                + "public static String content() { return \"";
        Files.writeString(source, prefix + "correct\"; } }");
        Files.writeString(otherSource, prefix + "stale\"; } }");
        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) { throw new AssertionError("A real JDK compiler is required for the stale-class regression"); }
        equal(0, compiler.run(null, null, null, "-proc:none", "-d", expected.toString(), source.toString()), "compile the reference fixture");
        equal(0, compiler.run(null, null, null, "-proc:none", "-d", stale.toString(), otherSource.toString()), "compile same-count stale fixture");
        String member = "fixture/Definitions.class";
        byte[] expectedBytes = Files.readAllBytes(expected.resolve(member));
        byte[] staleBytes = Files.readAllBytes(stale.resolve(member));
        String sourceHash = hash(Files.readAllBytes(source));
        Path selectedJar = root.resolve("correct-runtime.jar");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(selectedJar))) {
            zip.putNextEntry(new java.util.zip.ZipEntry(member));
            zip.write(expectedBytes);
            zip.closeEntry();
        }
        String jarHash = hash(Files.readAllBytes(selectedJar));
        var proof = new FtbQuestDefinitionPublisher.CompilationProof(
                Map.of("forge", expected), Map.of("forge", Map.of(member, hash(expectedBytes))));
        try (var loader = new URLClassLoader(new java.net.URL[]{stale.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> type = Class.forName("fixture.Definitions", true, loader);
            equal(46, type.getMethod("chapters").invoke(null), "stale generator retains46, so counts cannot detect it");
            equal(true, Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).startsWith(root),
                    "stale classes pass the old under-root origin check");
            refuses(() -> requireClassOrigin(proof, "forge", type), "same root/unchanged source/correct JAR cannot authorize stale classes",
                    "not the actual receipted compiler output");
        }
        Files.write(expected.resolve(member), staleBytes);
        try (var loader = new URLClassLoader(new java.net.URL[]{expected.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> type = Class.forName("fixture.Definitions", true, loader);
            equal(46, type.getMethod("chapters").invoke(null), "in-place stale replacement also retains46");
            refuses(() -> requireClassOrigin(proof, "forge", type), "correct receipted path alone is still insufficient",
                    "loaded class bytes differ");
        }
        Files.write(expected.resolve(member), expectedBytes);
        try (var loader = new URLClassLoader(new java.net.URL[]{expected.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> type = Class.forName("fixture.Definitions", true, loader);
            requireClassOrigin(proof, "forge", type);
            equal("correct", type.getMethod("content").invoke(null), "actual receipted class image is admitted");
        }
        equal(sourceHash, hash(Files.readAllBytes(source)), "fixture source bytes remain unchanged throughout the counterexample");
        equal(jarHash, hash(Files.readAllBytes(selectedJar)), "the selected correct JAR never changed");
        System.out.println("SYNTHETIC_STALE_CLASS_REGRESSION: same46/root/source/JAR; receipt directory and actual class bytes both enforced.");
    }

    private static void jarPayloadContract(Path fixture) throws Exception {
        String prefix = "architectury_inject_grindless_common_" + "1".repeat(32) + "_";
        String suffix = "fixturedevjar/PlatformMethods.class";
        String selectedName = prefix + "a".repeat(64) + suffix;
        String rebuiltName = prefix + "b".repeat(64) + suffix;
        byte[] selectedClass = shimFixture(selectedName, "forge");
        byte[] rebuiltClass = shimFixture(rebuiltName, "forge");
        equal(true, recognizedShim(selectedName, selectedClass) != null, "known structural fixture is recognized");
        Path selected = fixture.resolve("selected-payload.jar");
        Path rebuilt = fixture.resolve("rebuilt-payload.jar");
        Map<String, byte[]> a = new LinkedHashMap<>(Map.of("other.class", new byte[]{1, 2, 3},
                "settings.txt", new byte[]{4, 5}, selectedName, selectedClass));
        Map<String, byte[]> b = new LinkedHashMap<>(a);
        writeJar(selected, a);
        writeJar(rebuilt, b);
        equal(JarComparisonMode.EXACT_ENTRY_PAYLOADS, compareJarPayloads(rebuilt, selected).mode(), "exact path remains exact");
        b.remove(selectedName);
        b.put(rebuiltName, rebuiltClass);
        writeJar(rebuilt, b);
        JarComparison alias = compareJarPayloads(rebuilt, selected);
        equal(JarComparisonMode.ARCHITECTURY_SELF_UTF8_ALIAS, alias.mode(), "only the opaque self hash may vary");
        equal(selectedName, alias.selectedEntry(), "comparison preserves selected entry evidence");
        equal(rebuiltName, alias.rebuiltEntry(), "comparison preserves rebuilt entry evidence");
        int end = 13 + ((rebuiltClass[11] & 255) << 8) + (rebuiltClass[12] & 255);
        int checked = 0;
        for (int index = 0; index < rebuiltClass.length; index++) {
            if (index >= 11 && index < end) { continue; }
            byte[] mutant = rebuiltClass.clone();
            mutant[index] ^= 1;
            equal(null, recognizedShim(rebuiltName, mutant), "every non-self byte remains sealed " + index);
            checked++;
        }
        equal(139, checked, "all header/CP/method/Code/attribute bytes are covered");
        byte[] nullBody = rebuiltClass.clone();
        nullBody[nullBody.length - 9] = 1;
        nullBody[nullBody.length - 8] = 0;
        b.put(rebuiltName, nullBody);
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "aconst_null/nop/areturn cannot borrow the exception");
        b.put(rebuiltName, shimFixture(rebuiltName, "other"));
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "changed return constant is not an alias");
        Map<String, byte[]> changedBoth = new LinkedHashMap<>(a);
        changedBoth.put(selectedName, shimFixture(selectedName, "other"));
        writeJar(selected, changedBoth);
        refuses(() -> compareJarPayloads(rebuilt, selected), "even matching altered bodies are not the recognized template");
        writeJar(selected, a);
        b.put(rebuiltName, rebuiltClass);
        b.put("settings.txt", new byte[]{9});
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "another resource must remain byte-identical");
        b.put("settings.txt", a.get("settings.txt"));
        b.put("other.class", new byte[]{9});
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "another class must remain byte-identical");
        b.put("other.class", a.get("other.class"));
        b.remove(rebuiltName);
        b.put(rebuiltName.toUpperCase(java.util.Locale.ROOT), rebuiltClass);
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "case-only entry rename is not this_class aliasing");
        b.remove(rebuiltName.toUpperCase(java.util.Locale.ROOT));
        b.put(rebuiltName, selectedClass);
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "entry/internal name mismatch refuses");
        b.put(rebuiltName, rebuiltClass);
        for (String changed : List.of(prefix.replace("1".repeat(32), "2".repeat(32)) + "b".repeat(64) + suffix,
                prefix + "b".repeat(64) + "otherdevjar/PlatformMethods.class",
                rebuiltName.replace("grindless_common", "foreign_common"))) {
            b.remove(rebuiltName);
            b.put(changed, shimFixture(changed, "forge"));
            writeJar(rebuilt, b);
            refuses(() -> compareJarPayloads(rebuilt, selected), "module/hash32/suffix identity cannot vary");
            b.remove(changed);
            b.put(rebuiltName, rebuiltClass);
        }
        String extra = prefix + "c".repeat(64) + suffix;
        a.put(extra, shimFixture(extra, "forge"));
        b.put(extra, a.get(extra));
        writeJar(selected, a);
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "an additional shim refuses even when its bytes match");
        b.put("unexpected.txt", new byte[]{0});
        writeJar(rebuilt, b);
        refuses(() -> compareJarPayloads(rebuilt, selected), "additional entries are never ignored");
        System.out.println("SYNTHETIC_SHIM_CONTRACT: exact path, one sealed self alias and body/header/identity/resource refusals.");
    }

    private static void writeJar(Path path, Map<String, byte[]> entries) throws IOException {
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    /** Small hand-authored classfile fixture, not imported code or a historical reference. */
    private static byte[] shimFixture(String entry, String target) throws IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.DataOutputStream(bytes)) {
            out.writeInt(0xCAFEBABE); out.writeShort(0); out.writeShort(52); out.writeShort(10);
            out.writeByte(1); out.writeUTF(entry.substring(0, entry.length() - 6));
            out.writeByte(7); out.writeShort(1);
            out.writeByte(1); out.writeUTF("java/lang/Object");
            out.writeByte(7); out.writeShort(3);
            out.writeByte(1); out.writeUTF("getCurrentTarget");
            out.writeByte(1); out.writeUTF("()Ljava/lang/String;");
            out.writeByte(1); out.writeUTF(target);
            out.writeByte(8); out.writeShort(7);
            out.writeByte(1); out.writeUTF("Code");
            out.writeShort(0x0011); out.writeShort(2); out.writeShort(4);
            out.writeShort(0); out.writeShort(0); out.writeShort(1);
            out.writeShort(0x0009); out.writeShort(5); out.writeShort(6); out.writeShort(1);
            out.writeShort(9); out.writeInt(15);
            out.writeShort(1); out.writeShort(0); out.writeInt(3);
            out.writeByte(0x12); out.writeByte(8); out.writeByte(0xB0);
            out.writeShort(0); out.writeShort(0); out.writeShort(0);
        }
        return bytes.toByteArray();
    }

    private static Map<Path, String> snapshot(Path root) throws IOException {
        Map<Path, String> result = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                result.put(root.relativize(path), hash(Files.readAllBytes(path)) + ":" + Files.getLastModifiedTime(path).toMillis());
            }
        }
        return result;
    }

    private static void equal(Object expected, Object actual, String label) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
        assertions++;
    }

    private static void refuses(Checked body, String label) throws Exception {
        refuses(body, label, null);
    }

    private static void refuses(Checked body, String label, String message) throws Exception {
        try {
            body.run();
        } catch (IOException expected) {
            if (message != null && !expected.getMessage().contains(message)) {
                throw new AssertionError(label + ": wrong refusal: " + expected.getMessage(), expected);
            }
            assertions++;
            return;
        }
        throw new AssertionError("Expected refusal: " + label);
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }
}
