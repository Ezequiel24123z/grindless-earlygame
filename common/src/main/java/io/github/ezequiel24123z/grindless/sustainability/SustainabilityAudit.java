package io.github.ezequiel24123z.grindless.sustainability;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.AuditFinding.Severity.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Least-fixed-point availability/unlimited net supply, protected circulation and ADR-0131 checks.
 * No reachability assumption, optional producer or unannotated tier can establish renewability.
 */
public final class SustainabilityAudit {
    /** Exact bootstrap draw before an established renewable operation, including seed reserves. */
    public record Reserve(Resource resource, long guaranteed, long required, long margin, String status) {
    }

    /** Tier facts; renewable excludes finite circulating stock, and an unknown ledger is not a pass. */
    public record TierResult(int tier, Set<Resource> mandatory, Set<Resource> recurring,
                             Set<Resource> available, Set<Resource> renewable,
                             Set<String> availableRoutes, Set<String> renewableRoutes,
                             List<Reserve> reserves, String unknown) {
        public TierResult {
            mandatory = sortedSet(mandatory);
            recurring = sortedSet(recurring);
            available = sortedSet(available);
            renewable = sortedSet(renewable);
            availableRoutes = sortedSet(availableRoutes);
            renewableRoutes = sortedSet(renewableRoutes);
            reserves = List.copyOf(reserves);
        }
    }

    /** An audit result separates exact debt-ratchet eligibility from actual certification. */
    public record Result(Snapshot authorities, SustainabilityAnnotations annotations,
                         Map<Integer, TierResult> tiers, DebtManifest debt, DebtManifest.Applied findings) {
        public Result {
            tiers = sortedMap(tiers);
        }

        /** Unknowns and waived errors cannot certify sustainability. */
        public boolean sustainabilityPass() {
            return findings.errorFree() && findings.unknowns() == 0 && findings.warnings() == 0;
        }
    }

    private final Snapshot snapshot;
    private final SustainabilityAnnotations annotations;
    private final Map<String, AuditFinding> findings = new TreeMap<>();

    private SustainabilityAudit(Snapshot snapshot, SustainabilityAnnotations annotations) {
        this.snapshot = snapshot;
        this.annotations = annotations;
        snapshot.findings().forEach(this::finding);
    }

    /** Runs the audit without writing files or choosing any live classifications. */
    public static Result run(Snapshot snapshot, SustainabilityAnnotations annotations, DebtManifest debt) {
        SustainabilityAudit audit = new SustainabilityAudit(snapshot, annotations);
        audit.validateDeclarations();
        Map<Integer, TierResult> tiers = new TreeMap<>();
        for (int tier = 0; tier <= 15; tier++) {
            tiers.put(tier, audit.analyseTier(tier));
        }
        audit.recycling();
        audit.positiveCycles();
        return new Result(snapshot, annotations, tiers, debt,
                debt.apply(new ArrayList<>(audit.findings.values())));
    }

    private void validateDeclarations() {
        for (Resource resource : snapshot.resources()) {
            ResourceMetadata metadata = annotations.resources().get(resource);
            if (metadata == null || metadata.resourceClass() == null) {
                add("classification_unknown", resource.key(), "", UNKNOWN, "Resource class is not authored.");
            }
            if (metadata != null && metadata.firstUseTier() == null) {
                add("first_use_unknown", resource.key(), "", UNKNOWN, "First-use tier is not authored.");
            }
            if (metadata != null && renewableClass(metadata.resourceClass())
                    && metadata.firstRenewableTier() == null) {
                add("renewable_tier_unknown", resource.key(), "", UNKNOWN, "First-renewable tier is not authored.");
            }
            if (resource.kind() == Kind.TAG) {
                TagResolution tag = snapshot.tags().get(resource);
                if (tag == null || !tag.complete()) {
                    add("unresolved_tag", resource.key(), "", UNKNOWN,
                            tag == null ? "Tag has no resolved JSON authority."
                                    : "Unresolved tag references: " + tag.unresolved());
                }
            }
        }
        for (Resource resource : annotations.resources().keySet()) {
            if (!snapshot.resources().contains(resource)) {
                add("annotation_resource_missing", resource.key(), "", ERROR,
                        "Annotation names no resource in the live authorities.");
            }
        }
        for (Route route : snapshot.routes().values()) {
            RoutePolicy policy = annotations.routes().get(route.id());
            if (policy == null) {
                add("route_tier_unknown", route.id(), "", UNKNOWN, "Route unlock/optional policy is not authored.");
            }
            if (route.family() != null && !annotations.machines().containsKey(route.family())) {
                add("machine_binding_unknown", route.family(), "", UNKNOWN,
                        "Machine construction and unlock binding is not authored.");
            }
            if (zeroInputUnjustified(route)) {
                add("zero_input_unjustified", route.id(), "", ERROR,
                        "A repeatable route has no material fuel/feed or justified environmental terminal.");
            }
            if (policy != null) {
                for (var override : policy.outputRoles().entrySet()) {
                    if (!route.outputs().containsKey(override.getKey())
                            || override.getValue() == OutputRole.DISCHARGED
                            && !route.discharged().contains(override.getKey())) {
                        add("output_role_invalid", route.id(), override.getKey().key(), ERROR,
                                "Output-role overrides cannot invent an output or a physical discharge.");
                    }
                }
                for (String source : policy.environmentalSources()) {
                    Terminal terminal = snapshot.terminals().get(source);
                    if (terminal == null || terminal.kind() != TerminalKind.ENVIRONMENT || !validTerminal(terminal)) {
                        add("environmental_justification_invalid", route.id(), source, ERROR,
                                "Environmental justification must reference a real, justified environmental terminal.");
                    }
                }
            }
        }
        for (String id : annotations.routes().keySet()) {
            if (!snapshot.routes().containsKey(id)) {
                add("annotation_route_missing", id, "", ERROR, "Route annotation has no live authority.");
            }
        }
        for (Terminal terminal : snapshot.terminals().values()) {
            if (!validTerminal(terminal)) {
                add("terminal_invalid", terminal.id(), "", ERROR,
                        "A source needs provenance/justification, and an environment resource needs a SourceSite.");
            }
        }
        for (Resource resource : annotations.resources().keySet()) {
            ResourceMetadata metadata = annotations.resources().get(resource);
            if (metadata.sideStreamClass() == SideStreamClass.BYPRODUCT_CURRENT
                    && consumers(resource).isEmpty()) {
                add("current_consumer_missing", resource.key(), "", ERROR,
                        "A current by-product needs an implemented consumer, not an invented sink.");
            }
            if (metadata.sideStreamClass() == SideStreamClass.BYPRODUCT_FUTURE
                    && !hasFuturePlan(metadata)) {
                add("future_plan_missing", resource.key(), "", ERROR,
                        "Future feedstock needs a matching committed plan, system and tier.");
            }
            if (metadata.sideStreamClass() == SideStreamClass.POTENTIAL_FUTURE_USE
                    && (metadata.uncertainty() == null || metadata.uncertainty().isBlank())) {
                add("potential_use_unqualified", resource.key(), "", ERROR,
                        "Potential future usefulness must explicitly state its uncertainty.");
            }
            if (metadata.sideStreamClass() == SideStreamClass.CATALYST_SPENT
                    && !validCatalystOrigin(resource, metadata.originatingCatalyst())) {
                add("catalyst_origin_missing", resource.key(), "", ERROR,
                        "Spent catalyst must name an originating catalyst used by its producer.");
            }
            if (metadata.sideStreamClass() == SideStreamClass.WASTE_RECOVERABLE
                    && (metadata.recycling() == null
                    || (metadata.recycling().route() == null && metadata.recycling().plannedTier() == null))) {
                add("recovery_unimplemented", resource.key(), "", ERROR,
                        "Recoverable waste needs an implemented recycling route or an explicitly planned tier.");
            }
            if (metadata.sideStreamClass() != null && metadata.handling() == null) {
                add("handling_unknown", resource.key(), "", UNKNOWN, "Handling advice is not authored.");
            }
            if ((metadata.resourceClass() == ResourceClass.CLOSED_LOOP
                    || metadata.resourceClass() == ResourceClass.LOSSY_LOOP)
                    && annotations.loops().values().stream().noneMatch(loop -> loop.carrier().equals(resource))) {
                add("loop_declaration_missing", resource.key(), "", ERROR,
                        "A loop class needs a declared carrier, member routes and make-up source.");
            }
            if (metadata.resourceClass() == ResourceClass.CATALYTIC
                    && snapshot.routes().values().stream().noneMatch(route -> route.catalysts().containsKey(resource))) {
                add("catalyst_use_missing", resource.key(), "", ERROR,
                        "A catalytic classification needs an implemented non-consumed use.");
            }
        }
    }

