package io.github.ezequiel24123z.grindless.machine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * An upgrade installed in a chassis slot.
 *
 * <p><b>Every upgrade spends one resource to buy another</b> (ADR-0028). None of them is simply
 * better, because an upgrade that is strictly good has no decision in it — the optimal play is to
 * fill every slot with it, and it becomes a tax you pay once rather than a choice you make.
 *
 * <p>Which upgrade is right depends on whether you are power-limited, material-limited or
 * throughput-limited, and that changes across the game. {@link #SPEED} is almost never the answer,
 * which is deliberate and matches the mod's stance on overclocking: the way to produce more is to
 * build wider, not to run one machine harder.
 */
public enum MachineUpgrade {

    SPEED("Speed", "shorter cycle time", "FU per operation, superlinearly; more waste heat"),
    PARALLEL("Parallel", "N recipes per cycle", "FU linearly; buffer space; larger input bursts"),
    EFFICIENCY("Efficiency", "less FU per operation", "cycle time"),
    YIELD("Yield", "more output, fewer losses", "cycle time; needs a catalyst"),
    PRECISION("Precision", "tighter condition hold, so the optimal band is actually hit",
            "constant FU upkeep"),
    INSULATION("Insulation", "far cheaper to hold a high temperature",
            "slow thermal response, so recipe switching hurts"),
    CONTAINMENT("Containment", "pressure and field beyond the chassis norm", "constant FU upkeep"),
    CATALYST_FEED("Catalyst Feed", "catalysts replaced automatically from a buffer",
            "an upgrade slot"),
    DAMPING("Damping", "much less Resonance emitted", "cycle time"),
    RECOVERY("Recovery", "captures byproducts that otherwise vent",
            "an upgrade slot; an output buffer");

    private static final Map<MachineUpgrade, Set<MachineUpgrade>> CONFLICTS;

    static {
        Map<MachineUpgrade, Set<MachineUpgrade>> conflicts = new EnumMap<>(MachineUpgrade.class);
        for (MachineUpgrade upgrade : values()) {
            conflicts.put(upgrade, EnumSet.noneOf(MachineUpgrade.class));
        }
        // Three pairs are mutually exclusive, because wanting both is wanting the trade not to
        // exist. Declared once and mirrored, so a pair can never be half-declared.
        exclude(conflicts, SPEED, EFFICIENCY);
        exclude(conflicts, SPEED, PRECISION);
        exclude(conflicts, INSULATION, PARALLEL);

        Map<MachineUpgrade, Set<MachineUpgrade>> frozen = new EnumMap<>(MachineUpgrade.class);
        conflicts.forEach((key, value) -> frozen.put(key, Collections.unmodifiableSet(value)));
        CONFLICTS = Collections.unmodifiableMap(frozen);
    }

    private final String displayName;
    private final String buys;
    private final String spends;

    MachineUpgrade(String displayName, String buys, String spends) {
        this.displayName = displayName;
        this.buys = buys;
        this.spends = spends;
    }

    public String displayName() {
        return displayName;
    }

    /** What installing this upgrade gains, for the tooltip. */
    public String buys() {
        return buys;
    }

    /** What it costs. Shown with equal prominence, because the trade is the point. */
    public String spends() {
        return spends;
    }

    /** The upgrades that cannot be installed alongside this one. */
    public Set<MachineUpgrade> conflicts() {
        return CONFLICTS.get(this);
    }

    /** Whether this upgrade and {@code other} are mutually exclusive. */
    public boolean conflictsWith(MachineUpgrade other) {
        return CONFLICTS.get(this).contains(other);
    }

    private static void exclude(Map<MachineUpgrade, Set<MachineUpgrade>> conflicts,
                                MachineUpgrade a, MachineUpgrade b) {
        conflicts.get(a).add(b);
        conflicts.get(b).add(a);
    }
}
