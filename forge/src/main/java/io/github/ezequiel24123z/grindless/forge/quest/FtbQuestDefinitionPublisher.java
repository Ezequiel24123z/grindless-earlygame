package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProjection;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * Publishes the actual generator, including on an unmodified pinned older checkout.
 * Deliberately depends only on the generator's historical public API, not the new update planner.
 */
public final class FtbQuestDefinitionPublisher {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    static final List<String> FILENAMES = List.of("grindless_t0.snbt", "grindless_t1.snbt");
    private static final Set<String> CATALOGUE_46 = Set.of(
            "starter_supplies", "quest_book_help", "superflat", "construction", "multitool", "manual_work",
            "crank", "extractor", "quern", "water", "clay_mixer", "grog", "firing", "ceramics",
            "geological_collector", "aggregate", "settling", "water_recovery", "mineral_feed",
            "biocultivator", "biomass", "fibreboard", "biomass_dynamo", "fluid_logistics", "conveyors",
            "workshop_flow", "assembly_bench", "research_terminal", "data_core", "calibrate",
            "calibrated_core", "relay", "thermal", "pylon", "scanner", "terrestrial", "pulverizer",
            "furnace", "press", "assembler", "matrix_line", "glass", "logistics", "kiln", "factory_world",
            "industrial_matrix");
    static final Set<String> ADDED_50 = Set.of("first_grid", "forming_dies", "copper_coil", "flux_cell");
    static final Set<String> ADDED_51 = Set.of("wet_beneficiation");
    static final Set<String> ADDED_52 = Set.of("gas_utilities");
    private static final String SHIM_PREFIX = "architectury_inject_grindless_common_";
    private static final String SHIM_FINGERPRINT = "0a84a9c64d4531a5942f7fab6de29d225f189424344d81b42e4642cb468923d6";
    private static final Pattern SHIM_NAME = Pattern.compile(
            "(" + SHIM_PREFIX + "[0-9a-f]{32}_)([0-9a-f]{64})([A-Za-z0-9]+/PlatformMethods)\\.class");

    private FtbQuestDefinitionPublisher() {
    }

    /**
     * Source-file mode supports dd0 without backporting or modifying its source:
     * java -cp PINNED_CLASSES_AND_LIBRARIES FtbQuestDefinitionPublisher.java
     * ROOT SOURCE RUNTIME JAR JAR_SHA256 COMPILATION COMPILATION_SHA256 OUTPUT.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 8) {
            throw new IllegalArgumentException("Expected ROOT SOURCE RUNTIME JAR JAR_SHA256 COMPILATION COMPILATION_SHA256 OUTPUT");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        verifySource(root, args[1], args[2]);
        Path jar = Path.of(args[3]).toAbsolutePath().normalize();
        byte[] jarBytes = readRegular(jar, 64 * 1024 * 1024);
        if (!hash(jarBytes).equals(args[4])) {
            throw new IOException("Publisher runtime JAR differs from the explicitly verified SHA256");
        }
        CompilationProof proof = verifyCompilation(root, args[1], jar, Path.of(args[5]), args[6]);
        requireClassOrigin(proof, "forge", FtbQuestDefinitions.class);
        requireClassOrigin(proof, "common", io.github.ezequiel24123z.grindless.quest.QuestCatalogue.class);
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path output = Path.of(args[7]).toAbsolutePath().normalize();
        if (!output.startsWith(root.resolve("build")) && !output.startsWith(root.resolve("forge").resolve("build"))) {
            throw new IOException("Definition publication belongs only in the pinned checkout's build output");
        }
        JsonObject published = definitionSet(FtbQuestDefinitions.chapters(), args[1], args[2], args[4], args[6]);
        writeNew(output, encode(published));
        System.out.println("FTB_DEFINITIONS_PUBLISHED source=" + args[1] + " quests="
                + published.get("questCount").getAsInt() + " sha256=" + hash(readRegular(output, 4 * 1024 * 1024))
                + " compilation=" + args[6]);
    }

    static JsonObject definitionSet(Map<String, CompoundTag> documents, String source, String runtime, String jarHash,
                                    String compilationHash)
            throws IOException {
        requireRevision(source);
        requireRevision(runtime);
        requireHash(jarHash);
        requireHash(compilationHash);
        if (!documents.keySet().equals(Set.copyOf(FILENAMES))) {
            throw new IOException("Publisher requires exactly the two generated native chapters");
        }
        JsonObject result = new JsonObject();
        result.addProperty("schemaVersion", 2);
        result.addProperty("tool", "grindless-ftb-definition-set");
        result.addProperty("sourceRevision", source);
        result.addProperty("runtimeSourceRevision", runtime);
        result.addProperty("grindlessSha256", jarHash);
        result.addProperty("compilationSha256", compilationHash);
        int quests = currentQuestCount();
        requireKnownDefinitions(documents, quests);
        if (!documents.equals(FtbQuestDefinitions.chapters())) {
            throw new IOException("Publication requires the complete actual source-bound generator, not a historical subset");
        }
        result.addProperty("questCount", quests);
        JsonArray files = new JsonArray();
        for (String name : FILENAMES) {
            CompoundTag document = documents.get(name).copy();
            byte[] contents = (document + "\n").getBytes(StandardCharsets.UTF_8);
            if (contents.length == 0 || contents.length > 1024 * 1024) {
                throw new IOException("Generated native chapter exceeds its bound");
            }
            JsonObject file = file(name, contents);
            file.addProperty("contentsBase64", Base64.getEncoder().encodeToString(contents));
            files.add(file);
        }
        result.add("files", files);
        return result;
    }

    /** Historical identities validate lineage; they never generate an authentic historical publication. */
    static Set<String> knownLogicalIds(int count) throws IOException {
        Set<String> ids = new HashSet<>(CATALOGUE_46);
        switch (count) {
            case 46 -> { }
            case 50 -> ids.addAll(ADDED_50);
            case 51 -> {
                ids.addAll(ADDED_50);
                ids.addAll(ADDED_51);
            }
            case 52 -> {
                ids.addAll(ADDED_50);
                ids.addAll(ADDED_51);
                ids.addAll(ADDED_52);
            }
            default -> throw new IOException("Unknown native catalogue content version: " + count);
        }
        return Set.copyOf(ids);
    }

