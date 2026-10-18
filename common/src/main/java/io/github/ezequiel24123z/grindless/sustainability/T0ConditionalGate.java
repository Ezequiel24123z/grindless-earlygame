package io.github.ezequiel24123z.grindless.sustainability;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/** Hard conditional model gate, deliberately separate from global, Tier and physical acceptance. */
public final class T0ConditionalGate {
    private T0ConditionalGate() {
    }

    /** Every retained finding has an attributable scope decision, not a waiver. */
    public record Decision(String fingerprint, boolean blocking, String basis) {
    }

    /** The proof's dependency scope includes R0 claims without making them progression roots. */
    public record Scope(Set<Resource> resources, Set<String> routes, Set<String> families,
                        Set<String> terminals, Set<String> objectives) {
        public Scope {
            resources = sortedSet(resources);
            routes = sortedSet(routes);
            families = sortedSet(families);
            terminals = sortedSet(terminals);
            objectives = sortedSet(objectives);
        }
    }

    /** A pass certifies only the finite conditional model under its exported entry assumptions. */
    public record Gate(boolean pass, String status, Scope scope, Map<String, String> unmetObligations,
                       List<Decision> decisions) {
        public Gate {
            unmetObligations = sortedMap(unmetObligations);
            decisions = List.copyOf(decisions);
        }
    }

    public static Gate evaluate(SustainabilityAudit.Result result) {
        SustainabilityAudit audit = new SustainabilityAudit(result.authorities(), result.annotations());
        SustainabilityAudit.TierResult tier = result.tiers().get(0);
        TierPlan plan = result.annotations().ledger().get(0);
        Scope scope = scope(result, audit, tier, plan);
        Map<String, String> unmet = new TreeMap<>();
        if (plan == null || plan.unknown() != null || tier == null || tier.unknown() != null) {
            unmet.put("authored_t0_ledger", "An authored T0 ledger and evaluated obligations are required.");
        }
        if (plan == null || plan.entryProfile() == null) {
            unmet.put("finite_conditional_entry", "An explicit finite conditional entry profile is required.");
        }
        if (tier != null) {
            for (Resource resource : tier.mandatory()) {
                if (!audit.has(tier.available(), resource)) {
                    unmet.put("available:" + resource.key(), "A required resource has no established availability.");
                }
            }
            for (Resource resource : tier.recurring()) {
                if (!audit.has(tier.renewable(), resource)) {
                    unmet.put("recurring:" + resource.key(), "A recurring supply is not proved renewable.");
                }
            }
            for (var entry : result.annotations().resources().entrySet()) {
                ResourceMetadata metadata = entry.getValue();
                if (metadata.resourceClass() == ResourceClass.RENEWABLE
                        && Integer.valueOf(0).equals(metadata.firstRenewableTier())
                        && !audit.has(tier.renewable(), entry.getKey())) {
                    unmet.put("renewable_claim:" + entry.getKey().key(), "The explicit R0 claim is not proved.");
                }
            }
            for (var reserve : tier.reserves()) {
                if (reserve.margin() < 0 || !reserve.status().equals("conditional_established")) {
                    unmet.put("joint_reserve:" + reserve.resource().key(),
                            "Shared finite construction, claims, seeds and production custody are not established.");
                }
            }
            if (plan != null && plan.entryProfile() != null && plan.entryProfile().manualWork() != null
                    && (tier.manualWork() == null || tier.manualWork().marginActions() < 0
                    || !tier.manualWork().status().equals("conditional_established"))) {
                unmet.put("finite_local_work", "The actual batch-local work budget is not established.");
            }
        }
        checkExit(result, audit, tier, plan, scope, unmet);
        List<Decision> decisions = new ArrayList<>();
        List<SustainabilityAudit.CycleComponent> components = SustainabilityAudit.cycleComponents(result.authorities());
        for (AuditFinding finding : result.findings().findings()) {
            decisions.add(decide(finding, result, audit, tier, scope, components));
        }
        boolean pass = unmet.isEmpty() && decisions.stream().noneMatch(Decision::blocking);
        return new Gate(pass, pass ? "conditional_model_pass" : "blocked", scope, unmet, decisions);
    }

