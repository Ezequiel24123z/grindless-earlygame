package io.github.ezequiel24123z.grindless.sustainability;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Typed declarations owned by the coordinator. No record here can replace a recipe batch.
 * S1 supplies the vocabulary only; it does not author live T0 classifications or tier roots.
 */
public record SustainabilityAnnotations(Map<Resource, ResourceMetadata> resources,
                                        Map<String, RoutePolicy> routes,
                                        Map<String, Machine> machines,
                                        Map<String, ClosedLoop> loops,
                                        Map<String, CommittedPlan> plans,
                                        Map<Integer, TierPlan> ledger) {
    public SustainabilityAnnotations {
        resources = sortedMap(resources);
        routes = sortedMap(routes);
        machines = sortedMap(machines);
        loops = sortedMap(loops);
        plans = sortedMap(plans);
        ledger = sortedMap(ledger);
    }

    /** The seven ADR-0131 resource classes. */
    public enum ResourceClass {
        BOOTSTRAP, RENEWABLE, CLOSED_LOOP, CATALYTIC, LOSSY_LOOP, FINITE_OPTIONAL,
        FORBIDDEN_DEPENDENCY
    }

    /** The ten ADR-0131 side-stream classes, including truthful non-material destinations. */
    public enum SideStreamClass {
        BYPRODUCT_CURRENT, BYPRODUCT_FUTURE, BYPRODUCT_OPTIONAL, WASTE_RECOVERABLE,
        WASTE_HAZARDOUS, WASTE_INERT, PROCESS_LOSS, EMISSION, CATALYST_SPENT, POTENTIAL_FUTURE_USE
    }

    /** Handling describes an implemented action, not a fictional consumer. */
    public enum Action { REUSE, RECYCLE, STORE, CONTAIN, TREAT, DISCARD, VENT, PROCESS_LOSS }

    /** Optional presentation roles cannot change the authority's physical discharge semantics. */
    public enum OutputRole { PRIMARY, SIDE_STREAM, DISCHARGED }

    /** A future consumer must reference a committed plan, its system and its actual tier. */
    public record FutureConsumer(int tier, String system, String planReference) {
        public FutureConsumer {
            SustainabilityModel.tier(tier);
            requireText(system, "future system");
            requireText(planReference, "future plan reference");
        }
    }

    /** A committed design reference, not a claim that its consumer is already implemented. */
    public record CommittedPlan(String reference, String system, int tier, String source) {
        public CommittedPlan {
            SustainabilityModel.tier(tier);
            requireText(reference, "plan reference");
            requireText(system, "plan system");
            requireText(source, "plan source");
        }
    }

    /** Hazard containment must be constructible; treatment must name a real, unlocked route. */
    public record Handling(Action action, boolean discardAllowed, boolean hazardous,
                           Resource containment, String treatmentRoute) {
    }

    /** A recycling reference or an explicitly planned tier, never a copied recovery yield. */
    public record Recycling(String route, Integer plannedTier) {
        public Recycling {
            if (plannedTier != null) {
                SustainabilityModel.tier(plannedTier);
            }
        }
    }

    /**
     * Declared resource metadata. Null classifications/tiers mean unknown, not a default pass.
     * Producers, current consumers, mandatory status and description keys are derived.
     */
    public record ResourceMetadata(ResourceClass resourceClass, SideStreamClass sideStreamClass,
                                   Integer firstUseTier, Integer firstRenewableTier,
                                   String canonicalRoute, List<FutureConsumer> futureConsumers,
                                   Handling handling, Recycling recycling,
                                   Resource originatingCatalyst, String uncertainty) {
        public ResourceMetadata {
            if (firstUseTier != null) {
                SustainabilityModel.tier(firstUseTier);
            }
            if (firstRenewableTier != null) {
                SustainabilityModel.tier(firstRenewableTier);
            }
            futureConsumers = List.copyOf(futureConsumers);
        }
    }

    /** Environmental justifications reference actual terminals rather than inventing matter. */
    public record RoutePolicy(int unlockTier, boolean optional, Set<String> environmentalSources,
                              Map<Resource, OutputRole> outputRoles) {
        public RoutePolicy {
            SustainabilityModel.tier(unlockTier);
            environmentalSources = sortedSet(environmentalSources);
            outputRoles = sortedMap(outputRoles);
        }

        /** Declares a route without output-role overrides, the ordinary case. */
        public RoutePolicy(int unlockTier, boolean optional, Set<String> environmentalSources) {
            this(unlockTier, optional, environmentalSources, Map.of());
        }
    }

    /** Maps a machine family to its constructible block or authored assembly result. */
    public record Machine(String family, Resource construction, int unlockTier) {
        public Machine {
            requireText(family, "machine family");
            java.util.Objects.requireNonNull(construction, "machine construction");
            SustainabilityModel.tier(unlockTier);
        }
    }

    /** Named loop members and their make-up route; batch quantities are read from those routes. */
    public record ClosedLoop(String id, Resource carrier, Set<String> memberRoutes,
                             String makeUpRoute) {
        public ClosedLoop {
            requireText(id, "loop id");
            memberRoutes = sortedSet(memberRoutes);
        }
    }

    /** Roots and selected non-optional quest ids; missing tiers remain explicit unknowns. */
    public record TierPlan(int tier, Set<Resource> roots, Set<String> requiredQuests,
                           Resource exitObjective, String unknown) {
        public TierPlan {
            SustainabilityModel.tier(tier);
            roots = sortedSet(roots);
            requiredQuests = sortedSet(requiredQuests);
        }
    }

    /** Returns the intentionally unannotated live S1 model. */
    public static SustainabilityAnnotations empty() {
        return new SustainabilityAnnotations(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }
}
