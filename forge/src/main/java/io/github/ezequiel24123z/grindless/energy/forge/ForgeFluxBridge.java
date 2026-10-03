package io.github.ezequiel24123z.grindless.energy.forge;

import io.github.ezequiel24123z.grindless.energy.FluxConversion;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Adapters between {@link FluxStorage} and Forge Energy, in both directions.
 *
 * <p>FU and FE are the same size (ADR-0006), so the only real work here is the width difference:
 * FU is {@code long} and FE is {@code int}. Every crossing saturates at {@link Integer#MAX_VALUE}
 * rather than overflowing, and the amount the other side reports as moved is always believed over
 * the amount requested. See ADR-0037.
 */
public final class ForgeFluxBridge {

    private ForgeFluxBridge() {
    }

    /** Views a Forge energy handler as a Flux buffer. */
    public static FluxStorage asFlux(IEnergyStorage fe) {
        return new ForgeBackedFlux(fe);
    }

    /** Exposes a Flux buffer to Forge, for {@code getCapability(ForgeCapabilities.ENERGY)}. */
    public static IEnergyStorage asForge(FluxStorage flux) {
        return new FluxBackedForge(flux);
    }

    private record ForgeBackedFlux(IEnergyStorage fe) implements FluxStorage {

        @Override
        public long receive(long maxReceive, boolean simulate) {
            return FluxConversion.fromFe(fe.receiveEnergy(FluxConversion.toFe(maxReceive), simulate));
        }

        @Override
        public long extract(long maxExtract, boolean simulate) {
            return FluxConversion.fromFe(fe.extractEnergy(FluxConversion.toFe(maxExtract), simulate));
        }

        @Override
        public long getStored() {
            return FluxConversion.fromFe(fe.getEnergyStored());
        }

        @Override
        public long getCapacity() {
            return FluxConversion.fromFe(fe.getMaxEnergyStored());
        }

        @Override
        public boolean canReceive() {
            return fe.canReceive();
        }

        @Override
        public boolean canExtract() {
            return fe.canExtract();
        }
    }

    private record FluxBackedForge(FluxStorage flux) implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return FluxConversion.toFe(flux.receive(FluxConversion.fromFe(maxReceive), simulate));
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return FluxConversion.toFe(flux.extract(FluxConversion.fromFe(maxExtract), simulate));
        }

        @Override
        public int getEnergyStored() {
            return FluxConversion.toFe(flux.getStored());
        }

        @Override
        public int getMaxEnergyStored() {
            return FluxConversion.toFe(flux.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return flux.canExtract();
        }

        @Override
        public boolean canReceive() {
            return flux.canReceive();
        }
    }
}
