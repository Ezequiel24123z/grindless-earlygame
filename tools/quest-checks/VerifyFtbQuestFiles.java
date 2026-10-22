package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.JsonObject;
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
import java.util.Map;
import java.util.Set;

import static io.github.ezequiel24123z.grindless.forge.quest.FtbQuestDefinitionPublisher.*;

/** Real native-parser/filesystem contracts, not a real dd0 export or native team/world execution. */
public final class VerifyFtbQuestFiles {
    private static final Set<String> ADDED = Set.of("first_grid", "forming_dies", "copper_coil", "flux_cell");
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
        Map<String, CompoundTag> current = FtbQuestDefinitions.chapters();
        Map<String, FtbQuestFiles.Chapter> encodedCurrent = FtbQuestFiles.generated(current);
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
        JsonObject reference = definitionSet(previous, FtbQuestUpdatePlanner.LEGACY_SOURCE,
                FtbQuestUpdatePlanner.LEGACY_SOURCE, FtbQuestUpdatePlanner.LEGACY_JAR, "f".repeat(64));
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
        refuses(() -> FtbQuestFiles.ensure(config, current), "startup preserves its fail-closed46-to50 boundary");
        equal(before, snapshot(instance), "startup missing-ID refusal never changes either chapter");
        JsonObject plan = FtbQuestUpdatePlanner.prepare(instance, encode(reference), destination);
        equal(before, snapshot(instance), "whole native preflight writes no target byte");
        equal(2, plan.getAsJsonArray("files").size(), "only the two known definition filenames are replacements");
        equal(50, plan.getAsJsonObject("receipt").get("questCount").getAsInt(), "new receipt identifies50 definitions");
        for (var element : plan.getAsJsonArray("files")) {
            JsonObject replacement = element.getAsJsonObject();
            String name = replacement.get("fileName").getAsString();
            equal(hash(Files.readAllBytes(chapters.resolve(name))), replacement.get("beforeSha256").getAsString(),
                    "before hashes bind actual old bytes " + name);
            equal(hash(encodedCurrent.get(name).contents()), replacement.get("afterSha256").getAsString(),
                    "after hashes bind the append-only publisher's actual bytes " + name);
        }
        equal(hash(encode(reference)), plan.get("previousDefinitionsSha256").getAsString(), "plan binds the selected reference artifact");

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
        JsonObject changedTask = definitionSet(changedContract, FtbQuestUpdatePlanner.LEGACY_SOURCE,
                FtbQuestUpdatePlanner.LEGACY_SOURCE, FtbQuestUpdatePlanner.LEGACY_JAR, "f".repeat(64));
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
        JsonObject changedReward = definitionSet(changedContract, FtbQuestUpdatePlanner.LEGACY_SOURCE,
                FtbQuestUpdatePlanner.LEGACY_SOURCE, FtbQuestUpdatePlanner.LEGACY_JAR, "f".repeat(64));
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
        equal("SYNTHETIC OPAQUE PAID/TEAM STATE: NEVER PARSE OR REWRITE", Files.readString(team), "native progress and paid history stay opaque and unchanged");

        Path compatibleConfig = fixture.resolve("compatible").resolve("config");
        FtbQuestFiles.ensure(compatibleConfig, current);
        Path compatibleT1 = FtbQuestFiles.chapterDirectory(compatibleConfig).resolve("grindless_t1.snbt");
        CompoundTag compatible = current.get("grindless_t1.snbt").copy();
        compatible.putString("title", "USER-COMPATIBLE COMPLETE50 CHAPTER");
        Files.writeString(compatibleT1, compatible + "\n");
        byte[] compatibleBytes = Files.readAllBytes(compatibleT1);
        FtbQuestFiles.ensure(compatibleConfig, current);
        equal(true, Arrays.equals(compatibleBytes, Files.readAllBytes(compatibleT1)), "ensure remains identity-complete, never unconditional exact-byte management");
        System.out.println("FTB_MANAGED_FILE_CONTRACT_PASS assertions=" + assertions + " fixture=" + fixture.toAbsolutePath());
        System.out.println("SYNTHETIC_REFERENCE_ONLY: this is not a genuine dd0 publication, registered native execution, paid-receipt reopen or owner-world pass.");
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
