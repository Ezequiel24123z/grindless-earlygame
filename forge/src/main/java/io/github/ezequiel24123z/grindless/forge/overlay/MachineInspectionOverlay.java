package io.github.ezequiel24123z.grindless.forge.overlay;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Original native equivalent, with no optional overlay classes or client rendering on the server. */
public final class MachineInspectionOverlay {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue DEVELOPER;
    public static final ForgeConfigSpec.BooleanValue ALLOW_WITH_OTHER_OVERLAYS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("machineInspection");
        ENABLED = builder.comment("Opt in to Grindless's native aimed-block inspection. Toggle in Controls (default F8).")
                .define("enabled", false);
        DEVELOPER = builder.comment("Show internal recipe identities and observation ticks. Shift alone never enables this.")
                .define("developer", false);
        ALLOW_WITH_OTHER_OVERLAYS = builder.comment(
                        "Allow the native panel alongside Jade, WAILA or The One Probe. False avoids duplicate HUDs.")
                .define("allowWithOtherOverlays", false);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private MachineInspectionOverlay() {
    }

    public static void init(IEventBus bus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "grindless-inspection-client.toml");
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(MachineInspectionNetwork::init));
    }
}
