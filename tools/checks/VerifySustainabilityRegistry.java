package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;
import static io.github.ezequiel24123z.grindless.sustainability.TagResolver.ElementStatus.*;

/**
 * The game-specific registry edge: real vanilla registries plus the checked unconditional
 * Grindless registration profile. Parsing declarations never initializes Architectury or a server.
 */
public final class VerifySustainabilityRegistry {
    private static final Path JAVA = Path.of("common", "src", "main", "java", "io", "github",
            "ezequiel24123z", "grindless");
    private static int passed;

    private VerifySustainabilityRegistry() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<String, String> hashes = new TreeMap<>();
        Map<Resource, String> declared = registrations(root, hashes);
        TagResolver.ElementLookup registry = registry(declared, hashes);
        eq("actual vanilla membership does not use the default AIR fallback", ABSENT,
                registry.element(item("minecraft:grindless_missing_fixture")));
        eq("actual vanilla item registry contains iron", PRESENT,
                registry.element(item("minecraft:iron_ingot")));
        eq("actual vanilla block registry is not confused with items", PRESENT,
                registry.element(new Resource(Kind.ITEM, "minecraft:oak_planks", 0, "blocks")));
        eq("actual vanilla fluid registry contains water", PRESENT,
                registry.element(fluid("minecraft:water")));
        eq("a checked unconditional Grindless declaration is present in the standalone profile", PRESENT,
                registry.element(item("grindless:biomass")));
        eq("an unobserved Grindless name is unknown, never absent by namespace", UNKNOWN,
                registry.element(item("grindless:grindless_missing_fixture")));
        eq("a block-only declaration does not prove an item or global item absence", UNKNOWN,
                registry.element(item("grindless:horizon_shell")));
        eq("a block-only declaration remains a block", PRESENT,
                registry.element(new Resource(Kind.ITEM, "grindless:horizon_shell", 0, "blocks")));
        eq("unbound external registry conditions stay unknown", UNKNOWN,
                registry.element(item("other_mod:conditional_member")));
        conditionalDeclarations(root);

