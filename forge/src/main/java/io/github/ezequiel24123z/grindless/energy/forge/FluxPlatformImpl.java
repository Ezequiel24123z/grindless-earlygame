package io.github.ezequiel24123z.grindless.energy.forge;

import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Optional;

/**
 * Forge implementation of {@code FluxPlatform}, resolved by Architectury's {@code @ExpectPlatform}.
 *
 * <p>This class also serves NeoForge 1.20.1, which keeps the {@code net.minecraftforge}
 * capability packages unchanged (ADR-0002).
 */
public final class FluxPlatformImpl {

    private static final Runnable NO_OP = () -> { };

    private FluxPlatformImpl() {
    }

    /**
     * Resolves the neighbour's {@code ForgeCapabilities.ENERGY} capability.
     *
     * <p>The capability is resolved eagerly rather than held as a {@code LazyOptional}, because the
     * caller may keep the result across ticks and a stale lazy handle would survive the block being
     * removed. Callers look the neighbour up again when they need it.
     */
    public static FluxStorage findEnergy(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        Optional<IEnergyStorage> energy = blockEntity.getCapability(ForgeCapabilities.ENERGY, side).resolve();
        return energy.map(ForgeFluxBridge::asFlux).orElse(null);
    }

    /**
     * Registers an invalidation listener on the neighbour's capability.
     *
     * <p>Forge hands a capability out as a {@code LazyOptional} and calls
     * {@code LazyOptional.invalidate()} when it stops being valid — on block removal, on
     * replacement, or when the owner revokes it. {@code addListener} is how a holder finds out,
     * and it is the only safe way to cache a neighbour reference across ticks.
     *
     * <p>Silently does nothing when there is no capability to listen to. That is correct rather
     * than a failure: there is nothing to go stale, and the caller re-resolves on the next
     * neighbour-changed event anyway.
     *
     * <p>The returned handle removes the listener. {@code LazyOptional} keeps its listeners in a
     * set and each registration here is a new lambda, so without removal every re-resolve adds one
     * more to a capability that lives as long as the neighbour does.
     */
    public static Runnable onInvalidated(Level level, BlockPos pos, Direction side,
                                         Runnable onInvalidated) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return NO_OP;
        }
        LazyOptional<IEnergyStorage> capability =
                blockEntity.getCapability(ForgeCapabilities.ENERGY, side);
        if (!capability.isPresent()) {
            return NO_OP;
        }
        NonNullConsumer<LazyOptional<IEnergyStorage>> listener = ignored -> onInvalidated.run();
        capability.addListener(listener);
        return () -> capability.removeListener(listener);
    }
}
