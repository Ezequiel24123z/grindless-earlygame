package io.github.ezequiel24123z.grindless.sustainability;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Executes a finite establishment witness against exact authority batches. Machines and seed
 * catalysts cannot be spent twice; conditional quest rewards cannot fund their own prerequisites.
 */
final class ConstructionReserve {
    private static final Resource WORK_ACTIONS = new Resource(Kind.ENVIRONMENT,
            "witness:remaining_local_work_actions");
    private final Snapshot snapshot;
    private final SustainabilityAnnotations annotations;
    private final SustainabilityAudit audit;
    private final int tier;
    private final Set<Resource> available;
    private final Set<Resource> renewable;
    private final EntryProfile profile;
    private final boolean deriving;
    private final List<Route> selection;
    private final String completedEntryRoute;
    private final Map<String, Long> bootstrapSources = new TreeMap<>();
    private final Map<Resource, Long> guaranteed = new TreeMap<>();
    private final Map<Resource, Long> playerDelivered = new TreeMap<>();
    private final Map<Resource, Long> claimedRewards = new TreeMap<>();
    private final Map<Resource, Long> delivered = new TreeMap<>();
    private final Map<Resource, Long> stock = new TreeMap<>();
    private final Map<Resource, Long> peak = new TreeMap<>();
    private final Map<Resource, Long> shortages = new TreeMap<>();
    private final Map<Resource, Long> locked = new TreeMap<>();
    private final Set<Resource> installed = new TreeSet<>();
    private final Map<String, Map<Resource, Long>> installationRequirements = new TreeMap<>();
    private final Set<Resource> pending = new HashSet<>();
    private final Set<String> claimed = new HashSet<>();
    private final Set<String> pendingRoutes = new HashSet<>();
    private final Set<String> preparing = new HashSet<>();
    private final Set<String> circulating = new HashSet<>();
    private final List<AuditFinding> findings = new ArrayList<>();
    private boolean uncertain;
    private boolean startupBound;
    private int steps;
    private long manualActions;
    private long requiredManualActions;
    private SustainabilityAudit.GoalWitness exitProduction;
    private final Map<String, Long> executions = new TreeMap<>();
    private final List<SustainabilityAudit.ExecutedBatch> history = new ArrayList<>();

    /** Internal immutable checkpoint; only SustainabilityAudit can issue an admissible witness. */
    record Custody(Map<Resource, Long> guaranteed, Map<Resource, Long> playerDelivered,
                   Map<Resource, Long> claimedRewards, Map<Resource, Long> delivered,
                   Map<Resource, Long> stock, Map<Resource, Long> peak, Map<Resource, Long> locked,
                   Set<Resource> installed, Map<String, Map<Resource, Long>> installationRequirements,
                   Set<String> claimed, Map<String, Long> bootstrapSources, long manualActions,
                   long requiredManualActions, int steps, Map<String, Long> executions,
                   List<SustainabilityAudit.ExecutedBatch> history) {
        Custody {
            guaranteed = sortedMap(guaranteed);
            playerDelivered = sortedMap(playerDelivered);
            claimedRewards = sortedMap(claimedRewards);
            delivered = sortedMap(delivered);
            stock = sortedMap(stock);
            peak = sortedMap(peak);
            locked = sortedMap(locked);
            installed = sortedSet(installed);
            Map<String, Map<Resource, Long>> parts = new TreeMap<>();
            installationRequirements.forEach((family, requirements) -> parts.put(family, sortedMap(requirements)));
            installationRequirements = sortedMap(parts);
            claimed = sortedSet(claimed);
            bootstrapSources = sortedMap(bootstrapSources);
            executions = sortedMap(executions);
            history = List.copyOf(history);
        }
    }

    ConstructionReserve(Snapshot snapshot, SustainabilityAnnotations annotations, SustainabilityAudit audit,
                        int tier, Set<Resource> available, Set<Resource> renewable) {
        this.snapshot = snapshot;
        this.annotations = annotations;
        this.audit = audit;
        this.tier = tier;
        this.available = available;
        this.renewable = renewable;
        this.profile = audit.entryProfile(tier);
        this.deriving = audit.deriving(tier);
        this.selection = List.of();
        this.completedEntryRoute = null;
        for (Terminal terminal : snapshot.terminals().values()) {
            if (terminal.kind() == TerminalKind.EXACT_BOOTSTRAP && terminal.unlockTier() <= tier
                    && terminal.justification() != null && !terminal.justification().isBlank()) {
                merge(guaranteed, terminal.resource(), terminal.guaranteed());
                merge(delivered, terminal.resource(), terminal.guaranteed());
                merge(stock, terminal.resource(), terminal.guaranteed());
                bootstrapSources.put(terminal.id(), terminal.guaranteed());
            }
        }
    }

    /** No terminal loading or budget reset is permitted on this path. */
    ConstructionReserve(Snapshot snapshot, SustainabilityAnnotations annotations, SustainabilityAudit audit,
                        Set<Resource> available, Set<Resource> renewable, Custody prior, EntryProfile profile,
                        List<String> selected, String completedEntryRoute) {
        this.snapshot = snapshot;
        this.annotations = annotations;
        this.audit = audit;
        this.tier = 1;
        this.available = available;
        this.renewable = renewable;
        this.profile = profile;
        this.deriving = false;
        this.selection = selected.stream().map(snapshot.routes()::get).toList();
        this.completedEntryRoute = completedEntryRoute;
        guaranteed.putAll(prior.guaranteed());
        playerDelivered.putAll(prior.playerDelivered());
        claimedRewards.putAll(prior.claimedRewards());
        delivered.putAll(prior.delivered());
        stock.putAll(prior.stock());
        peak.putAll(prior.peak());
        locked.putAll(prior.locked());
        installed.addAll(prior.installed());
        prior.installationRequirements().forEach((family, parts) ->
                installationRequirements.put(family, new TreeMap<>(parts)));
        claimed.addAll(prior.claimed());
        bootstrapSources.putAll(prior.bootstrapSources());
        manualActions = prior.manualActions();
        requiredManualActions = prior.requiredManualActions();
        steps = prior.steps();
        executions.putAll(prior.executions());
        history.addAll(prior.history());
    }

