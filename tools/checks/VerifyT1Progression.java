package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
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
        System.out.println("ALL T1 FIRST-FACTORY CONTRACTS PASSED: " + (checks + 2)
                + "; source/conditional-model scope, not native play or full T1 closure");
    }

    static SustainabilityAudit.Result canonicalResult(Path root) throws Exception {
        return VerifyEconomy.canonicalResult(root);
    }

    public static int verify(SustainabilityAudit.Result t0) {
        int before = assertions;
        Snapshot snapshot = t0.authorities();
        equal("the reused boundary is the actual conditional T0 gate", true, T0ConditionalGate.evaluate(t0).pass());
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
        for (String optional : List.of("extraction", "logistics", "storage", "kiln", "factory_space")) {
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
        Map<String, Route> missing = new TreeMap<>(snapshot.routes());
        missing.remove("craft/flux_cell");
        Snapshot incomplete = new Snapshot(snapshot.resources(), missing, snapshot.terminals(), snapshot.tags(),
                snapshot.objectives(), snapshot.inputHashes(), snapshot.findings());
        rejects("a missing mandatory JSON craft is not replaced by a successful empty projection",
                () -> T1Sustainability.selectedRoutes(incomplete));
        String json = T1Sustainability.json(t0);
        equal("repeated projection of the same actual proof is byte-deterministic", json, T1Sustainability.json(t0));
        equal("normal output uses canonical LF", false, json.contains("\r"));
        Map<String, String> rawHashes = new TreeMap<>(snapshot.inputHashes());
        rawHashes.put("test:raw_checkout_hash_only", "0".repeat(64));
        Snapshot differentRawBytes = new Snapshot(snapshot.resources(), snapshot.routes(), snapshot.terminals(),
                snapshot.tags(), snapshot.objectives(), rawHashes, snapshot.findings());
        var rawFingerprintFixture = new SustainabilityAudit.Result(differentRawBytes, t0.annotations(), t0.tiers(),
                t0.debt(), t0.findings());
        equal("raw checkout identity remains distinguishable in separate provenance", false,
                AuditReports.authorityHash(snapshot).equals(AuditReports.authorityHash(differentRawBytes)));
        equal("raw source fingerprint changes do not rewrite canonical recipe/conditional-model facts",
                json, T1Sustainability.json(rawFingerprintFixture));
        equal("human batch costs are likewise independent of checkout-only provenance",
                T1Sustainability.markdown(t0), T1Sustainability.markdown(rawFingerprintFixture));
        var document = JsonParser.parseString(json).getAsJsonObject();
        equal("raw source hashes are not embedded in committed model data", false, document.has("authority_sha256"));
        for (String flag : List.of("t1_accepted", "t1_renewable_closure_proven",
                "t1_finite_establishment_replayed", "global_optimum_claimed")) {
            equal("bounded source evidence cannot invent " + flag, false, document.get(flag).getAsBoolean());
        }
        equal("every direct consumption/catalyst edge is exported", inputs.size(),
                document.getAsJsonArray("mandatory_input_evidence").size());
        return assertions - before;
    }

    private static void atlas() {
        var rows = AtlasLogic.allEntries(ProcessGraph.generate(AuthorityAdapters.materialViews()));
        for (String task : List.of("thermal", "pylon", "scanner", "terrestrial", "pulverizer",
                "furnace", "press", "assembler", "matrix_line", "glass", "logistics", "kiln",
                "factory_world", "industrial_matrix", "first_grid", "forming_dies", "copper_coil", "flux_cell")) {
            var selected = T1Progression.atlasForQuest(task, rows);
            Set<String> resolved = new TreeSet<>(selected.nonAtlasRouteIds());
            selected.entries().forEach(entry -> resolved.add(entry.id()));
            equal("runtime teaching resolves every selected authority: " + task,
                    new TreeSet<>(T1Progression.questRouteIds(task)), resolved);
            equal("only real generator fuel is explicitly outside the Atlas: " + task, true,
                    selected.nonAtlasRouteIds().stream().allMatch(T1Progression.POWER_ROUTE::equals));
        }
        rejects("a missing actual Flux Cell Atlas row fails explicitly",
                () -> T1Progression.atlasForQuest("flux_cell",
                        rows.stream().filter(row -> !row.id().equals("craft/flux_cell")).toList()));
        var duplicate = new ArrayList<>(rows);
        duplicate.add(rows.stream().filter(row -> row.id().equals("craft/flux_cell")).findFirst().orElseThrow());
        rejects("a duplicate live Atlas row cannot duplicate a recipe explanation",
                () -> T1Progression.atlasForQuest("flux_cell", duplicate));
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
