package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/** Deterministic schema-1 JSON and human reports; generated outputs are not source authorities. */
public final class AuditReports {
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Gson NORMALIZED = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    private AuditReports() {
    }

    /** Hashes of the exact UTF-8 output bytes, suitable for independent repeat-run comparison. */
    public record Hashes(String json, String markdown) {
    }

    /**
     * Writes the two reports without timestamps, absolute paths or a fabricated certification.
     *
     * @param directory generated report directory
     * @param result audit facts and findings
     * @param revision source revision label, not an identity for uncommitted source or class bytes
     * @param reportOnly whether this invocation is a report-only inventory, not an enforced audit
     * @return hashes of the bytes written
     * @throws IOException if report output fails
     */
    public static Hashes write(Path directory, SustainabilityAudit.Result result,
                               String revision, boolean reportOnly) throws IOException {
        byte[] json = json(result, revision, reportOnly).getBytes(StandardCharsets.UTF_8);
        byte[] markdown = markdown(result, revision, reportOnly).getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(directory);
        Files.write(directory.resolve("audit.json"), json);
        Files.write(directory.resolve("audit.md"), markdown);
        return new Hashes(AuthorityAdapters.sha256(json), AuthorityAdapters.sha256(markdown));
    }

    /** Returns schema-1 audit JSON in deterministic key and collection order. */
    public static String json(SustainabilityAudit.Result result, String revision, boolean reportOnly) {
        Snapshot snapshot = result.authorities();
        AuthorityAdapters.ImplementationIdentity implementation = AuthorityAdapters.implementationIdentity();
        Map<String, Object> document = row(
                "schema_version", 1,
                "source_revision", revision,
                "source_state", "working_tree_content_hashed_commit_cleanliness_unverified",
                "authority_sha256", authorityHash(snapshot),
                "authority_snapshot_sha256", snapshotHash(snapshot),
                "authority_sources_sha256", sourceHash(sourceHashes(snapshot, false)),
                "implementation_sha256", implementation.sha256(),
                "implementation_state", implementation.unresolved().isEmpty()
                        ? "resolved_s1_class_resources_commit_cleanliness_unverified"
                        : "incomplete_s1_class_resources_commit_cleanliness_unverified",
                "implementation_hashes", implementation.classHashes(),
                "implementation_unresolved", implementation.unresolved(),
                "implementation_source_sha256", sourceHash(sourceHashes(snapshot, true)),
                "implementation_source_hashes", sourceHashes(snapshot, true),
                "tag_resolution_scope", tagResolutionScope(snapshot),
                "resolved_pack_hashes", packHashes(snapshot),
                "registry_binding_hashes", registryHashes(snapshot),
                "registry_binding_conditions", registryConditions(snapshot),
                "mode", reportOnly ? (canonicalPack(snapshot) ? "report_only_s2_canonical_inventory" : "report_only_s1")
                        + (hasAnnotations(result) ? "_partial_annotations" : "_no_annotations") : "enforced",
                "sustainability_pass", !reportOnly && result.sustainabilityPass(),
                "ratchet_pass", result.findings().ratchetPass(),
                "failure_visible_debt", result.findings().waivedDebt(),
                "summary", summary(result),
                "input_hashes", snapshot.inputHashes(),
                "debt", debt(result.debt()));
        List<Object> resources = new ArrayList<>();
        for (Resource resource : snapshot.resources()) {
            List<Map<String, Object>> producerProvenance = relationships(snapshot, resource, true);
            List<Map<String, Object>> consumerProvenance = relationships(snapshot, resource, false);
            Set<String> producers = unconditionalRoutes(producerProvenance);
            Set<String> consumers = unconditionalRoutes(consumerProvenance);
            Map<String, Object> mandatory = new TreeMap<>();
            for (var tier : result.tiers().values()) {
                mandatory.put("T" + tier.tier(), tier.unknown() == null ? tier.mandatory().contains(resource) : "unknown");
            }
            resources.add(row("id", resource.key(), "kind", lower(resource.kind()),
                    "registry", resource.registry(), "matrix_rating", resource.rating(),
                    "description_key", resource.descriptionKey(), "producers", producers, "current_consumers", consumers,
                    "producer_provenance", producerProvenance, "consumer_provenance", consumerProvenance,
                    "mandatory", mandatory, "annotation", metadata(result.annotations().resources().get(resource))));
        }
        document.put("resources", resources);
        List<Object> routes = new ArrayList<>();
        for (Route route : snapshot.routes().values()) {
            RoutePolicy policy = result.annotations().routes().get(route.id());
            Map<String, Object> roles = new TreeMap<>();
            if (policy != null) {
                policy.outputRoles().forEach((resource, role) -> roles.put(resource.key(), lower(role)));
            }
            routes.add(row("id", route.id(), "authority", lower(route.authority()), "source", route.source(),
                    "machine_family", route.family(), "inputs", quantitiesById(route.inputs()),
                    "net_consumption", quantitiesById(route.netInputs()), "catalysts", quantitiesById(route.catalysts()),
                    "non_consumed_requirements", quantitiesById(route.catalysts()),
                    "outputs", quantitiesById(route.outputs()), "discharged", keys(route.discharged()),
                    "duration_ticks", route.durationTicks(), "fu_per_tick", route.fuPerTick(),
                    "conditions", route.conditions(), "prerequisites", route.prerequisites(),
                    "one_shot", route.oneShot(), "mod_owned", route.modOwned(), "automatable", route.automatable(),
                    "unlock_tier", policy == null ? null : policy.unlockTier(),
                    "optional", policy == null ? null : policy.optional(),
                    "environmental_sources", policy == null ? null : policy.environmentalSources(),
                    "output_role_overrides", roles));
        }
        document.put("routes", routes);
        List<Object> tags = new ArrayList<>();
        snapshot.tags().forEach((resource, tag) -> tags.add(row("id", resource.key(), "complete", tag.complete(),
                "members", keys(tag.members()), "unresolved", tag.unresolved(), "sources", tag.sources())));
        document.put("tags", tags);
        List<Object> terminals = new ArrayList<>();
        snapshot.terminals().values().forEach(terminal -> terminals.add(row("id", terminal.id(),
                "resource", terminal.resource().key(), "kind", lower(terminal.kind()),
                "source_site", lower(terminal.site()), "automatable", terminal.automatable(),
                "mod_owned", terminal.modOwned(), "guaranteed", terminal.guaranteed(),
                "unlock_tier", terminal.unlockTier(), "justification", terminal.justification(), "source", terminal.source())));
        document.put("terminals", terminals);
        List<Object> objectives = new ArrayList<>();
        snapshot.objectives().values().forEach(objective -> objectives.add(row("id", objective.id(),
                "subject", objective.subject() == null ? null : objective.subject().key(),
                "count", objective.count(), "requires", objective.requires(), "reward_route", objective.rewardRoute(),
                "source", objective.source())));
        document.put("objectives", objectives);
        List<Object> machines = new ArrayList<>();
        result.annotations().machines().values().forEach(machine -> machines.add(row("family", machine.family(),
                "construction", machine.construction().key(), "unlock_tier", machine.unlockTier())));
        document.put("machines", machines);
        List<Object> loops = new ArrayList<>();
        result.annotations().loops().values().forEach(loop -> loops.add(row("id", loop.id(),
                "carrier", loop.carrier().key(), "member_routes", loop.memberRoutes(), "make_up_route", loop.makeUpRoute())));
        document.put("closed_loops", loops);
        document.put("committed_plans", result.annotations().plans());
        List<Object> tiers = new ArrayList<>();
        for (var tier : result.tiers().values()) {
            TierPlan plan = result.annotations().ledger().get(tier.tier());
            List<Object> reserves = tier.reserves().stream().map(reserve -> (Object) row(
                    "resource", reserve.resource().key(), "guaranteed", reserve.guaranteed(),
                    "player_provided", reserve.playerProvided(), "claimed_rewards", reserve.claimedRewards(),
                    "required", reserve.required(), "margin", reserve.margin(), "status", reserve.status())).toList();
            Map<String, Object> entry = plan == null || plan.entryProfile() == null
                    ? null : entryProfile(plan.entryProfile());
            SustainabilityAudit.WorkReserve work = tier.manualWork();
            SustainabilityAudit.GoalWitness goal = tier.exitProduction();
            tiers.add(row("tier", tier.tier(), "unknown", tier.unknown(),
                    "declared_roots", plan == null ? null : keys(plan.roots()),
                    "selected_quests", plan == null ? null : plan.requiredQuests(),
                    "exit_objective", plan == null || plan.exitObjective() == null ? null : plan.exitObjective().key(),
                    "mandatory", keys(tier.mandatory()), "recurring", keys(tier.recurring()),
                    "available", keys(tier.available()), "renewable", keys(tier.renewable()),
                    "available_routes", tier.availableRoutes(), "renewable_routes", tier.renewableRoutes(),
                    "entry_profile", entry, "entry_profile_sha256", entry == null ? null : hash(entry),
                    "manual_work_reserve", work == null ? null : row("terminal", work.terminal(),
                            "budget_actions", work.budgetActions(), "required_actions", work.requiredActions(),
                            "margin_actions", work.marginActions(), "status", work.status()),
                    "exit_production", goal == null ? null : row("resource", goal.resource().key(),
                            "route", goal.route(), "required_count", goal.requiredCount(),
                            "batch_output", goal.batchOutput(), "executed_batches", goal.executedBatches(),
                            "produced", goal.produced(), "status", goal.status()),
                    "retained_process_and_circulation_inventory", quantitiesById(tier.retainedInventory()),
                    "construction_reserves", reserves));
        }
        document.put("tiers", tiers);
        List<Object> findings = new ArrayList<>();
        for (AuditFinding finding : result.findings().findings()) {
            findings.add(row("fingerprint", finding.fingerprint(), "rule", finding.rule(),
                    "subject", finding.subject(), "detail", finding.detail(),
                    "severity", lower(finding.severity()), "status", lower(finding.disposition()),
                    "message", finding.message(), "sources", finding.sources()));
        }
        document.put("findings", findings);
        return JSON.toJson(document) + "\n";
    }

