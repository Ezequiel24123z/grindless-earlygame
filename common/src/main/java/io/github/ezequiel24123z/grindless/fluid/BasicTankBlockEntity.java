package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Unpressurised ambient tank. Hot fluid will not enter. Gases at 0.1 MPa will.
 */
public final class BasicTankBlockEntity extends BlockEntity implements FluidEndpoint {

    private static final String KEY_FLUID = "Fluid";

    private final FluidBuffer buffer = FluidBuffer.ambient(FluidLogic.TANK_CAPACITY);
    private final NativeFluidStore nativeFluids = new NativeFluidStore(buffer, () -> {
        setChanged();
        sync();
    });

    public BasicTankBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.BASIC_TANK.get(), pos, state);
    }

    private BasicTankBlockEntity(BlockEntityType<BasicTankBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BasicTankBlockEntity tank) {
        if (level.isClientSide()) {
            return;
        }
        MachineProperties.publish(level, pos, FluidLogic.tankStatus(!tank.buffer.isEmpty()));
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return acceptedAmount(from, state) > 0;
    }

    @Override
    public int acceptedAmount(Direction from, FluidState state) {
        return available() ? buffer.accepted(state) : 0;
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        if (!available()) {
            return state == null ? FluidState.EMPTY : state;
        }
        FluidState leftover = buffer.offer(state);
        if (leftover.millibuckets() != (state == null ? 0 : state.millibuckets())) {
            setChanged();
            sync();
        }
        return leftover;
    }

    @Override
    public boolean canExtract(Direction from) {
        return available() && !buffer.isEmpty();
    }

    @Override
    public FluidState extract(Direction from, int millibuckets) {
        if (!canExtract(from)) {
            return FluidState.EMPTY;
        }
        FluidState taken = buffer.extract(null, millibuckets);
        if (!taken.isEmpty()) {
            setChanged();
            sync();
        }
        return taken;
    }

    @Override
    public FluidState contents() {
        return attached() ? buffer.state() : FluidState.EMPTY;
    }

    @Override
    public NativeFluidStore nativeStore() {
        return available() ? nativeFluids : null;
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    /**
     * Settles an already staged container exchange using the existing native receipt. The item
     * handler only ever sees a copy; player inventory and the tank are restored on an exception.
     */
    public boolean exchangeContainer(Direction face, FluidState transferred, boolean intoTank,
                                     BooleanSupplier unchanged, NativeFluidStore.Settlement container) {
        if (transferred == null || transferred.isEmpty() || !available() || !unchanged.getAsBoolean()) {
            return false;
        }
        if (intoTank && acceptedAmount(face, transferred) != transferred.millibuckets()
                || !intoTank && (!canExtract(face) || !buffer.state().is(transferred.id())
                || buffer.state().millibuckets() < transferred.millibuckets())) {
            return false;
        }
        List<NativeFluidStore.Request> requests = intoTank ? List.of() : List.of(
                new NativeFluidStore.Request(transferred.id(), transferred.millibuckets(), List.of(
                        new NativeFluidStore.Access(nativeFluids,
                                () -> canExtract(face) && unchanged.getAsBoolean()))));
        Optional<NativeFluidStore.Batch> batch = NativeFluidStore.plan(nativeFluids, requests);
        return batch.isPresent() && batch.get().commit(new NativeFluidStore.Settlement() {
            @Override
            public void apply() {
                if (!available() || !unchanged.getAsBoolean()) {
                    throw new IllegalStateException("Tank or container changed during a container exchange");
                }
                if (intoTank && !buffer.offer(transferred).isEmpty()) {
                    throw new IllegalStateException("Reserved container fluid did not fit the Basic Tank");
                }
                container.apply();
            }

            @Override
            public void restore() {
                container.restore();
            }
        });
    }

    /** Raw saved stock is retained even after removal so the break path has one durable owner. */
    public boolean hasStoredFluid() {
        return !buffer.isEmpty();
    }

    public ItemStack preservedDrop() {
        ItemStack stack = new ItemStack(getBlockState().getBlock());
        BlockItem.setBlockEntityData(stack, getType(), saveWithoutMetadata());
        return stack;
    }

    public Component fillFeedback() {
        return feedback(buffer.state());
    }

    public static Component feedback(FluidState state) {
        return state.isEmpty()
                ? Component.translatable("message.grindless.tank.empty", FluidLogic.TANK_CAPACITY)
                : Component.translatable("message.grindless.tank.contents", state.id(),
                        state.millibuckets(), FluidLogic.TANK_CAPACITY);
    }

    private boolean attached() {
        return !isRemoved() && (level == null
                || level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this);
    }

    private boolean available() {
        return attached() && (level == null || !level.isClientSide());
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
