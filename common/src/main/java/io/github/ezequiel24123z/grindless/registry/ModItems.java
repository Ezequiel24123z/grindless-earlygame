package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.item.ProspectorsScannerItem;
import io.github.ezequiel24123z.grindless.item.FluxConduitItem;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.material.SupplyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Items, and the register every block item is also added to.
 *
 * <p>Bootstrap items plus Slice D fabrication (dies, coil, casing) and the Slice E
 * linker. Motors and circuit boards arrive with T2.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Grindless.MOD_ID, Registries.ITEM);

    /** The T0 handheld. Replaces the stone-tool phase outright. */
    public static final RegistrySupplier<Item> MULTITOOL = register("multitool",
            () -> new Item(new Item.Properties().stacksTo(1)));

    /** Research currency. Produced by the factory, spent in the Research Terminal. */
    public static final RegistrySupplier<Item> DATA_CORE = register("data_core",
            () -> new Item(new Item.Properties()));

    /** The first fabricated component; every machine above T1 is built on one. */
    public static final RegistrySupplier<Item> MACHINE_CASING = register("machine_casing",
            () -> new Item(new Item.Properties()));

    /** Press catalyst. Selects plate from an ingot; not consumed. */
    public static final RegistrySupplier<Item> PLATE_DIE = register("plate_die",
            () -> new Item(new Item.Properties()));

    /** Press catalyst. Selects rod from an ingot; not consumed. */
    public static final RegistrySupplier<Item> ROD_DIE = register("rod_die",
            () -> new Item(new Item.Properties()));

    /** Press catalyst. Selects gear from an ingot; not consumed. */
    public static final RegistrySupplier<Item> GEAR_DIE = register("gear_die",
            () -> new Item(new Item.Properties()));

    /** Press catalyst. Selects a copper coil from a copper ingot; not consumed. */
    public static final RegistrySupplier<Item> COIL_DIE = register("coil_die",
            () -> new Item(new Item.Properties()));

    /** T1 electrical reagent. The Wire Mill is the dedicated T2 route. */
    public static final RegistrySupplier<Item> COPPER_COIL = register("copper_coil",
            () -> new Item(new Item.Properties()));

    /** R1 gangue. A Grindless reagent, not a material form (ADR-0033). */
    public static final RegistrySupplier<Item> SLAG = register("slag",
            () -> new Item(new Item.Properties()));

    /** T1 handheld. Surveys the standing chunk and its neighbours. */
    public static final RegistrySupplier<Item> PROSPECTORS_SCANNER = register("prospectors_scanner",
            () -> new ProspectorsScannerItem(new Item.Properties().stacksTo(1)));

    /** T1 handheld. Right-click two pylons to join them across any distance. */
    public static final RegistrySupplier<Item> FLUX_CONDUIT = register("flux_conduit",
            () -> new FluxConduitItem(new Item.Properties().stacksTo(1)));

    /**
     * Every material item Grindless supplies, registered whether or not the pack needs it.
     *
     * <p>Registration cannot depend on which other mods are installed: registry IDs are saved into
     * worlds, so adding or removing a mod must not add or remove ours. Whether an item is actually
     * wanted is decided later, from tags (ADR-0050).
     */
    public static final List<RegistrySupplier<Item>> SUPPLY = registerSupply();

    private ModItems() {
    }

    private static List<RegistrySupplier<Item>> registerSupply() {
        List<RegistrySupplier<Item>> supply = new ArrayList<>();
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            supply.add(register(entry.itemName(),
                    () -> new SupplyItem(entry, new Item.Properties())));
        }
        return Collections.unmodifiableList(supply);
    }

    public static <T extends Item> RegistrySupplier<T> register(String name, Supplier<T> item) {
        return ITEMS.register(name, item);
    }

    public static void register() {
        ITEMS.register();
    }
}
