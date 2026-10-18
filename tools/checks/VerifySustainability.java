package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Hard synthetic rule/adapter assertions plus the explicitly report-only, unannotated S1 audit.
 * Fixtures are invented test data, never additions to the live gameplay catalogue.
 */
public final class VerifySustainability {
    private static final Resource SAFE = item("safe_feed");
    private static final Resource RAW = item("raw");
    private static final Resource PRODUCT = item("product");
    private static final Resource SIDE = item("side");
    private static final Resource TOOL = item("tool");
    private static final Resource MACHINE = item("machine");
    private static final Resource WATER = new Resource(Kind.FLUID, "fixture:water");
    private static final Resource STEAM = new Resource(Kind.FLUID, "fixture:steam");
    private static final Map<String, int[]> GROUPS = new LinkedHashMap<>();
    private static String group;
    private static int passed;
    private static int failures;

    private VerifySustainability() {
    }

    /** Runs through the existing automatically discovered check convention. */
    public static void main(String[] args) {
        boolean regressionsOnly = args.length > 0 && args[0].equals("--regressions-only");
        boolean algorithmOnly = regressionsOnly || args.length > 0 && args[0].equals("--algorithm-only");
        if (args.length == 0 || !args[0].equals("--live-only")) {
            try {
                if (!regressionsOnly) {
                    modelContracts();
                    fixedPoints();
                    sourceRules();
                    tagsAndJustifications();
                    constructionReserve();
                    recycling();
                    resourceCompatibility();
                    sideStreamCompatibility();
                    debtRatchet();
                    deterministicProjection();
                }
                algorithmRegressions();
            } catch (Exception failure) {
                fail("Unexpected fixture exception: " + failure);
                failure.printStackTrace(System.out);
            }
        }
        if (!algorithmOnly) {
            try {
                live();
            } catch (Exception failure) {
                fail("Live adapter/report execution failed (not an audit finding): " + failure);
                failure.printStackTrace(System.out);
            }
        }
        GROUPS.forEach((name, counts) -> System.out.println(
                "FIXTURE " + name + ": " + counts[0] + " passed, " + counts[1] + " failed"));
        System.out.println(passed + " passed, " + failures + " failed");
        System.out.println(failures == 0
                ? "ALL SUSTAINABILITY FIXTURE/ADAPTER CHECKS PASSED; LIVE AUDIT REMAINS REPORT-ONLY"
                : failures + " SUSTAINABILITY FIXTURE/ADAPTER CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void algorithmRegressions() {
        for (Runnable fixture : List.<Runnable>of(VerifySustainability::protectedCirculation,
                VerifySustainability::forkedStartup, VerifySustainability::optionalOnlyProduction,
                VerifySustainability::sharedHandlingBudget, VerifySustainability::loopEnergy,
                VerifySustainability::normalizedLoops, VerifySustainability::sequentialReturnedSeed,
                VerifySustainability::convergedMakeUp, VerifySustainability::formedConstruction,
                VerifySustainability::productiveZeroCarrier, VerifySustainability::environmentWithoutTier,
                VerifySustainability::conditionalEntry, VerifySustainability::manualExitGoal,
                VerifySustainability::localWorkNativeProduction, VerifySustainability::manualEstablishment,
                VerifySustainability::unitPotentialCertificates, VerifySustainability::conditionalGateScope,
                VerifySustainability::jointConditionalObligations, VerifySustainability::storedNativeFeed,
                VerifySustainability::singletonOutputCustody)) {
            try {
                fixture.run();
            } catch (Exception failure) {
                fail("Algorithm counterexample must return a verdict, not throw: " + failure);
                failure.printStackTrace(System.out);
            }
        }
    }

    private static void storedNativeFeed() {
        group("R21 exact stored native feed precedes unnecessary full circulation");
        Snapshot live;
        try {
            live = AuthorityAdapters.live(Path.of(""));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect native fuel batches", failure);
        }
        Resource biomass = new Resource(Kind.ITEM, SourceLogic.BIOMASS);
        Resource terminal = item("large_native_consumer");
        Fixture fixture = productiveFixture(live, 0, true).seed(TOOL, 1).seed(biomass, 100).seed(terminal, 1);
        fixture.workProfile(Map.of(), 9, Set.of("GEOLOGICAL_COLLECTOR", "BIOCULTIVATOR"));
        fixture.add(route("large_native_draw", Map.of(SAFE, 1L, ENERGY, 16_000L), Map.of(),
                Map.of(PRODUCT, 1L), "NATIVE_CONSUMER"));
        fixture.machines.put("NATIVE_CONSUMER", new Machine("NATIVE_CONSUMER", terminal, 0));
        fixture.meta(PRODUCT, renewable("large_native_draw")).root(PRODUCT);
        var result = fixture.audit();
        eq("the exact large batch is completed with real native fuel", 1L, result.tiers().get(0)
                .establishmentBatches().getOrDefault("large_native_draw", 0L));
        yes("ready canonical fuel does not unnecessarily reproduce a hundred full material cycles",
                result.tiers().get(0).establishmentBatches().getOrDefault("source/geological_aggregate", 0L) < 10);
        yes("the native fuel was really spent through completed authority batches",
                result.tiers().get(0).establishmentBatches().getOrDefault("fuel/biomass_dynamo/biomass", 0L) >= 10);
        no("stored native fuel does not become portable manual work", hasRule(result, "entry_work_insufficient"));
        eq("choosing a ready batch does not hide an unresolved finite reserve", List.of(),
                result.findings().findings().stream().filter(finding ->
                        finding.rule().equals("construction_reserve_unknown") && finding.detail().equals("T0"))
                        .map(finding -> finding.fingerprint() + ": " + finding.message()).toList());
    }

    private static void singletonOutputCustody() {
        group("R22 exact singleton tag output is one physical stock, not an alias duplicate");
        Resource tag = new Resource(Kind.TAG, "fixture:resolved_output");
        Resource member = item("resolved_output_member");
        for (long quantity : List.of(1L, 2L)) {
            Fixture fixture = new Fixture().seed(RAW, quantity);
            fixture.builder.tag(tag, new TagResolution(Set.of(member), true, Set.of(), List.of("fixture:singleton")));
            fixture.add(route("tag_batch", Map.of(RAW, 1L), Map.of(), Map.of(tag, 1L), null));
            fixture.add(route("construct_from_member", Map.of(member, 1L), Map.of(), Map.of(MACHINE, 1L), null));
            fixture.add(route("finish_from_tag", Map.of(tag, 1L), Map.of(), Map.of(PRODUCT, 1L), "WORK"));
            fixture.machines.put("WORK", new Machine("WORK", MACHINE, 0));
            fixture.meta(member, metadata(ResourceClass.BOOTSTRAP, null, "tag_batch",
                    List.of(), null, null, null, null));
            fixture.entryProfile = new EntryProfile("fixture:tag_custody", Map.of(), 0, null,
                    Set.of("Finite synthetic authority batches only"), "fixture:tag_custody");
            fixture.root(PRODUCT);
            var result = fixture.audit();
            eq("each accepted tagged output is spent once for construction or the actual product", quantity,
                    result.tiers().get(0).establishmentBatches().getOrDefault("tag_batch", 0L));
            eq("a concrete member and its tag share custody rather than duplicating one output", quantity - 1,
                    result.tiers().get(0).establishmentBatches().getOrDefault("finish_from_tag", 0L));
            eq("only the sufficient finite batch bill establishes both obligations", quantity == 2,
                    result.tiers().get(0).reserves().stream().allMatch(reserve ->
                            reserve.margin() >= 0 && reserve.status().equals("conditional_established")));
        }
        Fixture ambiguous = new Fixture().seed(RAW, 1);
        ambiguous.builder.tag(tag, new TagResolution(Set.of(member, item("alternative_member")), true,
                Set.of(), List.of("fixture:multi_member")));
        ambiguous.add(route("tag_batch", Map.of(RAW, 1L), Map.of(), Map.of(tag, 1L), null));
        ambiguous.meta(member, metadata(ResourceClass.BOOTSTRAP, null, "tag_batch",
                List.of(), null, null, null, null)).root(member);
        no("a complete multi-member tag never chooses a fictional exact output member",
                available(ambiguous.audit(), member));
    }

    private static void conditionalGateScope() {
        group("R19 hard T0 gate follows global proof scope and unresolved gain warnings");
        Fixture fixture = gateFixture();
        var result = fixture.audit();
        yes("a real finite manual goal with renewable supplies passes its conditional model gate",
                T0ConditionalGate.evaluate(result).pass());
        no("that scoped pass is not a global sustainability certificate", result.sustainabilityPass());
        JsonObject report = JsonParser.parseString(AuditReports.json(result, "fixture:conditional-gate", true))
                .getAsJsonObject();
        yes("the additive hard-gate result is exported separately",
                report.getAsJsonObject("active_t0_conditional_gate").get("pass").getAsBoolean());
        no("the additive hard gate never exports Tier acceptance",
                report.getAsJsonObject("active_t0_conditional_gate").get("tier_acceptance").getAsBoolean());
        eq("gate exports are deterministic", AuditReports.json(result, "fixture:conditional-gate", true),
                AuditReports.json(fixture.audit(), "fixture:conditional-gate", true));

        fixture.resources.remove(SAFE);
        result = fixture.audit();
        yes("a global classification gap on a real dependency is blocking",
                gateBlocks(result, "classification_unknown:" + SAFE.key()));
        fixture = gateFixture();
        fixture.builder.resource(SIDE);
        result = fixture.audit();
        yes("an unrelated unaccepted inventory resource stays visible without becoming a T0 root",
                hasRule(result, "classification_unknown") && T0ConditionalGate.evaluate(result).pass());
        fixture.builder.finding(new AuditFinding("unmapped_authority_unknown", "unbound_source", "",
                AuditFinding.Severity.UNKNOWN, "Its scope has not been established.", List.of()));
        no("a global unknown with no demonstrated scope fails closed", T0ConditionalGate.evaluate(fixture.audit()).pass());
        fixture = gateFixture();
        fixture.builder.finding(new AuditFinding("integrity_failure", "outside_inventory", "",
                AuditFinding.Severity.ERROR, "A global authority integrity defect.", List.of()));
        yes("global integrity errors cannot be filtered away by resource scope",
                gateBlocks(fixture.audit(), "integrity_failure:outside_inventory"));
        fixture = gateFixture();
        fixture.builder.finding(new AuditFinding("local_unknown", "outside_inventory", "T0",
                AuditFinding.Severity.UNKNOWN, "Explicit unresolved T0 evidence.", List.of()));
        yes("an explicit T0 unknown blocks even without a recognized entity",
                gateBlocks(fixture.audit(), "local_unknown:outside_inventory:T0"));

        fixture = gateFixture();
        fixture.environment("side_supply", Map.of(SIDE, 1L), null).meta(SIDE, renewable("side_supply"));
        fixture.builder.finding(new AuditFinding("authority_unknown", "side_supply", "",
                AuditFinding.Severity.UNKNOWN, "A required source binding is missing.", List.of()));
        result = fixture.audit();
        no("an R0 output is not automatically made a progression root", result.tiers().get(0).mandatory().contains(SIDE));
        yes("its globally unresolved canonical source still blocks the R0 gate",
                gateBlocks(result, "authority_unknown:side_supply"));
        fixture = gateFixture();
        fixture.environment("side_supply", Map.of(SIDE, 1L), "UNBOUND_FAMILY").meta(SIDE, renewable("side_supply"));
        yes("a missing canonical family binding is relevant even before the route can unlock",
                gateBlocks(fixture.audit(), "machine_binding_unknown:UNBOUND_FAMILY"));
        fixture = gateFixture();
        Resource choice = new Resource(Kind.TAG, "fixture:choice");
        fixture.builder.tag(choice, new TagResolution(Set.of(SAFE), false, Set.of("unbound_member"), List.of()));
        fixture.meta(choice, renewable("base"));
        yes("an incomplete tag used by an R0 claim is not hidden by an available concrete member",
                gateBlocks(fixture.audit(), "unresolved_tag:" + choice.key()));

        fixture = gateFixture();
        Resource exactOutput = item("active_tag_output");
        Resource outputTag = new Resource(Kind.TAG, "fixture:active_singleton_output");
        fixture.builder.tag(outputTag, new TagResolution(Set.of(exactOutput), true, Set.of(),
                List.of("fixture:active_singleton")));
        fixture.add(route("active_co_product", Map.of(SAFE, 1L), Map.of(),
                Map.of(SIDE, 1L, outputTag, 1L), null)).meta(SIDE, renewable("active_co_product"));
        result = fixture.audit();
        yes("the concrete singleton output belongs to proof scope even without a consumer",
                T0ConditionalGate.evaluate(result).scope().resources().contains(exactOutput));
        yes("tag metadata cannot hide the unknown classification of its actual produced member",
                gateBlocks(result, "classification_unknown:" + exactOutput.key()));

        fixture = optionalSideGate();
        result = fixture.audit();
        yes("nonmandatory output first use may stay unknown without a T0 consumer",
                hasRule(result, "first_use_unknown") && T0ConditionalGate.evaluate(result).pass());
        ResourceMetadata original = fixture.resources.get(SIDE);
        fixture.meta(SIDE, new ResourceMetadata(original.resourceClass(), original.sideStreamClass(),
                original.firstUseTier(), original.firstRenewableTier(), original.canonicalRoute(),
                original.futureConsumers(), null, null, null, null));
        yes("the first-use exception never exempts optional side handling",
                gateBlocks(fixture.audit(), "handling_unknown:" + SIDE.key()));
        fixture = optionalSideGate();
        fixture.add(route("accepted_consumer", Map.of(SIDE, 1L), Map.of(), Map.of(RAW, 1L), null));
        yes("an accepted T0 consumer prevents the output-only first-use exception",
                gateBlocks(fixture.audit(), "first_use_unknown:" + SIDE.key()));

        fixture = gateFixture();
        fixture.add(route("gain_a", Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 2L), null));
        fixture.add(route("gain_b", Map.of(SIDE, 2L), Map.of(), Map.of(SAFE, 1L), null));
        fixture.add(route("gain_c", Map.of(SAFE, 1L), Map.of(), Map.of(RAW, 2L), null));
        result = fixture.audit();
        eq("the relevant gain counterexample has warnings, not invented integrity errors", 0,
                result.findings().activeErrors());
        no("a relevant resource_positive_cycle warning blocks despite its warning severity",
                T0ConditionalGate.evaluate(result).pass());
        yes("the representative resource is outside the initial active proof",
                !T0ConditionalGate.evaluate(result).scope().resources().contains(RAW));
        T0ConditionalGate.Gate gainGate = T0ConditionalGate.evaluate(result);
        yes("whole-component relevance includes a warning whose representative is not a T0 root",
                result.findings().findings().stream().filter(finding ->
                        finding.rule().equals("resource_positive_cycle") && finding.subject().equals(RAW.key()))
                        .anyMatch(finding -> gainGate.decisions().stream().anyMatch(decision ->
                                decision.fingerprint().equals(finding.fingerprint()) && decision.blocking())));
        fixture = gateFixture();
        fixture.add(route("prototype_forward", Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 2L), null));
        fixture.add(route("prototype_reverse", Map.of(SIDE, 1L), Map.of(), Map.of(RAW, 1L), null));
        fixture.policies.put("prototype_forward", new RoutePolicy((Integer) null, false, Set.of()));
        fixture.policies.put("prototype_reverse", new RoutePolicy((Integer) null, false, Set.of()));
        result = fixture.audit();
        yes("a proven disjoint prototype gain remains visible", hasRule(result, "resource_positive_cycle"));
        yes("a demonstrably disjoint unaccepted component does not certify or block T0",
                T0ConditionalGate.evaluate(result).pass());
        fixture.builder.finding(new AuditFinding("informational_source_note", SAFE.key(), "",
                AuditFinding.Severity.WARNING, "A non-gain informational note.", List.of()));
        yes("informational warnings are not silently converted into errors",
                T0ConditionalGate.evaluate(fixture.audit()).pass());
        fixture = gateFixture();
        fixture.builder.finding(new AuditFinding("cycle_balance_unproven", "unmapped_component", "",
                AuditFinding.Severity.UNKNOWN, "No complete component identity.", List.of()));
        yes("an unmapped cycle component fails closed", gateBlocks(fixture.audit(),
                "cycle_balance_unproven:unmapped_component"));

