package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.BasicTankBlock;
import io.github.ezequiel24123z.grindless.fluid.ClayConduitBlock;
import io.github.ezequiel24123z.grindless.fluid.HandPumpBlock;
import io.github.ezequiel24123z.grindless.belt.BeltBlock;
import io.github.ezequiel24123z.grindless.belt.ManipulatorBlock;
import io.github.ezequiel24123z.grindless.belt.MergerBlock;
import io.github.ezequiel24123z.grindless.belt.OverflowGateBlock;
import io.github.ezequiel24123z.grindless.belt.SorterBlock;
import io.github.ezequiel24123z.grindless.belt.SplitterBlock;
import io.github.ezequiel24123z.grindless.belt.TunnelBeltBlock;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlock;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlock;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlock;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.machine.ResearchTerminalBlock;
import io.github.ezequiel24123z.grindless.machine.TerrestrialExtractorBlock;
import io.github.ezequiel24123z.grindless.machine.ThermalGeneratorBlock;
import io.github.ezequiel24123z.grindless.network.CapacitorBankBlock;
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

    /** T0 progression gate. Consumes Data Cores and Flux Units to unlock blueprints. */
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

    /** T1 storage. Adds capacity to the covering network; no supply cube of its own. */
    public static final RegistrySupplier<CapacitorBankBlock> CAPACITOR_BANK =
            register("capacitor_bank",
                    () -> new CapacitorBankBlock(machine().strength(3.0F)));

    /** T1 tap. Exchanges FU with the covering network at F1. Not a pylon. */
    public static final RegistrySupplier<FluxTransformerBlock> FLUX_TRANSFORMER =
            register("flux_transformer",
                    () -> new FluxTransformerBlock(machine().strength(3.0F)));

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
