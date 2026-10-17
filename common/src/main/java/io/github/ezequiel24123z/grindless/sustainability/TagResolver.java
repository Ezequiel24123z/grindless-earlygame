package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Resolves registry-specific, nested tag JSON without a registry or loader.
 * Missing/optional/cyclic references remain explicit; catalogue item names never fill a tag.
 */
public final class TagResolver {
    /**
     * Supplies the complete vanilla/Forge definitions in independently established pack order.
     * Lowest priority comes first; the repository's local definitions are applied last.
     * A classloader enumeration without resource-pack priority evidence does not meet this contract.
     */
    @FunctionalInterface
    public interface OrderedClasspath {
        /**
         * Returns definitions for one registry-specific resource path.
         *
         * @param resourcePath classpath resource path
         * @return all standalone vanilla/Forge definitions in increasing priority
         * @throws IOException if resource discovery fails
         */
        List<URL> resources(String resourcePath) throws IOException;
    }

    private final Path root;
    private final OrderedClasspath classpath;
    private final boolean classpathFallback;
    private final boolean precedenceKnown;
    private final Map<Resource, Path> local = new TreeMap<>();
    private final Map<Resource, TagResolution> cache = new HashMap<>();
    private final Map<Resource, Definitions> definitions = new TreeMap<>();
    private final Map<String, String> hashes = new TreeMap<>();
    private final List<AuditFinding> findings = new ArrayList<>();

    /**
     * Indexes every local tag registry, with optional unverified classpath discovery.
     * Discovered classpath members are evidence, not a complete runtime tag resolution.
     *
     * @param root repository root
     * @param classLoader resolved runtime classpath, never a cache scan
     * @param classpathFallback whether vanilla/Forge classpath fallback is explicitly permitted
     * @throws IOException if a local authority cannot be read
     */
    public TagResolver(Path root, ClassLoader classLoader, boolean classpathFallback) throws IOException {
        this(root, path -> Collections.list(classLoader.getResources(path)), classpathFallback, false);
    }

    /**
     * Indexes local tags above a caller-established, complete standalone resource-pack order.
     *
     * @param root repository root
     * @param classpath complete ordered vanilla/Forge resources, not an unordered cache scan
     * @throws IOException if a local authority cannot be read
     */
    public TagResolver(Path root, OrderedClasspath classpath) throws IOException {
        this(root, classpath, true, true);
    }

