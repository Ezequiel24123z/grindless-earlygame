package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.recipe.SolarLogic;
import io.github.ezequiel24123z.grindless.recipe.T1Recipes;
import io.github.ezequiel24123z.grindless.recipe.ThermalLogic;
import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Read-only adapters over existing catalogues and shipped JSON. They make no sustainability,
 * tier, optional-quest or environmental-terminal declarations.
 */
public final class AuthorityAdapters {
    static final String IMPLEMENTATION_SOURCE_PREFIX = "implementation_source:";
    private static final String PROJECT_PACKAGE = "io.github.ezequiel24123z.grindless";
    private static final String IMPLEMENTATION_PATH = PROJECT_PACKAGE.replace('.', '/') + "/sustainability/";
    private static final Path JAVA_ROOT = Path.of("common", "src", "main", "java");
    private static final Pattern NON_CODE = Pattern.compile(
            "\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|/\\*[\\s\\S]*?\\*/|//[^\\r\\n]*");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern PROJECT_REFERENCE = Pattern.compile(
            "\\b" + Pattern.quote(PROJECT_PACKAGE) + "(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern WILDCARD_IMPORT = Pattern.compile(
            "\\bimport\\s+(?:static\\s+)?(" + Pattern.quote(PROJECT_PACKAGE)
                    + "(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+)\\.\\*\\s*;");

    private AuthorityAdapters() {
    }

    /**
     * Reads the S1 standalone inventory, deliberately exposing all non-local tag dependencies.
     *
     * @param root repository root
     * @return unannotated live authorities
     * @throws IOException if an authority cannot be read
     */
    public static Snapshot live(Path root) throws IOException {
        Builder builder = new Builder();
        TagResolver tags = new TagResolver(root, AuthorityAdapters.class.getClassLoader(), false);
        for (Resource tag : tags.localTags()) {
            tags.resolve(tag);
        }
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            builder.resource(item("grindless:" + entry.itemName()));
        }
        List<ProcessRecipe> graph = ProcessGraph.generate(materialViews());
        for (ProcessRecipe recipe : graph) {
            Route route = process(recipe);
            builder.route(route);
            for (Resource resource : referenced(route)) {
                if (resource.kind() == Kind.TAG) {
                    tags.resolve(resource);
                }
            }
        }
        readCrafting(root, builder, tags);
        readQuests(builder);
        readGenerators(graph, builder);
        tags.contribute(builder);

