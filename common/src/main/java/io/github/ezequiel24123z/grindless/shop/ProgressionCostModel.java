package io.github.ezequiel24123z.grindless.shop;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.recipe.T1Recipes;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityAudit;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel;
import io.github.ezequiel24123z.grindless.sustainability.T0ConditionalGate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Projects the existing finite canonical witness; it is not a second recipe planner. */
public final class ProgressionCostModel {
    public record ExitMinimum(String quest, String output, long required, long outputPerBatch,
                              long batches, long produced, Map<String, Long> finalCraftInputs,
                              Map<String, Long> inputsAfterCoreExpansion, long calibrationCycles,
                              long calibrationTicks, long calibrationFu, Set<String> immediateInfrastructure) {
        public ExitMinimum {
            finalCraftInputs = sorted(finalCraftInputs);
            inputsAfterCoreExpansion = sorted(inputsAfterCoreExpansion);
            immediateInfrastructure = Collections.unmodifiableSet(new TreeSet<>(immediateInfrastructure));
        }
    }

    public record Batch(String route, String source, String family, long batches, long serialTicks,
                        long fuDemand, Map<String, Long> grossInputs, Map<String, Long> grossOutputs,
                        Map<String, Long> catalystsPerExecution) {
        public Batch {
            grossInputs = sorted(grossInputs);
            grossOutputs = sorted(grossOutputs);
            catalystsPerExecution = sorted(catalystsPerExecution);
        }
    }

    public record Witness(String status, boolean conditionalGatePass, Map<String, String> unmetObligations,
                          SustainabilityAudit.GoalWitness exit, SustainabilityAudit.WorkReserve manualWork,
                          List<SustainabilityAudit.Reserve> finiteReserves, Map<String, Long> retainedInventory,
                          Map<String, Long> installedMachinesAndParts, List<Batch> batches,
                          Map<String, Long> grossInputs, Map<String, Long> grossOutputs,
                          long serialWorkTicks, long fuDemand, Set<String> conditions,
                          Set<String> unresolvedInfrastructure) {
        public Witness {
            unmetObligations = Collections.unmodifiableMap(new TreeMap<>(unmetObligations));
            finiteReserves = List.copyOf(finiteReserves);
            retainedInventory = sorted(retainedInventory);
            installedMachinesAndParts = sorted(installedMachinesAndParts);
            batches = List.copyOf(batches);
            grossInputs = sorted(grossInputs);
            grossOutputs = sorted(grossOutputs);
            conditions = Collections.unmodifiableSet(new TreeSet<>(conditions));
            unresolvedInfrastructure = Collections.unmodifiableSet(new TreeSet<>(unresolvedInfrastructure));
        }
    }

    private ProgressionCostModel() {
    }

