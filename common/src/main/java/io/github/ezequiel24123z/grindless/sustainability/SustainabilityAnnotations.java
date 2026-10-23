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

    /** ADR-0131 supply classes plus ADR-0188's explicitly nonrenewable durable manual capital. */
    public enum ResourceClass {
        BOOTSTRAP, MANUAL_CAPITAL, RENEWABLE, CLOSED_LOOP, CATALYTIC, LOSSY_LOOP, FINITE_OPTIONAL,
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

    /** Environmental evidence does not assign progression: a null tier remains unavailable. */
    public record RoutePolicy(Integer unlockTier, boolean optional, Set<String> environmentalSources,
                              Map<Resource, OutputRole> outputRoles) {
        public RoutePolicy {
            if (unlockTier != null) {
                SustainabilityModel.tier(unlockTier);
            }
            environmentalSources = sortedSet(environmentalSources);
            outputRoles = sortedMap(outputRoles);
        }

        /** Retains the original primitive-tier constructor. */
        public RoutePolicy(int unlockTier, boolean optional, Set<String> environmentalSources,
                           Map<Resource, OutputRole> outputRoles) {
            this(Integer.valueOf(unlockTier), optional, environmentalSources, outputRoles);
        }

        /** Declares a route without output-role overrides, the ordinary case. */
        public RoutePolicy(int unlockTier, boolean optional, Set<String> environmentalSources) {
            this(Integer.valueOf(unlockTier), optional, environmentalSources, Map.of());
        }

        /** Declares environmental facts while leaving an unaccepted route's tier unknown. */
        public RoutePolicy(Integer unlockTier, boolean optional, Set<String> environmentalSources) {
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

    /**
     * Conditional player custody and bounded work, never gifts or portable ENERGY.
     * An empty bill can be a derivation template, but cannot certify an unfunded witness.
     */
    public record EntryProfile(String id, Map<Resource, Long> playerProvided, long manualWorkActions,
                               ManualWork manualWork, Set<String> conditions, String source) {
        public EntryProfile {
            requireText(id, "entry profile id");
            requireText(source, "entry profile source");
            playerProvided = quantities(playerProvided);
            conditions = sortedSet(conditions);
            if (conditions.isEmpty() || manualWorkActions < 0 || manualWorkActions > 0 && manualWork == null
                    || playerProvided.keySet().stream().anyMatch(resource ->
                    resource.kind() != Kind.ITEM && resource.kind() != Kind.FLUID)) {
                throw new IllegalArgumentException("An entry profile needs physical provisions and explicit conditions");
            }
        }

        /** Uses a derived sufficient bill without changing its stated acquisition assumptions. */
        public EntryProfile withBill(Map<Resource, Long> provided, long actions) {
            return new EntryProfile(id, provided, actions, manualWork, conditions, source);
        }
    }

    /** Roots and selected non-optional quest ids; missing tiers remain explicit unknowns. */
    public record TierPlan(int tier, Set<Resource> roots, Set<String> requiredQuests,
                           Resource exitObjective, String unknown, EntryProfile entryProfile) {
        public TierPlan {
            SustainabilityModel.tier(tier);
            roots = sortedSet(roots);
            requiredQuests = sortedSet(requiredQuests);
        }

        /** Keeps S1 callers unprofiled rather than fabricating a sufficient entry kit. */
        public TierPlan(int tier, Set<Resource> roots, Set<String> requiredQuests,
                        Resource exitObjective, String unknown) {
            this(tier, roots, requiredQuests, exitObjective, unknown, null);
        }

        /** Replaces only the conditional entry bill after a separate finite replay. */
        public TierPlan withEntryProfile(EntryProfile profile) {
            return new TierPlan(tier, roots, requiredQuests, exitObjective, unknown, profile);
        }
    }

    /** Returns the intentionally unannotated live S1 model. */
    public static SustainabilityAnnotations empty() {
        return new SustainabilityAnnotations(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }
}