    static int currentQuestCount() throws IOException {
        Set<String> actual = new HashSet<>();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            if (!actual.add(task.id())) { throw new IOException("Duplicate source-bound logical quest ID"); }
        }
        int count = QuestCatalogue.tasks().size();
        if (!actual.equals(knownLogicalIds(count))) {
            throw new IOException("Source-bound catalogue does not match a known exact logical identity set");
        }
        return count;
    }

    static void requireKnownDefinitions(Map<String, CompoundTag> documents, int count) throws IOException {
        if (!documents.keySet().equals(Set.copyOf(FILENAMES))) {
            throw new IOException("Known native content requires exactly the two owned chapters");
        }
        Map<String, String> expected = new LinkedHashMap<>();
        for (String logical : knownLogicalIds(count)) {
            expected.put(FtbQuestDefinitions.code(FtbQuestDefinitions.id("quest", logical)), logical);
        }
        Set<String> seen = new HashSet<>();
        for (String filename : FILENAMES) {
            CompoundTag document = documents.get(filename);
            String chapter = filename.equals("grindless_t0.snbt") ? "T0" : "T1";
            if (document == null || !document.getString("id").equals(
                    FtbQuestDefinitions.code(FtbQuestDefinitions.id("chapter", chapter)))
                    || !(document.get("quests") instanceof ListTag quests) || quests.getElementType() != Tag.TAG_COMPOUND) {
                throw new IOException("Unknown native chapter identity or quest list");
            }
            for (Tag value : quests) {
                CompoundTag quest = (CompoundTag) value;
                String logical = expected.get(quest.getString("id"));
                if (logical == null || !seen.add(logical)) {
                    throw new IOException("Unknown, duplicate or replaced native quest identity");
                }
                QuestCatalogue.Task declared = QuestCatalogue.byId(logical);
                if (declared == null || !QuestCatalogue.chapter(declared).name().equals(chapter)
                        || !(quest.get("tags") instanceof ListTag tags) || tags.size() != 1
                        || tags.getElementType() != Tag.TAG_STRING
                        || !tags.getString(0).equals(QuestProjection.id(logical))) {
                    throw new IOException("Native logical tag or chapter placement drifted");
                }
                if (!(quest.get("tasks") instanceof ListTag tasks) || tasks.size() != 1
                        || tasks.getElementType() != Tag.TAG_COMPOUND || !tasks.getCompound(0).getString("id").equals(
                        FtbQuestDefinitions.code(FtbQuestDefinitions.id("task", logical)))) {
                    throw new IOException("Native task identity drifted");
                }
                if (!(quest.get("rewards") instanceof ListTag rewards)
                        || (!rewards.isEmpty() && rewards.getElementType() != Tag.TAG_COMPOUND)
                        || rewards.size() != declared.rewards().size()) {
                    throw new IOException("Native reward identities drifted");
                }
                List<Long> rewardIds = FtbQuestDefinitions.rewardIds(declared);
                for (int index = 0; index < rewardIds.size(); index++) {
                    if (!rewards.getCompound(index).getString("id").equals(FtbQuestDefinitions.code(rewardIds.get(index)))) {
                        throw new IOException("Native reward identity drifted");
                    }
                }
                if ((ADDED_50.contains(logical) || ADDED_51.contains(logical) || ADDED_52.contains(logical))
                        && !rewards.isEmpty()) {
                    throw new IOException("Reviewed catalogue additions must remain reward-free");
                }
            }
        }
        if (!seen.equals(knownLogicalIds(count))) {
            throw new IOException("Missing native quest identities for known content version: " + count);
        }
    }

    static JsonObject file(String name, byte[] contents) {
        JsonObject file = new JsonObject();
        file.addProperty("fileName", name);
        file.addProperty("bytes", contents.length);
        file.addProperty("sha256", hash(contents));
        return file;
    }

    static byte[] encode(JsonObject object) {
        return (JSON.toJson(object) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    static void requireRevision(String value) throws IOException {
        if (value == null || !value.matches("[0-9a-f]{40}")) {
            throw new IOException("A full lowercase source/runtime commit is required");
        }
    }

    static void requireHash(String value) throws IOException {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IOException("A full lowercase SHA256 is required");
        }
    }

    static byte[] readRegular(Path path, int limit) throws IOException {
        checkPath(path);
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.size() > limit || attributes.size() <= 0) {
            throw new IOException("Publisher input must be a bounded regular non-symlink file");
        }
        byte[] bytes;
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(limit + 1);
        }
        if (bytes.length != attributes.size()) {
            throw new IOException("Publisher input size changed while reading");
        }
        return bytes;
    }

    static void checkPath(Path path) throws IOException {
        if (path.toAbsolutePath().getRoot().toString().startsWith("\\\\")) {
            throw new IOException("Publisher paths must be local, not UNC/network shares");
        }
        for (Path current = path.toAbsolutePath(); current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Publisher paths must not traverse symlinks");
            }
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attributes.isOther()) {
                    throw new IOException("Publisher paths must not traverse special/reparse files");
                }
            }
        }
    }

    static void writeNew(Path output, byte[] contents) throws IOException {
        checkPath(output);
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Publisher output already exists; no prior artifact was overwritten");
        }
        Files.createDirectories(output.getParent());
        checkPath(output);
        Path temporary = Files.createTempFile(output.getParent(), ".grindless-ftb-", ".tmp");
        try {
            Files.write(temporary, contents, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            checkPath(output);
            Files.move(temporary, output);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void verifySource(Path root, String source, String runtime) throws IOException {
        requireRevision(source);
        requireRevision(runtime);
        checkPath(root);
        if (!Path.of(git(root, "rev-parse", "--show-toplevel")).toAbsolutePath().normalize().equals(root)
                || !git(root, "rev-parse", "HEAD").equals(source)
                || !git(root, "status", "--porcelain", "--untracked-files=normal").isEmpty()) {
            throw new IOException("Publisher requires the exact clean committed source checkout");
        }
        git(root, "merge-base", "--is-ancestor", runtime, source);
        git(root, "diff", "--exit-code", runtime, source, "--", "common", "forge", "settings.gradle",
                "gradle.properties", "gradle", "gradlew", "gradlew.bat");
        String boundary = "// Application build inputs stay above this boundary";
        String previous = git(root, "show", runtime + ":build.gradle").replace("\r\n", "\n");
        String current = Files.readString(root.resolve("build.gradle")).replace("\r\n", "\n");
        if (!previous.contains(boundary) || !current.contains(boundary)
                || !previous.split(java.util.regex.Pattern.quote(boundary), 2)[0].trim()
                .equals(current.split(java.util.regex.Pattern.quote(boundary), 2)[0].trim())) {
            throw new IOException("Publisher source/runtime application build inputs differ");
        }
    }

    static CompilationProof verifyCompilation(Path root, String source, Path selectedJar, Path receipt,
                                               String receiptHash) throws IOException {
        requireHash(receiptHash);
        byte[] bytes = readRegular(receipt, 8 * 1024 * 1024);
        if (!hash(bytes).equals(receiptHash)) {
            throw new IOException("FTB compilation receipt SHA256 mismatch");
        }
        JsonObject manifest = readObject(bytes);
        requireKeys(manifest, "schemaVersion", "tool", "sourceRevision", "sourceRoot", "outputRoot",
                "toolSha256", "javaVersion", "gradleVersion", "sources", "classes", "runtimeClasspath", "remappedJar");
        Path declaredRoot = Path.of(text(manifest, "sourceRoot")).toAbsolutePath().normalize();
        Path output = Path.of(text(manifest, "outputRoot")).toAbsolutePath().normalize();
        if (!manifest.get("schemaVersion").isJsonPrimitive() || !manifest.get("schemaVersion").getAsJsonPrimitive().isNumber()
                || !manifest.get("schemaVersion").getAsString().equals("1")
                || !text(manifest, "tool").equals("grindless-ftb-compilation")
                || !text(manifest, "sourceRevision").equals(source) || !declaredRoot.equals(root)
                || !output.getParent().equals(root.resolve("build"))
                || !output.getFileName().toString().matches("ftb-compilation-[0-9a-f]{32}")
                || !receipt.toAbsolutePath().normalize().equals(output.resolve("compilation.json"))) {
            throw new IOException("Unknown or incorrectly bound FTB compilation receipt");
        }
        checkPath(output);
        requireHash(text(manifest, "toolSha256"));
        if (!fileHash(output.resolve("compiler.init.gradle")).equals(text(manifest, "toolSha256"))) {
            throw new IOException("FTB compiler recipe differs from the actual recorded build");
        }
        Set<String> expectedSources = new HashSet<>(List.of(git(root, "ls-files", "-z", "--", "build.gradle",
                "settings.gradle", "gradle.properties", "gradle", "gradlew", "gradlew.bat", "common", "forge").split("\u0000")));
        expectedSources.remove("");
        Map<String, String> sources = imageManifest(manifest.getAsJsonArray("sources"));
        if (!sources.keySet().equals(expectedSources)) {
            throw new IOException("FTB compilation source inventory differs from the pinned checkout");
        }
        for (var entry : sources.entrySet()) {
            Path path = safeMember(root, entry.getKey());
            if (!fileHash(path).equals(entry.getValue())) {
                throw new IOException("FTB source bytes differ from the actual compilation receipt");
            }
        }
        if (!manifest.get("classes").isJsonArray() || manifest.getAsJsonArray("classes").size() != 2) {
            throw new IOException("FTB compilation requires both actual compiler outputs");
        }
        Map<String, Path> directories = new LinkedHashMap<>();
        Map<String, Map<String, String>> images = new LinkedHashMap<>();
        for (JsonElement element : manifest.getAsJsonArray("classes")) {
            JsonObject project = element.getAsJsonObject();
            requireKeys(project, "project", "path", "files", "inputs");
            String name = text(project, "project");
            Path directory = Path.of(text(project, "path")).toAbsolutePath().normalize();
            if (!Set.of("common", "forge").contains(name) || directories.putIfAbsent(name, directory) != null
                    || !directory.equals(output.resolve(name).resolve("classes").resolve("java").resolve("main"))) {
                throw new IOException("FTB compiler output is not the fresh declared class directory");
            }
            Map<String, String> files = imageManifest(project.getAsJsonArray("files"));
            if (files.keySet().stream().noneMatch(path -> path.endsWith(".class"))
                    || !files.equals(directoryImage(directory))) {
                throw new IOException("Stale or modified classes differ from the actual FTB compilation receipt");
            }
            images.put(name, files);
            JsonObject inputs = project.getAsJsonObject("inputs");
            requireKeys(inputs, "sources", "classpath", "release", "compilerArgs");
            forbidHistoricalOutputs(root, verifyClasspath(inputs.getAsJsonArray("sources")));
            forbidHistoricalOutputs(root, verifyClasspath(inputs.getAsJsonArray("classpath")));
        }
        Set<Path> runtimePaths = verifyClasspath(manifest.getAsJsonArray("runtimeClasspath"));
        forbidHistoricalOutputs(root, runtimePaths);
        if (!runtimePaths.containsAll(directories.values())) {
            throw new IOException("FTB runtime classpath omits the verified compiler output");
        }
        JsonObject compiledJar = manifest.getAsJsonObject("remappedJar");
        requireKeys(compiledJar, "path", "sha256");
        Path builtJar = Path.of(text(compiledJar, "path")).toAbsolutePath().normalize();
        if (!builtJar.startsWith(output.resolve("forge")) || !fileHash(builtJar).equals(text(compiledJar, "sha256"))) {
            throw new IOException("Selected runtime JAR payload differs from the actual pinned compilation/remap output");
        }
        JarComparison comparison = compareJarPayloads(builtJar, selectedJar);
        System.out.println("FTB_JAR_COMPARISON mode=" + comparison.mode()
                + " selected_sha256=" + fileHash(selectedJar) + " rebuilt_sha256=" + text(compiledJar, "sha256")
                + " compilation_sha256=" + receiptHash
                + (comparison.mode() == JarComparisonMode.ARCHITECTURY_SELF_UTF8_ALIAS
                ? " selected_entry=" + comparison.selectedEntry() + " rebuilt_entry=" + comparison.rebuiltEntry()
                + " canonical_sha256=" + SHIM_FINGERPRINT : ""));
        return new CompilationProof(directories, images);
    }

    static JarComparison compareJarPayloads(Path rebuilt, Path selected) throws IOException {
        Map<String, String> rebuiltImage = jarImage(rebuilt);
        Map<String, String> selectedImage = jarImage(selected);
        if (rebuiltImage.equals(selectedImage)) {
            return new JarComparison(JarComparisonMode.EXACT_ENTRY_PAYLOADS, null, null);
        }
        if (rebuiltImage.size() != selectedImage.size()) { throw jarMismatch(); }
        for (var entry : selectedImage.entrySet()) {
            String other = rebuiltImage.get(entry.getKey());
            if (other != null && !other.equals(entry.getValue())) { throw jarMismatch(); }
        }
        Set<String> selectedOnly = new HashSet<>(selectedImage.keySet());
        selectedOnly.removeAll(rebuiltImage.keySet());
        Set<String> rebuiltOnly = new HashSet<>(rebuiltImage.keySet());
        rebuiltOnly.removeAll(selectedImage.keySet());
        if (selectedOnly.size() != 1 || rebuiltOnly.size() != 1
                || shimCount(selectedImage) != 1 || shimCount(rebuiltImage) != 1) {
            throw jarMismatch();
        }
        String selectedEntry = selectedOnly.iterator().next();
        String rebuiltEntry = rebuiltOnly.iterator().next();
        byte[] a = jarMember(selected, selectedEntry);
        byte[] b = jarMember(rebuilt, rebuiltEntry);
        if (!hash(a).equals(selectedImage.get(selectedEntry)) || !hash(b).equals(rebuiltImage.get(rebuiltEntry))) {
            throw new IOException("JAR changed during the narrow Architectury comparison");
        }
        ShimIdentity left = recognizedShim(selectedEntry, a);
        ShimIdentity right = recognizedShim(rebuiltEntry, b);
        if (left == null || right == null || !left.identity().equals(right.identity())
                || left.jarId().equals(right.jarId())) {
            throw jarMismatch();
        }
        return new JarComparison(JarComparisonMode.ARCHITECTURY_SELF_UTF8_ALIAS, selectedEntry, rebuiltEntry);
    }

    private static long shimCount(Map<String, String> image) {
        return image.keySet().stream().filter(name -> name.startsWith(SHIM_PREFIX)
                && name.endsWith("/PlatformMethods.class")).count();
    }

    private static byte[] jarMember(Path jar, String name) throws IOException {
        checkPath(jar);
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = zip.getEntry(name);
            if (entry == null || entry.isDirectory() || entry.getSize() > 1024) { throw jarMismatch(); }
            try (var input = zip.getInputStream(entry)) {
                byte[] bytes = input.readNBytes(1025);
                if (bytes.length > 1024) { throw jarMismatch(); }
                return bytes;
            }
        }
    }

    static ShimIdentity recognizedShim(String entry, byte[] bytes) {
        var name = SHIM_NAME.matcher(entry);
        if (!name.matches() || bytes.length < 13 || bytes.length > 1024) { return null; }
        var header = java.nio.ByteBuffer.wrap(bytes);
        if (header.getInt(0) != 0xCAFEBABE || header.getShort(4) != 0 || header.getShort(6) != 52
                || header.getShort(8) != 10 || bytes[10] != 1) {
            return null;
        }
        int selfLength = Short.toUnsignedInt(header.getShort(11));
        int end = 13 + selfLength;
        if (end > bytes.length || bytes.length - end != 128 || header.getShort(end + 85) != 0x0011) { return null; }
        String self = new String(bytes, 13, selfLength, StandardCharsets.US_ASCII);
        if (!entry.equals(self + ".class")) { return null; }
        // Only CP#1's this_class UTF8 length/value varies. Every other byte of this one
        // observed getCurrentTarget()->"forge" template, including all Code bytes, is sealed.
        byte[] shape = new byte[145];
        System.arraycopy(bytes, 0, shape, 0, 11);
        System.arraycopy(new byte[]{0, 4, 83, 69, 76, 70}, 0, shape, 11, 6);
        System.arraycopy(bytes, end, shape, 17, 128);
        if (!hash(shape).equals(SHIM_FINGERPRINT)) { return null; }
        return new ShimIdentity(name.group(1) + name.group(3), name.group(2));
    }

    private static IOException jarMismatch() {
        return new IOException("Selected runtime JAR payload differs from the actual pinned compilation/remap output");
    }

    static void requireClassOrigin(CompilationProof proof, String project, Class<?> type) throws Exception {
        Path origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        checkPath(origin);
        if (!origin.equals(proof.directories().get(project))) {
            throw new IOException("Publisher classes are not the actual receipted compiler output");
        }
        String relative = type.getName().replace('.', '/') + ".class";
        String expected = proof.images().get(project).get(relative);
        try (var input = type.getResourceAsStream("/" + relative)) {
            if (input == null || expected == null || !hash(input.readNBytes(8 * 1024 * 1024)).equals(expected)
                    || !fileHash(origin.resolve(relative)).equals(expected)) {
                throw new IOException("Actually loaded class bytes differ from the FTB compilation receipt");
            }
        }
    }

    private static Set<Path> verifyClasspath(JsonArray entries) throws IOException {
        if (entries == null || entries.size() > 2048) {
            throw new IOException("Invalid FTB compiler/runtime input inventory");
        }
        Set<Path> seen = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String kind = text(entry, "kind");
            Path path = Path.of(text(entry, "path")).toAbsolutePath().normalize();
            if (!seen.add(path)) { throw new IOException("Duplicated FTB compilation classpath entry"); }
            if (kind.equals("file")) {
                requireKeys(entry, "path", "kind", "sha256");
                if (!fileHash(path).equals(text(entry, "sha256"))) {
                    throw new IOException("FTB compiler/runtime file drifted after compilation");
                }
            } else if (kind.equals("directory")) {
                requireKeys(entry, "path", "kind", "files");
                if (!directoryImage(path).equals(imageManifest(entry.getAsJsonArray("files")))) {
                    throw new IOException("FTB compiler/runtime directory drifted after compilation");
                }
            } else if (kind.equals("missing")) {
                requireKeys(entry, "path", "kind");
                checkPath(path);
                if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("An absent FTB classpath entry appeared after compilation");
                }
            } else {
                throw new IOException("Unknown FTB compilation input kind");
            }
        }
        return seen;
    }

    private static void forbidHistoricalOutputs(Path root, Set<Path> paths) throws IOException {
        for (Path path : paths) {
            if (path.startsWith(root.resolve("common").resolve("build"))
                    || path.startsWith(root.resolve("forge").resolve("build"))) {
                throw new IOException("Historical build outputs cannot supply FTB publication classes or compiler inputs");
            }
        }
    }

    private static Map<String, String> imageManifest(JsonArray files) throws IOException {
        if (files == null || files.size() > 20000) { throw new IOException("Invalid FTB compilation image manifest"); }
        Map<String, String> result = new TreeMap<>();
        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            requireKeys(file, "path", "sha256");
            String name = text(file, "path");
            String digest = text(file, "sha256");
            requireHash(digest);
            if (result.putIfAbsent(name, digest) != null) { throw new IOException("Duplicate FTB image entry"); }
        }
        return result;
    }

    private static Map<String, String> directoryImage(Path directory) throws IOException {
        checkPath(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Missing actual FTB compilation output directory");
        }
        Map<String, String> result = new TreeMap<>();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.toList()) {
                checkPath(path);
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    if (result.size() >= 20000) { throw new IOException("FTB image inventory exceeds its bound"); }
                    result.put(directory.relativize(path).toString().replace('\\', '/'), fileHash(path));
                }
            }
        }
        return result;
    }

    private static String fileHash(Path path) throws IOException {
        checkPath(path);
        var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.size() > 1024L * 1024 * 1024) {
            throw new IOException("FTB compilation input must be a bounded regular file");
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[81920];
                long total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > attributes.size()) { throw new IOException("FTB compilation input size drifted"); }
                    digest.update(buffer, 0, count);
                }
                if (total != attributes.size()) { throw new IOException("FTB compilation input size drifted"); }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static Map<String, String> jarImage(Path jar) throws IOException {
        checkPath(jar);
        Map<String, String> result = new TreeMap<>();
        long total = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) { continue; }
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) { bytes = input.readNBytes(16 * 1024 * 1024 + 1); }
                total += bytes.length;
                if (bytes.length > 16 * 1024 * 1024 || total > 64 * 1024 * 1024 || result.size() >= 10000
                        || result.putIfAbsent(entry.getName(), hash(bytes)) != null) {
                    throw new IOException("Malformed or excessive remapped FTB JAR payload");
                }
            }
        }
        if (result.isEmpty()) { throw new IOException("Missing remapped FTB JAR payload"); }
        return result;
    }

    private static Path safeMember(Path root, String name) throws IOException {
        if (name.isEmpty() || name.contains("\\") || name.contains(":")) {
            throw new IOException("Unsafe FTB source image member");
        }
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root) || !root.relativize(path).toString().replace('\\', '/').equals(name)) {
            throw new IOException("Unsafe FTB source image member");
        }
        return path;
    }

    private static void requireKeys(JsonObject object, String... keys) throws IOException {
        if (object == null || !object.keySet().equals(Set.of(keys))) {
            throw new IOException("Unknown or missing FTB compilation fields");
        }
    }

    private static String text(JsonObject object, String name) throws IOException {
        JsonElement value = object == null ? null : object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IOException("Malformed FTB compilation string: " + name);
        }
        return value.getAsString();
    }

    static JsonObject readObject(byte[] bytes) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()))) {
            reader.setLenient(false);
            JsonElement value = json(reader, 0);
            if (!value.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("FTB metadata must be one complete JSON object");
            }
            return value.getAsJsonObject();
        } catch (IllegalStateException | NumberFormatException exception) {
            throw new IOException("Malformed FTB metadata JSON", exception);
        }
    }

    private static JsonElement json(JsonReader reader, int depth) throws IOException {
        if (depth > 40) { throw new IOException("FTB metadata exceeds its nesting bound"); }
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject();
                JsonObject value = new JsonObject();
                Set<String> names = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!names.add(name)) { throw new IOException("Duplicate FTB metadata field"); }
                    value.add(name, json(reader, depth + 1));
                }
                reader.endObject();
                yield value;
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                JsonArray value = new JsonArray();
                while (reader.hasNext()) { value.add(json(reader, depth + 1)); }
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
            default -> throw new IOException("Malformed FTB metadata JSON token");
        };
    }

    record CompilationProof(Map<String, Path> directories, Map<String, Map<String, String>> images) {
    }

    enum JarComparisonMode {
        EXACT_ENTRY_PAYLOADS, ARCHITECTURY_SELF_UTF8_ALIAS
    }

    record JarComparison(JarComparisonMode mode, String selectedEntry, String rebuiltEntry) {
    }

    record ShimIdentity(String identity, String jarId) {
    }

    private static String git(Path root, String... arguments) throws IOException {
        var command = new java.util.ArrayList<>(List.of("git", "--no-pager", "-C", root.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output;
        try (var input = process.getInputStream()) {
            output = input.readNBytes(1024 * 1024 + 1);
        }
        try {
            if (process.waitFor() != 0 || output.length > 1024 * 1024) {
                throw new IOException("Read-only publisher Git verification failed");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Publisher Git verification interrupted", exception);
        }
        return new String(output, StandardCharsets.UTF_8).trim();
    }
}