    /** Exact minimum at the final craft/calibration boundary, excluding construction and raw processing. */
    public static ExitMinimum exitMinimum() {
        T1Recipes.Shaped relay = EconomyGraph.relay();
        QuestCatalogue.Task goal = QuestCatalogue.byId("relay");
        if (goal == null || !goal.subject().equals(relay.result()) || goal.count() < 1 || relay.resultCount() < 1) {
            throw new IllegalStateException("The cost model has no matching physical T0 exit");
        }
        long batches = goal.count() / relay.resultCount() + (goal.count() % relay.resultCount() == 0 ? 0 : 1);
        Map<String, Long> inputs = multiply(EconomyGraph.shapedInputs(relay.pattern(), relay.key()), batches);
        Map<String, Long> expanded = new TreeMap<>(inputs);
        Long cores = expanded.remove(T1Recipes.CALIBRATED_DATA_CORE);
        if (cores == null || cores < 1) {
            throw new IllegalStateException("The physical T0 exit must retain its calibrated-core production");
        }
        BootstrapRecipes.Shapeless core = BootstrapRecipes.shapeless().stream()
                .filter(craft -> craft.result().equals(ResearchLogic.DATA_CORE)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing canonical Data Core craft"));
        core.ingredients().forEach(ingredient -> expanded.merge(ingredient, cores, Math::addExact));
        return new ExitMinimum(goal.id(), relay.result(), goal.count(), relay.resultCount(), batches,
                Math.multiplyExact(batches, relay.resultCount()), inputs, expanded, cores,
                Math.multiplyExact(cores, ResearchLogic.CYCLE_TICKS),
                Math.multiplyExact(Math.multiplyExact(cores, ResearchLogic.CYCLE_TICKS), ResearchLogic.FU_PER_TICK),
                Set.of("item:grindless:research_terminal", "item:minecraft:crafting_table"));
    }

    public static Witness witness(SustainabilityAudit.Result result) {
        SustainabilityAudit.TierResult tier = result.tiers().get(0);
        if (tier == null) {
            throw new IllegalArgumentException("A canonical T0 audit result is required");
        }
        var gate = T0ConditionalGate.evaluate(result);
        List<Batch> batches = new ArrayList<>();
        Map<String, Long> grossInputs = new TreeMap<>();
        Map<String, Long> grossOutputs = new TreeMap<>();
        Map<String, Long> installed = new TreeMap<>();
        Set<String> unresolved = new TreeSet<>();
        long ticks = 0;
        long fu = 0;
        for (var executed : tier.establishmentBatches().entrySet()) {
            SustainabilityModel.Route route = result.authorities().routes().get(executed.getKey());
            if (route == null || executed.getValue() < 1 || route.id().startsWith("shop/")) {
                throw new IllegalArgumentException("Invalid or shop-dependent canonical cost witness: " + executed.getKey());
            }
            long count = executed.getValue();
            long work = Math.multiplyExact(route.durationTicks(), count);
            long demand = Math.multiplyExact(work, route.fuPerTick());
            Map<String, Long> consumed = quantities(route.inputs(), count);
            Map<String, Long> output = quantities(route.retainedOutputs(), count);
            consumed.forEach((resource, amount) -> grossInputs.merge(resource, amount, Math::addExact));
            output.forEach((resource, amount) -> grossOutputs.merge(resource, amount, Math::addExact));
            if (route.family() != null) {
                var machine = result.annotations().machines().get(route.family());
                if (machine != null) {
                    installed.put(machine.construction().key(), 1L);
                } else {
                    unresolved.add(route.family());
                }
            }
            route.catalysts().forEach((resource, amount) -> installed.merge(resource.key(), amount, Math::max));
            batches.add(new Batch(route.id(), route.source(), route.family(), count, work, demand,
                    consumed, output, quantities(route.catalysts(), 1)));
            ticks = Math.addExact(ticks, work);
            fu = Math.addExact(fu, demand);
        }
        var plan = result.annotations().ledger().get(0);
        Set<String> conditions = plan == null || plan.entryProfile() == null
                ? Set.of("No finite conditional entry profile is bound") : plan.entryProfile().conditions();
        return new Witness(gate.status(), gate.pass(), gate.unmetObligations(), tier.exitProduction(),
                tier.manualWork(), tier.reserves(), quantities(tier.retainedInventory(), 1), installed,
                batches, grossInputs, grossOutputs, ticks, fu, conditions, unresolved);
    }

    public static Map<String, Long> starterAllocation() {
        QuestCatalogue.Task starter = QuestCatalogue.byId(QuestCatalogue.STARTER_SUPPLIES);
        if (starter == null) {
            throw new IllegalStateException("The original starter claim must remain available");
        }
        Map<String, Long> allocation = new TreeMap<>();
        starter.rewards().forEach(reward -> allocation.merge(reward.itemId(), (long) reward.count(), Math::addExact));
        return sorted(allocation);
    }

    private static Map<String, Long> quantities(Map<SustainabilityModel.Resource, Long> input, long factor) {
        Map<String, Long> result = new TreeMap<>();
        input.forEach((resource, amount) -> result.put(resource.key(), Math.multiplyExact(amount, factor)));
        return result;
    }

    private static Map<String, Long> multiply(Map<String, Long> input, long factor) {
        Map<String, Long> result = new TreeMap<>();
        input.forEach((resource, amount) -> result.put(resource, Math.multiplyExact(amount, factor)));
        return result;
    }

    private static Map<String, Long> sorted(Map<String, Long> quantities) {
        return Collections.unmodifiableMap(new TreeMap<>(quantities));
    }
}
