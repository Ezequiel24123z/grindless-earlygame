package io.github.ezequiel24123z.grindless.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.Function;

/** Native main-inventory settlement only: no drops, capabilities, equipment or external callbacks. */
public final class ShopInventory {
    public record Prepared(ShopResult result, Transaction transaction) {
    }

    public static final class Transaction {
        private final Inventory inventory;
        private final ItemStack[] before;
        private final SimpleContainer after;
        private boolean used;

        private Transaction(Inventory inventory, ItemStack[] before, SimpleContainer after) {
            this.inventory = inventory;
            this.before = before;
            this.after = after;
        }

        public ShopResult commit() {
            if (used) {
                return ShopResult.STALE_REQUEST;
            }
            used = true;
            for (int slot = 0; slot < before.length; slot++) {
                if (!ItemStack.matches(inventory.getItem(slot), before[slot])) {
                    return ShopResult.STALE_INVENTORY;
                }
            }
            for (int slot = 0; slot < before.length; slot++) {
                if (!ItemStack.matches(before[slot], after.getItem(slot))) {
                    inventory.setItem(slot, after.getItem(slot).copy());
                }
            }
            inventory.setChanged();
            return ShopResult.TRADED;
        }
    }

    private ShopInventory() {
    }

    public static Prepared prepare(Inventory inventory, ShopCatalogue.Offer offer, Set<String> claimed) {
        return prepare(inventory, offer, claimed, ShopInventory::resolve);
    }

    static Prepared prepare(Inventory inventory, ShopCatalogue.Offer offer, Set<String> claimed,
                            Function<String, Item> resolve) {
        if (offer == null) {
            return rejected(ShopResult.INVALID_OFFER);
        }
        if (!claimed.containsAll(offer.requires())) {
            return rejected(ShopResult.LOCKED);
        }
        ItemStack[] before = new ItemStack[inventory.items.size()];
        ItemStack[] contents = new ItemStack[before.length];
        for (int slot = 0; slot < before.length; slot++) {
            before[slot] = inventory.getItem(slot).copy();
            contents[slot] = before[slot].copy();
        }
        SimpleContainer staged = new SimpleContainer(contents);
        for (var payment : offer.payment().entrySet()) {
            Item item = resolve.apply(payment.getKey());
            if (item == null || item == Items.AIR) {
                return rejected(ShopResult.INVALID_OFFER);
            }
            int remaining = payment.getValue();
            for (int slot = 0; slot < staged.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = staged.getItem(slot);
                if (!stack.is(item) || stack.hasTag()) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
            }
            if (remaining != 0) {
                return rejected(ShopResult.MISSING_PAYMENT);
            }
        }
        Item output = resolve.apply(offer.goods().item());
        if (output == null || output == Items.AIR) {
            return rejected(ShopResult.INVALID_OFFER);
        }
        ItemStack delivery = new ItemStack(output, offer.goods().count());
        if (delivery.getCount() > delivery.getMaxStackSize()) {
            return rejected(ShopResult.INVALID_OFFER);
        }
        if (!staged.addItem(delivery).isEmpty()) {
            return rejected(ShopResult.INVENTORY_FULL);
        }
        return new Prepared(ShopResult.READY, new Transaction(inventory, before, staged));
    }

    public static Item resolve(String item) {
        ResourceLocation id = ResourceLocation.tryParse(item);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
    }

    private static Prepared rejected(ShopResult result) {
        return new Prepared(result, null);
    }
}
