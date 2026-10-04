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
 * One end of a T1 tunnel pair. Items enter the back and leave the partner's front, skipping
 * one to five empty blocks (ADR-0071).
 */
public final class TunnelBeltBlockEntity extends BlockEntity implements BeltEndpoint {

    private static final String KEY_HELD = "Held";

    private ItemStack held = ItemStack.EMPTY;

    public TunnelBeltBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUNNEL_BELT.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    public ItemStack held() {
        return held;
    }

    @Override
    public boolean canInsert(Direction from, ItemStack stack) {
        return !stack.isEmpty() && held.isEmpty() && from == facing().getOpposite()
                && partner() != null && open(partner(), stack);
    }

    @Override
    public ItemStack insert(Direction from, ItemStack stack) {
        if (!canInsert(from, stack)) {
            return stack;
        }
        TunnelBeltBlockEntity other = partner();
        ItemStack leftover = push(other, stack);
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
        TunnelBeltBlockEntity other = partner();
        if (other == null) {
            publish();
            return;
        }
        held = push(other, held);
        setChanged();
        publish();
    }

    TunnelBeltBlockEntity partner() {
        if (level == null) {
            return null;
        }
        Direction facing = facing();
        for (int steps = 1; steps <= TunnelLogic.maxSteps(); steps++) {
            if (!TunnelLogic.inRange(steps)) {
                continue;
            }
            BlockPos at = worldPosition.relative(facing, steps);
            if (level.getBlockEntity(at) instanceof TunnelBeltBlockEntity other
                    && other.facing() == facing) {
                return other;
            }
        }
        return null;
    }

    private boolean open(TunnelBeltBlockEntity other, ItemStack stack) {
        if (level == null) {
            return false;
        }
        Direction facing = other.facing();
        BlockEntity next = level.getBlockEntity(other.worldPosition.relative(facing));
        return next instanceof BeltEndpoint endpoint
                && endpoint.canInsert(facing.getOpposite(), stack);
    }

    private ItemStack push(TunnelBeltBlockEntity other, ItemStack stack) {
        if (level == null || other == null) {
            return stack;
        }
        Direction facing = other.facing();
        BlockEntity next = level.getBlockEntity(other.worldPosition.relative(facing));
        if (!(next instanceof BeltEndpoint endpoint)) {
            return stack;
        }
        return endpoint.insert(facing.getOpposite(), stack);
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
