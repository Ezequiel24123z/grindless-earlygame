package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.research.Blueprint;
import io.github.ezequiel24123z.grindless.research.ResearchData;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.research.ResearchSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;

/**
 * The Research Terminal: spends a matching core and F0 to unlock the next blueprint.
 *
 * <p>No menu. Right-click with a core to insert, empty-handed to take it back. Hoppers can feed
 * it, because research is a production target (ADR-0057). Progress is world-scoped: a core the
 * factory pushed in still counts.
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
        boolean unlocked = unlocked();
        boolean hasCore = hasCore();
        if (hasCore && !unlocked) {
            working = subscriptions().subscribe(working, this::work);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
        publishStatus(hasCore, unlocked, hasPowerWaiting(), false);
    }

    public void onNeighbourChanged() {
        updateSubscriptions();
    }

    /**
     * Inserts or extracts a Data Core. Returns whether the click was this machine's.
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
        Blueprint next = current();
        if (next == null) {
            player.displayClientMessage(
                    Component.translatable("chat.grindless.research.already",
                            Component.translatable("blueprint.grindless." + Blueprint.INDUSTRIAL.id())),
                    true);
            return true;
        }
        if (!isCore(held, next)) {
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
        boolean unlocked = unlocked();
        boolean hasCore = hasCore();
        if (!hasCore || unlocked) {
            publishStatus(hasCore, unlocked, false, false);
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
        if (progress >= cycleTicks() && getLevel() instanceof ServerLevel level) {
            progress = 0.0;
            Blueprint next = current();
            if (next != null && ResearchData.get(level).unlock(next)) {
                input.setItem(0, ItemStack.EMPTY);
                ResearchSync.broadcast(level.getServer());
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("chat.grindless.research.unlocked",
                                Component.translatable("blueprint.grindless." + next.id())),
                        false);
            }
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
        Blueprint next = current();
        return next != null && isCore(input.getItem(0), next);
    }

    private boolean unlocked() {
        return current() == null;
    }

    private Blueprint current() {
        if (!(getLevel() instanceof ServerLevel server)) {
            return Blueprint.VOLTAIC;
        }
        return ResearchLogic.next(ResearchData.get(server).unlocked());
    }

    private int cycleTicks() {
        Blueprint next = current();
        return next == null ? ResearchLogic.CYCLE_TICKS : ResearchLogic.cycleTicks(next);
    }

    private static boolean isCore(ItemStack stack, Blueprint blueprint) {
        if (stack.isEmpty() || blueprint == null) {
            return false;
        }
        return ResearchLogic.coreId(blueprint).equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private void publishStatus(boolean hasCore, boolean unlocked, boolean powered, boolean workingNow) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        MachineStatus observed = ResearchLogic.status(hasCore, unlocked, powered, workingNow);
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

    // ---- Container: hoppers may insert Data Cores, nothing else -----------------------------

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
        return isCore(stack, current()) && current() != null && input.getItem(0).isEmpty();
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
        // The core is spent, not extracted. A hopper pulling it out would stall research.
        return false;
    }
}
