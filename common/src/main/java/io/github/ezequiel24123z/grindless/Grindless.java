package io.github.ezequiel24123z.grindless;

import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.registry.ModCreativeTabs;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Every platform bootstrap funnels into {@link #init()}.
 *
 * <p>Nothing in this package may reference loader specific classes; anything that needs to
 * differ between Fabric and Forge goes through an {@code @ExpectPlatform} stub.
 */
public final class Grindless {

    public static final String MOD_ID = "grindless";
    public static final String MOD_NAME = "Grindless";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Grindless() {
    }

    /** Builds a {@link ResourceLocation} inside our namespace. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /**
     * Registers every game object. Order matters: blocks must exist before the block
     * entities that reference them, and both must exist before the creative tab that
     * lists them.
     */
    public static void init() {
        GrindlessConfig.load();

        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenus.register();
        ModCreativeTabs.register();

        MaterialRegistry.bootstrap();

        LOG.info("[{}] common init complete", MOD_NAME);
    }
}
