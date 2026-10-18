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
        eq("a finite conditional entry is explicitly identified", template.id(), derivation.profile().id());
        eq("the entire prototype route inventory remains present", true,
                snapshot.routes().keySet().containsAll(sources.snapshot().routes().keySet()));
        eq("the T0 ledger is authored rather than a silently successful unknown", null, tier.unknown());
        eq("future unaccepted tiers remain unknown", true, result.tiers().get(1).unknown() != null);
        eq("future unknowns are not a global sustainability certificate", false, result.sustainabilityPass());
        eq("the active declaration cannot waive debt", 0, debt.entries().size());
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
        Resource exit = annotations.ledger().get(0).exitObjective();
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
