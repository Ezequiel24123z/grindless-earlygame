package io.github.ezequiel24123z.grindless.container;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A machine inventory whose NBT preserves empty slots instead of compacting recipe inputs and outputs. */
public final class SlottedContainer extends SimpleContainer {

    private static final Logger LOG = LoggerFactory.getLogger(SlottedContainer.class);

    /** Creates an empty inventory with fixed slot meanings. */
    public SlottedContainer(int size) {
        super(size);
    }

    @Override
    public ListTag createTag() {
        NonNullList<ItemStack> slots = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < slots.size(); slot++) {
            slots.set(slot, getItem(slot));
        }
        return ContainerHelper.saveAllItems(new CompoundTag(), slots).getList("Items", Tag.TAG_COMPOUND);
    }

    /**
     * Restores indexed slots, or the recorded order of a legacy compact list.
     * Missing historical positions cannot be inferred safely from an item's type.
     */
    @Override
    public void fromTag(ListTag saved) {
        ListTag indexed = saved.copy();
        boolean legacy = false;
        for (int index = 0; index < indexed.size(); index++) {
            CompoundTag entry = indexed.getCompound(index);
            if (!entry.contains("Slot")) {
                entry.putByte("Slot", (byte) index);
                legacy = true;
            }
        }
        if (legacy) {
            LOG.warn("Loading a legacy compact machine inventory in recorded order; "
                    + "original empty slot positions are unavailable");
        }
        CompoundTag root = new CompoundTag();
        root.put("Items", indexed);
        NonNullList<ItemStack> slots = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(root, slots);
        clearContent();
        for (int slot = 0; slot < slots.size(); slot++) {
            setItem(slot, slots.get(slot));
        }
    }
}
