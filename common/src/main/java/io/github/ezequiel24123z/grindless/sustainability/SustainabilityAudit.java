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
    /** Gifts, claimed rewards and conditional player custody remain separate budget axes. */
    public record Reserve(Resource resource, long guaranteed, long playerProvided, long claimedRewards,
                          long required, long margin, String status) {
        /** Preserves the S1 exact-guarantee constructor without inventing player supplies. */
        public Reserve(Resource resource, long guaranteed, long required, long margin, String status) {
            this(resource, guaranteed, 0, 0, required, margin, status);
        }
    }

    /** Local crank actions are not an ENERGY reserve and cannot establish R on their own. */
    public record WorkReserve(String terminal, long budgetActions, long requiredActions,
                              long marginActions, String status) {
    }

    /** Concrete exit production, not a promise that a manual goal is an automatic renewable root. */
    public record GoalWitness(Resource resource, String route, long requiredCount, long batchOutput,
                              long executedBatches, long produced, String status) {
    }

    /** One completed authority batch: physical withdrawals exclude non-portable local work. */
    public record ExecutedBatch(String route, Map<Resource, Long> consumed,
                                Map<Resource, Long> produced, long manualActions) {
        public ExecutedBatch {
            consumed = sortedMap(consumed);
            produced = sortedMap(produced);
        }
    }

    /**
     * Immutable in-process custody, issued only by this auditor, never caller-supplied inventory.
     * Delivery totals are accounting facts; only freeStock can fund another withdrawal.
     */
    public static final class WitnessState {
        private final Snapshot source;
        private final SustainabilityAnnotations assumptions;
        private final TierResult evidence;
        private final DebtManifest debt;
        private final DebtManifest.Applied findings;
        private final int tier;
        private final ConstructionReserve.Custody custody;

        private WitnessState(Result result, TierResult evidence, int tier, ConstructionReserve.Custody custody) {
            source = result.authorities();
            assumptions = result.annotations();
            this.evidence = evidence;
            debt = result.debt();
            DebtManifest.Applied applied = result.findings();
            findings = new DebtManifest.Applied(List.copyOf(applied.findings()), applied.ratchetPass(),
                    applied.activeErrors(), applied.waivedDebt(), applied.warnings(), applied.unknowns());
            this.tier = tier;
            this.custody = custody;
        }

        private boolean matches(Result result) {
            TierResult actual = result.tiers().get(0);
            return tier == 0 && source.equals(result.authorities()) && assumptions.equals(result.annotations())
                    && debt.equals(result.debt()) && findings.equals(result.findings())
                    && actual != null && evidence.equals(actual.withoutWitness());
        }

        public int tier() { return tier; }
        public Map<Resource, Long> freeStock() { return custody.stock(); }
        public Map<Resource, Long> retainedCatalysts() { return custody.locked(); }
        public Set<Resource> installedMachines() { return custody.installed(); }
        public Map<String, Map<Resource, Long>> installationParts() { return custody.installationRequirements(); }
        public Set<String> claimedRoutes() { return custody.claimed(); }
        public Map<String, Long> bootstrapSources() { return custody.bootstrapSources(); }
        public Map<Resource, Long> guaranteedDeliveries() { return custody.guaranteed(); }
        public Map<Resource, Long> playerDeliveries() { return custody.playerDelivered(); }
        public Map<Resource, Long> rewardDeliveries() { return custody.claimedRewards(); }
        public Map<Resource, Long> cumulativeDeliveries() { return custody.delivered(); }
        public Map<Resource, Long> peakDraw() { return custody.peak(); }
        public long consumedManualActions() { return custody.manualActions(); }
        public long requiredManualActions() { return custody.requiredManualActions(); }
        public int operations() { return custody.steps(); }
        public Map<String, Long> establishmentBatches() { return custody.executions(); }
        public List<ExecutedBatch> history() { return custody.history(); }
    }

    /** A finite continuation is not recursive renewable closure, continuous operation or world evidence. */
    public record FiniteResult(boolean admitted, boolean completed, String status,
                               WitnessState predecessor, WitnessState state,
                               List<ExecutedBatch> batches, List<Reserve> reserves, WorkReserve manualWork,
                               GoalWitness exitProduction, List<AuditFinding> findings) {
        public FiniteResult {
            batches = List.copyOf(batches);
            reserves = List.copyOf(reserves);
            findings = List.copyOf(findings);
        }
    }

    /** Tier facts; renewable excludes finite circulating stock, and an unknown ledger is not a pass. */
    public record TierResult(int tier, Set<Resource> mandatory, Set<Resource> recurring,
                             Set<Resource> available, Set<Resource> renewable,
                             Set<String> availableRoutes, Set<String> renewableRoutes,
                             List<Reserve> reserves, Map<Resource, Long> retainedInventory, WorkReserve manualWork,
                             GoalWitness exitProduction, Map<String, Long> establishmentBatches, String unknown,
                             WitnessState witnessState) {
        public TierResult {
            mandatory = sortedSet(mandatory);
            recurring = sortedSet(recurring);
            available = sortedSet(available);
            renewable = sortedSet(renewable);
            availableRoutes = sortedSet(availableRoutes);
            renewableRoutes = sortedSet(renewableRoutes);
            reserves = List.copyOf(reserves);
            retainedInventory = sortedMap(retainedInventory);
            establishmentBatches = sortedMap(establishmentBatches);
        }

        /** Preserves all pre-continuation callers; report fields alone cannot manufacture custody. */
        public TierResult(int tier, Set<Resource> mandatory, Set<Resource> recurring,
                          Set<Resource> available, Set<Resource> renewable,
                          Set<String> availableRoutes, Set<String> renewableRoutes,
                          List<Reserve> reserves, Map<Resource, Long> retainedInventory, WorkReserve manualWork,
                          GoalWitness exitProduction, Map<String, Long> establishmentBatches, String unknown) {
            this(tier, mandatory, recurring, available, renewable, availableRoutes, renewableRoutes, reserves,
                    retainedInventory, manualWork, exitProduction, establishmentBatches, unknown, null);
        }

        private TierResult withoutWitness() {
            return new TierResult(tier, mandatory, recurring, available, renewable, availableRoutes, renewableRoutes,
                    reserves, retainedInventory, manualWork, exitProduction, establishmentBatches, unknown);
        }

        private TierResult withWitness(WitnessState state) {
            return new TierResult(tier, mandatory, recurring, available, renewable, availableRoutes, renewableRoutes,
                    reserves, retainedInventory, manualWork, exitProduction, establishmentBatches, unknown, state);
        }

        /** Preserves the result constructor used before completed-batch provenance was exported. */
        public TierResult(int tier, Set<Resource> mandatory, Set<Resource> recurring,
                          Set<Resource> available, Set<Resource> renewable,
                          Set<String> availableRoutes, Set<String> renewableRoutes,
                          List<Reserve> reserves, Map<Resource, Long> retainedInventory, WorkReserve manualWork,
                          GoalWitness exitProduction, String unknown) {
            this(tier, mandatory, recurring, available, renewable, availableRoutes, renewableRoutes,
                    reserves, retainedInventory, manualWork, exitProduction, Map.of(), unknown);
        }

        /** Preserves unprofiled S1 result construction. */
        public TierResult(int tier, Set<Resource> mandatory, Set<Resource> recurring,
                          Set<Resource> available, Set<Resource> renewable,
                          Set<String> availableRoutes, Set<String> renewableRoutes,
                          List<Reserve> reserves, String unknown) {
            this(tier, mandatory, recurring, available, renewable, availableRoutes, renewableRoutes,
                    reserves, Map.of(), null, null, unknown);
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
    private final Map<Integer, ConstructionReserve.Custody> finiteStates = new TreeMap<>();
    private int derivingTier = -1;
    private EntryProfile derivedEntry;

    SustainabilityAudit(Snapshot snapshot, SustainabilityAnnotations annotations) {
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
        Result result = new Result(snapshot, annotations, tiers, debt,
                debt.apply(new ArrayList<>(audit.findings.values())));
        TierResult t0 = tiers.get(0);
        TierPlan plan = annotations.ledger().get(0);
        if (plan != null && plan.entryProfile() != null && t0.unknown() == null
                && audit.finiteStates.containsKey(0) && T0ConditionalGate.evaluate(result).pass()) {
            tiers.put(0, t0.withWitness(new WitnessState(result, t0, 0, audit.finiteStates.get(0))));
            result = new Result(snapshot, annotations, tiers, debt, result.findings());
        }
        return result;
    }

    /**
     * Continues the completed conditional T0 witness on the same authorities and entry assumptions.
     * The successor may select real routes/machine bindings, but cannot add stock, profiles or gifts.
     */
    public static FiniteResult continueFinite(Result predecessor, SustainabilityAnnotations successor,
                                              List<String> selectedRoutes, String exitRoute) {
        FiniteResult refusal = predecessorRefusal(predecessor);
        if (refusal != null) {
            return refusal;
        }
        TierResult t0 = predecessor.tiers().get(0);
        WitnessState state = t0.witnessState();
        if (successor == null || selectedRoutes == null || selectedRoutes.isEmpty()
                || new HashSet<>(selectedRoutes).size() != selectedRoutes.size()
                || exitRoute == null || !selectedRoutes.contains(exitRoute)
                || !preservesEntry(predecessor.annotations(), successor, selectedRoutes)) {
            return refused("continuation_incompatible", "Keep the predecessor's entry, classifications, loops and plans; "
                    + "select distinct live batches and a real manufacturing exit.");
        }
        SustainabilityAudit audit = new SustainabilityAudit(predecessor.authorities(), successor);
        for (String id : selectedRoutes) {
            Route route = predecessor.authorities().routes().get(id);
            if (route == null || !audit.unlocked(route, 1) || route.oneShot()
                    || !exitProductionRoute(route)) {
                return refused("continuation_route_unavailable", "No applicable repeatable selected authority: " + id);
            }
        }
        ConstructionReserve reserve = new ConstructionReserve(predecessor.authorities(), successor, audit,
                t0.available(), t0.renewable(), state.custody, state.assumptions.ledger().get(0).entryProfile(),
                selectedRoutes, t0.exitProduction().route());
        boolean completed = reserve.continueFinite(selectedRoutes, exitRoute);
        List<Reserve> reserves = reserve.reserves();
        completed &= reserve.findings().isEmpty();
        WitnessState after = new WitnessState(predecessor, null, 1, reserve.custody());
        return new FiniteResult(true, completed, completed ? "conditional_established" : "blocked",
                state, after, after.history().subList(state.history().size(), after.history().size()),
                reserves, reserve.manualWorkReserve(), reserve.exitProduction(), reserve.findings());
    }

    /** Shared admission precedes even the T1-specific selection/binding projection. */
    static FiniteResult predecessorRefusal(Result predecessor) {
        if (predecessor == null) {
            return refused("predecessor_missing", "A completed conditional T0 audit is required.");
        }
        TierResult t0 = predecessor.tiers().get(0);
        if (t0 == null || t0.unknown() != null) {
            return refused("predecessor_unknown", "An unknown or absent T0 ledger is not a finite predecessor.");
        }
        if (!T0ConditionalGate.evaluate(predecessor).pass()) {
            return refused("predecessor_failed", "The actual conditional T0 gate must pass without suppressed findings.");
        }
        WitnessState state = t0.witnessState();
        if (state == null) {
            return refused("predecessor_state_missing", "Report totals and retainedInventory are not complete custody.");
        }
        if (!state.matches(predecessor)) {
            return refused("predecessor_incompatible", "Authority, source, assumptions or evaluated T0 evidence changed.");
        }
        return null;
    }

    private static boolean preservesEntry(SustainabilityAnnotations before, SustainabilityAnnotations after,
                                          List<String> selected) {
        return before.resources().equals(after.resources()) && before.loops().equals(after.loops())
                && before.plans().equals(after.plans()) && before.ledger().equals(after.ledger())
                && before.machines().entrySet().stream().allMatch(entry ->
                entry.getValue().equals(after.machines().get(entry.getKey())))
                && before.routes().entrySet().stream().allMatch(entry -> {
                    RoutePolicy next = after.routes().get(entry.getKey());
                    return entry.getValue().equals(next) || selected.contains(entry.getKey())
                            && entry.getValue().unlockTier() == null && next != null
                            && Integer.valueOf(1).equals(next.unlockTier())
                            && entry.getValue().environmentalSources().equals(next.environmentalSources())
                            && entry.getValue().outputRoles().equals(next.outputRoles());
                }) && after.routes().keySet().stream().allMatch(id ->
                before.routes().containsKey(id) || selected.contains(id));
    }

    private static FiniteResult refused(String rule, String message) {
        return new FiniteResult(false, false, "refused", null, null, List.of(), List.of(), null, null,
                List.of(new AuditFinding(rule, "finite_continuation", "T1", ERROR, message, List.of())));
    }

    /** A candidate is useful only together with the fresh, finite replay, not the derivation run. */
    public record EntryDerivation(EntryProfile profile, List<AuditFinding> derivationFindings, Result replay) {
        public EntryDerivation {
            derivationFindings = List.copyOf(derivationFindings);
        }
    }

    /**
     * Derives a sufficient, not globally minimal, bill from actual execution deficits.
     * Only the returned replay executes with that finite bill; derivation cannot certify a tier.
     */
    public static EntryDerivation deriveEntryProfile(Snapshot snapshot, SustainabilityAnnotations annotations,
                                                     DebtManifest debt, int tier) {
        TierPlan plan = annotations.ledger().get(tier);
        if (plan == null || plan.entryProfile() == null) {
            throw new IllegalArgumentException("Entry derivation requires an explicitly conditional profile template");
        }
        SustainabilityAudit derivation = new SustainabilityAudit(snapshot, annotations);
        derivation.derivingTier = tier;
        derivation.validateDeclarations();
        derivation.analyseTier(tier);
        EntryProfile profile = derivation.derivedEntry;
        if (profile == null) {
            return new EntryDerivation(null, new ArrayList<>(derivation.findings.values()),
                    run(snapshot, annotations, debt));
        }
        Map<Integer, TierPlan> ledger = new TreeMap<>(annotations.ledger());
        ledger.put(tier, plan.withEntryProfile(profile));
        SustainabilityAnnotations finite = new SustainabilityAnnotations(annotations.resources(), annotations.routes(),
                annotations.machines(), annotations.loops(), annotations.plans(), ledger);
        return new EntryDerivation(profile, new ArrayList<>(derivation.findings.values()), run(snapshot, finite, debt));
    }

    EntryProfile entryProfile(int tier) {
        TierPlan plan = annotations.ledger().get(tier);
        return plan == null ? null : plan.entryProfile();
    }

    boolean deriving(int tier) {
        return derivingTier == tier;
    }

    private void includeEntryWitness(ConstructionReserve witness, int tier) {
        if (!deriving(tier)) {
            return;
        }
        EntryProfile bill = witness.derivedProfile();
        if (bill == null) {
            return;
        }
        Map<Resource, Long> provided = new TreeMap<>(bill.playerProvided());
        if (derivedEntry != null) {
            derivedEntry.playerProvided().forEach((resource, amount) -> provided.merge(resource, amount, Math::max));
        }
        // Independent proofs replay separately; they do not consume one another's finite custody.
        derivedEntry = bill.withBill(provided, Math.max(bill.manualWorkActions(),
                derivedEntry == null ? 0 : derivedEntry.manualWorkActions()));
    }

    boolean manualWorkReady(Route route, int tier, Set<Resource> available) {
        return manualWorkReady(route, tier, available, entryProfile(tier));
    }

    boolean manualWorkReady(Route route, int tier, Set<Resource> available, EntryProfile profile) {
        ManualWork work = profile == null ? null : profile.manualWork();
        if (work == null || !work.applies(route) || !deriving(tier) && profile.manualWorkActions() == 0) {
            return false;
        }
        Terminal source = snapshot.terminals().get(work.terminal());
        return source != null && source.kind() == TerminalKind.MANUAL_LABOUR
                && source.resource().kind() == Kind.ENVIRONMENT && source.unlockTier() <= tier
                && validTerminal(source) && has(available, work.tool());
    }

    private boolean inputsAvailable(Route route, int tier, Set<Resource> available) {
        return route.inputs().keySet().stream().allMatch(resource -> has(available, resource)
                || resource.equals(ENERGY) && manualWorkReady(route, tier, available));
    }

    private void validateDeclarations() {
        for (TierPlan plan : annotations.ledger().values()) {
            EntryProfile profile = plan.entryProfile();
            if (profile == null) {
                continue;
            }
            for (Resource resource : profile.playerProvided().keySet()) {
                if (snapshot.terminals().values().stream().noneMatch(source ->
                        source.resource().equals(resource) && source.kind() == TerminalKind.MANUAL_WORLD
                                && source.unlockTier() <= plan.tier() && validTerminal(source))) {
                    add("entry_profile_source_invalid", resource.key(), "T" + plan.tier(), ERROR,
                            "Conditional custody must name an actual, justified manual acquisition authority.");
                }
            }
            ManualWork work = profile.manualWork();
            Terminal source = work == null ? null : snapshot.terminals().get(work.terminal());
            if (work != null && (source == null || source.kind() != TerminalKind.MANUAL_LABOUR
                    || source.resource().kind() != Kind.ENVIRONMENT || !validTerminal(source))) {
                add("entry_work_source_invalid", profile.id(), "T" + plan.tier(), ERROR,
                        "Manual work needs a real, scoped labour source, not a portable ENERGY terminal.");
            }
        }
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
            if (policy == null || policy.unlockTier() == null) {
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
                        && inputsAvailable(route, tier, available)
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
        for (String id : plan.requiredQuests()) {
            addObjectiveRoots(id, roots, new HashSet<>(), plan.exitObjective());
        }
        if (plan.exitObjective() != null) {
            roots.remove(plan.exitObjective());
        }
        Set<Resource> mandatory = new TreeSet<>();
        Set<Resource> recurring = new TreeSet<>();
        Set<String> closureRoutes = new TreeSet<>();
        Set<Resource> construction = new TreeSet<>();
        Set<Resource> visitedRecurring = new TreeSet<>();
        for (Resource root : roots) {
            closure(root, true, tier, mandatory, recurring, closureRoutes, construction, visitedRecurring);
        }
        if (plan.exitObjective() != null) {
            mandatory.add(plan.exitObjective());
            Route exit = canonical(plan.exitObjective(), tier, true);
            if (exit == null || outputAmount(exit, plan.exitObjective()) <= 0) {
                add("exit_goal_route_missing", plan.exitObjective().key(), "T" + tier, ERROR,
                        "An exit goal needs a concrete unlocked production route, not just stock or a gift.");
            } else if (!exitProductionRoute(exit)) {
                add("exit_goal_route_invalid", plan.exitObjective().key(), "T" + tier, ERROR,
                        "A manufacturing goal cannot select a quest claim, gift or other one-shot supply.");
            } else {
                closureRoute(exit, true, tier, mandatory, recurring, closureRoutes, construction, visitedRecurring);
            }
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
        Set<Resource> establishmentConstruction = new TreeSet<>(construction);
        Set<String> establishmentRoutes = new TreeSet<>(closureRoutes);
        Set<Resource> productionObligations = new TreeSet<>(roots);
        if (plan.entryProfile() != null) {
            Set<Resource> proofResources = new TreeSet<>(mandatory);
            Set<Resource> proofRecurring = new TreeSet<>(recurring);
            Set<Resource> proofVisited = new TreeSet<>(visitedRecurring);
            for (var entry : annotations.resources().entrySet()) {
                ResourceMetadata metadata = entry.getValue();
                if (renewableClass(metadata.resourceClass()) && metadata.firstRenewableTier() != null
                        && metadata.firstRenewableTier() <= tier) {
                    productionObligations.add(entry.getKey());
                    closure(entry.getKey(), true, tier, proofResources, proofRecurring, establishmentRoutes,
                            establishmentConstruction, proofVisited);
                }
            }
            handlingClosure(tier, proofResources, proofRecurring, establishmentRoutes,
                    establishmentConstruction, proofVisited);
        }
        ConstructionReserve reserve = new ConstructionReserve(snapshot, annotations, this, tier, available, renewable);
        List<Reserve> reserves = reserve.analyse(establishmentConstruction, establishmentRoutes, productionObligations);
        finiteStates.put(tier, reserve.custody());
        includeEntryWitness(reserve, tier);
        reserve.findings().forEach(this::finding);
        return new TierResult(tier, mandatory, recurring, available, renewable, availableRoutes,
                renewableRoutes, reserves, reserve.retainedInventory(), reserve.manualWorkReserve(),
                reserve.exitProduction(), reserve.establishmentBatches(), null);
    }

    private void addObjectiveRoots(String id, Set<Resource> roots, Set<String> visited, Resource exitGoal) {
        if (!visited.add(id)) {
            return;
        }
        Objective objective = snapshot.objectives().get(id);
        if (objective == null || objective.subject() == null) {
            add("mandatory_objective_unknown", id, "", ERROR, "Selected quest has no physical authority.");
            return;
        }
        if (!objective.subject().equals(exitGoal)) {
            roots.add(objective.subject());
        }
        objective.requires().forEach(required -> addObjectiveRoots(required, roots, visited, exitGoal));
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
        EntryProfile profile = entryProfile(tier);
        if (profile != null && profile.manualWork() != null && route.inputs().containsKey(ENERGY)
                && profile.manualWork().applies(route) && (deriving(tier) || profile.manualWorkActions() > 0)) {
            closure(profile.manualWork().tool(), false, tier, mandatory, recurring, routes,
                    construction, visitedRecurring);
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
            if (!route.automatable()) {
                if (diagnose) {
                    add("loop_manual_route", loop.id(), route.id(), ERROR,
                            "Manual execution cannot establish an automatic renewable circulation.");
                }
                return null;
            }
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
        includeEntryWitness(witness, tier);
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
        for (CycleComponent component : cycleComponents(snapshot)) {
            List<Route> members = component.routes();
            if (members.isEmpty() || members.stream().anyMatch(this::hasEnvironment)) {
                continue;
            }
            boolean unproven = members.size() > component.resources().size();
            for (Resource carrier : component.resources()) {
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
            if (unproven && !unitPotentialConservative(members)) {
                add("cycle_balance_unproven", component.representative().key(), "", UNKNOWN,
                        "The bounded batch-vector review cannot exclude other cycles in this component: "
                                + members.stream().map(Route::id).toList());
            }
        }
    }

    record CycleComponent(Resource representative, Set<Resource> resources, List<Route> routes) {
        CycleComponent {
            resources = sortedSet(resources);
            routes = List.copyOf(routes);
        }
    }

    static List<CycleComponent> cycleComponents(Snapshot snapshot) {
        List<Route> routes = snapshot.routes().values().stream().filter(route -> !route.oneShot()).toList();
        Map<Resource, Set<Resource>> edges = new TreeMap<>();
        for (Route route : routes) {
            route.inputs().keySet().stream().filter(resource -> resource.kind() != Kind.ENERGY).forEach(input ->
                    edges.computeIfAbsent(input, ignored -> new TreeSet<>()).addAll(route.retainedOutputs().keySet()));
        }
        List<CycleComponent> components = new ArrayList<>();
        Set<Resource> processed = new HashSet<>();
        for (Resource start : edges.keySet()) {
            if (processed.contains(start)) {
                continue;
            }
            Set<Resource> component = new TreeSet<>();
            for (Resource other : reachable(start, edges)) {
                if (reachable(other, edges).contains(start)) {
                    component.add(other);
                }
            }
            processed.addAll(component);
            List<Route> members = routes.stream().filter(route -> route.inputs().keySet().stream().anyMatch(component::contains)
                    && route.retainedOutputs().keySet().stream().anyMatch(component::contains)).toList();
            components.add(new CycleComponent(start, component, members));
        }
        return List.copyOf(components);
    }

    /** Sufficient for this audit's gain predicate, not a physical mass or renewability claim. */
    static boolean unitPotentialConservative(List<Route> routes) {
        return !routes.isEmpty() && routes.stream().allMatch(route -> {
            Map<Resource, BigInteger> net = balance(List.of(route), Map.of(route.id(), BigInteger.ONE));
            BigInteger material = net.entrySet().stream().filter(entry -> entry.getKey().kind() != Kind.ENERGY)
                    .map(Map.Entry::getValue).reduce(BigInteger.ZERO, BigInteger::add);
            return material.signum() <= 0 && net.entrySet().stream()
                    .filter(entry -> entry.getKey().kind() == Kind.ENERGY)
                    .allMatch(entry -> entry.getValue().signum() <= 0);
        });
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

    static boolean exitProductionRoute(Route route) {
        return route.authority() != Authority.QUEST_REWARD && !route.oneShot();
    }

    boolean unlocked(Route route, int tier) {
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy == null || policy.unlockTier() == null || policy.unlockTier() > tier) {
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
        return unlocked(route, tier) && route.automatable() && !route.oneShot() && !optional(route)
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
                && !hasEnvironment(route) && !hasManualInput(route);
    }

    private boolean hasManualInput(Route route) {
        return !route.automatable() && route.netInputs().keySet().stream().anyMatch(resource ->
                resource.kind() == Kind.ENVIRONMENT && snapshot.terminals().values().stream().anyMatch(source ->
                        source.resource().equals(resource) && source.kind() == TerminalKind.MANUAL_LABOUR
                                && !source.automatable() && source.guaranteed() == null && validTerminal(source)));
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
                .map(route -> annotations.routes().get(route.id()))
                .filter(policy -> policy != null && policy.unlockTier() != null)
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
