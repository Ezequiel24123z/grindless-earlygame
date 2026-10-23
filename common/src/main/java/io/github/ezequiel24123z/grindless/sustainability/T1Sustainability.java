package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.GeneratorKind;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.ManufacturingPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * A bounded T1 projection over the real conditional T0 audit and live recipe authorities.
 * This is not another fixed-point auditor, construction planner or renewable-closure certificate.
 */
public final class T1Sustainability {
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Resource RELAY = AuthorityAdapters.physical(item(FabricationLogic.RELAY_MATRIX));

    public record InputEvidence(String route, Resource resource, long amount, boolean catalyst,
                                String status, List<String> producers) {
        public InputEvidence {
            producers = List.copyOf(producers);
        }
    }

    public record SeedAllocation(long batchOutput, long constructionDraw, long remaining,
                                 Map<String, Long> machineDraws) {
        public SeedAllocation {
            machineDraws = Collections.unmodifiableMap(new TreeMap<>(machineDraws));
        }
    }

    private T1Sustainability() {
    }

    /** Missing declarations fail at their actual authority seam, including optional named routes. */
    public static Map<String, List<Route>> selectedRoutes(Snapshot snapshot) {
        Map<String, List<Route>> selected = new TreeMap<>();
        for (T1Progression.Line line : T1Progression.lines()) {
            List<Route> routes = new ArrayList<>();
            for (String id : line.routeIds()) {
                Route route = snapshot.routes().get(id);
                if (route == null) {
                    throw new IllegalArgumentException("T1 line " + line.id() + " names no live authority: " + id);
                }
                routes.add(route);
            }
            selected.put(line.id(), List.copyOf(routes));
        }
        return Collections.unmodifiableMap(selected);
    }

    /** Method options in required lines, not an instruction to execute every dual-method alternative. */
    public static List<Route> mandatoryRoutes(Snapshot snapshot) {
        Map<String, Route> routes = new TreeMap<>();
        Map<String, List<Route>> selected = selectedRoutes(snapshot);
        T1Progression.lines().stream().filter(T1Progression.Line::mandatory)
                .forEach(line -> selected.get(line.id()).forEach(route -> routes.put(route.id(), route)));
        return List.copyOf(routes.values());
    }

    /**
     * Classifies each method option's direct consumed/catalytic edge. T1 producer links are deliberately not a
     * claim of automatic net supply; recursive T0 supply comes only from the existing audit.
     */
    public static List<InputEvidence> inputs(SustainabilityAudit.Result t0) {
        List<Route> selected = mandatoryRoutes(t0.authorities());
        List<InputEvidence> inputs = new ArrayList<>();
        for (Route route : selected) {
            route.inputs().forEach((resource, amount) ->
                    inputs.add(evidence(t0, selected, route, resource, amount, false)));
            route.catalysts().forEach((resource, amount) ->
                    inputs.add(evidence(t0, selected, route, resource, amount, true)));
        }
        return List.copyOf(inputs);
    }

    /** The existing seed batch is compared with actual Matrix-consuming construction crafts. */
    public static SeedAllocation seedAllocation(Snapshot snapshot) {
        Route seed = require(snapshot, T1Progression.ENTRY_ROUTE);
        Long output = seed.retainedOutputs().get(RELAY);
        if (output == null) {
            throw new IllegalArgumentException("The T0 exit no longer produces its physical Relay Matrix");
        }
        Map<String, Long> draws = new TreeMap<>();
        for (Route route : mandatoryRoutes(snapshot)) {
            if (route.id().startsWith("craft/") && route.inputs().containsKey(RELAY)) {
                draws.put(route.id(), route.inputs().get(RELAY));
            }
        }
        long draw = 0;
        for (long amount : draws.values()) {
            draw = Math.addExact(draw, amount);
        }
        return new SeedAllocation(output, draw, Math.subtractExact(output, draw), draws);
    }

