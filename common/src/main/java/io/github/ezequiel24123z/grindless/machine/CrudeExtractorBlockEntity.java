package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.ItemInsert;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.planet.PlanetProduct;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.vein.ChunkVein;
import io.github.ezequiel24123z.grindless.vein.VeinData;
import io.github.ezequiel24123z.grindless.vein.VeinGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;

import java.util.List;

/**
 * The Crude Extractor: pulls one unit of the chunk's vein every twenty seconds at F0.
 *
 * <p>T0 has no pylons, so it accepts power from an adjacent dynamo into its own buffer as well as
 * from a Flux Network once one covers it. Output sits in a single slot that hoppers can pull
 * from, and the machine also pushes into neighbouring inventories so a chest on any face is
 * enough (ADR-0053).
 */
public final class CrudeExtractorBlockEntity extends MachineBlockEntity implements Container {

    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_OUTPUT = "Output";

    private final SimpleContainer output = new SimpleContainer(1);
    private final StatusDebounce display = new StatusDebounce();

    private double progress;
    private TickSubscription working;

    public CrudeExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUDE_EXTRACTOR.get(), pos, state);
        output.addListener(container -> {
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

    /**
     * Consuming from the buffer is this machine's own draw, so it writes stored energy directly
     * rather than going through {@link SimpleFluxStorage#extract}: the buffer is insert-only to
     * the rest of the world, and extract would refuse.
     */
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
        ChunkVein vein = vein();
        boolean stuck = progress >= ExtractorLogic.CYCLE_TICKS && !canAcceptProduct(productOf(vein));
        if (vein != null && !stuck) {
            working = subscriptions().subscribe(working, this::work);
        } else if (working != null) {
            working.unsubscribe();
            working = null;
        }
        publishStatus(vein, stuck, hasPowerWaiting(), false);
    }

    public void onNeighbourChanged() {
        updateSubscriptions();
    }

    private void work() {
        ChunkVein vein = vein();
        ItemStack product = productOf(vein);
        boolean stuck = progress >= ExtractorLogic.CYCLE_TICKS && !canAcceptProduct(product);
        if (vein == null || stuck) {
            publishStatus(vein, stuck, false, false);
            updateSubscriptions();
            return;
        }

        requestPower(ExtractorLogic.FU_PER_TICK);
        long drawn = drawPower(ExtractorLogic.FU_PER_TICK);
        if (drawn <= 0L && energy().getStored() >= ExtractorLogic.FU_PER_TICK) {
            energy().setStored(energy().getStored() - ExtractorLogic.FU_PER_TICK);
            drawn = ExtractorLogic.FU_PER_TICK;
        }

        boolean powered = drawn > 0L || hasPowerWaiting();
        double rate = vein.rateAfter(extracted(), upgrades().veinFloor(ChunkVein.FLOOR_FRACTION));
        double gained = ExtractorLogic.work(rate, drawn, ExtractorLogic.FU_PER_TICK);
        if (gained > 0.0) {
            progress += gained;
            setChanged();
        }
        if (progress >= ExtractorLogic.CYCLE_TICKS) {
            if (eject(product)) {
                progress -= ExtractorLogic.CYCLE_TICKS;
                recordExtraction();
                setChanged();
            }
        }
        stuck = progress >= ExtractorLogic.CYCLE_TICKS && !canAcceptProduct(product);
        publishStatus(vein, stuck, powered, gained > 0.0);
        if (stuck) {
            updateSubscriptions();
        }
    }

    /** Covered by a network that still holds charge, or charge in the local buffer. */
    private boolean hasPowerWaiting() {
        if (energy().getStored() >= ExtractorLogic.FU_PER_TICK) {
            return true;
        }
        FluxNetwork network = network();
        return network != null && network.stored() > 0L;
    }

    private ChunkVein vein() {
        if (!(getLevel() instanceof ServerLevel server)) {
            return null;
        }
        List<Material> mineable = PlanetCatalogue.veins(
                server.dimension().location().toString(), MaterialRegistry.snapshot().mineable());
        ChunkPos chunk = new ChunkPos(getBlockPos());
        return VeinGenerator.generate(server.getSeed(), chunk.x, chunk.z, mineable);
    }

    private long extracted() {
        return getLevel() instanceof ServerLevel server
                ? VeinData.get(server).extractedFrom(new ChunkPos(getBlockPos()))
                : 0L;
    }

    private void recordExtraction() {
        if (getLevel() instanceof ServerLevel server) {
            VeinData.get(server).recordExtraction(new ChunkPos(getBlockPos()), 1L);
        }
    }

    private ItemStack productOf(ChunkVein vein) {
        if (!(getLevel() instanceof ServerLevel server)) {
            return ItemStack.EMPTY;
        }
        return PlanetProduct.resolve(server.dimension().location().toString(), vein);
    }

    private boolean canAcceptProduct(ItemStack product) {
        if (product.isEmpty()) {
            return false;
        }
        ItemStack current = output.getItem(0);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(current, product)
                && current.getCount() < current.getMaxStackSize();
    }

    /**
     * Puts {@code product} in the output slot or a neighbour. Returns whether it left the machine
     * or at least sat in the slot; false means the cycle cannot complete.
     */
    private boolean eject(ItemStack product) {
        if (product.isEmpty() || getLevel() == null) {
            return false;
        }
        ItemStack leftover = HopperBlockEntity.addItem(null, output, product.copy(), null);
        if (leftover.isEmpty()) {
            pushOutput();
            return true;
        }
        return false;
    }

    private void pushOutput() {
        ItemStack stack = output.getItem(0);
        if (stack.isEmpty() || getLevel() == null) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (stack.isEmpty()) {
                break;
            }
            stack = ItemInsert.intoNeighbour(getLevel(), getBlockPos(), side, stack);
        }
        output.setItem(0, stack);
    }

    private void publishStatus(ChunkVein vein, boolean stuck, boolean powered, boolean workingNow) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        MachineStatus observed = ExtractorLogic.status(vein != null, stuck, powered, workingNow);
        if (display.observe(observed)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(KEY_PROGRESS, progress);
        tag.put(KEY_OUTPUT, output.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0.0, tag.getDouble(KEY_PROGRESS));
        output.fromTag(tag.getList(KEY_OUTPUT, net.minecraft.nbt.Tag.TAG_COMPOUND));
    }

    // ---- Container: hoppers pull, nothing may insert ------------------------------------

    @Override
    public int getContainerSize() {
        return output.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return output.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return output.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return output.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        output.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        output.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }
}
