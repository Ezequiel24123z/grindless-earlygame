package io.github.ezequiel24123z.grindless.fluid.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.Map;

/**
 * Exposes a {@link FluidEndpoint} as Forge's fluid capability, so pipes from other mods can
 * push water into a tank the same way a Clay Conduit does.
 *
 * <p>Forge {@code FluidStack} has no temperature or pressure. Export drops both; import is
 * treated as ambient (20 °C, 0.1 MPa). Carbon monoxide is a Grindless id with no registered
 * {@code Fluid}, so it stays on {@code FluidEndpoint} and does not appear to Forge pipes
 * (ADR-0062).
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class MachineFluidCapability {

    private static final ResourceLocation ID = Grindless.id("fluids");

    private MachineFluidCapability() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        if (!(blockEntity instanceof FluidEndpoint endpoint)) {
            return;
        }
        Provider provider = new Provider(endpoint);
        event.addCapability(ID, provider);
        event.addListener(provider::invalidate);
    }

    private static final class Provider implements ICapabilityProvider {

        private final Map<Direction, LazyOptional<IFluidHandler>> sides = new EnumMap<>(Direction.class);
        private final LazyOptional<IFluidHandler> unsided;

        private Provider(FluidEndpoint endpoint) {
            for (Direction direction : Direction.values()) {
                sides.put(direction, LazyOptional.of(() -> new Handler(endpoint, direction)));
            }
            unsided = LazyOptional.of(() -> new Handler(endpoint, null));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability != ForgeCapabilities.FLUID_HANDLER) {
                return LazyOptional.empty();
            }
            return (side == null ? unsided : sides.get(side)).cast();
        }

        private void invalidate() {
            unsided.invalidate();
            sides.values().forEach(LazyOptional::invalidate);
        }
    }

    private static final class Handler implements IFluidHandler {

        private final FluidEndpoint endpoint;
        private final Direction side;

        private Handler(FluidEndpoint endpoint, Direction side) {
            this.endpoint = endpoint;
            this.side = side;
        }

        private Direction face() {
            return side == null ? Direction.UP : side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return toForge(endpoint.contents());
        }

        @Override
        public int getTankCapacity(int tank) {
            return endpoint.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return endpoint.canInsert(face(), fromForge(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            FluidState incoming = fromForge(resource);
            if (incoming.isEmpty() || !endpoint.canInsert(face(), incoming)) {
                return 0;
            }
            if (action.simulate()) {
                FluidState leftover = simulateInsert(incoming);
                return incoming.millibuckets() - leftover.millibuckets();
            }
            FluidState leftover = endpoint.insert(face(), incoming);
            return incoming.millibuckets() - leftover.millibuckets();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidState want = fromForge(resource);
            if (want.isEmpty() || !endpoint.canExtract(face())
                    || !endpoint.contents().is(want.id())) {
                return FluidStack.EMPTY;
            }
            return drain(want.millibuckets(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !endpoint.canExtract(face())) {
                return FluidStack.EMPTY;
            }
            if (action.simulate()) {
                return toForge(endpoint.contents().withAmount(
                        Math.min(maxDrain, endpoint.contents().millibuckets())));
            }
            return toForge(endpoint.extract(face(), maxDrain));
        }

        /**
         * Fill simulation must not mutate the buffer. Insert then extract the taken amount
         * would be a round trip; instead, compare against capacity of the same substance.
         */
        private FluidState simulateInsert(FluidState incoming) {
            FluidState current = endpoint.contents();
            if (current.isEmpty()) {
                int take = Math.min(incoming.millibuckets(), endpoint.capacity());
                return incoming.withAmount(incoming.millibuckets() - take);
            }
            if (!current.id().equals(incoming.id())) {
                return incoming;
            }
            int space = Math.max(0, endpoint.capacity() - current.millibuckets());
            return incoming.withAmount(incoming.millibuckets() - Math.min(incoming.millibuckets(), space));
        }
    }

    static FluidStack toForge(FluidState state) {
        if (state == null || state.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(state.id()));
        if (fluid == null || fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, state.millibuckets());
    }

    static FluidState fromForge(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidState.EMPTY;
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (id == null) {
            return FluidState.EMPTY;
        }
        return FluidState.of(id.toString(), stack.getAmount());
    }
}
