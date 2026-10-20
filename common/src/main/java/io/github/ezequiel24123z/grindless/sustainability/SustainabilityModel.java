package io.github.ezequiel24123z.grindless.sustainability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Loader-free authority data for the ADR-0135 audit. Quantities belong here, not in annotations.
 */
public final class SustainabilityModel {
    private SustainabilityModel() {
    }

    /** The physical resource axes and the two explicitly non-item resource axes. */
    public enum Kind { ITEM, TAG, FLUID, ENERGY, ENVIRONMENT }

    /**
     * A registry-qualified resource; Matrix ratings and tag registries are part of its identity.
     *
     * @param kind resource axis
     * @param id namespaced registry id, or a named energy/environment id
     * @param rating physical Matrix rating, zero for ordinary resources
     * @param registry tag/member registry, normally {@code items}
     */
    public record Resource(Kind kind, String id, int rating, String registry)
            implements Comparable<Resource> {
        public Resource {
            Objects.requireNonNull(kind, "kind");
            requireText(id, "resource id");
            requireText(registry, "registry");
            if (rating < 0 || rating > 15 || (rating > 0 && kind != Kind.ITEM)) {
                throw new IllegalArgumentException("Invalid physical rating");
            }
        }

        /** Creates an ordinary resource in the item registry. */
        public Resource(Kind kind, String id) {
            this(kind, id, 0, "items");
        }

        /** Returns a stable, human-readable identity, never a localized label. */
        public String key() {
            String axis = kind.name().toLowerCase(Locale.ROOT);
            return axis + (registry.equals("items") ? "" : "[" + registry + "]") + ":" + id
                    + (rating == 0 ? "" : " [F" + rating + "]");
        }

        /** Returns the conventional description key; it is derived, never annotated. */
        public String descriptionKey() {
            return kind.name().toLowerCase(Locale.ROOT) + "." + id.replace(':', '.').replace('/', '.');
        }

        @Override
        public int compareTo(Resource other) {
            return key().compareTo(other.key());
        }
    }

    /** The FU pseudo-resource. */
    public static final Resource ENERGY = new Resource(Kind.ENERGY, "FU");

    /** The live source which owns a route's quantities. */
    public enum Authority {
        PROCESS_GRAPH, BOOTSTRAP_RECIPES, T1_RECIPES, CRAFTING_JSON, GENERATOR_FUEL,
        QUEST_REWARD, BOOTSTRAP_SOURCE, VANILLA_TRANSFORMATION
    }

    /**
     * A live route, including gross batches, non-consumed requirements and discharged outputs.
     * Energy demand is also present in {@code inputs}; {@code fuPerTick} retains its rate.
     */
    public record Route(String id, Authority authority, String source, String family,
                        Map<Resource, Long> inputs, Map<Resource, Long> catalysts,
                        Map<Resource, Long> outputs, Set<Resource> discharged,
                        long durationTicks, long fuPerTick, Map<String, String> conditions,
                        Set<String> prerequisites, boolean oneShot, boolean modOwned, boolean automatable) {
        public Route {
            requireText(id, "route id");
            Objects.requireNonNull(authority, "authority");
            requireText(source, "route source");
            inputs = quantities(inputs);
            catalysts = quantities(catalysts);
            outputs = quantities(outputs);
            discharged = sortedSet(discharged);
            conditions = sortedMap(conditions);
            prerequisites = sortedSet(prerequisites);
            if (durationTicks < 0 || fuPerTick < 0 || outputs.isEmpty()
                    || !outputs.keySet().containsAll(discharged)) {
                throw new IllegalArgumentException("Invalid route batch: " + id);
            }
        }

        /** S1 fixture compatibility only; canonical adapters must explicitly bind execution semantics. */
        public Route(String id, Authority authority, String source, String family,
                     Map<Resource, Long> inputs, Map<Resource, Long> catalysts,
                     Map<Resource, Long> outputs, Set<Resource> discharged,
                     long durationTicks, long fuPerTick, Map<String, String> conditions,
                     Set<String> prerequisites, boolean oneShot, boolean modOwned) {
            this(id, authority, source, family, inputs, catalysts, outputs, discharged, durationTicks, fuPerTick,
                    conditions, prerequisites, oneShot, modOwned, true);
        }

        /** Returns only consumption not returned by this same batch. */
        public Map<Resource, Long> netInputs() {
            Map<Resource, Long> net = new TreeMap<>();
            inputs.forEach((resource, amount) -> {
                long returned = discharged.contains(resource) ? 0 : outputs.getOrDefault(resource, 0L);
                if (amount > returned) {
                    net.put(resource, amount - returned);
                }
            });
            return Collections.unmodifiableMap(net);
        }

        /** Returns physically obtainable outputs, excluding losses and emissions sent to a sink. */
        public Map<Resource, Long> retainedOutputs() {
            Map<Resource, Long> retained = new TreeMap<>(outputs);
            discharged.forEach(retained::remove);
            return Collections.unmodifiableMap(retained);
        }
    }

