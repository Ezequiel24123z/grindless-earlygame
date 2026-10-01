package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlock;
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
 * <p>The T0 bootstrap set only. These are plain blocks for now; they gain their block entities and
 * menus as each machine is implemented.
 */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Grindless.MOD_ID, Registries.BLOCK);

    /** T0 manual generator. The first source of Flux Units, and the end of the grind. */
    public static final RegistrySupplier<HandCrankDynamoBlock> HAND_CRANK_DYNAMO =
            register("hand_crank_dynamo",
                    () -> new HandCrankDynamoBlock(machine().strength(2.0F)));

    /** T0 extractor. Slow and cheap, but it never needs a tunnel. */
    public static final RegistrySupplier<Block> CRUDE_EXTRACTOR = register("crude_extractor",
            () -> new Block(machine().strength(2.5F)));

    /** T0 progression gate. Consumes Data Cores and Flux Units to unlock blueprints. */
    public static final RegistrySupplier<Block> RESEARCH_TERMINAL = register("research_terminal",
            () -> new Block(machine().strength(3.0F)));

    private ModBlocks() {
    }

    /** Shared base properties for machine blocks: metallic, pickaxe-mined. */
    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
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
