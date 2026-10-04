package io.github.ezequiel24123z.grindless.logic;

import io.github.ezequiel24123z.grindless.belt.BeltStacks;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.PoweredLogisticsBlockEntity;
import io.github.ezequiel24123z.grindless.machine.StatusDebounce;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runs the machine in front while the inventory behind holds fewer than 500 of one item
 * (ADR-0083).
 */
public final class LogicControllerBlockEntity extends PoweredLogisticsBlockEntity {

    private static final String KEY_FILTER = "Filter";

    private final StatusDebounce display = new StatusDebounce();
    private TickSubscription working;
    private String filter = "";
    private BlockPos heldMachine;

    public LogicControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGIC_CONTROLLER.get(), pos, state);
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
            return Component.translatable("chat.grindless.logic_controller.clear");
        }
        return Component.translatable("chat.grindless.logic_controller.filter",
                held.isEmpty() ? Component.literal(filter) : held.getHoverName());
    }

    /** Drops the hold so a mined controller does not leave a machine stopped. */
    public void release() {
        if (heldMachine != null && getLevel() != null
                && getLevel().getBlockEntity(heldMachine) instanceof MachineBlockEntity machine) {
            machine.setLogisticsHold(false);
        }
        heldMachine = null;
    }

    @Override
    public void setRemoved() {
        release();
        super.setRemoved();
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::work);
    }

    private void work() {
        Direction front = facing();
        Direction back = front.getOpposite();
        boolean blank = filter == null || filter.isBlank();
        int count = blank ? 0 : countBehind(back);
        boolean run = blank || SignalLogic.shouldRun(count, SignalLogic.THRESHOLD);
        BlockEntity ahead = getLevel().getBlockEntity(getBlockPos().relative(front));
        if (ahead instanceof MachineBlockEntity machine) {
            machine.setLogisticsHold(!run);
            heldMachine = machine.getBlockPos();
        } else {
            release();
        }
        int signal = run ? SignalLogic.RUN : SignalLogic.HOLD;
        for (Direction side : Direction.values()) {
            BlockEntity neighbour = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (neighbour instanceof SignalCableBlockEntity cable) {
                cable.setValue(signal);
            }
        }
        show(run ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    private int countBehind(Direction back) {
        BlockEntity behind = getLevel().getBlockEntity(getBlockPos().relative(back));
        if (!(behind instanceof Container container)) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && filter.equals(BeltStacks.idOf(stack))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void show(MachineStatus status) {
        if (display.observe(status)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
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
