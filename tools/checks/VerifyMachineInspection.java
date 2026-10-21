package io.github.ezequiel24123z.grindless.inspection;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Energy;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Conditions;
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
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.SiteRequirement;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.StoredItem;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Structure;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Tank;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Target;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Validity;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.process.ConditionFault;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortContract;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.ClientState;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Reply;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.ReplyKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Request;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.RequestLimiter;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        pressure();
        malformed();
        targetIdentity();
        packets();
        clientLifecycle();
        targetingAndRate();
        portProjection();
        bindings();
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
        rejects("a formed structure cannot claim a failing cell", () -> new Structure("synthetic_structure",
                Validity.FORMED, "", List.of(), Optional.of(new Position(1, 64, 2))));
        rejects("nonfinite held conditions remain an error", () -> new Conditions(Double.NaN, 0.1,
                "AIR", "STATIC", Optional.empty(), Optional.empty(), Optional.empty()));
        rejects("nonfinite required conditions remain an error", () -> new Conditions(20, 0.1,
                "AIR", "STATIC", Optional.of(Double.POSITIVE_INFINITY), Optional.empty(), Optional.empty()));
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
        List<Face> faces = new ArrayList<>(List.of(Face.NORTH, Face.EAST));
        Port port = new Port(PortRole.ITEM_INPUT, Face.NORTH, Ownership.CONFIRMED,
                Optional.of(new Position(0, 64, 0)), faces);
        faces.clear();
        check("port access directions do not retain mutable input lists", port.accessFaces().size() == 2);
        rejectsMutation("port access directions are immutable", () -> port.accessFaces().clear());
        rejects("duplicate transfer directions are invalid", () ->
                new PortSummary(PortRole.ITEM_INPUT, Face.NORTH, List.of(Face.NORTH, Face.NORTH)));
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
        mutation("an unknown schema is rejected", tag -> tag.putInt("schema", MachineInspectionCodec.VERSION + 1));
        mutation("an older unsupported schema is rejected", tag -> tag.putInt("schema", 0));
        mutation("a schema with the wrong numeric type is rejected", tag -> tag.putLong("schema", 1));
        mutation("a missing target is rejected", tag -> tag.remove("target"));
        mutation("target coordinates cannot be defaulted to zero", tag -> tag.getCompound("target").remove("x"));
        mutation("unknown states cannot become idle", tag -> tag.putString("state", "FUTURE_STATE"));
        mutation("state ordinals cannot be mistaken for names", tag -> tag.putInt("state", 0));
        mutation("an omitted held flag is rejected", tag -> tag.remove("held"));
        mutation("placement requirements cannot be omitted as if there were none", tag -> tag.remove("requirements"));
        mutation("unknown placement requirements are rejected",
                tag -> tag.getList("requirements", 10).getCompound(0).putString("kind", "FAKE_SITE"));
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
        mutation("a wrong-type condition block is rejected", tag -> tag.getCompound("process")
                .putString("conditions", "wrong"));
        mutation("partial held conditions cannot become ambient defaults", tag -> tag.getCompound("process")
                .getCompound("conditions").remove("temperature"));
        mutation("a required pressure cannot omit its band", tag -> tag.getCompound("process")
                .getCompound("conditions").putDouble("required_pressure", 0.0));
        mutation("a pressure band cannot omit its required optimum", tag -> tag.getCompound("process")
                .getCompound("conditions").putDouble("required_pressure_half_width", 0.001));
        mutation("a pressure requirement has an exact double wire type", tag -> {
            pressureFields(tag, 0.0, 0.001);
            tag.getCompound("process").getCompound("conditions").putInt("required_pressure", 0);
        });
        mutation("a pressure half-width has an exact double wire type", tag -> {
            pressureFields(tag, 0.0, 0.001);
            tag.getCompound("process").getCompound("conditions").putString("required_pressure_half_width", "0.001");
        });
        mutation("an inspection cannot use v1 to hide or strip a new pressure requirement", tag -> {
            pressureFields(tag, 0.0, 0.001);
            tag.putInt("schema", 1);
        });
        mutation("a wrong-type optional process is rejected", tag -> tag.putString("process", "wrong"));
        mutation("a wrong-type optional energy is rejected", tag -> tag.putInt("energy", 0));
        mutation("negative stored energy is rejected", tag -> tag.getCompound("energy").putLong("stored", -1));
        mutation("a wrong-type tank identity is rejected", tag -> tag.getList("tanks", 10).getCompound(0).putInt("id", 1));
        mutation("nonfinite fluid temperature is rejected", tag -> tag.getList("tanks", 10).getCompound(0)
                .putDouble("temperature", Double.NEGATIVE_INFINITY));
        mutation("unknown port roles are rejected", tag -> tag.getCompound("port").putString("role", "FAKE"));
        mutation("port access directions cannot silently disappear", tag -> tag.getCompound("port").remove("access"));
        mutation("unknown access directions are rejected", tag -> {
            ListTag faces = new ListTag();
            CompoundTag face = new CompoundTag();
            face.putString("face", "SIDEWAYS");
            faces.add(face);
            tag.getCompound("port").put("access", faces);
        });
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

    private static void pressure() throws Exception {
        Conditions legacy = new Conditions(20, 0.1, "AIR", "STATIC",
                Optional.empty(), Optional.empty(), Optional.empty());
        check("the old conditions constructor invents no ambient requirement",
                legacy.requiredPressureMPa().isEmpty() && legacy.requiredPressureHalfWidthMPa().isEmpty());
        CompoundTag old = MachineInspectionCodec.encode(fullSnapshot());
        check("new writers explicitly identify schema v2", old.getInt("schema") == 2);
        check("an unnamed requirement has no pressure wire fields",
                !old.getCompound("process").getCompound("conditions").contains("required_pressure")
                        && !old.getCompound("process").getCompound("conditions").contains("required_pressure_half_width"));
        old.putInt("schema", 1);
        check("strict legacy payloads remain readable without inventing pressure",
                MachineInspectionCodec.decode(old).equals(fullSnapshot()));
        check("legacy observations are written back only as the current version",
                MachineInspectionCodec.encode(MachineInspectionCodec.decode(old)).getInt("schema")
                        == MachineInspectionCodec.VERSION);
        Conditions absent = MachineInspectionCodec.decode(old).process().orElseThrow().conditions().orElseThrow();
        check("a legacy payload's held pressure never becomes a required pressure",
                absent.pressureMPa() == 0.1 && absent.requiredPressureMPa().isEmpty()
                        && absent.requiredPressureHalfWidthMPa().isEmpty());
        CompoundTag oldAbsent = MachineInspectionCodec.encode(
                snapshot(List.of(), Optional.empty(), List.of(), Optional.empty()));
        oldAbsent.putInt("schema", 1);
        check("legacy absence of process authority remains absent",
                MachineInspectionCodec.decode(oldAbsent).process().isEmpty());
        for (double optimum : new double[] {0.0, 1.25, 0.000001}) {
            MachineInspectionSnapshot named = pressureSnapshot(optimum, 0.0000001);
            CompoundTag encoded = MachineInspectionCodec.encode(named);
            check("a named pressure and explicit band round-trip " + optimum,
                    MachineInspectionCodec.decode(encoded).equals(named));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.write(encoded, new DataOutputStream(bytes));
            CompoundTag binary = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            check("binary NBT retains exact pressure units and precision " + optimum,
                    MachineInspectionCodec.decode(binary).equals(named));
            Conditions conditions = named.process().orElseThrow().conditions().orElseThrow();
            check("held and required pressure remain distinct " + optimum, conditions.pressureMPa() == 0.1
                    && conditions.requiredPressureMPa().orElseThrow() == optimum
                    && conditions.requiredPressureHalfWidthMPa().orElseThrow() == 0.0000001);
        }
        for (double optimum : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            rejects("invalid inspected pressure optimum " + optimum, () -> pressureSnapshot(optimum, 0.001));
        }
        for (double width : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            rejects("invalid inspected pressure width " + width, () -> pressureSnapshot(0.0, width));
        }
        rejects("overflowed inspected pressure bands are rejected",
                () -> pressureSnapshot(Double.MAX_VALUE, Double.MAX_VALUE));
        rejects("partial pressure requirements cannot enter a snapshot",
                () -> new Conditions(20, 0.1, "AIR", "STATIC", Optional.empty(),
                        Optional.of(0.0), Optional.empty(), Optional.empty(), Optional.empty()));
        rejects("orphaned pressure bands cannot enter a snapshot",
                () -> new Conditions(20, 0.1, "AIR", "STATIC", Optional.empty(),
                        Optional.empty(), Optional.of(0.001), Optional.empty(), Optional.empty()));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            Reply reply = new Reply(1, ReplyKind.AVAILABLE, Optional.of(pressureSnapshot(0.0, 0.001)));
            MachineInspectionProtocol.encodeReply(reply, buffer);
            check("pressure survives the existing bounded reply envelope",
                    MachineInspectionProtocol.decodeReply(buffer).equals(reply));
            buffer.clear();
            buffer.writeVarInt(2);
            buffer.writeByte(0);
            buffer.writeNbt(old);
            check("the existing reply protocol accepts strict legacy pressure absence",
                    MachineInspectionProtocol.decodeReply(buffer).snapshot().orElseThrow().equals(fullSnapshot()));
            for (int version : new int[] {1, MachineInspectionCodec.VERSION + 1}) {
                buffer.clear();
                buffer.writeVarInt(3);
                buffer.writeByte(0);
                CompoundTag incompatible = MachineInspectionCodec.encode(pressureSnapshot(0.0, 0.001));
                incompatible.putInt("schema", version);
                buffer.writeNbt(incompatible);
                Reply rejected = MachineInspectionProtocol.decodeReply(buffer);
                check("incompatible pressure payloads are explicitly MALFORMED, never success " + version,
                        rejected.kind() == ReplyKind.MALFORMED && rejected.snapshot().isEmpty());
            }
        } finally {
            buffer.release();
        }
    }

    private static MachineInspectionSnapshot pressureSnapshot(double optimum, double halfWidth) {
        Process process = new Process("synthetic/pressure", List.of(), List.of(), List.of(), 40, 8,
                Optional.of(new Progress(0.5, 40)),
                Optional.of(new Conditions(20, 0.1, "AIR", "STATIC", Optional.empty(),
                        Optional.of(optimum), Optional.of(halfWidth), Optional.empty(), Optional.empty())));
        return snapshot(List.of(), Optional.of(process), List.of(), Optional.empty());
    }

    private static void pressureFields(CompoundTag tag, double optimum, double halfWidth) {
        CompoundTag conditions = tag.getCompound("process").getCompound("conditions");
        conditions.putDouble("required_pressure", optimum);
        conditions.putDouble("required_pressure_half_width", halfWidth);
    }

    private static void packets() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            Request request = new Request(Integer.MAX_VALUE, new Position(-17, -64, 300));
            MachineInspectionProtocol.encodeRequest(request, buffer);
            check("the largest request identity fits the exact fixed request bound",
                    buffer.readableBytes() == MachineInspectionProtocol.MAX_REQUEST_BYTES);
            check("request position and identity round-trip", MachineInspectionProtocol.decodeRequest(buffer).equals(request));
            buffer.clear();
            MachineInspectionProtocol.encodeRequest(request, buffer);
            buffer.writeByte(1);
            rejects("oversized requests are rejected before a world query",
                    () -> MachineInspectionProtocol.decodeRequest(buffer));
            buffer.clear();
            MachineInspectionProtocol.encodeRequest(new Request(1, new Position(1, 64, 2)), buffer);
            buffer.writeByte(1);
            rejects("even a within-size request cannot hide trailing data",
                    () -> MachineInspectionProtocol.decodeRequest(buffer));
            for (ReplyKind kind : ReplyKind.values()) {
                buffer.clear();
                Reply reply = new Reply(1, kind, kind == ReplyKind.AVAILABLE
                        ? Optional.of(fullSnapshot()) : Optional.empty());
                MachineInspectionProtocol.encodeReply(reply, buffer);
                check(kind + " reply round-trips without optional mod APIs",
                        MachineInspectionProtocol.decodeReply(buffer).equals(reply));
            }
            buffer.clear();
            buffer.writeVarInt(1);
            buffer.writeByte(0);
            CompoundTag invalid = MachineInspectionCodec.encode(fullSnapshot());
            invalid.remove("state");
            buffer.writeNbt(invalid);
            Reply malformed = MachineInspectionProtocol.decodeReply(buffer);
            check("malformed structured data becomes an explicit error, not an idle observation",
                    malformed.kind() == ReplyKind.MALFORMED && malformed.snapshot().isEmpty());
            buffer.clear();
            buffer.writeVarInt(1);
            buffer.writeByte(0);
            buffer.writeNbt(null);
            check("absent successful NBT is explicitly malformed",
                    MachineInspectionProtocol.decodeReply(buffer).kind() == ReplyKind.MALFORMED);
            buffer.clear();
            buffer.writeZero(MachineInspectionProtocol.MAX_REPLY_BYTES + 1);
            rejects("oversized replies are rejected before NBT decoding",
                    () -> MachineInspectionProtocol.decodeReply(buffer));
            buffer.clear();
            buffer.writeVarInt(1);
            buffer.writeByte(127);
            rejects("unknown reply headers are not accepted", () -> MachineInspectionProtocol.decodeReply(buffer));
            buffer.clear();
            MachineInspectionProtocol.encodeReply(new Reply(1, ReplyKind.UNAVAILABLE, Optional.empty()), buffer);
            buffer.writeByte(1);
            rejects("replies cannot smuggle trailing payloads", () -> MachineInspectionProtocol.decodeReply(buffer));
            buffer.clear();
            MachineInspectionProtocol.encodeReply(new Reply(Integer.MAX_VALUE, ReplyKind.AVAILABLE,
                    Optional.of(maximalSnapshot())), buffer);
            check("all DTO cardinality/text maxima fit the native packet budget",
                    buffer.readableBytes() <= MachineInspectionProtocol.MAX_REPLY_BYTES);
            check("maximal valid data remains readable", MachineInspectionProtocol.decodeReply(buffer)
                    .snapshot().orElseThrow().equals(maximalSnapshot()));
        } finally {
            buffer.release();
        }
        rejects("negative request identities are invalid", () -> new Request(-1, new Position(0, 0, 0)));
        rejects("an available reply cannot omit its observation",
                () -> new Reply(1, ReplyKind.AVAILABLE, Optional.empty()));
        rejects("an unavailable reply cannot carry successful data",
                () -> new Reply(1, ReplyKind.UNAVAILABLE, Optional.of(fullSnapshot())));
    }

    private static void clientLifecycle() {
        ClientState state = new ClientState();
        var target = new MachineInspectionProtocol.Target("minecraft:overworld",
                new Position(1, 64, 2), "grindless:clay_mixer");
        int first = state.request(target, 0).orElseThrow();
        check("no observation is fabricated while waiting", state.snapshot(0).isEmpty());
        check("a matching reply is accepted", state.receive(
                new Reply(first, ReplyKind.AVAILABLE, Optional.of(fullSnapshot())), 1));
        check("queries cannot be sent every tick", state.request(target, 1).isEmpty());
        check("native fractional work is shown unchanged",
                state.snapshot(1).orElseThrow().process().orElseThrow().progress().orElseThrow().completedWork() == 0.5);
        check("client time never predicts progress",
                state.snapshot(30).orElseThrow().process().orElseThrow().progress().orElseThrow().completedWork() == 0.5);
        check("old data expires rather than continuing a local animation", state.snapshot(42).isEmpty());
        int second = state.request(target, 43).orElseThrow();
        check("late replies cannot overwrite the latest request", !state.receive(
                new Reply(first, ReplyKind.AVAILABLE, Optional.of(fullSnapshot())), 44));
        check("a current unavailable response clears old success",
                state.receive(new Reply(second, ReplyKind.UNAVAILABLE, Optional.empty()), 44)
                        && state.snapshot(44).isEmpty() && state.kind() == ReplyKind.UNAVAILABLE);
        var other = new MachineInspectionProtocol.Target("minecraft:overworld",
                new Position(2, 64, 2), "grindless:clay_mixer");
        check("rapid target changes do not bypass the global client interval", state.request(other, 45).isEmpty());
        check("the previous target is discarded immediately", state.snapshot(45).isEmpty() && state.target().equals(other));
        int moved = state.request(other, 53).orElseThrow();
        check("a same-kind machine at another position cannot inherit data", !state.receive(
                new Reply(moved, ReplyKind.AVAILABLE, Optional.of(fullSnapshot())), 54) && state.snapshot(54).isEmpty());
        int pending = state.request(target, 63).orElseThrow();
        check("one outstanding request is not replaced at every refresh interval",
                state.request(target, 73).isEmpty() && state.request(target, 103).isEmpty());
        check("responses older than the request window cannot refresh the cache",
                !state.receive(new Reply(pending, ReplyKind.AVAILABLE, Optional.of(fullSnapshot())), 104));
        int fresh = state.request(target, 105).orElseThrow();
        check("malformed data is explicitly retained as an error state", state.receive(
                new Reply(fresh, ReplyKind.MALFORMED, Optional.empty()), 106)
                && state.kind() == ReplyKind.MALFORMED && state.snapshot(106).isEmpty());
        state.reset();
        int newScope = state.request(target, 0).orElseThrow();
        check("world/disconnect reset does not reuse the previous nonce", newScope != fresh);
        check("an old-world reply is ignored even at identical coordinates", !state.receive(
                new Reply(fresh, ReplyKind.AVAILABLE, Optional.of(fullSnapshot())), 1));
        check("looking away clears the target and pending response", state.request(null, 1).isEmpty()
                && state.target() == null && state.snapshot(1).isEmpty());
    }

    private static void targetingAndRate() {
        Position block = new Position(0, 0, 0);
        check("the preliminary bound admits a block face exactly six blocks away",
                MachineInspectionProtocol.withinDistance(block, 0.5, 0.5, -6));
        check("the target centre is not a false reach proxy",
                MachineInspectionProtocol.withinDistance(block, 7, 0.5, 0.5));
        check("a face beyond the exact distance is rejected",
                !MachineInspectionProtocol.withinDistance(block, 0.5, 0.5, -6.001));
        check("diagonal distance is measured in three dimensions",
                !MachineInspectionProtocol.withinDistance(block, 6, 6, 0.5));
        check("an eye inside the target has zero minimum distance",
                MachineInspectionProtocol.withinDistance(block, 0.5, 0.5, 0.5));
        check("nonfinite coordinates are invalid", !MachineInspectionProtocol.withinDistance(block,
                Double.NaN, 0.5, 0.5));
        Vec3 eye = Vec3.ZERO;
        check("the actual native hit point exactly at the cap is allowed",
                MachineInspectionProtocol.withinHitDistance(eye.distanceToSqr(
                        new Vec3(MachineInspectionProtocol.MAX_DISTANCE, 0, 0))));
        check("the very next representable point beyond the cap is rejected",
                !MachineInspectionProtocol.withinHitDistance(eye.distanceToSqr(
                        new Vec3(Math.nextUp(MachineInspectionProtocol.MAX_DISTANCE), 0, 0))));
        check("a near block cannot authorize an over-range hit on its far face",
                MachineInspectionProtocol.withinDistance(new Position(5, 0, 0), 0, 0, 0)
                        && !MachineInspectionProtocol.withinHitDistance(eye.distanceToSqr(new Vec3(6, 1, 0))));
        check("nonfinite actual hit distance is rejected",
                !MachineInspectionProtocol.withinHitDistance(Double.NaN)
                        && !MachineInspectionProtocol.withinHitDistance(Double.POSITIVE_INFINITY));
        check("negative hit distance is invalid", !MachineInspectionProtocol.withinHitDistance(-1));
        RequestLimiter limiter = new RequestLimiter();
        check("the first request is admitted", limiter.admit(0));
        check("same-time replay cannot inspect again", !limiter.admit(0));
        check("a pre-boundary request is rejected", !limiter.admit(
                MachineInspectionProtocol.SERVER_INTERVAL_NANOS - 1));
        check("the exact rate boundary is admitted", limiter.admit(MachineInspectionProtocol.SERVER_INTERVAL_NANOS));
        check("rejected requests do not advance the rate window", limiter.admit(
                MachineInspectionProtocol.SERVER_INTERVAL_NANOS * 2));
        RequestLimiter negativeClock = new RequestLimiter();
        check("monotonic clocks need not start at a positive epoch", negativeClock.admit(-1_000_000_000L)
                && !negativeClock.admit(-999_999_999L)
                && negativeClock.admit(-1_000_000_000L + MachineInspectionProtocol.SERVER_INTERVAL_NANOS));
    }

    private static void portProjection() {
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            for (WorkshopPortContract.Slot slot : WorkshopPortContract.slots(kind.structure())) {
                if (!slot.accepts(kind)) {
                    continue;
                }
                List<Direction> orientations = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
                for (int turns = 0; turns < orientations.size(); turns++) {
                    List<Face> faces = MachineInspection.portFaces(kind.structure(), slot.offset().rotate(turns),
                            kind, orientations.get(turns));
                    check(kind + " orientation " + turns + " exposes only declared horizontal contract faces",
                            !faces.isEmpty() && !faces.contains(Face.UP) && !faces.contains(Face.DOWN)
                                    && faces.size() == slot.faces().size());
                    for (int face = 0; face < orientations.size(); face++) {
                        check(kind + " face " + face + " matches native permission at rotation " + turns,
                                faces.contains(Face.valueOf(orientations.get(face).name()))
                                        == slot.permitsFace(turns, face));
                    }
                }
            }
        }
        check("a north-facing firing port can transfer north and east, not just its facing",
                MachineInspection.portFaces(WorkshopStructure.FIRING_CHAMBER,
                        new WorkshopStructure.Offset(1, 0, 0), WorkshopPortContract.Kind.WORKSHOP_ITEM_INPUT,
                        Direction.NORTH).equals(List.of(Face.NORTH, Face.EAST)));
        check("a settling channel uses side faces, not its north orientation",
                MachineInspection.portFaces(WorkshopStructure.SETTLING_TROUGH,
                        new WorkshopStructure.Offset(0, 0, 1), WorkshopPortContract.Kind.SETTLING_FLUID_INPUT,
                        Direction.NORTH).equals(List.of(Face.EAST, Face.WEST)));
        rejects("an arbitrary cell cannot invent a contract face", () -> MachineInspection.portFaces(
                WorkshopStructure.FIRING_CHAMBER, new WorkshopStructure.Offset(99, 0, 0),
                WorkshopPortContract.Kind.WORKSHOP_ITEM_INPUT, Direction.NORTH));
        for (WorkshopStructure structure : WorkshopStructure.values()) {
            check(structure + " port cardinality fits the inspection bound",
                    WorkshopPortContract.maximumPorts(structure) <= MachineInspectionSnapshot.MAX_PORTS);
        }
    }

    private static void bindings() throws Exception {
        Path common = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
        Path forge = Path.of("forge/src/main/java/io/github/ezequiel24123z/grindless/forge");
        String query = Files.readString(common.resolve("inspection/MachineInspection.java"));
        String network = Files.readString(forge.resolve("overlay/MachineInspectionNetwork.java"));
        String client = Files.readString(forge.resolve("overlay/MachineInspectionOverlayClient.java"));
        String config = Files.readString(forge.resolve("overlay/MachineInspectionOverlay.java"));
        String panel = Files.readString(common.resolve("client/inspection/MachineInspectionPanel.java"));
        String tooltip = Files.readString(common.resolve("client/inspection/MachineInspectionTooltip.java"));
        check("the real query guards loading before reading any target state or block entity",
                query.indexOf("if (!level.hasChunkAt(position))") >= 0
                        && query.indexOf("if (!level.hasChunkAt(position))") < query.indexOf("level.getBlockState(position)")
                        && query.indexOf("if (!level.hasChunkAt(position))") < query.indexOf("level.getBlockEntity(position)"));
        check("a removed or wrong-type entity cannot masquerade as the replaced target",
                query.contains("entity.isRemoved() || !entity.getType().isValid(block)")
                        && query.indexOf("!entity.getType().isValid(block)") < query.indexOf("MachineStatus.of(block)"));
        check("inspection does not select recipes, wake machines or serialize their private state",
                !query.contains("ProcessLookup.find(") && !query.contains("updateSubscriptions(")
                        && !query.contains("saveWithoutMetadata(") && !query.contains("drawPower(")
                        && !query.contains("VeinData.get("));
        check("native recipe getters, not menu progress proxies, supply process and calibration work",
                query.contains("machine.inspectionProgress()") && query.contains("terminal.inspectionProgress()")
                        && query.contains("extractor.inspectionProgress()"));
        check("inspection names pressure only when the selected recipe names it",
                query.contains("recipe.namesPressure() ? Optional.of(recipe.pressureMPa()) : Optional.empty()")
                        && query.contains("recipe.namesPressure() ? Optional.of(recipe.pressureHalfWidthMPa())"
                        + " : Optional.empty()"));
        check("the tooltip preserves precise required and held pressure rather than rounding to ambient or zero",
                tooltip.contains("conditions.requiredPressureMPa().ifPresent(pressure")
                        && tooltip.contains("\"inspection.grindless.required_pressure\", Double.toString(pressure)")
                        && tooltip.contains("Double.toString(conditions.requiredPressureHalfWidthMPa().orElseThrow())")
                        && tooltip.contains("Double.toString(conditions.pressureMPa())"));
        check("one fresh formation observation is reused for header and details",
                query.contains("Optional<Structure> formation = structure(entity)")
                        && query.contains("display(entity, reported, formation)")
                        && !query.contains("process.inspectionStructureFault()"));
        check("port handling precedes generic mutable capability/storage access",
                query.indexOf("return Optional.of(port(level, position, blockId, port))") < query.indexOf("items(entity)")
                        && query.contains("WorkshopPortBlockEntity.Inspection inspection = entity.inspection()")
                        && !query.contains("entity.permitsFace(") && !query.contains("entity.contents(")
                        && !query.contains("entity.getItem("));
        check("port stores are read only from an actual loaded confirmed controller",
                query.contains("level.hasChunkAt(inspection.ownerPos())")
                        && query.contains("level.getBlockEntity(inspection.ownerPos()) instanceof ProcessMachineBlockEntity")
                        && query.contains("owner.workshopProducedFluid()")
                        && query.contains("owner.getItem(slot)"));
        check("the first native failing offset is not rotated twice",
                query.contains("Optional.ofNullable(inspection.offset())")
                        && query.contains("process.getBlockPos().getX() + offset.x()"));
        check("multiple access faces derive from the native geometry/role contract",
                query.contains("slot.permitsFace(turns, turns(direction))")
                        && query.contains("candidate.offset().rotate(turns).equals(rotatedOffset)"));
        check("the server rate guard precedes its world reads", network.indexOf(".admit(System.nanoTime())")
                < network.indexOf("sender.serverLevel()"));
        check("loaded target and bounded distance are checked before the authoritative ray",
                network.indexOf("!level.hasChunkAt(position)") < network.indexOf("level.clip(")
                        && network.indexOf("MachineInspectionProtocol.withinDistance(") < network.indexOf("level.clip("));
        check("the authoritative ray must hit the requested block before querying it",
                network.indexOf("!hit.getBlockPos().equals(position)") < network.indexOf("MachineInspection.query("));
        check("the server measures the actual ray hit, not only the nearby block or look-vector length",
                network.contains("withinHitDistance(hit.getLocation().distanceToSqr(eye))")
                        && network.indexOf("withinHitDistance(") < network.indexOf("MachineInspection.query("));
        check("the client shares the actual-hit cap with the server", client.contains("withinHitDistance("));
        check("the ray cannot read an unloaded intermediate chunk",
                network.contains("!rayChunksLoaded(level, eye, end)")
                        && network.contains("ClipContext.Block.OUTLINE") && network.contains("ClipContext.Fluid.NONE"));
        check("there is no chunk enumeration or force-loading fallback",
                !query.contains(".getChunk(") && !network.contains(".getChunk(")
                        && !query.contains("getBlockEntities(") && !network.contains("getBlockEntities("));
        check("network work is admitted on the main thread and never broadcast",
                network.contains(".consumerMainThread(") && network.contains("PacketDistributor.PLAYER")
                        && !network.contains("PacketDistributor.ALL"));
        check("server rate ownership is weak and explicitly cleared", network.contains("WeakHashMap")
                && network.contains("LIMITERS.clear()"));
        check("optional channel absence is accepted and checked before sending",
                network.contains("NetworkRegistry.ABSENT.equals(version)")
                        && network.contains("NetworkRegistry.ACCEPTVANILLA.equals(version)")
                        && client.contains("MachineInspectionNetwork.available("));
        check("server construction imports no renderer",
                !config.contains("net.minecraft.client") && !config.contains("MachineInspectionPanel")
                        && !config.contains("MachineInspectionOverlayClient"));
        check("the Forge entrypoint has the one explicit initialization hook",
                Files.readString(forge.resolve("GrindlessForge.java")).contains("MachineInspectionOverlay.init(bus)"));
        check("the client is side-gated and reuses the incumbent events", client.contains("value = Dist.CLIENT")
                && client.contains("ClientTickEvent.CLIENT_POST") && client.contains("ClientGuiEvent.RENDER_HUD"));
        check("client state cannot retain an old world", client.contains("WeakReference<ClientLevel>")
                && client.contains("scope.get() != client.level") && client.contains("CLIENT_PLAYER_QUIT")
                && client.contains("CLIENT_PLAYER_RESPAWN") && client.contains("CLIENT_STOPPING"));
        check("the client never guesses machine state from a client block entity",
                !client.contains("getBlockEntity(") && !client.contains("menuData(")
                        && client.contains("STATE.snapshot(clock)") && client.contains("STATE.target(), aimed(client)"));
        check("all three optional modes default off", config.contains(".define(\"enabled\", false)")
                && config.contains(".define(\"developer\", false)")
                && config.contains(".define(\"allowWithOtherOverlays\", false)"));
        check("other overlays are detected by mod identity, not reflective API calls",
                client.contains("isLoaded(\"jade\")") && client.contains("isLoaded(\"waila\")")
                        && client.contains("isLoaded(\"wthit\")") && client.contains("isLoaded(\"theoneprobe\")")
                        && !client.contains("Class.forName("));
        check("native font, non-colour status text and pagination are present",
                panel.contains("Minecraft.getInstance().font") && panel.contains("font.split(")
                        && panel.contains("Math.floorMod(requestedPage, pages)")
                        && tooltip.contains("snapshot.state().symbol()"));
        check("deep internals require developer opt-in, not just Shift",
                tooltip.contains("if (developer)") && client.contains("MachineInspectionOverlay.DEVELOPER.get()"));
        check("no optional overlay dependency is added",
                !Files.readString(Path.of("forge/build.gradle")).toLowerCase().contains("jade")
                        && !Files.readString(Path.of("common/build.gradle")).toLowerCase().contains("jade"));
        try (var sources = Files.list(common.resolve("inspection"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                check(source.getFileName() + " is loader/optional-API neutral",
                        !text.contains("net.minecraftforge") && !text.contains("snownee.jade")
                                && !text.contains("mcp.mobius") && !text.contains("Class.forName("));
            }
        }
        var lang = JsonParser.parseString(Files.readString(Path.of(
                "common/src/main/resources/assets/grindless/lang/en_us.json"))).getAsJsonObject();
        check("inspection labels absolute MPa and the optimal rather than hard-cutoff band",
                lang.get("inspection.grindless.required_pressure").getAsString()
                        .equals("Process pressure: %s MPa absolute (optimal +/- %s MPa)"));
        for (Enum<?> value : State.values()) {
            check("state " + value + " has localized text", lang.has(
                    "inspection.grindless.state." + value.name().toLowerCase(java.util.Locale.ROOT)));
        }
        for (Enum<?> value : SiteRequirement.values()) {
            check("placement " + value + " has localized text", lang.has(
                    "inspection.grindless.site." + value.name().toLowerCase(java.util.Locale.ROOT)));
        }
        for (ConditionFault value : ConditionFault.values()) {
            if (value != ConditionFault.NONE) {
                check("condition " + value + " has localized recovery", lang.has(
                        "inspection.grindless.condition." + value.name().toLowerCase(java.util.Locale.ROOT)));
            }
        }
        List<List<? extends Enum<?>>> groups = List.of(List.of(ItemRole.values()), List.of(PortRole.values()),
                List.of(Face.values()), List.of(Ownership.values()), List.of(ManualKind.values()),
                List.of(Validity.values()));
        for (List<? extends Enum<?>> group : groups) {
            String domain = group.get(0) instanceof ItemRole ? "stored"
                    : group.get(0) instanceof PortRole ? "port_role"
                    : group.get(0) instanceof Face ? "face"
                    : group.get(0) instanceof Ownership ? "ownership"
                    : group.get(0) instanceof ManualKind ? "manual" : "validity";
            for (Enum<?> value : group) {
                check(domain + " " + value + " has localized text", lang.has(
                        "inspection.grindless." + domain + "." + value.name().toLowerCase(java.util.Locale.ROOT)));
            }
        }
    }

    private static MachineInspectionSnapshot maximalSnapshot() {
        String id = "grindless:" + "a".repeat(MachineInspectionSnapshot.MAX_TEXT - "grindless:".length());
        List<StoredItem> items = new ArrayList<>();
        for (int slot = 0; slot < MachineInspectionSnapshot.MAX_ITEMS; slot++) {
            items.add(new StoredItem(slot, ItemRole.SHARED, id, Integer.MAX_VALUE, 15));
        }
        List<Resource> resources = Collections.nCopies(MachineInspectionSnapshot.MAX_RESOURCES,
                new Resource(ResourceKind.FLUID, id, Integer.MAX_VALUE, 0, false));
        List<Resource> outputs = Collections.nCopies(MachineInspectionSnapshot.MAX_RESOURCES,
                new Resource(ResourceKind.FLUID, id, Integer.MAX_VALUE, 0, true));
        Process process = new Process("a".repeat(MachineInspectionSnapshot.MAX_TEXT), resources, outputs, resources,
                Integer.MAX_VALUE, Long.MAX_VALUE, Optional.of(new Progress(Integer.MAX_VALUE, Integer.MAX_VALUE)),
                Optional.of(new Conditions(Double.MAX_VALUE, Double.MAX_VALUE, "a".repeat(160), "a".repeat(160),
                        Optional.of(Double.MAX_VALUE), Optional.of(Double.MAX_VALUE / 4),
                        Optional.of(Double.MAX_VALUE / 4), Optional.of("a".repeat(160)), Optional.of("a".repeat(160)))));
        return new MachineInspectionSnapshot(new Target(id, new Position(1, 64, 2), Long.MAX_VALUE), id,
                State.INVALID_STRUCTURE, "a".repeat(MachineInspectionSnapshot.MAX_TEXT), true, Optional.of(process),
                items, Optional.of(new Energy(Long.MAX_VALUE, Long.MAX_VALUE)),
                Collections.nCopies(MachineInspectionSnapshot.MAX_TANKS, new Tank(id, Integer.MAX_VALUE, Integer.MAX_VALUE,
                        Optional.of(Integer.MAX_VALUE), Double.MAX_VALUE, Double.MAX_VALUE)),
                Optional.of(new Manual(ManualKind.CRANK_HANDLE, Long.MAX_VALUE)),
                Optional.of(new Structure("a".repeat(MachineInspectionSnapshot.MAX_TEXT), Validity.INVALID,
                        "a".repeat(MachineInspectionSnapshot.MAX_TEXT), Collections.nCopies(
                        MachineInspectionSnapshot.MAX_PORTS, new PortSummary(PortRole.FLUID_OUTPUT, Face.SOUTH,
                                List.of(Face.values()))),
                        Optional.of(new Position(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE)))),
                Optional.of(new Port(PortRole.FLUID_OUTPUT, Face.SOUTH, Ownership.CONFIRMED,
                        Optional.of(new Position(1, 64, 2)), List.of(Face.values()))),
                List.of(SiteRequirement.TERRESTRIAL, SiteRequirement.ROCK_BED, SiteRequirement.OPEN_SKY,
                        SiteRequirement.DAYLIGHT), true);
    }

    private static MachineInspectionSnapshot fullSnapshot() {
        Process process = new Process("synthetic/test", List.of(resource()),
                List.of(new Resource(ResourceKind.ITEM, "grindless:unfired_ceramic", 3, 0, false)),
                List.of(), 40, 8, Optional.of(new Progress(0.5, 40)),
                Optional.of(new Conditions(20, 0.1, "AIR", "STIRRED",
                        Optional.empty(), Optional.empty(), Optional.of("STIRRED"))));
        return new MachineInspectionSnapshot(new Target("minecraft:overworld", new Position(1, 64, 2), 123),
                "grindless:clay_mixer", State.WORKING, "", false, Optional.of(process),
                List.of(new StoredItem(0, ItemRole.INPUT, "minecraft:clay_ball", 2, 0)),
                Optional.of(new Energy(64, 320)), List.of(new Tank("minecraft:water", 250, 1000,
                Optional.of(50), 20, 0.1)), Optional.of(new Manual(ManualKind.CRANK_HANDLE, 64)),
                Optional.of(new Structure("synthetic_structure", Validity.FORMED, "",
                        List.of(new PortSummary(PortRole.ITEM_INPUT, Face.NORTH)))),
                Optional.of(new Port(PortRole.ITEM_INPUT, Face.NORTH, Ownership.CANDIDATE,
                        Optional.of(new Position(0, 64, 2)))), List.of(SiteRequirement.DAYLIGHT), false);
    }

    private static MachineInspectionSnapshot snapshot(List<StoredItem> items, Optional<Process> process,
                                                     List<Tank> tanks, Optional<Port> port) {
        return new MachineInspectionSnapshot(new Target("minecraft:overworld", new Position(1, 64, 2), 123),
                "grindless:clay_mixer", State.UNKNOWN, "", false, process, items, Optional.empty(), tanks,
                Optional.empty(), Optional.empty(), port, List.of(), false);
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