        authoritySourceHashes(root).forEach(builder::input);
        implementationSourceHashes(root).forEach((path, hash) -> builder.input(IMPLEMENTATION_SOURCE_PREFIX + path, hash));
        builder.finding(unknown("canonical_tag_resolution_unknown", "local_only",
                "S1 reads local tag JSON only, not the complete ordered vanilla/Forge runtime resource pack. "
                        + "Observed local members are not evidence of canonical runtime tag completeness."));
        builder.finding(unknown("generator_fuel_unknown", "furnace_fuel_table",
                "The platform furnace-fuel registry is dynamic. Only live loader-free biomass/gas tables are projected."));
        builder.finding(unknown("bootstrap_sources_unknown", "manual_world_and_labour",
                "No manual gathering, crank, collector or world terminal is declared in S1."));
        builder.finding(unknown("vanilla_allowlist_unknown", "vanilla_transformations",
                "No canonical vanilla transformation allowlist is authored in S1."));
        return builder.build();
    }

    /** Returns the standalone material view, never adding synthetic fixture materials to it. */
    public static List<ProcessGraph.MaterialView> materialViews() {
        List<ProcessGraph.MaterialView> result = new ArrayList<>();
        for (SupplyCatalogue.Supplied material : SupplyCatalogue.materials()) {
            result.add(new ProcessGraph.MaterialView(material.name(),
                    has(material, MaterialForm.RAW), has(material, MaterialForm.ORE),
                    has(material, MaterialForm.CRUSHED), has(material, MaterialForm.OXIDE),
                    has(material, MaterialForm.INGOT), has(material, MaterialForm.PLATE),
                    has(material, MaterialForm.ROD), has(material, MaterialForm.GEAR),
                    has(material, MaterialForm.WASHED_CRUSHED)));
        }
        return List.copyOf(result);
    }

    /** Adapts one process with its actual quantities, Matrix ratings, sinks, conditions and rates. */
    public static Route process(ProcessRecipe recipe) {
        Map<Resource, Long> inputs = new TreeMap<>();
        Map<Resource, Long> catalysts = new TreeMap<>();
        Map<Resource, Long> outputs = new TreeMap<>();
        Set<Resource> discharged = new TreeSet<>();
        recipe.inputs().forEach(input -> add(inputs, resource(input.kind(), input.id(), input.matrixRating()),
                input.count()));
        recipe.catalysts().forEach(input -> add(catalysts,
                resource(input.kind(), input.id(), input.matrixRating()), input.count()));
        for (OutputSpec output : recipe.outputs()) {
            Resource resource = resource(output.kind(), output.id(), output.matrixRating());
            add(outputs, resource, output.count());
            if (output.vented()) {
                discharged.add(resource);
            }
        }
        if (recipe.fuPerTick() > 0) {
            add(inputs, ENERGY, Math.multiplyExact(recipe.fuPerTick(), recipe.durationTicks()));
        }
        Map<String, String> conditions = new TreeMap<>();
        if (recipe.namesTemperature()) {
            conditions.put("temperature_c", Double.toString(recipe.temperatureC()));
        }
        if (recipe.namesAtmosphere()) {
            conditions.put("atmosphere", recipe.atmosphere());
        }
        if (recipe.namesAgitation()) {
            conditions.put("agitation", recipe.agitation());
        }
        if (recipe.milestone() != null) {
            conditions.put("informational_milestone", recipe.milestone());
        }
        return new Route(recipe.id(), Authority.PROCESS_GRAPH, "ProcessGraph.generate",
                recipe.family().name(), inputs, catalysts, outputs, discharged,
                recipe.durationTicks(), recipe.fuPerTick(), conditions, Set.of(), false, true);
    }

    /**
     * Reads an explicitly chosen vanilla recipe from the resolved classpath, or reports its absence.
     * This method does not choose the allowlist and does not unlock the recipe.
     */
    public static void vanillaTransformation(String id, ClassLoader loader, Builder builder) throws IOException {
        if (!id.startsWith("minecraft:")) {
            throw new IllegalArgumentException("The vanilla allowlist must name vanilla recipes");
        }
        String path = "data/minecraft/recipes/" + id.substring("minecraft:".length()) + ".json";
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                builder.finding(new AuditFinding("vanilla_recipe_missing", id, "",
                        AuditFinding.Severity.ERROR, "Allowlisted vanilla recipe is absent from the classpath.",
                        List.of("classpath:" + path)));
                return;
            }
            byte[] bytes = stream.readAllBytes();
            String source = "classpath:" + path;
            builder.input(source, sha256(bytes));
            Route route = crafting("vanilla/" + id, JsonParser.parseString(
                    new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(), source,
                    Authority.VANILLA_TRANSFORMATION, builder);
            if (route != null) {
                builder.route(route);
            }
        }
    }

    /** Hashes actual input/report bytes without embedding timestamps or absolute paths. */
    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Inventories transitive project source references from the actual adapter entry points.
     * Source references include inlined constants, imports and same-package types; source
     * hashes do not substitute for the separately normalized, evaluated snapshot.
     */
    static Map<String, String> authoritySourceHashes(Path root) throws IOException {
        Path repository = root.toAbsolutePath().normalize();
        Path java = repository.resolve(JAVA_ROOT);
        Path project = java.resolve(Path.of("io", "github", "ezequiel24123z", "grindless"));
        Map<String, Path> index = new TreeMap<>();
        try (var files = Files.walk(project)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                String name = java.relativize(file).toString().replace('\\', '.').replace('/', '.');
                index.put(name.substring(0, name.length() - 5), file);
            }
        }
        Set<String> pending = new TreeSet<>();
        for (Class<?> entry : List.of(ProcessGraph.class, SupplyCatalogue.class, BootstrapRecipes.class,
                T1Recipes.class, QuestCatalogue.class, FluidLogic.class, ThermalLogic.class, SolarLogic.class,
                SourceLogic.class, MaterialForm.class, FluxTier.class, IngredientSpec.class,
                OutputSpec.class, ProcessRecipe.class)) {
            pending.add(entry.getName());
        }
        Set<String> visited = new TreeSet<>();
        Map<String, String> hashes = new TreeMap<>();
        while (!pending.isEmpty()) {
            String name = pending.iterator().next();
            pending.remove(name);
            if (!visited.add(name)) {
                continue;
            }
            Path file = index.get(name);
            if (file == null) {
                throw new IOException("Missing authority source: " + name);
            }
            byte[] bytes = Files.readAllBytes(file);
            hashes.put(repository.relativize(file).toString().replace('\\', '/'), sha256(bytes));
            String code = NON_CODE.matcher(new String(bytes, StandardCharsets.UTF_8)).replaceAll(" ");
            String packageName = name.substring(0, name.lastIndexOf('.'));
            Matcher identifiers = IDENTIFIER.matcher(code);
            while (identifiers.find()) {
                String samePackage = packageName + "." + identifiers.group();
                if (index.containsKey(samePackage)) {
                    pending.add(samePackage);
                }
            }
            Matcher references = PROJECT_REFERENCE.matcher(code);
            while (references.find()) {
                addSourceReference(references.group(), index, pending);
            }
            Matcher wildcards = WILDCARD_IMPORT.matcher(code);
            while (wildcards.find()) {
                String prefix = wildcards.group(1);
                if (!index.containsKey(prefix)) {
                    index.keySet().stream().filter(type -> type.substring(0, type.lastIndexOf('.')).equals(prefix))
                            .forEach(pending::add);
                }
            }
            pending.removeAll(visited);
        }
        return SustainabilityModel.sortedMap(hashes);
    }

    /** Resolved S1 class bytes and explicit unreadable resources, independent of a Git label. */
    record ImplementationIdentity(Map<String, String> classHashes, Set<String> unresolved) {
        ImplementationIdentity {
            classHashes = SustainabilityModel.sortedMap(classHashes);
            unresolved = SustainabilityModel.sortedSet(unresolved);
        }

        /** Returns the normalized binary-manifest identity, including any coverage gaps. */
        String sha256() {
            StringBuilder manifest = new StringBuilder("s1_class_resources_v1\n");
            classHashes.forEach((path, hash) -> manifest.append(path).append('\0').append(hash).append('\n'));
            unresolved.forEach(path -> manifest.append("unresolved\0").append(path).append('\n'));
            return AuthorityAdapters.sha256(manifest.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Hashes the resolved core and available S1 check entry points, including nested and
     * compiler-generated helpers referenced by their class-file constant pools.
     * Working-tree source hashes are separate: neither identity claims a clean commit.
     */
    static ImplementationIdentity implementationIdentity() {
        ClassLoader loader = AuthorityAdapters.class.getClassLoader();
        Set<String> pending = new TreeSet<>();
        for (Class<?> type : List.of(AuditFinding.class, AuditReports.class, AuthorityAdapters.class,
                ConstructionReserve.class, DebtManifest.class, SustainabilityAnnotations.class,
                SustainabilityAudit.class, SustainabilityModel.class, TagResolver.class)) {
            pending.add(type.getName().replace('.', '/') + ".class");
        }
        for (String check : List.of("VerifySustainability", "VerifySustainabilityAdapters")) {
            String path = IMPLEMENTATION_PATH + check + ".class";
            if (loader.getResource(path) != null) {
                pending.add(path);
            }
        }
        Set<String> visited = new TreeSet<>();
        Map<String, String> hashes = new TreeMap<>();
        Set<String> unresolved = new TreeSet<>();
        while (!pending.isEmpty()) {
            String path = pending.iterator().next();
            pending.remove(path);
            if (!visited.add(path)) {
                continue;
            }
            try (InputStream stream = loader.getResourceAsStream(path)) {
                if (stream == null) {
                    unresolved.add(path);
                    continue;
                }
                byte[] bytes = stream.readAllBytes();
                hashes.put(path, sha256(bytes));
                pending.addAll(implementationReferences(bytes));
            } catch (IOException | RuntimeException unreadable) {
                unresolved.add(path);
            }
            pending.removeAll(visited);
        }
        return new ImplementationIdentity(hashes, unresolved);
    }

    private static Map<String, String> implementationSourceHashes(Path root) throws IOException {
        Path repository = root.toAbsolutePath().normalize();
        Path directory = repository.resolve(JAVA_ROOT).resolve(Path.of("io", "github", "ezequiel24123z",
                "grindless", "sustainability"));
        Map<String, String> hashes = new TreeMap<>();
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                hashes.put(repository.relativize(file).toString().replace('\\', '/'), sha256(Files.readAllBytes(file)));
            }
        }
        for (String check : List.of("VerifySustainability.java", "VerifySustainabilityAdapters.java")) {
            Path file = repository.resolve(Path.of("tools", "checks", check));
            if (Files.isRegularFile(file)) {
                hashes.put(repository.relativize(file).toString().replace('\\', '/'), sha256(Files.readAllBytes(file)));
            }
        }
        return hashes;
    }

    private static void addSourceReference(String reference, Map<String, Path> index, Set<String> pending) {
        String type = reference;
        while (type.startsWith(PROJECT_PACKAGE + ".")) {
            if (index.containsKey(type)) {
                pending.add(type);
                return;
            }
            type = type.substring(0, type.lastIndexOf('.'));
        }
    }

    private static Set<String> implementationReferences(byte[] bytes) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != 0xCAFEBABE) {
                throw new IOException("Invalid class resource");
            }
            input.readUnsignedShort();
            input.readUnsignedShort();
            int count = input.readUnsignedShort();
            String[] utf8 = new String[count];
            List<Integer> classes = new ArrayList<>();
            for (int index = 1; index < count; index++) {
                switch (input.readUnsignedByte()) {
                    case 1 -> utf8[index] = input.readUTF();
                    case 7 -> classes.add(input.readUnsignedShort());
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> input.skipNBytes(4);
                    case 5, 6 -> {
                        input.skipNBytes(8);
                        index++;
                    }
                    case 8, 16, 19, 20 -> input.skipNBytes(2);
                    case 15 -> input.skipNBytes(3);
                    default -> throw new IOException("Unknown class constant");
                }
            }
            Set<String> references = new TreeSet<>();
            for (int index : classes) {
                String name = utf8[index];
                while (name.startsWith("[")) {
                    name = name.substring(1);
                }
                if (name.startsWith("L") && name.endsWith(";")) {
                    name = name.substring(1, name.length() - 1);
                }
                if (name.startsWith(IMPLEMENTATION_PATH)) {
                    references.add(name + ".class");
                }
            }
            return references;
        }
    }

    private static boolean has(SupplyCatalogue.Supplied material, MaterialForm form) {
        return material.vanilla().contains(form) || SupplyCatalogue.isSupplied(material.name(), form);
    }

    private static void readCrafting(Path root, Builder builder, TagResolver tags) throws IOException {
        Map<String, Route> catalogues = new TreeMap<>();
        for (BootstrapRecipes.Shaped craft : BootstrapRecipes.shaped()) {
            catalogues.put(craft.name(), shaped(craft.name(), craft.pattern(), craft.key(), craft.result(),
                    craft.resultCount(), Authority.BOOTSTRAP_RECIPES));
        }
        for (BootstrapRecipes.Shapeless craft : BootstrapRecipes.shapeless()) {
            Map<Resource, Long> inputs = new TreeMap<>();
            craft.ingredients().forEach(input -> add(inputs, qualified(input), 1));
            catalogues.put(craft.name(), craft(craft.name(), inputs, craft.result(), 1,
                    Authority.BOOTSTRAP_RECIPES, "BootstrapRecipes.shapeless"));
        }
        for (T1Recipes.Shaped craft : T1Recipes.shaped()) {
            catalogues.put(craft.name(), shaped(craft.name(), craft.pattern(), craft.key(), craft.result(),
                    craft.resultCount(), Authority.T1_RECIPES));
        }
        Path directory = root.resolve(Path.of("common", "src", "main", "resources", "data", "grindless", "recipes"));
        Set<String> seen = new TreeSet<>();
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String relative = directory.relativize(file).toString().replace('\\', '/');
                String name = relative.substring(0, relative.length() - 5);
                String source = root.relativize(file).toString().replace('\\', '/');
                byte[] bytes = Files.readAllBytes(file);
                builder.input(source, sha256(bytes));
                Route route = crafting("craft/" + name, JsonParser.parseString(
                        new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(), source,
                        Authority.CRAFTING_JSON, builder);
                seen.add(name);
                if (route == null) {
                    continue;
                }
                Route expected = catalogues.get(name);
                if (expected != null) {
                    if (!route.inputs().equals(expected.inputs()) || !route.outputs().equals(expected.outputs())) {
                        builder.finding(new AuditFinding("craft_authority_mismatch", route.id(), "",
                                AuditFinding.Severity.ERROR, "Shipped crafting JSON differs from its live Java catalogue.",
                                List.of(source, expected.source())));
                    }
                    route = new Route(route.id(), expected.authority(), expected.source() + " + " + source,
                            route.family(), route.inputs(), route.catalysts(), route.outputs(), route.discharged(),
                            route.durationTicks(), route.fuPerTick(), route.conditions(), route.prerequisites(),
                            route.oneShot(), route.modOwned());
                }
                builder.route(route);
                for (Resource resource : referenced(route)) {
                    if (resource.kind() == Kind.TAG && !resource.id().startsWith("choices:")) {
                        tags.resolve(resource);
                    }
                }
            }
        }
        for (String name : catalogues.keySet()) {
            if (!seen.contains(name)) {
                builder.finding(new AuditFinding("craft_authority_missing", "craft/" + name, "",
                        AuditFinding.Severity.ERROR, "A catalogue craft has no shipped JSON.", List.of()));
            }
        }
    }

    private static Route shaped(String name, List<String> pattern, Map<String, String> key,
                                String result, long count, Authority authority) {
        Map<Resource, Long> inputs = new TreeMap<>();
        for (String row : pattern) {
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ') {
                    add(inputs, qualified(key.get(String.valueOf(symbol))), 1);
                }
            }
        }
        return craft(name, inputs, result, count, authority, authority.name() + ".shaped");
    }

    private static Route craft(String name, Map<Resource, Long> inputs, String output, long count,
                               Authority authority, String source) {
        return new Route("craft/" + name, authority, source, null, inputs, Map.of(),
                Map.of(item(output), count), Set.of(), 0, 0, Map.of(), Set.of(), false, true);
    }

    private static Route crafting(String id, JsonObject json, String source, Authority authority, Builder builder) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        Map<Resource, Long> inputs = new TreeMap<>();
        String family = null;
        if (type.equals("minecraft:crafting_shaped")) {
            JsonObject keys = json.getAsJsonObject("key");
            if (json.getAsJsonArray("pattern").size() > 2) {
                family = "CRAFTING_TABLE";
            }
            for (JsonElement row : json.getAsJsonArray("pattern")) {
                if (row.getAsString().length() > 2) {
                    family = "CRAFTING_TABLE";
                }
                for (char symbol : row.getAsString().toCharArray()) {
                    if (symbol != ' ') {
                        add(inputs, ingredient(keys.get(String.valueOf(symbol)), id + "/" + symbol, builder), 1);
                    }
                }
            }
        } else if (type.equals("minecraft:crafting_shapeless")) {
            if (json.getAsJsonArray("ingredients").size() > 4) {
                family = "CRAFTING_TABLE";
            }
            int index = 0;
            for (JsonElement element : json.getAsJsonArray("ingredients")) {
                add(inputs, ingredient(element, id + "/" + index++, builder), 1);
            }
        } else {
            builder.finding(unknown("recipe_type_unknown", id, "Unsupported authored recipe type: " + type));
            return null;
        }
        JsonObject result = json.getAsJsonObject("result");
        long count = result.has("count") ? result.get("count").getAsLong() : 1;
        return new Route(id, authority, source, family, inputs, Map.of(),
                Map.of(item(result.get("item").getAsString()), count), Set.of(), 0, 0,
                Map.of(), Set.of(), false, authority != Authority.VANILLA_TRANSFORMATION);
    }

    private static Resource ingredient(JsonElement json, String id, Builder builder) {
        if (json.isJsonArray()) {
            Set<Resource> alternatives = new TreeSet<>();
            int index = 0;
            for (JsonElement element : json.getAsJsonArray()) {
                alternatives.add(ingredient(element, id + "/" + index++, builder));
            }
            Resource choice = new Resource(Kind.TAG, "choices:" + id);
            builder.tag(choice, new TagResolution(alternatives,
                    alternatives.stream().noneMatch(resource -> resource.kind() == Kind.TAG),
                    alternatives.stream().filter(resource -> resource.kind() == Kind.TAG)
                            .map(Resource::key).collect(java.util.stream.Collectors.toSet()), List.of(id)));
            return choice;
        }
        JsonObject object = json.getAsJsonObject();
        if (object.has("item")) {
            return item(object.get("item").getAsString());
        }
        if (object.has("tag")) {
            return new Resource(Kind.TAG, object.get("tag").getAsString());
        }
        throw new IllegalArgumentException("Unsupported ingredient at " + id);
    }

    private static void readQuests(Builder builder) {
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            Resource subject = task.evidence() == QuestCatalogue.Evidence.DIMENSION ? null
                    : new Resource(Kind.ITEM, task.subject(),
                    task.matrixRating() == null ? 0 : task.matrixRating().ordinal(), "items");
            String routeId = "quest/" + task.id();
            builder.objective(new Objective(task.id(), subject, task.count(), Set.copyOf(task.requires()),
                    routeId, "QuestCatalogue.tasks"));
            if (subject == null) {
                builder.finding(unknown("objective_unknown", task.id(), "Dimension evidence requires a world adapter."));
                continue;
            }
            builder.route(new Route(routeId, Authority.QUEST_REWARD, "QuestCatalogue.tasks/" + task.id(),
                    null, Map.of(), Map.of(subject, (long) task.count()),
                    Map.of(item(task.reward().itemId()), (long) task.reward().count()), Set.of(), 0, 0,
                    Map.of("evidence", task.evidence().name()),
                    task.requires().stream().map(id -> "quest/" + id)
                            .collect(java.util.stream.Collectors.toSet()), true, true));
        }
    }

    private static void readGenerators(List<ProcessRecipe> graph, Builder builder) {
        for (boolean biomass : List.of(true, false)) {
            long power = biomass ? FluxTier.F0.nominal() : ThermalLogic.FU_PER_TICK;
            String family = biomass ? "BIOMASS_DYNAMO" : "THERMAL_GENERATOR";
            builder.route(new Route("fuel/" + family.toLowerCase(java.util.Locale.ROOT) + "/biomass",
                    Authority.GENERATOR_FUEL, "SourceLogic.BIOMASS_BURN_TICKS + " + family,
                    family, Map.of(item(SourceLogic.BIOMASS), 1L), Map.of(),
                    Map.of(ENERGY, Math.multiplyExact(power, SourceLogic.BIOMASS_BURN_TICKS)), Set.of(),
                    SourceLogic.BIOMASS_BURN_TICKS, 0, Map.of("generation_fu_per_tick", Long.toString(power)),
                    Set.of(), false, true));
        }
        Set<String> fluids = new TreeSet<>(Set.of(FluidLogic.CARBON_MONOXIDE, FluidLogic.HYDROGEN));
        graph.forEach(recipe -> {
            recipe.fluidInputs().forEach(input -> fluids.add(input.id()));
            recipe.fluidOutputs().forEach(output -> fluids.add(output.id()));
        });
        for (String fluid : fluids) {
            int ticks = FluidLogic.burnTicks(fluid);
            if (ticks > 0) {
                builder.route(new Route("fuel/thermal_generator/" + fluid, Authority.GENERATOR_FUEL,
                        "FluidLogic.burnTicks + ThermalLogic.FU_PER_TICK", "THERMAL_GENERATOR",
                        Map.of(new Resource(Kind.FLUID, fluid), (long) FluidLogic.BUCKET), Map.of(),
                        Map.of(ENERGY, Math.multiplyExact(ThermalLogic.FU_PER_TICK, ticks)), Set.of(),
                        ticks, 0, Map.of("generation_fu_per_tick", Long.toString(ThermalLogic.FU_PER_TICK)),
                        Set.of(), false, true));
            }
        }
        builder.route(new Route("power/solar_array", Authority.GENERATOR_FUEL, "SolarLogic.generate",
                "SOLAR_ARRAY", Map.of(), Map.of(), Map.of(ENERGY, SolarLogic.FU_PER_TICK), Set.of(), 1, 0,
                Map.of("condition", "daylight"), Set.of(), false, true));
    }

    private static Set<Resource> referenced(Route route) {
        Set<Resource> resources = new TreeSet<>(route.inputs().keySet());
        resources.addAll(route.outputs().keySet());
        resources.addAll(route.catalysts().keySet());
        return resources;
    }

    private static Resource resource(String kind, String id, int rating) {
        return new Resource(switch (kind) {
            case IngredientSpec.TAG -> Kind.TAG;
            case IngredientSpec.FLUID -> Kind.FLUID;
            default -> Kind.ITEM;
        }, id, rating, "items");
    }

    private static Resource qualified(String value) {
        int colon = value.indexOf(':');
        return resource(value.substring(0, colon), value.substring(colon + 1), 0);
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, id);
    }

    private static void add(Map<Resource, Long> quantities, Resource resource, long amount) {
        quantities.merge(resource, amount, Math::addExact);
    }

    private static AuditFinding unknown(String rule, String subject, String message) {
        return new AuditFinding(rule, subject, "", AuditFinding.Severity.UNKNOWN, message, List.of());
    }
}
