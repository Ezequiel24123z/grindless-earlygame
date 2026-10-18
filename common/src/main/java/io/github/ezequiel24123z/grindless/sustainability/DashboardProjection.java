package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/** Read-only schema-1 presentation of the canonical result; never an independent audit or catalogue. */
public final class DashboardProjection {
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping()
            .setPrettyPrinting().create();
    private static final BigDecimal MAX_SAFE_INTEGER = new BigDecimal("9007199254740991");
    private static final Pattern PRIVATE = Pattern.compile(
            "(?i)(?:\\b[A-Z]:[\\\\/]|\\\\\\\\[^\\s]+|/(?:Users|home|private|mnt)/"
                    + "|\\b(?:gh[pousr]_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+|sk-[A-Za-z0-9_-]{16,})"
                    + "|\\bBearer\\s+\\S+|\\bAuthorization\\s*:\\s*\\S+"
                    + "|\\b(?:password|token|secret|api[_ -]?key)\\s*[:=]\\s*\\S+"
                    + "|\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"
                    + "|https?://[^/\\s]+@|(?:^|\\n)\\s*(?:at [\\w.$]+\\(|Caused by:|\\[\\d{2}:\\d{2}:\\d{2}\\]))");

    private DashboardProjection() {
    }

    /** Identity is supplied by the clean-source exporter, not inferred from a passing model gate. */
    public record Context(String repository, String repositoryUrl, String branch, String commit,
                          String generatedAt, String mode, String status) {
        public Context {
            if (commit == null || !commit.matches("[0-9a-f]{40}")
                    || !Set.of("local", "pr", "stable").contains(mode)) {
                throw new IllegalArgumentException("A full source commit and explicit dashboard mode are required.");
            }
        }
    }

    /** Projects one actual result and the exact report representation used by AuditReports.json. */
    public static JsonObject document(SustainabilityAudit.Result result, Context context) {
        JsonObject audit = JSON.toJsonTree(AuditReports.document(result, context.commit(), true)).getAsJsonObject();
        JsonObject document = row("schema_version", 1, "context", row(
                "repository", context.repository(), "repository_url", context.repositoryUrl(),
                "branch", context.branch(), "commit", context.commit(), "generated_at", context.generatedAt(),
                "mode", context.mode(), "status", context.status(), "active_tier", 0,
                "active_slice", "Conditional T0 model; global and player acceptance remain separate",
                "pull_request_url", null, "pull_request_state", null, "milestones", null, "recent_commits", null));
        document.add("classifications", classifications());
        document.add("resources", resources(result, audit));
        document.add("routes", routes(result, audit));
        document.add("tiers", tiers(result, audit));
        document.add("machines", machines(result));
        document.add("quests", quests(result, audit));
        document.add("findings", findings(result, audit));
        document.add("assets", JsonNull.INSTANCE);
        document.add("validation", row("status", null,
                "checks", List.of(
                        row("id", "active_t0_conditional_gate", "name", "Active T0 conditional model gate",
                                "status", audit.getAsJsonObject("active_t0_conditional_gate").get("status"),
                                "source_commit", context.commit(), "evidence", row("summary",
                                        "Actual Result from this invocation; adjacent audit.json and provenance.json "
                                                + "retain exact identities. Not physical, client, global or Tier acceptance."),
                                "details", audit.get("active_t0_conditional_gate")),
                        row("id", "global_sustainability_model", "name", "Global audit diagnostics",
                                "status", null, "source_commit", context.commit(), "evidence", null,
                                "details", row("sustainability_pass", audit.get("sustainability_pass"),
                                        "ratchet_pass", audit.get("ratchet_pass"), "summary", audit.get("summary"),
                                        "scope", "Missing future ledgers are unassessed, not accepted future gameplay."))),
                "ci", null, "manual_acceptance", null, "required_deliverables", null));
        for (String field : List.of("active_t0_conditional_gate", "summary", "tags", "terminals",
                "closed_loops", "committed_plans", "cycle_balance_certificates", "debt")) {
            document.add(field, audit.get(field).deepCopy());
        }
        return exactNumbers(document).getAsJsonObject();
    }

