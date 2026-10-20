package io.github.ezequiel24123z.grindless.quest;

import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Counts physical quest evidence once per inventory slot, including the player's offhand slot. */
public final class QuestInventoryEvidence {

    private QuestInventoryEvidence() {
    }

    /** Counts only the named item and, for a Matrix task, its exact valid physical rating. */
    public static int count(Container inventory, QuestCatalogue.Task task) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matches(task, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean matches(QuestCatalogue.Task task, ItemStack stack) {
        if (stack.isEmpty() || !task.subject().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            return false;
        }
        if (task.evidence() != QuestCatalogue.Evidence.MATRIX) {
            return true;
        }
        return stack.getItem() instanceof ControlMatrixItem matrix
                && matrix.spec(stack).map(value -> value.rating() == task.matrixRating()).orElse(false);
    }
}
