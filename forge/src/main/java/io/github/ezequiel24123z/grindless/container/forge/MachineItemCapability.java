package io.github.ezequiel24123z.grindless.container.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
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

/**
 * Exposes a machine that is a vanilla {@code Container} as Forge's item capability, so pipes from
 * other mods can pull from it the same way a hopper does.
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class MachineItemCapability {

    private static final ResourceLocation ID = Grindless.id("items");

    private MachineItemCapability() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        if (blockEntity instanceof MachineBlockEntity && blockEntity instanceof Container container) {
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
}