    Custody custody() {
        return new Custody(guaranteed, playerDelivered, claimedRewards, delivered, stock, peak, locked, installed,
                installationRequirements, claimed, bootstrapSources, manualActions, requiredManualActions,
                steps, executions, history);
    }

    /** Execute the selected finite batches in teaching order, not another closure/planning algorithm. */
    boolean continueFinite(List<String> selected, String exitId) {
        Map<String, Long> before = new TreeMap<>(executions);
        boolean completed = true;
        for (String id : selected) {
            if (id.equals(exitId)) {
                continue;
            }
            Route route = snapshot.routes().get(id);
            if (executions.getOrDefault(id, 0L).equals(before.getOrDefault(id, 0L)) && !execute(route, 1)) {
                findings.add(new AuditFinding("finite_route_unexecuted", id, "T1", AuditFinding.Severity.ERROR,
                        "The selected real batch could not execute from conserved predecessor custody.",
                        List.of(route.source())));
                completed = false;
                break;
            }
            for (Machine machine : annotations.machines().values()) {
                if (route.retainedOutputs().containsKey(machine.construction()) && !install(machine.construction())) {
                    completed = false;
                    break;
                }
            }
            if (!completed) {
                break;
            }
        }
        Route exit = snapshot.routes().get(exitId);
        Resource goal = exit.retainedOutputs().size() == 1
                ? exit.retainedOutputs().keySet().iterator().next() : null;
        long count = goal == null ? 0 : audit.outputAmount(exit, goal);
        long beforeExit = executions.getOrDefault(exitId, 0L);
        completed = goal != null && count > 0 && completed && execute(exit, 1);
        long batches = executions.getOrDefault(exitId, 0L) - beforeExit;
        exitProduction = new SustainabilityAudit.GoalWitness(goal, exitId, count, count, batches,
                Math.multiplyExact(count, batches), completed ? "conditional_established" : "unproved");
        if (!completed) {
            findings.add(new AuditFinding("finite_exit_unexecuted", exitId, "T1", AuditFinding.Severity.ERROR,
                    "The actual exit batch must execute; inherited or caller-reported goal stock cannot replace it.",
                    List.of(exit.source())));
        }
        return completed;
    }

    List<AuditFinding> findings() {
        return List.copyOf(findings);
    }

    EntryProfile derivedProfile() {
        return profile == null ? null : profile.withBill(playerDelivered, requiredManualActions);
    }

    SustainabilityAudit.WorkReserve manualWorkReserve() {
        if (profile == null || profile.manualWork() == null) {
            return null;
        }
        long budget = deriving ? requiredManualActions : profile.manualWorkActions();
        long margin = Math.subtractExact(budget, requiredManualActions);
        return new SustainabilityAudit.WorkReserve(profile.manualWork().terminal(), budget, requiredManualActions,
                margin, margin < 0 ? "insufficient" : uncertain ? "unknown" : "conditional_established");
    }

    SustainabilityAudit.GoalWitness exitProduction() {
        return exitProduction;
    }

    Map<String, Long> establishmentBatches() {
        return profile == null || deriving ? Map.of() : sortedMap(executions);
    }

    Map<Resource, Long> retainedInventory() {
        return sortedMap(locked);
    }

    List<SustainabilityAudit.Reserve> analyse(Set<Resource> construction, Set<String> closureRoutes,
                                            Set<Resource> roots) {
        // Establish a finite feed's own renewable producer before spending that feed elsewhere.
        for (Resource resource : guaranteed.keySet()) {
            if (audit.has(renewable, resource)) {
                Route route = replenishing(resource);
                if (route != null && route.family() != null) {
                    install(annotations.machines().get(route.family()).construction());
                }
            }
        }
        for (Resource block : construction) {
            install(block);
        }
        for (ClosedLoop loop : annotations.loops().values()) {
            if (roots.contains(loop.carrier()) || loop.memberRoutes().stream().anyMatch(closureRoutes::contains)) {
                Map<String, BigInteger> batches = audit.loopBatches(loop);
                if (batches != null) {
                    circulate(loop, batches);
                }
            }
        }
        for (String id : closureRoutes) {
            Route route = snapshot.routes().get(id);
            if (route == null || !audit.unlocked(route, tier)) {
                continue;
            }
            TierPlan plan = annotations.ledger().get(tier);
            Route exit = plan.exitObjective() == null ? null : audit.canonical(plan.exitObjective(), tier, false);
            if (exit != null && exit.id().equals(id)) {
                continue;
            }
            if (formed(route)) {
                retainInstallationRequirements(route);
            } else {
                for (var catalyst : route.catalysts().entrySet()) {
                    if (!route.oneShot()) {
                        lock(catalyst.getKey(), catalyst.getValue());
                    }
                }
            }
            boolean loopMember = annotations.loops().values().stream().anyMatch(loop -> loop.memberRoutes().contains(id));
            boolean constructionRoute = construction.stream().anyMatch(block -> {
                Route producer = audit.canonical(block, tier, false);
                return producer != null && producer.id().equals(id);
            });
            if (!loopMember && !constructionRoute && !route.oneShot()
                    && route.retainedOutputs().keySet().stream().anyMatch(resource -> audit.has(renewable, resource))) {
                execute(route, 1);
            }
        }
        if (profile != null) {
            for (Resource root : roots) {
                if (owned(root) < 1) {
                    obtain(root, 1);
                }
            }
            for (String id : annotations.ledger().get(tier).requiredQuests()) {
                establishObjective(id, new HashSet<>());
            }
        }
        establishExitGoal();
        return reserves();
    }

