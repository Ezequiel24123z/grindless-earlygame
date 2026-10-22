package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Publishes the actual generator, including on an unmodified pinned older checkout.
 * Deliberately depends only on the generator's historical public API, not the new update planner.
 */
public final class FtbQuestDefinitionPublisher {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    static final List<String> FILENAMES = List.of("grindless_t0.snbt", "grindless_t1.snbt");

    private FtbQuestDefinitionPublisher() {
    }

    /**
     * Source-file mode supports dd0 without backporting or modifying its source:
     * java -cp PINNED_CLASSES_AND_LIBRARIES FtbQuestDefinitionPublisher.java
     * ROOT SOURCE RUNTIME JAR JAR_SHA256 OUTPUT.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException("Expected ROOT SOURCE RUNTIME JAR JAR_SHA256 OUTPUT");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        verifySource(root, args[1], args[2]);
        Path jar = Path.of(args[3]).toAbsolutePath().normalize();
        byte[] jarBytes = readRegular(jar, 64 * 1024 * 1024);
        if (!hash(jarBytes).equals(args[4])) {
            throw new IOException("Publisher runtime JAR differs from the explicitly verified SHA256");
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        requireClassOrigin(root, FtbQuestDefinitions.class);
        requireClassOrigin(root, io.github.ezequiel24123z.grindless.quest.QuestCatalogue.class);
        Path output = Path.of(args[5]).toAbsolutePath().normalize();
        if (!output.startsWith(root.resolve("build")) && !output.startsWith(root.resolve("forge").resolve("build"))) {
            throw new IOException("Definition publication belongs only in the pinned checkout's build output");
        }
        JsonObject published = definitionSet(FtbQuestDefinitions.chapters(), args[1], args[2], args[4]);
        writeNew(output, encode(published));
        System.out.println("FTB_DEFINITIONS_PUBLISHED source=" + args[1] + " quests="
                + published.get("questCount").getAsInt() + " sha256=" + hash(readRegular(output, 4 * 1024 * 1024)));
    }

    static JsonObject definitionSet(Map<String, CompoundTag> documents, String source, String runtime, String jarHash)
            throws IOException {
        requireRevision(source);
        requireRevision(runtime);
        requireHash(jarHash);
        if (!documents.keySet().equals(Set.copyOf(FILENAMES))) {
            throw new IOException("Publisher requires exactly the two generated native chapters");
        }
        JsonObject result = new JsonObject();
        result.addProperty("schemaVersion", 1);
        result.addProperty("tool", "grindless-ftb-definition-set");
        result.addProperty("sourceRevision", source);
        result.addProperty("runtimeSourceRevision", runtime);
        result.addProperty("grindlessSha256", jarHash);
        int quests = documents.values().stream().mapToInt(document -> document.getList("quests", Tag.TAG_COMPOUND).size()).sum();
        if (quests != 46 && quests != 50) {
            throw new IOException("Unknown generated native definition version: " + quests);
        }
        result.addProperty("questCount", quests);
        JsonArray files = new JsonArray();
        for (String name : FILENAMES) {
            byte[] contents = (documents.get(name) + "\n").getBytes(StandardCharsets.UTF_8);
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

    static void requireClassOrigin(Path root, Class<?> type) throws Exception {
        Path origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        checkPath(origin);
        if (!origin.startsWith(root)) {
            throw new IOException("Publisher generator/catalogue classes do not belong to the pinned source checkout");
        }
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
