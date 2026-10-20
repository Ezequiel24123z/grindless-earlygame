package io.github.ezequiel24123z.grindless.inspection;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** The optional overlay transports only this allowlisted schema, never a block entity's NBT. */
public final class MachineInspectionCodec {

    public static final String KEY = "grindless:inspection";
    public static final int VERSION = 1;

    private MachineInspectionCodec() {
    }

    public static CompoundTag encode(MachineInspectionSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", VERSION);
        CompoundTag target = position(snapshot.target().position());
        target.putString("dimension", snapshot.target().dimension());
        target.putLong("time", snapshot.target().gameTime());
        tag.put("target", target);
        tag.putString("block", snapshot.blockId());
        tag.putString("state", snapshot.state().name());
        tag.putString("reason", snapshot.reasonKey());
        tag.putBoolean("held", snapshot.held());
        tag.putBoolean("truncated", snapshot.truncated());
        tag.put("requirements", list(snapshot.requirements(), requirement -> {
            CompoundTag value = new CompoundTag();
            value.putString("kind", requirement.name());
            return value;
        }));
        tag.put("items", list(snapshot.items(), item -> {
            CompoundTag value = new CompoundTag();
            value.putInt("slot", item.slot());
            value.putString("role", item.role().name());
            value.putString("id", item.id());
            value.putInt("count", item.count());
            value.putInt("rating", item.matrixRating());
            return value;
        }));
        tag.put("tanks", list(snapshot.tanks(), tank -> {
            CompoundTag value = new CompoundTag();
            value.putString("id", tank.id());
            value.putInt("amount", tank.amount());
            value.putInt("capacity", tank.capacity());
            tank.outputQuota().ifPresent(quota -> value.putInt("output", quota));
            value.putDouble("temperature", tank.temperatureC());
            value.putDouble("pressure", tank.pressureMPa());
            return value;
        }));
        snapshot.process().ifPresent(process -> {
            CompoundTag value = new CompoundTag();
            value.putString("id", process.id());
            value.putInt("duration", process.durationTicks());
            value.putLong("draw", process.fuPerTick());
            value.put("inputs", list(process.inputs(), MachineInspectionCodec::resource));
            value.put("outputs", list(process.outputs(), MachineInspectionCodec::resource));
            value.put("catalysts", list(process.catalysts(), MachineInspectionCodec::resource));
            process.progress().ifPresent(progress -> value.putDouble("work", progress.completedWork()));
            process.conditions().ifPresent(conditions -> value.put("conditions", conditions(conditions)));
            tag.put("process", value);
        });
        snapshot.energy().ifPresent(energy -> {
            CompoundTag value = new CompoundTag();
            value.putLong("stored", energy.stored());
            value.putLong("capacity", energy.capacity());
            tag.put("energy", value);
        });
        snapshot.manual().ifPresent(manual -> {
            CompoundTag value = new CompoundTag();
            value.putString("kind", manual.kind().name());
            value.putLong("charge", manual.fuPerAction());
            tag.put("manual", value);
        });
        snapshot.structure().ifPresent(structure -> {
            CompoundTag value = new CompoundTag();
            value.putString("kind", structure.kind());
            value.putString("validity", structure.validity().name());
            value.putString("reason", structure.reasonKey());
            value.put("ports", list(structure.ports(), port -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("role", port.role().name());
                entry.putString("face", port.face().name());
                entry.put("access", faces(port.accessFaces()));
                return entry;
            }));
            structure.problem().ifPresent(problem -> value.put("problem", position(problem)));
            tag.put("structure", value);
        });
        snapshot.port().ifPresent(port -> {
            CompoundTag value = new CompoundTag();
            value.putString("role", port.role().name());
            value.putString("face", port.face().name());
            value.putString("ownership", port.ownership().name());
            value.put("access", faces(port.accessFaces()));
            port.owner().ifPresent(owner -> value.put("owner", position(owner)));
            tag.put("port", value);
        });
        return tag;
    }

    /** Invalid or newer data is rejected explicitly rather than becoming an idle, empty machine. */
    public static MachineInspectionSnapshot decode(CompoundTag tag) {
        fields(tag, Set.of("schema", "target", "block", "state", "reason", "held", "truncated", "items", "tanks",
                        "requirements"),
                Set.of("process", "energy", "manual", "structure", "port"));
        if (integer(tag, "schema") != VERSION) {
            throw new IllegalArgumentException("unsupported machine inspection schema");
        }
        CompoundTag target = compound(tag, "target");
        fields(target, Set.of("dimension", "time", "x", "y", "z"), Set.of());
        return new MachineInspectionSnapshot(
                new Target(string(target, "dimension"), readPosition(target), number(target, "time")),
                string(tag, "block"), enumeration(tag, "state", State.class), string(tag, "reason"),
                bool(tag, "held"),
                optional(tag, "process", MachineInspectionCodec::readProcess),
                readList(tag, "items", MachineInspectionSnapshot.MAX_ITEMS, value -> {
                    fields(value, Set.of("slot", "role", "id", "count", "rating"), Set.of());
                    return new StoredItem(integer(value, "slot"), enumeration(value, "role", ItemRole.class),
                            string(value, "id"), integer(value, "count"), integer(value, "rating"));
                }),
                optional(tag, "energy", value -> {
                    fields(value, Set.of("stored", "capacity"), Set.of());
                    return new Energy(number(value, "stored"), number(value, "capacity"));
                }),
                readList(tag, "tanks", MachineInspectionSnapshot.MAX_TANKS, value -> {
                    fields(value, Set.of("id", "amount", "capacity", "temperature", "pressure"), Set.of("output"));
                    return new Tank(string(value, "id"), integer(value, "amount"), integer(value, "capacity"),
                            value.contains("output") ? Optional.of(integer(value, "output")) : Optional.empty(),
                            decimal(value, "temperature"), decimal(value, "pressure"));
                }),
                optional(tag, "manual", value -> {
                    fields(value, Set.of("kind", "charge"), Set.of());
                    return new Manual(enumeration(value, "kind", ManualKind.class), number(value, "charge"));
                }),
                optional(tag, "structure", value -> {
                    fields(value, Set.of("kind", "validity", "reason", "ports"), Set.of("problem"));
                    return new Structure(string(value, "kind"), enumeration(value, "validity", Validity.class),
                            string(value, "reason"),
                            readList(value, "ports", MachineInspectionSnapshot.MAX_PORTS, entry -> {
                                fields(entry, Set.of("role", "face", "access"), Set.of());
                                return new PortSummary(enumeration(entry, "role", PortRole.class),
                                        enumeration(entry, "face", Face.class), readFaces(entry));
                            }), optional(value, "problem", problem -> {
                                fields(problem, Set.of("x", "y", "z"), Set.of());
                                return readPosition(problem);
                            }));
                }),
                optional(tag, "port", value -> {
                    fields(value, Set.of("role", "face", "ownership", "access"), Set.of("owner"));
                    return new Port(enumeration(value, "role", PortRole.class),
                            enumeration(value, "face", Face.class), enumeration(value, "ownership", Ownership.class),
                            optional(value, "owner", owner -> {
                                fields(owner, Set.of("x", "y", "z"), Set.of());
                                return readPosition(owner);
                            }), readFaces(value));
                }),
                readList(tag, "requirements", MachineInspectionSnapshot.MAX_REQUIREMENTS, value -> {
                    fields(value, Set.of("kind"), Set.of());
                    return enumeration(value, "kind", SiteRequirement.class);
                }), bool(tag, "truncated"));
    }

    private static Process readProcess(CompoundTag value) {
        fields(value, Set.of("id", "duration", "draw", "inputs", "outputs", "catalysts"), Set.of("work", "conditions"));
        int duration = integer(value, "duration");
        return new Process(string(value, "id"),
                readList(value, "inputs", MachineInspectionSnapshot.MAX_RESOURCES, MachineInspectionCodec::readResource),
                readList(value, "outputs", MachineInspectionSnapshot.MAX_RESOURCES, MachineInspectionCodec::readResource),
                readList(value, "catalysts", MachineInspectionSnapshot.MAX_RESOURCES, MachineInspectionCodec::readResource),
                duration, number(value, "draw"),
                value.contains("work") ? Optional.of(new Progress(decimal(value, "work"), duration)) : Optional.empty(),
                optional(value, "conditions", conditions -> {
                    fields(conditions, Set.of("temperature", "pressure", "atmosphere", "agitation"),
                            Set.of("required_temperature", "required_atmosphere", "required_agitation"));
                    return new Conditions(decimal(conditions, "temperature"), decimal(conditions, "pressure"),
                            string(conditions, "atmosphere"), string(conditions, "agitation"),
                            conditions.contains("required_temperature")
                                    ? Optional.of(decimal(conditions, "required_temperature")) : Optional.empty(),
                            conditions.contains("required_atmosphere")
                                    ? Optional.of(string(conditions, "required_atmosphere")) : Optional.empty(),
                            conditions.contains("required_agitation")
                                    ? Optional.of(string(conditions, "required_agitation")) : Optional.empty());
                }));
    }

    private static CompoundTag conditions(Conditions conditions) {
        CompoundTag value = new CompoundTag();
        value.putDouble("temperature", conditions.temperatureC());
        value.putDouble("pressure", conditions.pressureMPa());
        value.putString("atmosphere", conditions.atmosphere());
        value.putString("agitation", conditions.agitation());
        conditions.requiredTemperatureC().ifPresent(temperature -> value.putDouble("required_temperature", temperature));
        conditions.requiredAtmosphere().ifPresent(atmosphere -> value.putString("required_atmosphere", atmosphere));
        conditions.requiredAgitation().ifPresent(agitation -> value.putString("required_agitation", agitation));
        return value;
    }

    private static Resource readResource(CompoundTag value) {
        fields(value, Set.of("kind", "id", "count", "rating", "vented"), Set.of());
        return new Resource(enumeration(value, "kind", ResourceKind.class), string(value, "id"),
                integer(value, "count"), integer(value, "rating"), bool(value, "vented"));
    }

    private static CompoundTag resource(Resource resource) {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", resource.kind().name());
        tag.putString("id", resource.id());
        tag.putInt("count", resource.count());
        tag.putInt("rating", resource.matrixRating());
        tag.putBoolean("vented", resource.vented());
        return tag;
    }

    private static ListTag faces(List<Face> faces) {
        return list(faces, face -> {
            CompoundTag value = new CompoundTag();
            value.putString("face", face.name());
            return value;
        });
    }

    private static List<Face> readFaces(CompoundTag value) {
        return readList(value, "access", MachineInspectionSnapshot.MAX_FACES, face -> {
            fields(face, Set.of("face"), Set.of());
            return enumeration(face, "face", Face.class);
        });
    }

    private static CompoundTag position(Position position) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", position.x());
        tag.putInt("y", position.y());
        tag.putInt("z", position.z());
        return tag;
    }

    private static Position readPosition(CompoundTag tag) {
        return new Position(integer(tag, "x"), integer(tag, "y"), integer(tag, "z"));
    }

    private static <T> Optional<T> optional(CompoundTag tag, String key, Function<CompoundTag, T> reader) {
        return tag.contains(key) ? Optional.of(reader.apply(compound(tag, key))) : Optional.empty();
    }

    private static <T> ListTag list(List<T> values, Function<T, CompoundTag> writer) {
        ListTag result = new ListTag();
        values.forEach(value -> result.add(writer.apply(value)));
        return result;
    }

    private static <T> List<T> readList(CompoundTag tag, String key, int maximum,
                                      Function<CompoundTag, T> reader) {
        require(tag, key, Tag.TAG_LIST);
        ListTag list = (ListTag) tag.get(key);
        if (list.size() > maximum || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) {
            throw new IllegalArgumentException("invalid machine inspection list: " + key);
        }
        List<T> result = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            result.add(reader.apply(list.getCompound(index)));
        }
        return List.copyOf(result);
    }

    private static void fields(CompoundTag tag, Set<String> required, Set<String> optional) {
        if (!tag.getAllKeys().containsAll(required)
                || tag.getAllKeys().stream().anyMatch(key -> !required.contains(key) && !optional.contains(key))) {
            throw new IllegalArgumentException("invalid machine inspection fields");
        }
    }

    private static CompoundTag compound(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_COMPOUND);
        return tag.getCompound(key);
    }

    private static String string(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_STRING);
        String value = tag.getString(key);
        if (value.length() > MachineInspectionSnapshot.MAX_TEXT) {
            throw new IllegalArgumentException("machine inspection text exceeds its bound");
        }
        return value;
    }

    private static int integer(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_INT);
        return tag.getInt(key);
    }

    private static long number(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_LONG);
        return tag.getLong(key);
    }

    private static double decimal(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_DOUBLE);
        return tag.getDouble(key);
    }

    private static boolean bool(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_BYTE);
        byte value = tag.getByte(key);
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("invalid machine inspection boolean: " + key);
        }
        return value == 1;
    }

    private static <T extends Enum<T>> T enumeration(CompoundTag tag, String key, Class<T> type) {
        try {
            return Enum.valueOf(type, string(tag, key));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("invalid machine inspection enum: " + key, invalid);
        }
    }

    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) {
            throw new IllegalArgumentException("invalid machine inspection field type: " + key);
        }
    }
}
