package io.github.ezequiel24123z.grindless.inspection;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.fluid.BasicTankBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.ClayConduitBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.HandPumpBlockEntity;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Energy;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Conditions;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ItemRole;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Face;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Manual;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ManualKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Position;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Ownership;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Port;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.PortRole;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.PortSummary;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Process;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Progress;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Resource;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ResourceKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.State;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.SiteRequirement;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.StoredItem;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Structure;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Tank;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Target;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Validity;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ExtractorLogic;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ResearchTerminalBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ThermalGeneratorBlockEntity;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.process.ConditionFault;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortBlockEntity;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortContract;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Server-thread, aimed-target query shared by optional overlays and future read-only consumers. */
public final class MachineInspection {

    private MachineInspection() {
    }

    public static Optional<MachineInspectionSnapshot> query(ServerLevel level, BlockPos position) {
        if (!level.hasChunkAt(position)) {
            return Optional.empty();
        }
        BlockState block = level.getBlockState(position);
        String blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
        if (!blockId.startsWith("grindless:")) {
            return Optional.empty();
        }
        BlockEntity entity = level.getBlockEntity(position);
        if (entity == null || entity.isRemoved() || !entity.getType().isValid(block)
                || !(entity instanceof MachineBlockEntity) && !(entity instanceof FluidEndpoint)) {
            return Optional.empty();
        }
        if (entity instanceof WorkshopPortBlockEntity port) {
            return Optional.of(port(level, position, blockId, port));
        }
        MachineStatus reported = MachineStatus.of(block).orElse(null);
        Optional<Structure> formation = structure(entity);
        Display display = display(entity, reported, formation);
        boolean held = entity instanceof MachineBlockEntity machine && machine.logisticsHold();
        Optional<Process> process = process(entity, reported);
        List<StoredItem> items = items(entity);
        Optional<Energy> energy = entity instanceof MachineBlockEntity machine
                ? Optional.of(new Energy(machine.energy().getStored(), machine.energy().getCapacity()))
                : Optional.empty();
        Optional<Manual> manual = entity instanceof HandCrankDynamoBlockEntity
                ? Optional.of(new Manual(ManualKind.HAND_DYNAMO, HandCrankDynamoBlockEntity.CHARGE_PER_CRANK))
                : entity instanceof ProcessMachineBlockEntity machine && machine.kind().tier() == FluxTier.F0
                ? Optional.of(new Manual(ManualKind.CRANK_HANDLE, SourceLogic.MANUAL_CHARGE)) : Optional.empty();
        boolean truncated = entity instanceof Container container
                && container.getContainerSize() > MachineInspectionSnapshot.MAX_ITEMS;
        if (entity instanceof ProcessMachineBlockEntity machine && machine.inspectionRecipe().isPresent()) {
            ProcessRecipe recipe = machine.inspectionRecipe().get();
            truncated |= recipe.inputs().size() > MachineInspectionSnapshot.MAX_RESOURCES
                    || recipe.outputs().size() > MachineInspectionSnapshot.MAX_RESOURCES
                    || recipe.catalysts().size() > MachineInspectionSnapshot.MAX_RESOURCES;
        }
        return Optional.of(new MachineInspectionSnapshot(
                new Target(level.dimension().location().toString(),
                        new Position(position.getX(), position.getY(), position.getZ()), level.getGameTime()),
                blockId, held ? State.IDLE : display.state(), held ? "inspection.grindless.reason.held" : display.reason(),
                held, process, items, energy, tanks(entity), manual, formation, Optional.empty(),
                requirements(entity), truncated));
    }