    /** Only these sites can justify renewable environmental terminals. */
    public enum SourceSite { GEOLOGICAL_ROCK_BED, SUNLIT_CULTURE, WATER_BODY, ATMOSPHERE, OPEN_SKY_DAYLIGHT }

    /** Finite/manual/vanilla sources can establish availability but never renewability. */
    public enum TerminalKind {
        ENVIRONMENT, EXACT_BOOTSTRAP, MANUAL_WORLD, MANUAL_LABOUR,
        WORLD_GENERATION, MOB_DROP, LOOT, TREE_CROP_GROWTH, BLOCK_GENERATOR, TRADING
    }

    /**
     * A source authority. Exact guarantees are quantities of this source, not recipe annotations.
     * Environmental/manual sources have no finite guaranteed quantity.
     */
    public record Terminal(String id, Resource resource, TerminalKind kind, SourceSite site,
                           boolean automatable, boolean modOwned, Long guaranteed,
                           int unlockTier, String justification, String source) {
        public Terminal {
            requireText(id, "terminal id");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(kind, "terminal kind");
            requireText(source, "terminal source");
            tier(unlockTier);
            if (guaranteed != null && guaranteed <= 0) {
                throw new IllegalArgumentException("A guarantee must be positive");
            }
            if ((kind == TerminalKind.EXACT_BOOTSTRAP) != (guaranteed != null)) {
                throw new IllegalArgumentException("Only exact bootstrap sources carry guarantees");
            }
        }

        /** Returns whether this is a justified, mod-owned, automatable environmental source. */
        public boolean renewable() {
            return kind == TerminalKind.ENVIRONMENT && resource.kind() == Kind.ENVIRONMENT
                    && site != null && automatable && modOwned
                    && justification != null && !justification.isBlank();
        }
    }

    /** A checked local-work binding; its charge and eligible families come from actual mechanics. */
    public record ManualWork(String terminal, Resource tool, long fuPerAction,
                             Set<String> processFamilies, String source) {
        public ManualWork {
            requireText(terminal, "manual-work terminal");
            Objects.requireNonNull(tool, "manual-work tool");
            requireText(source, "manual-work source");
            processFamilies = sortedSet(processFamilies);
            if (tool.kind() != Kind.ITEM || fuPerAction <= 0 || processFamilies.isEmpty()) {
                throw new IllegalArgumentException("Invalid manual-work binding");
            }
        }

        /** Research, generators and other F0 devices are not ProcessMachine recipients. */
        public boolean applies(Route route) {
            return route.authority() == Authority.PROCESS_GRAPH && route.family() != null
                    && processFamilies.contains(route.family());
        }
    }

    /** Local/classpath tag expansion, retaining incomplete nested references and provenance. */
    public record TagResolution(Set<Resource> members, boolean complete,
                                Set<String> unresolved, List<String> sources) {
        public TagResolution {
            members = sortedSet(members);
            unresolved = sortedSet(unresolved);
            sources = sources.stream().sorted().distinct().toList();
        }
    }

    /** Quest/checklist evidence without an implicit Tier mandate; null reward route means no material reward. */
    public record Objective(String id, Resource subject, long count, Set<String> requires,
                            String rewardRoute, String source) {
        public Objective {
            requireText(id, "objective id");
            requires = sortedSet(requires);
            if (count <= 0) {
                throw new IllegalArgumentException("Objective count must be positive");
            }
        }
    }

