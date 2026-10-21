package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * A bounded T1 projection over the real conditional T0 audit and live recipe authorities.
 * This is not another fixed-point auditor, construction planner or renewable-closure certificate.
 */
public final class T1Sustainability {
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Resource RELAY = item(FabricationLogic.RELAY_MATRIX);

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

    public static List<Route> mandatoryRoutes(Snapshot snapshot) {
        Map<String, Route> routes = new TreeMap<>();
        Map<String, List<Route>> selected = selectedRoutes(snapshot);
        T1Progression.lines().stream().filter(T1Progression.Line::mandatory)
                .forEach(line -> selected.get(line.id()).forEach(route -> routes.put(route.id(), route)));
        return List.copyOf(routes.values());
    }

    /**
     * Classifies each direct consumed/catalytic edge. T1 producer links are deliberately not a
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

    /** Deterministic generated data; no source commit, timestamp or private execution path is embedded. */
    public static Map<String, Object> document(SustainabilityAudit.Result t0) {
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
                "schema_version", 1,
                "scope", "bounded_first_factory_authority_and_T0_boundary_projection",
                "authority_sha256", AuditReports.authorityHash(snapshot),
                "authority_snapshot_sha256", AuditReports.snapshotHash(snapshot),
                "quantity_authority", "Live ProcessGraph/catalogues and shipped JSON, never this generated document",
                "t1_accepted", false,
                "t1_renewable_closure_proven", false,
                "t1_finite_establishment_replayed", false,
                "global_optimum_claimed", false,
                "entry_route", T1Progression.ENTRY_ROUTE,
                "reproduction_route", T1Progression.REPRODUCTION_ROUTE,
                "exit_route", T1Progression.EXIT_ROUTE,
                "lines", lines,
                "mandatory_input_evidence", inputEvidence,
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
                        "scope", "Existing SustainabilityAudit/ConstructionReserve finite replay, not a T1 replay"),
                "side_streams", sideStreams(t0),
                "unproved", List.of(
                        "The existing auditor does not normalize untagged default-F1 outputs into the explicit F1 input identity.",
                        "Prior-tier finite custody has not been carried through a complete T1 ConstructionReserve replay.",
                        "Manual casing and Flux Cell crafts are not automatic recurring production routes.",
                        "Continuous factory power, nighttime recovery, routing throughput and captured-gas handling need separate evidence.",
                        "Native client, Survival play, actual crafting and longitudinal save compatibility are not proved by this export."));
        return Collections.unmodifiableMap(document);
    }

    public static String json(SustainabilityAudit.Result t0) {
        return JSON.toJson(document(t0)) + "\n";
    }

    /** Human-readable batch costs are generated from the same routes, never maintained by hand. */
    public static String markdown(SustainabilityAudit.Result t0) {
        document(t0);
        Snapshot snapshot = t0.authorities();
        Map<String, List<Route>> selected = selectedRoutes(snapshot);
        SeedAllocation seed = seedAllocation(snapshot);
        StringBuilder text = new StringBuilder("# Generated T1 first-factory production graph\n\n");
        text.append("Generated by `tools/t1/export`; do not edit quantities in this report.\n\n")
                .append("Authority SHA-256: `").append(AuditReports.authorityHash(snapshot)).append("`.\n\n")
                .append("This is live catalogue data plus the existing conditional T0 audit. ")
                .append("It is not T1 acceptance, a T1 construction replay, renewable closure, ")
                .append("an optimum, elapsed playtime or native/client evidence.\n\n")
                .append("The actual manual seed batch produces **").append(seed.batchOutput())
                .append("** controllers; mandatory first-machine crafts draw **").append(seed.constructionDraw())
                .append("**, leaving **").append(seed.remaining()).append("**. ")
                .append("These are source costs, not a promised world inventory.\n\n")
                .append("## Exact batches\n\n")
                .append("Each row is one batch at its catalogue conditions. Alternative recipes ")
                .append("are not simultaneous machine loads. Generator FU output is in the output ")
                .append("column; the demand column is consumption only.\n\n")
                .append("| Line | Role | Live authority | Machine | Consumed inputs | Retained catalysts | Outputs | Ticks | FU/t | Demand FU/batch |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- | ---: | ---: | ---: |\n");
        for (T1Progression.Line line : T1Progression.lines()) {
            for (Route route : selected.get(line.id())) {
                text.append("| ").append(line.id()).append(" | ").append(line.requirement())
                        .append(" | `").append(route.id()).append("` | ")
                        .append(route.family() == null ? "Manual craft" : route.family())
                        .append(" | ").append(describe(route.inputs(), Set.of()))
                        .append(" | ").append(describe(route.catalysts(), Set.of()))
                        .append(" | ").append(describe(route.outputs(), route.discharged()))
                        .append(" | ").append(route.durationTicks()).append(" | ").append(route.fuPerTick())
                        .append(" | ").append(Math.multiplyExact(route.durationTicks(), route.fuPerTick()))
                        .append(" |\n");
            }
        }
        text.append("\n## Supply and proof boundaries\n\n")
                .append("The JSON records every mandatory consumed/catalytic edge separately. ")
                .append("`T0_AUDITED_RENEWABLE` means the existing conditional audit proves that ")
                .append("boundary under its stated environment and finite-establishment assumptions. ")
                .append("A live T1 producer link is not a recursive net-supply certificate.\n\n")
                .append("The F2 recipe consumes explicitly F1 and produces explicitly F2. ")
                .append("The runtime NBT mechanism reads an untagged Relay as F1, but the existing ")
                .append("audit resource identities are not silently normalized. No complete T1 ")
                .append("custody/construction replay is claimed. Casing and Flux Cell crafts remain ")
                .append("manual, not automatic recurring routes.\n\n")
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
        if (t0.tiers().get(0).renewable().contains(resource)) {
            var metadata = t0.annotations().resources().get(resource);
            List<String> producers = metadata != null && metadata.canonicalRoute() != null
                    ? List.of(metadata.canonicalRoute()) : List.of();
            return new InputEvidence(route.id(), resource, amount, catalyst, "T0_AUDITED_RENEWABLE", producers);
        }
        if (resource.equals(RELAY)) {
            return new InputEvidence(route.id(), resource, amount, catalyst,
                    "PHYSICAL_T0_SEED_OR_T1_REPRODUCTION_NOT_T0_RENEWABLE",
                    List.of(T1Progression.ENTRY_ROUTE, T1Progression.REPRODUCTION_ROUTE));
        }
        if (resource.kind() == Kind.ITEM && resource.id().equals(RELAY.id())
                && resource.rating() == MatrixArchitecture.RELAY.firstFrontier().ordinal()) {
            return new InputEvidence(route.id(), resource, amount, catalyst,
                    "PHYSICAL_DEFAULT_F1_BINDING_NOT_NORMALIZED_BY_AUDITOR",
                    List.of(T1Progression.REPRODUCTION_ROUTE));
        }
        List<String> producers = selected.stream().filter(candidate -> candidate.retainedOutputs().keySet().stream()
                        .anyMatch(output -> sameSupply(t0.authorities(), output, resource)))
                .map(Route::id).sorted().toList();
        return new InputEvidence(route.id(), resource, amount, catalyst,
                producers.isEmpty() ? "UNRESOLVED" : "LIVE_T1_INTERMEDIATE_NOT_RENEWABILITY_PROOF", producers);
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