    /** STARVED is deliberately not split for process machines: its authority merges input and power. */
    private static Display display(BlockEntity entity, MachineStatus reported, Optional<Structure> formation) {
        if (formation.isPresent() && formation.get().validity() != Validity.FORMED) {
            return new Display(formation.get().validity() == Validity.UNKNOWN ? State.UNKNOWN
                    : State.INVALID_STRUCTURE, formation.get().reasonKey());
        }
        if (entity instanceof BasicTankBlockEntity || entity instanceof ClayConduitBlockEntity) {
            return new Display(State.STORAGE, "");
        }
        if (entity instanceof ProcessMachineBlockEntity process) {
            SourceSite.Fault site = process.inspectionSiteFault();
            if (site != SourceSite.Fault.NONE) {
                return new Display(State.INVALID_ENVIRONMENT, site.translationKey());
            }
            if (process.inspectionRecipe().isEmpty()
                    && (reported == MachineStatus.RUNNING || reported == MachineStatus.OUT_OF_BAND)) {
                return new Display(State.UNKNOWN, "inspection.grindless.reason.process_unavailable");
            }
            ConditionFault condition = process.inspectionConditionFault();
            if (condition != ConditionFault.NONE) {
                return new Display(condition == ConditionFault.MISSING_CATALYST ? State.MISSING_INPUT
                        : State.INVALID_ENVIRONMENT, "inspection.grindless.condition."
                        + condition.name().toLowerCase(Locale.ROOT));
            }
        }
        if (reported == MachineStatus.STARVED && entity instanceof HandPumpBlockEntity) {
            return new Display(State.INVALID_ENVIRONMENT, "inspection.grindless.reason.water_source");
        }
        if (reported == MachineStatus.STARVED
                && (entity instanceof ResearchTerminalBlockEntity || entity instanceof CrudeExtractorBlockEntity)) {
            return new Display(State.NO_ENERGY, "inspection.grindless.reason.no_energy");
        }
        if (reported == null) {
            return new Display(State.UNKNOWN, "inspection.grindless.reason.unknown");
        }
        return switch (reported) {
            case IDLE -> new Display(State.IDLE, "");
            case RUNNING -> new Display(State.WORKING, "");
            case BLOCKED -> new Display(State.BLOCKED, "inspection.grindless.reason.output_blocked");
            case STARVED -> new Display(State.BLOCKED, "inspection.grindless.reason.input_or_power");
            case OUT_OF_BAND -> new Display(entity instanceof ProcessMachineBlockEntity ? State.UNKNOWN
                    : State.INVALID_ENVIRONMENT, entity instanceof ProcessMachineBlockEntity
                    ? "inspection.grindless.reason.reported_fault" : "inspection.grindless.reason.conditions");
        };
    }