    /** An immutable, deterministically ordered projection of the live authorities. */
    public record Snapshot(Set<Resource> resources, Map<String, Route> routes,
                           Map<String, Terminal> terminals, Map<Resource, TagResolution> tags,
                           Map<String, Objective> objectives, Map<String, String> inputHashes,
                           List<AuditFinding> findings) {
        public Snapshot {
            resources = sortedSet(resources);
            routes = sortedMap(routes);
            terminals = sortedMap(terminals);
            tags = sortedMap(tags);
            objectives = sortedMap(objectives);
            inputHashes = sortedMap(inputHashes);
            findings = List.copyOf(findings);
        }
    }

    /** Collects raw authority data, rejecting accidental duplicate route or source identities. */
    public static final class Builder {
        private final Set<Resource> resources = new TreeSet<>();
        private final Map<String, Route> routes = new TreeMap<>();
        private final Map<String, Terminal> terminals = new TreeMap<>();
        private final Map<Resource, TagResolution> tags = new TreeMap<>();
        private final Map<String, Objective> objectives = new TreeMap<>();
        private final Map<String, String> hashes = new TreeMap<>();
        private final List<AuditFinding> findings = new ArrayList<>();

        public Builder() {
        }

        /** Extends checked authorities without changing or omitting their existing evidence. */
        public Builder(Snapshot snapshot) {
            resources.addAll(snapshot.resources());
            routes.putAll(snapshot.routes());
            terminals.putAll(snapshot.terminals());
            tags.putAll(snapshot.tags());
            objectives.putAll(snapshot.objectives());
            hashes.putAll(snapshot.inputHashes());
            findings.addAll(snapshot.findings());
        }

        /** Adds a resource even when no implemented producer exists. */
        public Builder resource(Resource resource) {
            resources.add(resource);
            return this;
        }

        /** Adds one route and all of its referenced resources. */
        public Builder route(Route route) {
            if (routes.putIfAbsent(route.id(), route) != null) {
                throw new IllegalArgumentException("Duplicate route: " + route.id());
            }
            resources.addAll(route.inputs().keySet());
            resources.addAll(route.catalysts().keySet());
            resources.addAll(route.outputs().keySet());
            return this;
        }

        /** Adds a source, without treating a finite source as renewable. */
        public Builder terminal(Terminal terminal) {
            if (terminals.putIfAbsent(terminal.id(), terminal) != null) {
                throw new IllegalArgumentException("Duplicate terminal: " + terminal.id());
            }
            resources.add(terminal.resource());
            return this;
        }

        /** Adds a registry-specific tag expansion. */
        public Builder tag(Resource tag, TagResolution resolution) {
            tags.put(tag, resolution);
            resources.add(tag);
            resources.addAll(resolution.members());
            return this;
        }

        /** Adds an objective; its mandatory status is deliberately not inferred here. */
        public Builder objective(Objective objective) {
            if (objectives.putIfAbsent(objective.id(), objective) != null) {
                throw new IllegalArgumentException("Duplicate objective: " + objective.id());
            }
            if (objective.subject() != null) {
                resources.add(objective.subject());
            }
            return this;
        }

        /** Records a content fingerprint of an actual authority input. */
        public Builder input(String source, String sha256) {
            hashes.put(source, sha256);
            return this;
        }

        /** Preserves an extraction finding instead of silently dropping unsupported data. */
        public Builder finding(AuditFinding finding) {
            findings.add(finding);
            return this;
        }

        /** Builds an immutable projection. */
        public Snapshot build() {
            return new Snapshot(resources, routes, terminals, tags, objectives, hashes, findings);
        }
    }

    static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    static void tier(int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException("Tier must be T0-T15");
        }
    }

    static <K extends Comparable<? super K>, V> Map<K, V> sortedMap(Map<K, V> values) {
        return Collections.unmodifiableMap(new TreeMap<>(values));
    }

    static <T extends Comparable<? super T>> Set<T> sortedSet(Set<T> values) {
        return Collections.unmodifiableSet(new TreeSet<>(values));
    }

    static Map<Resource, Long> quantities(Map<Resource, Long> values) {
        values.forEach((resource, amount) -> {
            Objects.requireNonNull(resource, "resource");
            if (amount == null || amount <= 0) {
                throw new IllegalArgumentException("Batch quantities must be positive");
            }
        });
        return sortedMap(values);
    }
}
