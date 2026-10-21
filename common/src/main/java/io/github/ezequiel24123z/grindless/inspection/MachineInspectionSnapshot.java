package io.github.ezequiel24123z.grindless.inspection;

import io.github.ezequiel24123z.grindless.process.ConditionBand;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A bounded, immutable observation, not an executable recipe or a saved machine.
 * Empty optionals mean the authority does not expose that information.
 */
public record MachineInspectionSnapshot(
        Target target,
        String blockId,
        State state,
        String reasonKey,
        boolean held,
        Optional<Process> process,
        List<StoredItem> items,
        Optional<Energy> energy,
        List<Tank> tanks,
        Optional<Manual> manual,
        Optional<Structure> structure,
        Optional<Port> port,
        List<SiteRequirement> requirements,
        boolean truncated) {

    public static final int MAX_ITEMS = 16;
    public static final int MAX_TANKS = 4;
    public static final int MAX_RESOURCES = 8;
    public static final int MAX_PORTS = 8;
    public static final int MAX_FACES = 6;
    public static final int MAX_REQUIREMENTS = 4;
    public static final int MAX_TEXT = 160;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final Pattern KEY = Pattern.compile("[a-z0-9_.]+");

    public MachineInspectionSnapshot {
        Objects.requireNonNull(target, "target");
        blockId = id(blockId);
        Objects.requireNonNull(state, "state");
        reasonKey = key(reasonKey);
        process = Objects.requireNonNull(process, "process");
        items = bounded(items, MAX_ITEMS);
        if (items.stream().map(StoredItem::slot).distinct().count() != items.size()) {
            throw new IllegalArgumentException("duplicate inspected inventory slot");
        }
        energy = Objects.requireNonNull(energy, "energy");
        tanks = bounded(tanks, MAX_TANKS);
        manual = Objects.requireNonNull(manual, "manual");
        structure = Objects.requireNonNull(structure, "structure");
        port = Objects.requireNonNull(port, "port");
        requirements = bounded(requirements, MAX_REQUIREMENTS);
    }

    public boolean describes(String dimension, Position position, String block) {
        return target.dimension().equals(dimension) && target.position().equals(position) && blockId.equals(block);
    }

    /** Display semantics only. These values never replace MachineStatus or its saved ordinals. */
    public enum State {
        IDLE, WORKING, BLOCKED, MISSING_INPUT, OUTPUT_FULL, NO_ENERGY, NO_FLUID,
        INVALID_STRUCTURE, INVALID_ENVIRONMENT, STORAGE, INTERFACE, UNKNOWN;

        public String translationKey() {
            return "inspection.grindless.state." + name().toLowerCase(Locale.ROOT);
        }

        public String symbol() {
            return switch (this) {
                case WORKING -> "[>]";
                case IDLE, STORAGE, INTERFACE -> "[-]";
                case UNKNOWN -> "[?]";
                default -> "[!]";
            };
        }
    }

    public enum ItemRole {
        INPUT, OUTPUT, FUEL, SHARED
    }

    public enum ResourceKind {
        ITEM, TAG, FLUID
    }

    public enum Validity {
        FORMED, INVALID, UNKNOWN
    }

    public enum PortRole {
        ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT
    }

    public enum Ownership {
        CONFIRMED, CANDIDATE, ORPHAN, UNKNOWN
    }

    public enum Face {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    }

    public enum ManualKind {
        CRANK_HANDLE, HAND_DYNAMO
    }

    public enum SiteRequirement {
        TERRESTRIAL, ROCK_BED, OPEN_SKY, DAYLIGHT, WATER_SOURCE
    }

    public record Position(int x, int y, int z) {
    }

    public record Target(String dimension, Position position, long gameTime) {
        public Target {
            dimension = id(dimension);
            Objects.requireNonNull(position, "position");
        }
    }

    /** Registry identity and quantity only; no ItemStack, mutable NBT or custom item names. */
    public record StoredItem(int slot, ItemRole role, String id, int count, int matrixRating) {
        public StoredItem {
            if (slot < 0 || slot >= MAX_ITEMS || count <= 0 || matrixRating < -1 || matrixRating > 15) {
                throw new IllegalArgumentException("invalid inspected item");
            }
            Objects.requireNonNull(role, "role");
            id = MachineInspectionSnapshot.id(id);
        }
    }

    public record Resource(ResourceKind kind, String id, int count, int matrixRating, boolean vented) {
        public Resource {
            Objects.requireNonNull(kind, "kind");
            id = MachineInspectionSnapshot.id(id);
            if (count <= 0 || matrixRating < 0 || matrixRating > 15
                    || matrixRating != 0 && kind != ResourceKind.ITEM
                    || vented && kind != ResourceKind.FLUID) {
                throw new IllegalArgumentException("invalid inspected resource");
            }
        }
    }

    /**
     * Work units at nominal speed, not elapsed time or a wall-clock ETA.
     * Only a native progress field may supply completedWork.
     */
    public record Progress(double completedWork, int requiredWork) {
        public Progress {
            if (!Double.isFinite(completedWork) || completedWork < 0 || requiredWork <= 0
                    || completedWork > requiredWork) {
                throw new IllegalArgumentException("invalid inspected progress");
            }
        }

        public int percent() {
            return (int) Math.floor(completedWork / requiredWork * 100.0);
        }

        public double nominalSecondsRemaining() {
            return (requiredWork - completedWork) / 20.0;
        }
    }

    public record Process(String id, List<Resource> inputs, List<Resource> outputs,
                          List<Resource> catalysts, int durationTicks, long fuPerTick,
                          Optional<Progress> progress, Optional<Conditions> conditions) {
        public Process(String id, List<Resource> inputs, List<Resource> outputs, List<Resource> catalysts,
                       int durationTicks, long fuPerTick, Optional<Progress> progress) {
            this(id, inputs, outputs, catalysts, durationTicks, fuPerTick, progress, Optional.empty());
        }

        public Process {
            id = text(id);
            inputs = bounded(inputs, MAX_RESOURCES);
            outputs = bounded(outputs, MAX_RESOURCES);
            catalysts = bounded(catalysts, MAX_RESOURCES);
            if (durationTicks <= 0 || fuPerTick < 0) {
                throw new IllegalArgumentException("invalid inspected process");
            }
            if (inputs.stream().anyMatch(Resource::vented) || catalysts.stream().anyMatch(Resource::vented)) {
                throw new IllegalArgumentException("an inspected input cannot be discharged output");
            }
            progress = Objects.requireNonNull(progress, "progress");
            conditions = Objects.requireNonNull(conditions, "conditions");
            if (progress.isPresent() && progress.get().requiredWork() != durationTicks) {
                throw new IllegalArgumentException("progress must belong to the inspected process");
            }
        }
    }

    /** Absolute MPa throughout; an absent required optimum and half-width impose no pressure requirement. */
    public record Conditions(double temperatureC, double pressureMPa, String atmosphere, String agitation,
                             Optional<Double> requiredTemperatureC, Optional<Double> requiredPressureMPa,
                             Optional<Double> requiredPressureHalfWidthMPa, Optional<String> requiredAtmosphere,
                             Optional<String> requiredAgitation) {
        public Conditions(double temperatureC, double pressureMPa, String atmosphere, String agitation,
                          Optional<Double> requiredTemperatureC, Optional<String> requiredAtmosphere,
                          Optional<String> requiredAgitation) {
            this(temperatureC, pressureMPa, atmosphere, agitation, requiredTemperatureC, Optional.empty(),
                    Optional.empty(), requiredAtmosphere, requiredAgitation);
        }

        public Conditions {
            if (!Double.isFinite(temperatureC) || !Double.isFinite(pressureMPa) || pressureMPa < 0) {
                throw new IllegalArgumentException("invalid inspected conditions");
            }
            atmosphere = text(atmosphere);
            agitation = text(agitation);
            requiredTemperatureC = Objects.requireNonNull(requiredTemperatureC, "requiredTemperatureC");
            if (requiredTemperatureC.isPresent() && !Double.isFinite(requiredTemperatureC.get())) {
                throw new IllegalArgumentException("invalid required inspection temperature");
            }
            requiredPressureMPa = Objects.requireNonNull(requiredPressureMPa, "requiredPressureMPa");
            requiredPressureHalfWidthMPa = Objects.requireNonNull(requiredPressureHalfWidthMPa,
                    "requiredPressureHalfWidthMPa");
            if (requiredPressureMPa.isPresent() != requiredPressureHalfWidthMPa.isPresent()) {
                throw new IllegalArgumentException("an inspected pressure requirement needs its complete band");
            }
            if (requiredPressureMPa.isPresent()) {
                double optimum = requiredPressureMPa.get();
                double halfWidth = requiredPressureHalfWidthMPa.orElseThrow();
                if (!Double.isFinite(optimum) || optimum < 0 || !Double.isFinite(halfWidth) || halfWidth <= 0) {
                    throw new IllegalArgumentException("invalid required inspection pressure");
                }
                ConditionBand band = ConditionBand.absolute(optimum, halfWidth);
                if (!Double.isFinite(band.toleranceMin()) || !Double.isFinite(band.toleranceMax())) {
                    throw new IllegalArgumentException("invalid required inspection pressure band");
                }
            }
            requiredAtmosphere = Objects.requireNonNull(requiredAtmosphere, "requiredAtmosphere").map(
                    MachineInspectionSnapshot::text);
            requiredAgitation = Objects.requireNonNull(requiredAgitation, "requiredAgitation").map(
                    MachineInspectionSnapshot::text);
        }
    }

    public record Energy(long stored, long capacity) {
        public Energy {
            if (stored < 0 || capacity <= 0 || stored > capacity) {
                throw new IllegalArgumentException("invalid inspected energy");
            }
        }
    }

    public record Tank(String id, int amount, int capacity, Optional<Integer> outputQuota,
                       double temperatureC, double pressureMPa) {
        public Tank {
            id = amount == 0 && "".equals(id) ? "" : MachineInspectionSnapshot.id(id);
            outputQuota = Objects.requireNonNull(outputQuota, "outputQuota");
            if (capacity <= 0 || amount < 0 || amount > capacity
                    || amount == 0 && !id.isEmpty()
                    || outputQuota.isPresent() && (outputQuota.get() < 0 || outputQuota.get() > amount)
                    || !Double.isFinite(temperatureC) || !Double.isFinite(pressureMPa) || pressureMPa < 0) {
                throw new IllegalArgumentException("invalid inspected tank");
            }
        }
    }

    public record Manual(ManualKind kind, long fuPerAction) {
        public Manual {
            Objects.requireNonNull(kind, "kind");
            if (fuPerAction <= 0) {
                throw new IllegalArgumentException("invalid inspected manual charge");
            }
        }
    }

    public record PortSummary(PortRole role, Face face, List<Face> accessFaces) {
        public PortSummary(PortRole role, Face face) {
            this(role, face, List.of());
        }

        public PortSummary {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(face, "face");
            accessFaces = faces(accessFaces);
        }
    }

    public record Structure(String kind, Validity validity, String reasonKey, List<PortSummary> ports,
                            Optional<Position> problem) {
        public Structure(String kind, Validity validity, String reasonKey, List<PortSummary> ports) {
            this(kind, validity, reasonKey, ports, Optional.empty());
        }

        public Structure {
            kind = text(kind);
            Objects.requireNonNull(validity, "validity");
            reasonKey = key(reasonKey);
            ports = bounded(ports, MAX_PORTS);
            problem = Objects.requireNonNull(problem, "problem");
            if (validity == Validity.FORMED && problem.isPresent()) {
                throw new IllegalArgumentException("a formed inspection cannot name a failing cell");
            }
        }
    }

    /** A candidate is explicitly not a confirmed controller owner. */
    public record Port(PortRole role, Face face, Ownership ownership, Optional<Position> owner,
                       List<Face> accessFaces) {
        public Port(PortRole role, Face face, Ownership ownership, Optional<Position> owner) {
            this(role, face, ownership, owner, List.of());
        }

        public Port {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(face, "face");
            Objects.requireNonNull(ownership, "ownership");
            owner = Objects.requireNonNull(owner, "owner");
            accessFaces = faces(accessFaces);
            if ((ownership == Ownership.CONFIRMED || ownership == Ownership.CANDIDATE) != owner.isPresent()) {
                throw new IllegalArgumentException("invalid inspected port ownership");
            }
        }
    }

    private static List<Face> faces(List<Face> values) {
        List<Face> result = bounded(values, MAX_FACES);
        if (result.stream().distinct().count() != result.size()) {
            throw new IllegalArgumentException("duplicate inspected access face");
        }
        return result;
    }

    static String text(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_TEXT
                || value.chars().anyMatch(c -> c < 32 || c == 127 || c == 167)) {
            throw new IllegalArgumentException("invalid inspected text");
        }
        return value;
    }

    static String id(String value) {
        if (!ID.matcher(text(value)).matches()) {
            throw new IllegalArgumentException("invalid inspected registry id");
        }
        return value;
    }

    static String key(String value) {
        if ("".equals(value)) {
            return "";
        }
        if (!KEY.matcher(text(value)).matches()) {
            throw new IllegalArgumentException("invalid inspected translation key");
        }
        return value;
    }

    private static <T> List<T> bounded(List<T> values, int maximum) {
        if (Objects.requireNonNull(values, "values").size() > maximum) {
            throw new IllegalArgumentException("inspection list exceeds its bound");
        }
        return List.copyOf(values);
    }
}