    List<SustainabilityAudit.Reserve> reserves() {
        List<SustainabilityAudit.Reserve> result = new ArrayList<>();
        Set<Resource> reserved = new TreeSet<>(guaranteed.keySet());
        reserved.addAll(claimedRewards.keySet());
        reserved.addAll(profile == null ? Set.of() : deriving ? playerDelivered.keySet() : profile.playerProvided().keySet());
        reserved.addAll(shortages.keySet());
        for (Resource resource : reserved) {
            long amount = guaranteed.getOrDefault(resource, 0L);
            long provided = profile == null ? 0 : (deriving ? playerDelivered : profile.playerProvided())
                    .getOrDefault(resource, 0L);
            long rewards = claimedRewards.getOrDefault(resource, 0L);
            long total = Math.addExact(Math.addExact(amount, provided), rewards);
            long missing = shortages.getOrDefault(resource, 0L);
            long required = Math.max(peak.getOrDefault(resource, 0L), missing > 0 ? Math.addExact(total, missing) : 0);
            long margin = Math.subtractExact(total, required);
            String status = margin < 0 ? "insufficient" : uncertain ? "unknown"
                    : profile == null ? "established" : "conditional_established";
            result.add(new SustainabilityAudit.Reserve(resource, amount, provided, rewards, required, margin, status));
            if (margin < 0) {
                findings.add(new AuditFinding(profile == null ? "bootstrap_starvation" : "entry_profile_insufficient",
                        resource.key(), "T" + tier,
                        AuditFinding.Severity.ERROR,
                        "Guaranteed " + amount + ", conditional player custody " + provided + " and actually claimed "
                                + rewards + " cannot cover establishment draw " + required
                                + " including construction and recoverable seed; margin " + margin + ".",
                        sourceReferences(resource)));
            }
        }
        return result;
    }

    private void establishExitGoal() {
        TierPlan plan = annotations.ledger().get(tier);
        Resource goal = plan.exitObjective();
        if (goal == null) {
            return;
        }
        Route route = audit.canonical(goal, tier, false);
        long batch = route == null ? 0 : audit.outputAmount(route, goal);
        if (route == null || batch <= 0 || !SustainabilityAudit.exitProductionRoute(route)) {
            return;
        }
        long required = batch;
        Set<String> objectives = new TreeSet<>(plan.requiredQuests());
        Set<String> visited = new HashSet<>();
        while (!objectives.isEmpty()) {
            String id = objectives.iterator().next();
            objectives.remove(id);
            if (!visited.add(id)) {
                continue;
            }
            Objective objective = snapshot.objectives().get(id);
            if (objective != null) {
                if (goal.equals(objective.subject())) {
                    required = Math.max(required, objective.count());
                }
                objectives.addAll(objective.requires());
            }
        }
        long batches = 1 + (required - 1) / batch;
        boolean supply = route.netInputs().keySet().stream().allMatch(resource -> audit.has(renewable, resource));
        long before = executions.getOrDefault(route.id(), 0L);
        boolean completed = supply && execute(route, batches);
        long executed = executions.getOrDefault(route.id(), 0L) - before;
        exitProduction = new SustainabilityAudit.GoalWitness(goal, route.id(), required, batch,
                executed, Math.multiplyExact(batch, executed),
                completed ? profile == null ? "established" : "conditional_established" : "unproved");
        if (!supply || !completed) {
            findings.add(new AuditFinding(supply ? "exit_goal_production_unproven" : "exit_goal_supply_not_renewable",
                    goal.key(), "T" + tier, AuditFinding.Severity.ERROR,
                    supply ? "The required real exit batch was not executed; existing goal stock cannot replace it."
                            : "Every net-consumed exit input needs renewable supply even for a manual exit craft.",
                    List.of(route.source())));
        }
    }

    private void establishObjective(String id, Set<String> visited) {
        if (!visited.add(id)) {
            return;
        }
        Objective objective = snapshot.objectives().get(id);
        if (objective == null || objective.subject() == null) {
            return;
        }
        objective.requires().forEach(required -> establishObjective(required, visited));
        if (!objective.subject().equals(annotations.ledger().get(tier).exitObjective())
                && owned(objective.subject()) < objective.count()) {
            obtain(objective.subject(), objective.count() - owned(objective.subject())
                    + free(objective.subject()));
        }
    }

    boolean circulate(ClosedLoop loop, Map<String, BigInteger> batches) {
        if (!circulating.add(loop.id())) {
            return false;
        }
        try {
            return circulateBatches(loop, batches);
        } finally {
            circulating.remove(loop.id());
        }
    }

