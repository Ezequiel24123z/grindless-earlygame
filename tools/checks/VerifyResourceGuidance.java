package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.client.ResourceGuidanceClient;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidance;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceJson;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceProjection;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceRuntime;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.RenewableProcesses;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Optional;

import static io.github.ezequiel24123z.grindless.guidance.ResourceGuidance.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Pure projection/codec/presentation and source-surface checks. Fixtures are never exported as
 * accepted gameplay. The actual canonical parity/drift check also runs in VerifySustainabilityT0.
 */
public final class VerifyResourceGuidance {
    private static final String PREFIX = "tooltip.grindless.resource.";
    private static final Path COMMON = Path.of("common", "src", "main", "java", "io", "github",
            "ezequiel24123z", "grindless");
    private static final Path FORGE = Path.of("forge", "src", "main", "java", "io", "github",
            "ezequiel24123z", "grindless", "forge");
    private static final Path LANGUAGE = Path.of("common", "src", "main", "resources", "assets",
            "grindless", "lang", "en_us.json");
    private static int assertions;

    private VerifyResourceGuidance() {
    }

    public static void main(String[] args) throws Exception {
        vocabulary();
        nullsAndHandling();
        playerReadingContext();
        identities();
        references();
        malformed();
        lineEndings();
        loader();
        contexts();
        authoredMatrixContexts();
        matrixDeclarations();
        surfaces(Path.of("").toAbsolutePath().normalize());
        System.out.println("RESOURCE GUIDANCE: " + assertions
                + " passed; pure/source checks only, not generated canonical, rendered client or Tier acceptance.");
    }

    /** Exhaustive equality with the actual Result, not a pasted report or a second catalogue. */
    public static int verifyProjection(SustainabilityAudit.Result result, Document projection) {
        int start = assertions;
        equal("all and only authored resource keys", result.annotations().resources().keySet(), projection.resources().keySet());
        result.annotations().resources().forEach((resource, expected) -> {
            ResourceGuidance.Entry actual = projection.resources().get(resource);
            equal(resource.key() + " preserves every declaration including nulls", expected, actual.metadata());
            equal(resource.key() + " resource class", expected.resourceClass(), actual.metadata().resourceClass());
            equal(resource.key() + " side-stream class", expected.sideStreamClass(), actual.metadata().sideStreamClass());
            equal(resource.key() + " first-use known/unknown", expected.firstUseTier(), actual.metadata().firstUseTier());
            equal(resource.key() + " renewable tier known/unknown", expected.firstRenewableTier(), actual.metadata().firstRenewableTier());
            equal(resource.key() + " canonical route", expected.canonicalRoute(), actual.metadata().canonicalRoute());
            equal(resource.key() + " future consumers", expected.futureConsumers(), actual.metadata().futureConsumers());
            equal(resource.key() + " handling/action/discard/hazard/containment/treatment", expected.handling(), actual.metadata().handling());
            equal(resource.key() + " recycling route/planned tier", expected.recycling(), actual.metadata().recycling());
            equal(resource.key() + " spent catalyst origin", expected.originatingCatalyst(), actual.metadata().originatingCatalyst());
            equal(resource.key() + " uncertainty", expected.uncertainty(), actual.metadata().uncertainty());
            verifyReferences(result, resource, actual.producers(), true);
            verifyReferences(result, resource, actual.consumers(), false);
            Map<String, String> expectedSources = new TreeMap<>();
            for (Terminal terminal : result.authorities().terminals().values()) {
                if (terminal.resource().equals(resource)) {
                    expectedSources.put("terminal:" + terminal.id(), terminal.source());
                }
            }
            source(result, expectedSources, "canonical", expected.canonicalRoute());
            if (expected.handling() != null) {
                source(result, expectedSources, "treatment", expected.handling().treatmentRoute());
            }
            if (expected.recycling() != null) {
                source(result, expectedSources, "recycling", expected.recycling().route());
            }
            expected.futureConsumers().forEach(consumer -> {
                var plan = result.annotations().plans().get(consumer.planReference());
                expectedSources.put("plan:" + plan.reference(), plan.source());
            });
            equal(resource.key() + " exact public source references", expectedSources, actual.sources());
        });
        equal("strict typed round trip", projection, ResourceGuidanceJson.read(ResourceGuidanceJson.write(projection)));
        equal("repeated projections are deterministic", projection, ResourceGuidanceProjection.project(result));
        return assertions - start;
    }

    public static int verifyCanonical(Path root, SustainabilityAudit.Result result) throws IOException {
        int start = assertions;
        Document expected = ResourceGuidanceProjection.project(result);
        Path asset = root.resolve(Path.of("common", "src", "main", "resources")).resolve(ResourceGuidance.ASSET);
        if (!Files.isRegularFile(asset)) {
            throw new IOException("Canonical resource guidance is missing. Run tools/generate-resource-guidance.ps1 "
                    + "(or its .sh twin) against these real authorities, then rebuild resources.");
        }
        String bytes = Files.readString(asset, StandardCharsets.UTF_8);
        Document actual = checkedAsset(expected, bytes);
        verifyProjection(result, actual);
        verifyLocalization(root, result);
        Resource grog = new Resource(Kind.ITEM, "grindless:ceramic_grog");
        Resource slag = new Resource(Kind.ITEM, "grindless:slag");
        Resource water = new Resource(Kind.FLUID, "minecraft:water");
        Resource flint = new Resource(Kind.ITEM, "minecraft:flint");
        equal("grog retains real T0 grog-body use", true, actual.resources().get(grog).consumers().stream()
                .anyMatch(reference -> reference.route().equals("bootstrap/grog_body")
                        && Integer.valueOf(0).equals(reference.unlockTier()) && reference.unconditional()));
        equal("water and grog both retain truthful reuse", Action.REUSE, actual.resources().get(grog).metadata().handling().action());
        equal("recovered water retains truthful reuse", Action.REUSE, actual.resources().get(water).metadata().handling().action());
        equal("slag never acquires an accepted first use", null, actual.resources().get(slag).metadata().firstUseTier());
        equal("slag's prototype consumer stays tier-unknown", true, actual.resources().get(slag).consumers().stream()
                .anyMatch(reference -> reference.route().equals("ceramic/refractory_brick") && reference.unlockTier() == null));
        equal("slag has no accepted T0 consumer", false, actual.resources().get(slag).consumers().stream()
                .anyMatch(reference -> Integer.valueOf(0).equals(reference.unlockTier())));
        equal("slag remains uncertain potential use", SideStreamClass.POTENTIAL_FUTURE_USE,
                actual.resources().get(slag).metadata().sideStreamClass());
        equal("slag ordinary discard is preserved", true, actual.resources().get(slag).metadata().handling().discardAllowed());
        equal("flint remains optional without a first-use tier", null, actual.resources().get(flint).metadata().firstUseTier());
        equal("flint storage does not forbid ordinary discard", new Handling(Action.STORE, true, false, null, null),
                actual.resources().get(flint).metadata().handling());
        Resource relay = new Resource(Kind.ITEM, "grindless:relay_matrix", 1, "items");
        equal("canonical guidance declares the same physical F1 as the T0 exit", relay,
                result.annotations().ledger().get(0).exitObjective());
        equal("the physical F1 declaration retains finite bootstrap metadata", ResourceClass.BOOTSTRAP,
                actual.resources().get(relay).metadata().resourceClass());
        equal("generic registration inventory does not acquire the physical F1 declaration", false,
                actual.resources().containsKey(new Resource(Kind.ITEM, relay.id())));
        equal("guidance derives the actual manual and automatic physical F1 producers",
                Set.of("craft/relay_matrix", "assemble/relay_matrix"),
                actual.resources().get(relay).producers().stream().map(RouteReference::route)
                        .collect(java.util.stream.Collectors.toSet()));
        return assertions - start;
    }

    private static Document checkedAsset(Document expected, String bytes) {
        Document actual = ResourceGuidanceJson.read(bytes);
        equal("committed generated guidance has no missing/invented/drifted field", expected, actual);
        equal("only checkout line endings may differ from deterministic generated bytes",
                ResourceGuidanceJson.write(expected), bytes.replace("\r\n", "\n"));
        return actual;
    }

    public static int verifyLocalization(Path root, SustainabilityAudit.Result result) throws IOException {
        int start = assertions;
        JsonObject language = JsonParser.parseString(Files.readString(root.resolve(LANGUAGE))).getAsJsonObject();
        result.annotations().resources().forEach((resource, metadata) -> {
            if (metadata.uncertainty() != null) {
                String key = ResourceGuidance.uncertaintyKey(resource);
                equal("canonical uncertainty is localized once for " + resource.key(), true, language.has(key));
                equal("English uncertainty cannot drift from its declaration", metadata.uncertainty(), language.get(key).getAsString());
                String playerKey = ResourceGuidanceClient.playerUncertaintyKey(resource);
                equal("reading surfaces have a player-facing uncertainty for " + resource.key(), true, language.has(playerKey));
                equal("player uncertainty is not an empty success", false, language.get(playerKey).getAsString().isBlank());
            }
        });
        return assertions - start;
    }

