package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.process.Agitation;
import io.github.ezequiel24123z.grindless.process.ConditionReport;
import io.github.ezequiel24123z.grindless.process.ConditionState;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.ManufacturingPolicy;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.T1Recipes;
import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/** Live first-factory contracts, not a fixture promoted to T1 acceptance or renewable closure. */
public final class VerifyT1Progression {
    private static int assertions;

    private VerifyT1Progression() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        String exporter = Files.readString(root.resolve(Path.of("tools", "t1", "T1Export.java")));
        var fingerprinted = java.util.regex.Pattern.compile(
                "for \\(Class<\\?> type : List\\.of\\(([\\s\\S]*?)\\)\\) \\{").matcher(exporter);
        List<String> imageExpressions = fingerprinted.find()
                ? java.util.Arrays.stream(fingerprinted.group(1).split(",")).map(String::strip).toList() : List.of();
        equal("the exporter fingerprints typed inputs, the physical decoder and actual wet-proof implementations (source contract)", true,
                List.of("AuthorityAdapters.class", "VerifySustainabilityRegistry.class",
                        "IngredientSpec.class", "IngredientSpec.MatrixRole.class", "ControlMatrixSpec.class",
                        "ControlMatrixNbt.class", "MatrixArchitecture.class", "ResourceGuidanceContext.class",
                        "QuestCatalogue.class", "QuestCatalogue.Task.class", "QuestCatalogue.Reward.class",
                        "QuestCatalogue.Chapter.class", "QuestCatalogue.Evidence.class", "QuestCatalogue.Type.class",
                        "ProcessLogic.class", "ProcessLookup.class", "FluxTier.class", "MachineMenuKind.class",
                        "ProcessMachineKind.class", "ProcessMachineKind.ASSEMBLER.getClass()",
                        "ProcessMachineKind.CHEMICAL_WASHER.getClass()", "ProcessMachineKind.ARC_FURNACE.getClass()",
                        "MachineEnvelopes.class", "ConditionState.class", "Agitation.class",
                        "ProcessConditions.class", "ProcessConditions.Builder.class",
                        "ConditionEnvelope.class", "ConditionEnvelope.Builder.class", "ConditionBand.class",
                        "ConditionReport.class", "ConditionFault.class",
                        "GeneratorKind.class", "SustainabilityAudit.class", "SustainabilityAudit.ExecutedBatch.class",
                        "SustainabilityAudit.WitnessState.class", "SustainabilityAudit.FiniteResult.class",
                        "SustainabilityAudit.TierResult.class", "SustainabilityAudit.Result.class",
                        "ConstructionReserve.class", "ConstructionReserve.Custody.class",
                        "SustainabilityAnnotations.class", "SustainabilityAnnotations.EntryProfile.class",
                        "SustainabilityModel.class", "SustainabilityModel.Snapshot.class",
                        "T0ConditionalGate.class", "T0Sustainability.class", "DebtManifest.class",
                        "AuditFinding.class", "AuditReports.class", "VerifyEconomy.class").stream()
                        .allMatch(imageExpressions::contains));
        var sources = java.util.regex.Pattern.compile(
                "SOURCES = List\\.of\\(([\\s\\S]*?)\\);").matcher(exporter);
        List<String> sourceExpressions = sources.find()
                ? java.util.Arrays.stream(sources.group(1).split(",")).map(String::strip).toList() : List.of();
        equal("the exporter binds the exact sources of physical defaults, catalogue and wet applicability claims (source contract)", true,
                List.of("\"tools/checks/VerifySustainabilityRegistry.java\"",
                        "JAVA + \"matrix/ControlMatrixNbt.java\"", "JAVA + \"matrix/MatrixArchitecture.java\"",
                        "JAVA + \"guidance/ResourceGuidanceContext.java\"",
                        "JAVA + \"quest/QuestCatalogue.java\"", "JAVA + \"machine/ProcessMachineKind.java\"",
                        "JAVA + \"recipe/ProcessLogic.java\"", "JAVA + \"recipe/ProcessLookup.java\"",
                        "JAVA + \"energy/FluxTier.java\"", "JAVA + \"menu/MachineMenuKind.java\"",
                        "JAVA + \"process/MachineEnvelopes.java\"", "JAVA + \"process/ConditionState.java\"",
                        "JAVA + \"process/Agitation.java\"", "JAVA + \"process/ProcessConditions.java\"",
                        "JAVA + \"process/ConditionEnvelope.java\"", "JAVA + \"process/ConditionBand.java\"",
                        "JAVA + \"process/ConditionReport.java\"", "JAVA + \"process/ConditionFault.java\"",
                        "JAVA + \"machine/GeneratorKind.java\"", "JAVA + \"sustainability/SustainabilityAudit.java\"",
                        "JAVA + \"sustainability/ConstructionReserve.java\"",
                        "JAVA + \"sustainability/SustainabilityAnnotations.java\"", "JAVA + \"sustainability/SustainabilityModel.java\"",
                        "JAVA + \"sustainability/T0ConditionalGate.java\"", "JAVA + \"sustainability/T0Sustainability.java\"",
                        "JAVA + \"sustainability/DebtManifest.java\"", "JAVA + \"sustainability/AuditFinding.java\"",
                        "JAVA + \"sustainability/AuditReports.java\"", "\"tools/checks/VerifyEconomy.java\"")
                        .stream().allMatch(sourceExpressions::contains));
        equal("the exporter pins the actual shared manufacturing policy API, not a duplicated sustainability catalogue",
                true, sourceExpressions.contains("JAVA + \"recipe/ManufacturingPolicy.java\"")
                        && imageExpressions.contains("ManufacturingPolicy.class")
                        && imageExpressions.contains("Class.forName(\"io.github.ezequiel24123z.grindless.recipe.ManufacturingPolicy$Kind\")"));
        SustainabilityAudit.Result t0 = canonicalResult(root);
        int checks = verify(t0);
        Path generated = root.resolve(Path.of("docs", "tiers", "t1-production-graph.json"));
        if (!Files.isRegularFile(generated)) {
            throw new AssertionError("Run tools/t1/export before the integrated gate; do not author its JSON by hand");
        }
        equal("committed first-factory data equals the actual canonical projection",
                T1Sustainability.json(t0), Files.readString(generated, StandardCharsets.UTF_8).replace("\r\n", "\n"));
        Path markdown = generated.resolveSibling("t1-production-graph.md");
        equal("generated human costs equal the same actual routes", T1Sustainability.markdown(t0),
                Files.readString(markdown, StandardCharsets.UTF_8).replace("\r\n", "\n"));
        System.out.println("ALL T1 FIRST-FACTORY CONTRACTS PASSED: " + (checks + 5)
                + "; source/conditional-model scope, not native play or full T1 closure");
    }

    static SustainabilityAudit.Result canonicalResult(Path root) throws Exception {
        return VerifyEconomy.canonicalResult(root);
    }

    public static int verify(SustainabilityAudit.Result t0) {
        int before = assertions;
        Snapshot snapshot = t0.authorities();
        var gate = T0ConditionalGate.evaluate(t0);
        equal("the reused boundary is the actual conditional T0 gate; unmet=" + gate.unmetObligations()
                        + "; blocking=" + gate.decisions().stream().filter(T0ConditionalGate.Decision::blocking)
                        .map(T0ConditionalGate.Decision::fingerprint).toList(),
                true, gate.pass());
        Resource firing = item("grindless:firing_chamber");
        var firingCapital = t0.annotations().resources().get(firing);
        equal("the manual-Furnace-fed formed controller is finite construction, not an R0 item promise",
                SustainabilityAnnotations.ResourceClass.BOOTSTRAP, firingCapital.resourceClass());
        equal("local automatic construction does not author an unlimited controller renewal tier",
                null, firingCapital.firstRenewableTier());
        equal("the real allowed controller constructor remains selected", "bench/firing_chamber",
                firingCapital.canonicalRoute());
        equal("loss of automatic controller-item supply does not remove renewable operating ceramics", true,
                t0.tiers().get(0).renewable().contains(item("grindless:ceramic_insulator")));
        equal("new declarations do not silently activate the canonical T0 model's next tier",
                true, t0.tiers().get(1).unknown() != null);
        Map<String, List<Route>> lines = T1Sustainability.selectedRoutes(snapshot);
        equal("each declared line resolves", T1Progression.lines().size(), lines.size());
        Set<String> lineIds = new HashSet<>();
        for (T1Progression.Line line : T1Progression.lines()) {
            equal("line identities are unique: " + line.id(), true, lineIds.add(line.id()));
            equal("line lookups preserve the declaration: " + line.id(), line, T1Progression.byId(line.id()));
            for (String dependency : line.requires()) {
                T1Progression.Line parent = T1Progression.byId(dependency);
                equal("mandatory lines never require an optional branch: " + line.id(), true,
                        !line.mandatory() || parent.mandatory());
            }
            acyclic(line.id(), new HashSet<>(), new HashSet<>());
            for (Route route : lines.get(line.id())) {
                equal("every selected route is the actual live object: " + route.id(), route,
                        snapshot.routes().get(route.id()));
                equal("a selected manufacturing route is not a quest gift: " + route.id(), false, route.oneShot());
                equal("a named line does not invent an external route: " + route.id(), true, route.modOwned());
            }
        }
        equal("only the physical F2 line is the exit",
                List.of("relay_f2"), T1Progression.lines().stream()
                        .filter(line -> line.requirement() == T1Progression.Requirement.EXIT)
                        .map(T1Progression.Line::id).toList());
        for (String optional : List.of("component_fabrication", "wire_forming", "tool_reproduction",
                "wet_beneficiation", "extraction", "logistics", "storage", "kiln", "factory_space")) {
            equal("the optional branch is not a first-factory mandate: " + optional, false,
                    T1Progression.byId(optional).mandatory());
        }

        List<Route> mandatory = T1Sustainability.mandatoryRoutes(snapshot);
        Set<String> families = Set.of("CRAFTING_TABLE", "THERMAL_GENERATOR", "PULVERIZER",
                "ARC_FURNACE", "PRESS", "ASSEMBLER");
        for (Route route : mandatory) {
            equal("first-factory machines stay in the existing bounded families: " + route.id(), true,
                    route.family() == null || families.contains(route.family()));
            equal("every mandatory process keeps an F1-or-lower draw: " + route.id(), true,
                    route.fuPerTick() <= FluxTier.F1.nominal());
            equal("no optional travel or finite extraction route becomes required: " + route.id(), false,
                    route.id().contains("factory_portal") || route.id().contains("terrestrial_extractor")
                            || route.id().contains("wire_mill") || route.id().contains("chemical_reactor"));
            for (ProcessMachineKind kind : ProcessMachineKind.values()) {
                if (kind.family().name().equals(route.family())) {
                    equal("actual selected process rating is not beyond F1: " + route.id(), true,
                            kind.tier().ordinal() <= FluxTier.F1.ordinal());
                }
            }
        }
        var seed = T1Sustainability.seedAllocation(snapshot);
        T1Recipes.Shaped craft = T1Recipes.shaped().stream().filter(row -> row.name().equals("relay_matrix"))
                .findFirst().orElseThrow();
        equal("seed quantity comes from the existing craft", (long) craft.resultCount(), seed.batchOutput());
        equal("the seed funds exactly the real five first-machine crafts",
                Set.of("craft/thermal_generator", "craft/pulverizer", "craft/arc_furnace",
                        "craft/press", "craft/assembler"), seed.machineDraws().keySet());
        equal("the existing initial batch can fund construction", true, seed.remaining() >= 0);
        equal("construction consumes each controller rather than treating it as a catalyst",
                seed.batchOutput(), seed.constructionDraw());
        Route reproduction = snapshot.routes().get(T1Progression.REPRODUCTION_ROUTE);
        Route manual = snapshot.routes().get(T1Progression.ENTRY_ROUTE);
        equal("the automatic F1 route keeps the manual physical input bill",
                physicalInputs(manual), physicalInputs(reproduction));
        equal("automatic F1 reproduction accounts for its complete rated FU demand separately",
                Math.multiplyExact(reproduction.durationTicks(), reproduction.fuPerTick()),
                reproduction.inputs().get(ENERGY));
        equal("the automatic F1 route keeps the complete manual batch", manual.outputs(), reproduction.outputs());
        equal("the manual exit is still manual", false, manual.automatable());
        equal("the Assembler removes repetition through the existing executor", true, reproduction.automatable());
        equal("the Bench still cannot manufacture the frontier Matrix", false,
                snapshot.routes().containsKey("bench/relay_matrix"));
        physicalControllerSupply(t0);
        finiteContinuation(t0);
        componentRoutes(snapshot);
        componentFabrication(snapshot);
        thermalGeneratorAssembly(t0);
        wireForming(snapshot);
        toolReproduction(snapshot);
        wetBeneficiation(t0);

        Route exit = snapshot.routes().get(T1Progression.EXIT_ROUTE);
        Resource ratedF1 = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX, FluxTier.F1.ordinal(), "items");
        Resource ratedF2 = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX, FluxTier.F2.ordinal(), "items");
        equal("the F2 recipe explicitly consumes the prior physical rating", true, exit.inputs().containsKey(ratedF1));
        equal("the terminal product is physically F2, not an untagged item", Set.of(ratedF2), exit.outputs().keySet());
        equal("the exit keeps exactly the prior controller, structure, dielectric and power coupling",
                Set.of(ratedF1, item(FabricationLogic.MACHINE_CASING), item("minecraft:glass"),
                        item(FabricationLogic.FLUX_CELL)), physicalInputs(exit).keySet());
        equal("physical F2 manufacturing accounts for its complete rated FU demand separately",
                Math.multiplyExact(exit.durationTicks(), exit.fuPerTick()), exit.inputs().get(ENERGY));
        equal("F2 manufacturing does not acquire a world permission", false,
                exit.conditions().containsKey("informational_milestone") || !exit.prerequisites().isEmpty());
        equal("a legacy untagged Relay is physically F1 under the existing NBT mechanism",
                FluxTier.F1, ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating());
        equal("the exit is still produced by the first-tier Assembler", "ASSEMBLER", exit.family());
        equal("under-volting remains usable instead of becoming a permission wall", true,
                FluxTier.F1.throughputFactor(FluxTier.F0) > 0.0);

        for (Map.Entry<String, String> die : Map.of(
                "press/plate/iron", FabricationLogic.PLATE_DIE,
                "press/rod/iron", FabricationLogic.ROD_DIE,
                "press/gear/iron", FabricationLogic.GEAR_DIE,
                "press/coil/copper", FabricationLogic.COIL_DIE).entrySet()) {
            Route route = snapshot.routes().get(die.getKey());
            equal("the actual Press die is retained: " + die.getKey(), Set.of(item(die.getValue())),
                    route.catalysts().keySet());
            equal("a die is not silently consumed: " + die.getKey(), false,
                    route.inputs().containsKey(item(die.getValue())));
        }
        Route power = snapshot.routes().get(T1Progression.POWER_ROUTE);
        equal("canonical F1 fuel is the mod-owned renewable biomass", Set.of(item(SourceLogic.BIOMASS)),
                power.inputs().keySet());
        equal("the existing T0 audit proves the fuel boundary", true,
                t0.tiers().get(0).renewable().contains(item(SourceLogic.BIOMASS)));
        equal("actual Thermal fuel energy has its real F1 rate",
                Math.multiplyExact(power.durationTicks(), FluxTier.F1.nominal()), power.outputs().get(ENERGY));
        for (String id : List.of("b1_r1/iron", "b1_r1/copper")) {
            Route route = snapshot.routes().get(id);
            equal("reduction records actual slag: " + id, true, route.outputs().containsKey(item("grindless:slag")));
            equal("reduction CO stays physically discharged: " + id, true,
                    route.discharged().contains(new Resource(Kind.FLUID, "grindless:carbon_monoxide")));
        }
        var inputs = T1Sustainability.inputs(t0);
        for (var input : inputs) {
            equal("every mandatory edge has a source or an explicit physical/model boundary: "
                    + input.route() + "/" + input.resource().key(), false, input.status().equals("UNRESOLVED"));
            for (String producer : input.producers()) {
                equal("input evidence never names a proposed or absent producer: " + producer, true,
                        snapshot.routes().containsKey(producer));
            }
        }
        var casingSeed = inputs.stream().filter(input -> input.route().equals("craft/assembler")
                && input.resource().equals(item(FabricationLogic.MACHINE_CASING))).findFirst().orElseThrow();
        equal("the first Assembler retains finite manual casing construction",
                "MANUAL_FINITE_FIRST_ASSEMBLER_SEED", casingSeed.status());
        equal("the first Assembler never depends on its own automatic casing output",
                List.of("craft/machine_casing"), casingSeed.producers());
        for (Map.Entry<String, String> component : Map.of(
                FabricationLogic.MACHINE_CASING, T1Progression.CASING_ROUTE,
                FabricationLogic.FLUX_CELL, T1Progression.FLUX_CELL_ROUTE).entrySet()) {
            var recurring = inputs.stream().filter(input -> input.route().equals(T1Progression.EXIT_ROUTE)
                    && input.resource().equals(item(component.getKey()))).findFirst().orElseThrow();
            equal("the F2 component has an implemented automatic recurrent route: " + component.getKey(),
                    "IMPLEMENTED_AUTOMATIC_RECURRENT_INPUT_NOT_RENEWABILITY_PROOF", recurring.status());
            equal("the recurrent component does not substitute a manual craft: " + component.getKey(),
                    List.of(component.getValue()), recurring.producers());
        }
        var flint = t0.annotations().resources().get(item("minecraft:flint"));
        equal("T1 first-use teaching leaves the canonical T0 flint annotation unchanged",
                SustainabilityAnnotations.SideStreamClass.BYPRODUCT_OPTIONAL, flint.sideStreamClass());
        equal("the canonical T0 profile does not acquire an invented flint first-use tier", null, flint.firstUseTier());
        atlas();
        rejects("a stale line identity fails visibly", () -> T1Progression.byId("not_a_line"));
        rejects("a stale task identity fails visibly", () -> T1Progression.byQuest("not_a_task"));
        rejects("duplicate declaration route identities cannot masquerade as separate production",
                () -> new T1Progression.Line("duplicate", T1Progression.Requirement.MANDATORY,
                        List.of("craft/press", "craft/press"), Set.of()));
        for (String id : List.of("craft/machine_casing", "craft/flux_cell",
                T1Progression.CASING_ROUTE, T1Progression.FLUX_CELL_ROUTE,
                "assemble/plate_die", "assemble/rod_die", "assemble/gear_die", "assemble/coil_die",
                "assemble/thermal_generator_core", "craft/thermal_generator_from_core",
                "craft/wire_mill", "mill/wire/iron", "mill/wire/copper", "mill/coil/copper",
                "assemble/motor", "craft/chemical_washer", "b2/iron", "b2/copper",
                "b2_r1/iron", "b2_r1/copper")) {
            Map<String, Route> missing = new TreeMap<>(snapshot.routes());
            missing.remove(id);
            Snapshot incomplete = new Snapshot(snapshot.resources(), missing, snapshot.terminals(), snapshot.tags(),
                    snapshot.objectives(), snapshot.inputHashes(), snapshot.findings());
            rejects("a missing selected component or optional manufacturing authority fails visibly: " + id,
                    () -> T1Sustainability.selectedRoutes(incomplete));
        }
        String json = T1Sustainability.json(t0);
        equal("repeated projection of the same actual proof is byte-deterministic", json, T1Sustainability.json(t0));
        equal("normal output uses canonical LF", false, json.contains("\r"));
        Map<String, String> rawHashes = new TreeMap<>(snapshot.inputHashes());
        rawHashes.put("test:raw_checkout_hash_only", "0".repeat(64));
        Snapshot differentRawBytes = new Snapshot(snapshot.resources(), snapshot.routes(), snapshot.terminals(),
                snapshot.tags(), snapshot.objectives(), rawHashes, snapshot.findings());
        var rawFingerprintFixture = new SustainabilityAudit.Result(differentRawBytes, t0.annotations(), t0.tiers(),
                t0.debt(), t0.findings());
        equal("an existing checkpoint cannot be relabelled onto changed raw authority sources",
                "predecessor_incompatible", T1Sustainability.finiteWitness(rawFingerprintFixture).findings().get(0).rule());
        var freshlyBound = SustainabilityAudit.run(differentRawBytes, t0.annotations(), t0.debt());
        equal("raw checkout identity remains distinguishable in separate provenance", false,
                AuditReports.authorityHash(snapshot).equals(AuditReports.authorityHash(differentRawBytes)));
        equal("raw source fingerprint changes do not rewrite canonical recipe/conditional-model facts",
                json, T1Sustainability.json(freshlyBound));
        equal("human batch costs are likewise independent of checkout-only provenance",
                T1Sustainability.markdown(t0), T1Sustainability.markdown(freshlyBound));
        var document = JsonParser.parseString(json).getAsJsonObject();
        var wireDocument = document.getAsJsonArray("lines")
                .get(T1Progression.lines().indexOf(T1Progression.byId("wire_forming"))).getAsJsonObject();
        equal("the existing exporter projects the new line identity", "wire_forming",
                wireDocument.get("id").getAsString());
        equal("the existing exporter keeps wire forming optional", "OPTIONAL",
                wireDocument.get("requirement").getAsString());
        equal("the existing exporter includes all four live base routes", 4,
                wireDocument.getAsJsonArray("routes").size());
        var toolDocument = document.getAsJsonArray("lines")
                .get(T1Progression.lines().indexOf(T1Progression.byId("tool_reproduction"))).getAsJsonObject();
        equal("the existing exporter projects the optional tool line", "tool_reproduction",
                toolDocument.get("id").getAsString());
        equal("the existing exporter keeps tool reproduction optional", "OPTIONAL",
                toolDocument.get("requirement").getAsString());
        equal("the existing exporter includes all four live die routes", 4,
                toolDocument.getAsJsonArray("routes").size());
        var factoryDocument = document.getAsJsonArray("lines")
                .get(T1Progression.lines().indexOf(T1Progression.byId("component_fabrication"))).getAsJsonObject();
        equal("the existing exporter projects optional component fabrication", "component_fabrication",
                factoryDocument.get("id").getAsString());
        equal("the existing exporter keeps component fabrication optional", "OPTIONAL",
                factoryDocument.get("requirement").getAsString());
        equal("the existing exporter includes the actual core and its manual consumer", 2,
                factoryDocument.getAsJsonArray("routes").size());
        var wetDocument = document.getAsJsonArray("lines")
                .get(T1Progression.lines().indexOf(T1Progression.byId("wet_beneficiation"))).getAsJsonObject();
        equal("the existing exporter projects the optional wet line", "wet_beneficiation",
                wetDocument.get("id").getAsString());
        equal("the existing exporter keeps wet beneficiation optional", "OPTIONAL",
                wetDocument.get("requirement").getAsString());
        equal("the existing exporter includes all six live wet authorities", 6,
                wetDocument.getAsJsonArray("routes").size());
        equal("raw source hashes are not embedded in committed model data", false, document.has("authority_sha256"));
        equal("manual-only Furnace policy is attributed to the coordinator, not invented owner confirmation",
                "MANUAL_ONLY_COORDINATOR_DECISION_NOT_OWNER_CONFIRMED",
                document.get("external_furnace_policy").getAsString());
        equal("the Furnace uses its actual existing manual authority, not a new recipe",
                "vanilla/minecraft:furnace", document.get("external_furnace_manual_route").getAsString());
        equal("the retired automatic Furnace projections do not survive as an exemption",
                List.of(),
                java.util.stream.StreamSupport.stream(
                                document.getAsJsonArray("external_furnace_automatic_routes").spliterator(), false)
                        .map(element -> element.getAsString()).toList());
        for (String flag : List.of("t1_accepted", "t1_renewable_closure_proven", "global_optimum_claimed")) {
            equal("bounded source evidence cannot invent " + flag, false, document.get(flag).getAsBoolean());
        }
        var finite = T1Sustainability.finiteWitness(t0);
        equal("the finite flag comes from the actual executor verdict, not an authored true",
                finite.completed(), document.get("t1_finite_establishment_replayed").getAsBoolean());
        equal("actual completed finite batches are exported", finite.batches().size(),
                document.getAsJsonObject("finite_execution").getAsJsonArray("batches").size());
        equal("every finite blocking finding is exported", finite.findings().size(),
                document.getAsJsonObject("finite_execution").getAsJsonArray("findings").size());
        equal("finite establishment removes only its own proof limitation",
                List.of("The finite conditional continuation does not certify recursive T1 renewable closure or the broader T1 family contract.",
                        "This finite witness chooses automatic recurring components where available; manual alternatives remain valid player choices. Complete recurrent input, power and routing closure is not proved.",
                        "Continuous factory power, nighttime/restart recovery, routing throughput and captured-gas handling need separate evidence.",
                        "Native client, Survival play, actual crafting and longitudinal save compatibility are not proved by this export."),
                T1Sustainability.document(t0).get("unproved"));
        equal("every direct consumption/catalyst edge is exported", inputs.size(),
                document.getAsJsonArray("mandatory_input_evidence").size());
        return assertions - before;
    }

    private static void finiteContinuation(SustainabilityAudit.Result t0) {
        var finite = T1Sustainability.finiteWitness(t0);
        equal("the actual immutable conditional T0 predecessor is admitted", true, finite.admitted());
        equal("the actual mandatory first-factory batches and physical F2 execute; findings=" + finite.findings()
                        + "; state=" + T1Sustainability.finiteDiagnostics(t0, finite),
                true, finite.completed());
        equal("no conservative execution finding is hidden", List.of(), finite.findings());
        var before = t0.tiers().get(0).witnessState();
        var after = finite.state();
        equal("live continuation needs complete predecessor and resulting custody", true, before != null && after != null);
        if (before == null || after == null) {
            return;
        }
        var diagnostics = T1Sustainability.finiteDiagnostics(t0, finite);
        equal("bounded diagnostics report the actual ending operation counter", after.operations(),
                diagnostics.get("after_operations"));
        equal("bounded diagnostics count only actual completed batches", finite.batches().size(),
                diagnostics.get("completed_batches"));
        equal("the checkpoint is the actual issued predecessor", before, finite.predecessor());
        equal("no bootstrap source is loaded a second time", before.bootstrapSources(), after.bootstrapSources());
        equal("guaranteed delivery accounting remains unchanged", before.guaranteedDeliveries(), after.guaranteedDeliveries());
        equal("no initial terminal stock is mistaken for a fresh budget", true,
                after.operations() > before.operations() && after.operations() <= 4096);
        equal("no work budget is reset at T1", t0.tiers().get(0).manualWork().budgetActions(),
                finite.manualWork().budgetActions());
        equal("the spent T0 startup work is never refunded", true,
                after.consumedManualActions() >= before.consumedManualActions());
        equal("conditional material deliveries stay inside the original finite bill", true,
                after.playerDeliveries().entrySet().stream().allMatch(entry -> entry.getValue()
                        <= t0.annotations().ledger().get(0).entryProfile().playerProvided().getOrDefault(entry.getKey(), 0L)));
        equal("already installed T0 controllers are preserved", true,
                after.installedMachines().containsAll(before.installedMachines()));
        before.installationParts().forEach((family, parts) ->
                equal("T0 formed parts stay dedicated: " + family, parts, after.installationParts().get(family)));
        equal("already paid one-shot routes remain claimed", true, after.claimedRoutes().containsAll(before.claimedRoutes()));
        for (String id : before.claimedRoutes()) {
            equal("a prior one-shot cannot complete again: " + id, false,
                    finite.batches().stream().anyMatch(batch -> batch.route().equals(id)));
        }
        for (String id : after.claimedRoutes()) {
            equal("all one-shots execute at most once across both tiers: " + id, 1L,
                    after.establishmentBatches().get(id));
        }
        Map<String, Long> completed = new TreeMap<>();
        finite.batches().forEach(batch -> completed.merge(batch.route(), 1L, Math::addExact));
        for (String id : T1Progression.finiteRouteIds()) {
            equal("every actual finite-witness selection has a completed batch: " + id, true,
                    completed.getOrDefault(id, 0L) > 0);
        }
        equal("the finite witness does not force both valid Flux Cell methods", false, completed.containsKey("craft/flux_cell"));
        equal("manual Flux Cells remain a real player-facing alternative", true,
                T1Progression.byId("relay_f2").routeIds().contains("craft/flux_cell"));
        for (T1Progression.Line line : T1Progression.lines()) {
            if (!line.mandatory()) {
                equal("optional selections never become finite construction obligations: " + line.id(),
                        false, line.routeIds().stream().anyMatch(completed::containsKey));
            }
        }
        Resource f1 = AuthorityAdapters.physical(item(FabricationLogic.RELAY_MATRIX));
        Resource f2 = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX, FluxTier.F2.ordinal(), "items");
        var allocation = T1Sustainability.seedAllocation(t0.authorities());
        equal("the completed T0 exit supplies exactly the existing five physical F1", 5L, before.freeStock().get(f1));
        long draw = 0;
        for (var machine : allocation.machineDraws().entrySet()) {
            equal("each first controller is fabricated once: " + machine.getKey(), 1L, completed.get(machine.getKey()));
            Route route = t0.authorities().routes().get(machine.getKey());
            manualNormalConstruction(route);
            Resource block = route.retainedOutputs().keySet().iterator().next();
            equal("each fabricated first controller is installed: " + block.key(), true, after.installedMachines().contains(block));
            equal("an installed controller is not also spendable stock: " + block.key(), 0L,
                    after.freeStock().getOrDefault(block, 0L));
            long spent = finite.batches().stream().filter(batch -> batch.route().equals(machine.getKey()))
                    .mapToLong(batch -> batch.consumed().getOrDefault(f1, 0L)).sum();
            equal("each live machine really consumes its one physical F1: " + machine.getKey(), machine.getValue(), spent);
            draw = Math.addExact(draw, spent);
        }
        equal("only the inherited five controllers fund the first factory", allocation.constructionDraw(), draw);
        equal("the manual T0 terminal craft is never replayed in T1", false,
                finite.batches().stream().anyMatch(batch -> batch.route().equals(T1Progression.ENTRY_ROUTE)));
        int reproduction = -1;
        for (int index = 0; index < finite.batches().size(); index++) {
            var batch = finite.batches().get(index);
            if (batch.route().equals(T1Progression.REPRODUCTION_ROUTE)) {
                reproduction = index;
                break;
            }
            if (batch.consumed().getOrDefault(f1, 0L) > 0) {
                equal("before reproduction only the five real machine crafts withdraw seed F1",
                        true, allocation.machineDraws().containsKey(batch.route()));
            }
        }
        equal("the Assembler actually reproduces physical F1 before further control withdrawals", true, reproduction >= 0);
        if (reproduction >= 0) {
            equal("actual reproduction yields the unchanged exact physical batch", Map.of(f1, allocation.batchOutput()),
                    finite.batches().get(reproduction).produced());
            for (String machine : allocation.machineDraws().keySet()) {
                final int at = reproduction;
                equal("all five seed-funded machines precede reproduction: " + machine, true,
                        finite.batches().subList(0, at).stream().anyMatch(batch -> batch.route().equals(machine)));
            }
        }
        var exit = finite.exitProduction();
        equal("the exit uses the actual exact-rated F2 authority", T1Progression.EXIT_ROUTE, exit.route());
        equal("the manufactured exit identity is exactly F2", f2, exit.resource());
        equal("the actual F2 batch executes once rather than accepting a final stock count", 1L, exit.executedBatches());
        equal("actual F2 stock equals newly produced physical output", exit.produced(), after.freeStock().get(f2));
        long producedF1 = finite.batches().stream().mapToLong(batch -> batch.produced().getOrDefault(f1, 0L)).sum();
        long consumedF1 = finite.batches().stream().mapToLong(batch -> batch.consumed().getOrDefault(f1, 0L)).sum();
        equal("the entire F1 balance is physical, not an exit-delivery credit",
                before.freeStock().get(f1) + producedF1 - consumedF1, after.freeStock().get(f1));
        for (String die : List.of(FabricationLogic.PLATE_DIE, FabricationLogic.ROD_DIE,
                FabricationLogic.GEAR_DIE, FabricationLogic.COIL_DIE)) {
            equal("each actual Press die remains locked: " + die, 1L, after.retainedCatalysts().get(item(die)));
            equal("a retained die is not also free material: " + die, 0L, after.freeStock().getOrDefault(item(die), 0L));
            equal("a retained die is not an installed surrounding part: " + die, false,
                    after.installationParts().values().stream().anyMatch(parts -> parts.containsKey(item(die))));
        }
        Map<Resource, Long> expected = physicalCustody(before);
        finite.batches().forEach(batch -> {
            batch.consumed().forEach((resource, count) -> expected.merge(resource, -count, Math::addExact));
            batch.produced().forEach((resource, count) -> expected.merge(resource, count, Math::addExact));
        });
        after.playerDeliveries().forEach((resource, count) ->
                expected.merge(resource, count - before.playerDeliveries().getOrDefault(resource, 0L), Math::addExact));
        Map<Resource, Long> actual = physicalCustody(after);
        expected.entrySet().removeIf(entry -> entry.getValue() == 0 || entry.getKey().kind() == Kind.ENVIRONMENT);
        actual.entrySet().removeIf(entry -> entry.getValue() == 0 || entry.getKey().kind() == Kind.ENVIRONMENT);
        equal("all physical stock, catalyst and installation axes balance actual completed authority batches", expected, actual);
        equal("no physical balance is negative", true, actual.values().stream().allMatch(count -> count >= 0));
        equal("a missing predecessor is explicitly refused at the T1 boundary", "predecessor_missing",
                T1Sustainability.finiteWitness(null).findings().get(0).rule());
        equal("displaying the manual F1 alternative does not replay the inherited T0 exit in this witness", false,
                T1Progression.finiteRouteIds().contains(T1Progression.ENTRY_ROUTE));

        // Model-contract mutation only: an incorrect reproduction output cannot supply the exact
        // predecessor. Even the resulting physical F2 stock cannot replace execution of the exit.
        Snapshot snapshot = t0.authorities();
        Route original = snapshot.routes().get(T1Progression.REPRODUCTION_ROUTE);
        Map<String, Route> routes = new TreeMap<>(snapshot.routes());
        routes.put(original.id(), new Route(original.id(), original.authority(), original.source(), original.family(),
                original.inputs(), original.catalysts(), Map.of(f2, allocation.batchOutput()), original.discharged(),
                original.durationTicks(), original.fuPerTick(), original.conditions(), original.prerequisites(),
                original.oneShot(), original.modOwned(), original.automatable()));
        Snapshot wrongRating = new Snapshot(snapshot.resources(), routes, snapshot.terminals(), snapshot.tags(),
                snapshot.objectives(), snapshot.inputHashes(), snapshot.findings());
        var mutatedT0 = SustainabilityAudit.run(wrongRating, t0.annotations(), t0.debt());
        var wrong = T1Sustainability.finiteWitness(mutatedT0);
        equal("a physically incompatible reproduction output cannot complete finite T1", false, wrong.completed());
        equal("a different-rating reproduction does not secretly replay the manual seed", false,
                wrong.batches().stream().anyMatch(batch -> batch.route().equals(T1Progression.ENTRY_ROUTE)));
        equal("stock of the goal's identity still cannot replace its real manufacturing batch", true,
                wrong.exitProduction() != null && wrong.exitProduction().executedBatches() == 0);
    }

    private static Map<Resource, Long> physicalCustody(SustainabilityAudit.WitnessState state) {
        Map<Resource, Long> result = new TreeMap<>(state.freeStock());
        state.retainedCatalysts().forEach((resource, count) -> result.merge(resource, count, Math::addExact));
        state.installedMachines().forEach(resource -> result.merge(resource, 1L, Math::addExact));
        state.installationParts().values().forEach(parts ->
                parts.forEach((resource, count) -> result.merge(resource, count, Math::addExact)));
        return result;
    }

    private static void physicalControllerSupply(SustainabilityAudit.Result t0) {
        Snapshot snapshot = t0.authorities();
        Resource f1 = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX,
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating().ordinal(), "items");
        equal("the existing seed batch remains exactly five physical controllers", 5L,
                T1Sustainability.seedAllocation(snapshot).batchOutput());
        equal("only actual authored manual and Assembler routes produce physical F1",
                Set.of(T1Progression.ENTRY_ROUTE, T1Progression.REPRODUCTION_ROUTE),
                snapshot.routes().values().stream().filter(route -> route.retainedOutputs().containsKey(f1))
                        .map(Route::id).collect(java.util.stream.Collectors.toSet()));
        equal("the manual and Assembler producer batches agree on actual physical F1", Map.of(f1, 5L),
                snapshot.routes().get(T1Progression.REPRODUCTION_ROUTE).outputs());
        for (var input : T1Sustainability.inputs(t0)) {
            if (input.resource().equals(f1)) {
                equal("all physical F1 inputs retain the finite-seed/reproduction boundary: " + input.route(),
                        "PHYSICAL_T0_SEED_OR_T1_REPRODUCTION_NOT_T0_RENEWABLE", input.status());
                equal("physical F1 inputs name both actual matching producers: " + input.route(),
                        List.of(T1Progression.ENTRY_ROUTE, T1Progression.REPRODUCTION_ROUTE), input.producers());
            }
        }
        equal("the explicit F1 predecessor has no invented minimum-control role", false,
                snapshot.routes().get(T1Progression.EXIT_ROUTE).conditions().keySet().stream()
                        .anyMatch(key -> key.startsWith("minimum_control_")));
        equal("physical identity agreement is not a T0 renewable Matrix claim", false,
                t0.tiers().get(0).renewable().contains(f1));
        Map<String, Route> changed = new TreeMap<>(snapshot.routes());
        for (String id : List.of(T1Progression.REPRODUCTION_ROUTE, T1Progression.ENTRY_ROUTE)) {
            Route actual = changed.get(id);
            Resource f2 = new Resource(Kind.ITEM, f1.id(), FluxTier.F2.ordinal(), "items");
            changed.put(id, new Route(actual.id(), actual.authority(), actual.source(), actual.family(),
                    actual.inputs(), actual.catalysts(), Map.of(f2, 5L), actual.discharged(),
                    actual.durationTicks(), actual.fuPerTick(), actual.conditions(), actual.prerequisites(),
                    actual.oneShot(), actual.modOwned(), actual.automatable()));
            Snapshot mutated = new Snapshot(snapshot.resources(), changed, snapshot.terminals(), snapshot.tags(),
                    snapshot.objectives(), snapshot.inputHashes(), snapshot.findings());
            var fixture = new SustainabilityAudit.Result(mutated, t0.annotations(), t0.tiers(), t0.debt(), t0.findings());
            var predecessor = T1Sustainability.inputs(fixture).stream()
                    .filter(input -> input.route().equals(T1Progression.EXIT_ROUTE) && input.resource().equals(f1))
                    .findFirst().orElseThrow();
            boolean none = id.equals(T1Progression.ENTRY_ROUTE);
            equal("a different physical output cannot remain an F1 producer: " + id,
                    none ? List.of() : List.of(T1Progression.ENTRY_ROUTE), predecessor.producers());
            equal("an identity mismatch remains an explicit unresolved edge: " + id,
                    none ? "UNRESOLVED" : "PHYSICAL_T0_SEED_OR_T1_REPRODUCTION_NOT_T0_RENEWABLE",
                    predecessor.status());
            if (none) {
                rejects("the seed allocator cannot substitute explicit F2 for default F1",
                        () -> T1Sustainability.seedAllocation(mutated));
            }
        }
    }

    private static void wireForming(Snapshot snapshot) {
        T1Progression.Line line = T1Progression.byId("wire_forming");
        equal("optional wire forming selects only the existing base manufacturing and mill routes",
                List.of("craft/wire_mill", "mill/wire/iron", "mill/wire/copper", "mill/coil/copper"),
                line.routeIds());
        equal("optional wire forming follows only established assembly", Set.of("assembly"), line.requires());
        equal("optional wire forming remains after assembly", true,
                T1Progression.lines().indexOf(line)
                        > T1Progression.lines().indexOf(T1Progression.byId("assembly")));
        equal("optional wire routes never enter the first-F2 mandatory path", false,
                T1Sustainability.mandatoryRoutes(snapshot).stream()
                        .anyMatch(route -> line.routeIds().contains(route.id())));
        equal("the optional branch never draws from the five first-machine controllers",
                new T1Sustainability.SeedAllocation(5, 5, 0, Map.of(
                        "craft/thermal_generator", 1L, "craft/pulverizer", 1L, "craft/arc_furnace", 1L,
                        "craft/press", 1L, "craft/assembler", 1L)),
                T1Sustainability.seedAllocation(snapshot));
        Route manufacture = snapshot.routes().get("craft/wire_mill");
        equal("the ordinary Wire Mill's actual base kind stays F1", FluxTier.F1, ProcessMachineKind.WIRE_MILL.tier());
        manualNormalConstruction(manufacture);
        equal("the base mill consumes one controller at its actual F1 kind rating", 1L,
                manufacture.inputs().get(new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX,
                        ProcessMachineKind.WIRE_MILL.tier().ordinal(), "items")));
        equal("the audit labels that F1 supply as one valid minimum-control option",
                "item:grindless:relay_matrix [F1]", manufacture.conditions().get("minimum_control_key_M"));
        equal("the audit does not promise exhaustive alternate-controller supply",
                "canonical_option_not_exhaustive_alternatives",
                manufacture.conditions().get("control_supply_projection"));
        equal("the F2 frontier still has no minimum-control predecessor",
                false, snapshot.routes().get(T1Progression.EXIT_ROUTE).conditions().keySet().stream()
                        .anyMatch(key -> key.startsWith("minimum_control_")));
        equal("the base mill output is one ordinary item, not an upgraded chassis",
                Map.of(item(FabricationLogic.WIRE_MILL), 1L), manufacture.outputs());
        for (String id : line.routeIds()) {
            Route route = snapshot.routes().get(id);
            if (!id.equals("craft/wire_mill")) {
                equal("the optional operating route reuses process authority: " + id,
                        Authority.PROCESS_GRAPH, route.authority());
                equal("the optional operating route retains its F1 draw: " + id,
                        FluxTier.F1.nominal(), route.fuPerTick());
                equal("the optional milling route uses the real base Wire Mill: " + id,
                        "WIRE_MILL", route.family());
            }
        }
        equal("the Assembler task itself remains on mandatory assembly",
                T1Progression.byId("assembly"), T1Progression.byQuest("assembler"));
    }

    private static void toolReproduction(Snapshot snapshot) {
        T1Progression.Line line = T1Progression.byId("tool_reproduction");
        equal("optional tool reproduction selects only the four source-backed die routes",
                List.of("assemble/plate_die", "assemble/rod_die", "assemble/gear_die", "assemble/coil_die"),
                line.routeIds());
        equal("optional tool reproduction follows only the existing Assembler line",
                Set.of("assembly"), line.requires());
        equal("the tool reproduction line remains optional", T1Progression.Requirement.OPTIONAL,
                line.requirement());
        for (String die : List.of("plate_die", "rod_die", "gear_die", "coil_die")) {
            Route route = snapshot.routes().get("assemble/" + die);
            equal("the die route uses the existing Assembler: " + die, "ASSEMBLER", route.family());
            equal("the die route remains ordinary live process authority: " + die,
                    Authority.PROCESS_GRAPH, route.authority());
            equal("the die route has its exact ordinary output: " + die,
                    Map.of(item("grindless:" + die), 1L), route.outputs());
            equal("the die route is automated without a new prerequisite: " + die, true, route.automatable());
            equal("the die route adds no catalyst: " + die, Map.of(), route.catalysts());
        }
        for (String task : List.of("forming_dies", "copper_coil", "assembler", "matrix_line")) {
            equal("automatic die reproduction does not rewrite quest route selection: " + task, false,
                    T1Progression.questRouteIds(task).stream().anyMatch(line.routeIds()::contains));
        }
        equal("mandatory forming still depends only on reduction", Set.of("reduction"),
                T1Progression.byId("forming").requires());
        equal("the first F2 exit still depends only on assembly", Set.of("assembly"),
                T1Progression.byId("relay_f2").requires());
        equal("the first F2 quest retains its existing production route ids",
                List.of("craft/flux_cell", T1Progression.FLUX_CELL_ROUTE, T1Progression.EXIT_ROUTE),
                T1Progression.byId("relay_f2").routeIds());
        equal("no automatic die route enters the mandatory first-F2 path", false,
                T1Sustainability.mandatoryRoutes(snapshot).stream()
                        .anyMatch(route -> line.routeIds().contains(route.id())));
    }

    private static void wetBeneficiation(SustainabilityAudit.Result t0) {
        Snapshot snapshot = t0.authorities();
        T1Progression.Line line = T1Progression.byId("wet_beneficiation");
        equal("wet beneficiation selects only the six accepted existing authorities",
                List.of("assemble/motor", "craft/chemical_washer", "b2/iron", "b2/copper",
                        "b2_r1/iron", "b2_r1/copper"), line.routeIds());
        equal("wet beneficiation follows only established assembly", Set.of("assembly"), line.requires());
        equal("wet beneficiation remains optional", T1Progression.Requirement.OPTIONAL, line.requirement());
        equal("wet teaching follows assembly without reordering the first factory", true,
                T1Progression.lines().indexOf(line)
                        > T1Progression.lines().indexOf(T1Progression.byId("assembly")));
        equal("the new lesson selects its actual optional line", line, T1Progression.byQuest("wet_beneficiation"));
        equal("the new lesson selects all six authorities without a copied bill",
                line.routeIds(), T1Progression.questRouteIds("wet_beneficiation"));
        equal("wet routes never enter the mandatory first-F2 path", false,
                T1Sustainability.mandatoryRoutes(snapshot).stream()
                        .anyMatch(route -> line.routeIds().contains(route.id())));
        equal("first F2 still depends only on assembly", Set.of("assembly"),
                T1Progression.byId("relay_f2").requires());
        equal("first F2 retains its complete route selection",
                List.of("craft/flux_cell", T1Progression.FLUX_CELL_ROUTE, T1Progression.EXIT_ROUTE),
                T1Progression.byId("relay_f2").routeIds());
        equal("wet construction does not consume the finite first-five controller allocation",
                new T1Sustainability.SeedAllocation(5, 5, 0, Map.of(
                        "craft/thermal_generator", 1L, "craft/pulverizer", 1L, "craft/arc_furnace", 1L,
                        "craft/press", 1L, "craft/assembler", 1L)),
                T1Sustainability.seedAllocation(snapshot));
        ProcessMachineKind washer = ProcessMachineKind.CHEMICAL_WASHER;
        equal("the existing Washer really is default F1", FluxTier.F1, washer.tier());
        equal("the existing Washer really holds ambient STIRRED conditions",
                ConditionState.AMBIENT.withAgitation(Agitation.STIRRED), washer.heldConditions());
        equal("the existing fluid buffer fits the complete live wash batch", true,
                ProcessLogic.WASH_WATER_MB <= FluidLogic.MACHINE_CAPACITY);
        List<ProcessGraph.MaterialView> materials = AuthorityAdapters.materialViews();
        var processes = ProcessGraph.generate(materials);
        var atlas = T1Progression.atlasForQuest("wet_beneficiation", AtlasLogic.allEntries(processes));
        equal("every wet route is actually representable in the Atlas", Set.of(), atlas.nonAtlasRouteIds());
        for (String id : line.routeIds()) {
            Route route = snapshot.routes().get(id);
            if (id.equals("craft/chemical_washer")) {
                manualNormalConstruction(route);
                equal("manual Washer construction retains its actual ordinary Atlas output",
                        List.of("item:" + FabricationLogic.CHEMICAL_WASHER), atlas.entries().stream()
                                .filter(entry -> entry.id().equals(id)).findFirst().orElseThrow().outputs());
                continue;
            }
            var process = processes.stream().filter(recipe -> recipe.id().equals(id)).findFirst().orElseThrow();
            equal("wet projection preserves all live inputs, outputs, conditions and costs: " + id,
                    AuthorityAdapters.process(process), route);
            equal("wet Atlas preserves the complete live recipe: " + id, AtlasLogic.entry(process),
                    atlas.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow());
            equal("wet routes reuse ordinary process authority: " + id, Authority.PROCESS_GRAPH, route.authority());
            equal("wet routes retain an actual F1 power demand: " + id, FluxTier.F1.nominal(), route.fuPerTick());
            equal("wet routes account for every paid tick: " + id,
                    Math.multiplyExact(route.durationTicks(), route.fuPerTick()), route.inputs().get(ENERGY));
            equal("wet routes add no permission gate: " + id, Set.of(), route.prerequisites());
            ProcessMachineKind kind = java.util.Arrays.stream(ProcessMachineKind.values())
                    .filter(candidate -> candidate.family() == process.family()).findFirst().orElseThrow();
            equal("the real machine is rated for the selected wet process: " + id, true,
                    process.fuPerTick() <= kind.tier().nominal());
            equal("the existing input slots fit the actual grouped wet inputs: " + id, true,
                    process.itemInputs().size() <= kind.menuKind().inputs());
            equal("the existing output slots fit both actual wet output streams: " + id, true,
                    process.itemOutputs().size() <= kind.menuKind().outputs());
            var conditions = ProcessLookup.conditionsOf(process);
            equal("the real machine envelope admits the selected wet process: " + id, true,
                    conditions.checkEnvelope(kind.envelope()).canRun());
            equal("held conditions support the actual full-speed wet cadence: " + id,
                    ConditionReport.OPTIMAL, conditions.evaluate(kind.heldConditions()));
        }
        Route motor = snapshot.routes().get("assemble/motor");
        equal("the Motor component remains an actual Assembler product", "ASSEMBLER", motor.family());
        equal("the component Motor retains its complete controller-free bill",
                Map.of(item(FabricationLogic.MACHINE_CASING), 1L, item(FabricationLogic.COPPER_COIL), 2L,
                        new Resource(Kind.TAG, "forge:rods/iron"), 1L), physicalInputs(motor));
        equal("the component Motor still has no minimum-control role", false,
                motor.conditions().keySet().stream().anyMatch(key -> key.startsWith("minimum_control_")));
        Route manufacture = snapshot.routes().get("craft/chemical_washer");
        Resource controller = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX, FluxTier.F1.ordinal(), "items");
        equal("the baseline Washer retains its material quantities and consumes exactly one F1 control option",
                Map.of(item(FabricationLogic.MACHINE_CASING), 1L, item(FabricationLogic.MOTOR), 2L,
                        new Resource(Kind.TAG, "forge:plates/iron"), 4L, controller, 1L), physicalInputs(manufacture));
        equal("manual Washer construction exposes its actual shaped-key control role", List.of("minimum_control_key_M"),
                manufacture.conditions().keySet().stream().filter(key -> key.startsWith("minimum_control_")).toList());
        equal("the Washer names real F1 supply rather than an invented rated item",
                "item:grindless:relay_matrix [F1]", manufacture.conditions().get("minimum_control_key_M"));
        equal("the Washer's minimum role remains one canonical supply option, not exhaustive alternatives",
                "canonical_option_not_exhaustive_alternatives",
                manufacture.conditions().get("control_supply_projection"));
        equal("the Washer still produces the ordinary baseline machine",
                Map.of(item(FabricationLogic.CHEMICAL_WASHER), 1L), manufacture.outputs());
        equal("manual Washer final construction does not claim a powered cycle", 0L, manufacture.durationTicks());
        equal("manual Washer final construction does not claim a powered draw", 0L, manufacture.fuPerTick());
        for (String material : List.of("iron", "copper")) {
            Resource crushed = new Resource(Kind.TAG, "grindless:crushed_materials/" + material);
            Resource washed = new Resource(Kind.TAG, "grindless:washed_crushed_materials/" + material);
            Resource water = new Resource(Kind.FLUID, ProcessLogic.WATER);
            Resource secondary = new Resource(Kind.TAG, "forge:ingots/"
                    + ProcessGraph.secondaryOf(materials, material).orElseThrow());
            Route wash = snapshot.routes().get("b2/" + material);
            equal("B2 consumes actual crushed feed, water and energy: " + material,
                    Set.of(crushed, water, ENERGY), wash.inputs().keySet());
            equal("B2 preserves the authority's crushed batch: " + material,
                    (long) ProcessLogic.WASH_CRUSHED, wash.inputs().get(crushed));
            equal("B2 preserves the authority's water quantity: " + material,
                    (long) ProcessLogic.WASH_WATER_MB, wash.inputs().get(water));
            equal("B2 preserves both real output streams: " + material,
                    Set.of(washed, secondary), wash.outputs().keySet());
            equal("B2 retains its washed primary batch: " + material,
                    (long) ProcessLogic.WASH_CRUSHED, wash.outputs().get(washed));
            equal("B2 retains the pack-derived secondary ingot: " + material, 1L, wash.outputs().get(secondary));
            equal("neither B2 stream is silently discharged: " + material, wash.outputs(), wash.retainedOutputs());
            equal("B2 cadence is taken from ProcessLogic: " + material,
                    (long) ProcessLogic.WASH_TICKS, wash.durationTicks());
            Route dryReduction = snapshot.routes().get("b1_r1/" + material);
            Route wetReduction = snapshot.routes().get("b2_r1/" + material);
            Map<Resource, Long> reducedInputs = new TreeMap<>(dryReduction.inputs());
            Long feed = reducedInputs.remove(crushed);
            equal("the existing dry reduction really consumes crushed feed: " + material, true, feed != null);
            reducedInputs.put(washed, feed);
            equal("B2-R1 changes only the actual feed to washed material: " + material,
                    reducedInputs, wetReduction.inputs());
            equal("B2-R1 retains reduction products, slag and CO: " + material,
                    dryReduction.outputs(), wetReduction.outputs());
            equal("B2-R1 retains actual emissions: " + material,
                    dryReduction.discharged(), wetReduction.discharged());
            equal("B2-R1 retains actual reduction cadence: " + material,
                    dryReduction.durationTicks(), wetReduction.durationTicks());
            equal("water supply comes from the existing conditional T0 source proof: " + material, true,
                    t0.tiers().get(0).renewable().contains(water));
            equal("optional wet selection does not fabricate a T0 washed-feed certificate: " + material, false,
                    t0.tiers().get(0).renewable().contains(washed));
            var incomplete = materials.stream().map(view -> view.name().equals(material)
                    ? new ProcessGraph.MaterialView(view.name(), view.raw(), view.ore(), view.crushed(), view.oxide(),
                    view.ingot(), view.plate(), view.rod(), view.gear(), false) : view).toList();
            rejects("missing washed-form metadata cannot silently retain a wet lesson: " + material,
                    () -> T1Progression.atlasForQuest("wet_beneficiation",
                            AtlasLogic.allEntries(ProcessGraph.generate(incomplete))));
        }
        rejects("missing secondary-material metadata cannot manufacture a trace output",
                () -> T1Progression.atlasForQuest("wet_beneficiation", AtlasLogic.allEntries(ProcessGraph.generate(
                        materials.stream().filter(view -> view.name().equals("iron")).toList()))));
    }

    private static void componentFabrication(Snapshot snapshot) {
        rejects("the obsolete factory-reproduction goal is retired, not renamed as a live machine factory",
                () -> T1Progression.byId("factory_reproduction"));
        T1Progression.Line line = T1Progression.byId("component_fabrication");
        equal("optional fabrication selects a real component and manual consumer only",
                List.of("assemble/thermal_generator_core", "craft/thermal_generator_from_core"), line.routeIds());
        equal("optional component fabrication follows assembly", Set.of("assembly"), line.requires());
        equal("component fabrication stays optional", T1Progression.Requirement.OPTIONAL, line.requirement());
        equal("component fabrication is taught after assembly",
                T1Progression.lines().indexOf(T1Progression.byId("assembly")) + 1,
                T1Progression.lines().indexOf(line));
        for (String machine : List.of("pulverizer", "arc_furnace", "press", "assembler", "thermal_generator",
                "wire_mill", "chemical_washer")) {
            equal("obsolete automatic final construction is absent: " + machine, false,
                    snapshot.routes().containsKey("assemble/" + machine));
            manualNormalConstruction(snapshot.routes().get("craft/" + machine));
        }
        for (Route route : snapshot.routes().values()) {
            equal("no live automatic producer can manufacture policy-classified normal machines/processors: " + route.id(), false,
                    route.automatable() && route.retainedOutputs().keySet().stream()
                            .anyMatch(resource -> ManufacturingPolicy.isManualFinalMachine(resource.id())));
        }
        equal("the Assembler quest remains bound to the existing assembly line",
                T1Progression.byId("assembly"), T1Progression.byQuest("assembler"));
        equal("the Assembler lesson keeps its established route selection",
                List.of("craft/assembler", T1Progression.CASING_ROUTE, T1Progression.REPRODUCTION_ROUTE,
                        "craft/wire_mill", "mill/wire/iron", "mill/wire/copper", "mill/coil/copper"),
                T1Progression.questRouteIds("assembler"));
        equal("the optional factory line does not become a first-F2 prerequisite",
                Set.of("assembly"), T1Progression.byId("relay_f2").requires());
        equal("the first F2 route keeps its exact identity", "assemble/relay_matrix_f2",
                T1Progression.EXIT_ROUTE);
    }

    private static void thermalGeneratorAssembly(SustainabilityAudit.Result t0) {
        Snapshot snapshot = t0.authorities();
        Resource component = item(FabricationLogic.THERMAL_GENERATOR_CORE);
        Route core = snapshot.routes().get("assemble/thermal_generator_core");
        Route finished = snapshot.routes().get("craft/thermal_generator_from_core");
        Route manual = snapshot.routes().get("craft/thermal_generator");
        equal("the manual first Thermal remains the first power seed", false, manual.automatable());
        equal("the mandatory power line still selects manual Thermal construction",
                List.of("craft/thermal_generator", "craft/flux_pylon_mk1", T1Progression.POWER_ROUTE),
                T1Progression.byId("power").routeIds());
        equal("the core is one ordinary component without rating/state", Map.of(component, 1L), core.outputs());
        equal("the core has exactly its three physical source groups",
                Set.of(item("minecraft:furnace"), new Resource(Kind.TAG, "grindless:carbon"),
                        new Resource(Kind.TAG, "forge:ingots/iron")), physicalInputs(core).keySet());
        var furnaceCapital = t0.annotations().resources().get(item("minecraft:furnace"));
        equal("core manufacture consumes manually supplied Furnace capital, not a renewable machine item",
                SustainabilityAnnotations.ResourceClass.MANUAL_CAPITAL, furnaceCapital.resourceClass());
        equal("the capital source is the actual unchanged manual vanilla authority",
                "vanilla/minecraft:furnace", furnaceCapital.canonicalRoute());
        equal("local machine-only core processing does not imply automatic net Furnace supply", false,
                t0.tiers().get(0).renewable().contains(item("minecraft:furnace")));
        equal("the core never consumes a hidden Matrix", false,
                physicalInputs(core).keySet().stream().anyMatch(resource -> resource.id().equals(FabricationLogic.RELAY_MATRIX)));
        Resource controller = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX, FluxTier.F1.ordinal(), "items");
        equal("final Thermal exposes exactly one canonical F1 control option", 1L, finished.inputs().get(controller));
        equal("manual final Thermal exposes its source-backed minimum control option", true,
                finished.conditions().values().contains("item:grindless:relay_matrix [F1]"));
        equal("the core has no minimum-control role", false, core.conditions().keySet().stream()
                .anyMatch(key -> key.startsWith("minimum_control_")));
        equal("the final controller retains truthful canonical-option provenance",
                "canonical_option_not_exhaustive_alternatives", finished.conditions().get("control_supply_projection"));
        Map<Resource, Long> composed = new TreeMap<>(physicalInputs(core));
        physicalInputs(finished).forEach((resource, amount) -> composed.merge(resource, amount, Math::addExact));
        equal("exactly one core is consumed and cancelled from the composed input bill", 1L, composed.remove(component));
        equal("the same physical F1 controller is present once in both manual final methods", 1L,
                composed.get(controller));
        equal("both Thermal steps preserve the full manual construction materials", physicalInputs(manual), composed);
        equal("the manual core consumer preserves the ordinary machine identity", manual.outputs(), finished.outputs());
        manualNormalConstruction(finished);
        equal("the core remains actual automatic process authority", Authority.PROCESS_GRAPH, core.authority());
        equal("the core uses the existing Assembler", "ASSEMBLER", core.family());
        equal("the core remains automatic work", true, core.automatable());
        equal("the core retains its actual complete cycle", (long) FabricationLogic.ASSEMBLE_TICKS, core.durationTicks());
        equal("the core retains its actual F1 draw", FabricationLogic.FU_PER_TICK, core.fuPerTick());
        equal("the core accounts for all paid work",
                Math.multiplyExact(core.durationTicks(), core.fuPerTick()), core.inputs().get(ENERGY));
        for (Route stage : List.of(core, finished)) {
            equal("Thermal adds no retained catalyst: " + stage.id(), Map.of(), stage.catalysts());
            equal("Thermal adds no permission: " + stage.id(), Set.of(), stage.prerequisites());
            equal("neither Thermal step enters the mandatory first-F2 path: " + stage.id(), false,
                    T1Sustainability.mandatoryRoutes(snapshot).contains(stage));
        }
        equal("manual final assembly adds no fictitious second powered process", core.durationTicks(),
                Math.addExact(core.durationTicks(), finished.durationTicks()));
        equal("the composed work charges only the actual automatic core operation", core.inputs().get(ENERGY),
                Math.addExact(core.inputs().get(ENERGY), finished.inputs().getOrDefault(ENERGY, 0L)));
        var metadata = t0.annotations().resources().get(component);
        equal("the new component does not invent a renewable supply class", null, metadata.resourceClass());
        equal("the new component is not mislabeled a by-product", null, metadata.sideStreamClass());
        equal("its actual optional first-use tier is recorded", Integer.valueOf(1), metadata.firstUseTier());
        equal("its renewable tier remains explicitly unproved", null, metadata.firstRenewableTier());
        equal("its canonical producer is the real core operation", core.id(), metadata.canonicalRoute());
        equal("normal component stock may be stored or discarded without invented hazards",
                new SustainabilityAnnotations.Handling(SustainabilityAnnotations.Action.STORE, true, false, null, null),
                metadata.handling());
        equal("the component invents no future consumer or recycling route", true,
                metadata.futureConsumers().isEmpty() && metadata.recycling() == null);
        var guidance = io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceProjection.project(t0)
                .resources().get(component);
        equal("shared guidance retains the exact canonical metadata", metadata, guidance.metadata());
        equal("shared guidance derives its one real producer from the graph", List.of(core.id()),
                guidance.producers().stream().map(reference -> reference.route()).toList());
        equal("shared guidance derives its one real consumer from the graph", List.of(finished.id()),
                guidance.consumers().stream().map(reference -> reference.route()).toList());
        equal("shared guidance records the actual producer source", core.source(),
                guidance.sources().get("canonical:" + core.id()));
        equal("the new manufacturing routes stay optional T1 declarations, not T0 roots", true,
                List.of(core, finished).stream().allMatch(stage -> {
                    var policy = t0.annotations().routes().get(stage.id());
                    return policy.optional() && Integer.valueOf(1).equals(policy.unlockTier());
                }));
    }

    private static void componentRoutes(Snapshot snapshot) {
        for (String component : List.of("machine_casing", "flux_cell")) {
            Route manual = snapshot.routes().get("craft/" + component);
            Route automatic = snapshot.routes().get("assemble/" + component);
            equal("the manual component craft remains a non-automatic alternative: " + component,
                    false, manual.automatable());
            equal("the automatic component uses the existing live graph: " + component,
                    Authority.PROCESS_GRAPH, automatic.authority());
            equal("the component uses the existing Assembler: " + component, "ASSEMBLER", automatic.family());
            equal("the component is an implemented automatic route: " + component, true, automatic.automatable());
            equal("the component keeps the actual shipped manual input multiset: " + component,
                    physicalInputs(manual), physicalInputs(automatic));
            equal("the component keeps the actual shipped ordinary output multiset: " + component,
                    manual.outputs(), automatic.outputs());
            equal("the component uses the existing shared cycle: " + component,
                    (long) FabricationLogic.ASSEMBLE_TICKS, automatic.durationTicks());
            equal("the component uses the existing shared draw: " + component,
                    FabricationLogic.FU_PER_TICK, automatic.fuPerTick());
            equal("the component accounts for its complete process energy: " + component,
                    Math.multiplyExact(automatic.durationTicks(), automatic.fuPerTick()), automatic.inputs().get(ENERGY));
            equal("the component adds no die or other catalyst: " + component, Map.of(), automatic.catalysts());
            equal("the component adds no permission or future-tier prerequisite: " + component,
                    Set.of(), automatic.prerequisites());
        }
        equal("forming keeps manual casing construction before the first Assembler", true,
                T1Progression.byId("forming").routeIds().contains("craft/machine_casing"));
        equal("automatic casing cannot become a prerequisite for constructing its machine", false,
                T1Progression.byId("forming").routeIds().contains(T1Progression.CASING_ROUTE));
        equal("automatic casing belongs to the already-established assembly line", true,
                T1Progression.byId("assembly").routeIds().contains(T1Progression.CASING_ROUTE));
        equal("automatic Flux Cells belong to the post-assembly exit line", true,
                T1Progression.byId("relay_f2").routeIds().contains(T1Progression.FLUX_CELL_ROUTE));
        equal("the Flux Cell exit line still follows assembly", Set.of("assembly"),
                T1Progression.byId("relay_f2").requires());
        equal("the Assembler lesson follows construction and reproduction with only optional base wire forming",
                List.of("craft/assembler", T1Progression.CASING_ROUTE, T1Progression.REPRODUCTION_ROUTE,
                        "craft/wire_mill", "mill/wire/iron", "mill/wire/copper", "mill/coil/copper"),
                T1Progression.questRouteIds("assembler"));
        equal("the Flux Cell lesson retains the manual alternative alongside automatic production",
                List.of("craft/flux_cell", T1Progression.FLUX_CELL_ROUTE), T1Progression.questRouteIds("flux_cell"));
    }

    private static void atlas() {
        var rows = AtlasLogic.allEntries(ProcessGraph.generate(AuthorityAdapters.materialViews()));
        for (String task : List.of("thermal", "pylon", "scanner", "terrestrial", "pulverizer",
                "furnace", "press", "assembler", "matrix_line", "glass", "logistics", "kiln",
                "factory_world", "industrial_matrix", "first_grid", "forming_dies", "copper_coil", "flux_cell",
                "wet_beneficiation")) {
            var selected = T1Progression.atlasForQuest(task, rows);
            Set<String> resolved = new TreeSet<>(selected.nonAtlasRouteIds());
            selected.entries().forEach(entry -> resolved.add(entry.id()));
            equal("runtime teaching resolves every selected authority: " + task,
                    new TreeSet<>(T1Progression.questRouteIds(task)), resolved);
            equal("only real generator fuel is explicitly outside the Atlas: " + task, true,
                    selected.nonAtlasRouteIds().stream().allMatch(T1Progression.POWER_ROUTE::equals));
        }
        for (Map.Entry<String, String> selection : Map.of(
                "craft/flux_cell", "flux_cell",
                T1Progression.FLUX_CELL_ROUTE, "flux_cell",
                T1Progression.CASING_ROUTE, "assembler",
                T1Progression.REPRODUCTION_ROUTE, "assembler",
                "craft/wire_mill", "assembler",
                "mill/wire/iron", "assembler",
                "mill/wire/copper", "assembler",
                "mill/coil/copper", "assembler").entrySet()) {
            String id = selection.getKey();
            String task = selection.getValue();
            rejects("a missing actual component Atlas row fails explicitly: " + id,
                    () -> T1Progression.atlasForQuest(task,
                            rows.stream().filter(row -> !row.id().equals(id)).toList()));
            var duplicate = new ArrayList<>(rows);
            duplicate.add(rows.stream().filter(row -> row.id().equals(id)).findFirst().orElseThrow());
            rejects("a duplicate live component row cannot duplicate an explanation: " + id,
                    () -> T1Progression.atlasForQuest(task, duplicate));
        }
        equal("the glass lesson explains mod-owned renewable feed, not a vanilla farm", true,
                T1Progression.questRouteIds("glass").containsAll(List.of("source/geological_aggregate",
                        "source/biomass", "bootstrap/dry_aggregate", "bootstrap/biocarbon")));
    }

    private static void acyclic(String id, Set<String> visiting, Set<String> visited) {
        if (visited.contains(id)) {
            return;
        }
        if (!visiting.add(id)) {
            throw new AssertionError("Cyclic first-factory line dependency at " + id);
        }
        T1Progression.byId(id).requires().forEach(parent -> acyclic(parent, visiting, visited));
        visiting.remove(id);
        visited.add(id);
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, id);
    }

    private static Map<Resource, Long> physicalInputs(Route route) {
        Map<Resource, Long> inputs = new TreeMap<>(route.inputs());
        inputs.keySet().removeIf(resource -> resource.kind() == Kind.ENERGY);
        return inputs;
    }

    private static void manualNormalConstruction(Route route) {
        equal("manual final construction exists", true, route != null);
        equal("normal final manufacture is manual: " + route.id(), false, route.automatable());
        equal("normal final manufacture uses the real crafting authority: " + route.id(), true,
                Set.of(Authority.CRAFTING_JSON, Authority.T1_RECIPES, Authority.BOOTSTRAP_RECIPES).contains(route.authority()));
        equal("normal final manufacture uses the crafting table: " + route.id(), "CRAFTING_TABLE", route.family());
        equal("manual manufacture has no synthetic powered tick charge: " + route.id(), 0L, route.durationTicks());
        equal("manual manufacture has no synthetic FU draw: " + route.id(), 0L, route.fuPerTick());
        equal("manual manufacture injects no ENERGY requirement: " + route.id(), false, route.inputs().containsKey(ENERGY));
        equal("manual capital refers to genuine normal-machine output contracts: " + route.id(), true,
                !route.retainedOutputs().isEmpty() && route.retainedOutputs().keySet().stream()
                        .allMatch(resource -> ManufacturingPolicy.isManualFinalMachine(resource.id())));
    }

    private static void rejects(String name, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            assertions++;
            return;
        }
        throw new AssertionError(name);
    }

    private static void equal(String name, Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
