package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlock;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlock;
import io.github.ezequiel24123z.grindless.machine.MachineShellBlock;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
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
    public static final RegistrySupplier<CrudeExtractorBlock> CRUDE_EXTRACTOR = register("crude_extractor",
            () -> new CrudeExtractorBlock(machine().strength(2.5F)));

    /** T0 progression gate. Consumes Data Cores and Flux Units to unlock blueprints. */
    public static final RegistrySupplier<Block> RESEARCH_TERMINAL = register("research_terminal",
            () -> new MachineShellBlock(BlockCatalogue.Geometry.TERMINAL, machine().strength(3.0F)));

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
