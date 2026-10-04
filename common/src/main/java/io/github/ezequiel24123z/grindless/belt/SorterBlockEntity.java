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
 * Inline sorter: one inlet (the back), passthrough front, optional left and right filters.
 *
 * <p>Empty filters do not steal. A matching side that is backed up holds the item (ADR-0072).
 */
public final class SorterBlockEntity extends BlockEntity implements BeltEndpoint {

    private static final String KEY_LEFT = "Left";
    private static final String KEY_RIGHT = "Right";
    private static final String KEY_LAST = "Last";
    private static final String KEY_HELD = "Held";

    private String leftFilter = "";
    private String rightFilter = "";
    private int lastIndex = -1;
    private ItemStack held = ItemStack.EMPTY;

    public SorterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTER.get(), pos, state);
    }

    public String leftFilter() {
        return leftFilter;
    }

    public String rightFilter() {
        return rightFilter;
    }

    public void setFilter(int outlet, String id) {
        String value = id == null ? "" : id;
        if (outlet == SorterLogic.LEFT) {
            leftFilter = value;
        } else if (outlet == SorterLogic.RIGHT) {
            rightFilter = value;
        }
        setChanged();
    }

    public ItemStack held() {
        return held;
    }

    public Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    public Direction outletDirection(int outlet) {
        Direction facing = facing();
        return switch (outlet) {
            case SorterLogic.LEFT -> facing.getCounterClockWise();
            case SorterLogic.RIGHT -> facing.getClockWise();
            default -> facing;
        };
    }

    public int outletOf(Direction direction) {
        Direction facing = facing();
        if (direction == facing) {
            return SorterLogic.FRONT;
        }
        if (direction == facing.getCounterClockWise()) {
            return SorterLogic.LEFT;
        }
        if (direction == facing.getClockWise()) {
            return SorterLogic.RIGHT;
        }
        return -1;
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
        lastIndex = chosen;
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
        return SorterLogic.route(BeltStacks.idOf(stack), leftFilter, rightFilter,
                open(SorterLogic.FRONT, stack), open(SorterLogic.LEFT, stack),
                open(SorterLogic.RIGHT, stack), lastIndex);
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
        tag.putString(KEY_LEFT, leftFilter);
        tag.putString(KEY_RIGHT, rightFilter);
        tag.putInt(KEY_LAST, lastIndex);
        if (!held.isEmpty()) {
            tag.put(KEY_HELD, held.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        leftFilter = tag.getString(KEY_LEFT);
        rightFilter = tag.getString(KEY_RIGHT);
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
