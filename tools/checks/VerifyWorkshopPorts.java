package io.github.ezequiel24123z.grindless.structure;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.fluid.BasicTankBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.NativeFluidStore;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Rotated role contracts and real process/native-store quota boundaries; not a loaded-world Forge proof. */
public final class VerifyWorkshopPorts {

    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        // Exercise real registered blocks without trying to add intrusive holders after freeze.
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(kind.id()),
                    new WorkshopPortBlock(kind, BlockBehaviour.Properties.of()));
        }
        Bootstrap.bootStrap();
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        contracts();
        rotations();
        limitsAndAliases();
        nativeTransfer();
        noPortStorage();
        readOnlyInspection();
        manifest();
        models();
        System.out.println("ALL WORKSHOP PORT CHECKS PASSED (" + checks + ")");
    }

    private static void contracts() {
        eq("four justified roles only", 4, WorkshopPortRole.values().length);
        eq("six family-preserving variants only", 6, WorkshopPortContract.Kind.values().length);
        Set<String> ids = new HashSet<>();
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            check("variant IDs are unique", ids.add(kind.id()));
            BlockCatalogue.Entry entry = BlockCatalogue.block(kind.blockName());
            check("variant has a registered asset contract", entry != null && entry.facing() && !entry.hasStatus());
            eq("variant geometry retains its family", kind.geometry(), entry.geometry());
            check("every variant has a legal slot", WorkshopPortContract.slots(kind.structure()).stream()
                    .anyMatch(slot -> slot.accepts(kind)));
        }
        eq("Firing retains five structural bricks", 5,
                WorkshopPortContract.minimumBricks(WorkshopStructure.FIRING_CHAMBER));
        eq("Firing has exactly the two approved cells", Set.of(
                        new WorkshopStructure.Offset(1, 0, 0), new WorkshopStructure.Offset(1, 1, 0)),
                Set.copyOf(WorkshopPortContract.slots(WorkshopStructure.FIRING_CHAMBER).stream()
                        .map(WorkshopPortContract.Slot::offset).toList()));
        for (WorkshopPortRole role : WorkshopPortRole.values()) {
            eq("Firing permits only one of each item role", role.item() ? 1 : 0,
                    WorkshopPortContract.maximum(WorkshopStructure.FIRING_CHAMBER, role));
            eq("Trough permits at most one of each role", 1,
                    WorkshopPortContract.maximum(WorkshopStructure.SETTLING_TROUGH, role));
        }
        check("future ordinary machines gain no port slots", WorkshopPortContract.slots(WorkshopStructure.NONE).isEmpty());
    }

    private static void rotations() {
        for (WorkshopStructure structure : List.of(WorkshopStructure.FIRING_CHAMBER, WorkshopStructure.SETTLING_TROUGH)) {
            for (int turn = 0; turn < 4; turn++) {
                Map<WorkshopStructure.Offset, WorkshopStructure.Present> world = legacy(structure, turn);
                check("every original default structure remains valid", structure.inspect(turn, world::get).formed());
                for (WorkshopStructure.Cell cell : structure.cells()) {
                    WorkshopStructure.Offset rotated = cell.offset().rotate(turn);
                    for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
                        world.put(rotated, new WorkshopStructure.Present(kind.id(), turn));
                        WorkshopStructure.Inspection found = structure.inspect(turn, world::get);
                        boolean allowed = WorkshopPortContract.slot(structure, cell.offset())
                                .map(slot -> slot.accepts(kind)).orElse(false);
                        eq("exact cell/family/role admission under every rotation", allowed, found.formed());
                        if (allowed) {
                            eq("formation exposes exactly the recognised port", List.of(new WorkshopStructure.Port(rotated, kind)),
                                    found.ports());
                            world.put(rotated, new WorkshopStructure.Present(kind.id(), (turn + 1) % 4));
                            WorkshopStructure.Inspection facing = structure.inspect(turn, world::get);
                            eq("rotated port is rejected", WorkshopStructure.Fault.WRONG_FACING, facing.fault());
                            eq("wrong-facing diagnostic identifies the exact cell", rotated, facing.offset());
                        } else {
                            eq("incompatible port has an explicit fault", WorkshopStructure.Fault.INCOMPATIBLE_PORT,
                                    found.fault());
                            eq("incompatible diagnostic identifies the exact cell", rotated, found.offset());
                        }
                        world.put(rotated, new WorkshopStructure.Present(cell.block(), cell.oriented() ? turn : -1));
                    }
                }
                List<WorkshopPortContract.Slot> slots = WorkshopPortContract.slots(structure);
                for (WorkshopPortContract.Slot slot : slots) {
                    for (int worldFace = -1; worldFace < 4; worldFace++) {
                        boolean expected = worldFace >= 0
                                && slot.faces().contains(Math.floorMod(worldFace - turn, 4));
                        eq("side permissions rotate with the structural family", expected,
                                slot.permitsFace(turn, worldFace));
                    }
                }
                if (structure == WorkshopStructure.FIRING_CHAMBER) {
                    for (WorkshopPortContract.Kind first : List.of(WorkshopPortContract.Kind.WORKSHOP_ITEM_INPUT,
                            WorkshopPortContract.Kind.WORKSHOP_ITEM_OUTPUT)) {
                        for (WorkshopPortContract.Kind second : List.of(WorkshopPortContract.Kind.WORKSHOP_ITEM_INPUT,
                                WorkshopPortContract.Kind.WORKSHOP_ITEM_OUTPUT)) {
                            world = legacy(structure, turn);
                            world.put(slots.get(0).offset().rotate(turn), new WorkshopStructure.Present(first.id(), turn));
                            world.put(slots.get(1).offset().rotate(turn), new WorkshopStructure.Present(second.id(), turn));
                            WorkshopStructure.Inspection found = structure.inspect(turn, world::get);
                            eq("two Firing ports must have distinct roles", first != second, found.formed());
                            if (first == second) {
                                eq("duplicate role has an excess-port fault", WorkshopStructure.Fault.EXCESS_PORTS, found.fault());
                            }
                        }
                    }
                } else {
                    for (WorkshopPortContract.Kind input : List.of(WorkshopPortContract.Kind.SETTLING_ITEM_INPUT,
                            WorkshopPortContract.Kind.SETTLING_FLUID_INPUT)) {
                        for (WorkshopPortContract.Kind output : List.of(WorkshopPortContract.Kind.SETTLING_ITEM_OUTPUT,
                                WorkshopPortContract.Kind.SETTLING_FLUID_OUTPUT)) {
                            world = legacy(structure, turn);
                            world.put(slots.get(0).offset().rotate(turn), new WorkshopStructure.Present(input.id(), turn));
                            world.put(slots.get(1).offset().rotate(turn), new WorkshopStructure.Present(output.id(), turn));
                            check("all four useful Trough role combinations form", structure.inspect(turn, world::get).formed());
                        }
                    }
                    WorkshopStructure.Offset covered = new WorkshopStructure.Offset(0, 1, 2).rotate(turn);
                    world.put(covered, new WorkshopStructure.Present("minecraft:stone", -1));
                    WorkshopStructure.Inspection obstruction = structure.inspect(turn, world::get);
                    eq("ports do not excuse covered working channels", WorkshopStructure.Fault.COVERED, obstruction.fault());
                    eq("covered diagnostic has the exact rotated offset", covered, obstruction.offset());
                }
                world = legacy(structure, turn);
                WorkshopStructure.Offset unloaded = structure.cells().get(0).offset().rotate(turn);
                world.remove(unloaded);
                WorkshopStructure.Inspection missingChunk = structure.inspect(turn, world::get);
                eq("an unloaded cell is never accepted", WorkshopStructure.Fault.UNLOADED, missingChunk.fault());
                eq("unloaded diagnostic has the exact rotated offset", unloaded, missingChunk.offset());
            }
        }
    }

    private static void limitsAndAliases() throws Exception {
        ProcessMachineBlockEntity owner = owner(1750, 750);
        owner.setItem(0, new ItemStack(Items.DIRT, 2));
        QuotaView view = new QuotaView(owner);
        List<FluidInsert.Source> aliases = List.of(source(view, Direction.EAST), source(view, Direction.WEST));
        eq("output view retains exactly the 1000 mB primary reservation", 750, view.contents().millibuckets());
        eq("all faces share the actual controller store", owner.nativeStore(), view.nativeStore());
        eq("two quota faces cannot advertise two payments", 1, FluidInsert.available(aliases).size());
        eq("two quota faces expose 750, not the full 1750 or two quotas", 750,
                FluidInsert.available(aliases).get(0).millibuckets());
        ProcessMachineBlockEntity consumer = consumer();
        check("quota plus one cannot reserve primary input", plan(consumer, aliases, 751).isEmpty());
        eq("failed reservation leaves actual controller fluid intact", 1750, owner.contents().millibuckets());
        eq("failed reservation leaves actual output ownership intact", 750, owner.inspectionFluidOutput());
        eq("failed reservation does not charge the real consumer", 1, consumer.contents().millibuckets());
        check("exact quota settles once", plan(consumer, aliases, 750).orElseThrow().commit());
        eq("exact quota leaves primary water in the controller", 1000, owner.contents().millibuckets());
        eq("exact quota retires produced ownership", 0, owner.inspectionFluidOutput());
        check("the original dirt inputs remain untouched", owner.getItem(0).is(Items.DIRT) && owner.getItem(0).getCount() == 2);

        owner = owner(1750, 750);
        view = new QuotaView(owner);
        aliases = List.of(source(view, Direction.EAST), source(view, Direction.WEST));
        consumer = consumer();
        NativeFluidStore.Batch limited = plan(consumer, aliases, 750).orElseThrow();
        view.visibleLimit = 749;
        check("a changed access bound aborts even with unchanged native snapshot", !limited.commit());
        eq("a refused changed limit preserves all actual stock", 1750, owner.contents().millibuckets());
        eq("a refused changed limit preserves native quota", 750, owner.inspectionFluidOutput());

        view.visibleLimit = Integer.MAX_VALUE;
        NativeFluidStore.Batch retiredGeneration = plan(consumer, aliases, 750).orElseThrow();
        view.revision++;
        check("a repaired/rebound proxy still retires a previous native access", !retiredGeneration.commit());
        NativeFluidStore.Batch replaced = plan(consumer, aliases, 750).orElseThrow();
        ProcessMachineBlockEntity newIdentity = owner(1750, 750);
        view.owner = newIdentity;
        check("replacement at the same coordinates retires the captured native store", !replaced.commit());
        eq("a stale owner plan cannot consume the old controller", 1750, owner.contents().millibuckets());
        eq("a stale owner plan cannot consume the new controller", 1750, newIdentity.contents().millibuckets());
        check("fresh access rebinds to the new native identity", plan(consumer, aliases, 750).orElseThrow().commit());
        eq("newly bound access consumes only the new controller quota", 1000, newIdentity.contents().millibuckets());

        owner = owner(1750, 750);
        view = new QuotaView(owner);
        consumer = consumer();
        List<FluidInsert.Source> one = List.of(source(view, Direction.EAST));
        NativeFluidStore.Batch rotated = plan(consumer, one, 750).orElseThrow();
        view.allowed = false;
        check("closed/rotated access retires an already planned face", !rotated.commit());
        view.allowed = true;
        NativeFluidStore.Batch removed = plan(consumer, one, 750).orElseThrow();
        owner.setRemoved();
        check("unloaded/removed structural identity cannot settle", !removed.commit());

        ProcessMachineBlockEntity bounded = owner(1000, 750);
        bounded.setItem(0, new ItemStack(Items.DIRT, 2));
        eq("a ready primary batch is not output-port stock", FluidState.EMPTY, bounded.workshopProducedFluid());
        ProcessMachineBlockEntity shortInput = owner(750, 750);
        eq("a completed batch exposes its real 750 mB with no remaining input items", 750,
                shortInput.workshopProducedFluid().millibuckets());
        shortInput.setItem(0, new ItemStack(Items.DIRT, 2));
        eq("an incomplete future primary batch cannot indefinitely hide recovered output", 750,
                shortInput.workshopProducedFluid().millibuckets());
        ProcessMachineBlockEntity noItems = owner(1000, 750);
        eq("a family's preload target is not an actual primary reservation", 750,
                noItems.workshopProducedFluid().millibuckets());
    }

    private static void nativeTransfer() throws Exception {
        ProcessMachineBlockEntity owner = owner(1750, 750);
        QuotaView view = new QuotaView(owner);
        BasicTankBlockEntity target = tank();
        target.insert(null, FluidState.of(FluidLogic.WATER, 15_975));
        eq("partial native acceptance moves exactly the real space", 25,
                FluidInsert.moveNative(source(view, Direction.EAST), source(target, Direction.WEST), 50));
        eq("partial transfer fills the actual Basic Tank", 16_000, target.contents().millibuckets());
        eq("partial transfer debits source once", 1725, owner.contents().millibuckets());
        eq("partial transfer debits produced quota only", 725, owner.inspectionFluidOutput());
        eq("full target cannot consume any more source", 0,
                FluidInsert.moveNative(source(view, Direction.EAST), source(target, Direction.WEST), 50));
        eq("a same-store loop cannot duplicate or reclassify fluid", 0,
                FluidInsert.moveNative(source(view, Direction.EAST), source(owner, Direction.WEST), 50));

        ProcessMachineBlockEntity failing = owner(1750, 750);
        BasicTankBlockEntity destination = tank();
        FluidState before = failing.contents();
        RuntimeException injected = new IllegalStateException("native target admission fixture");
        try {
            NativeFluidStore.transfer(new NativeFluidStore.Access(failing.nativeStore(), () -> true,
                            () -> failing.workshopProducedFluid().millibuckets()),
                    destination.nativeStore(), before.withAmount(50), () -> {
                        if (failing.contents().millibuckets() != before.millibuckets()) {
                            throw injected;
                        }
                        return true;
                    });
            throw new AssertionError("native settlement exception must propagate");
        } catch (RuntimeException actual) {
            check("native transfer preserves its original exception", actual == injected);
        }
        eq("exception restores the exact native source state", before, failing.contents());
        eq("exception restores produced quota", 750, failing.inspectionFluidOutput());
        eq("exception leaves no destination fluid", FluidState.EMPTY, destination.contents());
    }

    private static void noPortStorage() throws Exception {
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            WorkshopPortBlock block = (WorkshopPortBlock) BuiltInRegistries.BLOCK.get(new ResourceLocation(kind.id()));
            BlockState state = block.defaultBlockState();
            eq("ports have facing only, never a fake one-valued status", 1, state.getProperties().size());
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                var expected = kind.family() == WorkshopPortContract.Family.BRICK ? Shapes.block()
                        : WorkshopPartBlock.shape(kind.geometry(), facing);
                check("every rotated port preserves its actual collision family",
                        !Shapes.joinIsNotEmpty(expected, block.getShape(state.setValue(MachineProperties.FACING, facing),
                                null, BlockPos.ZERO, CollisionContext.empty()), BooleanOp.NOT_SAME));
            }
            Constructor<WorkshopPortBlockEntity> constructor = WorkshopPortBlockEntity.class.getDeclaredConstructor(
                    BlockEntityType.class, WorkshopPortContract.Kind.class, BlockPos.class, BlockState.class);
            constructor.setAccessible(true);
            WorkshopPortBlockEntity port = constructor.newInstance(null, kind, BlockPos.ZERO, state);
            CompoundTag invented = new CompoundTag();
            invented.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, 1000)));
            invented.putString("Owner", "an-untrusted-old-controller");
            invented.putBoolean("Formed", true);
            port.load(invented);
            check("an orphan never trusts saved formation", !port.inspection().formed() && port.inspection().ownerPos() == null);
            eq("an orphan has no item inventory", 0, port.getContainerSize());
            eq("an orphan has no tank", 0, port.capacity());
            eq("an orphan has no fluid contents", FluidState.EMPTY, port.contents());
            check("an orphan has no native owner", port.nativeStore() == null);
            for (Direction direction : Direction.values()) {
                check("every orphan face is closed", !port.permitsFace(direction));
                eq("every orphan insertion returns the actual unaccepted fluid",
                        FluidState.of(FluidLogic.WATER, 1000), port.insert(direction, FluidState.of(FluidLogic.WATER, 1000)));
                eq("every orphan extraction is empty", FluidState.EMPTY, port.extract(direction, 1000));
            }
            check("unsided access cannot bypass a port face", !port.permitsFace(null));
            CompoundTag saved = port.saveWithoutMetadata();
            check("a port never serializes independent fluid or a trusted link",
                    !saved.contains("Fluid") && !saved.contains("Items") && !saved.contains("Owner") && !saved.contains("Formed"));
            int[] invalidations = {0};
            port.addCapabilityInvalidator(() -> invalidations[0]++);
            long revision = port.capabilityRevision();
            port.setBlockState(state.setValue(MachineProperties.FACING, Direction.EAST));
            check("rotation retires retained capability revisions", port.capabilityRevision() != revision);
            port.setRemoved();
            check("removal invalidates capability listeners", invalidations[0] >= 2);
        }
    }

    private static void readOnlyInspection() throws Exception {
        ProcessMachineBlockEntity machine = owner(1750, 750);
        ProcessRecipe cached = ProcessLookup.recipes().stream()
                .filter(recipe -> recipe.family() == machine.kind().family()).findFirst().orElseThrow();
        Field cache = ProcessMachineBlockEntity.class.getDeclaredField("cached");
        cache.setAccessible(true);
        cache.set(machine, cached);
        CompoundTag before = machine.saveWithoutMetadata().copy();
        eq("inspection reads the actual cached recipe", cached, machine.inspectionRecipe().orElseThrow());
        machine.inspectionProgress();
        machine.inspectionConditionFault();
        machine.inspectionStructureFault();
        machine.inspectionSiteFault();
        machine.inspectionFluidOutput();
        machine.workshopProducedFluid();
        eq("inspection does not mutate saved state", before, machine.saveWithoutMetadata());
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        check("inspection hides stale recipe generations", machine.inspectionRecipe().isEmpty());
        eq("hiding a stale generation does not resolve or replace the cached recipe", cached, cache.get(machine));
    }

    private static void manifest() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of(
                "docs", "tiers", "t0-workshop-interfaces.json"))).getAsJsonObject();
        eq("manifest capacity derives from the current authority", FluidLogic.TANK_CAPACITY,
                manifest.getAsJsonObject("tank").get("capacity_mB").getAsInt());
        eq("manifest temperature rating derives from the current authority", FluidLogic.AMBIENT_MAX_C,
                manifest.getAsJsonObject("tank").get("maximum_temperature_C").getAsDouble());
        eq("manifest pressure rating derives from the current authority", FluidLogic.AMBIENT_MPA,
                manifest.getAsJsonObject("tank").get("maximum_pressure_MPa").getAsDouble());
        JsonObject presentation = manifest.getAsJsonObject("presentation");
        eq("readability manifest retains the four shared textures", 4,
                presentation.get("shared_original_role_textures").getAsInt());
        eq("readability manifest retains the measured minimum, not an artistic approval", 4,
                presentation.get("settling_role_label_minimum_glyph_model_units").getAsInt());
        check("readability labels change neither collision nor automation policy",
                !presentation.get("collision_changed").getAsBoolean()
                        && !presentation.get("upward_automation_enabled_by_label").getAsBoolean()
                        && !presentation.get("human_appearance_accepted").getAsBoolean());
        Set<String> variants = new HashSet<>();
        for (var value : manifest.getAsJsonArray("variants")) {
            JsonObject row = value.getAsJsonObject();
            WorkshopPortContract.Kind kind = WorkshopPortContract.kind(row.get("id").getAsString()).orElseThrow();
            check("manifest variant occurs once", variants.add(kind.id()));
            eq("manifest family agrees with role contract", kind.family().name(), row.get("family").getAsString());
            eq("manifest role agrees with role contract", kind.role().name(), row.get("role").getAsString());
            eq("manifest texture agrees with role contract", kind.role().texture(), row.get("texture").getAsString());
        }
        eq("manifest covers every variant", WorkshopPortContract.Kind.values().length, variants.size());
        for (var value : manifest.getAsJsonArray("structures")) {
            JsonObject row = value.getAsJsonObject();
            WorkshopStructure structure = WorkshopStructure.valueOf(row.get("name").getAsString());
            eq("manifest minimum is not a second authority", WorkshopPortContract.minimumBricks(structure),
                    row.get("minimum_plain_bricks").getAsInt());
            eq("manifest port count is not a second authority", WorkshopPortContract.maximumPorts(structure),
                    row.get("maximum_ports").getAsInt());
            for (WorkshopPortRole role : WorkshopPortRole.values()) {
                eq("manifest role caps match runtime", WorkshopPortContract.maximum(structure, role),
                        row.getAsJsonObject("maximum_each_role").get(role.name()).getAsInt());
            }
            List<WorkshopPortContract.Slot> slots = WorkshopPortContract.slots(structure);
            eq("manifest enumerates every slot", slots.size(), row.getAsJsonArray("slots").size());
            for (int i = 0; i < slots.size(); i++) {
                JsonObject projected = row.getAsJsonArray("slots").get(i).getAsJsonObject();
                eq("manifest slot family matches runtime", slots.get(i).family().name(),
                        projected.get("family").getAsString());
                Set<WorkshopPortRole> roles = new HashSet<>();
                projected.getAsJsonArray("roles").forEach(role ->
                        roles.add(WorkshopPortRole.valueOf(role.getAsString())));
                eq("manifest slot roles match runtime", slots.get(i).roles(), roles);
                var offset = projected.getAsJsonArray("offset");
                eq("manifest position is exact", slots.get(i).offset(),
                        new WorkshopStructure.Offset(offset.get(0).getAsInt(), offset.get(1).getAsInt(), offset.get(2).getAsInt()));
                Set<Integer> faces = new HashSet<>();
                projected.getAsJsonArray("local_horizontal_faces").forEach(face -> faces.add(face.getAsInt()));
                eq("manifest exterior faces match the role contract", slots.get(i).faces(), faces);
            }
        }
    }

    private static void models() throws Exception {
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            JsonObject model = JsonParser.parseString(Files.readString(Path.of("common", "src", "main",
                    "resources", "assets", "grindless", "models", "block",
                    kind.blockName() + "_idle.json"))).getAsJsonObject();
            eq("port models inherit ordinary native block presentation", "minecraft:block/block",
                    model.get("parent").getAsString());
            eq("transparent role markings use the actual cutout render layer", "minecraft:cutout",
                    model.get("render_type").getAsString());
            eq("all six variants share the four original role overlays", "grindless:block/" + kind.role().texture(),
                    model.getAsJsonObject("textures").get("port").getAsString());
            boolean brick = kind.family() == WorkshopPortContract.Family.BRICK;
            JsonArray elements = model.getAsJsonArray("elements");
            int baseCount = brick ? 1 : WorkshopGeometry.boxes(kind.geometry()).size();
            eq("models keep the complete base plus the side markings and settling readability label",
                    baseCount + (brick ? 2 : kind.family() == WorkshopPortContract.Family.SCREEN ? 4 : 3),
                    elements.size());
            if (brick) {
                eq("brick ports reference vanilla brick rather than importing another asset", "minecraft:block/bricks",
                        model.getAsJsonObject("textures").get("brick").getAsString());
                vector("brick casing starts at the original cube boundary",
                        elements.get(0).getAsJsonObject().getAsJsonArray("from"), 0, 0, 0);
                vector("brick casing ends at the original cube boundary",
                        elements.get(0).getAsJsonObject().getAsJsonArray("to"), 16, 16, 16);
            } else {
                List<WorkshopGeometry.Box> boxes = WorkshopGeometry.boxes(kind.geometry());
                for (int i = 0; i < boxes.size(); i++) {
                    WorkshopGeometry.Box box = boxes.get(i);
                    JsonObject actual = elements.get(i).getAsJsonObject();
                    vector("a settling port retains every original working/support box",
                            actual.getAsJsonArray("from"), box.x0(), box.y0(), box.z0());
                    vector("a settling port never replaces narrow rails/screens with a full cube",
                            actual.getAsJsonArray("to"), box.x1(), box.y1(), box.z1());
                }
            }
            Set<Integer> markedFaces = new HashSet<>();
            int readableLabels = 0;
            for (int i = baseCount; i < elements.size(); i++) {
                JsonObject mark = elements.get(i).getAsJsonObject();
                JsonArray from = mark.getAsJsonArray("from");
                JsonArray to = mark.getAsJsonArray("to");
                int flatAxes = 0;
                for (int axis = 0; axis < 3; axis++) {
                    flatAxes += from.get(axis).getAsDouble() == to.get(axis).getAsDouble() ? 1 : 0;
                }
                eq("role markings add a face, not another solid casing", 1, flatAxes);
                JsonObject faces = mark.getAsJsonObject("faces");
                eq("each marking lies on exactly one model face", 1, faces.size());
                for (var face : faces.entrySet()) {
                    JsonObject definition = face.getValue().getAsJsonObject();
                    eq("the marking resolves through the shared role slot", "#port", definition.get("texture").getAsString());
                    JsonArray uv = definition.getAsJsonArray("uv");
                    eq("overlay UVs have four components", 4, uv.size());
                    if (face.getKey().equals("up")) {
                        check("only narrow settling parts need the existing-bed readability label", !brick);
                        vector("the top label lies just above the existing bed and inside the rails",
                                from, 3.01, 4.01, 0.01);
                        vector("the top label ends before the screen bars, never under them",
                                to, 12.99, 4.01, 6.99);
                        eq("readability label crops only transparent U0 margin", 2, uv.get(0).getAsInt());
                        eq("readability label crops only transparent V0 margin", 3, uv.get(1).getAsInt());
                        eq("readability label retains the full glyph width", 14, uv.get(2).getAsInt());
                        eq("readability label retains the full glyph height", 10, uv.get(3).getAsInt());
                        eq("readability label keeps the supplied glyph orientation", 0, definition.get("rotation").getAsInt());
                        check("the visible five-pixel glyph spans at least four native model units",
                                (to.get(2).getAsDouble() - from.get(2).getAsDouble()) * 5.0
                                        / (uv.get(3).getAsDouble() - uv.get(1).getAsDouble()) >= 4.0);
                        readableLabels++;
                        continue;
                    }
                    int turns = switch (face.getKey()) {
                        case "north" -> 0;
                        case "east" -> 1;
                        case "south" -> 2;
                        case "west" -> 3;
                        default -> throw new AssertionError("unexpected marked face " + face.getKey());
                    };
                    markedFaces.add(turns);
                    eq("full overlay U0", 0, uv.get(0).getAsInt());
                    eq("full overlay V0", 0, uv.get(1).getAsInt());
                    eq("full overlay U1", 16, uv.get(2).getAsInt());
                    eq("full overlay V1", 16, uv.get(3).getAsInt());
                }
            }
            Set<Integer> permitted = WorkshopPortContract.slots(kind.structure()).stream()
                    .filter(slot -> slot.accepts(kind)).findFirst().orElseThrow().faces();
            eq("markings identify the same faces that actually transfer", permitted, markedFaces);
            eq("each settling port has exactly one broad unoccluded role label", brick ? 0 : 1, readableLabels);
            if (!brick) {
                var image = ImageIO.read(Path.of("common", "src", "main", "resources", "assets", "grindless",
                        "textures", "block", kind.role().texture() + ".png").toFile());
                check("the existing original role overlay is a 16px image",
                        image != null && image.getWidth() == 16 && image.getHeight() == 16);
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        if ((image.getRGB(x, y) >>> 24) != 0) {
                            check("the approved top-label crop never removes actual glyph or arrow pixels",
                                    x >= 2 && x < 14 && y >= 3 && y < 10);
                        }
                    }
                }
            }
        }
    }

    private static void vector(String label, JsonArray actual, double x, double y, double z) {
        eq(label + " X", x, actual.get(0).getAsDouble());
        eq(label + " Y", y, actual.get(1).getAsDouble());
        eq(label + " Z", z, actual.get(2).getAsDouble());
    }

    private static Map<WorkshopStructure.Offset, WorkshopStructure.Present> legacy(WorkshopStructure structure, int turn) {
        Map<WorkshopStructure.Offset, WorkshopStructure.Present> world = new HashMap<>();
        for (WorkshopStructure.Cell cell : structure.cells()) {
            world.put(cell.offset().rotate(turn), new WorkshopStructure.Present(cell.block(), cell.oriented() ? turn : -1));
        }
        return world;
    }

    private static ProcessMachineBlockEntity owner(int amount, int quota) throws Exception {
        ProcessMachineBlockEntity machine = machine(ProcessMachineKind.SETTLING_TROUGH);
        CompoundTag saved = machine.saveWithoutMetadata();
        saved.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, amount)));
        saved.putInt("FluidOutputQuota", quota);
        machine.load(saved);
        return machine;
    }

    private static ProcessMachineBlockEntity consumer() throws Exception {
        ProcessMachineBlockEntity machine = machine(ProcessMachineKind.CHEMICAL_REACTOR);
        CompoundTag saved = machine.saveWithoutMetadata();
        saved.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.HYDROGEN, 1)));
        machine.load(saved);
        return machine;
    }

    private static ProcessMachineBlockEntity machine(ProcessMachineKind kind) throws Exception {
        Constructor<ProcessMachineBlockEntity> constructor = ProcessMachineBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, ProcessMachineKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, kind, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static BasicTankBlockEntity tank() throws Exception {
        Constructor<BasicTankBlockEntity> constructor = BasicTankBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static java.util.Optional<NativeFluidStore.Batch> plan(ProcessMachineBlockEntity consumer,
                                                                  List<FluidInsert.Source> sources, int amount) {
        return FluidInsert.plan(consumer.nativeStore(), List.of(IngredientSpec.fluid(FluidLogic.HYDROGEN, 1),
                IngredientSpec.fluid(FluidLogic.WATER, amount)), sources);
    }

    private static FluidInsert.Source source(FluidEndpoint endpoint, Direction face) {
        return new FluidInsert.Source(face, () -> endpoint);
    }

    /** A role-only fixture over a REAL controller and its one native store, never a second buffer. */
    private static final class QuotaView implements FluidEndpoint {
        private ProcessMachineBlockEntity owner;
        private boolean allowed = true;
        private int visibleLimit = Integer.MAX_VALUE;
        private long revision;

        private QuotaView(ProcessMachineBlockEntity owner) {
            this.owner = owner;
        }

        @Override
        public boolean canInsert(Direction from, FluidState state) {
            return false;
        }

        @Override
        public FluidState insert(Direction from, FluidState state) {
            return state;
        }

        @Override
        public boolean canExtract(Direction from) {
            return allowed && !owner.isRemoved() && (from == Direction.EAST || from == Direction.WEST)
                    && !contents().isEmpty();
        }

        @Override
        public FluidState extract(Direction from, int amount) {
            return canExtract(from) ? owner.extract(null, Math.min(amount, contents().millibuckets())) : FluidState.EMPTY;
        }

        @Override
        public FluidState contents() {
            FluidState produced = owner.workshopProducedFluid();
            return produced.withAmount(Math.min(visibleLimit, produced.millibuckets()));
        }

        @Override
        public int capacity() {
            return owner.capacity();
        }

        @Override
        public NativeFluidStore nativeStore() {
            return owner.nativeStore();
        }

        @Override
        public long nativeAccessRevision() {
            return revision;
        }
    }

    private static void check(String label, boolean passed) {
        checks++;
        if (!passed) {
            throw new AssertionError(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        check(label + ": expected " + expected + ", got " + actual, expected.equals(actual));
    }
}