    private static Map<String, Object> entryProfile(EntryProfile profile) {
        ManualWork work = profile.manualWork();
        return row("id", profile.id(), "kind", "conditional_player_entry_not_a_world_or_quest_guarantee",
                "player_provided", quantitiesById(profile.playerProvided()),
                "manual_work_actions", profile.manualWorkActions(), "conditions", profile.conditions(),
                "source", profile.source(), "minimality", "sufficient_witness_no_global_optimality_claim",
                "manual_work", work == null ? null : row("terminal", work.terminal(), "tool", work.tool().key(),
                        "fu_per_action", work.fuPerAction(), "eligible_process_families", work.processFamilies(),
                        "scope", "eligible_batch_local_only_never_portable_energy_or_renewable_labour",
                        "rounding", "each_batch_rounds_actions_up_unused_local_charge_is_not_credited",
                        "source", work.source()));
    }

    /** Returns a human report with explicit evidence boundaries and all failure-visible findings. */
    public static String markdown(SustainabilityAudit.Result result, String revision, boolean reportOnly) {
        StringBuilder text = new StringBuilder("# Sustainability audit\n\n");
        Snapshot snapshot = result.authorities();
        if (reportOnly) {
            text.append(canonicalPack(snapshot) ? "**CANONICAL RESOURCE INVENTORY REPORT-ONLY (S2, "
                            : "**LIVE AUDIT REPORT-ONLY (S1, ")
                    .append(hasAnnotations(result) ? "partial annotations)**\n\n" : "no annotations)**\n\n")
                    .append("Synthetic rule checks are hard assertions, not live sustainability evidence. ")
                    .append("This invocation cannot certify a tier or make run-checks a sustainability pass.\n\n");
        } else {
            text.append("**Sustainability: ").append(result.sustainabilityPass() ? "PASS" : "NOT CERTIFIED")
                    .append("**\n\n");
        }
        AuthorityAdapters.ImplementationIdentity implementation = AuthorityAdapters.implementationIdentity();
        text.append("Audit schema: 1. Source revision label: `").append(revision).append("`.\n")
                .append("Authority content and source SHA-256: `").append(authorityHash(snapshot)).append("`.\n")
                .append("Normalized authority snapshot SHA-256: `").append(snapshotHash(snapshot)).append("`.\n")
                .append("Authority source manifest SHA-256: `").append(sourceHash(sourceHashes(snapshot, false))).append("`.\n")
                .append("Resolved S1 implementation class-resource SHA-256: `").append(implementation.sha256()).append("`.\n")
                .append("Working-tree S1 implementation source SHA-256: `")
                .append(sourceHash(sourceHashes(snapshot, true))).append("`.\n")
                .append("Unresolved implementation class resources: ").append(implementation.unresolved()).append(".\n")
                .append("Class resources identify the resolved core and available S1 check entry points, including helpers. ")
                .append("Source hashes identify files read, not proof that those sources built the loaded classes. ")
                .append("Commit cleanliness and publication are unverified; a revision label never identifies uncommitted code.\n\n")
                .append("Tag resolution scope: `").append(tagResolutionScope(snapshot)).append("`. ")
                .append("Local-only discovery and unordered classpath fallback are not canonical vanilla/Forge runtime resolution. ")
                .append(canonicalPack(snapshot)
                        ? "The standalone pack adapter reads the Gradle-resolved vanilla/Forge resources and local Grindless data. "
                                + "It evaluates referenced/local tags; other indexed definitions are not silently made progression roots. "
                                + "Append-only overlaps prove set-membership commutativity, not runtime pack priority. "
                                + "Every unresolved registry lookup or order-sensitive overlap remains incomplete. "
                                + "Source registration declarations inventory prototypes, not observed runtime registry membership. "
                                + "Registry binding conditions: " + registryConditions(snapshot) + ". "
                        : "")
                .append("Concrete producer lists expand only complete singleton outputs; multi-member or incomplete outputs ")
                .append("retain tag-qualified candidate provenance and uncertainty in `audit.json`. Complete tag inputs ")
                .append("accept their members. These relationships do not establish route availability or renewability.\n\n");
        text.append("`non_consumed_requirements` includes retained tools, chemical catalysts and installed structure parts. ")
                .append("The compatibility field `catalysts` describes the same quantities; it does not chemically classify casings. ")
                .append("A formed installation retains its parts once per family, separately from movable process seeds.\n\n");
        text.append("`automatable` is an explicit execution fact, independent of mod ownership. Manual crafts never ")
                .append("establish automatic renewable production. An exit goal must execute real batches from renewable ")
                .append("consumed supplies; goal stock or a gift does not replace that production witness.\n\n")
                .append("Conditional entry profiles describe normally acquired player custody, not world/quest gifts. ")
                .append("Their sufficient bill is derived, then replayed with finite material and local-work budgets; ")
                .append("no globally minimal starting kit is claimed. Crank actions fund only an eligible ProcessMachine ")
                .append("batch, require the actual tool and never become portable ENERGY or renewable labour. Native ")
                .append("working FU is retained for recurrent circulation separately from the startup labour budget.\n\n");
        var findings = result.findings();
        text.append("Active errors: **").append(findings.activeErrors()).append("**; waived_debt: **")
                .append(findings.waivedDebt()).append("**; warnings: **").append(findings.warnings())
                .append("**; unknowns: **").append(findings.unknowns()).append("**.\n\n")
                .append("Ratchet eligibility: ").append(findings.ratchetPass() ? "PASS" : "FAIL")
                .append(". Waived errors remain failures, and unknowns are not closure.\n\n")
                .append("## Tier facts\n\n| Tier | Ledger | Mandatory | Available | Renewable | Reserve findings |\n")
                .append("| --- | --- | ---: | ---: | ---: | ---: |\n");
        for (var tier : result.tiers().values()) {
            text.append("| T").append(tier.tier()).append(" | ").append(tier.unknown() == null ? "authored" : "unknown")
                    .append(" | ").append(tier.mandatory().size()).append(" | ").append(tier.available().size())
                    .append(" | ").append(tier.renewable().size()).append(" | ")
                    .append(tier.reserves().stream().filter(reserve ->
                            !Set.of("established", "conditional_established").contains(reserve.status())).count())
                    .append(" |\n");
        }
        text.append("\nCounts describe fixed-point facts only, not manual progression, client interaction, ")
                .append("saved-world compatibility, a gameplay percentage or Tier acceptance.\n\n")
                .append("## Findings by rule\n\n| Rule | Count |\n| --- | ---: |\n");
        countsPerRule(result).forEach((rule, count) -> text.append("| ").append(rule).append(" | ").append(count).append(" |\n"));
        text.append("\n## Exact findings\n\n| Fingerprint | Severity | Status | Evidence |\n| --- | --- | --- | --- |\n");
        for (AuditFinding finding : findings.findings()) {
            text.append("| `").append(escape(finding.fingerprint())).append("` | ").append(lower(finding.severity()))
                    .append(" | ").append(lower(finding.disposition())).append(" | ").append(escape(finding.message()));
            if (!finding.sources().isEmpty()) {
                text.append(" Sources: ").append(escape(String.join(", ", finding.sources())));
            }
            text.append(" |\n");
        }
        text.append("\n## Construction reserve\n\n")
                .append("| Tier | Resource | Guaranteed | Player-provided | Actually claimed rewards | Draw | Margin | Status |\n")
                .append("| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |\n");
        for (var tier : result.tiers().values()) {
            for (var reserve : tier.reserves()) {
                text.append("| T").append(tier.tier()).append(" | ").append(escape(reserve.resource().key()))
                        .append(" | ").append(reserve.guaranteed()).append(" | ").append(reserve.playerProvided())
                        .append(" | ").append(reserve.claimedRewards()).append(" | ").append(reserve.required())
                        .append(" | ").append(reserve.margin()).append(" | ").append(reserve.status()).append(" |\n");
            }
        }
        text.append("\nManual-world bootstrap is never an exact guarantee. Recipe conditions and rates, ")
                .append("source hashes, unresolved tag membership, objectives and all quantities are in `audit.json`.\n");
        return text.toString();
    }

