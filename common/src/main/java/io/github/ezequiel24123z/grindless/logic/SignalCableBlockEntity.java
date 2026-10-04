package io.github.ezequiel24123z.grindless.logic;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One signal tile. The integer moves one block per tick, toward the facing (ADR-0083).
 */
public final class SignalCableBlockEntity extends BlockEntity {

    private static final String KEY_VALUE = "Signal";

    private int value;

    public SignalCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_CABLE.get(), pos, state);
    }

    public int value() {
        return value;
    }

    public void setValue(int next) {
        if (this.value == next) {
            return;
        }
        this.value = next;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SignalCableBlockEntity cable) {
        if (level.isClientSide()) {
            return;
        }
        Direction facing = state.getValue(MachineProperties.FACING);
        BlockEntity next = level.getBlockEntity(pos.relative(facing));
        if (next instanceof SignalCableBlockEntity ahead) {
            ahead.setValue(cable.value);
        }
        MachineProperties.publish(level, pos,
                cable.value != 0
                        ? io.github.ezequiel24123z.grindless.machine.MachineStatus.RUNNING
                        : io.github.ezequiel24123z.grindless.machine.MachineStatus.IDLE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_VALUE, value);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        value = tag.getInt(KEY_VALUE);
    }
}
