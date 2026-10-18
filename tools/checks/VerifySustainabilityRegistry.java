package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
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
        Set<Resource> declared = registrations(root, hashes);
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
        eq("a Grindless namespace alone never creates an item", ABSENT,
                registry.element(item("grindless:grindless_missing_fixture")));
        eq("a block-only declaration is not a fake BlockItem", ABSENT,
                registry.element(item("grindless:horizon_shell")));
        eq("a block-only declaration remains a block", PRESENT,
                registry.element(new Resource(Kind.ITEM, "grindless:horizon_shell", 0, "blocks")));
        eq("unbound external registry conditions stay unknown", UNKNOWN,
                registry.element(item("other_mod:conditional_member")));
        conditionalDeclarations();

        Snapshot snapshot = AuthorityAdapters.canonical(root, registry);
        TagResolution iron = snapshot.tags().get(tag("forge:ingots/iron"));
        eq("the resolved real Forge iron tag is complete", true, iron.complete());
        eq("the resolved real Forge iron tag contains its actual registered vanilla member",
                Set.of(item("minecraft:iron_ingot")), iron.members());
        eq("the canonical boards tag combines actual vanilla and Grindless registrations", true,
                snapshot.tags().get(tag("grindless:boards")).complete());
        eq("the canonical boards tag retains the registered native replacement", true,
                snapshot.tags().get(tag("grindless:boards")).members().contains(item("grindless:fibreboard")));
        for (Resource resource : declared) {
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

    private static TagResolver.ElementLookup registry(Set<Resource> declared, Map<String, String> inputs) {
        Map<String, Set<String>> vanilla = new TreeMap<>();
        vanilla.put("items", new TreeSet<>(BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).toList()));
        vanilla.put("blocks", new TreeSet<>(BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString).toList()));
        vanilla.put("fluids", new TreeSet<>(BuiltInRegistries.FLUID.keySet().stream().map(Object::toString).toList()));
        vanilla.forEach((kind, keys) -> inputs.put("registry_membership:vanilla_" + kind,
                AuthorityAdapters.sha256(String.join("\n", keys).getBytes(StandardCharsets.UTF_8))));
        inputs.put("registry_profile:standalone_unconditional_grindless", AuthorityAdapters.sha256(
                ("condition=successful_unchanged_Grindless_init\n"
                        + "evidence=direct_static_initializers_checked_helper_bodies_and_live_SupplyCatalogue\n"
                        + String.join("\n", declared.stream().map(Resource::key).sorted().toList()))
                        .getBytes(StandardCharsets.UTF_8)));
        for (String condition : List.of("vanilla_bootstrap_evaluated_in_this_check",
                "successful_Grindless_init_required_source_profile_checked_not_a_fresh_Forge_registry_event")) {
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
                if (present || declared.contains(resource)) {
                    return PRESENT;
                }
                if (id.getNamespace().equals("minecraft")
                        || id.getNamespace().equals("grindless") && Set.of("items", "blocks").contains(resource.registry())) {
                    return ABSENT;
                }
                return UNKNOWN;
            }

            @Override
            public Map<String, String> inputHashes() {
                return evidence;
            }
        };
    }

    private static Set<Resource> registrations(Path root, Map<String, String> hashes) throws IOException {
        ClassTree items = source(root, JAVA.resolve(Path.of("registry", "ModItems.java")), hashes);
        ClassTree blocks = source(root, JAVA.resolve(Path.of("registry", "ModBlocks.java")), hashes);
        ClassTree common = source(root, JAVA.resolve("Grindless.java"), hashes);
        ClassTree forge = source(root, Path.of("forge", "src", "main", "java", "io", "github",
                "ezequiel24123z", "grindless", "forge", "GrindlessForge.java"), hashes);
        eq("the registration namespace comes from the actual common entrypoint", "\"grindless\"",
                normalized(field(common, "MOD_ID").getInitializer()));
        eq("the item authority is the actual ITEM registry", "DeferredRegister.create(Grindless.MOD_ID,Registries.ITEM)",
                normalized(field(items, "ITEMS").getInitializer()));
        eq("the block authority is the actual BLOCK registry", "DeferredRegister.create(Grindless.MOD_ID,Registries.BLOCK)",
                normalized(field(blocks, "BLOCKS").getInitializer()));
        eq("item helper has no unexamined conditional registration", "{returnITEMS.register(name,item);}",
                normalized(method(items, "register", 2).getBody()));
        eq("block helper unconditionally registers exactly one matching BlockItem",
                "{RegistrySupplier<T>registered=BLOCKS.register(name,block);"
                        + "ModItems.register(name,()->newBlockItem(registered.get(),newItem.Properties()));returnregistered;}",
                normalized(method(blocks, "register", 2).getBody()));
        eq("the supply field always invokes its authoritative list", "registerSupply()",
                normalized(field(items, "SUPPLY").getInitializer()));
        eq("supply registration has no tag/provider/profile-dependent filtering",
                "{List<RegistrySupplier<Item>>supply=newArrayList<>();"
                        + "for(SupplyCatalogue.Entryentry:SupplyCatalogue.entries()){"
                        + "supply.add(register(entry.itemName(),()->newSupplyItem(entry,newItem.Properties())));}"
                        + "returnCollections.unmodifiableList(supply);}",
                normalized(method(items, "registerSupply", 0).getBody()));
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
        eq("the item deferred register is actually submitted", "{ITEMS.register();}",
                normalized(method(items, "register", 0).getBody()));
        eq("the block deferred register is actually submitted", "{BLOCKS.register();}",
                normalized(method(blocks, "register", 0).getBody()));
        Set<Resource> declared = new TreeSet<>();
        addDeclarations(items, false, declared);
        addDeclarations(blocks, true, declared);
        SupplyCatalogue.entries().forEach(entry -> declared.add(item("grindless:" + entry.itemName())));
        return Set.copyOf(declared);
    }

    private static void addDeclarations(ClassTree type, boolean blocks, Set<Resource> declared) throws IOException {
        for (Tree member : type.getMembers()) {
            if (!(member instanceof VariableTree field) || !field.getType().toString().startsWith("RegistrySupplier<")) {
                continue;
            }
            if (!field.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC, Modifier.FINAL))
                    || !(field.getInitializer() instanceof MethodInvocationTree call)
                    || call.getArguments().isEmpty() || !(call.getArguments().get(0) instanceof LiteralTree literal)
                    || !(literal.getValue() instanceof String id)
                    || !Set.of("register", "BLOCKS.register").contains(call.getMethodSelect().toString())
                    || !blocks && !call.getMethodSelect().toString().equals("register")) {
                throw new IOException("Conditional/unrecognized registry declaration: "
                        + type.getSimpleName() + "." + field.getName() + "; membership not evaluated.");
            }
            if (blocks) {
                declared.add(new Resource(Kind.ITEM, "grindless:" + id, 0, "blocks"));
            }
            if (!blocks || call.getMethodSelect().toString().equals("register")) {
                declared.add(item("grindless:" + id));
            }
        }
    }

    private static void conditionalDeclarations() throws IOException {
        ClassTree conditional = parse("Fixture", "class Fixture {"
                + " static final RegistrySupplier<Item> CONDITIONAL = enabled ? register(\"maybe\") : null; }");
        try {
            addDeclarations(conditional, false, new TreeSet<>());
            throw new AssertionError("A conditional registry declaration cannot be certified.");
        } catch (IOException expected) {
            eq("conditions that were not evaluated remain explicit rather than namespace-present", true,
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
        return type.getMembers().stream().filter(MethodTree.class::isInstance).map(MethodTree.class::cast)
                .filter(method -> method.getName().contentEquals(name) && method.getParameters().size() == parameters)
                .findFirst().orElseThrow(() -> new IOException("Missing registration authority method: " + name));
    }

    private static String normalized(Tree tree) {
        return tree.toString().replaceAll("\\s+", "");
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
