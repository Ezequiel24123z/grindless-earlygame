package io.github.ezequiel24123z.grindless.forge.quest;

import dev.ftb.mods.ftblibrary.snbt.SNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Append-only chapter installation before the native loader; loaded semantics are checked separately. */
public final class FtbQuestFiles {

    private static final List<String> FILENAMES = List.of("grindless_t0.snbt", "grindless_t1.snbt");
    private static final List<String> OBJECT_LISTS = List.of("quests", "tasks", "rewards");
    private static final Pattern NATIVE_ID = Pattern.compile("[0-7][0-9a-fA-F]{15}");
    private static final int MAX_CHAPTER_BYTES = 1024 * 1024;

    private FtbQuestFiles() {
    }

    public static void ensure(Path config) throws IOException {
        ensure(config, FtbQuestDefinitions.chapters());
    }

    /**
     * Installs only absent owned filenames. Existing files are parsed and checked for identity
     * completeness, not compared byte-for-byte with generated defaults.
     */
    public static void ensure(Path config, Map<String, CompoundTag> documents) throws IOException {
        Map<String, Chapter> generated = generated(documents);
        Map<String, String> owners = owners(generated);
        Path directory = chapterDirectory(config);
        List<Path> ancestors = ancestors(directory);
        checkDirectories(ancestors);
        refusePendingUpdate(config);
        Set<String> present = preflight(directory, generated, owners);

        // Complete the whole preflight before creating even directories or temporary files.
        createDirectories(ancestors);
        for (Map.Entry<String, Chapter> entry : generated.entrySet()) {
            if (!present.contains(entry.getKey())) {
                install(directory.resolve(entry.getKey()), entry.getValue().contents(), ancestors);
            }
        }
    }

    static Map<String, Chapter> generated(Map<String, CompoundTag> documents) throws IOException {
        if (documents == null || !documents.keySet().equals(Set.copyOf(FILENAMES))) {
            throw new IOException("Expected exactly the two Grindless native chapter filenames");
        }
        Map<String, Chapter> generated = new LinkedHashMap<>();
        for (String filename : FILENAMES) {
            CompoundTag source = documents.get(filename);
            if (source == null) {
                throw new IOException("Missing generated native chapter: " + filename);
            }
            CompoundTag document = source.copy();
            requireObjects(document, document, filename);
            Map<String, Integer> ids = countObjectIds(document);
            for (Map.Entry<String, Integer> entry : ids.entrySet()) {
                if (entry.getValue() != 1) {
                    throw new IOException("Duplicate generated native quest object id: " + entry.getKey());
                }
            }
            byte[] contents = (document + "\n").getBytes(StandardCharsets.UTF_8);
            if (contents.length > MAX_CHAPTER_BYTES) {
                throw new IOException("Generated native chapter exceeds size limit: " + filename);
            }
            generated.put(filename, new Chapter(document, contents, Set.copyOf(ids.keySet())));
        }
        owners(generated);
        return generated;
    }

    private static Map<String, String> owners(Map<String, Chapter> generated) throws IOException {
        Map<String, String> owners = new LinkedHashMap<>();
        for (var chapter : generated.entrySet()) {
            for (String id : chapter.getValue().ids()) {
                if (owners.putIfAbsent(id, chapter.getKey()) != null) {
                    throw new IOException("Duplicate generated native quest object id: " + id);
                }
            }
        }
        return owners;
    }

    static Path chapterDirectory(Path config) {
        return config.toAbsolutePath().resolve("ftbquests").resolve("quests").resolve("chapters");
    }

    static List<Path> ancestors(Path directory) {
        List<Path> ancestors = new ArrayList<>();
        for (Path path = directory.toAbsolutePath(); path != null; path = path.getParent()) {
            ancestors.add(path);
        }
        Collections.reverse(ancestors);
        return ancestors;
    }

    static void refusePendingUpdate(Path config) throws IOException {
        Path pending = config.toAbsolutePath().resolve("ftbquests").resolve(".grindless-definition-update.json");
        if (attributes(pending) != null) {
            throw new IOException("Incomplete managed native chapter update; recover its original plan before startup");
        }
    }

