package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Counterexamples for the S1 adapters, report provenance and debt ratchet.
 * All invented resources and ordered packs are fixtures, never canonical gameplay evidence.
 */
public final class VerifySustainabilityAdapters {
    private static int passed;
    private static int failed;

    private VerifySustainabilityAdapters() {
    }

    /** Runs the independent adapter fixtures through the existing check discovery convention. */
    public static void main(String[] args) {
        try {
            debtCannotWaiveGeneratedDebt();
            tagComposition();
            registryEntriesAndRemovals();
            checkedDeclarationInventory();
            canonicalInventory();
            reportRelationships();
            contentAndImplementationIdentity();
        } catch (Exception failure) {
            failed++;
            failure.printStackTrace(System.out);
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed != 0) {
            throw new AssertionError("Sustainability adapter counterexamples failed");
        }
    }

    private static void debtCannotWaiveGeneratedDebt() {
        DebtManifest first = manifest("a:b");
        DebtManifest chain = manifest("a:b", "stale_debt:a:b");
        DebtManifest.Applied result = chain.apply(List.of());
        eq("a waiver chain cannot pass without any original error", false, result.ratchetPass());
        eq("both absent original errors create active stale debt", 2L, result.activeErrors());
        eq("generated stale errors are never waived", 0L, result.waivedDebt());
        eq("a waiver chain is not error free", false, result.errorFree());
        eq("both stale fingerprints remain visible", Set.of("stale_debt:a:b", "stale_debt:stale_debt:a:b"),
                fingerprints(result));
        for (AuditFinding finding : result.findings()) {
            eq("generated debt retains error severity", AuditFinding.Severity.ERROR, finding.severity());
            eq("generated debt retains active disposition", AuditFinding.Disposition.ACTIVE, finding.disposition());
        }

        result = first.apply(List.of());
        eq("a single stale entry fails the ratchet", false, result.ratchetPass());
        eq("a single stale entry has one active error", 1L, result.activeErrors());
        eq("a single stale entry has no waiver", 0L, result.waivedDebt());
        eq("a single stale entry is failure visible", false, result.errorFree());
        eq("a single stale entry has its exact fingerprint", Set.of("stale_debt:a:b"), fingerprints(result));

        AuditFinding error = new AuditFinding("a", "b", "", AuditFinding.Severity.ERROR,
                "Original fixture error", List.of("fixture:authority"));
        result = first.apply(List.of(error));
        eq("an actual existing error can satisfy the ratchet", true, result.ratchetPass());
        eq("a real waiver removes only the active count", 0L, result.activeErrors());
        eq("a real waiver retains the failure-visible debt count", 1L, result.waivedDebt());
        eq("a real waiver is not sustainability success", false, result.errorFree());
        eq("a real waiver retains severity", AuditFinding.Severity.ERROR, result.findings().get(0).severity());
        eq("a real waiver has the debt disposition", AuditFinding.Disposition.WAIVED_DEBT,
                result.findings().get(0).disposition());
        eq("applying debt never mutates the original finding", AuditFinding.Disposition.ACTIVE, error.disposition());

        result = chain.apply(List.of(error));
        eq("a real waiver cannot legitimize a second stale entry", false, result.ratchetPass());
        eq("the real original error alone is waived", 1L, result.waivedDebt());
        eq("the absent generated-error target remains active", 1L, result.activeErrors());
        AuditFinding stale = result.findings().stream().filter(finding -> finding.rule().equals("stale_debt"))
                .findFirst().orElseThrow();
        eq("the chained stale entry targets the absent original fingerprint",
                "stale_debt:stale_debt:a:b", stale.fingerprint());
        eq("the chained stale entry stays active", AuditFinding.Disposition.ACTIVE, stale.disposition());

        AuditFinding warning = new AuditFinding("a", "b", "", AuditFinding.Severity.WARNING,
                "Not an original error", List.of());
        result = first.apply(List.of(warning));
        eq("a warning cannot satisfy an error waiver", false, result.ratchetPass());
        eq("the original warning survives", 1L, result.warnings());
        eq("the invalid waiver stays active", 1L, result.activeErrors());
        eq("an invalid waiver is never counted as waived debt", 0L, result.waivedDebt());

        AuditFinding originalStale = new AuditFinding("stale_debt", "a:b", "", AuditFinding.Severity.ERROR,
                "An original error sharing the generated fingerprint", List.of("fixture:original"));
        result = chain.apply(List.of(originalStale));
        eq("a generated stale error wins a collision with an original waiver", false, result.ratchetPass());
        eq("colliding generated errors retain active disposition", 1L, result.activeErrors());
        eq("a colliding original waiver cannot hide newly generated debt", 0L, result.waivedDebt());
    }

