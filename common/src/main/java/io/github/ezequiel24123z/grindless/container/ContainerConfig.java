package io.github.ezequiel24123z.grindless.container;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * The shared container contract: one set of controls obeyed by every buffer in the mod.
 *
 * <p>Item crates, fluid tanks and the input and output buffers built into machines all use this.
 * Learn the interface once and it is the same on a T1 tank and a T6 colony module, which is the
 * point — a tag-driven mod produces a great many containers, and a player should never have to
 * learn a second way to filter one.
 *
 * <p>Holds configuration only, never contents. Separating them keeps this class usable by item
 * and fluid containers alike, and keeps the per-tick path away from anything that only changes
 * when a player touches a screen.
 *
 * <h2>Auto-void is built defensively</h2>
 *
 * <p>Voiding is off by default on every container, always. When enabled it <b>trims, never
 * empties</b>: discarding applies only above the threshold, so a voiding container still holds its
 * buffer. It announces itself as a logic signal while actively voiding, and it refuses to discard
 * anything irreplaceable regardless of settings (ADR-0018).
 */
public final class ContainerConfig {

    private static final String KEY_SIDES = "Sides";
    private static final String KEY_BUFFER_TARGET = "BufferTarget";
    private static final String KEY_CAPACITY_LIMIT = "CapacityLimit";
    private static final String KEY_AUTO_VOID = "AutoVoid";
    private static final String KEY_VOID_MODE = "VoidMode";
    private static final String KEY_VOID_THRESHOLD = "VoidThreshold";
    private static final String KEY_INSERT_PRIORITY = "InsertPriority";
    private static final String KEY_EXTRACT_PRIORITY = "ExtractPriority";

    private static final Direction[] DIRECTIONS = Direction.values();

    private final SideMode[] sides = new SideMode[DIRECTIONS.length];
    private final ChangeListeners listeners = new ChangeListeners();

    private long bufferTarget;
    private long capacityLimit = -1L;
    private boolean autoVoid;
    private VoidMode voidMode = VoidMode.OVERFLOW;
    private long voidThreshold;
    private int insertPriority;
    private int extractPriority;

    public ContainerConfig() {
        java.util.Arrays.fill(sides, SideMode.BOTH);
    }

    /**
     * Listeners notified when configuration changes.
     *
     * <p>Configuration is a subscription input as much as contents are: closing a face or enabling
     * auto-output can make tick work necessary or unnecessary, so a machine that only listens to
     * contents will miss it and silently stop (ADR-0042).
     */
    public ChangeListeners listeners() {
        return listeners;
    }

    // ---- Side I/O -------------------------------------------------------------------------

    public SideMode sideMode(Direction side) {
        return sides[side.ordinal()];
    }

    public void setSideMode(Direction side, SideMode mode) {
        if (sides[side.ordinal()] != mode) {
            sides[side.ordinal()] = mode;
            listeners.notifyChanged();
        }
    }

    /** Whether the outside may push in through {@code side}. A {@code null} side is internal
     * access — the machine's own logic — which is never restricted by face configuration. */
    public boolean allowsInsert(Direction side) {
        return side == null || sides[side.ordinal()].allowsInsert();
    }

    /** Whether the outside may pull out through {@code side}. */
    public boolean allowsExtract(Direction side) {
        return side == null || sides[side.ordinal()].allowsExtract();
    }

    // ---- Buffer target and capacity -------------------------------------------------------

    /**
     * The amount to keep on hand. Drones and logic read below this as demand and above it as
     * surplus, which is what lets a network balance itself without a central controller.
     */
    public long bufferTarget() {
        return bufferTarget;
    }

    public void setBufferTarget(long amount) {
        long clamped = Math.max(0L, amount);
        if (bufferTarget != clamped) {
            bufferTarget = clamped;
            listeners.notifyChanged();
        }
    }

    /** A cap below the container's physical maximum, or {@code -1} for no cap. Useful to stop one
     * material from eating a shared buffer. */
    public long capacityLimit() {
        return capacityLimit;
    }

    public void setCapacityLimit(long limit) {
        long normalised = limit < 0L ? -1L : limit;
        if (capacityLimit != normalised) {
            capacityLimit = normalised;
            listeners.notifyChanged();
        }
    }

    /** The effective capacity of a slot whose physical maximum is {@code physical}. */
    public long effectiveCapacity(long physical) {
        return capacityLimit < 0L ? physical : Math.min(physical, capacityLimit);
    }

    // ---- Auto-void ------------------------------------------------------------------------

    public boolean isAutoVoid() {
        return autoVoid;
    }

    /**
     * Switches voiding on or off.
     *
     * <p>Callers must treat enabling as an explicit, confirmed action rather than a stray click in
     * a crowded UI, and a container with this set must be visibly marked in world — a particle
     * effect and a glow — so a base you built three months ago still shows at a glance which
     * containers are discarding.
     */
    public void setAutoVoid(boolean enabled) {
        if (autoVoid != enabled) {
            autoVoid = enabled;
            listeners.notifyChanged();
        }
    }

