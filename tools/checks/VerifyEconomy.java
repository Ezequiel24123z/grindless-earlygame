package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.shop.EconomyCycleProof;
import io.github.ezequiel24123z.grindless.shop.EconomyGraph;
import io.github.ezequiel24123z.grindless.shop.EconomyProjection;
import io.github.ezequiel24123z.grindless.shop.ProgressionCostModel;
import io.github.ezequiel24123z.grindless.shop.ShopCatalogue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Live-catalogue costs and the existing finite conditional replay, not a new progression planner. */
public final class VerifyEconomy {
    private static int assertions;

    private VerifyEconomy() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        SustainabilityAudit.Result result = canonicalResult(root);
        verify(result);
        Path data = root.resolve(Path.of("docs", "tiers", "t0-economy.json"));
        if (!Files.isRegularFile(data)) {
            throw new AssertionError("Generate docs/tiers/t0-economy.json with tools/economy/export before the integrated gate");
        }
        var committed = JsonParser.parseString(Files.readString(data)).getAsJsonObject();
        var projected = JsonParser.parseString(EconomyProjection.json(result)).getAsJsonObject();
        committed.remove("source_comparison");
        equal("committed balance/cost data equals the live authority projection", projected, committed);
        System.out.println("ALL ECONOMY CHECKS PASSED: " + assertions);
    }

    static SustainabilityAudit.Result canonicalResult(Path root) throws Exception {
        var sources = VerifySustainabilityRegistry.canonicalSources(root, T0Sustainability.vanillaAllowlist());
        var snapshot = T0Sustainability.withEntrySources(sources.snapshot());
        var template = T0Sustainability.entryTemplate(sources.localWork());
        var annotations = T0Sustainability.annotations(snapshot, sources.facts(), sources.localWork(), template);
        var debt = DebtManifest.read(root.resolve(Path.of("tools", "sustainability", "debt.json")));
        return SustainabilityAudit.deriveEntryProfile(snapshot, annotations, debt, 0).replay();
    }

    public static int verify(SustainabilityAudit.Result result) {
        String unordered = "{\"z\":{\"b\":1,\"a\":2},\"a\":[{\"b\":1,\"a\":2},3]}";
        String reordered = "{\"a\":[{\"a\":2,\"b\":1},3],\"z\":{\"a\":2,\"b\":1}}";
        String differentArray = "{\"a\":[3,{\"a\":2,\"b\":1}],\"z\":{\"a\":2,\"b\":1}}";
        String canonical = EconomyProjection.canonicalJson(JsonParser.parseString(unordered));
        equal("nested object order is canonical rather than JVM Map.of iteration order", canonical,
                EconomyProjection.canonicalJson(JsonParser.parseString(reordered)));
        equal("array order is preserved, not normalized into different gameplay data", false,
                canonical.equals(EconomyProjection.canonicalJson(JsonParser.parseString(differentArray))));
        equal("canonical document text uses LF independently of the checkout", false, canonical.contains("\r"));
        var minimum = ProgressionCostModel.exitMinimum();
        var relay = EconomyGraph.relay();
        equal("exit goal comes from the actual quest", (long) QuestCatalogue.byId("relay").count(), minimum.required());
        equal("first Matrix output batch comes from T1Recipes", (long) relay.resultCount(), minimum.outputPerBatch());
        equal("exact final craft quantities come from the pattern",
                EconomyGraph.shapedInputs(relay.pattern(), relay.key()), minimum.finalCraftInputs());
        equal("minimum expanded manufacturing inputs include the actual ceramic core",
                Map.of("item:minecraft:glass", 2L, "tag:forge:ingots/copper", 2L,
                        "item:minecraft:redstone", 3L, BootstrapRecipes.INSULATOR, 1L),
                minimum.inputsAfterCoreExpansion());
        equal("the core physically calibrates once per exit batch", minimum.batches(), minimum.calibrationCycles());
        equal("calibration time is not a handwritten pacing target",
                minimum.calibrationCycles() * ResearchLogic.CYCLE_TICKS, minimum.calibrationTicks());
        equal("calibration energy is exact at its canonical rated work",
                minimum.calibrationTicks() * ResearchLogic.FU_PER_TICK, minimum.calibrationFu());
        equal("the minimum boundary states its immediate infrastructure",
                Set.of("item:minecraft:crafting_table", "item:grindless:research_terminal"), minimum.immediateInfrastructure());
        var witness = ProgressionCostModel.witness(result);
        equal("reuse requires the existing conditional gate, not an unsupported summary", true, witness.conditionalGatePass());
        equal("one projected cost row per real replayed route", result.tiers().get(0).establishmentBatches().size(),
                witness.batches().size());
        long ticks = 0;
        long fu = 0;
        for (var batch : witness.batches()) {
            var route = result.authorities().routes().get(batch.route());
            equal("cost row binds an existing live route", true, route != null);
            equal("complete-batch count is the actual replay count",
                    result.tiers().get(0).establishmentBatches().get(batch.route()), batch.batches());
            equal("duration includes every executed batch", route.durationTicks() * batch.batches(), batch.serialTicks());
            equal("FU includes every executed batch", route.fuPerTick() * batch.serialTicks(), batch.fuDemand());
            equal("critical production never relies on buying the goal", false, batch.route().startsWith("shop/"));
            if (route.oneShot()) {
                equal("one-time quest rewards stay finite in the cost model", 1L, batch.batches());
            }
            ticks = Math.addExact(ticks, batch.serialTicks());
            fu = Math.addExact(fu, batch.fuDemand());
        }
        equal("serial work is the sum, not elapsed player time", ticks, witness.serialWorkTicks());
        equal("total demand reconciles the individual live rows", fu, witness.fuDemand());
        for (String machine : Set.of("geological_collector", "biocultivator", "biomass_dynamo", "quern", "clay_mixer",
                "firing_chamber", "settling_trough", "research_terminal")) {
            equal("full canonical witness accounts for " + machine, true,
                    witness.installedMachinesAndParts().containsKey("item:grindless:" + machine));
        }
        equal("old starter quantity is not nerfed by the model", 128L,
                ProgressionCostModel.starterAllocation().get("minecraft:cobblestone"));
        equal("old water allocation remains", 2L, ProgressionCostModel.starterAllocation().get("minecraft:water_bucket"));
        var document = EconomyProjection.document(result);
        var json = JsonParser.parseString(EconomyProjection.json(result)).getAsJsonObject();
        equal("projection is deterministic", EconomyProjection.json(result), EconomyProjection.json(result));
        equal("every declared offer is exported", ShopCatalogue.offers().size(), json.getAsJsonArray("offers").size());
        for (var element : json.getAsJsonArray("offers")) {
            var offer = element.getAsJsonObject();
            equal("initial acquisition never invents a permanent finite exchange cap",
                    true, offer.get("repeatable_exchange").getAsBoolean());
            equal("the same physical payment can be renewed later",
                    true, offer.get("same_payment_accepts_later_production").getAsBoolean());
            if (offer.get("id").getAsString().startsWith("bootstrap_")) {
                var dirt = offer.getAsJsonObject("paid_inputs_status").getAsJsonObject("minecraft:dirt");
                var resource = new SustainabilityModel.Resource(SustainabilityModel.Kind.ITEM, "minecraft:dirt");
                var declared = result.annotations().resources().get(resource);
                equal("restart provenance preserves actual dirt classification",
                        declared.resourceClass().name(), dirt.get("classification").getAsString());
                equal("restart provenance preserves the actual later renewable dirt route",
                        declared.canonicalRoute(), dirt.get("canonical_route").getAsString());
                equal("shop never overrides canonical renewable metadata",
                        false, dirt.get("resource_classification_overridden").getAsBoolean());
            }
        }
        equal("all conversion/quest/offer edges are exported", EconomyGraph.rows().size(), json.getAsJsonArray("exploit_graph").size());
        equal("no shop dependency is added to canonical recipes", false, document.get("shop_required_for_t0"));
        equal("scope keeps human pacing unmeasured", true, json.getAsJsonArray("unmeasured").toString().contains("overall_pacing"));
        equal("the composed certificate remains conservative", true, EconomyCycleProof.check().conservative());
        equal("a real finite witness retains startup assumptions", false, witness.conditions().isEmpty());
        return assertions;
    }

    private static void equal(String name, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