    /** Serializes exact Java integers as strings only where JavaScript would otherwise round them. */
    public static String json(JsonObject document) {
        JsonObject exact = exactNumbers(document).getAsJsonObject();
        requirePublic(exact);
        return JSON.toJson(exact) + "\n";
    }

    /** Rejects private output rather than silently replacing a canonical identity or quantity. */
    public static void requirePublic(JsonElement value) {
        if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet().forEach(entry -> {
                requirePublic(new JsonPrimitive(entry.getKey()));
                requirePublic(entry.getValue());
            });
        } else if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(DashboardProjection::requirePublic);
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && PRIVATE.matcher(value.getAsString()).find()) {
            throw new IllegalArgumentException("Public dashboard output contains a private path, credential, contact or raw log.");
        }
    }

    private static JsonArray classifications() {
        JsonArray entries = new JsonArray();
        for (ResourceClass value : ResourceClass.values()) {
            entries.add(classification(lower(value), "resource", switch (value) {
                case BOOTSTRAP -> "Finite establishment supply, not recurring renewable production.";
                case RENEWABLE -> "Declared unlimited mod-owned supply; the model must prove upstream closure.";
                case CLOSED_LOOP -> "Protected circulating inventory, not an unlimited net source.";
                case CATALYTIC -> "Non-consumed requirement; availability and replacement remain distinct.";
                case LOSSY_LOOP -> "A circulating route with losses requiring renewable make-up.";
                case FINITE_OPTIONAL -> "Finite optional supply, never the permanent mandatory dependency.";
                case FORBIDDEN_DEPENDENCY -> "A dependency forbidden as canonical mandatory supply.";
            }));
        }
        for (SideStreamClass value : SideStreamClass.values()) {
            entries.add(classification(lower(value), "byproduct", switch (value) {
                case BYPRODUCT_CURRENT -> "A side stream with an implemented consumer.";
                case BYPRODUCT_FUTURE -> "A future consumer requires a named committed plan and tier.";
                case BYPRODUCT_OPTIONAL -> "Optional side stock, not a mandatory progression dependency.";
                case WASTE_RECOVERABLE -> "Waste with a declared recovery route or explicitly planned recovery.";
                case WASTE_HAZARDOUS -> "Hazardous waste requires implemented containment or treatment.";
                case WASTE_INERT -> "Inert optional waste need not acquire an artificial consumer.";
                case PROCESS_LOSS -> "A process loss need not become an item stack.";
                case EMISSION -> "A declared emission, not retained material inventory.";
                case CATALYST_SPENT -> "Spent catalyst with an identified originating catalyst.";
                case POTENTIAL_FUTURE_USE -> "Uncertain future usefulness, not a promised consumer or tier.";
            }));
        }
        return entries;
    }

    private static JsonObject classification(String id, String domain, String description) {
        String label = id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1).replace('_', ' ');
        return row("id", id, "domain", domain, "label", label, "description", description,
                "localization_key", null, "color", null, "icon", null);
    }

    private static JsonArray resources(SustainabilityAudit.Result result, JsonObject audit) {
        JsonArray entries = new JsonArray();
        for (JsonElement element : audit.getAsJsonArray("resources")) {
            JsonObject entry = element.getAsJsonObject().deepCopy();
            String id = entry.get("id").getAsString();
            JsonObject metadata = object(entry.get("annotation"));
            entry.addProperty("name", id);
            entry.add("resource_classes", singleton(metadata.get("resource_class")));
            entry.add("byproduct_classes", singleton(metadata.get("side_stream_class")));
            for (String field : List.of("first_use_tier", "first_renewable_tier", "canonical_route",
                    "future_consumers", "handling", "originating_catalyst", "uncertainty")) {
                entry.add(field, copy(metadata.get(field)));
            }
            entry.add("description", copy(metadata.get("uncertainty")));
            entry.add("mandatory_by_tier", entry.get("mandatory").deepCopy());
            boolean mandatory = false;
            boolean unknown = false;
            for (JsonElement value : entry.getAsJsonObject("mandatory").asMap().values()) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                    mandatory |= value.getAsBoolean();
                } else {
                    unknown = true;
                }
            }
            entry.add("mandatory_for_progression", mandatory ? new JsonPrimitive(true)
                    : unknown ? JsonNull.INSTANCE : new JsonPrimitive(false));
            JsonObject proofs = new JsonObject();
            Set<String> renewableRoutes = new TreeSet<>();
            Set<String> producers = strings(entry.getAsJsonArray("producers"));
            for (var tier : result.tiers().values()) {
                if (tier.unknown() != null) {
                    proofs.add("T" + tier.tier(), JsonNull.INSTANCE);
                } else {
                    Resource resource = resource(result.authorities(), id);
                    proofs.add("T" + tier.tier(), row("available", tier.available().contains(resource),
                            "renewable", tier.renewable().contains(resource),
                            "mandatory", tier.mandatory().contains(resource), "recurring", tier.recurring().contains(resource)));
                    tier.renewableRoutes().stream().filter(producers::contains).forEach(renewableRoutes::add);
                }
            }
            entry.add("model_evidence_by_tier", JSON.toJsonTree(proofs));
            entry.add("renewable_routes", JSON.toJsonTree(renewableRoutes));
            JsonArray terminals = new JsonArray();
            audit.getAsJsonArray("terminals").forEach(terminal -> {
                if (terminal.getAsJsonObject().get("resource").getAsString().equals(id)) {
                    terminals.add(terminal.deepCopy());
                }
            });
            entry.add("terminal_sources", terminals);
            JsonArray bootstrap = new JsonArray();
            result.annotations().ledger().values().forEach(plan -> {
                EntryProfile profile = plan.entryProfile();
                if (profile != null) {
                    profile.playerProvided().forEach((resource, amount) -> {
                        if (resource.key().equals(id)) {
                            bootstrap.add(row("resource_id", id, "amount", amount, "tier", plan.tier(),
                                    "profile_id", profile.id(), "source", profile.source(),
                                    "scope", "Conditional player custody, not a guaranteed gift or renewable source."));
                        }
                    });
                }
            });
            entry.add("bootstrap_source", bootstrap.isEmpty() ? JsonNull.INSTANCE : bootstrap);
            JsonObject recycling = object(metadata.get("recycling"));
            entry.add("recycling_routes", singleton(recycling.get("route")));
            entry.add("tag_resolution", find(audit.getAsJsonArray("tags"), id));
            entry.add("finding_ids", findingIds(result, id));
            entry.add("validation_status", findingStatus(result, id));
            String canonical = string(metadata.get("canonical_route"));
            Route route = canonical == null ? null : result.authorities().routes().get(canonical);
            entry.add("automation", route == null ? JsonNull.INSTANCE
                    : row("route_id", route.id(), "automatable", route.automatable(), "mod_owned", route.modOwned()));
            entry.add("source_path", JSON.toJsonTree(sourcePath(result.authorities(),
                    route == null ? firstSource(entry.getAsJsonArray("producer_provenance")) : route.source())));
            for (String field : List.of("input_dependencies", "output_rate", "vanilla_dependency", "deadlock_risk",
                    "catalyst_behavior", "icon")) {
                entry.add(field, JsonNull.INSTANCE);
            }
            entries.add(entry);
        }
        return entries;
    }

    private static JsonArray routes(SustainabilityAudit.Result result, JsonObject audit) {
        JsonArray entries = new JsonArray();
        for (JsonElement element : audit.getAsJsonArray("routes")) {
            JsonObject entry = element.getAsJsonObject().deepCopy();
            String id = entry.get("id").getAsString();
            Route route = result.authorities().routes().get(id);
            RoutePolicy policy = result.annotations().routes().get(id);
            JsonArray inputs = amounts(route.inputs(), true, null);
            amounts(route.catalysts(), false, "catalyst").forEach(inputs::add);
            JsonArray outputs = new JsonArray();
            route.outputs().forEach((resource, amount) -> {
                OutputRole declared = policy == null ? null : policy.outputRoles().get(resource);
                ResourceMetadata metadata = result.annotations().resources().get(resource);
                String role = declared == null ? null : switch (declared) {
                    case PRIMARY -> "primary";
                    case SIDE_STREAM -> "byproduct";
                    case DISCHARGED -> "discharged";
                };
                outputs.add(row("resource_id", resource.key(), "amount", amount, "role", role,
                        "declared_role", lower(declared),
                        "declared_side_stream_class", metadata == null ? null : lower(metadata.sideStreamClass()),
                        "discharged", route.discharged().contains(resource)));
            });
            boolean selected = result.annotations().resources().values().stream()
                    .anyMatch(metadata -> id.equals(metadata.canonicalRoute()));
            entry.addProperty("name", id);
            entry.add("inputs", inputs);
            entry.add("outputs", outputs);
            entry.add("tier", copy(entry.get("unlock_tier")));
            entry.add("canonical", policy != null && policy.optional() ? new JsonPrimitive(false)
                    : selected ? new JsonPrimitive(true) : JsonNull.INSTANCE);
            boolean recycling = result.annotations().resources().values().stream().anyMatch(metadata ->
                    metadata.recycling() != null && id.equals(metadata.recycling().route()));
            entry.addProperty("origin", recycling ? "recycle" : lower(route.authority()));
            entry.add("machine_id", JSON.toJsonTree(route.family() != null && result.annotations().machines().containsKey(route.family())
                    ? route.family() : null));
            entry.add("source_path", JSON.toJsonTree(sourcePath(result.authorities(), route.source())));
            entry.add("validation_status", findingStatus(result, id));
            entry.add("finding_ids", findingIds(result, id));
            entries.add(entry);
        }
        return entries;
    }

    private static JsonArray tiers(SustainabilityAudit.Result result, JsonObject audit) {
        JsonArray entries = new JsonArray();
        for (JsonElement element : audit.getAsJsonArray("tiers")) {
            JsonObject entry = element.getAsJsonObject().deepCopy();
            int tier = entry.get("tier").getAsInt();
            boolean assessed = entry.get("unknown").isJsonNull();
            entry.addProperty("id", tier);
            for (String field : List.of("name", "purpose", "implementation_state", "acceptance_state",
                    "improved_resources", "vanilla_dependencies_eliminated", "manual_checks")) {
                entry.add(field, JsonNull.INSTANCE);
            }
            entry.add("entry", copy(entry.get("entry_profile")));
            entry.add("exit", assessed ? row("resource_id", entry.get("exit_objective"),
                    "production_witness", entry.get("exit_production"), "acceptance_state", null) : JsonNull.INSTANCE);
            entry.add("bootstrap_resources", assessed
                    ? quantitiesAsRows(object(entry.get("entry_profile")).get("player_provided")) : JsonNull.INSTANCE);
            entry.add("deadlock_protection", assessed ? JSON.toJsonTree(List.of(
                    row("construction_reserves", entry.get("construction_reserves")),
                    row("local_work_reserve", entry.get("manual_work_reserve")),
                    row("retained_inventory", entry.get("retained_process_and_circulation_inventory")))) : JsonNull.INSTANCE);
            entry.add("resource_ids", assessed ? entry.get("mandatory").deepCopy() : JsonNull.INSTANCE);
            entry.add("machine_ids", assessed ? JSON.toJsonTree(result.annotations().machines().values().stream()
                    .filter(machine -> machine.unlockTier() == tier).map(Machine::family).toList()) : JsonNull.INSTANCE);
            entry.add("quest_ids", assessed ? copy(entry.get("selected_quests")) : JsonNull.INSTANCE);
            entry.add("useful_byproducts", assessed ? JSON.toJsonTree(result.annotations().resources().entrySet().stream()
                    .filter(value -> Integer.valueOf(tier).equals(value.getValue().firstUseTier())
                            && value.getValue().sideStreamClass() == SideStreamClass.BYPRODUCT_CURRENT)
                    .map(value -> value.getKey().key()).toList()) : JsonNull.INSTANCE);
            entry.add("finite_optional", assessed ? JSON.toJsonTree(result.annotations().resources().entrySet().stream()
                    .filter(value -> Integer.valueOf(tier).equals(value.getValue().firstUseTier())
                            && value.getValue().resourceClass() == ResourceClass.FINITE_OPTIONAL)
                    .map(value -> value.getKey().key()).toList()) : JsonNull.INSTANCE);
            entry.add("recycling_routes", assessed ? JSON.toJsonTree(result.annotations().resources().values().stream()
                    .filter(value -> value.recycling() != null && value.recycling().route() != null)
                    .map(value -> value.recycling().route()).filter(id -> {
                        RoutePolicy policy = result.annotations().routes().get(id);
                        return policy != null && Integer.valueOf(tier).equals(policy.unlockTier());
                    }).distinct().sorted().toList()) : JsonNull.INSTANCE);
            entry.add("blockers", assessed ? JsonNull.INSTANCE : JSON.toJsonTree(List.of(row(
                    "unknown", entry.get("unknown"), "scope", "Unassessed ledger; not a demonstrated future gameplay failure."))));
            if (!assessed) {
                entry.add("renewable_routes", JsonNull.INSTANCE);
            }
            entry.add("source_path", JsonNull.INSTANCE);
            entries.add(entry);
        }
        return entries;
    }

    private static JsonArray machines(SustainabilityAudit.Result result) {
        JsonArray entries = new JsonArray();
        for (Machine machine : result.annotations().machines().values()) {
            entries.add(row("id", machine.family(), "name", machine.family(), "family", machine.family(),
                    "tier", machine.unlockTier(), "construction", machine.construction().key(),
                    "resource_ids", List.of(machine.construction().key()), "recipe_ids", result.authorities().routes().values()
                            .stream().filter(route -> machine.family().equals(route.family())).map(Route::id).toList(),
                    "kind", null, "conditions", null, "automation", null, "persistence", null, "preview", null,
                    "source_path", null, "validation_status", null));
        }
        return entries;
    }

    private static JsonArray quests(SustainabilityAudit.Result result, JsonObject audit) {
        JsonArray entries = new JsonArray();
        for (JsonElement element : audit.getAsJsonArray("objectives")) {
            JsonObject entry = element.getAsJsonObject().deepCopy();
            String id = entry.get("id").getAsString();
            String rewardId = string(entry.get("reward_route"));
            Route reward = rewardId == null ? null : result.authorities().routes().get(rewardId);
            RoutePolicy policy = reward == null ? null : result.annotations().routes().get(reward.id());
            entry.addProperty("name", id);
            entry.add("reward", reward == null ? JsonNull.INSTANCE : amounts(reward.outputs(), null, null));
            entry.add("optional", policy == null ? JsonNull.INSTANCE : new JsonPrimitive(policy.optional()));
            entry.add("source_path", JSON.toJsonTree(sourcePath(result.authorities(), string(entry.get("source")))));
            entry.add("line", JsonNull.INSTANCE);
            entry.add("teaches", JsonNull.INSTANCE);
            entry.add("validation_status", findingStatus(result, id));
            entries.add(entry);
        }
        return entries;
    }

    private static JsonArray findings(SustainabilityAudit.Result result, JsonObject audit) {
        JsonArray entries = new JsonArray();
        Set<String> resources = new TreeSet<>();
        result.authorities().resources().forEach(resource -> resources.add(resource.key()));
        for (JsonElement element : audit.getAsJsonArray("findings")) {
            JsonObject entry = element.getAsJsonObject().deepCopy();
            String id = entry.get("fingerprint").getAsString();
            String subject = entry.get("subject").getAsString();
            String detail = entry.get("detail").getAsString();
            entry.addProperty("id", id);
            entry.add("resource_id", JSON.toJsonTree(resources.contains(subject) ? subject : null));
            entry.add("tier", detail.matches("T(?:[0-9]|1[0-5])")
                    ? new JsonPrimitive(Integer.parseInt(detail.substring(1))) : JsonNull.INSTANCE);
            entry.addProperty("waived", entry.get("status").getAsString().equals("waived_debt"));
            JsonElement decision = JsonNull.INSTANCE;
            for (JsonElement candidate : audit.getAsJsonObject("active_t0_conditional_gate").getAsJsonArray("finding_decisions")) {
                if (candidate.getAsJsonObject().get("fingerprint").getAsString().equals(id)) {
                    decision = candidate.deepCopy();
                    break;
                }
            }
            entry.add("active_t0_decision", decision);
            DebtManifest.Entry debt = result.debt().entries().get(id);
            entry.add("remediation", JSON.toJsonTree(debt == null ? null : debt.remediation()));
            entry.add("source_path", JSON.toJsonTree(entry.getAsJsonArray("sources").isEmpty() ? null
                    : sourcePath(result.authorities(), entry.getAsJsonArray("sources").get(0).getAsString())));
            entries.add(entry);
        }
        return entries;
    }

    private static JsonArray amounts(Map<Resource, Long> amounts, Boolean consumed, String role) {
        JsonArray entries = new JsonArray();
        amounts.forEach((resource, amount) -> entries.add(row("resource_id", resource.key(), "amount", amount,
                "consumed", consumed, "role", role)));
        return entries;
    }

    private static JsonElement quantitiesAsRows(JsonElement quantities) {
        if (quantities == null || !quantities.isJsonObject()) {
            return JsonNull.INSTANCE;
        }
        JsonArray entries = new JsonArray();
        quantities.getAsJsonObject().entrySet().forEach(entry -> entries.add(
                row("resource_id", entry.getKey(), "amount", entry.getValue())));
        return entries;
    }

    private static JsonArray findingIds(SustainabilityAudit.Result result, String subject) {
        return JSON.toJsonTree(result.findings().findings().stream().filter(finding -> finding.subject().equals(subject))
                .map(AuditFinding::fingerprint).toList()).getAsJsonArray();
    }

    private static JsonElement findingStatus(SustainabilityAudit.Result result, String subject) {
        return result.findings().findings().stream().filter(finding -> finding.subject().equals(subject))
                .map(AuditFinding::severity).min(Enum::compareTo)
                .<JsonElement>map(severity -> new JsonPrimitive(lower(severity))).orElse(JsonNull.INSTANCE);
    }

    private static String sourcePath(Snapshot snapshot, String reference) {
        if (reference == null) {
            return null;
        }
        if (snapshot.inputHashes().containsKey(reference) && reference.indexOf(':') < 0) {
            return reference;
        }
        String type = reference.split("[.#:]", 2)[0];
        List<String> matches = new ArrayList<>();
        snapshot.inputHashes().keySet().forEach(source -> {
            String path = source.startsWith(AuthorityAdapters.IMPLEMENTATION_SOURCE_PREFIX)
                    ? source.substring(AuthorityAdapters.IMPLEMENTATION_SOURCE_PREFIX.length()) : source;
            if (path.indexOf(':') < 0 && path.endsWith("/" + type + ".java")) {
                matches.add(path);
            }
        });
        return matches.stream().distinct().count() == 1 ? matches.get(0) : null;
    }

    private static String firstSource(JsonArray references) {
        return references.isEmpty() ? null : string(references.get(0).getAsJsonObject().get("source"));
    }

    private static Resource resource(Snapshot snapshot, String key) {
        return snapshot.resources().stream().filter(resource -> resource.key().equals(key)).findFirst().orElseThrow();
    }

    private static JsonElement find(JsonArray entries, String id) {
        for (JsonElement entry : entries) {
            if (entry.getAsJsonObject().get("id").getAsString().equals(id)) {
                return entry.deepCopy();
            }
        }
        return JsonNull.INSTANCE;
    }

    private static Set<String> strings(JsonArray entries) {
        Set<String> values = new TreeSet<>();
        entries.forEach(entry -> values.add(entry.getAsString()));
        return values;
    }

    private static JsonElement singleton(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        JsonArray entries = new JsonArray();
        entries.add(value.deepCopy());
        return entries;
    }

    private static JsonObject object(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static String string(JsonElement value) {
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static JsonElement copy(JsonElement value) {
        return value == null ? JsonNull.INSTANCE : value.deepCopy();
    }

    private static String lower(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    private static JsonObject row(Object... fields) {
        JsonObject entry = new JsonObject();
        for (int index = 0; index < fields.length; index += 2) {
            entry.add((String) fields[index], JSON.toJsonTree(fields[index + 1]));
        }
        return entry;
    }

    private static JsonElement exactNumbers(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject exact = new JsonObject();
            value.getAsJsonObject().entrySet().forEach(entry -> exact.add(entry.getKey(), exactNumbers(entry.getValue())));
            return exact;
        }
        if (value.isJsonArray()) {
            JsonArray exact = new JsonArray();
            value.getAsJsonArray().forEach(entry -> exact.add(exactNumbers(entry)));
            return exact;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            BigDecimal number = value.getAsBigDecimal();
            if (number.stripTrailingZeros().scale() <= 0 && number.abs().compareTo(MAX_SAFE_INTEGER) > 0) {
                return new JsonPrimitive(number.toPlainString());
            }
        }
        return value.deepCopy();
    }
}
