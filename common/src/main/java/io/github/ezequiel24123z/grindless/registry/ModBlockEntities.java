package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlockEntity;
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

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
