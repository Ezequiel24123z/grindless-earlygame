package io.github.ezequiel24123z.grindless.machine;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.container.ItemInsert;
import io.github.ezequiel24123z.grindless.fluid.FluidBuffer;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ConditionFault;
import io.github.ezequiel24123z.grindless.process.ConditionReport;
import io.github.ezequiel24123z.grindless.process.ProcessConditions;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared consumer for Pulverizer, Arc Furnace, Press, Assembler, Kiln, Wire Mill,
 * Chemical Reactor and Chemical Washer.
 *
 * <p>Looks up a generated {@link ProcessRecipe}, draws F1, and writes progress. Gaseous outputs
 * push into an adjacent tank when one will take them, and vent otherwise (ADR-0036, ADR-0062).
 * Hoppers and neighbouring inventories are the item logistics; conduits and tanks are the fluid
 * logistics.
 */
public final class ProcessMachineBlockEntity extends MachineBlockEntity
        implements WorldlyContainer, ExtendedMenuProvider, MachineMenuHost, FluidEndpoint {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_RECIPE = "Recipe";
    private static final String KEY_FLUID = "Fluid";

    private final ProcessMachineKind kind;
    private final SimpleContainer items;
    private final StatusDebounce display = new StatusDebounce();
    private final SimpleContainerData data = new SimpleContainerData(ProcessMachineMenu.DATA_SIZE);
    private final FluidBuffer fluid;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final int[] allSlots;

    private double progress;
    private String cachedRecipeId = "";
    private ProcessRecipe cached;
    private TickSubscription working;
    private ConditionFault lastFault = ConditionFault.NONE;

    public ProcessMachineBlockEntity(ProcessMachineKind kind, BlockPos pos, BlockState state) {
        super(kind.type(), pos, state);
        this.kind = kind;
        this.fluid = new FluidBuffer(FluidLogic.MACHINE_CAPACITY, kind.fluidMaxC(), kind.fluidMaxP());
        this.items = new SimpleContainer(kind.menuKind().size());
        this.inputSlots = range(0, kind.menuKind().inputs());
        this.outputSlots = range(kind.menuKind().inputs(), kind.menuKind().size());
        this.allSlots = range(0, kind.menuKind().size());
        items.addListener(container -> {
            cached = null;
            cachedRecipeId = "";
            setChanged();
            updateSubscriptions();
        });
        syncData();
    }

    public ProcessMachineKind kind() {
        return kind;
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        return kind.envelope();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return kind.envelope();
    }

    @Override
    protected FluxTier ratedTier() {
        return FluxTier.F1;
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
    protected void onFirstTick() {
        setConditions(kind.heldConditions());
    }

    @Override
    public void setUpgrades(UpgradeSet newUpgrades) {
        cached = null;
        cachedRecipeId = "";
        super.setUpgrades(newUpgrades);
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        ProcessRecipe recipe = recipe();
        boolean missing = hasPartialInput() && recipe == null;
        boolean stuck = recipe != null && progress >= recipe.durationTicks() && !canOutput(recipe);
        if (recipe != null && !stuck) {
            working = subscriptions().subscribe(working, this::work);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
        publishStatus(recipe, missing, stuck, hasPowerWaiting(), false, ConditionReport.OPTIMAL);
        syncData();
    }

    public void onNeighbourChanged() {
        cached = null;
        cachedRecipeId = "";
        updateSubscriptions();
    }

    private void work() {
        pullFluids();
        ProcessRecipe recipe = recipe();
        if (recipe == null) {
            progress = 0.0;
            publishStatus(null, hasPartialInput(), false, false, false, ConditionReport.OPTIMAL);
            updateSubscriptions();
            return;
        }
        ProcessConditions conditions = ProcessLookup.conditionsOf(recipe);
        ConditionReport report = conditions.evaluate(conditions());
        lastFault = report.fault();
        if (!report.canRun()) {
            publishStatus(recipe, false, false, hasPowerWaiting(), false, report);
            syncData();
            return;
        }
        boolean stuck = progress >= recipe.durationTicks() && !canOutput(recipe);
        if (stuck) {
            publishStatus(recipe, false, true, hasPowerWaiting(), false, report);
            updateSubscriptions();
            return;
        }

        requestPower(recipe.fuPerTick());
        long drawn = drawPower(recipe.fuPerTick());
        if (drawn <= 0L && energy().getStored() >= recipe.fuPerTick()) {
            energy().setStored(energy().getStored() - recipe.fuPerTick());
            drawn = recipe.fuPerTick();
        }

        boolean powered = drawn > 0L || hasPowerWaiting();
        double gained = ProcessLogic.work(drawn, recipe.fuPerTick(), report.efficiency(), timeMultiplier());
        if (gained > 0.0) {
            progress += gained;
            setChanged();
        }
        if (progress >= recipe.durationTicks() && finish(recipe)) {
            progress -= recipe.durationTicks();
            cached = null;
            cachedRecipeId = "";
            setChanged();
        }
        stuck = recipe() != null && progress >= recipe.durationTicks() && !canOutput(recipe);
        publishStatus(recipe(), !ProcessLookup.matches(recipe, inputs()), stuck, powered, gained > 0.0, report);
        syncData();
        if (stuck || recipe() == null) {
            updateSubscriptions();
        }
    }

    private boolean finish(ProcessRecipe recipe) {
        if (!canOutput(recipe)) {
            return false;
        }
        List<ItemStack> produced = resolvedOutputs(recipe);
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack stack = items.getItem(i);
            stack.shrink(recipe.itemInputs().get(i).count());
            items.setItem(i, stack);
        }
        int outBase = kind.menuKind().inputs();
        for (ItemStack producedStack : produced) {
            ItemStack leftover = HopperBlockEntity.addItem(null, outputView(), producedStack, null);
            if (!leftover.isEmpty()) {
                items.setItem(outBase, leftover);
            }
        }
        pushOutputs();
        consumeFluid(recipe);
        captureOrVent(recipe);
        return true;
    }

    private SimpleContainer outputView() {
        SimpleContainer view = new SimpleContainer(kind.menuKind().outputs());
        int base = kind.menuKind().inputs();
        for (int i = 0; i < view.getContainerSize(); i++) {
            view.setItem(i, items.getItem(base + i));
        }
        view.addListener(container -> {
            for (int i = 0; i < view.getContainerSize(); i++) {
                items.setItem(base + i, view.getItem(i));
            }
        });
        return view;
    }

    private void pushOutputs() {
        if (getLevel() == null) {
            return;
        }
        int base = kind.menuKind().inputs();
        for (int slot = base; slot < items.getContainerSize(); slot++) {
            ItemStack stack = items.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Direction side : Direction.values()) {
                if (stack.isEmpty()) {
                    break;
                }
                stack = ItemInsert.intoNeighbour(getLevel(), getBlockPos(), side, stack);
            }
            items.setItem(slot, stack);
        }
    }

    private boolean canOutput(ProcessRecipe recipe) {
        List<ItemStack> produced = resolvedOutputs(recipe);
        if (produced.size() < recipe.itemOutputs().size()) {
            return false;
        }
        int base = kind.menuKind().inputs();
        ItemStack[] simulated = new ItemStack[kind.menuKind().outputs()];
        for (int i = 0; i < simulated.length; i++) {
            simulated[i] = items.getItem(base + i).copy();
        }
        for (ItemStack stack : produced) {
            if (!merge(simulated, stack)) {
                return false;
            }
        }
        if (!ProcessLookup.matchesFluid(recipe, fluid.state(), neighbourFluids())) {
            return false;
        }
        FluidState afterConsume = fluid.state();
        List<IngredientSpec> fluids = recipe.fluidInputs();
        if (!fluids.isEmpty() && afterConsume.is(fluids.get(0).id())) {
            afterConsume = afterConsume.withAmount(afterConsume.millibuckets() - fluids.get(0).count());
        }
        for (OutputSpec output : recipe.fluidOutputs()) {
            if (output.vented()) {
                continue;
            }
            FluidState made = FluidLogic.emitted(output.id(), output.count());
            int take = FluidLogic.accepted(afterConsume, made, fluid.capacity(),
                    fluid.maxC(), fluid.maxP());
            if (take < output.count()) {
                return false;
            }
            afterConsume = FluidLogic.insert(afterConsume, made, fluid.capacity(),
                    fluid.maxC(), fluid.maxP());
        }
        return true;
    }

    private static boolean merge(ItemStack[] slots, ItemStack incoming) {
        ItemStack remaining = incoming.copy();
        for (int i = 0; i < slots.length && !remaining.isEmpty(); i++) {
            ItemStack current = slots[i];
            if (current.isEmpty()) {
                slots[i] = remaining;
                return true;
            }
            if (ItemStack.isSameItemSameTags(current, remaining)
                    && current.getCount() + remaining.getCount() <= current.getMaxStackSize()) {
                current.grow(remaining.getCount());
                return true;
            }
        }
        return remaining.isEmpty();
    }

    private List<ItemStack> resolvedOutputs(ProcessRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (OutputSpec output : recipe.itemOutputs()) {
            ProcessLookup.resolve(output).ifPresent(stacks::add);
        }
        return stacks;
    }

    private ProcessRecipe recipe() {
        if (cached != null) {
            return cached;
        }
        Optional<ProcessRecipe> found = ProcessLookup.find(kind.family(), inputs(), fluid.state(),
                neighbourFluids());
        cached = found.orElse(null);
        cachedRecipeId = cached == null ? "" : cached.id();
        return cached;
    }

    private ItemStack[] inputs() {
        ItemStack[] stacks = new ItemStack[kind.menuKind().inputs()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = items.getItem(i);
        }
        return stacks;
    }

    private void pullFluids() {
        if (getLevel() == null || fluid.space() <= 0) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (fluid.space() <= 0) {
                break;
            }
            net.minecraft.world.level.block.entity.BlockEntity neighbour =
                    getLevel().getBlockEntity(getBlockPos().relative(side));
            if (!(neighbour instanceof FluidEndpoint endpoint)
                    || !endpoint.canExtract(side.getOpposite())) {
                continue;
            }
            FluidState present = endpoint.contents();
            if (present.isEmpty() || !ProcessLookup.isPrimaryFluid(kind.family(), present.id())) {
                continue;
            }
            FluidState taken = endpoint.extract(side.getOpposite(),
                    Math.min(FluidLogic.CONDUIT_MB_PER_TICK, fluid.space()));
            FluidState leftover = fluid.offer(taken);
            if (!leftover.isEmpty()) {
                endpoint.insert(side.getOpposite(), leftover);
            }
            if (!taken.isEmpty()) {
                cached = null;
                cachedRecipeId = "";
                setChanged();
            }
        }
    }

    private void consumeFluid(ProcessRecipe recipe) {
        List<IngredientSpec> fluids = recipe.fluidInputs();
        if (!fluids.isEmpty()) {
            fluid.extract(fluids.get(0).id(), fluids.get(0).count());
        }
        for (int i = 1; i < fluids.size(); i++) {
            IngredientSpec spec = fluids.get(i);
            FluidInsert.takeFromNeighbours(getLevel(), getBlockPos(), spec.id(), spec.count());
        }
        cached = null;
        cachedRecipeId = "";
    }

    private void captureOrVent(ProcessRecipe recipe) {
        if (getLevel() == null) {
            return;
        }
        for (OutputSpec output : recipe.fluidOutputs()) {
            FluidState remaining = FluidLogic.emitted(output.id(), output.count());
            if (!output.vented()) {
                remaining = fluid.offer(remaining);
            }
            for (Direction side : Direction.values()) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = FluidInsert.intoNeighbour(getLevel(), getBlockPos(), side, remaining);
            }
        }
    }

    private List<FluidState> neighbourFluids() {
        if (getLevel() == null) {
            return List.of();
        }
        List<FluidState> neighbours = new ArrayList<>();
        for (Direction side : Direction.values()) {
            net.minecraft.world.level.block.entity.BlockEntity neighbour =
                    getLevel().getBlockEntity(getBlockPos().relative(side));
            if (neighbour instanceof FluidEndpoint endpoint) {
                neighbours.add(endpoint.contents());
            }
        }
        return neighbours;
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return fluid.accepted(state) > 0;
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        FluidState leftover = fluid.offer(state);
        if (leftover.millibuckets() != (state == null ? 0 : state.millibuckets())) {
            cached = null;
            cachedRecipeId = "";
            setChanged();
        }
        return leftover;
    }

    @Override
    public boolean canExtract(Direction from) {
        return !fluid.isEmpty();
    }

    @Override
    public FluidState extract(Direction from, int millibuckets) {
        FluidState taken = fluid.extract(null, millibuckets);
        if (!taken.isEmpty()) {
            cached = null;
            cachedRecipeId = "";
            setChanged();
        }
        return taken;
    }

    @Override
    public FluidState contents() {
        return fluid.state();
    }

    @Override
    public int capacity() {
        return fluid.capacity();
    }

    private boolean hasPartialInput() {
        if (!fluid.isEmpty()) {
            return true;
        }
        for (int i = 0; i < kind.menuKind().inputs(); i++) {
            if (!items.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPowerWaiting() {
        if (energy().getStored() >= ProcessLogic.FU_PER_TICK) {
            return true;
        }
        FluxNetwork network = network();
        return network != null && network.stored() > 0L;
    }

    private void publishStatus(ProcessRecipe recipe, boolean missing, boolean stuck, boolean powered,
                               boolean workingNow, ConditionReport report) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        lastFault = report.fault();
        MachineStatus observed = ProcessLogic.status(recipe != null, missing, stuck,
                report.canRun() || recipe == null, powered, workingNow);
        if (display.observe(observed)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    private void syncData() {
        ProcessRecipe recipe = cached;
        int cycle = recipe == null ? 1 : recipe.durationTicks();
        data.set(ProcessMachineMenu.DATA_ENERGY, (int) Math.min(Integer.MAX_VALUE, energy().getStored()));
        data.set(ProcessMachineMenu.DATA_CAPACITY, (int) Math.min(Integer.MAX_VALUE, energy().getCapacity()));
        data.set(ProcessMachineMenu.DATA_PROGRESS, (int) Math.min(cycle, progress));
        data.set(ProcessMachineMenu.DATA_CYCLE, cycle);
        data.set(ProcessMachineMenu.DATA_STATUS, display.shown().ordinal());
        data.set(ProcessMachineMenu.DATA_KIND, kind.menuKind().ordinal());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(KEY_PROGRESS, progress);
        tag.put(KEY_ITEMS, items.createTag());
        tag.putString(KEY_RECIPE, cachedRecipeId);
        tag.put(KEY_FLUID, FluidNbt.save(fluid.state()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0.0, tag.getDouble(KEY_PROGRESS));
        items.fromTag(tag.getList(KEY_ITEMS, net.minecraft.nbt.Tag.TAG_COMPOUND));
        cachedRecipeId = tag.getString(KEY_RECIPE);
        cached = null;
        fluid.set(FluidNbt.load(tag.getCompound(KEY_FLUID)));
        syncData();
    }

    @Override
    public MachineMenuKind menuKind() {
        return kind.menuKind();
    }

    @Override
    public Container menuContainer() {
        return items;
    }

    @Override
    public ContainerData menuData() {
        return data;
    }

    @Override
    public MachineStatus menuStatus() {
        return display.shown();
    }

    @Override
    public Component menuFault() {
        return lastFault.isFault()
                ? Component.literal(lastFault.message())
                : Component.empty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(kind.translationKey());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessMachineMenu(id, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(getBlockPos());
    }

    @Override
    public int getContainerSize() {
        return items.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return items.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return items.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= kind.menuKind().inputs()) {
            return false;
        }
        return ProcessLookup.accepts(kind.family(), slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return outputSlots;
        }
        if (side == Direction.UP) {
            return inputSlots;
        }
        return allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= kind.menuKind().inputs();
    }

    private static int[] range(int start, int end) {
        int[] slots = new int[Math.max(0, end - start)];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = start + i;
        }
        return slots;
    }
}