        Builder declarationsOnly = new Builder();
        AuthorityAdapters.contributeCheckedDeclarations(declarationsOnly, registry);
        eq("the checked inventory contains exactly the verified declarations with no parser extras",
                declared.keySet(), declarationsOnly.build().resources());
        eq("every checked declaration has one public provenance identity", (long) declared.size(),
                declarationsOnly.build().inputHashes().keySet().stream()
                        .filter(key -> key.startsWith("registry_declaration:")).count());
        Snapshot snapshot = AuthorityAdapters.canonical(root, registry);
        TagResolution iron = snapshot.tags().get(tag("forge:ingots/iron"));
        eq("the resolved real Forge iron tag is complete", true, iron.complete());
        eq("the resolved real Forge iron tag contains its actual registered vanilla member",
                Set.of(item("minecraft:iron_ingot")), iron.members());
        eq("the canonical boards tag combines actual vanilla and Grindless registrations", true,
                snapshot.tags().get(tag("grindless:boards")).complete());
        eq("the canonical boards tag retains the registered native replacement", true,
                snapshot.tags().get(tag("grindless:boards")).members().contains(item("grindless:fibreboard")));
        for (Resource resource : declared.keySet()) {
            eq("registered prototype is inventoried without tier acceptance: " + resource.key(), true,
                    snapshot.resources().contains(resource));
        }
        DebtManifest debt = DebtManifest.read(root.resolve(Path.of("tools", "sustainability", "debt.json")));
        var audit = SustainabilityAudit.run(snapshot, SustainabilityAnnotations.empty(), debt);
        Process git = new ProcessBuilder("git", "--no-pager", "rev-parse", "HEAD").directory(root.toFile())
                .redirectErrorStream(true).start();
        String revision = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (git.waitFor() != 0 || !revision.matches("[0-9a-f]{40}")) {
            throw new IOException("Cannot label the canonical registry inventory with its actual source revision.");
        }
        Path reports = root.resolve(Path.of("build", "sustainability", "canonical-inventory"));
        AuditReports.Hashes written = AuditReports.write(reports, audit, revision, true);
        JsonObject report = JsonParser.parseString(Files.readString(reports.resolve("audit.json"))).getAsJsonObject();
        eq("the public inventory preserves source-bound registry evidence", true,
                report.getAsJsonObject("input_hashes").has("registry_profile:standalone_unconditional_grindless"));
        eq("registry binding alone is not sustainability or Tier acceptance", false,
                report.get("sustainability_pass").getAsBoolean());
        System.out.println("CANONICAL REGISTRY INVENTORY: " + snapshot.resources().size() + " resources, "
                + snapshot.routes().size() + " routes, " + snapshot.tags().size() + " tags, "
                + snapshot.objectives().size() + " objectives; complete tags="
                + snapshot.tags().values().stream().filter(TagResolution::complete).count());
        System.out.println("CANONICAL JSON SHA-256 " + written.json());
        System.out.println("CANONICAL MARKDOWN SHA-256 " + written.markdown());
        System.out.println(passed + " passed, 0 failed");
    }

    private static TagResolver.ElementLookup registry(Map<Resource, String> declared, Map<String, String> inputs) {
        Map<String, Set<String>> vanilla = new TreeMap<>();
        vanilla.put("items", new TreeSet<>(BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).toList()));
        vanilla.put("blocks", new TreeSet<>(BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString).toList()));
        vanilla.put("fluids", new TreeSet<>(BuiltInRegistries.FLUID.keySet().stream().map(Object::toString).toList()));
        vanilla.forEach((kind, keys) -> inputs.put("registry_membership:vanilla_" + kind,
                AuthorityAdapters.sha256(String.join("\n", keys).getBytes(StandardCharsets.UTF_8))));
        inputs.put("registry_profile:standalone_unconditional_grindless", AuthorityAdapters.sha256(
                ("condition=successful_unchanged_Grindless_init\n"
                        + "evidence=direct_static_initializers_checked_helper_bodies_and_live_SupplyCatalogue\n"
                        + "absence=unknown_outside_the_checked_declaration_profile\n"
                        + String.join("\n", declared.entrySet().stream()
                        .map(entry -> entry.getKey().key() + "@" + entry.getValue()).sorted().toList()))
                        .getBytes(StandardCharsets.UTF_8)));
        for (String condition : List.of("vanilla_bootstrap_evaluated_in_this_check",
                "successful_Grindless_init_required_source_profile_checked_not_a_fresh_Forge_registry_event",
                "undeclared_Grindless_membership_unknown_no_observed_closed_runtime_universe")) {
            inputs.put("registry_condition:" + condition,
                    AuthorityAdapters.sha256(condition.getBytes(StandardCharsets.UTF_8)));
        }
        Map<String, String> evidence = Map.copyOf(inputs);
        return new TagResolver.ElementLookup() {
            @Override
            public TagResolver.ElementStatus element(Resource resource) {
                ResourceLocation id = ResourceLocation.tryParse(resource.id());
                if (id == null) {
                    return ABSENT;
                }
                boolean present = switch (resource.registry()) {
                    case "items" -> BuiltInRegistries.ITEM.containsKey(id);
                    case "blocks" -> BuiltInRegistries.BLOCK.containsKey(id);
                    case "fluids" -> BuiltInRegistries.FLUID.containsKey(id);
                    default -> false;
                };
                if (present || declared.containsKey(resource)) {
                    return PRESENT;
                }
                if (id.getNamespace().equals("minecraft") && vanilla.containsKey(resource.registry())) {
                    return ABSENT;
                }
                return UNKNOWN;
            }

            @Override
            public Map<String, String> inputHashes() {
                return evidence;
            }

            @Override
            public Map<Resource, String> declarations() {
                return Map.copyOf(declared);
            }
        };
    }

    private static Map<Resource, String> registrations(Path root, Map<String, String> hashes) throws IOException {
        ClassTree items = source(root, JAVA.resolve(Path.of("registry", "ModItems.java")), hashes);
        ClassTree blocks = source(root, JAVA.resolve(Path.of("registry", "ModBlocks.java")), hashes);
        ClassTree common = source(root, JAVA.resolve("Grindless.java"), hashes);
        ClassTree forge = source(root, Path.of("forge", "src", "main", "java", "io", "github",
                "ezequiel24123z", "grindless", "forge", "GrindlessForge.java"), hashes);
        eq("the registration namespace comes from the actual common entrypoint", "\"grindless\"",
                normalized(field(common, "MOD_ID").getInitializer()));
        eq("the actual common initialization reaches both registers unconditionally",
                "GrindlessConfig.load();ModBlocks.register();ModItems.register();",
                method(common, "init", 0).getBody().getStatements().stream().limit(3)
                        .map(VerifySustainabilityRegistry::normalized).reduce("", String::concat));
        eq("the actual Forge entrypoint calls common init after registering its mod event bus",
                List.of("IEventBusbus=FMLJavaModLoadingContext.get().getModEventBus()",
                        "EventBuses.registerModEventBus(Grindless.MOD_ID,bus);", "Grindless.init();"),
                method(forge, "<init>", 0).getBody().getStatements().stream().limit(3)
                        .map(VerifySustainabilityRegistry::normalized).toList());
        eq("the actual Forge mod annotation selects this registration entrypoint", true,
                forge.getModifiers().getAnnotations().stream().map(VerifySustainabilityRegistry::normalized)
                        .anyMatch(annotation -> annotation.equals("@Mod(Grindless.MOD_ID)")));
        Map<Resource, String> declared = new TreeMap<>();
        addDeclarations(items, false, declared);
        addDeclarations(blocks, true, declared);
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            declaration(declared, item("grindless:" + entry.itemName()),
                    "SupplyCatalogue.entries/" + entry.itemName());
        }
        eq("both actual registration authorities fit the closed declaration grammar", true, !declared.isEmpty());
        return Map.copyOf(declared);
    }

    private static void addDeclarations(ClassTree type, boolean blocks, Map<Resource, String> declared) throws IOException {
        closedRegistrationClass(type, blocks);
        for (Tree member : type.getMembers()) {
            if (!(member instanceof VariableTree field) || !field.getType().toString().startsWith("RegistrySupplier<")) {
                continue;
            }
            if (!field.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC, Modifier.FINAL))
                    || !(field.getInitializer() instanceof MethodInvocationTree call)
                    || call.getArguments().size() != 2 || !(call.getArguments().get(0) instanceof LiteralTree literal)
                    || !(literal.getValue() instanceof String id)
                    || !(call.getArguments().get(1) instanceof LambdaExpressionTree supplier)
                    || !supplier.getParameters().isEmpty() || !(supplier.getBody() instanceof NewClassTree instance)
                    || instance.getClassBody() != null
                    || !Set.of("register", "BLOCKS.register").contains(call.getMethodSelect().toString())
                    || !blocks && !call.getMethodSelect().toString().equals("register")) {
                throw new IOException("Conditional/unrecognized registry declaration: "
                        + type.getSimpleName() + "." + field.getName() + "; membership not evaluated.");
            }
            String provenance = type.getSimpleName() + "." + field.getName();
            if (blocks) {
                declaration(declared, new Resource(Kind.ITEM, "grindless:" + id, 0, "blocks"), provenance);
            }
            if (!blocks || call.getMethodSelect().toString().equals("register")) {
                declaration(declared, item("grindless:" + id), provenance);
            }
        }
    }

    private static void declaration(Map<Resource, String> declared, Resource resource, String source) throws IOException {
        if (declared.putIfAbsent(resource, source) != null) {
            throw new IOException("Duplicate registration declaration: " + resource.key() + "; membership not evaluated.");
        }
    }

    private static void closedRegistrationClass(ClassTree type, boolean blocks) throws IOException {
        if (!type.getModifiers().getFlags().contains(Modifier.FINAL) || type.getExtendsClause() != null
                || !type.getImplementsClause().isEmpty() || !type.getTypeParameters().isEmpty()) {
            throw new IOException("Unexamined registration authority class shape; membership not evaluated.");
        }
        String owner = blocks ? "BLOCKS" : "ITEMS";
        String element = blocks ? "Block" : "Item";
        Set<String> methods = new TreeSet<>();
        Set<String> authorityFields = new TreeSet<>();
        for (Tree member : type.getMembers()) {
            if (member instanceof VariableTree field) {
                String name = field.getName().toString();
                String fieldType = normalized(field.getType());
                boolean registry = name.equals(owner) && fieldType.equals("DeferredRegister<" + element + ">")
                        && normalized(field.getInitializer()).equals(
                        "DeferredRegister.create(Grindless.MOD_ID,Registries." + (blocks ? "BLOCK" : "ITEM") + ")");
                boolean supply = !blocks && name.equals("SUPPLY")
                        && fieldType.equals("List<RegistrySupplier<Item>>")
                        && normalized(field.getInitializer()).equals("registerSupply()");
                if (!field.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC, Modifier.FINAL))
                        || !registry && !supply && !fieldType.startsWith("RegistrySupplier<")) {
                    throw new IOException("Unexamined registration field " + type.getSimpleName() + "." + name
                            + "; membership not evaluated.");
                }
                if (registry || supply) {
                    authorityFields.add(name);
                }
                continue;
            }
            if (!(member instanceof MethodTree method)) {
                throw new IOException("Unexamined registration initializer/member in " + type.getSimpleName()
                        + ": " + member.getKind() + "; membership not evaluated.");
            }
            String name = method.getName().toString();
            int arity = method.getParameters().size();
            String signature = name + "#" + arity;
            if (!methods.add(signature)) {
                throw new IOException("Ambiguous registration helper " + signature + "; membership not evaluated.");
            }
            String body = normalized(method.getBody());
            boolean constructor = name.equals("<init>") && arity == 0 && body.equals("{}");
            boolean helper = name.equals("register") && arity == 2
                    && method.getModifiers().getFlags().contains(Modifier.STATIC)
                    && method.getParameters().stream().map(parameter -> normalized(parameter.getType())).toList()
                    .equals(List.of("String", "Supplier<T>"))
                    && normalized(method.getReturnType()).equals("RegistrySupplier<T>")
                    && method.getTypeParameters().stream().map(VerifySustainabilityRegistry::normalized).toList()
                    .equals(List.of("Textends" + element))
                    && body.equals(blocks ? "{RegistrySupplier<T>registered=BLOCKS.register(name,block);"
                    + "ModItems.register(name,()->newBlockItem(registered.get(),newItem.Properties()));returnregistered;}"
                    : "{returnITEMS.register(name,item);}");
            boolean submit = name.equals("register") && arity == 0
                    && method.getModifiers().getFlags().contains(Modifier.STATIC)
                    && normalized(method.getReturnType()).equals("void") && body.equals("{" + owner + ".register();}");
            boolean supply = !blocks && name.equals("registerSupply") && arity == 0
                    && method.getModifiers().getFlags().contains(Modifier.STATIC)
                    && normalized(method.getReturnType()).equals("List<RegistrySupplier<Item>>")
                    && body.equals("{List<RegistrySupplier<Item>>supply=newArrayList<>();"
                    + "for(SupplyCatalogue.Entryentry:SupplyCatalogue.entries()){"
                    + "supply.add(register(entry.itemName(),()->newSupplyItem(entry,newItem.Properties())));}"
                    + "returnCollections.unmodifiableList(supply);}");
            boolean properties = blocks && name.equals("machine") && arity == 0
                    && method.getModifiers().getFlags().contains(Modifier.STATIC)
                    && normalized(method.getReturnType()).equals("BlockBehaviour.Properties")
                    && body.equals("{returnBlockBehaviour.Properties.of().mapColor(MapColor.METAL)"
                    + ".sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()"
                    + ".lightLevel(MachineStatus::lightOf);}");
            if (!constructor && !helper && !submit && !supply && !properties) {
                throw new IOException("Unexamined registration method/helper " + type.getSimpleName() + "."
                        + signature + "; membership not evaluated.");
            }
        }
        if (!methods.containsAll(Set.of("<init>#0", "register#0", "register#2",
                blocks ? "machine#0" : "registerSupply#0"))
                || !authorityFields.contains(owner) || !blocks && !authorityFields.contains("SUPPLY")) {
            throw new IOException("Incomplete registration helper profile; membership not evaluated.");
        }
    }

    private static void conditionalDeclarations(Path root) throws IOException {
        String items = Files.readString(root.resolve(JAVA.resolve(Path.of("registry", "ModItems.java"))));
        String blocks = Files.readString(root.resolve(JAVA.resolve(Path.of("registry", "ModBlocks.java"))));
        rejectDeclaration("a conditional field is not unconditional membership", "ModItems", items,
                "public static final RegistrySupplier<Item> CONDITIONAL = enabled"
                        + " ? register(\"maybe\", () -> new Item(new Item.Properties())) : null;", false);
        rejectDeclaration("static registration blocks cannot be ignored", "ModItems", items,
                "static { if (enabled) register(\"hidden_static\", () -> new Item(new Item.Properties())); }", false);
        rejectDeclaration("other field types cannot hide registration", "ModItems", items,
                "static final Object HIDDEN = register(\"hidden_field\", () -> new Item(new Item.Properties()));", false);
        rejectDeclaration("opaque initializer helpers cannot establish a closed declaration set", "ModItems", items,
                "static final Object HIDDEN = unresolvedHelper();", false);
        rejectDeclaration("an uninvoked method is not a declaration or silently ignored authority", "ModItems", items,
                "static void unused() { register(\"ghost\", () -> new Item(new Item.Properties())); }", false);
        rejectDeclaration("computed names cannot invent a literal-prefix registration", "ModItems", items,
                "static final RegistrySupplier<Item> CONCAT = register(\"base\" + suffix,"
                        + " () -> new Item(new Item.Properties()));", false);
        rejectDeclaration("a disabled three-argument overload is never bound to the two-argument helper",
                "ModItems", items, "static final RegistrySupplier<Item> OFF = register(\"disabled\","
                        + " () -> new Item(new Item.Properties()), false);"
                        + "static <T extends Item> RegistrySupplier<T> register(String name, Supplier<T> item,"
                        + " boolean enabled) { return enabled ? ITEMS.register(name, item) : null; }", false);
        rejectDeclaration("same-arity overload resolution is not guessed", "ModItems", items,
                "static RegistrySupplier<Item> register(String name, Runnable factory) { return null; }", false);
        rejectDeclaration("unknown declaration helpers are not evaluated", "ModItems", items,
                "static final RegistrySupplier<Item> ALIAS = alias(\"alias\","
                        + " () -> new Item(new Item.Properties()));", false);
        rejectDeclaration("direct deferred calls also require their actual API arity", "ModBlocks", blocks,
                "static final RegistrySupplier<Block> OFF = BLOCKS.register(\"disabled\","
                        + " () -> new Block(BlockBehaviour.Properties.of()), false);", true);
        rejectProfile("a helper with a different parameter binding is not silently accepted", "ModItems",
                items.replace("register(String name, Supplier<T> item)",
                        "register(Object name, Supplier<T> item)"), false);
        rejectProfile("the validated API's import binding cannot be substituted", "ModItems",
                items.replace("import dev.architectury.registry.registries.DeferredRegister;",
                        "import unknown.DeferredRegister;"), false);
    }

    private static void rejectDeclaration(String message, String name, String source, String extra,
                                          boolean blocks) throws IOException {
        int end = source.lastIndexOf('}');
        rejectProfile(message, name, source.substring(0, end) + extra + source.substring(end), blocks);
    }

    private static void rejectProfile(String message, String name, String source, boolean blocks) throws IOException {
        try {
            addDeclarations(parse(name, source), blocks, new TreeMap<>());
            throw new AssertionError(message + ": an unexamined profile was certified.");
        } catch (IOException expected) {
            eq(message, true,
                    expected.getMessage().contains("membership not evaluated"));
        }
    }

    private static ClassTree source(Path root, Path relative, Map<String, String> hashes) throws IOException {
        byte[] bytes = Files.readAllBytes(root.resolve(relative));
        hashes.put("registration_source:" + relative.toString().replace('\\', '/'), AuthorityAdapters.sha256(bytes));
        String name = relative.getFileName().toString().replace(".java", "");
        return parse(name, new String(bytes, StandardCharsets.UTF_8));
    }

    private static ClassTree parse(String name, String text) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("A JDK compiler is required to inspect the actual registration source profile.");
        }
        JavaFileObject input = new SimpleJavaFileObject(URI.create("string:///" + name + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return text;
            }
        };
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics, List.of("-proc:none"),
                    null, List.of(input));
            var parsed = task.parse().iterator().next();
            if (diagnostics.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IOException("Cannot inspect syntactically invalid registration source: " + name);
            }
            if (Set.of("ModItems", "ModBlocks").contains(name)) {
                if (!normalized(parsed.getPackageName()).equals("io.github.ezequiel24123z.grindless.registry")) {
                    throw new IOException("Unexamined registration authority package; membership not evaluated.");
                }
                Map<String, String> bindings = new TreeMap<>(Map.of(
                        "DeferredRegister", "dev.architectury.registry.registries.DeferredRegister",
                        "RegistrySupplier", "dev.architectury.registry.registries.RegistrySupplier",
                        "Supplier", "java.util.function.Supplier",
                        "Grindless", "io.github.ezequiel24123z.grindless.Grindless",
                        "Item", "net.minecraft.world.item.Item",
                        "Registries", "net.minecraft.core.registries.Registries"));
                if (name.equals("ModItems")) {
                    bindings.putAll(Map.of("SupplyCatalogue", "io.github.ezequiel24123z.grindless.material.SupplyCatalogue",
                            "SupplyItem", "io.github.ezequiel24123z.grindless.material.SupplyItem",
                            "ArrayList", "java.util.ArrayList", "Collections", "java.util.Collections",
                            "List", "java.util.List"));
                } else {
                    bindings.putAll(Map.of("Block", "net.minecraft.world.level.block.Block",
                            "BlockItem", "net.minecraft.world.item.BlockItem",
                            "BlockBehaviour", "net.minecraft.world.level.block.state.BlockBehaviour",
                            "MachineStatus", "io.github.ezequiel24123z.grindless.machine.MachineStatus",
                            "MapColor", "net.minecraft.world.level.material.MapColor",
                            "SoundType", "net.minecraft.world.level.block.SoundType"));
                }
                for (var binding : bindings.entrySet()) {
                    List<String> imports = parsed.getImports().stream().filter(statement ->
                                    !statement.isStatic() && statement.getQualifiedIdentifier().toString()
                                            .endsWith("." + binding.getKey()))
                            .map(statement -> statement.getQualifiedIdentifier().toString()).toList();
                    if (!imports.equals(List.of(binding.getValue()))) {
                        throw new IOException("Unexamined registration API import " + binding.getKey()
                                + "; membership not evaluated.");
                    }
                }
            }
            return parsed.getTypeDecls().stream().filter(ClassTree.class::isInstance).map(ClassTree.class::cast)
                    .filter(type -> type.getSimpleName().contentEquals(name)).findFirst()
                    .orElseThrow(() -> new IOException("Missing registration authority class: " + name));
        }
    }

    private static VariableTree field(ClassTree type, String name) throws IOException {
        return type.getMembers().stream().filter(VariableTree.class::isInstance).map(VariableTree.class::cast)
                .filter(field -> field.getName().contentEquals(name)).findFirst()
                .orElseThrow(() -> new IOException("Missing registration authority field: " + name));
    }

    private static MethodTree method(ClassTree type, String name, int parameters) throws IOException {
        List<MethodTree> methods = type.getMembers().stream().filter(MethodTree.class::isInstance).map(MethodTree.class::cast)
                .filter(method -> method.getName().contentEquals(name) && method.getParameters().size() == parameters)
                .toList();
        if (methods.size() != 1) {
            throw new IOException("Missing or ambiguous registration authority method: " + name
                    + "; membership not evaluated.");
        }
        return methods.get(0);
    }

    private static String normalized(Tree tree) {
        return tree == null ? "" : tree.toString().replaceAll("\\s+", "");
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, id);
    }

    private static Resource tag(String id) {
        return new Resource(Kind.TAG, id);
    }

    private static Resource fluid(String id) {
        return new Resource(Kind.FLUID, id, 0, "fluids");
    }

    private static void eq(String message, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
        passed++;
    }
}
