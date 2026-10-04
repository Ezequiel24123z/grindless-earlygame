package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/**
 * Creative tabs. One tab for now; it splits by system once the catalogue outgrows a single page.
 *
 * <p>Registered last, because the tab enumerates items that must already exist. Material items are
 * added through a callback that runs on every rebuild of the tab, because which of them are shown
 * depends on tags that are not loaded until a world is (ADR-0050).
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Grindless.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + Grindless.MOD_ID + ".main"),
                    () -> new ItemStack(ModItems.MULTITOOL.get())));

    private ModCreativeTabs() {
    }

    public static void register() {
        TABS.register();
        CreativeTabRegistry.append(MAIN,
                lazy(ModItems.MULTITOOL),
                lazy(ModItems.DATA_CORE),
                lazy(ModItems.ADVANCED_DATA_CORE),
                lazy(ModItems.MACHINE_CASING),
                lazy(ModItems.PLATE_DIE),
                lazy(ModItems.ROD_DIE),
                lazy(ModItems.GEAR_DIE),
                lazy(ModItems.COIL_DIE),
                lazy(ModItems.COPPER_COIL),
                lazy(ModItems.MOTOR),
                lazy(ModItems.INGOT_MOULD),
                lazy(ModItems.PLATE_MOULD),
                lazy(ModItems.VANADIA_PELLET),
                lazy(ModItems.SLAG),
                lazy(ModItems.PROSPECTORS_SCANNER),
                lazy(ModItems.PROCESS_ATLAS),
                lazy(ModItems.FLUX_CONDUIT),
                lazy(ModBlocks.HAND_CRANK_DYNAMO),
                lazy(ModBlocks.CRUDE_EXTRACTOR),
                lazy(ModBlocks.RESEARCH_TERMINAL),
                lazy(ModBlocks.THERMAL_GENERATOR),
                lazy(ModBlocks.PULVERIZER),
                lazy(ModBlocks.ARC_FURNACE),
                lazy(ModBlocks.PRESS),
                lazy(ModBlocks.ASSEMBLER),
                lazy(ModBlocks.KILN),
                lazy(ModBlocks.WIRE_MILL),
                lazy(ModBlocks.CHEMICAL_REACTOR),
                lazy(ModBlocks.CHEMICAL_WASHER),
                lazy(ModBlocks.ELECTROLYSIS_CELL),
                lazy(ModBlocks.ATMOSPHERIC_INTAKE),
                lazy(ModBlocks.FLUID_WELL),
                lazy(ModBlocks.INDUCTION_FURNACE),
                lazy(ModBlocks.CASTER),
                lazy(ModBlocks.FLOTATION_CELL),
                lazy(ModBlocks.MAGNETIC_SEPARATOR),
                lazy(ModBlocks.CONVEYOR_BELT),
                lazy(ModBlocks.SPLITTER),
                lazy(ModBlocks.MERGER),
                lazy(ModBlocks.TUNNEL_BELT),
                lazy(ModBlocks.OVERFLOW_GATE),
                lazy(ModBlocks.SORTER),
                lazy(ModBlocks.CRUDE_MANIPULATOR),
                lazy(ModBlocks.TERRESTRIAL_EXTRACTOR),
                lazy(ModBlocks.CLAY_CONDUIT),
                lazy(ModBlocks.HAND_PUMP),
                lazy(ModBlocks.BASIC_TANK),
                lazy(ModBlocks.CAPACITOR_BANK),
                lazy(ModBlocks.FLUX_TRANSFORMER),
                lazy(ModBlocks.FLUX_PYLON_MK1),
                lazy(ModBlocks.FLUX_PYLON_MK2),
                lazy(ModBlocks.FLUX_PYLON_MK3));
        CreativeTabRegistry.modify(MAIN, (flags, output, adminTab) -> {
            for (RegistrySupplier<Item> item : ModItems.SUPPLY) {
                if (!MaterialRegistry.isRedundantFallback(item.getId())) {
                    output.accept(item.get());
                }
            }
        });
    }

    /**
     * Defers {@code item.get()} until the registry has been populated. Calling {@code get()} here
     * directly throws "Registry Object not present" while the mod is still being constructed.
     */
    private static Supplier<ItemLike> lazy(Supplier<? extends ItemLike> item) {
        return item::get;
    }
}