    private TagResolver(Path root, OrderedClasspath classpath, boolean classpathFallback,
                        boolean precedenceKnown) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        this.classpath = classpath;
        this.classpathFallback = classpathFallback;
        this.precedenceKnown = precedenceKnown;
        Path data = this.root.resolve(Path.of("common", "src", "main", "resources", "data"));
        try (var files = Files.walk(data)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                Path relative = data.relativize(file);
                if (relative.getNameCount() < 4 || !relative.getName(1).toString().equals("tags")) {
                    continue;
                }
                String registry = relative.getName(2).toString();
                String path = relative.subpath(3, relative.getNameCount()).toString().replace('\\', '/');
                Resource tag = new Resource(Kind.TAG, relative.getName(0) + ":"
                        + path.substring(0, path.length() - 5), 0, registry);
                local.put(tag, file);
            }
        }
    }

    /** Returns all local tag identities, including block tags and tags not yet used by recipes. */
    public Set<Resource> localTags() {
        return Collections.unmodifiableSet(local.keySet());
    }

    /** Resolves a tag and records all source hashes and unresolved evidence. */
    public TagResolution resolve(Resource tag) throws IOException {
        if (tag.kind() != Kind.TAG) {
            throw new IllegalArgumentException("Only tag resources can be resolved");
        }
        if (!cache.containsKey(tag)) {
            cache.put(tag, expand(tag, new TreeSet<>()));
            Set<Resource> pending = new TreeSet<>(definitions.keySet());
            pending.removeAll(cache.keySet());
            while (!pending.isEmpty()) {
                Resource nested = pending.iterator().next();
                cache.put(nested, expand(nested, new TreeSet<>()));
                pending.addAll(definitions.keySet());
                pending.removeAll(cache.keySet());
            }
        }
        return cache.get(tag);
    }

    /** Copies all accumulated resolutions and extraction evidence into a snapshot. */
    public void contribute(Builder builder) {
        cache.forEach(builder::tag);
        hashes.forEach(builder::input);
        findings.forEach(builder::finding);
    }

    private TagResolution expand(Resource tag, Set<Resource> visiting) throws IOException {
        if (!visiting.add(tag)) {
            return new TagResolution(Set.of(), false, Set.of("cycle:" + tag.key()), List.of());
        }
        Definitions input = definitions(tag);
        List<String> sources = new ArrayList<>(input.sources());
        List<JsonElement> values = new ArrayList<>();
        Set<Resource> members = new TreeSet<>();
        Set<String> unresolved = new TreeSet<>();
        if (input.layers().isEmpty()) {
            unresolved.add(tag.key());
        }
        for (JsonObject definition : input.layers()) {
            if (definition.has("replace") && definition.get("replace").getAsBoolean()) {
                values.clear();
                unresolved.clear();
            }
            if (!definition.has("values") || !definition.get("values").isJsonArray()) {
                unresolved.add("invalid_values:" + tag.key());
                continue;
            }
            definition.getAsJsonArray("values").forEach(values::add);
        }
        unresolved.addAll(input.uncertainties());
        for (JsonElement value : values) {
            String id = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString()
                    : value.getAsString();
            if (id.startsWith("#")) {
                Resource nested = new Resource(Kind.TAG, id.substring(1), 0, tag.registry());
                TagResolution resolution = expand(nested, visiting);
                members.addAll(resolution.members());
                unresolved.addAll(resolution.unresolved());
                sources.addAll(resolution.sources());
            } else {
                members.add(new Resource(tag.registry().equals("fluids") ? Kind.FLUID : Kind.ITEM,
                        id, 0, tag.registry()));
            }
        }
        visiting.remove(tag);
        return new TagResolution(members, unresolved.isEmpty(), unresolved, sources);
    }

    private Definitions definitions(Resource tag) throws IOException {
        Definitions cached = definitions.get(tag);
        if (cached != null) {
            return cached;
        }
        List<JsonObject> layers = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        Set<String> uncertainties = new TreeSet<>();
        boolean standaloneClasspath = tag.id().startsWith("minecraft:") || tag.id().startsWith("forge:");
        if (standaloneClasspath) {
            if (!classpathFallback) {
                uncertainties.add("classpath_not_checked:" + tag.key());
            } else {
                String[] id = tag.id().split(":", 2);
                String resourcePath = "data/" + id[0] + "/tags/" + tag.registry() + "/" + id[1] + ".json";
                List<URL> urls = classpath.resources(resourcePath);
                for (int priority = 0; priority < urls.size(); priority++) {
                    try (InputStream stream = urls.get(priority).openStream()) {
                        byte[] bytes = stream.readAllBytes();
                        String hash = AuthorityAdapters.sha256(bytes);
                        String source = "classpath:" + resourcePath
                                + (precedenceKnown ? "@priority=" : "@enumeration_index=") + priority + "@sha256=" + hash;
                        layers.add(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
                        sources.add(source);
                        hashes.put(source, hash);
                    }
                }
                if (!precedenceKnown) {
                    uncertainties.add("classpath_precedence_unknown:" + tag.key());
                }
                if (!local.containsKey(tag) && !layers.isEmpty()) {
                    findings.add(new AuditFinding("unresolved_tag", tag.key(), "local_json",
                            AuditFinding.Severity.UNKNOWN,
                            "No local JSON; classpath definitions retain their provenance and any priority uncertainty.",
                            sources));
                }
            }
        }
        Path file = local.get(tag);
        if (file != null) {
            byte[] bytes = Files.readAllBytes(file);
            String source = root.relativize(file).toString().replace('\\', '/');
            layers.add(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
            sources.add(source);
            hashes.put(source, AuthorityAdapters.sha256(bytes));
        }
        Definitions result = new Definitions(List.copyOf(layers), List.copyOf(sources), Set.copyOf(uncertainties));
        definitions.put(tag, result);
        return result;
    }

    private record Definitions(List<JsonObject> layers, List<String> sources, Set<String> uncertainties) {
    }
}
