package io.github.ezequiel24123z.grindless.client;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import io.github.ezequiel24123z.grindless.research.ResearchSync;

/**
 * Client-only setup. Screens bind here so a dedicated server never loads them.
 */
public final class GrindlessClient {

    private GrindlessClient() {
    }

    public static void init() {
        ResearchSync.register();
        MenuRegistry.registerScreenFactory(ModMenus.PROCESS_MACHINE.get(), ProcessMachineScreen::new);
    }
}
