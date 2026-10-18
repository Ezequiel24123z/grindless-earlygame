package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidance;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceJson;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceProjection;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceRuntime;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.client.ResourceGuidanceClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Snapshot;

/** Standalone game-edge exporter using the existing canonical S2 entry point and Gradle classpath. */
public final class DashboardExport {
    static final List<String> RUNTIME = List.of("index.html", "dashboard.css", "dashboard.js", "dashboard-model.js");
    static final Set<String> ARTIFACT = Set.of("index.html", "dashboard.css", "dashboard.js",
            "dashboard-model.js", "data.json", "provenance.json", "audit.json", "audit.md");
    private static final String ASSETS = "docs/assets/t0-asset-manifest.json";
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping()
            .setPrettyPrinting().create();

    private DashboardExport() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: DashboardExport <root> <build-output-directory> <local|pr|stable> <export|check|guidance-write|guidance-check>");
        }
        Path root = Path.of(args[0]).toRealPath();
        if (args[3].equals("guidance-write") || args[3].equals("guidance-check")) {
            resourceGuidance(root, args[3].equals("guidance-write"));
            return;
        }
        boolean checkOnly = args[3].equals("check");
        if (!checkOnly && !args[3].equals("export")) {
            throw new IllegalArgumentException("Choose export or check.");
        }
        String revision = git(root, "rev-parse", "HEAD");
        if (!checkOnly) {
            requireClean(root, revision);
        }
        String[] repository = repository(git(root, "remote", "get-url", "origin"));
        JsonObject plan = read(root.resolve(Path.of(".agent", "work-plan.json")));
        DashboardProjection.Context context = new DashboardProjection.Context(repository[0], repository[1],
                git(root, "branch", "--show-current"), revision, Instant.now().toString(), args[2],
                plan.get("product_gate").getAsString());
        SustainabilityAudit.Result result = canonicalResult(root);
        JsonObject data = DashboardProjection.document(result, context);
        data.add("assets", assets(root));
        data.getAsJsonObject("context").addProperty("source_state", checkOnly
                ? "verification_only_no_artifact_or_clean_commit_claim"
                : "clean_source_commit_with_observed_binary_and_authority_identities");
        int assertions = VerifyDashboardProjection.verify(result, data, revision);
        String auditJson = AuditReports.json(result, revision, true);
        String auditMarkdown = AuditReports.markdown(result, revision, true);
        DashboardProjection.requirePublic(JsonParser.parseString(auditJson), result);
        DashboardProjection.requirePublic(JSON.toJsonTree(auditMarkdown), result);
        if (checkOnly) {
            assertions += VerifyDashboardProjection.counterexamples(result, context);
        }
        Map<String, Object> audit = AuditReports.document(result, revision, true);
        String json = DashboardProjection.json(data);
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (String name : RUNTIME) {
            files.put(name, Files.readAllBytes(root.resolve(Path.of("tools", "dashboard", name))));
        }
        files.put("data.json", json.getBytes(StandardCharsets.UTF_8));
        files.put("audit.json", auditJson.getBytes(StandardCharsets.UTF_8));
        files.put("audit.md", auditMarkdown.getBytes(StandardCharsets.UTF_8));
        Map<String, String> fileHashes = new TreeMap<>();
        files.forEach((name, bytes) -> fileHashes.put(name, AuthorityAdapters.sha256(bytes)));
        JsonObject provenance = JSON.toJsonTree(Map.of(
                "schema_version", 1, "source_commit", revision, "repository", repository[0],
                "generated_at", context.generatedAt(), "files_sha256", fileHashes,
                "projection_assertions", assertions,
                "scope", "Canonical conditional model projection; not global, manual, Tier or merge acceptance.",
                "source_cleanliness", checkOnly ? "Working-tree verification; no clean-source artifact claim."
                        : "Tracked and ordinary untracked files checked before and after generation.")).getAsJsonObject();
        for (String name : List.of("source_revision", "source_state", "authority_sha256", "authority_snapshot_sha256",
                "authority_sources_sha256", "implementation_sha256", "implementation_state", "implementation_hashes",
                "implementation_unresolved", "implementation_source_sha256", "implementation_source_hashes",
                "tag_resolution_scope", "resolved_pack_hashes", "registry_binding_hashes", "registry_binding_conditions",
                "input_hashes", "mode", "summary", "active_t0_conditional_gate")) {
            provenance.add(name, JSON.toJsonTree(audit.get(name)));
        }
        provenance.add("tool_source_hashes", JSON.toJsonTree(toolSources(root)));
        provenance.add("tool_class_directory_hashes", JSON.toJsonTree(toolClasses(root)));
        provenance.add("projector_class_hashes", JSON.toJsonTree(projectorClasses()));
        provenance.add("asset_manifest", JSON.toJsonTree(Map.of("source_path", ASSETS,
                "sha256", AuthorityAdapters.sha256(Files.readAllBytes(root.resolve(ASSETS))),
                "scope", "Historical source-bound inventory, not current visual acceptance.")));
        files.put("provenance.json", DashboardProjection.json(provenance, result).getBytes(StandardCharsets.UTF_8));
        if (checkOnly) {
            System.out.println("DASHBOARD PROJECTION: " + assertions + " passed; no artifact or acceptance claim.");
            return;
        }
        requireClean(root, revision);
        Path output = output(root, Path.of(args[1]));
        try (var children = Files.list(output)) {
            if (children.anyMatch(file -> !ARTIFACT.contains(file.getFileName().toString())
                    || !Files.isRegularFile(file) || Files.isSymbolicLink(file))) {
                throw new IOException("Artifact output contains an unexpected file; choose an empty build subdirectory.");
            }
        }
        for (var entry : files.entrySet()) {
            Path temporary = Files.createTempFile(output, ".dashboard-", ".tmp");
            try {
                Files.write(temporary, entry.getValue());
                Files.move(temporary, output.resolve(entry.getKey()), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        requireClean(root, revision);
        System.out.println("CANONICAL DASHBOARD " + revision + ": " + result.authorities().resources().size()
                + " resources, " + result.authorities().routes().size() + " routes, "
                + result.authorities().tags().size() + " tags, " + result.authorities().objectives().size()
                + " objectives; " + assertions + " projection assertions.");
        System.out.println("DATA SHA-256 " + fileHashes.get("data.json"));
        System.out.println("AUDIT JSON SHA-256 " + fileHashes.get("audit.json"));
        System.out.println("AUDIT MARKDOWN SHA-256 " + fileHashes.get("audit.md"));
        System.out.println("ARTIFACT " + root.relativize(output).toString().replace('\\', '/'));
    }

    static SustainabilityAudit.Result canonicalResult(Path root) throws IOException {
        var sources = VerifySustainabilityRegistry.canonicalSources(root, T0Sustainability.vanillaAllowlist());
        Snapshot snapshot = T0Sustainability.withEntrySources(sources.snapshot());
        var template = T0Sustainability.entryTemplate(sources.localWork());
        var annotations = T0Sustainability.annotations(snapshot, sources.facts(), sources.localWork(), template);
        DebtManifest debt = DebtManifest.read(root.resolve(Path.of("tools", "sustainability", "debt.json")));
        return SustainabilityAudit.deriveEntryProfile(snapshot, annotations, debt, 0).replay();
    }

    /** Reuses the exact game-edge binding; generation is intentionally not a dependency of classes. */
    private static void resourceGuidance(Path root, boolean write) throws Exception {
        SustainabilityAudit.Result result = canonicalResult(root);
        var projection = ResourceGuidanceProjection.project(result);
        int assertions = VerifyResourceGuidance.verifyProjection(result, projection);
        Path resource = root.resolve(Path.of("common", "src", "main", "resources"))
                .resolve(ResourceGuidance.ASSET);
        if (write) {
            VerifyResourceGuidance.verifyLocalization(root, result);
            Files.createDirectories(resource.getParent());
            Files.writeString(resource, ResourceGuidanceJson.write(projection), StandardCharsets.UTF_8);
        }
        assertions += VerifyResourceGuidance.verifyCanonical(root, result);
        JsonObject provenance = JSON.toJsonTree(Map.of(
                "schema_version", 1, "action", write ? "write" : "check",
                "source_state", "Evaluated worktree sources; no clean-commit or acceptance claim.",
                "declaration_content_sha256", projection.contentSha256(),
                "authority_sha256", AuditReports.authorityHash(result.authorities()),
                "tool_source_hashes", toolSources(root),
                "tool_class_directory_hashes", toolClasses(root),
                "guidance_source_hashes", guidanceSources(root),
                "guidance_class_hashes", guidanceClasses())).getAsJsonObject();
        Path evidence = output(root, Path.of("build", "resource-guidance"));
        Files.writeString(evidence.resolve("provenance.json"), DashboardProjection.json(provenance), StandardCharsets.UTF_8);
        System.out.println("CANONICAL RESOURCE GUIDANCE: " + projection.resources().size() + " declarations; "
                + assertions + " comparisons; SHA-256 " + projection.contentSha256());
        System.out.println("Prepared declarations only; not current-world, client, Survival or Tier acceptance.");
    }

    static JsonObject assets(Path root) throws IOException {
        JsonObject source = read(root.resolve(ASSETS)).getAsJsonObject("source_bound_readiness");
        if (source == null) {
            throw new IOException("The declared source-bound asset inventory is missing.");
        }
        JsonObject assets = new JsonObject();
        JsonArray entries = new JsonArray();
        for (String collection : List.of("blocks", "standalone_items", "multiblocks")) {
            for (JsonElement element : source.getAsJsonArray(collection)) {
                JsonObject entry = new JsonObject();
                JsonObject original = element.getAsJsonObject();
                entry.add("id", original.get("id").deepCopy());
                entry.add("name", original.get("id").deepCopy());
                entry.addProperty("kind", collection);
                entry.addProperty("source_path", ASSETS);
                entry.add("source_commit", source.get("source_revision").deepCopy());
                entry.add("status", JsonNull.INSTANCE);
                entry.add("description", source.get("scope").deepCopy());
                entry.add("details", original.deepCopy());
                entries.add(entry);
            }
        }
        assets.add("entries", entries);
        for (String field : List.of("generation_queue", "qc_queue", "checks")) {
            assets.add(field, JsonNull.INSTANCE);
        }
        assets.add("source_commit", source.get("source_revision").deepCopy());
        assets.add("inventory_status", source.get("status").deepCopy());
        assets.add("coverage", source.get("coverage").deepCopy());
        assets.add("visual_readiness", source.get("visual_readiness").deepCopy());
        return assets;
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static void requireClean(Path root, String revision) throws IOException, InterruptedException {
        if (!revision.matches("[0-9a-f]{40}") || !git(root, "rev-parse", "HEAD").equals(revision)
                || !git(root, "status", "--porcelain", "--untracked-files=normal").isEmpty()) {
            throw new IOException("A clean, unchanged source commit is required for an artifact. Commit first or use -CheckOnly.");
        }
    }

    private static String git(Path root, String... arguments) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>(List.of("git", "--no-pager", "-c", "core.quotepath=false"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
        String value = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) {
            throw new IOException("Git source identity could not be resolved; no artifact was published.");
        }
        return value;
    }

    private static String[] repository(String remote) {
        String normalized = remote;
        if (remote.startsWith("git@")) {
            normalized = "https://" + remote.substring(4).replaceFirst(":", "/");
        }
        URI uri = URI.create(normalized);
        String path = uri.getPath().replaceFirst("\\.git$", "").replaceFirst("^/", "");
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getQuery() != null
                || uri.getFragment() != null || !path.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("A sanitizable HTTPS or Git SSH repository identity is required.");
        }
        return new String[]{path, "https://" + uri.getHost() + "/" + path};
    }

    private static Map<String, String> toolSources(Path root) throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        for (String file : List.of("tools/dashboard/DashboardExport.java", "tools/dashboard/DashboardServer.java",
                "tools/dashboard/export.ps1", "tools/dashboard/export.sh", "tools/dashboard/serve.ps1",
                "tools/dashboard/serve.sh", "tools/dashboard/tests/VerifyDashboardProjection.java",
                "tools/dashboard/tests/real-data-check.js", "tools/checks/VerifySustainability.java",
                "tools/checks/VerifySustainabilityAdapters.java", "tools/checks/VerifySustainabilityRegistry.java",
                "tools/checks/VerifySustainabilityT0.java", "tools/checks/VerifyResourceGuidance.java",
                "tools/generate-resource-guidance.ps1", "tools/generate-resource-guidance.sh",
                "tools/dashboard/tests/verify-export-root.ps1", "tools/dashboard/tests/verify-export-root.sh")) {
            hashes.put(file, AuthorityAdapters.sha256(Files.readAllBytes(root.resolve(file))));
        }
        return hashes;
    }

    private static Map<String, String> guidanceSources(Path root) throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        String base = "common/src/main/java/io/github/ezequiel24123z/grindless/";
        for (String file : List.of("guidance/ResourceGuidance.java", "guidance/ResourceGuidanceJson.java",
                "guidance/ResourceGuidanceProjection.java", "guidance/ResourceGuidanceRuntime.java",
                "guidance/ResourceGuidanceContext.java", "client/ResourceGuidanceClient.java",
                "sustainability/AuditReports.java")) {
            hashes.put(base + file, AuthorityAdapters.sha256(Files.readAllBytes(root.resolve(base + file))));
        }
        String language = "common/src/main/resources/assets/grindless/lang/en_us.json";
        hashes.put(language, AuthorityAdapters.sha256(Files.readAllBytes(root.resolve(language))));
        return hashes;
    }

    private static Map<String, String> guidanceClasses() throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        for (Class<?> type : List.of(ResourceGuidance.class, ResourceGuidanceJson.class, ResourceGuidanceProjection.class,
                ResourceGuidanceRuntime.class, ResourceGuidanceContext.class, ResourceGuidanceClient.class, AuditReports.class)) {
            guidanceClass(type, hashes);
        }
        return hashes;
    }

    private static void guidanceClass(Class<?> type, Map<String, String> hashes) throws IOException {
        String name = type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("An executed guidance helper class is missing.");
            }
            hashes.put(name, AuthorityAdapters.sha256(stream.readAllBytes()));
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            guidanceClass(nested, hashes);
        }
    }

    private static Map<String, String> toolClasses(Path root) throws Exception {
        Path directory = Path.of(DashboardExport.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!directory.startsWith(root.resolve("build")) || !Files.isDirectory(directory)) {
            throw new IOException("Dashboard tools must be freshly compiled in this worktree's build directory.");
        }
        Map<String, String> hashes = new TreeMap<>();
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                hashes.put(directory.relativize(file).toString().replace('\\', '/'),
                        AuthorityAdapters.sha256(Files.readAllBytes(file)));
            }
        }
        return hashes;
    }

    private static Map<String, String> projectorClasses() throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        for (String type : List.of("DashboardProjection", "DashboardProjection$Context", "DashboardProjection$1")) {
            String path = DashboardProjection.class.getPackageName().replace('.', '/') + "/" + type + ".class";
            try (InputStream stream = DashboardProjection.class.getClassLoader().getResourceAsStream(path)) {
                if (stream == null) {
                    throw new IOException("A compiled dashboard projection class is missing.");
                }
                hashes.put(path, AuthorityAdapters.sha256(stream.readAllBytes()));
            }
        }
        return hashes;
    }

    private static Path output(Path root, Path selected) throws IOException {
        Path build = root.resolve("build");
        Path directory = (selected.isAbsolute() ? selected : root.resolve(selected)).normalize();
        if (!directory.startsWith(build) || directory.equals(build)) {
            throw new IOException("Dashboard artifacts belong in a dedicated ignored build subdirectory.");
        }
        Path current = root;
        for (Path part : root.relativize(directory)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Artifact output cannot traverse a symbolic link.");
            }
        }
        Files.createDirectories(directory);
        return directory.toRealPath();
    }
}