    private static Optional<Process> process(BlockEntity entity, MachineStatus reported) {
        if (entity instanceof ProcessMachineBlockEntity machine) {
            return machine.inspectionRecipe().map(recipe -> new Process(recipe.id(),
                    recipe.inputs().stream().limit(MachineInspectionSnapshot.MAX_RESOURCES)
                            .map(MachineInspection::resource).toList(),
                    recipe.outputs().stream().limit(MachineInspectionSnapshot.MAX_RESOURCES)
                            .map(MachineInspection::resource).toList(),
                    recipe.catalysts().stream().limit(MachineInspectionSnapshot.MAX_RESOURCES)
                            .map(MachineInspection::resource).toList(),
                    recipe.durationTicks(), recipe.fuPerTick(),
                    Optional.of(work(machine.inspectionProgress(), recipe.durationTicks())),
                    Optional.of(new Conditions(machine.conditions().temperature(), machine.conditions().pressure(),
                            machine.conditions().atmosphere().name(), machine.conditions().agitation().name(),
                            recipe.namesTemperature() ? Optional.of(recipe.temperatureC()) : Optional.empty(),
                            recipe.namesAtmosphere() ? Optional.of(recipe.atmosphere()) : Optional.empty(),
                            recipe.namesAgitation() ? Optional.of(recipe.agitation()) : Optional.empty()))));
        }
        if (entity instanceof ResearchTerminalBlockEntity terminal
                && !terminal.getItem(0).isEmpty()
                && BuiltInRegistries.ITEM.getKey(terminal.getItem(0).getItem()).toString().equals(ResearchLogic.DATA_CORE)) {
            return Optional.of(new Process("research/calibration",
                    List.of(new Resource(ResourceKind.ITEM, ResearchLogic.DATA_CORE, 1, 0, false)),
                    List.of(new Resource(ResourceKind.ITEM, ResearchLogic.CALIBRATED_DATA_CORE, 1, 0, false)),
                    List.of(), ResearchLogic.CYCLE_TICKS, ResearchLogic.FU_PER_TICK,
                    Optional.of(work(terminal.inspectionProgress(), ResearchLogic.CYCLE_TICKS))));
        }
        if (entity instanceof CrudeExtractorBlockEntity extractor && (extractor.inspectionProgress() > 0
                || reported == MachineStatus.RUNNING || reported == MachineStatus.STARVED)) {
            return Optional.of(new Process("crude_extraction", List.of(), List.of(), List.of(),
                    ExtractorLogic.CYCLE_TICKS, ExtractorLogic.FU_PER_TICK,
                    Optional.of(work(extractor.inspectionProgress(), ExtractorLogic.CYCLE_TICKS))));
        }
        if (entity instanceof ThermalGeneratorBlockEntity generator) {
            int duration = generator.menuData().get(ProcessMachineMenu.DATA_CYCLE);
            int completed = generator.menuData().get(ProcessMachineMenu.DATA_PROGRESS);
            // A cycle of one may be the menu's zero-duration guard, not a real fuel cycle.
            if (duration > 1 && completed >= 0 && completed <= duration) {
                return Optional.of(new Process("fuel_burn", List.of(), List.of(), List.of(), duration, 0,
                        Optional.of(new Progress(completed, duration))));
            }
        }
        return Optional.empty();
    }

    private static Progress work(double completed, int duration) {
        return new Progress(Math.min(completed, duration), duration);
    }

