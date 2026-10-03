package io.github.ezequiel24123z.grindless.energy.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Exposes every Grindless machine's Flux buffer to Forge as an {@code IEnergyStorage}.
 *
 * <h2>Why this is an event and not an override</h2>
 *
 * <p>The obvious implementation is to override {@code getCapability} on the block entity. That
 * cannot be done here: machines live in {@code common}, which may not import Forge classes, and
 * the compiler has already caught one such leak in this package — {@code onLoad} looked like
 * vanilla and was not.
 *
 * <p>{@code AttachCapabilitiesEvent} solves it cleanly. Forge fires it for every block entity as
 * it is created, so a listener in {@code forge} can attach a capability to a {@code common} class
 * without that class knowing Forge exists. Nothing is moved and nothing is duplicated.
 *
 * <h2>Invalidation, which is the part that matters</h2>
 *
 * <p>A capability handed out and never invalidated is the bug ADR-0044 is about: a neighbour
 * caches the reference, the block is broken, and the cache goes on pointing at a machine that is
 * no longer there.
 *
 * <p>The chain that prevents it was verified against Forge's source rather than assumed:
 *
 * <ol>
 *   <li>{@code BlockEntity.setRemoved()} calls {@code invalidateCaps()};</li>
 *   <li>{@code CapabilityProvider.invalidateCaps()} marks itself invalid and calls
 *       {@code CapabilityDispatcher.invalidate()};</li>
 *   <li>that runs every {@code Runnable} registered through
 *       {@link AttachCapabilitiesEvent#addListener}, which is the one below;</li>
 *   <li>which invalidates our {@link LazyOptional}, firing the listener
 *       {@code NeighbourCache} registered on it, which clears the cache.</li>
 * </ol>
 *
 * <p>Registering the capability without that listener would compile, work in testing, and leak
 * stale references in play — so the {@code addListener} call is the load-bearing line here, not
 * the {@code addCapability} one.
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class MachineEnergyCapability {

    private static final ResourceLocation ID = Grindless.id("energy");

    private MachineEnergyCapability() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof MachineBlockEntity machine) {
            Provider provider = new Provider(machine);
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    /**
     * Hands out one {@link IEnergyStorage} view of the machine's buffer.
     *
     * <p>The view is resolved once and reused, which is safe only because a machine's buffer is
     * final and never replaced — swapping it would leave this pointing at the discarded object.
     *
     * <p>The capability is offered on every face. Per-face energy gating is deliberately not
     * implemented yet: the container contract can express it, but honouring it here needs the
     * capability re-invalidated whenever a face is reconfigured, and there is no screen to
     * reconfigure one from. A machine's own push logic already respects its side configuration.
     */
    private static final class Provider implements ICapabilityProvider {

        private final LazyOptional<IEnergyStorage> energy;

        private Provider(MachineBlockEntity machine) {
            this.energy = LazyOptional.of(() -> ForgeFluxBridge.asForge(machine.energy()));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == ForgeCapabilities.ENERGY
                    ? energy.cast()
                    : LazyOptional.empty();
        }

        private void invalidate() {
            energy.invalidate();
        }
    }
}
