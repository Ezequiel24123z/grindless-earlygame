package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

/**
 * The quest book. No slots: a claim is a button, not an item moved by hand (ADR-0100).
 *
 * <p>Button ids are indexes into {@link QuestCatalogue#tasks()}. The first data-slot bank carries
 * {@link QuestLogic.Status}; the second carries the current held-item count. The client therefore
 * shows live progress without reading a player inventory or the quest save itself.
 */
public final class QuestBookMenu extends AbstractContainerMenu {

    private final Player player;
    private final SimpleContainerData state;

    public QuestBookMenu(int id, Inventory inventory) {
        super(ModMenus.QUEST_BOOK.get(), id);
        this.player = inventory.player;
        this.state = new SimpleContainerData(QuestCatalogue.tasks().size() * 2);
        addDataSlots(state);
    }

    public QuestLogic.Status status(int index) {
        if (index < 0 || index >= QuestCatalogue.tasks().size()) {
            return QuestLogic.Status.UNMET;
        }
        return QuestLogic.byCode(state.get(index));
    }

    /** Current inventory evidence for an item task, synchronized from the server. */
    public int held(int index) {
        int taskCount = QuestCatalogue.tasks().size();
        if (index < 0 || index >= taskCount) {
            return 0;
        }
        return state.get(taskCount + index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(ModItems.QUEST_BOOK.get())
                || player.getOffhandItem().is(ModItems.QUEST_BOOK.get());
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        if (id < 0 || id >= tasks.size()) {
            return false;
        }
        QuestCatalogue.Task task = tasks.get(id);
        QuestProgress progress = QuestProgress.get(server);
        Set<String> claimed = progress.claimed(server.getUUID());
        if (QuestLogic.consider(task, claimed, evidenceMet(server, task)) != QuestLogic.Status.CLAIMABLE) {
            return false;
        }
        if (!progress.claim(server.getUUID(), task.id())) {
            return false;
        }
        give(server, task.reward());
        server.displayClientMessage(Component.translatable("chat.grindless.quest.claimed",
                Component.translatable("gui.grindless.quest.task." + task.id())), false);
        refresh();
        return true;
    }

    private void refresh() {
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        QuestProgress progress = QuestProgress.get(server);
        Set<String> claimed = progress.claimed(server.getUUID());
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        for (int i = 0; i < tasks.size(); i++) {
            QuestCatalogue.Task task = tasks.get(i);
            int held = held(server, task.subject());
            state.set(i, QuestLogic.consider(task, claimed, QuestEvidence.met(task, held,
                    server.level().dimension().location().toString())).ordinal());
            state.set(tasks.size() + i, held);
        }
    }

    private static boolean evidenceMet(ServerPlayer player, QuestCatalogue.Task task) {
        return QuestEvidence.met(task, held(player, task.subject()),
                player.level().dimension().location().toString());
    }

    private static int held(ServerPlayer player, String itemId) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && itemId.equals(BuiltInRegistries.ITEM.getKey(offhand.getItem()).toString())) {
            count += offhand.getCount();
        }
        return count;
    }

    private static void give(ServerPlayer player, QuestCatalogue.Reward reward) {
        ResourceLocation id = ResourceLocation.tryParse(reward.itemId());
        if (id == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            return;
        }
        ItemStack stack = new ItemStack(item, reward.count());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
