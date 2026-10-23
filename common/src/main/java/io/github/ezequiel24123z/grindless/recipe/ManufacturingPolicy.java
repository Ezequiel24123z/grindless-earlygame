package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ADR-0188's final-machine boundary, not a recipe or sustainability catalogue.
 *
 * <p>These explicit identities correspond to the actual ModBlocks/ModBlockEntities bindings.
 * Process controllers use the same WorkshopStructure declaration as ProcessMachineKind.
 * NONE and a SourceSite predicate are not formation. Non-process exceptions are grounded in
 * GroundArrayStructure and the actual transfer/storage implementations, not names or geometry.
 * Registered spatial prototypes are classified without granting them a survival recipe.
 * External products have no inferred machine role; OTHER is not a declaration of an allowed
 * manufacturing method. The coordinator's scoped no-implicit-exception decision also includes
 * the vanilla Furnace: it is a real processor, not an automatic component exception.
 * That conservative decision is not an owner-confirmed Furnace-specific choice.
 *
 * <p>No native registries or loader initialization: graph rebuilding and metadata inspection
 * must remain usable before Minecraft bootstrap. Structural checks bind this list to runtime.
 */
public final class ManufacturingPolicy {
    public enum Kind {
        NORMAL_MACHINE, FORMED_CONTROLLER, INFRASTRUCTURE, OTHER
    }

    private ManufacturingPolicy() {
    }

    public static boolean isManualFinalMachine(String itemId) {
        return kind(itemId) == Kind.NORMAL_MACHINE;
    }

    /** Classification only. Manufacturing methods for other products come from their real routes. */
    public static Kind kind(String itemId) {
        if (itemId == null) {
            return Kind.OTHER;
        }
        MachineFamily process = switch (itemId) {
            case "grindless:quern" -> MachineFamily.QUERN;
            case "grindless:clay_mixer" -> MachineFamily.CLAY_MIXER;
            case "grindless:firing_chamber" -> MachineFamily.FIRING_CHAMBER;
            case "grindless:settling_trough" -> MachineFamily.SETTLING_TROUGH;
            case "grindless:geological_collector" -> MachineFamily.GEOLOGICAL_COLLECTOR;
            case "grindless:biocultivator" -> MachineFamily.BIOCULTIVATOR;
            case "grindless:assembly_bench" -> MachineFamily.ASSEMBLY_BENCH;
            case "grindless:pulverizer" -> MachineFamily.PULVERIZER;
            case "grindless:arc_furnace" -> MachineFamily.ARC_FURNACE;
            case "grindless:press" -> MachineFamily.PRESS;
            case "grindless:assembler" -> MachineFamily.ASSEMBLER;
            case "grindless:kiln" -> MachineFamily.KILN;
            case "grindless:wire_mill" -> MachineFamily.WIRE_MILL;
            case "grindless:chemical_reactor" -> MachineFamily.CHEMICAL_REACTOR;
            case "grindless:chemical_washer" -> MachineFamily.CHEMICAL_WASHER;
            case "grindless:electrolysis_cell" -> MachineFamily.ELECTROLYSIS_CELL;
            case "grindless:atmospheric_intake" -> MachineFamily.ATMOSPHERIC_INTAKE;
            case "grindless:induction_furnace" -> MachineFamily.INDUCTION_FURNACE;
            case "grindless:caster" -> MachineFamily.CASTER;
            case "grindless:flotation_cell" -> MachineFamily.FLOTATION;
            case "grindless:boiler" -> MachineFamily.BOILER;
            case "grindless:condenser" -> MachineFamily.CONDENSER;
            default -> null;
        };
        if (process != null) {
            return WorkshopStructure.forFamily(process) == WorkshopStructure.NONE
                    ? Kind.NORMAL_MACHINE : Kind.FORMED_CONTROLLER;
        }
        return switch (itemId) {
            case "minecraft:furnace", "grindless:hand_crank_dynamo", "grindless:crude_extractor",
                    "grindless:research_terminal", "grindless:biomass_dynamo",
                    "grindless:thermal_generator", "grindless:solar_array",
                    "grindless:terrestrial_extractor", "grindless:fluid_well",
                    "grindless:hand_pump", "grindless:electric_pump",
                    "grindless:logic_controller", "grindless:pattern_scanner",
                    "grindless:deconstructor", "grindless:factory_portal",
                    "grindless:lunar_link", "grindless:starward_link" -> Kind.NORMAL_MACHINE;
            // The only additional runtime-formed controller; links/pads are not formed arrays.
            case "grindless:ground_array" -> Kind.FORMED_CONTROLLER;
            // Routing does not transform the carried product. The inline magnet is a BeltEndpoint.
            case "grindless:conveyor_belt", "grindless:splitter", "grindless:merger",
                    "grindless:tunnel_belt", "grindless:overflow_gate", "grindless:sorter",
                    "grindless:magnetic_separator", "grindless:crude_manipulator",
                    "grindless:fluid_manipulator", "grindless:flux_belt",
                    "grindless:stack_manipulator", "grindless:filter_manipulator",
                    "grindless:clay_conduit", "grindless:pressure_pipe",
                    "grindless:basic_tank", "grindless:industrial_tank",
                    "grindless:flux_pylon_mk1", "grindless:flux_pylon_mk2",
                    "grindless:flux_pylon_mk3", "grindless:flux_conduit",
                    "grindless:capacitor_bank", "grindless:flux_transformer",
                    "grindless:signal_cable", "grindless:redstone_interface",
                    "grindless:settling_channel", "grindless:settling_screen",
                    "grindless:workshop_item_input", "grindless:workshop_item_output",
                    "grindless:settling_item_input", "grindless:settling_item_output",
                    "grindless:settling_fluid_input", "grindless:settling_fluid_output",
                    "grindless:array_casing", "grindless:launch_pad",
                    "grindless:station_berth" -> Kind.INFRASTRUCTURE;
            default -> Kind.OTHER;
        };
    }

    /** Declared normal products, including the scoped vanilla Furnace; no construction routes. */
    public static Set<String> manualFinalMachines() {
        return Stream.concat(BlockCatalogue.blocks().stream().map(block -> "grindless:" + block.name()),
                        Stream.of("minecraft:furnace"))
                .filter(ManufacturingPolicy::isManualFinalMachine).collect(Collectors.toUnmodifiableSet());
    }
}