    private static List<StoredItem> items(BlockEntity entity) {
        if (!(entity instanceof Container container)) {
            return List.of();
        }
        List<StoredItem> result = new ArrayList<>();
        int bound = Math.min(container.getContainerSize(), MachineInspectionSnapshot.MAX_ITEMS);
        for (int slot = 0; slot < bound; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                ItemRole role = entity instanceof ProcessMachineBlockEntity process
                        ? slot < process.kind().menuKind().inputs() ? ItemRole.INPUT : ItemRole.OUTPUT
                        : entity instanceof CrudeExtractorBlockEntity ? ItemRole.OUTPUT
                        : entity instanceof ThermalGeneratorBlockEntity ? ItemRole.FUEL : ItemRole.SHARED;
                result.add(item(slot, role, stack));
            }
        }
        return List.copyOf(result);
    }

    private static List<Tank> tanks(BlockEntity entity) {
        if (!(entity instanceof FluidEndpoint endpoint)) {
            return List.of();
        }
        FluidState contents = endpoint.contents();
        return List.of(new Tank(contents.id(), contents.millibuckets(), endpoint.capacity(),
                entity instanceof ProcessMachineBlockEntity process
                        ? Optional.of(process.inspectionFluidOutput()) : Optional.empty(),
                contents.temperatureC(), contents.pressureMPa()));
    }

    private static Optional<Structure> structure(BlockEntity entity) {
        if (!(entity instanceof ProcessMachineBlockEntity process) || process.kind().structure() == WorkshopStructure.NONE) {
            return Optional.empty();
        }
        WorkshopStructure.Inspection inspection = process.inspectionStructure();
        if (inspection.ports().size() > MachineInspectionSnapshot.MAX_PORTS) {
            throw new IllegalArgumentException("native structure exceeds the inspection port bound");
        }
        WorkshopStructure.Fault fault = inspection.fault();
        Direction orientation = process.getBlockState().getValue(MachineProperties.FACING);
        List<PortSummary> ports = inspection.ports().stream().map(port -> new PortSummary(
                role(port.kind().role()), Face.valueOf(orientation.name()),
                portFaces(process.kind().structure(), port.offset(), port.kind(), orientation))).toList();
        return Optional.of(new Structure(process.kind().structure().name().toLowerCase(Locale.ROOT),
                fault == WorkshopStructure.Fault.NONE ? Validity.FORMED
                        : fault == WorkshopStructure.Fault.UNLOADED ? Validity.UNKNOWN : Validity.INVALID,
                fault == WorkshopStructure.Fault.NONE ? "" : fault.translationKey(), ports,
                Optional.ofNullable(inspection.offset()).map(offset -> new Position(
                        process.getBlockPos().getX() + offset.x(), process.getBlockPos().getY() + offset.y(),
                        process.getBlockPos().getZ() + offset.z()))));
    }

    private static MachineInspectionSnapshot port(ServerLevel level, BlockPos position, String blockId,
                                                   WorkshopPortBlockEntity entity) {
        WorkshopPortBlockEntity.Inspection inspection = entity.inspection();
        ProcessMachineBlockEntity owner = null;
        if (inspection.formed() && inspection.ownerPos() != null && level.hasChunkAt(inspection.ownerPos())
                && level.getBlockEntity(inspection.ownerPos()) instanceof ProcessMachineBlockEntity controller
                && !controller.isRemoved()) {
            owner = controller;
        }
        boolean confirmed = owner != null;
        WorkshopStructure.Fault fault = inspection.fault();
        Ownership ownership = confirmed ? Ownership.CONFIRMED
                : inspection.ownerPos() != null ? Ownership.CANDIDATE
                : fault == WorkshopStructure.Fault.ORPHANED_PORT ? Ownership.ORPHAN : Ownership.UNKNOWN;
        List<Face> faces = List.of();
        List<StoredItem> items = List.of();
        List<Tank> tanks = List.of();
        boolean truncated = false;
        if (confirmed) {
            WorkshopPortContract.Kind kind = WorkshopPortContract.kind(blockId).orElseThrow(() ->
                    new IllegalArgumentException("inspected port has no native kind"));
            faces = portFaces(owner.kind().structure(), new WorkshopStructure.Offset(
                    position.getX() - owner.getBlockPos().getX(), position.getY() - owner.getBlockPos().getY(),
                    position.getZ() - owner.getBlockPos().getZ()), kind, inspection.facing());
            // Port storage getters refresh capabilities. Read only the confirmed controller's native stores instead.
            if (inspection.role().item()) {
                int from = inspection.role().input() ? 0 : owner.kind().menuKind().inputs();
                int to = inspection.role().input() ? owner.kind().menuKind().inputs() : owner.getContainerSize();
                List<StoredItem> captured = new ArrayList<>();
                for (int slot = from; slot < Math.min(to, MachineInspectionSnapshot.MAX_ITEMS); slot++) {
                    ItemStack stack = owner.getItem(slot);
                    if (!stack.isEmpty()) {
                        captured.add(item(slot, inspection.role().input() ? ItemRole.INPUT : ItemRole.OUTPUT, stack));
                    }
                }
                items = List.copyOf(captured);
                truncated = to > MachineInspectionSnapshot.MAX_ITEMS;
            } else {
                FluidState contents = inspection.role() == WorkshopPortRole.FLUID_OUTPUT
                        ? owner.workshopProducedFluid() : owner.contents();
                tanks = List.of(new Tank(contents.id(), contents.millibuckets(), owner.capacity(), Optional.empty(),
                        contents.temperatureC(), contents.pressureMPa()));
            }
        }
        Optional<Position> ownerPosition = Optional.ofNullable(inspection.ownerPos())
                .map(pos -> new Position(pos.getX(), pos.getY(), pos.getZ()));
        State state = confirmed ? State.INTERFACE
                : fault == WorkshopStructure.Fault.UNLOADED || fault == WorkshopStructure.Fault.NONE
                || inspection.formed() ? State.UNKNOWN : State.INVALID_STRUCTURE;
        String reason = confirmed ? "" : fault == WorkshopStructure.Fault.NONE
                ? "inspection.grindless.reason.owner_unavailable" : fault.translationKey();
        return new MachineInspectionSnapshot(new Target(level.dimension().location().toString(),
                new Position(position.getX(), position.getY(), position.getZ()), level.getGameTime()),
                blockId, state, reason, false, Optional.empty(), items, Optional.empty(), tanks, Optional.empty(),
                Optional.empty(), Optional.of(new Port(role(inspection.role()), Face.valueOf(inspection.facing().name()),
                ownership, ownerPosition, faces)), List.of(), truncated);
    }

    /** Facing is structural orientation; actual contract access may be on several different faces. */
    static List<Face> portFaces(WorkshopStructure structure, WorkshopStructure.Offset rotatedOffset,
                                WorkshopPortContract.Kind kind, Direction orientation) {
        int turns = turns(orientation);
        WorkshopPortContract.Slot slot = WorkshopPortContract.slots(structure).stream()
                .filter(candidate -> candidate.offset().rotate(turns).equals(rotatedOffset) && candidate.accepts(kind))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("inspected port has no native contract slot"));
        return List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST).stream()
                .filter(direction -> slot.permitsFace(turns, turns(direction)))
                .map(direction -> Face.valueOf(direction.name())).toList();
    }

    private static int turns(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("inspected workshop orientation must be horizontal");
        };
    }

    private static PortRole role(WorkshopPortRole role) {
        return switch (role) {
            case ITEM_INPUT -> PortRole.ITEM_INPUT;
            case ITEM_OUTPUT -> PortRole.ITEM_OUTPUT;
            case FLUID_INPUT -> PortRole.FLUID_INPUT;
            case FLUID_OUTPUT -> PortRole.FLUID_OUTPUT;
        };
    }

    private static StoredItem item(int slot, ItemRole role, ItemStack stack) {
        int rating = stack.getItem() instanceof ControlMatrixItem matrix
                ? matrix.spec(stack).map(spec -> spec.technologyRating()).orElse(-1) : 0;
        return new StoredItem(slot, role, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(), rating);
    }

    private static List<SiteRequirement> requirements(BlockEntity entity) {
        if (entity instanceof HandPumpBlockEntity) {
            return List.of(SiteRequirement.WATER_SOURCE);
        }
        if (entity instanceof ProcessMachineBlockEntity process) {
            return switch (process.kind().site()) {
                case NONE -> List.of();
                case GEOLOGICAL -> List.of(SiteRequirement.TERRESTRIAL, SiteRequirement.ROCK_BED);
                case SUNLIT_CULTURE -> List.of(SiteRequirement.TERRESTRIAL,
                        SiteRequirement.OPEN_SKY, SiteRequirement.DAYLIGHT);
            };
        }
        return List.of();
    }

    private static Resource resource(IngredientSpec ingredient) {
        return new Resource(kind(ingredient.kind()), ingredient.id(), ingredient.count(),
                ingredient.matrixRating(), false);
    }

    private static Resource resource(OutputSpec output) {
        return new Resource(kind(output.kind()), output.id(), output.count(), output.matrixRating(), output.vented());
    }

    private static ResourceKind kind(String kind) {
        return switch (kind) {
            case IngredientSpec.ITEM -> ResourceKind.ITEM;
            case IngredientSpec.TAG -> ResourceKind.TAG;
            case IngredientSpec.FLUID -> ResourceKind.FLUID;
            default -> throw new IllegalArgumentException("unknown inspected resource kind");
        };
    }

    private record Display(State state, String reason) {
    }
}
