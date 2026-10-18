package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.registry.ModMenus;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * Shared machine menu: energy, slots, named fault (ADR-0058).
 *
 * <p>Extra packet data is the block position so the client can bind the same host the server
 * opened. Slot positions depend on {@link MachineMenuKind}; the screen does not.
 */
public final class ProcessMachineMenu extends AbstractContainerMenu {

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_CYCLE = 3;
    public static final int DATA_STATUS = 4;
    public static final int DATA_KIND = 5;
    public static final int DATA_STRUCTURE = 6;
    public static final int DATA_FLUID = 7;
    public static final int DATA_FLUID_OUTPUT = 8;
    public static final int DATA_SITE = 9;
    public static final int DATA_SIZE = 10;

    private final MachineMenuHost host;
    private final ContainerData data;
    private final MachineMenuKind kind;

    public ProcessMachineMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, hostAt(inventory, buf));
    }

    public ProcessMachineMenu(int id, Inventory inventory, MachineMenuHost host) {
        super(ModMenus.PROCESS_MACHINE.get(), id);
        this.host = host;
        this.kind = host.menuKind();
        this.data = host.menuData();
        addMachineSlots(host.menuContainer());
        addPlayerSlots(inventory);
        addDataSlots(data);
    }

    private static MachineMenuHost hostAt(Inventory inventory, FriendlyByteBuf buf) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(buf.readBlockPos());
        if (blockEntity instanceof MachineMenuHost host) {
            return host;
        }
        throw new IllegalStateException("machine menu opened without a host");
    }

    private void addMachineSlots(Container container) {
        for (MachineMenuLayout.ItemSlot slot : MachineMenuLayout.itemSlots(kind)) {
            addSlot(slot.output()
                    ? new OutputSlot(container, slot.index(), slot.x(), slot.y())
                    : new Slot(container, slot.index(), slot.x(), slot.y()));
        }
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18,
                        MachineMenuLayout.INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, MachineMenuLayout.HOTBAR_Y));
        }
    }

    public MachineMenuKind kind() {
        return MachineMenuKind.values()[Math.max(0, Math.min(data.get(DATA_KIND),
                MachineMenuKind.values().length - 1))];
    }

    public int energy() {
        return data.get(DATA_ENERGY);
    }

    public int capacity() {
        return Math.max(1, data.get(DATA_CAPACITY));
    }

    public int progress() {
        return data.get(DATA_PROGRESS);
    }

    public int cycle() {
        return Math.max(1, data.get(DATA_CYCLE));
    }

    public int statusOrdinal() {
        return data.get(DATA_STATUS);
    }

    /** The server's structure diagnosis; the client must not infer it from its own chunk view. */
    public WorkshopStructure.Fault structureFault() {
        int ordinal = data.get(DATA_STRUCTURE);
        WorkshopStructure.Fault[] faults = WorkshopStructure.Fault.values();
        return ordinal >= 0 && ordinal < faults.length ? faults[ordinal] : WorkshopStructure.Fault.UNLOADED;
    }

    /** Primary fluid volume synchronized through the ordinary menu data protocol. */
    public int fluidAmount() {
        return Math.max(0, data.get(DATA_FLUID));
    }

    /** Stored-fluid output quota, before active-recipe reservations are applied. */
    public int fluidOutputAmount() {
        return Math.max(0, data.get(DATA_FLUID_OUTPUT));
    }

    /**
     * Bootstrap's four water-only families use the existing synchronized amount/quota and
     * the same capacity constant as their native buffer. This does not infer other fluid IDs.
     */
    public Optional<MachineFluidView> waterView() {
        return MachineMenuLayout.waterTank(kind()).map(tank ->
                new MachineFluidView(FluidLogic.WATER, fluidAmount(), FluidLogic.MACHINE_CAPACITY,
                        fluidOutputAmount()));
    }

    public io.github.ezequiel24123z.grindless.source.SourceSite.Fault siteFault() {
        var faults = io.github.ezequiel24123z.grindless.source.SourceSite.Fault.values();
        int ordinal = data.get(DATA_SITE);
        return ordinal >= 0 && ordinal < faults.length ? faults[ordinal] : faults[1];
    }

    public MachineMenuHost host() {
        return host;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return original;
        }
        ItemStack stack = slot.getItem();
        original = stack.copy();
        int machineSlots = kind.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, kind.inputs(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return host.stillValid(player);
    }

    /** Output slots refuse shift-click inserts and player placement. */
    private static final class OutputSlot extends Slot {

        private OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** Client-side fallback when the block entity is gone; never used on the server. */
    public static ContainerData emptyData(MachineMenuKind kind) {
        SimpleContainerData data = new SimpleContainerData(DATA_SIZE);
        data.set(DATA_KIND, kind.ordinal());
        data.set(DATA_CYCLE, 1);
        data.set(DATA_CAPACITY, 1);
        return data;
    }
}
