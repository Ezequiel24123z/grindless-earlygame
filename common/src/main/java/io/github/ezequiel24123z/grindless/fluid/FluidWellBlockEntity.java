package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.StatusDebounce;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Powered chunk water. Every chunk is an aquifer; brine and geothermal wait for a sink
 * (ADR-0078). Stays subscribed: the aquifer does not turn off, and a tank draining does
 * not tell the well.
 */
public final class FluidWellBlockEntity extends MachineBlockEntity implements FluidEndpoint {

    private static final String KEY_FLUID = "Fluid";

    private final FluidBuffer buffer = FluidBuffer.ambient(FluidLogic.MACHINE_CAPACITY);
    private final StatusDebounce display = new StatusDebounce();
    private TickSubscription working;

    public FluidWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_WELL.get(), pos, state);
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return narrowEnvelope();
    }

    @Override
    protected FluxTier ratedTier() {
        return FluxTier.F1;
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::work);
    }

    public void onNeighbourChanged() {
        updateSubscriptions();
    }

    private void work() {
        boolean powered = draw();
        boolean moved = false;
        if (powered && buffer.space() > 0) {
            int take = Math.min(FluidLogic.WELL_MB_PER_TICK, buffer.space());
            buffer.offer(FluidState.of(FluidLogic.WATER, take));
            moved = true;
        }
        if (!buffer.isEmpty() && getLevel() != null) {
            FluidState pulse = buffer.extract(FluidLogic.WATER, FluidLogic.WELL_MB_PER_TICK);
            FluidState leftover = FluidInsert.intoNeighbour(getLevel(), getBlockPos(), facing(), pulse);
            if (!leftover.isEmpty()) {
                buffer.offer(leftover);
            } else {
                moved = true;
            }
        }
        boolean full = buffer.space() <= 0 && !moved;
        MachineStatus observed;
        if (!powered && buffer.isEmpty()) {
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

    private boolean draw() {
        requestPower(ProcessLogic.FU_PER_TICK);
        long drawn = drawPower(ProcessLogic.FU_PER_TICK);
        if (drawn <= 0L && energy().getStored() >= ProcessLogic.FU_PER_TICK) {
            energy().setStored(energy().getStored() - ProcessLogic.FU_PER_TICK);
            drawn = ProcessLogic.FU_PER_TICK;
        }
        return drawn > 0L;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
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
        FluidState taken = buffer.extract(FluidLogic.WATER, millibuckets);
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
