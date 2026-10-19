package io.github.ezequiel24123z.grindless.machine;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.container.ItemInsert;
import io.github.ezequiel24123z.grindless.container.SlottedContainer;
import io.github.ezequiel24123z.grindless.fluid.FluidBuffer;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.NativeFluidStore;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.menu.MachineFluidView;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ConditionFault;
import io.github.ezequiel24123z.grindless.process.ConditionReport;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import io.github.ezequiel24123z.grindless.factory.FactoryCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
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
    private static final String KEY_FLUID_OUTPUT = "FluidOutputQuota";
    /** Layout 2 added the fourth Assembler input; layout 3 preserves every inventory slot. */
    private static final String KEY_ITEM_LAYOUT = "ItemLayout";
    private static final int CURRENT_ITEM_LAYOUT = 3;
    private static final int FOUR_INPUT_LAYOUT = 2;

    private final ProcessMachineKind kind;
    private final SimpleContainer items;
    private final StatusDebounce display = new StatusDebounce();
    private final SimpleContainerData data = new SimpleContainerData(ProcessMachineMenu.DATA_SIZE);
    private final FluidBuffer fluid;
    private final NativeFluidStore nativeFluids;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final int[] allSlots;

    private double progress;
    private String cachedRecipeId = "";
    private String workRecipeId = "";
    private ProcessRecipe cached;
    private List<ProcessRecipe> recipeGeneration = ProcessLookup.recipes();
    private TickSubscription working;
    private TickSubscription filling;
    private TickSubscription exporting;
    private TickSubscription draining;
    private int fluidOutput;
    private long lastWorkTick = Long.MIN_VALUE;
    private ConditionFault lastFault = ConditionFault.NONE;
    private WorkshopStructure.Fault structureFault = WorkshopStructure.Fault.NONE;
    private SourceSite.Fault siteFault = SourceSite.Fault.NONE;
    private boolean loadingInventory;

    public ProcessMachineBlockEntity(ProcessMachineKind kind, BlockPos pos, BlockState state) {
        this(kind.type(), kind, pos, state);
    }

    private ProcessMachineBlockEntity(BlockEntityType<ProcessMachineBlockEntity> type,
                                      ProcessMachineKind kind, BlockPos pos, BlockState state) {
        super(type, pos, state, kind.tier());
        this.kind = kind;
        this.fluid = new FluidBuffer(FluidLogic.MACHINE_CAPACITY, kind.fluidMaxC(), kind.fluidMaxP());
        this.nativeFluids = new NativeFluidStore(fluid, () -> fluidOutput, quota -> fluidOutput = quota, () -> {
            cached = null;
            setChanged();
            updateSubscriptions();
        });
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
        return powerTier();
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
        if (!kind.heldConditions().equals(conditions())) {
            setConditions(kind.heldConditions());
        }
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
        siteFault = checkSite();
        List<FluidInsert.Source> sources = FluidInsert.neighbours(getLevel(), getBlockPos());
        ProcessRecipe recipe = recipe(sources);
        ConditionReport regulation = kind.regulationReport(recipe);
        boolean completed = recipe != null && progress >= recipe.durationTicks();
        boolean missing = recipe == null ? hasPartialInput() : completed && !hasInputs(recipe, sources);
        boolean stuck = completed && !missing && !canOutput(recipe);
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
        if (fluidOutput > 0) {
            draining = subscriptions().subscribe(draining, this::pushProducedFluid);
        } else if (draining != null) {
            draining.unsubscribe();
            draining = null;
        }
        if (recipe == null || missing || stuck || structureFault != WorkshopStructure.Fault.NONE
                || siteFault != SourceSite.Fault.NONE || !regulation.canRun()) {
            publishStatus(recipe, missing, stuck, hasPowerWaiting(), false, regulation);
        }
        syncData();
    }

    public void onNeighbourChanged() {
        cached = null;
        updateSubscriptions();
    }

    private void work() {
        work(getLevel().getGameTime(), FluidInsert.neighbours(getLevel(), getBlockPos()));
    }

    private void work(long tick, List<FluidInsert.Source> sources) {
        if (lastWorkTick == tick) {
            return;
        }
        lastWorkTick = tick;
        structureFault = checkStructure();
        siteFault = checkSite();
        if (structureFault != WorkshopStructure.Fault.NONE || siteFault != SourceSite.Fault.NONE) {
            publishStatus(cached, false, false, false, false, ConditionReport.OPTIMAL);
            syncData();
            return;
        }
        ProcessRecipe recipe = recipe(sources);
        if (recipe == null) {
            progress = 0.0;
            cachedRecipeId = "";
            workRecipeId = "";
            publishStatus(null, hasPartialInput(), false, false, false, ConditionReport.OPTIMAL);
            updateSubscriptions();
            return;
        }
        ConditionReport report = regulateConditions(recipe);
        if (!report.canRun()) {
            publishStatus(recipe, false, false, hasPowerWaiting(), false, report);
            syncData();
            return;
        }
        if (progress >= recipe.durationTicks()) {
            boolean missing = !hasInputs(recipe, sources);
            boolean stuck = !missing && !canOutput(recipe);
            if (missing || stuck) {
                publishStatus(recipe, missing, stuck, hasPowerWaiting(), false, report);
            } else if (!complete(recipe, sources)) {
                publishStatus(recipe, true, false, hasPowerWaiting(), false, report);
            }
            updateSubscriptions();
            syncData();
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
            complete(recipe, sources);
        }
        ProcessRecipe next = recipe(sources);
        boolean completed = next != null && progress >= next.durationTicks();
        boolean missing = next == null ? hasPartialInput() : completed && !hasInputs(next, sources);
        boolean stuck = completed && !missing && !canOutput(next);
        publishStatus(next, missing, stuck, powered, gained > 0.0, report);
        syncData();
        updateSubscriptions();
    }

    private ConditionReport regulateConditions(ProcessRecipe recipe) {
        var held = kind.regulatesConditions() ? kind.heldConditions(recipe) : kind.heldConditions();
        if (!held.equals(conditions())) {
            setConditions(held);
        }
        ConditionReport report = kind.regulationReport(recipe);
        if (report.canRun()) {
            report = ProcessLookup.conditionsOf(recipe).evaluate(conditions());
        }
        lastFault = report.fault();
        return report;
    }

    private boolean hasInputs(ProcessRecipe recipe, List<FluidInsert.Source> sources) {
        return ProcessLookup.matches(recipe, inputs())
                && FluidInsert.plan(nativeFluids, recipe.fluidInputs(), sources).isPresent();
    }

    private boolean complete(ProcessRecipe recipe, List<FluidInsert.Source> sources) {
        if (!ProcessLookup.matches(recipe, inputs()) || !canOutput(recipe)) {
            return false;
        }
        Optional<NativeFluidStore.Batch> plan = FluidInsert.plan(nativeFluids, recipe.fluidInputs(), sources);
        if (plan.isEmpty()) {
            return false;
        }
        ItemStack[] outputs = plannedOutputs(recipe);
        if (outputs == null) {
            return false;
        }
        Completion settlement = new Completion(recipe, outputs);
        boolean completed;
        loadingInventory = true;
        try {
            completed = plan.get().commit(settlement);
        } finally {
            loadingInventory = false;
        }
        if (!completed) {
            Grindless.LOG.warn("Native fluid sources changed while completing {} at {}; "
                    + "tentative withdrawals were restored", recipe.id(), getBlockPos());
            return false;
        }
        captureOrVent(recipe);
        pushOutputs();
        setChanged();
        return true;
    }

    private final class Completion implements NativeFluidStore.Settlement {
        private final ProcessRecipe recipe;
        private final ItemStack[] outputs;
        private final ItemStack[] beforeItems = new ItemStack[items.getContainerSize()];
        private final double beforeProgress = progress;
        private final String beforeRecipeId = cachedRecipeId;
        private final String beforeWorkRecipeId = workRecipeId;
        private final ProcessRecipe beforeRecipe = cached;

        private Completion(ProcessRecipe recipe, ItemStack[] outputs) {
            this.recipe = recipe;
            this.outputs = outputs;
            for (int slot = 0; slot < beforeItems.length; slot++) {
                beforeItems[slot] = items.getItem(slot).copy();
            }
        }

        @Override
        public void apply() {
            for (int i = 0; i < recipe.itemInputs().size(); i++) {
                items.removeItem(i, recipe.itemInputs().get(i).count());
            }
            for (int i = 0; i < outputs.length; i++) {
                items.setItem(kind.menuKind().inputs() + i, outputs[i]);
            }
            storeFluidOutputs(recipe);
            progress = Math.max(0.0, progress - recipe.durationTicks());
            cached = null;
        }

        @Override
        public void restore() {
            for (int slot = 0; slot < beforeItems.length; slot++) {
                items.setItem(slot, beforeItems[slot].copy());
            }
            progress = beforeProgress;
            cachedRecipeId = beforeRecipeId;
            workRecipeId = beforeWorkRecipeId;
            cached = beforeRecipe;
        }
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

    private void pushProducedFluid() {
        if (!isDue(4) || getLevel() == null) {
            return;
        }
        Direction front = getBlockState().getValue(MachineProperties.FACING);
        if (!containerConfig().allowsExtract(front)) {
            return;
        }
        ProcessRecipe active = recipe();
        int reserved = active == null || active.fluidInputs().isEmpty()
                ? 0 : active.fluidInputs().get(0).count();
        int available = Math.min(fluidOutput, Math.max(0, fluid.state().millibuckets() - reserved));
        FluidState offered = fluid.state().withAmount(Math.min(available, FluidLogic.CONDUIT_MB_PER_TICK));
        if (offered.isEmpty()) {
            return;
        }
        FluidState leftover = FluidInsert.intoNeighbour(getLevel(), getBlockPos(), front, offered);
        int moved = offered.millibuckets() - leftover.millibuckets();
        if (moved < 0 || moved > offered.millibuckets()) {
            throw new IllegalStateException("A fluid endpoint returned an invalid remainder");
        }
        if (moved > 0) {
            fluid.extract(null, moved);
            fluidOutput -= moved;
            cached = null;
            setChanged();
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
        return recipe(FluidInsert.neighbours(getLevel(), getBlockPos()));
    }

    private ProcessRecipe recipe(List<FluidInsert.Source> sources) {
        refreshRecipeGeneration();
        if (cached != null) {
            return cached;
        }
        Optional<ProcessRecipe> found = recipeGeneration.stream()
                .filter(row -> row.id().equals(workRecipeId) && row.family() == kind.family()
                        && progress >= row.durationTicks() && ProcessLookup.matches(row, inputs()))
                .findFirst();
        if (found.isEmpty()) {
            found = ProcessLookup.find(kind.family(), inputs(), fluid.state(), FluidInsert.available(sources));
        }
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

    private boolean refreshRecipeGeneration() {
        List<ProcessRecipe> published = ProcessLookup.recipes();
        if (published == recipeGeneration) {
            return false;
        }
        recipeGeneration = published;
        cached = null;
        return true;
    }

    private WorkshopStructure.Fault checkStructure() {
        if (kind.structure() == WorkshopStructure.NONE || getLevel() == null) {
            return WorkshopStructure.Fault.NONE;
        }
        int turns = horizontalTurns(getBlockState());
        return kind.structure().check(turns, offset -> {
            BlockPos cell = getBlockPos().offset(offset.x(), offset.y(), offset.z());
            if (!getLevel().hasChunkAt(cell)) {
                return null;
            }
            BlockState state = getLevel().getBlockState(cell);
            return new WorkshopStructure.Present(state.isAir() ? "minecraft:air"
                    : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                    state.hasProperty(MachineProperties.FACING) ? horizontalTurns(state) : -1);
        });
    }

    private static int horizontalTurns(BlockState state) {
        return switch (state.getValue(MachineProperties.FACING)) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalStateException("a workshop component must face horizontally");
        };
    }

    private SourceSite.Fault checkSite() {
        if (kind.site() == SourceSite.NONE || getLevel() == null) {
            return SourceSite.Fault.NONE;
        }
        String dimension = getLevel().dimension().location().toString();
        boolean terrestrial = getLevel().dimension().equals(Level.OVERWORLD) || FactoryCatalogue.isFactory(dimension);
        boolean rock = getLevel().getBlockState(getBlockPos().below()).is(
                TagKey.create(Registries.BLOCK, Grindless.id("geological_bed")));
        boolean sky = getLevel().canSeeSky(getBlockPos().above());
        boolean daylight = getLevel().getBrightness(LightLayer.SKY, getBlockPos().above())
                - getLevel().getSkyDarken() >= 9;
        return kind.site().evaluate(terrestrial, rock, sky, daylight);
    }

    /** A bounded bootstrap fallback when the player has not yet made automatic power. */
    public void crankByHand() {
        if (kind.tier() != FluxTier.F0 || getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        energy().setStored(Math.min(energy().getCapacity(), energy().getStored() + SourceLogic.MANUAL_CHARGE));
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
            int accepted = Math.min(FluidLogic.CONDUIT_MB_PER_TICK, acceptedAmount(side, present));
            if (accepted <= 0) {
                continue;
            }
            FluidState taken = endpoint.extract(side.getOpposite(), accepted);
            FluidState leftover = fluid.offer(taken);
            if (!leftover.isEmpty()) {
                throw new IllegalStateException("Reserved primary fluid did not fit the process buffer");
            }
            if (!taken.isEmpty()) {
                cached = null;
                setChanged();
            }
        }
    }

    private void storeFluidOutputs(ProcessRecipe recipe) {
        for (OutputSpec output : recipe.fluidOutputs()) {
            if (output.vented()) {
                continue;
            }
            if (!fluid.offer(FluidLogic.emitted(output.id(), output.count())).isEmpty()) {
                throw new IllegalStateException("Reserved fluid output did not fit the process buffer");
            }
            fluidOutput += output.count();
        }
    }

    private void captureOrVent(ProcessRecipe recipe) {
        if (getLevel() == null) {
            return;
        }
        for (OutputSpec output : recipe.fluidOutputs()) {
            if (!output.vented()) {
                continue;
            }
            FluidState remaining = FluidLogic.emitted(output.id(), output.count());
            for (Direction side : Direction.values()) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = FluidInsert.intoNeighbour(getLevel(), getBlockPos(), side, remaining);
            }
        }
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return acceptedAmount(from, state) > 0;
    }

    @Override
    public int acceptedAmount(Direction from, FluidState state) {
        if (state == null || state.isEmpty() || from != null && !containerConfig().allowsInsert(from)) {
            return 0;
        }
        int target = ProcessLookup.primaryFluidTarget(kind.family(), inputs(), state.id());
        return Math.min(fluid.accepted(state), Math.max(0, target - fluid.state().millibuckets()));
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        int accepted = acceptedAmount(from, state);
        if (accepted == 0) {
            return state;
        }
        if (!fluid.offer(state.withAmount(accepted)).isEmpty()) {
            throw new IllegalStateException("Reserved inserted fluid did not fit the process buffer");
        }
        cached = null;
        setChanged();
        updateSubscriptions();
        return state.withAmount(state.millibuckets() - accepted);
    }

    @Override
    public boolean canExtract(Direction from) {
        return !fluid.isEmpty() && (from == null || containerConfig().allowsExtract(from));
    }

    @Override
    public FluidState extract(Direction from, int millibuckets) {
        if (!canExtract(from)) {
            return FluidState.EMPTY;
        }
        FluidState taken = fluid.extract(null, millibuckets);
        if (!taken.isEmpty()) {
            fluidOutput = Math.max(0, fluidOutput - taken.millibuckets());
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
    public NativeFluidStore nativeStore() {
        return nativeFluids;
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
        if (getLevel() != null && getLevel().isClientSide()) {
            return;
        }
        lastFault = report.fault();
        MachineStatus observed = ProcessLogic.status(recipe != null, missing, stuck,
                report.canRun() || recipe == null, powered, workingNow);
        if (recipe == null && fluidOutput > 0) {
            observed = MachineStatus.BLOCKED;
        }
        if (structureFault != WorkshopStructure.Fault.NONE || siteFault != SourceSite.Fault.NONE) {
            observed = MachineStatus.OUT_OF_BAND;
        }
        boolean stableIdle = recipe == null && !missing && structureFault == WorkshopStructure.Fault.NONE
                && siteFault == SourceSite.Fault.NONE;
        if ((stableIdle ? display.settle(MachineStatus.IDLE) : display.observe(observed))
                && getLevel() != null) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    private void syncData() {
        ProcessRecipe recipe = cached;
        FluidState storedFluid = fluid.state();
        int cycle = recipe == null ? 1 : recipe.durationTicks();
        data.set(ProcessMachineMenu.DATA_ENERGY, (int) Math.min(Integer.MAX_VALUE, energy().getStored()));
        data.set(ProcessMachineMenu.DATA_CAPACITY, (int) Math.min(Integer.MAX_VALUE, energy().getCapacity()));
        data.set(ProcessMachineMenu.DATA_PROGRESS, (int) Math.min(cycle, progress));
        data.set(ProcessMachineMenu.DATA_CYCLE, cycle);
        data.set(ProcessMachineMenu.DATA_STATUS, display.shown().ordinal());
        data.set(ProcessMachineMenu.DATA_KIND, kind.menuKind().ordinal());
        data.set(ProcessMachineMenu.DATA_STRUCTURE, structureFault.ordinal());
        data.set(ProcessMachineMenu.DATA_FLUID, storedFluid.millibuckets());
        data.set(ProcessMachineMenu.DATA_FLUID_OUTPUT, fluidOutput);
        data.set(ProcessMachineMenu.DATA_SITE, siteFault.ordinal());
        if (getLevel() != null && !getLevel().isClientSide()) {
            data.set(ProcessMachineMenu.DATA_FLUID_IS_WATER, MachineFluidView.waterFlag(storedFluid));
        }
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
        tag.putInt(KEY_FLUID_OUTPUT, fluidOutput);
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
            fluidOutput = 0;
            if (tag.contains(KEY_FLUID_OUTPUT)) {
                if (tag.get(KEY_FLUID_OUTPUT) instanceof NumericTag value
                        && Double.isFinite(value.getAsDouble())
                        && value.getAsDouble() == value.getAsInt()
                        && value.getAsInt() >= 0 && value.getAsInt() <= fluid.state().millibuckets()) {
                    fluidOutput = value.getAsInt();
                } else {
                    Grindless.LOG.warn("Ignoring invalid process fluid output quota at {}; stored fluid is preserved",
                            getBlockPos());
                }
            }
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
