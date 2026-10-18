package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Data-only standalone pack binding over Gradle's resolved vanilla and Forge artifacts.
 * Layer enumeration is not priority: append-only overlaps prove set-membership commutativity.
 */
public final class CanonicalPack implements TagResolver.OrderedClasspath {
    private static final String FORGE_LOADER = "net/minecraftforge/resource/ResourcePackLoader.class";
    private static final List<String> BINDING_CLASSES = List.of(FORGE_LOADER,
            "net/minecraftforge/server/ServerLifecycleHooks.class",
            "net/minecraftforge/common/ForgeHooks.class",
            "net/minecraft/tags/TagLoader.class", "net/minecraft/tags/TagEntry.class",
            "net/minecraft/tags/TagFile.class", "net/minecraft/server/MinecraftServer.class",
            "net/minecraft/world/level/DataPackConfig.class",
            "net/minecraft/server/packs/repository/PackRepository.class",
            "net/minecraft/server/packs/repository/ServerPacksSource.class");
    private final Path root;
    private final Map<String, List<Definition>> definitions = new TreeMap<>();
    private final Map<String, String> hashes = new TreeMap<>();
    private final Set<Resource> tags = new TreeSet<>();

    private record Definition(String pack, URL url, String sha256, JsonObject json) {
    }

