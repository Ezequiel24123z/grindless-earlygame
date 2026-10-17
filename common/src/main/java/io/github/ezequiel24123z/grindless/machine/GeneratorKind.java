package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Real generator variants sharing one fuel, energy and persistence implementation. */
public enum GeneratorKind {
    THERMAL(FluxTier.F1, BlockCatalogue.Geometry.GENERATOR, "thermal_generator"),
    BIOMASS(FluxTier.F0, BlockCatalogue.Geometry.BIOGEN, "biomass_dynamo");

    private final FluxTier tier;
    private final BlockCatalogue.Geometry geometry;
    private final String id;

    GeneratorKind(FluxTier tier, BlockCatalogue.Geometry geometry, String id) {
        this.tier = tier;
        this.geometry = geometry;
        this.id = id;
    }

    public FluxTier tier() { return tier; }
    public BlockCatalogue.Geometry geometry() { return geometry; }
    public String id() { return id; }

    public BlockEntityType<ThermalGeneratorBlockEntity> type() {
        return this == THERMAL ? ModBlockEntities.THERMAL_GENERATOR.get() : ModBlockEntities.BIOMASS_DYNAMO.get();
    }

    public boolean acceptsGas() { return this == THERMAL; }
}
