package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.GsonBuilder;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Normal canonical binding and writer for the bounded T1 graph, costs and proof limitations. */
public final class T1Export {
    private static final String JAVA = "common/src/main/java/io/github/ezequiel24123z/grindless/";
    private static final List<String> SOURCES = List.of(
            JAVA + "progression/T1Progression.java", JAVA + "sustainability/T1Sustainability.java",
            JAVA + "recipe/ProcessGraph.java", JAVA + "recipe/T1Recipes.java",
            JAVA + "recipe/IngredientSpec.java", JAVA + "matrix/ControlMatrixSpec.java",
            JAVA + "sustainability/AuthorityAdapters.java",
            JAVA + "recipe/FabricationLogic.java", JAVA + "matrix/ControlMatrixNbt.java",
            "common/src/main/resources/data/grindless/recipes/flux_cell.json",
            "tools/checks/VerifyT1Progression.java", "tools/t1/T1Export.java",
            "tools/t1/export.ps1", "tools/t1/export.sh");

    private T1Export() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !(args[2].equals("write") || args[2].equals("check"))) {
            throw new IllegalArgumentException("Usage: T1Export <root> <repository-output.json> <write|check>");
        }
        Path root = Path.of(args[0]).toRealPath();
        Path output = root.resolve(args[1]).normalize();
        if (!output.startsWith(root) || !output.getFileName().toString().endsWith(".json")) {
            throw new IOException("The T1 graph must be a JSON file inside the selected worktree");
        }
        for (Path component = output; component != null && !component.equals(root); component = component.getParent()) {
            if (Files.isSymbolicLink(component)) {
                throw new IOException("The T1 graph output cannot traverse a symbolic link");
            }
        }
        String name = output.getFileName().toString();
        Path markdown = output.resolveSibling(name.substring(0, name.length() - 5) + ".md");
        if (Files.isSymbolicLink(markdown)) {
            throw new IOException("The generated T1 Markdown cannot overwrite a symbolic link");
        }
        SustainabilityAudit.Result t0 = VerifyT1Progression.canonicalResult(root);
        int checks = VerifyT1Progression.verify(t0);
        String json = T1Sustainability.json(t0);
        String report = T1Sustainability.markdown(t0);
        if (args[2].equals("check")) {
            check(output, json);
            check(markdown, report);
        } else {
            Files.createDirectories(output.getParent());
            Files.writeString(output, json, StandardCharsets.UTF_8);
            Files.writeString(markdown, report, StandardCharsets.UTF_8);
        }
        Map<String, String> sourceHashes = new TreeMap<>();
        for (String source : SOURCES) {
            sourceHashes.put(source, AuthorityAdapters.sha256(Files.readAllBytes(root.resolve(source))));
        }
        Map<String, String> classes = new TreeMap<>();
        for (Class<?> type : List.of(T1Progression.class, T1Progression.Requirement.class, T1Progression.Line.class,
                T1Progression.AtlasSelection.class, T1Sustainability.class,
                T1Sustainability.InputEvidence.class, T1Sustainability.SeedAllocation.class,
                AuthorityAdapters.class, IngredientSpec.class, IngredientSpec.MatrixRole.class, ControlMatrixSpec.class,
                VerifyT1Progression.class, T1Export.class)) {
            String resource = "/" + type.getName().replace('.', '/') + ".class";
            try (InputStream stream = type.getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new IOException("The executed T1 implementation class cannot be fingerprinted: " + type.getName());
                }
                classes.put(type.getName(), AuthorityAdapters.sha256(stream.readAllBytes()));
            }
        }
        Map<String, Object> provenance = new TreeMap<>();
        provenance.put("schema_version", 1);
        provenance.put("invoking_source_commit", revision(root));
        provenance.put("source_state", "Evaluated selected worktree and Gradle classpath; clean publication not asserted");
        provenance.put("action", args[2]);
        provenance.put("authority_sha256", AuditReports.authorityHash(t0.authorities()));
        provenance.put("source_hashes", sourceHashes);
        provenance.put("executed_class_hashes", classes);
        provenance.put("checks", checks);
        provenance.put("model_sha256", AuthorityAdapters.sha256(json.getBytes(StandardCharsets.UTF_8)));
        provenance.put("report_sha256", AuthorityAdapters.sha256(report.getBytes(StandardCharsets.UTF_8)));
        provenance.put("raw_file_sha256", AuthorityAdapters.sha256(Files.readAllBytes(output)));
        provenance.put("hash_scope", "Model/report hashes use canonical LF; raw_file_sha256 preserves checkout bytes");
        provenance.put("native_client_campaign", "NOT_EXECUTED_BY_THIS_EXPORT");
        Path evidence = root.resolve(Path.of("build", "t1"));
        Files.createDirectories(evidence);
        Files.writeString(evidence.resolve("provenance.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(provenance) + "\n", StandardCharsets.UTF_8);
        System.out.println("T1 FIRST-FACTORY " + args[2] + ": " + checks
                + " contracts; catalogue/T0-boundary scope, not a T1 renewable-closure or native-play pass");
        System.out.println("MODEL SHA-256 " + provenance.get("model_sha256"));
        System.out.println("REPORT SHA-256 " + provenance.get("report_sha256"));
    }

    private static void check(Path output, String expected) throws IOException {
        if (!Files.isRegularFile(output)
                || !Files.readString(output, StandardCharsets.UTF_8).replace("\r\n", "\n").equals(expected)) {
            throw new AssertionError("Generated T1 data is missing or stale: " + output.getFileName()
                    + "; run tools/t1/export, do not repair quantities or fingerprints by hand");
        }
    }

    private static String revision(Path root) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "--no-pager", "rev-parse", "HEAD")
                .directory(root.toFile()).redirectErrorStream(true).start();
        String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0 || !result.matches("[a-f0-9]{40}")) {
            throw new IOException("Cannot bind the T1 export to its actual invoking revision");
        }
        return result;
    }
}