    private boolean circulateBatches(ClosedLoop loop, Map<String, BigInteger> batches) {
        BigInteger operations = batches.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
        if (operations.compareTo(BigInteger.valueOf(4096)) > 0) {
            unknown(loop.carrier(), "Normalized circulation exceeds the executable witness bound.");
            return false;
        }
        Set<Resource> internal = new TreeSet<>();
        Set<Resource> outputs = new TreeSet<>();
        for (String id : batches.keySet()) {
            Route route = snapshot.routes().get(id);
            if (route == null || !prepare(route)) {
                return false;
            }
            internal.addAll(route.inputs().keySet());
            outputs.addAll(route.retainedOutputs().keySet());
        }
        internal.retainAll(outputs);
        Map<Resource, Long> borrowed = new TreeMap<>();
        for (Resource resource : internal) {
            long held = locked.getOrDefault(resource, 0L);
            if (held > 0) {
                borrowed.put(resource, held);
                locked.remove(resource);
                merge(stock, resource, held);
            }
        }
        Map<String, Long> remaining = new TreeMap<>();
        batches.forEach((id, count) -> remaining.put(id, count.longValueExact()));
        boolean completed = false;
        try {
            startupBound = false;
            List<String> order = startupOrder(new TreeMap<>(remaining), startupInventory(), internal,
                    batches.keySet(), new ArrayList<>(), new HashSet<>(), new int[1]);
            if (order == null && !startupBound && profile != null) {
                for (Resource resource : internal) {
                    if (manualMembers(resource).isEmpty()) {
                        continue;
                    }
                    long gross = 0;
                    for (var entry : remaining.entrySet()) {
                        gross = Math.addExact(gross, Math.multiplyExact(entry.getValue(),
                                snapshot.routes().get(entry.getKey()).inputs().getOrDefault(resource, 0L)));
                    }
                    // A conservative prefix bound, paid once from remaining custody, never from the same R.
                    if (gross > free(resource)) {
                        provideManual(resource, gross);
                    }
                }
                order = startupOrder(new TreeMap<>(remaining), startupInventory(), internal,
                        batches.keySet(), new ArrayList<>(), new HashSet<>(), new int[1]);
            }
            if (order == null && startupBound) {
                unknown(loop.carrier(), "Startup ordering exceeded its bounded search; no infeasibility is claimed.");
                return false;
            }
            Map<Resource, Long> initial = new TreeMap<>(stock);
            Map<Resource, Long> minimum = new TreeMap<>(stock);
            long energyBalance = stock.getOrDefault(ENERGY, 0L);
            long minimumEnergy = energyBalance;
            int position = 0;
            while (remaining.values().stream().anyMatch(count -> count > 0)) {
                boolean progressed = false;
                for (var entry : remaining.entrySet()) {
                    if (entry.getValue() == 0 || order != null && !entry.getKey().equals(order.get(position))) {
                        continue;
                    }
                    Route route = snapshot.routes().get(entry.getKey());
                    boolean ready = true;
                    for (var input : route.inputs().entrySet()) {
                        if (free(input.getKey()) < input.getValue()
                                && !(input.getKey().equals(ENERGY) && localWork(route))
                                && (!circulationSupply(input.getKey(), internal, batches.keySet())
                                || !obtain(input.getKey(), input.getValue()))) {
                            ready = false;
                            break;
                        }
                    }
                    if (!ready) {
                        continue;
                    }
                    for (var input : route.inputs().entrySet()) {
                        if (initial.containsKey(input.getKey())) {
                            minimum.merge(input.getKey(),
                                    stock.getOrDefault(input.getKey(), 0L) - input.getValue(), Math::min);
                        }
                    }
                    if (!execute(route, 1)) {
                        return false;
                    }
                    energyBalance = Math.subtractExact(energyBalance, route.inputs().getOrDefault(ENERGY, 0L));
                    minimumEnergy = Math.min(minimumEnergy, energyBalance);
                    energyBalance = Math.addExact(energyBalance, route.retainedOutputs().getOrDefault(ENERGY, 0L));
                    for (Resource resource : initial.keySet()) {
                        minimum.merge(resource, stock.getOrDefault(resource, 0L), Math::min);
                    }
                    entry.setValue(entry.getValue() - 1);
                    position++;
                    progressed = true;
                    break;
                }
                if (!progressed) {
                    Resource missing = null;
                    long deficit = Long.MAX_VALUE;
                    for (var entry : remaining.entrySet()) {
                        if (entry.getValue() == 0) {
                            continue;
                        }
                        for (var input : snapshot.routes().get(entry.getKey()).inputs().entrySet()) {
                            Resource resource = input.getKey();
                            long need = input.getValue() - free(resource);
                            if (need > 0 && internal.contains(resource) && !audit.has(renewable, resource)
                                    && (resource.equals(loop.carrier()) || guaranteed.containsKey(resource))
                                    && need < deficit) {
                                missing = resource;
                                deficit = need;
                            }
                        }
                    }
                    if (missing != null) {
                        seedShortage(missing, deficit);
                    } else {
                        unknown(loop.carrier(), "No executable normalized startup ordering was established.");
                    }
                    return false;
                }
            }
            for (Resource resource : internal) {
                long before = initial.getOrDefault(resource, 0L);
                if (free(resource) < before && !obtain(resource, before)) {
                    return false;
                }
                long seed = Math.max(borrowed.getOrDefault(resource, 0L),
                        before - minimum.getOrDefault(resource, before));
                if (seed > 0 && !lock(resource, seed)) {
                    return false;
                }
            }
            if (profile != null && internal.contains(ENERGY)) {
                // Startup labour may fill the first cycle, never the next one: retain real native FU.
                long nativeSeed = Math.subtractExact(initial.getOrDefault(ENERGY, 0L), minimumEnergy);
                if (nativeSeed > 0 && !lock(ENERGY, nativeSeed)) {
                    return false;
                }
            }
            completed = true;
            return true;
        } finally {
            if (!completed) {
                borrowed.forEach((resource, amount) -> {
                    if (free(resource) >= amount) {
                        lock(resource, amount);
                    }
                });
            }
        }
    }

    private Map<Resource, Long> startupInventory() {
        Map<Resource, Long> simulated = new TreeMap<>(stock);
        if (profile != null && profile.manualWork() != null) {
            simulated.put(WORK_ACTIONS, deriving ? Long.MAX_VALUE : profile.manualWorkActions() - manualActions);
        }
        return simulated;
    }

    private boolean circulationSupply(Resource resource, Set<Resource> internal, Set<String> members) {
        if (!audit.has(renewable, resource)) {
            return false;
        }
        if (!internal.contains(resource)) {
            return true;
        }
        Route source = replenishing(resource);
        return source != null && !members.contains(source.id()) || snapshot.terminals().values().stream()
                .anyMatch(terminal -> terminal.resource().equals(resource) && terminal.renewable());
    }