    /** Returns exact finding counts, including unknowns and waived debt, ordered by rule id. */
    public static Map<String, Long> countsPerRule(SustainabilityAudit.Result result) {
        Map<String, Long> counts = new TreeMap<>();
        result.findings().findings().forEach(finding -> counts.merge(finding.rule(), 1L, Long::sum));
        return counts;
    }

    /** Returns an identity for evaluated authority facts and their separate source manifest. */
    public static String authorityHash(Snapshot snapshot) {
        return hash(row("snapshot_sha256", snapshotHash(snapshot),
                "authority_sources_sha256", sourceHash(sourceHashes(snapshot, false))));
    }

    /**
     * Returns a deterministic hash of every evaluated snapshot fact, independent of source
     * labels, source-file hashes and implementation identity.
     *
     * @param snapshot actual report-determining authority data
     * @return SHA-256 of the normalized UTF-8 authority projection
     */
    public static String snapshotHash(Snapshot snapshot) {
        List<Object> routes = new ArrayList<>();
        for (Route route : snapshot.routes().values()) {
            routes.add(row("id", route.id(), "authority", lower(route.authority()), "source", route.source(),
                    "family", route.family(), "inputs", quantitiesById(route.inputs()),
                    "catalysts", quantitiesById(route.catalysts()), "outputs", quantitiesById(route.outputs()),
                    "discharged", keys(route.discharged()), "duration_ticks", route.durationTicks(),
                    "fu_per_tick", route.fuPerTick(), "conditions", route.conditions(),
                    "prerequisites", route.prerequisites(), "one_shot", route.oneShot(), "mod_owned", route.modOwned(),
                    "automatable", route.automatable()));
        }
        List<Object> terminals = new ArrayList<>();
        snapshot.terminals().values().forEach(terminal -> terminals.add(row("id", terminal.id(),
                "resource", terminal.resource().key(), "kind", lower(terminal.kind()), "site", lower(terminal.site()),
                "automatable", terminal.automatable(), "mod_owned", terminal.modOwned(), "guaranteed", terminal.guaranteed(),
                "unlock_tier", terminal.unlockTier(), "justification", terminal.justification(), "source", terminal.source())));
        List<Object> tags = new ArrayList<>();
        snapshot.tags().forEach((resource, tag) -> tags.add(row("id", resource.key(), "complete", tag.complete(),
                "members", keys(tag.members()), "unresolved", tag.unresolved(), "sources", tag.sources())));
        List<Object> objectives = new ArrayList<>();
        snapshot.objectives().values().forEach(objective -> objectives.add(row("id", objective.id(),
                "subject", objective.subject() == null ? null : objective.subject().key(), "count", objective.count(),
                "requires", objective.requires(), "reward_route", objective.rewardRoute(), "source", objective.source())));
        List<String> findings = snapshot.findings().stream().map(finding -> NORMALIZED.toJson(row(
                "fingerprint", finding.fingerprint(), "rule", finding.rule(), "subject", finding.subject(),
                "detail", finding.detail(), "severity", lower(finding.severity()), "status", lower(finding.disposition()),
                "message", finding.message(), "sources", finding.sources()))).sorted().toList();
        return hash(row("resources", keys(snapshot.resources()), "routes", routes, "terminals", terminals,
                "tags", tags, "objectives", objectives, "findings", findings));
    }

