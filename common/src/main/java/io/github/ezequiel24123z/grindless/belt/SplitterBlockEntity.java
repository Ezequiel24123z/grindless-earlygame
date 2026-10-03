package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * One-block splitter: one input (the back) and three outputs (front, left, right).
 *
 * <p>Filters live on the three faces. An empty filter accepts everything; a matching filter
 * beats an open face. Equal priority round-robins (ADR-0060).
 */
public final class SplitterBlockEntity extends BlockEntity implements BeltEndpoint {

    private static final String KEY_FILTERS = "Filters";
    private static final String KEY_PRIORITY = "Priority";
    private static final String KEY_LAST = "Last";
    private static final String KEY_HELD = "Held";

    private final String[] filters = new String[]{"", "", ""};
    private final int[] priority = new int[]{0, 0, 0};
    private int lastIndex = -1;
    private ItemStack held = ItemStack.EMPTY;

    public SplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPLITTER.get(), pos, state);
    }

    public String filter(int outlet) {
        return filters[outlet];
    }

    public void setFilter(int outlet, String id) {
        filters[outlet] = id == null ? "" : id;
        setChanged();
    }

    public Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    public Direction outletDirection(int outlet) {
        Direction facing = facing();
        return switch (outlet) {
            case SplitterLogic.LEFT -> facing.getCounterClockWise();
            case SplitterLogic.RIGHT -> facing.getClockWise();
            default -> facing;
        };
    }

    public int outletOf(Direction direction) {
        Direction facing = facing();
        if (direction == facing) {
            return SplitterLogic.FRONT;
        }
        if (direction == facing.getCounterClockWise()) {
            return SplitterLogic.LEFT;
        }
        if (direction == facing.getClockWise()) {
            return SplitterLogic.RIGHT;
        }
        return -1;
    }

    @Override
    public boolean canInsert(Direction from, ItemStack stack) {
        if (stack.isEmpty() || from != facing().getOpposite()) {
            return false;
        }
        return held.isEmpty() && route(stack) >= 0;
    }

    @Override
    public ItemStack insert(Direction from, ItemStack stack) {
        if (!canInsert(from, stack) && (held.isEmpty() || from != facing().getOpposite())) {
            if (from != facing().getOpposite() || stack.isEmpty()) {
                return stack;
            }
        }
        if (!held.isEmpty()) {
            return stack;
        }
        int chosen = route(stack);
        if (chosen < 0) {
            return stack;
        }
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

    /** Tries to empty a held leftover into its last chosen outlet. */
    public void flush() {
        if (held.isEmpty() || level == null || level.isClientSide()) {
            return;
        }
        int chosen = lastIndex >= 0 ? lastIndex : route(held);
        if (chosen < 0) {
            publish();
            return;
        }
        held = push(chosen, held);
        setChanged();
        publish();
    }

    private int route(ItemStack stack) {
        String id = BeltStacks.idOf(stack);
        List<SplitterLogic.Outlet> outlets = new ArrayList<>();
        for (int i = 0; i < SplitterLogic.OUTLETS; i++) {
            boolean matches = SplitterLogic.matches(id, filters[i]);
            outlets.add(new SplitterLogic.Outlet(i, !filters[i].isEmpty(), matches,
                    backedUp(i, stack), priority[i]));
        }
        return SplitterLogic.route(outlets, lastIndex);
    }

    private boolean backedUp(int outlet, ItemStack stack) {
        if (level == null) {
            return true;
        }
        BlockEntity next = level.getBlockEntity(worldPosition.relative(outletDirection(outlet)));
        if (!(next instanceof BeltEndpoint endpoint)) {
            return true;
        }
        return !endpoint.canInsert(outletDirection(outlet).getOpposite(), stack);
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
        boolean moving = !held.isEmpty();
        MachineProperties.publish(level, worldPosition,
                moving ? MachineStatus.RUNNING : MachineStatus.IDLE);
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < SplitterLogic.OUTLETS; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", filters[i]);
            entry.putInt(KEY_PRIORITY, priority[i]);
            list.add(entry);
        }
        tag.put(KEY_FILTERS, list);
        tag.putInt(KEY_LAST, lastIndex);
        if (!held.isEmpty()) {
            tag.put(KEY_HELD, held.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ListTag list = tag.getList(KEY_FILTERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < SplitterLogic.OUTLETS && i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            filters[i] = entry.getString("Id");
            priority[i] = entry.getInt(KEY_PRIORITY);
        }
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
