package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.PoweredLogisticsBlockEntity;
import io.github.ezequiel24123z.grindless.machine.StatusDebounce;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moves one bucket a second from the back face to the front (ADR-0082).
 */
public final class FluidManipulatorBlockEntity extends PoweredLogisticsBlockEntity {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_HELD = "Held";

    private final StatusDebounce display = new StatusDebounce();
    private TickSubscription working;
    private int progress;
    private FluidState held = FluidState.EMPTY;

    public FluidManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_MANIPULATOR.get(), pos, state);
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
        boolean hasSource = FluidPull.hasExtractable(getLevel(), getBlockPos(), pickup);
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
            held = deliver(drop, held);
            setChanged();
        } else {
            progress++;
            if (progress >= PressureLogic.MANIPULATOR_TICKS) {
                FluidState taken = FluidPull.fromNeighbour(getLevel(), getBlockPos(), pickup,
                        PressureLogic.MANIPULATOR_MB);
                if (!taken.isEmpty()) {
                    held = deliver(drop, taken);
                }
                progress = 0;
                setChanged();
            }
        }
        show(held.isEmpty() && !hasSource ? MachineStatus.IDLE : MachineStatus.RUNNING);
    }

    private FluidState deliver(Direction drop, FluidState state) {
        return FluidInsert.intoNeighbour(getLevel(), getBlockPos(), drop, state);
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
        tag.put(KEY_HELD, FluidNbt.save(held));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(KEY_PROGRESS));
        held = FluidNbt.load(tag.getCompound(KEY_HELD));
    }
}
