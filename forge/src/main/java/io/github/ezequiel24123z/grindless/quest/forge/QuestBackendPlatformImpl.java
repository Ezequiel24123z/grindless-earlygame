package io.github.ezequiel24123z.grindless.quest.forge;

import io.github.ezequiel24123z.grindless.forge.quest.HeraclesQuestIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class QuestBackendPlatformImpl {

    private QuestBackendPlatformImpl() {
    }

    public static boolean open(ServerPlayer player) {
        return ModList.get().isLoaded("heracles") && HeraclesQuestIntegration.open(player);
    }
}
