package io.github.ezequiel24123z.grindless.energy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The seam between Flux and whatever energy system the loader provides.
 *
 * <p>Architectury has no unified energy API, so the bridge is hand-written per platform:
 * capabilities on Forge, Team Reborn Energy on Fabric (ADR-0006). Everything above this class
 * speaks {@link FluxStorage} and never knows which loader it is running on.
 *
 * <p>The implementations live in {@code energy.forge.FluxPlatformImpl} and
 * {@code energy.fabric.FluxPlatformImpl}; the names and packages are fixed by Architectury's
 * {@code @ExpectPlatform} contract and must not be changed independently of this class.
 */
public final class FluxPlatform {

    private FluxPlatform() {
    }

    /**
     * Looks up the energy buffer exposed by the block at {@code pos}, as a {@link FluxStorage}.
     *
     * <p>This is how Grindless pushes power into, or pulls it out of, machines from other mods.
     * Because FU and FE are 1:1, the returned view is exact and never rounds.
     *
     * @param side the face being connected to, or {@code null} for a side-agnostic lookup
     * @return a view of the neighbour's buffer, or {@code null} if it exposes none
     */
    @ExpectPlatform
    public static FluxStorage findEnergy(Level level, BlockPos pos, Direction side) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }

    /**
     * Registers {@code onInvalidated} to run when the energy buffer at {@code pos} stops being
     * valid — because the block was broken, replaced, or revoked its capability.
     *
     * <p>This is what makes caching a neighbour safe. Without it, a cached reference survives the
     * neighbour's removal and the machine goes on pushing power into a block that is no longer
     * there, which presents as "my machine stopped working until I broke and replaced it".
     *
     * <p>The listener fires at most once per registration. It must not assume the block entity
     * still exists.
     *
     * <p>The returned handle removes the registration, and it matters: a neighbour's capability
     * outlives any one holder, so a holder that re-resolves without unregistering leaves one more
     * listener behind each time, each of them keeping the holder reachable. Running the handle
     * after the listener has fired, or when there was nothing to listen to, is harmless.
     *
     * @return a handle that unregisters the listener; never {@code null}
     */
    @ExpectPlatform
    public static Runnable onInvalidated(Level level, BlockPos pos, Direction side,
                                         Runnable onInvalidated) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }
}
