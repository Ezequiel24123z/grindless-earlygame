package io.github.ezequiel24123z.grindless.forge.telemetry.dev;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.telemetry.dev.DevTelemetry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Counts accepted placement observations, not unique machines, loaded chunks or persisted identity. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class DevTelemetryEvents {
    private DevTelemetryEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !DevTelemetry.active(level.getServer())) {
            return;
        }
        var id = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (id.getNamespace().equals(Grindless.MOD_ID)
                && BlockCatalogue.blocks().stream().anyMatch(block -> block.name().equals(id.getPath()))) {
            DevTelemetry.machinePlaced(level, id.toString());
        }
    }
}