    private static Map<String, String> sourceHashes(Snapshot snapshot, boolean implementation) {
        Map<String, String> hashes = new TreeMap<>();
        snapshot.inputHashes().forEach((path, value) -> {
            boolean implementationSource = path.startsWith(AuthorityAdapters.IMPLEMENTATION_SOURCE_PREFIX);
            if (implementation == implementationSource) {
                hashes.put(implementationSource ? path.substring(AuthorityAdapters.IMPLEMENTATION_SOURCE_PREFIX.length()) : path,
                        value);
            }
        });
        return hashes;
    }

    private static String sourceHash(Map<String, String> hashes) {
        return hash(hashes);
    }

    private static String hash(Object normalized) {
        return AuthorityAdapters.sha256(NORMALIZED.toJson(normalized).getBytes(StandardCharsets.UTF_8));
    }

    private static String tagResolutionScope(Snapshot snapshot) {
        if (canonicalPack(snapshot)) {
            return "standalone_gradle_resolved_resources_registry_evidence_per_tag";
        }
        return snapshot.findings().stream().anyMatch(finding -> finding.rule().equals("canonical_tag_resolution_unknown"))
                ? "local_only_noncanonical" : "snapshot_declared_resolutions_not_runtime_certification";
    }

    private static boolean canonicalPack(Snapshot snapshot) {
        return snapshot.inputHashes().keySet().stream().anyMatch(key -> key.startsWith("standalone_pack_contract:"));
    }