    /** Searches actual batch orderings; exhausting the bound is not proof of insufficient seed. */
    private List<String> startupOrder(Map<String, Long> remaining, Map<Resource, Long> inventory,
                                      Set<Resource> internal, Set<String> members, List<String> prefix,
                                      Set<String> visited, int[] states) {
        if (++states[0] > 4096 || prefix.size() > 128) {
            startupBound = true;
            return null;
        }
        if (remaining.values().stream().allMatch(count -> count == 0)) {
            return List.copyOf(prefix);
        }
        if (!visited.add(remaining + "|" + inventory)) {
            return null;
        }
        for (var entry : remaining.entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }
            Route route = snapshot.routes().get(entry.getKey());
            Map<Resource, Long> next = new TreeMap<>(inventory);
            boolean ready = true;
            for (var input : route.inputs().entrySet()) {
                if (circulationSupply(input.getKey(), internal, members)) {
                    continue;
                }
                long shortage = input.getValue() - next.getOrDefault(input.getKey(), 0L);
                if (input.getKey().equals(ENERGY) && shortage > 0 && localWork(route)) {
                    long actions = actions(shortage);
                    if (next.getOrDefault(WORK_ACTIONS, 0L) >= actions) {
                        next.put(WORK_ACTIONS, next.get(WORK_ACTIONS) - actions);
                        next.put(ENERGY, 0L);
                        continue;
                    }
                }
                if (!spendSimulated(next, input.getKey(), input.getValue())) {
                    ready = false;
                    break;
                }
            }
            if (!ready) {
                continue;
            }
            producedOutputs(route).forEach((resource, amount) -> merge(next, resource, amount));
            entry.setValue(entry.getValue() - 1);
            prefix.add(entry.getKey());
            List<String> result = startupOrder(remaining, next, internal, members, prefix, visited, states);
            prefix.remove(prefix.size() - 1);
            entry.setValue(entry.getValue() + 1);
            if (result != null || startupBound) {
                return result;
            }
        }
        return null;
    }

    private boolean spendSimulated(Map<Resource, Long> inventory, Resource resource, long amount) {
        long direct = Math.min(amount, inventory.getOrDefault(resource, 0L));
        inventory.put(resource, inventory.getOrDefault(resource, 0L) - direct);
        long missing = amount - direct;
        TagResolution tag = snapshot.tags().get(resource);
        if (tag != null) {
            for (Resource member : tag.members()) {
                long used = Math.min(missing, inventory.getOrDefault(member, 0L));
                inventory.put(member, inventory.getOrDefault(member, 0L) - used);
                missing -= used;
            }
        }
        return missing == 0;
    }

    private boolean install(Resource block) {
        if (installed.contains(block)) {
            return true;
        }
        if (!obtain(block, 1)) {
            return false;
        }
        take(block, 1);
        installed.add(block);
        return true;
    }

    private boolean lock(Resource resource, long amount) {
        long held = locked.getOrDefault(resource, 0L);
        if (held >= amount) {
            return true;
        }
        if (!obtain(resource, amount - held)) {
            return false;
        }
        take(resource, amount - held);
        locked.put(resource, amount);
        return true;
    }

    private boolean retainInstallationRequirements(Route route) {
        Map<Resource, Long> retained = installationRequirements.computeIfAbsent(route.family(), ignored -> new TreeMap<>());
        for (var requirement : route.catalysts().entrySet()) {
            long held = retained.getOrDefault(requirement.getKey(), 0L);
            long extra = requirement.getValue() - held;
            if (extra > 0) {
                if (!obtain(requirement.getKey(), extra)) {
                    return false;
                }
                take(requirement.getKey(), extra);
                retained.put(requirement.getKey(), requirement.getValue());
            }
        }
        return true;
    }

    private static boolean formed(Route route) {
        return route.family() != null && route.conditions().containsKey("workshop_structure");
    }

    private boolean obtain(Resource resource, long amount) {
        if (free(resource) >= amount) {
            return true;
        }
        if (++steps > 4096) {
            unknown(resource, "Establishment witness exceeded its operation bound; no reserve pass is claimed.");
            return false;
        }
        boolean environment = snapshot.terminals().values().stream().anyMatch(source ->
                source.resource().equals(resource) && source.unlockTier() <= tier && source.renewable());
        if (environment) {
            stock.put(resource, amount);
            return true;
        }
        if (!pending.add(resource)) {
            if (provideManual(resource, amount)) {
                return true;
            }
            shortage(resource, amount - free(resource));
            return false;
        }
        int previousFindings = findings.size();
        Map<Resource, Long> previousShortages = new TreeMap<>(shortages);
        boolean previouslyUncertain = uncertain;
        try {
            for (Route source : snapshot.routes().values()) {
                if (source.authority() == Authority.QUEST_REWARD && source.oneShot()
                        && audit.outputAmount(source, resource) > 0 && audit.unlocked(source, tier)
                        && selectedClaim(source) && !claimed.contains(source.id())) {
                    execute(source, 1);
                    if (free(resource) >= amount) {
                        return true;
                    }
                }
            }
            Route route = replenishing(resource);
            Machine readyMachine = route == null || route.family() == null
                    ? null : annotations.machines().get(route.family());
            if (profile != null && audit.has(renewable, resource) && route != null && route.automatable()
                    && audit.unlocked(route, tier) && (route.family() == null
                    || readyMachine != null && installed.contains(readyMachine.construction()))
                    && audit.outputAmount(route, resource) > route.inputs().getOrDefault(resource, 0L)) {
                // Spend existing native feed before rebuilding an entire productive circulation.
                while (free(resource) < amount && route.inputs().entrySet().stream().allMatch(input ->
                        free(input.getKey()) >= input.getValue()) && execute(route, 1)) {
                    if (steps > 4096) {
                        break;
                    }
                }
                if (free(resource) >= amount) {
                    return true;
                }
            }
            if (profile != null && audit.has(renewable, resource)) {
                for (ClosedLoop loop : annotations.loops().values()) {
                    if (circulating.contains(loop.id())) {
                        continue;
                    }
                    Map<String, BigInteger> batches = audit.loopBatches(loop);
                    if (batches == null) {
                        continue;
                    }
                    BigInteger gain = BigInteger.ZERO;
                    for (var batch : batches.entrySet()) {
                        Route member = snapshot.routes().get(batch.getKey());
                        long net = Math.subtractExact(member.retainedOutputs().getOrDefault(resource, 0L),
                                member.inputs().getOrDefault(resource, 0L));
                        gain = gain.add(batch.getValue().multiply(BigInteger.valueOf(net)));
                    }
                    if (gain.signum() > 0) {
                        while (free(resource) < amount && circulate(loop, batches)) {
                            if (steps > 4096) {
                                break;
                            }
                        }
                        if (free(resource) >= amount) {
                            return true;
                        }
                    }
                }
            }
            if (route != null && audit.unlocked(route, tier)) {
                Machine machine = route.family() == null ? null : annotations.machines().get(route.family());
                if (machine != null && pending.contains(machine.construction()) && provideManual(resource, amount)) {
                    return true;
                }
                long yield = audit.outputAmount(route, resource) - route.inputs().getOrDefault(resource, 0L);
                if (yield > 0) {
                    long missing = amount - free(resource);
                    long batches = 1 + (missing - 1) / yield;
                    execute(route, batches);
                    if (free(resource) >= amount) {
                        return true;
                    }
                }
            }
            for (Route fallback : snapshot.routes().values()) {
                if (manualCraft(fallback) && (route == null || !fallback.id().equals(route.id()))) {
                    long yield = audit.outputAmount(fallback, resource)
                            - fallback.inputs().getOrDefault(resource, 0L);
                    if (yield > 0) {
                        long missing = amount - free(resource);
                        execute(fallback, 1 + (missing - 1) / yield);
                        if (free(resource) >= amount) {
                            return true;
                        }
                    }
                }
            }
            if (provideManual(resource, amount)) {
                return true;
            }
            shortage(resource, amount - free(resource));
            return false;
        } finally {
            if (free(resource) >= amount) {
                // Alternative branches are not obligations. Keep every completed sub-batch and its costs.
                findings.subList(previousFindings, findings.size()).clear();
                shortages.clear();
                shortages.putAll(previousShortages);
                uncertain = previouslyUncertain;
            }
            pending.remove(resource);
        }
    }

    private boolean manualCraft(Route route) {
        if (route.id().equals(completedEntryRoute)
                || route.automatable() || route.oneShot() || !audit.unlocked(route, tier)) {
            return false;
        }
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy == null || policy.optional()) {
            return false;
        }
        return switch (route.authority()) {
            case CRAFTING_JSON, BOOTSTRAP_RECIPES, T1_RECIPES -> true;
            case VANILLA_TRANSFORMATION -> route.durationTicks() == 0 && route.fuPerTick() == 0
                    && (route.family() == null || route.family().equals("CRAFTING_TABLE"))
                    && ("minecraft:crafting_shaped".equals(route.conditions().get("crafting_type"))
                    || "minecraft:crafting_shapeless".equals(route.conditions().get("crafting_type")));
            default -> false;
        };
    }

    private boolean selectedClaim(Route route) {
        RoutePolicy policy = annotations.routes().get(route.id());
        if (policy != null && !policy.optional()) {
            return true;
        }
        TierPlan plan = annotations.ledger().get(tier);
        return plan != null && plan.requiredQuests().stream()
                .map(snapshot.objectives()::get).filter(java.util.Objects::nonNull)
                .anyMatch(objective -> route.id().equals(objective.rewardRoute()));
    }

    private Route replenishing(Resource resource) {
        Route selectedFallback = null;
        if (completedEntryRoute != null) {
            List<Route> producers = selection.stream().filter(route ->
                    !route.id().equals(completedEntryRoute) && audit.outputAmount(route, resource)
                            > route.inputs().getOrDefault(resource, 0L)).toList();
            // Prefer a real selected automatic producer only after its machine is installed.
            for (Route route : producers) {
                Machine machine = route.family() == null ? null : annotations.machines().get(route.family());
                if (route.automatable() && (route.family() == null
                        || machine != null && installed.contains(machine.construction()))) {
                    return route;
                }
            }
            for (Route route : producers) {
                if (manualCraft(route)) {
                    return route;
                }
            }
            if (!producers.isEmpty()) {
                selectedFallback = producers.get(0);
            }
        }
        for (ClosedLoop loop : annotations.loops().values()) {
            if (loop.carrier().equals(resource) && loop.makeUpRoute() != null) {
                Route makeUp = snapshot.routes().get(loop.makeUpRoute());
                if (makeUp != null && audit.outputAmount(makeUp, resource) > 0) {
                    return makeUp;
                }
            }
        }
        Route canonical = audit.canonical(resource, tier, false);
        return canonical != null && !canonical.id().equals(completedEntryRoute) ? canonical : selectedFallback;
    }

    private boolean execute(Route route, long batches) {
        for (long batch = 0; batch < batches; batch++) {
            if (++steps > 4096) {
                unknown(route.retainedOutputs().keySet().iterator().next(),
                        "Establishment witness exceeded its operation bound; no reserve pass is claimed.");
                return false;
            }
            if (!executeBatch(route)) {
                return false;
            }
        }
        return true;
    }

    private boolean prepare(Route route) {
        if (!preparing.add(route.id())) {
            unknown(route.retainedOutputs().keySet().iterator().next(),
                    "Handling/construction preparation is circular; no establishment pass is claimed.");
            return false;
        }
        try {
            return prepareRequirements(route);
        } finally {
            preparing.remove(route.id());
        }
    }

    private boolean prepareRequirements(Route route) {
        if (!audit.unlocked(route, tier) || audit.zeroInputUnjustified(route)
                || !audit.environmentReady(route, available)) {
            return false;
        }
        if (route.family() != null) {
            Machine machine = annotations.machines().get(route.family());
            if (machine == null || !install(machine.construction())) {
                return false;
            }
            if (route.inputs().containsKey(ENERGY) && localWork(route)
                    && !lock(profile.manualWork().tool(), 1)) {
                return false;
            }
        }
        if (formed(route)) {
            if (!retainInstallationRequirements(route)) {
                return false;
            }
        } else {
            for (var catalyst : route.catalysts().entrySet()) {
                boolean ready = route.oneShot() ? obtainClaimRequirement(catalyst.getKey(), catalyst.getValue())
                        : lock(catalyst.getKey(), catalyst.getValue());
                if (!ready) {
                    return false;
                }
            }
        }
        Set<Resource> outputs = new TreeSet<>(route.retainedOutputs().keySet());
        outputs.addAll(producedOutputs(route).keySet());
        for (Resource output : outputs) {
            ResourceMetadata metadata = annotations.resources().get(output);
            Handling handling = metadata == null ? null : metadata.handling();
            if (handling == null || !handling.hazardous()
                    && metadata.sideStreamClass() != SideStreamClass.WASTE_HAZARDOUS) {
                continue;
            }
            if (handling.containment() != null && !install(handling.containment())) {
                return false;
            }
            Route treatment = handling.treatmentRoute() == null ? null
                    : snapshot.routes().get(handling.treatmentRoute());
            if (treatment != null && !treatment.id().equals(route.id()) && !prepare(treatment)) {
                return false;
            }
        }
        return true;
    }

    private boolean obtainClaimRequirement(Resource resource, long amount) {
        return obtain(resource, Math.max(0L, amount - locked.getOrDefault(resource, 0L)));
    }

    private boolean claimRequirementsCoexist(Route route) {
        for (var requirement : route.catalysts().entrySet()) {
            if (!obtainClaimRequirement(requirement.getKey(), requirement.getValue())) {
                return false;
            }
        }
        // Later preparation may consume an earlier requirement; only the final joint stock proves the claim.
        for (var requirement : route.catalysts().entrySet()) {
            if (free(requirement.getKey()) < requirement.getValue()
                    - locked.getOrDefault(requirement.getKey(), 0L)) {
                unknown(requirement.getKey(), "The claim's non-consumed requirements do not coexist after preparation.");
                return false;
            }
        }
        return true;
    }

    private boolean executeBatch(Route route) {
        if (route.id().equals(completedEntryRoute)
                || !audit.unlocked(route, tier) || audit.zeroInputUnjustified(route)
                || !audit.environmentReady(route, available) || route.oneShot() && claimed.contains(route.id())
                || !pendingRoutes.add(route.id())) {
            return false;
        }
        Map<Resource, Long> allocated = new TreeMap<>();
        Map<Resource, Long> borrowed = new TreeMap<>();
        long batchWork = 0;
        boolean completed = false;
        try {
            for (String id : route.prerequisites()) {
                Route required = snapshot.routes().get(id);
                if (!claimed.contains(id) && (required == null || !execute(required, 1))) {
                    return false;
                }
            }
            if (!prepare(route)) {
                return false;
            }
            for (var input : route.inputs().entrySet()) {
                long need = input.getValue();
                if (input.getKey().equals(ENERGY) && free(ENERGY) < need && localWork(route)
                        && (!audit.has(renewable, ENERGY) || !circulating.isEmpty() || pending.contains(ENERGY))) {
                    long held = Math.min(free(ENERGY), need);
                    long requested = actions(need - held);
                    requiredManualActions = Math.max(requiredManualActions, Math.addExact(manualActions, requested));
                    if (!deriving && requested > profile.manualWorkActions() - manualActions) {
                        findings.add(new AuditFinding("entry_work_insufficient", profile.id(), "T" + tier,
                                AuditFinding.Severity.ERROR, "The finite local-work action budget cannot fund "
                                + route.id() + "; no ENERGY stock is created.", List.of(profile.manualWork().source())));
                        return false;
                    }
                    manualActions = Math.addExact(manualActions, requested);
                    batchWork = Math.addExact(batchWork, requested);
                    take(ENERGY, held, allocated);
                    continue;
                }
                long held = locked.getOrDefault(input.getKey(), 0L);
                if (held > 0 && free(input.getKey()) < need
                        && route.retainedOutputs().getOrDefault(input.getKey(), 0L) >= need) {
                    long lend = Math.min(held, need - free(input.getKey()));
                    locked.put(input.getKey(), held - lend);
                    merge(stock, input.getKey(), lend);
                    borrowed.put(input.getKey(), lend);
                }
                if (!obtain(input.getKey(), need)) {
                    if (route.retainedOutputs().getOrDefault(input.getKey(), 0L) >= need
                            && !audit.has(renewable, input.getKey())) {
                        seedShortage(input.getKey(), need - free(input.getKey()));
                    }
                    return false;
                }
                take(input.getKey(), need, allocated);
            }
            if (route.oneShot() && !claimRequirementsCoexist(route)) {
                return false;
            }
            Map<Resource, Long> outputs = producedOutputs(route);
            for (var output : outputs.entrySet()) {
                merge(stock, output.getKey(), output.getValue());
            }
            if (route.oneShot()) {
                claimed.add(route.id());
                outputs.forEach((resource, amount) -> {
                    merge(delivered, resource, amount);
                    merge(claimedRewards, resource, amount);
                });
            }
            completed = true;
            executions.merge(route.id(), 1L, Math::addExact);
            history.add(new SustainabilityAudit.ExecutedBatch(route.id(), allocated, outputs, batchWork));
            return true;
        } finally {
            if (!completed) {
                allocated.forEach((resource, amount) -> merge(stock, resource, amount));
                manualActions = Math.subtractExact(manualActions, batchWork);
            }
            borrowed.forEach((resource, amount) -> {
                take(resource, amount);
                merge(locked, resource, amount);
            });
            pendingRoutes.remove(route.id());
        }
    }

    private Map<Resource, Long> producedOutputs(Route route) {
        Map<Resource, Long> outputs = new TreeMap<>();
        route.retainedOutputs().forEach((resource, amount) -> {
            TagResolution tag = snapshot.tags().get(resource);
            Resource exact = tag != null && tag.complete() && tag.members().size() == 1
                    ? tag.members().iterator().next() : resource;
            merge(outputs, exact, amount);
        });
        return outputs;
    }

    private long free(Resource resource) {
        long amount = stock.getOrDefault(resource, 0L);
        TagResolution tag = snapshot.tags().get(resource);
        if (tag != null) {
            for (Resource member : tag.members()) {
                amount = Math.addExact(amount, stock.getOrDefault(member, 0L));
            }
        }
        return amount;
    }

    private long owned(Resource resource) {
        long amount = free(resource);
        TagResolution tag = snapshot.tags().get(resource);
        Set<Resource> matches = new TreeSet<>(tag == null ? Set.of() : tag.members());
        matches.add(resource);
        for (Resource member : matches) {
            amount = Math.addExact(amount, locked.getOrDefault(member, 0L));
            if (installed.contains(member)) {
                amount = Math.addExact(amount, 1L);
            }
            for (Map<Resource, Long> parts : installationRequirements.values()) {
                amount = Math.addExact(amount, parts.getOrDefault(member, 0L));
            }
        }
        return amount;
    }

    private void take(Resource resource, long amount) {
        take(resource, amount, new TreeMap<>());
    }

    private void take(Resource resource, long amount, Map<Resource, Long> allocated) {
        long direct = Math.min(amount, stock.getOrDefault(resource, 0L));
        debit(resource, direct);
        if (direct > 0) {
            merge(allocated, resource, direct);
        }
        long remaining = amount - direct;
        TagResolution tag = snapshot.tags().get(resource);
        if (tag != null && remaining > 0) {
            for (Resource member : tag.members()) {
                long take = Math.min(remaining, stock.getOrDefault(member, 0L));
                debit(member, take);
                if (take > 0) {
                    merge(allocated, member, take);
                }
                remaining -= take;
            }
        }
        if (remaining > 0) {
            throw new IllegalStateException("Establishment witness overspent " + resource.key());
        }
    }

    private void debit(Resource resource, long amount) {
        stock.put(resource, stock.getOrDefault(resource, 0L) - amount);
        peak.merge(resource, Math.max(0L, delivered.getOrDefault(resource, 0L) - stock.get(resource)), Math::max);
    }

    private void shortage(Resource resource, long amount) {
        if (amount <= 0) {
            return;
        }
        if (guaranteed.containsKey(resource) || claimedRewards.containsKey(resource)
                || profile != null && manualMembers(resource).size() > 0) {
            shortages.merge(resource, amount, Math::max);
            return;
        }
        boolean manual = snapshot.terminals().values().stream().anyMatch(source -> source.resource().equals(resource)
                && (source.kind() == TerminalKind.MANUAL_WORLD || source.kind() == TerminalKind.MANUAL_LABOUR));
        unknown(resource, manual ? "Manual dependence is not an exact bootstrap guarantee."
                : "No exact establishment witness for this dependency; it is not treated as guaranteed.");
    }

    private void seedShortage(Resource resource, long amount) {
        if (amount <= 0) {
            return;
        }
        boolean unquantified = profile == null && snapshot.terminals().values().stream().anyMatch(source ->
                source.resource().equals(resource) && source.kind() != TerminalKind.EXACT_BOOTSTRAP
                        && !source.renewable());
        if (unquantified) {
            unknown(resource, "Circulation needs a seed, but its manual/finite source has no exact guarantee.");
        } else {
            shortages.merge(resource, amount, Math::max);
        }
    }

    private boolean localWork(Route route) {
        return profile != null && audit.manualWorkReady(route, tier, available, profile);
    }

    private long actions(long fu) {
        return 1 + (fu - 1) / profile.manualWork().fuPerAction();
    }

    private List<Resource> manualMembers(Resource resource) {
        if (resource.kind() == Kind.ENERGY || resource.kind() == Kind.ENVIRONMENT) {
            return List.of();
        }
        TagResolution tag = snapshot.tags().get(resource);
        Set<Resource> candidates = tag == null ? Set.of(resource) : tag.complete() ? tag.members() : Set.of();
        return candidates.stream().filter(candidate -> snapshot.terminals().values().stream().anyMatch(source ->
                source.resource().equals(candidate) && source.kind() == TerminalKind.MANUAL_WORLD
                        && source.unlockTier() <= tier && source.justification() != null
                        && !source.justification().isBlank())).sorted().toList();
    }

    private boolean provideManual(Resource resource, long amount) {
        if (profile == null) {
            return false;
        }
        for (Resource member : manualMembers(resource)) {
            long missing = amount - free(resource);
            if (missing <= 0) {
                return true;
            }
            long remaining = deriving ? missing : profile.playerProvided().getOrDefault(member, 0L)
                    - playerDelivered.getOrDefault(member, 0L);
            long supplied = Math.min(missing, remaining);
            if (supplied > 0) {
                merge(playerDelivered, member, supplied);
                merge(delivered, member, supplied);
                merge(stock, member, supplied);
            }
        }
        return free(resource) >= amount;
    }

    private void unknown(Resource resource, String message) {
        uncertain = true;
        findings.add(new AuditFinding("construction_reserve_unknown", resource.key(), "T" + tier,
                AuditFinding.Severity.UNKNOWN, message, sourceReferences(resource)));
    }

    private List<String> sourceReferences(Resource resource) {
        return snapshot.terminals().values().stream().filter(source -> source.resource().equals(resource))
                .map(Terminal::source).sorted().distinct().toList();
    }

    private static void merge(Map<Resource, Long> values, Resource resource, long amount) {
        values.merge(resource, amount, Math::addExact);
    }
}
