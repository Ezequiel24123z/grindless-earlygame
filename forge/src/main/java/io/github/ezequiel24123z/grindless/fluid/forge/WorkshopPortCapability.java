package io.github.ezequiel24123z.grindless.fluid.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortBlockEntity;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortRole;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes only a workshop port's current role and approved faces (ADR-0045).
 *
 * <p>Handlers retain the port, never a controller or its storage. Every access revalidates the
 * face and captured ownership revision; lifecycle invalidation also retires resolved handlers.
 * Reformed ports obtain fresh side optionals; null/unsided requests are never exposed.
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class WorkshopPortCapability {

    private static final ResourceLocation ID = Grindless.id("workshop_port");

    private WorkshopPortCapability() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof WorkshopPortBlockEntity port)) {
            return;
        }
        Provider provider = new Provider(port);
        event.addCapability(ID, provider);
        event.addListener(provider::invalidate);
    }

    private static final class Provider implements ICapabilityProvider {

        private final WorkshopPortBlockEntity port;
        private final Map<Direction, LazyOptional<?>> sides = new EnumMap<>(Direction.class);
        private WorkshopPortRole exposedRole;
        private boolean listening;
        private long generation;

        private Provider(WorkshopPortBlockEntity port) {
            this.port = port;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (side == null) {
                return LazyOptional.empty();
            }
            WorkshopPortRole role = port.role();
            Capability<?> offered = role.item()
                    ? ForgeCapabilities.ITEM_HANDLER : ForgeCapabilities.FLUID_HANDLER;
            if (capability != offered) {
                return LazyOptional.empty();
            }
            if (!listening) {
                // Attachment runs inside BlockEntity's constructor, before port fields initialize.
                listening = true;
                port.addCapabilityInvalidator(this::invalidate);
            }
            if (!port.permitsFace(side)) {
                return LazyOptional.empty();
            }
            if (exposedRole != role) {
                exposedRole = role;
                invalidate();
            }
            // Both queries can invalidate/clear sides; capture before touching the map.
            long revision = port.capabilityRevision();
            LazyOptional<?> result = sides.get(side);
            if (result == null) {
                Access access = new Access(this, role, side, revision);
                result = role.item()
                        ? LazyOptional.of(() -> new ItemHandler(access))
                        : LazyOptional.of(() -> new FluidHandler(access));
                sides.put(side, result);
            }
            return result.cast();
        }

        private void invalidate() {
            generation++;
            List<LazyOptional<?>> previous = List.copyOf(sides.values());
            sides.clear();
            // Listeners may immediately query and create the next generation.
            previous.forEach(LazyOptional::invalidate);
        }
    }

    private static final class Access {

        private final Provider provider;
        private final WorkshopPortRole role;
        private final Direction side;
        private final long revision;
        private final long generation;

        private Access(Provider provider, WorkshopPortRole role, Direction side, long revision) {
            this.provider = provider;
            this.role = role;
            this.side = side;
            this.revision = revision;
            this.generation = provider.generation;
        }

        private boolean available() {
            // Revalidation must precede revision/generation checks; it can retire this handler.
            return provider.port.permitsFace(side) && provider.port.capabilityRevision() == revision
                    && provider.port.role() == role
                    && provider.generation == generation;
        }
    }

    /** Composition deliberately does not expose SidedInvWrapper's modifiable-handler API. */
    private static final class ItemHandler implements IItemHandler {

        private final Access access;
        private final SidedInvWrapper delegate;

        private ItemHandler(Access access) {
            this.access = access;
            this.delegate = new SidedInvWrapper(access.provider.port, access.side);
        }

        private boolean validSlot(int slot) {
            return access.available() && slot >= 0 && slot < delegate.getSlots();
        }

        @Override
        public int getSlots() {
            return access.available() ? delegate.getSlots() : 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return validSlot(slot) ? delegate.getStackInSlot(slot).copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return validSlot(slot) && access.role.input()
                    ? delegate.insertItem(slot, stack.copy(), simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return validSlot(slot) && !access.role.input() && amount > 0
                    ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return validSlot(slot) ? delegate.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return validSlot(slot) && access.role.input() && delegate.isItemValid(slot, stack);
        }
    }

    private static final class FluidHandler implements IFluidHandler {

        private final Access access;
        private final MachineFluidCapability.Handler delegate;

        private FluidHandler(Access access) {
            this.access = access;
            this.delegate = new MachineFluidCapability.Handler(access.provider.port, access.side);
        }

        @Override
        public int getTanks() {
            return access.available() ? delegate.getTanks() : 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return access.available() && tank == 0
                    ? delegate.getFluidInTank(tank) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return access.available() && tank == 0 ? delegate.getTankCapacity(tank) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return access.available() && tank == 0 && access.role.input()
                    && delegate.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return access.available() && access.role.input() && action != null
                    ? delegate.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return access.available() && !access.role.input() && action != null
                    ? delegate.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return access.available() && !access.role.input() && maxDrain > 0 && action != null
                    ? delegate.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }
}
