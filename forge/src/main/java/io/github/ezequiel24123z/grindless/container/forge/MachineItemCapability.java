package io.github.ezequiel24123z.grindless.container.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.belt.BeltBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import java.util.EnumMap;
import java.util.Map;

/**
 * Exposes a machine or belt that is a vanilla {@code Container} as Forge's item capability, so
 * hoppers and pipes from other mods can pull from it the same way a hopper does.
 *
 * <p>Forge hoppers talk to {@code ITEM_HANDLER}, not to {@code Container} directly. A plain
 * {@code InvWrapper} would let them extract from a {@code WorldlyContainer} that forbids it
 * (the Research Terminal spends its Data Core; hoppers must not steal it). Sided wrappers honour
 * {@code canTakeItemThroughFace} / {@code canPlaceItemThroughFace}.
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class MachineItemCapability {

    private static final ResourceLocation ID = Grindless.id("items");

    private MachineItemCapability() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        if (!(blockEntity instanceof MachineBlockEntity)
                && !(blockEntity instanceof BeltBlockEntity)) {
            return;
        }
        if (blockEntity instanceof WorldlyContainer worldly) {
            SidedProvider provider = new SidedProvider(worldly);
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        } else if (blockEntity instanceof Container container) {
            Provider provider = new Provider(container);
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static final class Provider implements ICapabilityProvider {

        private final LazyOptional<IItemHandler> items;

        private Provider(Container container) {
            this.items = LazyOptional.of(() -> new InvWrapper(container));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == ForgeCapabilities.ITEM_HANDLER
                    ? items.cast()
                    : LazyOptional.empty();
        }

        private void invalidate() {
            items.invalidate();
        }
    }

    private static final class SidedProvider implements ICapabilityProvider {

        private final Map<Direction, LazyOptional<IItemHandler>> sides = new EnumMap<>(Direction.class);
        private final LazyOptional<IItemHandler> unsided;

        private SidedProvider(WorldlyContainer container) {
            for (Direction direction : Direction.values()) {
                sides.put(direction, LazyOptional.of(() -> new SidedInvWrapper(container, direction)));
            }
            unsided = LazyOptional.of(() -> new SidedInvWrapper(container, Direction.UP));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability != ForgeCapabilities.ITEM_HANDLER) {
                return LazyOptional.empty();
            }
            return (side == null ? unsided : sides.get(side)).cast();
        }

        private void invalidate() {
            unsided.invalidate();
            sides.values().forEach(LazyOptional::invalidate);
        }
    }
}
