package io.github.ezequiel24123z.grindless;

import dev.architectury.event.events.common.TickEvent;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.item.ExosuitTick;
import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import io.github.ezequiel24123z.grindless.network.PylonStructure;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.registry.ModCreativeTabs;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.recipe.ModRecipes;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import io.github.ezequiel24123z.grindless.registry.ModSounds;
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
        ModRecipes.register();
        ModSounds.register();
        ModCreativeTabs.register();

        MaterialRegistry.bootstrap();
        registerNetworkTick();

        LOG.info("[{}] common init complete", MOD_NAME);
    }

    /**
     * Resolves every Flux Network's supply against the demand registered during the tick.
     *
     * <p>Runs after block entities have ticked, so the satisfaction a machine sees was computed
     * from the demand of the tick before. That one-tick lag is deliberate and invisible: it is
     * what makes the brownout <em>proportional</em>, since every machine has declared its draw
     * before any of them is served (ADR-0046). Resolving mid-tick instead would favour whichever
     * machines happen to tick first.
     *
     * <p>The loop is over networks, not pylons or machines, so its cost is the number of separate
     * grids a player has built — a number that stays small even in a large base.
     */
    private static void registerNetworkTick() {
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            ExosuitTick.tickLevel(level);
            FluxNetworkData.get(level).tickNetworks((pylon, status) -> {
                    // A pylon in an unloaded chunk is skipped rather than loaded to repaint it.
                    if (level.isLoaded(pylon)) {
                        MachineProperties.publish(level, pylon, status);
                        PylonStructure.syncShafts(level, pylon, status);
                    }
                });
        });
    }
}