    private static void verifyReferences(SustainabilityAudit.Result result, Resource resource,
                                         List<RouteReference> actual, boolean producers) {
        List<Map<String, Object>> expected = AuditReports.relationships(result.authorities(), resource, producers);
        equal("all and only canonical provenance edges for " + resource.key(), expected.size(), actual.size());
        for (Map<String, Object> edge : expected) {
            var policy = result.annotations().routes().get((String) edge.get("route"));
            RouteReference reference = new RouteReference((String) edge.get("route"), (String) edge.get("source"),
                    (String) edge.get("via"), ReferenceKind.valueOf(((String) edge.get("kind")).toUpperCase(Locale.ROOT)),
                    Certainty.valueOf(((String) edge.get("certainty")).toUpperCase(Locale.ROOT)),
                    (boolean) edge.get("unconditional"), policy == null ? null : policy.unlockTier(),
                    policy == null ? null : policy.optional(), (boolean) edge.get("discharged"));
            equal("canonical provenance/tier/null/role preserved: " + edge, true, actual.contains(reference));
        }
    }

    private static void source(SustainabilityAudit.Result result, Map<String, String> target, String kind, String id) {
        if (id != null) {
            target.put(kind + ":" + id, result.authorities().routes().get(id).source());
        }
    }

    private static void vocabulary() {
        for (ResourceClass resourceClass : ResourceClass.values()) {
            for (SideStreamClass side : SideStreamClass.values()) {
                for (Action action : Action.values()) {
                    for (boolean discard : List.of(false, true)) {
                        ResourceMetadata declaration = new ResourceMetadata(resourceClass, side, 0, 15, "fixture/source",
                                List.of(new FutureConsumer(5, "fixture_system", "fixture_plan")),
                                new Handling(action, discard, true, item("fixture:container"), "fixture/current"),
                                new Recycling("fixture/current", 5), item("fixture:catalyst"), "Fixture uncertainty, not gameplay.");
                        var result = fixture(declaration);
                        verifyProjection(result, ResourceGuidanceProjection.project(result));
                    }
                }
            }
        }
    }

    private static void nullsAndHandling() {
        for (Integer use : Arrays.asList(null, 0, 15)) {
            for (Integer renewable : Arrays.asList(null, 0, 15)) {
                ResourceMetadata metadata = new ResourceMetadata(null, null, use, renewable, null, List.of(),
                        null, new Recycling(null, null), null, null);
                var result = fixture(metadata);
                Document projection = ResourceGuidanceProjection.project(result);
                verifyProjection(result, projection);
                var loaded = new ResourceGuidanceRuntime.Loaded(projection, null);
                List<Component> lines = ResourceGuidanceClient.lines(loaded, item("fixture:resource"), true, false);
                equal("unknown resource class is explicitly unknown, never renewable", false,
                        hasTranslation(lines, PREFIX + "class.renewable"));
                equal("null metadata does not acquire a handling action", false, hasTranslation(lines, PREFIX + "handling"));
                equal("declarations are not world acceptance", true, hasTranslation(lines, PREFIX + "declaration"));
            }
        }
        var result = fixture(metadata());
        Document document = ResourceGuidanceProjection.project(result);
        expectFailure("runtime document is immutable", () -> document.resources().clear());
        expectFailure("source references are immutable", () -> document.resources().get(item("fixture:resource")).sources().clear());
        expectFailure("route references are immutable", () -> document.resources().get(item("fixture:resource")).consumers().clear());
        equal("unannotated hover stays quiet", List.of(), ResourceGuidanceClient.lines(
                new ResourceGuidanceRuntime.Loaded(document, null), item("fixture:future"), false, false));
        equal("explicit missing context is visibly unknown", true, hasTranslation(ResourceGuidanceClient.lines(
                new ResourceGuidanceRuntime.Loaded(document, null), item("fixture:future"), true, false), PREFIX + "unknown"));
        equal("unconfirmed fluid identity never becomes water", true, hasTranslation(ResourceGuidanceClient.lines(
                new ResourceGuidanceRuntime.Loaded(document, null), null, true, false), PREFIX + "unknown"));
        equal("unavailable metadata never returns empty success", true, hasTranslation(ResourceGuidanceClient.lines(
                new ResourceGuidanceRuntime.Loaded(null, "missing_asset"), null, false, false), PREFIX + "unavailable"));
        var forbiddenDiscard = new ResourceMetadata(null, null, null, null, null, List.of(),
                new Handling(null, false, true, null, null), null, null, null);
        var hazardous = ResourceGuidanceProjection.project(fixture(forbiddenDiscard));
        List<Component> hazardLines = ResourceGuidanceClient.lines(new ResourceGuidanceRuntime.Loaded(hazardous, null),
                item("fixture:resource"), true, true);
        equal("declared no-discard is not silently changed to unknown or allowed", true,
                hasTranslation(hazardLines, PREFIX + "discard_not_allowed"));
        equal("hazard handling remains visible even with unknown action", true, hasTranslation(hazardLines, PREFIX + "hazardous"));
        equal("unknown containment is not an invented API", true, hasTranslation(hazardLines, PREFIX + "containment"));
        equal("unknown treatment is explicit", true, hasTranslation(hazardLines, PREFIX + "treatment"));
        // This vocabulary/DTO fixture references its actual fixture producer. It is not a
        // Survival construction proof and must not invent a runtime craft/capital authority.
        var capital = new ResourceMetadata(ResourceClass.MANUAL_CAPITAL, null, 0, null, "fixture/source",
                List.of(), new Handling(Action.STORE, true, false, null, null), null, null, null);
        Document capitalDocument = ResourceGuidanceProjection.project(fixture(capital));
        equal("the existing metadata format losslessly carries manual durable capital", capitalDocument,
                ResourceGuidanceJson.read(ResourceGuidanceJson.write(capitalDocument)));
        List<Component> capitalLines = ResourceGuidanceClient.lines(new ResourceGuidanceRuntime.Loaded(capitalDocument, null),
                item("fixture:resource"), true, false);
        equal("manual capital has an explicit localized classification", true,
                hasTranslation(capitalLines, PREFIX + "class.manual_capital"));
        equal("manual capital never borrows the renewable tooltip label", false,
                hasTranslation(capitalLines, PREFIX + "class.renewable"));
        var absentCapital = new ResourceMetadata(ResourceClass.MANUAL_CAPITAL, null, 0, null, "craft/capital",
                List.of(), new Handling(Action.STORE, true, false, null, null), null, null, null);
        try {
            ResourceGuidanceProjection.project(fixture(absentCapital));
            throw new AssertionError("a declared absent capital route must remain a failure");
        } catch (IllegalArgumentException expected) {
            equal("missing capital authority keeps its exact absent-route diagnosis",
                    "A declared guidance route is absent: craft/capital", expected.getMessage());
        }
    }

    private static void identities() {
        Map<Resource, ResourceGuidance.Entry> entries = new TreeMap<>();
        ResourceMetadata unknown = new ResourceMetadata(null, null, null, null, null, List.of(), null, null, null, null);
        for (Kind kind : Kind.values()) {
            entries.put(new Resource(kind, "fixture:identity"), new ResourceGuidance.Entry(unknown, List.of(), List.of(), Map.of()));
        }
        entries.put(new Resource(Kind.TAG, "fixture:identity", 0, "fluids"),
                new ResourceGuidance.Entry(unknown, List.of(), List.of(), Map.of()));
        entries.put(new Resource(Kind.ITEM, "fixture:identity", 0, "blocks"),
                new ResourceGuidance.Entry(unknown, List.of(), List.of(), Map.of()));
        for (int rating = 1; rating <= 15; rating++) {
            entries.put(new Resource(Kind.ITEM, "fixture:identity", rating, "items"),
                    new ResourceGuidance.Entry(unknown, List.of(), List.of(), Map.of()));
        }
        Document document = ResourceGuidanceJson.prepare(entries);
        equal("resource axes, registry and physical ratings never collapse", document,
                ResourceGuidanceJson.read(ResourceGuidanceJson.write(document)));
        equal("every independent identity remains present", Kind.values().length + 17, document.resources().size());
    }

