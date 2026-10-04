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
 * Pulls rated fluid from behind and pushes it out the front. Does not invent water (ADR-0082).
 */
public final class ElectricPumpBlockEntity extends PoweredLogisticsBlockEntity implements FluidEndpoint {

    private static final String KEY_FLUID = "Fluid";

    private final FluidBuffer buffer = PressureLogic.buffer(FluidLogic.MACHINE_CAPACITY);
    private final StatusDebounce display = new StatusDebounce();
    private TickSubscription working;

    public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_PUMP.get(), pos, state);
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::work);
    }

    private void work() {
        Direction output = facing();
        Direction input = output.getOpposite();
        boolean source = FluidPull.hasExtractable(getLevel(), getBlockPos(), input);
        boolean powered = (source || !buffer.isEmpty()) && draw();
        boolean moved = false;
        if (powered && source && buffer.space() > 0) {
            int take = Math.min(PressureLogic.PUMP_MB_PER_TICK, buffer.space());
            FluidState pulled = FluidPull.fromNeighbour(getLevel(), getBlockPos(), input, take);
            if (!pulled.isEmpty()) {
                buffer.offer(pulled);
                moved = true;
            }
        }
        if (powered && !buffer.isEmpty()) {
            FluidState pulse = buffer.extract(buffer.state().id(), PressureLogic.PUMP_MB_PER_TICK);
            FluidState leftover = FluidInsert.intoNeighbour(getLevel(), getBlockPos(), output, pulse);
            if (!leftover.isEmpty()) {
                buffer.offer(leftover);
            }
            moved = leftover.millibuckets() < pulse.millibuckets() || moved;
        }
        boolean full = buffer.space() <= 0 && !moved;
        MachineStatus observed;
        if (!powered && (source || !buffer.isEmpty())) {
            observed = MachineStatus.STARVED;
        } else if (!source && buffer.isEmpty()) {
            observed = MachineStatus.STARVED;
        } else if (full) {
            observed = MachineStatus.BLOCKED;
        } else if (moved) {
            observed = MachineStatus.RUNNING;
        } else {
            observed = MachineStatus.IDLE;
        }
        if (display.observe(observed)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
        if (moved) {
            setChanged();
        }
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return false;
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        return state == null ? FluidState.EMPTY : state;
    }

    @Override
    public boolean canExtract(Direction from) {
        return from == facing() && !buffer.isEmpty();
    }

    @Override
    public FluidState extract(Direction from, int millibuckets) {
        if (!canExtract(from)) {
            return FluidState.EMPTY;
        }
        FluidState taken = buffer.extract(buffer.state().id(), millibuckets);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public FluidState contents() {
        return buffer.state();
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(KEY_FLUID, FluidNbt.save(buffer.state()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer.set(FluidNbt.load(tag.getCompound(KEY_FLUID)));
    }
}