    private static void checkExit(SustainabilityAudit.Result result, SustainabilityAudit audit,
                                  SustainabilityAudit.TierResult tier, TierPlan plan, Scope scope,
                                  Map<String, String> unmet) {
        Resource goal = plan == null ? null : plan.exitObjective();
        Route route = goal == null ? null : audit.canonical(goal, 0, false);
        SustainabilityAudit.GoalWitness witness = tier == null ? null : tier.exitProduction();
        long required = result.authorities().objectives().values().stream()
                .filter(objective -> goal != null && goal.equals(objective.subject())
                        && scope.objectives().contains(objective.id()))
                .mapToLong(Objective::count).max().orElse(0);
        if (goal == null || route == null || !SustainabilityAudit.exitProductionRoute(route)
                || witness == null || !goal.equals(witness.resource()) || !route.id().equals(witness.route())
                || witness.batchOutput() <= 0 || witness.batchOutput() != audit.outputAmount(route, goal)
                || witness.requiredCount() <= 0 || witness.requiredCount() < required
                || witness.executedBatches() <= 0 || witness.produced() < witness.requiredCount()
                || !BigInteger.valueOf(witness.batchOutput()).multiply(BigInteger.valueOf(witness.executedBatches()))
                .equals(BigInteger.valueOf(witness.produced()))
                || !witness.status().equals("conditional_established")
                || route.netInputs().keySet().stream().anyMatch(resource -> !audit.has(tier.renewable(), resource))) {
            unmet.put("concrete_exit_production",
                    "The real canonical exit batches and counts must execute jointly from renewable consumed supplies.");
        }
    }

    private static Scope scope(SustainabilityAudit.Result result, SustainabilityAudit audit,
                               SustainabilityAudit.TierResult tier, TierPlan plan) {
        ScopeBuilder scope = new ScopeBuilder(result, audit);
        if (tier != null) {
            tier.mandatory().forEach(scope::need);
            tier.recurring().forEach(scope::need);
            tier.establishmentBatches().keySet().forEach(scope::route);
            tier.reserves().forEach(reserve -> scope.need(reserve.resource()));
        }
        result.annotations().resources().forEach((resource, metadata) -> {
            if (Integer.valueOf(0).equals(metadata.firstRenewableTier())) {
                scope.need(resource);
            }
        });
        if (plan != null) {
            plan.roots().forEach(scope::need);
            if (plan.exitObjective() != null) {
                scope.need(plan.exitObjective());
            }
            plan.requiredQuests().forEach(scope::objective);
            EntryProfile profile = plan.entryProfile();
            if (profile != null) {
                profile.playerProvided().keySet().forEach(scope::need);
                if (profile.manualWork() != null && profile.manualWorkActions() > 0) {
                    scope.terminal(profile.manualWork().terminal());
                    scope.need(profile.manualWork().tool());
                }
            }
        }
        scope.expand();
        return new Scope(scope.resources, scope.routes, scope.families, scope.terminals, scope.objectives);
    }

