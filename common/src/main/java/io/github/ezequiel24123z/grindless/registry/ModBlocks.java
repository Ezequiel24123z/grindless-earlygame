package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.centre.ArrivalMarkBlock;
import io.github.ezequiel24123z.grindless.centre.CentreCatalogue;
import io.github.ezequiel24123z.grindless.centre.HorizonShellBlock;
import io.github.ezequiel24123z.grindless.flight.LaunchPadBlock;
import io.github.ezequiel24123z.grindless.fluid.BasicTankBlock;
import io.github.ezequiel24123z.grindless.fluid.ClayConduitBlock;
import io.github.ezequiel24123z.grindless.fluid.ElectricPumpBlock;
import io.github.ezequiel24123z.grindless.fluid.FluidManipulatorBlock;
import io.github.ezequiel24123z.grindless.fluid.FluidWellBlock;
import io.github.ezequiel24123z.grindless.fluid.HandPumpBlock;
import io.github.ezequiel24123z.grindless.fluid.IndustrialTankBlock;
import io.github.ezequiel24123z.grindless.fluid.PressurePipeBlock;
import io.github.ezequiel24123z.grindless.belt.BeltBlock;
import io.github.ezequiel24123z.grindless.belt.FilterManipulatorBlock;
import io.github.ezequiel24123z.grindless.belt.FluxBeltBlock;
import io.github.ezequiel24123z.grindless.belt.MagneticSeparatorBlock;
import io.github.ezequiel24123z.grindless.belt.ManipulatorBlock;
import io.github.ezequiel24123z.grindless.belt.StackManipulatorBlock;
import io.github.ezequiel24123z.grindless.belt.MergerBlock;
import io.github.ezequiel24123z.grindless.belt.OverflowGateBlock;
import io.github.ezequiel24123z.grindless.belt.SorterBlock;
import io.github.ezequiel24123z.grindless.belt.SplitterBlock;
import io.github.ezequiel24123z.grindless.belt.TunnelBeltBlock;
import io.github.ezequiel24123z.grindless.pattern.DeconstructorBlock;
import io.github.ezequiel24123z.grindless.pattern.PatternScannerBlock;
import io.github.ezequiel24123z.grindless.logic.LogicControllerBlock;
import io.github.ezequiel24123z.grindless.logic.RedstoneInterfaceBlock;
import io.github.ezequiel24123z.grindless.logic.SignalCableBlock;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlock;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlock;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlock;
import io.github.ezequiel24123z.grindless.machine.SolarArrayBlock;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.machine.ResearchTerminalBlock;
import io.github.ezequiel24123z.grindless.machine.TerrestrialExtractorBlock;
import io.github.ezequiel24123z.grindless.machine.ThermalGeneratorBlock;
import io.github.ezequiel24123z.grindless.network.CapacitorBankBlock;
import io.github.ezequiel24123z.grindless.planet.LunarLinkBlock;
import io.github.ezequiel24123z.grindless.planet.LunarRegolithBlock;
import io.github.ezequiel24123z.grindless.star.DriftDeckBlock;
import io.github.ezequiel24123z.grindless.star.StarwardLinkBlock;
import io.github.ezequiel24123z.grindless.station.StationBerthBlock;
import io.github.ezequiel24123z.grindless.structure.ArrayCasingBlock;
import io.github.ezequiel24123z.grindless.structure.GroundArrayBlock;
import io.github.ezequiel24123z.grindless.network.FluxTransformerBlock;
import io.github.ezequiel24123z.grindless.network.PylonBlock;
import io.github.ezequiel24123z.grindless.network.PylonShaftBlock;
import io.github.ezequiel24123z.grindless.network.PylonTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * Blocks. Every entry registers its own {@link BlockItem} into {@link ModItems}, so the two
 * registers stay in step and nothing can ship a block that cannot be picked up.
 *
 * <p>T0 through Kiln / R2 and the T2 Wire Mill. Dynamo, extractors, pylons, terminal,
 * Thermal Generator, process machines, belts, fluids, the T1 extractor, capacitor bank,
 * transformer, Kiln and Wire Mill have block entities.
 */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Grindless.MOD_ID, Registries.BLOCK);

    /** T0 manual generator. The first source of Flux Units, and the end of the grind. */
    public static final RegistrySupplier<HandCrankDynamoBlock> HAND_CRANK_DYNAMO =
            register("hand_crank_dynamo",
                    () -> new HandCrankDynamoBlock(machine().strength(2.0F)));

    /** T0 extractor. Slow and cheap, but it never needs a tunnel. */
    public static final RegistrySupplier<CrudeExtractorBlock> CRUDE_EXTRACTOR = register("crude_extractor",
            () -> new CrudeExtractorBlock(machine().strength(2.5F)));

    /** T0 physical calibration. Turns a Data Core and F0 Flux into a Calibrated Data Core. */
    public static final RegistrySupplier<ResearchTerminalBlock> RESEARCH_TERMINAL =
            register("research_terminal",
                    () -> new ResearchTerminalBlock(machine().strength(3.0F)));

    /** T1 walk-away power: furnace fuel at F1. */
    public static final RegistrySupplier<ThermalGeneratorBlock> THERMAL_GENERATOR =
            register("thermal_generator",
                    () -> new ThermalGeneratorBlock(machine().strength(3.0F)));

    /** T1 dry mill. B1: 1 raw → 2 crushed. */
    public static final RegistrySupplier<ProcessMachineBlock> PULVERIZER =
            register("pulverizer",
                    () -> new ProcessMachineBlock(ProcessMachineKind.PULVERIZER, machine().strength(3.0F)));

    /** T1 carbothermic reduction. R1: feed + carbon → ingot + slag; CO vents. */
    public static final RegistrySupplier<ProcessMachineBlock> ARC_FURNACE =
            register("arc_furnace",
                    () -> new ProcessMachineBlock(ProcessMachineKind.ARC_FURNACE, machine().strength(3.5F)));

    /** T1 forming. One ingot and a die; the die is not consumed. */
    public static final RegistrySupplier<ProcessMachineBlock> PRESS =
            register("press",
                    () -> new ProcessMachineBlock(ProcessMachineKind.PRESS, machine().strength(3.0F)));

    /** T1 fabrication. The last crafting-table machine; T2+ is manufactured here. */
    public static final RegistrySupplier<ProcessMachineBlock> ASSEMBLER =
            register("assembler",
                    () -> new ProcessMachineBlock(ProcessMachineKind.ASSEMBLER, machine().strength(3.5F)));

    /** T1 roast. 1 u feed → 1 u oxide; 1 B SO₂ vents or captures. */
    public static final RegistrySupplier<ProcessMachineBlock> KILN =
            register("kiln",
                    () -> new ProcessMachineBlock(ProcessMachineKind.KILN, machine().strength(3.0F)));

    /** T2 drawing. 1 ingot → 2 wire; 2 copper wire → 1 coil. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> WIRE_MILL =
            register("wire_mill",
                    () -> new ProcessMachineBlock(ProcessMachineKind.WIRE_MILL, machine().strength(3.5F)));

    /** T2 contact process. SO₂ → SO₃ → sulfuric acid. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> CHEMICAL_REACTOR =
            register("chemical_reactor",
                    () -> new ProcessMachineBlock(ProcessMachineKind.CHEMICAL_REACTOR, machine().strength(3.5F)));

    /** T2 wet line. Crushed and water become washed crushed plus a vein byproduct. */
    public static final RegistrySupplier<ProcessMachineBlock> CHEMICAL_WASHER =
            register("chemical_washer",
                    () -> new ProcessMachineBlock(ProcessMachineKind.CHEMICAL_WASHER, machine().strength(3.5F)));

    /** T2 electrolysis. 2 B water → 2 B hydrogen + 1 B oxygen. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> ELECTROLYSIS_CELL =
            register("electrolysis_cell",
                    () -> new ProcessMachineBlock(ProcessMachineKind.ELECTROLYSIS_CELL, machine().strength(3.5F)));

    /** T2 air skim. Free air → 2 B oxygen. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> ATMOSPHERIC_INTAKE =
            register("atmospheric_intake",
                    () -> new ProcessMachineBlock(ProcessMachineKind.ATMOSPHERIC_INTAKE, machine().strength(3.5F)));

    /** T2 aquifer. Powered chunk water. No vanilla source. Manufactured, not crafted. */
    public static final RegistrySupplier<FluidWellBlock> FLUID_WELL =
            register("fluid_well",
                    () -> new FluidWellBlock(machine().strength(3.5F)));

    /** T2 clean re-melt. 1 ingot → 144 mB molten. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> INDUCTION_FURNACE =
            register("induction_furnace",
                    () -> new ProcessMachineBlock(ProcessMachineKind.INDUCTION_FURNACE, machine().strength(3.5F)));

    /** T2 flotation. Crushed and surfactant become concentrate and tailings. */
    public static final RegistrySupplier<ProcessMachineBlock> FLOTATION_CELL =
            register("flotation_cell",
                    () -> new ProcessMachineBlock(ProcessMachineKind.FLOTATION, machine().strength(3.5F)));

    /** T2 caster. 144 mB molten and a mould → one solid form. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> CASTER =
            register("caster",
                    () -> new ProcessMachineBlock(ProcessMachineKind.CASTER, machine().strength(3.5F)));

    /** T1 unpowered conveyor. 8 items/s, two lanes, lane data not entities. */
    public static final RegistrySupplier<BeltBlock> CONVEYOR_BELT =
            register("conveyor_belt",
                    () -> new BeltBlock(machine().strength(1.5F)));

    /** Filter-plus-priority junction. Front, left and right. */
    public static final RegistrySupplier<SplitterBlock> SPLITTER =
            register("splitter",
                    () -> new SplitterBlock(machine().strength(2.0F)));

    /** Three inlets, one outlet. Round-robin. */
    public static final RegistrySupplier<MergerBlock> MERGER =
            register("merger",
                    () -> new MergerBlock(machine().strength(2.0F)));

    /** Entrance/exit pair. Skips one to five empty blocks. */
    public static final RegistrySupplier<TunnelBeltBlock> TUNNEL_BELT =
            register("tunnel_belt",
                    () -> new TunnelBeltBlock(machine().strength(1.5F)));

    /** Front until it backs up, then the clockwise side. */
    public static final RegistrySupplier<OverflowGateBlock> OVERFLOW_GATE =
            register("overflow_gate",
                    () -> new OverflowGateBlock(machine().strength(2.0F)));

    /** Inline filter. Matching sides peel; unmatched continue. */
    public static final RegistrySupplier<SorterBlock> SORTER =
            register("sorter",
                    () -> new SorterBlock(machine().strength(2.0F)));

    /** T2 magnet. Ferromagnetic items leave left; the rest continue. Manufactured, not crafted. */
    public static final RegistrySupplier<MagneticSeparatorBlock> MAGNETIC_SEPARATOR =
            register("magnetic_separator",
                    () -> new MagneticSeparatorBlock(machine().strength(2.0F)));

    /** T2 daylight. 32 FU/t while the sky is open. Manufactured, not crafted. */
    public static final RegistrySupplier<SolarArrayBlock> SOLAR_ARRAY =
            register("solar_array",
                    () -> new SolarArrayBlock(machine().strength(2.0F)));

    /** T2 boiler. 1 B water → 1 B steam. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> BOILER =
            register("boiler",
                    () -> new ProcessMachineBlock(ProcessMachineKind.BOILER, machine().strength(3.5F)));

    /** T2 condenser. 1 B steam → 1 B water. Manufactured, not crafted. */
    public static final RegistrySupplier<ProcessMachineBlock> CONDENSER =
            register("condenser",
                    () -> new ProcessMachineBlock(ProcessMachineKind.CONDENSER, machine().strength(3.5F)));

    /** Crude inserter. One item a second, unpowered. */
    public static final RegistrySupplier<ManipulatorBlock> CRUDE_MANIPULATOR =
            register("crude_manipulator",
                    () -> new ManipulatorBlock(machine().strength(2.0F)));

    /** T1 extractor. F1, five seconds per unit, surveyed chunks only. */
    public static final RegistrySupplier<TerrestrialExtractorBlock> TERRESTRIAL_EXTRACTOR =
            register("terrestrial_extractor",
                    () -> new TerrestrialExtractorBlock(machine().strength(3.5F)));

    /** T1 gravity pipe. Ambient liquids only. */
    public static final RegistrySupplier<ClayConduitBlock> CLAY_CONDUIT =
            register("clay_conduit",
                    () -> new ClayConduitBlock(machine().strength(1.5F)));

    /** T1 unpowered water source. */
    public static final RegistrySupplier<HandPumpBlock> HAND_PUMP =
            register("hand_pump",
                    () -> new HandPumpBlock(machine().strength(2.0F)));

    /** T1 tank. Unpressurised; refuses hot fluid. */
    public static final RegistrySupplier<BasicTankBlock> BASIC_TANK =
            register("basic_tank",
                    () -> new BasicTankBlock(machine().strength(2.5F)));

    /** T2 pipe. Steam and melt, gas and uphill. Manufactured, not crafted. */
    public static final RegistrySupplier<PressurePipeBlock> PRESSURE_PIPE =
            register("pressure_pipe",
                    () -> new PressurePipeBlock(machine().strength(2.0F)));

    /** T2 pump. Moves rated fluid. Does not summon water. Manufactured, not crafted. */
    public static final RegistrySupplier<ElectricPumpBlock> ELECTRIC_PUMP =
            register("electric_pump",
                    () -> new ElectricPumpBlock(machine().strength(2.5F)));

    /** T2 tank. 64 B at the steam and melt rating. One block. Manufactured, not crafted. */
    public static final RegistrySupplier<IndustrialTankBlock> INDUSTRIAL_TANK =
            register("industrial_tank",
                    () -> new IndustrialTankBlock(machine().strength(3.0F)));

    /** T2 fluid inserter. One bucket a second. Manufactured, not crafted. */
    public static final RegistrySupplier<FluidManipulatorBlock> FLUID_MANIPULATOR =
            register("fluid_manipulator",
                    () -> new FluidManipulatorBlock(machine().strength(2.0F)));

    /** T2 belt. 16 items/s while it spends LV. Manufactured, not crafted. */
    public static final RegistrySupplier<FluxBeltBlock> FLUX_BELT =
            register("flux_belt",
                    () -> new FluxBeltBlock(machine().strength(2.0F)));

    /** T2 inserter. Twelve items a second. Manufactured, not crafted. */
    public static final RegistrySupplier<StackManipulatorBlock> STACK_MANIPULATOR =
            register("stack_manipulator",
                    () -> new StackManipulatorBlock(machine().strength(2.0F)));

    /** T2 inserter. One whitelist. Manufactured, not crafted. */
    public static final RegistrySupplier<FilterManipulatorBlock> FILTER_MANIPULATOR =
            register("filter_manipulator",
                    () -> new FilterManipulatorBlock(machine().strength(2.0F)));

    /** T2 signal wire. One integer, one direction. Manufactured, not crafted. */
    public static final RegistrySupplier<SignalCableBlock> SIGNAL_CABLE =
            register("signal_cable",
                    () -> new SignalCableBlock(machine().strength(1.5F)));

    /** T2 controller. Holds a machine at 500 items. Manufactured, not crafted. */
    public static final RegistrySupplier<LogicControllerBlock> LOGIC_CONTROLLER =
            register("logic_controller",
                    () -> new LogicControllerBlock(machine().strength(2.5F)));

    /** T2 bridge between a signal cable and vanilla redstone. Manufactured, not crafted. */
    public static final RegistrySupplier<RedstoneInterfaceBlock> REDSTONE_INTERFACE =
            register("redstone_interface",
                    () -> new RedstoneInterfaceBlock(machine().strength(2.0F)));

    /** T2 scanner. Stores an item id and reports its replication cost. Manufactured, not crafted. */
    public static final RegistrySupplier<PatternScannerBlock> PATTERN_SCANNER =
            register("pattern_scanner",
                    () -> new PatternScannerBlock(machine().strength(2.5F)));

    /** T2 sink. One item becomes one Matter. Manufactured, not crafted. */
    public static final RegistrySupplier<DeconstructorBlock> DECONSTRUCTOR =
            register("deconstructor",
                    () -> new DeconstructorBlock(machine().strength(3.0F)));

    /** T1 storage. Adds capacity to the covering network; no supply cube of its own. */
    public static final RegistrySupplier<CapacitorBankBlock> CAPACITOR_BANK =
            register("capacitor_bank",
                    () -> new CapacitorBankBlock(machine().strength(3.0F)));

    /** T1 tap. Exchanges FU with the covering network at F1. Not a pylon. */
    public static final RegistrySupplier<FluxTransformerBlock> FLUX_TRANSFORMER =
            register("flux_transformer",
                    () -> new FluxTransformerBlock(machine().strength(3.0F)));

    /** Centre of the Ground Array. Storage, once eight casings stand (ADR-0094). */
    public static final RegistrySupplier<GroundArrayBlock> GROUND_ARRAY = register("ground_array",
            () -> new GroundArrayBlock(machine().strength(3.5F)));

    /** One of the eight blocks around a Ground Array. Not a machine. */
    public static final RegistrySupplier<ArrayCasingBlock> ARRAY_CASING = register("array_casing",
            () -> new ArrayCasingBlock(machine().strength(3.0F)));

    /**
     * A pad a survey rocket climbs from (ADR-0097). Not a link.
     */
    public static final RegistrySupplier<LaunchPadBlock> LAUNCH_PAD = register("launch_pad",
            () -> new LaunchPadBlock(machine().strength(3.5F)));

    /**
     * Departure to Luna. On Luna, the same block is the way home (ADR-0095).
     * Placeholder: the rocket replaces this flight. Not deleted (ADR-0097).
     */
    public static final RegistrySupplier<LunarLinkBlock> LUNAR_LINK = register("lunar_link",
            () -> new LunarLinkBlock(machine().strength(3.5F)));

    /**
     * Luna's surface. A full cube, so neighbours occlude. Not a machine silhouette.
     */
    public static final RegistrySupplier<LunarRegolithBlock> LUNAR_REGOLITH = register("lunar_regolith",
            () -> new LunarRegolithBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.GRAVEL)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    /**
     * A berth a supraluminal station climbs from (ADR-0098). Not a link.
     */
    public static final RegistrySupplier<StationBerthBlock> STATION_BERTH = register("station_berth",
            () -> new StationBerthBlock(machine().strength(3.5F)));

    /**
     * Departure to the Drift. On the Drift, the same block is the way home (ADR-0096).
     * Placeholder: the station replaces this hop. The block stays registered and no longer
     * moves a player (ADR-0098).
     */
    public static final RegistrySupplier<StarwardLinkBlock> STARWARD_LINK = register("starward_link",
            () -> new StarwardLinkBlock(machine().strength(3.5F)));

    /**
     * The Drift's floor. A full cube, so neighbours occlude. Plating, not a machine.
     */
    public static final RegistrySupplier<DriftDeckBlock> DRIFT_DECK = register("drift_deck",
            () -> new DriftDeckBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .sound(SoundType.METAL)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    /**
     * The mass of the galactic centre (ADR-0099). No item: it cannot be carried home,
     * and it cannot be broken, so the chamber stays one room.
     */
    public static final RegistrySupplier<HorizonShellBlock> HORIZON_SHELL =
            BLOCKS.register("horizon_shell", () -> new HorizonShellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.STONE)
                    .strength(-1.0F, 3600000.0F)));

    /**
     * The mark in that chamber. No item. It lights the room, because the dimension has
     * no skylight.
     */
    public static final RegistrySupplier<ArrivalMarkBlock> ARRIVAL_MARK =
            BLOCKS.register("arrival_mark", () -> new ArrivalMarkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.METAL)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> CentreCatalogue.MARK_LIGHT)));

    /** The three Flux Pylons. Power reaches a machine because it stands inside one's supply
     * area — there are no wires between pylons and machines. */
    public static final RegistrySupplier<PylonBlock> FLUX_PYLON_MK1 = register("flux_pylon_mk1",
            () -> new PylonBlock(PylonTier.MK1, machine().strength(3.0F)));

    public static final RegistrySupplier<PylonBlock> FLUX_PYLON_MK2 = register("flux_pylon_mk2",
            () -> new PylonBlock(PylonTier.MK2, machine().strength(3.5F)));

    public static final RegistrySupplier<PylonBlock> FLUX_PYLON_MK3 = register("flux_pylon_mk3",
            () -> new PylonBlock(PylonTier.MK3, machine().strength(4.0F)));

    /**
     * Occupies the two blocks above a pylon. No item: breaking it breaks the pylon, which drops.
     */
    public static final RegistrySupplier<PylonShaftBlock> FLUX_PYLON_SHAFT =
            BLOCKS.register("flux_pylon_shaft", () -> new PylonShaftBlock(machine().strength(3.0F)));

    private ModBlocks() {
    }

    /**
     * Shared base properties for machine blocks: metallic, pickaxe-mined, lit by their status.
     *
     * <p>{@code noOcclusion} because none of their models fill the cube; without it the faces of
     * every neighbouring block would be culled and the machine would look like a hole.
     */
    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(MachineStatus::lightOf);
    }

    /** Registers a block and its matching {@link BlockItem} under the same name. */
    public static <T extends Block> RegistrySupplier<T> register(String name, Supplier<T> block) {
        RegistrySupplier<T> registered = BLOCKS.register(name, block);
        ModItems.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    public static void register() {
        BLOCKS.register();
    }
}
