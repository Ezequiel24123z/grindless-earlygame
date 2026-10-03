package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.container.ItemExtract;
import io.github.ezequiel24123z.grindless.container.ItemInsert;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crude inserter: one item a second, unpowered, pickup behind and drop ahead.
 */
public final class ManipulatorBlockEntity extends BlockEntity {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_HELD = "Held";

    private int progress;
    private ItemStack held = ItemStack.EMPTY;

    public ManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUDE_MANIPULATOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            ManipulatorBlockEntity arm) {
        if (level.isClientSide()) {
            return;
        }
        arm.work();
    }

    private void work() {
        Direction drop = facing();
        Direction pickup = drop.getOpposite();
        boolean transferring = !held.isEmpty();
        if (transferring) {
            held = deliver(drop, held);
            setChanged();
        } else {
            boolean hasWork = ItemExtract.hasExtractable(level, worldPosition, pickup)
                    && canDeliver(drop);
            if (hasWork) {
                progress++;
                if (ManipulatorLogic.ready(progress)) {
                    ItemStack taken = ItemExtract.fromNeighbour(level, worldPosition, pickup,
                            ManipulatorLogic.STACK);
                    if (!taken.isEmpty()) {
                        held = deliver(drop, taken);
                    }
                    progress = 0;
                }
                setChanged();
            } else {
                progress = 0;
            }
            transferring = !held.isEmpty();
        }
        boolean hasWork = transferring
                || ItemExtract.hasExtractable(level, worldPosition, pickup);
        MachineProperties.publish(level, worldPosition,
                ManipulatorLogic.status(hasWork, transferring));
    }

    private ItemStack deliver(Direction drop, ItemStack stack) {
        BlockEntity dest = level.getBlockEntity(worldPosition.relative(drop));
        if (dest instanceof BeltEndpoint belt) {
            return belt.insert(drop.getOpposite(), stack);
        }
        return ItemInsert.intoNeighbour(level, worldPosition, drop, stack);
    }

    private boolean canDeliver(Direction drop) {
        return level.getBlockEntity(worldPosition.relative(drop)) != null;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
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