    private static Map<String, String> packHashes(Snapshot snapshot) {
        Map<String, String> hashes = new TreeMap<>();
        snapshot.inputHashes().forEach((source, hash) -> {
            if (source.startsWith("resolved_pack:") || source.startsWith("pack_binding:")
                    || source.startsWith("standalone_pack_contract:")) {
                hashes.put(source, hash);
            }
        });
        return hashes;
    }

    private static Map<String, String> registryHashes(Snapshot snapshot) {
        Map<String, String> hashes = new TreeMap<>();
        snapshot.inputHashes().forEach((source, hash) -> {
            if (source.startsWith("registry_membership:") || source.startsWith("registration_source:")
                    || source.startsWith("registry_profile:") || source.startsWith("registry_condition:")
                    || source.startsWith("registry_declaration:")) {
                hashes.put(source, hash);
            }
        });
        return hashes;
    }

    private static boolean hasAnnotations(SustainabilityAudit.Result result) {
        SustainabilityAnnotations annotations = result.annotations();
        return !annotations.resources().isEmpty() || !annotations.routes().isEmpty() || !annotations.machines().isEmpty()
                || !annotations.loops().isEmpty() || !annotations.plans().isEmpty() || !annotations.ledger().isEmpty();
    }

    private static Set<String> registryConditions(Snapshot snapshot) {
        Set<String> conditions = new TreeSet<>();
        snapshot.inputHashes().keySet().stream().filter(key -> key.startsWith("registry_condition:"))
                .map(key -> key.substring("registry_condition:".length())).forEach(conditions::add);
        return conditions;
    }

