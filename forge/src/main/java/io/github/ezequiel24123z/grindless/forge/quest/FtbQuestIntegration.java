package io.github.ezequiel24123z.grindless.forge.quest;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.net.OpenQuestBookMessage;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;

/** Native framework entry and retryable old-receipt import; there is no Grindless quest screen or claim packet. */
public final class FtbQuestIntegration {

    private static ServerQuestFile ready;

    private FtbQuestIntegration() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            FTBQuestsAPI.api().registerFilterAdapter(new FtbMatrixFilter());
            try {
                FtbQuestDefinitionPublisher.requireKnownDefinitions(FtbQuestDefinitions.chapters(),
                        FtbQuestDefinitionPublisher.currentQuestCount());
                FtbQuestFiles.ensure(FMLPaths.CONFIGDIR.get());
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot install native Grindless FTB chapters; existing files were preserved",
                        exception);
            }
        }));
        registerStarted(MinecraftForge.EVENT_BUS);
        LifecycleEvent.SERVER_BEFORE_START.register(FtbQuestIntegration::reset);
        LifecycleEvent.SERVER_STOPPING.register(FtbQuestIntegration::reset);
        TeamEvent.PLAYER_LOGGED_IN.register(event -> importPlayer(event.getPlayer()));
        TeamEvent.PLAYER_CHANGED.register(event -> importPlayer(event.getPlayer()));
        ObjectCompletedEvent.QUEST.register(event -> {
            for (ServerPlayer player : event.getOnlineMembers()) {
                QuestProgress legacy = QuestProgress.get(player);
                if (legacy.usesNativeClaims(player.getUUID())) {
                    FtbQuestNative.refresh(legacy, player.getUUID(), event.getData());
                }
            }
            return EventResult.pass();
        });
        MinecraftForge.EVENT_BUS.addListener(FtbQuestIntegration::tick);
        MinecraftForge.EVENT_BUS.addListener(FtbQuestChecks::registerCommands);
    }

    static void registerStarted(IEventBus bus) {
        // Architectury 9.2.14 delivers the whole SERVER_STARTED loop, including FTB's load, at HIGH.
        bus.addListener(EventPriority.LOWEST, FtbQuestIntegration::started);
    }

    static void reset(MinecraftServer server) {
        ready = null;
    }

    private static void started(ServerStartedEvent event) {
        ready = null;
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file.server != event.getServer()) {
            throw new IllegalStateException("FTB Quests did not initialize its native server quest file");
        }
        FtbQuestNative.verify(file);
        ready = file;
        Grindless.LOG.info("Native FTB Quests ready: {} stable Grindless quests; legacy reward claims are import-only",
                QuestCatalogue.tasks().size());
    }

    static ServerQuestFile verifiedFile(MinecraftServer server) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file != ready || file.server != server) {
            throw new IllegalStateException("Native Grindless FTB definitions have not been verified for this server");
        }
        return file;
    }

    public static void open(ServerPlayer player) {
        if (!importPlayer(player)) {
            return;
        }
        player.closeContainer();
        new OpenQuestBookMessage(FtbQuestDefinitions.id("chapter", "T0")).sendTo(player);
    }

    private static boolean importPlayer(ServerPlayer player) {
        if (!player.server.isSameThread()) {
            throw new IllegalStateException("Native quest import must execute on the server thread");
        }
        try {
            TeamData data = verifiedFile(player.server).getOrCreateTeamData(player);
            FtbQuestNative.migrate(QuestProgress.get(player), player.getUUID(), data);
            data.saveIfChanged();
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Grindless.LOG.error("Refused native quest admission before legacy reward import completed", exception);
            player.connection.disconnect(Component.translatable("chat.grindless.quest.backend_unavailable"));
            return false;
        }
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player.tickCount % 20 == 0
                && event.player instanceof ServerPlayer player) {
            QuestProgress legacy = QuestProgress.get(player);
            if (ready != null && legacy.usesNativeClaims(player.getUUID())) {
                TeamData data = verifiedFile(player.server).getOrCreateTeamData(player);
                if (data == null) {
                    throw new IllegalStateException("Native quest player has no framework team progress");
                }
                FtbQuestNative.refresh(legacy, player.getUUID(), data);
            }
        }
    }
}
