package io.github.ezequiel24123z.grindless.quest;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Stages a complete reward with vanilla stacking before touching the player's main inventory. */
public final class QuestRewardDelivery {

    private QuestRewardDelivery() {
    }

    static Optional<Runnable> prepare(Inventory inventory, List<ItemStack> rewards) {
        ItemStack[] contents = new ItemStack[inventory.items.size()];
        for (int slot = 0; slot < contents.length; slot++) {
            contents[slot] = inventory.getItem(slot).copy();
        }
        SimpleContainer staged = new SimpleContainer(contents);
        for (ItemStack reward : rewards) {
            if (!staged.addItem(reward.copy()).isEmpty()) {
                return Optional.empty();
            }
        }
        return Optional.of(() -> {
            for (int slot = 0; slot < staged.getContainerSize(); slot++) {
                if (!ItemStack.matches(inventory.getItem(slot), staged.getItem(slot))) {
                    inventory.setItem(slot, staged.getItem(slot));
                }
            }
            inventory.setChanged();
        });
    }
}
