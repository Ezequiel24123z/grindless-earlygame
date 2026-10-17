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
    private final Snapshot snapshot;
    private final SustainabilityAnnotations annotations;
    private final SustainabilityAudit audit;
    private final int tier;
    private final Set<Resource> available;
    private final Set<Resource> renewable;
    private final Map<Resource, Long> guaranteed = new TreeMap<>();
    private final Map<Resource, Long> delivered = new TreeMap<>();
    private final Map<Resource, Long> stock = new TreeMap<>();
    private final Map<Resource, Long> peak = new TreeMap<>();
    private final Map<Resource, Long> shortages = new TreeMap<>();
    private final Map<Resource, Long> locked = new TreeMap<>();
    private final Set<Resource> installed = new TreeSet<>();
    private final Set<Resource> pending = new HashSet<>();
    private final Set<String> claimed = new HashSet<>();
    private final Set<String> pendingRoutes = new HashSet<>();
    private final Set<String> preparing = new HashSet<>();
    private final List<AuditFinding> findings = new ArrayList<>();
    private boolean uncertain;
    private boolean startupBound;
    private int steps;

    ConstructionReserve(Snapshot snapshot, SustainabilityAnnotations annotations, SustainabilityAudit audit,
                        int tier, Set<Resource> available, Set<Resource> renewable) {
        this.snapshot = snapshot;
        this.annotations = annotations;
        this.audit = audit;
        this.tier = tier;
        this.available = available;
        this.renewable = renewable;
        for (Terminal terminal : snapshot.terminals().values()) {
            if (terminal.kind() == TerminalKind.EXACT_BOOTSTRAP && terminal.unlockTier() <= tier
                    && terminal.justification() != null && !terminal.justification().isBlank()) {
                merge(guaranteed, terminal.resource(), terminal.guaranteed());
                merge(delivered, terminal.resource(), terminal.guaranteed());
                merge(stock, terminal.resource(), terminal.guaranteed());
            }
        }
        for (Route route : snapshot.routes().values()) {
            if (route.oneShot() && audit.unlocked(route, tier)
                    && route.catalysts().keySet().stream().allMatch(resource -> audit.has(available, resource))) {
                route.retainedOutputs().forEach((resource, amount) -> merge(guaranteed, resource, amount));
            }
        }
    }

    List<AuditFinding> findings() {
        return List.copyOf(findings);
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
            for (var catalyst : route.catalysts().entrySet()) {
                if (!route.oneShot()) {
                    lock(catalyst.getKey(), catalyst.getValue());
                }
            }
            boolean loopMember = annotations.loops().values().stream().anyMatch(loop -> loop.memberRoutes().contains(id));
            boolean constructionRoute = construction.stream().anyMatch(block -> {
                Route producer = audit.canonical(block, tier, false);
                return producer != null && producer.id().equals(id);
            });
            if (!loopMember && !constructionRoute
                    && route.retainedOutputs().keySet().stream().anyMatch(resource -> audit.has(renewable, resource))) {
                execute(route, 1);
            }
        }
        List<SustainabilityAudit.Reserve> result = new ArrayList<>();
        Set<Resource> reserved = new TreeSet<>(guaranteed.keySet());
        reserved.addAll(shortages.keySet());
        for (Resource resource : reserved) {
            long amount = guaranteed.getOrDefault(resource, 0L);
            long missing = shortages.getOrDefault(resource, 0L);
            long required = Math.max(peak.getOrDefault(resource, 0L), missing > 0 ? Math.addExact(amount, missing) : 0);
            long margin = Math.subtractExact(amount, required);
            String status = margin < 0 ? "insufficient" : uncertain ? "unknown" : "established";
            result.add(new SustainabilityAudit.Reserve(resource, amount, required, margin, status));
            if (margin < 0) {
                findings.add(new AuditFinding("bootstrap_starvation", resource.key(), "T" + tier,
                        AuditFinding.Severity.ERROR,
                        "Exact guarantee " + amount + " cannot cover establishment draw " + required
                                + " including construction and the recoverable seed; margin " + margin + ".",
                        sourceReferences(resource)));
            }
        }
        return result;
    }

    boolean circulate(ClosedLoop loop, Map<String, BigInteger> batches) {
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
        Map<Resource, Long> initial = new TreeMap<>(stock);
        Map<Resource, Long> minimum = new TreeMap<>(stock);
        Map<String, Long> remaining = new TreeMap<>();
        batches.forEach((id, count) -> remaining.put(id, count.longValueExact()));
        boolean completed = false;
        try {
            startupBound = false;
            List<String> order = startupOrder(new TreeMap<>(remaining), new TreeMap<>(stock), internal,
                    batches.keySet(), new ArrayList<>(), new HashSet<>(), new int[1]);
            if (order == null && startupBound) {
                unknown(loop.carrier(), "Startup ordering exceeded its bounded search; no infeasibility is claimed.");
                return false;
            }
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
                if (!circulationSupply(input.getKey(), internal, members)
                        && !spendSimulated(next, input.getKey(), input.getValue())) {
                    ready = false;
                    break;
                }
            }
            if (!ready) {
                continue;
            }
            route.retainedOutputs().forEach((resource, amount) -> merge(next, resource, amount));
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
            shortage(resource, amount - free(resource));
            return false;
        }
        try {
            for (Route source : snapshot.routes().values()) {
                if (source.oneShot() && audit.unlocked(source, tier) && !claimed.contains(source.id())
                        && audit.outputAmount(source, resource) > 0) {
                    execute(source, 1);
                    if (free(resource) >= amount) {
                        return true;
                    }
                }
            }
            Route route = replenishing(resource);
            if (route != null && audit.unlocked(route, tier)) {
                long yield = audit.outputAmount(route, resource) - route.inputs().getOrDefault(resource, 0L);
                if (yield > 0) {
                    long missing = amount - free(resource);
                    long batches = 1 + (missing - 1) / yield;
                    if (execute(route, batches) && free(resource) >= amount) {
                        return true;
                    }
                }
            }
            shortage(resource, amount - free(resource));
            return false;
        } finally {
            pending.remove(resource);
        }
    }

    private Route replenishing(Resource resource) {
        for (ClosedLoop loop : annotations.loops().values()) {
            if (loop.carrier().equals(resource) && loop.makeUpRoute() != null) {
                Route makeUp = snapshot.routes().get(loop.makeUpRoute());
                if (makeUp != null && audit.outputAmount(makeUp, resource) > 0) {
                    return makeUp;
                }
            }
        }
        return audit.canonical(resource, tier, false);
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
        }
        for (var catalyst : route.catalysts().entrySet()) {
            boolean ready = route.oneShot() ? obtain(catalyst.getKey(), catalyst.getValue())
                    || locked.getOrDefault(catalyst.getKey(), 0L) >= catalyst.getValue()
                    : lock(catalyst.getKey(), catalyst.getValue());
            if (!ready) {
                return false;
            }
        }
        for (Resource output : route.retainedOutputs().keySet()) {
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

    private boolean executeBatch(Route route) {
        if (!audit.unlocked(route, tier) || audit.zeroInputUnjustified(route)
                || !audit.environmentReady(route, available) || route.oneShot() && claimed.contains(route.id())
                || !pendingRoutes.add(route.id())) {
            return false;
        }
        Map<Resource, Long> allocated = new TreeMap<>();
        Map<Resource, Long> borrowed = new TreeMap<>();
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
            for (var output : route.retainedOutputs().entrySet()) {
                merge(stock, output.getKey(), output.getValue());
            }
            if (route.oneShot()) {
                claimed.add(route.id());
                route.retainedOutputs().forEach((resource, amount) -> merge(delivered, resource, amount));
            }
            completed = true;
            return true;
        } finally {
            if (!completed) {
                allocated.forEach((resource, amount) -> merge(stock, resource, amount));
            }
            borrowed.forEach((resource, amount) -> {
                take(resource, amount);
                merge(locked, resource, amount);
            });
            pendingRoutes.remove(route.id());
        }
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
        if (guaranteed.containsKey(resource)) {
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
        boolean unquantified = snapshot.terminals().values().stream().anyMatch(source ->
                source.resource().equals(resource) && source.kind() != TerminalKind.EXACT_BOOTSTRAP
                        && !source.renewable());
        if (unquantified) {
            unknown(resource, "Circulation needs a seed, but its manual/finite source has no exact guarantee.");
        } else {
            shortages.merge(resource, amount, Math::max);
        }
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
