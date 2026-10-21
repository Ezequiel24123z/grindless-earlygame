package io.github.ezequiel24123z.grindless.quest.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.forge.quest.FtbQuestIntegration;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class QuestBackendPlatformImpl {

    private QuestBackendPlatformImpl() {
    }

    public static boolean open(ServerPlayer player) {
        if (ModList.get().isLoaded("ftbquests")) {
            FtbQuestIntegration.open(player);
        } else {
            Grindless.LOG.error("Cannot open native Grindless quests: FTB Quests is not installed");
            player.displayClientMessage(Component.translatable("chat.grindless.quest.backend_unavailable"), false);
        }
        return true;
    }
}
