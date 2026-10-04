package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.BasicTankBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.ClayConduitBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.HandPumpBlockEntity;
import io.github.ezequiel24123z.grindless.belt.BeltBlockEntity;
import io.github.ezequiel24123z.grindless.belt.ManipulatorBlockEntity;
import io.github.ezequiel24123z.grindless.belt.MergerBlockEntity;
import io.github.ezequiel24123z.grindless.belt.OverflowGateBlockEntity;
import io.github.ezequiel24123z.grindless.belt.SorterBlockEntity;
import io.github.ezequiel24123z.grindless.belt.SplitterBlockEntity;
import io.github.ezequiel24123z.grindless.belt.TunnelBeltBlockEntity;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlockEntity;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.machine.ResearchTerminalBlockEntity;
import io.github.ezequiel24123z.grindless.machine.TerrestrialExtractorBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ThermalGeneratorBlockEntity;
import io.github.ezequiel24123z.grindless.network.CapacitorBankBlockEntity;
import io.github.ezequiel24123z.grindless.network.FluxTransformerBlockEntity;
import io.github.ezequiel24123z.grindless.network.PylonBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entities.
 *
 * <p>Only machines that actually do something appear here. A block with no behaviour deliberately
 * has no block entity, because an empty block entity still costs memory and a ticker slot for
 * every copy a player places.
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Grindless.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    /** The first power in the game: F0, 8 FU/t, cranked by hand. */
    public static final RegistrySupplier<BlockEntityType<HandCrankDynamoBlockEntity>>
            HAND_CRANK_DYNAMO = BLOCK_ENTITIES.register("hand_crank_dynamo",
                    () -> BlockEntityType.Builder
                            .of(HandCrankDynamoBlockEntity::new, ModBlocks.HAND_CRANK_DYNAMO.get())
                            .build(null));

    /** T0 extractor: one unit of the chunk vein every twenty seconds at F0. */
    public static final RegistrySupplier<BlockEntityType<CrudeExtractorBlockEntity>> CRUDE_EXTRACTOR =
            BLOCK_ENTITIES.register("crude_extractor",
                    () -> BlockEntityType.Builder
                            .of(CrudeExtractorBlockEntity::new, ModBlocks.CRUDE_EXTRACTOR.get())
                            .build(null));

    /** T0 progression: one Data Core and thirty seconds at F0 unlocks Voltaic. */
    public static final RegistrySupplier<BlockEntityType<ResearchTerminalBlockEntity>> RESEARCH_TERMINAL =
            BLOCK_ENTITIES.register("research_terminal",
                    () -> BlockEntityType.Builder
                            .of(ResearchTerminalBlockEntity::new, ModBlocks.RESEARCH_TERMINAL.get())
                            .build(null));

    /** T1 generator: furnace fuel at 32 FU/t. */
    public static final RegistrySupplier<BlockEntityType<ThermalGeneratorBlockEntity>> THERMAL_GENERATOR =
            BLOCK_ENTITIES.register("thermal_generator",
                    () -> BlockEntityType.Builder
                            .of(ThermalGeneratorBlockEntity::new, ModBlocks.THERMAL_GENERATOR.get())
                            .build(null));

    /** T1 pulverizer. Same block-entity class as the arc furnace; the kind is the difference. */
    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> PULVERIZER =
            BLOCK_ENTITIES.register("pulverizer",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.PULVERIZER, pos, state),
                                    ModBlocks.PULVERIZER.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> ARC_FURNACE =
            BLOCK_ENTITIES.register("arc_furnace",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.ARC_FURNACE, pos, state),
                                    ModBlocks.ARC_FURNACE.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> PRESS =
            BLOCK_ENTITIES.register("press",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.PRESS, pos, state),
                                    ModBlocks.PRESS.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> ASSEMBLER =
            BLOCK_ENTITIES.register("assembler",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.ASSEMBLER, pos, state),
                                    ModBlocks.ASSEMBLER.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> KILN =
            BLOCK_ENTITIES.register("kiln",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.KILN, pos, state),
                                    ModBlocks.KILN.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> WIRE_MILL =
            BLOCK_ENTITIES.register("wire_mill",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.WIRE_MILL, pos, state),
                                    ModBlocks.WIRE_MILL.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ProcessMachineBlockEntity>> CHEMICAL_REACTOR =
            BLOCK_ENTITIES.register("chemical_reactor",
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ProcessMachineBlockEntity(
                                    ProcessMachineKind.CHEMICAL_REACTOR, pos, state),
                                    ModBlocks.CHEMICAL_REACTOR.get())
                            .build(null));

    /**
     * One type for all three pylon tiers.
     *
     * <p>The tier lives on the block rather than the block entity, so a single type can serve
     * every tier — and the tier cannot drift from what the player sees, because the block
     * <em>is</em> the tier.
     */
    public static final RegistrySupplier<BlockEntityType<BeltBlockEntity>> CONVEYOR_BELT =
            BLOCK_ENTITIES.register("conveyor_belt",
                    () -> BlockEntityType.Builder
                            .of(BeltBlockEntity::new, ModBlocks.CONVEYOR_BELT.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<SplitterBlockEntity>> SPLITTER =
            BLOCK_ENTITIES.register("splitter",
                    () -> BlockEntityType.Builder
                            .of(SplitterBlockEntity::new, ModBlocks.SPLITTER.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<MergerBlockEntity>> MERGER =
            BLOCK_ENTITIES.register("merger",
                    () -> BlockEntityType.Builder
                            .of(MergerBlockEntity::new, ModBlocks.MERGER.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<TunnelBeltBlockEntity>> TUNNEL_BELT =
            BLOCK_ENTITIES.register("tunnel_belt",
                    () -> BlockEntityType.Builder
                            .of(TunnelBeltBlockEntity::new, ModBlocks.TUNNEL_BELT.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<OverflowGateBlockEntity>> OVERFLOW_GATE =
            BLOCK_ENTITIES.register("overflow_gate",
                    () -> BlockEntityType.Builder
                            .of(OverflowGateBlockEntity::new, ModBlocks.OVERFLOW_GATE.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<SorterBlockEntity>> SORTER =
            BLOCK_ENTITIES.register("sorter",
                    () -> BlockEntityType.Builder
                            .of(SorterBlockEntity::new, ModBlocks.SORTER.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ManipulatorBlockEntity>> CRUDE_MANIPULATOR =
            BLOCK_ENTITIES.register("crude_manipulator",
                    () -> BlockEntityType.Builder
                            .of(ManipulatorBlockEntity::new, ModBlocks.CRUDE_MANIPULATOR.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<TerrestrialExtractorBlockEntity>>
            TERRESTRIAL_EXTRACTOR = BLOCK_ENTITIES.register("terrestrial_extractor",
                    () -> BlockEntityType.Builder
                            .of(TerrestrialExtractorBlockEntity::new,
                                    ModBlocks.TERRESTRIAL_EXTRACTOR.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<ClayConduitBlockEntity>> CLAY_CONDUIT =
            BLOCK_ENTITIES.register("clay_conduit",
                    () -> BlockEntityType.Builder
                            .of(ClayConduitBlockEntity::new, ModBlocks.CLAY_CONDUIT.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<HandPumpBlockEntity>> HAND_PUMP =
            BLOCK_ENTITIES.register("hand_pump",
                    () -> BlockEntityType.Builder
                            .of(HandPumpBlockEntity::new, ModBlocks.HAND_PUMP.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<BasicTankBlockEntity>> BASIC_TANK =
            BLOCK_ENTITIES.register("basic_tank",
                    () -> BlockEntityType.Builder
                            .of(BasicTankBlockEntity::new, ModBlocks.BASIC_TANK.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<PylonBlockEntity>> FLUX_PYLON =
            BLOCK_ENTITIES.register("flux_pylon",
                    () -> BlockEntityType.Builder
                            .of(PylonBlockEntity::new,
                                    ModBlocks.FLUX_PYLON_MK1.get(),
                                    ModBlocks.FLUX_PYLON_MK2.get(),
                                    ModBlocks.FLUX_PYLON_MK3.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<CapacitorBankBlockEntity>> CAPACITOR_BANK =
            BLOCK_ENTITIES.register("capacitor_bank",
                    () -> BlockEntityType.Builder
                            .of(CapacitorBankBlockEntity::new, ModBlocks.CAPACITOR_BANK.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<FluxTransformerBlockEntity>> FLUX_TRANSFORMER =
            BLOCK_ENTITIES.register("flux_transformer",
                    () -> BlockEntityType.Builder
                            .of(FluxTransformerBlockEntity::new, ModBlocks.FLUX_TRANSFORMER.get())
                            .build(null));

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
