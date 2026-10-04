package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** One item a second, whitelist of one id. An empty filter passes everything (ADR-0082). */
public final class FilterManipulatorBlockEntity extends ItemArmBlockEntity {

    private static final String KEY_FILTER = "Filter";

    private String filter = "";

    public FilterManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_MANIPULATOR.get(), pos, state);
    }

    public void setFilter(String itemId) {
        this.filter = itemId == null ? "" : itemId;
        setChanged();
    }

    public String filter() {
        return filter;
    }

    public Component filterLabel(ItemStack held) {
        if (filter == null || filter.isBlank()) {
            return Component.translatable("chat.grindless.filter_manipulator.clear");
        }
        return Component.translatable("chat.grindless.filter_manipulator.filter",
                held.isEmpty() ? Component.literal(filter) : held.getHoverName());
    }

    @Override
    protected int stackSize() {
        return ArmLogic.FILTER_STACK;
    }

    @Override
    protected boolean allows(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ArmLogic.allows(filter, BeltStacks.idOf(stack));
    }

    @Override
    protected boolean allowsUnseen() {
        return filter == null || filter.isBlank();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (filter != null && !filter.isBlank()) {
            tag.putString(KEY_FILTER, filter);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        filter = tag.getString(KEY_FILTER);
    }
}
