package io.github.ezequiel24123z.grindless.quest;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Recoverable utility-book access; only an explicit quest claim may deliver starter supplies. */
public final class QuestBookAccess {

    public enum Result {
        PRESENT,
        DELIVERED,
        INVENTORY_FULL,
        /** Reserved legacy code; post-starter recovery no longer emits it. */
        STARTER_CLAIMED
    }

    private QuestBookAccess() {
    }

    public static Result ensure(ServerPlayer player) {
        if (!player.server.isSameThread()) {
            throw new IllegalStateException("Quest book recovery must execute on the server thread");
        }
        Result result = ensure(QuestProgress.get(player), player.getUUID(), player.getInventory(),
                ModItems.QUEST_BOOK.get());
        if (result == Result.DELIVERED) {
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("chat.grindless.quest.book.restored"), false);
        }
        return result;
    }

    static Result ensure(QuestProgress progress, UUID player, Inventory inventory, Item book) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(book)) {
                return Result.PRESENT;
            }
        }
        Optional<Runnable> delivery = QuestRewardDelivery.prepare(inventory, List.of(new ItemStack(book)));
        if (delivery.isEmpty()) {
            return Result.INVENTORY_FULL;
        }
        delivery.get().run();
        return Result.DELIVERED;
    }
}
