package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlockEntity;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ResearchTerminalBlockEntity;
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

    /**
     * One type for all three pylon tiers.
     *
     * <p>The tier lives on the block rather than the block entity, so a single type can serve
     * every tier — and the tier cannot drift from what the player sees, because the block
     * <em>is</em> the tier.
     */
    public static final RegistrySupplier<BlockEntityType<PylonBlockEntity>> FLUX_PYLON =
            BLOCK_ENTITIES.register("flux_pylon",
                    () -> BlockEntityType.Builder
                            .of(PylonBlockEntity::new,
                                    ModBlocks.FLUX_PYLON_MK1.get(),
                                    ModBlocks.FLUX_PYLON_MK2.get(),
                                    ModBlocks.FLUX_PYLON_MK3.get())
                            .build(null));

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
