package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Creative tabs. One tab for now; it splits by system once the catalogue outgrows a single page.
 *
 * <p>Registered last, because the tab enumerates items that must already exist.
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
                ModItems.MULTITOOL.get(),
                ModItems.DATA_CORE.get(),
                ModItems.MACHINE_CASING.get(),
                ModBlocks.HAND_CRANK_DYNAMO.get(),
                ModBlocks.CRUDE_EXTRACTOR.get(),
                ModBlocks.RESEARCH_TERMINAL.get());
    }
}