    private static Decision decide(AuditFinding finding, SustainabilityAudit.Result result, SustainabilityAudit audit,
                                   SustainabilityAudit.TierResult tier, Scope scope,
                                   List<SustainabilityAudit.CycleComponent> components) {
        boolean t0 = tierContext(finding.detail(), 0) || tierContext(finding, 0);
        boolean otherTier = java.util.stream.IntStream.rangeClosed(1, 15).anyMatch(value ->
                tierContext(finding, value));
        if (t0 && finding.severity() != AuditFinding.Severity.WARNING) {
            return decision(finding, true, "Explicit T0 evidence remains unresolved.");
        }
        if (finding.severity() == AuditFinding.Severity.ERROR && !otherTier) {
            return decision(finding, true, "Global integrity errors, including waived debt, block this gate.");
        }
        if (otherTier) {
            return decision(finding, false, "Outside T0; retained under that tier's own ledger/profile assumptions, "
                    + "not a demonstrated failure or acceptance of future gameplay.");
        }
        if (finding.rule().equals("cycle_balance_unproven") || finding.rule().equals("resource_positive_cycle")) {
            var matching = components.stream().filter(component ->
                    component.resources().stream().anyMatch(resource -> resource.key().equals(finding.subject())))
                    .toList();
            Set<String> members = new TreeSet<>();
            matching.forEach(component -> component.routes().forEach(route -> members.add(route.id())));
            if (!finding.detail().isEmpty()) {
                for (String id : finding.detail().split("\\+")) {
                    if (!result.authorities().routes().containsKey(id)) {
                        return decision(finding, true, "The complete gain-component scope cannot be established.");
                    }
                    members.add(id);
                }
            }
            if (matching.isEmpty() || matching.stream().anyMatch(component ->
                    component.resources().stream().anyMatch(scope.resources()::contains))
                    || members.stream().anyMatch(scope.routes()::contains)) {
                return decision(finding, true, "The entire unresolved or positive component can affect the T0/R0 proof.");
            }
            return decision(finding, false, "All component resources and member routes are outside the active proof: "
                    + members);
        }
        if (finding.severity() == AuditFinding.Severity.WARNING) {
            return decision(finding, false, "Informational warning retained; it is not an unresolved gain predicate.");
        }
        Resource resource = result.authorities().resources().stream()
                .filter(candidate -> candidate.key().equals(finding.subject())).findFirst().orElse(null);
        if (finding.rule().equals("first_use_unknown") && resource != null && tier != null
                && !tier.mandatory().contains(resource)
                && result.authorities().routes().values().stream()
                .anyMatch(route -> audit.outputAmount(route, resource) > 0)
                && !hasT0Use(resource, result, audit, scope)) {
            return decision(finding, false, "A nonmandatory output has no accepted T0 consumer. First use remains "
                    + "unknown; its classification, R0 route/proof and handling are still separate obligations.");
        }
        if (resource != null) {
            return decision(finding, scope.resources().contains(resource),
                    scope.resources().contains(resource) ? "The exact resource is in the T0/R0 dependency scope."
                            : "No active root, R0 claim, selected/finished batch, tag supply or source needs this resource.");
        }
        if (result.authorities().routes().containsKey(finding.subject())) {
            return decision(finding, scope.routes().contains(finding.subject()),
                    scope.routes().contains(finding.subject()) ? "The route is necessary or actually executed."
                            : "The exact route is not used by any active canonical, bootstrap, R0 or exit obligation.");
        }
        if (result.annotations().machines().containsKey(finding.subject())
                || result.authorities().routes().values().stream().anyMatch(route ->
                finding.subject().equals(route.family()))) {
            return decision(finding, scope.families().contains(finding.subject()),
                    scope.families().contains(finding.subject()) ? "The machine family is required by the proof."
                            : "No scoped route or installation uses this machine family.");
        }
        if (result.authorities().terminals().containsKey(finding.subject())) {
            return decision(finding, scope.terminals().contains(finding.subject()),
                    "Terminal scope follows actual source requirements and the finite entry profile.");
        }
        if (result.authorities().objectives().containsKey(finding.subject())) {
            return decision(finding, scope.objectives().contains(finding.subject()),
                    "Objective scope follows selected quests, prerequisites and executed conditional claims.");
        }
        if (finding.rule().equals("generator_fuel_unknown") && finding.subject().equals("furnace_fuel_table")
                && scope.routes().stream().map(result.authorities().routes()::get).allMatch(route ->
                route.authority() != Authority.VANILLA_TRANSFORMATION || primitiveCraft(route))) {
            return decision(finding, false, "Every scoped vanilla transformation is a zero-work shaped/shapeless "
                    + "construction craft. Explicit process/generator fuel batches do not use the dynamic furnace table.");
        }
        return decision(finding, true, "Irrelevance to the complete T0/R0 proof has not been demonstrated.");
    }

