package io.github.ezequiel24123z.grindless.energy.forge;

import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Optional;

/**
 * Forge implementation of {@code FluxPlatform}, resolved by Architectury's {@code @ExpectPlatform}.
 *
 * <p>This class also serves NeoForge 1.20.1, which keeps the {@code net.minecraftforge}
 * capability packages unchanged (ADR-0002).
 */
public final class FluxPlatformImpl {

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
}
