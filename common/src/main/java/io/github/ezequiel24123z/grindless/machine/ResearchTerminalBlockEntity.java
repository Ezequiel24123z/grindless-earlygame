package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;

/**
 * The Research Terminal: uses F0 to calibrate a Data Core into a physical recipe component.
 *
 * <p>No menu. Right-click with a Data Core to insert, empty-handed to take it back. Hoppers can
 * feed the input and pull the calibrated output. Progress belongs to this machine, never to a
 * player or world.
 */
public final class ResearchTerminalBlockEntity extends MachineBlockEntity implements WorldlyContainer {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_INPUT = "Input";
    private static final int[] SLOTS = {0};

    private final SimpleContainer input = new SimpleContainer(1);
    private final StatusDebounce display = new StatusDebounce();

    private double progress;
    private TickSubscription working;

    public ResearchTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TERMINAL.get(), pos, state);
        input.addListener(container -> {
            setChanged();
            updateSubscriptions();
        });
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
        return FluxTier.F0;
    }

    @Override
    protected SimpleFluxStorage createEnergyBuffer() {
        return new SimpleFluxStorage(bufferCapacity(), ratedTier().nominal(), 0L, () -> {
            setChanged();
            if (getLevel() != null && !getLevel().isClientSide()) {
                updateSubscriptions();
            }
        });
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        boolean hasCore = hasCore();
        boolean hasOutput = hasOutput();
        if (hasCore) {
            working = subscriptions().subscribe(working, this::work);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
        publishStatus(hasCore, hasOutput, hasPowerWaiting(), false);
    }

    public void onNeighbourChanged() {
        updateSubscriptions();
    }

    /**
     * Inserts a Data Core or extracts the stored input/output. Returns whether the click was
     * this machine's.
     */
    public boolean interact(Player player, ItemStack held) {
        if (held.isEmpty()) {
            ItemStack core = input.getItem(0);
            if (core.isEmpty()) {
                return false;
            }
            if (!player.addItem(core.copy())) {
                player.drop(core.copy(), false);
            }
            input.setItem(0, ItemStack.EMPTY);
            return true;
        }
        if (!isDataCore(held)) {
            return false;
        }
        if (!input.getItem(0).isEmpty()) {
            return true;
        }
        ItemStack one = held.split(1);
        input.setItem(0, one);
        return true;
    }

    private void work() {
        boolean hasCore = hasCore();
        if (!hasCore) {
            publishStatus(false, hasOutput(), false, false);
            updateSubscriptions();
            return;
        }

        requestPower(ResearchLogic.FU_PER_TICK);
        long drawn = drawPower(ResearchLogic.FU_PER_TICK);
        if (drawn <= 0L && energy().getStored() >= ResearchLogic.FU_PER_TICK) {
            energy().setStored(energy().getStored() - ResearchLogic.FU_PER_TICK);
            drawn = ResearchLogic.FU_PER_TICK;
        }

        boolean powered = drawn > 0L || hasPowerWaiting();
        double gained = ResearchLogic.work(drawn, ResearchLogic.FU_PER_TICK);
        if (gained > 0.0) {
            progress += gained;
            setChanged();
        }
        if (progress >= ResearchLogic.CYCLE_TICKS) {
            progress = 0.0;
            input.setItem(0, new ItemStack(ModItems.CALIBRATED_DATA_CORE.get()));
            setChanged();
            updateSubscriptions();
            return;
        }
        publishStatus(true, false, powered, gained > 0.0);
    }

    private boolean hasPowerWaiting() {
        if (energy().getStored() >= ResearchLogic.FU_PER_TICK) {
            return true;
        }
        FluxNetwork network = network();
        return network != null && network.stored() > 0L;
    }

    private boolean hasCore() {
        return isDataCore(input.getItem(0));
    }

    private boolean hasOutput() {
        ItemStack stack = input.getItem(0);
        return !stack.isEmpty() && !isDataCore(stack);
    }

    private static boolean isDataCore(ItemStack stack) {
        return !stack.isEmpty() && ResearchLogic.DATA_CORE.equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private void publishStatus(boolean hasCore, boolean hasOutput, boolean powered, boolean workingNow) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        MachineStatus observed = ResearchLogic.status(hasCore, hasOutput, powered, workingNow);
        if (display.observe(observed)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(KEY_PROGRESS, progress);
        tag.put(KEY_INPUT, input.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0.0, tag.getDouble(KEY_PROGRESS));
        input.fromTag(tag.getList(KEY_INPUT, net.minecraft.nbt.Tag.TAG_COMPOUND));
    }

    // ---- Container: hoppers may insert Data Cores and pull physical outputs -----------------

    @Override
    public int getContainerSize() {
        return input.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return input.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return input.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return input.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        input.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        input.clearContent();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isDataCore(stack) && input.getItem(0).isEmpty();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isDataCore(stack);
    }
}
