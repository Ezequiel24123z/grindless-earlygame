package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Moves up to twelve items a second (ADR-0082). */
public final class StackManipulatorBlockEntity extends ItemArmBlockEntity {

    public StackManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STACK_MANIPULATOR.get(), pos, state);
    }

    @Override
    protected int stackSize() {
        return ArmLogic.STACK;
    }

    @Override
    protected boolean allows(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }
}
