package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.registry.ModMenus;
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
    public static final int DATA_SIZE = 6;

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
        switch (kind) {
            case GENERATOR -> addSlot(new Slot(container, 0, 80, 36));
            case PULVERIZER, KILN, WIRE_MILL, CHEMICAL_REACTOR -> {
                addSlot(new Slot(container, 0, 52, 35));
                addSlot(new OutputSlot(container, 1, 116, 35));
            }
            case CHEMICAL_WASHER -> {
                addSlot(new Slot(container, 0, 44, 35));
                addSlot(new OutputSlot(container, 1, 116, 26));
                addSlot(new OutputSlot(container, 2, 116, 44));
            }
            case ARC_FURNACE -> {
                addSlot(new Slot(container, 0, 44, 26));
                addSlot(new Slot(container, 1, 44, 44));
                addSlot(new OutputSlot(container, 2, 116, 26));
                addSlot(new OutputSlot(container, 3, 116, 44));
            }
            case PRESS -> {
                addSlot(new Slot(container, 0, 44, 26));
                addSlot(new Slot(container, 1, 44, 44));
                addSlot(new OutputSlot(container, 2, 116, 35));
            }
            case ASSEMBLER -> {
                addSlot(new Slot(container, 0, 44, 17));
                addSlot(new Slot(container, 1, 44, 35));
                addSlot(new Slot(container, 2, 44, 53));
                addSlot(new OutputSlot(container, 3, 116, 35));
            }
        }
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
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
