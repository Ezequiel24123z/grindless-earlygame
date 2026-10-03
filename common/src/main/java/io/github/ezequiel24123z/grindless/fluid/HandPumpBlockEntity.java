package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Unpowered water source. Looks at the block it faces and the block below for a water source,
 * fills a 1 B buffer, and pushes into the facing neighbour.
 */
public final class HandPumpBlockEntity extends BlockEntity implements FluidEndpoint {

    private static final String KEY_FLUID = "Fluid";

    private final FluidBuffer buffer = FluidBuffer.ambient(FluidLogic.BUCKET);

    public HandPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAND_PUMP.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HandPumpBlockEntity pump) {
        if (level.isClientSide()) {
            return;
        }
        pump.work();
    }

    private void work() {
        boolean source = hasWaterSource();
        boolean moved = false;
        if (source && buffer.space() > 0) {
            int take = Math.min(FluidLogic.PUMP_MB_PER_TICK, buffer.space());
            buffer.offer(FluidState.of(FluidLogic.WATER, take));
            moved = true;
        }
        if (!buffer.isEmpty()) {
            FluidState pulse = buffer.extract(FluidLogic.WATER, FluidLogic.PUMP_MB_PER_TICK);
            FluidState leftover = FluidInsert.intoNeighbour(level, worldPosition, facing(), pulse);
            if (!leftover.isEmpty()) {
                buffer.offer(leftover);
            } else {
                moved = true;
            }
        }
        MachineProperties.publish(level, worldPosition, FluidLogic.pumpStatus(source, moved));
        if (moved) {
            setChanged();
            sync();
        }
    }

    private boolean hasWaterSource() {
        if (level == null) {
            return false;
        }
        return isSource(worldPosition.relative(facing())) || isSource(worldPosition.below());
    }

    private boolean isSource(BlockPos pos) {
        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.isSource();
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
        return state;
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
        setChanged();
        sync();
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

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