    /** The existing executor consumes actual T0 custody, not seed arithmetic or a new entry profile. */
    public static SustainabilityAudit.FiniteResult finiteWitness(SustainabilityAudit.Result t0) {
        SustainabilityAudit.FiniteResult refusal = SustainabilityAudit.predecessorRefusal(t0);
        if (refusal != null) {
            return refusal;
        }
        // The teaching order is already dependency ordered. Optional branches are not establishment obligations.
        Set<String> selected = new LinkedHashSet<>(T1Progression.finiteRouteIds());
        return SustainabilityAudit.continueFinite(t0, finiteAnnotations(t0, selected),
                List.copyOf(selected), T1Progression.EXIT_ROUTE);
    }

    /**
     * Bounded observations of an actual attempted continuation, never another replay.
     * Sparse quantities remain null/not recorded; diagnostics must not invent zero custody.
     */
    public static Map<String, Object> finiteDiagnostics(SustainabilityAudit.Result t0,
                                                        SustainabilityAudit.FiniteResult finite) {
        var before = finite.predecessor();
        var after = finite.state();
        List<Resource> focus = new ArrayList<>();
        if (t0 != null) {
            for (String key : List.of("energy:FU", "item:grindless:mineral_aggregate", "item:grindless:biomass",
                    "tag:minecraft:stone_crafting_materials", "item:minecraft:cobblestone")) {
                t0.authorities().resources().stream().filter(resource -> resource.key().equals(key))
                        .findFirst().ifPresent(focus::add);
            }
            List<Resource> aliases = new ArrayList<>();
            for (Resource resource : focus) {
                TagResolution tag = t0.authorities().tags().get(resource);
                if (tag != null) {
                    tag.members().stream().limit(4).filter(member -> !focus.contains(member)).forEach(aliases::add);
                }
            }
            focus.addAll(aliases.stream().distinct().toList());
        }
        List<Object> balances = new ArrayList<>();
        for (Resource resource : focus) {
            TagResolution tag = t0.authorities().tags().get(resource);
            balances.add(row("resource", resource.key(),
                    "physical_members", tag == null ? List.of() : tag.members().stream().limit(4).map(Resource::key).toList(),
                    "free_before", before == null ? null : before.freeStock().get(resource),
                    "free_after", after == null ? null : after.freeStock().get(resource),
                    "locked_before", before == null ? null : before.retainedCatalysts().get(resource),
                    "locked_after", after == null ? null : after.retainedCatalysts().get(resource)));
        }
        Map<String, Long> counts = new TreeMap<>();
        finite.batches().forEach(batch -> counts.merge(batch.route(), 1L, Math::addExact));
        Map<String, Long> boundedCounts = new TreeMap<>();
        counts.entrySet().stream().limit(16).forEach(entry -> boundedCounts.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(row("admitted", finite.admitted(), "completed", finite.completed(),
                "before_operations", before == null ? null : before.operations(),
                "after_operations", after == null ? null : after.operations(),
                "completed_batches", finite.batches().size(), "completed_counts", boundedCounts,
                "other_completed_route_count", counts.size() - boundedCounts.size(),
                "last_completed_routes", finite.batches().stream().skip(Math.max(0, finite.batches().size() - 8))
                        .map(SustainabilityAudit.ExecutedBatch::route).toList(),
                "consumed_actions_before", before == null ? null : before.consumedManualActions(),
                "consumed_actions_after", after == null ? null : after.consumedManualActions(),
                "required_actions_before", before == null ? null : before.requiredManualActions(),
                "required_actions_after", after == null ? null : after.requiredManualActions(),
                "cumulative_work_reserve", finite.manualWork(),
                "unestablished_reserves", finite.reserves().stream().filter(reserve ->
                        reserve.margin() < 0 || !reserve.status().equals("conditional_established")).limit(8).toList(),
                "balances", balances, "sparse_quantity_scope", "null means not recorded, not assumed zero"));
    }

    /** Only selected execution bindings are added; T0 classifications, entry, loops and ledger stay exact. */
    private static SustainabilityAnnotations finiteAnnotations(SustainabilityAudit.Result t0, Set<String> selected) {
        SustainabilityAnnotations original = t0.annotations();
        Map<String, RoutePolicy> policies = new TreeMap<>(original.routes());
        for (String id : selected) {
            RoutePolicy prior = policies.get(id);
            if (prior == null || prior.unlockTier() == null) {
                policies.put(id, new RoutePolicy(1, false,
                        prior == null ? Set.of() : prior.environmentalSources(),
                        prior == null ? Map.of() : prior.outputRoles()));
            }
        }
        Map<String, Machine> machines = new TreeMap<>(original.machines());
        for (ProcessMachineKind kind : List.of(ProcessMachineKind.PULVERIZER, ProcessMachineKind.ARC_FURNACE,
                ProcessMachineKind.PRESS, ProcessMachineKind.ASSEMBLER)) {
            String name = kind.name().toLowerCase(Locale.ROOT);
            Route craft = require(t0.authorities(), "craft/" + name);
            Resource block = item("grindless:" + name);
            if (!ManufacturingPolicy.isManualFinalMachine(block.id()) || craft.automatable()
                    || !craft.retainedOutputs().equals(Map.of(block, 1L))) {
                throw new IllegalArgumentException("The selected machine craft no longer constructs " + kind.family());
            }
            machines.putIfAbsent(kind.family().name(), new Machine(kind.family().name(), block, kind.tier().ordinal()));
        }
        Route power = require(t0.authorities(), T1Progression.POWER_ROUTE);
        Resource generator = item("grindless:" + GeneratorKind.THERMAL.id());
        Route generatorCraft = require(t0.authorities(), "craft/" + GeneratorKind.THERMAL.id());
        if (!ManufacturingPolicy.isManualFinalMachine(generator.id()) || generatorCraft.automatable()
                || !generatorCraft.retainedOutputs().equals(Map.of(generator, 1L))) {
            throw new IllegalArgumentException("The selected Thermal construction no longer matches its actual kind");
        }
        machines.putIfAbsent(power.family(), new Machine(power.family(), generator, GeneratorKind.THERMAL.tier().ordinal()));
        return new SustainabilityAnnotations(original.resources(), policies, machines, original.loops(),
                original.plans(), original.ledger());
    }

    /** Deterministic generated data; no source commit, timestamp or private execution path is embedded. */
    public static Map<String, Object> document(SustainabilityAudit.Result t0) {
        return document(t0, finiteWitness(t0));
    }

    private static Map<String, Object> document(SustainabilityAudit.Result t0, SustainabilityAudit.FiniteResult finite) {
        T0ConditionalGate.Gate gate = T0ConditionalGate.evaluate(t0);
        if (!gate.pass()) {
            throw new IllegalArgumentException("The T1 boundary requires the actual passing conditional T0 model: "
                    + gate.unmetObligations());
        }
        Snapshot snapshot = t0.authorities();
        Map<String, List<Route>> selected = selectedRoutes(snapshot);
        SeedAllocation allocation = seedAllocation(snapshot);
        List<Object> lines = new ArrayList<>();
        for (T1Progression.Line line : T1Progression.lines()) {
            lines.add(row("id", line.id(), "requirement", line.requirement().name(),
                    "requires_lines", new TreeSet<>(line.requires()),
                    "route_selection_scope", "Authored methods/options, not an obligation to execute every alternative",
                    "routes", selected.get(line.id()).stream().map(T1Sustainability::routeDocument).toList()));
        }
        List<Object> inputEvidence = new ArrayList<>();
        for (InputEvidence input : inputs(t0)) {
            inputEvidence.add(row("route", input.route(), "resource", input.resource().key(), "amount", input.amount(),
                    "catalyst_retained", input.catalyst(), "status", input.status(), "producers", input.producers()));
        }
        Route power = require(snapshot, T1Progression.POWER_ROUTE);
        Long generated = power.retainedOutputs().get(ENERGY);
        if (generated == null || generated <= 0 || power.durationTicks() <= 0
                || generated % power.durationTicks() != 0) {
            throw new IllegalArgumentException("The selected Thermal fuel authority has no exact positive FU rate");
        }
        long rate = generated / power.durationTicks();
        var witness = t0.tiers().get(0);
        var plan = t0.annotations().ledger().get(0);
        Map<String, Object> document = row(
                "schema_version", 2,
                "scope", "bounded_first_factory_authorities_and_conserved_conditional_finite_execution",
                "authority_snapshot_sha256", AuditReports.snapshotHash(snapshot),
                "provenance_scope", "Normalized evaluated authority facts here; raw sources/classes/revision in build/t1/provenance.json",
                "quantity_authority", "Live ProcessGraph/catalogues and shipped JSON, never this generated document",
                "t1_accepted", false,
                "t1_renewable_closure_proven", false,
                "t1_finite_establishment_replayed", finite.completed(),
                "global_optimum_claimed", false,
                "entry_route", T1Progression.ENTRY_ROUTE,
                "reproduction_route", T1Progression.REPRODUCTION_ROUTE,
                "exit_route", T1Progression.EXIT_ROUTE,
                "lines", lines,
                "finite_selected_routes", T1Progression.finiteRouteIds(),
                "manufacturing_policy", "Policy-classified normal finals are manual capital; operating processes automate. "
                        + "The coordinator includes the vanilla Furnace; other products retain their actual authored methods.",
                "external_furnace_policy", "MANUAL_ONLY_COORDINATOR_DECISION_NOT_OWNER_CONFIRMED",
                "external_furnace_manual_route", require(snapshot, "vanilla/minecraft:furnace").id(),
                "external_furnace_automatic_routes", snapshot.routes().values().stream()
                        .filter(route -> route.automatable()
                                && route.retainedOutputs().containsKey(item("minecraft:furnace")))
                        .map(Route::id).toList(),
                "mandatory_input_evidence", inputEvidence,
                "finite_diagnostics", finiteDiagnostics(t0, finite),
                "finite_execution", finiteDocument(finite),
                "seed_allocation", row("batch_output", allocation.batchOutput(),
                        "construction_draw", allocation.constructionDraw(), "remaining", allocation.remaining(),
                        "machine_draws", allocation.machineDraws(),
                        "scope", "Source-derived first-machine allocation, not observed world custody"),
                "power", row("canonical_route", power.id(), "fuel_inputs", quantities(power.inputs()),
                        "fuel_is_T0_renewable", !power.inputs().isEmpty()
                                && witness.renewable().containsAll(power.netInputs().keySet()),
                        "generation_fu_per_tick", rate, "F1_nominal_fu_per_tick", FluxTier.F1.nominal(),
                        "generated_fu_per_batch", generated, "burn_ticks", power.durationTicks(),
                        "concurrency", "Per-route draws are not a simultaneous factory load or a continuous autonomy proof",
                        "under_volting", "FluxTier throughput degrades; it is not a global recipe permission"),
                "reused_T0_proof", row("conditional_gate_pass", gate.pass(),
                        "entry_profile", plan.entryProfile().id(),
                        "conditions", new TreeSet<>(plan.entryProfile().conditions()),
                        "exit_production", witness.exitProduction(),
                        "retained_construction_and_catalysts", quantities(witness.retainedInventory()),
                        "finite_reserves", witness.reserves(),
                        "scope", "Existing SustainabilityAudit/ConstructionReserve conditional predecessor; not player inventory"),
                "side_streams", sideStreams(t0),
                "unproved", List.of(
                        finite.completed()
                                ? "The finite conditional continuation does not certify recursive T1 renewable closure or the broader T1 family contract."
                                : "The finite conditional continuation is blocked; inspect its actual completed batches and findings.",
                        "This finite witness chooses automatic recurring components where available; manual alternatives remain valid player choices. Complete recurrent input, power and routing closure is not proved.",
                        "Continuous factory power, nighttime/restart recovery, routing throughput and captured-gas handling need separate evidence.",
                        "Native client, Survival play, actual crafting and longitudinal save compatibility are not proved by this export."));
        return Collections.unmodifiableMap(document);
    }

    public static String json(SustainabilityAudit.Result t0) {
        return JSON.toJson(document(t0)) + "\n";
    }

    /** Human-readable batch costs are generated from the same routes, never maintained by hand. */
    public static String markdown(SustainabilityAudit.Result t0) {
        SustainabilityAudit.FiniteResult finite = finiteWitness(t0);
        document(t0, finite);
        Snapshot snapshot = t0.authorities();
        Map<String, List<Route>> selected = selectedRoutes(snapshot);
        SeedAllocation seed = seedAllocation(snapshot);
        StringBuilder text = new StringBuilder("# Generated T1 first-factory production graph\n\n");
        text.append("Generated by `tools/t1/export`; do not edit quantities in this report.\n\n")
                .append("Normalized authority snapshot SHA-256: `").append(AuditReports.snapshotHash(snapshot)).append("`.\n\n")
                .append("Raw source/class fingerprints and invoking revision are recorded separately ")
                .append("in `build/t1/provenance.json`; checkout line endings do not become recipe data.\n\n")
                .append("This is live catalogue data plus a conserved finite continuation of the conditional T0 audit. ")
                .append("It is not T1 acceptance, recursive renewable closure, ")
                .append("an optimum, elapsed playtime or native/client evidence.\n\n")
                .append("The actual manual seed batch produces **").append(seed.batchOutput())
                .append("** controllers; mandatory first-machine crafts draw **").append(seed.constructionDraw())
                .append("**, leaving **").append(seed.remaining()).append("**. ")
                .append("These are source costs, not a promised world inventory.\n\n")
                .append("## Exact batches\n\n")
                .append("Each row is one batch at its catalogue conditions. Alternative recipes ")
                .append("are choices, not mandatory duplicate crafts or simultaneous machine loads. ")
                .append("Policy-classified normal finals, including the vanilla Furnace, are assembled manually; their operating processes automate. ")
                .append("Generator FU output is in the output ")
                .append("column; the demand column is consumption only.\n\n")
                .append("| Line | Role | Live authority | Machine | Consumed inputs | Retained catalysts | Outputs | Ticks | FU/t | Demand FU/batch |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- | ---: | ---: | ---: |\n");
        for (T1Progression.Line line : T1Progression.lines()) {
            for (Route route : selected.get(line.id())) {
                text.append("| ").append(line.id()).append(" | ").append(line.requirement())
                        .append(" | `").append(route.id()).append("` | ")
                        .append(route.family() == null ? "Manual craft" : route.family())
                        .append(" | ").append(describe(route.inputs(), Set.of()))
                        .append(route.conditions().keySet().stream().anyMatch(key -> key.startsWith("minimum_control_"))
                                ? " (minimum control; valid higher-rated registered control may substitute; canonical supply shown)" : "")
                        .append(" | ").append(describe(route.catalysts(), Set.of()))
                        .append(" | ").append(describe(route.outputs(), route.discharged()))
                        .append(" | ").append(route.durationTicks()).append(" | ").append(route.fuPerTick())
                        .append(" | ").append(Math.multiplyExact(route.durationTicks(), route.fuPerTick()))
                        .append(" |\n");
            }
        }
        text.append("\n## Conserved finite execution\n\n")
                .append("Status: **").append(finite.status()).append("**. Completed authority batches: **")
                .append(finite.batches().size()).append("**. No terminal, reward, installed part or work budget ")
                .append("is reset. The JSON exposes before/after custody, exact physical batch withdrawals/outputs ")
                .append("and all blocking findings; these are in-process model facts, not a Survival inventory.\n\n");
        for (AuditFinding finding : finite.findings()) {
            text.append("- `").append(finding.fingerprint()).append("`: ").append(finding.message()).append("\n");
        }
        text.append("\n## Supply and proof boundaries\n\n")
                .append("The coordinator selects manual-only final vanilla Furnace manufacture; this is not an owner-confirmed ")
                .append("Furnace preference. Its existing vanilla manual authority and material bill remain. ")
                .append("Automatic Thermal core work may consume a manually supplied Furnace once for finite capital, ")
                .append("but that does not establish automatic net core supply.\n\n")
                .append("The JSON records every mandatory consumed/catalytic edge separately. ")
                .append("`T0_AUDITED_RENEWABLE` means the existing conditional audit proves that ")
                .append("boundary under its stated environment and finite-establishment assumptions. ")
                .append("A live T1 producer link is not a recursive net-supply certificate.\n\n")
                .append("The F2 recipe consumes explicitly F1 and produces explicitly F2. ")
                .append("Minimum-control roles are separate route conditions; their exact-rated audit ")
                .append("input is one valid canonical supply option, not an exhaustive alternatives catalogue. ")
                .append("Authored Relay defaults and explicit F1 operands now share the existing ")
                .append("NBT decoder's physical F1 identity; generic registration declarations stay separate. The finite ")
                .append("execution above is conditional, not a whole-T1 or world proof. The first Assembler uses a finite ")
                .append("manual casing seed from the forming line; automatic casing production only ")
                .append("starts after that Assembler exists. Existing manual casing and Flux Cell ")
                .append("crafts remain valid alternatives, not just emergency fallbacks. This witness selects ")
                .append("one Flux Cell method, not both; the player need not automate either component. Their Assembler routes ")
                .append("provide implemented automatic recurring components with the same material ")
                .append("bills, not a net-renewability or complete factory-autonomy proof.\n\n")
                .append("Thermal power uses the mod-owned T0 biomass route. Per-route rated draw ")
                .append("does not prove continuous factory capacity, night/restart behavior or routing. ")
                .append("Under-volting remains the existing throughput rule, not a permission.\n\n")
                .append("Flint has its first mandatory T1 consumer in the Pulverizer craft; ")
                .append("the canonical T0 optional-side-stock annotation is unchanged. Slag remains ")
                .append("discardable potential future use without an invented mandatory sink. ")
                .append("Vented CO is an emission, not stored generator fuel; optional capture/burning ")
                .append("needs actual handling evidence.\n\n")
                .append("Finite ore veins, mob farms, structures and the optional Factory World ")
                .append("are not canonical first-factory resource prerequisites. ")
                .append("The installed T0 development build and its worlds are untouched.\n");
        return text.toString();
    }

    private static InputEvidence evidence(SustainabilityAudit.Result t0, List<Route> selected,
                                          Route route, Resource resource, long amount, boolean catalyst) {
        if (resource.equals(ENERGY)) {
            return new InputEvidence(route.id(), resource, amount, catalyst,
                    "T1_THERMAL_FROM_T0_RENEWABLE_BIOMASS", List.of(T1Progression.POWER_ROUTE));
        }
        var capital = t0.annotations().resources().get(resource);
        if (capital != null && capital.resourceClass() == ResourceClass.MANUAL_CAPITAL) {
            return new InputEvidence(route.id(), resource, amount, catalyst,
                    "MANUAL_CAPITAL_NOT_RECURRING_ITEM_SUPPLY",
                    capital.canonicalRoute() == null ? List.of() : List.of(capital.canonicalRoute()));
        }
        if (t0.tiers().get(0).renewable().contains(resource)) {
            var metadata = t0.annotations().resources().get(resource);
            List<String> producers = metadata != null && metadata.canonicalRoute() != null
                    ? List.of(metadata.canonicalRoute()) : List.of();
            return new InputEvidence(route.id(), resource, amount, catalyst, "T0_AUDITED_RENEWABLE", producers);
        }
        if (resource.equals(RELAY)) {
            List<String> producers = List.of(T1Progression.ENTRY_ROUTE, T1Progression.REPRODUCTION_ROUTE).stream()
                    .filter(id -> require(t0.authorities(), id).retainedOutputs().containsKey(resource)).toList();
            return new InputEvidence(route.id(), resource, amount, catalyst,
                    producers.isEmpty() ? "UNRESOLVED" : "PHYSICAL_T0_SEED_OR_T1_REPRODUCTION_NOT_T0_RENEWABLE",
                    producers);
        }
        boolean casingSeed = route.id().equals("craft/assembler")
                && resource.equals(item(FabricationLogic.MACHINE_CASING));
        boolean recurringComponent = resource.equals(item(FabricationLogic.MACHINE_CASING))
                || resource.equals(item(FabricationLogic.FLUX_CELL));
        List<String> producers = selected.stream().filter(candidate -> candidate.retainedOutputs().keySet().stream()
                        .anyMatch(output -> sameSupply(t0.authorities(), output, resource)))
                .filter(candidate -> casingSeed ? candidate.id().equals("craft/machine_casing")
                        : !recurringComponent || candidate.automatable())
                .map(Route::id).sorted().toList();
        String status = producers.isEmpty() ? "UNRESOLVED"
                : recurringComponent
                        ? (casingSeed ? "MANUAL_FINITE_FIRST_ASSEMBLER_SEED"
                                : "IMPLEMENTED_AUTOMATIC_RECURRENT_INPUT_NOT_RENEWABILITY_PROOF")
                        : "LIVE_T1_INTERMEDIATE_NOT_RENEWABILITY_PROOF";
        return new InputEvidence(route.id(), resource, amount, catalyst,
                status, producers);
    }

    private static Map<String, Object> finiteDocument(SustainabilityAudit.FiniteResult finite) {
        List<Object> batches = new ArrayList<>();
        for (SustainabilityAudit.ExecutedBatch batch : finite.batches()) {
            batches.add(row("route", batch.route(), "consumed_physical", quantities(batch.consumed()),
                    "produced_retained", quantities(batch.produced()), "local_work_actions", batch.manualActions()));
        }
        return row("admitted", finite.admitted(), "completed", finite.completed(), "status", finite.status(),
                "predecessor", custodyDocument(finite.predecessor()), "final_state", custodyDocument(finite.state()),
                "batches", batches, "cumulative_reserves", finite.reserves(), "manual_work", finite.manualWork(),
                "exit_production", finite.exitProduction(), "findings", finite.findings(),
                "scope", "Source-bound in-process conditional proof; not loadable save data, player custody, "
                        + "renewable closure, continuous power/routing or whole-tier acceptance.");
    }

    private static Map<String, Object> custodyDocument(SustainabilityAudit.WitnessState state) {
        if (state == null) {
            return null;
        }
        Map<String, Object> parts = new TreeMap<>();
        state.installationParts().forEach((family, resources) -> parts.put(family, quantities(resources)));
        return row("tier", state.tier(), "free_stock", quantities(state.freeStock()),
                "retained_catalysts", quantities(state.retainedCatalysts()),
                "installed_machines", state.installedMachines().stream().map(Resource::key).toList(),
                "installation_parts", parts, "claimed_one_shot_routes", state.claimedRoutes(),
                "bootstrap_sources", state.bootstrapSources(), "guaranteed_deliveries", quantities(state.guaranteedDeliveries()),
                "player_deliveries", quantities(state.playerDeliveries()), "reward_deliveries", quantities(state.rewardDeliveries()),
                "cumulative_deliveries_not_stock", quantities(state.cumulativeDeliveries()),
                "peak_draw", quantities(state.peakDraw()), "consumed_manual_actions", state.consumedManualActions(),
                "required_manual_actions", state.requiredManualActions(), "operations", state.operations(),
                "cumulative_completed_batches", state.establishmentBatches());
    }

    private static boolean sameSupply(Snapshot snapshot, Resource output, Resource wanted) {
        if (output.equals(wanted)) {
            return true;
        }
        TagResolution target = snapshot.tags().get(wanted);
        if (target != null && target.complete() && target.members().contains(output)) {
            return true;
        }
        TagResolution produced = snapshot.tags().get(output);
        return produced != null && produced.complete() && produced.members().size() == 1
                && produced.members().contains(wanted);
    }

    private static Map<String, Object> routeDocument(Route route) {
        return row("id", route.id(), "authority", route.authority().name(), "source", route.source(),
                "machine_family", route.family(), "inputs", quantities(route.inputs()),
                "catalysts", quantities(route.catalysts()), "outputs", quantities(route.outputs()),
                "discharged", new TreeSet<>(route.discharged().stream().map(Resource::key).toList()),
                "duration_ticks", route.durationTicks(), "fu_per_tick", route.fuPerTick(),
                "nominal_fu_per_batch", Math.multiplyExact(route.durationTicks(), route.fuPerTick()),
                "conditions", route.conditions(), "one_shot", route.oneShot(),
                "automatable", route.automatable(), "mod_owned", route.modOwned());
    }

    private static List<Object> sideStreams(SustainabilityAudit.Result t0) {
        Resource flint = item("minecraft:flint");
        Resource slag = item("grindless:slag");
        var flintT0 = t0.annotations().resources().get(flint);
        var slagT0 = t0.annotations().resources().get(slag);
        return List.of(
                row("resource", flint.key(), "T0_classification", flintT0.sideStreamClass().name(),
                        "T1_classification", "BYPRODUCT_CURRENT", "first_mandatory_use_tier", FluxTier.F1.ordinal(),
                        "implemented_consumer", "craft/pulverizer", "canonical_source", flintT0.canonicalRoute(),
                        "scope", "Additive first T1 use; the canonical T0 annotation remains unchanged"),
                row("resource", slag.key(), "classification", slagT0.sideStreamClass().name(),
                        "discard_allowed", slagT0.handling().discardAllowed(), "mandatory_T1_feedstock", false,
                        "uncertainty", slagT0.uncertainty()),
                row("resource", new Resource(Kind.FLUID, "grindless:carbon_monoxide").key(),
                        "classification", "EMISSION", "actual_discharge", "ProcessRecipe vented output",
                        "mandatory_retained_feedstock", false,
                        "handling", "Do not count vented CO as stored fuel. Optional capture and Thermal burning "
                                + "require actual storage/routing; no containment or gas-handling capacity is certified."));
    }

    private static Route require(Snapshot snapshot, String id) {
        Route route = snapshot.routes().get(id);
        if (route == null) {
            throw new IllegalArgumentException("Missing live T1 authority: " + id);
        }
        return route;
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, id);
    }

    private static String describe(Map<Resource, Long> quantities, Set<Resource> discharged) {
        if (quantities.isEmpty()) {
            return "None";
        }
        return String.join("; ", quantities.entrySet().stream().map(entry -> entry.getValue() + " x `"
                + entry.getKey().key().replace("|", "\\|") + "`"
                + (discharged.contains(entry.getKey()) ? " (vented)" : "")).toList());
    }

    private static Map<String, Long> quantities(Map<Resource, Long> input) {
        Map<String, Long> result = new TreeMap<>();
        input.forEach((resource, amount) -> result.put(resource.key(), amount));
        return result;
    }

    private static Map<String, Object> row(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("A projection row needs key/value pairs");
        }
        Map<String, Object> result = new TreeMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}