    private TierResult analyseTier(int tier) {
        Set<Resource> available = new TreeSet<>();
        Set<String> availableRoutes = new TreeSet<>();
        for (Terminal terminal : snapshot.terminals().values()) {
            if (terminal.unlockTier() <= tier && validTerminal(terminal)) {
                available.add(terminal.resource());
            }
        }
        boolean changed;
        do {
            changed = expandTags(available);
            for (Route route : snapshot.routes().values()) {
                if (unlocked(route, tier) && !zeroInputUnjustified(route)
                        && requirements(route, available, availableRoutes, tier)
                        && contains(available, route.inputs().keySet())
                        && contains(available, route.catalysts().keySet())
                        && environmentReady(route, available)) {
                    changed |= availableRoutes.add(route.id());
                    changed |= addOutputs(available, route);
                }
            }
        } while (changed);

        Set<Resource> renewable = new TreeSet<>();
        snapshot.terminals().values().stream().filter(terminal -> terminal.unlockTier() <= tier
                && validTerminal(terminal) && terminal.renewable()).forEach(terminal -> renewable.add(terminal.resource()));
        Set<String> renewableRoutes = new TreeSet<>();
        Map<String, LoopProof> maintained = new TreeMap<>();
        do {
            changed = expandTags(renewable);
            for (Route route : snapshot.routes().values()) {
                if (canRenew(route, tier, available, renewable, availableRoutes)) {
                    boolean used = false;
                    for (Resource resource : route.retainedOutputs().keySet()) {
                        Route selected = canonical(resource, tier, false);
                        if (selected != null && selected.id().equals(route.id())
                                && route.outputs().get(resource) > route.inputs().getOrDefault(resource, 0L)
                                && resolvableOutput(resource)) {
                            changed |= renewable.add(resource);
                            used = true;
                        }
                    }
                    if (used) {
                        renewableRoutes.add(route.id());
                    }
                }
            }
            for (ClosedLoop loop : annotations.loops().values()) {
                LoopProof proof = proveLoop(loop, tier, available, renewable, availableRoutes, false);
                if (proof != null) {
                    if (!maintained.containsKey(loop.id())) {
                        changed = true;
                    }
                    maintained.put(loop.id(), proof);
                    changed |= renewable.addAll(proof.unlimited());
                    renewableRoutes.addAll(loop.memberRoutes());
                    if (loop.makeUpRoute() != null && proof.unlimited().contains(loop.carrier())) {
                        renewableRoutes.add(loop.makeUpRoute());
                    }
                }
            }
        } while (changed);
        expandTags(renewable);
        // Inference is monotone; a temporarily unavailable multi-hop feed is not a final error.
        for (ClosedLoop loop : annotations.loops().values()) {
            proveLoop(loop, tier, available, renewable, availableRoutes, true);
        }

        TierPlan plan = annotations.ledger().get(tier);
        if (plan == null || plan.unknown() != null || (plan.roots().isEmpty()
                && plan.requiredQuests().isEmpty() && plan.exitObjective() == null)) {
            String reason = plan == null || plan.unknown() == null ? "Tier roots and exit are not authored."
                    : plan.unknown();
            add("tier_unknown", "T" + tier, "", UNKNOWN, reason);
            return new TierResult(tier, Set.of(), Set.of(), available, renewable, availableRoutes,
                    renewableRoutes, List.of(), reason);
        }
        Set<Resource> roots = new TreeSet<>(plan.roots());
        if (plan.exitObjective() != null) {
            roots.add(plan.exitObjective());
        }
        for (String id : plan.requiredQuests()) {
            addObjectiveRoots(id, roots, new HashSet<>());
        }
        Set<Resource> mandatory = new TreeSet<>();
        Set<Resource> recurring = new TreeSet<>();
        Set<String> closureRoutes = new TreeSet<>();
        Set<Resource> construction = new TreeSet<>();
        Set<Resource> visitedRecurring = new TreeSet<>();
        for (Resource root : roots) {
            closure(root, true, tier, mandatory, recurring, closureRoutes, construction, visitedRecurring);
        }
        int previousClosure;
        do {
            previousClosure = closureRoutes.size();
            for (LoopProof proof : maintained.values()) {
                if (proof.loop().makeUpRoute() != null
                        && proof.inventory().stream().anyMatch(mandatory::contains)) {
                    Route makeUp = snapshot.routes().get(proof.loop().makeUpRoute());
                    if (makeUp != null && !closureRoutes.contains(makeUp.id())) {
                        closureRoute(makeUp, true, tier, mandatory, recurring, closureRoutes,
                                construction, visitedRecurring);
                    }
                }
            }
            handlingClosure(tier, mandatory, recurring, closureRoutes, construction, visitedRecurring);
        } while (closureRoutes.size() != previousClosure);
        recurring.removeIf(resource -> maintainedInventory(resource, maintained)
                && !externalWithdrawal(resource, closureRoutes, maintained));
        for (Resource resource : mandatory) {
            ResourceMetadata metadata = annotations.resources().get(resource);
            if (!has(available, resource)) {
                add("mandatory_unavailable", resource.key(), "T" + tier, ERROR,
                        "Least-fixed-point availability has no seed or constructible producer.");
            }
            if (recurring.contains(resource) && !has(renewable, resource) && finiteSource(resource)) {
                add("finite_dependency", resource.key(), "T" + tier, ERROR,
                        "Canonical recurring consumption reaches finite/bootstrap/manual supply.");
            }
            if (recurring.contains(resource) && !has(renewable, resource) && vanillaSource(resource)) {
                add("vanilla_dependency", resource.key(), "T" + tier, ERROR,
                        "A canonical dependency reaches a vanilla farm, loot, trade, generator or world source.");
            }
            if (!has(renewable, resource) && maintainedInventory(resource, maintained)
                    && externalWithdrawal(resource, closureRoutes, maintained)) {
                add("protected_inventory_withdrawal", resource.key(), "T" + tier, ERROR,
                        "Finite circulating inventory cannot fund net withdrawal outside its proved circulation.");
            }
            if (metadata != null) {
                compatibility(resource, metadata, tier, mandatory, recurring, available, renewable);
            }
        }
        for (var entry : annotations.resources().entrySet()) {
            ResourceMetadata metadata = entry.getValue();
            if (metadata.firstRenewableTier() != null && metadata.firstRenewableTier() <= tier
                    && renewableClass(metadata.resourceClass()) && !has(renewable, entry.getKey())
                    && (metadata.resourceClass() == ResourceClass.RENEWABLE
                    || maintained.values().stream().noneMatch(proof -> proof.loop().carrier().equals(entry.getKey())))) {
                add("renewable_claim", entry.getKey().key(), "T" + tier, ERROR,
                        "The declared class lacks unlimited renewable supply or an executable circulation witness.");
            }
            handling(entry.getKey(), metadata, tier, available, availableRoutes);
        }
        for (Resource block : construction) {
            if (!has(available, block)) {
                add("self_constructing_producer", block.key(), "T" + tier, ERROR,
                        "The establishment closure cannot construct this producer from an available seed.");
            }
        }
        ConstructionReserve reserve = new ConstructionReserve(snapshot, annotations, this, tier, available, renewable);
        List<Reserve> reserves = reserve.analyse(construction, closureRoutes, roots);
        reserve.findings().forEach(this::finding);
        return new TierResult(tier, mandatory, recurring, available, renewable, availableRoutes,
                renewableRoutes, reserves, null);
    }

