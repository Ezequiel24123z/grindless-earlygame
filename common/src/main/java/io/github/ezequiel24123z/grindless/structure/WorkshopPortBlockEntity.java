package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.NativeFluidStore;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** A transient, unique-controller access view. All items, fluid and saved work remain in the controller. */
public final class WorkshopPortBlockEntity extends BlockEntity implements WorldlyContainer, FluidEndpoint {

    public record Inspection(BlockPos ownerPos, WorkshopPortRole role, Direction facing,
                             WorkshopStructure.Fault fault, boolean formed) {
    }

    private record Binding(ProcessMachineBlockEntity owner, WorkshopStructure.Fault fault,
                           WorkshopPortContract.Slot slot) {
    }

    private final WorkshopPortContract.Kind kind;
    private final List<Runnable> invalidators = new ArrayList<>();
    private WeakReference<ProcessMachineBlockEntity> lastOwner = new WeakReference<>(null);
    private WorkshopStructure.Fault lastFault = WorkshopStructure.Fault.ORPHANED_PORT;
    private Direction lastFacing;
    private long capabilityRevision;

    public WorkshopPortBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.WORKSHOP_PORT.get(), ((WorkshopPortBlock) state.getBlock()).kind(), pos, state);
    }

    private WorkshopPortBlockEntity(BlockEntityType<WorkshopPortBlockEntity> type,
                                    WorkshopPortContract.Kind kind, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.kind = kind;
    }

    public WorkshopPortRole role() {
        return kind.role();
    }

    public void addCapabilityInvalidator(Runnable callback) {
        invalidators.add(callback);
    }

    /** Read-only inspection does not cache a formation result or mutate controller/recipe state. */
    public Inspection inspection() {
        Binding binding = resolve();
        return new Inspection(binding.owner() == null ? null : binding.owner().getBlockPos(), role(),
                facing(), binding.fault(), binding.owner() != null && binding.fault() == WorkshopStructure.Fault.NONE);
    }

    public boolean permitsFace(Direction side) {
        Binding binding = current();
        return side != null && level != null && !level.isClientSide()
                && binding.owner() != null && binding.slot() != null
                && binding.slot().permitsFace(turns(facing()), turns(side));
    }

    public void revalidate() {
        current();
    }

    /** A retained loader view must not silently rebind itself to a replacement controller. */
    public long capabilityRevision() {
        current();
        return capabilityRevision;
    }

    private Binding current() {
        Binding binding = resolve();
        Direction facing = facing();
        if (lastOwner.get() != binding.owner() || lastFault != binding.fault() || lastFacing != facing) {
            lastOwner = new WeakReference<>(binding.owner());
            lastFault = binding.fault();
            lastFacing = facing;
            capabilityRevision++;
            invalidateViews();
        }
        return binding;
    }

    private Binding resolve() {
        Binding binding = resolveGeometry();
        if (binding.owner() == null) {
            return binding;
        }
        WorkshopStructure.Inspection structure = binding.owner().inspectionStructureGeometry();
        if (!structure.formed()) {
            return new Binding(null, structure.fault(), null);
        }
        for (WorkshopStructure.Port port : structure.ports()) {
            WorkshopStructure.Offset offset = port.offset();
            BlockPos position = binding.owner().getBlockPos().offset(offset.x(), offset.y(), offset.z());
            if (!(level.getBlockEntity(position) instanceof WorkshopPortBlockEntity other)) {
                return new Binding(null, WorkshopStructure.Fault.ORPHANED_PORT, null);
            }
            Binding otherOwner = other == this ? binding : other.resolveGeometry();
            if (otherOwner.owner() != binding.owner()) {
                return new Binding(null, otherOwner.fault() == WorkshopStructure.Fault.NONE
                        ? WorkshopStructure.Fault.STALE_OWNER : otherOwner.fault(), null);
            }
        }
        return binding;
    }

    private Binding resolveGeometry() {
        if (isRemoved() || level == null || !level.hasChunkAt(worldPosition)
                || level.getBlockEntity(worldPosition) != this
                || !(getBlockState().getBlock() instanceof WorkshopPortBlock block) || block.kind() != kind) {
            return new Binding(null, WorkshopStructure.Fault.STALE_OWNER, null);
        }
        int turns = turns(facing());
        ProcessMachineBlockEntity owner = null;
        WorkshopPortContract.Slot ownerSlot = null;
        WorkshopStructure.Fault fault = WorkshopStructure.Fault.ORPHANED_PORT;
        for (WorkshopPortContract.Slot slot : WorkshopPortContract.slots(kind.structure())) {
            if (!slot.accepts(kind)) {
                continue;
            }
            WorkshopStructure.Offset relative = slot.offset().rotate(turns);
            BlockPos candidate = worldPosition.offset(-relative.x(), -relative.y(), -relative.z());
            if (!level.hasChunkAt(candidate)) {
                return new Binding(null, WorkshopStructure.Fault.UNLOADED, null);
            }
            BlockEntity entity = level.getBlockEntity(candidate);
            if (!(entity instanceof ProcessMachineBlockEntity controller) || entity.isRemoved()
                    || controller.kind().structure() != kind.structure()) {
                continue;
            }
            if (!controller.getBlockState().hasProperty(MachineProperties.FACING)
                    || controller.getBlockState().getValue(MachineProperties.FACING) != facing()) {
                fault = WorkshopStructure.Fault.WRONG_FACING;
                continue;
            }
            WorkshopStructure.Inspection structure = controller.inspectionStructureGeometry();
            if (!structure.formed()) {
                fault = structure.fault();
                continue;
            }
            if (owner != null && owner != controller) {
                return new Binding(null, WorkshopStructure.Fault.AMBIGUOUS_OWNER, null);
            }
            owner = controller;
            ownerSlot = slot;
        }
        return owner == null ? new Binding(null, fault, null)
                : new Binding(owner, WorkshopStructure.Fault.NONE, ownerSlot);
    }

    /** Used by a controller's final formation query; geometric queries never call back into this. */
    public boolean belongsTo(ProcessMachineBlockEntity controller) {
        return resolve().owner() == controller;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineProperties.FACING);
    }

    private static int turns(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> -1;
        };
    }

    private void invalidateViews() {
        for (Runnable invalidator : List.copyOf(invalidators)) {
            invalidator.run();
        }
    }

    @Override
    public void setRemoved() {
        lastOwner.clear();
        lastFault = WorkshopStructure.Fault.STALE_OWNER;
        capabilityRevision++;
        invalidateViews();
        super.setRemoved();
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (invalidators != null) {
            lastOwner.clear();
            lastFacing = null;
            capabilityRevision++;
            invalidateViews();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastOwner.clear();
        lastFault = WorkshopStructure.Fault.ORPHANED_PORT;
        lastFacing = null;
        capabilityRevision++;
        invalidateViews();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WorkshopPortBlockEntity port) {
        if (level.isClientSide() || Math.floorMod(level.getGameTime() + pos.asLong(), 4) != 0) {
            return;
        }
        port.revalidate();
        if (port.role() != WorkshopPortRole.FLUID_OUTPUT) {
            return;
        }
        int remaining = FluidLogic.CONDUIT_MB_PER_TICK;
        for (Direction side : Direction.values()) {
            if (remaining == 0 || !port.permitsFace(side)) {
                continue;
            }
            BlockPos target = pos.relative(side);
            FluidInsert.Source source = new FluidInsert.Source(side, () ->
                    level.hasChunkAt(pos) && level.getBlockEntity(pos) == port ? port : null);
            FluidInsert.Source destination = new FluidInsert.Source(side.getOpposite(), () -> {
                if (!level.hasChunkAt(target)) {
                    return null;
                }
                BlockEntity entity = level.getBlockEntity(target);
                return entity instanceof FluidEndpoint endpoint ? endpoint : null;
            });
            remaining -= FluidInsert.moveNative(source, destination, remaining);
        }
    }

    private ProcessMachineBlockEntity itemOwner() {
        return role().item() ? current().owner() : null;
    }

    private boolean visibleSlot(ProcessMachineBlockEntity owner, int slot) {
        return owner != null && slot >= 0 && slot < owner.getContainerSize()
                && (role().input() ? slot < owner.kind().menuKind().inputs() : slot >= owner.kind().menuKind().inputs());
    }

    @Override
    public int getContainerSize() {
        ProcessMachineBlockEntity owner = itemOwner();
        return owner == null ? 0 : owner.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        ProcessMachineBlockEntity owner = itemOwner();
        return visibleSlot(owner, slot) ? owner.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ProcessMachineBlockEntity owner = itemOwner();
        return level != null && !level.isClientSide() && !role().input() && visibleSlot(owner, slot)
                ? owner.removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, Integer.MAX_VALUE);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ProcessMachineBlockEntity owner = itemOwner();
        if (level != null && !level.isClientSide() && role().input() && visibleSlot(owner, slot)
                && (stack.isEmpty() || owner.canPlaceItem(slot, stack))) {
            owner.setItem(slot, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ProcessMachineBlockEntity owner = itemOwner();
        return role().input() && visibleSlot(owner, slot) && owner.canPlaceItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        ProcessMachineBlockEntity owner = itemOwner();
        if (!permitsFace(side) || owner == null) {
            return new int[0];
        }
        return role().input() ? owner.getSlotsForFace(Direction.UP).clone()
                : owner.getSlotsForFace(Direction.DOWN).clone();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return permitsFace(side) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return permitsFace(side) && !role().input() && visibleSlot(itemOwner(), slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        // Removing a storage-free access point must never clear its controller.
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (kind != null) {
            ProcessMachineBlockEntity owner = itemOwner();
            if (owner != null && level != null && !level.isClientSide()) {
                owner.setChanged();
                owner.onNeighbourChanged();
            }
        }
    }

    @Override
    public boolean canInsert(Direction from, FluidState state) {
        return acceptedAmount(from, state) > 0;
    }

    @Override
    public int acceptedAmount(Direction from, FluidState state) {
        Binding binding = current();
        return role() == WorkshopPortRole.FLUID_INPUT && permitsFace(from) && binding.owner() != null
                ? binding.owner().acceptedAmount(null, state) : 0;
    }

    @Override
    public FluidState insert(Direction from, FluidState state) {
        Binding binding = current();
        return role() == WorkshopPortRole.FLUID_INPUT && permitsFace(from) && binding.owner() != null
                ? binding.owner().insert(null, state) : state == null ? FluidState.EMPTY : state;
    }

    @Override
    public boolean canExtract(Direction from) {
        return role() == WorkshopPortRole.FLUID_OUTPUT && permitsFace(from) && !contents().isEmpty();
    }

    @Override
    public FluidState extract(Direction from, int millibuckets) {
        Binding binding = current();
        if (binding.owner() == null || !canExtract(from)) {
            return FluidState.EMPTY;
        }
        return binding.owner().extract(null, Math.min(Math.max(0, millibuckets), contents().millibuckets()));
    }

    @Override
    public FluidState contents() {
        Binding binding = current();
        if (role().item() || binding.owner() == null) {
            return FluidState.EMPTY;
        }
        return role() == WorkshopPortRole.FLUID_OUTPUT ? binding.owner().workshopProducedFluid()
                : binding.owner().contents();
    }

    @Override
    public NativeFluidStore nativeStore() {
        Binding binding = current();
        return role().item() || binding.owner() == null ? null : binding.owner().nativeStore();
    }

    @Override
    public long nativeAccessRevision() {
        return capabilityRevision();
    }

    @Override
    public int capacity() {
        Binding binding = current();
        return role().item() || binding.owner() == null ? 0 : binding.owner().capacity();
    }
}
