package io.github.ezequiel24123z.grindless.machine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The upgrades currently installed in a machine, and what they add up to.
 *
 * <p>Upgrades stack: two Speed upgrades are twice the trade, not a duplicate. The limits are the
 * chassis slot count and the mutually exclusive pairs, and both are enforced here rather than in
 * the UI — a screen is one of several ways upgrades get installed, and a rule enforced in only one
 * of them is not a rule.
 *
 * <p>Instances are immutable; installing or removing returns a new set. Machines hold one and
 * recompute their multipliers when it changes, which is rare, so nothing here runs per tick.
 *
 * <p><b>Balance note.</b> The multipliers below are structural, not balanced. Only the Speed trade
 * has a documented figure — double speed for roughly triple power — and the rest are placeholders
 * of the right shape. Final numbers wait on the Flux cost model, for the same reason
 * {@code PROCESSES.md} defers power: guessing them before the energy layer is tuned produces a
 * spreadsheet nobody trusts.
 */
public final class UpgradeSet {

    /** No upgrades installed. */
    public static final UpgradeSet EMPTY = new UpgradeSet(new EnumMap<>(MachineUpgrade.class));

    private final Map<MachineUpgrade, Integer> counts;

    private UpgradeSet(Map<MachineUpgrade, Integer> counts) {
        this.counts = Collections.unmodifiableMap(counts);
    }

    /**
     * Copies the current counts into a fresh mutable map.
     *
     * <p>Built from the enum class rather than with {@code new EnumMap<>(counts)}, which throws
     * {@code IllegalArgumentException} on an empty source map because it has no instance to infer
     * the key type from. That is the path taken by installing the very first upgrade, so the
     * convenient-looking constructor fails in the single most common case.
     */
    private Map<MachineUpgrade, Integer> copyOfCounts() {
        Map<MachineUpgrade, Integer> copy = new EnumMap<>(MachineUpgrade.class);
        copy.putAll(counts);
        return copy;
    }

    /** How many of {@code upgrade} are installed. */
    public int count(MachineUpgrade upgrade) {
        return counts.getOrDefault(upgrade, 0);
    }

    /** Whether at least one {@code upgrade} is installed. */
    public boolean has(MachineUpgrade upgrade) {
        return count(upgrade) > 0;
    }

    /** Total upgrades installed, counting duplicates. */
    public int size() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The installed upgrades and their counts. */
    public Map<MachineUpgrade, Integer> counts() {
        return counts;
    }

    /**
     * Why {@code upgrade} cannot be installed in a {@code mark} chassis, or {@code null} if it can.
     *
     * <p>Returns a reason rather than a boolean for the same reason condition checks do
     * (ADR-0041): "no free slots" and "conflicts with Speed" have different fixes, and a player
     * told only "no" goes hunting.
     */
    public String rejectionReason(MachineUpgrade upgrade, ChassisMark mark) {
        if (size() >= mark.upgradeSlots()) {
            return "No free upgrade slots — " + mark.displayName()
                    + " holds " + mark.upgradeSlots();
        }
        for (MachineUpgrade installed : counts.keySet()) {
            if (installed.conflictsWith(upgrade)) {
                return upgrade.displayName() + " cannot be combined with "
                        + installed.displayName();
            }
        }
        return null;
    }

    /** Whether {@code upgrade} can be installed in a {@code mark} chassis. */
    public boolean canInstall(MachineUpgrade upgrade, ChassisMark mark) {
        return rejectionReason(upgrade, mark) == null;
    }

    /**
     * Returns a set with one more {@code upgrade} installed.
     *
     * @throws IllegalArgumentException if it does not fit or conflicts; call
     *                                  {@link #canInstall} first
     */
    public UpgradeSet install(MachineUpgrade upgrade, ChassisMark mark) {
        String reason = rejectionReason(upgrade, mark);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        Map<MachineUpgrade, Integer> next = copyOfCounts();
        next.merge(upgrade, 1, Integer::sum);
        return new UpgradeSet(next);
    }

    /**
     * Returns a set with one fewer {@code upgrade}.
     *
     * <p>Upgrades are reusable and come out intact, so experimenting with a loadout is free. That
     * is deliberate: an upgrade you cannot take back is a decision you make once by reading a wiki
     * rather than by playing.
     */
    public UpgradeSet remove(MachineUpgrade upgrade) {
        if (!has(upgrade)) {
            return this;
        }
        Map<MachineUpgrade, Integer> next = copyOfCounts();
        int remaining = next.get(upgrade) - 1;
        if (remaining == 0) {
            next.remove(upgrade);
        } else {
            next.put(upgrade, remaining);
        }
        return new UpgradeSet(next);
    }

    /**
     * Cycle time multiplier. Below 1.0 is faster.
     *
     * <p>Speed halves the cycle per upgrade; Efficiency, Yield and Damping each lengthen it,
     * because that is what they spend.
     */
    public double timeMultiplier() {
        double multiplier = Math.pow(0.5, count(MachineUpgrade.SPEED));
        multiplier *= Math.pow(1.3, count(MachineUpgrade.EFFICIENCY));
        multiplier *= Math.pow(1.25, count(MachineUpgrade.YIELD));
        multiplier *= Math.pow(1.2, count(MachineUpgrade.DAMPING));
        return multiplier;
    }

    /**
     * Energy-per-operation multiplier.
     *
     * <p>Speed costs roughly triple power for double speed, which is the one figure
     * {@code MACHINES.md} states outright and the reason Speed is usually the wrong choice.
     */
    public double energyMultiplier() {
        double multiplier = Math.pow(3.0, count(MachineUpgrade.SPEED));
        multiplier *= Math.pow(0.75, count(MachineUpgrade.EFFICIENCY));
        multiplier *= parallelCount();
        return multiplier;
    }

    /** How many recipes run per cycle. Power scales with this linearly, so it is the honest,
     * boring way to scale and it is always available. */
    public int parallelCount() {
        return 1 + count(MachineUpgrade.PARALLEL);
    }

    /** Whether the machine holds its conditions tightly enough to sit inside a narrow band. */
    public boolean hasPrecision() {
        return has(MachineUpgrade.PRECISION);
    }

    /** Whether the machine can exceed its chassis norms for pressure and field. */
    public boolean hasContainment() {
        return has(MachineUpgrade.CONTAINMENT);
    }

    /** Whether spent catalysts are replaced automatically from a buffer. */
    public boolean hasCatalystFeed() {
        return has(MachineUpgrade.CATALYST_FEED);
    }

    /** Whether byproducts that would otherwise vent are captured. */
    public boolean hasRecovery() {
        return has(MachineUpgrade.RECOVERY);
    }
}