    private CanonicalPack(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Reads only the classpath exported by the repository's Gradle task, never a cache scan.
     * Missing or ambiguous authorities fail extraction instead of becoming an empty pack.
     */
    public static CanonicalPack resolved(Path root) throws IOException {
        CanonicalPack pack = new CanonicalPack(root);
        Path manifest = pack.root.resolve(Path.of("forge", "build", "checks", "sustainability-pack-classpath.json"));
        if (!Files.isRegularFile(manifest)) {
            throw new IOException("Missing Gradle pack export; run :common:writeCheckClasspath.");
        }
        JsonObject input = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
        if (input.get("schema_version").getAsInt() != 1
                || !input.get("gradle_configuration").getAsString().equals("forge:compileClasspath")) {
            throw new IOException("Unsupported sustainability pack export.");
        }
        String minecraft = input.get("minecraft_version").getAsString();
        String forge = input.get("forge_version").getAsString();
        Set<Path> inspected = new TreeSet<>();
        Set<String> found = new TreeSet<>();
        for (JsonElement element : input.getAsJsonArray("classpath")) {
            Path file = Path.of(element.getAsString()).toAbsolutePath().normalize();
            if (!inspected.add(file) || Files.isDirectory(file)) {
                continue;
            }
            if (!Files.isRegularFile(file)) {
                throw new IOException("A Gradle-resolved pack input is absent.");
            }
            try (ZipFile zip = new ZipFile(file.toFile())) {
                boolean forgeApi = zip.getEntry(FORGE_LOADER) != null;
                ZipEntry version = zip.getEntry("version.json");
                boolean vanillaData = zip.getEntry("data/minecraft/tags/items/planks.json") != null;
                if (!forgeApi && !vanillaData) {
                    continue;
                }
                String id = forgeApi ? "forge@" + forge : "vanilla@" + minecraft;
                if (forgeApi && vanillaData || !found.add(id)) {
                    throw new IOException("Ambiguous resolved standalone pack authority: " + id);
                }
                if (!forgeApi && (version == null || !JsonParser.parseString(
                        new String(read(zip, version), StandardCharsets.UTF_8)).getAsJsonObject()
                        .get("id").getAsString().equals(minecraft))) {
                    throw new IOException("Resolved vanilla data does not match Gradle's Minecraft version.");
                }
                pack.index(file, zip, id);
            }
        }
        if (!found.equals(Set.of("vanilla@" + minecraft, "forge@" + forge))) {
            throw new IOException("Resolved classpath does not contain both standalone vanilla and Forge data authorities.");
        }
        for (List<Definition> layers : pack.definitions.values()) {
            // A stable representative composition is safe only where uncertainties() proves it commutes.
            layers.sort(java.util.Comparator.comparing(Definition::pack));
        }
        for (Path file : List.of(Path.of("gradle.properties"), Path.of("common", "build.gradle"),
                Path.of("forge", "build.gradle"), Path.of("forge", "src", "main", "resources",
                        "META-INF", "mods.toml"))) {
            pack.hashes.put(file.toString().replace('\\', '/'),
                    AuthorityAdapters.sha256(Files.readAllBytes(pack.root.resolve(file))));
        }
        pack.hashes.put("standalone_pack_contract:commutative_tag_membership_not_classpath_priority",
                AuthorityAdapters.sha256(("vanilla=" + minecraft + "\nforge=" + forge
                        + "\nmods=grindless\nselection=Gradle-resolved-content-markers\n"
                        + "tag_order=only_commutative_overlaps_are_complete\n").getBytes(StandardCharsets.UTF_8)));
        return pack;
    }

    private void index(Path file, ZipFile zip, String pack) throws IOException {
        hashes.put("resolved_pack:" + pack, AuthorityAdapters.sha256(Files.readAllBytes(file)));
        for (String name : BINDING_CLASSES) {
            ZipEntry entry = zip.getEntry(name);
            if (entry != null) {
                hashes.put("pack_binding:" + pack + "/" + name, AuthorityAdapters.sha256(read(zip, entry)));
            }
        }
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.endsWith(".json") || !name.startsWith("data/")) {
                continue;
            }
            String[] parts = name.split("/", 4);
            if (parts.length < 4 || !Set.of("minecraft", "forge").contains(parts[1])
                    || !Set.of("tags", "recipes").contains(parts[2])) {
                continue;
            }
            byte[] bytes = read(zip, entry);
            String sha256 = AuthorityAdapters.sha256(bytes);
            URL url = new URL("jar:" + file.toUri().toURL().toExternalForm() + "!/" + name);
            Definition definition = new Definition(pack, url, sha256,
                    JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
            definitions.computeIfAbsent(name, ignored -> new ArrayList<>()).add(definition);
            hashes.put("resolved_resource:" + pack + "/" + name, sha256);
            if (parts[2].equals("tags")) {
                String[] tag = parts[3].split("/", 2);
                if (tag.length == 2 && Set.of("items", "blocks", "fluids").contains(tag[0])) {
                    tags.add(new Resource(Kind.TAG, parts[1] + ":" + tag[1].substring(0, tag[1].length() - 5),
                            0, tag[0]));
                }
            }
        }
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws IOException {
        try (var stream = zip.getInputStream(entry)) {
            return stream.readAllBytes();
        }
    }

    /** All actual item/block/fluid tag definitions, independently of recipe references. */
    public Set<Resource> tags() {
        return SustainabilityModel.sortedSet(tags);
    }

    @Override
    public List<URL> resources(String resourcePath) {
        return definitions.getOrDefault(resourcePath, List.of()).stream().map(Definition::url).toList();
    }

    @Override
    public Set<String> uncertainties(String resourcePath) throws IOException {
        List<JsonObject> layers = new ArrayList<>(definitions.getOrDefault(resourcePath, List.of())
                .stream().map(Definition::json).toList());
        Path resources = root.resolve(Path.of("common", "src", "main", "resources"));
        Path local = resources.resolve(Path.of("", resourcePath.split("/"))).normalize();
        if (!local.startsWith(resources)) {
            throw new IllegalArgumentException("Pack resource paths cannot escape the resource root.");
        }
        if (Files.isRegularFile(local)) {
            layers.add(JsonParser.parseString(Files.readString(local)).getAsJsonObject());
        }
        if (layers.size() > 1 && layers.stream().anyMatch(layer ->
                layer.has("replace") && layer.get("replace").getAsBoolean()
                        || layer.has("remove") && !layer.getAsJsonArray("remove").isEmpty())) {
            return Set.of("standalone_pack_priority_unproven:" + resourcePath);
        }
        return Set.of();
    }

    @Override
    public String source(String resourcePath, int index, String sha256) {
        Definition definition = definitions.get(resourcePath).get(index);
        return "classpath:" + resourcePath + "@pack=" + definition.pack()
                + "@composition=commutative_membership_or_explicit_unknown@sha256=" + sha256;
    }

    /** Adds artifact/resource/API hashes without exporting private filesystem paths. */
    public void contribute(Builder builder) {
        hashes.forEach(builder::input);
    }
}