    private static List<Map<String, Object>> relationships(Snapshot snapshot, Resource resource, boolean producers) {
        List<Map<String, Object>> relationships = new ArrayList<>();
        for (Route route : snapshot.routes().values()) {
            if (producers) {
                route.outputs().keySet().forEach(output -> addRelationship(relationships, snapshot,
                        resource, route, output, "output"));
            } else if (!route.oneShot()) {
                route.netInputs().keySet().forEach(input -> addRelationship(relationships, snapshot,
                        resource, route, input, "consumed_input"));
                route.catalysts().keySet().forEach(catalyst -> addRelationship(relationships, snapshot,
                        resource, route, catalyst, "catalyst"));
            }
        }
        return List.copyOf(relationships);
    }

    private static void addRelationship(List<Map<String, Object>> relationships, Snapshot snapshot,
                                        Resource resource, Route route, Resource operand, String kind) {
        boolean direct = resource.equals(operand);
        TagResolution tag = operand.kind() == Kind.TAG ? snapshot.tags().get(operand) : null;
        if (!direct && (tag == null || !tag.members().contains(resource))) {
            return;
        }
        boolean output = kind.equals("output");
        boolean complete = tag != null && tag.complete() && tag.unresolved().isEmpty();
        boolean unconditional = direct || complete && (!output || tag.members().size() == 1);
        String certainty = direct ? "exact_resource" : !complete ? "incomplete_tag"
                : output ? tag.members().size() == 1 ? "complete_singleton_tag" : "conditional_tag_member"
                : "complete_tag_member";
        String uncertainty = unconditional ? null : !complete
                ? "Tag membership or resource-pack precedence is incomplete; this is candidate provenance only."
                : "A tag output selects a runtime member; membership does not prove production of this concrete candidate.";
        relationships.add(row("route", route.id(), "source", route.source(), "via", operand.key(), "kind", kind,
                "certainty", certainty, "unconditional", unconditional, "tag_complete", tag == null ? null : complete,
                "tag_sources", tag == null ? List.of() : tag.sources(), "uncertainty", uncertainty,
                "discharged", output && route.discharged().contains(operand)));
    }