    private static boolean hasT0Use(Resource resource, SustainabilityAudit.Result result,
                                   SustainabilityAudit audit, Scope scope) {
        Set<Resource> supplied = Set.of(resource);
        TierPlan plan = result.annotations().ledger().get(0);
        EntryProfile profile = plan == null ? null : plan.entryProfile();
        ManualWork work = profile == null || profile.manualWorkActions() == 0 ? null : profile.manualWork();
        for (Route route : result.authorities().routes().values()) {
            RoutePolicy policy = result.annotations().routes().get(route.id());
            if (policy == null || !Integer.valueOf(0).equals(policy.unlockTier())) {
                continue;
            }
            if (route.inputs().keySet().stream().anyMatch(input -> audit.has(supplied, input))
                    || route.catalysts().keySet().stream().anyMatch(input -> audit.has(supplied, input))) {
                return true;
            }
            for (String id : policy.environmentalSources()) {
                Terminal terminal = result.authorities().terminals().get(id);
                if (terminal != null && audit.has(supplied, terminal.resource())) {
                    return true;
                }
            }
            Machine machine = route.family() == null ? null : result.annotations().machines().get(route.family());
            if (machine != null && machine.unlockTier() == 0 && audit.has(supplied, machine.construction())) {
                return true;
            }
            if (work != null && work.applies(route) && route.inputs().containsKey(ENERGY)) {
                Terminal labour = result.authorities().terminals().get(work.terminal());
                if (audit.has(supplied, work.tool()) || labour != null && audit.has(supplied, labour.resource())) {
                    return true;
                }
            }
            for (Resource output : route.outputs().keySet()) {
                ResourceMetadata metadata = result.annotations().resources().get(output);
                Handling handling = metadata == null ? null : metadata.handling();
                if (handling != null && handling.containment() != null
                        && audit.has(supplied, handling.containment())) {
                    return true;
                }
            }
        }
        return scope.objectives().stream().map(result.authorities().objectives()::get)
                .anyMatch(objective -> objective.subject() != null && audit.has(supplied, objective.subject()));
    }

    private static boolean primitiveCraft(Route route) {
        return !route.automatable() && !route.oneShot() && route.durationTicks() == 0 && route.fuPerTick() == 0
                && (route.family() == null || route.family().equals("CRAFTING_TABLE"))
                && Set.of("minecraft:crafting_shaped", "minecraft:crafting_shapeless")
                .contains(route.conditions().getOrDefault("crafting_type", ""));
    }

    private static boolean tierContext(String value, int tier) {
        return value.equals("T" + tier) || value.startsWith("T" + tier + "/");
    }

    private static boolean tierContext(AuditFinding finding, int tier) {
        // Only these rule fields carry a tier; an entity id or arbitrary detail that looks like T1 does not.
        return switch (finding.rule()) {
            case "tier_unknown" -> finding.detail().isEmpty() && finding.subject().equals("T" + tier);
            case "loop_makeup_not_renewable" -> tierContext(finding.detail(), tier);
            case "entry_profile_source_invalid", "entry_work_source_invalid", "exit_goal_route_missing",
                    "exit_goal_route_invalid", "mandatory_unavailable", "finite_dependency", "vanilla_dependency",
                    "protected_inventory_withdrawal", "renewable_claim", "self_constructing_producer",
                    "canonical_route_missing", "finite_optional_required", "forbidden_dependency",
                    "resource_used_before_tier", "catalyst_consumed", "loop_declaration_missing",
                    "optional_side_stream_required", "inert_waste_required", "potential_use_required",
                    "non_material_required", "future_required_early", "current_consumer_unavailable",
                    "hazardous_handling", "loop_balance_unproven", "unseeded_loop", "loop_gain_unjustified",
                    "loop_startup_unproven", "canonical_route_required", "bootstrap_starvation",
                    "entry_profile_insufficient", "exit_goal_production_unproven", "exit_goal_supply_not_renewable",
                    "entry_work_insufficient", "construction_reserve_unknown" -> finding.detail().equals("T" + tier);
            default -> false;
        };
    }

    private static Decision decision(AuditFinding finding, boolean blocking, String basis) {
        return new Decision(finding.fingerprint(), blocking, basis);
    }

    private static final class ScopeBuilder {
        private final SustainabilityAudit.Result result;
        private final Snapshot snapshot;
        private final SustainabilityAudit audit;
        private final Set<Resource> resources = new TreeSet<>();
        private final Set<Resource> needed = new TreeSet<>();
        private final Set<String> routes = new TreeSet<>();
        private final Set<String> families = new TreeSet<>();
        private final Set<String> terminals = new TreeSet<>();
        private final Set<String> objectives = new TreeSet<>();
        private final Set<String> loops = new TreeSet<>();
        private final ArrayDeque<Resource> pendingResources = new ArrayDeque<>();
        private final ArrayDeque<Route> pendingRoutes = new ArrayDeque<>();