    private void addObjectiveRoots(String id, Set<Resource> roots, Set<String> visited) {
        if (!visited.add(id)) {
            return;
        }
        Objective objective = snapshot.objectives().get(id);
        if (objective == null || objective.subject() == null) {
            add("mandatory_objective_unknown", id, "", ERROR, "Selected quest has no physical authority.");
            return;
        }
        roots.add(objective.subject());
        objective.requires().forEach(required -> addObjectiveRoots(required, roots, visited));
    }

    private void closure(Resource resource, boolean operational, int tier, Set<Resource> mandatory,
                         Set<Resource> recurring, Set<String> routes, Set<Resource> construction,
                         Set<Resource> visitedRecurring) {
        boolean fresh = mandatory.add(resource);
        if (operational) {
            recurring.add(resource);
            fresh |= visitedRecurring.add(resource);
        }
        if (!fresh) {
            return;
        }
        Route route = canonical(resource, tier, true);
        if (route == null) {
            List<Route> unresolved = snapshot.routes().values().stream().filter(candidate ->
                    !candidate.oneShot() && unlocked(candidate, tier) && outputAmount(candidate, resource) > 0).toList();
            if (!unresolved.isEmpty()) {
                if (unresolved.stream().allMatch(this::optional)) {
                    add("canonical_route_missing", resource.key(), "T" + tier, ERROR,
                            "Mandatory production has only optional routes; no canonical dependency is established.");
                }
                // Diagnostic closure is conservative, not permission to select an optional producer.
                for (Route candidate : unresolved) {
                    closureRoute(candidate, operational, tier, mandatory, recurring, routes, construction, visitedRecurring);
                }
            }
            Resource member = uniqueTerminalMember(resource);
            if (member != null && !member.equals(resource)) {
                closure(member, operational, tier, mandatory, recurring, routes, construction, visitedRecurring);
            }
            return;
        }
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy != null && policy.optional()) {
            add("canonical_optional", resource.key(), route.id(), ERROR,
                    "An optional compatibility route cannot be the permanent canonical dependency.");
        }
        if (!route.modOwned() && route.authority() != Authority.VANILLA_TRANSFORMATION && operational) {
            add("non_mod_owned_canonical", resource.key(), route.id(), ERROR,
                    "An external route cannot establish canonical mod-owned renewable supply.");
        }
        closureRoute(route, operational, tier, mandatory, recurring, routes, construction, visitedRecurring);
    }

    private void closureRoute(Route route, boolean operational, int tier, Set<Resource> mandatory,
                              Set<Resource> recurring, Set<String> routes, Set<Resource> construction,
                              Set<Resource> visitedRecurring) {
        routes.add(route.id());
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy != null) {
            for (String id : policy.environmentalSources()) {
                Terminal terminal = snapshot.terminals().get(id);
                if (terminal != null) {
                    closure(terminal.resource(), operational, tier, mandatory, recurring, routes,
                            construction, visitedRecurring);
                }
            }
        }
        for (Resource input : route.inputs().keySet()) {
            closure(input, operational && route.netInputs().containsKey(input), tier,
                    mandatory, recurring, routes, construction, visitedRecurring);
        }
        for (Resource catalyst : route.catalysts().keySet()) {
            closure(catalyst, false, tier, mandatory, recurring, routes, construction, visitedRecurring);
        }
        if (route.family() != null) {
            Machine machine = annotations.machines().get(route.family());
            if (machine != null) {
                construction.add(machine.construction());
                closure(machine.construction(), false, tier,
                        mandatory, recurring, routes, construction, visitedRecurring);
            }
        }
        for (String prerequisite : route.prerequisites()) {
            Route required = snapshot.routes().get(prerequisite);
            if (required != null && !routes.contains(prerequisite)) {
                closureRoute(required, false, tier, mandatory, recurring, routes, construction, visitedRecurring);
            }
        }
    }

    private void handlingClosure(int tier, Set<Resource> mandatory, Set<Resource> recurring,
                                 Set<String> routes, Set<Resource> construction, Set<Resource> visitedRecurring) {
        int previous;
        do {
            previous = routes.size();
            for (String id : new TreeSet<>(routes)) {
                for (Resource output : snapshot.routes().get(id).retainedOutputs().keySet()) {
                    ResourceMetadata metadata = annotations.resources().get(output);
                    Handling handling = metadata == null ? null : metadata.handling();
                    if (handling == null || !handling.hazardous()
                            && metadata.sideStreamClass() != SideStreamClass.WASTE_HAZARDOUS) {
                        continue;
                    }
                    if (handling.containment() != null) {
                        construction.add(handling.containment());
                        closure(handling.containment(), false, tier, mandatory, recurring, routes,
                                construction, visitedRecurring);
                    }
                    Route treatment = handling.treatmentRoute() == null ? null
                            : snapshot.routes().get(handling.treatmentRoute());
                    if (treatment != null && !routes.contains(treatment.id())) {
                        closureRoute(treatment, true, tier, mandatory, recurring, routes, construction, visitedRecurring);
                    }
                }
            }
        } while (routes.size() != previous);
    }

    private static boolean maintainedInventory(Resource resource, Map<String, LoopProof> proofs) {
        return proofs.values().stream().anyMatch(proof -> proof.inventory().contains(resource));
    }

    private boolean externalWithdrawal(Resource resource, Set<String> routes, Map<String, LoopProof> proofs) {
        Set<String> internal = new TreeSet<>();
        proofs.values().stream().filter(proof -> proof.inventory().contains(resource))
                .forEach(proof -> internal.addAll(proof.loop().memberRoutes()));
        return routes.stream().filter(id -> !internal.contains(id)).map(snapshot.routes()::get)
                .anyMatch(route -> route.netInputs().containsKey(resource));
    }

    private void compatibility(Resource resource, ResourceMetadata metadata, int tier, Set<Resource> mandatory,
                               Set<Resource> recurring, Set<Resource> available, Set<Resource> renewable) {
        ResourceClass type = metadata.resourceClass();
        if (type == ResourceClass.FINITE_OPTIONAL || type == ResourceClass.FORBIDDEN_DEPENDENCY) {
            add(type == ResourceClass.FINITE_OPTIONAL ? "finite_optional_required" : "forbidden_dependency",
                    resource.key(), "T" + tier, ERROR, "This class cannot belong to mandatory progression.");
        }
        if (metadata.firstUseTier() != null && metadata.firstUseTier() > tier) {
            add("resource_used_before_tier", resource.key(), "T" + tier, ERROR,
                    "Mandatory use precedes the declared first-use tier.");
        }
        if (type == ResourceClass.CATALYTIC && recurring.contains(resource)
                && canonicalConsumption(resource, tier, mandatory)) {
            add("catalyst_consumed", resource.key(), "T" + tier, ERROR,
                    "A catalytic class cannot hide net canonical consumption.");
        }
        if ((type == ResourceClass.CLOSED_LOOP || type == ResourceClass.LOSSY_LOOP)
                && annotations.loops().values().stream().noneMatch(loop -> loop.carrier().equals(resource))) {
            add("loop_declaration_missing", resource.key(), "T" + tier, ERROR,
                    "A loop class needs named member routes and a seed/make-up proof.");
        }
        SideStreamClass side = metadata.sideStreamClass();
        if (side == SideStreamClass.BYPRODUCT_OPTIONAL) {
            add("optional_side_stream_required", resource.key(), "T" + tier, ERROR,
                    "Optional by-product must not become a mandatory dependency.");
        } else if (side == SideStreamClass.WASTE_INERT) {
            add("inert_waste_required", resource.key(), "T" + tier, ERROR, "Inert waste must stay optional/discardable.");
        } else if (side == SideStreamClass.POTENTIAL_FUTURE_USE) {
            add("potential_use_required", resource.key(), "T" + tier, ERROR,
                    "Uncertain future usefulness cannot be required at any tier.");
        } else if (side == SideStreamClass.PROCESS_LOSS || side == SideStreamClass.EMISSION) {
            add("non_material_required", resource.key(), "T" + tier, ERROR,
                    "A declared process loss/emission cannot be a required material feed.");
        } else if (side == SideStreamClass.BYPRODUCT_FUTURE && metadata.futureConsumers().stream()
                .noneMatch(consumer -> consumer.tier() <= tier && validFutureConsumer(consumer))) {
            add("future_required_early", resource.key(), "T" + tier, ERROR,
                    "Future feedstock is required before its committed consumer tier.");
        }
    }

    private void handling(Resource resource, ResourceMetadata metadata, int tier, Set<Resource> available,
                          Set<String> availableRoutes) {
        SideStreamClass side = metadata.sideStreamClass();
        Handling handling = metadata.handling();
        if (side == SideStreamClass.BYPRODUCT_CURRENT && firstProductionTier(resource) <= tier
                && !consumers(resource).isEmpty()
                && consumers(resource).stream().noneMatch(id -> unlocked(snapshot.routes().get(id), tier))) {
            add("current_consumer_unavailable", resource.key(), "T" + tier, ERROR,
                    "A current consumer must be unlocked by the side stream's production tier.");
        }
        if (side == SideStreamClass.WASTE_INERT && (handling == null || !handling.discardAllowed()
                || handling.hazardous())) {
            add("inert_waste_not_discardable", resource.key(), "", ERROR,
                    "Inert waste must have honest, non-hazardous, optional disposal.");
        }
        if (side != SideStreamClass.WASTE_HAZARDOUS && (handling == null || !handling.hazardous())) {
            return;
        }
        int production = firstProductionTier(resource);
        if (production > tier) {
            return;
        }
        boolean contained = handling != null && handling.containment() != null
                && has(available, handling.containment());
        boolean treated = handling != null && handling.treatmentRoute() != null
                && consumers(resource).contains(handling.treatmentRoute())
                && availableRoutes.contains(handling.treatmentRoute());
        boolean safeAction = handling != null && handling.action() != null && switch (handling.action()) {
            case STORE, CONTAIN -> contained;
            case REUSE, RECYCLE, TREAT -> treated;
            default -> false;
        };
        if (handling == null || !handling.hazardous() || handling.action() == null
                || handling.discardAllowed() || !safeAction) {
            add("hazardous_handling", resource.key(), "T" + production, ERROR,
                    "Hazard handling must be implemented and available at the first production tier.");
        }
    }

    private record LoopProof(ClosedLoop loop, Set<Resource> inventory, Set<Resource> unlimited) {
    }

    private LoopProof proveLoop(ClosedLoop loop, int tier, Set<Resource> available,
                               Set<Resource> renewable, Set<String> availableRoutes, boolean diagnose) {
        List<Route> members = loop.memberRoutes().stream().map(snapshot.routes()::get).toList();
        if (members.isEmpty() || members.stream().anyMatch(java.util.Objects::isNull)) {
            if (diagnose) {
                add("loop_route_missing", loop.id(), "", ERROR, "A declared loop member has no live route.");
            }
            return null;
        }
        if (members.stream().anyMatch(route -> !unlocked(route, tier))) {
            return null;
        }
        if (members.stream().anyMatch(this::optional)) {
            if (diagnose) {
                add("loop_optional_route", loop.id(), "", ERROR, "An optional route cannot establish a canonical loop.");
            }
            return null;
        }
        if (members.stream().noneMatch(route -> route.inputs().containsKey(loop.carrier()))
                || members.stream().noneMatch(route -> route.retainedOutputs().containsKey(loop.carrier()))) {
            if (diagnose) {
                add("loop_carrier_missing", loop.id(), "", ERROR, "The carrier must participate in the actual loop batches.");
            }
            return null;
        }
        Map<String, BigInteger> batches = loopBatches(loop);
        if (batches == null) {
            if (diagnose) {
                add("loop_balance_unproven", loop.id(), "T" + tier, UNKNOWN,
                        "No exact positive normalized batch vector was established within the audit bound.");
            }
            return null;
        }
        Map<Resource, BigInteger> net = balance(members, batches);
        int carrier = net.getOrDefault(loop.carrier(), BigInteger.ZERO).signum();
        if (carrier > 0) {
            if (diagnose) {
                add("closed_loop_gain", loop.id(), "", ERROR,
                        "Normalized batches produce more carrier than they consume: " + batches);
            }
            return null;
        }
        ResourceMetadata metadata = annotations.resources().get(loop.carrier());
        if (metadata != null && metadata.resourceClass() == ResourceClass.CLOSED_LOOP && carrier < 0) {
            if (diagnose) {
                add("closed_loop_loss", loop.id(), "", ERROR, "A losing carrier must be classified as a lossy loop.");
            }
            return null;
        }
        if (metadata != null && metadata.resourceClass() == ResourceClass.LOSSY_LOOP && carrier >= 0) {
            if (diagnose) {
                add("lossy_loop_without_loss", loop.id(), "", ERROR, "A lossy-loop class needs actual carrier loss.");
            }
            return null;
        }
        if (carrier < 0 && loop.makeUpRoute() == null) {
            if (diagnose) {
                add("loop_makeup_not_renewable", loop.id(), "T" + tier + "/" + loop.carrier().key(), ERROR,
                        "A losing carrier must name its independently renewable make-up route.");
            }
            return null;
        }
        if (!has(available, loop.carrier()) || members.stream().anyMatch(route ->
                !availableRoutes.contains(route.id()) || !contains(available, route.inputs().keySet())
                        || !contains(available, route.catalysts().keySet()))) {
            if (diagnose) {
                add("unseeded_loop", loop.id(), "T" + tier, ERROR,
                        "Loop and producer construction must first be available from a real seed.");
            }
            return null;
        }
        Set<Resource> outputs = new TreeSet<>();
        members.forEach(route -> outputs.addAll(route.retainedOutputs().keySet()));
        for (Route route : members) {
            if (route.oneShot() || !route.modOwned() || !environmentReady(route, renewable)) {
                return null;
            }
        }
        Route makeUp = loop.makeUpRoute() == null ? null : snapshot.routes().get(loop.makeUpRoute());
        boolean independentMakeUp = makeUp != null && canRenew(makeUp, tier, available, renewable, availableRoutes)
                && outputAmount(makeUp, loop.carrier()) > makeUp.inputs().getOrDefault(loop.carrier(), 0L);
        for (var entry : net.entrySet()) {
            if (entry.getValue().signum() < 0 && !has(renewable, entry.getKey())) {
                if (!independentMakeUp || !entry.getKey().equals(loop.carrier())) {
                    if (diagnose) {
                        add("loop_makeup_not_renewable", loop.id(), "T" + tier + "/" + entry.getKey().key(), ERROR,
                                "Every normalized net draw, including energy, needs independent renewable supply.");
                    }
                    return null;
                }
            }
        }
        Set<Resource> unlimited = new TreeSet<>();
        for (Resource output : outputs) {
            if (net.getOrDefault(output, BigInteger.ZERO).signum() > 0 && resolvableOutput(output)) {
                Route selected = canonical(output, tier, false);
                if (selected != null && loop.memberRoutes().contains(selected.id())) {
                    unlimited.add(output);
                }
            }
        }
        boolean materialGain = unlimited.stream().anyMatch(resource -> resource.kind() != Kind.ENERGY);
        boolean materialFeed = net.entrySet().stream().anyMatch(entry -> entry.getValue().signum() < 0
                && entry.getKey().kind() != Kind.ENERGY);
        if (materialGain && !materialFeed && members.stream().noneMatch(this::hasEnvironment)) {
            if (diagnose) {
                add("loop_gain_unjustified", loop.id(), "T" + tier, ERROR,
                        "Returned circulating inventory is not an external material source for positive net output.");
            }
            return null;
        }
        Set<Resource> supplies = new TreeSet<>(renewable);
        if (independentMakeUp) {
            supplies.add(loop.carrier());
        }
        ConstructionReserve witness = new ConstructionReserve(snapshot, annotations, this, tier, available, supplies);
        if (!witness.circulate(loop, batches)) {
            if (diagnose) {
                add("loop_startup_unproven", loop.id(), "T" + tier, UNKNOWN,
                        "Qualitative availability does not establish an executable, quantity-sufficient circulation.");
            }
            return null;
        }
        if (independentMakeUp) {
            unlimited.add(loop.carrier());
        }
        outputs.removeIf(resource -> net.getOrDefault(resource, BigInteger.ZERO).signum() > 0
                || !resolvableOutput(resource));
        return new LoopProof(loop, outputs, unlimited);
    }

    private void recycling() {
        for (var entry : annotations.resources().entrySet()) {
            Resource resource = entry.getKey();
            Recycling recycling = entry.getValue().recycling();
            if (recycling == null || recycling.route() == null) {
                continue;
            }
            Route reverse = snapshot.routes().get(recycling.route());
            if (reverse == null || !reverse.inputs().containsKey(resource)) {
                add("recovery_unimplemented", resource.key(), "", ERROR,
                        "Declared recovery route must exist and consume this resource.");
                continue;
            }
            Route forward = canonical(resource, 15, true);
            if (forward == null || outputAmount(forward, resource) == 0) {
                add("recycling_forward_unknown", resource.key(), "", UNKNOWN,
                        "Recycling needs the canonical forward batch for an exact comparison.");
                continue;
            }
            BigInteger reverseInput = BigInteger.valueOf(reverse.inputs().get(resource));
            BigInteger forwardOutput = BigInteger.valueOf(outputAmount(forward, resource));
            for (var output : reverse.retainedOutputs().entrySet()) {
                BigInteger returned = BigInteger.valueOf(output.getValue()).multiply(forwardOutput);
                BigInteger consumed = BigInteger.valueOf(forward.inputs().getOrDefault(output.getKey(), 0L))
                        .multiply(reverseInput);
                if (returned.compareTo(consumed) > 0) {
                    add("resource_positive_recycling", resource.key(), output.getKey().key(), ERROR,
                            "Recovery exceeds the canonical forward consumption after exact batch normalization.");
                }
            }
        }
    }

    private void positiveCycles() {
        List<Route> routes = snapshot.routes().values().stream().filter(route -> !route.oneShot()).toList();
        for (int i = 0; i < routes.size(); i++) {
            Route forward = routes.get(i);
            if (hasEnvironment(forward)) {
                continue;
            }
            for (int j = i + 1; j < routes.size(); j++) {
                Route reverse = routes.get(j);
                if (hasEnvironment(reverse)) {
                    continue;
                }
                for (var bridge : forward.retainedOutputs().entrySet()) {
                    Long reverseInput = reverse.inputs().get(bridge.getKey());
                    if (reverseInput == null || bridge.getKey().kind() == Kind.ENERGY) {
                        continue;
                    }
                    BigInteger produced = BigInteger.valueOf(bridge.getValue());
                    BigInteger consumed = BigInteger.valueOf(reverseInput);
                    BigInteger gcd = produced.gcd(consumed);
                    Map<Resource, BigInteger> balance = new TreeMap<>();
                    scaledNet(balance, forward, consumed.divide(gcd));
                    scaledNet(balance, reverse, produced.divide(gcd));
                    if (balance.entrySet().stream().filter(entry -> entry.getKey().kind() != Kind.ENERGY)
                            .allMatch(entry -> entry.getValue().signum() >= 0)
                            && balance.values().stream().anyMatch(value -> value.signum() > 0)) {
                        add("resource_positive_cycle", bridge.getKey().key(),
                                forward.id() + "+" + reverse.id(), WARNING,
                                "Normalized positive-gain cycle without an environmental material source needs review.");
                    }
                }
            }
        }
        Map<Resource, Set<Resource>> edges = new TreeMap<>();
        for (Route route : routes) {
            route.inputs().keySet().stream().filter(resource -> resource.kind() != Kind.ENERGY).forEach(input ->
                    edges.computeIfAbsent(input, ignored -> new TreeSet<>()).addAll(route.retainedOutputs().keySet()));
        }
        Set<Resource> processed = new HashSet<>();
        for (Resource start : edges.keySet()) {
            if (processed.contains(start)) {
                continue;
            }
            Set<Resource> reachable = reachable(start, edges);
            Set<Resource> component = new TreeSet<>();
            for (Resource other : reachable) {
                if (reachable(other, edges).contains(start)) {
                    component.add(other);
                }
            }
            processed.addAll(component);
            List<Route> members = routes.stream().filter(route -> route.inputs().keySet().stream().anyMatch(component::contains)
                    && route.retainedOutputs().keySet().stream().anyMatch(component::contains)).toList();
            if (members.isEmpty() || members.stream().anyMatch(this::hasEnvironment)) {
                continue;
            }
            boolean unproven = members.size() > component.size();
            for (Resource carrier : component) {
                Map<String, BigInteger> batches = normalizedBatches(members, carrier);
                if (batches == null) {
                    unproven = true;
                    continue;
                }
                Map<Resource, BigInteger> net = balance(members, batches);
                if (net.entrySet().stream().filter(entry -> entry.getKey().kind() != Kind.ENERGY)
                        .allMatch(entry -> entry.getValue().signum() >= 0)
                        && net.values().stream().anyMatch(value -> value.signum() > 0)) {
                    add("resource_positive_cycle", carrier.key(),
                            String.join("+", members.stream().map(Route::id).toList()), WARNING,
                            "Exact normalized positive-gain cycle needs review: " + batches);
                }
            }
            if (unproven) {
                add("cycle_balance_unproven", start.key(), "", UNKNOWN,
                        "The bounded batch-vector review cannot exclude other cycles in this component: "
                                + members.stream().map(Route::id).toList());
            }
        }
    }

    Map<String, BigInteger> loopBatches(ClosedLoop loop) {
        List<Route> members = loop.memberRoutes().stream().map(snapshot.routes()::get).toList();
        return members.isEmpty() || members.stream().anyMatch(java.util.Objects::isNull)
                ? null : normalizedBatches(members, loop.carrier());
    }

    private static Map<Resource, BigInteger> balance(List<Route> routes, Map<String, BigInteger> batches) {
        Map<Resource, BigInteger> result = new TreeMap<>();
        routes.forEach(route -> scaledNet(result, route, batches.get(route.id())));
        return result;
    }

    /** Attempts an exact positive integer batch witness; failure is unproven, not infeasibility. */
    static Map<String, BigInteger> normalizedBatches(List<Route> routes, Resource carrier) {
        if (routes.size() > 32) {
            return null;
        }
        Set<Resource> inputs = new TreeSet<>();
        Set<Resource> outputs = new TreeSet<>();
        routes.forEach(route -> {
            inputs.addAll(route.inputs().keySet());
            outputs.addAll(route.retainedOutputs().keySet());
        });
        inputs.retainAll(outputs);
        inputs.remove(carrier);
        Map<String, BigInteger> batches = new TreeMap<>();
        routes.forEach(route -> batches.put(route.id(), BigInteger.ONE));
        Map<Resource, BigInteger> unit = balance(routes, batches);
        if (unit.getOrDefault(carrier, BigInteger.ZERO).signum() == 0
                && inputs.stream().allMatch(resource -> unit.getOrDefault(resource, BigInteger.ZERO).signum() >= 0)) {
            return batches;
        }
        if (inputs.stream().allMatch(resource -> unit.getOrDefault(resource, BigInteger.ZERO).signum() == 0)) {
            return batches;
        }
        if (inputs.size() > 128) {
            return null;
        }
        List<BigInteger[]> rows = new ArrayList<>();
        for (Resource resource : inputs) {
            BigInteger[] row = new BigInteger[routes.size()];
            for (int column = 0; column < routes.size(); column++) {
                Route route = routes.get(column);
                row[column] = BigInteger.valueOf(route.retainedOutputs().getOrDefault(resource, 0L))
                        .subtract(BigInteger.valueOf(route.inputs().getOrDefault(resource, 0L)));
            }
            rows.add(row);
        }
        List<Integer> pivots = new ArrayList<>();
        for (int column = 0; column < routes.size() && pivots.size() < rows.size(); column++) {
            int pivot = pivots.size();
            int candidate = pivot;
            while (candidate < rows.size() && rows.get(candidate)[column].signum() == 0) {
                candidate++;
            }
            if (candidate == rows.size()) {
                continue;
            }
            BigInteger[] selected = rows.get(candidate);
            rows.set(candidate, rows.get(pivot));
            rows.set(pivot, selected);
            reduceRow(selected);
            for (int below = pivot + 1; below < rows.size(); below++) {
                BigInteger[] row = rows.get(below);
                BigInteger multiplier = row[column];
                if (multiplier.signum() != 0) {
                    BigInteger divisor = selected[column];
                    for (int entry = column; entry < row.length; entry++) {
                        row[entry] = row[entry].multiply(divisor).subtract(selected[entry].multiply(multiplier));
                    }
                    reduceRow(row);
                }
            }
            pivots.add(column);
        }
        if (pivots.size() == routes.size()) {
            return null;
        }
        BigInteger[] values = new BigInteger[routes.size()];
        java.util.Arrays.fill(values, BigInteger.ONE);
        pivots.forEach(column -> values[column] = BigInteger.ZERO);
        for (int pivot = pivots.size() - 1; pivot >= 0; pivot--) {
            int column = pivots.get(pivot);
            BigInteger[] row = rows.get(pivot);
            BigInteger sum = BigInteger.ZERO;
            for (int entry = column + 1; entry < values.length; entry++) {
                sum = sum.add(row[entry].multiply(values[entry]));
            }
            BigInteger denominator = row[column];
            BigInteger scale = denominator.abs().divide(denominator.abs().gcd(sum.abs()));
            for (int entry = 0; entry < values.length; entry++) {
                values[entry] = values[entry].multiply(scale);
            }
            values[column] = sum.negate().multiply(scale).divide(denominator);
        }
        if (java.util.Arrays.stream(values).anyMatch(value -> value.signum() <= 0)) {
            return null;
        }
        BigInteger gcd = java.util.Arrays.stream(values).reduce(BigInteger.ZERO, BigInteger::gcd);
        for (int column = 0; column < routes.size(); column++) {
            batches.put(routes.get(column).id(), values[column].divide(gcd));
        }
        Map<Resource, BigInteger> normalized = balance(routes, batches);
        return inputs.stream().allMatch(resource -> normalized.getOrDefault(resource, BigInteger.ZERO).signum() == 0)
                ? batches : null;
    }

    private static void reduceRow(BigInteger[] row) {
        BigInteger gcd = java.util.Arrays.stream(row).reduce(BigInteger.ZERO, (left, right) -> left.gcd(right.abs()));
        if (gcd.signum() > 0 && !gcd.equals(BigInteger.ONE)) {
            for (int column = 0; column < row.length; column++) {
                row[column] = row[column].divide(gcd);
            }
        }
    }

    private static Set<Resource> reachable(Resource start, Map<Resource, Set<Resource>> edges) {
        Set<Resource> seen = new HashSet<>();
        List<Resource> pending = new ArrayList<>(List.of(start));
        for (int i = 0; i < pending.size(); i++) {
            Resource resource = pending.get(i);
            if (seen.add(resource)) {
                pending.addAll(edges.getOrDefault(resource, Set.of()));
            }
        }
        return seen;
    }

    Route canonical(Resource resource, int tier, boolean diagnose) {
        ResourceMetadata metadata = annotations.resources().get(resource);
        if (metadata != null && metadata.canonicalRoute() != null) {
            Route route = snapshot.routes().get(metadata.canonicalRoute());
            if (route == null || outputAmount(route, resource) == 0) {
                if (diagnose) {
                    add("canonical_route_invalid", resource.key(), "", ERROR,
                            "Canonical route must actually produce this resource.");
                }
                return null;
            }
            return unlocked(route, tier) ? route : null;
        }
        List<Route> candidates = snapshot.routes().values().stream().filter(route ->
                outputAmount(route, resource) > 0 && unlocked(route, tier) && !optional(route)
                        && !route.oneShot()).toList();
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (candidates.size() > 1 && diagnose) {
            add("canonical_route_required", resource.key(), "T" + tier, ERROR,
                    "Multiple non-optional producers require an explicit canonical route: "
                            + candidates.stream().map(Route::id).toList());
        }
        return null;
    }

    boolean unlocked(Route route, int tier) {
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy == null || policy.unlockTier() > tier) {
            return false;
        }
        if (route.family() == null) {
            return true;
        }
        Machine machine = annotations.machines().get(route.family());
        return machine != null && machine.unlockTier() <= tier;
    }

    boolean has(Set<Resource> resources, Resource want) {
        if (resources.contains(want)) {
            return true;
        }
        TagResolution tag = snapshot.tags().get(want);
        return tag != null && tag.members().stream().anyMatch(resources::contains);
    }

    long outputAmount(Route route, Resource resource) {
        long direct = route.retainedOutputs().getOrDefault(resource, 0L);
        if (direct > 0) {
            return direct;
        }
        TagResolution tag = snapshot.tags().get(resource);
        if (tag != null) {
            return route.retainedOutputs().entrySet().stream().filter(entry -> tag.members().contains(entry.getKey()))
                    .mapToLong(Map.Entry::getValue).sum();
        }
        for (var output : route.retainedOutputs().entrySet()) {
            TagResolution producedTag = snapshot.tags().get(output.getKey());
            if (producedTag != null && producedTag.complete() && producedTag.members().size() == 1
                    && producedTag.members().contains(resource)) {
                return output.getValue();
            }
        }
        return 0;
    }

    boolean environmentReady(Route route, Set<Resource> resources) {
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy == null) {
            return false;
        }
        return policy.environmentalSources().stream().allMatch(id -> {
            Terminal terminal = snapshot.terminals().get(id);
            return terminal != null && validTerminal(terminal) && terminal.kind() == TerminalKind.ENVIRONMENT
                    && has(resources, terminal.resource());
        });
    }

    boolean canRenew(Route route, int tier, Set<Resource> available, Set<Resource> renewable,
                     Set<String> availableRoutes) {
        return unlocked(route, tier) && !route.oneShot() && !optional(route)
                && (route.modOwned() || route.authority() == Authority.VANILLA_TRANSFORMATION)
                && !zeroInputUnjustified(route) && requirements(route, available, availableRoutes, tier)
                && contains(available, route.inputs().keySet()) && contains(available, route.catalysts().keySet())
                && contains(renewable, route.netInputs().keySet()) && environmentReady(route, renewable);
    }

    private boolean requirements(Route route, Set<Resource> available, Set<String> routes, int tier) {
        if (!routes.containsAll(route.prerequisites())) {
            return false;
        }
        if (route.family() == null) {
            return true;
        }
        Machine machine = annotations.machines().get(route.family());
        return machine != null && machine.unlockTier() <= tier && has(available, machine.construction());
    }

    private boolean contains(Set<Resource> resources, Set<Resource> wanted) {
        return wanted.stream().allMatch(resource -> has(resources, resource));
    }

    private boolean expandTags(Set<Resource> resources) {
        boolean changed = false;
        for (var entry : snapshot.tags().entrySet()) {
            TagResolution tag = entry.getValue();
            if (tag.members().stream().anyMatch(resources::contains)) {
                changed |= resources.add(entry.getKey());
            }
            if (resources.contains(entry.getKey()) && tag.complete() && tag.members().size() == 1) {
                changed |= resources.add(tag.members().iterator().next());
            }
        }
        return changed;
    }

    private boolean addOutputs(Set<Resource> resources, Route route) {
        boolean changed = false;
        for (Resource resource : route.retainedOutputs().keySet()) {
            if (resolvableOutput(resource)) {
                changed |= resources.add(resource);
            }
        }
        return changed;
    }

    private boolean resolvableOutput(Resource resource) {
        TagResolution tag = snapshot.tags().get(resource);
        return resource.kind() != Kind.TAG || (tag != null && tag.complete() && !tag.members().isEmpty());
    }

    boolean zeroInputUnjustified(Route route) {
        boolean gain = route.retainedOutputs().entrySet().stream().anyMatch(output ->
                output.getValue() > route.inputs().getOrDefault(output.getKey(), 0L));
        return !route.oneShot() && gain && route.netInputs().keySet().stream().noneMatch(resource ->
                resource.kind() == Kind.ITEM || resource.kind() == Kind.FLUID || resource.kind() == Kind.TAG)
                && !hasEnvironment(route);
    }

    private boolean hasEnvironment(Route route) {
        RoutePolicy policy = annotations.routes().get(route.id());
        return policy != null && !policy.environmentalSources().isEmpty()
                && policy.environmentalSources().stream().allMatch(id -> {
                    Terminal source = snapshot.terminals().get(id);
                    return source != null && source.kind() == TerminalKind.ENVIRONMENT && validTerminal(source);
                });
    }

    private static boolean validTerminal(Terminal terminal) {
        return terminal.justification() != null && !terminal.justification().isBlank()
                && (terminal.kind() != TerminalKind.ENVIRONMENT
                || terminal.site() != null && terminal.resource().kind() == Kind.ENVIRONMENT);
    }

    private boolean optional(Route route) {
        RoutePolicy policy = annotations.routes().get(route.id());
        return policy != null && policy.optional();
    }

    private boolean finiteSource(Resource resource) {
        boolean direct = snapshot.terminals().values().stream().anyMatch(terminal -> terminal.resource().equals(resource)
                && !terminal.renewable())
                || snapshot.routes().values().stream().anyMatch(route -> route.oneShot() && outputAmount(route, resource) > 0);
        TagResolution tag = snapshot.tags().get(resource);
        return direct || tag != null && tag.members().stream().anyMatch(member ->
                snapshot.terminals().values().stream().anyMatch(terminal -> terminal.resource().equals(member)
                        && !terminal.renewable()));
    }

    private boolean vanillaSource(Resource resource) {
        TagResolution tag = snapshot.tags().get(resource);
        return snapshot.terminals().values().stream().anyMatch(terminal ->
                (terminal.resource().equals(resource) || tag != null && tag.members().contains(terminal.resource()))
                && switch (terminal.kind()) {
                    case WORLD_GENERATION, MOB_DROP, LOOT, TREE_CROP_GROWTH, BLOCK_GENERATOR, TRADING -> true;
                    default -> !terminal.modOwned();
                });
    }

    private Resource uniqueTerminalMember(Resource resource) {
        TagResolution tag = snapshot.tags().get(resource);
        if (tag == null) {
            return resource;
        }
        List<Resource> members = tag.members().stream().filter(member -> snapshot.terminals().values().stream()
                .anyMatch(terminal -> terminal.resource().equals(member))).toList();
        return members.size() == 1 ? members.get(0) : null;
    }

    Set<String> consumers(Resource resource) {
        Set<String> consumers = new TreeSet<>();
        snapshot.routes().values().stream().filter(route -> !route.oneShot()
                && (route.netInputs().containsKey(resource) || route.catalysts().containsKey(resource)))
                .forEach(route -> consumers.add(route.id()));
        return consumers;
    }

    private boolean canonicalConsumption(Resource resource, int tier, Set<Resource> mandatory) {
        return snapshot.routes().values().stream().anyMatch(route -> route.netInputs().containsKey(resource)
                && route.retainedOutputs().keySet().stream().anyMatch(output -> mandatory.contains(output)
                && canonical(output, tier, false) != null
                && canonical(output, tier, false).id().equals(route.id())));
    }

    private boolean validCatalystOrigin(Resource spent, Resource origin) {
        return origin != null && !origin.equals(spent) && snapshot.resources().contains(origin)
                && snapshot.routes().values().stream().anyMatch(route -> outputAmount(route, spent) > 0
                && (route.inputs().containsKey(origin) || route.catalysts().containsKey(origin)));
    }

    private boolean hasFuturePlan(ResourceMetadata metadata) {
        return !metadata.futureConsumers().isEmpty() && metadata.futureConsumers().stream().allMatch(this::validFutureConsumer);
    }

    private boolean validFutureConsumer(FutureConsumer consumer) {
        CommittedPlan plan = annotations.plans().get(consumer.planReference());
        return plan != null && plan.tier() == consumer.tier() && plan.system().equals(consumer.system());
    }

    private int firstProductionTier(Resource resource) {
        return snapshot.routes().values().stream().filter(route -> outputAmount(route, resource) > 0)
                .map(route -> annotations.routes().get(route.id())).filter(java.util.Objects::nonNull)
                .mapToInt(RoutePolicy::unlockTier).min().orElse(16);
    }

    private static boolean renewableClass(ResourceClass type) {
        return type == ResourceClass.RENEWABLE || type == ResourceClass.CLOSED_LOOP || type == ResourceClass.LOSSY_LOOP;
    }

    private static void scaledNet(Map<Resource, BigInteger> net, Route route, BigInteger batches) {
        route.inputs().forEach((resource, amount) -> net.merge(resource,
                BigInteger.valueOf(amount).multiply(batches).negate(), BigInteger::add));
        route.retainedOutputs().forEach((resource, amount) -> net.merge(resource,
                BigInteger.valueOf(amount).multiply(batches), BigInteger::add));
    }

    private void add(String rule, String subject, String detail, AuditFinding.Severity severity, String message) {
        List<String> sources = new ArrayList<>();
        Route route = snapshot.routes().get(subject);
        if (route != null) {
            sources.add(route.source());
        } else {
            snapshot.routes().values().stream().filter(candidate ->
                    candidate.inputs().keySet().stream().anyMatch(resource -> resource.key().equals(subject))
                            || candidate.outputs().keySet().stream().anyMatch(resource -> resource.key().equals(subject))
                            || candidate.catalysts().keySet().stream().anyMatch(resource -> resource.key().equals(subject)))
                    .map(Route::source).forEach(sources::add);
            snapshot.terminals().values().stream().filter(terminal -> terminal.resource().key().equals(subject))
                    .map(Terminal::source).forEach(sources::add);
        }
        finding(new AuditFinding(rule, subject, detail, severity, message, sources));
    }

    private void finding(AuditFinding finding) {
        findings.merge(finding.fingerprint(), finding.active(), AuditFinding::merge);
    }
}
