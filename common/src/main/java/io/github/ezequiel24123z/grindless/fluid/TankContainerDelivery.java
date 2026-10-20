package io.github.ezequiel24123z.grindless.fluid;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Vanilla inventory staging for a tank exchange, with no dropped or silently discarded result. */
public final class TankContainerDelivery {

    private TankContainerDelivery() {
    }

    public static Optional<Prepared> prepare(Inventory inventory, InteractionHand hand, ItemStack expectedHand,
                                             boolean creative, boolean intoTank, ItemStack result) {
        int handSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected
                : inventory.items.size() + inventory.armor.size();
        if (expectedHand.isEmpty() || handSlot < 0 || handSlot >= inventory.getContainerSize()
                || !ItemStack.matches(inventory.getItem(handSlot), expectedHand)) {
            return Optional.empty();
        }
        ItemStack[] before = new ItemStack[inventory.getContainerSize()];
        ItemStack[] after = new ItemStack[before.length];
        for (int slot = 0; slot < before.length; slot++) {
            before[slot] = inventory.getItem(slot).copy();
            after[slot] = before[slot].copy();
        }
        if (!creative || !intoTank) {
            if (expectedHand.getCount() == 1) {
                after[handSlot] = result.copy();
            } else {
                after[handSlot].shrink(1);
                ItemStack[] main = new ItemStack[inventory.items.size()];
                for (int slot = 0; slot < main.length; slot++) {
                    main[slot] = after[slot].copy();
                }
                SimpleContainer staged = new SimpleContainer(main);
                if (!staged.addItem(result.copy()).isEmpty()) {
                    return Optional.empty();
                }
                for (int slot = 0; slot < main.length; slot++) {
                    after[slot] = staged.getItem(slot).copy();
                }
            }
        }
        return Optional.of(new Prepared(inventory, inventory.selected, before, after));
    }

    public static final class Prepared implements NativeFluidStore.Settlement {
        private final Inventory inventory;
        private final int selected;
        private final ItemStack[] before;
        private final ItemStack[] after;

        private Prepared(Inventory inventory, int selected, ItemStack[] before, ItemStack[] after) {
            this.inventory = inventory;
            this.selected = selected;
            this.before = before;
            this.after = after;
        }

        public boolean unchanged() {
            if (inventory.selected != selected) {
                return false;
            }
            for (int slot = 0; slot < before.length; slot++) {
                if (!ItemStack.matches(inventory.getItem(slot), before[slot])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void apply() {
            if (!unchanged()) {
                throw new IllegalStateException("Player inventory changed during a tank container exchange");
            }
            assign(after);
        }

        @Override
        public void restore() {
            assign(before);
        }

        private void assign(ItemStack[] snapshot) {
            for (int slot = 0; slot < snapshot.length; slot++) {
                if (!ItemStack.matches(inventory.getItem(slot), snapshot[slot])) {
                    inventory.setItem(slot, snapshot[slot].copy());
                }
            }
            inventory.setChanged();
        }
    }
}
