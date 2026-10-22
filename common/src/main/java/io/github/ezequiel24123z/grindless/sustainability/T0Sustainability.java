package io.github.ezequiel24123z.grindless.sustainability;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ManufacturingPolicy;
import io.github.ezequiel24123z.grindless.recipe.RenewableProcesses;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Authorized T0 declarations under ADR-0131/0133/0137/0141, not a second recipe catalogue.
 * Renewable labels are claims the auditor must prove; no declaration supplies an initial gift.
 */
public final class T0Sustainability {
    private static final Set<String> REQUIRED_QUESTS = Set.of("multitool", "quern", "water", "clay_mixer",
            "ceramics", "workshop_flow", "settling", "calibrate", "relay");
    private static final Set<String> OPTIONAL_QUESTS = Set.of(QuestCatalogue.STARTER_SUPPLIES, "crank", "extractor");
    private static final Set<Resource> ACQUIRED_ENTRY = Set.of(item("minecraft:cobblestone"),
            item("minecraft:oak_planks"), item("minecraft:stick"), item("minecraft:brick"));
    private static final String PROFILE = "t0_native_sources_conditional_entry_v1";

    private T0Sustainability() {
    }

    /** These are manual construction crafts, never fuel-free furnace transformations. */
    public static Set<String> vanillaAllowlist() {
        return Set.of("minecraft:crafting_table", "minecraft:furnace", "minecraft:bricks");
    }

    /** Approved process families come from the actual T0 catalogues, not their FU rating. */
    public static Set<String> processFamilies() {
        Set<String> families = new TreeSet<>(Set.of(MachineFamily.ASSEMBLY_BENCH.name()));
        BootstrapProcesses.recipes().forEach(recipe -> families.add(recipe.family().name()));
        RenewableProcesses.recipes().forEach(recipe -> families.add(recipe.family().name()));
        return sortedSet(families);
    }

    /** Adds finite ordinary custody assumptions, without inventing acquisition yields or kit quantities. */
    public static Snapshot withEntrySources(Snapshot authorities) {
        Builder builder = new Builder(authorities);
        for (Resource resource : ACQUIRED_ENTRY) {
            builder.terminal(new Terminal("entry/acquired/" + resource.id(), resource, TerminalKind.MANUAL_WORLD,
                    null, false, false, null, 0,
                    "Conditional finite player-acquired starter stock, not a world or quest gift. Boards, sticks "
                            + "and first bricks may already have been manually crafted or smelted; their acquisition "
                            + "is an explicit entry assumption, not a renewable route or a promised spawn inventory.",
                    "ADR-0141 conditional entry + BootstrapRecipes/WorkshopStructure actual establishment draw"));
        }
        Snapshot bound = builder.build();
        boolean sourcesBound = bound.inputHashes().containsKey("mechanics_binding:CrankHandle_ProcessMachine_F0")
                && bound.terminals().values().stream().anyMatch(terminal ->
                terminal.kind() == TerminalKind.MANUAL_LABOUR && terminal.guaranteed() == null)
                && Set.of(SourceSite.WATER_BODY, SourceSite.GEOLOGICAL_ROCK_BED, SourceSite.SUNLIT_CULTURE).stream()
                .allMatch(site -> bound.terminals().values().stream().anyMatch(terminal ->
                        terminal.site() == site && terminal.unlockTier() == 0 && terminal.renewable()));
        if (!sourcesBound) {
            return bound;
        }
        return new Snapshot(bound.resources(), bound.routes(), bound.terminals(), bound.tags(), bound.objectives(),
                bound.inputHashes(), bound.findings().stream().filter(finding ->
                !finding.fingerprint().equals("bootstrap_sources_unknown:manual_world_and_labour")).toList());
    }

