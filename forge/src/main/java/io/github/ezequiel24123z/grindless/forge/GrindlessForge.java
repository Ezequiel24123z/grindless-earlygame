package io.github.ezequiel24123z.grindless.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.ezequiel24123z.grindless.Grindless;
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
    }
}
