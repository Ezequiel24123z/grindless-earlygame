package io.github.ezequiel24123z.grindless.quest;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** The single server-side reward claim path, shared by the legacy menu and optional renderers. */
public final class QuestClaimService {

    public enum Result {
        CLAIMED,
        ALREADY_CLAIMED,
        LOCKED,
        UNMET,
        BOOK_REQUIRED,
        UNKNOWN_TASK,
        INVALID_REWARD
    }

    private QuestClaimService() {
    }

    public static boolean holdingBook(Player player) {
        return player.getMainHandItem().is(ModItems.QUEST_BOOK.get())
                || player.getOffhandItem().is(ModItems.QUEST_BOOK.get());
    }

    public static Result claim(ServerPlayer player, String taskId) {
        if (!player.server.isSameThread()) {
            throw new IllegalStateException("Quest claims must execute on the server thread");
        }
        Result result = claim(QuestProgress.get(player), player.getUUID(), taskId, holdingBook(player),
                task -> QuestEvidence.met(task, QuestInventoryEvidence.count(player.getInventory(), task),
                        player.level().dimension().location().toString()),
                QuestClaimService::rewardStack, stack -> {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                });
        if (result == Result.INVALID_REWARD || result == Result.UNKNOWN_TASK) {
            Grindless.LOG.error("Rejected quest claim {} from {}: {}", taskId, player.getUUID(), result);
        }
        if (result == Result.CLAIMED) {
            player.displayClientMessage(Component.translatable("chat.grindless.quest.claimed",
                    Component.translatable("gui.grindless.quest.task." + taskId)), false);
        } else {
            player.displayClientMessage(Component.translatable("chat.grindless.quest.rejected."
                    + result.name().toLowerCase(java.util.Locale.ROOT)), false);
        }
        return result;
    }

    static Result claim(QuestProgress progress, UUID player, String taskId, boolean holdingBook,
                        Predicate<QuestCatalogue.Task> evidence, Function<QuestCatalogue.Reward, ItemStack> resolve,
                        Consumer<ItemStack> give) {
        QuestCatalogue.Task task = QuestCatalogue.byId(taskId);
        if (task == null || player == null) {
            return Result.UNKNOWN_TASK;
        }
        Set<String> claimed = progress.claimed(player);
        if (claimed.contains(taskId)) {
            return Result.ALREADY_CLAIMED;
        }
        if (!holdingBook) {
            return Result.BOOK_REQUIRED;
        }
        QuestLogic.Status status = QuestLogic.consider(task, claimed, evidence.test(task));
        if (status == QuestLogic.Status.LOCKED) {
            return Result.LOCKED;
        }
        if (status != QuestLogic.Status.CLAIMABLE) {
            return Result.UNMET;
        }
        ItemStack reward = resolve.apply(task.reward());
        if (reward == null || reward.isEmpty() || reward.getCount() != task.reward().count()
                || reward.getCount() > reward.getMaxStackSize()) {
            return Result.INVALID_REWARD;
        }
        if (!progress.claim(player, taskId)) {
            return Result.ALREADY_CLAIMED;
        }
        give.accept(reward);
        return Result.CLAIMED;
    }

    private static ItemStack rewardStack(QuestCatalogue.Reward reward) {
        ResourceLocation id = ResourceLocation.tryParse(reward.itemId());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || reward.count() < 1) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, reward.count());
    }
}
