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
        physicalSourceCounterexamples(root);

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
        mechanicsSource(root, "machine", "ProcessMachineKind", hashes);
        mechanicsSource(root, "source", "SourceLogic", hashes);
        ClassTree worldSite = mechanicsSource(root, "source", "SourceSite", hashes);
        mechanicsSource(root, "fluid", "FluidLogic", hashes);
        mechanicsSource(root, "research", "ResearchLogic", hashes);
        ClassTree blocks = mechanicsSource(root, "registry", "ModBlocks", hashes);
        Map<String, Machine> constructions = new TreeMap<>();
        for (ProcessMachineKind kind : ProcessMachineKind.values()) {
            if (T0Sustainability.processFamilies().contains(kind.family().name())) {
                Resource controller = controller(blocks, kind.name(), "ProcessMachineBlock",
                        "ProcessMachineKind." + kind.name(), declared);
                constructions.put(kind.family().name(), new Machine(kind.family().name(), controller, 0));
            }
        }
        for (String[] binding : List.of(
                new String[]{"HAND_PUMP", "HandPumpBlock"},
                new String[]{"RESEARCH_TERMINAL", "ResearchTerminalBlock"},
                new String[]{"BIOMASS_DYNAMO", "ThermalGeneratorBlock", "GeneratorKind.BIOMASS"},
                new String[]{"HAND_CRANK_DYNAMO", "HandCrankDynamoBlock"})) {
            Resource controller = controller(blocks, binding[0], binding[1],
                    binding.length > 2 ? binding[2] : null, declared);
            constructions.put(binding[0], new Machine(binding[0], controller, 0));
        }

        eq("the real tool accepts only ProcessMachine/F0 recipients and server-side cooldown-limited actions",
                "{if(!(context.getLevel().getBlockEntity(context.getClickedPos())instanceofProcessMachineBlockEntitymachine)"
                        + "||machine.kind().tier()!=FluxTier.F0||context.getPlayer()==null){returnInteractionResult.PASS;}"
                        + "if(!context.getLevel().isClientSide()&&!context.getPlayer().getCooldowns().isOnCooldown(this)){"
                        + "machine.crankByHand();context.getPlayer().getCooldowns().addCooldown(this,SourceLogic.MANUAL_COOLDOWN);}"
                        + "returnInteractionResult.sidedSuccess(context.getLevel().isClientSide());}",
                normalized(method(crank, "useOn", 1).getBody()));
        eq("the real recipient stores the charge locally with its F0/server guard and capacity clamp",
                "{if(kind.tier()!=FluxTier.F0||getLevel()==null||getLevel().isClientSide()){return;}"
                        + "energy().setStored(Math.min(energy().getCapacity(),energy().getStored()+SourceLogic.MANUAL_CHARGE));}",
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
                        + "setChanged();updateSubscriptions();return;}publishStatus(true,false,powered,gained>0.0);}",
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
        eq("the physical Dynamo charge comes from its actual constant", true,
                normalized(method(dynamo, "crank", 0).getBody())
                        .startsWith("{energy().setStored(energy().getStored()+CHARGE_PER_CRANK);"));
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

    private static Resource controller(ClassTree blocks, String fieldName, String constructor,
                                       String kind, Map<Resource, String> declared) throws IOException {
        VariableTree field = field(blocks, fieldName);
        if (!(field.getInitializer() instanceof MethodInvocationTree register)
                || !normalized(register.getMethodSelect()).equals("register") || register.getArguments().size() != 2
                || !(register.getArguments().get(0) instanceof LiteralTree literal)
                || !(literal.getValue() instanceof String name)
                || !(register.getArguments().get(1) instanceof LambdaExpressionTree supplier)
                || !supplier.getParameters().isEmpty()
                || !(supplier.getBody() instanceof NewClassTree body)) {
            throw new IOException("Unsupported checked controller binding: ModBlocks." + fieldName);
        }
        eq("a checked controller constructs the actual implementing block: " + fieldName, constructor,
                normalized(body.getIdentifier()));
        if (kind != null) {
            eq("the registered block actually selects its declared process/generator kind: " + fieldName,
                    kind, normalized(body.getArguments().get(0)));
        }
        Resource resource = item("grindless:" + name);
        eq("a controller binding reuses the single verified declaration inventory: " + fieldName,
                true, declared.containsKey(resource));
        return resource;
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