    public static EntryProfile entryTemplate(ManualWork work) {
        return new EntryProfile(PROFILE, Map.of(), 0, work, Set.of(
                "The player normally acquired the finite starter materials in the derived bill; none are guaranteed gifts.",
                "The real Crank Handle is constructed and only server-side eligible F0 ProcessMachines receive local work.",
                "The Hand Pump has an adjacent or below WATER-tagged source; geological and bioculture sites satisfy "
                        + "their actual terrestrial/rock/open-sky/light predicates while used.",
                "The witness is conditional material/work establishment, not observed survival, client crafting, "
                        + "night/restart behavior or saved-world compatibility."),
                "ADR-0141 + current source-bound mechanics + actual crafting/process/quest authorities");
    }

    public static SustainabilityAnnotations annotations(Snapshot snapshot, SustainabilityAnnotations sourceFacts,
                                                        ManualWork work, EntryProfile profile) {
        Map<String, RoutePolicy> policies = new TreeMap<>(sourceFacts.routes());
        BootstrapProcesses.recipes().forEach(recipe -> unlock(policies, recipe.id(), false));
        RenewableProcesses.recipes().forEach(recipe -> unlock(policies, recipe.id(), false));
        for (Route route : snapshot.routes().values()) {
            if (MachineFamily.ASSEMBLY_BENCH.name().equals(route.family())) {
                unlock(policies, route.id(), false);
            }
        }
        BootstrapRecipes.shaped().forEach(craft -> unlock(policies, "craft/" + craft.name(), false));
        BootstrapRecipes.shapeless().forEach(craft -> unlock(policies, "craft/" + craft.name(), false));
        vanillaAllowlist().forEach(id -> unlock(policies, "vanilla/" + id, false));
        unlock(policies, "source/hand_pump", false);
        unlock(policies, "calibrate/data_core", false);
        unlock(policies, "fuel/biomass_dynamo/biomass", false);
        unlock(policies, "craft/relay_matrix", false);
        REQUIRED_QUESTS.forEach(id -> unlock(policies, "quest/" + id, false));
        OPTIONAL_QUESTS.forEach(id -> unlock(policies, "quest/" + id, true));
        policies.put("assemble/thermal_generator_core", new RoutePolicy(1, true, Set.of()));
        policies.put("craft/thermal_generator_from_core", new RoutePolicy(1, true, Set.of()));

        Map<String, Machine> machines = new TreeMap<>(sourceFacts.machines());
        machines.put("CRAFTING_TABLE", new Machine("CRAFTING_TABLE", item("minecraft:crafting_table"), 0));
        Set<Resource> used = uses(snapshot, policies, machines);
        Map<Resource, ResourceMetadata> metadata = new TreeMap<>();
        Map<Resource, String> renewable = new TreeMap<>();
        renewable.put(new Resource(Kind.FLUID, "minecraft:water"), "source/hand_pump");
        renewable.put(ENERGY, "fuel/biomass_dynamo/biomass");
        renewable.put(item(SourceLogic.AGGREGATE), "source/geological_aggregate");
        renewable.put(item("minecraft:clay_ball"), "source/geological_aggregate");
        renewable.put(item(SourceLogic.BIOMASS), "source/biomass");
        renewable.put(item("minecraft:cobblestone"), "bootstrap/dry_aggregate");
        renewable.put(item("minecraft:gravel"), "bootstrap/dry_aggregate");
        renewable.put(item("minecraft:dirt"), "bootstrap/wet_aggregate");
        renewable.put(item("minecraft:redstone"), "bootstrap/wet_aggregate");
        renewable.put(item("minecraft:raw_iron"), "bootstrap/wet_aggregate");
        renewable.put(item("minecraft:raw_copper"), "bootstrap/wet_aggregate");
        renewable.put(tag("forge:raw_materials/iron"), "bootstrap/wet_aggregate");
        renewable.put(tag("forge:raw_materials/copper"), "bootstrap/wet_aggregate");
        renewable.put(item("minecraft:sand"), "bootstrap/sand");
        renewable.put(item("minecraft:flint"), "bootstrap/flint_separation");
        renewable.put(item("minecraft:charcoal"), "bootstrap/biocarbon");
        renewable.put(tag("grindless:carbon"), "bootstrap/biocarbon");
        renewable.put(item(SourceLogic.FIBREBOARD), "bootstrap/fibreboard");
        renewable.put(tag("grindless:boards"), "bootstrap/fibreboard");
        renewable.put(item("minecraft:stick"), "bootstrap/sticks");
        renewable.put(item("minecraft:paper"), "bootstrap/paper");
        renewable.put(item(BootstrapProcesses.UNFIRED), "bootstrap/sand_body");
        renewable.put(item(BootstrapProcesses.INSULATOR), "bootstrap/insulators");
        renewable.put(item(BootstrapProcesses.GROG), "bootstrap/insulators");
        renewable.put(item("minecraft:brick"), "bootstrap/bricks");
        renewable.put(item("minecraft:glass"), "bootstrap/glass");
        renewable.put(item("minecraft:iron_ingot"), "bootstrap/reduce_iron");
        renewable.put(item("minecraft:copper_ingot"), "bootstrap/reduce_copper");
        renewable.put(tag("forge:ingots/iron"), "bootstrap/reduce_iron");
        renewable.put(tag("forge:ingots/copper"), "bootstrap/reduce_copper");
        renewable.put(tag("minecraft:stone_crafting_materials"), "bootstrap/dry_aggregate");
        renewable.put(item(ResearchLogic.DATA_CORE), "bench/data_core");
        renewable.put(item(ResearchLogic.CALIBRATED_DATA_CORE), "calibrate/data_core");
        renewable.put(item("grindless:slag"), "bootstrap/reduce_copper");
        for (Route route : snapshot.routes().values()) {
            if (MachineFamily.ASSEMBLY_BENCH.name().equals(route.family())) {
                route.retainedOutputs().keySet().stream()
                        .filter(resource -> !ManufacturingPolicy.isManualFinalMachine(resource.id()))
                        .forEach(resource -> renewable.putIfAbsent(resource, route.id()));
            }
        }
        renewable.forEach((resource, route) -> metadata.put(resource, new ResourceMetadata(ResourceClass.RENEWABLE,
                null, used.contains(resource) ? 0 : null, 0, route, List.of(), null, null, null, null)));
        for (Machine machine : machines.values()) {
            Resource capital = machine.construction();
            if (ManufacturingPolicy.isManualFinalMachine(capital.id())) {
                // Select the actual existing manual authority, never invent a construction bill.
                String id = "craft/" + capital.id().substring(capital.id().indexOf(':') + 1);
                Route craft = snapshot.routes().get(id);
                String canonical = craft != null && !craft.automatable() && !craft.oneShot()
                        && craft.retainedOutputs().containsKey(capital) ? id : null;
                metadata.put(capital, new ResourceMetadata(ResourceClass.MANUAL_CAPITAL, null, 0, null,
                        canonical, List.of(), new Handling(Action.STORE, true, false, null, null), null, null,
                        "Manual final assembly establishes durable capital; its operating outputs, not the "
                                + "machine item itself, may have automatic renewable supply."));
            }
        }
        Resource grog = item(BootstrapProcesses.GROG);
        metadata.put(grog, new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.BYPRODUCT_CURRENT,
                0, 0, renewable.get(grog), List.of(), new Handling(Action.REUSE, true, false, null, null),
                null, null, null));
        Resource water = new Resource(Kind.FLUID, "minecraft:water");
        metadata.put(water, new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.BYPRODUCT_CURRENT,
                0, 0, renewable.get(water), List.of(), new Handling(Action.REUSE, true, false, null, null),
                null, null, null));
        Resource slag = item("grindless:slag");
        metadata.put(slag, new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.POTENTIAL_FUTURE_USE,
                null, 0, renewable.get(slag), List.of(), new Handling(Action.DISCARD, true, false, null, null),
                null, null, "The implemented ceramic/refractory_brick ARC_FURNACE consumer is a prototype with "
                + "no accepted future use tier. Ordinary Item discard is allowed; no automatic disposal, recycling "
                + "or numbered future plan is claimed."));
        Resource flint = item("minecraft:flint");
        metadata.put(flint, new ResourceMetadata(ResourceClass.RENEWABLE, SideStreamClass.BYPRODUCT_OPTIONAL,
                null, 0, renewable.get(flint), List.of(), new Handling(Action.STORE, true, false, null, null),
                null, null, "Optional T0 side stock with no accepted T0 consumer. The implemented pulverizer "
                + "craft and vanilla compatibility do not establish a first-use tier or a mandatory root."));
        for (Resource resource : List.of(item("minecraft:oak_planks"), tag("minecraft:planks"))) {
            metadata.put(resource, new ResourceMetadata(ResourceClass.BOOTSTRAP, null, 0, null,
                    null, List.of(), null, null, null, null));
        }
        Resource exit = snapshot.objectives().get(QuestCatalogue.T0_COMPLETION).subject();
        metadata.put(exit, new ResourceMetadata(ResourceClass.BOOTSTRAP, null, 0, null,
                "craft/relay_matrix", List.of(), null, null, null, null));
        metadata.put(item(FabricationLogic.THERMAL_GENERATOR_CORE), new ResourceMetadata(null, null, 1, null,
                "assemble/thermal_generator_core", List.of(), new Handling(Action.STORE, true, false, null, null),
                null, null, "Assembler component consumed by manual craft/thermal_generator_from_core. "
                + "Automatic net supply and renewable establishment are unproved."));
        for (Terminal terminal : snapshot.terminals().values()) {
            if (terminal.kind() == TerminalKind.ENVIRONMENT && terminal.unlockTier() == 0
                    && Set.of("environment/water_source", "environment/geological_bed",
                    "environment/bioculture_sunlight").contains(terminal.id())) {
                metadata.put(terminal.resource(), new ResourceMetadata(ResourceClass.RENEWABLE, null,
                        0, 0, null, List.of(), null, null, null, null));
            }
        }
        metadata.put(work.tool(), new ResourceMetadata(ResourceClass.RENEWABLE, null, 0, 0,
                "bench/crank_handle", List.of(), null, null, null, null));
        Terminal labour = snapshot.terminals().get(work.terminal());
        if (labour != null && labour.kind() == TerminalKind.MANUAL_LABOUR) {
            metadata.put(labour.resource(), new ResourceMetadata(ResourceClass.BOOTSTRAP, null, 0, null,
                    null, List.of(), null, null, null,
                    "Finite eligible batch-local startup actions, never portable ENERGY or renewable labour."));
        }
        Map<String, ClosedLoop> loops = Map.of("t0_geology_bioculture_biomass_power",
                new ClosedLoop("t0_geology_bioculture_biomass_power", item(SourceLogic.AGGREGATE),
                        Set.of("source/geological_aggregate", "source/biomass", "fuel/biomass_dynamo/biomass"), null));
        return new SustainabilityAnnotations(metadata, policies, machines, loops, Map.of(),
                Map.of(0, new TierPlan(0, Set.of(ENERGY), REQUIRED_QUESTS, exit, null, profile)));
    }

    private static Set<Resource> uses(Snapshot snapshot, Map<String, RoutePolicy> policies,
                                      Map<String, Machine> machines) {
        Set<Resource> used = new TreeSet<>();
        for (Route route : snapshot.routes().values()) {
            RoutePolicy policy = policies.get(route.id());
            if (policy != null && policy.unlockTier() != null && policy.unlockTier() == 0) {
                used.addAll(route.inputs().keySet());
                used.addAll(route.catalysts().keySet());
            }
        }
        machines.values().forEach(machine -> used.add(machine.construction()));
        for (Resource resource : new TreeSet<>(used)) {
            TagResolution tag = snapshot.tags().get(resource);
            if (tag != null && tag.complete()) {
                used.addAll(tag.members());
            }
        }
        return used;
    }

    private static void unlock(Map<String, RoutePolicy> policies, String route, boolean optional) {
        RoutePolicy facts = policies.get(route);
        policies.put(route, new RoutePolicy(0, optional, facts == null ? Set.of() : facts.environmentalSources(),
                facts == null ? Map.of() : facts.outputRoles()));
    }

    private static Resource item(String id) {
        return new Resource(Kind.ITEM, id);
    }

    private static Resource tag(String id) {
        return new Resource(Kind.TAG, id);
    }
}
