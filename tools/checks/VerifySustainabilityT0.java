package io.github.ezequiel24123z.grindless.sustainability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Source-bound T0 declarations and their finite conditional establishment replay.
 * This check is not an observed survival inventory, client craft or Tier acceptance.
 */
public final class VerifySustainabilityT0 {
    private static int passed;
    private static int failed;

    private VerifySustainabilityT0() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        var sources = VerifySustainabilityRegistry.canonicalSources(root, T0Sustainability.vanillaAllowlist());
        Snapshot snapshot = T0Sustainability.withEntrySources(sources.snapshot());
        EntryProfile template = T0Sustainability.entryTemplate(sources.localWork());
        SustainabilityAnnotations annotations = T0Sustainability.annotations(snapshot, sources.facts(),
                sources.localWork(), template);
        DebtManifest debt = DebtManifest.read(root.resolve(Path.of("tools", "sustainability", "debt.json")));
        var derivation = SustainabilityAudit.deriveEntryProfile(snapshot, annotations, debt, 0);
        var result = derivation.replay();
        var tier = result.tiers().get(0);
        T0ConditionalGate.Gate gate = T0ConditionalGate.evaluate(result);
        // Keep semantic failures visible even when the independent generated-asset equality is
        // also stale. The equality and the hard pass assertion below are not skipped or waived.
        gate.unmetObligations().forEach((id, reason) -> System.out.println("GATE BLOCKED " + id + ": " + reason));
        gate.decisions().stream().filter(T0ConditionalGate.Decision::blocking)
                .forEach(decision -> System.out.println("GATE BLOCKED " + decision.fingerprint() + ": " + decision.basis()));
        passed += VerifyResourceGuidance.verifyCanonical(root, result);
        Path report = root.resolve(Path.of("build", "sustainability", "t0-conditional"));
        String revision = revision(root);
        AuditReports.Hashes hashes = AuditReports.write(report, result, revision, true);
        eq("repeated exports of identical conditional evidence are deterministic", hashes,
                AuditReports.write(report, result, revision, true));
        System.out.println("T0 CONDITIONAL INVENTORY: " + snapshot.resources().size() + " resources, "
                + snapshot.routes().size() + " routes, " + snapshot.tags().size() + " tags, "
                + snapshot.objectives().size() + " objectives.");
        System.out.println("DERIVED ENTRY BILL: " + quantities(derivation.profile().playerProvided())
                + "; local ProcessMachine actions=" + derivation.profile().manualWorkActions());
        System.out.println("T0 JSON SHA-256 " + hashes.json());
        System.out.println("T0 MARKDOWN SHA-256 " + hashes.markdown());
        for (AuditFinding finding : derivation.derivationFindings()) {
            if (finding.detail().equals("T0")) {
                System.out.println("DERIVATION ONLY " + finding.severity() + " " + finding.fingerprint()
                        + ": " + finding.message());
            }
        }
        for (AuditFinding finding : result.findings().findings()) {
            if (finding.detail().equals("T0")) {
                System.out.println("T0 " + finding.severity() + " " + finding.fingerprint() + ": " + finding.message());
            }
        }
        eq("the hard gate covers relevant global evidence, not only detail T0", true, gate.pass());
        eq("a finite conditional entry is explicitly identified", template.id(), derivation.profile().id());
        eq("the entire prototype route inventory remains present", true,
                snapshot.routes().keySet().containsAll(sources.snapshot().routes().keySet()));
        eq("the T0 ledger is authored rather than a silently successful unknown", null, tier.unknown());
        eq("future unaccepted tiers remain unknown", true, result.tiers().get(1).unknown() != null);
        eq("future unknowns are not a global sustainability certificate", false, result.sustainabilityPass());
        eq("the active declaration cannot waive debt", 0, debt.entries().size());
        Resource book = new Resource(Kind.ITEM, "grindless:quest_book");
        eq("the real held-book interface has source-bound access evidence", true,
                snapshot.inputHashes().containsKey("mechanics_binding:QuestBookItem_QuestBookMenu"));
        eq("UI access is a real first T0 use even without an industrial consumer", 0,
                annotations.resources().get(book).firstUseTier());
        for (Route route : snapshot.routes().values()) {
            if (route.authority() == Authority.QUEST_REWARD) {
                eq("every actual claim requires the book beforehand without consuming it: " + route.id(), 1L,
                        route.catalysts().get(book));
            }
        }
        eq("there is no initial guaranteed or player-provided quest book", false,
                tier.reserves().stream().anyMatch(reserve -> reserve.resource().equals(book)
                        && (reserve.guaranteed() > 0 || reserve.playerProvided() > 0)));
        eq("the interface book is actually fabricated in the same finite witness", true,
                tier.establishmentBatches().getOrDefault("craft/quest_book", 0L)
                        + tier.establishmentBatches().getOrDefault("bench/quest_book", 0L) > 0);
        eq("no quest invents a free book for the first interface access", false,
                snapshot.routes().values().stream().filter(route -> route.authority() == Authority.QUEST_REWARD)
                        .anyMatch(route -> route.outputs().containsKey(book)));
        eq("actual source/entry authorities resolve the provisional raw-inventory absence finding", 0L,
                result.findings().findings().stream().filter(finding ->
                        finding.rule().equals("bootstrap_sources_unknown")).count());
        Snapshot unbound = new Builder().finding(new AuditFinding("bootstrap_sources_unknown",
                "manual_world_and_labour", "", AuditFinding.Severity.UNKNOWN,
                "No source binding has been supplied.", java.util.List.of())).build();
        eq("conditional provisions alone cannot erase an unbound source fact", 1L,
                T0Sustainability.withEntrySources(unbound).findings().stream().filter(finding ->
                        finding.rule().equals("bootstrap_sources_unknown")).count());
        for (Terminal terminal : snapshot.terminals().values()) {
            if (terminal.kind() == TerminalKind.MANUAL_WORLD || terminal.kind() == TerminalKind.MANUAL_LABOUR) {
                eq("manual acquisition/work is never a guaranteed gift: " + terminal.id(), null, terminal.guaranteed());
                eq("manual acquisition/work is never an automatic renewable source: " + terminal.id(),
                        false, terminal.renewable());
            }
        }
        for (String id : T0Sustainability.vanillaAllowlist()) {
            Route craft = snapshot.routes().get("vanilla/" + id);
            eq("each allowlisted construction has actual resolved recipe data: " + id, true, craft != null);
            eq("vanilla construction crafts stay manual: " + id, false, craft != null && craft.automatable());
        }
        Resource furnace = new Resource(Kind.ITEM, "minecraft:furnace");
        ResourceMetadata furnaceCapital = annotations.resources().get(furnace);
        eq("the coordinator's external Furnace decision uses the existing manual vanilla authority",
                "vanilla/minecraft:furnace", furnaceCapital.canonicalRoute());
        eq("the vanilla Furnace is finite manual capital, not recurring automatic item supply",
                ResourceClass.MANUAL_CAPITAL, furnaceCapital.resourceClass());
        eq("no Matrix ingredient is added to the vanilla Furnace", false,
                snapshot.routes().get(furnaceCapital.canonicalRoute()).inputs().keySet().stream()
                .anyMatch(input -> input.id().equals("grindless:relay_matrix")));
        eq("the old Bench Furnace constructor is absent", false, snapshot.routes().containsKey("bench/furnace"));
        eq("the old Assembler Furnace constructor is absent", false,
                snapshot.routes().containsKey("assemble/bootstrap/furnace"));
        Resource firingController = annotations.machines().get("FIRING_CHAMBER").construction();
        ResourceMetadata firingCapital = annotations.resources().get(firingController);
        eq("the allowed formed-controller constructor is finite capital with its manual Furnace input",
                ResourceClass.BOOTSTRAP, firingCapital.resourceClass());
        eq("finite formed capital does not promise R0 controller-item renewal", null,
                firingCapital.firstRenewableTier());
        eq("the actual automatic controller authority is retained", "bench/firing_chamber", firingCapital.canonicalRoute());
        eq("local automatic controller assembly remains permitted", true,
                snapshot.routes().get(firingCapital.canonicalRoute()).automatable());
        eq("the actual controller bill still consumes one real Furnace", 1L,
                snapshot.routes().get(firingCapital.canonicalRoute()).inputs().get(furnace));
        eq("controller-item supply is not automatically renewable", false, tier.renewable().contains(firingController));
        eq("the installed controller still supports actual renewable ceramics", true,
                tier.renewable().contains(new Resource(Kind.ITEM, "grindless:ceramic_insulator")));
        for (Machine machine : annotations.machines().values()) {
            Resource capital = machine.construction();
            if (!io.github.ezequiel24123z.grindless.recipe.ManufacturingPolicy.isManualFinalMachine(capital.id())) {
                continue;
            }
            ResourceMetadata metadata = annotations.resources().get(capital);
            eq("real normal-machine capital has its explicit nonrenewable class: " + capital.key(),
            ResourceClass.MANUAL_CAPITAL, metadata.resourceClass());
            eq("manual capital has no automatic item-renewability tier: " + capital.key(), null,
            metadata.firstRenewableTier());
            eq("manual capital uses an actual manual construction route: " + capital.key(), false,
            snapshot.routes().get(metadata.canonicalRoute()).automatable());
            eq("a normal machine item is not an R0 recurring supply: " + capital.key(), false,
            tier.renewable().contains(capital));
            eq("possession/installation is not a recurring raw-feed obligation: " + capital.key(), false,
            tier.recurring().contains(capital));
            eq("normal capital remains constructible and available: " + capital.key(), true,
            tier.available().contains(capital));
        }
        for (Route route : snapshot.routes().values()) {
            RoutePolicy policy = annotations.routes().get(route.id());
            if (policy == null || !Integer.valueOf(0).equals(policy.unlockTier())) {
                continue;
            }
            for (Resource output : route.retainedOutputs().keySet()) {
                if (io.github.ezequiel24123z.grindless.recipe.ManufacturingPolicy.isManualFinalMachine(output.id())) {
            eq("all actual T0 normal-machine outputs, not only process-family bindings, have capital metadata: "
                            + route.id() + "/" + output.key(),
                    ResourceClass.MANUAL_CAPITAL, annotations.resources().get(output).resourceClass());
            eq("a T0 capital output is never a renewable item claim: " + output.key(), false,
                    tier.renewable().contains(output));
                }
            }
        }
        Resource exit = annotations.ledger().get(0).exitObjective();
        Resource relayDeclaration = new Resource(Kind.ITEM, "grindless:relay_matrix");
        eq("the T0 exit retains the exact physical F1 quest identity",
                new Resource(Kind.ITEM, relayDeclaration.id(), 1, "items"), exit);
        eq("the T0 exit metadata and quest use the same physical identity", exit,
                snapshot.objectives().get("relay").subject());
        eq("the manual exit produces the unchanged complete physical batch", Map.of(exit, 5L),
                snapshot.routes().get("craft/relay_matrix").outputs());
        eq("the Relay claim retains five physical F1 stacks as evidence", 5L,
                snapshot.routes().get("quest/relay").catalysts().get(exit));
        eq("physical T0 exit metadata stays finite bootstrap, not automatic supply", ResourceClass.BOOTSTRAP,
                annotations.resources().get(exit).resourceClass());
        eq("a generic declaration does not borrow the physical exit's metadata", false,
                annotations.resources().containsKey(relayDeclaration));
        eq("canonical inventory retains the separate generic declaration", true,
                snapshot.resources().contains(relayDeclaration));
        eq("the manual Matrix is not a recurring product root", false, tier.recurring().contains(exit));
        eq("the manual Matrix is not renewable T0 production", false, tier.renewable().contains(exit));
        eq("the existing manual Matrix craft is unchanged", false,
                snapshot.routes().get("craft/relay_matrix").automatable());
        eq("the Bench never manufactures Matrix", false, snapshot.routes().containsKey("bench/relay_matrix"));
        eq("slag first use is not a made-up T0 or future tier", null,
                annotations.resources().get(new Resource(Kind.ITEM, "grindless:slag")).firstUseTier());
        Resource flint = new Resource(Kind.ITEM, "minecraft:flint");
        ResourceMetadata flintMetadata = annotations.resources().get(flint);
        eq("flint stays an optional by-product rather than a forced T0 consumer", SideStreamClass.BYPRODUCT_OPTIONAL,
                flintMetadata.sideStreamClass());
        eq("flint has no invented first-use tier", null, flintMetadata.firstUseTier());
        eq("optional flint stock is ordinarily discardable, not hazardous",
                new Handling(Action.STORE, true, false, null, null), flintMetadata.handling());
        eq("an optional side stock is not silently promoted to mandatory progression", false,
                tier.mandatory().contains(flint));
        eq("the real ceramic component has a complete no-gain certificate", 0L,
                result.findings().findings().stream().filter(finding ->
                        finding.rule().equals("cycle_balance_unproven")).count());
        for (var entry : annotations.resources().entrySet()) {
            ResourceMetadata metadata = entry.getValue();
            if (metadata.resourceClass() == ResourceClass.RENEWABLE
                    && Integer.valueOf(0).equals(metadata.firstRenewableTier())) {
                eq("every authored T0 unlimited-supply claim is actually proved: " + entry.getKey().key(),
                        true, tier.renewable().contains(entry.getKey()));
            }
        }
        for (Resource resource : tier.mandatory()) {
            eq("every mandatory T0 resource is available: " + resource.key(), true,
                    tier.available().contains(resource));
            ResourceMetadata metadata = annotations.resources().get(resource);
            eq("mandatory T0 classification is explicit: " + resource.key(), true,
                    metadata != null && metadata.resourceClass() != null);
            eq("mandatory T0 first use is explicit: " + resource.key(), true,
                    metadata != null && Integer.valueOf(0).equals(metadata.firstUseTier()));
        }
        for (Resource resource : tier.recurring()) {
            eq("every recurrent T0 supply is renewable: " + resource.key(), true,
                    tier.renewable().contains(resource));
        }
        eq("the finite material replay has no unproved or insufficient reserve", true,
                tier.reserves().stream().allMatch(reserve -> reserve.margin() >= 0
                        && reserve.status().equals("conditional_established")));
        eq("the finite local-work replay has nonnegative real margin", true,
                tier.manualWork() != null && tier.manualWork().marginActions() >= 0
                        && tier.manualWork().status().equals("conditional_established"));
        eq("the actual manual exit batch is executed, not replaced with goal stock", true,
                tier.exitProduction() != null && tier.exitProduction().executedBatches() > 0
                        && tier.exitProduction().produced() >= tier.exitProduction().requiredCount()
                        && tier.exitProduction().status().equals("conditional_established"));
        eq("the real quest requires at least the existing five-Matrix batch", true,
                tier.exitProduction() != null && tier.exitProduction().requiredCount() >= 5);
        var custody = tier.witnessState();
        eq("the applicable passing conditional T0 replay issues complete immutable custody", true, custody != null);
        if (custody != null) {
            eq("the existing report still exposes locked catalysts, not the complete stock",
                    tier.retainedInventory(), custody.retainedCatalysts());
            eq("actual exit production is present once as free physical F1", tier.exitProduction().produced(),
                    custody.freeStock().get(exit));
            eq("manufactured exit stock is not duplicated as a delivery", 0L,
                    custody.cumulativeDeliveries().getOrDefault(exit, 0L));
            eq("the complete checkpoint preserves actual completed-batch counts",
                    tier.establishmentBatches(), custody.establishmentBatches());
            eq("every completed batch has a concrete physical history entry",
                    tier.establishmentBatches().values().stream().mapToLong(Long::longValue).sum(),
                    (long) custody.history().size());
            eq("the required startup-work bound is preserved exactly",
                    tier.manualWork().requiredActions(), custody.requiredManualActions());
            eq("actual spent startup labour is distinct from the required-work high-water mark", true,
                    custody.consumedManualActions() >= 0
                            && custody.consumedManualActions() <= custody.requiredManualActions());
            // Installation reserves one paid unit, not every unit with the same resource identity.
            // Compare independent executed/delivered funding with all physical custody axes.
            var physical = VerifySustainability.physicalCustody(custody);
            for (Resource block : custody.installedMachines()) {
                long free = custody.freeStock().getOrDefault(block, 0L);
                long locked = custody.retainedCatalysts().getOrDefault(block, 0L);
                long parts = custody.installationParts().values().stream()
                        .mapToLong(requirements -> requirements.getOrDefault(block, 0L)).reduce(0L, Math::addExact);
                String context = block.key() + " [installed=1, free=" + free
                        + ", locked=" + locked + ", installation_parts=" + parts + "]";
                eq("installed construction and separately funded stock conserve physical units: " + context,
                        VerifySustainability.constructionSupply(custody, block), physical.get(block));
                eq("no construction custody axis borrows an installed unit: " + context, true,
                        free >= 0 && locked >= 0 && custody.installationParts().values().stream()
                                .allMatch(requirements -> requirements.getOrDefault(block, 0L) >= 0));
            }
            eq("formed surrounding parts remain physically reserved", true, !custody.installationParts().isEmpty());
            eq("every inherited one-shot has exactly one completed batch", true, custody.claimedRoutes().stream()
                    .allMatch(id -> custody.establishmentBatches().getOrDefault(id, 0L) == 1));
        }
        eq("all concrete T0 findings remain hard failures", 0L, result.findings().findings().stream()
                .filter(finding -> finding.detail().equals("T0")).count());
        System.out.println(passed + " passed, " + failed + " failed; conditional model only, no Tier acceptance.");
        if (failed != 0) {
            throw new AssertionError("T0 conditional establishment is not proved; inspect the failure-visible report.");
        }
    }

    private static Map<String, Long> quantities(Map<Resource, Long> amounts) {
        Map<String, Long> result = new TreeMap<>();
        amounts.forEach((resource, amount) -> result.put(resource.key(), amount));
        return result;
    }

    private static String revision(Path root) throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "--no-pager", "rev-parse", "HEAD").directory(root.toFile())
                .redirectErrorStream(true).start();
        String revision = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (git.waitFor() != 0 || !revision.matches("[0-9a-f]{40}")) {
            throw new IOException("Cannot label the conditional T0 report with its actual source revision.");
        }
        return revision;
    }

    private static void eq(String message, Object expected, Object actual) {
        if (java.util.Objects.equals(expected, actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + message + ": expected " + expected + ", got " + actual);
        }
    }
}
