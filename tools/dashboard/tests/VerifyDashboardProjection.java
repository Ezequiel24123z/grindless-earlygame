package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/** Exhaustive real-result projection checks plus explicitly synthetic serializer/privacy counterexamples. */
public final class VerifyDashboardProjection {
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping()
            .setPrettyPrinting().create();
    private static int assertions;

    private VerifyDashboardProjection() {
    }

    static int verify(SustainabilityAudit.Result result, JsonObject data, String revision) {
        assertions = 0;
        JsonObject audit = JSON.toJsonTree(AuditReports.document(result, revision, true)).getAsJsonObject();
        eq("schema", 1, data.get("schema_version").getAsInt());
        eq("actual revision", revision, data.getAsJsonObject("context").get("commit").getAsString());
        eq("canonical resource identities", keys(result.authorities().resources()), index(data, "resources").keySet());
        eq("canonical route identities", result.authorities().routes().keySet(), index(data, "routes").keySet());
        eq("canonical objective identities", result.authorities().objectives().keySet(), index(data, "quests").keySet());
        eq("declared machine identities", result.annotations().machines().keySet(), index(data, "machines").keySet());
        eq("complete finding identities", result.findings().findings().stream().map(AuditFinding::fingerprint)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new)), index(data, "findings").keySet());
        eq("active gate, including scope and every finding decision", audit.get("active_t0_conditional_gate"),
                data.get("active_t0_conditional_gate"));
        eq("actual counts, not hard-coded reference counts", audit.get("summary"), data.get("summary"));
        eq("full tag resolutions", audit.get("tags"), data.get("tags"));
        eq("full terminal sources", audit.get("terminals"), data.get("terminals"));
        eq("no invented deliverable denominator", JsonNull.INSTANCE,
                data.getAsJsonObject("validation").get("required_deliverables"));
        eq("CI not inferred from a local exporter", JsonNull.INSTANCE, data.getAsJsonObject("validation").get("ci"));
        eq("manual acceptance not inferred from model", JsonNull.INSTANCE,
                data.getAsJsonObject("validation").get("manual_acceptance"));
        Set<String> expectedClasses = new TreeSet<>();
        for (ResourceClass value : ResourceClass.values()) expectedClasses.add(lower(value));
        for (SideStreamClass value : SideStreamClass.values()) expectedClasses.add(lower(value));
        eq("only approved taxonomy ids", expectedClasses, index(data, "classifications").keySet());
        Map<String, JsonObject> resources = index(data, "resources");
        Map<String, JsonObject> auditResources = index(audit, "resources");
        for (Resource resource : result.authorities().resources()) {
            JsonObject entry = resources.get(resource.key());
            JsonObject source = auditResources.get(resource.key());
            ResourceMetadata metadata = result.annotations().resources().get(resource);
            eq("registry identity " + resource.key(), resource.registry(), entry.get("registry").getAsString());
            eq("physical rating " + resource.key(), resource.rating(), entry.get("matrix_rating").getAsInt());
            eq("producers " + resource.key(), source.get("producers"), entry.get("producers"));
            eq("consumers " + resource.key(), source.get("current_consumers"), entry.get("current_consumers"));
            eq("producer certainty " + resource.key(), source.get("producer_provenance"), entry.get("producer_provenance"));
            eq("consumer certainty " + resource.key(), source.get("consumer_provenance"), entry.get("consumer_provenance"));
            eq("mandatory evidence " + resource.key(), source.get("mandatory"), entry.get("mandatory_by_tier"));
            eq("resource class " + resource.key(), metadata == null ? null : lower(metadata.resourceClass()),
                    singleton(entry.get("resource_classes")));
            eq("side-stream class " + resource.key(), metadata == null ? null : lower(metadata.sideStreamClass()),
                    singleton(entry.get("byproduct_classes")));
            eq("first use " + resource.key(), metadata == null ? null : metadata.firstUseTier(),
                    nullableInt(entry.get("first_use_tier")));
            eq("first renewal " + resource.key(), metadata == null ? null : metadata.firstRenewableTier(),
                    nullableInt(entry.get("first_renewable_tier")));
            if (metadata == null) {
                eq("unknown handling stays unknown " + resource.key(), JsonNull.INSTANCE, entry.get("handling"));
                eq("unknown future consumers stay unknown " + resource.key(), JsonNull.INSTANCE, entry.get("future_consumers"));
            } else {
                eq("declared handling " + resource.key(), source.getAsJsonObject("annotation").get("handling"), entry.get("handling"));
                eq("declared future consumers " + resource.key(),
                        source.getAsJsonObject("annotation").get("future_consumers"), entry.get("future_consumers"));
            }
            boolean mandatory = false;
            boolean unknown = false;
            for (var tier : result.tiers().values()) {
                JsonElement proof = entry.getAsJsonObject("model_evidence_by_tier").get("T" + tier.tier());
                if (tier.unknown() != null) {
                    unknown = true;
                    eq("unassessed tier is not false renewal " + resource.key(), JsonNull.INSTANCE, proof);
                } else {
                    mandatory |= tier.mandatory().contains(resource);
                    eq("available membership " + resource.key(), tier.available().contains(resource),
                            proof.getAsJsonObject().get("available").getAsBoolean());
                    eq("renewable membership " + resource.key(), tier.renewable().contains(resource),
                            proof.getAsJsonObject().get("renewable").getAsBoolean());
                }
            }
            eq("global mandatory tri-state " + resource.key(), mandatory ? new JsonPrimitive(true)
                    : unknown ? JsonNull.INSTANCE : new JsonPrimitive(false), entry.get("mandatory_for_progression"));
        }
        Map<String, JsonObject> routes = index(data, "routes");
        for (Route route : result.authorities().routes().values()) {
            JsonObject entry = routes.get(route.id());
            eq("gross consumed amounts " + route.id(), route.inputs(), amounts(entry.getAsJsonArray("inputs"), resources, true));
            eq("retained non-consumed amounts " + route.id(), route.catalysts(),
                    amounts(entry.getAsJsonArray("inputs"), resources, false));
            eq("gross output amounts " + route.id(), route.outputs(), amounts(entry.getAsJsonArray("outputs"), resources, null));
            eq("exact duration " + route.id(), route.durationTicks(), entry.get("duration_ticks").getAsLong());
            eq("exact power " + route.id(), route.fuPerTick(), entry.get("fu_per_tick").getAsLong());
            eq("actual automation " + route.id(), route.automatable(), entry.get("automatable").getAsBoolean());
            eq("actual ownership " + route.id(), route.modOwned(), entry.get("mod_owned").getAsBoolean());
            eq("actual one-shot " + route.id(), route.oneShot(), entry.get("one_shot").getAsBoolean());
            RoutePolicy policy = result.annotations().routes().get(route.id());
            eq("unknown tier not T0 " + route.id(), policy == null ? null : policy.unlockTier(), nullableInt(entry.get("tier")));
            eq("conditions " + route.id(), JSON.toJsonTree(route.conditions()), entry.get("conditions"));
            eq("prerequisites " + route.id(), JSON.toJsonTree(route.prerequisites()), entry.get("prerequisites"));
            for (JsonElement element : entry.getAsJsonArray("outputs")) {
                JsonObject output = element.getAsJsonObject();
                Resource resource = find(result, output.get("resource_id").getAsString());
                OutputRole declared = policy == null ? null : policy.outputRoles().get(resource);
                eq("declared role " + route.id(), lower(declared), nullableString(output.get("declared_role")));
                eq("no invented output role " + route.id(), declared == null, output.get("role").isJsonNull());
                eq("actual discharge " + route.id(), route.discharged().contains(resource), output.get("discharged").getAsBoolean());
            }
        }
        Map<String, JsonObject> quests = index(data, "quests");
        for (Objective objective : result.authorities().objectives().values()) {
            JsonObject entry = quests.get(objective.id());
            eq("physical quest subject", objective.subject() == null ? null : objective.subject().key(), nullableString(entry.get("subject")));
            eq("actual quest count", objective.count(), entry.get("count").getAsLong());
            eq("all quest dependencies", JSON.toJsonTree(objective.requires()), entry.get("requires"));
            Route reward = result.authorities().routes().get(objective.rewardRoute());
            if (reward != null) {
                eq("exact actual rewards", reward.outputs(), amounts(entry.getAsJsonArray("reward"), resources, null));
            } else {
                eq("unreported reward", JsonNull.INSTANCE, entry.get("reward"));
            }
        }
        eq("all sixteen results retained", result.tiers().size(), data.getAsJsonArray("tiers").size());
        for (JsonElement element : data.getAsJsonArray("tiers")) {
            JsonObject entry = element.getAsJsonObject();
            var tier = result.tiers().get(entry.get("id").getAsInt());
            eq("tier diagnosis retained", tier.unknown(), nullableString(entry.get("unknown")));
            eq("Tier acceptance never inferred", JsonNull.INSTANCE, entry.get("acceptance_state"));
            eq("implementation never inferred from model", JsonNull.INSTANCE, entry.get("implementation_state"));
            if (tier.unknown() != null) {
                eq("unknown entry not empty accepted kit", JsonNull.INSTANCE, entry.get("entry"));
                eq("unknown routes not a passed empty tier", JsonNull.INSTANCE, entry.get("renewable_routes"));
                eq("unknown resources not a zero requirement", JsonNull.INSTANCE, entry.get("resource_ids"));
            }
        }
        Map<String, JsonObject> projectedFindings = index(data, "findings");
        for (AuditFinding finding : result.findings().findings()) {
            JsonObject entry = projectedFindings.get(finding.fingerprint());
            eq("exact finding severity", lower(finding.severity()), entry.get("severity").getAsString());
            eq("exact finding disposition", lower(finding.disposition()), entry.get("status").getAsString());
            eq("exact finding message", finding.message(), entry.get("message").getAsString());
            eq("exact finding sources", JSON.toJsonTree(finding.sources()), entry.get("sources"));
        }
        DashboardProjection.requirePublic(data);
        assertions++;
        return assertions;
    }

    static int counterexamples(SustainabilityAudit.Result result, DashboardProjection.Context context) {
        int before = assertions;
        String original = AuditReports.json(result, context.commit(), true);
        eq("exported tree and JSON are one report", original,
                JSON.toJson(AuditReports.document(result, context.commit(), true)) + "\n");
        boolean protectedTree = false;
        try {
            AuditReports.document(result, context.commit(), true).put("fixture", "must not mutate");
        } catch (UnsupportedOperationException expected) {
            protectedTree = true;
        }
        eq("report top-level view is protected", true, protectedTree);
        JsonObject projected = DashboardProjection.document(result, context);
        projected.getAsJsonArray("resources").get(0).getAsJsonObject().addProperty("id", "fixture:not_gameplay");
        projected.getAsJsonArray("routes").get(0).getAsJsonObject().getAsJsonArray("inputs").add("fixture:not_gameplay");
        eq("projection cannot mutate result/report", original, AuditReports.json(result, context.commit(), true));
        JsonObject extremes = new JsonObject();
        extremes.addProperty("largest_safe", 9007199254740991L);
        extremes.addProperty("first_unsafe", 9007199254740992L);
        extremes.addProperty("long_max", Long.MAX_VALUE);
        extremes.addProperty("long_min", Long.MIN_VALUE);
        extremes.add("unknown", JsonNull.INSTANCE);
        JsonObject exact = JsonParser.parseString(DashboardProjection.json(extremes)).getAsJsonObject();
        eq("safe native integer stays numeric", true, exact.get("largest_safe").getAsJsonPrimitive().isNumber());
        for (String field : Set.of("first_unsafe", "long_max", "long_min")) {
            eq("unsafe native integer becomes exact decimal text", true, exact.get(field).getAsJsonPrimitive().isString());
            eq("all integer digits survive", extremes.get(field).getAsString(), exact.get(field).getAsString());
        }
        eq("null stays null", JsonNull.INSTANCE, exact.get("unknown"));
        eq("serialization does not mutate numeric input", true, extremes.get("long_max").getAsJsonPrimitive().isNumber());
        for (String privateValue : Set.of("C:\\Users\\private\\example", "/home/private/example",
                "fixture@example.invalid", "Bearer fixture-credential", "password=fixture-not-a-secret",
                "[12:34:56] fixture raw log", "https://fixture:credential@example.invalid/repository")) {
            boolean rejected = false;
            try {
                DashboardProjection.requirePublic(new JsonPrimitive(privateValue));
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            eq("private fixture rejected without publishing it", true, rejected);
        }
        return assertions - before;
    }

    private static Map<Resource, Long> amounts(JsonArray entries, Map<String, JsonObject> resources, Boolean consumed) {
        Map<Resource, Long> amounts = new TreeMap<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            if (consumed != null && !Objects.equals(consumed, entry.get("consumed").getAsBoolean())) continue;
            String id = entry.get("resource_id").getAsString();
            eq("amount resource reference resolves", true, resources.containsKey(id));
            JsonObject resource = resources.get(id);
            Resource identity = new Resource(Kind.valueOf(resource.get("kind").getAsString().toUpperCase(java.util.Locale.ROOT)),
                    registryId(resource), resource.get("matrix_rating").getAsInt(), resource.get("registry").getAsString());
            eq("resource reference retains full identity", id, identity.key());
            eq("no duplicate amount entry", null, amounts.put(identity, entry.get("amount").getAsLong()));
        }
        return amounts;
    }

    private static String registryId(JsonObject resource) {
        String key = resource.get("id").getAsString();
        String id = key.substring(key.indexOf(':') + 1);
        return resource.get("matrix_rating").getAsInt() == 0 ? id : id.substring(0, id.lastIndexOf(" [F"));
    }

    private static Map<String, JsonObject> index(JsonObject data, String field) {
        Map<String, JsonObject> values = new TreeMap<>();
        for (JsonElement element : data.getAsJsonArray(field)) {
            JsonObject entry = element.getAsJsonObject();
            String id = entry.get("id").getAsString();
            eq("unique " + field + " id", null, values.put(id, entry));
        }
        return values;
    }

    private static Set<String> keys(Set<Resource> resources) {
        Set<String> values = new TreeSet<>();
        resources.forEach(resource -> values.add(resource.key()));
        return values;
    }

    private static Resource find(SustainabilityAudit.Result result, String key) {
        return result.authorities().resources().stream().filter(resource -> resource.key().equals(key)).findFirst().orElseThrow();
    }

    private static Integer nullableInt(JsonElement value) {
        return value == null || value.isJsonNull() ? null : value.getAsInt();
    }

    private static String nullableString(JsonElement value) {
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static String singleton(JsonElement value) {
        return value.isJsonNull() ? null : value.getAsJsonArray().get(0).getAsString();
    }

    private static String lower(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static void eq(String message, Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
