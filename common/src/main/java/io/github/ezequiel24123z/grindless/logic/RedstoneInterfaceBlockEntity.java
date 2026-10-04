package io.github.ezequiel24123z.grindless.logic;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Redstone on the front writes the cable behind. No redstone input lets the cable drive
 * the front (ADR-0083).
 */
public final class RedstoneInterfaceBlockEntity extends BlockEntity {

    private int emitted;

    public RedstoneInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_INTERFACE.get(), pos, state);
    }

    public int emitted() {
        return emitted;
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            RedstoneInterfaceBlockEntity face) {
        if (level.isClientSide()) {
            return;
        }
        Direction front = state.getValue(MachineProperties.FACING);
        Direction back = front.getOpposite();
        int incoming = level.getSignal(pos.relative(front), back);
        BlockEntity behind = level.getBlockEntity(pos.relative(back));
        int nextEmitted = 0;
        if (incoming > 0) {
            if (behind instanceof SignalCableBlockEntity cable) {
                cable.setValue(SignalLogic.fromRedstone(incoming));
            }
        } else if (behind instanceof SignalCableBlockEntity cable) {
            nextEmitted = SignalLogic.toRedstone(cable.value());
        }
        if (nextEmitted != face.emitted) {
            face.emitted = nextEmitted;
            face.setChanged();
            level.updateNeighborsAt(pos, state.getBlock());
        }
        MachineProperties.publish(level, pos,
                face.emitted > 0 || incoming > 0
                        ? io.github.ezequiel24123z.grindless.machine.MachineStatus.RUNNING
                        : io.github.ezequiel24123z.grindless.machine.MachineStatus.IDLE);
    }
}