    private static Set<String> unconditionalRoutes(List<Map<String, Object>> relationships) {
        Set<String> routes = new TreeSet<>();
        relationships.stream().filter(relationship -> Boolean.TRUE.equals(relationship.get("unconditional")))
                .forEach(relationship -> routes.add((String) relationship.get("route")));
        return routes;
    }

    private static Map<String, Object> summary(SustainabilityAudit.Result result) {
        Snapshot snapshot = result.authorities();
        return row("resources", snapshot.resources().size(), "routes", snapshot.routes().size(),
                "terminals", snapshot.terminals().size(), "tags", snapshot.tags().size(),
                "objectives", snapshot.objectives().size(), "annotations", result.annotations().resources().size(),
                "active_errors", result.findings().activeErrors(), "waived_debt", result.findings().waivedDebt(),
                "warnings", result.findings().warnings(), "unknowns", result.findings().unknowns(),
                "unresolved_tags", result.findings().findings().stream().filter(f -> f.rule().equals("unresolved_tag")).count(),
                "counts_per_rule", countsPerRule(result));
    }

    private static Map<String, Object> metadata(ResourceMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        Handling handling = metadata.handling();
        return row("resource_class", lower(metadata.resourceClass()), "side_stream_class", lower(metadata.sideStreamClass()),
                "first_use_tier", metadata.firstUseTier(), "first_renewable_tier", metadata.firstRenewableTier(),
                "canonical_route", metadata.canonicalRoute(), "future_consumers", metadata.futureConsumers().stream()
                        .map(consumer -> row("tier", consumer.tier(), "system", consumer.system(),
                                "plan_reference", consumer.planReference())).toList(),
                "handling", handling == null ? null : row("action", lower(handling.action()),
                        "discard_allowed", handling.discardAllowed(), "hazardous", handling.hazardous(),
                        "containment", handling.containment() == null ? null : handling.containment().key(),
                        "treatment_route", handling.treatmentRoute()),
                "recycling", metadata.recycling() == null ? null : row("route", metadata.recycling().route(),
                        "planned_tier", metadata.recycling().plannedTier()),
                "originating_catalyst", metadata.originatingCatalyst() == null ? null : metadata.originatingCatalyst().key(),
                "uncertainty", metadata.uncertainty());
    }

    private static Map<String, Object> debt(DebtManifest debt) {
        return row("schema_version", debt.schemaVersion(), "version", debt.version(), "entries",
                debt.entries().values().stream().map(entry -> row("fingerprint", entry.fingerprint(),
                        "origin", entry.origin(), "remediation", entry.remediation(), "owning_task", entry.owningTask())).toList());
    }

    private static Map<String, Long> quantitiesById(Map<Resource, Long> quantities) {
        Map<String, Long> values = new TreeMap<>();
        quantities.forEach((resource, amount) -> values.put(resource.key(), amount));
        return values;
    }

    private static Set<String> keys(Set<Resource> resources) {
        Set<String> keys = new TreeSet<>();
        resources.forEach(resource -> keys.add(resource.key()));
        return keys;
    }

    private static String lower(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    private static String escape(String text) {
        return text.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static Map<String, Object> row(Object... entries) {
        Map<String, Object> row = new TreeMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            row.put((String) entries[i], entries[i + 1]);
        }
        return row;
    }
}