    private static void playerReadingContext() {
        Resource resource = item("fixture:resource");
        String sourceHash = "b".repeat(64);
        String uncertaintyCode = "canonical_uncertainty_code_" + sourceHash;
        ResourceMetadata metadata = new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.POTENTIAL_FUTURE_USE,
                null, 0, "fixture/source", List.of(), new Handling(Action.STORE, true, false, null, null),
                null, null, uncertaintyCode);
        var original = ResourceGuidanceProjection.project(fixture(metadata)).resources().get(resource);
        var entry = new ResourceGuidance.Entry(metadata, original.producers().stream().map(reference -> withSource(reference, sourceHash)).toList(),
                original.consumers().stream().map(reference -> withSource(reference, sourceHash)).toList(),
                Map.of("auditor_source_manifest", sourceHash));
        var loaded = new ResourceGuidanceRuntime.Loaded(ResourceGuidanceJson.prepare(Map.of(resource, entry)), null);
        List<Component> player = ResourceGuidanceClient.playerContext(loaded, resource);
        for (String key : List.of("source", "reference_source", "more_references", "more", "declaration",
                "candidate_reference", "unassessed_reference", "producer", "consumer")) {
            equal("mandatory reading omits technical/Shift context " + key, false, hasTranslation(player, PREFIX + key));
        }
        equal("source hashes are not embedded in player component arguments", false, player.toString().contains(sourceHash));
        equal("canonical uncertainty codes do not leak through missing player localization", false,
                player.toString().contains(uncertaintyCode));
        for (String key : List.of("player_scope", "supply", "side_stream", "player_tiers", "player_source",
                "handling", "discard_allowed", "recycling", "player_use", "player_future_unknown")) {
            equal("player context preserves useful source/use/handling/unknown data " + key, true, hasTranslation(player, PREFIX + key));
        }
        List<Component> explicit = ResourceGuidanceClient.lines(loaded, resource, true, true);
        equal("explicit detail still exposes provenance", true, hasTranslation(explicit, PREFIX + "reference_source"));
        equal("technical source identity is retained for explicit detail", true, explicit.toString().contains(sourceHash));
        var prototype = new ResourceGuidance.Entry(metadata, entry.producers(),
                entry.consumers().stream().filter(reference -> reference.unlockTier() == null).toList(), entry.sources());
        var unassessed = new ResourceGuidanceRuntime.Loaded(ResourceGuidanceJson.prepare(Map.of(resource, prototype)), null);
        equal("unknown consumer tier remains unknown in player reading", true, hasTranslation(
                ResourceGuidanceClient.playerContext(unassessed, resource), PREFIX + "player_unassessed_use"));
        equal("missing resource stays unknown in player reading", true, hasTranslation(
                ResourceGuidanceClient.playerContext(loaded, item("fixture:missing")), PREFIX + "player_unknown"));
        equal("unavailable reading metadata is visible without a technical repair instruction", true, hasTranslation(
                ResourceGuidanceClient.playerContext(new ResourceGuidanceRuntime.Loaded(null, "missing_asset"), resource),
                PREFIX + "player_unavailable"));
    }

    private static RouteReference withSource(RouteReference reference, String source) {
        return new RouteReference(reference.route(), source, reference.via(), reference.kind(), reference.certainty(),
                reference.unconditional(), reference.unlockTier(), reference.optional(), reference.discharged());
    }

    private static void references() {
        var result = fixture(metadata());
        Document document = ResourceGuidanceProjection.project(result);
        var entry = document.resources().get(item("fixture:resource"));
        equal("declared T0 consumer stays T0", true, entry.consumers().stream()
                .anyMatch(reference -> reference.route().equals("fixture/current") && Integer.valueOf(0).equals(reference.unlockTier())));
        equal("prototype with no policy stays unknown", true, entry.consumers().stream()
                .anyMatch(reference -> reference.route().equals("fixture/prototype") && reference.unlockTier() == null
                        && reference.optional() == null));
        equal("catalyst identity is not called a consumed input", true, entry.consumers().stream()
                .anyMatch(reference -> reference.kind() == ReferenceKind.CATALYST));
        equal("multi-member tag output remains only candidate provenance", true, entry.producers().stream()
                .anyMatch(reference -> !reference.unconditional() && reference.certainty() == Certainty.CONDITIONAL_TAG_MEMBER));
        var alteredFindings = new SustainabilityAudit.Result(result.authorities(), result.annotations(), Map.of(),
                result.debt(), result.debt().apply(List.of(new AuditFinding("fixture_error", "fixture", "",
                        AuditFinding.Severity.ERROR, "No gameplay acceptance.", List.of()))));
        equal("a global verdict does not enter the declaration fingerprint", document, ResourceGuidanceProjection.project(alteredFindings));
        Map<String, Route> routes = new TreeMap<>(result.authorities().routes());
        Route original = routes.get("fixture/source");
        routes.put(original.id(), new Route(original.id(), original.authority(), original.source(), original.family(),
                original.inputs(), original.catalysts(), Map.of(item("fixture:resource"), 999L), original.discharged(),
                original.durationTicks(), original.fuPerTick(), original.conditions(), original.prerequisites(),
                original.oneShot(), original.modOwned(), original.automatable()));
        Snapshot changed = new Snapshot(result.authorities().resources(), routes, result.authorities().terminals(),
                result.authorities().tags(), result.authorities().objectives(), result.authorities().inputHashes(), List.of());
        equal("recipe quantity changes are not copied into the guidance payload", document, ResourceGuidanceProjection.project(
                new SustainabilityAudit.Result(changed, result.annotations(), Map.of(), result.debt(), result.findings())));
        Map<Resource, ResourceGuidance.Entry> invented = new TreeMap<>(document.resources());
        invented.put(item("fixture:invented"), entry);
        expectFailure("canonical comparison rejects invented keys even with a valid new self-hash", () ->
                verifyProjection(result, ResourceGuidanceJson.prepare(invented)));
        invented.remove(item("fixture:invented"));
        ResourceMetadata altered = new ResourceMetadata(ResourceClass.BOOTSTRAP, null, 1, null, null, List.of(),
                null, null, null, null);
        invented.put(item("fixture:resource"), new ResourceGuidance.Entry(altered, entry.producers(), entry.consumers(), entry.sources()));
        expectFailure("a drifted declaration with a valid self-hash is rejected", () ->
                verifyProjection(result, ResourceGuidanceJson.prepare(invented)));
        invented.clear();
        expectFailure("canonical comparison rejects missing keys", () -> verifyProjection(result, ResourceGuidanceJson.prepare(invented)));
        ResourceMetadata privateSource = new ResourceMetadata(null, null, null, null, null, List.of(), null, null,
                null, "C:\\Users\\private\\source");
        expectFailure("private paths never enter a packaged projection", () -> ResourceGuidanceProjection.project(fixture(privateSource)));
    }

    private static void malformed() {
        String valid = ResourceGuidanceJson.write(ResourceGuidanceProjection.project(fixture(metadata())));
        equal("Windows checkout line endings do not invent content drift", ResourceGuidanceJson.read(valid),
                ResourceGuidanceJson.read(valid.replace("\n", "\r\n")));
        JsonObject original = JsonParser.parseString(valid).getAsJsonObject();
        for (String field : List.copyOf(original.keySet())) {
            mutate(original, object -> object.remove(field), "missing root " + field);
        }
        mutate(original, object -> object.addProperty("schema_version", 2), "unsupported schema");
        mutate(original, object -> object.addProperty("content_sha256", "0".repeat(64)), "fingerprint mismatch");
        mutate(original, object -> object.addProperty("sustainability_pass", true), "forbidden verdict");
        JsonObject row = original.getAsJsonArray("resources").get(0).getAsJsonObject();
        for (String field : List.copyOf(row.keySet())) {
            mutate(original, object -> first(object).remove(field), "missing declaration field " + field);
        }
        for (String field : List.of("count", "amount", "entry_bill", "player_state", "mandatory", "output_role")) {
            mutate(original, object -> first(object).addProperty(field, 33), "invented/non-declaration field " + field);
        }
        mutate(original, object -> first(object).addProperty("first_use_tier", -1), "negative tier");
        mutate(original, object -> first(object).addProperty("first_use_tier", 16), "out-of-range tier");
        mutate(original, object -> first(object).addProperty("first_use_tier", 0.5), "fractional tier");
        mutate(original, object -> first(object).addProperty("first_use_tier", "0"), "string tier coercion");
        mutate(original, object -> first(object).addProperty("resource_class", "new_renewable_default"), "invented class");
        mutate(original, object -> first(object).getAsJsonObject("handling").addProperty("discard_allowed", "true"), "string boolean");
        mutate(original, object -> first(object).getAsJsonObject("handling").remove("hazardous"), "lost hazard field");
        mutate(original, object -> first(object).getAsJsonObject("resource").addProperty("rating", 16), "invalid identity rating");
        mutate(original, object -> first(object).getAsJsonObject("resource").addProperty("id", ""), "empty identity");
        mutate(original, object -> first(object).getAsJsonObject("resource").addProperty("count", 1), "copied quantity in identity");
        mutate(original, object -> first(object).getAsJsonArray("consumers").get(0).getAsJsonObject()
                .addProperty("unlock_tier", 16), "invented consumer tier");
        mutate(original, object -> first(object).getAsJsonArray("consumers").get(0).getAsJsonObject()
                .addProperty("amount", 1), "copied consumer quantity");
        mutate(original, object -> object.getAsJsonArray("resources").add(first(object).deepCopy()), "duplicate resource");
        expectFailure("duplicate root fields are rejected rather than overwritten", () -> ResourceGuidanceJson.read(
                valid.replace("\"schema_version\": 1,", "\"schema_version\": 1,\n  \"schema_version\": 1,")));
        expectFailure("comments are not canonical generated metadata", () -> ResourceGuidanceJson.read("/*fixture*/" + valid));
        expectFailure("truncated data is rejected", () -> ResourceGuidanceJson.read(valid.substring(0, valid.length() / 2)));
    }

    private static void loader() {
        String valid = ResourceGuidanceJson.write(ResourceGuidanceProjection.project(fixture(metadata())));
        equal("packaged valid declarations load without repository/JDK inspection", true,
                ResourceGuidanceRuntime.read(bytes(valid)).available());
        equal("missing resource is unavailable", "missing_asset", ResourceGuidanceRuntime.read(bytes(null)).diagnostic());
        equal("invalid resource is unavailable", "invalid_asset", ResourceGuidanceRuntime.read(bytes("{}")).diagnostic());
        equal("empty declarations are not an empty success", "empty_declarations", ResourceGuidanceRuntime.read(
                bytes(ResourceGuidanceJson.write(ResourceGuidanceJson.prepare(Map.of())))).diagnostic());
        equal("oversized resource fails closed", "oversized_asset",
                ResourceGuidanceRuntime.read(bytes(" ".repeat(2 * 1024 * 1024 + 1))).diagnostic());
        ClassLoader failed = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                throw new IllegalStateException("Fixture resource failure");
            }
        };
        equal("resource access failure remains visibly unavailable", "invalid_asset", ResourceGuidanceRuntime.read(failed).diagnostic());
    }

    private static void lineEndings() {
        Document expected = ResourceGuidanceProjection.project(fixture(metadata()));
        String lf = ResourceGuidanceJson.write(expected);
        String crlf = lf.replace("\n", "\r\n");
        Document checkedLf = checkedAsset(expected, lf);
        Document checkedCrlf = checkedAsset(expected, crlf);
        var loadedCrlf = ResourceGuidanceRuntime.read(bytes(crlf));
        equal("a CRLF checkout is loadable through the actual packaged reader", true, loadedCrlf.available());
        equal("a CRLF checkout keeps the complete declaration DTO", expected, loadedCrlf.document());
        equal("LF/CRLF drift comparison retains the same declaration digest", checkedLf.contentSha256(), checkedCrlf.contentSha256());
        equal("CRLF package loading retains the same declaration digest", expected.contentSha256(),
                loadedCrlf.document().contentSha256());
        String lfByteHash = AuthorityAdapters.sha256(lf.getBytes(StandardCharsets.UTF_8));
        String crlfByteHash = AuthorityAdapters.sha256(crlf.getBytes(StandardCharsets.UTF_8));
        equal("file-byte hashes are distinct from checkout-independent declaration identity", false, lfByteHash.equals(crlfByteHash));
        String missingCrlf = ResourceGuidanceJson.write(ResourceGuidanceJson.prepare(Map.of())).replace("\n", "\r\n");
        expectFailure("CRLF tolerance does not waive missing declarations or a different valid self-hash",
                () -> checkedAsset(expected, missingCrlf));
        Map<Resource, ResourceGuidance.Entry> invented = new TreeMap<>(expected.resources());
        invented.put(item("fixture:invented"), invented.values().iterator().next());
        String inventedCrlf = ResourceGuidanceJson.write(ResourceGuidanceJson.prepare(invented)).replace("\n", "\r\n");
        expectFailure("CRLF tolerance does not admit invented declarations with a recomputed self-hash",
                () -> checkedAsset(expected, inventedCrlf));
    }

    private static void contexts() {
        equal("rated qualified Atlas identity stays rated", new Resource(Kind.ITEM, "grindless:relay_matrix", 2, "items"),
                ResourceGuidanceContext.resource("item:grindless:relay_matrix [F2]").orElseThrow());
        equal("fluid identity stays fluid", new Resource(Kind.FLUID, "minecraft:water"),
                ResourceGuidanceContext.resource("fluid:minecraft:water").orElseThrow());
        equal("localized prose is not parsed as an authority", true,
                ResourceGuidanceContext.resource("Two buckets of water").isEmpty());
        equal("non-item Matrix rating is not fabricated", true,
                ResourceGuidanceContext.resource("fluid:minecraft:water [F2]").isEmpty());
        equal("a minimum-control label selects its canonical declaration without inventing an ID",
                new Resource(Kind.ITEM, "grindless:relay_matrix", 1, "items"),
                ResourceGuidanceContext.resource("item:grindless:relay_matrix [F1+]").orElseThrow());
        for (String invalid : List.of("item:grindless:relay_matrix [F0+]",
                "item:grindless:relay_matrix [F16+]", "fluid:minecraft:water [F2+]",
                "tag:forge:ingots/iron [F1+]", "item:grindless:relay_matrix [F1++]",
                "item:grindless:relay_matrix [F1+] trailing")) {
            equal("strict minimum-control labels reject " + invalid, true,
                    ResourceGuidanceContext.resource(invalid).isEmpty());
        }
        List<AtlasLogic.Entry> entries = AtlasLogic.allEntries(ProcessGraph.generate(List.of()));
        var mill = entries.stream().filter(entry -> entry.id().equals("craft/wire_mill")).findFirst().orElseThrow();
        var control = ResourceGuidanceContext.operands(mill).stream()
                .filter(operand -> operand.resource().id().equals("grindless:relay_matrix")).findFirst().orElseThrow();
        equal("teaching does not silently drop the minimum input", 1, control.amount());
        equal("teaching preserves the input's typed minimum role",
                io.github.ezequiel24123z.grindless.recipe.IngredientSpec.MatrixRole.MINIMUM_CONTROL,
                control.matrixRole());
        equal("teaching preserves exact output identities", false, ResourceGuidanceContext.operands(mill).stream()
                .filter(operand -> operand.role() == ResourceGuidanceContext.Role.OUTPUT)
                .anyMatch(operand -> operand.matrixRole()
                        != io.github.ezequiel24123z.grindless.recipe.IngredientSpec.MatrixRole.EXACT));
        for (String page : List.of("bootstrap", "workshop", "settling", "relay", "renewables")) {
            equal("native guide data is present for " + page, false, ResourceGuidanceContext.guideRecipes(page, entries).isEmpty());
        }
        for (String page : List.of("power", "extraction", "factory", "industrial", "factory_world")) {
            equal("parked topic is not broadened: " + page, List.of(), ResourceGuidanceContext.guideRecipes(page, entries));
        }
        for (var recipe : RenewableProcesses.recipes()) {
            var operands = ResourceGuidanceContext.operands(AtlasLogic.entry(recipe));
            for (var input : recipe.fluidInputs()) {
                equal("fluid amounts still come from native recipe " + recipe.id(), true,
                        operands.stream().anyMatch(operand -> operand.role() == ResourceGuidanceContext.Role.INPUT
                                && operand.resource().kind() == Kind.FLUID && operand.resource().id().equals(input.id())
                                && operand.amount() == input.count()));
            }
            for (var output : recipe.itemOutputs()) {
                equal("every output retains its native count, never inferred role " + recipe.id(), true,
                        operands.stream().anyMatch(operand -> operand.role() == ResourceGuidanceContext.Role.OUTPUT
                                && operand.resource().id().equals(output.id()) && operand.amount() == output.count()));
            }
        }
        var catalyst = new AtlasLogic.Entry("fixture", "fixture", MachineFamily.QUERN,
                List.of("item:fixture:in"), List.of("item:fixture:out"),
                List.of("item:fixture:tool"), Double.NaN, null, null, 1, 0);
        equal("recipe-list catalyst role is retained", ResourceGuidanceContext.Role.CATALYST,
                ResourceGuidanceContext.operands(catalyst).get(1).role());
        var starter = QuestCatalogue.byId(QuestCatalogue.STARTER_SUPPLIES);
        equal("the starter is FREE evidence", QuestCatalogue.Evidence.FREE, starter.evidence());
        equal("FREE evidence has no item or resource subject", true,
                ResourceGuidanceContext.questSubject(starter).isEmpty());
        var relay = QuestCatalogue.byId("relay");
        equal("the final relay quest is strict physical Matrix evidence", QuestCatalogue.Evidence.MATRIX,
                relay.evidence());
        equal("the final relay requires exact F1", io.github.ezequiel24123z.grindless.energy.FluxTier.F1,
                relay.matrixRating());
        equal("physical F1 quest context keeps that exact rating", 1,
                ResourceGuidanceContext.questSubject(relay).orElseThrow().rating());
        var stock = QuestCatalogue.byId("matrix_line");
        equal("ordinary factory-stock display uses the physical default supply option", 1,
                ResourceGuidanceContext.questSubject(stock).orElseThrow().rating());
        equal("the display option does not replace generic stock evidence with exact Matrix evidence",
                QuestCatalogue.Evidence.ITEM, stock.evidence());
        equal("generic stock evidence still specifies no required Matrix rating", null, stock.matrixRating());
        var industrial = QuestCatalogue.byId("industrial_matrix");
        equal("industrial evidence really requires a physical Matrix", QuestCatalogue.Evidence.MATRIX, industrial.evidence());
        equal("industrial Matrix evidence keeps exact F2", 2,
                ResourceGuidanceContext.questSubject(industrial).orElseThrow().rating());
    }

    private static void authoredMatrixContexts() {
        Resource f1 = new Resource(Kind.ITEM, FabricationLogic.RELAY_MATRIX, FluxTier.F1.ordinal(), "items");
        var entry = new ResourceGuidance.Entry(new ResourceMetadata(ResourceClass.BOOTSTRAP, null, 0, null,
                "craft/relay_matrix", List.of(), null, null, null, null), List.of(), List.of(), Map.of());
        var loaded = new ResourceGuidanceRuntime.Loaded(ResourceGuidanceJson.prepare(Map.of(f1, entry)), null);
        String original = ResourceGuidanceJson.write(loaded.document());
        Function<String, Optional<MatrixArchitecture>> architectures = id -> id.equals(FabricationLogic.RELAY_MATRIX)
                ? Optional.of(MatrixArchitecture.RELAY) : Optional.empty();
        List<AtlasLogic.Entry> entries = AtlasLogic.allEntries(ProcessGraph.generate(List.of()));
        for (var selection : Map.of(
                "craft/relay_matrix", ResourceGuidanceContext.Role.OUTPUT,
                "assemble/relay_matrix", ResourceGuidanceContext.Role.OUTPUT,
                "craft/thermal_generator", ResourceGuidanceContext.Role.INPUT,
                "craft/thermal_generator_from_core", ResourceGuidanceContext.Role.INPUT,
                "craft/wire_mill", ResourceGuidanceContext.Role.INPUT).entrySet()) {
            var authored = entries.stream().filter(row -> row.id().equals(selection.getKey())).findFirst().orElseThrow();
            var matrices = ResourceGuidanceContext.operands(authored).stream()
                    .filter(operand -> operand.resource().id().equals(FabricationLogic.RELAY_MATRIX)).toList();
            equal("real Atlas row retains one logical Matrix operand: " + authored.id(), 1, matrices.size());
            var operand = matrices.get(0);
            equal("authored Atlas identity agrees with physical F1-only metadata: " + authored.id(), f1, operand.resource());
            equal("authored Atlas retains the actual input/output role: " + authored.id(), selection.getValue(), operand.role());
            equal("authored Atlas retains the complete native quantity: " + authored.id(),
                    operand.role() == ResourceGuidanceContext.Role.OUTPUT ? FabricationLogic.RELAY_MATRIX_BATCH : 1,
                    operand.amount());
            equal("the physical declaration is found without a generic raw DTO key: " + authored.id(), f1,
                    ResourceGuidanceClient.declaration(loaded, operand.resource(), architectures).orElseThrow().declaration());
            equal("actual Atlas context cannot become unknown after physical metadata projection: " + authored.id(), false,
                    hasTranslation(ResourceGuidanceClient.lines(loaded, operand.resource(), true, true), PREFIX + "unknown"));
            equal("actual Atlas context retains declared supply rather than an empty success: " + authored.id(), true,
                    hasTranslation(ResourceGuidanceClient.lines(loaded, operand.resource(), true, true), PREFIX + "supply"));
        }
        for (String task : List.of("relay", "matrix_line")) {
            Resource subject = ResourceGuidanceContext.questSubject(QuestCatalogue.byId(task)).orElseThrow();
            equal("actual quest context uses the physical F1 declaration: " + task, f1, subject);
            equal("actual quest reading does not regress to unknown with an F1-only DTO: " + task, false,
                    hasTranslation(ResourceGuidanceClient.playerContext(loaded, subject), PREFIX + "player_unknown"));
            equal("actual quest reading retains declared supply: " + task, true,
                    hasTranslation(ResourceGuidanceClient.playerContext(loaded, subject), PREFIX + "supply"));
        }
        var f2Route = entries.stream().filter(row -> row.id().equals("assemble/relay_matrix_f2")).findFirst().orElseThrow();
        var predecessor = ResourceGuidanceContext.operands(f2Route).stream()
                .filter(operand -> operand.role() == ResourceGuidanceContext.Role.INPUT
                        && operand.resource().id().equals(FabricationLogic.RELAY_MATRIX)).findFirst().orElseThrow();
        equal("the exact F2 predecessor stays physical F1", f1, predecessor.resource());
        equal("the exact F2 predecessor never becomes minimum control", IngredientSpec.MatrixRole.EXACT, predecessor.matrixRole());
        for (FluxTier tier : List.of(FluxTier.F2, FluxTier.F3)) {
            Resource rated = new Resource(Kind.ITEM, f1.id(), tier.ordinal(), "items");
            equal("explicit higher descriptors keep their exact identity: " + tier, rated,
                    ResourceGuidanceContext.resource("item:" + f1.id() + " [" + tier.name() + "]").orElseThrow());
            equal("F1-only metadata cannot leak into a higher physical declaration: " + tier, true,
                    ResourceGuidanceClient.declaration(loaded, rated, architectures).isEmpty());
        }
        Resource f2 = new Resource(Kind.ITEM, f1.id(), FluxTier.F2.ordinal(), "items");
        equal("industrial quest display stays physically F2", f2,
                ResourceGuidanceContext.questSubject(QuestCatalogue.byId("industrial_matrix")).orElseThrow());
        equal("the actual F2 Atlas output stays physically F2", f2, ResourceGuidanceContext.operands(f2Route).stream()
                .filter(operand -> operand.role() == ResourceGuidanceContext.Role.OUTPUT).findFirst().orElseThrow().resource());
        for (Resource unrelated : List.of(new Resource(Kind.ITEM, "minecraft:paper"),
                new Resource(Kind.ITEM, "grindless:unknown_matrix"), new Resource(Kind.ITEM, "grindless:integrated_matrix"),
                new Resource(Kind.ITEM, "other_mod:relay_matrix"), new Resource(Kind.TAG, f1.id()),
                new Resource(Kind.FLUID, f1.id()), new Resource(Kind.ITEM, f1.id(), 0, "blocks"),
                new Resource(Kind.ITEM, f1.id(), 0, "fluids"))) {
            equal("other IDs/axes/registries cannot inherit the canonical Relay default: " + unrelated.key(), true,
                    unrelated == ResourceGuidanceContext.canonicalSupply(unrelated));
            equal("other IDs/axes/registries cannot borrow F1-only metadata: " + unrelated.key(), true,
                    ResourceGuidanceClient.declaration(loaded, unrelated, architectures).isEmpty());
        }
        for (String descriptor : List.of("item:minecraft:paper", "item:grindless:unknown_matrix",
                "item:grindless:integrated_matrix", "tag:" + f1.id(), "fluid:" + f1.id())) {
            equal("non-Matrix and wrong-axis descriptors remain unrated: " + descriptor, 0,
                    ResourceGuidanceContext.resource(descriptor).orElseThrow().rating());
        }
        equal("ordinary non-Matrix quest display remains unrated", 0,
                ResourceGuidanceContext.questSubject(QuestCatalogue.byId("data_core")).orElseThrow().rating());
        equal("generic authored ingredients retain unspecified admission rating0", 0,
                IngredientSpec.item(f1.id(), 1).matrixRating());
        var f3 = new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F3);
        equal("explicit minimum-control admission still accepts a stronger valid Matrix", true,
                IngredientSpec.minimumControl(f1.id(), FluxTier.F1, 1).acceptsMatrix(f1.id(), f3));
        equal("exact F1 admission still refuses a stronger physical Matrix", false,
                IngredientSpec.matrix(f1.id(), FluxTier.F1, 1).acceptsMatrix(f1.id(), f3));
        equal("authored display never manufactures a generic DTO declaration", Set.of(f1), loaded.document().resources().keySet());
        equal("authored display lookup does not mutate the DTO or its digest", original,
                ResourceGuidanceJson.write(loaded.document()));
    }

    private static void matrixDeclarations() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        String relay = "fixture:relay_matrix";
        String otherRelay = "fixture:other_relay_matrix";
        String integrated = "fixture:integrated_matrix";
        Map<String, MatrixArchitecture> items = Map.of(relay, MatrixArchitecture.RELAY,
                otherRelay, MatrixArchitecture.RELAY, integrated, MatrixArchitecture.INTEGRATED);
        Function<String, Optional<MatrixArchitecture>> architectures = id -> Optional.ofNullable(items.get(id));
        Resource ordinary = item(relay);
        ResourceGuidance.Entry ordinaryEntry = new ResourceGuidance.Entry(new ResourceMetadata(ResourceClass.BOOTSTRAP,
                null, 0, null, "fixture/relay_default", List.of(), null, null, null, null),
                List.of(), List.of(), Map.of("fixture", "Not gameplay declarations"));
        Document document = ResourceGuidanceJson.prepare(Map.of(ordinary, ordinaryEntry));
        var loaded = new ResourceGuidanceRuntime.Loaded(document, null);
        String original = ResourceGuidanceJson.write(document);
        matrixLookup("legacy Relay without NBT", relay, MatrixArchitecture.RELAY, null, 1, ordinary, loaded, architectures);
        CompoundTag unrelated = new CompoundTag();
        unrelated.putString("Unrelated", "keep");
        matrixLookup("Relay with unrelated legacy NBT", relay, MatrixArchitecture.RELAY, unrelated, 1, ordinary, loaded, architectures);
        matrixLookup("explicit Relay F1", relay, MatrixArchitecture.RELAY, rating(1), 1, ordinary, loaded, architectures);
        matrixLookup("Relay F2 has no ordinary T0 fallback", relay, MatrixArchitecture.RELAY, rating(2), 2, null, loaded, architectures);
        matrixLookup("Relay F3 has no ordinary T0 fallback", relay, MatrixArchitecture.RELAY, rating(3), 3, null, loaded, architectures);
        matrixLookup("a different Relay item cannot borrow this declaration", otherRelay, MatrixArchitecture.RELAY,
                null, 1, null, loaded, architectures);
        matrixLookup("another architecture cannot borrow Relay's declaration", integrated, MatrixArchitecture.INTEGRATED,
                null, 4, null, loaded, architectures);

        List<Consumer<CompoundTag>> malformed = List.of(tag -> tag.putString(ControlMatrixNbt.RATING, "1"),
                tag -> tag.put(ControlMatrixNbt.RATING, new CompoundTag()),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, 1.5),
                tag -> tag.putFloat(ControlMatrixNbt.RATING, 1.25f),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 0),
                tag -> tag.putInt(ControlMatrixNbt.RATING, -1),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 4),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 16),
                tag -> tag.putLong(ControlMatrixNbt.RATING, 4_294_967_297L),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.NaN),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.POSITIVE_INFINITY));
        for (int index = 0; index < malformed.size(); index++) {
            CompoundTag invalid = new CompoundTag();
            invalid.putString("Unrelated", "keep");
            malformed.get(index).accept(invalid);
            matrixLookup("invalid explicit Matrix never inherits default, case" + index, relay, MatrixArchitecture.RELAY,
                    invalid, null, null, loaded, architectures);
        }
        Resource ratedF1 = new Resource(Kind.ITEM, relay, 1, "items");
        ResourceGuidance.Entry exactF1 = new ResourceGuidance.Entry(new ResourceMetadata(ResourceClass.CATALYTIC,
                null, null, null, "fixture/exact_f1", List.of(), null, null, null, null),
                List.of(), List.of(), Map.of());
        Map<Resource, ResourceGuidance.Entry> rated = new TreeMap<>(document.resources());
        rated.put(ratedF1, exactF1);
        Resource ratedF2 = new Resource(Kind.ITEM, relay, 2, "items");
        ResourceGuidance.Entry exactF2 = new ResourceGuidance.Entry(new ResourceMetadata(ResourceClass.FINITE_OPTIONAL,
                null, null, null, "fixture/exact_f2", List.of(), null, null, null, null),
                List.of(), List.of(), Map.of());
        rated.put(ratedF2, exactF2);
        var explicit = new ResourceGuidanceRuntime.Loaded(ResourceGuidanceJson.prepare(rated), null);
        matrixLookup("an explicit F1 declaration wins for legacy default", relay, MatrixArchitecture.RELAY,
                null, 1, ratedF1, explicit, architectures);
        matrixLookup("an explicit F1 declaration wins for tagged F1", relay, MatrixArchitecture.RELAY,
                rating(1), 1, ratedF1, explicit, architectures);
        matrixLookup("an explicit F2 declaration is used exactly", relay, MatrixArchitecture.RELAY,
                rating(2), 2, ratedF2, explicit, architectures);
        matrixLookup("explicit F1/F2 declarations do not extend to F3", relay, MatrixArchitecture.RELAY,
                rating(3), 3, null, explicit, architectures);
        equal("explicit rated metadata wins over the ordinary object", true,
                ResourceGuidanceClient.declaration(explicit, ratedF1, architectures).orElseThrow().entry() == exactF1);
        for (Consumer<CompoundTag> mutation : malformed) {
            CompoundTag invalid = new CompoundTag();
            mutation.accept(invalid);
            matrixLookup("even explicit declarations cannot make invalid stack data default", relay, MatrixArchitecture.RELAY,
                    invalid, null, null, explicit, architectures);
        }

        Resource integratedOrdinary = item(integrated);
        ResourceGuidance.Entry integratedEntry = new ResourceGuidance.Entry(new ResourceMetadata(null, null, null, null,
                "fixture/integrated_default", List.of(), null, null, null, null), List.of(), List.of(), Map.of());
        var separate = new ResourceGuidanceRuntime.Loaded(
                ResourceGuidanceJson.prepare(Map.of(ordinary, ordinaryEntry, integratedOrdinary, integratedEntry)), null);
        matrixLookup("Integrated's own ordinary declaration applies only at its real F4 default", integrated,
                MatrixArchitecture.INTEGRATED, null, 4, integratedOrdinary, separate, architectures);
        matrixLookup("Integrated retrospective F1 is valid but not its default", integrated, MatrixArchitecture.INTEGRATED,
                rating(1), 1, null, separate, architectures);
        equal("an ordinary declaration cannot cross item identity", true,
                ResourceGuidanceClient.declaration(separate, new Resource(Kind.ITEM, integrated, 4, "items"), architectures)
                        .orElseThrow().entry() == integratedEntry);
        equal("a block-registry resource cannot inherit item metadata", true, ResourceGuidanceClient.declaration(
                loaded, new Resource(Kind.ITEM, relay, 1, "blocks"), architectures).isEmpty());
        equal("a missing Matrix item cannot obtain a default", true, ResourceGuidanceClient.declaration(
                loaded, new Resource(Kind.ITEM, "fixture:missing", 1, "items"), architectures).isEmpty());
        ItemStack notMatrix = new ItemStack(Items.PAPER, 2);
        notMatrix.setTag(rating(1));
        CompoundTag paperBefore = notMatrix.getTag().copy();
        Resource paper = ResourceGuidanceClient.item(notMatrix).orElseThrow();
        equal("a non-Matrix item with Rating NBT remains an ordinary paper identity", new Resource(Kind.ITEM, "minecraft:paper"), paper);
        equal("a non-Matrix item cannot inherit Relay data", true, ResourceGuidanceClient.declaration(loaded, paper, architectures).isEmpty());
        equal("non-Matrix NBT/count stay unchanged", paperBefore, notMatrix.getTag());
        equal("non-Matrix count stays unchanged", 2, notMatrix.getCount());
        equal("lookup does not invent rated DTO keys", Set.of(ordinary), document.resources().keySet());
        equal("the entire original DTO/digest remains unchanged", original, ResourceGuidanceJson.write(document));
    }

    private static CompoundTag rating(int rating) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ControlMatrixNbt.RATING, rating);
        return tag;
    }

    /**
     * Uses the real NBT decoder and the same validated-spec identity conversion as ControlMatrixItem.
     * A vanilla carrier avoids registering fixture gameplay items or starting Forge.
     */
    private static void matrixLookup(String label, String id, MatrixArchitecture architecture, CompoundTag tag,
                                     Integer expectedPhysical, Resource expectedDeclaration, ResourceGuidanceRuntime.Loaded loaded,
                                     Function<String, Optional<MatrixArchitecture>> architectures) {
        ItemStack carrier = new ItemStack(Items.PAPER, 3);
        if (tag != null) {
            carrier.setTag(tag.copy());
        }
        CompoundTag originalTag = carrier.getTag();
        CompoundTag original = originalTag == null ? null : originalTag.copy();
        Component labelBefore = carrier.getHoverName();
        String dtoBefore = ResourceGuidanceJson.write(loaded.document());
        var spec = ControlMatrixNbt.load(architecture, carrier.getTag());
        Optional<Resource> physical = ResourceGuidanceClient.matrixItem(id, spec);
        Optional<Resource> authority = spec.map(value -> AuthorityAdapters.physical(
                new Resource(Kind.ITEM, id, value.rating().ordinal(), "items")));
        equal(label + " authority projection preserves valid ratings and never defaults invalid explicit NBT",
                physical, authority);
        equal(label + " preserves validated physical rating", expectedPhysical, physical.map(Resource::rating).orElse(null));
        var declaration = physical.flatMap(resource -> ResourceGuidanceClient.declaration(loaded, resource, architectures));
        equal(label + " selects only the correct declaration", expectedDeclaration,
                declaration.map(ResourceGuidanceClient.DeclarationMatch::declaration).orElse(null));
        equal(label + " cannot rewrite the physical identity", expectedPhysical, physical.map(Resource::rating).orElse(null));
        equal(label + " cannot rewrite the validated spec", spec, ControlMatrixNbt.load(architecture, carrier.getTag()));
        equal(label + " leaves NBT bytes/structure intact", original, carrier.getTag());
        equal(label + " does not create or replace the tag object", true, originalTag == carrier.getTag());
        equal(label + " leaves the stack label intact", labelBefore, carrier.getHoverName());
        equal(label + " leaves the stack count intact", 3, carrier.getCount());
        equal(label + " leaves the carrier item intact", Items.PAPER, carrier.getItem());
        equal(label + " does not mutate or extend the DTO/digest", dtoBefore, ResourceGuidanceJson.write(loaded.document()));
    }

    private static void surfaces(Path root) throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(root.resolve(LANGUAGE))).getAsJsonObject();
        for (Enum<?> value : ResourceClass.values()) translation(lang, "class", value);
        for (Enum<?> value : SideStreamClass.values()) translation(lang, "side", value);
        for (Enum<?> value : Action.values()) translation(lang, "action", value);
        for (String key : List.of("unavailable", "unknown", "not_declared", "declaration", "uncertainty_unavailable",
                "consumer", "producer", "unassessed_reference", "candidate_reference", "handling_unknown")) {
            equal("missing/unknown/handling/reference text is localized: " + key, true, lang.has(PREFIX + key));
        }
        String runtime = Files.readString(root.resolve(COMMON.resolve(Path.of("guidance", "ResourceGuidanceRuntime.java"))));
        String presenter = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "ResourceGuidanceClient.java"))));
        String context = Files.readString(root.resolve(COMMON.resolve(Path.of("guidance", "ResourceGuidanceContext.java"))));
        for (String forbidden : List.of("AuthorityAdapters", "ToolProvider", "java.nio.file", "T0Sustainability",
                "SustainabilityAudit", "ConstructionReserve", "EntryProfile")) {
            equal("runtime does not invoke source/model audit " + forbidden, false,
                    runtime.contains(forbidden) || presenter.contains(forbidden) || context.contains(forbidden));
        }
        equal("runtime only reads prepared classpath bytes", true, runtime.contains("getResourceAsStream(ResourceGuidance.ASSET)"));
        String init = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "GrindlessClient.java"))));
        equal("loading happens at client setup, not hover", true, init.contains("ResourceGuidanceRuntime.initialize("));
        equal("physical Matrix identity still comes from the actual validated stack spec", true,
                presenter.contains("matrixItem(id, matrix.spec(stack))"));
        equal("default declaration lookup reuses the real NBT default and actual item architecture", true,
                presenter.contains("ControlMatrixNbt.load(architecture, null)")
                        && presenter.contains("BuiltInRegistries.ITEM.get(location) instanceof ControlMatrixItem matrix"));
        equal("the shared presenter uses declaration lookup without replacing the physical input", true,
                presenter.contains("Optional<DeclarationMatch> match = declaration(loaded, resource)"));
        String hover = Files.readString(root.resolve(FORGE.resolve("ResourceGuidanceTooltips.java")));
        equal("annotated vanilla and mod items share native hover", true, hover.contains("ItemTooltipEvent event")
                && hover.contains("ResourceGuidanceClient.append(") && !hover.contains("startsWith(\"grindless"));
        equal("fluid containers forward actual identity and amount without an invented fluid", true,
                hover.contains("FluidUtil.getFluidContained(event.getItemStack())") && hover.contains("actual.getAmount()")
                        && hover.contains("ForgeRegistries.FLUIDS.getKey(actual.getFluid())"));
        String machine = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "ProcessMachineScreen.java"))));
        equal("machine hover retains unknown identity and shared guidance", true, machine.contains("ResourceGuidanceClient.append(")
                && machine.contains(".orElse(null)") && machine.contains("gui.grindless.fluid_unknown_details"));
        String atlas = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "ProcessAtlasScreen.java"))));
        equal("Atlas context uses current recipe operands and shared guidance", true,
                atlas.contains("ResourceGuidanceContext.operands(rows.get(selected))") && atlas.contains("ResourceGuidanceClient.append("));
        equal("Atlas context retains the minimum role instead of naming only exact F1", true,
                atlas.contains("operand.matrixRole() == MatrixRole.MINIMUM_CONTROL")
                        && atlas.contains("\"recipe.grindless.minimum_control\""));
        String guide = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "FieldGuideScreen.java"))));
        String teaching = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "QuestTeachingClient.java"))));
        String legacy = Files.readString(root.resolve(COMMON.resolve(Path.of("client", "QuestBookScreen.java"))));
        String heracles = Files.readString(root.resolve(FORGE.resolve(Path.of("quest", "HeraclesQuestClient.java"))));
        String ftb = Files.readString(root.resolve(FORGE.resolve(Path.of("quest", "FtbQuestDefinitions.java"))));
        equal("quest help uses live Atlas quantities and lossless native pagination", true,
                teaching.contains("AtlasLogic.line(entry)") && legacy.contains("QuestTeaching.paginate(")
                        && teaching.contains("ResourceGuidanceClient.playerContext("));
        equal("optional legacy renderers consume the same lesson projection", true,
                legacy.contains("QuestTeachingClient.content(") && heracles.contains("QuestTeachingClient.content("));
        equal("the official FTB data uses concise translated notes and native item tasks", true,
                ftb.contains("QuestTeaching.bodyKey(task)") && ftb.contains("\"ftbquests:item\"")
                        && ftb.contains("\"ftbquests:checkmark\""));
        equal("FTB does not embed the retired custom guide, recipe panels or widgets", false,
                ftb.contains("QuestTeachingClient") || ftb.contains("AtlasLogic.line(")
                        || ftb.contains("ResourceGuidanceClient") || ftb.contains("DisplayWidget"));
        equal("quest teaching keeps real output roles and the shared declaration formatter", true,
                teaching.contains("ResourceGuidanceContext.operands(entry)")
                        && teaching.contains("ResourceGuidanceContext.Role.OUTPUT")
                        && teaching.contains("ResourceGuidanceClient.playerContext(output)"));
        equal("port teaching projects the actual shared role/position/count authority", true,
                teaching.contains("WorkshopPortContract.slots(structure)")
                        && teaching.contains("WorkshopPortContract.maximumPorts(structure)")
                        && teaching.contains("WorkshopPortContract.minimumBricks(structure)")
                        && teaching.contains("WorkshopPortContract.maximum(structure, role)"));
        equal("tank teaching reads capacity from the existing fluid authority", true,
                teaching.contains("FluidLogic.TANK_CAPACITY"));
        equal("teaching never opts mandatory reading into technical detail", false,
                teaching.contains("ResourceGuidanceClient.lines("));
        equal("the compatibility Guide contains no parallel route manual", true,
                guide.contains("gui.grindless.quest.guide_migration")
                        && !guide.contains("GuideCatalogue.bodyKey") && !guide.contains("ProcessLookup"));
        equal("both book displays share canonical resource context", true,
                legacy.contains("ResourceGuidanceContext.questSubject(task)") && legacy.contains("ResourceGuidanceClient.append(")
                        && heracles.contains("ResourceGuidanceContext.questSubject(task)") && heracles.contains("ResourceGuidanceClient.playerContext("));
        equal("Heracles description does not request technical detail", false, heracles.contains("ResourceGuidanceClient.lines("));
        String jei = Files.readString(root.resolve(FORGE.resolve(Path.of("jei", "GrindlessProcessCategory.java"))));
        equal("JEI forwards the actual displayed Forge fluid", true,
                jei.contains("view.getDisplayedIngredient(ForgeTypes.FLUID_STACK)") && jei.contains("actual.getFluid()")
                        && jei.contains("ResourceGuidanceClient.lines("));
        equal("JEI preserves red nonconsumed catalysts", true,
                jei.contains("Component.translatable(\"jei.grindless.not_consumed\").withStyle(ChatFormatting.RED)"));
        equal("JEI does not add contradictory unknown text to a default Matrix declaration", true,
                jei.contains("ResourceGuidanceClient.declaration(loaded, resource.orElse(null)).isEmpty()"));
        equal("JEI does not invent registered fluids", false, jei.contains("DeferredRegister") || jei.contains("new ForgeFlowingFluid"));
        String bootstrap = lang.get("gui.grindless.quest.lesson.starter_supplies").getAsString();
        String renewables = List.of("geological_collector", "biocultivator", "biomass_dynamo", "water", "biomass").stream()
                .map(id -> lang.get("gui.grindless.quest.lesson." + id).getAsString())
                .collect(java.util.stream.Collectors.joining("\n"));
        QuestCatalogue.Task starter = QuestCatalogue.byId(QuestCatalogue.STARTER_SUPPLIES);
        equal("the teaching starter is the first canonical quest", starter.id(), QuestCatalogue.tasks().get(0).id());
        equal("the starter has an explicit free goal", QuestCatalogue.Evidence.FREE, starter.evidence());
        equal("the starter has no hidden quest prerequisite", List.of(), starter.requires());
        equal("the first quest is ready without physical evidence", QuestLogic.Status.CLAIMABLE,
                QuestLogic.consider(starter, Set.of(), false));
        equal("a recorded starter claim cannot become ready again", QuestLogic.Status.CLAIMED,
                QuestLogic.consider(starter, Set.of(starter.id()), false));
        Set<String> basicSupplies = Set.of("minecraft:cobblestone", "minecraft:oak_log", "minecraft:bread",
                "minecraft:brick", "minecraft:clay_ball", "minecraft:sand", "minecraft:charcoal",
                "minecraft:iron_ingot", "minecraft:water_bucket");
        equal("the advertised starter kit has actual rewards", false, starter.rewards().isEmpty());
        equal("starter rewards remain basic supplies, not machines, cores, insulators or Matrices", true,
                starter.rewards().stream().allMatch(reward -> basicSupplies.contains(reward.itemId())));
        equal("the starter note offers free native framework rewards", true,
                bootstrap.contains("free rewards in FTB Quests"));
        equal("the starter note describes eleven native rewards without promising legacy atomic delivery", true,
                bootstrap.contains("all eleven stacks")
                        && starter.rewards().size() == 11
                        && bootstrap.contains("rewards are claimed separately")
                        && bootstrap.contains("may drop on the ground")
                        && !bootstrap.contains("nothing is claimed or dropped")
                        && lang.get("gui.grindless.quest.starter_policy").getAsString()
                        .contains("FTB Quests"));
        equal("the finite kit leads to a lasting workshop supply", true,
                lang.get("gui.grindless.quest.lesson.superflat").getAsString().contains("lasting supply")
                        && lang.has("gui.grindless.quest.lesson.manual_work")
                        && lang.has("gui.grindless.quest.lesson.firing"));
        equal("teaching does not prescribe an initial glass/copper/redstone kit", false, bootstrap.startsWith("Bring"));
        equal("quests lead with mod-owned recurring sources", true, renewables.contains("Geological Collector")
                && renewables.contains("Biocultivator") && renewables.contains("Biomass Dynamo")
                && renewables.contains("Hand Pump")
                && lang.get("gui.grindless.quest.lesson.superflat").getAsString()
                .contains("lasting supply through your workshop"));
        equal("natural instructions retain daylight and restart cautions", true,
                renewables.contains("It needs daylight")
                        && renewables.contains("check it after a restart")
                        && renewables.contains("reserve for the night"));
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            String title = lang.get("gui.grindless.quest.task." + task.id()).getAsString();
            String body = lang.get(QuestTeaching.bodyKey(task)).getAsString();
            equal("native quest titles remain short: " + task.id(), true,
                    !title.isBlank() && title.length() <= 40);
            equal("native quest notes remain short single paragraphs: " + task.id(), true,
                    !body.isBlank() && !body.contains("\n") && body.split("\\s+").length <= 45);
        }
        for (String page : List.of("starter_supplies", "clay_mixer", "settling", "relay", "biomass")) {
            String body = lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId(page))).getAsString();
            equal("T0 prose does not duplicate recipe numbers or witness bill: " + page, false,
                    body.matches("(?s).*\\b(?:250|750|1000|33|41|21|20)\\b.*"));
            for (String jargon : List.of("a model's sufficient stock", "Tier-accepted", "Declarations do not certify")) {
                equal("T0 instructions use player language instead of " + jargon, false, body.contains(jargon));
            }
        }
        equal("manual five-Matrix/calibrated exit is explicit without a Bench shortcut", true,
                lang.get("gui.grindless.quest.lesson.relay").getAsString().contains("Craft five Relay Matrices by hand")
                        && lang.get("gui.grindless.quest.lesson.relay").getAsString().contains("Calibrated Data Core")
                        && lang.get("gui.grindless.quest.lesson.relay").getAsString().contains("five valid F1 Matrices")
                        && lang.get("gui.grindless.quest.lesson.relay").getAsString().contains("complete this milestone")
                        && lang.get("gui.grindless.quest.lesson.relay").getAsString().contains("does not consume them")
                        && lang.get("gui.grindless.quest.lesson.relay").getAsString()
                        .contains("Assembly Bench cannot make this batch"));
        String exporter = Files.readString(root.resolve(Path.of("tools", "dashboard", "DashboardExport.java")));
        for (String source : List.of("VerifySustainability", "VerifySustainabilityAdapters",
                "VerifySustainabilityRegistry", "VerifySustainabilityT0", "VerifyResourceGuidance")) {
            equal("every compiled check helper has explicit source provenance: " + source, true,
                    exporter.contains("\"tools/checks/" + source + ".java\""));
        }
        equal("fresh tool class directory hashes cover new helpers, not a frozen class-name list", true,
                exporter.contains("Files.walk(directory)") && exporter.contains("directory.startsWith(root.resolve(\"build\"))"));
        equal("guidance proof keeps content, authority, source and executed class identities separate", true,
                exporter.contains("\"authority_sha256\", AuditReports.authorityHash(result.authorities())")
                        && exporter.contains("\"guidance_source_hashes\", guidanceSources(root)")
                        && exporter.contains("\"guidance_class_hashes\", guidanceClasses()"));
    }

    private static void translation(JsonObject lang, String domain, Enum<?> value) {
        equal("vocabulary is localized: " + value, true, lang.has(PREFIX + domain + "." + value.name().toLowerCase(Locale.ROOT)));
    }

    private static boolean hasTranslation(List<Component> lines, String key) {
        return lines.stream().anyMatch(line -> hasTranslation(line, key));
    }

    private static boolean hasTranslation(Component line, String key) {
        if (line.getContents() instanceof TranslatableContents translation) {
            if (translation.getKey().equals(key)) return true;
            for (Object argument : translation.getArgs()) {
                if (argument instanceof Component component && hasTranslation(component, key)) return true;
            }
        }
        return line.getSiblings().stream().anyMatch(sibling -> hasTranslation(sibling, key));
    }

    private static void mutate(JsonObject original, Consumer<JsonObject> change, String label) {
        JsonObject mutated = original.deepCopy();
        change.accept(mutated);
        String bytes = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create()
                .toJson(mutated) + "\n";
        expectFailure(label, () -> ResourceGuidanceJson.read(bytes));
    }

    private static JsonObject first(JsonObject root) {
        return root.getAsJsonArray("resources").get(0).getAsJsonObject();
    }

    private static ClassLoader bytes(String content) {
        return new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                equal("loader requests only the committed prepared asset", ResourceGuidance.ASSET, name);
                return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static ResourceMetadata metadata() {
        return new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.POTENTIAL_FUTURE_USE,
                null, 0, "fixture/source", List.of(), new Handling(Action.DISCARD, true, false, null, null),
                null, null, null);
    }

    private static SustainabilityAudit.Result fixture(ResourceMetadata metadata) {
        Resource resource = item("fixture:resource");
        Resource other = item("fixture:other");
        Resource tag = new Resource(Kind.TAG, "fixture:members");
        Builder builder = new Builder().resource(resource).resource(other);
        builder.route(route("fixture/source", Map.of(), Map.of(), Map.of(resource, 1L)));
        builder.route(route("fixture/current", Map.of(resource, 1L), Map.of(), Map.of(other, 1L)));
        builder.route(route("fixture/prototype", Map.of(resource, 1L), Map.of(), Map.of(other, 1L)));
        builder.route(route("fixture/catalyst", Map.of(other, 1L), Map.of(resource, 1L), Map.of(other, 1L)));
        builder.route(route("fixture/tag_out", Map.of(), Map.of(), Map.of(tag, 1L)));
        builder.tag(tag, new TagResolution(Set.of(resource, other), true, Set.of(), List.of("fixture/tag")));
        builder.terminal(new Terminal("fixture/terminal", resource, TerminalKind.MANUAL_WORLD, null,
                false, false, null, 0, "Fixture, not real entry supplies.", "fixture/terminal_source"));
        SustainabilityAnnotations annotations = new SustainabilityAnnotations(Map.of(resource, metadata),
                Map.of("fixture/source", new RoutePolicy(0, false, Set.of()),
                        "fixture/current", new RoutePolicy(0, false, Set.of()),
                        "fixture/catalyst", new RoutePolicy(0, false, Set.of())),
                Map.of(), Map.of(), Map.of("fixture_plan", new CommittedPlan("fixture_plan", "fixture_system", 5, "fixture/plan")),
                Map.of());
        DebtManifest debt = DebtManifest.empty();
        return new SustainabilityAudit.Result(builder.build(), annotations, Map.of(), debt, debt.apply(List.of()));
    }

    private static Route route(String id, Map<Resource, Long> inputs, Map<Resource, Long> catalysts, Map<Resource, Long> outputs) {
        return new Route(id, Authority.PROCESS_GRAPH, "fixture/source_reference", "QUERN", inputs, catalysts,
                outputs, Set.of(), 1, 0, Map.of(), Set.of(), false, true, true);
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, id);
    }

    @FunctionalInterface
    private interface Attempt { void run() throws Exception; }

    private static void expectFailure(String label, Attempt attempt) {
        boolean failed = false;
        try {
            attempt.run();
        } catch (Exception | AssertionError expected) {
            failed = true;
        }
        equal(label, true, failed);
    }

    private static void equal(String label, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + ", actual " + actual);
        }
    }
}
