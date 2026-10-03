package io.github.ezequiel24123z.grindless.belt;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Converts lane data to stacks and NBT. The only place a belt item becomes a real stack
 * (ADR-0008).
 */
public final class BeltStacks {

    private static final String KEY_ID = "Id";
    private static final String KEY_COUNT = "Count";
    private static final String KEY_POS = "Pos";
    private static final String KEY_ITEMS = "Items";

    private BeltStacks() {
    }

    public static ItemStack toStack(LaneItem item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(item.id());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id), item.count());
    }

    public static LaneItem fromStack(ItemStack stack, double position) {
        if (stack.isEmpty()) {
            return null;
        }
        return new LaneItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(), position);
    }

    public static String idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static void saveLane(CompoundTag into, Lane lane) {
        ListTag items = new ListTag();
        for (LaneItem item : lane.items()) {
            CompoundTag tag = new CompoundTag();
            tag.putString(KEY_ID, item.id());
            tag.putByte(KEY_COUNT, (byte) item.count());
            tag.putDouble(KEY_POS, item.position());
            items.add(tag);
        }
        into.put(KEY_ITEMS, items);
    }

    public static void loadLane(CompoundTag from, Lane lane) {
        lane.clear();
        ListTag items = from.getList(KEY_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag tag = items.getCompound(i);
            String id = tag.getString(KEY_ID);
            int count = tag.getByte(KEY_COUNT);
            if (id.isEmpty() || count <= 0) {
                continue;
            }
            lane.restore(new LaneItem(id, count, tag.getDouble(KEY_POS)));
        }
    }
}
