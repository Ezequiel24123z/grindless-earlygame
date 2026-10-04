package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One rated tank. Steam and molten metal fit. Neighbours do not merge (ADR-0082).
 */
public final class IndustrialTankBlockEntity extends BlockEntity implements FluidEndpoint {

    private static final String KEY_FLUID = "Fluid";

    private final FluidBuffer buffer = PressureLogic.buffer(PressureLogic.TANK_CAPACITY);

    public IndustrialTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_TANK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IndustrialTankBlockEntity tank) {
        if (level.isClientSide()) {
            return;
        }
        MachineProperties.publish(level, pos, FluidLogic.tankStatus(!tank.buffer.isEmpty()));
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return buffer.accepted(state) > 0;
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        FluidState leftover = buffer.offer(state);
        if (state != null && leftover.millibuckets() != state.millibuckets()) {
            setChanged();
            sync();
        }
        return leftover;
    }

    @Override
    public boolean canExtract(Direction from) {
        return !buffer.isEmpty();
    }

    @Override
    public FluidState extract(Direction from, int millibuckets) {
        FluidState taken = buffer.extract(null, millibuckets);
        if (!taken.isEmpty()) {
            setChanged();
            sync();
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
