package io.github.ezequiel24123z.grindless.fabric;

import io.github.ezequiel24123z.grindless.Grindless;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint. Everything it does is delegate to the common bootstrap; anything that has to
 * differ between loaders belongs behind an {@code @ExpectPlatform} stub in {@code common}, not
 * here.
 */
public final class GrindlessFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Grindless.init();
    }
}
