package io.github.ezequiel24123z.grindless.machine;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.container.ItemInsert;
import io.github.ezequiel24123z.grindless.container.SlottedContainer;
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
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared consumer for Pulverizer, Arc Furnace, Press, Assembler, Kiln, Wire Mill,
 * Chemical Reactor and Chemical Washer.
 *
 * <p>Looks up a generated {@link ProcessRecipe}, draws its requested Flux, and writes progress. Gaseous outputs
 * push into an adjacent tank when one will take them, and vent otherwise (ADR-0036, ADR-0062).
 * Hoppers and neighbouring inventories are the item logistics; conduits and tanks are the fluid
 * logistics.
 */
public final class ProcessMachineBlockEntity extends MachineBlockEntity
        implements WorldlyContainer, ExtendedMenuProvider, MachineMenuHost, FluidEndpoint {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_RECIPE = "Recipe";
    private static final String KEY_WORK_RECIPE = "WorkRecipe";
    private static final String KEY_FLUID = "Fluid";
    /** Layout 2 added the fourth Assembler input; layout 3 preserves every inventory slot. */
    private static final String KEY_ITEM_LAYOUT = "ItemLayout";
    private static final int CURRENT_ITEM_LAYOUT = 3;
    private static final int FOUR_INPUT_LAYOUT = 2;

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
    private String workRecipeId = "";
    private ProcessRecipe cached;
    private TickSubscription working;
    private TickSubscription filling;
    private TickSubscription exporting;
    private ConditionFault lastFault = ConditionFault.NONE;
    private WorkshopStructure.Fault structureFault = WorkshopStructure.Fault.NONE;
    private boolean loadingInventory;

    public ProcessMachineBlockEntity(ProcessMachineKind kind, BlockPos pos, BlockState state) {
        super(kind.type(), pos, state, kind.tier());
        this.kind = kind;
        this.fluid = new FluidBuffer(FluidLogic.MACHINE_CAPACITY, kind.fluidMaxC(), kind.fluidMaxP());
        this.items = new SlottedContainer(kind.menuKind().size());
        this.inputSlots = range(0, kind.menuKind().inputs());
        this.outputSlots = range(kind.menuKind().inputs(), kind.menuKind().size());
        this.allSlots = range(0, kind.menuKind().size());
        items.addListener(container -> {
            if (loadingInventory) {
                return;
            }
            cached = null;
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
        return new SimpleFluxStorage(bufferCapacity(), powerTier().nominal(), 0L, () -> {
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
        super.setUpgrades(newUpgrades);
    }

    @Override
    protected void updateSubscriptions() {
        if (loadingInventory || getLevel() == null) {
            return;
        }
        structureFault = checkStructure();
        ProcessRecipe recipe = recipe();
        boolean missing = hasPartialInput() && recipe == null;
        boolean stuck = recipe != null && progress >= recipe.durationTicks() && !canOutput(recipe);
        if (recipe != null && !stuck) {
            working = subscriptions().subscribe(working, this::work);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
        if (ProcessLookup.needsPrimaryFluid(kind.family(), inputs(), fluid.state())) {
            filling = subscriptions().subscribe(filling, this::fillFromNeighbours);
        } else if (filling != null) {
            filling.unsubscribe();
            filling = null;
        }
        if (hasOutput()) {
            exporting = subscriptions().subscribe(exporting, this::exportWaitingOutput);
        } else if (exporting != null) {
            exporting.unsubscribe();
            exporting = null;
        }
        if (recipe == null || stuck || structureFault != WorkshopStructure.Fault.NONE) {
            publishStatus(recipe, missing, stuck, hasPowerWaiting(), false, ConditionReport.OPTIMAL);
        }
        syncData();
    }

    public void onNeighbourChanged() {
        cached = null;
        updateSubscriptions();
    }

    private void work() {
        structureFault = checkStructure();
        if (structureFault != WorkshopStructure.Fault.NONE) {
            publishStatus(cached, false, false, false, false, ConditionReport.OPTIMAL);
            syncData();
            return;
        }
        pullFluids();
        ProcessRecipe recipe = recipe();
        if (recipe == null) {
            progress = 0.0;
            cachedRecipeId = "";
            workRecipeId = "";
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
        if (progress >= recipe.durationTicks() && complete(recipe)) {
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
        if (progress >= recipe.durationTicks()) {
            complete(recipe);
        }
        ProcessRecipe next = recipe();
        stuck = next != null && progress >= next.durationTicks() && !canOutput(next);
        publishStatus(next, hasPartialInput() && next == null, stuck, powered, gained > 0.0, report);
        syncData();
        updateSubscriptions();
    }

    private boolean complete(ProcessRecipe recipe) {
        if (!finish(recipe)) {
            return false;
        }
        progress = Math.max(0.0, progress - recipe.durationTicks());
        cached = null;
        setChanged();
        return true;
    }

    private boolean finish(ProcessRecipe recipe) {
        if (!ProcessLookup.matches(recipe, inputs()) || !canOutput(recipe)) {
            return false;
        }
        ItemStack[] outputs = plannedOutputs(recipe);
        if (outputs == null) {
            return false;
        }
        loadingInventory = true;
        try {
            for (int i = 0; i < recipe.itemInputs().size(); i++) {
                items.removeItem(i, recipe.itemInputs().get(i).count());
            }
            for (int i = 0; i < outputs.length; i++) {
                items.setItem(kind.menuKind().inputs() + i, outputs[i]);
            }
            consumeFluid(recipe);
            captureOrVent(recipe);
            pushOutputs();
        } finally {
            loadingInventory = false;
        }
        setChanged();
        return true;
    }

    private boolean hasOutput() {
        for (int slot : outputSlots) {
            if (!items.getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void exportWaitingOutput() {
        if (isDue(8)) {
            pushOutputs();
            updateSubscriptions();
        }
    }

    private void fillFromNeighbours() {
        if (isDue(4)) {
            pullFluids();
            updateSubscriptions();
        }
    }

    private void pushOutputs() {
        if (getLevel() == null) {
            return;
        }
        int base = kind.menuKind().inputs();
        for (int slot = base; slot < items.getContainerSize(); slot++) {
            ItemStack stack = items.getItem(slot).copy();
            if (stack.isEmpty()) {
                continue;
            }
            for (Direction side : Direction.values()) {
                if (stack.isEmpty()) {
                    break;
                }
                stack = ItemInsert.intoNeighbour(getLevel(), getBlockPos(), side, stack);
            }
            if (!ItemStack.matches(items.getItem(slot), stack)) {
                items.setItem(slot, stack);
            }
        }
    }

    private ItemStack[] plannedOutputs(ProcessRecipe recipe) {
        List<ItemStack> produced = resolvedOutputs(recipe);
        if (produced.size() < recipe.itemOutputs().size()) {
            return null;
        }
        int base = kind.menuKind().inputs();
        ItemStack[] simulated = new ItemStack[kind.menuKind().outputs()];
        for (int i = 0; i < simulated.length; i++) {
            simulated[i] = items.getItem(base + i).copy();
        }
        for (ItemStack stack : produced) {
            if (!merge(simulated, stack)) {
                return null;
            }
        }
        return simulated;
    }

    private boolean canOutput(ProcessRecipe recipe) {
        if (plannedOutputs(recipe) == null) {
            return false;
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
        for (ItemStack current : slots) {
            if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, remaining)) {
                int moved = Math.min(remaining.getCount(),
                        Math.max(0, current.getMaxStackSize() - current.getCount()));
                current.grow(moved);
                remaining.shrink(moved);
            }
            if (remaining.isEmpty()) {
                return true;
            }
        }
        for (int i = 0; i < slots.length && !remaining.isEmpty(); i++) {
            if (slots[i].isEmpty()) {
                slots[i] = remaining.split(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
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
        if (cached != null) {
            if (!workRecipeId.isEmpty() && !workRecipeId.equals(cached.id())) {
                progress = 0.0;
            }
            workRecipeId = cached.id();
        }
        return cached;
    }

    private WorkshopStructure.Fault checkStructure() {
        if (kind.structure() == WorkshopStructure.NONE || getLevel() == null) {
            return WorkshopStructure.Fault.NONE;
        }
        int turns = switch (getBlockState().getValue(MachineProperties.FACING)) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalStateException("a workshop controller must face horizontally");
        };
        return kind.structure().check(turns, offset -> {
            BlockPos cell = getBlockPos().offset(offset.x(), offset.y(), offset.z());
            return getLevel().hasChunkAt(cell)
                    ? BuiltInRegistries.BLOCK.getKey(getLevel().getBlockState(cell).getBlock()).toString()
                    : null;
        });
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
        return state != null && ProcessLookup.isPrimaryFluid(kind.family(), state.id())
                && fluid.accepted(state) > 0;
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        if (!canInsert(from, state)) {
            return state;
        }
        FluidState leftover = fluid.offer(state);
        if (leftover.millibuckets() != (state == null ? 0 : state.millibuckets())) {
            cached = null;
            setChanged();
            updateSubscriptions();
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
            setChanged();
            updateSubscriptions();
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
        long needed = cached == null ? kind.tier().nominal() : cached.fuPerTick();
        if (energy().getStored() >= needed) {
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
        if (structureFault != WorkshopStructure.Fault.NONE) {
            observed = MachineStatus.OUT_OF_BAND;
        }
        boolean stableIdle = recipe == null && !missing && structureFault == WorkshopStructure.Fault.NONE;
        if (stableIdle ? display.settle(MachineStatus.IDLE) : display.observe(observed)) {
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
        data.set(ProcessMachineMenu.DATA_STRUCTURE, structureFault.ordinal());
        data.set(ProcessMachineMenu.DATA_FLUID, fluid.state().millibuckets());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(KEY_PROGRESS, progress);
        tag.put(KEY_ITEMS, items.createTag());
        tag.putInt(KEY_ITEM_LAYOUT, CURRENT_ITEM_LAYOUT);
        tag.putString(KEY_RECIPE, cachedRecipeId);
        tag.putString(KEY_WORK_RECIPE, workRecipeId);
        tag.put(KEY_FLUID, FluidNbt.save(fluid.state()));
    }

    @Override
    public void load(CompoundTag tag) {
        loadingInventory = true;
        try {
            super.load(tag);
            progress = Math.max(0.0, tag.getDouble(KEY_PROGRESS));
            ListTag savedItems = tag.getList(KEY_ITEMS, net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
            if (kind == ProcessMachineKind.ASSEMBLER
                    && tag.getInt(KEY_ITEM_LAYOUT) < FOUR_INPUT_LAYOUT) {
                migrateLegacyAssemblerOutput(savedItems);
            }
            items.fromTag(savedItems);
            cachedRecipeId = tag.getString(KEY_RECIPE);
            workRecipeId = tag.contains(KEY_WORK_RECIPE) ? tag.getString(KEY_WORK_RECIPE) : cachedRecipeId;
            cached = null;
            fluid.set(FluidNbt.load(tag.getCompound(KEY_FLUID)));
        } finally {
            loadingInventory = false;
        }
        updateSubscriptions();
        syncData();
    }

    /**
     * Assemblers used slots 0-2 for inputs and slot 3 for their output before the Relay Matrix
     * batch needed a fourth ingredient. Shift that one persisted output to its new slot before
     * {@link SlottedContainer#fromTag(ListTag)} reads it, preserving existing worlds.
     */
    private static void migrateLegacyAssemblerOutput(ListTag savedItems) {
        for (int index = 0; index < savedItems.size(); index++) {
            CompoundTag stack = savedItems.getCompound(index);
            int slot = stack.contains("Slot") ? stack.getByte("Slot") : index;
            if (slot == 3) {
                stack.putByte("Slot", (byte) 4);
            }
        }
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
