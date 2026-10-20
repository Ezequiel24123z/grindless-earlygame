package io.github.ezequiel24123z.grindless.inspection;

import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Energy;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Face;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ItemRole;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Manual;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ManualKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Ownership;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Port;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.PortRole;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.PortSummary;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Position;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Process;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Progress;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Resource;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ResourceKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.State;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.StoredItem;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Structure;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Tank;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Target;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Validity;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Synthetic observations exercise the transport contract, not a played or rendered machine. */
public final class VerifyMachineInspection {

    private static int assertions;
    private static int failures;

    public static void main(String[] args) throws Exception {
        immutableData();
        progress();
        codec();
        malformed();
        targetIdentity();
        check("persisted MachineStatus order is untouched", List.of(MachineStatus.values()).stream()
                .map(Enum::name).toList().equals(List.of("IDLE", "RUNNING", "BLOCKED", "STARVED", "OUT_OF_BAND")));
        System.out.println(assertions + " MACHINE INSPECTION ASSERTIONS; " + failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void immutableData() {
        List<StoredItem> items = new ArrayList<>();
        items.add(new StoredItem(0, ItemRole.INPUT, "minecraft:clay_ball", 2, 0));
        MachineInspectionSnapshot snapshot = snapshot(items, Optional.empty(), List.of(), Optional.empty());
        items.clear();
        check("the caller cannot remove captured contents", snapshot.items().size() == 1);
        rejectsMutation("captured contents are immutable", () -> snapshot.items().clear());
        List<Resource> inputs = new ArrayList<>(List.of(resource()));
        Process process = new Process("synthetic/test", inputs, List.of(), List.of(), 40, 8,
                Optional.of(new Progress(0.5, 40)));
        inputs.clear();
        check("process requirements do not retain the caller's list", process.inputs().size() == 1);
        rejectsMutation("process requirements are immutable", () -> process.inputs().clear());
        List<PortSummary> ports = new ArrayList<>(List.of(new PortSummary(PortRole.ITEM_INPUT, Face.NORTH)));
        Structure structure = new Structure("synthetic_structure", Validity.FORMED, "", ports);
        ports.clear();
        check("formation does not retain the caller's port list", structure.ports().size() == 1);
        rejectsMutation("recognised ports are immutable", () -> structure.ports().clear());
        rejects("oversized item lists are not accepted", () -> snapshot(
                Collections.nCopies(MachineInspectionSnapshot.MAX_ITEMS + 1,
                        new StoredItem(0, ItemRole.INPUT, "minecraft:stone", 1, 0)),
                Optional.empty(), List.of(), Optional.empty()));
        rejects("duplicate item slots are not accepted", () -> snapshot(
                List.of(new StoredItem(0, ItemRole.INPUT, "minecraft:stone", 1, 0),
                        new StoredItem(0, ItemRole.INPUT, "minecraft:dirt", 1, 0)),
                Optional.empty(), List.of(), Optional.empty()));
        rejects("oversized process lists are not accepted", () -> new Process("synthetic/test",
                Collections.nCopies(MachineInspectionSnapshot.MAX_RESOURCES + 1, resource()),
                List.of(), List.of(), 40, 8, Optional.empty()));
        rejects("oversized port lists are not accepted", () -> new Structure("synthetic_structure",
                Validity.FORMED, "", Collections.nCopies(MachineInspectionSnapshot.MAX_PORTS + 1,
                new PortSummary(PortRole.ITEM_INPUT, Face.NORTH))));
        rejects("negative energy is rejected", () -> new Energy(-1, 100));
        rejects("energy above capacity is rejected", () -> new Energy(101, 100));
        rejects("tank overflow is rejected", () -> new Tank("minecraft:water", 1001, 1000,
                Optional.empty(), 20, 0.1));
        rejects("a nonempty tank needs an actual identity", () -> new Tank("", 1, 1000,
                Optional.empty(), 20, 0.1));
        rejects("reserved output cannot exceed actual volume", () -> new Tank("minecraft:water", 100, 1000,
                Optional.of(101), 20, 0.1));
        rejects("a claimed owner needs its position", () -> new Port(PortRole.FLUID_OUTPUT, Face.NORTH,
                Ownership.CONFIRMED, Optional.empty()));
        rejects("an orphan cannot invent an owner", () -> new Port(PortRole.FLUID_OUTPUT, Face.NORTH,
                Ownership.ORPHAN, Optional.of(new Position(0, 0, 0))));
        rejects("formatting/control text cannot enter the DTO", () ->
                new Process("synthetic/\u00a7cspoof", List.of(), List.of(), List.of(), 1, 0, Optional.empty()));
        rejects("registry identities are not arbitrary prose", () ->
                new StoredItem(0, ItemRole.INPUT, "not an item", 1, 0));
    }

    private static void progress() {
        check("fractional native work is preserved", new Progress(0.5, 40).completedWork() == 0.5);
        check("percentage uses native work, not an unrelated menu field", new Progress(0.5, 40).percent() == 1);
        check("zero work remains zero", new Progress(0, 40).percent() == 0);
        check("a completed cycle is exactly complete", new Progress(40, 40).percent() == 100);
        check("a nearly complete cycle is not rounded to complete", new Progress(39.999, 40).percent() == 99);
        check("remaining nominal work is labelled separately from ETA",
                new Progress(10, 40).nominalSecondsRemaining() == 1.5);
        check("large work totals do not overflow integer multiplication",
                new Progress(Integer.MAX_VALUE / 2.0, Integer.MAX_VALUE).percent() == 50);
        rejects("NaN native work is not fabricated as zero", () -> new Progress(Double.NaN, 40));
        rejects("infinite work is not fabricated as complete", () -> new Progress(Double.POSITIVE_INFINITY, 40));
        rejects("negative work is rejected", () -> new Progress(-1, 40));
        rejects("zero duration is rejected", () -> new Progress(0, 0));
        rejects("work above the declared cycle is rejected", () -> new Progress(41, 40));
        rejects("progress cannot borrow a different process duration", () ->
                new Process("synthetic/test", List.of(), List.of(), List.of(), 40, 8,
                        Optional.of(new Progress(0.5, 60))));
    }

    private static void codec() throws Exception {
        MachineInspectionSnapshot full = fullSnapshot();
        CompoundTag encoded = MachineInspectionCodec.encode(full);
        check("all primitive facets round-trip through the native NBT codec",
                MachineInspectionCodec.decode(encoded).equals(full));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.write(encoded, new DataOutputStream(bytes));
        CompoundTag restored = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        check("binary NBT keeps the same observation", MachineInspectionCodec.decode(restored).equals(full));
        check("a representative observation remains small", bytes.size() < 4096);
        MachineInspectionSnapshot absent = snapshot(List.of(), Optional.empty(), List.of(), Optional.empty());
        check("absent process and progress remain absent", MachineInspectionCodec.decode(
                MachineInspectionCodec.encode(absent)).process().isEmpty());
        check("missing authority is explicit UNKNOWN, not a fabricated idle machine", absent.state() == State.UNKNOWN);
        check("no energy/tank/manual authority is invented", absent.energy().isEmpty() && absent.tanks().isEmpty()
                && absent.manual().isEmpty());
        check("observations contain no class names, world references, ItemStack or saved work tags",
                !encoded.toString().contains("WorkRecipe") && !encoded.toString().contains("Items")
                        && !encoded.toString().contains("BlockEntity"));
        for (State state : State.values()) {
            CompoundTag variant = encoded.copy();
            variant.putString("state", state.name());
            check(state + " is encoded by name, not an ordinal",
                    MachineInspectionCodec.decode(variant).state() == state);
            check(state + " has a non-colour status symbol", !state.symbol().isBlank());
        }
    }

    private static void malformed() {
        mutation("a missing schema is rejected", tag -> tag.remove("schema"));
        mutation("an unknown schema is rejected", tag -> tag.putInt("schema", 2));
        mutation("a schema with the wrong numeric type is rejected", tag -> tag.putLong("schema", 1));
        mutation("a missing target is rejected", tag -> tag.remove("target"));
        mutation("target coordinates cannot be defaulted to zero", tag -> tag.getCompound("target").remove("x"));
        mutation("unknown states cannot become idle", tag -> tag.putString("state", "FUTURE_STATE"));
        mutation("state ordinals cannot be mistaken for names", tag -> tag.putInt("state", 0));
        mutation("an omitted held flag is rejected", tag -> tag.remove("held"));
        mutation("nonboolean byte values are rejected", tag -> tag.putByte("held", (byte) 7));
        mutation("an omitted contents list is rejected", tag -> tag.remove("items"));
        mutation("a contents list of strings is rejected", tag -> {
            ListTag values = new ListTag();
            values.add(StringTag.valueOf("not an item"));
            tag.put("items", values);
        });
        mutation("oversized contents are rejected before mapping", tag -> {
            ListTag values = tag.getList("items", 10);
            CompoundTag item = values.getCompound(0).copy();
            while (values.size() <= MachineInspectionSnapshot.MAX_ITEMS) {
                values.add(item.copy());
            }
        });
        mutation("partial items do not become empty slots", tag -> tag.getList("items", 10).getCompound(0).remove("id"));
        mutation("unknown resource kinds are rejected", tag -> tag.getCompound("process")
                .getList("inputs", 10).getCompound(0).putString("kind", "ANYTHING"));
        mutation("negative requirements are rejected", tag -> tag.getCompound("process")
                .getList("inputs", 10).getCompound(0).putInt("count", -1));
        mutation("NaN work is rejected", tag -> tag.getCompound("process").putDouble("work", Double.NaN));
        mutation("a wrong-type optional process is rejected", tag -> tag.putString("process", "wrong"));
        mutation("a wrong-type optional energy is rejected", tag -> tag.putInt("energy", 0));
        mutation("negative stored energy is rejected", tag -> tag.getCompound("energy").putLong("stored", -1));
        mutation("a wrong-type tank identity is rejected", tag -> tag.getList("tanks", 10).getCompound(0).putInt("id", 1));
        mutation("nonfinite fluid temperature is rejected", tag -> tag.getList("tanks", 10).getCompound(0)
                .putDouble("temperature", Double.NEGATIVE_INFINITY));
        mutation("unknown port roles are rejected", tag -> tag.getCompound("port").putString("role", "FAKE"));
        mutation("a confirmed owner cannot omit its position", tag -> {
            tag.getCompound("port").putString("ownership", "CONFIRMED");
            tag.getCompound("port").remove("owner");
        });
        mutation("unexpected saved machine internals are rejected", tag -> tag.putString("WorkRecipe", "not transport"));
        mutation("unexpected nested data is rejected", tag -> tag.getCompound("energy").putInt("secret", 1));
        mutation("oversized IDs are rejected", tag -> tag.putString("block", "grindless:" + "x".repeat(161)));
        mutation("spoofed translation formatting is rejected", tag -> tag.putString("reason", "inspection.\u00a7cspoof"));
        mutation("oversized ports are rejected before mapping", tag -> {
            ListTag values = tag.getCompound("structure").getList("ports", 10);
            CompoundTag port = values.getCompound(0).copy();
            while (values.size() <= MachineInspectionSnapshot.MAX_PORTS) {
                values.add(port.copy());
            }
        });
    }

    private static void targetIdentity() {
        MachineInspectionSnapshot snapshot = fullSnapshot();
        check("the exact target matches", snapshot.describes("minecraft:overworld", new Position(1, 64, 2),
                "grindless:clay_mixer"));
        check("a reply from another block cannot be shown", !snapshot.describes(
                "minecraft:overworld", new Position(2, 64, 2), "grindless:clay_mixer"));
        check("a reply from another dimension cannot be shown", !snapshot.describes(
                "minecraft:the_nether", new Position(1, 64, 2), "grindless:clay_mixer"));
        check("a replaced block cannot inherit an old reply", !snapshot.describes(
                "minecraft:overworld", new Position(1, 64, 2), "grindless:quern"));
    }

    private static MachineInspectionSnapshot fullSnapshot() {
        Process process = new Process("synthetic/test", List.of(resource()),
                List.of(new Resource(ResourceKind.ITEM, "grindless:unfired_ceramic", 3, 0, false)),
                List.of(), 40, 8, Optional.of(new Progress(0.5, 40)));
        return new MachineInspectionSnapshot(new Target("minecraft:overworld", new Position(1, 64, 2), 123),
                "grindless:clay_mixer", State.WORKING, "", false, Optional.of(process),
                List.of(new StoredItem(0, ItemRole.INPUT, "minecraft:clay_ball", 2, 0)),
                Optional.of(new Energy(64, 320)), List.of(new Tank("minecraft:water", 250, 1000,
                Optional.of(50), 20, 0.1)), Optional.of(new Manual(ManualKind.CRANK_HANDLE, 64)),
                Optional.of(new Structure("synthetic_structure", Validity.FORMED, "",
                        List.of(new PortSummary(PortRole.ITEM_INPUT, Face.NORTH)))),
                Optional.of(new Port(PortRole.ITEM_INPUT, Face.NORTH, Ownership.CANDIDATE,
                        Optional.of(new Position(0, 64, 2)))), false);
    }

    private static MachineInspectionSnapshot snapshot(List<StoredItem> items, Optional<Process> process,
                                                     List<Tank> tanks, Optional<Port> port) {
        return new MachineInspectionSnapshot(new Target("minecraft:overworld", new Position(1, 64, 2), 123),
                "grindless:clay_mixer", State.UNKNOWN, "", false, process, items, Optional.empty(), tanks,
                Optional.empty(), Optional.empty(), port, false);
    }

    private static Resource resource() {
        return new Resource(ResourceKind.ITEM, "minecraft:clay_ball", 2, 0, false);
    }

    private static void mutation(String name, Consumer<CompoundTag> mutation) {
        CompoundTag tag = MachineInspectionCodec.encode(fullSnapshot());
        mutation.accept(tag);
        rejects(name, () -> MachineInspectionCodec.decode(tag));
    }

    private static void rejects(String name, Runnable operation) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(name, rejected);
    }

    private static void rejectsMutation(String name, Runnable operation) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (UnsupportedOperationException expected) {
            rejected = true;
        }
        check(name, rejected);
    }

    private static void check(String name, boolean passed) {
        assertions++;
        if (!passed) {
            failures++;
            System.err.println("FAIL: " + name);
        }
    }
}
