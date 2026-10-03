package io.github.ezequiel24123z.grindless.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.material.RegistryTagView;
import io.github.ezequiel24123z.grindless.research.ResearchSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entrypoint. This class also serves NeoForge 1.20.1, which is a soft-fork of Forge 47 and
 * keeps the {@code net.minecraftforge} packages unchanged — see ADR-0002.
 *
 * <p>Architectury needs the mod's event bus registered before common init runs, because its
 * deferred registers hook Forge registry events.
 */
@Mod(Grindless.MOD_ID)
public final class GrindlessForge {

    public GrindlessForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Grindless.MOD_ID, bus);
        Grindless.init();
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent event) ->
                MaterialRegistry.rebuild(RegistryTagView.ofItems()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ResearchSync.sendTo(player);
            }
        });
    }
}
