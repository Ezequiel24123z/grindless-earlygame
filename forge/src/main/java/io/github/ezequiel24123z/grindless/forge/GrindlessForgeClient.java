package io.github.ezequiel24123z.grindless.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.client.GrindlessClient;
import io.github.ezequiel24123z.grindless.forge.quest.HeraclesQuestClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Grindless.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GrindlessForgeClient {

    private GrindlessForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        GrindlessClient.init();
        if (ModList.get().isLoaded("heracles")) {
            event.enqueueWork(HeraclesQuestClient::init);
        }
    }
}
