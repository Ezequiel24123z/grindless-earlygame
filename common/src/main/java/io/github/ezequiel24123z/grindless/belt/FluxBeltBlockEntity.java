package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.PoweredLogisticsBlockEntity;
import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One Flux Belt tile. Twice the conveyor, and only while it spends LV (ADR-0082).
 *
 * <p>An empty tile draws nothing and is not subscribed. Items that arrive subscribe it.
 * Without power the items stay where they are.
 */
public final class FluxBeltBlockEntity extends PoweredLogisticsBlockEntity
        implements BeltEndpoint, BeltView, WorldlyContainer {

    private static final String KEY_LANES = "Lanes";
    private static final int[] INSERT_SLOTS = {0};
    private static final int[] EXTRACT_SLOTS = {1};

    private final Lane left = new Lane();
    private final Lane right = new Lane();
    private TickSubscription working;

    public FluxBeltBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_BELT.get(), pos, state);
    }

    @Override
    public Lane left() {
        return left;
    }

    @Override
    public Lane right() {
        return right;
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        if (left.isEmpty() && right.isEmpty()) {
            if (working != null) {
                working.unsubscribe();
                working = null;
            }
            MachineProperties.publish(getLevel(), getBlockPos(), MachineStatus.IDLE);
            return;
        }
        working = subscriptions().subscribe(working, this::work);
    }

    private void work() {
        if (left.isEmpty() && right.isEmpty()) {
            updateSubscriptions();
            return;
        }
        if (!draw()) {
            MachineProperties.publish(getLevel(), getBlockPos(), MachineStatus.STARVED);
            return;
        }
        double delta = FluxBeltLogic.travel(1);
        left.advance(delta);
        right.advance(delta);
        boolean moved = offer(left) | offer(right);
        if (moved || !left.isEmpty() || !right.isEmpty()) {
            setChanged();
            sync();
        }
        MachineProperties.publish(getLevel(), getBlockPos(),
                left.isEmpty() && right.isEmpty() ? MachineStatus.IDLE : MachineStatus.RUNNING);
        if (left.isEmpty() && right.isEmpty()) {
            updateSubscriptions();
        }
    }

    private boolean offer(Lane lane) {
        LaneItem ready = lane.peekFront();
        if (ready == null || !BeltLogic.readyToLeave(ready.position())) {
            return false;
        }
        Direction facing = facing();
        BlockEntity next = getLevel().getBlockEntity(getBlockPos().relative(facing));
        if (!(next instanceof BeltEndpoint endpoint)) {
            return false;
        }
        ItemStack stack = BeltStacks.toStack(ready);
        if (stack.isEmpty() || !endpoint.canInsert(facing.getOpposite(), stack)) {
            return false;
        }
        ItemStack leftover = endpoint.insert(facing.getOpposite(), stack);
        if (leftover.isEmpty()) {
            lane.takeReady();
            return true;
        }
        return false;
    }

    @Override
    public boolean canInsert(Direction from, ItemStack stack) {
        return stack != null && !stack.isEmpty() && incomingLane(from).canAccept();
    }

    @Override
    public ItemStack insert(Direction from, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        Lane lane = incomingLane(from);
        LaneItem item = BeltStacks.fromStack(stack, 0.0);
        if (item != null && lane.insert(item)) {
            setChanged();
            sync();
            updateSubscriptions();
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean canExtract(Direction from) {
        return pickLane(from).peekFront() != null;
    }

    @Override
    public ItemStack preview(Direction from) {
        return BeltStacks.toStack(pickLane(from).nearest(extractTarget(from)));
    }

    @Override
    public ItemStack extract(Direction from, int max) {
        if (max <= 0) {
            return ItemStack.EMPTY;
        }
        Lane lane = pickLane(from);
        LaneItem item = lane.takeNearest(extractTarget(from));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        setChanged();
        sync();
        updateSubscriptions();
        return BeltStacks.toStack(item);
    }

    private Lane incomingLane(Direction from) {
        Direction facing = facing();
        if (from == facing.getCounterClockWise()) {
            return left;
        }
        if (from == facing.getClockWise()) {
            return right;
        }
        return emptier();
    }

    private Lane pickLane(Direction from) {
        Direction facing = facing();
        if (from == facing.getCounterClockWise()) {
            return left;
        }
        if (from == facing.getClockWise()) {
            return right;
        }
        Lane front = left.peekFront() != null && (right.peekFront() == null
                || left.peekFront().position() >= right.peekFront().position()) ? left : right;
        return front.peekFront() != null ? front : emptier();
    }

    private double extractTarget(Direction from) {
        Direction facing = facing();
        if (from == facing) {
            return 1.0;
        }
        if (from == facing.getOpposite()) {
            return 0.0;
        }
        return 0.5;
    }

    private Lane emptier() {
        if (left.size() != right.size()) {
            return left.size() < right.size() ? left : right;
        }
        double leftPos = left.peekFront() == null ? 1.0 : left.peekFront().position();
        double rightPos = right.peekFront() == null ? 1.0 : right.peekFront().position();
        return leftPos >= rightPos ? left : right;
    }

    private void sync() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag lanes = new ListTag();
        CompoundTag leftTag = new CompoundTag();
        BeltStacks.saveLane(leftTag, left);
        lanes.add(leftTag);
        CompoundTag rightTag = new CompoundTag();
        BeltStacks.saveLane(rightTag, right);
        lanes.add(rightTag);
        tag.put(KEY_LANES, lanes);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        left.clear();
        right.clear();
        ListTag lanes = tag.getList(KEY_LANES, Tag.TAG_COMPOUND);
        if (lanes.size() > 0) {
            BeltStacks.loadLane(lanes.getCompound(0), left);
        }
        if (lanes.size() > 1) {
            BeltStacks.loadLane(lanes.getCompound(1), right);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return INSERT_SLOTS;
        }
        if (side == Direction.DOWN) {
            return EXTRACT_SLOTS;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && canInsert(side == null ? Direction.UP : side, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && canExtract(side == null ? Direction.DOWN : side);
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return left.isEmpty() && right.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == 0) {
            return ItemStack.EMPTY;
        }
        return BeltStacks.toStack(pickLane(Direction.DOWN).peekFront());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 1) {
            return ItemStack.EMPTY;
        }
        return extract(Direction.DOWN, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 64);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && stack != null && !stack.isEmpty()) {
            insert(Direction.UP, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        left.clear();
        right.clear();
        setChanged();
        updateSubscriptions();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && (left.canAccept() || right.canAccept());
    }
}
