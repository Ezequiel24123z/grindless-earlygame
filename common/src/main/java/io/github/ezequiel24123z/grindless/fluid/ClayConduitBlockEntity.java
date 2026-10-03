package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One Clay Conduit tile. Ambient liquid, gravity or level, facing is downhill-or-along.
 */
public final class ClayConduitBlockEntity extends BlockEntity implements FluidEndpoint {

    private static final String KEY_FLUID = "Fluid";

    private final FluidBuffer buffer = FluidBuffer.ambient(FluidLogic.CONDUIT_CAPACITY);

    public ClayConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLAY_CONDUIT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ClayConduitBlockEntity tile) {
        if (level.isClientSide()) {
            return;
        }
        tile.work();
    }

    private void work() {
        boolean moved = false;
        if (!buffer.isEmpty()) {
            Direction facing = facing();
            BlockPos dest = worldPosition.relative(facing);
            if (FluidLogic.canGravityFlow(buffer.state(), worldPosition.getY(), dest.getY())) {
                FluidState pulse = buffer.extract(buffer.state().id(), FluidLogic.CONDUIT_MB_PER_TICK);
                FluidState leftover = FluidInsert.intoNeighbour(level, worldPosition, facing, pulse);
                if (!leftover.isEmpty()) {
                    buffer.offer(leftover);
                }
                moved = leftover.millibuckets() < pulse.millibuckets();
            }
        }
        MachineProperties.publish(level, worldPosition, FluidLogic.conduitStatus(moved || !buffer.isEmpty()));
        if (moved) {
            setChanged();
            sync();
        }
    }

    private Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return from != facing() && buffer.accepted(state) > 0 && FluidLogic.canGravityFlow(state,
                worldPosition.getY(), worldPosition.getY());
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        if (!canInsert(from, state)) {
            return state;
        }
        FluidState leftover = buffer.offer(state);
        setChanged();
        sync();
        return leftover;
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
