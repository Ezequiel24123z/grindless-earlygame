package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.container.ItemExtract;
import io.github.ezequiel24123z.grindless.container.ItemInsert;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.PoweredLogisticsBlockEntity;
import io.github.ezequiel24123z.grindless.machine.StatusDebounce;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A powered inserter. Subclasses choose how many items move and which items are allowed.
 */
public abstract class ItemArmBlockEntity extends PoweredLogisticsBlockEntity {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_HELD = "Held";

    private final StatusDebounce display = new StatusDebounce();
    private TickSubscription working;
    private int progress;
    private ItemStack held = ItemStack.EMPTY;

    protected ItemArmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract int stackSize();

    protected abstract boolean allows(ItemStack stack);

    /** A neighbour that will not say which item it holds. The stack arm still takes it. */
    protected boolean allowsUnseen() {
        return true;
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::work);
    }

    private void work() {
        Direction drop = facing();
        Direction pickup = drop.getOpposite();
        boolean transferring = !held.isEmpty();
        boolean hasSource = acceptableSource(pickup);
        boolean hasWork = transferring || hasSource;
        if (!hasWork) {
            progress = 0;
            show(MachineStatus.IDLE);
            return;
        }
        if (!draw()) {
            show(MachineStatus.STARVED);
            return;
        }
        if (transferring) {
            ItemStack before = held;
            held = deliver(drop, held);
            if (!held.isEmpty() && held.getCount() == before.getCount()) {
                show(MachineStatus.BLOCKED);
            } else {
                show(MachineStatus.RUNNING);
            }
            setChanged();
            return;
        }
        progress++;
        if (ArmLogic.CYCLE_TICKS <= progress) {
            ItemStack taken = ItemExtract.fromNeighbour(getLevel(), getBlockPos(), pickup, stackSize());
            if (!taken.isEmpty() && allows(taken)) {
                held = deliver(drop, taken);
            } else if (!taken.isEmpty()) {
                ItemStack back = ItemInsert.intoNeighbour(getLevel(), getBlockPos(), pickup, taken);
                held = back;
            }
            progress = 0;
            setChanged();
        }
        show(MachineStatus.RUNNING);
    }

    private boolean acceptableSource(Direction pickup) {
        if (!ItemExtract.hasExtractable(getLevel(), getBlockPos(), pickup)) {
            return false;
        }
        ItemStack preview = ItemExtract.preview(getLevel(), getBlockPos(), pickup);
        if (preview.isEmpty()) {
            return allowsUnseen();
        }
        return allows(preview);
    }

    private ItemStack deliver(Direction drop, ItemStack stack) {
        BlockEntity dest = getLevel().getBlockEntity(getBlockPos().relative(drop));
        if (dest instanceof BeltEndpoint belt) {
            return belt.insert(drop.getOpposite(), stack);
        }
        return ItemInsert.intoNeighbour(getLevel(), getBlockPos(), drop, stack);
    }

    private void show(MachineStatus status) {
        if (display.observe(status)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_PROGRESS, progress);
        if (!held.isEmpty()) {
            tag.put(KEY_HELD, held.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(KEY_PROGRESS));
        held = tag.contains(KEY_HELD) ? ItemStack.of(tag.getCompound(KEY_HELD)) : ItemStack.EMPTY;
    }
}
