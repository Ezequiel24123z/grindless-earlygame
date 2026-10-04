package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Inline magnet: one inlet (the back), passthrough front, ferromagnetic items to the left.
 *
 * <p>There is no filter to set. A full left side holds the metal (ADR-0080).
 */
public final class MagneticSeparatorBlockEntity extends BlockEntity implements BeltEndpoint {

    private static final String KEY_HELD = "Held";

    private ItemStack held = ItemStack.EMPTY;

    public MagneticSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNETIC_SEPARATOR.get(), pos, state);
    }

    public ItemStack held() {
        return held;
    }

    public Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    public Direction outletDirection(int outlet) {
        Direction facing = facing();
        return outlet == MagneticLogic.LEFT ? facing.getCounterClockWise() : facing;
    }

    @Override
    public boolean canInsert(Direction from, ItemStack stack) {
        return !stack.isEmpty() && held.isEmpty() && from == facing().getOpposite()
                && route(stack) >= 0;
    }

    @Override
    public ItemStack insert(Direction from, ItemStack stack) {
        if (!canInsert(from, stack)) {
            return stack;
        }
        int chosen = route(stack);
        ItemStack leftover = push(chosen, stack);
        if (!leftover.isEmpty()) {
            held = leftover.copy();
            leftover = ItemStack.EMPTY;
        }
        setChanged();
        publish();
        return leftover;
    }

    @Override
    public boolean canExtract(Direction from) {
        return false;
    }

    @Override
    public ItemStack extract(Direction from, int max) {
        return ItemStack.EMPTY;
    }

    public void flush() {
        if (held.isEmpty() || level == null || level.isClientSide()) {
            return;
        }
        int chosen = route(held);
        if (chosen < 0) {
            publish();
            return;
        }
        held = push(chosen, held);
        setChanged();
        publish();
    }

    private int route(ItemStack stack) {
        return MagneticLogic.route(BeltStacks.idOf(stack),
                open(MagneticLogic.FRONT, stack), open(MagneticLogic.LEFT, stack));
    }

    private boolean open(int outlet, ItemStack stack) {
        if (level == null) {
            return false;
        }
        Direction direction = outletDirection(outlet);
        BlockEntity next = level.getBlockEntity(worldPosition.relative(direction));
        return next instanceof BeltEndpoint endpoint
                && endpoint.canInsert(direction.getOpposite(), stack);
    }

    private ItemStack push(int outlet, ItemStack stack) {
        if (level == null) {
            return stack;
        }
        Direction direction = outletDirection(outlet);
        BlockEntity next = level.getBlockEntity(worldPosition.relative(direction));
        if (!(next instanceof BeltEndpoint endpoint)) {
            return stack;
        }
        return endpoint.insert(direction.getOpposite(), stack);
    }

    private void publish() {
        if (level == null) {
            return;
        }
        MachineProperties.publish(level, worldPosition,
                held.isEmpty() ? MachineStatus.IDLE : MachineStatus.RUNNING);
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!held.isEmpty()) {
            tag.put(KEY_HELD, held.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        held = tag.contains(KEY_HELD) ? ItemStack.of(tag.getCompound(KEY_HELD)) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