    public VoidMode voidMode() {
        return voidMode;
    }

    public void setVoidMode(VoidMode mode) {
        if (voidMode != mode) {
            voidMode = mode;
            listeners.notifyChanged();
        }
    }

    /** The level above which voiding applies. */
    public long voidThreshold() {
        return voidThreshold;
    }

    public void setVoidThreshold(long threshold) {
        long clamped = Math.max(0L, threshold);
        if (voidThreshold != clamped) {
            voidThreshold = clamped;
            listeners.notifyChanged();
        }
    }

    /**
     * How much of {@code amount} should be discarded.
     *
     * <p>Returns zero unless voiding is on, the contents exceed the threshold, and the material is
     * eligible. Never returns more than the surplus, which is what makes "it trims, it never
     * empties" a property of the code rather than a promise in the documentation.
     *
     * @param amount      what the container currently holds
     * @param matchesFilter whether this material matches the container's filter
     * @param protectedMaterial whether this material is on the replication blacklist — creative
     *                          items, quest rewards and pack-unique items are never discarded,
     *                          regardless of settings
     */
    public long voidAmount(long amount, boolean matchesFilter, boolean protectedMaterial) {
        if (!autoVoid || protectedMaterial || amount <= voidThreshold) {
            return 0L;
        }
        if (voidMode == VoidMode.FILTERED && !matchesFilter) {
            return 0L;
        }
        return amount - voidThreshold;
    }

    /** Whether the container is actively discarding, for the logic signal and the in-world
     * marking. An alarm can be built on this. */
    public boolean isVoiding(long amount, boolean matchesFilter, boolean protectedMaterial) {
        return voidAmount(amount, matchesFilter, protectedMaterial) > 0L;
    }

    // ---- Priority -------------------------------------------------------------------------

    /** Insertion priority. Independent of extraction so overflow and top-up routes resolve
     * predictably rather than fighting each other. Higher is preferred. */
    public int insertPriority() {
        return insertPriority;
    }

    public void setInsertPriority(int priority) {
        if (insertPriority != priority) {
            insertPriority = priority;
            listeners.notifyChanged();
        }
    }

    /** Extraction priority. Higher is preferred. */
    public int extractPriority() {
        return extractPriority;
    }

    public void setExtractPriority(int priority) {
        if (extractPriority != priority) {
            extractPriority = priority;
            listeners.notifyChanged();
        }
    }

    // ---- Persistence ----------------------------------------------------------------------

    /** Writes this configuration into {@code tag}. */
    public CompoundTag save(CompoundTag tag) {
        byte[] encoded = new byte[DIRECTIONS.length];
        for (int i = 0; i < DIRECTIONS.length; i++) {
            encoded[i] = (byte) sides[i].ordinal();
        }
        tag.putByteArray(KEY_SIDES, encoded);
        tag.putLong(KEY_BUFFER_TARGET, bufferTarget);
        tag.putLong(KEY_CAPACITY_LIMIT, capacityLimit);
        tag.putBoolean(KEY_AUTO_VOID, autoVoid);
        tag.putByte(KEY_VOID_MODE, (byte) voidMode.ordinal());
        tag.putLong(KEY_VOID_THRESHOLD, voidThreshold);
        tag.putInt(KEY_INSERT_PRIORITY, insertPriority);
        tag.putInt(KEY_EXTRACT_PRIORITY, extractPriority);
        return tag;
    }

    /**
     * Reads this configuration from {@code tag}.
     *
     * <p>Every ordinal read is bounds-checked. Enum ordinals are the one save field that silently
     * becomes garbage when a later version reorders or removes a constant, and an unchecked read
     * would throw during world load — which presents as a corrupt save rather than as a mod bug.
     */
    public void load(CompoundTag tag) {
        byte[] encoded = tag.getByteArray(KEY_SIDES);
        for (int i = 0; i < DIRECTIONS.length; i++) {
            sides[i] = i < encoded.length
                    ? decode(encoded[i], SideMode.values(), SideMode.BOTH)
                    : SideMode.BOTH;
        }
        bufferTarget = Math.max(0L, tag.getLong(KEY_BUFFER_TARGET));
        long storedLimit = tag.getLong(KEY_CAPACITY_LIMIT);
        capacityLimit = storedLimit < 0L ? -1L : storedLimit;
        autoVoid = tag.getBoolean(KEY_AUTO_VOID);
        voidMode = decode(tag.getByte(KEY_VOID_MODE), VoidMode.values(), VoidMode.OVERFLOW);
        voidThreshold = Math.max(0L, tag.getLong(KEY_VOID_THRESHOLD));
        insertPriority = tag.getInt(KEY_INSERT_PRIORITY);
        extractPriority = tag.getInt(KEY_EXTRACT_PRIORITY);
        listeners.notifyChanged();
    }

    private static <E extends Enum<E>> E decode(byte ordinal, E[] values, E fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }
}
