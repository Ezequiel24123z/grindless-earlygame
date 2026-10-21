package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
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
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.TagResolver.ElementStatus.*;

/**
 * The game-specific registry edge: real vanilla registries plus the checked unconditional
 * Grindless registration profile. Parsing declarations never initializes Architectury or a server.
 */
public final class VerifySustainabilityRegistry {
    private static final Path JAVA = Path.of("common", "src", "main", "java", "io", "github",
            "ezequiel24123z", "grindless");
    private static final Path FORGE_JAVA = Path.of("forge", "src", "main", "java", "io", "github",
            "ezequiel24123z", "grindless");
    private static final Map<String, Path> QUEST_SOURCES = Map.ofEntries(
            Map.entry("QuestBookItem", JAVA.resolve(Path.of("item", "QuestBookItem.java"))),
            Map.entry("QuestBookMenu", JAVA.resolve(Path.of("menu", "QuestBookMenu.java"))),
            Map.entry("QuestCatalogue", JAVA.resolve(Path.of("quest", "QuestCatalogue.java"))),
            Map.entry("QuestClaimService", JAVA.resolve(Path.of("quest", "QuestClaimService.java"))),
            Map.entry("QuestRewardDelivery", JAVA.resolve(Path.of("quest", "QuestRewardDelivery.java"))),
            Map.entry("QuestBookAccess", JAVA.resolve(Path.of("quest", "QuestBookAccess.java"))),
            Map.entry("QuestBookAccessEvents",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "QuestBookAccessEvents.java"))),
            Map.entry("QuestLogic", JAVA.resolve(Path.of("quest", "QuestLogic.java"))),
            Map.entry("QuestEvidence", JAVA.resolve(Path.of("quest", "QuestEvidence.java"))),
            Map.entry("QuestInventoryEvidence", JAVA.resolve(Path.of("quest", "QuestInventoryEvidence.java"))),
            Map.entry("QuestProgress", JAVA.resolve(Path.of("quest", "QuestProgress.java"))),
            Map.entry("QuestBackendPlatform", JAVA.resolve(Path.of("quest", "QuestBackendPlatform.java"))),
            Map.entry("QuestBackendPlatformImpl",
                    FORGE_JAVA.resolve(Path.of("quest", "forge", "QuestBackendPlatformImpl.java"))),
            Map.entry("GrindlessForge", FORGE_JAVA.resolve(Path.of("forge", "GrindlessForge.java"))),
            Map.entry("GrindlessForgeClient", FORGE_JAVA.resolve(Path.of("forge", "GrindlessForgeClient.java"))),
            Map.entry("FtbQuestIntegration",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "FtbQuestIntegration.java"))),
            Map.entry("FtbQuestNative",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "FtbQuestNative.java"))),
            Map.entry("FtbQuestDefinitions",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "FtbQuestDefinitions.java"))),
            Map.entry("FtbMatrixFilter",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "FtbMatrixFilter.java"))),
            Map.entry("HeraclesQuestNetwork",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "HeraclesQuestNetwork.java"))),
            Map.entry("HeraclesQuestIntegration",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "HeraclesQuestIntegration.java"))),
            Map.entry("GrindlessClaimTask",
                    FORGE_JAVA.resolve(Path.of("forge", "quest", "GrindlessClaimTask.java"))));
    private static int passed;

    private VerifySustainabilityRegistry() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        if (args.length == 1 && args[0].equals("--quest-binding-only")) {
            questAccessCounterexamples(root);
            System.out.println(passed + " passed, 0 failed; source-only quest binding, no game bootstrap.");
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Expected no arguments or --quest-binding-only.");
        }
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
        physicalSourceCounterexamples(root);
        questAccessCounterexamples(root);

        Builder declarationsOnly = new Builder();
        AuthorityAdapters.contributeCheckedDeclarations(declarationsOnly, registry);
        eq("the checked inventory contains exactly the verified declarations with no parser extras",
                declared.keySet(), declarationsOnly.build().resources());
        eq("every checked declaration has one public provenance identity", (long) declared.size(),
                declarationsOnly.build().inputHashes().keySet().stream()
                        .filter(key -> key.startsWith("registry_declaration:")).count());
        SourceBinding sources = sourceBinding(root, AuthorityAdapters.canonical(root, registry), declared);
        Snapshot snapshot = sources.snapshot();
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
        var audit = SustainabilityAudit.run(snapshot, sources.facts(), debt);
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
        eq("the partial source annotations are described honestly in the report mode",
                "report_only_s2_canonical_inventory_partial_annotations", report.get("mode").getAsString());
        eq("the inspected source facts remove the actual ungrounded-generator errors", false,
                audit.findings().findings().stream().anyMatch(finding -> finding.rule().equals("zero_input_unjustified")));
        for (String id : List.of("air/oxygen", "power/solar_array")) {
            eq("source facts do not accept the unplaced prototype: " + id, null,
                    sources.facts().routes().get(id).unlockTier());
            eq("the prototype retains its honest route-tier unknown: " + id, true,
                    audit.findings().findings().stream().anyMatch(finding ->
                            finding.rule().equals("route_tier_unknown") && finding.subject().equals(id)));
            eq("a physical source terminal does not activate the prototype machine: " + id, false,
                    audit.tiers().get(0).availableRoutes().contains(id));
        }
        eq("the optional manual Dynamo has accepted T0 placement, not an invented future tier", 0,
                sources.facts().routes().get("power/hand_crank_dynamo").unlockTier());
        eq("the optional manual Dynamo has no invented environmental permission", Set.of(),
                sources.facts().routes().get("power/hand_crank_dynamo").environmentalSources());
        eq("physical manual action facts never become guaranteed initial gifts", true,
                snapshot.terminals().values().stream().filter(terminal -> terminal.kind() == TerminalKind.MANUAL_LABOUR)
                        .allMatch(terminal -> terminal.guaranteed() == null));
        System.out.println("CANONICAL REGISTRY INVENTORY: " + snapshot.resources().size() + " resources, "
                + snapshot.routes().size() + " routes, " + snapshot.tags().size() + " tags, "
                + snapshot.objectives().size() + " objectives; complete tags="
                + snapshot.tags().values().stream().filter(TagResolution::complete).count());
        System.out.println("CANONICAL JSON SHA-256 " + written.json());
        System.out.println("CANONICAL MARKDOWN SHA-256 " + written.markdown());
        System.out.println(passed + " passed, 0 failed");
    }

    record SourceBinding(Snapshot snapshot, ManualWork localWork, SustainabilityAnnotations facts) {
    }

    static SourceBinding canonicalSources(Path root, Set<String> vanillaAllowlist) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<String, String> hashes = new TreeMap<>();
        Map<Resource, String> declared = registrations(root, hashes);
        return sourceBinding(root, AuthorityAdapters.canonical(root, registry(declared, hashes), vanillaAllowlist), declared);
    }

    private static SourceBinding sourceBinding(Path root, Snapshot inventory,
                                               Map<Resource, String> declared) throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        ClassTree crank = mechanicsSource(root, "item", "CrankHandleItem", hashes);
        ClassTree process = mechanicsSource(root, "machine", "ProcessMachineBlockEntity", hashes);
        ClassTree base = mechanicsSource(root, "machine", "MachineBlockEntity", hashes);
        ClassTree pump = mechanicsSource(root, "fluid", "HandPumpBlockEntity", hashes);
        ClassTree research = mechanicsSource(root, "machine", "ResearchTerminalBlockEntity", hashes);
        ClassTree dynamo = mechanicsSource(root, "machine", "HandCrankDynamoBlockEntity", hashes);
        ClassTree dynamoBlock = mechanicsSource(root, "machine", "HandCrankDynamoBlock", hashes);
        ClassTree solar = mechanicsSource(root, "machine", "SolarArrayBlockEntity", hashes);
        Map<String, ClassTree> quest = questSources(root, hashes);
        ClassTree items = mechanicsSource(root, "registry", "ModItems", hashes);
        mechanicsSource(root, "machine", "ProcessMachineKind", hashes);
        mechanicsSource(root, "source", "SourceLogic", hashes);
        ClassTree worldSite = mechanicsSource(root, "source", "SourceSite", hashes);
        mechanicsSource(root, "fluid", "FluidLogic", hashes);
        mechanicsSource(root, "research", "ResearchLogic", hashes);
        ClassTree blocks = mechanicsSource(root, "registry", "ModBlocks", hashes);
        Map<String, Machine> constructions = new TreeMap<>();
        for (ProcessMachineKind kind : ProcessMachineKind.values()) {
            if (T0Sustainability.processFamilies().contains(kind.family().name())) {
                Resource controller = registeredItem(blocks, kind.name(), "ProcessMachineBlock",
                        "ProcessMachineKind." + kind.name(), declared);
                constructions.put(kind.family().name(), new Machine(kind.family().name(), controller, 0));
            }
        }
        for (String[] binding : List.of(
                new String[]{"HAND_PUMP", "HandPumpBlock"},
                new String[]{"RESEARCH_TERMINAL", "ResearchTerminalBlock"},
                new String[]{"BIOMASS_DYNAMO", "ThermalGeneratorBlock", "GeneratorKind.BIOMASS"},
                new String[]{"HAND_CRANK_DYNAMO", "HandCrankDynamoBlock"})) {
            Resource controller = registeredItem(blocks, binding[0], binding[1],
                    binding.length > 2 ? binding[2] : null, declared);
            constructions.put(binding[0], new Machine(binding[0], controller, 0));
        }
        Resource questBook = registeredItem(items, "QUEST_BOOK", "QuestBookItem", null, declared);
        eq("legacy held-book guards and native framework claim authority match their exact source profiles", true,
                questBookAccess(quest));
        for (Route route : inventory.routes().values()) {
            if (route.authority() == Authority.QUEST_REWARD) {
                eq("the audited legacy-book claim route retains its held-book catalyst: " + route.id(), 1L,
                        route.catalysts().get(questBook));
                eq("the interface book is not consumed by a claim: " + route.id(), false,
                        route.inputs().containsKey(questBook));
            }
        }

        eq("the real tool accepts only ProcessMachine/F0 recipients and server-side cooldown-limited actions",
                "{if(!(context.getLevel().getBlockEntity(context.getClickedPos())instanceofProcessMachineBlockEntitymachine)"
                        + "||machine.kind().tier()!=FluxTier.F0||context.getPlayer()==null){returnInteractionResult.PASS;}"
                        + "if(!context.getLevel().isClientSide()&&!context.getPlayer().getCooldowns().isOnCooldown(this)){"
                        + "machine.crankByHand();context.getPlayer().getCooldowns().addCooldown(this,SourceLogic.MANUAL_COOLDOWN);}"
                        + "returnInteractionResult.sidedSuccess(context.getLevel().isClientSide());}",
                normalized(method(crank, "useOn", 1).getBody()));
        eq("the real recipient clamps local F0/server work and observes only the actual positive delta afterwards",
                PROCESS_CRANK_BODY,
                normalized(method(process, "crankByHand", 0).getBody()));
        eq("the local process buffer cannot export crank energy",
                "{returnnewSimpleFluxStorage(bufferCapacity(),powerTier().nominal(),0L,()->{setChanged();"
                        + "if(getLevel()!=null&&!getLevel().isClientSide()){updateSubscriptions();}});}",
                normalized(method(process, "createEnergyBuffer", 0).getBody()));
        eq("the process buffer retains the real two-second sizing contract", "{return2;}",
                normalized(method(base, "bufferSeconds", 0).getBody()));
        eq("buffer capacity is actual rated throughput times its time window",
                "{returnpowerTier().nominal()*20L*bufferSeconds();}",
                normalized(method(base, "bufferCapacity", 0).getBody()));
        eq("process machines do not silently override that buffer window", false,
                process.getMembers().stream().filter(MethodTree.class::isInstance).map(MethodTree.class::cast)
                        .anyMatch(method -> method.getName().contentEquals("bufferSeconds")));
        eq("one full source-derived crank charge fits an empty F0 process buffer", true,
                SourceLogic.MANUAL_CHARGE <= FluxTier.F0.nominal() * 20L * 2L);
        Set<String> eligible = new TreeSet<>();
        for (ProcessMachineKind kind : ProcessMachineKind.values()) {
            if (kind.tier() == FluxTier.F0) {
                eligible.add(kind.family().name());
            }
        }
        ManualWork work = new ManualWork("manual/crank_handle", item("grindless:crank_handle"),
                SourceLogic.MANUAL_CHARGE, eligible, "CrankHandleItem.useOn + ProcessMachineBlockEntity.crankByHand"
                + " + ProcessMachineKind.values/tier/family + SourceLogic.MANUAL_CHARGE");
        for (Route route : inventory.routes().values()) {
            boolean recipient = route.authority() == Authority.PROCESS_GRAPH && java.util.Arrays.stream(ProcessMachineKind.values())
                    .anyMatch(kind -> kind.family().name().equals(route.family()) && kind.tier() == FluxTier.F0);
            eq("local work eligibility follows actual type/rating rather than namespace: " + route.id(),
                    recipient, work.applies(route));
        }
        eq("ResearchTerminal is not a ProcessMachine recipient", "MachineBlockEntity",
                normalized(research.getExtendsClause()));
        eq("geological production uses its actual rock-bed site",
                io.github.ezequiel24123z.grindless.source.SourceSite.GEOLOGICAL,
                ProcessMachineKind.GEOLOGICAL_COLLECTOR.site());
        eq("bioculture uses its actual terrestrial open-sky/daylight site",
                io.github.ezequiel24123z.grindless.source.SourceSite.SUNLIT_CULTURE,
                ProcessMachineKind.BIOCULTIVATOR.site());
        eq("the current intake has no invented atmosphere/dimension guard",
                io.github.ezequiel24123z.grindless.source.SourceSite.NONE,
                ProcessMachineKind.ATMOSPHERIC_INTAKE.site());
        eq("the actual shared site authority enforces the declared terrestrial/rock/sky/daylight conditions",
                "{if(this==NONE){returnFault.NONE;}if(!terrestrial){returnFault.WRONG_DIMENSION;}"
                        + "if(this==GEOLOGICAL){returnrockBed?Fault.NONE:Fault.NEEDS_ROCK_BED;}"
                        + "if(!openSky){returnFault.NEEDS_OPEN_SKY;}returndaylight?Fault.NONE:Fault.NEEDS_DAYLIGHT;}",
                normalized(method(worldSite, "evaluate", 4).getBody()));
        eq("actual site eligibility checks the loaded world, geological tag and measured sky brightness",
                "{if(kind.site()==SourceSite.NONE||getLevel()==null){returnSourceSite.Fault.NONE;}"
                        + "Stringdimension=getLevel().dimension().location().toString();"
                        + "booleanterrestrial=getLevel().dimension().equals(Level.OVERWORLD)||FactoryCatalogue.isFactory(dimension);"
                        + "booleanrock=getLevel().getBlockState(getBlockPos().below()).is("
                        + "TagKey.create(Registries.BLOCK,Grindless.id(\"geological_bed\")));"
                        + "booleansky=getLevel().canSeeSky(getBlockPos().above());"
                        + "booleandaylight=getLevel().getBrightness(LightLayer.SKY,getBlockPos().above())"
                        + "-getLevel().getSkyDarken()>=9;returnkind.site().evaluate(terrestrial,rock,sky,daylight);}",
                normalized(method(process, "checkSite", 0).getBody()));
        eq("site and formation failures stop the actual work path before recipe execution", true,
                normalized(method(process, "work", 2).getBody()).startsWith(
                        "{if(lastWorkTick==tick){return;}lastWorkTick=tick;structureFault=checkStructure();siteFault=checkSite();"
                                + "if(structureFault!=WorkshopStructure.Fault.NONE||siteFault!=SourceSite.Fault.NONE){"
                                + "publishStatus(cached,false,false,false,false,ConditionReport.OPTIMAL);syncData();return;}"));

        eq("the pump tests a real adjacent or below water source without consuming it",
                "{if(level==null){returnfalse;}returnisSource(worldPosition.relative(facing()))"
                        + "||isSource(worldPosition.below());}", normalized(method(pump, "hasWaterSource", 0).getBody()));
        eq("the pump uses the actual WATER tag and source-state predicates",
                "{net.minecraft.world.level.material.FluidStatefluid=level.getFluidState(pos);"
                        + "returnfluid.is(FluidTags.WATER)&&fluid.isSource();}",
                normalized(method(pump, "isSource", 1).getBody()));
        eq("the automatic pump runs on the server without player work or FU",
                "{if(level.isClientSide()){return;}pump.work();}", normalized(method(pump, "tick", 4).getBody()));
        eq("the pump's physical source batch is bounded and never edits the world source",
                "{booleansource=hasWaterSource();"
                        + "booleanmoved=false;if(source&&buffer.space()>0){inttake=Math.min(FluidLogic.PUMP_MB_PER_TICK,"
                        + "buffer.space());buffer.offer(FluidState.of(FluidLogic.WATER,take));moved=true;}"
                        + "if(!buffer.isEmpty()){FluidStatepulse=buffer.extract(FluidLogic.WATER,FluidLogic.PUMP_MB_PER_TICK);"
                        + "FluidStateleftover=FluidInsert.intoNeighbour(level,worldPosition,facing(),pulse);"
                        + "if(!leftover.isEmpty()){buffer.offer(leftover);}else{moved=true;}}"
                        + "MachineProperties.publish(level,worldPosition,FluidLogic.pumpStatus(source,moved));"
                        + "if(moved){setChanged();sync();}}", normalized(method(pump, "work", 0).getBody()));
        eq("the pump's buffer uses the shared actual capacity", "FluidBuffer.ambient(FluidLogic.BUCKET)",
                normalized(field(pump, "buffer").getInitializer()));
        eq("the shared pump pulse fits in the actual buffer", true,
                FluidLogic.PUMP_MB_PER_TICK > 0 && FluidLogic.PUMP_MB_PER_TICK <= FluidLogic.BUCKET);

        eq("the Terminal's full work cycle draws real FU and transforms one physical item, without a permission",
                "{booleanhasCore=hasCore();if(!hasCore){publishStatus(false,hasOutput(),false,false);"
                        + "updateSubscriptions();return;}requestPower(ResearchLogic.FU_PER_TICK);"
                        + "longdrawn=drawPower(ResearchLogic.FU_PER_TICK);"
                        + "if(drawn<=0L&&energy().getStored()>=ResearchLogic.FU_PER_TICK){"
                        + "energy().setStored(energy().getStored()-ResearchLogic.FU_PER_TICK);drawn=ResearchLogic.FU_PER_TICK;}"
                        + "booleanpowered=drawn>0L||hasPowerWaiting();doublegained=ResearchLogic.work(drawn,ResearchLogic.FU_PER_TICK);"
                        + "if(gained>0.0){progress+=gained;setChanged();}if(progress>=ResearchLogic.CYCLE_TICKS){"
                        + "progress=0.0;input.setItem(0,newItemStack(ModItems.CALIBRATED_DATA_CORE.get()));"
                        + "setChanged();updateSubscriptions();DevTelemetry.processCompleted(getLevel(),"
                        + "\"calibrate/data_core\");return;}publishStatus(true,false,powered,gained>0.0);}",
                normalized(method(research, "work", 0).getBody()));
        eq("the physical core predicate reads its real registered item id",
                "{return!stack.isEmpty()&&ResearchLogic.DATA_CORE.equals("
                        + "BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());}",
                normalized(method(research, "isDataCore", 1).getBody()));
        eq("the Terminal allows only one core per slot", "{return1;}",
                normalized(method(research, "getMaxStackSize", 0).getBody()));
        eq("the physical input accepts only the actual uncalibrated core", "{returnisDataCore(stack)"
                        + "&&input.getItem(0).isEmpty();}",
                normalized(method(research, "canPlaceItem", 2).getBody()));
        eq("hopper insertion delegates to that real input guard", "{returncanPlaceItem(slot,stack);}",
                normalized(method(research, "canPlaceItemThroughFace", 3).getBody()));
        eq("hopper extraction is available only after the physical transformation", "{return!isDataCore(stack);}",
                normalized(method(research, "canTakeItemThroughFace", 3).getBody()));

        eq("the optional physical Dynamo requires an actual server-side player click",
                "{if(level.isClientSide()){returnInteractionResult.SUCCESS;}"
                        + "if(level.getBlockEntity(pos)instanceofHandCrankDynamoBlockEntitydynamo){dynamo.crank();"
                        + "returnInteractionResult.CONSUME;}returnInteractionResult.PASS;}",
                normalized(method(dynamoBlock, "use", 6).getBody()));
        eq("the physical Dynamo charge retains its actual constant and observes only accepted work",
                "{longbefore=energy().getStored();energy().setStored(energy().getStored()+CHARGE_PER_CRANK);"
                        + "DevTelemetry.manualWork(getLevel(),\"hand_crank_dynamo\",energy().getStored()-before);"
                        + "fruitlessPushes=0;updateSubscriptions();publishStatus();}",
                normalized(method(dynamo, "crank", 0).getBody()));
        eq("the physical Dynamo is rated at F0", "{returnFluxTier.F0;}",
                normalized(method(dynamo, "ratedTier", 0).getBody()));
        eq("the actual Dynamo output buffer exports its own rated physical charge",
                "{returnnewSimpleFluxStorage(bufferCapacity(),0L,FluxTier.F0.nominal(),this::setChanged);}",
                normalized(method(dynamo, "createEnergyBuffer", 0).getBody()));
        eq("the physical output is bounded by its actual per-tick rating and period", true,
                dynamoPushRate(dynamo));
        eq("the actual unobstructed push period is one tick", "1",
                normalized(field(dynamo, "PUSH_PERIOD").getInitializer()));
        eq("solar's real site is daylight and open sky, not a terrestrial culture predicate",
                "{returngetLevel()!=null&&getLevel().isDay()&&getLevel().canSeeSky(getBlockPos().above());}",
                normalized(method(solar, "daylight", 0).getBody()));
        eq("solar actually gates generation on that site", true,
                normalized(method(solar, "work", 0).getBody()).startsWith(
                        "{booleanday=daylight();if(day){energy().setStored(energy().getStored()+SolarLogic.generate(true));}"));

        Builder builder = new Builder(inventory);
        hashes.forEach(builder::input);
        builder.input("mechanics_binding:CrankHandle_ProcessMachine_F0",
                AuthorityAdapters.sha256(work.toString().getBytes(StandardCharsets.UTF_8)));
        builder.input("mechanics_binding:QuestBookItem_QuestBookMenu",
                AuthorityAdapters.sha256((questBook.key() + "\n"
                        + quest.entrySet().stream().map(entry -> entry.getKey() + "\n" + questText(entry.getValue()))
                        .reduce("", (left, right) -> left + right + "\n"))
                        .getBytes(StandardCharsets.UTF_8)));
        builder.terminal(new Terminal(work.terminal(), new Resource(Kind.ENVIRONMENT, "grindless:local_crank_actions"),
                TerminalKind.MANUAL_LABOUR, null, false, true, null, 0,
                "A player with the constructed Crank Handle may charge only a server-side F0 ProcessMachine; "
                        + "actions are finite conditional bootstrap work, not portable ENERGY or R.", work.source()));
        builder.terminal(new Terminal("environment/water_source", new Resource(Kind.ENVIRONMENT,
                "grindless:water_source_site"), TerminalKind.ENVIRONMENT, SourceSite.WATER_BODY,
                true, true, null, 0, "HandPumpBlockEntity requires a WATER-tagged source in front or below; "
                + "it offers water automatically without draining that world source or using FU.",
                "HandPumpBlockEntity.hasWaterSource/isSource/work"));
        builder.terminal(new Terminal("environment/geological_bed", new Resource(Kind.ENVIRONMENT,
                "grindless:geological_rock_site"), TerminalKind.ENVIRONMENT, SourceSite.GEOLOGICAL_ROCK_BED,
                true, true, null, 0, "Collector work requires Overworld or Factory and a geological_bed-tagged block "
                + "below. The site is not mined; the recipe separately consumes real water and FU.",
                "ProcessMachineKind.GEOLOGICAL_COLLECTOR.site + ProcessMachineBlockEntity.checkSite + SourceSite.evaluate"));
        builder.terminal(new Terminal("environment/bioculture_sunlight", new Resource(Kind.ENVIRONMENT,
                "grindless:bioculture_sunlit_site"), TerminalKind.ENVIRONMENT, SourceSite.SUNLIT_CULTURE,
                true, true, null, 0, "Bioculture requires Overworld or Factory, open sky above and measured sky "
                + "brightness minus sky darkening >=9. No night, restart or unconditional all-dimension claim.",
                "ProcessMachineKind.BIOCULTIVATOR.site + ProcessMachineBlockEntity.checkSite + SourceSite.evaluate"));
        builder.terminal(new Terminal("environment/intake_runtime", new Resource(Kind.ENVIRONMENT,
                "grindless:unrestricted_intake_site"), TerminalKind.ENVIRONMENT, SourceSite.ATMOSPHERE,
                true, true, null, 0, "The current Atmospheric Intake has SourceSite.NONE: no atmosphere, vacuum or "
                + "dimension predicate is implemented. This describes that runtime limit, not physical realism or Tier acceptance.",
                "ProcessMachineKind.ATMOSPHERIC_INTAKE.site + ProcessMachineBlockEntity.checkSite"));
        builder.terminal(new Terminal("environment/solar_daylight", new Resource(Kind.ENVIRONMENT,
                "grindless:solar_open_sky_daylight"), TerminalKind.ENVIRONMENT, SourceSite.OPEN_SKY_DAYLIGHT,
                true, true, null, 0, "SolarArrayBlockEntity checks a live level, isDay() and canSeeSky(position.above()). "
                + "It has no terrestrial dimension restriction; this conditional source fact does not unlock the panel.",
                "SolarArrayBlockEntity.daylight/work + SolarLogic.generate"));
        builder.terminal(new Terminal("manual/hand_crank_dynamo", new Resource(Kind.ENVIRONMENT,
                "grindless:hand_crank_dynamo_actions"), TerminalKind.MANUAL_LABOUR, null,
                false, true, null, 0, "A real optional Dynamo receives player clicks, stores a bounded charge "
                + "and delivers it at F0. No initial gift, automatic labour or canonical steady supply is implied. "
                + "A finite budget for these clicks is not provided by the separate local CrankHandle work profile.",
                "HandCrankDynamoBlock.use + HandCrankDynamoBlockEntity.crank"));
        builder.route(new Route("source/hand_pump", Authority.BOOTSTRAP_SOURCE,
                "HandPumpBlockEntity.work/isSource + FluidLogic.PUMP_MB_PER_TICK/BUCKET", "HAND_PUMP",
                Map.of(), Map.of(), Map.of(new Resource(Kind.FLUID, FluidLogic.WATER), (long) FluidLogic.BUCKET), Set.of(),
                FluidLogic.BUCKET / FluidLogic.PUMP_MB_PER_TICK, 0,
                Map.of("condition", "adjacent_or_below_WATER_tagged_source_not_drained",
                        "pulse_mb_per_tick", Integer.toString(FluidLogic.PUMP_MB_PER_TICK),
                        "batch", "one_buffer_of_aggregated_unobstructed_pulses"),
                Set.of(), false, true, true));
        builder.route(new Route("calibrate/data_core", Authority.BOOTSTRAP_SOURCE,
                "ResearchLogic + ResearchTerminalBlockEntity.work/WorldlyContainer", "RESEARCH_TERMINAL",
                Map.of(item(ResearchLogic.DATA_CORE), 1L, ENERGY,
                        Math.multiplyExact((long) ResearchLogic.CYCLE_TICKS, ResearchLogic.FU_PER_TICK)),
                Map.of(), Map.of(item(ResearchLogic.CALIBRATED_DATA_CORE), 1L), Set.of(),
                ResearchLogic.CYCLE_TICKS, ResearchLogic.FU_PER_TICK,
                Map.of("physical_item_transformation", "one_core_to_one_calibrated_core",
                        "power", "native_buffer_or_network_not_CrankHandle"), Set.of(), false, true, true));
        builder.route(new Route("power/hand_crank_dynamo", Authority.BOOTSTRAP_SOURCE,
                "HandCrankDynamoBlockEntity.CHARGE_PER_CRANK/crank + HandCrankDynamoBlock.use", "HAND_CRANK_DYNAMO",
                Map.of(new Resource(Kind.ENVIRONMENT, "grindless:hand_crank_dynamo_actions"), 1L), Map.of(),
                Map.of(ENERGY, HandCrankDynamoBlockEntity.CHARGE_PER_CRANK), Set.of(),
                HandCrankDynamoBlockEntity.CHARGE_PER_CRANK / FluxTier.F0.nominal(), 0,
                Map.of("generation_fu_per_tick", Long.toString(FluxTier.F0.nominal()),
                        "condition", "finite_player_clicks_with_a_constructed_physical_dynamo",
                        "charge_headroom", Long.toString(HandCrankDynamoBlockEntity.CHARGE_PER_CRANK),
                        "output", "rated_unobstructed_physical_export"), Set.of(), false, true, false));
        Snapshot snapshot = builder.build();
        eq("adding physical source facts preserves every existing inventory reference", true,
                snapshot.resources().containsAll(inventory.resources()));
        eq("actual calibration is automated but still cannot use local CrankHandle work", false,
                work.applies(snapshot.routes().get("calibrate/data_core")));
        eq("a physical hand-cranked producer is explicitly not automatic", false,
                snapshot.routes().get("power/hand_crank_dynamo").automatable());
        eq("the actual water source is explicitly automatic despite its name", true,
                snapshot.routes().get("source/hand_pump").automatable());
        eq("the source's aggregate water batch uses an exact whole number of actual pulses", 0,
                FluidLogic.BUCKET % FluidLogic.PUMP_MB_PER_TICK);
        eq("the source aggregate preserves every actual pulse rather than changing a recipe quantity",
                (long) FluidLogic.BUCKET,
                snapshot.routes().get("source/hand_pump").durationTicks() * FluidLogic.PUMP_MB_PER_TICK);
        eq("the physical source water matches the process graph's actual fluid identity", true,
                inventory.routes().get("source/geological_aggregate").inputs()
                        .containsKey(new Resource(Kind.FLUID, FluidLogic.WATER)));
        eq("the specific solar site remains distinguishable from the terrestrial culture site", false,
                snapshot.terminals().get("environment/solar_daylight").site()
                        .equals(snapshot.terminals().get("environment/bioculture_sunlight").site()));
        Map<String, RoutePolicy> policies = Map.of(
                "source/hand_pump", new RoutePolicy((Integer) null, false, Set.of("environment/water_source")),
                "source/geological_aggregate", new RoutePolicy((Integer) null, false, Set.of("environment/geological_bed")),
                "source/biomass", new RoutePolicy((Integer) null, false, Set.of("environment/bioculture_sunlight")),
                "air/oxygen", new RoutePolicy((Integer) null, false, Set.of("environment/intake_runtime")),
                "power/solar_array", new RoutePolicy((Integer) null, false, Set.of("environment/solar_daylight")),
                "power/hand_crank_dynamo", new RoutePolicy(0, true, Set.of()));
        return new SourceBinding(snapshot, work, new SustainabilityAnnotations(Map.of(), policies,
                constructions, Map.of(), Map.of(), Map.of()));
    }

    private static Resource registeredItem(ClassTree registry, String fieldName, String constructor,
                                       String kind, Map<Resource, String> declared) throws IOException {
        VariableTree field = field(registry, fieldName);
        if (!(field.getInitializer() instanceof MethodInvocationTree register)
                || !normalized(register.getMethodSelect()).equals("register") || register.getArguments().size() != 2
                || !(register.getArguments().get(0) instanceof LiteralTree literal)
                || !(literal.getValue() instanceof String name)
                || !(register.getArguments().get(1) instanceof LambdaExpressionTree supplier)
                || !supplier.getParameters().isEmpty()
                || !(supplier.getBody() instanceof NewClassTree body)) {
            throw new IOException("Unsupported checked registration binding: " + registry.getSimpleName() + "." + fieldName);
        }
        eq("a checked registration constructs the actual implementing type: " + fieldName, constructor,
                normalized(body.getIdentifier()));
        if (kind != null) {
            eq("the registered block actually selects its declared process/generator kind: " + fieldName,
                    kind, normalized(body.getArguments().get(0)));
        }
        Resource resource = item("grindless:" + name);
        eq("an implementation binding reuses the single verified declaration inventory: " + fieldName,
                true, declared.containsKey(resource));
        return resource;
    }

    private static Map<String, ClassTree> questSources(Path root, Map<String, String> hashes) throws IOException {
        Map<String, ClassTree> sources = new TreeMap<>();
        for (var entry : QUEST_SOURCES.entrySet()) {
            byte[] bytes = Files.readAllBytes(root.resolve(entry.getValue()));
            hashes.put("mechanics_source:" + entry.getValue().toString().replace('\\', '/'),
                    AuthorityAdapters.sha256(bytes));
            sources.put(entry.getKey(), parse(entry.getKey(), new String(bytes, StandardCharsets.UTF_8)));
        }
        return sources;
    }

    // Exact source profiles bind this call chain; unexamined Java forms are not equivalent proof.
    private static boolean questBookAccess(Map<String, ClassTree> sources) throws IOException {
        if (!sources.keySet().equals(QUEST_SOURCES.keySet())) {
            throw new IOException("Incomplete quest claim authorities; claim binding not evaluated.");
        }
        for (ClassTree type : sources.values()) {
            if (!questClassShape(type)) {
                return false;
            }
        }
        ClassTree progress = sources.get("QuestProgress");
        VariableTree claimed = field(progress, "claimed");
        return claimed.getModifiers().getFlags().equals(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                && normalized(claimed.getType()).equals("Map<UUID,Set<String>>")
                && normalized(claimed.getInitializer()).equals("newHashMap<>()")
                && questCatalogueTasks(sources.get("QuestCatalogue"))
                && questMethod(sources, "QuestCatalogue", """
                private QuestCatalogue() {
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static List<Task> tasks() {
                    return TASKS;
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static Task byId(String id) {
                    for (Task task : TASKS) {
                        if (task.id().equals(id)) {
                            return task;
                        }
                    }
                    return null;
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static Chapter chapter(Task task) {
                    return switch (task.id()) {
                        case "thermal", "pylon", "scanner", "terrestrial", "pulverizer", "furnace", "press",
                                "assembler", "matrix_line", "glass", "logistics", "kiln", "factory_world",
                                "industrial_matrix" -> Chapter.T1;
                        default -> Chapter.T0;
                    };
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static boolean unlocked(Chapter chapter, Set<String> claimed) {
                    return chapter == Chapter.T0 || claimed.contains(T0_COMPLETION);
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static List<String> requirements(Task task) {
                    if (chapter(task) != Chapter.T1 || task.requires().contains(T0_COMPLETION)) {
                        return task.requires();
                    }
                    List<String> required = new ArrayList<>(task.requires());
                    required.add(T0_COMPLETION);
                    return List.copyOf(required);
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static List<String> lines(Chapter chapter) {
                    return TASKS.stream().filter(task -> chapter(task) == chapter).map(Task::line).distinct().toList();
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                public static List<String> lines() {
                    List<String> lines = new ArrayList<>();
                    for (Task task : TASKS) {
                        if (!lines.contains(task.line())) {
                            lines.add(task.line());
                        }
                    }
                    return List.copyOf(lines);
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                private static Task task(String id, String line, Evidence evidence, String subject, int count,
                                         List<String> requires, Reward reward) {
                    return new Task(id, line, evidence, subject, count, null, List.copyOf(requires), reward);
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                private static Task lesson(String id, String line, Evidence evidence, String subject, int count,
                                           List<String> requires) {
                    return new Task(id, line, evidence, subject, count, null, List.copyOf(requires), List.of(), Type.LESSON);
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                private static Task matrixTask(String id, String line, String subject, FluxTier rating, int count,
                                               List<String> requires, Reward reward) {
                    return new Task(id, line, Evidence.MATRIX, subject, count, rating, List.copyOf(requires), reward);
                }
                """)
                && questMethod(sources, "QuestCatalogue", """
                private static Reward reward(String itemId, int count) {
                    return new Reward(itemId, count);
                }
                """)
                && questMethod(sources, "QuestBookItem", """
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (player instanceof ServerPlayer server) {
                        if (!QuestBackendPlatform.open(server)) {
                            MenuRegistry.openMenu(server, new SimpleMenuProvider(
                                    (id, inventory, opener) -> new QuestBookMenu(id, inventory),
                                    Component.translatable("gui.grindless.quest.title")));
                        }
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
                """)
                && questMethod(sources, "QuestBookMenu", """
                @Override
                public boolean stillValid(Player player) {
                    return QuestClaimService.holdingBook(player)
                            || player.getMainHandItem().is(ModItems.FIELD_GUIDE.get())
                            || player.getOffhandItem().is(ModItems.FIELD_GUIDE.get());
                }
                """)
                && questMethod(sources, "QuestBookMenu", """
                public QuestBookMenu(int id, Inventory inventory) {
                    super(ModMenus.QUEST_BOOK.get(), id);
                    this.player = inventory.player;
                    this.state = new SimpleContainerData(QuestCatalogue.tasks().size() * 2 + 1);
                    for (int task = 0; task < QuestCatalogue.tasks().size(); task++) {
                        state.set(task, QuestLogic.Status.UNMET.ordinal());
                    }
                    addDataSlots(state);
                }
                """)
                && questMethod(sources, "QuestBookMenu", """
                public boolean chapterUnlocked(QuestCatalogue.Chapter chapter) {
                    return chapter == QuestCatalogue.Chapter.T0 || state.get(QuestCatalogue.tasks().size() * 2) == 1;
                }
                """)
                && questMethod(sources, "QuestBookMenu", """
                @Override
                public boolean clickMenuButton(Player player, int id) {
                    if (!(player instanceof ServerPlayer server)) {
                        return false;
                    }
                    List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
                    if (id < 0 || id >= tasks.size()) {
                        return false;
                    }
                    QuestClaimService.Result result = QuestClaimService.claim(server, tasks.get(id).id());
                    refresh();
                    return result == QuestClaimService.Result.CLAIMED;
                }
                """)
                && questMethod(sources, "QuestBookMenu", """
                private void refresh() {
                    if (!(player instanceof ServerPlayer server)) {
                        return;
                    }
                    QuestProgress progress = QuestProgress.get(server);
                    Set<String> claimed = progress.claimed(server.getUUID());
                    List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
                    state.set(tasks.size() * 2, QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, claimed) ? 1 : 0);
                    for (int i = 0; i < tasks.size(); i++) {
                        QuestCatalogue.Task task = tasks.get(i);
                        int held = held(server, task);
                        state.set(i, QuestLogic.consider(task, claimed, QuestEvidence.met(task, held,
                                server.level().dimension().location().toString())).ordinal());
                        state.set(tasks.size() + i, held);
                    }
                }
                """)
                && questMethod(sources, "QuestBookMenu", """
                private static int held(ServerPlayer player, QuestCatalogue.Task task) {
                    return QuestInventoryEvidence.count(player.getInventory(), task);
                }
                """)
                && questMethod(sources, "QuestClaimService", """
                public static boolean holdingBook(Player player) {
                    return player.getMainHandItem().is(ModItems.QUEST_BOOK.get())
                            || player.getOffhandItem().is(ModItems.QUEST_BOOK.get());
                }
                """)
                && questMethod(sources, "QuestClaimService", """
                public static Result claim(ServerPlayer player, String taskId) {
                    if (!player.server.isSameThread()) {
                        throw new IllegalStateException("Quest claims must execute on the server thread");
                    }
                    Result result = claim(QuestProgress.get(player), player.getUUID(), taskId, holdingBook(player),
                            task -> QuestEvidence.met(task, QuestInventoryEvidence.count(player.getInventory(), task),
                                    player.level().dimension().location().toString()),
                            QuestClaimService::rewardStack,
                            rewards -> QuestRewardDelivery.prepare(player.getInventory(), rewards));
                    if (result == Result.INVALID_REWARD || result == Result.UNKNOWN_TASK) {
                        Grindless.LOG.error("Rejected quest claim {} from {}: {}", taskId, player.getUUID(), result);
                    }
                    if (result == Result.CLAIMED) {
                        DevTelemetry.milestone(player.level(), taskId);
                        player.inventoryMenu.broadcastChanges();
                        player.displayClientMessage(Component.translatable("chat.grindless.quest.claimed",
                                Component.translatable("gui.grindless.quest.task." + taskId)), false);
                    } else {
                        player.displayClientMessage(Component.translatable("chat.grindless.quest.rejected."
                                + result.name().toLowerCase(java.util.Locale.ROOT)), false);
                    }
                    return result;
                }
                """)
                && questMethod(sources, "QuestClaimService", """
                static Result claim(QuestProgress progress, UUID player, String taskId, boolean holdingBook,
                                    Predicate<QuestCatalogue.Task> evidence, Function<QuestCatalogue.Reward, ItemStack> resolve,
                                    Function<List<ItemStack>, Optional<Runnable>> prepare) {
                    QuestCatalogue.Task task = QuestCatalogue.byId(taskId);
                    if (task == null || player == null) {
                        return Result.UNKNOWN_TASK;
                    }
                    if (progress.usesNativeClaims(player)) {
                        return Result.NATIVE_BACKEND;
                    }
                    Set<String> claimed = progress.claimed(player);
                    if (claimed.contains(taskId)) {
                        return Result.ALREADY_CLAIMED;
                    }
                    if (!holdingBook) {
                        return Result.BOOK_REQUIRED;
                    }
                    QuestLogic.Status status = QuestLogic.consider(task, claimed, evidence.test(task));
                    if (status == QuestLogic.Status.LOCKED) {
                        return Result.LOCKED;
                    }
                    if (status != QuestLogic.Status.CLAIMABLE) {
                        return Result.UNMET;
                    }
                    if (task.type() == QuestCatalogue.Type.LESSON) {
                        if (!task.rewards().isEmpty()) {
                            return Result.INVALID_REWARD;
                        }
                        return progress.claim(player, taskId) ? Result.CLAIMED : Result.ALREADY_CLAIMED;
                    }
                    if (task.rewards().isEmpty()) {
                        return Result.INVALID_REWARD;
                    }
                    List<ItemStack> rewards = new ArrayList<>();
                    for (QuestCatalogue.Reward declared : task.rewards()) {
                        ItemStack reward = resolve.apply(declared);
                        if (reward == null || reward.isEmpty() || reward.getCount() != declared.count()
                                || reward.getCount() > reward.getMaxStackSize()) {
                            return Result.INVALID_REWARD;
                        }
                        rewards.add(reward.copy());
                    }
                    Optional<Runnable> delivery = prepare.apply(List.copyOf(rewards));
                    if (delivery.isEmpty()) {
                        return Result.INVENTORY_FULL;
                    }
                    if (!progress.claim(player, taskId)) {
                        return Result.ALREADY_CLAIMED;
                    }
                    delivery.get().run();
                    return Result.CLAIMED;
                }
                """)
                && questMethod(sources, "QuestClaimService", """
                private static ItemStack rewardStack(QuestCatalogue.Reward reward) {
                    ResourceLocation id = ResourceLocation.tryParse(reward.itemId());
                    if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || reward.count() < 1) {
                        return ItemStack.EMPTY;
                    }
                    var item = BuiltInRegistries.ITEM.get(id);
                    return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, reward.count());
                }
                """)
                && questMethod(sources, "QuestRewardDelivery", """
                private QuestRewardDelivery() {
                }
                """)
                && questMethod(sources, "QuestRewardDelivery", """
                static Optional<Runnable> prepare(Inventory inventory, List<ItemStack> rewards) {
                    ItemStack[] contents = new ItemStack[inventory.items.size()];
                    for (int slot = 0; slot < contents.length; slot++) {
                        contents[slot] = inventory.getItem(slot).copy();
                    }
                    SimpleContainer staged = new SimpleContainer(contents);
                    for (ItemStack reward : rewards) {
                        if (!staged.addItem(reward.copy()).isEmpty()) {
                            return Optional.empty();
                        }
                    }
                    return Optional.of(() -> {
                        for (int slot = 0; slot < staged.getContainerSize(); slot++) {
                            if (!ItemStack.matches(inventory.getItem(slot), staged.getItem(slot))) {
                                inventory.setItem(slot, staged.getItem(slot));
                            }
                        }
                        inventory.setChanged();
                    });
                }
                """)
                && questMethod(sources, "QuestBookAccess", """
                private QuestBookAccess() {
                }
                """)
                && questMethod(sources, "QuestBookAccess", """
                public static Result ensure(ServerPlayer player) {
                    if (!player.server.isSameThread()) {
                        throw new IllegalStateException("Quest book recovery must execute on the server thread");
                    }
                    Result result = ensure(QuestProgress.get(player), player.getUUID(), player.getInventory(),
                            ModItems.QUEST_BOOK.get());
                    if (result == Result.DELIVERED) {
                        player.inventoryMenu.broadcastChanges();
                        player.displayClientMessage(Component.translatable("chat.grindless.quest.book.restored"), false);
                    }
                    return result;
                }
                """)
                && questMethod(sources, "QuestBookAccess", """
                static Result ensure(QuestProgress progress, UUID player, Inventory inventory, Item book) {
                    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                        if (inventory.getItem(slot).is(book)) {
                            return Result.PRESENT;
                        }
                    }
                    Optional<Runnable> delivery = QuestRewardDelivery.prepare(inventory, List.of(new ItemStack(book)));
                    if (delivery.isEmpty()) {
                        return Result.INVENTORY_FULL;
                    }
                    delivery.get().run();
                    return Result.DELIVERED;
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                private QuestBookAccessEvents() {
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                @SubscribeEvent
                public static void login(PlayerEvent.PlayerLoggedInEvent event) {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        recover(player, true);
                    }
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                @SubscribeEvent
                public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        recover(player, true);
                    }
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                @SubscribeEvent
                public static void tick(TickEvent.PlayerTickEvent event) {
                    if (event.phase == TickEvent.Phase.END && event.player.tickCount % 20 == 0
                            && event.player instanceof ServerPlayer player && WAITING_FOR_SPACE.contains(player.getUUID())) {
                        recover(player, false);
                    }
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                @SubscribeEvent
                public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
                    WAITING_FOR_SPACE.remove(event.getEntity().getUUID());
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                @SubscribeEvent
                public static void stopped(ServerStoppedEvent event) {
                    WAITING_FOR_SPACE.clear();
                }
                """)
                && questMethod(sources, "QuestBookAccessEvents", """
                private static void recover(ServerPlayer player, boolean notifyFull) {
                    QuestBookAccess.Result result = QuestBookAccess.ensure(player);
                    if (result == QuestBookAccess.Result.INVENTORY_FULL) {
                        WAITING_FOR_SPACE.add(player.getUUID());
                        if (notifyFull) {
                            player.displayClientMessage(Component.translatable("chat.grindless.quest.book.space"), false);
                        }
                    } else {
                        WAITING_FOR_SPACE.remove(player.getUUID());
                    }
                }
                """)
                && questMethod(sources, "QuestLogic", """
                public static Status consider(QuestCatalogue.Task task, Set<String> claimed, boolean evidenceMet) {
                    if (!QuestCatalogue.unlocked(QuestCatalogue.chapter(task), claimed)) {
                        return Status.LOCKED;
                    }
                    if (claimed.contains(task.id())) {
                        return Status.CLAIMED;
                    }
                    for (String required : task.requires()) {
                        if (!claimed.contains(required)) {
                            return Status.LOCKED;
                        }
                    }
                    return task.evidence() == QuestCatalogue.Evidence.FREE || evidenceMet
                            ? Status.CLAIMABLE : Status.UNMET;
                }
                """)
                && questMethod(sources, "QuestEvidence", """
                public static boolean met(QuestCatalogue.Task task, int held, String dimension) {
                    return switch (task.evidence()) {
                        case ITEM, MATRIX -> held >= task.count();
                        case DIMENSION -> task.subject().equals(dimension);
                        case FREE -> true;
                    };
                }
                """)
                && questMethod(sources, "QuestInventoryEvidence", """
                public static int count(Container inventory, QuestCatalogue.Task task) {
                    int count = 0;
                    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                        ItemStack stack = inventory.getItem(slot);
                        if (matches(task, stack)) {
                            count += stack.getCount();
                        }
                    }
                    return count;
                }
                """)
                && questMethod(sources, "QuestInventoryEvidence", """
                private static boolean matches(QuestCatalogue.Task task, ItemStack stack) {
                    if (stack.isEmpty()
                            || !task.subject().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                        return false;
                    }
                    if (task.evidence() != QuestCatalogue.Evidence.MATRIX) {
                        return true;
                    }
                    return stack.getItem() instanceof ControlMatrixItem matrix
                            && matrix.spec(stack).map(value -> value.rating() == task.matrixRating()).orElse(false);
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public static QuestProgress get(ServerPlayer player) {
                    Path file = player.server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(KEY + ".dat");
                    return player.server.overworld().getDataStorage()
                            .computeIfAbsent(QuestProgress::load, () -> createIfMissing(file), KEY);
                }
                """)
                && questMethod(sources, "QuestProgress", """
                static QuestProgress createIfMissing(Path file) {
                    try {
                        Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    } catch (NoSuchFileException missing) {
                        return new QuestProgress();
                    } catch (IOException | SecurityException exception) {
                        throw new IllegalStateException("Cannot inspect quest progress; refusing to recreate its reward ledger",
                                exception);
                    }
                    throw new IllegalStateException("Existing quest progress could not be loaded; refusing to recreate its reward ledger");
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public Set<String> claimed(UUID player) {
                    Set<String> result = new HashSet<>(legacyClaims(player));
                    result.addAll(nativeCompleted.getOrDefault(player, Set.of()));
                    return Set.copyOf(result);
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public Set<String> legacyClaims(UUID player) {
                    return Set.copyOf(claimed.getOrDefault(player, Set.of()));
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public void updateNativeCompleted(UUID player, Set<String> completed) {
                    if (!usesNativeClaims(player) || completed.stream().anyMatch(id -> QuestCatalogue.byId(id) == null)) {
                        throw new IllegalArgumentException("Native completion requires migrated authority and known quest ids");
                    }
                    nativeCompleted.put(player, Set.copyOf(completed));
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public boolean usesNativeClaims(UUID player) {
                    return nativeClaims.contains(player);
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public void useNativeClaims(UUID player) {
                    if (player == null) {
                        throw new IllegalArgumentException("Native claim authority requires a player");
                    }
                    if (nativeClaims.add(player)) {
                        setDirty();
                    }
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public boolean claim(UUID player, String task) {
                    if (player == null || usesNativeClaims(player) || QuestCatalogue.byId(task) == null) {
                        return false;
                    }
                    Set<String> tasks = claimed.computeIfAbsent(player, id -> new HashSet<>());
                    if (!tasks.add(task)) {
                        return false;
                    }
                    setDirty();
                    return true;
                }
                """)
                && questMethod(sources, "QuestProgress", """
                @Override
                public CompoundTag save(CompoundTag tag) {
                    ListTag players = new ListTag();
                    for (Map.Entry<UUID, Set<String>> entry : claimed.entrySet()) {
                        CompoundTag one = new CompoundTag();
                        one.putString(TAG_PLAYER, entry.getKey().toString());
                        ListTag tasks = new ListTag();
                        for (String id : entry.getValue()) {
                            tasks.add(StringTag.valueOf(id));
                        }
                        one.put(TAG_TASKS, tasks);
                        players.add(one);
                    }
                    tag.put(TAG_PLAYERS, players);
                    ListTag nativePlayers = new ListTag();
                    for (UUID player : nativeClaims) {
                        nativePlayers.add(StringTag.valueOf(player.toString()));
                    }
                    tag.put(TAG_NATIVE_CLAIMS, nativePlayers);
                    return tag;
                }
                """)
                && questMethod(sources, "QuestProgress", """
                public static QuestProgress load(CompoundTag tag) {
                    QuestProgress progress = new QuestProgress();
                    ListTag players = tag.getList(TAG_PLAYERS, Tag.TAG_COMPOUND);
                    for (int i = 0; i < players.size(); i++) {
                        CompoundTag one = players.getCompound(i);
                        UUID player;
                        try {
                            player = UUID.fromString(one.getString(TAG_PLAYER));
                        } catch (IllegalArgumentException ex) {
                            continue;
                        }
                        Set<String> tasks = new HashSet<>();
                        ListTag list = one.getList(TAG_TASKS, Tag.TAG_STRING);
                        for (int t = 0; t < list.size(); t++) {
                            String id = list.getString(t);
                            if (QuestCatalogue.byId(id) != null) {
                                tasks.add(id);
                            }
                        }
                        if (!tasks.isEmpty()) {
                            progress.claimed.put(player, tasks);
                        }
                    }
                    if (tag.contains(TAG_NATIVE_CLAIMS)) {
                        if (!(tag.get(TAG_NATIVE_CLAIMS) instanceof ListTag nativePlayers)
                                || !nativePlayers.isEmpty() && nativePlayers.getElementType() != Tag.TAG_STRING) {
                            throw new IllegalArgumentException("Invalid native quest claim authority list in saved progress");
                        }
                        for (int i = 0; i < nativePlayers.size(); i++) {
                            try {
                                progress.nativeClaims.add(UUID.fromString(nativePlayers.getString(i)));
                            } catch (IllegalArgumentException exception) {
                                throw new IllegalArgumentException("Invalid native quest claim authority in saved progress", exception);
                            }
                        }
                    }
                    return progress;
                }
                """)
                && questMethod(sources, "QuestBackendPlatform", """
                @ExpectPlatform
                public static boolean open(ServerPlayer player) {
                    throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
                }
                """)
                && questMethod(sources, "QuestBackendPlatformImpl", """
                public static boolean open(ServerPlayer player) {
                    if (ModList.get().isLoaded("ftbquests")) {
                        FtbQuestIntegration.open(player);
                        return true;
                    }
                    return ModList.get().isLoaded("heracles") && HeraclesQuestIntegration.open(player);
                }
                """)
                && questMethod(sources, "GrindlessForge", """
                public GrindlessForge() {
                    IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
                    EventBuses.registerModEventBus(Grindless.MOD_ID, bus);
                    Grindless.init();
                    MachineInspectionOverlay.init(bus);
                    if (ModList.get().isLoaded("ftbquests")) {
                        FtbQuestIntegration.init(bus);
                    } else if (ModList.get().isLoaded("heracles")) {
                        HeraclesQuestIntegration.init(bus);
                    }
                    MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent event) ->
                            MaterialRegistry.rebuild(RegistryTagView.ofItems()));
                }
                """)
                && questMethod(sources, "GrindlessForgeClient", """
                private GrindlessForgeClient() {
                }
                """)
                && questMethod(sources, "GrindlessForgeClient", """
                @SubscribeEvent
                public static void onClientSetup(FMLClientSetupEvent event) {
                    GrindlessClient.init();
                    if (!ModList.get().isLoaded("ftbquests") && ModList.get().isLoaded("heracles")) {
                        event.enqueueWork(HeraclesQuestClient::init);
                    }
                }
                """)
                && questMethod(sources, "FtbQuestIntegration", """
                private FtbQuestIntegration() {
                }
                """)
                && questMethod(sources, "FtbQuestIntegration", """
                public static void init(IEventBus modBus) {
                    modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
                        FTBQuestsAPI.api().registerFilterAdapter(new FtbMatrixFilter());
                        try {
                            FtbQuestFiles.ensure(FMLPaths.CONFIGDIR.get());
                        } catch (IOException exception) {
                            throw new IllegalStateException("Cannot install native Grindless FTB chapters; existing files were preserved",
                                    exception);
                        }
                    }));
                    LifecycleEvent.SERVER_STARTED.register(FtbQuestIntegration::started);
                    LifecycleEvent.SERVER_STOPPING.register(server -> ready = false);
                    TeamEvent.PLAYER_LOGGED_IN.register(event -> importPlayer(event.getPlayer()));
                    TeamEvent.PLAYER_CHANGED.register(event -> importPlayer(event.getPlayer()));
                    ObjectCompletedEvent.QUEST.register(event -> {
                        for (ServerPlayer player : event.getOnlineMembers()) {
                            QuestProgress legacy = QuestProgress.get(player);
                            if (legacy.usesNativeClaims(player.getUUID())) {
                                FtbQuestNative.refresh(legacy, player.getUUID(), event.getData());
                            }
                        }
                    });
                    MinecraftForge.EVENT_BUS.addListener(FtbQuestIntegration::tick);
                    MinecraftForge.EVENT_BUS.addListener(FtbQuestChecks::registerCommands);
                }
                """)
                && questMethod(sources, "FtbQuestIntegration", """
                private static void started(MinecraftServer server) {
                    ServerQuestFile file = ServerQuestFile.INSTANCE;
                    if (file == null || file.server != server) {
                        throw new IllegalStateException("FTB Quests did not initialize its native server quest file");
                    }
                    FtbQuestNative.verify(file);
                    ready = true;
                    Grindless.LOG.info("Native FTB Quests ready: 46 stable Grindless quests; legacy reward claims are import-only");
                }
                """)
                && questMethod(sources, "FtbQuestIntegration", """
                public static void open(ServerPlayer player) {
                    if (!importPlayer(player)) {
                        return;
                    }
                    player.closeContainer();
                    new OpenQuestBookMessage(FtbQuestDefinitions.id("chapter", "T0")).sendTo(player);
                }
                """)
                && questMethod(sources, "FtbQuestIntegration", """
                private static boolean importPlayer(ServerPlayer player) {
                    if (!player.server.isSameThread()) {
                        throw new IllegalStateException("Native quest import must execute on the server thread");
                    }
                    try {
                        if (!ready) {
                            throw new IllegalStateException("Native Grindless FTB definitions have not been verified");
                        }
                        TeamData data = ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
                        FtbQuestNative.migrate(QuestProgress.get(player), player.getUUID(), data);
                        data.saveIfChanged();
                        return true;
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        Grindless.LOG.error("Refused native quest admission before legacy reward import completed", exception);
                        player.connection.disconnect(Component.translatable("chat.grindless.quest.backend_unavailable"));
                        return false;
                    }
                }
                """)
                && questMethod(sources, "FtbQuestIntegration", """
                private static void tick(TickEvent.PlayerTickEvent event) {
                    if (event.phase == TickEvent.Phase.END && event.player.tickCount % 20 == 0
                            && event.player instanceof ServerPlayer player) {
                        QuestProgress legacy = QuestProgress.get(player);
                        if (ready && legacy.usesNativeClaims(player.getUUID())) {
                            TeamData data = ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
                            if (data == null) {
                                throw new IllegalStateException("Native quest player has no framework team progress");
                            }
                            FtbQuestNative.refresh(legacy, player.getUUID(), data);
                        }
                    }
                }
                """)
                && questMethod(sources, "FtbQuestNative", """
                private FtbQuestNative() {
                }
                """)
                && questMethod(sources, "FtbQuestNative", """
                public static void verify(BaseQuestFile file) {
                    for (CompoundTag document : FtbQuestDefinitions.chapters().values()) {
                        Chapter chapter = file.getChapter(parseId(document));
                        if (chapter == null) {
                            throw new IllegalStateException("FTB Quests did not load the Grindless chapter " + document.getString("id"));
                        }
                        Chapter expectedChapter = new Chapter(chapter.id, file, chapter.getGroup());
                        expectedChapter.readData(document);
                        sameData(expectedChapter, chapter);
                        ListTag quests = document.getList("quests", Tag.TAG_COMPOUND);
                        if (chapter.getQuests().size() != quests.size()) {
                            throw new IllegalStateException("Unexpected native Grindless chapter contents: " + chapter.id);
                        }
                        for (int index = 0; index < quests.size(); index++) {
                            CompoundTag definition = quests.getCompound(index);
                            Quest quest = file.getQuest(parseId(definition));
                            if (quest == null || quest.getChapter() != chapter) {
                                throw new IllegalStateException("Missing or displaced native Grindless quest: " + definition.getString("id"));
                            }
                            Quest expected = new Quest(quest.id, chapter);
                            expected.readData(definition);
                            sameData(expected, quest);
                            ListTag tasks = definition.getList("tasks", Tag.TAG_COMPOUND);
                            if (quest.getTasks().size() != 1 || tasks.size() != 1) {
                                throw new IllegalStateException("Unexpected native Grindless task count: " + quest.id);
                            }
                            CompoundTag taskDefinition = tasks.getCompound(0);
                            Task task = file.getTask(parseId(taskDefinition));
                            Task expectedTask = switch (taskDefinition.getString("type")) {
                                case "ftbquests:item" -> new ItemTask(parseId(taskDefinition), expected);
                                case "ftbquests:checkmark" -> new CheckmarkTask(parseId(taskDefinition), expected);
                                case "ftbquests:dimension" -> new DimensionTask(parseId(taskDefinition), expected);
                                default -> throw new IllegalStateException("Unsupported generated native task");
                            };
                            expectedTask.readData(taskDefinition);
                            if (task == null || task.getQuest() != quest || task.getClass() != expectedTask.getClass()) {
                                throw new IllegalStateException("Missing or replaced native Grindless task: " + taskDefinition.getString("id"));
                            }
                            sameData(expectedTask, task);
                            ListTag rewards = definition.getList("rewards", Tag.TAG_COMPOUND);
                            if (quest.getRewards().size() != rewards.size()) {
                                throw new IllegalStateException("Unexpected native Grindless reward count: " + quest.id);
                            }
                            for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
                                CompoundTag rewardDefinition = rewards.getCompound(rewardIndex);
                                Reward reward = file.getReward(parseId(rewardDefinition));
                                ItemReward expectedReward = new ItemReward(parseId(rewardDefinition), expected);
                                expectedReward.readData(rewardDefinition);
                                if (reward == null || reward.getQuest() != quest || reward.getClass() != ItemReward.class) {
                                    throw new IllegalStateException("Missing or replaced native Grindless item reward");
                                }
                                sameData(expectedReward, reward);
                            }
                        }
                    }
                }
                """)
                && questMethod(sources, "FtbQuestNative", """
                public static void migrate(QuestProgress legacy, UUID player, TeamData nativeProgress) {
                    if (player == null || nativeProgress == null) {
                        throw new IllegalArgumentException("Native quest import requires a player and team progress");
                    }
                    Set<String> paid = legacy.legacyClaims(player);
                    legacy.useNativeClaims(player);
                    long now = System.currentTimeMillis();
                    for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
                        if (!paid.contains(task.id())) {
                            continue;
                        }
                        for (long rewardId : FtbQuestDefinitions.rewardIds(task)) {
                            Reward reward = nativeProgress.getFile().getReward(rewardId);
                            if (reward == null || reward.isTeamReward()
                                    || !nativeProgress.isRewardClaimed(player, reward)
                                    && !nativeProgress.markRewardAsClaimed(player, reward, now)) {
                                throw new IllegalStateException("Cannot import paid native reward receipt: " + rewardId);
                            }
                        }
                    }
                    for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
                        if (!paid.contains(task.id()) || !QuestCatalogue.unlocked(QuestCatalogue.chapter(task), paid)) {
                            continue;
                        }
                        Task nativeTask = nativeProgress.getFile().getTask(FtbQuestDefinitions.taskId(task.id()));
                        Quest nativeQuest = nativeProgress.getFile().getQuest(FtbQuestDefinitions.questId(task.id()));
                        if (nativeTask == null || nativeQuest == null || nativeProgress.isLocked()) {
                            throw new IllegalStateException("Cannot import native quest completion: " + task.id());
                        }
                        if (nativeProgress.getProgress(nativeTask) < nativeTask.getMaxProgress()) {
                            nativeProgress.setProgress(nativeTask, nativeTask.getMaxProgress());
                        }
                        if (!nativeProgress.isCompleted(nativeTask)) {
                            nativeProgress.setCompleted(nativeTask.id, new Date(now));
                        }
                        if (!nativeProgress.isCompleted(nativeQuest)) {
                            nativeProgress.setCompleted(nativeQuest.id, new Date(now));
                        }
                    }
                    refresh(legacy, player, nativeProgress);
                }
                """)
                && questMethod(sources, "FtbQuestNative", """
                public static void refresh(QuestProgress legacy, UUID player, TeamData nativeProgress) {
                    Set<String> completed = new LinkedHashSet<>();
                    for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
                        Quest quest = nativeProgress.getFile().getQuest(FtbQuestDefinitions.questId(task.id()));
                        if (quest == null) {
                            throw new IllegalStateException("Missing native quest during compatibility observation: " + task.id());
                        }
                        if (nativeProgress.isCompleted(quest)) {
                            completed.add(task.id());
                        }
                    }
                    legacy.updateNativeCompleted(player, completed);
                }
                """)
                && questMethod(sources, "FtbQuestNative", """
                private static void sameData(QuestObjectBase expected, QuestObjectBase actual) {
                    CompoundTag expectedData = new CompoundTag();
                    CompoundTag actualData = new CompoundTag();
                    expected.writeData(expectedData);
                    actual.writeData(actualData);
                    if (!expectedData.equals(actualData)) {
                        throw new IllegalStateException("Modified or colliding native Grindless definition: " + expected.getCodeString());
                    }
                }
                """)
                && questMethod(sources, "FtbQuestNative", """
                private static long parseId(CompoundTag tag) {
                    return Long.parseLong(tag.getString("id"), 16);
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                @Override
                public String getName() {
                    return "Grindless";
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                @Override
                public boolean isFilterStack(ItemStack stack) {
                    return stack.hasTag() && stack.getTag().contains(BINDING);
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                @Override
                public boolean doesItemMatch(ItemStack filterStack, ItemStack toCheck) {
                    return getMatcher(filterStack).test(toCheck);
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                @Override
                public Matcher getMatcher(ItemStack filterStack) {
                    if (!isFilterStack(filterStack) || !filterStack.getTag().contains(BINDING, Tag.TAG_STRING)) {
                        return NO_MATCH;
                    }
                    QuestCatalogue.Task declared = QuestCatalogue.byId(filterStack.getTag().getString(BINDING));
                    if (declared == null || declared.evidence() != QuestCatalogue.Evidence.MATRIX
                            || !sameSubject(filterStack, declared)
                            || ControlMatrixItem.spec(filterStack)
                            .filter(spec -> spec.rating() == declared.matrixRating()).isEmpty()) {
                        return NO_MATCH;
                    }
                    return candidate -> sameSubject(candidate, declared)
                            && ControlMatrixItem.spec(candidate)
                            .filter(spec -> spec.rating() == declared.matrixRating()).isPresent();
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                @Override
                public boolean hasItemTagFilter() {
                    return false;
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                @Override
                public ItemStack makeTagFilterStack(TagKey<Item> tag) {
                    throw new UnsupportedOperationException("Matrix quest filters cannot be converted to item tags");
                }
                """)
                && questMethod(sources, "FtbMatrixFilter", """
                private static boolean sameSubject(ItemStack stack, QuestCatalogue.Task task) {
                    return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(task.subject());
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                private FtbQuestDefinitions() {
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                public static long id(String kind, String key) {
                    try {
                        byte[] hash = MessageDigest.getInstance("SHA-256")
                                .digest(("grindless/ftb/" + kind + "/" + key).getBytes(StandardCharsets.UTF_8));
                        long result = ByteBuffer.wrap(hash).getLong() & Long.MAX_VALUE;
                        if (result <= 1) {
                            throw new IllegalStateException("Reserved native quest id for " + kind + "/" + key);
                        }
                        return result;
                    } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException("Java must provide SHA-256", exception);
                    }
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                public static long questId(String catalogueId) {
                    if (QuestCatalogue.byId(catalogueId) == null) {
                        throw new IllegalArgumentException("Unknown quest catalogue id: " + catalogueId);
                    }
                    return id("quest", catalogueId);
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                public static long taskId(String catalogueId) {
                    questId(catalogueId);
                    return id("task", catalogueId);
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                public static List<Long> rewardIds(QuestCatalogue.Task task) {
                    return java.util.stream.IntStream.range(0, task.rewards().size())
                            .mapToObj(index -> id("reward", task.id() + "/" + index)).toList();
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                public static String code(long id) {
                    return String.format(Locale.ROOT, "%016X", id);
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                public static Map<String, CompoundTag> chapters() {
                    Map<String, CompoundTag> chapters = new LinkedHashMap<>();
                    Set<Long> ids = new HashSet<>();
                    Map<String, Integer> depths = new LinkedHashMap<>();
                    for (QuestProjection.Entry entry : QuestProjection.entries()) {
                        depths.put(entry.task().id(), entry.task().requires().stream()
                                .filter(parent -> QuestCatalogue.chapter(QuestCatalogue.byId(parent))
                                        == QuestCatalogue.chapter(entry.task()))
                                .mapToInt(parent -> depths.get(parent) + 1).max().orElse(0));
                    }
                    for (QuestCatalogue.Chapter chapter : QuestCatalogue.Chapter.values()) {
                        String name = "grindless_" + chapter.name().toLowerCase(Locale.ROOT);
                        CompoundTag document = new CompoundTag();
                        document.putString("id", unique(ids, id("chapter", chapter.name())));
                        document.putString("filename", name);
                        document.putString("title", json(Component.literal("Grindless " + chapter.name())));
                        document.putInt("order_index", chapter.ordinal());
                        document.putString("progression_mode", "linear");
                        document.putBoolean("consume_items", false);
                        document.putBoolean("default_repeatable_quest", false);
                        ListTag quests = new ListTag();
                        int lineTop = 0;
                        for (String line : QuestCatalogue.lines(chapter)) {
                            Map<Integer, Integer> rows = new LinkedHashMap<>();
                            for (QuestProjection.Entry entry : QuestProjection.entries()) {
                                QuestCatalogue.Task task = entry.task();
                                if (QuestCatalogue.chapter(task) != chapter || !task.line().equals(line)) {
                                    continue;
                                }
                                int depth = depths.get(task.id());
                                int row = rows.getOrDefault(depth, 0);
                                rows.put(depth, row + 1);
                                quests.add(quest(entry, depth * 2.5, lineTop + row * 1.5, ids));
                            }
                            lineTop += rows.values().stream().mapToInt(Integer::intValue).max().orElse(1) * 2 + 1;
                        }
                        document.put("quests", quests);
                        chapters.put(name + ".snbt", document);
                    }
                    return Collections.unmodifiableMap(chapters);
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                private static CompoundTag quest(QuestProjection.Entry entry, double x, double y, Set<Long> ids) {
                    QuestCatalogue.Task task = entry.task();
                    CompoundTag quest = new CompoundTag();
                    quest.putString("id", unique(ids, questId(task.id())));
                    quest.putString("title", json(Component.translatable("gui.grindless.quest.task." + task.id())));
                    quest.putDouble("x", x);
                    quest.putDouble("y", y);
                    quest.put("tags", strings(List.of(entry.id())));
                    quest.put("dependencies", strings(QuestCatalogue.requirements(task).stream()
                            .map(parent -> code(questId(parent))).toList()));
                    quest.putBoolean("can_repeat", false);
                    quest.putString("progression_mode", "linear");
                    quest.putBoolean("hide_until_deps_complete", QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1);
                    quest.putBoolean("hide_details_until_startable", QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1);
                    ListTag description = new ListTag();
                    description.add(StringTag.valueOf(json(body(task))));
                    if (task.id().equals("fluid_logistics")) {
                        description.add(StringTag.valueOf(json(Component.translatable("gui.grindless.quest.tank_containers",
                                FluidLogic.TANK_CAPACITY))));
                    }
                    quest.put("description", description);
                    ListTag tasks = new ListTag();
                    CompoundTag evidence = new CompoundTag();
                    evidence.putString("id", unique(ids, taskId(task.id())));
                    evidence.putString("type", switch (task.evidence()) {
                        case ITEM, MATRIX -> "ftbquests:item";
                        case FREE -> "ftbquests:checkmark";
                        case DIMENSION -> "ftbquests:dimension";
                    });
                    if (task.evidence() == QuestCatalogue.Evidence.ITEM || task.evidence() == QuestCatalogue.Evidence.MATRIX) {
                        if (task.evidence() == QuestCatalogue.Evidence.MATRIX) {
                            CompoundTag item = new CompoundTag();
                            item.putString("id", task.subject());
                            item.putByte("Count", (byte) 1);
                            CompoundTag physical = new CompoundTag();
                            physical.putInt(ControlMatrixNbt.RATING, task.matrixRating().ordinal());
                            physical.putString(FtbMatrixFilter.BINDING, task.id());
                            item.put("tag", physical);
                            evidence.put("item", item);
                        } else {
                            evidence.putString("item", task.subject());
                        }
                        evidence.putLong("count", task.count());
                        evidence.putBoolean("consume_items", false);
                        evidence.putBoolean("only_from_crafting", false);
                        evidence.putBoolean("match_nbt", false);
                    } else if (task.evidence() == QuestCatalogue.Evidence.DIMENSION) {
                        evidence.putString("dimension", task.subject());
                    }
                    tasks.add(evidence);
                    quest.put("tasks", tasks);
                    ListTag rewards = new ListTag();
                    List<Long> rewardIds = rewardIds(task);
                    for (int index = 0; index < task.rewards().size(); index++) {
                        QuestCatalogue.Reward declared = task.rewards().get(index);
                        CompoundTag reward = new CompoundTag();
                        reward.putString("id", unique(ids, rewardIds.get(index)));
                        reward.putString("type", "ftbquests:item");
                        reward.putString("item", declared.itemId());
                        reward.putInt("count", declared.count());
                        reward.putBoolean("team_reward", false);
                        reward.putString("auto", "disabled");
                        rewards.add(reward);
                    }
                    quest.put("rewards", rewards);
                    return quest;
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                private static Component body(QuestCatalogue.Task task) {
                    return switch (task.id()) {
                        case "manual_work" -> Component.translatable(QuestTeaching.bodyKey(task),
                                SourceLogic.MANUAL_CHARGE, SourceLogic.MANUAL_COOLDOWN);
                        case "biomass_dynamo" -> Component.translatable(QuestTeaching.bodyKey(task),
                                FluxTier.F0.nominal(), SourceLogic.BIOMASS_BURN_TICKS);
                        default -> Component.translatable(QuestTeaching.bodyKey(task));
                    };
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                private static String json(Component component) {
                    return Component.Serializer.toJson(component);
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                private static ListTag strings(List<String> values) {
                    ListTag result = new ListTag();
                    values.forEach(value -> result.add(StringTag.valueOf(value)));
                    return result;
                }
                """)
                && questMethod(sources, "FtbQuestDefinitions", """
                private static String unique(Set<Long> ids, long id) {
                    if (!ids.add(id)) {
                        throw new IllegalStateException("Duplicate native quest object id: " + code(id));
                    }
                    return code(id);
                }
                """)
                && questMethod(sources, "HeraclesQuestIntegration", """
                public static boolean open(ServerPlayer player) {
                    if (!available) {
                        diagnostic(player);
                        return false;
                    }
                    refresh(player, true);
                    player.closeContainer();
                    ModUtils.openGroup(player, HeraclesDefinitions.GROUP);
                    return true;
                }
                """)
                && questMethod(sources, "HeraclesQuestIntegration", """
                public static void claim(ServerPlayer sender, String taskId) {
                    if (!available) {
                        diagnostic(sender);
                    } else {
                        QuestClaimService.claim(sender, taskId);
                    }
                    refresh(sender, true);
                }
                """)
                && questMethod(sources, "HeraclesQuestIntegration", """
                private static void refresh(ServerPlayer player, boolean force) {
                    Set<String> claimed = QuestProgress.get(player).claimed(player.getUUID());
                    if (available) {
                        var nativeProgress = QuestProgressHandler.getProgress(player.server, player.getUUID());
                        List<String> changed = HeraclesClaimProjection.reconcile(nativeProgress, claimed, definitions);
                        if (!changed.isEmpty()) {
                            QuestProgressHandler.read(player.server).setDirty();
                            QuestProgressHandler.sync(player, changed);
                        }
                    }
                    HeraclesQuestNetwork.Snapshot snapshot = new HeraclesQuestNetwork.Snapshot(available,
                            QuestClaimService.holdingBook(player), available ? QuestCatalogue.tasks().stream().map(task -> {
                                int held = QuestInventoryEvidence.count(player.getInventory(), task);
                                return new HeraclesQuestNetwork.State(task.id(), held, QuestLogic.consider(task, claimed,
                                        QuestEvidence.met(task, held, player.level().dimension().location().toString())));
                            }).toList() : List.of());
                    if (force || !snapshot.equals(LAST_SENT.get(player.getUUID()))) {
                        HeraclesQuestNetwork.send(player, snapshot);
                        LAST_SENT.put(player.getUUID(), snapshot);
                    }
                }
                """)
                && questMethod(sources, "HeraclesQuestIntegration", """
                private static void diagnostic(ServerPlayer player) {
                    player.displayClientMessage(Component.translatable("chat.grindless.quest.backend_unavailable"), false);
                }
                """)
                && questMethod(sources, "HeraclesQuestNetwork", """
                public static void init() {
                    Channel.INSTANCE.messageBuilder(ClaimRequest.class, 0, NetworkDirection.PLAY_TO_SERVER)
                            .encoder((packet, buffer) -> buffer.writeUtf(packet.taskId(), 64))
                            .decoder(buffer -> new ClaimRequest(buffer.readUtf(64)))
                            .consumerMainThread((packet, context) -> {
                                ServerPlayer sender = context.get().getSender();
                                if (sender != null) {
                                    HeraclesQuestIntegration.claim(sender, packet.taskId());
                                }
                            }).add();
                    Channel.INSTANCE.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                            .encoder(HeraclesQuestNetwork::encode)
                            .decoder(HeraclesQuestNetwork::decode)
                            .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                    () -> () -> HeraclesQuestClient.receive(packet))).add();
                }
                """)
                && questMethod(sources, "HeraclesQuestNetwork", """
                public static void claim(String taskId) {
                    Channel.INSTANCE.sendToServer(new ClaimRequest(taskId));
                }
                """)
                && questMethod(sources, "HeraclesQuestNetwork", """
                public static void send(ServerPlayer player, Snapshot snapshot) {
                    Channel.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
                }
                """)
                && questMethod(sources, "GrindlessClaimTask", """
                @Override
                public NumericTag test(QuestTaskType<?> type, NumericTag progress, ServerPlayer player) {
                    Set<String> claimed = QuestProgress.get(player).claimed(player.getUUID());
                    return storage().of(QuestCatalogue.unlocked(QuestCatalogue.chapter(QuestCatalogue.byId(catalogueId)), claimed)
                            && claimed.contains(catalogueId));
                }
                """)
                && questMethod(sources, "GrindlessClaimTask", """
                @Override
                public NumericTag init(QuestTaskType<?> type, NumericTag progress, ServerPlayer player) {
                    return test(type, progress, player);
                }
                """)
                && questMethod(sources, "GrindlessClaimTask", """
                @Override
                public BooleanTaskStorage storage() {
                    return BooleanTaskStorage.INSTANCE;
                }
                """)
                && questMethod(sources, "GrindlessClaimTask", """
                @Override
                public float getProgress(NumericTag progress) {
                    return storage().readBoolean(progress) ? 1 : 0;
                }
                """)
                && questMethod(sources, "GrindlessClaimTask", """
                @Override
                public QuestTaskType<GrindlessClaimTask> type() {
                    return TYPE;
                }
                """);
    }

    // The catalogue remains the quantity authority; only its closed, literal construction forms are accepted.
    private static boolean questCatalogueTasks(ClassTree catalogue) throws IOException {
        VariableTree tasks = field(catalogue, "TASKS");
        if (!tasks.getModifiers().getFlags().equals(Set.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL))
                || !tasks.getModifiers().getAnnotations().isEmpty()
                || !questText(tasks.getType()).equals("List<Task>")
                || !(tasks.getInitializer() instanceof MethodInvocationTree list)
                || !questText(list.getMethodSelect()).equals("List.of") || !list.getTypeArguments().isEmpty()
                || list.getArguments().isEmpty()
                || !(list.getArguments().get(0) instanceof NewClassTree starter)
                || !questText(starter.getIdentifier()).equals("Task") || starter.getEnclosingExpression() != null
                || starter.getClassBody() != null || !starter.getTypeArguments().isEmpty()
                || starter.getArguments().size() != 8
                || !starter.getArguments().subList(0, 7).stream().map(VerifySustainabilityRegistry::questText).toList()
                        .equals(List.of("STARTER_SUPPLIES", "\"bootstrap\"", "Evidence.FREE", "\"\"", "1",
                                "null", "List.of()"))
                || !(starter.getArguments().get(7) instanceof MethodInvocationTree rewards)
                || !questText(rewards.getMethodSelect()).equals("List.of") || !rewards.getTypeArguments().isEmpty()
                || rewards.getArguments().isEmpty() || !rewards.getArguments().stream()
                        .allMatch(VerifySustainabilityRegistry::questReward)) {
            return false;
        }
        for (Tree entry : list.getArguments().subList(1, list.getArguments().size())) {
            if (!(entry instanceof MethodInvocationTree task) || !task.getTypeArguments().isEmpty()) {
                return false;
            }
            var args = task.getArguments();
            boolean lesson = questText(task.getMethodSelect()).equals("lesson");
            if (args.size() != (lesson ? 6 : 7)) {
                return false;
            }
            boolean goal = switch (questText(task.getMethodSelect())) {
                case "task" -> Set.of("Evidence.ITEM", "Evidence.DIMENSION").contains(questText(args.get(2)))
                        && args.get(3) instanceof LiteralTree subject && subject.getValue() instanceof String;
                case "lesson" -> Set.of("Evidence.ITEM", "Evidence.FREE").contains(questText(args.get(2)))
                        && args.get(3) instanceof LiteralTree subject && subject.getValue() instanceof String;
                case "matrixTask" -> args.get(2) instanceof LiteralTree subject
                        && subject.getValue() instanceof String
                        && Set.of("FluxTier.F1", "FluxTier.F2").contains(questText(args.get(3)));
                default -> false;
            };
            if (!goal || !(args.get(0) instanceof LiteralTree id) || !(id.getValue() instanceof String)
                    || !(args.get(1) instanceof LiteralTree line) || !(line.getValue() instanceof String)
                    || !(args.get(4) instanceof LiteralTree count) || !(count.getValue() instanceof Integer quantity)
                    || quantity < 1 || !questStringList(args.get(5)) || !lesson && !questReward(args.get(6))) {
                return false;
            }
        }
        return true;
    }

    private static boolean questStringList(Tree tree) {
        return tree instanceof MethodInvocationTree list && questText(list.getMethodSelect()).equals("List.of")
                && list.getTypeArguments().isEmpty() && list.getArguments().stream()
                        .allMatch(value -> value instanceof LiteralTree literal && literal.getValue() instanceof String);
    }

    private static boolean questReward(Tree tree) {
        return tree instanceof MethodInvocationTree reward && questText(reward.getMethodSelect()).equals("reward")
                && reward.getTypeArguments().isEmpty() && reward.getArguments().size() == 2
                && reward.getArguments().get(0) instanceof LiteralTree item && item.getValue() instanceof String id
                && !id.isBlank() && reward.getArguments().get(1) instanceof LiteralTree count
                && count.getValue() instanceof Integer quantity && quantity >= 1 && quantity <= 64;
    }

    private static boolean questMethod(Map<String, ClassTree> sources, String owner, String declaration)
            throws IOException {
        declaration = declaration.replace("public " + owner + "(", "public QuestBindingContract(")
                .replace("private " + owner + "(", "private QuestBindingContract(");
        ClassTree contract = parse("QuestBindingContract", "class QuestBindingContract {" + declaration + "}");
        MethodTree expected = (MethodTree) contract.getMembers().get(0);
        MethodTree actual = method(sources.get(owner), expected.getName().toString(), expected.getParameters().size());
        return questText(expected.getModifiers()).equals(questText(actual.getModifiers()))
                && questText(expected.getReturnType()).equals(questText(actual.getReturnType()))
                && questText(expected.getReceiverParameter()).equals(questText(actual.getReceiverParameter()))
                && expected.getTypeParameters().stream().map(VerifySustainabilityRegistry::questText).toList()
                        .equals(actual.getTypeParameters().stream().map(VerifySustainabilityRegistry::questText).toList())
                && expected.getParameters().stream().map(VerifySustainabilityRegistry::questText).toList()
                        .equals(actual.getParameters().stream().map(VerifySustainabilityRegistry::questText).toList())
                && expected.getThrows().stream().map(VerifySustainabilityRegistry::questText).toList()
                        .equals(actual.getThrows().stream().map(VerifySustainabilityRegistry::questText).toList())
                && questText(expected.getDefaultValue()).equals(questText(actual.getDefaultValue()))
                && questText(expected.getBody()).equals(questText(actual.getBody()));
    }

    private static String questText(Tree tree) {
        return tree == null ? "" : tree.toString();
    }

    private static boolean questField(ClassTree type, String declaration) throws IOException {
        ClassTree contract = parse("QuestFieldContract", "class QuestFieldContract {" + declaration + "}");
        VariableTree expected = (VariableTree) contract.getMembers().get(0);
        return questText(expected).equals(questText(field(type, expected.getName().toString())));
    }

    private static boolean questClassShape(ClassTree type) throws IOException {
        String name = type.getSimpleName().toString();
        Set<String> expectedMethods = switch (name) {
            case "QuestBookItem" -> Set.of("<init>#1", "use#3", "appendHoverText#4");
            case "QuestBookMenu" -> Set.of("<init>#2", "status#1", "held#1", "quickMoveStack#2",
                    "stillValid#1", "broadcastChanges#0", "clickMenuButton#2", "refresh#0", "held#2",
                    "chapterUnlocked#1");
            case "QuestCatalogue" -> Set.of("<init>#0", "tasks#0", "byId#1", "lines#0",
                    "task#7", "matrixTask#7", "reward#2", "lesson#6", "chapter#1", "unlocked#2",
                    "requirements#1", "lines#1");
            case "QuestClaimService" -> Set.of("<init>#0", "holdingBook#1", "claim#2", "claim#7", "rewardStack#1");
            case "QuestRewardDelivery" -> Set.of("<init>#0", "prepare#2");
            case "QuestBookAccess" -> Set.of("<init>#0", "ensure#1", "ensure#4");
            case "QuestBookAccessEvents" -> Set.of("<init>#0", "login#1", "respawn#1", "tick#1",
                    "logout#1", "stopped#1", "recover#2");
            case "QuestLogic" -> Set.of("<init>#0", "consider#3", "byCode#1", "problems#1", "indexOf#2");
            case "QuestEvidence" -> Set.of("<init>#0", "met#3");
            case "QuestInventoryEvidence" -> Set.of("<init>#0", "count#2", "matches#2");
            case "QuestProgress" -> Set.of("get#1", "createIfMissing#1", "claimed#1", "legacyClaims#1", "updateNativeCompleted#2",
                    "usesNativeClaims#1", "useNativeClaims#1", "claim#2", "save#1", "load#1");
            case "QuestBackendPlatform", "QuestBackendPlatformImpl" -> Set.of("<init>#0", "open#1");
            case "GrindlessForge" -> Set.of("<init>#0");
            case "GrindlessForgeClient" -> Set.of("<init>#0", "onClientSetup#1");
            case "FtbQuestIntegration" -> Set.of("<init>#0", "init#1", "started#1", "open#1", "importPlayer#1", "tick#1");
            case "FtbQuestNative" -> Set.of("<init>#0", "verify#1", "migrate#3", "refresh#3", "sameData#2", "parseId#1");
            case "FtbQuestDefinitions" -> Set.of("<init>#0", "id#2", "questId#1", "taskId#1", "rewardIds#1",
                    "code#1", "chapters#0", "quest#4", "body#1", "json#1", "strings#1", "unique#2");
            case "FtbMatrixFilter" -> Set.of("getName#0", "isFilterStack#1", "doesItemMatch#2", "getMatcher#1",
                    "hasItemTagFilter#0", "makeTagFilterStack#1", "sameSubject#2");
            case "HeraclesQuestNetwork" -> Set.of("<init>#0", "init#0", "claim#1", "send#2", "encode#2", "decode#1");
            case "HeraclesQuestIntegration" -> Set.of("<init>#0", "init#1", "started#1", "reloaded#1", "verify#0",
                    "unavailable#1", "open#1", "claim#2", "login#1", "logout#1", "tick#1", "refresh#2", "diagnostic#1");
            case "GrindlessClaimTask" -> Set.of("test#3", "init#3", "getProgress#1", "storage#0", "type#0");
            default -> Set.of();
        };
        Set<String> expectedFields = switch (name) {
            case "QuestBookMenu" -> Set.of("player", "state");
            case "QuestCatalogue" -> Set.of("STARTER_SUPPLIES", "T0_COMPLETION", "TASKS");
            case "QuestBookAccessEvents" -> Set.of("WAITING_FOR_SPACE");
            case "QuestProgress" -> Set.of("KEY", "TAG_PLAYERS", "TAG_PLAYER", "TAG_TASKS", "TAG_NATIVE_CLAIMS",
                    "claimed", "nativeClaims", "nativeCompleted");
            case "FtbQuestIntegration" -> Set.of("ready");
            case "FtbMatrixFilter" -> Set.of("BINDING");
            case "HeraclesQuestNetwork" -> Set.of("PROTOCOL");
            case "HeraclesQuestIntegration" -> Set.of("definitions", "encoded", "LAST_SENT", "available", "failure");
            case "GrindlessClaimTask" -> Set.of("id", "catalogueId", "TYPE");
            default -> Set.of();
        };
        Set<String> expectedNested = switch (name) {
            case "QuestCatalogue" -> Set.of("Evidence", "Reward", "Task", "Chapter", "Type");
            case "QuestClaimService", "QuestBookAccess" -> Set.of("Result");
            case "QuestLogic" -> Set.of("Status");
            case "HeraclesQuestNetwork" -> Set.of("Channel", "ClaimRequest", "State", "Snapshot");
            default -> Set.of();
        };
        String parent = switch (name) {
            case "QuestBookItem" -> "Item";
            case "QuestBookMenu" -> "AbstractContainerMenu";
            case "QuestProgress" -> "SavedData";
            default -> "";
        };
        if (!normalized(type.getExtendsClause()).equals(parent) || !type.getTypeParameters().isEmpty()
                || (name.equals("GrindlessClaimTask")
                ? type.getKind() != Tree.Kind.RECORD || !type.getModifiers().getFlags().contains(Modifier.PUBLIC)
                        || !type.getImplementsClause().stream()
                        .map(VerifySustainabilityRegistry::normalized).toList()
                        .equals(List.of("QuestTask<ServerPlayer,NumericTag,GrindlessClaimTask>"))
                : type.getKind() != Tree.Kind.CLASS
                        || !type.getModifiers().getFlags().containsAll(Set.of(Modifier.PUBLIC, Modifier.FINAL))
                        || (name.equals("FtbMatrixFilter")
                        ? !type.getImplementsClause().stream()
                                .map(VerifySustainabilityRegistry::normalized).toList().equals(List.of("ItemFilterAdapter"))
                        : !type.getImplementsClause().isEmpty()))) {
            return false;
        }
        if (name.equals("QuestBookAccessEvents")) {
            ClassTree contract = parse("QuestEventsContract", """
                    @Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
                    public final class QuestEventsContract {}
                    """);
            if (!questText(type.getModifiers()).equals(questText(contract.getModifiers()))) {
                return false;
            }
        } else if (name.equals("GrindlessForge")) {
            ClassTree contract = parse("QuestForgeContract", """
                    @Mod(Grindless.MOD_ID)
                    public final class QuestForgeContract {}
                    """);
            if (!questText(type.getModifiers()).equals(questText(contract.getModifiers()))) {
                return false;
            }
        } else if (name.equals("GrindlessForgeClient")) {
            ClassTree contract = parse("QuestClientContract", """
                    @Mod.EventBusSubscriber(modid = Grindless.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
                    public final class QuestClientContract {}
                    """);
            if (!questText(type.getModifiers()).equals(questText(contract.getModifiers()))) {
                return false;
            }
        } else if (!type.getModifiers().getAnnotations().isEmpty()) {
            return false;
        }
        Set<String> methods = new TreeSet<>();
        Set<String> fields = new TreeSet<>();
        Set<String> nested = new TreeSet<>();
        for (Tree member : type.getMembers()) {
            if (member instanceof MethodTree method) {
                if (!methods.add(method.getName() + "#" + method.getParameters().size())) {
                    return false;
                }
            } else if (member instanceof VariableTree field) {
                if (!fields.add(field.getName().toString()) || name.equals("GrindlessClaimTask")
                        && !field.getName().contentEquals("TYPE")
                        && (!normalized(field.getType()).equals("String") || field.getInitializer() != null)) {
                    return false;
                }
            } else if (member instanceof ClassTree child) {
                if (!nested.add(child.getSimpleName().toString())) {
                    return false;
                }
                String declaration = switch (name + ":" + child.getSimpleName()) {
                    case "QuestCatalogue:Evidence" -> "public enum Evidence { ITEM, DIMENSION, MATRIX, FREE }";
                    case "QuestCatalogue:Chapter" -> "public enum Chapter { T0, T1 }";
                    case "QuestCatalogue:Type" -> "public enum Type { REWARD, LESSON }";
                    case "QuestCatalogue:Reward" -> "public record Reward(String itemId, int count) {}";
                    case "QuestCatalogue:Task" -> """
                            public record Task(String id, String line, Evidence evidence, String subject, int count,
                                               FluxTier matrixRating, List<String> requires, List<Reward> rewards, Type type) {
                                public Task {
                                    requires = List.copyOf(requires);
                                    rewards = List.copyOf(rewards);
                                }

                                public Task(String id, String line, Evidence evidence, String subject, int count,
                                            FluxTier matrixRating, List<String> requires, List<Reward> rewards) {
                                    this(id, line, evidence, subject, count, matrixRating, requires, rewards, Type.REWARD);
                                }

                                public Task(String id, String line, Evidence evidence, String subject, int count,
                                            FluxTier matrixRating, List<String> requires, Reward reward) {
                                    this(id, line, evidence, subject, count, matrixRating, requires, List.of(reward));
                                }

                                public Reward reward() {
                                    if (rewards.isEmpty()) {
                                        throw new IllegalStateException("This lesson has no material reward: " + id);
                                    }
                                    return rewards.get(0);
                                }
                            }
                            """;
                    case "QuestClaimService:Result" -> """
                            public enum Result {
                                CLAIMED, ALREADY_CLAIMED, LOCKED, UNMET, BOOK_REQUIRED, UNKNOWN_TASK, INVALID_REWARD,
                                INVENTORY_FULL, NATIVE_BACKEND
                            }
                            """;
                    case "QuestBookAccess:Result" ->
                            "public enum Result { PRESENT, DELIVERED, INVENTORY_FULL, STARTER_CLAIMED }";
                    case "QuestLogic:Status" -> "public enum Status { CLAIMED, LOCKED, UNMET, CLAIMABLE }";
                    case "HeraclesQuestNetwork:Channel" -> """
                            private static final class Channel {
                                private static final SimpleChannel INSTANCE =
                                        NetworkRegistry.newSimpleChannel(Grindless.id("quest_projection"),
                                                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
                            }
                            """;
                    case "HeraclesQuestNetwork:ClaimRequest" -> "public record ClaimRequest(String taskId) {}";
                    case "HeraclesQuestNetwork:State" ->
                            "public record State(String taskId, int held, QuestLogic.Status status) {}";
                    case "HeraclesQuestNetwork:Snapshot" -> """
                            public record Snapshot(boolean available, boolean holdingBook, List<State> tasks) {
                                public Snapshot {
                                    tasks = List.copyOf(tasks);
                                }
                            }
                            """;
                    default -> "";
                };
                if (declaration.isEmpty()) {
                    return false;
                }
                ClassTree contract = parse("QuestNestedContract", "class QuestNestedContract {" + declaration + "}");
                if (!questText(child).equals(questText(contract.getMembers().get(0)))) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (!methods.equals(expectedMethods) || !fields.equals(expectedFields) || !nested.equals(expectedNested)) {
            return false;
        }
        List<String> declarations = switch (name) {
            case "QuestBookMenu" -> List.of("private final Player player;",
                    "private final SimpleContainerData state;");
            case "QuestCatalogue" -> List.of("public static final String STARTER_SUPPLIES = \"starter_supplies\";",
                    "public static final String T0_COMPLETION = \"relay\";");
            case "QuestBookAccessEvents" -> List.of(
                    "private static final Set<UUID> WAITING_FOR_SPACE = new HashSet<>();");
            case "QuestProgress" -> List.of("public static final String KEY = \"grindless_quest_progress\";",
                    "private static final String TAG_PLAYERS = \"Players\";",
                    "private static final String TAG_PLAYER = \"Player\";",
                    "private static final String TAG_TASKS = \"Tasks\";",
                    "private static final String TAG_NATIVE_CLAIMS = \"NativeClaims\";",
                    "private final Map<UUID, Set<String>> claimed = new HashMap<>();",
                    "private final Set<UUID> nativeClaims = new HashSet<>();",
                    "private final Map<UUID, Set<String>> nativeCompleted = new HashMap<>();");
            case "FtbQuestIntegration" -> List.of("private static boolean ready;");
            case "FtbMatrixFilter" -> List.of("public static final String BINDING = \"grindless:quest_matrix\";");
            case "HeraclesQuestNetwork" -> List.of("private static final String PROTOCOL = \"1\";");
            case "HeraclesQuestIntegration" -> List.of("private static Map<String, Quest> definitions = Map.of();",
                    "private static Map<String, String> encoded = Map.of();",
                    "private static final Map<UUID, HeraclesQuestNetwork.Snapshot> LAST_SENT = new HashMap<>();",
                    "private static boolean available;", "private static String failure;");
            case "GrindlessClaimTask" -> List.of("""
                    public static final QuestTaskType<GrindlessClaimTask> TYPE = new QuestTaskType<>() {
                        @Override
                        public ResourceLocation id() {
                            return Grindless.id("claim");
                        }

                        @Override
                        public Codec<GrindlessClaimTask> codec(String taskId) {
                            return Codec.STRING.comapFlatMap(value -> QuestCatalogue.byId(value) == null
                                            ? DataResult.error(() -> "Unknown Grindless quest: " + value)
                                            : DataResult.success(new GrindlessClaimTask(taskId, value)),
                                    GrindlessClaimTask::catalogueId).fieldOf("quest").codec();
                        }
                    };
                    """);
            default -> List.of();
        };
        for (String declaration : declarations) {
            if (!questField(type, declaration)) {
                return false;
            }
        }
        return true;
    }

    private static void questAccessCounterexamples(Path root) throws IOException {
        Map<String, ClassTree> sources = questSources(root, new TreeMap<>());
        eq("legacy claim/access guards and native FTB migration, definitions and entrypoints are recognized", true,
                questBookAccess(sources));
        rejectQuest(root, sources, "the service cannot replace the physical book with permission",
                "QuestClaimService", "return player.getMainHandItem().is(ModItems.QUEST_BOOK.get())", "return true");
        rejectQuest(root, sources, "another held item cannot establish the registered book prerequisite",
                "QuestClaimService", "ModItems.QUEST_BOOK.get()", "Items.STICK");
        rejectQuest(root, sources, "an unheld legacy menu cannot certify access",
                "QuestBookMenu", "return QuestClaimService.holdingBook(player)", "return true");
        rejectQuest(root, sources, "a different menu opener cannot certify this claim interface",
                "QuestBookItem", "new QuestBookMenu(id, inventory)", "new DifferentMenu(id, inventory)");
        rejectQuest(root, sources, "the optional backend cannot suppress the legacy fallback",
                "QuestBookItem", "if (!QuestBackendPlatform.open(server))", "if (false)");
        rejectQuest(root, sources, "a live service call cannot pass a fabricated book fact",
                "QuestClaimService", "taskId, holdingBook(player),", "taskId, true,");
        rejectQuest(root, sources, "the common claim book guard cannot be removed",
                "QuestClaimService", "if (!holdingBook)", "if (false)");
        rejectQuest(root, sources, "client-thread claims cannot certify the server contract",
                "QuestClaimService", "if (!player.server.isSameThread())", "if (false)");
        rejectQuest(root, sources, "the ledger owner must be the actual server player",
                "QuestClaimService", "player.getUUID(), taskId", "new UUID(0, 0), taskId");
        rejectQuest(root, sources, "evidence must use the same player's current physical inventory",
                "QuestClaimService", "QuestInventoryEvidence.count(player.getInventory(), task)",
                "QuestInventoryEvidence.count(player.getEnderChestInventory(), task)");
        rejectQuest(root, sources, "unknown tasks and missing player identities must be rejected",
                "QuestClaimService", "if (task == null || player == null)", "if (task == null)");
        rejectQuest(root, sources, "the service cannot remove the native claim authority fence",
                "QuestClaimService", "        if (progress.usesNativeClaims(player)) {\n"
                        + "            return Result.NATIVE_BACKEND;\n        }\n", "");
        rejectQuest(root, sources, "the native claim service fence must inspect the actual player",
                "QuestClaimService", "progress.usesNativeClaims(player)", "progress.usesNativeClaims(new UUID(0, 0))");
        rejectQuest(root, sources, "a retired legacy claim cannot report successful reward delivery",
                "QuestClaimService", "return Result.NATIVE_BACKEND;", "return Result.CLAIMED;");
        rejectQuest(root, sources, "the native backend result must append without changing old result ordinals",
                "QuestClaimService", "INVENTORY_FULL,\n        NATIVE_BACKEND",
                "NATIVE_BACKEND,\n        INVENTORY_FULL");
        rejectQuest(root, sources, "current evidence cannot become an unconditional predicate",
                "QuestClaimService", "evidence.test(task)", "true");
        rejectQuest(root, sources, "the requested task's prerequisites cannot be ignored",
                "QuestLogic", "if (!claimed.contains(required))", "if (false)");
        rejectQuest(root, sources, "retained T1 claims cannot bypass the final T0 chapter gate",
                "QuestLogic", "if (!QuestCatalogue.unlocked(QuestCatalogue.chapter(task), claimed))", "if (false)");
        rejectQuest(root, sources, "the chapter gate cannot be rebound to an unrelated item claim",
                "QuestCatalogue", "T0_COMPLETION = \"relay\"", "T0_COMPLETION = \"multitool\"");
        rejectQuest(root, sources, "T1 cannot be unlocked merely by opening a menu",
                "QuestCatalogue", "chapter == Chapter.T0 || claimed.contains(T0_COMPLETION)", "true");
        rejectQuest(root, sources, "projected T1 prerequisites must retain the actual T0 anchor",
                "QuestCatalogue", "required.add(T0_COMPLETION);", "required.add(STARTER_SUPPLIES);");
        rejectQuest(root, sources, "the chapter payload cannot claim an unconditional server unlock",
                "QuestBookMenu", "QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, claimed) ? 1 : 0", "1");
        rejectQuest(root, sources, "lesson construction cannot mint an unexamined material reward",
                "QuestCatalogue", "List.of(), Type.LESSON", "List.of(reward(\"minecraft:iron_ingot\", 64)), Type.LESSON");
        rejectQuest(root, sources, "an explicit lesson cannot enter ordinary material delivery",
                "QuestClaimService", "if (task.type() == QuestCatalogue.Type.LESSON)", "if (false)");
        rejectQuest(root, sources, "a lesson cannot ignore an invalid declared reward",
                "QuestClaimService", "if (!task.rewards().isEmpty())", "if (false)");
        rejectQuest(root, sources, "native task tests cannot expose retained T1 completion before Relay",
                "GrindlessClaimTask",
                "QuestCatalogue.unlocked(QuestCatalogue.chapter(QuestCatalogue.byId(catalogueId)), claimed)",
                "true");
        rejectQuest(root, sources, "milestone telemetry cannot observe rejected or duplicate requests",
                "QuestClaimService", "if (result == Result.CLAIMED) {",
                "DevTelemetry.milestone(player.level(), taskId); if (result == Result.CLAIMED) {");
        rejectQuest(root, sources, "FREE must be appended without changing the old evidence ordinals",
                "QuestCatalogue",
                "MATRIX,\n        /** An explicit free claim, with no item, location or payment objective. */\n        FREE",
                "FREE, MATRIX");
        rejectQuest(root, sources, "free evidence must be an explicit case, not a permissive default",
                "QuestEvidence", "case FREE -> true;", "default -> true;");
        rejectQuest(root, sources, "a free claim cannot acquire a physical evidence requirement",
                "QuestEvidence", "case FREE -> true;", "case FREE -> held >= task.count();");
        rejectQuest(root, sources, "free status cannot make ordinary item goals unconditional",
                "QuestLogic", "task.evidence() == QuestCatalogue.Evidence.FREE || evidenceMet",
                "task.evidence() != QuestCatalogue.Evidence.MATRIX || evidenceMet");
        rejectQuest(root, sources, "the first starter task cannot require a prior claim",
                "QuestCatalogue", "Evidence.FREE, \"\", 1, null, List.of(), List.of(",
                "Evidence.FREE, \"\", 1, null, List.of(\"multitool\"), List.of(");
        rejectQuest(root, sources, "the starter cannot silently become a physical item objective",
                "QuestCatalogue", "Evidence.FREE, \"\", 1, null, List.of(), List.of(",
                "Evidence.ITEM, \"minecraft:stone\", 1, null, List.of(), List.of(");
        rejectQuest(root, sources, "catalogue lookup must return the requested task, not the starter",
                "QuestCatalogue", "return task;", "return TASKS.get(0);");
        rejectQuest(root, sources, "the task reward list must be an immutable snapshot",
                "QuestCatalogue", "rewards = List.copyOf(rewards);", "rewards = rewards;");
        rejectQuest(root, sources, "a record accessor cannot hide all but the first reward",
                "QuestCatalogue", "public Reward reward() {",
                "public List<Reward> rewards() { return List.of(reward()); } public Reward reward() {");
        rejectQuest(root, sources, "catalogue initialization cannot hide an unexamined reward callback",
                "QuestCatalogue", "reward(\"minecraft:bread\", 64)", "reward(System.getProperty(\"fixture\"), 64)");
        rejectQuest(root, sources, "physical evidence must meet the real task quantity",
                "QuestEvidence", "held >= task.count()", "held >= 0");
        rejectQuest(root, sources, "physical matrix evidence must have the exact rating",
                "QuestInventoryEvidence", "value.rating() == task.matrixRating()", "true");
        rejectQuest(root, sources, "reward quantity must be checked before claiming",
                "QuestClaimService", "reward.getCount() != declared.count()", "reward.getCount() < 0");
        rejectQuest(root, sources, "a batch claim cannot silently validate only its first reward",
                "QuestClaimService", "for (QuestCatalogue.Reward declared : task.rewards())",
                "for (QuestCatalogue.Reward declared : List.of(task.reward()))");
        rejectQuest(root, sources, "an empty declared batch cannot become a successful claim",
                "QuestClaimService", "if (task.rewards().isEmpty())", "if (false)");
        rejectQuest(root, sources, "every resolved reward must respect its actual item's stack limit",
                "QuestClaimService", "reward.getCount() > reward.getMaxStackSize()", "false");
        rejectQuest(root, sources, "resolved reward stacks must be copied before preparation",
                "QuestClaimService", "rewards.add(reward.copy());", "rewards.add(reward);");
        rejectQuest(root, sources, "the delivery preparer must receive an immutable complete batch",
                "QuestClaimService", "prepare.apply(List.copyOf(rewards))", "prepare.apply(rewards)");
        rejectQuest(root, sources, "a missing reward id is not a real reward stack",
                "QuestClaimService", "!BuiltInRegistries.ITEM.containsKey(id)", "false");
        rejectQuest(root, sources, "rewards cannot precede the idempotent ledger transition",
                "QuestClaimService", "if (!progress.claim(player, taskId))",
                "delivery.get().run(); if (!progress.claim(player, taskId))");
        rejectQuest(root, sources, "the complete batch must fit before any ledger transition",
                "QuestClaimService", "Optional<Runnable> delivery = prepare.apply(List.copyOf(rewards));",
                "progress.claim(player, taskId); Optional<Runnable> delivery = prepare.apply(List.copyOf(rewards));");
        rejectQuest(root, sources, "a full inventory must leave the claim unrecorded",
                "QuestClaimService", "if (delivery.isEmpty())", "if (false)");
        rejectQuest(root, sources, "prepared delivery must run only once after the claim",
                "QuestClaimService", "delivery.get().run();", "delivery.get().run(); delivery.get().run();");
        rejectQuest(root, sources, "live delivery must target the same player's actual inventory",
                "QuestClaimService", "QuestRewardDelivery.prepare(player.getInventory(), rewards)",
                "QuestRewardDelivery.prepare(new net.minecraft.world.entity.player.Inventory(player), rewards)");
        rejectQuest(root, sources, "a repeated common claim cannot deliver another reward",
                "QuestClaimService", "if (claimed.contains(taskId))", "if (false)");
        rejectQuest(root, sources, "the persistent claim must reject an existing task",
                "QuestProgress", "if (!tasks.add(task))", "if (false)");
        rejectQuest(root, sources, "a swallowed SavedData load failure cannot use an unguarded empty-ledger supplier",
                "QuestProgress", "() -> createIfMissing(file)", "QuestProgress::new");
        rejectQuest(root, sources, "the creation guard must inspect the actual world data directory",
                "QuestProgress", ".resolve(\"data\")", ".resolve(\"unrelated_data\")");
        rejectQuest(root, sources, "the creation guard must inspect the current ledger, not a backup filename",
                "QuestProgress", "KEY + \".dat\"", "KEY + \".dat_old\"");
        rejectQuest(root, sources, "a dangling ledger symlink cannot bypass existing-data protection",
                "QuestProgress", "BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS", "BasicFileAttributes.class");
        rejectQuest(root, sources, "an inspection failure cannot recreate a fresh reward ledger",
                "QuestProgress",
                "throw new IllegalStateException(\"Cannot inspect quest progress; refusing to recreate its reward ledger\",\n"
                        + "                    exception);",
                "return new QuestProgress();");
        rejectQuest(root, sources, "an existing failed ledger cannot be replaced by empty progress",
                "QuestProgress",
                "throw new IllegalStateException(\"Existing quest progress could not be loaded; refusing to recreate its reward ledger\");",
                "return new QuestProgress();");
        rejectQuest(root, sources, "guarded ledger creation must use the actual filesystem API",
                "QuestProgress", "import java.nio.file.Files;", "import fixture.Files;");
        rejectQuest(root, sources, "the gate view must retain historical paid claims",
                "QuestProgress", "new HashSet<>(legacyClaims(player))", "new HashSet<>()");
        rejectQuest(root, sources, "native completion cannot be omitted from compatibility gates",
                "QuestProgress", "result.addAll(nativeCompleted.getOrDefault(player, Set.of()));", "");
        rejectQuest(root, sources, "the gate view must observe the same player's native completion",
                "QuestProgress", "nativeCompleted.getOrDefault(player, Set.of())",
                "nativeCompleted.getOrDefault(new UUID(0, 0), Set.of())");
        rejectQuest(root, sources, "the combined gate view must remain an immutable snapshot",
                "QuestProgress", "return Set.copyOf(result);", "return result;");
        rejectQuest(root, sources, "native completion cannot be exposed as a historical paid import receipt",
                "QuestProgress", "return Set.copyOf(claimed.getOrDefault(player, Set.of()));",
                "Set<String> paid = new HashSet<>(claimed.getOrDefault(player, Set.of())); "
                        + "paid.addAll(nativeCompleted.getOrDefault(player, Set.of())); return Set.copyOf(paid);");
        rejectQuest(root, sources, "callers cannot mutate the historical paid receipt set",
                "QuestProgress", "return Set.copyOf(claimed.getOrDefault(player, Set.of()));",
                "return claimed.getOrDefault(player, Set.of());");
        rejectQuest(root, sources, "native completion observations require native claim authority",
                "QuestProgress", "!usesNativeClaims(player) || ", "");
        rejectQuest(root, sources, "native completion observations must reject every unknown task id",
                "QuestProgress", "completed.stream().anyMatch(id -> QuestCatalogue.byId(id) == null)", "false");
        rejectQuest(root, sources, "native completion observations cannot enter the paid receipt map",
                "QuestProgress", "nativeCompleted.put(player, Set.copyOf(completed));",
                "claimed.put(player, Set.copyOf(completed));");
        rejectQuest(root, sources, "native completion observations must not alias the caller's set",
                "QuestProgress", "nativeCompleted.put(player, Set.copyOf(completed));",
                "nativeCompleted.put(player, completed);");
        rejectQuest(root, sources, "native completion observations must replace rather than accumulate stale gates",
                "QuestProgress", "nativeCompleted.put(player, Set.copyOf(completed));",
                "Set<String> retained = new HashSet<>(nativeCompleted.getOrDefault(player, Set.of())); "
                        + "retained.addAll(completed); nativeCompleted.put(player, Set.copyOf(retained));");
        rejectQuest(root, sources, "native completion observations must update the requested player",
                "QuestProgress", "nativeCompleted.put(player, Set.copyOf(completed));",
                "nativeCompleted.put(new UUID(0, 0), Set.copyOf(completed));");
        rejectQuest(root, sources, "native engine observations cannot dirty the legacy SavedData authority",
                "QuestProgress", "nativeCompleted.put(player, Set.copyOf(completed));",
                "nativeCompleted.put(player, Set.copyOf(completed)); setDirty();");
        rejectQuest(root, sources, "native completion observations cannot leak between SavedData instances",
                "QuestProgress", "private final Map<UUID, Set<String>> nativeCompleted = new HashMap<>();",
                "private static final Map<UUID, Set<String>> nativeCompleted = new HashMap<>();");
        rejectQuest(root, sources, "native observations cannot replace persisted paid receipts",
                "QuestProgress", "for (Map.Entry<UUID, Set<String>> entry : claimed.entrySet())",
                "for (Map.Entry<UUID, Set<String>> entry : nativeCompleted.entrySet())");
        rejectQuest(root, sources, "saving paid receipts cannot fold in the native gate view",
                "QuestProgress", "for (String id : entry.getValue())", "for (String id : claimed(entry.getKey()))");
        rejectQuest(root, sources, "native claim authority cannot leak between SavedData instances",
                "QuestProgress", "private final Set<UUID> nativeClaims = new HashSet<>();",
                "private static final Set<UUID> nativeClaims = new HashSet<>();");
        rejectQuest(root, sources, "one player's native authority cannot retire every player's legacy mint",
                "QuestProgress", "return nativeClaims.contains(player);", "return !nativeClaims.isEmpty();");
        rejectQuest(root, sources, "native authority must reject a missing player identity",
                "QuestProgress", "if (player == null)", "if (false)");
        rejectQuest(root, sources, "native authority must mark the requested UUID, not a fabricated player",
                "QuestProgress", "nativeClaims.add(player)", "nativeClaims.add(new UUID(0, 0))");
        rejectQuest(root, sources, "the first native authority transition must dirty SavedData",
                "QuestProgress", "if (nativeClaims.add(player)) {\n            setDirty();\n        }",
                "nativeClaims.add(player);");
        rejectQuest(root, sources, "retiring legacy rewards cannot erase retained import receipts",
                "QuestProgress", "if (nativeClaims.add(player)) {",
                "if (nativeClaims.add(player)) { claimed.remove(player);");
        rejectQuest(root, sources, "direct ledger claims cannot bypass the native authority fence",
                "QuestProgress", "player == null || usesNativeClaims(player) || QuestCatalogue.byId(task) == null",
                "player == null || QuestCatalogue.byId(task) == null");
        rejectQuest(root, sources, "the native marker key cannot overwrite the retained legacy receipts",
                "QuestProgress", "TAG_NATIVE_CLAIMS = \"NativeClaims\"", "TAG_NATIVE_CLAIMS = \"Players\"");
        rejectQuest(root, sources, "saving native authority must preserve the old player receipts",
                "QuestProgress", "tag.put(TAG_PLAYERS, players);", "");
        rejectQuest(root, sources, "native markers must save independently of legacy receipt ownership",
                "QuestProgress", "for (UUID player : nativeClaims)", "for (UUID player : claimed.keySet())");
        rejectQuest(root, sources, "a saved native marker cannot disappear and reopen the legacy mint",
                "QuestProgress", "tag.put(TAG_NATIVE_CLAIMS, nativePlayers);", "");
        rejectQuest(root, sources, "loading must retain legacy receipts under their original player UUID",
                "QuestProgress", "progress.claimed.put(player, tasks);",
                "progress.claimed.put(new UUID(0, 0), tasks);");
        rejectQuest(root, sources, "loading cannot validate a native UUID without restoring its authority",
                "QuestProgress", "progress.nativeClaims.add(UUID.fromString(nativePlayers.getString(i)));",
                "UUID.fromString(nativePlayers.getString(i));");
        rejectQuest(root, sources, "native-first players need their fence even without legacy receipts",
                "QuestProgress", "if (tag.contains(TAG_NATIVE_CLAIMS))",
                "if (!progress.claimed.isEmpty() && tag.contains(TAG_NATIVE_CLAIMS))");
        rejectQuest(root, sources, "legacy saves without a native marker must remain loadable",
                "QuestProgress", "if (tag.contains(TAG_NATIVE_CLAIMS))", "if (true)");
        rejectQuest(root, sources, "native authority cannot use the lenient NBT list reader on corrupt markers",
                "QuestProgress", "if (!(tag.get(TAG_NATIVE_CLAIMS) instanceof ListTag nativePlayers)\n"
                        + "                    || !nativePlayers.isEmpty() && nativePlayers.getElementType() != Tag.TAG_STRING) {\n"
                        + "                throw new IllegalArgumentException(\"Invalid native quest claim authority list in saved progress\");\n"
                        + "            }",
                "ListTag nativePlayers = tag.getList(TAG_NATIVE_CLAIMS, Tag.TAG_STRING);");
        rejectQuest(root, sources, "a present non-list native marker cannot be silently treated as absent",
                "QuestProgress", "tag.contains(TAG_NATIVE_CLAIMS)", "tag.contains(TAG_NATIVE_CLAIMS, Tag.TAG_LIST)");
        rejectQuest(root, sources, "a nonempty native marker list must contain string elements",
                "QuestProgress", "nativePlayers.getElementType() != Tag.TAG_STRING", "false");
        rejectQuest(root, sources, "an empty native marker list must not require a string element type",
                "QuestProgress", "!nativePlayers.isEmpty() && ", "");
        rejectQuest(root, sources, "a corrupt native marker list cannot return an unfenced legacy ledger",
                "QuestProgress",
                "throw new IllegalArgumentException(\"Invalid native quest claim authority list in saved progress\");",
                "return progress;");
        rejectQuest(root, sources, "a corrupt native UUID cannot be skipped and reopen the legacy mint",
                "QuestProgress",
                "throw new IllegalArgumentException(\"Invalid native quest claim authority in saved progress\", exception);",
                "continue;");
        for (String type : List.of("CompoundTag", "ListTag", "StringTag", "Tag")) {
            rejectQuest(root, sources, "quest persistence must retain the vanilla NBT binding: " + type,
                    "QuestProgress", "import net.minecraft.nbt." + type + ";", "import fixture." + type + ";");
        }
        rejectQuest(root, sources, "native marker corruption must throw the real Java rejection type",
                "QuestProgress", "import java.util.HashMap;",
                "import fixture.IllegalArgumentException; import java.util.HashMap;");
        rejectQuest(root, sources, "literal whitespace cannot silently switch the persistent ledger key",
                "QuestProgress", "\"grindless_quest_progress\"", "\"grindless_quest_ progress\"");
        rejectQuest(root, sources, "a consuming service cannot certify a reusable book",
                "QuestClaimService", "if (result == Result.CLAIMED) {",
                "if (result == Result.CLAIMED) { player.getMainHandItem().shrink(1);");
        rejectQuest(root, sources, "the staging capacity cannot count armor and offhand as delivery slots",
                "QuestRewardDelivery", "new ItemStack[inventory.items.size()]",
                "new ItemStack[inventory.getContainerSize()]");
        rejectQuest(root, sources, "staging cannot alias and mutate the player's existing stacks",
                "QuestRewardDelivery", "inventory.getItem(slot).copy()", "inventory.getItem(slot)");
        rejectQuest(root, sources, "staging must preserve pre-existing oversized saved stacks without clamping",
                "QuestRewardDelivery", "SimpleContainer staged = new SimpleContainer(contents);",
                "SimpleContainer staged = new SimpleContainer(contents.length); "
                        + "for (int slot = 0; slot < contents.length; slot++) { staged.setItem(slot, contents[slot]); }");
        rejectQuest(root, sources, "native capacity checks must cover every reward in the batch",
                "QuestRewardDelivery", "for (ItemStack reward : rewards)",
                "for (ItemStack reward : rewards.subList(0, 1))");
        rejectQuest(root, sources, "preparation cannot insert a partial batch into the live inventory",
                "QuestRewardDelivery", "if (!staged.addItem(reward.copy()).isEmpty()) {",
                "inventory.add(reward.copy()); if (!staged.addItem(reward.copy()).isEmpty()) {");
        rejectQuest(root, sources, "an overflowing batch cannot fall back to world drops",
                "QuestRewardDelivery", "return Optional.empty();",
                "return Optional.of(() -> rewards.forEach(stack -> inventory.player.drop(stack, false)));");
        rejectQuest(root, sources, "staged contents cannot be committed before the ledger accepts the claim",
                "QuestRewardDelivery", "return Optional.of(() -> {",
                "inventory.setItem(0, staged.getItem(0)); return Optional.of(() -> {");
        rejectQuest(root, sources, "commit must preserve unchanged live item objects",
                "QuestRewardDelivery", "if (!ItemStack.matches(inventory.getItem(slot), staged.getItem(slot)))",
                "if (true)");
        rejectQuest(root, sources, "a similarly named container cannot replace vanilla native stacking",
                "QuestRewardDelivery", "import net.minecraft.world.SimpleContainer;", "import fixture.SimpleContainer;");
        rejectQuest(root, sources, "an unexamined prepare overload cannot certify native batch delivery",
                "QuestRewardDelivery", "private QuestRewardDelivery() {",
                "static Optional<Runnable> prepare(Inventory inventory, ItemStack reward) "
                        + "{ return Optional.of(() -> {}); } private QuestRewardDelivery() {");
        rejectQuest(root, sources, "a same-spelled delivery helper cannot replace the checked authority",
                "QuestClaimService", "import java.util.ArrayList;",
                "import fixture.QuestRewardDelivery; import java.util.ArrayList;");
        rejectQuest(root, sources, "book recovery must remain server-thread-only",
                "QuestBookAccess", "if (!player.server.isSameThread())", "if (false)");
        rejectQuest(root, sources, "book recovery must use the actual player's ledger identity",
                "QuestBookAccess", "player.getUUID(), player.getInventory()", "new UUID(0, 0), player.getInventory()");
        rejectQuest(root, sources, "post-starter book recovery cannot be disabled",
                "QuestBookAccess", "for (int slot = 0; slot < inventory.getContainerSize(); slot++)",
                "if (progress.claimed(player).contains(QuestCatalogue.STARTER_SUPPLIES)) { return Result.STARTER_CLAIMED; }"
                        + " for (int slot = 0; slot < inventory.getContainerSize(); slot++)");
        rejectQuest(root, sources, "book detection must include every actual inventory slot, not only main slots",
                "QuestBookAccess", "inventory.getContainerSize()", "inventory.items.size()");
        rejectQuest(root, sources, "book detection must inspect each real slot exactly once",
                "QuestBookAccess", "inventory.getItem(slot).is(book)", "inventory.getItem(0).is(book)");
        rejectQuest(root, sources, "book recovery cannot mark the starter as already claimed",
                "QuestBookAccess", "delivery.get().run();",
                "progress.claim(player, QuestCatalogue.STARTER_SUPPLIES); delivery.get().run();");
        rejectQuest(root, sources, "book recovery cannot grant the starter kit implicitly",
                "QuestBookAccess", "List.of(new ItemStack(book))",
                "List.of(new ItemStack(book), new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64))");
        rejectQuest(root, sources, "book recovery must deliver just one utility book",
                "QuestBookAccess", "new ItemStack(book)", "new ItemStack(book, 2)");
        rejectQuest(root, sources, "book recovery must defer without drops when the inventory is full",
                "QuestBookAccess", "if (delivery.isEmpty())", "if (false)");
        rejectQuest(root, sources, "book recovery events cannot be confined to a client-only subscriber",
                "QuestBookAccessEvents", "@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)",
                "@Mod.EventBusSubscriber(modid = Grindless.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)");
        rejectQuest(root, sources, "both login and respawn must invoke recovery for the actual player",
                "QuestBookAccessEvents", "recover(player, true);",
                "recover(player.server.getPlayerList().getPlayers().get(0), true);");
        rejectQuest(root, sources, "book recovery cannot depend on the optional renderer being loaded",
                "QuestBookAccessEvents", "QuestBookAccess.Result result = QuestBookAccess.ensure(player);",
                "if (!net.minecraftforge.fml.ModList.get().isLoaded(\"heracles\")) { return; } "
                        + "QuestBookAccess.Result result = QuestBookAccess.ensure(player);");
        rejectQuest(root, sources, "full-inventory recovery must retain the same UUID for deferred retry",
                "QuestBookAccessEvents", "WAITING_FOR_SPACE.add(player.getUUID());",
                "WAITING_FOR_SPACE.add(new UUID(0, 0));");
        rejectQuest(root, sources, "deferred recovery must keep its bounded twenty-tick retry cadence",
                "QuestBookAccessEvents", "event.player.tickCount % 20 == 0", "event.player.tickCount % 2000 == 0");
        rejectQuest(root, sources, "deferred recovery cannot poll players who are not waiting for space",
                "QuestBookAccessEvents", "WAITING_FOR_SPACE.contains(player.getUUID())", "true");
        rejectQuest(root, sources, "deferred recovery must actually retry without repeated full-inventory chat",
                "QuestBookAccessEvents", "recover(player, false);", "recover(player, true);");
        rejectQuest(root, sources, "completed recovery must remove its waiting UUID",
                "QuestBookAccessEvents", "WAITING_FOR_SPACE.remove(player.getUUID());",
                "WAITING_FOR_SPACE.contains(player.getUUID());");
        rejectQuest(root, sources, "logout must remove the disconnecting player's waiting UUID",
                "QuestBookAccessEvents", "WAITING_FOR_SPACE.remove(event.getEntity().getUUID());",
                "WAITING_FOR_SPACE.remove(new UUID(0, 0));");
        rejectQuest(root, sources, "server stop must clear deferred recovery state",
                "QuestBookAccessEvents", "WAITING_FOR_SPACE.clear();", "WAITING_FOR_SPACE.isEmpty();");
        rejectQuest(root, sources, "legacy refresh cannot consume the book after a successful service call",
                "QuestBookMenu", "private void refresh() {",
                "private void refresh() { player.getMainHandItem().shrink(1);");
        rejectQuest(root, sources, "backend refresh cannot consume the book after a successful service call",
                "HeraclesQuestIntegration", "private static void refresh(ServerPlayer player, boolean force) {",
                "private static void refresh(ServerPlayer player, boolean force) { player.getMainHandItem().shrink(1);");
        rejectQuest(root, sources, "legacy buttons cannot claim a different catalogue task",
                "QuestBookMenu", "QuestClaimService.claim(server, tasks.get(id).id())",
                "QuestClaimService.claim(server, tasks.get(0).id())");
        rejectQuest(root, sources, "matching helper spelling cannot substitute another authority",
                "QuestBookMenu", "import io.github.ezequiel24123z.grindless.quest.QuestClaimService;",
                "import fixture.QuestClaimService;");
        rejectQuest(root, sources, "a claim overload outside the checked profile is unproved",
                "QuestClaimService", "private QuestClaimService() {",
                "public static Result claim(Player player, String taskId) { return Result.CLAIMED; } "
                        + "private QuestClaimService() {");
        rejectQuest(root, sources, "unexamined class initialization cannot certify the claim service",
                "QuestClaimService", "private QuestClaimService() {",
                "static { System.setProperty(\"fixture\", \"unexamined\"); } private QuestClaimService() {");
        rejectQuest(root, sources, "a claim packet must be server-bound",
                "HeraclesQuestNetwork", "ClaimRequest.class, 0, NetworkDirection.PLAY_TO_SERVER",
                "ClaimRequest.class, 0, NetworkDirection.PLAY_TO_CLIENT");
        rejectQuest(root, sources, "a similarly named channel factory is not the inspected Forge channel",
                "HeraclesQuestNetwork", "NetworkRegistry.newSimpleChannel", "UncheckedChannels.newSimpleChannel");
        rejectQuest(root, sources, "network claim work must execute on the main thread",
                "HeraclesQuestNetwork", ".consumerMainThread((packet, context) -> {",
                ".consumerNetworkThread((packet, context) -> {");
        rejectQuest(root, sources, "the network player must come from the real connection sender",
                "HeraclesQuestNetwork", "ServerPlayer sender = context.get().getSender();",
                "ServerPlayer sender = context.get().getSender().server.getPlayerList().getPlayers().get(0);");
        rejectQuest(root, sources, "a null network sender cannot reach the claim authority",
                "HeraclesQuestNetwork", "if (sender != null)", "if (true)");
        rejectQuest(root, sources, "the integration must forward the same sender and task",
                "HeraclesQuestIntegration", "QuestClaimService.claim(sender, taskId);",
                "QuestClaimService.claim(sender.server.getPlayerList().getPlayers().get(0), taskId);");
        rejectQuest(root, sources, "a packet cannot redefine its task accessor",
                "HeraclesQuestNetwork", "public record ClaimRequest(String taskId) {",
                "public record ClaimRequest(String taskId) { public String taskId() { return \"multitool\"; }");
        rejectQuest(root, sources, "snapshot construction cannot hide a book-consuming callback",
                "HeraclesQuestNetwork", "public Snapshot {",
                "public Snapshot { net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()"
                        + ".getPlayerList().getPlayers().get(0).getMainHandItem().shrink(1);");
        rejectQuest(root, sources, "native completion must observe the same player's common ledger",
                "GrindlessClaimTask", "claimed(player.getUUID())", "claimed(null)");
        rejectQuest(root, sources, "native FTB must be selected before the optional legacy renderer",
                "QuestBackendPlatformImpl", "if (ModList.get().isLoaded(\"ftbquests\"))", "if (false)");
        rejectQuest(root, sources, "a selected native backend must actually open its framework UI",
                "QuestBackendPlatformImpl", "FtbQuestIntegration.open(player);", "");
        rejectQuest(root, sources, "a selected native backend cannot fall through to the custom menu",
                "QuestBackendPlatformImpl", "return true;", "return false;");
        rejectQuest(root, sources, "the native platform binding cannot substitute a same-named helper",
                "QuestBackendPlatformImpl",
                "import io.github.ezequiel24123z.grindless.forge.quest.FtbQuestIntegration;",
                "import fixture.FtbQuestIntegration;");
        rejectQuest(root, sources, "Forge cannot initialize rival quest reward authorities together",
                "GrindlessForge", "} else if (ModList.get().isLoaded(\"heracles\"))",
                "} if (ModList.get().isLoaded(\"heracles\"))");
        rejectQuest(root, sources, "the Heracles client must remain disabled when native FTB is present",
                "GrindlessForgeClient", "!ModList.get().isLoaded(\"ftbquests\") && ", "");
        rejectQuest(root, sources, "native Matrix evidence requires registration of the supported filter adapter",
                "FtbQuestIntegration", "FTBQuestsAPI.api().registerFilterAdapter(new FtbMatrixFilter());", "");
        rejectQuest(root, sources, "native adapter registration must use the actual framework API",
                "FtbQuestIntegration", "import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;",
                "import fixture.FTBQuestsAPI;");
        rejectQuest(root, sources, "the native probe must retain its actual command registration hook",
                "FtbQuestIntegration", "MinecraftForge.EVENT_BUS.addListener(FtbQuestChecks::registerCommands);", "");
        rejectQuest(root, sources, "native readiness cannot skip the loaded definition verification",
                "FtbQuestIntegration", "FtbQuestNative.verify(file);", "");
        rejectQuest(root, sources, "native opening cannot bypass admission and historical receipt import",
                "FtbQuestIntegration", "if (!importPlayer(player))", "if (false)");
        rejectQuest(root, sources, "native migration must execute on the server thread",
                "FtbQuestIntegration", "if (!player.server.isSameThread())", "if (false)");
        rejectQuest(root, sources, "unverified native definitions cannot admit players",
                "FtbQuestIntegration", "if (!ready)", "if (false)");
        rejectQuest(root, sources, "native receipt import must use the admitted player's UUID",
                "FtbQuestIntegration", "player.getUUID(), data", "new java.util.UUID(0, 0), data");
        rejectQuest(root, sources, "native imported receipts must be saved before successful admission",
                "FtbQuestIntegration", "data.saveIfChanged();", "");
        rejectQuest(root, sources, "failed native import cannot report successful admission",
                "FtbQuestIntegration", "return false;", "return true;");
        rejectQuest(root, sources, "only historical paid legacy receipts may be imported",
                "FtbQuestNative", "legacy.legacyClaims(player)", "legacy.claimed(player)");
        rejectQuest(root, sources, "native migration requires both a player and framework progress",
                "FtbQuestNative", "if (player == null || nativeProgress == null)", "if (false)");
        rejectQuest(root, sources, "native migration cannot omit permanent retirement of the common reward mint",
                "FtbQuestNative", "legacy.useNativeClaims(player);", "");
        rejectQuest(root, sources, "native migration cannot manufacture a historical starter receipt",
                "FtbQuestNative", "legacy.useNativeClaims(player);",
                "legacy.claim(player, QuestCatalogue.STARTER_SUPPLIES); legacy.useNativeClaims(player);");
        rejectQuest(root, sources, "native migration cannot mark unpaid catalogue rewards as already claimed",
                "FtbQuestNative", "if (!paid.contains(task.id()))", "if (false)");
        rejectQuest(root, sources, "native reward import cannot deliver an actual reward",
                "FtbQuestNative", "Reward reward = nativeProgress.getFile().getReward(rewardId);",
                "Reward reward = nativeProgress.getFile().getReward(rewardId); reward.claim(null, false);");
        rejectQuest(root, sources, "native paid receipts must belong to the actual player",
                "FtbQuestNative", "nativeProgress.markRewardAsClaimed(player, reward, now)",
                "nativeProgress.markRewardAsClaimed(new UUID(0, 0), reward, now)");
        rejectQuest(root, sources, "native receipt import cannot accept team-wide reward ownership",
                "FtbQuestNative", "reward.isTeamReward()", "false");
        rejectQuest(root, sources, "a failed native receipt mark cannot be treated as successful import",
                "FtbQuestNative", "!nativeProgress.markRewardAsClaimed(player, reward, now)", "false");
        rejectQuest(root, sources, "all paid reward receipts must precede any imported quest completion",
                "FtbQuestNative", "for (long rewardId : FtbQuestDefinitions.rewardIds(task)) {",
                "nativeProgress.setCompleted(FtbQuestDefinitions.questId(task.id()), new Date(now)); "
                        + "for (long rewardId : FtbQuestDefinitions.rewardIds(task)) {");
        rejectQuest(root, sources, "retained T1 completion import cannot bypass the original chapter gate",
                "FtbQuestNative", "!QuestCatalogue.unlocked(QuestCatalogue.chapter(task), paid)", "false");
        rejectQuest(root, sources, "locked framework data cannot accept imported completion",
                "FtbQuestNative", "nativeProgress.isLocked()", "false");
        rejectQuest(root, sources, "native task verification must bind the actual implementation class",
                "FtbQuestNative", "task.getClass() != expectedTask.getClass()", "false");
        rejectQuest(root, sources, "native reward verification must bind the stock item reward implementation",
                "FtbQuestNative", "reward.getClass() != ItemReward.class", "false");
        rejectQuest(root, sources, "modified native definition data cannot establish authority",
                "FtbQuestNative", "if (!expectedData.equals(actualData))", "if (false)");
        rejectQuest(root, sources, "native completion refresh cannot mint paid historical receipts",
                "FtbQuestNative", "legacy.updateNativeCompleted(player, completed);",
                "completed.forEach(id -> legacy.claim(player, id));");
        rejectQuest(root, sources, "native Quest imports cannot resolve to the Heracles type",
                "FtbQuestNative", "import dev.ftb.mods.ftbquests.quest.Quest;",
                "import earth.terrarium.heracles.api.quests.Quest;");
        rejectQuest(root, sources, "the retained Heracles Quest binding cannot resolve to native FTB",
                "HeraclesQuestIntegration", "import earth.terrarium.heracles.api.quests.Quest;",
                "import dev.ftb.mods.ftbquests.quest.Quest;");
        rejectQuest(root, sources, "unexamined native migration overloads cannot certify receipt import",
                "FtbQuestNative", "private FtbQuestNative() {",
                "public static void migrate(QuestProgress legacy, UUID player) { legacy.useNativeClaims(player); } "
                        + "private FtbQuestNative() {");
        rejectQuest(root, sources, "native object identities cannot silently change their stable namespace",
                "FtbQuestDefinitions", "\"grindless/ftb/\"", "\"grindless/ftb-replacement/\"");
        rejectQuest(root, sources, "native reward identities must cover the complete declared batch",
                "FtbQuestDefinitions", "IntStream.range(0, task.rewards().size())", "IntStream.range(0, 1)");
        rejectQuest(root, sources, "native quests cannot become repeatable reward mints",
                "FtbQuestDefinitions", "quest.putBoolean(\"can_repeat\", false);",
                "quest.putBoolean(\"can_repeat\", true);");
        rejectQuest(root, sources, "native chapter defaults cannot enable repeatable rewards",
                "FtbQuestDefinitions", "document.putBoolean(\"default_repeatable_quest\", false);",
                "document.putBoolean(\"default_repeatable_quest\", true);");
        rejectQuest(root, sources, "native quest dependencies must retain canonical chapter prerequisites",
                "FtbQuestDefinitions", "QuestCatalogue.requirements(task)", "task.requires()");
        rejectQuest(root, sources, "native free tasks must use the stock explicit checkmark",
                "FtbQuestDefinitions", "case FREE -> \"ftbquests:checkmark\";", "case FREE -> \"ftbquests:item\";");
        rejectQuest(root, sources, "native Matrix evidence must retain the stock ItemTask",
                "FtbQuestDefinitions", "case ITEM, MATRIX -> \"ftbquests:item\";",
                "case ITEM -> \"ftbquests:item\"; case MATRIX -> \"grindless:matrix\";");
        rejectQuest(root, sources, "native Matrix filter stacks must retain the canonical requested rating",
                "FtbQuestDefinitions", "physical.putInt(ControlMatrixNbt.RATING, task.matrixRating().ordinal());",
                "physical.putInt(ControlMatrixNbt.RATING, FluxTier.F0.ordinal());");
        rejectQuest(root, sources, "native Matrix filters must identify their logical catalogue task",
                "FtbQuestDefinitions", "physical.putString(FtbMatrixFilter.BINDING, task.id());", "");
        rejectQuest(root, sources, "native Matrix filters must actually attach the physical item tag",
                "FtbQuestDefinitions", "item.put(\"tag\", physical);", "");
        rejectQuest(root, sources, "stock native item tasks must require the actual physical quantity",
                "FtbQuestDefinitions", "evidence.putLong(\"count\", task.count());", "evidence.putLong(\"count\", 1);");
        rejectQuest(root, sources, "stock native evidence tasks cannot consume their physical proof",
                "FtbQuestDefinitions", "evidence.putBoolean(\"consume_items\", false);",
                "evidence.putBoolean(\"consume_items\", true);");
        rejectQuest(root, sources, "native rewards must retain their declared quantity",
                "FtbQuestDefinitions", "reward.putInt(\"count\", declared.count());", "reward.putInt(\"count\", 1);");
        rejectQuest(root, sources, "native reward claims must remain individual, never team-wide",
                "FtbQuestDefinitions", "reward.putBoolean(\"team_reward\", false);",
                "reward.putBoolean(\"team_reward\", true);");
        rejectQuest(root, sources, "native item rewards cannot acquire automatic delivery",
                "FtbQuestDefinitions", "reward.putString(\"auto\", \"disabled\");",
                "reward.putString(\"auto\", \"enabled\");");
        rejectQuest(root, sources, "duplicate native object identities cannot be accepted",
                "FtbQuestDefinitions", "if (!ids.add(id))", "if (false)");
        rejectQuest(root, sources, "native Matrix filters must retain the stable private binding key",
                "FtbMatrixFilter", "\"grindless:quest_matrix\"", "\"grindless:quest_matrix_other\"");
        rejectQuest(root, sources, "malformed Matrix markers must be intercepted rather than falling back to item-only matching",
                "FtbMatrixFilter", "stack.getTag().contains(BINDING)", "stack.getTag().contains(BINDING, Tag.TAG_STRING)");
        rejectQuest(root, sources, "invalid native Matrix filter definitions cannot produce a permissive matcher",
                "FtbMatrixFilter", "return NO_MATCH;", "return candidate -> true;");
        rejectQuest(root, sources, "the native Matrix adapter must test the real candidate, not its trusted filter stack",
                "FtbMatrixFilter", "getMatcher(filterStack).test(toCheck)", "getMatcher(filterStack).test(filterStack)");
        rejectQuest(root, sources, "native Matrix filter lookup must bind the declared logical task",
                "FtbMatrixFilter", "QuestCatalogue.byId(filterStack.getTag().getString(BINDING))",
                "QuestCatalogue.byId(\"relay\")");
        rejectQuest(root, sources, "native Matrix filter definitions must match their declared physical item",
                "FtbMatrixFilter", "!sameSubject(filterStack, declared)", "false");
        rejectQuest(root, sources, "native Matrix filter definitions must validate their own physical rating",
                "FtbMatrixFilter", ".filter(spec -> spec.rating() == declared.matrixRating()).isEmpty()",
                ".filter(spec -> true).isEmpty()");
        rejectQuest(root, sources, "native Matrix candidates must match the declared physical item",
                "FtbMatrixFilter", "sameSubject(candidate, declared)", "true");
        rejectQuest(root, sources, "native Matrix candidate validation cannot reuse the filter stack's specification",
                "FtbMatrixFilter", "ControlMatrixItem.spec(candidate)", "ControlMatrixItem.spec(filterStack)");
        rejectQuest(root, sources, "native Matrix candidates must retain the exact requested valid rating",
                "FtbMatrixFilter", ".filter(spec -> spec.rating() == declared.matrixRating()).isPresent()",
                ".filter(spec -> true).isPresent()");
        rejectQuest(root, sources, "native Matrix predicates must implement the supported framework adapter",
                "FtbMatrixFilter", "import dev.ftb.mods.ftbquests.api.ItemFilterAdapter;",
                "import fixture.ItemFilterAdapter;");
        rejectQuest(root, sources, "native Matrix predicates cannot shadow the inherited framework matcher type",
                "FtbMatrixFilter", "import net.minecraft.nbt.Tag;",
                "import fixture.Matcher; import net.minecraft.nbt.Tag;");
        rejectQuest(root, sources, "native Matrix predicates must use the actual physical Matrix parser",
                "FtbMatrixFilter", "import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;",
                "import fixture.ControlMatrixItem;");
    }

    private static void rejectQuest(Path root, Map<String, ClassTree> sources, String message,
                                    String owner, String before, String after) throws IOException {
        String original = Files.readString(root.resolve(QUEST_SOURCES.get(owner))).replace("\r\n", "\n");
        if (!original.contains(before)) {
            throw new AssertionError("Quest counterexample no longer targets the source: " + message);
        }
        Map<String, ClassTree> altered = new TreeMap<>(sources);
        try {
            altered.put(owner, parse(owner, original.replace(before, after)));
            eq(message, false, questBookAccess(altered));
        } catch (IOException rejection) {
            if (!rejection.getMessage().contains("claim binding not evaluated")
                    && !rejection.getMessage().contains("Missing or ambiguous registration authority method")) {
                throw rejection;
            }
            eq(message + " (explicit closed-profile rejection)", true, true);
        }
    }

    private static ClassTree mechanicsSource(Path root, String folder, String name,
                                             Map<String, String> hashes) throws IOException {
        Path relative = JAVA.resolve(Path.of(folder, name + ".java"));
        byte[] bytes = Files.readAllBytes(root.resolve(relative));
        hashes.put("mechanics_source:" + relative.toString().replace('\\', '/'), AuthorityAdapters.sha256(bytes));
        return parse(name, new String(bytes, StandardCharsets.UTF_8));
    }

    private static boolean dynamoPushRate(ClassTree dynamo) throws IOException {
        var statements = method(dynamo, "push", 0).getBody().getStatements();
        return statements.size() >= 2 && normalized(statements.get(0)).equals(
                "if(!isDue(PushBackoff.period(PUSH_PERIOD,fruitlessPushes))){return;}")
                && statements.get(1) instanceof VariableTree allowance
                && allowance.getName().contentEquals("allowance") && normalized(allowance.getType()).equals("long")
                && normalized(allowance.getInitializer()).equals(
                "Math.min(energy().getStored(),FluxTier.F0.nominal()*PUSH_PERIOD)")
                && normalized(field(dynamo, "PUSH_PERIOD").getInitializer()).equals("1");
    }

    private static final String PROCESS_CRANK_BODY =
            "{if(kind.tier()!=FluxTier.F0||getLevel()==null||getLevel().isClientSide()){return;}"
                    + "longbefore=energy().getStored();"
                    + "energy().setStored(Math.min(energy().getCapacity(),before+SourceLogic.MANUAL_CHARGE));"
                    + "longaccepted=energy().getStored()-before;"
                    + "if(accepted>0){DevTelemetry.manualWork(getLevel(),"
                    + "BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString(),accepted);}}";

    private static void physicalSourceCounterexamples(Path root) throws IOException {
        String name = "HandCrankDynamoBlockEntity";
        String source = Files.readString(root.resolve(JAVA.resolve(Path.of("machine", name + ".java"))));
        String altered = source.replace("PushBackoff.period(PUSH_PERIOD, fruitlessPushes)",
                "PushBackoff.period(PUSH_PERIOD * 2, fruitlessPushes)");
        eq("the slower-push fixture actually changes the inspected guard", false, source.equals(altered));
        eq("a mismatched due-period cannot certify the original physical delivery rate", false,
                dynamoPushRate(parse(name, altered)));
        altered = source.replace("FluxTier.F0.nominal() * PUSH_PERIOD",
                "FluxTier.F0.nominal() * PUSH_PERIOD * 2");
        eq("the oversized-allowance fixture actually changes the inspected statement", false, source.equals(altered));
        eq("a mismatched allowance cannot certify the original physical delivery rate", false,
                dynamoPushRate(parse(name, altered)));

        String process = "ProcessMachineBlockEntity";
        String original = Files.readString(root.resolve(JAVA.resolve(Path.of("machine", process + ".java"))));
        eq("the actual process manual-work profile includes post-settlement telemetry", PROCESS_CRANK_BODY,
                normalized(method(parse(process, original), "crankByHand", 0).getBody()));
        for (String[] mutation : List.of(
                new String[]{"non-F0 recipients", "kind.tier() != FluxTier.F0 || ", ""},
                new String[]{"missing world guard", "getLevel() == null || ", ""},
                new String[]{"client-side work", " || getLevel().isClientSide()", ""},
                new String[]{"unclamped energy", "Math.min(energy().getCapacity(), before + SourceLogic.MANUAL_CHARGE)",
                        "before + SourceLogic.MANUAL_CHARGE"},
                new String[]{"inflated charge", "before + SourceLogic.MANUAL_CHARGE",
                        "before + SourceLogic.MANUAL_CHARGE * 2"},
                new String[]{"requested rather than accepted work", "long accepted = energy().getStored() - before;",
                        "long accepted = SourceLogic.MANUAL_CHARGE;"},
                new String[]{"zero-delta notifications", "if (accepted > 0)", "if (accepted >= 0)"},
                new String[]{"requested rather than actual notification", "toString(), accepted);",
                        "toString(), SourceLogic.MANUAL_CHARGE);"},
                new String[]{"notification before energy admission", "long before = energy().getStored();",
                        "long before = energy().getStored();"
                                + "DevTelemetry.manualWork(getLevel(), \"premature\", SourceLogic.MANUAL_CHARGE);"},
                new String[]{"wrong machine identity", "BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString(), accepted",
                        "\"hand_crank_dynamo\", accepted"})) {
            String changed = original.replace(mutation[1], mutation[2]);
            eq("manual-work counterexample actually changes source: " + mutation[0], false, original.equals(changed));
            eq("manual-work source binding rejects " + mutation[0], false,
                    PROCESS_CRANK_BODY.equals(normalized(method(parse(process, changed), "crankByHand", 0).getBody())));
        }
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
            ClassTree type = parsed.getTypeDecls().stream().filter(ClassTree.class::isInstance).map(ClassTree.class::cast)
                    .filter(candidate -> candidate.getSimpleName().contentEquals(name)).findFirst()
                    .orElseThrow(() -> new IOException("Missing registration authority class: " + name));
            if (QUEST_SOURCES.containsKey(name)) {
                questImports(parsed, name);
            }
            return type;
        }
    }

    private static void questImports(CompilationUnitTree parsed, String name) throws IOException {
        Map<String, String> bindings = new TreeMap<>(Map.ofEntries(
                Map.entry("Grindless", "io.github.ezequiel24123z.grindless.Grindless"),
                Map.entry("QuestCatalogue", "io.github.ezequiel24123z.grindless.quest.QuestCatalogue"),
                Map.entry("QuestProjection", "io.github.ezequiel24123z.grindless.quest.QuestProjection"),
                Map.entry("QuestTeaching", "io.github.ezequiel24123z.grindless.quest.QuestTeaching"),
                Map.entry("FluxTier", "io.github.ezequiel24123z.grindless.energy.FluxTier"),
                Map.entry("FluidLogic", "io.github.ezequiel24123z.grindless.fluid.FluidLogic"),
                Map.entry("SourceLogic", "io.github.ezequiel24123z.grindless.source.SourceLogic"),
                Map.entry("FtbQuestFiles", "io.github.ezequiel24123z.grindless.forge.quest.FtbQuestFiles"),
                Map.entry("GrindlessClient", "io.github.ezequiel24123z.grindless.client.GrindlessClient"),
                Map.entry("MachineInspectionOverlay",
                        "io.github.ezequiel24123z.grindless.forge.overlay.MachineInspectionOverlay"),
                Map.entry("MaterialRegistry", "io.github.ezequiel24123z.grindless.material.MaterialRegistry"),
                Map.entry("RegistryTagView", "io.github.ezequiel24123z.grindless.material.RegistryTagView"),
                Map.entry("ModItems", "io.github.ezequiel24123z.grindless.registry.ModItems"),
                Map.entry("ModMenus", "io.github.ezequiel24123z.grindless.registry.ModMenus"),
                Map.entry("DevTelemetry", "io.github.ezequiel24123z.grindless.telemetry.dev.DevTelemetry"),
                Map.entry("ControlMatrixItem", "io.github.ezequiel24123z.grindless.item.ControlMatrixItem"),
                Map.entry("ControlMatrixNbt", "io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt"),
                Map.entry("FtbQuestChecks", "io.github.ezequiel24123z.grindless.forge.quest.FtbQuestChecks"),
                Map.entry("HeraclesDefinitions", "io.github.ezequiel24123z.grindless.forge.quest.HeraclesDefinitions"),
                Map.entry("HeraclesClaimProjection",
                        "io.github.ezequiel24123z.grindless.forge.quest.HeraclesClaimProjection"),
                Map.entry("HeraclesQuestClient", "io.github.ezequiel24123z.grindless.forge.quest.HeraclesQuestClient"),
                Map.entry("Player", "net.minecraft.world.entity.player.Player"),
                Map.entry("MinecraftServer", "net.minecraft.server.MinecraftServer"),
                Map.entry("ServerPlayer", "net.minecraft.server.level.ServerPlayer"),
                Map.entry("Item", "net.minecraft.world.item.Item"),
                Map.entry("TagKey", "net.minecraft.tags.TagKey"),
                Map.entry("ItemStack", "net.minecraft.world.item.ItemStack"),
                Map.entry("Items", "net.minecraft.world.item.Items"),
                Map.entry("Level", "net.minecraft.world.level.Level"),
                Map.entry("InteractionHand", "net.minecraft.world.InteractionHand"),
                Map.entry("InteractionResultHolder", "net.minecraft.world.InteractionResultHolder"),
                Map.entry("Inventory", "net.minecraft.world.entity.player.Inventory"),
                Map.entry("AbstractContainerMenu", "net.minecraft.world.inventory.AbstractContainerMenu"),
                Map.entry("SimpleContainerData", "net.minecraft.world.inventory.SimpleContainerData"),
                Map.entry("SimpleContainer", "net.minecraft.world.SimpleContainer"),
                Map.entry("SimpleMenuProvider", "net.minecraft.world.SimpleMenuProvider"),
                Map.entry("Component", "net.minecraft.network.chat.Component"),
                Map.entry("BuiltInRegistries", "net.minecraft.core.registries.BuiltInRegistries"),
                Map.entry("ResourceLocation", "net.minecraft.resources.ResourceLocation"),
                Map.entry("Container", "net.minecraft.world.Container"),
                Map.entry("SavedData", "net.minecraft.world.level.saveddata.SavedData"),
                Map.entry("LevelResource", "net.minecraft.world.level.storage.LevelResource"),
                Map.entry("CompoundTag", "net.minecraft.nbt.CompoundTag"),
                Map.entry("ListTag", "net.minecraft.nbt.ListTag"),
                Map.entry("StringTag", "net.minecraft.nbt.StringTag"),
                Map.entry("Tag", "net.minecraft.nbt.Tag"),
                Map.entry("NumericTag", "net.minecraft.nbt.NumericTag"),
                Map.entry("MenuRegistry", "dev.architectury.registry.menu.MenuRegistry"),
                Map.entry("ExpectPlatform", "dev.architectury.injectables.annotations.ExpectPlatform"),
                Map.entry("LifecycleEvent", "dev.architectury.event.events.common.LifecycleEvent"),
                Map.entry("EventBuses", "dev.architectury.platform.forge.EventBuses"),
                Map.entry("ModList", "net.minecraftforge.fml.ModList"),
                Map.entry("Mod", "net.minecraftforge.fml.common.Mod"),
                Map.entry("MinecraftForge", "net.minecraftforge.common.MinecraftForge"),
                Map.entry("IEventBus", "net.minecraftforge.eventbus.api.IEventBus"),
                Map.entry("FMLCommonSetupEvent", "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent"),
                Map.entry("FMLClientSetupEvent", "net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent"),
                Map.entry("FMLJavaModLoadingContext", "net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext"),
                Map.entry("FMLPaths", "net.minecraftforge.fml.loading.FMLPaths"),
                Map.entry("TagsUpdatedEvent", "net.minecraftforge.event.TagsUpdatedEvent"),
                Map.entry("SubscribeEvent", "net.minecraftforge.eventbus.api.SubscribeEvent"),
                Map.entry("TickEvent", "net.minecraftforge.event.TickEvent"),
                Map.entry("PlayerEvent", "net.minecraftforge.event.entity.player.PlayerEvent"),
                Map.entry("ServerStoppedEvent", "net.minecraftforge.event.server.ServerStoppedEvent"),
                Map.entry("SimpleChannel", "net.minecraftforge.network.simple.SimpleChannel"),
                Map.entry("NetworkRegistry", "net.minecraftforge.network.NetworkRegistry"),
                Map.entry("NetworkDirection", "net.minecraftforge.network.NetworkDirection"),
                Map.entry("PacketDistributor", "net.minecraftforge.network.PacketDistributor"),
                Map.entry("DistExecutor", "net.minecraftforge.fml.DistExecutor"),
                Map.entry("Dist", "net.minecraftforge.api.distmarker.Dist"),
                Map.entry("FTBQuestsAPI", "dev.ftb.mods.ftbquests.api.FTBQuestsAPI"),
                Map.entry("ItemFilterAdapter", "dev.ftb.mods.ftbquests.api.ItemFilterAdapter"),
                Map.entry("BaseQuestFile", "dev.ftb.mods.ftbquests.quest.BaseQuestFile"),
                Map.entry("Chapter", "dev.ftb.mods.ftbquests.quest.Chapter"),
                Map.entry("QuestObjectBase", "dev.ftb.mods.ftbquests.quest.QuestObjectBase"),
                Map.entry("TeamData", "dev.ftb.mods.ftbquests.quest.TeamData"),
                Map.entry("ServerQuestFile", "dev.ftb.mods.ftbquests.quest.ServerQuestFile"),
                Map.entry("Reward", "dev.ftb.mods.ftbquests.quest.reward.Reward"),
                Map.entry("ItemReward", "dev.ftb.mods.ftbquests.quest.reward.ItemReward"),
                Map.entry("Task", "dev.ftb.mods.ftbquests.quest.task.Task"),
                Map.entry("ItemTask", "dev.ftb.mods.ftbquests.quest.task.ItemTask"),
                Map.entry("CheckmarkTask", "dev.ftb.mods.ftbquests.quest.task.CheckmarkTask"),
                Map.entry("DimensionTask", "dev.ftb.mods.ftbquests.quest.task.DimensionTask"),
                Map.entry("ObjectCompletedEvent", "dev.ftb.mods.ftbquests.events.ObjectCompletedEvent"),
                Map.entry("OpenQuestBookMessage", "dev.ftb.mods.ftbquests.net.OpenQuestBookMessage"),
                Map.entry("TeamEvent", "dev.ftb.mods.ftbteams.api.event.TeamEvent"),
                Map.entry("QuestTask", "earth.terrarium.heracles.api.tasks.QuestTask"),
                Map.entry("QuestTaskType", "earth.terrarium.heracles.api.tasks.QuestTaskType"),
                Map.entry("BooleanTaskStorage", "earth.terrarium.heracles.api.tasks.storage.defaults.BooleanTaskStorage"),
                Map.entry("QuestProgressHandler", "earth.terrarium.heracles.common.handlers.progress.QuestProgressHandler"),
                Map.entry("ModUtils", "earth.terrarium.heracles.common.utils.ModUtils"),
                Map.entry("Quest", "earth.terrarium.heracles.api.quests.Quest"),
                Map.entry("Codec", "com.mojang.serialization.Codec"),
                Map.entry("DataResult", "com.mojang.serialization.DataResult"),
                Map.entry("Set", "java.util.Set"), Map.entry("List", "java.util.List"),
                Map.entry("ArrayList", "java.util.ArrayList"), Map.entry("Optional", "java.util.Optional"),
                Map.entry("Map", "java.util.Map"), Map.entry("HashMap", "java.util.HashMap"),
                Map.entry("HashSet", "java.util.HashSet"), Map.entry("UUID", "java.util.UUID"),
                Map.entry("Date", "java.util.Date"), Map.entry("LinkedHashSet", "java.util.LinkedHashSet"),
                Map.entry("Collections", "java.util.Collections"), Map.entry("LinkedHashMap", "java.util.LinkedHashMap"),
                Map.entry("Locale", "java.util.Locale"),
                Map.entry("ByteBuffer", "java.nio.ByteBuffer"),
                Map.entry("StandardCharsets", "java.nio.charset.StandardCharsets"),
                Map.entry("MessageDigest", "java.security.MessageDigest"),
                Map.entry("NoSuchAlgorithmException", "java.security.NoSuchAlgorithmException"),
                Map.entry("IOException", "java.io.IOException"),
                Map.entry("Path", "java.nio.file.Path"), Map.entry("Files", "java.nio.file.Files"),
                Map.entry("LinkOption", "java.nio.file.LinkOption"),
                Map.entry("NoSuchFileException", "java.nio.file.NoSuchFileException"),
                Map.entry("BasicFileAttributes", "java.nio.file.attribute.BasicFileAttributes"),
                Map.entry("Function", "java.util.function.Function"),
                Map.entry("Predicate", "java.util.function.Predicate"),
                Map.entry("Runnable", "java.lang.Runnable"),
                Map.entry("String", "java.lang.String"),
                Map.entry("Long", "java.lang.Long"), Map.entry("Integer", "java.lang.Integer"),
                Map.entry("System", "java.lang.System"), Map.entry("SecurityException", "java.lang.SecurityException"),
                Map.entry("UnsupportedOperationException", "java.lang.UnsupportedOperationException"),
                Map.entry("IllegalArgumentException", "java.lang.IllegalArgumentException"),
                Map.entry("IllegalStateException", "java.lang.IllegalStateException"),
                Map.entry("AssertionError", "java.lang.AssertionError")));
        QUEST_SOURCES.forEach((type, path) -> {
            String relative = path.toString().replace('\\', '/');
            bindings.put(type, relative.substring(relative.indexOf("/java/") + 6, relative.length() - 5)
                    .replace('/', '.'));
        });
        if (name.equals("FtbQuestNative")) {
            bindings.put("Quest", "dev.ftb.mods.ftbquests.quest.Quest");
        }
        if (name.equals("FtbMatrixFilter")) {
            bindings.put("Matcher", "dev.ftb.mods.ftbquests.api.ItemFilterAdapter.Matcher");
        }
        String owner = bindings.get(name);
        String packageName = owner.substring(0, owner.lastIndexOf('.'));
        if (!normalized(parsed.getPackageName()).equals(packageName) || parsed.getTypeDecls().size() != 1) {
            throw new IOException("Unexamined quest authority package/type; claim binding not evaluated.");
        }
        for (var statement : parsed.getImports()) {
            String imported = statement.getQualifiedIdentifier().toString();
            String simple = imported.substring(imported.lastIndexOf('.') + 1);
            if (statement.isStatic() || simple.equals("*")
                    || bindings.containsKey(simple) && !bindings.get(simple).equals(imported)) {
                throw new IOException("Unexamined quest authority import " + imported + "; claim binding not evaluated.");
            }
        }
        Set<String> required = switch (name) {
            case "QuestBookItem" -> Set.of("Item", "Level", "Player", "InteractionHand", "InteractionResultHolder",
                    "ItemStack", "ServerPlayer", "QuestBackendPlatform", "MenuRegistry", "SimpleMenuProvider",
                    "QuestBookMenu", "Component");
            case "QuestBookMenu" -> Set.of("AbstractContainerMenu", "Inventory", "SimpleContainerData", "ModMenus",
                    "ModItems",
                    "Player", "ServerPlayer", "QuestClaimService", "QuestCatalogue", "QuestProgress", "QuestLogic",
                    "QuestEvidence", "QuestInventoryEvidence", "Set", "List");
            case "QuestCatalogue" -> Set.of("FluxTier", "ArrayList", "List", "Set");
            case "QuestClaimService" -> Set.of("Player", "ServerPlayer", "ModItems", "QuestProgress", "QuestCatalogue",
                    "QuestLogic", "QuestEvidence", "QuestInventoryEvidence", "QuestRewardDelivery", "UUID", "Function",
                    "Predicate", "Set", "List", "ArrayList", "Optional", "Runnable", "ItemStack", "Items",
                    "BuiltInRegistries", "ResourceLocation", "Grindless", "Component", "IllegalStateException", "DevTelemetry");
            case "QuestRewardDelivery" -> Set.of("Inventory", "ItemStack", "SimpleContainer", "List", "Optional",
                    "Runnable");
            case "QuestBookAccess" -> Set.of("ServerPlayer", "QuestProgress", "QuestCatalogue", "QuestRewardDelivery",
                    "ModItems", "Inventory", "Item", "ItemStack", "Component", "UUID", "List", "Optional", "Runnable",
                    "IllegalStateException");
            case "QuestBookAccessEvents" -> Set.of("Grindless", "QuestBookAccess", "ServerPlayer", "Component",
                    "TickEvent", "PlayerEvent", "ServerStoppedEvent", "SubscribeEvent", "Mod", "Set", "HashSet", "UUID");
            case "QuestLogic" -> Set.of("QuestCatalogue", "Set");
            case "QuestEvidence" -> Set.of("QuestCatalogue");
            case "QuestInventoryEvidence" -> Set.of("QuestCatalogue", "Container", "ItemStack",
                    "ControlMatrixItem", "BuiltInRegistries");
            case "QuestProgress" -> Set.of("ServerPlayer", "SavedData", "Map", "HashMap", "Set", "HashSet",
                    "UUID", "QuestCatalogue", "CompoundTag", "ListTag", "StringTag", "Tag", "IllegalArgumentException",
                    "IllegalStateException", "LevelResource", "Path", "Files", "BasicFileAttributes", "LinkOption",
                    "NoSuchFileException", "IOException", "SecurityException");
            case "QuestBackendPlatform" -> Set.of("ServerPlayer", "ExpectPlatform", "AssertionError");
            case "QuestBackendPlatformImpl" -> Set.of("ServerPlayer", "ModList", "FtbQuestIntegration", "HeraclesQuestIntegration");
            case "GrindlessForge" -> Set.of("Grindless", "IEventBus", "FMLJavaModLoadingContext", "EventBuses",
                    "MachineInspectionOverlay", "ModList", "Mod", "FtbQuestIntegration", "HeraclesQuestIntegration",
                    "MinecraftForge", "TagsUpdatedEvent", "MaterialRegistry", "RegistryTagView");
            case "GrindlessForgeClient" -> Set.of("Grindless", "GrindlessClient", "Mod", "ModList", "Dist",
                    "SubscribeEvent", "FMLClientSetupEvent", "HeraclesQuestClient");
            case "FtbQuestIntegration" -> Set.of("Grindless", "QuestProgress", "FtbQuestNative", "FtbQuestDefinitions",
                    "FTBQuestsAPI", "FtbMatrixFilter", "FtbQuestFiles", "FtbQuestChecks",
                    "IEventBus", "FMLCommonSetupEvent", "FMLPaths", "IOException",
                    "LifecycleEvent", "TeamEvent", "ObjectCompletedEvent", "MinecraftForge", "TickEvent",
                    "MinecraftServer", "ServerPlayer", "ServerQuestFile", "TeamData", "OpenQuestBookMessage", "Component",
                    "IllegalStateException", "IllegalArgumentException");
            case "FtbQuestNative" -> Set.of("BaseQuestFile", "Chapter", "Quest", "QuestObjectBase", "TeamData",
                    "ItemReward", "Reward", "CheckmarkTask", "DimensionTask", "ItemTask", "Task",
                    "FtbQuestDefinitions", "QuestCatalogue", "QuestProgress", "CompoundTag", "ListTag", "Tag", "Date",
                    "LinkedHashSet", "Set", "UUID", "System", "Long", "IllegalArgumentException", "IllegalStateException");
            case "FtbQuestDefinitions" -> Set.of("QuestCatalogue", "QuestProjection", "QuestTeaching", "FluxTier",
                    "FluidLogic", "SourceLogic", "ControlMatrixNbt", "FtbMatrixFilter",
                    "CompoundTag", "ListTag", "StringTag", "Component", "ByteBuffer",
                    "StandardCharsets", "MessageDigest", "NoSuchAlgorithmException", "Collections", "HashSet",
                    "LinkedHashMap", "List", "Locale", "Map", "Set", "Long", "Integer", "String",
                    "IllegalStateException", "IllegalArgumentException");
            case "FtbMatrixFilter" -> Set.of("ItemFilterAdapter", "ControlMatrixItem", "QuestCatalogue",
                    "BuiltInRegistries", "Tag", "TagKey", "Item", "ItemStack", "UnsupportedOperationException");
            case "HeraclesQuestNetwork" -> Set.of("SimpleChannel", "NetworkRegistry", "NetworkDirection", "ServerPlayer",
                    "Grindless", "DistExecutor", "Dist", "HeraclesQuestIntegration", "HeraclesQuestClient",
                    "PacketDistributor", "List", "QuestLogic");
            case "HeraclesQuestIntegration" -> Set.of("ServerPlayer", "QuestClaimService", "ModUtils",
                    "HeraclesDefinitions", "QuestProgress", "QuestInventoryEvidence", "QuestEvidence", "QuestLogic",
                    "QuestCatalogue", "QuestProgressHandler", "HeraclesClaimProjection", "HeraclesQuestNetwork",
                    "Component", "Set", "List", "Map", "HashMap", "UUID", "Quest");
            case "GrindlessClaimTask" -> Set.of("QuestTask", "QuestTaskType", "ServerPlayer", "NumericTag",
                    "QuestProgress", "BooleanTaskStorage", "Codec", "DataResult", "ResourceLocation",
                    "Grindless", "QuestCatalogue", "Set");
            default -> throw new IOException("Unexamined quest authority; claim binding not evaluated.");
        };
        for (String simple : required) {
            String qualified = bindings.get(simple);
            String targetPackage = qualified.substring(0, qualified.lastIndexOf('.'));
            List<String> imports = parsed.getImports().stream().map(statement ->
                            statement.getQualifiedIdentifier().toString())
                    .filter(imported -> imported.endsWith("." + simple)).toList();
            if (targetPackage.equals(packageName) || targetPackage.equals("java.lang")) {
                if (!imports.isEmpty() && !imports.equals(List.of(qualified))) {
                    throw new IOException("Unexamined local quest type " + simple + "; claim binding not evaluated.");
                }
            } else if (!imports.equals(List.of(qualified))) {
                throw new IOException("Unexamined quest API type " + simple + "; claim binding not evaluated.");
            }
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
