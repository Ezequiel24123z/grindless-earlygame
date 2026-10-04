package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.menu.FieldGuideMenu;
import io.github.ezequiel24123z.grindless.menu.LandingMapMenu;
import io.github.ezequiel24123z.grindless.menu.ProcessAtlasMenu;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.menu.QuestBookMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Container menus. Slice A ships one shared machine menu (ADR-0058).
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Grindless.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<ProcessMachineMenu>> PROCESS_MACHINE =
            MENUS.register("process_machine",
                    () -> MenuRegistry.ofExtended(ProcessMachineMenu::new));

    public static final RegistrySupplier<MenuType<ProcessAtlasMenu>> PROCESS_ATLAS =
            MENUS.register("process_atlas",
                    () -> new MenuType<>(ProcessAtlasMenu::new, FeatureFlags.VANILLA_SET));

    /** The landing map. No slots (ADR-0097). */
    public static final RegistrySupplier<MenuType<LandingMapMenu>> LANDING_MAP =
            MENUS.register("landing_map",
                    () -> new MenuType<>(LandingMapMenu::new, FeatureFlags.VANILLA_SET));

    /** The quest book. No slots (ADR-0100). */
    public static final RegistrySupplier<MenuType<QuestBookMenu>> QUEST_BOOK =
            MENUS.register("quest_book",
                    () -> new MenuType<>(QuestBookMenu::new, FeatureFlags.VANILLA_SET));

    /** The field guide. No slots (ADR-0100). */
    public static final RegistrySupplier<MenuType<FieldGuideMenu>> FIELD_GUIDE =
            MENUS.register("field_guide",
                    () -> new MenuType<>(FieldGuideMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }

    public static void register() {
        MENUS.register();
    }
}
