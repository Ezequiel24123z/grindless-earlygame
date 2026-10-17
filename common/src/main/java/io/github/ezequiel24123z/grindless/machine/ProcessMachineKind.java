package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.process.Agitation;
import io.github.ezequiel24123z.grindless.process.Atmosphere;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ConditionState;
import io.github.ezequiel24123z.grindless.process.MachineEnvelopes;
import io.github.ezequiel24123z.grindless.process.ProcessField;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Process consumer: family, envelope, slots and the conditions the machine holds
 * so R1 and roast evaluate optimally without a player touching a dial (ADR-0058, ADR-0065).
 */
public enum ProcessMachineKind {

    PULVERIZER(MachineFamily.PULVERIZER, MachineMenuKind.PULVERIZER,
            BlockCatalogue.Geometry.MILL, "pulverizer") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.PULVERIZER.get();
        }
    },

    ARC_FURNACE(MachineFamily.ARC_FURNACE, MachineMenuKind.ARC_FURNACE,
            BlockCatalogue.Geometry.FURNACE, "arc_furnace") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.ARC_FURNACE;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT
                    .withTemperature(ProcessLogic.REDUCE_TEMPERATURE)
                    .withAtmosphere(Atmosphere.REDUCING)
                    .withField(ProcessField.ELECTRIC);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.ARC_FURNACE.get();
        }
    },

    PRESS(MachineFamily.PRESS, MachineMenuKind.PRESS,
            BlockCatalogue.Geometry.PRESS, "press") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.PRESS.get();
        }
    },

    ASSEMBLER(MachineFamily.ASSEMBLER, MachineMenuKind.ASSEMBLER,
            BlockCatalogue.Geometry.ASSEMBLER, "assembler") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.ASSEMBLER.get();
        }
    },

    KILN(MachineFamily.KILN, MachineMenuKind.KILN,
            BlockCatalogue.Geometry.KILN, "kiln") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.KILN;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT
                    .withTemperature(ProcessLogic.ROAST_TEMPERATURE)
                    .withAtmosphere(Atmosphere.OXIDISING);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.KILN.get();
        }
    },

    WIRE_MILL(MachineFamily.WIRE_MILL, MachineMenuKind.WIRE_MILL,
            BlockCatalogue.Geometry.WIRE_MILL, "wire_mill") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.WIRE_MILL.get();
        }
    },

    CHEMICAL_REACTOR(MachineFamily.CHEMICAL_REACTOR, MachineMenuKind.CHEMICAL_REACTOR,
            BlockCatalogue.Geometry.REACTOR, "chemical_reactor") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.CHEMICAL_REACTOR;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT
                    .withTemperature(ProcessLogic.CONTACT_TEMPERATURE)
                    .withAtmosphere(Atmosphere.OXIDISING)
                    .withAgitation(Agitation.STIRRED);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.CHEMICAL_REACTOR.get();
        }
    },

    CHEMICAL_WASHER(MachineFamily.CHEMICAL_WASHER, MachineMenuKind.CHEMICAL_WASHER,
            BlockCatalogue.Geometry.WASHER, "chemical_washer") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.CHEMICAL_WASHER;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT.withAgitation(Agitation.STIRRED);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.CHEMICAL_WASHER.get();
        }
    },

    ELECTROLYSIS_CELL(MachineFamily.ELECTROLYSIS_CELL, MachineMenuKind.ELECTROLYSIS_CELL,
            BlockCatalogue.Geometry.CELL, "electrolysis_cell") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.ELECTROLYSIS_CELL;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT.withField(ProcessField.ELECTRIC);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.ELECTROLYSIS_CELL.get();
        }
    },

    ATMOSPHERIC_INTAKE(MachineFamily.ATMOSPHERIC_INTAKE, MachineMenuKind.ATMOSPHERIC_INTAKE,
            BlockCatalogue.Geometry.INTAKE, "atmospheric_intake") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.ATMOSPHERIC_INTAKE;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.ATMOSPHERIC_INTAKE.get();
        }
    },

    INDUCTION_FURNACE(MachineFamily.INDUCTION_FURNACE, MachineMenuKind.INDUCTION_FURNACE,
            BlockCatalogue.Geometry.INDUCTION, "induction_furnace") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.INDUCTION_FURNACE;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT
                    .withTemperature(ProcessLogic.MELT_TEMPERATURE)
                    .withAtmosphere(Atmosphere.INERT);
        }

        @Override
        public boolean hotFluid() {
            return true;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.INDUCTION_FURNACE.get();
        }
    },

    CASTER(MachineFamily.CASTER, MachineMenuKind.CASTER,
            BlockCatalogue.Geometry.CASTER, "caster") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public boolean hotFluid() {
            return true;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.CASTER.get();
        }
    },

    FLOTATION(MachineFamily.FLOTATION, MachineMenuKind.FLOTATION,
            BlockCatalogue.Geometry.FLOTATION, "flotation_cell") {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.FLOTATION;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT.withAgitation(Agitation.STIRRED);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.FLOTATION_CELL.get();
        }
    },

    BOILER(MachineFamily.BOILER, MachineMenuKind.BOILER,
            BlockCatalogue.Geometry.BOILER, "boiler") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public double fluidMaxC() {
            return ProcessLogic.STEAM_MAX_C;
        }

        @Override
        public double fluidMaxP() {
            return ProcessLogic.STEAM_MAX_MPA;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.BOILER.get();
        }
    },

    CONDENSER(MachineFamily.CONDENSER, MachineMenuKind.CONDENSER,
            BlockCatalogue.Geometry.CONDENSER, "condenser") {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public double fluidMaxC() {
            return ProcessLogic.STEAM_MAX_C;
        }

        @Override
        public double fluidMaxP() {
            return ProcessLogic.STEAM_MAX_MPA;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.CONDENSER.get();
        }
    },

    QUERN(MachineFamily.QUERN, MachineMenuKind.QUERN,
            BlockCatalogue.Geometry.QUERN, "quern", FluxTier.F0) {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.QUERN.get();
        }
    },

    CLAY_MIXER(MachineFamily.CLAY_MIXER, MachineMenuKind.CLAY_MIXER,
            BlockCatalogue.Geometry.MIXER, "clay_mixer", FluxTier.F0) {
        @Override
        public ConditionEnvelope envelope() {
            return MachineEnvelopes.CHEMICAL_WASHER;
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT.withAgitation(Agitation.STIRRED);
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.CLAY_MIXER.get();
        }
    },

    FIRING_CHAMBER(MachineFamily.FIRING_CHAMBER, MachineMenuKind.FIRING_CHAMBER,
            BlockCatalogue.Geometry.FIREBOX, "firing_chamber", FluxTier.F0) {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().temperature(20, 1450)
                    .atmospheres(Atmosphere.OXIDISING, Atmosphere.REDUCING).build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT.withTemperature(BootstrapProcesses.FIRING_C)
                    .withAtmosphere(Atmosphere.OXIDISING);
        }

        @Override
        public WorkshopStructure structure() {
            return WorkshopStructure.FIRING_CHAMBER;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.FIRING_CHAMBER.get();
        }
    },

    SETTLING_TROUGH(MachineFamily.SETTLING_TROUGH, MachineMenuKind.SETTLING_TROUGH,
            BlockCatalogue.Geometry.SETTLER, "settling_trough", FluxTier.F0) {
        @Override
        public ConditionEnvelope envelope() {
            return ConditionEnvelope.builder().build();
        }

        @Override
        public ConditionState heldConditions() {
            return ConditionState.AMBIENT;
        }

        @Override
        public WorkshopStructure structure() {
            return WorkshopStructure.SETTLING_TROUGH;
        }

        @Override
        public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.SETTLING_TROUGH.get();
        }
    },

    GEOLOGICAL_COLLECTOR(MachineFamily.GEOLOGICAL_COLLECTOR, MachineMenuKind.GEOLOGICAL_COLLECTOR,
            BlockCatalogue.Geometry.COLLECTOR, "geological_collector", FluxTier.F0) {
        @Override public ConditionEnvelope envelope() { return ConditionEnvelope.builder().build(); }
        @Override public ConditionState heldConditions() { return ConditionState.AMBIENT; }
        @Override public SourceSite site() { return SourceSite.GEOLOGICAL; }
        @Override public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.GEOLOGICAL_COLLECTOR.get();
        }
    },
    BIOCULTIVATOR(MachineFamily.BIOCULTIVATOR, MachineMenuKind.BIOCULTIVATOR,
            BlockCatalogue.Geometry.CULTURE, "biocultivator", FluxTier.F0) {
        @Override public ConditionEnvelope envelope() { return ConditionEnvelope.builder().build(); }
        @Override public ConditionState heldConditions() { return ConditionState.AMBIENT; }
        @Override public SourceSite site() { return SourceSite.SUNLIT_CULTURE; }
        @Override public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.BIOCULTIVATOR.get();
        }
    },
    ASSEMBLY_BENCH(MachineFamily.ASSEMBLY_BENCH, MachineMenuKind.ASSEMBLY_BENCH,
            BlockCatalogue.Geometry.BENCH, "assembly_bench", FluxTier.F0) {
        @Override public ConditionEnvelope envelope() { return ConditionEnvelope.builder().build(); }
        @Override public ConditionState heldConditions() { return ConditionState.AMBIENT; }
        @Override public BlockEntityType<ProcessMachineBlockEntity> type() {
            return ModBlockEntities.ASSEMBLY_BENCH.get();
        }
    };

    private final MachineFamily family;
    private final MachineMenuKind menuKind;
    private final BlockCatalogue.Geometry geometry;
    private final String translation;
    private final FluxTier tier;

    ProcessMachineKind(MachineFamily family, MachineMenuKind menuKind,
                       BlockCatalogue.Geometry geometry, String translation) {
        this(family, menuKind, geometry, translation, FluxTier.F1);
    }

    ProcessMachineKind(MachineFamily family, MachineMenuKind menuKind,
                       BlockCatalogue.Geometry geometry, String translation, FluxTier tier) {
        this.family = family;
        this.menuKind = menuKind;
        this.geometry = geometry;
        this.translation = translation;
        this.tier = tier;
    }

    public MachineFamily family() {
        return family;
    }

    public MachineMenuKind menuKind() {
        return menuKind;
    }

    public BlockCatalogue.Geometry geometry() {
        return geometry;
    }

    public String translationKey() {
        return "block.grindless." + translation;
    }

    public abstract ConditionEnvelope envelope();

    public abstract ConditionState heldConditions();

    public abstract BlockEntityType<ProcessMachineBlockEntity> type();

    /** Rated input and buffer sizing, independent of the selected recipe's instantaneous draw. */
    public FluxTier tier() {
        return tier;
    }

    /** A fixed workshop structure only when the process requires one. */
    public WorkshopStructure structure() {
        return WorkshopStructure.NONE;
    }

    public SourceSite site() {
        return SourceSite.NONE;
    }

    /** The primitive chamber regulates its declared fuel-backed band rather than faking one fixed temperature. */
    public ConditionState heldConditions(ProcessRecipe recipe) {
        if (this != FIRING_CHAMBER || recipe == null) {
            return heldConditions();
        }
        return heldConditions().withTemperature(recipe.temperatureC())
                .withAtmosphere(Atmosphere.valueOf(recipe.atmosphere()));
    }

    /** Whether the fluid buffer may hold melt. The tank and the clay pipe still refuse it. */
    public boolean hotFluid() {
        return false;
    }

    /** Hottest fluid this buffer will accept. Melt uses {@link ProcessLogic#MOLTEN_MAX_C}. */
    public double fluidMaxC() {
        return hotFluid() ? ProcessLogic.MOLTEN_MAX_C : FluidLogic.AMBIENT_MAX_C;
    }

    /** Highest pressure this buffer will accept. Steam uses {@link ProcessLogic#STEAM_MAX_MPA}. */
    public double fluidMaxP() {
        return FluidLogic.AMBIENT_MPA;
    }
}