        fixture = gateFixture();
        fixture.builder.finding(new AuditFinding("generator_fuel_unknown", "furnace_fuel_table", "",
                AuditFinding.Severity.UNKNOWN, "The dynamic furnace-fuel table is not bound.", List.of()));
        yes("explicit graph batches without a dynamic furnace dependency do not need that external table",
                T0ConditionalGate.evaluate(fixture.audit()).pass());
        fixture.add(new Route("vanilla/dynamic_smelting", Authority.VANILLA_TRANSFORMATION,
                "fixture:runtime_furnace", null, Map.of(SAFE, 1L), Map.of(), Map.of(SIDE, 1L), Set.of(),
                200, 0, Map.of("crafting_type", "minecraft:smelting"), Set.of(), false, false, false));
        fixture.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, null, "vanilla/dynamic_smelting",
                List.of(), null, null, null, null)).root(SIDE);
        yes("the same furnace-table unknown blocks when a selected route really depends on it",
                gateBlocks(fixture.audit(), "generator_fuel_unknown:furnace_fuel_table"));
        fixture = gateFixture();
        fixture.entryProfile = null;
        no("unprofiled availability cannot be promoted to a finite conditional certificate",
                T0ConditionalGate.evaluate(fixture.audit()).pass());
    }

    private static Fixture gateFixture() {
        Fixture fixture = new Fixture();
        fixture.add(new Route("manual_exit", Authority.CRAFTING_JSON, "fixture:manual-exit", null,
                Map.of(SAFE, 1L), Map.of(), Map.of(PRODUCT, 5L), Set.of(), 0, 0, Map.of(),
                Set.of(), false, true, false));
        fixture.meta(PRODUCT, metadata(ResourceClass.BOOTSTRAP, null, "manual_exit",
                List.of(), null, null, null, null));
        fixture.builder.objective(new Objective("exit", PRODUCT, 5, Set.of(), null, "fixture:exit"));
        fixture.requiredQuests.add("exit");
        fixture.exitObjective = PRODUCT;
        fixture.entryProfile = new EntryProfile("fixture:gate", Map.of(), 0, null,
                Set.of("Conditional fixture authority facts, not live gameplay."), "fixture:gate");
        return fixture;
    }

    private static Fixture optionalSideGate() {
        Fixture fixture = gateFixture().environment("side_supply", Map.of(SIDE, 1L), null);
        return fixture.meta(SIDE, new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.BYPRODUCT_OPTIONAL,
                null, 0, "side_supply", List.of(), new Handling(Action.STORE, true, false, null, null),
                null, null, null));
    }

    private static boolean gateBlocks(SustainabilityAudit.Result result, String fingerprint) {
        return T0ConditionalGate.evaluate(result).decisions().stream()
                .anyMatch(decision -> decision.fingerprint().equals(fingerprint) && decision.blocking());
    }

    private static void jointConditionalObligations() {
        group("R20 independent seed proofs never replace shared finite establishment custody");
        Fixture fixture = jointLoopFixture(2);
        var result = fixture.audit();
        Resource second = item("second_supply");
        yes("both independently renewable producers also establish jointly", T0ConditionalGate.evaluate(result).pass());
        no("the second R0 production claim is not made a mandatory progression root",
                result.tiers().get(0).mandatory().contains(second));
        eq("both installations consume the same real finite entry custody", 2L,
                result.tiers().get(0).reserves().stream().filter(reserve -> reserve.resource().equals(RAW))
                        .findFirst().orElseThrow().required());
        eq("the nonmandatory R0 producer is actually installed once in the joint replay", 1L,
                result.tiers().get(0).establishmentBatches().get("second_controller"));
        yes("the nonmandatory R0 loop also executes in that same finite replay",
                result.tiers().get(0).establishmentBatches().getOrDefault("second_loop", 0L) > 0);
        fixture.entryProfile = fixture.entryProfile.withBill(Map.of(RAW, 1L), 0);
        result = fixture.audit();
        yes("the deliberately insufficient profile still satisfies the two independent qualitative loop proofs",
                renewable(result, SIDE) && renewable(result, second));
        yes("shared custody exposes their actual joint deficit", hasRule(result, "entry_profile_insufficient"));
        no("two worlds cannot spend the same finite construction resource to pass the gate",
                T0ConditionalGate.evaluate(result).pass());
        fixture.entryProfile = fixture.entryProfile.withBill(Map.of(), 0);
        var derived = SustainabilityAudit.deriveEntryProfile(fixture.builder.build(), fixture.annotations(),
                DebtManifest.empty(), 0);
        eq("the derived bill includes the joint replay, not only the one-unit independent maximum",
                Map.of(RAW, 2L), derived.profile().playerProvided());
        yes("that sufficient joint bill is separately replayed with finite shared custody",
                T0ConditionalGate.evaluate(derived.replay()).pass());
    }

    private static Fixture jointLoopFixture(long entry) {
        Fixture fixture = gateFixture().terminal("manual_raw", RAW, TerminalKind.MANUAL_WORLD, false, true)
                .meta(RAW, metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null));
        Resource firstCarrier = item("first_carrier");
        Resource secondCarrier = item("second_carrier");
        Resource firstMachine = item("first_machine");
        Resource secondMachine = item("second_machine");
        Resource second = item("second_supply");
        fixture.seed(firstCarrier, 1).seed(secondCarrier, 1);
        fixture.add(route("first_controller", Map.of(RAW, 1L), Map.of(), Map.of(firstMachine, 1L), null));
        fixture.add(route("second_controller", Map.of(RAW, 1L), Map.of(), Map.of(secondMachine, 1L), null));
        fixture.machines.put("FIRST", new Machine("FIRST", firstMachine, 0));
        fixture.machines.put("SECOND", new Machine("SECOND", secondMachine, 0));
        fixture.environment("first_loop", Map.of(firstCarrier, 1L), Map.of(),
                Map.of(firstCarrier, 1L, SIDE, 1L), "FIRST");
        fixture.environment("second_loop", Map.of(secondCarrier, 1L), Map.of(),
                Map.of(secondCarrier, 1L, second, 1L), "SECOND");
        fixture.meta(firstCarrier, metadata(ResourceClass.CLOSED_LOOP, null, "first_loop",
                List.of(), null, null, null, null));
        fixture.meta(secondCarrier, metadata(ResourceClass.CLOSED_LOOP, null, "second_loop",
                List.of(), null, null, null, null));
        fixture.meta(SIDE, renewable("first_loop")).meta(second, renewable("second_loop")).root(SIDE);
        fixture.loops.put("first", new ClosedLoop("first", firstCarrier, Set.of("first_loop"), null));
        fixture.loops.put("second", new ClosedLoop("second", secondCarrier, Set.of("second_loop"), null));
        fixture.entryProfile = fixture.entryProfile.withBill(Map.of(RAW, entry), 0);
        return fixture;
    }

    private static void unitPotentialCertificates() {
        group("R18 sufficient positive unit potential covers the entire real component");
        Snapshot live;
        try {
            live = AuthorityAdapters.live(Path.of(""));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect actual ceramic recycling batches", failure);
        }
        Resource grog = new Resource(Kind.ITEM, BootstrapProcesses.GROG);
        var component = SustainabilityAudit.cycleComponents(live).stream()
                .filter(candidate -> candidate.resources().contains(grog)).findFirst().orElseThrow();
        eq("the certificate covers all seven live ceramic component members",
                Set.of("bootstrap/brick_grog", "bootstrap/bricks", "bootstrap/grog_body",
                        "bootstrap/insulator_grog", "bootstrap/insulators", "bootstrap/sand_body",
                        "bootstrap/unfired_recovery"),
                new TreeSet<>(component.routes().stream().map(Route::id).toList()));
        yes("actual ceramic batches admit the sufficient nonincreasing unit potential",
                SustainabilityAudit.unitPotentialConservative(component.routes()));
        Fixture ceramics = new Fixture();
        component.routes().forEach(ceramics::add);
        no("a complete real ceramic certificate removes only its bounded-balance unknown",
                hasRule(ceramics.audit(), "cycle_balance_unproven"));
        no("the certificate does not itself produce renewable ceramics", renewable(ceramics.audit(), grog));

        Fixture gain = new Fixture();
        gain.add(route("gain_a", Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 2L), null));
        gain.add(route("gain_b", Map.of(SIDE, 2L), Map.of(), Map.of(PRODUCT, 1L), null));
        gain.add(route("gain_c", Map.of(PRODUCT, 1L), Map.of(), Map.of(RAW, 2L), null));
        yes("the exact 1:2:1 positive cycle retains its warning", hasRule(gain.audit(), "resource_positive_cycle"));
        gain.add(route("losing_alternative", Map.of(RAW, 100L), Map.of(), Map.of(SIDE, 1L), null));
        no("a losing alternative cannot cancel another member's gain certificate failure",
                SustainabilityAudit.unitPotentialConservative(new ArrayList<>(gain.builder.build().routes().values())));
        yes("an uncertified nonunique component retains its bounded-search unknown",
                hasRule(gain.audit(), "cycle_balance_unproven"));
        Fixture subcycle = new Fixture();
        subcycle.add(route("pair_forward", Map.of(RAW, 2L), Map.of(), Map.of(SIDE, 3L), null));
        subcycle.add(route("pair_reverse", Map.of(SIDE, 2L), Map.of(), Map.of(RAW, 2L), null));
        subcycle.add(route("losing_alternative", Map.of(RAW, 100L), Map.of(), Map.of(SIDE, 1L), null));
        yes("the detected positive subcycle remains a warning despite its losing alternative",
                hasRule(subcycle.audit(), "resource_positive_cycle"));

        Fixture energy = new Fixture();
        energy.add(route("energy_forward", Map.of(WATER, 1L), Map.of(), Map.of(STEAM, 1L, ENERGY, 2L), null));
        energy.add(route("energy_reverse", Map.of(STEAM, 1L, ENERGY, 1L), Map.of(), Map.of(WATER, 1L), null));
        no("material conservation alone cannot certify a pure energy gain",
                SustainabilityAudit.unitPotentialConservative(new ArrayList<>(energy.builder.build().routes().values())));
        yes("a pure energy gain remains review-visible", hasRule(energy.audit(), "resource_positive_cycle"));

        Route retained = route("retained_catalyst", Map.of(RAW, 1L), Map.of(SIDE, Long.MAX_VALUE),
                Map.of(RAW, 2L), null);
        no("a retained catalyst is never counted as consumed potential",
                SustainabilityAudit.unitPotentialConservative(List.of(retained)));
        yes("a retained catalyst cannot hide a genuine material gain",
                hasRule(new Fixture().add(retained).audit(), "resource_positive_cycle"));
        Route largeLoss = route("large_loss", Map.of(RAW, Long.MAX_VALUE, SIDE, Long.MAX_VALUE),
                Map.of(), Map.of(PRODUCT, Long.MAX_VALUE), null);
        yes("full-batch unit sums use exact arithmetic beyond long capacity",
                SustainabilityAudit.unitPotentialConservative(List.of(largeLoss)));
        Route largeGain = route("large_gain", Map.of(RAW, Long.MAX_VALUE),
                Map.of(), Map.of(SIDE, Long.MAX_VALUE, PRODUCT, 1L), null);
        no("overflow cannot hide a one-unit positive material delta",
                SustainabilityAudit.unitPotentialConservative(List.of(largeGain)));
    }

    private static void manualEstablishment() {
        group("R17 finite manual establishment does not replace canonical renewable production");
        Fixture first = firstBench(Authority.CRAFTING_JSON, false, false);
        var result = first.audit();
        yes("the first Bench can be built manually from finite real custody", available(result, MACHINE));
        yes("the established Bench supports its actual automatic renewable projection", renewable(result, MACHINE));
        yes("the established Bench supports subsequent automatic products", renewable(result, PRODUCT));
        eq("the finite witness attributes the first Bench to its real manual craft", 1L,
                result.tiers().get(0).establishmentBatches().get("manual_bench"));
        eq("the first Bench is not attributed to its unexecuted automatic projection", 0L,
                result.tiers().get(0).establishmentBatches().getOrDefault("auto_bench", 0L));
        no("an abandoned self-construction attempt is not a remaining reserve deficit",
                hasRule(result, "construction_reserve_unknown"));
        eq("manual construction actually consumes its finite input", 1L,
                result.tiers().get(0).reserves().stream().filter(reserve -> reserve.resource().equals(RAW))
                        .findFirst().orElseThrow().required());
        try {
            result.tiers().get(0).establishmentBatches().put("fabricated_execution", 1L);
            fail("completed-batch provenance must be immutable");
        } catch (UnsupportedOperationException expected) {
            yes("completed-batch provenance is immutable", true);
        }
        JsonObject report = JsonParser.parseString(AuditReports.json(result, "fixture:first-bench", true))
                .getAsJsonObject();
        eq("public provenance exports actual completed manual batches separately", 1L,
                report.getAsJsonArray("tiers").get(0).getAsJsonObject()
                        .getAsJsonObject("establishment_batches").get("manual_bench").getAsLong());
        for (Authority authority : List.of(Authority.BOOTSTRAP_RECIPES, Authority.T1_RECIPES,
                Authority.VANILLA_TRANSFORMATION)) {
            result = firstBench(authority, false, false).audit();
            eq("verified manual authority supports the same bounded craft: " + authority, 1L,
                    result.tiers().get(0).establishmentBatches().get("manual_bench"));
        }
        first = firstBench(Authority.CRAFTING_JSON, false, false);
        first.entryProfile = first.entryProfile.withBill(Map.of(), 0);
        result = first.audit();
        yes("no initial budget means no fictitious first Bench", hasRule(result, "entry_profile_insufficient"));
        eq("an unpaid manual craft never appears as executed", 0L,
                result.tiers().get(0).establishmentBatches().getOrDefault("manual_bench", 0L));
        for (String invalid : List.of("missing", "optional", "locked", "one_shot", "automatic", "not_crafting")) {
            first = firstBench(invalid.equals("not_crafting") ? Authority.GENERATOR_FUEL : Authority.CRAFTING_JSON,
                    invalid.equals("one_shot"), invalid.equals("automatic"));
            switch (invalid) {
                case "missing" -> first.policies.remove("manual_bench");
                case "optional" -> first.policy("manual_bench", 0, true);
                case "locked" -> first.policy("manual_bench", 1, false);
                default -> { }
            }
            result = first.audit();
            eq("an ineligible fallback cannot manufacture the first Bench: " + invalid, 0L,
                    result.tiers().get(0).establishmentBatches().getOrDefault("manual_bench", 0L));
            yes("an ineligible fallback leaves its real establishment gap visible: " + invalid,
                    hasRule(result, "construction_reserve_unknown"));
        }
        first = firstBench(Authority.CRAFTING_JSON, false, false);
        first.entryProfile = first.entryProfile.withBill(Map.of(), 0);
        var derived = SustainabilityAudit.deriveEntryProfile(first.builder.build(), first.annotations(),
                first.debt, 0);
        eq("the sufficient bootstrap bill is derived from actual craft batches", Map.of(RAW, 1L),
                derived.profile().playerProvided());
        eq("the fresh finite replay, not the derivation, contains one completed manual Bench", 1L,
                derived.replay().tiers().get(0).establishmentBatches().get("manual_bench"));
        no("the derived finite replay really funds establishment", hasRule(derived.replay(), "entry_profile_insufficient"));
        eq("completed finite replay exports are deterministic",
                AuditReports.json(derived.replay(), "fixture:first-bench", true),
                AuditReports.json(SustainabilityAudit.deriveEntryProfile(first.builder.build(), first.annotations(),
                        first.debt, 0).replay(), "fixture:first-bench", true));
        first = firstBench(Authority.CRAFTING_JSON, false, false);
        first.add(new Route("quest/shortcut", Authority.QUEST_REWARD, "fixture:optional-reward", null,
                Map.of(), Map.of(SAFE, 1L), Map.of(RAW, 1L), Set.of(), 0, 0, Map.of(), Set.of(),
                true, true, false)).policy("quest/shortcut", 0, true);
        first.builder.objective(new Objective("shortcut", SAFE, 1, Set.of(), "quest/shortcut", "fixture:shortcut"));
        result = first.audit();
        eq("an unselected optional claim is not silently added to native establishment", 0L,
                result.tiers().get(0).establishmentBatches().getOrDefault("quest/shortcut", 0L));
        eq("an unselected optional claim does not fund the first Bench", 0L,
                result.tiers().get(0).reserves().stream().filter(row -> row.resource().equals(RAW))
                        .findFirst().orElseThrow().claimedRewards());
        first.requiredQuests.add("shortcut");
        result = first.audit();
        eq("an explicitly selected optional objective can still execute its real conditional claim", 1L,
                result.tiers().get(0).establishmentBatches().get("quest/shortcut"));

        first = firstBench(Authority.CRAFTING_JSON, false, false).seed(PRODUCT, 5);
        Resource unbuilt = item("unbuilt_goal_machine");
        first.add(route("canonical_goal", Map.of(SAFE, 1L), Map.of(), Map.of(PRODUCT, 5L), "UNBUILT"));
        first.machines.put("UNBUILT", new Machine("UNBUILT", unbuilt, 0));
        first.meta(PRODUCT, renewable("canonical_goal"));
        first.add(new Route("manual_goal", Authority.CRAFTING_JSON, "fixture:manual-goal", null,
                Map.of(SAFE, 1L), Map.of(), Map.of(PRODUCT, 5L), Set.of(), 0, 0, Map.of(),
                Set.of(), false, true, false));
        first.requiredQuests.add("exit");
        first.builder.objective(new Objective("exit", PRODUCT, 5, Set.of(), null, "fixture:exit"));
        first.exitObjective = PRODUCT;
        result = first.audit();
        yes("manual availability and a gift cannot rescue an impossible canonical manufacturing goal",
                hasRule(result, "exit_goal_production_unproven"));
        eq("the impossible canonical exit has no completed batch", 0L,
                result.tiers().get(0).exitProduction().executedBatches());

        retainedFailedBranch();
    }

    private static Fixture firstBench(Authority authority, boolean oneShot, boolean automatable) {
        Fixture fixture = new Fixture().terminal("manual_raw", RAW, TerminalKind.MANUAL_WORLD, false, true)
                .meta(RAW, metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null));
        fixture.add(new Route("manual_bench", authority, "fixture:manual-bench", null,
                Map.of(RAW, 1L), Map.of(), Map.of(MACHINE, 1L), Set.of(), 0, 0,
                authority == Authority.VANILLA_TRANSFORMATION
                        ? Map.of("crafting_type", "minecraft:crafting_shaped") : Map.of(),
                Set.of(), oneShot, authority != Authority.VANILLA_TRANSFORMATION, automatable));
        fixture.add(route("auto_bench", Map.of(SAFE, 1L), Map.of(), Map.of(MACHINE, 1L), "BENCH"));
        fixture.add(route("auto_product", Map.of(SAFE, 1L), Map.of(), Map.of(PRODUCT, 1L), "BENCH"));
        fixture.machines.put("BENCH", new Machine("BENCH", MACHINE, 0));
        fixture.meta(MACHINE, renewable("auto_bench")).meta(PRODUCT, renewable("auto_product")).root(PRODUCT);
        fixture.entryProfile = new EntryProfile("finite_first_bench", Map.of(RAW, 1L), 0, null,
                Set.of("Conditional ordinary fixture material custody, not a gameplay kit."), "fixture:first-bench");
        return fixture;
    }

    private static void retainedFailedBranch() {
        Resource absent = item("zz_absent");
        Resource worker = item("worker");
        Fixture fixture = new Fixture().terminal("manual_raw", RAW, TerminalKind.MANUAL_WORLD, false, true)
                .terminal("manual_absent", absent, TerminalKind.MANUAL_WORLD, false, true)
                .seed(TOOL, 1).seed(worker, 1)
                .meta(RAW, metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null))
                .meta(absent, metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null));
        fixture.add(route("retained_sub_batch", Map.of(RAW, 2L, ENERGY, 160L), Map.of(), Map.of(SIDE, 1L), "WORK"));
        fixture.add(route("failed_canonical", Map.of(SIDE, 1L, absent, 1L), Map.of(), Map.of(MACHINE, 1L), null));
        fixture.add(new Route("manual_bench", Authority.CRAFTING_JSON, "fixture:manual-bench", null,
                Map.of(RAW, 1L), Map.of(), Map.of(MACHINE, 1L), Set.of(), 0, 0, Map.of(),
                Set.of(), false, true, false));
        fixture.add(route("next_work", Map.of(SAFE, 1L, ENERGY, 160L), Map.of(), Map.of(PRODUCT, 1L), "WORK"));
        fixture.machines.put("WORK", new Machine("WORK", worker, 0));
        fixture.machines.put("BENCH", new Machine("BENCH", MACHINE, 0));
        fixture.add(route("use_bench", Map.of(SAFE, 1L), Map.of(), Map.of(item("bench_use"), 1L), "BENCH"));
        fixture.meta(MACHINE, metadata(ResourceClass.BOOTSTRAP, null, "failed_canonical",
                List.of(), null, null, null, null));
        fixture.root(item("bench_use")).root(PRODUCT).workProfile(Map.of(RAW, 3L), 1, Set.of("WORK"));
        var result = fixture.audit();
        Map<String, Long> batches = result.tiers().get(0).establishmentBatches();
        eq("a legitimate retained sub-batch survives the failed outer attempt", 1L,
                batches.get("retained_sub_batch"));
        eq("a failed outer attempt never becomes a completed batch", 0L,
                batches.getOrDefault("failed_canonical", 0L));
        eq("the successful alternative is the craft actually executed", 1L, batches.get("manual_bench"));
        var reserve = result.tiers().get(0).reserves().stream().filter(row -> row.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("retained sub-batch materials are not refunded with the failed parent", 3L, reserve.required());
        eq("retained sub-batch materials are charged only once", 0L, reserve.margin());
        yes("the retained sub-batch's spent local work cannot be spent again",
                hasRule(result, "entry_work_insufficient"));
        eq("no second work batch is fabricated from a refunded action", 0L,
                batches.getOrDefault("next_work", 0L));
    }

    private static void environmentWithoutTier() {
        group("R11 environmental facts do not authorize an unknown progression tier");
        Fixture fixture = new Fixture().environment("unplaced_collector", Map.of(PRODUCT, 1L), null);
        RoutePolicy original = fixture.policies.get("unplaced_collector");
        fixture.policies.put("unplaced_collector", new RoutePolicy((Integer) null, false,
                original.environmentalSources()));
        var result = fixture.audit();
        no("unknown tier does not erase a real environmental explanation",
                hasRule(result, "zero_input_unjustified"));
        yes("an authored explanation retains its unknown route tier", hasRule(result, "route_tier_unknown"));
        for (int tier = 0; tier <= 15; tier++) {
            no("the unplaced collector is not available at T" + tier,
                    result.tiers().get(tier).available().contains(PRODUCT));
            no("the unplaced collector is not renewable at T" + tier,
                    result.tiers().get(tier).renewable().contains(PRODUCT));
        }
        String json = AuditReports.json(result, "fixture:unknown-tier", true);
        JsonObject row = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("routes").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(route -> route.get("id").getAsString().equals("unplaced_collector"))
                .findFirst().orElseThrow();
        yes("an unknown route tier is exported as JSON null, not T0", row.get("unlock_tier").isJsonNull());
        eq("unknown-tier exports are deterministic", json, AuditReports.json(result, "fixture:unknown-tier", true));
        eq("partial annotation reports do not falsely claim to have no annotations",
                "report_only_s1_partial_annotations",
                JsonParser.parseString(json).getAsJsonObject().get("mode").getAsString());
        fixture.policies.put("unplaced_collector", new RoutePolicy(0, false, original.environmentalSources()));
        result = fixture.audit();
        yes("only an explicit unlock makes the collector available", available(result, PRODUCT));
        yes("explicit progression can then use its real environmental proof", renewable(result, PRODUCT));
        no("explicit progression removes only the resolved route-tier unknown", hasRule(result, "route_tier_unknown"));
        throwsArgument("present negative tiers remain invalid", () -> new RoutePolicy(-1, false, Set.of()));
        throwsArgument("present tiers above F15 remain invalid", () -> new RoutePolicy(16, false, Set.of()));
        Fixture side = new Fixture().environment("unplaced_side", Map.of(SIDE, 1L), null);
        side.add(route("later_consumer", Map.of(SIDE, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        side.policy("later_consumer", 1, false);
        side.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.BYPRODUCT_CURRENT,
                "unplaced_side", List.of(), reuse(), null, null, null));
        RoutePolicy source = side.policies.get("unplaced_side");
        side.policies.put("unplaced_side", new RoutePolicy((Integer) null, false, source.environmentalSources()));
        result = side.audit();
        yes("a side stream with an unknown producer tier returns explicit uncertainty without unboxing null",
                hasRule(result, "route_tier_unknown"));
        no("an unplaced producer does not get a silently invented T0 handling deadline",
                hasRule(result, "current_consumer_unavailable"));
        side.policies.put("unplaced_side", new RoutePolicy(0, false, source.environmentalSources()));
        yes("an explicitly T0 producer restores the real consumer-tier obligation",
                hasRule(side.audit(), "current_consumer_unavailable"));
    }

    private static void conditionalEntry() {
        group("R12 finite conditional entry and non-portable local work");
        Fixture fixture = conditionalWorkFixture();
        var unprofiled = fixture.audit();
        no("manual acquisition without a profile is not an ENERGY source", available(unprofiled, ENERGY));
        no("an unfunded local-work recipe does not become available", available(unprofiled, PRODUCT));
        yes("an absent conditional entry budget stays explicit uncertainty",
                hasRule(unprofiled, "construction_reserve_unknown"));
        fixture.workProfile(Map.of(), 0, Set.of("F0_PROCESS"));
        var derived = SustainabilityAudit.deriveEntryProfile(fixture.builder.build(), fixture.annotations(),
                DebtManifest.empty(), 0);
        eq("the bill derives the controller and tool's exact source input draw", Map.of(RAW, 3L),
                derived.profile().playerProvided());
        eq("work actions derive from the actual batch cost and charge", 2L,
                derived.profile().manualWorkActions());
        var result = derived.replay();
        yes("the returned finite replay uses the permitted ProcessMachine batch", available(result, PRODUCT));
        no("bounded crank work never becomes portable ENERGY availability", available(result, ENERGY));
        no("bounded local work never becomes renewable ENERGY", renewable(result, ENERGY));
        no("manual-only power does not certify renewable process output", renewable(result, PRODUCT));
        no("the derived finite materials replay without replenished manual stock",
                hasRule(result, "entry_profile_insufficient"));
        no("the derived finite action budget is sufficient", hasRule(result, "entry_work_insufficient"));
        var reserve = result.tiers().get(0).reserves().stream().filter(row -> row.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("ordinary player-acquired goods never become guaranteed gifts", 0L, reserve.guaranteed());
        eq("conditional player custody is reported separately", 3L, reserve.playerProvided());
        eq("the sufficient bill reports its actual draw", 3L, reserve.required());
        eq("the finite replay reports its real margin", 0L, reserve.margin());
        eq("this proof is explicitly conditional", "conditional_established", reserve.status());
        var tier = result.tiers().get(0);
        yes("established conditional reserves are not counted as reserve findings in the human report",
                AuditReports.markdown(result, "fixture:conditional-entry", true).contains(
                        "| T0 | authored | " + tier.mandatory().size() + " | " + tier.available().size()
                                + " | " + tier.renewable().size() + " | 0 |"));
        eq("the action reserve is independent of all ENERGY stock", 2L,
                result.tiers().get(0).manualWork().requiredActions());
        fixture.entryProfile = derived.profile().withBill(Map.of(RAW, 2L), 2);
        result = fixture.audit();
        yes("one missing ordinary entry unit fails the finite replay", hasRule(result, "entry_profile_insufficient"));
        fixture.entryProfile = derived.profile().withBill(Map.of(RAW, 3L), 1);
        result = fixture.audit();
        yes("one missing local-work action fails the finite replay", hasRule(result, "entry_work_insufficient"));
        eq("insufficient labour has a negative independent action margin", -1L,
                result.tiers().get(0).manualWork().marginActions());

        Fixture terminal = new Fixture().seed(MACHINE, 1).seed(RAW, 1).seed(TOOL, 1);
        terminal.machines.put("RESEARCH_TERMINAL", new Machine("RESEARCH_TERMINAL", MACHINE, 0));
        terminal.add(new Route("research", Authority.BOOTSTRAP_SOURCE, "fixture:research", "RESEARCH_TERMINAL",
                Map.of(RAW, 1L, ENERGY, 320L), Map.of(), Map.of(PRODUCT, 1L), Set.of(),
                40, 8, Map.of(), Set.of(), false, true, true)).root(PRODUCT);
        terminal.workProfile(Map.of(), 1000, Set.of("F0_PROCESS"));
        result = terminal.audit();
        no("a physical core, Terminal and ample manual FU cannot calibrate without a producer",
                available(result, PRODUCT));
        no("F0 Terminal is not made a ProcessMachine by its rating", renewable(result, PRODUCT));
        terminal.environment("native_fuel_feed", Map.of(SIDE, 1L), null).meta(SIDE, renewable("native_fuel_feed"));
        Resource dynamo = item("native_dynamo");
        terminal.seed(dynamo, 1);
        terminal.machines.put("NATIVE_DYNAMO", new Machine("NATIVE_DYNAMO", dynamo, 0));
        terminal.add(new Route("native_power", Authority.GENERATOR_FUEL, "fixture:native_power", "NATIVE_DYNAMO",
                Map.of(SIDE, 1L), Map.of(), Map.of(ENERGY, 320L), Set.of(), 40, 0,
                Map.of(), Set.of(), false, true, true)).meta(ENERGY, renewable("native_power"));
        result = terminal.audit();
        yes("constructible native power can actually supply the same Terminal", available(result, PRODUCT));
        eq("calibration never debits the independent crank-work balance", 0L,
                result.tiers().get(0).manualWork().requiredActions());
        throwsArgument("conditional custody cannot carry abstract manual FU",
                () -> new EntryProfile("invalid", Map.of(ENERGY, 1L), 0, null,
                        Set.of("fixture:conditional"), "fixture:invalid"));
        throwsArgument("an unscoped action budget is rejected",
                () -> new EntryProfile("invalid", Map.of(), 1, null,
                        Set.of("fixture:conditional"), "fixture:invalid"));
        throwsArgument("a profile cannot omit its acquisition assumptions",
                () -> new EntryProfile("invalid", Map.of(RAW, 1L), 0, null,
                        Set.of(), "fixture:invalid"));
        String json = AuditReports.json(derived.replay(), "fixture:finite-entry", true);
        eq("conditional entry identity and output are deterministic", json,
                AuditReports.json(derived.replay(), "fixture:finite-entry", true));
        yes("the public report distinguishes player custody and profile identity",
                json.contains("\"player_provided\"") && json.contains("\"entry_profile_sha256\""));
        Resource part = item("nested_part");
        Resource failed = item("a_failed_outer");
        Resource success = item("b_later_outer");
        Fixture nested = new Fixture().seed(MACHINE, 1).seed(TOOL, 1);
        nested.machines.put("F0_PROCESS", new Machine("F0_PROCESS", MACHINE, 0));
        nested.add(route("nested_part", Map.of(ENERGY, 160L, SAFE, 1L), Map.of(), Map.of(part, 1L), "F0_PROCESS"));
        nested.add(route("failed_outer", Map.of(ENERGY, 160L, part, 1L, item("z_unavailable"), 1L),
                Map.of(), Map.of(failed, 1L), "F0_PROCESS")).root(failed);
        nested.add(route("later_outer", Map.of(ENERGY, 320L, part, 1L), Map.of(),
                Map.of(success, 1L), "F0_PROCESS")).root(success);
        nested.workProfile(Map.of(), 2, Set.of("F0_PROCESS"));
        result = nested.audit();
        yes("a failed outer batch does not refund completed prerequisite labour",
                hasRule(result, "entry_work_insufficient"));
        eq("successful nested work remains charged before a later batch", 3L,
                result.tiers().get(0).manualWork().requiredActions());
    }

    private static Fixture conditionalWorkFixture() {
        Fixture fixture = new Fixture().terminal("manual_raw", RAW, TerminalKind.MANUAL_WORLD, false, true);
        fixture.add(route("make_controller", Map.of(RAW, 2L), Map.of(), Map.of(MACHINE, 1L), null));
        fixture.add(route("make_tool", Map.of(RAW, 1L), Map.of(), Map.of(TOOL, 1L), null));
        fixture.machines.put("F0_PROCESS", new Machine("F0_PROCESS", MACHINE, 0));
        fixture.environment("local_work_process", Map.of(ENERGY, 320L, SAFE, 1L), Map.of(),
                Map.of(PRODUCT, 1L), "F0_PROCESS").root(PRODUCT);
        return fixture;
    }

    private static void manualExitGoal() {
        group("R13 concrete manual exit is not an automatic renewable product");
        Fixture fixture = exitFixture(false, 5);
        var result = fixture.audit();
        yes("a manually crafted goal can be available from renewable consumed supplies", available(result, PRODUCT));
        no("manual crafting with renewable inputs is not automatable supply", renewable(result, PRODUCT));
        no("the selected relay-like objective is duplicated as a recurring output root",
                result.tiers().get(0).recurring().contains(PRODUCT));
        yes("the manual goal's consumed feed remains recurring", result.tiers().get(0).recurring().contains(RAW));
        eq("the witness executes the entire real five-unit batch", 5L,
                result.tiers().get(0).exitProduction().produced());
        eq("the objective uses its actual count", 5L, result.tiers().get(0).exitProduction().requiredCount());
        eq("one real batch satisfies the five-unit objective", 1L,
                result.tiers().get(0).exitProduction().executedBatches());
        no("a proper manual goal is not reported as missing production", hasRule(result, "exit_goal_production_unproven"));
        Fixture claim = new Fixture();
        claim.add(new Route("quest/exit_reward", Authority.QUEST_REWARD, "fixture:reward", null,
                Map.of(), Map.of(SAFE, 1L), Map.of(PRODUCT, 5L), Set.of(), 0, 0,
                Map.of(), Set.of(), true, true, false));
        claim.meta(PRODUCT, metadata(ResourceClass.BOOTSTRAP, null, "quest/exit_reward",
                List.of(), null, null, null, null));
        claim.builder.objective(new Objective("exit", PRODUCT, 5, Set.of(), null, "fixture:exit"));
        claim.requiredQuests.add("exit");
        claim.exitObjective = PRODUCT;
        result = claim.audit();
        yes("an explicitly selected quest reward is not a manufacturing witness",
                hasRule(result, "exit_goal_route_invalid"));
        eq("a claimed gift never records a real exit production batch", null,
                result.tiers().get(0).exitProduction());
        Fixture rewardedCraft = rewardProducer(2);
        Resource manufactured = item("manufactured_goal");
        rewardedCraft.add(new Route("real_manual_goal", Authority.CRAFTING_JSON, "fixture:real_manual_goal", null,
                Map.of(PRODUCT, 1L), Map.of(), Map.of(manufactured, 5L), Set.of(), 0, 0,
                Map.of(), Set.of(), false, true, false));
        rewardedCraft.meta(manufactured, metadata(ResourceClass.BOOTSTRAP, null, "real_manual_goal",
                List.of(), null, null, null, null));
        rewardedCraft.builder.objective(new Objective("exit", manufactured, 5, Set.of(), null, "fixture:exit"));
        rewardedCraft.requiredQuests.add("exit");
        rewardedCraft.exitObjective = manufactured;
        result = rewardedCraft.audit();
        eq("a legitimate claimed reward may still fund the real manual craft's budget", 5L,
                result.tiers().get(0).exitProduction().produced());
        yes("budgeted rewards remain counted as claims, not gifts or manufactured goals",
                result.tiers().get(0).reserves().stream().anyMatch(reserve ->
                        reserve.resource().equals(RAW) && reserve.claimedRewards() == 2 && reserve.guaranteed() == 0));
        result = exitFixture(false, 7).audit();
        eq("a larger actual objective count is not silently defaulted to one", 7L,
                result.tiers().get(0).exitProduction().requiredCount());
        eq("batch rounding satisfies the real larger goal", 10L, result.tiers().get(0).exitProduction().produced());
        result = exitFixture(true, 5).audit();
        yes("the same synthetic product is R when an actual automatic executor owns its route",
                renewable(result, PRODUCT));
        Fixture gift = new Fixture().seed(PRODUCT, 99);
        gift.exitObjective = PRODUCT;
        result = gift.audit();
        yes("a gift can make goal stock available", available(result, PRODUCT));
        yes("goal stock or gifts cannot replace an actual production route", hasRule(result, "exit_goal_route_missing"));
        Fixture giftedCraft = exitFixture(false, 5).seed(PRODUCT, 99);
        result = giftedCraft.audit();
        eq("pre-existing goal stock never skips execution of the real manual batch", 5L,
                result.tiers().get(0).exitProduction().produced());
        Fixture finite = new Fixture().seed(RAW, 1);
        finite.add(new Route("manual_goal", Authority.CRAFTING_JSON, "fixture:manual_goal", null,
                Map.of(RAW, 1L), Map.of(), Map.of(PRODUCT, 5L), Set.of(), 0, 0,
                Map.of(), Set.of(), false, true, false));
        finite.exitObjective = PRODUCT;
        result = finite.audit();
        yes("a finite input can never stand in for renewable exit supply",
                hasRule(result, "exit_goal_supply_not_renewable"));
        Route manual = fixture.builder.build().routes().get("manual_goal");
        Route automatic = new Route(manual.id(), manual.authority(), manual.source(), manual.family(),
                manual.inputs(), manual.catalysts(), manual.outputs(), manual.discharged(),
                manual.durationTicks(), manual.fuPerTick(), manual.conditions(), manual.prerequisites(),
                manual.oneShot(), manual.modOwned(), true);
        no("executor semantics are part of the authority fingerprint",
                AuditReports.snapshotHash(new Builder().route(manual).build())
                        .equals(AuditReports.snapshotHash(new Builder().route(automatic).build())));
        Fixture manualLoop = new Fixture().seed(RAW, 1);
        manualLoop.add(new Route("manual_forward", Authority.CRAFTING_JSON, "fixture:manual_forward", null,
                Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 1L), Set.of(), 0, 0,
                Map.of(), Set.of(), false, true, false));
        manualLoop.add(route("automatic_return", Map.of(SIDE, 1L), Map.of(), Map.of(RAW, 1L), null));
        manualLoop.loops.put("manual_loop", new ClosedLoop("manual_loop", RAW,
                Set.of("manual_forward", "automatic_return"), null));
        manualLoop.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "automatic_return",
                List.of(), null, null, null, null)).root(RAW);
        yes("a manual member cannot establish automatic renewable circulation",
                hasRule(manualLoop.audit(), "loop_manual_route"));
    }

    private static Fixture exitFixture(boolean automatic, long count) {
        Fixture fixture = new Fixture().environment("native_feed", Map.of(RAW, 1L), null)
                .meta(RAW, renewable("native_feed"));
        fixture.add(new Route("manual_goal", Authority.CRAFTING_JSON, "fixture:manual_goal", null,
                Map.of(RAW, 1L), Map.of(), Map.of(PRODUCT, 5L), Set.of(), 0, 0,
                Map.of(), Set.of(), false, true, automatic));
        fixture.meta(PRODUCT, automatic ? renewable("manual_goal")
                : metadata(ResourceClass.BOOTSTRAP, null, "manual_goal", List.of(), null, null, null, null));
        fixture.builder.objective(new Objective("exit", PRODUCT, count, Set.of(), null, "fixture:exit_count"));
        fixture.requiredQuests.add("exit");
        fixture.exitObjective = PRODUCT;
        return fixture;
    }

    private static void localWorkNativeProduction() {
        group("R14 startup work retains a native recurrent FU seed");
        Snapshot live;
        try {
            live = AuthorityAdapters.live(Path.of(""));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect actual native production quantities", failure);
        }
        Fixture fixture = productiveFixture(live, 0, true).seed(TOOL, 1);
        fixture.workProfile(Map.of(), 9, Set.of("GEOLOGICAL_COLLECTOR", "BIOCULTIVATOR"));
        var result = fixture.audit();
        yes("finite local startup work can establish actual renewable native FU", renewable(result, ENERGY));
        no("manual work itself never enters R", renewable(result, new Resource(Kind.ENVIRONMENT, "fixture:local_work")));
        eq("the native 800+640 prefix costs exactly nine 160-FU startup actions", 9L,
                result.tiers().get(0).manualWork().requiredActions());
        eq("the next circulation retains the real native 1440-FU seed, not another labour allowance",
                1440L, result.tiers().get(0).retainedInventory().get(ENERGY));
        no("later native withdrawals do not demand repeated manual actions", hasRule(result, "entry_work_insufficient"));
        no("native-cycle replay neither loses the seed nor claims infinite manual custody",
                hasRule(result, "construction_reserve_unknown"));
        fixture.entryProfile = fixture.entryProfile.withBill(Map.of(), 8);
        result = fixture.audit();
        no("eight real local actions cannot fund the native startup prefix", renewable(result, ENERGY));
        yes("the insufficient startup remains proof-visible", hasRule(result, "loop_startup_unproven"));
        fixture = productiveFixture(live, 0, false).seed(TOOL, 1);
        fixture.workProfile(Map.of(), 100, Set.of("GEOLOGICAL_COLLECTOR", "BIOCULTIVATOR"));
        no("ample manual work cannot turn finite water into the native renewable system",
                renewable(fixture.audit(), ENERGY));
        derivedStartupBudget(live);
        conditionalLoopSeed();
        physicalManualSource();
    }

    private static void derivedStartupBudget(Snapshot live) {
        Fixture fixture = productiveFixture(live, 0, true, false).seed(TOOL, 1);
        Resource entry = item("acquired_entry");
        Resource power = new Resource(Kind.ITEM, "grindless:biomass_dynamo");
        fixture.terminal("manual_entry", entry, TerminalKind.MANUAL_WORLD, false, true);
        fixture.add(route("auto_bench", Map.of(SAFE, 1L, ENERGY, 160L), Map.of(), Map.of(MACHINE, 1L), "BENCH"));
        fixture.add(route("auto_power", Map.of(SAFE, 1L, ENERGY, 160L, entry, 3L), Map.of(), Map.of(power, 1L), "BENCH"));
        fixture.add(new Route("manual_bench", Authority.CRAFTING_JSON, "fixture:manual-bench", null,
                Map.of(entry, 1L), Map.of(), Map.of(MACHINE, 1L), Set.of(), 0, 0, Map.of(),
                Set.of(), false, true, false));
        fixture.add(new Route("manual_power", Authority.CRAFTING_JSON, "fixture:manual-power", null,
                Map.of(entry, 1L), Map.of(), Map.of(power, 1L), Set.of(), 0, 0, Map.of(),
                Set.of(), false, true, false));
        fixture.machines.put("BENCH", new Machine("BENCH", MACHINE, 0));
        fixture.meta(MACHINE, renewable("auto_bench")).meta(power, renewable("auto_power"));
        fixture.workProfile(Map.of(), 0, Set.of("BENCH", "GEOLOGICAL_COLLECTOR", "BIOCULTIVATOR"));
        var derived = SustainabilityAudit.deriveEntryProfile(fixture.builder.build(), fixture.annotations(),
                DebtManifest.empty(), 0);
        eq("entry work includes the successful standalone startup, not just the cheaper establishment order",
                10L, derived.profile().manualWorkActions());
        eq("alternative proofs share a sufficient custody bound, not the sum of their separate executions",
                Map.of(entry, 4L), derived.profile().playerProvided());
        yes("a fresh finite replay really proves native power with the combined bill",
                renewable(derived.replay(), ENERGY));
        no("independent startup costs cannot leave a false-negative insufficient replay",
                hasRule(derived.replay(), "entry_work_insufficient"));
        no("independent startup costs cannot leave an unproved profiled native circulation",
                findingAt(derived.replay(), "loop_startup_unproven", "native_production", "T0"));
        fixture.entryProfile = derived.profile().withBill(derived.profile().playerProvided(), 9);
        no("one action below the actual standalone proof requirement stays unproved",
                renewable(fixture.audit(), ENERGY));
    }

    private static void conditionalLoopSeed() {
        group("R15 finite player custody seeds a conserved circulation exactly once");
        Fixture fixture = conditionalSeedFixture(1);
        var result = fixture.audit();
        yes("a finite manually acquired seed can establish the environment-fed circulation",
                renewable(result, PRODUCT));
        no("the restored finite carrier is not unlimited net supply", renewable(result, RAW));
        no("a funded finite carrier is proved for its actual profiled tier",
                findingAt(result, "loop_startup_unproven", "cycle", "T0"));
        eq("the restored player-provided seed is retained once", 1L,
                result.tiers().get(0).retainedInventory().get(RAW));
        yes("seed custody is distinct from gifts and does not duplicate its reserve",
                result.tiers().get(0).reserves().stream().anyMatch(reserve -> reserve.resource().equals(RAW)
                        && reserve.playerProvided() == 1 && reserve.guaranteed() == 0
                        && reserve.required() == 1 && reserve.margin() == 0));
        fixture.entryProfile = null;
        no("an unquantified manual source alone cannot seed the same loop", renewable(fixture.audit(), PRODUCT));
        fixture = conditionalSeedFixture(1);
        fixture.entryProfile = fixture.entryProfile.withBill(Map.of(), 0);
        no("an empty conditional budget cannot replace the physical seed", renewable(fixture.audit(), PRODUCT));
        fixture = conditionalSeedFixture(2);
        result = fixture.audit();
        yes("a source-derived multi-batch startup can use a sufficient finite seed", renewable(result, PRODUCT));
        eq("multi-batch circulation retains its actual two-unit seed", 2L,
                result.tiers().get(0).retainedInventory().get(RAW));
        fixture.entryProfile = fixture.entryProfile.withBill(Map.of(RAW, 1L), 0);
        no("one unit cannot pay the actual two-unit normalized startup prefix", renewable(fixture.audit(), PRODUCT));
        Fixture spent = conditionalSeedFixture(1, "CYCLE_MACHINE");
        spent.add(route("controller", Map.of(RAW, 1L), Map.of(), Map.of(MACHINE, 1L), null));
        spent.machines.put("CYCLE_MACHINE", new Machine("CYCLE_MACHINE", MACHINE, 0));
        no("a provision already consumed by construction cannot be duplicated as a loop seed",
                renewable(spent.audit(), PRODUCT));
        Fixture derivation = conditionalSeedFixture(2);
        derivation.entryProfile = derivation.entryProfile.withBill(Map.of(), 0);
        var derived = SustainabilityAudit.deriveEntryProfile(derivation.builder.build(),
                derivation.annotations(), DebtManifest.empty(), 0);
        eq("derived conservative seed quantities come from normalized batches", 2L,
                derived.profile().playerProvided().get(RAW));
        yes("the derived finite seed is separately replayed rather than assumed", renewable(derived.replay(), PRODUCT));
    }

    private static Fixture conditionalSeedFixture(long seed) {
        return conditionalSeedFixture(seed, null);
    }

    private static Fixture conditionalSeedFixture(long seed, String family) {
        Fixture fixture = new Fixture().terminal("manual_raw", RAW, TerminalKind.MANUAL_WORLD, false, true);
        fixture.add(route("forward", Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 1L), family));
        fixture.environment("back", Map.of(SIDE, seed), Map.of(), Map.of(RAW, seed, PRODUCT, 1L), null);
        fixture.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "back", List.of(), null, null, null, null));
        fixture.meta(PRODUCT, renewable("back")).root(PRODUCT);
        fixture.loops.put("cycle", new ClosedLoop("cycle", RAW, Set.of("forward", "back"), null));
        fixture.entryProfile = new EntryProfile("entry", Map.of(RAW, seed), 0, null,
                Set.of("Player supplies a finite acquired RAW seed"), "fixture:conditional_seed");
        return fixture;
    }

    private static void physicalManualSource() {
        group("R16 real physical manual inputs justify facts, never finance a producer or R");
        Resource action = new Resource(Kind.ENVIRONMENT, "fixture:physical_click");
        Route manual = new Route("physical_click", Authority.BOOTSTRAP_SOURCE, "fixture:physical_click",
                null, Map.of(action, 1L), Map.of(), Map.of(ENERGY, 160L), Set.of(), 20, 0,
                Map.of(), Set.of(), false, true, false);
        Fixture fixture = new Fixture().terminal("physical_click", action, TerminalKind.MANUAL_LABOUR, false, true);
        fixture.add(manual).meta(ENERGY, metadata(ResourceClass.BOOTSTRAP, null, manual.id(),
                List.of(), null, null, null, null)).root(ENERGY);
        var result = fixture.audit();
        no("a real direct manual action justifies the nonautomatic producer's source fact",
                hasRule(result, "zero_input_unjustified"));
        no("that justified click is not renewable labour or FU", renewable(result, ENERGY));
        no("a source fact without any entry profile is not a sustainability pass", result.sustainabilityPass());
        fixture.entryProfile = new EntryProfile("entry", Map.of(), 0, null,
                Set.of("No physical Dynamo action budget has been supplied"), "fixture:unfunded_click");
        result = fixture.audit();
        yes("an action fact without a finite funding mechanism leaves establishment unknown",
                hasRule(result, "construction_reserve_unknown"));
        no("source justification alone is not a sustainability pass", result.sustainabilityPass());
        fixture.seed(TOOL, 1).workProfile(Map.of(), 1000, Set.of("F0_PROCESS"));
        result = fixture.audit();
        yes("CrankHandle actions cannot fund an unrelated physical Dynamo's input",
                hasRule(result, "construction_reserve_unknown"));
        eq("the unrelated manual producer cannot debit local ProcessMachine work", 0L,
                result.tiers().get(0).manualWork().requiredActions());
        Fixture automatic = new Fixture().terminal("physical_click", action, TerminalKind.MANUAL_LABOUR, false, true);
        automatic.add(new Route(manual.id(), manual.authority(), manual.source(), manual.family(), manual.inputs(),
                manual.catalysts(), manual.outputs(), manual.discharged(), manual.durationTicks(), manual.fuPerTick(),
                manual.conditions(), manual.prerequisites(), manual.oneShot(), manual.modOwned(), true));
        yes("automatic=true cannot disguise a manual action as environmental generation",
                hasRule(automatic.audit(), "zero_input_unjustified"));
        Fixture missing = new Fixture().add(manual);
        yes("an ENV input name without the actual labour declaration is unproven",
                hasRule(missing.audit(), "zero_input_unjustified"));
        Fixture wrong = new Fixture().terminal("physical_click", action, TerminalKind.WORLD_GENERATION, false, true);
        wrong.add(manual);
        yes("a different terminal kind cannot substitute for the real manual action",
                hasRule(wrong.audit(), "zero_input_unjustified"));
        Fixture namedOnly = new Fixture().terminal("physical_click",
                new Resource(Kind.ENVIRONMENT, "fixture:other_action"), TerminalKind.MANUAL_LABOUR, false, true);
        namedOnly.add(manual);
        yes("a matching terminal name cannot justify an unrelated input resource",
                hasRule(namedOnly.audit(), "zero_input_unjustified"));
        Fixture returned = new Fixture().terminal("physical_click", action, TerminalKind.MANUAL_LABOUR, false, true);
        returned.add(new Route(manual.id(), manual.authority(), manual.source(), null, Map.of(action, 1L),
                Map.of(), Map.of(action, 1L, ENERGY, 160L), Set.of(), 20, 0, Map.of(), Set.of(), false, true, false));
        yes("returning the action instead of consuming real positive labour does not justify a producer",
                hasRule(returned.audit(), "zero_input_unjustified"));
    }

    private static void formedConstruction() {
        group("R9 derived formed requirements and dedicated installations");
        Route firing = AuthorityAdapters.process(BootstrapProcesses.recipes().stream()
                .filter(recipe -> recipe.id().equals("bootstrap/insulators")).findFirst().orElseThrow());
        Resource bricks = new Resource(Kind.ITEM, "minecraft:bricks");
        eq("the formed route derives exactly seven real brick blocks", Map.of(bricks, 7L), firing.catalysts());
        eq("the route references the definition rather than a copied assembly item", "FIRING_CHAMBER",
                firing.conditions().get("workshop_structure"));
        Route settling = AuthorityAdapters.process(BootstrapProcesses.recipes().stream()
                .filter(recipe -> recipe.id().equals("bootstrap/settle_clay")).findFirst().orElseThrow());
        eq("settling needs the real channel and screen but no air or duplicated controller",
                Map.of(new Resource(Kind.ITEM, "grindless:settling_channel"), 1L,
                        new Resource(Kind.ITEM, "grindless:settling_screen"), 1L), settling.catalysts());
        no("the chamber cell list never includes the controller",
                WorkshopStructure.FIRING_CHAMBER.cells().stream()
                        .anyMatch(cell -> cell.offset().equals(new WorkshopStructure.Offset(0, 0, 0))));

        Fixture controllerOnly = formedFixture(0, false, firing.catalysts());
        var result = controllerOnly.audit();
        no("a controller alone does not make formed output available", available(result, PRODUCT));
        no("a controller alone cannot establish a formed renewable producer", renewable(result, PRODUCT));
        result = formedFixture(6, false, firing.catalysts()).audit();
        yes("qualitative availability cannot hide a one-brick formation deficit", hasRule(result, "bootstrap_starvation"));
        result = formedFixture(7, false, firing.catalysts()).audit();
        no("one fully paid formed installation has no invented deficit", hasRule(result, "bootstrap_starvation"));
        eq("two recipes in one family do not recharge the same seven bricks", 7L,
                result.tiers().get(0).reserves().stream().filter(reserve -> reserve.resource().equals(bricks))
                        .findFirst().orElseThrow().required());
        eq("the controller is paid once, separately from the surrounding cells", 1L,
                result.tiers().get(0).reserves().stream().filter(reserve -> reserve.resource().equals(MACHINE))
                        .findFirst().orElseThrow().required());
        result = formedFixture(7, true, firing.catalysts()).audit();
        yes("two installations cannot share the same physical seven blocks", hasRule(result, "bootstrap_starvation"));
        eq("dedicated installations require fourteen blocks", 14L,
                result.tiers().get(0).reserves().stream().filter(reserve -> reserve.resource().equals(bricks))
                        .findFirst().orElseThrow().required());
        result = formedFixture(14, true, firing.catalysts()).audit();
        no("two fully paid installations need no duplicate per-recipe reserve", hasRule(result, "bootstrap_starvation"));
        Map<Resource, Long> six = Map.of(bricks, 6L);
        Route changed = new Route(firing.id(), firing.authority(), firing.source(), firing.family(), firing.inputs(),
                six, firing.outputs(), firing.discharged(), firing.durationTicks(), firing.fuPerTick(),
                firing.conditions(), firing.prerequisites(), firing.oneShot(), firing.modOwned());
        no("authority fingerprints change with derived formation quantities",
                AuditReports.snapshotHash(new Builder().route(firing).build())
                        .equals(AuditReports.snapshotHash(new Builder().route(changed).build())));
        try {
            yes("the actual formation definition participates in the authority-source identity",
                    AuthorityAdapters.authoritySourceHashes(Path.of("")).containsKey(
                            "common/src/main/java/io/github/ezequiel24123z/grindless/structure/WorkshopStructure.java"));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect the live formation authority", failure);
        }
    }

    private static Fixture formedFixture(long bricks, boolean secondInstallation, Map<Resource, Long> required) {
        Fixture fixture = new Fixture().seed(MACHINE, 1);
        if (bricks > 0) {
            fixture.seed(new Resource(Kind.ITEM, "minecraft:bricks"), bricks);
        }
        fixture.machines.put("FORMED_A", new Machine("FORMED_A", MACHINE, 0));
        fixture.environment(new Route("formed_first", Authority.PROCESS_GRAPH, "fixture:formed_first", "FORMED_A",
                Map.of(), required, Map.of(PRODUCT, 1L), Set.of(), 1, 0,
                Map.of("workshop_structure", "FIRING_CHAMBER"), Set.of(), false, true));
        fixture.meta(PRODUCT, renewable("formed_first")).root(PRODUCT);
        String family = "FORMED_A";
        if (secondInstallation) {
            Resource controller = item("second_formed_controller");
            fixture.seed(controller, 1);
            family = "FORMED_B";
            fixture.machines.put(family, new Machine(family, controller, 0));
        }
        fixture.environment(new Route("formed_second", Authority.PROCESS_GRAPH, "fixture:formed_second", family,
                Map.of(), required, Map.of(SIDE, 1L), Set.of(), 1, 0,
                Map.of("workshop_structure", "FIRING_CHAMBER"), Set.of(), false, true));
        return fixture.meta(SIDE, renewable("formed_second")).root(SIDE);
    }

    private static void productiveZeroCarrier() {
        group("R10 unit conservative carrier with environmental productive surplus");
        Snapshot live;
        try {
            live = AuthorityAdapters.live(Path.of(""));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect the actual production batches", failure);
        }
        Route geology = live.routes().get("source/geological_aggregate");
        Route culture = live.routes().get("source/biomass");
        Route fuel = live.routes().get("fuel/biomass_dynamo/biomass");
        Resource aggregate = new Resource(Kind.ITEM, SourceLogic.AGGREGATE);
        Resource biomass = new Resource(Kind.ITEM, SourceLogic.BIOMASS);
        Resource water = new Resource(Kind.FLUID, "minecraft:water");
        List<Route> routes = List.of(geology, culture, fuel);
        eq("the productive system keeps the conservative unit vector",
                Map.of(geology.id(), BigInteger.ONE, culture.id(), BigInteger.ONE, fuel.id(), BigInteger.ONE),
                SustainabilityAudit.normalizedBatches(routes, aggregate));
        Map<Resource, Long> net = new TreeMap<>();
        routes.forEach(route -> {
            route.inputs().forEach((resource, quantity) -> net.merge(resource, -quantity, Math::addExact));
            route.retainedOutputs().forEach((resource, quantity) -> net.merge(resource, quantity, Math::addExact));
        });
        eq("the carrier is exactly conservative, not minted by its loop", 0L, net.get(aggregate));
        eq("actual unit batches produce the confirmed biomass surplus", 3L, net.get(biomass));
        eq("actual unit batches produce the confirmed FU surplus", 160L, net.get(ENERGY));
        eq("actual unit batches still consume an external water feed", -500L, net.get(water));
        long prefix = Math.addExact(geology.inputs().get(ENERGY), culture.inputs().get(ENERGY));
        eq("the native prefix before the first fuel requires both actual machine costs", 1440L, prefix);
        var result = productiveFixture(live, prefix, true).audit();
        yes("quantity-sufficient production proves native renewable energy", renewable(result, ENERGY));
        yes("the fixed point subsequently proves additional geological aggregate", renewable(result, aggregate));
        yes("the environmental productive surplus is not discarded by over-normalization", renewable(result, biomass));
        no("the conservative carrier is not rejected as a positive-carrier exploit", hasRule(result, "closed_loop_gain"));
        result = productiveFixture(live, prefix - 1, true).audit();
        no("one FU below the real executable prefix cannot seed renewable energy", renewable(result, ENERGY));
        yes("the insufficient prefix remains a startup proof gap", hasRule(result, "loop_startup_unproven"));
        result = productiveFixture(live, 0, true).audit();
        no("qualitative cycles without a usable energy seed cannot establish supply", renewable(result, ENERGY));
        result = productiveFixture(live, prefix, false).audit();
        no("manual finite water cannot establish the productive renewable system", renewable(result, ENERGY));
        yes("missing external renewable water stays a concrete net-draw failure",
                hasRule(result, "loop_makeup_not_renewable"));

        Fixture free = new Fixture().seed(RAW, 1).seed(ENERGY, 1);
        free.add(route("free_forward", Map.of(RAW, 1L, ENERGY, 1L), Map.of(), Map.of(SIDE, 2L), null));
        free.add(route("free_back", Map.of(SIDE, 1L), Map.of(),
                Map.of(RAW, 1L, ENERGY, 2L, PRODUCT, 1L), null));
        free.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "free_back", List.of(), null, null, null, null));
        free.meta(PRODUCT, renewable("free_back")).root(PRODUCT);
        free.loops.put("free", new ClosedLoop("free", RAW, Set.of("free_forward", "free_back"), null));
        result = free.audit();
        yes("the unit attempt cannot bless material gain without a real source or net feed",
                hasRule(result, "loop_gain_unjustified"));
        no("a free conservative-carrier surplus is not renewable", renewable(result, PRODUCT));
    }

    private static Fixture productiveFixture(Snapshot live, long entryFu, boolean renewableWater) {
        return productiveFixture(live, entryFu, renewableWater, true);
    }

    private static Fixture productiveFixture(Snapshot live, long entryFu, boolean renewableWater, boolean seededPower) {
        Fixture fixture = new Fixture();
        Resource aggregate = new Resource(Kind.ITEM, SourceLogic.AGGREGATE);
        Resource biomass = new Resource(Kind.ITEM, SourceLogic.BIOMASS);
        Resource water = new Resource(Kind.FLUID, "minecraft:water");
        if (entryFu > 0) {
            fixture.seed(ENERGY, entryFu);
        }
        if (renewableWater) {
            fixture.environment("native_water", Map.of(water, (long) SourceLogic.SOURCE_WATER * 2), null)
                    .meta(water, renewable("native_water"));
        } else {
            fixture.terminal("finite_water", water, TerminalKind.MANUAL_WORLD, false, true);
        }
        for (String id : List.of("source/geological_aggregate", "source/biomass", "fuel/biomass_dynamo/biomass")) {
            Route route = live.routes().get(id);
            Resource controller = new Resource(Kind.ITEM, "grindless:" + route.family().toLowerCase(java.util.Locale.ROOT));
            if (seededPower || route.authority() != Authority.GENERATOR_FUEL) {
                fixture.seed(controller, 1);
            }
            fixture.machines.put(route.family(), new Machine(route.family(), controller, 0));
            if (route.id().startsWith("source/")) {
                fixture.environment(route);
            } else {
                fixture.add(route);
            }
        }
        fixture.meta(aggregate, renewable("source/geological_aggregate"));
        fixture.meta(biomass, renewable("source/biomass"));
        fixture.meta(ENERGY, renewable("fuel/biomass_dynamo/biomass"));
        fixture.loops.put("native_production", new ClosedLoop("native_production", aggregate,
                Set.of("source/geological_aggregate", "source/biomass", "fuel/biomass_dynamo/biomass"), null));
        return fixture.root(aggregate).root(biomass).root(ENERGY);
    }

    private static void protectedCirculation() {
        group("R1 protected circulation is not net supply");
        for (Resource carrier : List.of(WATER, ENERGY)) {
            for (boolean makeUp : List.of(false, true)) {
                Fixture fixture = new Fixture().seed(carrier, 2);
                Set<String> members;
                String reverse;
                if (carrier.equals(WATER)) {
                    fixture.add(route("to_v", Map.of(carrier, 1L), Map.of(), Map.of(STEAM, 1L), null));
                    fixture.add(route("to_w", Map.of(STEAM, 1L), Map.of(), Map.of(carrier, 1L), null));
                    members = Set.of("to_v", "to_w");
                    reverse = "to_w";
                } else {
                    fixture.add(route("circulate_energy", Map.of(carrier, 1L), Map.of(),
                            Map.of(carrier, 1L), null));
                    members = Set.of("circulate_energy");
                    reverse = "circulate_energy";
                }
                if (makeUp) {
                    fixture.environment("independent_makeup", Map.of(carrier, 1L), null);
                }
                fixture.loops.put("protected", new ClosedLoop("protected", carrier, members,
                        makeUp ? "independent_makeup" : null));
                fixture.meta(carrier, metadata(ResourceClass.CLOSED_LOOP, null, reverse,
                        List.of(), null, null, null, null));
                fixture.add(route("withdraw", Map.of(carrier, 1L, SAFE, 1L), Map.of(),
                        Map.of(PRODUCT, 1L), null)).root(PRODUCT);
                var result = fixture.audit();
                String label = carrier.key() + " make-up=" + makeUp;
                yes(label + " is available at T0", available(result, carrier));
                eq(label + " unlimited carrier verdict at T0", makeUp, renewable(result, carrier));
                eq(label + " external product verdict at T0", makeUp, renewable(result, PRODUCT));
                yes(label + " external feed is in the T0 closure", result.tiers().get(0).mandatory().contains(carrier));
                eq(label + " tier certification", makeUp, tierCertified(result, 0));
                if (!makeUp) {
                    yes(label + " finite withdrawal is specifically rejected at T0",
                            findingAt(result, "finite_dependency", carrier.key(), "T0"));
                }
                exactReserve(label, result, carrier, 2, 1, 1, "established");
            }
        }
    }

    private static void forkedStartup() {
        group("R2 executable forked startup");
        Resource b = item("fork_b");
        Resource c = item("fork_c");
        for (long seed : List.of(1L, 2L)) {
            Fixture fixture = new Fixture().seed(RAW, seed);
            fixture.add(route("fork_b", Map.of(RAW, 1L), Map.of(), Map.of(b, 1L), null));
            fixture.add(route("fork_c", Map.of(RAW, 1L), Map.of(), Map.of(c, 1L), null));
            fixture.add(route("join", Map.of(b, 1L, c, 1L), Map.of(), Map.of(RAW, 2L), null));
            fixture.loops.put("fork", new ClosedLoop("fork", RAW, Set.of("fork_b", "fork_c", "join"), null));
            fixture.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "join",
                    List.of(), null, null, null, null)).root(RAW);
            var result = fixture.audit();
            yes("fork " + seed + " has qualitative T0 availability of both branches",
                    available(result, b) && available(result, c));
            no("fork " + seed + " never creates unlimited A", renewable(result, RAW));
            eq("fork " + seed + " executable T0 loop verdict", seed == 2,
                    result.tiers().get(0).renewableRoutes().containsAll(Set.of("fork_b", "fork_c", "join")));
            eq("fork " + seed + " tier certification", seed == 2, tierCertified(result, 0));
            exactReserve("fork " + seed, result, RAW, seed, 2, seed - 2,
                    seed == 2 ? "established" : "insufficient");
        }
        Fixture ordered = new Fixture().seed(RAW, 1);
        ordered.add(route("a_spend_first", Map.of(RAW, 1L), Map.of(), Map.of(b, 1L), null));
        ordered.add(route("b_return_first", Map.of(RAW, 1L, SAFE, 1L), Map.of(),
                Map.of(RAW, 1L, c, 1L), null));
        ordered.add(route("c_join", Map.of(b, 1L, c, 1L), Map.of(), Map.of(RAW, 1L), null));
        ordered.loops.put("ordered", new ClosedLoop("ordered", RAW,
                Set.of("a_spend_first", "b_return_first", "c_join"), null));
        ordered.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "c_join",
                List.of(), null, null, null, null)).root(RAW);
        var result = ordered.audit();
        yes("non-lexical startup ordering certifies the seeded T0 loop", tierCertified(result, 0));
        yes("non-lexical startup ordering establishes all repeated routes",
                result.tiers().get(0).renewableRoutes()
                        .containsAll(Set.of("a_spend_first", "b_return_first", "c_join")));
        exactReserve("non-lexical startup", result, RAW, 1, 1, 0, "established");
    }

    private static void optionalOnlyProduction() {
        group("R3 optional-only canonical closure");
        for (boolean declared : List.of(false, true)) {
            Fixture fixture = new Fixture().seed(RAW, 2);
            fixture.add(route("optional_only", Map.of(RAW, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
            fixture.policy("optional_only", 0, true);
            fixture.meta(PRODUCT, metadata(ResourceClass.BOOTSTRAP, null, declared ? "optional_only" : null,
                    List.of(), null, null, null, null)).root(PRODUCT);
            var result = fixture.audit();
            yes("optional canonical=" + declared + " is qualitatively available", available(result, PRODUCT));
            no("optional canonical=" + declared + " is renewable at T0", renewable(result, PRODUCT));
            yes("optional canonical=" + declared + " retains RAW in T0 closure",
                    result.tiers().get(0).mandatory().contains(RAW));
            yes("optional canonical=" + declared + " retains RAW net consumption",
                    result.tiers().get(0).recurring().contains(RAW));
            yes("optional canonical=" + declared + " has a precise canonical violation",
                    findingAt(result, declared ? "canonical_optional" : "canonical_route_missing",
                            PRODUCT.key(), declared ? "optional_only" : "T0"));
            yes("optional canonical=" + declared + " exposes finite RAW at T0",
                    findingAt(result, "finite_dependency", RAW.key(), "T0"));
            no("optional canonical=" + declared + " cannot certify T0", tierCertified(result, 0));
            exactReserve("optional canonical=" + declared, result, RAW, 2, 0, 2, "established");
        }
    }

    private static void sharedHandlingBudget() {
        group("R4 handling shares exact construction budget");
        for (boolean treatment : List.of(false, true)) {
            for (long seed : List.of(1L, 2L)) {
                Fixture fixture = new Fixture().seed(RAW, seed);
                fixture.add(route("build_machine", Map.of(RAW, 1L), Map.of(), Map.of(MACHINE, 1L), null));
                fixture.add(route("build_container", Map.of(RAW, 1L), Map.of(), Map.of(TOOL, 1L), null));
                fixture.machines.put("PRODUCER", new Machine("PRODUCER", MACHINE, 0));
                fixture.environment("hazardous_product", Map.of(PRODUCT, 1L, SIDE, 1L), "PRODUCER").root(PRODUCT);
                Handling handling = new Handling(Action.CONTAIN, false, true, TOOL, null);
                if (treatment) {
                    fixture.machines.put("TREATMENT", new Machine("TREATMENT", TOOL, 0));
                    fixture.add(route("treat", Map.of(SIDE, 1L, SAFE, 1L), Map.of(),
                            Map.of(item("treated"), 1L), "TREATMENT"));
                    handling = new Handling(Action.TREAT, false, true, null, "treat");
                }
                fixture.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.WASTE_HAZARDOUS,
                        "hazardous_product", List.of(), handling, null, null, null));
                var result = fixture.audit();
                String label = (treatment ? "treatment" : "containment") + " seed=" + seed;
                yes(label + " has independent qualitative machine/container availability",
                        available(result, MACHINE) && available(result, TOOL));
                yes(label + " includes handling construction in T0 closure",
                        result.tiers().get(0).mandatory().contains(TOOL));
                eq(label + " tier certification", seed == 2, tierCertified(result, 0));
                exactReserve(label, result, RAW, seed, 2, seed - 2,
                        seed == 2 ? "established" : "insufficient");
                eq(label + " exact starvation finding", seed == 1,
                        findingAt(result, "bootstrap_starvation", RAW.key(), "T0"));
            }
        }
    }

    private static void loopEnergy() {
        group("R5 finite or manual loop energy never throws");
        for (String fuel : List.of("finite", "manual", "renewable")) {
            Fixture fixture = new Fixture().seed(WATER, 1);
            if (fuel.equals("finite")) {
                fixture.seed(ENERGY, 2);
            } else if (fuel.equals("manual")) {
                fixture.terminal("manual_fu", ENERGY, TerminalKind.MANUAL_LABOUR, false, true);
                fixture.meta(ENERGY, metadata(ResourceClass.BOOTSTRAP, null, null,
                        List.of(), null, null, null, null));
            } else {
                fixture.environment("power", Map.of(ENERGY, 1L), null);
            }
            fixture.add(route("powered_boil", Map.of(WATER, 1L, ENERGY, 1L), Map.of(),
                    Map.of(STEAM, 1L), null));
            fixture.add(route("powered_return", Map.of(STEAM, 1L), Map.of(), Map.of(WATER, 1L), null));
            fixture.loops.put("powered", new ClosedLoop("powered", WATER,
                    Set.of("powered_boil", "powered_return"), null));
            fixture.meta(WATER, metadata(ResourceClass.CLOSED_LOOP, null, "powered_return",
                    List.of(), null, null, null, null)).root(WATER);
            var result = fixture.audit();
            yes(fuel + " energy loop returns an audit with T0 availability", available(result, WATER));
            eq(fuel + " energy loop repeated-operation verdict", fuel.equals("renewable"),
                    result.tiers().get(0).renewableRoutes().contains("powered_boil"));
            eq(fuel + " energy loop tier certification", fuel.equals("renewable"), tierCertified(result, 0));
            eq(fuel + " energy loop has a final energy-specific finding", !fuel.equals("renewable"),
                    findingAt(result, "loop_makeup_not_renewable", "powered", "T0/" + ENERGY.key()));
            if (fuel.equals("finite")) {
                exactReserve("finite FU", result, ENERGY, 2, 1, 1, "established");
                exactReserve("finite FU carrier", result, WATER, 1, 1, 0, "established");
            } else if (fuel.equals("renewable")) {
                exactReserve("renewable FU carrier", result, WATER, 1, 1, 0, "established");
            }
        }
    }

    private static void normalizedLoops() {
        group("R6 normalized multi-route coefficients and executable seeds");
        Resource b = item("normalized_b");
        Resource c = item("normalized_c");
        Fixture gain = new Fixture().seed(RAW, 1);
        gain.add(route("gain_a", Map.of(RAW, 1L), Map.of(), Map.of(b, 2L), null));
        gain.add(route("gain_b", Map.of(b, 1L), Map.of(), Map.of(c, 1L), null));
        gain.add(route("gain_c", Map.of(c, 2L), Map.of(), Map.of(RAW, 2L), null));
        gain.loops.put("gain", new ClosedLoop("gain", RAW, Set.of("gain_a", "gain_b", "gain_c"), null));
        gain.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "gain_c",
                List.of(), null, null, null, null)).root(RAW);
        var result = gain.audit();
        yes("1:2:1 gain has a real available T0 seed", available(result, RAW));
        no("1:2:1 gain does not become renewable inventory", renewable(result, RAW));
        yes("1:2:1 material gain is normalized and review-visible",
                findingAt(result, "resource_positive_cycle", RAW.key(), "gain_a+gain_b+gain_c"));
        no("1:2:1 gain cannot certify T0", tierCertified(result, 0));
        exactReserve("1:2:1 gain", result, RAW, 1, 1, 0, "established");
        for (long seed : List.of(2L, 4L)) {
            Fixture balanced = new Fixture().seed(RAW, seed);
            balanced.add(route("two_to_three", Map.of(RAW, 2L), Map.of(), Map.of(b, 3L), null));
            balanced.add(route("six_to_four", Map.of(b, 6L), Map.of(), Map.of(RAW, 4L), null));
            balanced.loops.put("normalized", new ClosedLoop("normalized", RAW,
                    Set.of("two_to_three", "six_to_four"), null));
            balanced.meta(RAW, metadata(ResourceClass.CLOSED_LOOP, null, "six_to_four",
                    List.of(), null, null, null, null)).root(RAW);
            result = balanced.audit();
            no("2:1 balanced " + seed + " never becomes unlimited A", renewable(result, RAW));
            no("2:1 balanced " + seed + " is not a carrier gain", hasRule(result, "closed_loop_gain"));
            no("2:1 balanced " + seed + " is not a positive cycle", hasRule(result, "resource_positive_cycle"));
            eq("2:1 balanced " + seed + " executable repeated-route verdict", seed == 4,
                    result.tiers().get(0).renewableRoutes().containsAll(Set.of("two_to_three", "six_to_four")));
            eq("2:1 balanced " + seed + " tier certification", seed == 4, tierCertified(result, 0));
            exactReserve("2:1 balanced " + seed, result, RAW, seed, 4, seed - 4,
                    seed == 4 ? "established" : "insufficient");
        }
    }

    private static void sequentialReturnedSeed() {
        group("R7 sequential returned seed is reused");
        Resource piece = item("piece");
        for (long seed : List.of(0L, 1L)) {
            Fixture fixture = new Fixture();
            if (seed > 0) {
                fixture.seed(WATER, seed);
            }
            fixture.add(route("pieces", Map.of(WATER, 1L, SAFE, 1L), Map.of(),
                    Map.of(WATER, 1L, piece, 1L), null));
            fixture.meta(WATER, metadata(ResourceClass.BOOTSTRAP, null, null,
                    List.of(), null, null, null, null));
            fixture.add(route("assemble_two", Map.of(piece, 2L), Map.of(), Map.of(MACHINE, 1L), null));
            fixture.machines.put("ASSEMBLED", new Machine("ASSEMBLED", MACHINE, 0));
            fixture.environment("assembled_work", Map.of(PRODUCT, 1L), "ASSEMBLED").root(PRODUCT);
            var result = fixture.audit();
            eq("returned seed " + seed + " makes the constructed producer available", seed == 1,
                    available(result, MACHINE));
            eq("returned seed " + seed + " product renewability at T0", seed == 1, renewable(result, PRODUCT));
            eq("returned seed " + seed + " tier certification", seed == 1, tierCertified(result, 0));
            exactReserve("returned seed " + seed, result, WATER, seed, 1, seed - 1,
                    seed == 1 ? "established" : "insufficient");
        }
    }

    private static void convergedMakeUp() {
        group("R8 final make-up findings and order invariance");
        List<SustainabilityAudit.Result> results = new ArrayList<>();
        for (boolean reversed : List.of(false, true)) {
            Fixture fixture = new Fixture().seed(WATER, 2);
            Resource x = item("makeup_x");
            Resource y = item("makeup_y");
            String source = reversed ? "a_source" : "z_source";
            String transform = reversed ? "z_transform" : "a_transform";
            fixture.environment(source, Map.of(x, 1L), null);
            fixture.add(route(transform, Map.of(x, 1L), Map.of(), Map.of(y, 1L), null));
            fixture.add(route("m_makeup", Map.of(y, 1L), Map.of(), Map.of(WATER, 1L), null));
            fixture.add(route("boil_two", Map.of(WATER, 2L), Map.of(), Map.of(STEAM, 1L), null));
            fixture.add(route("return_one", Map.of(STEAM, 1L), Map.of(), Map.of(WATER, 1L), null));
            fixture.loops.put("multi_hop", new ClosedLoop("multi_hop", WATER,
                    Set.of("boil_two", "return_one"), "m_makeup"));
            fixture.meta(WATER, metadata(ResourceClass.LOSSY_LOOP, null, "return_one",
                    List.of(), null, null, null, null)).root(WATER);
            var result = fixture.audit();
            results.add(result);
            yes("make-up order=" + reversed + " establishes renewable Y at T0", renewable(result, y));
            yes("make-up order=" + reversed + " includes Y in the T0 recurring closure",
                    result.tiers().get(0).mandatory().contains(y) && result.tiers().get(0).recurring().contains(y));
            yes("make-up order=" + reversed + " traces the actual X source in T0 closure",
                    result.tiers().get(0).mandatory().contains(x));
            yes("make-up order=" + reversed + " independently renews W at T0", renewable(result, WATER));
            yes("make-up order=" + reversed + " certifies the T0 fixture", tierCertified(result, 0));
            no("make-up order=" + reversed + " has no stale T0 make-up finding",
                    findingAt(result, "loop_makeup_not_renewable", "multi_hop", "T0/" + WATER.key()));
            no("make-up order=" + reversed + " has no stale make-up finding at any tier",
                    hasRule(result, "loop_makeup_not_renewable"));
            exactReserve("multi-hop make-up " + reversed, result, WATER, 2, 2, 0, "established");
        }
        eq("renaming source/transform preserves non-environment T0 renewable facts",
                results.get(0).tiers().get(0).renewable().stream().filter(resource -> resource.kind() != Kind.ENVIRONMENT).toList(),
                results.get(1).tiers().get(0).renewable().stream().filter(resource -> resource.kind() != Kind.ENVIRONMENT).toList());
        eq("renaming preserves the exact T0 construction reserve",
                results.get(0).tiers().get(0).reserves(), results.get(1).tiers().get(0).reserves());
    }

    private static boolean findingAt(SustainabilityAudit.Result result, String rule, String subject, String detail) {
        return result.findings().findings().stream().anyMatch(finding -> finding.rule().equals(rule)
                && finding.subject().equals(subject) && finding.detail().equals(detail));
    }

    private static boolean tierCertified(SustainabilityAudit.Result result, int tier) {
        String name = "T" + tier;
        return result.tiers().get(tier).unknown() == null && result.findings().findings().stream().noneMatch(finding -> {
            if (finding.subject().matches("T\\d+") && !finding.subject().equals(name)) {
                return false;
            }
            return !finding.detail().matches("T\\d+(?:/.*)?")
                    || finding.detail().equals(name) || finding.detail().startsWith(name + "/");
        });
    }

    private static void exactReserve(String what, SustainabilityAudit.Result result, Resource resource,
                                     long guaranteed, long required, long margin, String status) {
        SustainabilityAudit.Reserve actual = result.tiers().get(0).reserves().stream()
                .filter(value -> value.resource().equals(resource)).findFirst().orElse(null);
        eq(what + " exact T0 reserve", new SustainabilityAudit.Reserve(resource, guaranteed, required, margin, status), actual);
    }

    private static void modelContracts() {
        group("typed model contracts");
        eq("seven canonical resource classes", 7, ResourceClass.values().length);
        eq("ten canonical side-stream classes", 10, SideStreamClass.values().length);
        throwsArgument("negative authority batches are rejected", () -> route("invalid",
                Map.of(RAW, -1L), Map.of(), Map.of(PRODUCT, 1L), null));
        throwsArgument("invalid physical ratings are rejected", () -> new Resource(Kind.FLUID, "fixture:water", 2, "items"));
        no("Matrix physical ratings never collapse", new Resource(Kind.ITEM, "fixture:matrix", 1, "items")
                .equals(new Resource(Kind.ITEM, "fixture:matrix", 2, "items")));
        ProcessRecipe recipe = new ProcessRecipe("fixture/adapter", MachineFamily.CASTER,
                List.of(IngredientSpec.fluid("fixture:molten", 144),
                        IngredientSpec.matrix("fixture:matrix", FluxTier.F2, 3)),
                List.of(OutputSpec.matrix("fixture:matrix", FluxTier.F3, 2),
                        OutputSpec.ventedFluid("fixture:gas", 17)),
                1420, "REDUCING", "STIRRED", 7, 11,
                List.of(IngredientSpec.item("fixture:mould", 1)), "fixture_only");
        Route adapted = AuthorityAdapters.process(recipe);
        eq("process input batch is read from the authority", 144L,
                adapted.inputs().get(new Resource(Kind.FLUID, "fixture:molten")));
        eq("process energy is exact ticks times FU/t", 77L, adapted.inputs().get(ENERGY));
        eq("the original draw rate is retained", 11L, adapted.fuPerTick());
        eq("the original duration is retained", 7L, adapted.durationTicks());
        yes("physical input ratings survive", adapted.inputs().containsKey(
                new Resource(Kind.ITEM, "fixture:matrix", 2, "items")));
        yes("physical output ratings survive", adapted.outputs().containsKey(
                new Resource(Kind.ITEM, "fixture:matrix", 3, "items")));
        eq("non-consumed catalysts are separate", 1L, adapted.catalysts().get(item("mould")));
        eq("conditions retain the live temperature", "1420.0", adapted.conditions().get("temperature_c"));
        eq("conditions retain the live atmosphere", "REDUCING", adapted.conditions().get("atmosphere"));
        eq("conditions retain the live agitation", "STIRRED", adapted.conditions().get("agitation"));
        no("a discharged output cannot become a usable feed", adapted.retainedOutputs().containsKey(
                new Resource(Kind.FLUID, "fixture:gas")));
        Route returned = route("net", Map.of(WATER, 1000L), Map.of(), Map.of(WATER, 750L, PRODUCT, 1L), null);
        eq("same-route return reduces consumption to its exact net", 250L, returned.netInputs().get(WATER));
        Route catalyst = route("carrier", Map.of(WATER, 1000L), Map.of(), Map.of(WATER, 1000L), null);
        yes("fully returned input has no net consumption", catalyst.netInputs().isEmpty());
        for (Class<?> type : List.of(ResourceMetadata.class, RoutePolicy.class, ClosedLoop.class,
                Handling.class, Recycling.class, Machine.class, TierPlan.class)) {
            no(type.getSimpleName() + " has no copied recipe quantities",
                    java.util.Arrays.stream(type.getRecordComponents()).anyMatch(component ->
                            component.getName().matches("(?i).*(quantity|amount|yield|resultCount|duration|fuPerTick).*")));
        }
    }

    private static void fixedPoints() {
        group("unseeded and producer cycles");
        Fixture cycle = new Fixture();
        cycle.add(route("a_to_b", Map.of(RAW, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        cycle.add(route("b_to_a", Map.of(PRODUCT, 1L), Map.of(), Map.of(RAW, 1L), null));
        cycle.meta(RAW, renewable("b_to_a")).meta(PRODUCT, renewable("a_to_b")).root(RAW);
        var result = cycle.audit();
        no("an unseeded cycle is not available", available(result, RAW));
        no("reachability does not make an unseeded cycle renewable", renewable(result, RAW));
        yes("unseeded mandatory cycle is rejected", hasRule(result, "mandatory_unavailable"));
        yes("unseeded renewable declaration is rejected", hasRule(result, "renewable_claim"));

        Fixture self = producer(0, 1, 0);
        result = self.audit();
        no("a self-constructing producer is unavailable", available(result, MACHINE));
        no("its environmental material is not reachable through the nonexistent machine", renewable(result, RAW));
        yes("the construction cycle is failure-visible", hasRule(result, "self_constructing_producer"));
        Fixture constructed = producer(1, 1, 0);
        result = constructed.audit();
        yes("a real construction seed makes the producer available", available(result, MACHINE));
        yes("a seeded environmental producer can operate renewably", renewable(result, RAW));
        no("a seeded producer is not called self-constructing", hasRule(result, "self_constructing_producer"));

        group("seeded and environmental closed loops");
        result = loop(true, false, 0).audit();
        yes("seeded closed-loop carrier is available", available(result, WATER));
        no("finite closed-loop inventory is not unlimited water supply", renewable(result, WATER));
        no("finite closed-loop inventory is not unlimited intermediate supply", renewable(result, STEAM));
        yes("a seeded closed loop has executable repeated routes",
                result.tiers().get(0).renewableRoutes().containsAll(Set.of("boil", "condense")));
        exactReserve("seeded closed loop", result, WATER, 1, 1, 0, "established");
        no("a closed carrier is not a finite recurring dependency", hasRule(result, "finite_dependency"));
        no("a seeded balanced loop is not rejected", hasRule(result, "unseeded_loop"));
        no("balanced recycling is not a positive-gain cycle", hasRule(result, "resource_positive_cycle"));
        result = loop(false, false, 0).audit();
        no("declaring a loop does not invent its seed", renewable(result, WATER));
        yes("missing loop seed is explicit", hasRule(result, "unseeded_loop"));
        result = loop(false, true, 0).audit();
        yes("environmental-terminal loop is available without a finite seed", available(result, WATER));
        yes("environmental-terminal loop is accepted", renewable(result, WATER));
        no("an environmental loop is not bootstrap starvation", hasRule(result, "bootstrap_starvation"));
        Fixture optionalLoop = loop(true, false, 0).policy("boil", 0, true);
        yes("canonical loops reject optional members", hasRule(optionalLoop.audit(), "loop_optional_route"));
        no("balanced canonical loops accept non-optional members", hasRule(loop(true, false, 0).audit(), "loop_optional_route"));
        Fixture missingMember = loop(true, false, 0);
        missingMember.loops.put("water", new ClosedLoop("water", WATER, Set.of("missing"), null));
        yes("loop members must exist in live authority data", hasRule(missingMember.audit(), "loop_route_missing"));
        no("real loop members satisfy authority references", hasRule(loop(true, false, 0).audit(), "loop_route_missing"));
        Fixture missingCarrier = loop(true, false, 0).seed(TOOL, 1);
        missingCarrier.loops.put("water", new ClosedLoop("water", TOOL, Set.of("boil", "condense"), null));
        yes("loop carrier must participate in its batches", hasRule(missingCarrier.audit(), "loop_carrier_missing"));
        no("an actual carrier satisfies the loop membership rule", hasRule(loop(true, false, 0).audit(), "loop_carrier_missing"));
        Fixture carrierGain = new Fixture().seed(WATER, 1);
        carrierGain.add(route("boil", Map.of(WATER, 1L), Map.of(), Map.of(STEAM, 1L), null));
        carrierGain.add(route("condense", Map.of(STEAM, 1L), Map.of(), Map.of(WATER, 2L), null));
        carrierGain.loops.put("water", new ClosedLoop("water", WATER, Set.of("boil", "condense"), null));
        yes("a declared loop cannot mint its carrier", hasRule(carrierGain.audit(), "closed_loop_gain"));
        no("a balanced carrier satisfies the no-gain rule", hasRule(loop(true, false, 0).audit(), "closed_loop_gain"));

        group("net consumption and operational seed");
        Fixture finiteNet = new Fixture().seed(WATER, 2);
        finiteNet.add(route("partly_returned", Map.of(WATER, 2L), Map.of(),
                Map.of(WATER, 1L, PRODUCT, 1L), null));
        finiteNet.meta(PRODUCT, renewable("partly_returned")).root(PRODUCT);
        result = finiteNet.audit();
        no("a partial return does not hide a finite net input", renewable(result, PRODUCT));
        yes("partial-return finite consumption is detected", hasRule(result, "finite_dependency"));
        Fixture unseededReturn = new Fixture();
        unseededReturn.add(route("fully_returned", Map.of(WATER, 1L), Map.of(),
                Map.of(WATER, 1L, PRODUCT, 1L), null));
        unseededReturn.root(PRODUCT);
        no("a fully returned carrier still requires availability", renewable(unseededReturn.audit(), PRODUCT));
        Fixture seededReturn = new Fixture().seed(WATER, 1);
        seededReturn.add(route("returned_no_source", Map.of(WATER, 1L), Map.of(),
                Map.of(WATER, 1L, PRODUCT, 1L), null)).root(PRODUCT);
        result = seededReturn.audit();
        yes("a fully returned input is not a matter source", hasRule(result, "zero_input_unjustified"));
        no("a returned seed cannot mint a new material without justification", renewable(result, PRODUCT));
        Fixture returnedWithSource = new Fixture().seed(WATER, 1);
        returnedWithSource.environment("returned_with_source", Map.of(WATER, 1L), Map.of(),
                Map.of(WATER, 1L, PRODUCT, 1L), null).root(PRODUCT);
        result = returnedWithSource.audit();
        yes("a returned carrier can support justified environmental production", renewable(result, PRODUCT));
        no("a non-consumed carrier is not misclassified as newly produced unlimited stock", renewable(result, WATER));
        no("a coherent source satisfies the net-zero feed rule", hasRule(result, "zero_input_unjustified"));
    }

    private static void sourceRules() {
        group("finite manual and vanilla canonical sources");
        Fixture finite = new Fixture().seed(RAW, 1).meta(RAW, renewable(null)).root(RAW);
        var result = finite.audit();
        yes("finite bootstrap contributes availability", available(result, RAW));
        no("finite bootstrap is never a renewable terminal", renewable(result, RAW));
        yes("finite canonical dependency is detected", hasRule(result, "finite_dependency"));
        Fixture internal = new Fixture().environment("internal", Map.of(RAW, 1L), null)
                .meta(RAW, renewable("internal")).root(RAW);
        result = internal.audit();
        yes("a justified internal terminal route is renewable", renewable(result, RAW));
        no("a renewable internal route is not finite", hasRule(result, "finite_dependency"));
        for (TerminalKind kind : List.of(TerminalKind.WORLD_GENERATION, TerminalKind.MOB_DROP,
                TerminalKind.LOOT, TerminalKind.TREE_CROP_GROWTH, TerminalKind.BLOCK_GENERATOR, TerminalKind.TRADING)) {
            Fixture vanilla = new Fixture().terminal("vanilla", RAW, kind, false, false)
                    .meta(RAW, renewable(null)).root(RAW);
            result = vanilla.audit();
            yes(kind + " can be finite availability", available(result, RAW));
            no(kind + " cannot be an unlimited terminal", renewable(result, RAW));
            yes(kind + " canonical dependency is rejected", hasRule(result, "vanilla_dependency"));
        }
        for (TerminalKind kind : List.of(TerminalKind.MANUAL_WORLD, TerminalKind.MANUAL_LABOUR)) {
            Fixture manual = new Fixture().terminal("manual", RAW, kind, false, true)
                    .meta(RAW, renewable(null)).root(RAW);
            result = manual.audit();
            yes(kind + " contributes availability", available(result, RAW));
            no(kind + " never seeds renewability", renewable(result, RAW));
            yes(kind + " is reported as finite/manual dependence", hasRule(result, "finite_dependency"));
        }
        Fixture optional = new Fixture().environment("internal", Map.of(RAW, 1L), null)
                .terminal("optional_tree", RAW, TerminalKind.TREE_CROP_GROWTH, false, false)
                .meta(RAW, renewable("internal")).root(RAW);
        result = optional.audit();
        yes("an optional vanilla alternative does not erase an internal proof", renewable(result, RAW));
        no("optional vanilla alternative is not a canonical violation", hasRule(result, "vanilla_dependency"));

        group("canonical route and ownership rules");
        Fixture routes = new Fixture().environment("main", Map.of(PRODUCT, 1L), null);
        routes.add(route("compatibility", Map.of(RAW, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        routes.policy("compatibility", 0, true).meta(PRODUCT, renewable("main")).root(PRODUCT);
        result = routes.audit();
        yes("an optional alternative is excluded from the canonical closure", renewable(result, PRODUCT));
        no("optional alternative does not demand a second canonical choice", hasRule(result, "canonical_route_required"));
        routes.meta(PRODUCT, renewable("compatibility"));
        result = routes.audit();
        yes("an explicitly optional canonical route is rejected", hasRule(result, "canonical_optional"));
        no("an optional canonical route cannot prove renewability", renewable(result, PRODUCT));
        Fixture ambiguous = new Fixture().environment("one", Map.of(PRODUCT, 1L), null)
                .environment("two", Map.of(PRODUCT, 1L), null).root(PRODUCT);
        yes("multiple non-optional routes require a canonical choice",
                hasRule(ambiguous.audit(), "canonical_route_required"));
        ambiguous.meta(PRODUCT, renewable("one"));
        no("an explicit valid choice resolves ambiguity", hasRule(ambiguous.audit(), "canonical_route_required"));
        ambiguous.meta(PRODUCT, renewable("not_a_producer"));
        yes("a fabricated canonical id is rejected", hasRule(ambiguous.audit(), "canonical_route_invalid"));

        Fixture external = new Fixture();
        external.add(new Route("external", Authority.CRAFTING_JSON, "fixture:external", null,
                Map.of(SAFE, 1L), Map.of(), Map.of(PRODUCT, 1L), Set.of(), 1, 0,
                Map.of(), Set.of(), false, false)).meta(PRODUCT, renewable("external")).root(PRODUCT);
        result = external.audit();
        no("external production is not mod-owned supply", renewable(result, PRODUCT));
        yes("external canonical ownership is detected", hasRule(result, "non_mod_owned_canonical"));
        Fixture transform = new Fixture();
        transform.add(new Route("allowlisted", Authority.VANILLA_TRANSFORMATION, "fixture:resolved_classpath", null,
                Map.of(SAFE, 1L), Map.of(), Map.of(PRODUCT, 1L), Set.of(), 0, 0,
                Map.of(), Set.of(), false, false)).meta(PRODUCT, renewable("allowlisted")).root(PRODUCT);
        yes("verified vanilla transformations may transform internal feeds", renewable(transform.audit(), PRODUCT));
    }

    private static void tagsAndJustifications() {
        group("unresolved and registry-specific tags");
        Resource tag = new Resource(Kind.TAG, "forge:ingots/iron");
        Fixture missing = new Fixture();
        missing.builder.resource(tag);
        var result = missing.audit();
        yes("an unresolved tag is explicitly reported", hasRule(result, "unresolved_tag"));
        yes("unresolved-tag evidence is unknown, not silently a pass", result.findings().findings().stream()
                .anyMatch(finding -> finding.rule().equals("unresolved_tag")
                        && finding.severity() == AuditFinding.Severity.UNKNOWN));
        no("unresolved tags are not invented items", available(result, tag));
        no("unknowns alone do not fail the debt ratchet", !result.findings().ratchetPass());
        Fixture local = new Fixture();
        local.builder.tag(tag, new TagResolution(Set.of(SAFE), true, Set.of(), List.of("fixture:local_tag")));
        local.root(tag);
        result = local.audit();
        yes("a resolved tag can use its actual member", available(result, tag));
        yes("a resolved tag can use a renewable member", renewable(result, tag));
        no("a fully resolved tag is not unresolved", hasRule(result, "unresolved_tag"));
        Resource blockTag = new Resource(Kind.TAG, tag.id(), 0, "blocks");
        local.builder.tag(blockTag, new TagResolution(Set.of(item("rock")), true, Set.of(), List.of("fixture:block_tag")));
        result = local.audit();
        no("item-tag membership does not satisfy a block tag", available(result, blockTag));
        Fixture partial = new Fixture();
        partial.builder.tag(tag, new TagResolution(Set.of(SAFE), false, Set.of("tag:minecraft:planks"),
                List.of("fixture:partial")));
        result = partial.audit();
        yes("partial local membership remains explicitly unresolved", hasRule(result, "unresolved_tag"));
        no("missing nested membership is never invented", available(result, new Resource(Kind.ITEM, "minecraft:oak_planks")));
        Fixture unknownOutput = new Fixture();
        unknownOutput.environment("unresolved_output", Map.of(tag, 1L), null).root(tag);
        no("a producer does not silently resolve its output tag", renewable(unknownOutput.audit(), tag));
        Fixture knownEmpty = new Fixture();
        knownEmpty.builder.tag(tag, new TagResolution(Set.of(), true, Set.of(), List.of("fixture:known_empty")));
        knownEmpty.add(route("empty_required_feed", Map.of(tag, 1L), Map.of(), Map.of(PRODUCT, 1L), null)).root(PRODUCT);
        result = knownEmpty.audit();
        no("a completely resolved empty tag is not mislabeled unresolved", hasRule(result, "unresolved_tag"));
        no("a known empty tag cannot supply a required input", available(result, PRODUCT));
        no("known absence cannot be turned into renewable production", renewable(result, PRODUCT));

        group("environmental justification and automation");
        Fixture zero = new Fixture().add(route("free_matter", Map.of(), Map.of(), Map.of(PRODUCT, 1L), null))
                .meta(PRODUCT, renewable("free_matter")).root(PRODUCT);
        result = zero.audit();
        yes("unjustified zero-input route is detected", hasRule(result, "zero_input_unjustified"));
        no("unjustified zero-input matter is unavailable", available(result, PRODUCT));
        no("unjustified zero-input matter is not renewable", renewable(result, PRODUCT));
        Fixture justified = new Fixture().environment("justified", Map.of(PRODUCT, 1L), null).root(PRODUCT);
        result = justified.audit();
        no("a real environmental explanation satisfies the zero-input rule", hasRule(result, "zero_input_unjustified"));
        yes("a justified environmental route is renewable", renewable(result, PRODUCT));
        for (SourceSite site : SourceSite.values()) {
            Fixture sites = new Fixture();
            Resource environment = new Resource(Kind.ENVIRONMENT, "fixture:" + site.name());
            sites.builder.terminal(new Terminal("site", environment, TerminalKind.ENVIRONMENT,
                    site, true, true, null, 0, "Synthetic site", "fixture:site"));
            sites.add(route("site_route", Map.of(), Map.of(), Map.of(PRODUCT, 1L), null));
            sites.policies.put("site_route", new RoutePolicy(0, false, Set.of("site")));
            yes(site + " can justify an automatable collector", renewable(sites.root(PRODUCT).audit(), PRODUCT));
        }
        Fixture nonAutomated = new Fixture();
        Resource env = new Resource(Kind.ENVIRONMENT, "fixture:manual_site");
        nonAutomated.builder.terminal(new Terminal("manual_site", env, TerminalKind.ENVIRONMENT,
                SourceSite.WATER_BODY, false, true, null, 0, "Synthetic manual collection", "fixture:manual_site"));
        nonAutomated.add(route("manual_collection", Map.of(), Map.of(), Map.of(PRODUCT, 1L), null));
        nonAutomated.policies.put("manual_collection", new RoutePolicy(0, false, Set.of("manual_site")));
        result = nonAutomated.meta(PRODUCT, renewable("manual_collection")).root(PRODUCT).audit();
        yes("a nonautomated site can be available", available(result, PRODUCT));
        no("a nonautomated site cannot qualify as R", renewable(result, PRODUCT));
        yes("manual collector dependence stays failure-visible", hasRule(result, "finite_dependency"));
        Fixture invalid = new Fixture();
        invalid.builder.terminal(new Terminal("no_site", env, TerminalKind.ENVIRONMENT, null,
                true, true, null, 0, "No actual site", "fixture:no_site"));
        invalid.add(route("invalid_environment", Map.of(), Map.of(), Map.of(PRODUCT, 1L), null));
        invalid.policies.put("invalid_environment", new RoutePolicy(0, false, Set.of("no_site")));
        result = invalid.audit();
        yes("missing SourceSite is rejected", hasRule(result, "terminal_invalid"));
        yes("invalid environmental reference cannot justify a route", hasRule(result, "environmental_justification_invalid"));
        no("invalid environmental references cannot mint renewability", renewable(result, PRODUCT));
    }

    private static void constructionReserve() {
        group("exact construction guarantees and recovery margin");
        var result = twoMachines(5).audit();
        yes("qualitative availability alone would allow both machines", available(result, PRODUCT));
        yes("insufficient exact construction guarantee is detected", hasRule(result, "bootstrap_starvation"));
        SustainabilityAudit.Reserve reserve = result.tiers().get(0).reserves().stream()
                .filter(value -> value.resource().equals(RAW)).findFirst().orElseThrow();
        eq("shared construction requirements spend six units, not three twice for free", 6L, reserve.required());
        eq("insufficient construction margin is negative", -1L, reserve.margin());
        result = twoMachines(6).audit();
        no("the exact construction boundary is accepted", hasRule(result, "bootstrap_starvation"));
        reserve = result.tiers().get(0).reserves().stream().filter(value -> value.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("exact construction reserve reports zero margin", 0L, reserve.margin());
        result = twoMachines(7).audit();
        reserve = result.tiers().get(0).reserves().stream().filter(value -> value.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("positive construction recovery margin is reported", 1L, reserve.margin());

        result = producer(2, 2, 1).audit();
        yes("construction cannot consume its sole operational seed", hasRule(result, "bootstrap_starvation"));
        result = producer(3, 2, 1).audit();
        no("construction plus one initial seed is sufficient", hasRule(result, "bootstrap_starvation"));
        yes("a valid seeded producer has a renewable material proof", renewable(result, RAW));
        reserve = result.tiers().get(0).reserves().stream().filter(value -> value.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("an installed machine is not gratuitously constructed twice", 3L, reserve.required());
        eq("construction and an operational seed report the exact zero margin", 0L, reserve.margin());

        Fixture manual = producer(0, 2, 1).terminal("manual_raw", RAW, TerminalKind.MANUAL_WORLD, false, true);
        result = manual.audit();
        yes("manual gathering is explicit reserve uncertainty", hasRule(result, "construction_reserve_unknown"));
        eq("manual gathering is never added to exact guarantees", 0, result.tiers().get(0).reserves().size());

        group("conditional one-shot quest claims are not initial guarantees");
        Fixture oneReward = rewardProducer(1);
        result = oneReward.audit();
        yes("a one-shot reward cannot be reused to construct a two-unit machine",
                hasRule(result, "bootstrap_starvation"));
        reserve = result.tiers().get(0).reserves().stream().filter(value -> value.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("a quest claim never becomes an initial guarantee", 0L, reserve.guaranteed());
        eq("a claimed reward comes from its actual authority output", 1L, reserve.claimedRewards());
        result = rewardProducer(2).audit();
        no("a sufficient one-shot reward can establish the producer", hasRule(result, "bootstrap_starvation"));
        reserve = result.tiers().get(0).reserves().stream().filter(value -> value.resource().equals(RAW))
                .findFirst().orElseThrow();
        eq("a changed reward is still conditional rather than initially guaranteed", 0L, reserve.guaranteed());
        eq("a changed claimed quantity is read, not copied into annotations", 2L, reserve.claimedRewards());

        Fixture circularReward = producer(0, 1, 0);
        circularReward.add(new Route("quest/after_machine", Authority.QUEST_REWARD, "fixture:quest", null,
                Map.of(), Map.of(MACHINE, 1L), Map.of(RAW, 1L), Set.of(), 0, 0,
                Map.of(), Set.of(), true, true));
        result = circularReward.audit();
        no("a future quest reward cannot supply its own prerequisite", available(result, MACHINE));
        no("a conditional quest reward is not a free starting guarantee",
                result.tiers().get(0).reserves().stream().anyMatch(value -> value.resource().equals(RAW)));

        group("selected physical objectives and tiers");
        Fixture quests = new Fixture().environment("objective", Map.of(PRODUCT, 1L), null);
        quests.builder.objective(new Objective("required", PRODUCT, 5, Set.of(), null, "fixture:quest"));
        quests.builder.objective(new Objective("optional", RAW, 1, Set.of(), null, "fixture:optional_quest"));
        quests.requiredQuests.add("required");
        result = quests.audit();
        yes("only selected quest subjects become mandatory", result.tiers().get(0).mandatory().contains(PRODUCT));
        no("an optional quest is never made mandatory by the adapter", result.tiers().get(0).mandatory().contains(RAW));
        quests.requiredQuests.add("missing");
        yes("missing mandatory objective authority is rejected", hasRule(quests.audit(), "mandatory_objective_unknown"));
        Fixture later = new Fixture().environment("later", Map.of(PRODUCT, 1L), null).root(PRODUCT);
        later.policy("later", 1, false);
        result = later.audit();
        no("a later route is not available early", available(result, PRODUCT));
        yes("a later route becomes available at its own tier", result.tiers().get(1).available().contains(PRODUCT));
        eq("unauthored later tiers remain unknown", "Tier roots and exit are not authored.", result.tiers().get(1).unknown());
        later.policy("later", 0, false);
        yes("unlocking at the actual tier restores availability", available(later.audit(), PRODUCT));
    }

    private static void recycling() {
        group("normalized recycling and positive-gain review");
        Fixture bounded = recyclingFixture(2);
        var result = bounded.audit();
        no("equal normalized recycling does not mint raw stock", hasRule(result, "resource_positive_recycling"));
        Fixture gain = recyclingFixture(3);
        result = gain.audit();
        yes("resource-positive recycling is detected", hasRule(result, "resource_positive_recycling"));
        no("resource-positive recycling is not a green ratchet", result.findings().ratchetPass());
        gain.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.WASTE_RECOVERABLE,
                "forward", List.of(), dispose(), new Recycling("missing", null), null, null));
        yes("a made-up recovery route is rejected", hasRule(gain.audit(), "recovery_unimplemented"));
        Fixture wrongReturn = recyclingFixture(2);
        wrongReturn.add(route("foreign_recovery", Map.of(SIDE, 3L), Map.of(), Map.of(TOOL, 1L), null));
        wrongReturn.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.WASTE_RECOVERABLE,
                "forward", List.of(), dispose(), new Recycling("foreign_recovery", null), null, null));
        yes("recovery cannot return a resource absent from forward consumption",
                hasRule(wrongReturn.audit(), "resource_positive_recycling"));
        Fixture balanced = new Fixture();
        balanced.add(route("forward", Map.of(RAW, 2L), Map.of(), Map.of(SIDE, 3L), null));
        balanced.add(route("reverse", Map.of(SIDE, 3L), Map.of(), Map.of(RAW, 2L), null));
        no("balanced cycles do not trigger positive-gain review", hasRule(balanced.audit(), "resource_positive_cycle"));
        Fixture cycle = new Fixture();
        cycle.add(route("forward", Map.of(RAW, 2L), Map.of(), Map.of(SIDE, 3L), null));
        cycle.add(route("reverse", Map.of(SIDE, 3L), Map.of(), Map.of(RAW, 3L), null));
        result = cycle.audit();
        yes("undeclared positive-gain cycle is reported for review", hasRule(result, "resource_positive_cycle"));
        yes("positive-gain review has warning severity", result.findings().findings().stream().anyMatch(finding ->
                finding.rule().equals("resource_positive_cycle") && finding.severity() == AuditFinding.Severity.WARNING));
        no("review-only cycle warnings are not invented error debt", !result.findings().ratchetPass());
        Fixture normalized = new Fixture();
        normalized.add(route("forward", Map.of(RAW, 2L), Map.of(), Map.of(SIDE, 3L), null));
        normalized.add(route("reverse", Map.of(SIDE, 2L), Map.of(), Map.of(RAW, 2L), null));
        normalized.add(route("lossy_alternative", Map.of(RAW, 100L), Map.of(), Map.of(SIDE, 1L), null));
        yes("lossy alternatives cannot hide a normalized positive subcycle",
                hasRule(normalized.audit(), "resource_positive_cycle"));
        Fixture energy = new Fixture();
        energy.add(route("energy_forward", Map.of(WATER, 1L), Map.of(), Map.of(STEAM, 1L, ENERGY, 2L), null));
        energy.add(route("energy_reverse", Map.of(STEAM, 1L, ENERGY, 1L), Map.of(), Map.of(WATER, 1L), null));
        yes("positive energy cycles are reported as well as material gains", hasRule(energy.audit(), "resource_positive_cycle"));
    }

    private static void resourceCompatibility() {
        group("ADR-0131 resource compatibility pairs");
        var result = new Fixture().environment("renewable", Map.of(RAW, 1L), null)
                .meta(RAW, renewable("renewable")).root(RAW).audit();
        no("renewable class accepts an internal fixed-point proof", hasRule(result, "renewable_claim"));
        result = new Fixture().seed(RAW, 1).meta(RAW, renewable(null)).root(RAW).audit();
        yes("renewable class rejects finite provenance", hasRule(result, "renewable_claim"));
        no("bootstrap class accepts exact establishment supply", hasRule(twoMachines(6).audit(), "bootstrap_starvation"));
        yes("bootstrap class rejects starvation", hasRule(twoMachines(5).audit(), "bootstrap_starvation"));
        no("closed-loop class accepts a seeded balanced loop", hasRule(loop(true, false, 0).audit(), "renewable_claim"));
        yes("closed-loop class rejects an unseeded loop", hasRule(loop(false, false, 0).audit(), "renewable_claim"));
        Fixture missingLoop = loop(true, false, 0);
        missingLoop.loops.clear();
        yes("closed-loop class requires its declaration", hasRule(missingLoop.audit(), "loop_declaration_missing"));

        Fixture catalyst = new Fixture().seed(TOOL, 1);
        catalyst.add(route("catalysed", Map.of(SAFE, 1L), Map.of(TOOL, 1L), Map.of(PRODUCT, 1L), null));
        catalyst.meta(TOOL, metadata(ResourceClass.CATALYTIC, null, null, List.of(), null, null, null, null))
                .meta(PRODUCT, renewable("catalysed")).root(PRODUCT);
        result = catalyst.audit();
        yes("finite non-consumed tool can support renewable operation", renewable(result, PRODUCT));
        no("a genuine catalyst is not marked consumed", hasRule(result, "catalyst_consumed"));
        no("catalytic classification has an actual non-consumed use", hasRule(result, "catalyst_use_missing"));
        Fixture consumed = new Fixture().seed(TOOL, 1);
        consumed.add(route("consumed_tool", Map.of(SAFE, 1L, TOOL, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        consumed.meta(TOOL, metadata(ResourceClass.CATALYTIC, null, null, List.of(), null, null, null, null)).root(PRODUCT);
        yes("catalytic class rejects net consumption", hasRule(consumed.audit(), "catalyst_consumed"));
        yes("catalytic classification cannot invent non-consumed use", hasRule(consumed.audit(), "catalyst_use_missing"));

        result = loop(true, true, 1).audit();
        yes("lossy loop closes with renewable make-up", renewable(result, WATER));
        no("renewable make-up satisfies the loss rule", hasRule(result, "loop_makeup_not_renewable"));
        Fixture noMakeUp = loop(true, false, 1);
        result = noMakeUp.audit();
        no("lossy loop without replacement is not sustainable", renewable(result, WATER));
        yes("missing renewable make-up is rejected", hasRule(result, "loop_makeup_not_renewable"));
        Fixture mislabeledClosed = loop(true, true, 1);
        mislabeledClosed.meta(WATER, metadata(ResourceClass.CLOSED_LOOP, null, "condense",
                List.of(), null, null, null, null));
        yes("closed-loop class cannot hide actual loss", hasRule(mislabeledClosed.audit(), "closed_loop_loss"));
        Fixture mislabeledLoss = loop(true, false, 0);
        mislabeledLoss.meta(WATER, metadata(ResourceClass.LOSSY_LOOP, null, "condense",
                List.of(), null, null, null, null));
        yes("lossy-loop class needs an actual loss", hasRule(mislabeledLoss.audit(), "lossy_loop_without_loss"));
        for (ResourceClass type : List.of(ResourceClass.FINITE_OPTIONAL, ResourceClass.FORBIDDEN_DEPENDENCY)) {
            String rule = type == ResourceClass.FINITE_OPTIONAL ? "finite_optional_required" : "forbidden_dependency";
            Fixture fixture = new Fixture().seed(RAW, 1)
                    .meta(RAW, metadata(type, null, null, List.of(), null, null, null, null));
            no(type + " remains valid outside the mandatory closure", hasRule(fixture.audit(), rule));
            fixture.root(RAW);
            yes(type + " rejects mandatory use", hasRule(fixture.audit(), rule));
        }
        Fixture early = new Fixture().environment("first_use", Map.of(PRODUCT, 1L), null).root(PRODUCT);
        early.meta(PRODUCT, new ResourceMetadata(ResourceClass.RENEWABLE, null, 1, 0, "first_use",
                List.of(), null, null, null, null));
        yes("declared first-use tier cannot be silently bypassed", hasRule(early.audit(), "resource_used_before_tier"));
        early.meta(PRODUCT, renewable("first_use"));
        no("honest first-use tier passes its compatibility rule", hasRule(early.audit(), "resource_used_before_tier"));
    }

    private static void sideStreamCompatibility() {
        group("ADR-0131 current and future consumer pairs");
        Fixture current = sideStream(SideStreamClass.BYPRODUCT_CURRENT, reuse());
        current.add(route("consume_side", Map.of(SIDE, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        no("current by-product accepts a real consumer", hasRule(current.audit(), "current_consumer_missing"));
        Fixture noConsumer = sideStream(SideStreamClass.BYPRODUCT_CURRENT, reuse());
        yes("current by-product rejects a fictional consumer", hasRule(noConsumer.audit(), "current_consumer_missing"));
        current.policy("consume_side", 1, false);
        yes("a later consumer does not count as current at production", hasRule(current.audit(), "current_consumer_unavailable"));
        current.policy("consume_side", 0, false);
        no("current consumer available at production is accepted", hasRule(current.audit(), "current_consumer_unavailable"));
        Fixture future = future();
        no("future by-product accepts a committed plan", hasRule(future.audit(), "future_plan_missing"));
        future.plans.clear();
        yes("future by-product rejects a nonexistent plan", hasRule(future.audit(), "future_plan_missing"));
        future = future().root(SIDE);
        yes("future feedstock cannot be required early", hasRule(future.audit(), "future_required_early"));
        future = future();
        future.ledger.put(2, new TierPlan(2, Set.of(SIDE), Set.of(), null, null));
        var result = future.audit();
        no("future feedstock can be required at its committed consumer tier", hasRule(result, "future_required_early"));

        group("ADR-0131 optional inert loss emission and uncertain-use pairs");
        for (SideStreamClass side : List.of(SideStreamClass.BYPRODUCT_OPTIONAL, SideStreamClass.WASTE_INERT,
                SideStreamClass.PROCESS_LOSS, SideStreamClass.EMISSION, SideStreamClass.POTENTIAL_FUTURE_USE)) {
            String rule = switch (side) {
                case BYPRODUCT_OPTIONAL -> "optional_side_stream_required";
                case WASTE_INERT -> "inert_waste_required";
                case POTENTIAL_FUTURE_USE -> "potential_use_required";
                default -> "non_material_required";
            };
            Fixture fixture = sideStream(side, dispose());
            if (side == SideStreamClass.POTENTIAL_FUTURE_USE) {
                fixture.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, side, "side_source", List.of(),
                        dispose(), null, null, "Uncertain; no use is promised."));
            }
            no(side + " stays valid without a forced consumer", hasRule(fixture.audit(), rule));
            no(side + " does not require an invented current consumer", hasRule(fixture.audit(), "current_consumer_missing"));
            fixture.root(SIDE);
            yes(side + " rejects forced mandatory use", hasRule(fixture.audit(), rule));
        }
        Fixture inert = sideStream(SideStreamClass.WASTE_INERT, dispose());
        no("inert waste may be discarded", hasRule(inert.audit(), "inert_waste_not_discardable"));
        inert.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.WASTE_INERT, "side_source",
                List.of(), new Handling(Action.STORE, false, false, null, null), null, null, null));
        yes("inert waste cannot demand unlimited hoarding", hasRule(inert.audit(), "inert_waste_not_discardable"));
        Fixture potential = sideStream(SideStreamClass.POTENTIAL_FUTURE_USE, dispose());
        yes("potential future use needs explicit uncertainty", hasRule(potential.audit(), "potential_use_unqualified"));
        potential.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.POTENTIAL_FUTURE_USE,
                "side_source", List.of(), dispose(), null, null, "Uncertain; no committed consumer."));
        no("honestly uncertain potential use is accepted", hasRule(potential.audit(), "potential_use_unqualified"));

        group("ADR-0131 hazardous handling pairs");
        Fixture hazard = sideStream(SideStreamClass.WASTE_HAZARDOUS,
                new Handling(Action.CONTAIN, false, true, TOOL, null)).seed(TOOL, 1);
        no("hazardous waste accepts implemented available containment", hasRule(hazard.audit(), "hazardous_handling"));
        Fixture unsafe = sideStream(SideStreamClass.WASTE_HAZARDOUS, null);
        yes("hazardous output without handling is rejected", hasRule(unsafe.audit(), "hazardous_handling"));
        unsafe = sideStream(SideStreamClass.WASTE_HAZARDOUS,
                new Handling(Action.CONTAIN, false, true, TOOL, null));
        yes("a nonexistent containment item is not implemented handling", hasRule(unsafe.audit(), "hazardous_handling"));
        unsafe = sideStream(SideStreamClass.WASTE_HAZARDOUS,
                new Handling(Action.DISCARD, true, true, TOOL, null)).seed(TOOL, 1);
        yes("raw hazardous disposal is rejected even with a container", hasRule(unsafe.audit(), "hazardous_handling"));
        Fixture treated = sideStream(SideStreamClass.WASTE_HAZARDOUS,
                new Handling(Action.TREAT, false, true, null, "treatment"));
        treated.add(route("treatment", Map.of(SIDE, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        no("hazardous waste accepts actual available treatment", hasRule(treated.audit(), "hazardous_handling"));
        treated.policy("treatment", 1, false);
        yes("hazard handling cannot arrive after first production", hasRule(treated.audit(), "hazardous_handling"));
        treated.policy("treatment", 0, false);
        no("handling at first production satisfies the timing rule", hasRule(treated.audit(), "hazardous_handling"));

        group("ADR-0131 recoverable waste and spent-catalyst pairs");
        no("recoverable waste accepts a real bounded recycling route",
                hasRule(recyclingFixture(2).audit(), "recovery_unimplemented"));
        Fixture recovery = sideStream(SideStreamClass.WASTE_RECOVERABLE, dispose());
        yes("recoverable label without evidence is rejected", hasRule(recovery.audit(), "recovery_unimplemented"));
        recovery.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.WASTE_RECOVERABLE,
                "side_source", List.of(), dispose(), new Recycling(null, 3), null, null));
        no("an honestly planned recovery tier is not an invented current consumer",
                hasRule(recovery.audit(), "recovery_unimplemented"));
        Fixture spent = new Fixture().environment("origin", Map.of(TOOL, 1L), null);
        spent.add(route("spent", Map.of(TOOL, 1L), Map.of(), Map.of(SIDE, 1L), null));
        spent.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.CATALYST_SPENT,
                "spent", List.of(), dispose(), null, TOOL, null));
        no("spent catalyst accepts an actual originating input", hasRule(spent.audit(), "catalyst_origin_missing"));
        spent.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.CATALYST_SPENT,
                "spent", List.of(), dispose(), null, RAW, null));
        yes("spent catalyst rejects a fictional origin", hasRule(spent.audit(), "catalyst_origin_missing"));

        group("optional per-route output roles");
        Fixture role = sideStream(SideStreamClass.BYPRODUCT_OPTIONAL, dispose());
        RoutePolicy policy = role.policies.get("side_source");
        role.policies.put("side_source", new RoutePolicy(0, false, policy.environmentalSources(),
                Map.of(SIDE, OutputRole.SIDE_STREAM)));
        no("an actual output can have a role override", hasRule(role.audit(), "output_role_invalid"));
        role.policies.put("side_source", new RoutePolicy(0, false, policy.environmentalSources(),
                Map.of(SIDE, OutputRole.DISCHARGED)));
        yes("a role override cannot invent physical disposal", hasRule(role.audit(), "output_role_invalid"));
        role.policies.put("side_source", new RoutePolicy(0, false, policy.environmentalSources(),
                Map.of(RAW, OutputRole.PRIMARY)));
        yes("a role override cannot invent an output", hasRule(role.audit(), "output_role_invalid"));
    }

    private static void debtRatchet() {
        group("fingerprinted versioned debt ratchet");
        AuditFinding error = new AuditFinding("fixture_error", PRODUCT.key(), "exact_detail",
                AuditFinding.Severity.ERROR, "Synthetic violation", List.of("fixture:source"));
        var applied = DebtManifest.empty().apply(List.of(error));
        no("an unlisted error fails the ratchet", applied.ratchetPass());
        eq("an unlisted error stays active", 1L, applied.activeErrors());
        String fingerprint = "fixture_error:" + PRODUCT.key() + ":exact_detail";
        eq("fingerprints are rule:subject[:detail]", fingerprint, error.fingerprint());
        DebtManifest.Entry entry = new DebtManifest.Entry(fingerprint, "fixture:source",
                "Remove the synthetic violation", "fixture-task");
        DebtManifest listed = new DebtManifest(1, 2, Map.of(fingerprint, entry));
        applied = listed.apply(List.of(error));
        yes("exact listed debt is ratchet-eligible", applied.ratchetPass());
        eq("listed debt reports waived_debt", AuditFinding.Disposition.WAIVED_DEBT,
                applied.findings().get(0).disposition());
        eq("listed debt retains error severity", AuditFinding.Severity.ERROR, applied.findings().get(0).severity());
        eq("listed debt remains failure-visible", 1L, applied.waivedDebt());
        no("listed debt is not an error-free report", applied.errorFree());
        no("a pre-marked raw finding cannot waive itself without the manifest",
                DebtManifest.empty().apply(List.of(error.waived())).ratchetPass());
        yes("the exact manifest, not raw presentation state, grants a waiver",
                listed.apply(List.of(error.waived())).ratchetPass());
        AuditFinding downgrade = new AuditFinding(error.rule(), error.subject(), error.detail(),
                AuditFinding.Severity.UNKNOWN, "A duplicate cannot weaken this error", List.of("fixture:other"));
        no("a duplicate unknown cannot downgrade an unlisted error",
                DebtManifest.empty().apply(List.of(error, downgrade)).ratchetPass());
        eq("duplicate finding order does not affect the ratchet projection",
                DebtManifest.empty().apply(List.of(error, downgrade)),
                DebtManifest.empty().apply(List.of(downgrade, error)));
        Fixture fixture = new Fixture();
        fixture.builder.finding(error);
        fixture.debt = listed;
        no("waived debt cannot certify sustainability", fixture.audit().sustainabilityPass());
        applied = listed.apply(List.of());
        no("stale debt fails the ratchet", applied.ratchetPass());
        yes("stale debt reports an exact error", applied.findings().stream()
                .anyMatch(finding -> finding.rule().equals("stale_debt") && finding.severity() == AuditFinding.Severity.ERROR));
        AuditFinding unknown = new AuditFinding("fixture_unknown", PRODUCT.key(), "",
                AuditFinding.Severity.UNKNOWN, "Unauthored fact", List.of());
        AuditFinding warning = new AuditFinding("fixture_warning", PRODUCT.key(), "",
                AuditFinding.Severity.WARNING, "Review needed", List.of());
        applied = DebtManifest.empty().apply(List.of(unknown, warning));
        yes("unknowns and warnings are reported without becoming errors", applied.ratchetPass());
        eq("unknown count is retained", 1L, applied.unknowns());
        eq("warning count is retained", 1L, applied.warnings());
        no("debt cannot hide a similar new fingerprint",
                listed.apply(List.of(new AuditFinding("fixture_error", PRODUCT.key(), "different_detail",
                        AuditFinding.Severity.ERROR, "New error", List.of()))).ratchetPass());
        yes("removing resolved debt restores a clean ratchet", DebtManifest.empty().apply(List.of()).ratchetPass());
        throwsArgument("wildcard debt is forbidden", () -> new DebtManifest.Entry(
                "rule:*", "fixture", "Remove violation", "fixture-task"));
        throwsArgument("debt needs a concrete remediation", () -> new DebtManifest.Entry(
                fingerprint, "fixture", " ", "fixture-task"));
        throwsArgument("debt schema is explicit", () -> new DebtManifest(2, 1, Map.of()));
        throwsArgument("debt version is explicit", () -> new DebtManifest(1, 0, Map.of()));
    }

    private static void deterministicProjection() {
        group("deterministic schema and immutable projections");
        Fixture first = new Fixture().seed(RAW, 3);
        first.add(route("z", Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 1L), null));
        first.add(route("a", Map.of(SIDE, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        first.root(PRODUCT);
        Fixture second = new Fixture().seed(RAW, 3);
        second.add(route("a", Map.of(SIDE, 1L), Map.of(), Map.of(PRODUCT, 1L), null));
        second.add(route("z", Map.of(RAW, 1L), Map.of(), Map.of(SIDE, 1L), null));
        second.root(PRODUCT);
        String json = AuditReports.json(first.audit(), "fixture-revision", false);
        eq("JSON ordering is independent of insertion order", json,
                AuditReports.json(second.audit(), "fixture-revision", false));
        eq("human report ordering is independent of insertion order",
                AuditReports.markdown(first.audit(), "fixture-revision", false),
                AuditReports.markdown(second.audit(), "fixture-revision", false));
        JsonObject document = JsonParser.parseString(json).getAsJsonObject();
        eq("audit JSON uses schema 1", 1, document.get("schema_version").getAsInt());
        eq("reports preserve the supplied revision", "fixture-revision", document.get("source_revision").getAsString());
        no("unknown tiers do not certify the whole model", document.get("sustainability_pass").getAsBoolean());
        var snapshot = first.builder.build();
        try {
            snapshot.routes().clear();
            fail("authority projections must be immutable");
        } catch (UnsupportedOperationException expected) {
            yes("authority projections are immutable", true);
        }
    }

    private static void live() throws IOException, InterruptedException {
        group("live adapters and report-only gate");
        Path root = Path.of("").toAbsolutePath().normalize();
        Path packageDirectory = root.resolve(Path.of("common", "src", "main", "java", "io", "github",
                "ezequiel24123z", "grindless", "sustainability"));
        Pattern forbidden = Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?"
                + "(?:net\\.minecraft(?:forge)?|net\\.neoforged|dev\\.architectury)\\.");
        try (var sources = Files.walk(packageDirectory)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                no(source.getFileName() + " has no loader/game imports",
                        forbidden.matcher(Files.readString(source)).find());
            }
        }
        no("the auditor check has no loader/game imports", forbidden.matcher(Files.readString(
                root.resolve(Path.of("tools", "checks", "VerifySustainability.java")))).find());
        Snapshot authorities = AuthorityAdapters.live(root);
        SustainabilityAnnotations annotations = SustainabilityAnnotations.empty();
        DebtManifest debt = DebtManifest.read(root.resolve(Path.of("tools", "sustainability", "debt.json")));
        eq("S1 has no accepted debt baseline", 0, debt.entries().size());
        eq("S1 has no live resource classifications", 0, annotations.resources().size());
        eq("S1 has no live tier roots", 0, annotations.ledger().size());
        long expectedProcesses = ProcessGraph.generate(AuthorityAdapters.materialViews()).size();
        eq("every live generated process is adapted", expectedProcesses, authorities.routes().values().stream()
                .filter(route -> route.authority() == Authority.PROCESS_GRAPH).count());
        for (ProcessRecipe recipe : ProcessGraph.generate(AuthorityAdapters.materialViews())) {
            eq("live process batch " + recipe.id(), AuthorityAdapters.process(recipe), authorities.routes().get(recipe.id()));
        }
        for (Route route : authorities.routes().values()) {
            boolean automatic = switch (route.authority()) {
                case PROCESS_GRAPH, GENERATOR_FUEL -> true;
                case BOOTSTRAP_RECIPES, T1_RECIPES, CRAFTING_JSON, QUEST_REWARD -> false;
                default -> throw new AssertionError("An actual new authority needs an execution-semantics assertion: "
                        + route.authority());
            };
            eq("the real execution binding is explicit for " + route.id(), automatic, route.automatable());
        }
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            yes("live supply entry " + entry.itemName() + " is inventoried",
                    authorities.resources().contains(new Resource(Kind.ITEM, "grindless:" + entry.itemName())));
        }
        for (BootstrapRecipes.Shaped craft : BootstrapRecipes.shaped()) {
            Route route = authorities.routes().get("craft/" + craft.name());
            yes("shipped bootstrap shaped craft " + craft.name() + " is inventoried", route != null);
            eq("bootstrap output quantity " + craft.name() + " stays live", craft.resultCount(),
                    route.outputs().get(new Resource(Kind.ITEM, craft.result())));
        }
        for (BootstrapRecipes.Shapeless craft : BootstrapRecipes.shapeless()) {
            yes("shipped bootstrap shapeless craft " + craft.name() + " is inventoried",
                    authorities.routes().containsKey("craft/" + craft.name()));
        }
        Path recipes = root.resolve(Path.of("common", "src", "main", "resources", "data", "grindless", "recipes"));
        long recipeFiles;
        try (var files = Files.walk(recipes)) {
            recipeFiles = files.filter(path -> path.toString().endsWith(".json")).count();
        }
        eq("all authored crafting JSON is inventoried, including non-catalogue crafts", recipeFiles,
                authorities.routes().values().stream().filter(route -> route.id().startsWith("craft/")).count());
        eq("all quest objectives are inventoried without declaring all of them mandatory",
                QuestCatalogue.tasks().size(), authorities.objectives().size());
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            Route route = authorities.routes().get("quest/" + task.id());
            eq("exact quest reward " + task.id() + " stays live", task.reward().count(),
                    route.outputs().get(new Resource(Kind.ITEM, task.reward().itemId())));
            yes("quest reward " + task.id() + " stays one-shot", route.oneShot());
            eq("quest evidence count " + task.id() + " stays live", task.count(),
                    authorities.objectives().get(task.id()).count());
        }
        yes("biomass fuel authority is adapted", authorities.routes().values().stream()
                .anyMatch(route -> route.id().equals("fuel/biomass_dynamo/biomass")
                        && route.durationTicks() == SourceLogic.BIOMASS_BURN_TICKS));
        yes("gas fuel authority is adapted", authorities.routes().values().stream()
                .anyMatch(route -> route.id().contains("fuel/thermal_generator/grindless:hydrogen")));
        TagResolver local = new TagResolver(root, VerifySustainability.class.getClassLoader(), false);
        TagResolution boards = local.resolve(new Resource(Kind.TAG, "grindless:boards"));
        yes("local nested boards retain their authored fibreboard member", boards.members().contains(
                new Resource(Kind.ITEM, SourceLogic.FIBREBOARD)));
        no("missing nested vanilla JSON is never treated as complete", boards.complete());
        TagResolver classpath = new TagResolver(root, VerifySustainability.class.getClassLoader(), true);
        TagResolution planks = classpath.resolve(new Resource(Kind.TAG, "minecraft:planks"));
        yes("unordered vanilla tag fallback retains discovered members", !planks.members().isEmpty());
        no("unordered classpath lookup cannot establish resource-pack precedence", planks.complete());
        yes("classpath tag fallback preserves explicit provenance",
                planks.sources().stream().anyMatch(source -> source.startsWith("classpath:")));
        Builder fallback = new Builder();
        classpath.contribute(fallback);
        String precedence = "classpath_precedence_unknown:tag:minecraft:planks";
        yes("unordered vanilla fallback retains exact planks precedence uncertainty",
                planks.unresolved().contains(precedence) || fallback.build().findings().stream()
                        .anyMatch(finding -> finding.fingerprint().equals(precedence)));
        yes("fallback never silently pretends a local tag exists", fallback.build().findings().stream()
                .anyMatch(finding -> finding.rule().equals("unresolved_tag")
                        && finding.subject().equals("tag:minecraft:planks") && finding.detail().equals("local_json")));

        var result = SustainabilityAudit.run(authorities, annotations, debt);
        no("shipped JSON and Java catalogues do not drift", hasRule(result, "craft_authority_mismatch"));
        no("every live catalogue craft has shipped JSON", hasRule(result, "craft_authority_missing"));
        yes("missing local Forge iron tag stays explicit", unresolved(result, "tag:forge:ingots/iron"));
        yes("missing local Forge copper tag stays explicit", unresolved(result, "tag:forge:ingots/copper"));
        yes("nested missing vanilla planks stays explicit", unresolved(result, "tag:minecraft:planks"));
        eq("all sixteen live tiers remain explicitly unknown", 16L, result.tiers().values().stream()
                .filter(tier -> tier.unknown() != null).count());
        no("unannotated live model is not a sustainability pass", result.sustainabilityPass());
        Builder allowlist = new Builder();
        AuthorityAdapters.vanillaTransformation("minecraft:grindless_missing_fixture", VerifySustainability.class.getClassLoader(),
                allowlist);
        yes("missing allowlisted vanilla classpath recipe is explicit", allowlist.build().findings().stream()
                .anyMatch(finding -> finding.rule().equals("vanilla_recipe_missing")));
        Builder vanilla = new Builder();
        AuthorityAdapters.vanillaTransformation("minecraft:crafting_table", VerifySustainability.class.getClassLoader(), vanilla);
        yes("an allowlisted real vanilla transformation is read from the resolved classpath",
                vanilla.build().routes().containsKey("vanilla/minecraft:crafting_table"));
        no("a vanilla crafting transformation is not automatic merely because it is allowlisted",
                vanilla.build().routes().get("vanilla/minecraft:crafting_table").automatable());

        String revision = revision(root);
        Path output = root.resolve(Path.of("build", "sustainability"));
        AuditReports.Hashes first = AuditReports.write(output, result, revision, true);
        Snapshot repeated = AuthorityAdapters.live(root);
        var secondResult = SustainabilityAudit.run(repeated, SustainabilityAnnotations.empty(), debt);
        AuditReports.Hashes second = AuditReports.write(output, secondResult, revision, true);
        eq("two live extractions produce identical authority hashes",
                AuditReports.authorityHash(authorities), AuditReports.authorityHash(repeated));
        eq("two live runs produce identical audit.json hashes", first.json(), second.json());
        eq("two live runs produce identical audit.md hashes", first.markdown(), second.markdown());
        JsonObject document = JsonParser.parseString(Files.readString(output.resolve("audit.json"))).getAsJsonObject();
        eq("live report uses audit schema 1", 1, document.get("schema_version").getAsInt());
        eq("live report declares its S1 gate", "report_only_s1_no_annotations", document.get("mode").getAsString());
        no("live report never promotes fixtures into sustainability evidence",
                document.get("sustainability_pass").getAsBoolean());
        yes("human report states the S1 evidence boundary", Files.readString(output.resolve("audit.md"))
                .contains("LIVE AUDIT REPORT-ONLY (S1, no annotations)"));
        System.out.println("LIVE AUDIT REPORT-ONLY (S1, no annotations)");
        System.out.println("Live authorities: " + authorities.resources().size() + " resources, "
                + authorities.routes().size() + " routes, " + authorities.tags().size() + " tags, "
                + authorities.objectives().size() + " objectives");
        System.out.println("Live findings: " + result.findings().activeErrors() + " active errors, "
                + result.findings().waivedDebt() + " waived_debt, " + result.findings().warnings() + " warnings, "
                + result.findings().unknowns() + " unknowns; ratchet_pass=" + result.findings().ratchetPass()
                + "; sustainability_pass=false (report-only)");
        AuditReports.countsPerRule(result).forEach((rule, count) -> System.out.println("LIVE RULE " + rule + ": " + count));
        System.out.println("DETERMINISM audit.json SHA-256 " + first.json() + " = " + second.json());
        System.out.println("DETERMINISM audit.md SHA-256 " + first.markdown() + " = " + second.markdown());
    }

    private static String revision(Path root) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "--no-pager", "rev-parse", "HEAD")
                .directory(root.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        return process.waitFor() == 0 && output.matches("[0-9a-f]{40}") ? output : "unknown";
    }

    private static Fixture producer(long seed, long construction, long operationalSeed) {
        Fixture fixture = new Fixture();
        if (seed > 0) {
            fixture.seed(RAW, seed);
        }
        fixture.add(route("construct", Map.of(RAW, construction), Map.of(), Map.of(MACHINE, 1L), null));
        fixture.machines.put("COLLECTOR", new Machine("COLLECTOR", MACHINE, 0));
        fixture.environment("gather", operationalSeed == 0 ? Map.of() : Map.of(RAW, operationalSeed), Map.of(),
                Map.of(RAW, operationalSeed + 1), "COLLECTOR");
        return fixture.meta(RAW, renewable("gather")).root(RAW);
    }

    private static Fixture loop(boolean seed, boolean environmental, int loss) {
        Fixture fixture = new Fixture();
        long charge = 1L + loss;
        if (seed) {
            fixture.seed(WATER, charge);
        }
        if (environmental) {
            fixture.environment("make_up", Map.of(WATER, 1L), null);
        }
        fixture.add(route("boil", Map.of(WATER, charge), Map.of(), Map.of(STEAM, 1L), null));
        fixture.add(route("condense", Map.of(STEAM, 1L), Map.of(), Map.of(WATER, 1L), null));
        ResourceClass type = loss == 0 ? ResourceClass.CLOSED_LOOP : ResourceClass.LOSSY_LOOP;
        fixture.meta(WATER, metadata(type, null, "condense", List.of(), null, null, null, null));
        fixture.meta(STEAM, metadata(ResourceClass.BOOTSTRAP, null, "boil", List.of(), null, null, null, null));
        fixture.loops.put("water", new ClosedLoop("water", WATER, Set.of("boil", "condense"),
                environmental ? "make_up" : null));
        return fixture.root(WATER);
    }

    private static Fixture twoMachines(long guarantee) {
        Resource second = item("machine_b");
        Resource feed = item("feed_b");
        Fixture fixture = new Fixture().seed(RAW, guarantee);
        fixture.add(route("build_a", Map.of(RAW, 3L), Map.of(), Map.of(MACHINE, 1L), null));
        fixture.add(route("build_b", Map.of(RAW, 3L), Map.of(), Map.of(second, 1L), null));
        fixture.machines.put("A", new Machine("A", MACHINE, 0));
        fixture.machines.put("B", new Machine("B", second, 0));
        fixture.environment("feed", Map.of(feed, 1L), "B");
        fixture.add(route("finish", Map.of(feed, 1L), Map.of(), Map.of(PRODUCT, 1L), "A"));
        return fixture.meta(PRODUCT, renewable("finish")).root(PRODUCT);
    }

    private static Fixture rewardProducer(long reward) {
        Fixture fixture = new Fixture();
        fixture.add(new Route("quest/reward", Authority.QUEST_REWARD, "fixture:reward", null, Map.of(),
                Map.of(SAFE, 1L), Map.of(RAW, reward), Set.of(), 0, 0, Map.of(), Set.of(), true, true));
        fixture.add(route("construct", Map.of(RAW, 2L), Map.of(), Map.of(MACHINE, 1L), null));
        fixture.machines.put("COLLECTOR", new Machine("COLLECTOR", MACHINE, 0));
        fixture.environment("gather_product", Map.of(PRODUCT, 1L), "COLLECTOR");
        return fixture.meta(PRODUCT, renewable("gather_product")).root(PRODUCT);
    }

    private static Fixture recyclingFixture(long returned) {
        Fixture fixture = new Fixture();
        fixture.add(route("forward", Map.of(RAW, 2L), Map.of(), Map.of(SIDE, 3L), null));
        fixture.add(route("reverse", Map.of(SIDE, 3L), Map.of(), Map.of(RAW, returned), null));
        return fixture.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.WASTE_RECOVERABLE,
                "forward", List.of(), dispose(), new Recycling("reverse", null), null, null));
    }

    private static Fixture sideStream(SideStreamClass side, Handling handling) {
        return new Fixture().environment("side_source", Map.of(SIDE, 1L), null)
                .meta(SIDE, metadata(ResourceClass.BOOTSTRAP, side, "side_source", List.of(),
                        handling, null, null, null));
    }

    private static Fixture future() {
        Fixture fixture = sideStream(SideStreamClass.BYPRODUCT_FUTURE, dispose());
        fixture.plans.put("fixture-plan", new CommittedPlan("fixture-plan", "fixture-system", 2, "fixture:committed_plan"));
        return fixture.meta(SIDE, metadata(ResourceClass.BOOTSTRAP, SideStreamClass.BYPRODUCT_FUTURE,
                "side_source", List.of(new FutureConsumer(2, "fixture-system", "fixture-plan")),
                dispose(), null, null, null));
    }

    private static ResourceMetadata renewable(String route) {
        return metadata(ResourceClass.RENEWABLE, null, route, List.of(), null, null, null, null);
    }

    private static ResourceMetadata metadata(ResourceClass type, SideStreamClass side, String canonical,
                                             List<FutureConsumer> future, Handling handling,
                                             Recycling recycling, Resource origin, String uncertainty) {
        Integer renewableTier = type == ResourceClass.RENEWABLE || type == ResourceClass.CLOSED_LOOP
                || type == ResourceClass.LOSSY_LOOP ? 0 : null;
        return new ResourceMetadata(type, side, 0, renewableTier, canonical, future, handling, recycling, origin, uncertainty);
    }

    private static Handling dispose() {
        return new Handling(Action.DISCARD, true, false, null, null);
    }

    private static Handling reuse() {
        return new Handling(Action.REUSE, false, false, null, null);
    }

    private static Resource item(String name) {
        return new Resource(Kind.ITEM, "fixture:" + name);
    }

    private static Route route(String id, Map<Resource, Long> inputs, Map<Resource, Long> catalysts,
                               Map<Resource, Long> outputs, String family) {
        return new Route(id, Authority.PROCESS_GRAPH, "fixture:" + id, family, inputs, catalysts, outputs,
                Set.of(), 1, 0, Map.of(), Set.of(), false, true);
    }

    private static boolean available(SustainabilityAudit.Result result, Resource resource) {
        return result.tiers().get(0).available().contains(resource);
    }

    private static boolean renewable(SustainabilityAudit.Result result, Resource resource) {
        return result.tiers().get(0).renewable().contains(resource);
    }

    private static boolean hasRule(SustainabilityAudit.Result result, String rule) {
        return result.findings().findings().stream().anyMatch(finding -> finding.rule().equals(rule));
    }

    private static boolean unresolved(SustainabilityAudit.Result result, String subject) {
        return result.findings().findings().stream().anyMatch(finding ->
                finding.rule().equals("unresolved_tag") && finding.subject().equals(subject));
    }

    private static void group(String name) {
        group = name;
        GROUPS.computeIfAbsent(name, ignored -> new int[2]);
    }

    private static void eq(String what, Object expected, Object actual) {
        boolean equal = expected instanceof Number left && actual instanceof Number right
                ? new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0
                : java.util.Objects.equals(expected, actual);
        if (equal) {
            yes(what, true);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            passed++;
            GROUPS.get(group)[0]++;
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        yes(what, !actual);
    }

    private static void throwsArgument(String what, Runnable operation) {
        try {
            operation.run();
            fail(what + ": expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            yes(what, true);
        }
    }

    private static void fail(String what) {
        failures++;
        if (group != null) {
            GROUPS.get(group)[1]++;
        }
        System.out.println("  FAIL " + what);
    }

    private static final class Fixture {
        private final Builder builder = new Builder();
        private final Map<Resource, ResourceMetadata> resources = new TreeMap<>();
        private final Map<String, RoutePolicy> policies = new TreeMap<>();
        private final Map<String, Machine> machines = new TreeMap<>();
        private final Map<String, ClosedLoop> loops = new TreeMap<>();
        private final Map<String, CommittedPlan> plans = new TreeMap<>();
        private final Map<Integer, TierPlan> ledger = new TreeMap<>();
        private final Set<Resource> roots = new TreeSet<>(Set.of(SAFE));
        private final Set<String> requiredQuests = new TreeSet<>();
        private DebtManifest debt = DebtManifest.empty();
        private EntryProfile entryProfile;
        private Resource exitObjective;
        private int sources;

        private Fixture() {
            environment("base", Map.of(SAFE, 1L), null);
            meta(SAFE, renewable("base"));
        }

        private Fixture add(Route route) {
            builder.route(route);
            policies.put(route.id(), new RoutePolicy(0, false, Set.of()));
            route.outputs().keySet().forEach(resource -> resources.putIfAbsent(resource,
                    metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null)));
            return this;
        }

        private Fixture environment(String id, Map<Resource, Long> outputs, String family) {
            return environment(id, Map.of(), Map.of(), outputs, family);
        }

        private Fixture environment(String id, Map<Resource, Long> inputs, Map<Resource, Long> catalysts,
                                    Map<Resource, Long> outputs, String family) {
            return environment(route(id, inputs, catalysts, outputs, family));
        }

        private Fixture environment(Route route) {
            Resource source = new Resource(Kind.ENVIRONMENT, "fixture:" + route.id());
            builder.terminal(new Terminal("environment/" + route.id(), source, TerminalKind.ENVIRONMENT,
                    SourceSite.GEOLOGICAL_ROCK_BED, true, true, null, 0,
                    "Synthetic environmental source, not live gameplay", "fixture:" + route.id()));
            meta(source, renewable(null));
            add(route);
            policies.put(route.id(), new RoutePolicy(0, false, Set.of("environment/" + route.id())));
            return this;
        }

        private Fixture seed(Resource resource, long count) {
            builder.terminal(new Terminal("seed/" + sources++, resource, TerminalKind.EXACT_BOOTSTRAP,
                    null, false, true, count, 0, "Synthetic exact bootstrap guarantee", "fixture:seed"));
            resources.putIfAbsent(resource,
                    metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null));
            return this;
        }

        private Fixture terminal(String id, Resource resource, TerminalKind kind, boolean automatable,
                                 boolean modOwned) {
            builder.terminal(new Terminal(id, resource, kind, null, automatable, modOwned, null, 0,
                    "Synthetic optional/finite source", "fixture:" + id));
            return this;
        }

        private Fixture root(Resource resource) {
            roots.add(resource);
            return this;
        }

        private Fixture meta(Resource resource, ResourceMetadata metadata) {
            resources.put(resource, metadata);
            return this;
        }

        private Fixture policy(String id, int tier, boolean optional) {
            RoutePolicy old = policies.get(id);
            policies.put(id, new RoutePolicy(tier, optional, old.environmentalSources(), old.outputRoles()));
            return this;
        }

        private Fixture workProfile(Map<Resource, Long> provided, long actions, Set<String> families) {
            Resource labour = new Resource(Kind.ENVIRONMENT, "fixture:local_work");
            builder.terminal(new Terminal("manual_work", labour, TerminalKind.MANUAL_LABOUR, null,
                    false, true, null, 0, "Synthetic local-only crank actions", "fixture:manual_work"));
            meta(labour, metadata(ResourceClass.BOOTSTRAP, null, null, List.of(), null, null, null, null));
            entryProfile = new EntryProfile("fixture:conditional_entry", provided, actions,
                    new ManualWork("manual_work", TOOL, 160, families, "fixture:typed_process_F0_guard"),
                    Set.of("Given ordinary physical entry materials and bounded player actions"),
                    "fixture:conditional_entry_not_gameplay");
            return this;
        }

        private SustainabilityAnnotations annotations() {
            ledger.put(0, new TierPlan(0, roots, requiredQuests, exitObjective, null, entryProfile));
            return new SustainabilityAnnotations(resources, policies, machines, loops, plans, ledger);
        }

        private SustainabilityAudit.Result audit() {
            return SustainabilityAudit.run(builder.build(), annotations(), debt);
        }
    }
}
