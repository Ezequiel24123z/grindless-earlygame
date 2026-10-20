package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.shop.EconomyProjection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Existing canonical game-edge binding, with a separate generated economy/cost document. */
public final class EconomyExport {
    private static final List<String> AUTHORITIES = List.of("recipe/BootstrapRecipes.java",
            "recipe/BootstrapProcesses.java", "recipe/RenewableProcesses.java", "recipe/T1Recipes.java",
            "source/SourceLogic.java", "research/ResearchLogic.java", "quest/QuestCatalogue.java");
    private static final String JAVA = "common/src/main/java/io/github/ezequiel24123z/grindless/";

    private EconomyExport() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !(args[2].equals("write") || args[2].equals("check"))) {
            throw new IllegalArgumentException("Usage: EconomyExport <root> <repository-output-file> <write|check>");
        }
        Path root = Path.of(args[0]).toRealPath();
        Path output = root.resolve(args[1]).normalize();
        if (!output.startsWith(root) || !output.getFileName().toString().endsWith(".json")) {
            throw new IOException("Economy output must be a JSON file inside the selected worktree");
        }
        for (Path component = output; component != null && !component.equals(root); component = component.getParent()) {
            if (Files.isSymbolicLink(component)) {
                throw new IOException("Economy output cannot traverse a symbolic link");
            }
        }
        SustainabilityAudit.Result result = VerifyEconomy.canonicalResult(root);
        int checks = VerifyEconomy.verify(result);
        JsonObject projected = JsonParser.parseString(EconomyProjection.json(result)).getAsJsonObject();
        Map<String, Object> comparison = new TreeMap<>();
        for (String authority : AUTHORITIES) {
            String path = JAVA + authority;
            String baseline = git(root, "rev-parse", EconomyProjection.BASELINE + ":" + path);
            String current = git(root, "hash-object", "--path=" + path, path);
            comparison.put(path, Map.of("baseline_blob", baseline, "evaluated_blob", current,
                    "source_equal", baseline.equals(current)));
        }
        projected.add("source_comparison", new com.google.gson.Gson().toJsonTree(comparison));
        String json = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create()
                .toJson(projected) + "\n";
        if (args[2].equals("check")) {
            if (!Files.isRegularFile(output) || !JsonParser.parseString(Files.readString(output)).equals(projected)) {
                throw new AssertionError("Generated economy/cost data is stale; run the existing writer, do not edit the JSON");
            }
        } else {
            Files.createDirectories(output.getParent());
            Files.writeString(output, json, StandardCharsets.UTF_8);
        }
        Path evidence = root.resolve(Path.of("build", "economy"));
        Files.createDirectories(evidence);
        Map<String, Object> provenance = new TreeMap<>();
        provenance.put("schema_version", 1);
        provenance.put("invoking_source_commit", git(root, "rev-parse", "HEAD"));
        provenance.put("source_state", "Evaluated worktree and Gradle classpath; clean publication not asserted");
        provenance.put("action", args[2]);
        provenance.put("model_sha256", AuthorityAdapters.sha256(json.getBytes(StandardCharsets.UTF_8)));
        provenance.put("authority_sha256", AuditReports.authorityHash(result.authorities()));
        provenance.put("checks", checks);
        provenance.put("native_client_campaign", "NOT_EXECUTED_BY_THIS_EXPORT");
        Files.writeString(evidence.resolve("provenance.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(provenance) + "\n", StandardCharsets.UTF_8);
        System.out.println("ECONOMY/COST " + args[2] + ": " + checks + " checks; " + projected.getAsJsonArray("offers").size()
                + " offers; local canonical model only, not human pacing or native acceptance");
        System.out.println("MODEL SHA-256 " + provenance.get("model_sha256"));
    }

    private static String git(Path root, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "--no-pager"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0 || !output.matches("[a-f0-9]{40}")) {
            throw new IOException("Could not bind the requested local source object");
        }
        return output;
    }
}
