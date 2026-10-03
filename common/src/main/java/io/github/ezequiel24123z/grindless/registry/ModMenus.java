package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import net.minecraft.core.registries.Registries;
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

    private ModMenus() {
    }

    public static void register() {
        MENUS.register();
    }
}
