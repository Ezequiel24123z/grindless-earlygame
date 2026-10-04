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
 * One-block merger: three inlets (back, left, right) and one outlet (front).
 *
 * <p>Round-robin among the faces that actually try to insert (ADR-0071).
 */
public final class MergerBlockEntity extends BlockEntity implements BeltEndpoint {

    private static final String KEY_LAST = "Last";
    private static final String KEY_HELD = "Held";

    private int lastIndex = -1;
    private ItemStack held = ItemStack.EMPTY;

    public MergerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MERGER.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    public int inletOf(Direction from) {
        Direction facing = facing();
        if (from == facing.getOpposite()) {
            return MergerLogic.BACK;
        }
        if (from == facing.getCounterClockWise()) {
            return MergerLogic.LEFT;
        }
        if (from == facing.getClockWise()) {
            return MergerLogic.RIGHT;
        }
        return -1;
    }

    public ItemStack held() {
        return held;
    }

    @Override
    public boolean canInsert(Direction from, ItemStack stack) {
        if (stack.isEmpty() || !held.isEmpty()) {
            return false;
        }
        int inlet = inletOf(from);
        if (inlet < 0) {
            return false;
        }
        boolean[] ready = new boolean[MergerLogic.INLETS];
        ready[inlet] = true;
        return MergerLogic.pick(ready, lastIndex) == inlet;
    }

    @Override
    public ItemStack insert(Direction from, ItemStack stack) {
        if (!canInsert(from, stack)) {
            return stack;
        }
        held = stack.copy();
        lastIndex = inletOf(from);
        setChanged();
        publish();
        return ItemStack.EMPTY;
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
        Direction facing = facing();
        BlockEntity next = level.getBlockEntity(worldPosition.relative(facing));
        if (next instanceof BeltEndpoint endpoint
                && endpoint.canInsert(facing.getOpposite(), held)) {
            held = endpoint.insert(facing.getOpposite(), held);
            setChanged();
        }
        publish();
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
        tag.putInt(KEY_LAST, lastIndex);
        if (!held.isEmpty()) {
            tag.put(KEY_HELD, held.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastIndex = tag.getInt(KEY_LAST);
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
