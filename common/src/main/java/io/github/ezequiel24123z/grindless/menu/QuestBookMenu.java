package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestClaimService;
import io.github.ezequiel24123z.grindless.quest.QuestEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestInventoryEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

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
        this.state = new SimpleContainerData(QuestCatalogue.tasks().size() * 2 + 1);
        for (int task = 0; task < QuestCatalogue.tasks().size(); task++) {
            state.set(task, QuestLogic.Status.UNMET.ordinal());
        }
        addDataSlots(state);
    }

    public boolean chapterUnlocked(QuestCatalogue.Chapter chapter) {
        return chapter == QuestCatalogue.Chapter.T0 || state.get(QuestCatalogue.tasks().size() * 2) == 1;
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
        return QuestClaimService.holdingBook(player)
                || player.getMainHandItem().is(ModItems.FIELD_GUIDE.get())
                || player.getOffhandItem().is(ModItems.FIELD_GUIDE.get());
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
        QuestClaimService.Result result = QuestClaimService.claim(server, tasks.get(id).id());
        refresh();
        return result == QuestClaimService.Result.CLAIMED;
    }

    private void refresh() {
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        QuestProgress progress = QuestProgress.get(server);
        Set<String> claimed = progress.claimed(server.getUUID());
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        state.set(tasks.size() * 2, QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, claimed) ? 1 : 0);
        for (int i = 0; i < tasks.size(); i++) {
            QuestCatalogue.Task task = tasks.get(i);
            int held = held(server, task);
            state.set(i, QuestLogic.consider(task, claimed, QuestEvidence.met(task, held,
                    server.level().dimension().location().toString())).ordinal());
            state.set(tasks.size() + i, held);
        }
    }

    private static int held(ServerPlayer player, QuestCatalogue.Task task) {
        return QuestInventoryEvidence.count(player.getInventory(), task);
    }

}
