package io.github.ezequiel24123z.grindless.quest.forge;

import io.github.ezequiel24123z.grindless.forge.quest.HeraclesQuestIntegration;
import io.github.ezequiel24123z.grindless.forge.quest.FtbQuestIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class QuestBackendPlatformImpl {

    private QuestBackendPlatformImpl() {
    }

    public static boolean open(ServerPlayer player) {
        if (ModList.get().isLoaded("ftbquests")) {
            FtbQuestIntegration.open(player);
            return true;
        }
        return ModList.get().isLoaded("heracles") && HeraclesQuestIntegration.open(player);
    }
}
