package io.github.ezequiel24123z.grindless.machine;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.container.NeighbourCache;
import io.github.ezequiel24123z.grindless.container.SlottedContainer;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.FuelPlatform;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.recipe.ThermalLogic;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.SimpleContainer;

import java.util.EnumMap;
import java.util.Map;

/**
 * Burns furnace fuel at F1 and pushes 32 FU/t into the covering pylon, then neighbours.
 */
public final class ThermalGeneratorBlockEntity extends MachineBlockEntity
        implements WorldlyContainer, ExtendedMenuProvider, MachineMenuHost {

    private static final String KEY_BURN = "Burn";
    private static final String KEY_DURATION = "Duration";
    private static final String KEY_FUEL = "Fuel";
    private static final String KEY_GAS = "GasFuel";
    private static final int[] SLOTS = {0};

    private final SimpleContainer fuel = new SlottedContainer(1);
    private final GasFuel gas = new GasFuel();
    private final GeneratorKind kind;
    private final StatusDebounce display = new StatusDebounce();
    private final Map<Direction, NeighbourCache> neighbours = new EnumMap<>(Direction.class);
    private final SimpleContainerData data = new SimpleContainerData(ProcessMachineMenu.DATA_SIZE);

    private int burnRemaining;
    private int burnDuration;
    private int fruitlessPushes;
    private TickSubscription working;
    private boolean loading;
    private long lastWorkTick = Long.MIN_VALUE;

    public ThermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        this(GeneratorKind.THERMAL, pos, state);
    }

    public ThermalGeneratorBlockEntity(GeneratorKind kind, BlockPos pos, BlockState state) {
        this(kind.type(), kind, pos, state);
    }

    private ThermalGeneratorBlockEntity(BlockEntityType<ThermalGeneratorBlockEntity> type,
                                        GeneratorKind kind, BlockPos pos, BlockState state) {
        super(type, pos, state, kind.tier());
        this.kind = kind;
        fuel.addListener(container -> {
            if (loading) {
                return;
            }
            setChanged();
            updateSubscriptions();
        });
        syncData();
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
        return powerTier();
    }

    @Override
    protected int bufferSeconds() {
        return 10;
    }

    @Override
    protected SimpleFluxStorage createEnergyBuffer() {
        return new SimpleFluxStorage(bufferCapacity(), 0L, powerTier().nominal(), () -> {
            setChanged();
            if (getLevel() != null && !getLevel().isClientSide()) {
                updateSubscriptions();
            }
        });
    }

    @Override
    protected void updateSubscriptions() {
        if (loading || getLevel() == null) {
            return;
        }
        if (burnRemaining > 0 || canIgnite() || energy().getStored() > 0 || gas.hasReturn()) {
            working = subscriptions().subscribe(working, this::work);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
        publishStatus(false);
        syncData();
    }

    public void onNeighbourChanged() {
        neighbours.values().forEach(NeighbourCache::invalidate);
        fruitlessPushes = 0;
        updateSubscriptions();
    }

    private void work() {
        long tick = getLevel().getGameTime();
        if (lastWorkTick == tick) {
            return;
        }
        lastWorkTick = tick;
        returnGas();
        boolean burning = false;
        if (energy().getCapacity() - energy().getStored() >= kind.tier().nominal()
                && (burnRemaining > 0 || tryIgnite())) {
            burnRemaining--;
            energy().setStored(energy().getStored() + kind.tier().nominal());
            burning = true;
        }
        push();
        setChanged();
        publishStatus(burning);
        syncData();
        updateSubscriptions();
    }

    private boolean canIgnite() {
        return FuelPlatform.burnTicks(fuel.getItem(0)) > 0
                || kind.acceptsGas() && (gas.ready() || neighbourHasGas());
    }

    private boolean neighbourHasGas() {
        if (getLevel() == null) {
            return false;
        }
        for (Direction side : Direction.values()) {
            var neighbour = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (neighbour instanceof FluidEndpoint endpoint && endpoint.canExtract(side.getOpposite())
                    && gas.canCollect(endpoint.contents())) {
                return true;
            }
        }
        return false;
    }

    private boolean tryIgnite() {
        return tryIgniteItem(FuelPlatform.burnTicks(fuel.getItem(0)))
                || kind.acceptsGas() && tryIgniteGas();
    }

    private boolean tryIgniteItem(int ticks) {
        if (ticks > 0) {
            burnRemaining = ticks;
            burnDuration = ticks;
            ItemStack stack = fuel.getItem(0);
            stack.shrink(1);
            fuel.setItem(0, stack);
            return true;
        }
        return false;
    }

    /** Partial deliveries remain private until one complete bucket can ignite. */
    private boolean tryIgniteGas() {
        int ticks = gas.ignite();
        if (ticks > 0) {
            burnRemaining = ticks;
            burnDuration = ticks;
            return true;
        }
        if (getLevel() == null) {
            return false;
        }
        for (Direction side : Direction.values()) {
            var neighbour = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (!(neighbour instanceof FluidEndpoint endpoint)) {
                continue;
            }
            gas.collect(endpoint, side.getOpposite());
            ticks = gas.ignite();
            if (ticks > 0) {
                burnRemaining = ticks;
                burnDuration = ticks;
                return true;
            }
        }
        return false;
    }

    private void returnGas() {
        if (!gas.hasReturn() || gas.returnFace == null || getLevel() == null) {
            return;
        }
        var neighbour = getLevel().getBlockEntity(getBlockPos().relative(gas.returnFace.getOpposite()));
        if (neighbour instanceof FluidEndpoint endpoint) {
            gas.returnTo(endpoint);
        }
    }

    /** A refused contract-violation refund is escrow, not additional burnable capacity. */
    private static final class GasFuel {
        private FluidState stored = FluidState.EMPTY;
        private FluidState pendingReturn = FluidState.EMPTY;
        private Direction returnFace;

        private boolean hasReturn() {
            return !pendingReturn.isEmpty();
        }

        private boolean ready() {
            return !hasReturn() && stored.millibuckets() >= FluidLogic.BUCKET
                    && FluidLogic.burnTicks(stored.id()) > 0;
        }

        private boolean canCollect(FluidState available) {
            return !hasReturn() && stored.millibuckets() < FluidLogic.BUCKET
                    && available != null && !available.isEmpty() && FluidLogic.burnTicks(available.id()) > 0
                    && (stored.isEmpty() || stored.is(available.id()));
        }

        private void collect(FluidEndpoint endpoint, Direction face) {
            if (!endpoint.canExtract(face)) {
                return;
            }
            FluidState available = endpoint.contents();
            if (!canCollect(available)) {
                return;
            }
            int requested = Math.min(FluidLogic.BUCKET - stored.millibuckets(), available.millibuckets());
            FluidState taken = endpoint.extract(face, requested);
            if (taken == null || taken.isEmpty()) {
                return;
            }
            int kept = taken.is(available.id()) ? Math.min(requested, taken.millibuckets()) : 0;
            if (kept > 0) {
                stored = stored.merge(taken.withAmount(kept));
            }
            pendingReturn = taken.withAmount(taken.millibuckets() - kept);
            if (hasReturn()) {
                returnFace = face;
                returnTo(endpoint);
                if (hasReturn()) {
                    Grindless.LOG.warn("Retaining {} mB of {} after a fuel-gas extraction contract violation; "
                                    + "return to source face {} is blocked",
                            pendingReturn.millibuckets(), pendingReturn.id(), returnFace);
                }
            }
        }

        private void returnTo(FluidEndpoint endpoint) {
            if (!hasReturn() || returnFace == null) {
                return;
            }
            FluidState remaining = endpoint.insert(returnFace, pendingReturn);
            if (remaining == null || !remaining.isEmpty() && !remaining.is(pendingReturn.id())
                    || remaining.millibuckets() > pendingReturn.millibuckets()) {
                Grindless.LOG.warn("Fuel-gas endpoint returned an invalid refund remainder; escrow is retained");
                return;
            }
            pendingReturn = remaining;
            if (!hasReturn()) {
                returnFace = null;
            }
        }

        private int ignite() {
            if (!ready()) {
                return 0;
            }
            int ticks = FluidLogic.burnTicks(stored.id());
            stored = stored.withAmount(stored.millibuckets() - FluidLogic.BUCKET);
            return ticks;
        }

        private void save(CompoundTag root) {
            CompoundTag tag = new CompoundTag();
            tag.put("Stored", FluidNbt.save(stored));
            tag.put("Return", FluidNbt.save(pendingReturn));
            if (returnFace != null) {
                tag.putString("ReturnFace", returnFace.getName());
            }
            root.put(KEY_GAS, tag);
        }

        private void load(CompoundTag root) {
            CompoundTag tag = root.getCompound(KEY_GAS);
            stored = FluidNbt.load(tag.getCompound("Stored"));
            pendingReturn = FluidNbt.load(tag.getCompound("Return"));
            returnFace = Direction.byName(tag.getString("ReturnFace"));
        }
    }

    private void push() {
        long allowance = Math.min(energy().getStored(), kind.tier().nominal());
        long budget = allowance;
        FluxNetwork network = network();
        if (network != null) {
            budget -= network.receive(budget, false);
        }
        for (Direction side : Direction.values()) {
            if (budget <= 0L) {
                break;
            }
            if (!containerConfig().allowsExtract(side)) {
                continue;
            }
            FluxStorage target = neighbours
                    .computeIfAbsent(side, s -> new NeighbourCache(getLevel(), getBlockPos(), s))
                    .get();
            if (target == null || !target.canReceive()) {
                continue;
            }
            budget -= target.receive(budget, false);
        }
        long spent = allowance - budget;
        if (spent > 0L) {
            energy().setStored(energy().getStored() - spent);
            fruitlessPushes = 0;
        } else if (allowance > 0L) {
            fruitlessPushes++;
        }
    }

    private void publishStatus(boolean burning) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        if (observeStatus(burning)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    private boolean observeStatus(boolean burning) {
        MachineStatus observed = ThermalLogic.status(burning || burnRemaining > 0,
                energy().getStored(), fruitlessPushes);
        if (!burning && burnRemaining == 0 && gas.hasReturn()) {
            observed = MachineStatus.BLOCKED;
        }
        // An unsubscribed machine has no future observations to finish an idle debounce.
        boolean stableIdle = !burning && burnRemaining == 0 && energy().getStored() == 0
                && working == null && !gas.hasReturn();
        return stableIdle ? display.settle(MachineStatus.IDLE) : display.observe(observed);
    }

    private void syncData() {
        data.set(ProcessMachineMenu.DATA_ENERGY, (int) Math.min(Integer.MAX_VALUE, energy().getStored()));
        data.set(ProcessMachineMenu.DATA_CAPACITY, (int) Math.min(Integer.MAX_VALUE, energy().getCapacity()));
        data.set(ProcessMachineMenu.DATA_PROGRESS, burnDuration - burnRemaining);
        data.set(ProcessMachineMenu.DATA_CYCLE, Math.max(1, burnDuration));
        data.set(ProcessMachineMenu.DATA_STATUS, display.shown().ordinal());
        data.set(ProcessMachineMenu.DATA_KIND, MachineMenuKind.GENERATOR.ordinal());
    }

    @Override
    public void setRemoved() {
        neighbours.values().forEach(NeighbourCache::invalidate);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_BURN, burnRemaining);
        tag.putInt(KEY_DURATION, burnDuration);
        tag.put(KEY_FUEL, fuel.createTag());
        gas.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        loading = true;
        try {
            super.load(tag);
            burnRemaining = Math.max(0, tag.getInt(KEY_BURN));
            burnDuration = Math.max(0, tag.getInt(KEY_DURATION));
            fuel.fromTag(tag.getList(KEY_FUEL, net.minecraft.nbt.Tag.TAG_COMPOUND));
            gas.load(tag);
        } finally {
            loading = false;
        }
        updateSubscriptions();
        syncData();
    }

    @Override
    public MachineMenuKind menuKind() {
        return MachineMenuKind.GENERATOR;
    }

    @Override
    public Container menuContainer() {
        return fuel;
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
        return gas.hasReturn()
                ? Component.literal("Fuel-gas return is blocked; make room in the source endpoint")
                : Component.empty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grindless." + kind.id());
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
        return fuel.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return fuel.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return fuel.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return fuel.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return fuel.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        fuel.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        fuel.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // Coals are always fuel. The platform call covers other mods' furnace fuels
        // and must not be the only check: a missing transform would reject coal and
        // starve the first iron line.
        return stack.is(ItemTags.COALS) || FuelPlatform.burnTicks(stack) > 0;
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
        return true;
    }
}
