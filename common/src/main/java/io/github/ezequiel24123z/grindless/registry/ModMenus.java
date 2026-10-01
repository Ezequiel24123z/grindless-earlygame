package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/**
 * Container menus.
 *
 * <p>Empty for now. Every machine menu will be built on the shared container contract — filters,
 * buffer targets, capacity limits and auto-void — so the screens stay consistent across tiers.
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Grindless.MOD_ID, Registries.MENU);

    private ModMenus() {
    }

    public static void register() {
        MENUS.register();
    }
}
