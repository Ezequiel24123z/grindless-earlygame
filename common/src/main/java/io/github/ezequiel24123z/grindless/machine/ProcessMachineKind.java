package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.process.Atmosphere;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ConditionState;
import io.github.ezequiel24123z.grindless.process.MachineEnvelopes;
import io.github.ezequiel24123z.grindless.process.ProcessField;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
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
    };

    private final MachineFamily family;
    private final MachineMenuKind menuKind;
    private final BlockCatalogue.Geometry geometry;
    private final String translation;

    ProcessMachineKind(MachineFamily family, MachineMenuKind menuKind,
                       BlockCatalogue.Geometry geometry, String translation) {
        this.family = family;
        this.menuKind = menuKind;
        this.geometry = geometry;
        this.translation = translation;
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
}
