package io.github.ezequiel24123z.grindless.fluid.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortBlockEntity;
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
 * assigned the fluid type's intrinsic temperature, at least ambient, and 0.1 MPa. Tagged
 * fluids are refused because the native state cannot preserve their NBT. Carbon monoxide is a Grindless id with no registered
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
        if (!(blockEntity instanceof FluidEndpoint endpoint)
                || blockEntity instanceof WorkshopPortBlockEntity) {
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

    static final class Handler implements IFluidHandler {

        private final FluidEndpoint endpoint;
        private final Direction side;

        Handler(FluidEndpoint endpoint, Direction side) {
            this.endpoint = endpoint;
            this.side = side;
        }

        private Direction face() {
            return side == null ? Direction.UP : side;
        }

        private boolean available() {
            if (!(endpoint instanceof BlockEntity entity)) {
                return true;
            }
            return !entity.isRemoved() && (entity.getLevel() == null
                    || entity.getLevel().hasChunkAt(entity.getBlockPos())
                    && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 && available() ? toForge(endpoint.contents()) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 && available() ? endpoint.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && available() && endpoint.canInsert(face(), fromForge(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            FluidState incoming = fromForge(resource);
            if (!available() || incoming.isEmpty() || !endpoint.canInsert(face(), incoming)) {
                return 0;
            }
            if (action.simulate()) {
                return endpoint.acceptedAmount(face(), incoming);
            }
            FluidState leftover = endpoint.insert(face(), incoming);
            return incoming.millibuckets() - leftover.millibuckets();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidState want = fromForge(resource);
            if (!available() || want.isEmpty() || !endpoint.canExtract(face())
                    || !endpoint.contents().is(want.id())) {
                return FluidStack.EMPTY;
            }
            return drain(want.millibuckets(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!available() || maxDrain <= 0 || !endpoint.canExtract(face())) {
                return FluidStack.EMPTY;
            }
            FluidState present = endpoint.contents();
            FluidStack offered = toForge(present.withAmount(Math.min(maxDrain, present.millibuckets())));
            if (offered.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.simulate()) {
                return offered;
            }
            return toForge(endpoint.extract(face(), maxDrain));
        }

    }

    static FluidStack toForge(FluidState state) {
        if (state == null || state.isEmpty()) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(state.id());
        Fluid fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, state.millibuckets());
    }

    static FluidState fromForge(FluidStack stack) {
        if (stack == null || stack.isEmpty() || stack.hasTag()) {
            return FluidState.EMPTY;
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (id == null) {
            return FluidState.EMPTY;
        }
        double temperatureC = Math.max(FluidLogic.AMBIENT_C,
                stack.getFluid().getFluidType().getTemperature(stack) - 273.15);
        return FluidState.of(id.toString(), stack.getAmount(), temperatureC, FluidLogic.AMBIENT_MPA);
    }
}
