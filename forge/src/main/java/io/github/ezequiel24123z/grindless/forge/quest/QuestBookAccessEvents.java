package io.github.ezequiel24123z.grindless.forge.quest;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.quest.QuestBookAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Book delivery is available even when the optional quest renderer is absent. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class QuestBookAccessEvents {

    private static final Set<UUID> WAITING_FOR_SPACE = new HashSet<>();

    private QuestBookAccessEvents() {
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recover(player, true);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recover(player, true);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player.tickCount % 20 == 0
                && event.player instanceof ServerPlayer player && WAITING_FOR_SPACE.contains(player.getUUID())) {
            recover(player, false);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        WAITING_FOR_SPACE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        WAITING_FOR_SPACE.clear();
    }

    private static void recover(ServerPlayer player, boolean notifyFull) {
        QuestBookAccess.Result result = QuestBookAccess.ensure(player);
        if (result == QuestBookAccess.Result.INVENTORY_FULL) {
            WAITING_FOR_SPACE.add(player.getUUID());
            if (notifyFull) {
                player.displayClientMessage(Component.translatable("chat.grindless.quest.book.space"), false);
            }
        } else {
            WAITING_FOR_SPACE.remove(player.getUUID());
        }
    }
}