        private ScopeBuilder(SustainabilityAudit.Result result, SustainabilityAudit audit) {
            this.result = result;
            this.snapshot = result.authorities();
            this.audit = audit;
        }

        private void need(Resource resource) {
            resources.add(resource);
            if (needed.add(resource)) {
                pendingResources.addLast(resource);
            }
        }

        private void route(String id) {
            Route route = snapshot.routes().get(id);
            if (route != null && routes.add(id)) {
                pendingRoutes.addLast(route);
            }
        }

        private void terminal(String id) {
            Terminal terminal = snapshot.terminals().get(id);
            if (terminal != null && terminals.add(id)) {
                need(terminal.resource());
            }
        }

        private void objective(String id) {
            Objective objective = snapshot.objectives().get(id);
            if (objective == null || !objectives.add(id)) {
                return;
            }
            if (objective.subject() != null) {
                need(objective.subject());
            }
            objective.requires().forEach(this::objective);
            if (objective.rewardRoute() != null) {
                route(objective.rewardRoute());
            }
        }

        private void expand() {
            do {
                while (!pendingResources.isEmpty()) {
                    Resource resource = pendingResources.removeFirst();
                    ResourceMetadata metadata = result.annotations().resources().get(resource);
                    Route canonical = metadata != null && metadata.canonicalRoute() != null
                            ? snapshot.routes().get(metadata.canonicalRoute()) : audit.canonical(resource, 0, false);
                    if (canonical != null) {
                        route(canonical.id());
                    }
                    snapshot.terminals().values().stream().filter(source ->
                            source.resource().equals(resource) && source.unlockTier() == 0)
                            .forEach(source -> terminal(source.id()));
                    TagResolution tag = snapshot.tags().get(resource);
                    SustainabilityAudit.TierResult tier = result.tiers().get(0);
                    if (tag != null && tag.complete() && tier != null) {
                        tag.members().stream().filter(member -> tier.renewable().contains(member)
                                || tier.reserves().stream().anyMatch(reserve -> reserve.resource().equals(member)))
                                .forEach(this::need);
                    }
                }
                while (!pendingRoutes.isEmpty()) {
                    Route route = pendingRoutes.removeFirst();
                    snapshot.objectives().values().stream().filter(objective -> route.id().equals(objective.rewardRoute()))
                            .forEach(objective -> objective(objective.id()));
                    route.inputs().keySet().forEach(this::need);
                    route.catalysts().keySet().forEach(this::need);
                    Set<Resource> outputs = new TreeSet<>(route.outputs().keySet());
                    for (Resource output : route.outputs().keySet()) {
                        TagResolution tag = snapshot.tags().get(output);
                        if (tag != null && tag.complete() && tag.members().size() == 1) {
                            outputs.addAll(tag.members());
                        }
                    }
                    resources.addAll(outputs);
                    route.prerequisites().forEach(this::route);
                    if (route.family() != null) {
                        families.add(route.family());
                        Machine machine = result.annotations().machines().get(route.family());
                        if (machine != null) {
                            need(machine.construction());
                        }
                    }
                    RoutePolicy policy = result.annotations().routes().get(route.id());
                    if (policy != null) {
                        policy.environmentalSources().forEach(this::terminal);
                    }
                    for (Resource output : outputs) {
                        ResourceMetadata metadata = result.annotations().resources().get(output);
                        Handling handling = metadata == null ? null : metadata.handling();
                        if (handling != null) {
                            if (handling.containment() != null) {
                                need(handling.containment());
                            }
                            if (handling.treatmentRoute() != null) {
                                route(handling.treatmentRoute());
                            }
                        }
                    }
                }
                for (ClosedLoop loop : result.annotations().loops().values()) {
                    if (!loops.contains(loop.id()) && (needed.contains(loop.carrier())
                            || loop.memberRoutes().stream().anyMatch(routes::contains))) {
                        loops.add(loop.id());
                        need(loop.carrier());
                        loop.memberRoutes().forEach(this::route);
                        if (loop.makeUpRoute() != null) {
                            route(loop.makeUpRoute());
                        }
                    }
                }
            } while (!pendingResources.isEmpty() || !pendingRoutes.isEmpty());
        }
    }
}
