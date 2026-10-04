package io.github.ezequiel24123z.grindless.machine;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.container.NeighbourCache;
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
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
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
    private static final int[] SLOTS = {0};

    private final SimpleContainer fuel = new SimpleContainer(1);
    private final StatusDebounce display = new StatusDebounce();
    private final Map<Direction, NeighbourCache> neighbours = new EnumMap<>(Direction.class);
    private final SimpleContainerData data = new SimpleContainerData(ProcessMachineMenu.DATA_SIZE);

    private int burnRemaining;
    private int burnDuration;
    private int fruitlessPushes;
    private TickSubscription working;

    public ThermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_GENERATOR.get(), pos, state);
        fuel.addListener(container -> {
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
        return FluxTier.F1;
    }

    @Override
    protected int bufferSeconds() {
        return 10;
    }

    @Override
    protected SimpleFluxStorage createEnergyBuffer() {
        return new SimpleFluxStorage(bufferCapacity(), 0L, FluxTier.F1.nominal(), () -> {
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
        if (burnRemaining > 0 || canIgnite()) {
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
        if (burnRemaining <= 0 && !tryIgnite()) {
            publishStatus(false);
            updateSubscriptions();
            return;
        }
        burnRemaining--;
        energy().setStored(energy().getStored() + ThermalLogic.generate(true));
        push();
        setChanged();
        publishStatus(true);
        syncData();
        if (burnRemaining <= 0 && !canIgnite() && energy().getStored() <= 0L) {
            updateSubscriptions();
        }
    }

    private boolean canIgnite() {
        return FuelPlatform.burnTicks(fuel.getItem(0)) > 0 || neighbourHasCo();
    }

    private boolean neighbourHasCo() {
        if (getLevel() == null) {
            return false;
        }
        for (Direction side : Direction.values()) {
            var neighbour = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (neighbour instanceof FluidEndpoint endpoint && endpoint.canExtract(side.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private boolean tryIgnite() {
        ItemStack stack = fuel.getItem(0);
        int ticks = FuelPlatform.burnTicks(stack);
        if (ticks > 0) {
            stack.shrink(1);
            fuel.setItem(0, stack);
            burnRemaining = ticks;
            burnDuration = ticks;
            return true;
        }
        return tryIgniteGas();
    }

    /** One bucket of CO or hydrogen. Anything else goes back into the tank (ADR-0065, ADR-0077). */
    private boolean tryIgniteGas() {
        if (getLevel() == null) {
            return false;
        }
        for (Direction side : Direction.values()) {
            var neighbour = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (!(neighbour instanceof FluidEndpoint endpoint) || !endpoint.canExtract(side.getOpposite())) {
                continue;
            }
            FluidState taken = endpoint.extract(side.getOpposite(), FluidLogic.BUCKET);
            int ticks = FluidLogic.burnTicks(taken.id());
            if (ticks > 0 && taken.millibuckets() >= FluidLogic.BUCKET) {
                burnRemaining = ticks;
                burnDuration = ticks;
                return true;
            }
            if (!taken.isEmpty()) {
                endpoint.insert(side.getOpposite(), taken);
            }
        }
        return false;
    }

    private void push() {
        long allowance = Math.min(energy().getStored(), FluxTier.F1.nominal());
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
        MachineStatus observed = ThermalLogic.status(burning || burnRemaining > 0,
                energy().getStored(), fruitlessPushes);
        if (display.observe(observed)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnRemaining = Math.max(0, tag.getInt(KEY_BURN));
        burnDuration = Math.max(0, tag.getInt(KEY_DURATION));
        fuel.fromTag(tag.getList(KEY_FUEL, net.minecraft.nbt.Tag.TAG_COMPOUND));
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
        return Component.empty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grindless.thermal_generator");
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
