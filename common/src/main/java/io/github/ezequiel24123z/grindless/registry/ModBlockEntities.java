package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entities.
 *
 * <p>Empty for now — the T0 blocks are inert placeholders. The first real entries arrive with the
 * Hand Crank Dynamo's energy storage and the Crude Extractor's progress state.
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Grindless.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
