package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.item.MultitoolItem;
import io.github.ezequiel24123z.grindless.item.ProcessAtlasItem;
import io.github.ezequiel24123z.grindless.item.ProspectorsScannerItem;
import io.github.ezequiel24123z.grindless.item.FluxConduitItem;
import io.github.ezequiel24123z.grindless.item.SupraluminalStationItem;
import io.github.ezequiel24123z.grindless.item.SurveyRocketItem;
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
 * <p>Bootstrap items plus Slice D fabrication (dies, coil, casing), the Slice E
 * linker, the T2 motor, and the contact-process catalyst. Circuit boards arrive with etching.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Grindless.MOD_ID, Registries.ITEM);

    /** The T0 handheld. Replaces the stone-tool phase outright. */
    public static final RegistrySupplier<Item> MULTITOOL = register("multitool",
            () -> new MultitoolItem(new Item.Properties().stacksTo(1)));

    /** Research currency. Produced by the factory, spent in the Research Terminal. */
    public static final RegistrySupplier<Item> DATA_CORE = register("data_core",
            () -> new Item(new Item.Properties()));

    /** T2 research currency. Voltaic-gated; spent for Industrial (ADR-0073). */
    public static final RegistrySupplier<Item> ADVANCED_DATA_CORE = register("advanced_data_core",
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

    /** T2 fabricated component. Feeds later electrical crafts (ADR-0074). */
    public static final RegistrySupplier<Item> MOTOR = register("motor",
            () -> new Item(new Item.Properties()));

    /** Contact-process catalyst. Iron oxide on a ceramic brick support (ADR-0075). */
    public static final RegistrySupplier<Item> VANADIA_PELLET = register("vanadia_pellet",
            () -> new Item(new Item.Properties()));

    /** R1 gangue. A Grindless reagent, not a material form (ADR-0033). */
    public static final RegistrySupplier<Item> SLAG = register("slag",
            () -> new Item(new Item.Properties()));

    /** Furnace lining. Slag's named sink on the Arc Furnace (ADR-0091). */
    public static final RegistrySupplier<Item> REFRACTORY_BRICK = register("refractory_brick",
            () -> new Item(new Item.Properties()));

    /** 99 % silicon. Carbothermic product; not the circuit grade (ADR-0092). */
    public static final RegistrySupplier<Item> METALLURGICAL_SILICON = register("metallurgical_silicon",
            () -> new Item(new Item.Properties()));

    /** Six-nines silicon from zone refining. Not a circuit, a wafer or an ingot tag (ADR-0093). */
    public static final RegistrySupplier<Item> ELECTRONIC_SILICON = register("electronic_silicon",
            () -> new Item(new Item.Properties()));

    /** Luna's extractable. A reagent, not an ore and not a fluid (ADR-0095). */
    public static final RegistrySupplier<Item> HELIUM_3 = register("helium_3",
            () -> new Item(new Item.Properties()));

    /** T1 handheld. Surveys the standing chunk and its neighbours. */
    public static final RegistrySupplier<Item> PROSPECTORS_SCANNER = register("prospectors_scanner",
            () -> new ProspectorsScannerItem(new Item.Properties().stacksTo(1)));

    /** T1 handheld. Lists live process recipes. Not a solver (ADR-0066). */
    public static final RegistrySupplier<Item> PROCESS_ATLAS = register("process_atlas",
            () -> new ProcessAtlasItem(new Item.Properties().stacksTo(1)));

    /** T1 handheld. Right-click two pylons to join them across any distance. */
    public static final RegistrySupplier<Item> FLUX_CONDUIT = register("flux_conduit",
            () -> new FluxConduitItem(new Item.Properties().stacksTo(1)));

    /** Local-system rocket. Placed on a launch pad; it climbs, then the map opens (ADR-0097). */
    public static final RegistrySupplier<Item> SURVEY_ROCKET = register("survey_rocket",
            () -> new SurveyRocketItem(new Item.Properties().stacksTo(1)));

    /** Interstellar station. Placed on a berth; it climbs, and the ceiling is the arrival (ADR-0098). */
    public static final RegistrySupplier<Item> SUPRALUMINAL_STATION = register("supraluminal_station",
            () -> new SupraluminalStationItem(new Item.Properties().stacksTo(1)));

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