    /** Read-only native preflight. The stopped-target tool separately rechecks every byte before writing. */
    static Map<String, byte[]> managedSnapshot(Path config, Map<String, CompoundTag> previous,
                                               Map<String, CompoundTag> current) throws IOException {
        Path directory = chapterDirectory(config);
        checkDirectories(ancestors(directory));
        refusePendingUpdate(config);
        Map<String, byte[]> before = snapshot(directory);
        Map<String, Chapter> expected = generated(previous);
        Set<String> present = preflight(directory, expected, owners(generated(current)));
        if (!present.equals(Set.copyOf(FILENAMES))) {
            throw new IOException("Managed native chapter update requires both existing owned definitions");
        }
        Map<String, byte[]> after = snapshot(directory);
        if (!before.keySet().equals(after.keySet())) {
            throw new IOException("Native chapter inventory drifted during semantic preflight");
        }
        for (String name : before.keySet()) {
            if (!Arrays.equals(before.get(name), after.get(name))) {
                throw new IOException("Native chapter bytes drifted during semantic preflight: " + name);
            }
        }
        return Collections.unmodifiableMap(after);
    }

    private static Map<String, byte[]> snapshot(Path directory) throws IOException {
        Map<String, byte[]> snapshot = new java.util.TreeMap<>();
        long totalBytes = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path path : files) {
                String filename = path.getFileName().toString();
                if (!filename.toLowerCase(Locale.ROOT).endsWith(".snbt")) {
                    continue;
                }
                if (!filename.matches("[A-Za-z0-9][A-Za-z0-9._+-]*\\.snbt")
                        || snapshot.size() >= 1000) {
                    throw new IOException("Unsafe or excessive native chapter inventory");
                }
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.isOther()
                        || attributes.size() <= 0 || attributes.size() > MAX_CHAPTER_BYTES) {
                    throw new IOException("Native chapter must be a bounded regular non-symlink file: " + path);
                }
                byte[] bytes;
                try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                    bytes = input.readNBytes(MAX_CHAPTER_BYTES + 1);
                }
                if (bytes.length != attributes.size()) {
                    throw new IOException("Native chapter size drifted during preflight: " + path);
                }
                totalBytes += bytes.length;
                if (totalBytes > 16 * 1024 * 1024) {
                    throw new IOException("Managed native chapter inventory exceeds its aggregate byte bound");
                }
                snapshot.put(filename, bytes);
            }
        }
        return snapshot;
    }

    private static Set<String> preflight(Path directory, Map<String, Chapter> generated,
                                         Map<String, String> owners) throws IOException {
        Set<String> present = new LinkedHashSet<>();
        if (attributes(directory) == null) {
            return present;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                String filename = file.getFileName().toString();
                if (!filename.toLowerCase(Locale.ROOT).endsWith(".snbt")) {
                    continue;
                }
                CompoundTag document = readChapter(file);
                Map<String, Integer> ids = countObjectIds(document);
                for (Map.Entry<String, Integer> entry : ids.entrySet()) {
                    String owner = owners.get(entry.getKey());
                    if (owner == null) {
                        continue;
                    }
                    if (!owner.equals(filename)) {
                        throw new IOException("Native quest object id " + entry.getKey()
                                + " belongs to " + owner + ", not " + file);
                    }
                    if (entry.getValue() != 1) {
                        throw new IOException("Duplicate owned native quest object id "
                                + entry.getKey() + " in " + file);
                    }
                }
                Chapter expected = generated.get(filename);
                if (expected != null) {
                    if (!ids.keySet().containsAll(expected.ids())) {
                        throw new IOException("Incomplete owned native chapter was preserved: " + file);
                    }
                    requireObjects(expected.document(), document, file.toString());
                    present.add(filename);
                }
            }
        }
        return present;
    }

    static CompoundTag readChapter(Path file) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (attributes.isSymbolicLink() || attributes.isOther() || !attributes.isRegularFile()) {
            throw new IOException("Native chapter must be a regular non-symlink file: " + file);
        }
        if (attributes.size() > MAX_CHAPTER_BYTES) {
            throw new IOException("Native chapter exceeds size limit: " + file);
        }
        CompoundTag document;
        try {
            document = SNBT.read(file);
        } catch (RuntimeException exception) {
            throw new IOException("Cannot parse native chapter: " + file, exception);
        }
        if (document == null) {
            throw new IOException("Cannot parse native chapter: " + file);
        }
        return document;
    }

    /** Checks object identities and their lists, allowing omitted empty defaults and reordered lists. */
    private static void requireObjects(CompoundTag expected, CompoundTag actual, String filename)
            throws IOException {
        String expectedId = nativeId(expected.get("id"));
        if (expectedId == null || !expectedId.equals(nativeId(actual.get("id")))) {
            throw new IOException("Missing or misplaced native object id in " + filename);
        }
        for (String key : OBJECT_LISTS) {
            if (!(expected.get(key) instanceof ListTag required) || required.isEmpty()) {
                continue;
            }
            if (!(actual.get(key) instanceof ListTag children)) {
                throw new IOException("Missing native object list " + key + " in " + filename);
            }
            Map<String, CompoundTag> byId = new LinkedHashMap<>();
            for (Tag child : children) {
                if (!(child instanceof CompoundTag object)) {
                    throw new IOException("Malformed native object list " + key + " in " + filename);
                }
                String id = nativeId(object.get("id"));
                if (id == null || byId.putIfAbsent(id, object) != null) {
                    throw new IOException("Missing or duplicate native object id in " + filename);
                }
            }
            for (Tag child : required) {
                if (!(child instanceof CompoundTag object)) {
                    throw new IOException("Malformed generated native object list " + key + " in " + filename);
                }
                CompoundTag found = byId.get(nativeId(object.get("id")));
                if (found == null) {
                    throw new IOException("Incomplete native object list " + key + " in " + filename);
                }
                requireObjects(object, found, filename);
            }
        }
    }

    /** Counts only native object positions, never ItemStack ids or arbitrary item/custom NBT. */
    private static Map<String, Integer> countObjectIds(CompoundTag document) {
        Map<String, Integer> ids = new LinkedHashMap<>();
        countObjectId(document, ids);
        if (document.get("quests") instanceof ListTag quests) {
            for (Tag entry : quests) {
                if (!(entry instanceof CompoundTag quest)) {
                    continue;
                }
                countObjectId(quest, ids);
                for (String key : List.of("tasks", "rewards")) {
                    if (quest.get(key) instanceof ListTag objects) {
                        for (Tag child : objects) {
                            if (child instanceof CompoundTag object) {
                                countObjectId(object, ids);
                            }
                        }
                    }
                }
            }
        }
        return ids;
    }

    private static void countObjectId(CompoundTag object, Map<String, Integer> ids) {
        String id = nativeId(object.get("id"));
        if (id != null) {
            ids.merge(id, 1, Integer::sum);
        }
    }

    private static String nativeId(Tag tag) {
        if (!(tag instanceof StringTag string)) {
            return null;
        }
        String value = string.getAsString();
        if (!NATIVE_ID.matcher(value).matches() || value.equals("0000000000000000")) {
            return null;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static void install(Path destination, byte[] contents, List<Path> ancestors) throws IOException {
        checkDirectories(ancestors);
        if (attributes(destination) != null) {
            throw new FileAlreadyExistsException(destination.toString());
        }
        Path temporary = Files.createTempFile(destination.getParent(), ".grindless-chapter-", ".tmp");
        try {
            Files.write(temporary, contents, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            checkDirectories(ancestors);
            // No options: ATOMIC_MOVE may replace an existing target even without REPLACE_EXISTING.
            // Publish a complete same-directory temporary file with the non-replacing move contract.
            Files.move(temporary, destination);
        } finally {
            // Only this attempt's temporary file is removed; installed chapters and backups stay intact.
            Files.deleteIfExists(temporary);
        }
    }

    static void checkDirectories(List<Path> ancestors) throws IOException {
        for (Path directory : ancestors) {
            BasicFileAttributes attributes = attributes(directory);
            if (attributes != null && (attributes.isSymbolicLink() || attributes.isOther() || !attributes.isDirectory())) {
                throw new IOException("Native quest ancestor must be a non-symlink directory: " + directory);
            }
        }
    }

    private static void createDirectories(List<Path> ancestors) throws IOException {
        for (Path directory : ancestors) {
            if (attributes(directory) == null) {
                try {
                    Files.createDirectory(directory);
                } catch (FileAlreadyExistsException exception) {
                    // Another initializer may have created it; validate rather than following it.
                }
            }
            BasicFileAttributes attributes = attributes(directory);
            if (attributes == null || attributes.isSymbolicLink() || attributes.isOther() || !attributes.isDirectory()) {
                throw new IOException("Native quest ancestor must be a non-symlink directory: " + directory);
            }
        }
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException exception) {
            return null;
        }
    }

    record Chapter(CompoundTag document, byte[] contents, Set<String> ids) {
    }
}
