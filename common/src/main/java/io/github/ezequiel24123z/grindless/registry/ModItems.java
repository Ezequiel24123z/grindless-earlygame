package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.item.FluxCellItem;
import io.github.ezequiel24123z.grindless.item.HarnessItem;
import io.github.ezequiel24123z.grindless.item.DeconstructionPlannerItem;
import io.github.ezequiel24123z.grindless.item.BlueprintItem;
import io.github.ezequiel24123z.grindless.item.BlueprintToolItem;
import io.github.ezequiel24123z.grindless.item.DrillCellItem;
import io.github.ezequiel24123z.grindless.item.FluxDrillItem;
import io.github.ezequiel24123z.grindless.item.MultitoolItem;
import io.github.ezequiel24123z.grindless.item.ProcessAtlasItem;
import io.github.ezequiel24123z.grindless.item.ProspectorsScannerItem;
import io.github.ezequiel24123z.grindless.item.FluxConduitItem;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.material.SupplyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
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

    /** Caster catalysts. Not consumed (ADR-0079). */
    public static final RegistrySupplier<Item> INGOT_MOULD = register("ingot_mould",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_MOULD = register("plate_mould",
            () -> new Item(new Item.Properties()));

    /** Contact-process catalyst. Iron oxide on a ceramic brick support (ADR-0075). */
    public static final RegistrySupplier<Item> VANADIA_PELLET = register("vanadia_pellet",
            () -> new Item(new Item.Properties()));

    /** R1 gangue. A Grindless reagent, not a material form (ADR-0033). */
    public static final RegistrySupplier<Item> SLAG = register("slag",
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

    /** T2 handheld. Mines with charge, never durability (ADR-0084). */
    public static final RegistrySupplier<Item> FLUX_DRILL = register("flux_drill",
            () -> new FluxDrillItem(new Item.Properties().stacksTo(1)));

    /** Fuel for the Flux Drill. Not the armour Flux Cell. */
    public static final RegistrySupplier<Item> DRILL_CELL = register("drill_cell",
            () -> new DrillCellItem(new Item.Properties()));

    /** T2 handheld. Captures a box into a blueprint (ADR-0085). */
    public static final RegistrySupplier<Item> BLUEPRINT_TOOL = register("blueprint_tool",
            () -> new BlueprintToolItem(new Item.Properties().stacksTo(1)));

    /** A captured layout. Produced by the tool, not by a recipe. */
    public static final RegistrySupplier<Item> BLUEPRINT = register("blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(1)));

    /** T2 handheld. Marks a box. Does not pick it up (ADR-0086). */
    public static final RegistrySupplier<Item> DECONSTRUCTION_PLANNER = register("deconstruction_planner",
            () -> new DeconstructionPlannerItem(new Item.Properties().stacksTo(1)));

    /** Fungible intermediate. A smash yields one; it is not a fluid (ADR-0088). */
    public static final RegistrySupplier<Item> MATTER = register("matter",
            () -> new Item(new Item.Properties()));

    /** T1 suit. One slot, iron protection, no generator (ADR-0089). */
    public static final RegistrySupplier<Item> VOLTAIC_HELMET = register("voltaic_helmet",
            () -> new HarnessItem(ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> VOLTAIC_CHESTPLATE = register("voltaic_chestplate",
            () -> new HarnessItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> VOLTAIC_LEGGINGS = register("voltaic_leggings",
            () -> new HarnessItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> VOLTAIC_BOOTS = register("voltaic_boots",
            () -> new HarnessItem(ArmorItem.Type.BOOTS, new Item.Properties()));

    /** Suit buffer. Not the drill cell (ADR-0084, ADR-0089). */
    public static final RegistrySupplier<Item> FLUX_CELL = register("flux_cell",
            () -> new FluxCellItem(new Item.Properties().stacksTo(1)));

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