    private static void tagComposition() throws IOException {
        Path fixture = Path.of("build", "check-fixtures", "sustainability-adapters", "tag-composition")
                .toAbsolutePath().normalize();
        Path data = fixture.resolve(Path.of("common", "src", "main", "resources", "data"));
        Map<String, List<URL>> packs = new TreeMap<>();
        List<Path> authored = new ArrayList<>();
        Files.createDirectories(data);
        try {
            Resource combined = tag("minecraft:fixture/combined");
            authored.add(local(data, combined, "{\"replace\":false,\"values\":[\"fixture:a\"]}"));
            packs.put(resourcePath(combined), List.of(memory("combined-base", "{\"values\":[\"fixture:b\"]}")));
            TagResolver resolver = new TagResolver(fixture, ordered(packs));
            TagResolution resolution = resolver.resolve(combined);
            eq("append composes classpath base and local values", Set.of(item("a"), item("b")), resolution.members());
            eq("explicitly ordered complete sources can resolve a tag", true, resolution.complete());
            eq("composition retains both local and classpath provenance", 2, resolution.sources().size());

            Resource replaced = tag("forge:fixture/replaced");
            String reset = "{\"replace\":true,\"values\":[\"fixture:reset\"]}";
            String before = salted("base", reset, true);
            String after = salted("append", reset, false);
            packs.put(resourcePath(replaced), List.of(memory("before", before),
                    memory("reset", reset), memory("after", after)));
            resolution = new TagResolver(fixture, ordered(packs)).resolve(replaced);
            eq("replacement follows actual priority, not hash sorting",
                    Set.of(item("reset"), item("append")), resolution.members());
            eq("later append survives an earlier replacement", true, resolution.complete());

            Resource repeated = tag("forge:fixture/repeated");
            packs.put(resourcePath(repeated), List.of(memory("same-first", reset),
                    memory("middle", "{\"values\":[\"fixture:middle\"]}"), memory("same-last", reset)));
            resolver = new TagResolver(fixture, ordered(packs));
            resolution = resolver.resolve(repeated);
            eq("identical replacement bytes at a later priority still reset", Set.of(item("reset")),
                    resolution.members());
            eq("identical hashes do not deduplicate distinct priority layers", 3, resolution.sources().size());
            Builder evidence = new Builder();
            resolver.contribute(evidence);
            eq("all repeated resource layers retain separate input identities", 3, evidence.build().inputHashes().size());

            Resource localReset = tag("forge:fixture/local_reset");
            authored.add(local(data, localReset, "{\"replace\":true,\"values\":[\"fixture:a\"]}"));
            packs.put(resourcePath(localReset), List.of(memory("local-reset-base", "{\"values\":[\"fixture:b\"]}")));
            resolution = new TagResolver(fixture, ordered(packs)).resolve(localReset);
            eq("local replacement resets classpath contributions", Set.of(item("a")), resolution.members());
            eq("known local replacement remains complete in the declared fixture scope", true, resolution.complete());

            Resource nested = tag("grindless:fixture/nested");
            authored.add(local(data, nested, "{\"values\":[\"#minecraft:fixture/combined\"]}"));
            resolution = new TagResolver(fixture, ordered(packs)).resolve(nested);
            eq("nested tags use the same composed priority layers", Set.of(item("a"), item("b")), resolution.members());
            eq("nested completeness requires complete dependencies", true, resolution.complete());
            eq("nested provenance includes both dependency layers and the parent", 3, resolution.sources().size());

            Resource cycleA = tag("grindless:fixture/cycle_a");
            Resource cycleB = tag("grindless:fixture/cycle_b");
            authored.add(local(data, cycleA,
                    "{\"values\":[\"fixture:a\",\"#grindless:fixture/cycle_b\"]}"));
            authored.add(local(data, cycleB,
                    "{\"values\":[\"fixture:b\",\"#grindless:fixture/cycle_a\"]}"));
            resolver = new TagResolver(fixture, ordered(packs));
            TagResolution firstA = resolver.resolve(cycleA);
            TagResolution firstB = resolver.resolve(cycleB);
            eq("cycles cannot claim completeness", false, firstA.complete());
            eq("cycles retain explicit unresolved evidence", true,
                    firstA.unresolved().stream().anyMatch(value -> value.startsWith("cycle:")));
            eq("cycles retain only authored reachable members", Set.of(item("a"), item("b")), firstA.members());
            TagResolver reverse = new TagResolver(fixture, ordered(packs));
            eq("cycle resolution is independent of query order for B", firstB, reverse.resolve(cycleB));
            eq("cycle resolution is independent of query order for A", firstA, reverse.resolve(cycleA));

            ClassLoader unordered = new ClassLoader(null) {
                @Override
                public java.util.Enumeration<URL> getResources(String name) {
                    return Collections.enumeration(packs.getOrDefault(name, List.of()));
                }
            };
            resolution = new TagResolver(fixture, unordered, true).resolve(replaced);
            eq("classloader enumeration alone cannot establish pack precedence", false, resolution.complete());
            eq("unknown classpath precedence is explicit", true,
                    resolution.unresolved().contains("classpath_precedence_unknown:" + replaced.key()));
            eq("unverified fallback still retains observed source provenance", 3, resolution.sources().size());
            eq("unverified source enumeration is never labeled actual pack priority", true,
                    resolution.sources().stream().allMatch(source -> source.contains("@enumeration_index=")));

            resolution = new TagResolver(fixture, unordered, false).resolve(combined);
            eq("local-only external tags retain local members", Set.of(item("a")), resolution.members());
            eq("local-only membership is not canonical runtime resolution", false, resolution.complete());
            eq("uninspected classpath membership remains explicit", true,
                    resolution.unresolved().contains("classpath_not_checked:" + combined.key()));

            Resource otherPack = tag("other_mod:fixture/not_canonical");
            TagResolver standalone = new TagResolver(fixture,
                    ignored -> List.of(memory("not-canonical", "{\"values\":[\"fixture:invented\"]}")));
            resolution = standalone.resolve(otherPack);
            eq("fallback never imports arbitrary mod namespaces into the standalone pack", Set.of(), resolution.members());
            eq("a noncanonical missing tag stays unresolved", false, resolution.complete());
        } finally {
            for (Path file : authored) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static void reportRelationships() {
        Resource plate = new Resource(Kind.ITEM, "grindless:iron_plate");
        Resource copper = new Resource(Kind.ITEM, "grindless:copper_plate");
        Resource singleton = tag("forge:plates/iron");
        Resource multiple = tag("forge:fixture/plates");
        Resource incomplete = tag("forge:fixture/incomplete");
        Builder builder = new Builder()
                .tag(singleton, new TagResolution(Set.of(plate), true, Set.of(), List.of("fixture:iron-tag")))
                .tag(multiple, new TagResolution(Set.of(plate, copper), true, Set.of(), List.of("fixture:multi-tag")))
                .tag(incomplete, new TagResolution(Set.of(plate), false, Set.of("fixture:missing"),
                        List.of("fixture:incomplete-tag")))
                .route(route("press/plate/iron", Map.of(item("feed"), 1L), Map.of(), Map.of(singleton, 1L)))
                .route(route("cast/plate/iron", Map.of(item("feed"), 1L), Map.of(), Map.of(singleton, 1L)))
                .route(route("craft/advanced_data_core", Map.of(singleton, 2L), Map.of(), Map.of(item("core"), 1L)))
                .route(route("conditional/output", Map.of(item("feed"), 1L), Map.of(), Map.of(multiple, 1L)))
                .route(route("tag/consumer", Map.of(multiple, 1L), Map.of(), Map.of(item("product"), 1L)))
                .route(route("tag/catalyst", Map.of(item("feed"), 1L), Map.of(singleton, 1L), Map.of(item("product"), 1L)))
                .route(route("unknown/output", Map.of(item("feed"), 1L), Map.of(), Map.of(incomplete, 1L)))
                .route(route("unknown/consumer", Map.of(incomplete, 1L), Map.of(), Map.of(item("product"), 1L)))
                .route(route("direct/output", Map.of(item("feed"), 1L), Map.of(), Map.of(copper, 1L)));
        JsonObject report = document(builder.build());
        JsonObject iron = resourceRow(report, plate);
        eq("a concrete singleton member lists both actual tagged producers",
                Set.of("press/plate/iron", "cast/plate/iron"), strings(iron.getAsJsonArray("producers")));
        eq("tagged consumption and catalysts are projected onto a concrete member",
                Set.of("craft/advanced_data_core", "tag/consumer", "tag/catalyst"),
                strings(iron.getAsJsonArray("current_consumers")));
        JsonObject press = relationship(iron, "producer_provenance", "press/plate/iron");
        eq("singleton production retains its tag-qualified identity", singleton.key(), press.get("via").getAsString());
        eq("singleton production identifies its proof", "complete_singleton_tag", press.get("certainty").getAsString());
        eq("singleton production is not merely conditional membership", true, press.get("unconditional").getAsBoolean());
        eq("singleton production retains tag provenance", Set.of("fixture:iron-tag"),
                strings(press.getAsJsonArray("tag_sources")));
        JsonObject candidate = relationship(iron, "producer_provenance", "conditional/output");
        eq("multi-member output is never a proven supplier of every candidate", false,
                candidate.get("unconditional").getAsBoolean());
        eq("multi-member output states its uncertainty", false, candidate.get("uncertainty").isJsonNull());
        eq("multi-member output retains the qualified tag", multiple.key(), candidate.get("via").getAsString());
        eq("the tag itself still lists its actual producer", Set.of("conditional/output"),
                strings(resourceRow(report, multiple).getAsJsonArray("producers")));
        eq("complete multi-member inputs accept either member",
                Set.of("tag/consumer"), strings(resourceRow(report, copper).getAsJsonArray("current_consumers")));
        eq("direct exact-resource producers are preserved", Set.of("direct/output"),
                strings(resourceRow(report, copper).getAsJsonArray("producers")));
        eq("incomplete singleton membership cannot prove production", false,
                relationship(iron, "producer_provenance", "unknown/output").get("unconditional").getAsBoolean());
        eq("incomplete input membership cannot prove acceptance", false,
                relationship(iron, "consumer_provenance", "unknown/consumer").get("unconditional").getAsBoolean());
    }

    private static void registryEntriesAndRemovals() throws IOException {
        Path fixture = Path.of("build", "check-fixtures", "sustainability-adapters", "registry-entries")
                .toAbsolutePath().normalize();
        Path data = fixture.resolve(Path.of("common", "src", "main", "resources", "data"));
        Map<String, List<URL>> packs = new TreeMap<>();
        List<Path> authored = new ArrayList<>();
        Files.createDirectories(data);
        TagResolver.ElementLookup registry = resource -> switch (resource.id()) {
            case "fixture:a", "fixture:b" -> TagResolver.ElementStatus.PRESENT;
            case "fixture:unknown" -> TagResolver.ElementStatus.UNKNOWN;
            default -> TagResolver.ElementStatus.ABSENT;
        };
        try {
            Resource optional = tag("grindless:fixture/optional");
            authored.add(local(data, optional, """
                    {"values":["fixture:a",{"id":"fixture:absent","required":false},
                    {"id":"#forge:fixture/absent","required":false}]}
                    """));
            TagResolution resolution = new TagResolver(fixture, ordered(packs), registry).resolve(optional);
            eq("registry-proven absent optional elements/tags are not invented", Set.of(item("a")), resolution.members());
            eq("absent optional entries do not invalidate a tag", true, resolution.complete());

            Resource required = tag("grindless:fixture/required");
            authored.add(local(data, required, "{\"values\":[\"fixture:a\",\"fixture:absent\"]}"));
            resolution = new TagResolver(fixture, ordered(packs), registry).resolve(required);
            eq("required absent elements never become members", Set.of(item("a")), resolution.members());
            eq("a required missing registry element invalidates completeness", false, resolution.complete());
            eq("a missing required element retains exact evidence", true,
                    resolution.unresolved().contains("missing_element:item:fixture:absent"));

            Resource unknown = tag("grindless:fixture/unknown");
            authored.add(local(data, unknown,
                    "{\"values\":[\"fixture:a\",{\"id\":\"fixture:unknown\",\"required\":false}]}"));
            resolution = new TagResolver(fixture, ordered(packs), registry).resolve(unknown);
            eq("unbound optional membership is not fabricated beside a real member", Set.of(item("a")), resolution.members());
            eq("unknown optional membership cannot certify a falsely complete singleton", false, resolution.complete());
            eq("the unbound registry element remains explicit", true,
                    resolution.unresolved().contains("registry_membership_unknown:item:fixture:unknown"));

            Resource removed = tag("forge:fixture/removed");
            packs.put(resourcePath(removed), List.of(
                    memory("remove-base", "{\"values\":[\"fixture:a\",\"fixture:b\"]}"),
                    memory("remove-high", "{\"values\":[],\"remove\":[\"fixture:a\",\"fixture:absent\"]}")));
            resolution = new TagResolver(fixture, ordered(packs), registry).resolve(removed);
            eq("Forge remove is applied after the layer's additions", Set.of(item("b")), resolution.members());
            eq("Forge treats missing removals as optional", true, resolution.complete());
            packs.put(resourcePath(removed), List.of(
                    memory("remove-low", "{\"values\":[\"fixture:a\"],\"remove\":[\"fixture:a\"]}"),
                    memory("readd-high", "{\"values\":[\"fixture:a\"]}")));
            resolution = new TagResolver(fixture, ordered(packs), registry).resolve(removed);
            eq("later additions can reintroduce an earlier removed member", Set.of(item("a")), resolution.members());

            Resource uncertain = tag("forge:fixture/uncertain_order");
            TagResolver.OrderedClasspath unproved = new TagResolver.OrderedClasspath() {
                @Override
                public List<URL> resources(String path) throws IOException {
                    return List.of(memory("unproved", "{\"replace\":true,\"values\":[\"fixture:a\"]}"));
                }

                @Override
                public Set<String> uncertainties(String path) {
                    return Set.of("fixture_pack_priority_unproven");
                }
            };
            resolution = new TagResolver(fixture, unproved, registry).resolve(uncertain);
            eq("replacement cannot erase caller-declared order uncertainty", false, resolution.complete());
            eq("caller-declared order uncertainty remains attributable", true,
                    resolution.unresolved().contains("fixture_pack_priority_unproven"));
        } finally {
            for (Path file : authored) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static void checkedDeclarationInventory() {
        Resource declaredItem = item("checked_item");
        Resource declaredBlock = new Resource(Kind.ITEM, "fixture:checked_block", 0, "blocks");
        Map<Resource, String> declarations = Map.of(declaredItem, "fixture:checked_field",
                declaredBlock, "fixture:provenance_is_not_code/register(\"ghost\")/\"base\"+suffix");
        TagResolver.ElementLookup checked = new TagResolver.ElementLookup() {
            @Override
            public TagResolver.ElementStatus element(Resource resource) {
                return TagResolver.ElementStatus.UNKNOWN;
            }

            @Override
            public Map<Resource, String> declarations() {
                return declarations;
            }
        };
        Builder builder = new Builder().resource(item("catalogue_reference"));
        AuthorityAdapters.contributeCheckedDeclarations(builder, checked);
        Snapshot inventory = builder.build();
        eq("checked inventory is exactly catalogue references plus verified declarations, without extras",
                Set.of(item("catalogue_reference"), declaredItem, declaredBlock), inventory.resources());
        eq("the inventory and tag lookup consume the same declaration set", declarations.keySet(),
                checked.declarations().keySet());
        eq("public declaration provenance is retained for every checked declaration", 2L,
                inventory.inputHashes().keySet().stream().filter(key -> key.startsWith("registry_declaration:")).count());
        eq("an inventory reference is not a registry membership proof", TagResolver.ElementStatus.UNKNOWN,
                checked.element(declaredItem));
        eq("extending a snapshot starts with all of its authorities and evidence intact", inventory,
                new Builder(inventory).build());
        new Builder(inventory).resource(item("fixture:extra_checked_fact")).build();
        eq("extending a snapshot cannot mutate the original inventory", false,
                inventory.resources().contains(item("fixture:extra_checked_fact")));
        Builder unbound = new Builder();
        AuthorityAdapters.contributeCheckedDeclarations(unbound, ignored -> TagResolver.ElementStatus.UNKNOWN);
        eq("unbound registration evidence does not invent prototype inventory", Set.of(), unbound.build().resources());
        eq("unbound declaration inventory is an explicit finding, not a successful empty registry", true,
                unbound.build().findings().stream()
                        .anyMatch(finding -> finding.rule().equals("canonical_registration_inventory_unbound")));
    }

    private static void canonicalInventory() throws IOException, InterruptedException {
        Path root = Path.of("").toAbsolutePath().normalize();
        CanonicalPack pack = CanonicalPack.resolved(root);
        Resource iron = new Resource(Kind.TAG, "forge:ingots/iron");
        Resource copper = new Resource(Kind.TAG, "forge:ingots/copper");
        Resource planks = new Resource(Kind.TAG, "minecraft:planks");
        TagResolver resolver = new TagResolver(root, pack, ignored -> TagResolver.ElementStatus.UNKNOWN);
        TagResolution resolution = resolver.resolve(iron);
        eq("resolved Forge iron data contains its actual declared vanilla ingot", Set.of(
                new Resource(Kind.ITEM, "minecraft:iron_ingot")), resolution.members());
        eq("a real Forge JSON file is not registry-membership proof by itself", false, resolution.complete());
        eq("Forge registry lookup remains bounded and explicit when not supplied", Set.of(
                "registry_membership_unknown:item:minecraft:iron_ingot"), resolution.unresolved());
        eq("Forge iron provenance identifies the actual resolved pack, not a local fiction", true,
                resolution.sources().stream().anyMatch(source -> source.contains("@pack=forge@")));
        resolution = resolver.resolve(copper);
        eq("resolved Forge copper data retains its actual declaration", Set.of(
                new Resource(Kind.ITEM, "minecraft:copper_ingot")), resolution.members());
        resolution = resolver.resolve(planks);
        eq("resolved vanilla planks are discovered without a cache scan", true, !resolution.members().isEmpty());
        eq("vanilla pack provenance is separate from Forge", true,
                resolution.sources().stream().anyMatch(source -> source.contains("@pack=vanilla@")));
        eq("all registry-kind pack tag definitions are independently inventoried", true,
                pack.tags().containsAll(Set.of(iron, copper, planks)));
        Snapshot first = AuthorityAdapters.canonical(root, ignored -> TagResolver.ElementStatus.UNKNOWN);
        Snapshot second = AuthorityAdapters.canonical(root, ignored -> TagResolver.ElementStatus.UNKNOWN);
        eq("canonical authority content is deterministic", AuditReports.authorityHash(first),
                AuditReports.authorityHash(second));
        eq("unbound canonical tags do not supply registration declarations", 0L,
                first.inputHashes().keySet().stream().filter(source -> source.startsWith("registry_declaration:")).count());
        eq("a block-only registration never invents a registered item", false,
                first.resources().contains(new Resource(Kind.ITEM, "grindless:horizon_shell")));
        eq("a block reference remains inventoried without being promoted to a checked registration", true,
                first.resources().contains(new Resource(Kind.ITEM, "grindless:horizon_shell", 0, "blocks")));
        eq("unbound canonical registration inventory remains explicitly incomplete", true,
                first.findings().stream().anyMatch(finding -> finding.rule().equals("canonical_registration_inventory_unbound")));
        eq("resolved artifacts have real binary hashes", 2L, first.inputHashes().keySet().stream()
                .filter(source -> source.startsWith("resolved_pack:")).count());
        eq("public authority identities never expose the private classpath", false,
                first.inputHashes().keySet().stream().anyMatch(source -> source.contains(root.toString())
                        || source.contains(":\\") || source.startsWith("\\\\")));
        eq("a missing local file is not reported as missing canonical Forge data", false,
                first.findings().stream().anyMatch(finding -> finding.rule().equals("canonical_tag_resolution_unknown")
                        || finding.rule().equals("unresolved_tag") && finding.subject().equals(iron.key())));
        DebtManifest debt = DebtManifest.read(root.resolve(Path.of("tools", "sustainability", "debt.json")));
        Process git = new ProcessBuilder("git", "--no-pager", "rev-parse", "HEAD").directory(root.toFile())
                .redirectErrorStream(true).start();
        String revision = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (git.waitFor() != 0 || !revision.matches("[0-9a-f]{40}")) {
            throw new IOException("Cannot label the canonical inventory with its actual source revision.");
        }
        Path reports = root.resolve(Path.of("build", "sustainability", "canonical-inventory-unbound"));
        var audit = SustainabilityAudit.run(first, SustainabilityAnnotations.empty(), debt);
        var repeatedAudit = SustainabilityAudit.run(second, SustainabilityAnnotations.empty(), debt);
        AuditReports.Hashes written = AuditReports.write(reports, audit, revision, true);
        AuditReports.Hashes repeated = AuditReports.write(reports, repeatedAudit, revision, true);
        eq("canonical JSON exports are byte-deterministic", written.json(), repeated.json());
        eq("canonical Markdown exports are byte-deterministic", written.markdown(), repeated.markdown());
        JsonObject document = JsonParser.parseString(Files.readString(reports.resolve("audit.json"))).getAsJsonObject();
        eq("an unannotated canonical inventory is not the old local-only S1 scope",
                "report_only_s2_canonical_inventory_no_annotations", document.get("mode").getAsString());
        eq("a canonical inventory cannot certify sustainability without an accepted scope", false,
                document.get("sustainability_pass").getAsBoolean());
        eq("the public report includes artifact/API checksums rather than private classpath URLs", true,
                document.getAsJsonObject("resolved_pack_hashes").size() >= 7);
        System.out.println("CANONICAL RESOURCE INVENTORY: " + first.resources().size() + " resources, "
                + first.routes().size() + " routes, " + first.tags().size() + " referenced/local tags, "
                + first.objectives().size() + " objectives; registry membership intentionally unbound in this fixture.");
        System.out.println("CANONICAL JSON SHA-256 " + written.json());
        System.out.println("CANONICAL MARKDOWN SHA-256 " + written.markdown());
    }

    private static void contentAndImplementationIdentity() throws IOException {
        Snapshot first = energySnapshot(100, "first-source");
        Snapshot second = energySnapshot(101, "first-source");
        JsonObject firstReport = document(first);
        JsonObject secondReport = document(second);
        eq("the same source-revision label is retained", firstReport.get("source_revision"), secondReport.get("source_revision"));
        eq("identical source manifests are distinguishable from changed evaluated quantities",
                firstReport.get("authority_sources_sha256"), secondReport.get("authority_sources_sha256"));
        different("changed evaluated energy changes the normalized authority snapshot",
                firstReport.get("authority_snapshot_sha256"), secondReport.get("authority_snapshot_sha256"));
        different("changed evaluated energy changes the content-bound authority identity",
                AuditReports.authorityHash(first), AuditReports.authorityHash(second));
        eq("unchanged authority extraction is deterministic", AuditReports.authorityHash(first),
                AuditReports.authorityHash(energySnapshot(100, "first-source")));
        AuditFinding firstFinding = new AuditFinding("fixture_a", "resource", "", AuditFinding.Severity.UNKNOWN,
                "First raw authority finding", List.of("fixture:z", "fixture:a"));
        AuditFinding secondFinding = new AuditFinding("fixture_b", "resource", "", AuditFinding.Severity.WARNING,
                "Second raw authority finding", List.of("fixture:b"));
        Snapshot ordered = new Builder().route(route("b", Map.of(item("fuel"), 1L), Map.of(), Map.of(ENERGY, 100L)))
                .route(route("a", Map.of(item("feed"), 1L), Map.of(), Map.of(item("product"), 2L)))
                .finding(firstFinding).finding(secondFinding).build();
        Snapshot reversed = new Builder().route(route("a", Map.of(item("feed"), 1L), Map.of(), Map.of(item("product"), 2L)))
                .route(route("b", Map.of(item("fuel"), 1L), Map.of(), Map.of(ENERGY, 100L)))
                .finding(secondFinding).finding(firstFinding).build();
        eq("snapshot normalization ignores route and raw-finding insertion order",
                AuditReports.snapshotHash(ordered), AuditReports.snapshotHash(reversed));

        Map<String, String> sources = AuthorityAdapters.authoritySourceHashes(Path.of(""));
        String javaRoot = "common/src/main/java/io/github/ezequiel24123z/grindless/";
        for (String dependency : List.of("energy/FluxTier.java", "recipe/FabricationLogic.java",
                "recipe/ProcessLogic.java", "recipe/ProcessRecipe.java", "recipe/MachineFamily.java")) {
            eq("transitive authority source is fingerprinted: " + dependency, true, sources.containsKey(javaRoot + dependency));
        }
        eq("authority source identities never contain an absolute path", false,
                sources.keySet().stream().anyMatch(value -> value.contains(":") || value.startsWith("/")));

        AuthorityAdapters.ImplementationIdentity executed = AuthorityAdapters.implementationIdentity();
        String reportClass = "io/github/ezequiel24123z/grindless/sustainability/AuditReports.class";
        eq("the actual report implementation has a separate binary identity", true,
                executed.classHashes().containsKey(reportClass));
        eq("all S1 implementation bytes were available", Set.of(), executed.unresolved());
        try (InputStream bytes = AuditReports.class.getResourceAsStream("AuditReports.class")) {
            eq("implementation identity hashes the resolved report class bytes",
                    AuthorityAdapters.sha256(bytes.readAllBytes()), executed.classHashes().get(reportClass));
        }
        Map<String, String> changedClasses = new TreeMap<>(executed.classHashes());
        changedClasses.put(reportClass, AuthorityAdapters.sha256("different report implementation".getBytes(StandardCharsets.UTF_8)));
        AuthorityAdapters.ImplementationIdentity changed = new AuthorityAdapters.ImplementationIdentity(changedClasses, Set.of());
        different("report implementation changes are independently distinguishable", executed.sha256(), changed.sha256());
        eq("reports publish the executed S1 implementation hash", executed.sha256(),
                firstReport.get("implementation_sha256").getAsString());
        eq("reports never claim a commit-clean implementation identity",
                "resolved_s1_class_resources_commit_cleanliness_unverified",
                firstReport.get("implementation_state").getAsString());
        eq("source revision labels never imply commit cleanliness",
                "working_tree_content_hashed_commit_cleanliness_unverified",
                firstReport.get("source_state").getAsString());

        JsonObject changedSource = document(energySnapshot(100, "uncommitted-report-source"));
        eq("implementation source edits do not pretend to change authority quantities",
                firstReport.get("authority_sha256"), changedSource.get("authority_sha256"));
        different("uncommitted implementation source edits have a separate identity",
                firstReport.get("implementation_source_sha256"), changedSource.get("implementation_source_sha256"));
    }

    private static Snapshot energySnapshot(long energy, String implementationSource) {
        return new Builder().input("fixture/authority.java", "unchanged-authority-source")
                .input("implementation_source:fixture/AuditReports.java", implementationSource)
                .route(route("fixture/energy", Map.of(item("fuel"), 1L), Map.of(), Map.of(ENERGY, energy))).build();
    }

    private static JsonObject document(Snapshot snapshot) {
        DebtManifest debt = DebtManifest.empty();
        SustainabilityAudit.Result result = new SustainabilityAudit.Result(snapshot, SustainabilityAnnotations.empty(),
                Map.of(), debt, debt.apply(List.of()));
        return JsonParser.parseString(AuditReports.json(result, "same-source-revision", true)).getAsJsonObject();
    }

    private static JsonObject resourceRow(JsonObject report, Resource resource) {
        for (var value : report.getAsJsonArray("resources")) {
            JsonObject row = value.getAsJsonObject();
            if (row.get("id").getAsString().equals(resource.key())) {
                return row;
            }
        }
        throw new AssertionError("Missing resource " + resource.key());
    }

    private static JsonObject relationship(JsonObject resource, String field, String route) {
        for (var value : resource.getAsJsonArray(field)) {
            JsonObject row = value.getAsJsonObject();
            if (row.get("route").getAsString().equals(route)) {
                return row;
            }
        }
        throw new AssertionError("Missing " + field + " relationship " + route);
    }

    private static Set<String> strings(JsonArray values) {
        Set<String> result = new TreeSet<>();
        values.forEach(value -> result.add(value.getAsString()));
        return result;
    }

    private static DebtManifest manifest(String... fingerprints) {
        Map<String, DebtManifest.Entry> entries = new TreeMap<>();
        for (String fingerprint : fingerprints) {
            entries.put(fingerprint, new DebtManifest.Entry(fingerprint, "fixture:source",
                    "Remove this exact fixture violation", "fixture-adapters"));
        }
        return new DebtManifest(1, 1, entries);
    }

    private static Set<String> fingerprints(DebtManifest.Applied result) {
        Set<String> values = new TreeSet<>();
        result.findings().forEach(finding -> values.add(finding.fingerprint()));
        return values;
    }

    private static Route route(String id, Map<Resource, Long> inputs, Map<Resource, Long> catalysts,
                               Map<Resource, Long> outputs) {
        return new Route(id, Authority.PROCESS_GRAPH, "fixture:" + id, null, inputs, catalysts, outputs,
                Set.of(), 0, 0, Map.of(), Set.of(), false, true);
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, "fixture:" + id);
    }

    private static Resource tag(String id) {
        return new Resource(Kind.TAG, id);
    }

    private static String resourcePath(Resource tag) {
        String[] id = tag.id().split(":", 2);
        return "data/" + id[0] + "/tags/" + tag.registry() + "/" + id[1] + ".json";
    }

    private static Path local(Path data, Resource tag, String json) throws IOException {
        String[] id = tag.id().split(":", 2);
        Path file = data.resolve(id[0]).resolve("tags").resolve(tag.registry());
        for (String part : (id[1] + ".json").split("/")) {
            file = file.resolve(part);
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }

    private static TagResolver.OrderedClasspath ordered(Map<String, List<URL>> packs) {
        return path -> packs.getOrDefault(path, List.of());
    }

    private static URL memory(String id, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new URL(null, "fixture:/" + id, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(bytes);
                    }
                };
            }
        });
    }

    private static String salted(String member, String reset, boolean greater) {
        String resetHash = AuthorityAdapters.sha256(reset.getBytes(StandardCharsets.UTF_8));
        for (int salt = 0; salt < 10_000; salt++) {
            String json = "{\"values\":[\"fixture:" + member + "\"],\"fixture_identity\":" + salt + "}";
            int comparison = AuthorityAdapters.sha256(json.getBytes(StandardCharsets.UTF_8)).compareTo(resetHash);
            if (greater ? comparison > 0 : comparison < 0) {
                return json;
            }
        }
        throw new AssertionError("Could not construct adversarial hash ordering");
    }

    private static void different(String message, Object first, Object second) {
        eq(message, false, java.util.Objects.equals(first, second));
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
