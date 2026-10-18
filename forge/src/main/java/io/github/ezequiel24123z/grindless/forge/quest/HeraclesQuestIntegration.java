package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.JsonParseException;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.tasks.QuestTasks;
import earth.terrarium.heracles.common.handlers.progress.QuestProgressHandler;
import earth.terrarium.heracles.common.handlers.quests.QuestHandler;
import earth.terrarium.heracles.common.utils.ModUtils;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestClaimService;
import io.github.ezequiel24123z.grindless.quest.QuestEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestInventoryEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Optional binding to one pinned renderer; all gameplay claims remain in common. */
public final class HeraclesQuestIntegration {

    private static Map<String, Quest> definitions = Map.of();
    private static Map<String, String> encoded = Map.of();
    private static final Map<UUID, HeraclesQuestNetwork.Snapshot> LAST_SENT = new HashMap<>();
    private static boolean available;
    private static String failure;

    private HeraclesQuestIntegration() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            QuestTasks.register(GrindlessClaimTask.TYPE);
            HeraclesQuestNetwork.init();
            try {
                definitions = HeraclesDefinitions.create();
                encoded = HeraclesDefinitions.encode(definitions);
                HeraclesDefinitions.synchronize(FMLPaths.CONFIGDIR.get(), encoded);
            } catch (IOException | JsonParseException | IllegalStateException exception) {
                unavailable(exception);
            }
        }));
        MinecraftForge.EVENT_BUS.addListener(HeraclesQuestIntegration::started);
        MinecraftForge.EVENT_BUS.addListener(HeraclesQuestIntegration::reloaded);
        MinecraftForge.EVENT_BUS.addListener(HeraclesQuestIntegration::tick);
        MinecraftForge.EVENT_BUS.addListener(HeraclesQuestIntegration::login);
        MinecraftForge.EVENT_BUS.addListener(HeraclesQuestIntegration::logout);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            available = false;
            LAST_SENT.clear();
        });
    }

    private static void started(ServerStartedEvent event) {
        verify();
        if (available) {
            Grindless.LOG.info("Heracles quest projection ready: {} catalogue quests; legacy claims remain authoritative",
                    definitions.size());
        }
    }

    private static void reloaded(OnDatapackSyncEvent event) {
        verify();
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            refresh(player, true);
        }
    }

    private static void verify() {
        try {
            if (definitions.isEmpty() || QuestHandler.failedToLoad) {
                throw new IllegalStateException("Heracles did not load the generated Grindless definitions");
            }
            HeraclesDefinitions.verifyFiles(FMLPaths.CONFIGDIR.get(), encoded);
            for (var entry : definitions.entrySet()) {
                if (!HeraclesDefinitions.matches(entry.getValue(), QuestHandler.get(entry.getKey()))) {
                    throw new IllegalStateException("Missing, colliding or unsafe native quest: " + entry.getKey());
                }
            }
            available = true;
            failure = null;
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            unavailable(exception);
        }
    }

    private static void unavailable(Exception exception) {
        available = false;
        if (!exception.toString().equals(failure)) {
            Grindless.LOG.error("Grindless quest backend unavailable; the legacy Quest Book remains usable", exception);
        }
        failure = exception.toString();
    }

    public static boolean open(ServerPlayer player) {
        if (!available) {
            diagnostic(player);
            return false;
        }
        refresh(player, true);
        player.closeContainer();
        ModUtils.openGroup(player, HeraclesDefinitions.GROUP);
        return true;
    }

    public static void claim(ServerPlayer sender, String taskId) {
        if (!available) {
            diagnostic(sender);
        } else {
            QuestClaimService.claim(sender, taskId);
        }
        refresh(sender, true);
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player, true);
            if (!available) {
                diagnostic(player);
            }
        }
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player.tickCount % 20 == 0
                && event.player instanceof ServerPlayer player) {
            refresh(player, false);
        }
    }

    private static void refresh(ServerPlayer player, boolean force) {
        Set<String> claimed = QuestProgress.get(player).claimed(player.getUUID());
        if (available) {
            var nativeProgress = QuestProgressHandler.getProgress(player.server, player.getUUID());
            List<String> changed = HeraclesClaimProjection.reconcile(nativeProgress, claimed, definitions);
            if (!changed.isEmpty()) {
                QuestProgressHandler.read(player.server).setDirty();
                QuestProgressHandler.sync(player, changed);
            }
        }
        HeraclesQuestNetwork.Snapshot snapshot = new HeraclesQuestNetwork.Snapshot(available,
                QuestClaimService.holdingBook(player), available ? QuestCatalogue.tasks().stream().map(task -> {
                    int held = QuestInventoryEvidence.count(player.getInventory(), task);
                    return new HeraclesQuestNetwork.State(task.id(), held, QuestLogic.consider(task, claimed,
                            QuestEvidence.met(task, held, player.level().dimension().location().toString())));
                }).toList() : List.of());
        if (force || !snapshot.equals(LAST_SENT.get(player.getUUID()))) {
            HeraclesQuestNetwork.send(player, snapshot);
            LAST_SENT.put(player.getUUID(), snapshot);
        }
    }

    private static void diagnostic(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("chat.grindless.quest.backend_unavailable"), false);
    }
}
