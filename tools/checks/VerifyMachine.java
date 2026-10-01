package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.process.Atmosphere;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;

/** Throwaway check of the machine layer. Not part of the mod. */
public final class VerifyMachine {

    private static int failures = 0;

    public static void main(String[] args) {
        subscriptions();
        upgrades();
        chassis();

        System.out.println(failures == 0
                ? "ALL MACHINE CHECKS PASSED"
                : failures + " MACHINE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void subscriptions() {
        TickSubscriptions subs = new TickSubscriptions();
        yes("starts idle", subs.isIdle());

        int[] runs = {0};
        TickSubscription a = subs.subscribe(() -> runs[0]++);
        no("not idle once subscribed", subs.isIdle());
        subs.tick();
        subs.tick();
        eq("ran twice", 2, runs[0]);

        a.unsubscribe();
        subs.tick();
        eq("stopped after unsubscribe", 2, runs[0]);
        yes("idle again", subs.isIdle());

        // Passing the handle back must not subscribe the same work twice.
        TickSubscriptions dedupe = new TickSubscriptions();
        int[] once = {0};
        TickSubscription handle = dedupe.subscribe(null, () -> once[0]++);
        handle = dedupe.subscribe(handle, () -> once[0]++);
        handle = dedupe.subscribe(handle, () -> once[0]++);
        eq("deduped to one subscription", 1, dedupe.activeCount());
        dedupe.tick();
        eq("deduped work ran once", 1, once[0]);

        // The important case: work that cancels itself mid-tick. This is how a machine
        // goes idle, so it must not corrupt the iteration.
        TickSubscriptions selfCancel = new TickSubscriptions();
        int[] cancelRuns = {0};
        TickSubscription[] holder = new TickSubscription[1];
        holder[0] = selfCancel.subscribe(() -> {
            cancelRuns[0]++;
            holder[0].unsubscribe();
        });
        selfCancel.tick();
        selfCancel.tick();
        eq("self-cancelling work ran exactly once", 1, cancelRuns[0]);
        yes("idle after self-cancel", selfCancel.isIdle());

        // Work that subscribes more work during the pass.
        TickSubscriptions growing = new TickSubscriptions();
        int[] added = {0};
        TickSubscription[] seed = new TickSubscription[1];
        seed[0] = growing.subscribe(() -> {
            growing.subscribe(() -> added[0]++);
            seed[0].unsubscribe();
        });
        growing.tick();
        eq("added work ran in the same pass", 1, added[0]);
        eq("one subscription remains", 1, growing.activeCount());

        // One cancelling another mid-pass: the cancelled one must not run.
        TickSubscriptions crossCancel = new TickSubscriptions();
        int[] victimRuns = {0};
        TickSubscription[] victim = new TickSubscription[1];
        crossCancel.subscribe(() -> victim[0].unsubscribe());
        victim[0] = crossCancel.subscribe(() -> victimRuns[0]++);
        crossCancel.tick();
        eq("cancelled peer did not run", 0, victimRuns[0]);

        TickSubscriptions cleared = new TickSubscriptions();
        cleared.subscribe(() -> { });
        cleared.clear();
        yes("clear leaves it idle", cleared.isIdle());

        // Offsets must actually spread work out rather than agreeing.
        int offA = TickOffset.forPosition(100, 64, 100);
        int offB = TickOffset.forPosition(101, 64, 100);
        yes("offsets are non-negative", offA >= 0 && offB >= 0);
        yes("adjacent blocks get different offsets", offA % 20 != offB % 20);
        yes("offset is stable", offA == TickOffset.forPosition(100, 64, 100));

        // The case that matters: a row of machines, as players actually build. A weak hash
        // would give these the same offset and resynchronise everything.
        boolean[] seen = new boolean[20];
        int distinct = 0;
        for (int i = 0; i < 20; i++) {
            int bucket = TickOffset.forPosition(100 + i, 64, 100) % 20;
            if (!seen[bucket]) {
                seen[bucket] = true;
                distinct++;
            }
        }
        yes("a row of 20 spreads over many ticks (got " + distinct + ")", distinct >= 10);

        int due = 0;
        for (long t = 0; t < 20; t++) {
            if (TickOffset.isDue(t, offA, 20)) {
                due++;
            }
        }
        eq("period 20 fires once per 20 ticks", 1, due);
        yes("period 1 always fires", TickOffset.isDue(7L, offA, 1));
    }

    private static void upgrades() {
        UpgradeSet set = UpgradeSet.EMPTY;
        eq("starts empty", 0, set.size());
        eq("no parallel by default", 1, set.parallelCount());
        eq("no time change by default", 1.0, set.timeMultiplier());

        // MK I has one slot.
        set = set.install(MachineUpgrade.SPEED, ChassisMark.MK_I);
        eq("one installed", 1, set.size());
        no("second does not fit in MK I", set.canInstall(MachineUpgrade.YIELD, ChassisMark.MK_I));
        yes("but fits in MK II", set.canInstall(MachineUpgrade.YIELD, ChassisMark.MK_II));

        // Exclusions must hold in both directions.
        no("speed excludes efficiency",
                set.canInstall(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V));
        no("speed excludes precision",
                set.canInstall(MachineUpgrade.PRECISION, ChassisMark.MK_V));
        UpgradeSet eff = UpgradeSet.EMPTY.install(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V);
        no("efficiency excludes speed", eff.canInstall(MachineUpgrade.SPEED, ChassisMark.MK_V));
        yes("exclusion is symmetric",
                MachineUpgrade.SPEED.conflictsWith(MachineUpgrade.EFFICIENCY)
                        && MachineUpgrade.EFFICIENCY.conflictsWith(MachineUpgrade.SPEED));
        UpgradeSet par = UpgradeSet.EMPTY.install(MachineUpgrade.PARALLEL, ChassisMark.MK_V);
        no("parallel excludes insulation",
                par.canInstall(MachineUpgrade.INSULATION, ChassisMark.MK_V));

        yes("rejection explains itself",
                set.rejectionReason(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V) != null
                        && set.rejectionReason(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V)
                                .contains("Speed"));

        // Speed: double speed for roughly triple power, the one documented figure.
        eq("one speed halves the time", 0.5, set.timeMultiplier());
        eq("one speed triples the energy", 3.0, set.energyMultiplier());
        UpgradeSet twoSpeed = set.install(MachineUpgrade.SPEED, ChassisMark.MK_V);
        eq("speed stacks", 2, twoSpeed.count(MachineUpgrade.SPEED));
        eq("two speeds quarter the time", 0.25, twoSpeed.timeMultiplier());
        eq("two speeds are ninefold energy", 9.0, twoSpeed.energyMultiplier());

        // Parallel scales power linearly: the honest, boring axis.
        UpgradeSet twoPar = UpgradeSet.EMPTY
                .install(MachineUpgrade.PARALLEL, ChassisMark.MK_V)
                .install(MachineUpgrade.PARALLEL, ChassisMark.MK_V);
        eq("three recipes per cycle", 3, twoPar.parallelCount());
        eq("power scales linearly with parallel", 3.0, twoPar.energyMultiplier());
        eq("parallel does not change cycle time", 1.0, twoPar.timeMultiplier());

        // Efficiency: cheaper, slower. The opposite trade.
        yes("efficiency is slower", eff.timeMultiplier() > 1.0);
        yes("efficiency is cheaper", eff.energyMultiplier() < 1.0);

        UpgradeSet removed = twoSpeed.remove(MachineUpgrade.SPEED);
        eq("removing leaves one", 1, removed.count(MachineUpgrade.SPEED));
        eq("removing again empties", 0,
                removed.remove(MachineUpgrade.SPEED).count(MachineUpgrade.SPEED));
        eq("removing absent is harmless", 0,
                UpgradeSet.EMPTY.remove(MachineUpgrade.YIELD).size());
        eq("sets are immutable", 1, set.size());
    }

    private static void chassis() {
        eq("MK I has one slot", 1, ChassisMark.MK_I.upgradeSlots());
        eq("MK V has six", 6, ChassisMark.MK_V.upgradeSlots());
        yes("MK V is the top", ChassisMark.MK_V.next() == ChassisMark.MK_V);
        yes("marks step up", ChassisMark.MK_I.next() == ChassisMark.MK_II);

        // The Arc Furnace ladder from MACHINES.md: 1800, 2300, 2800, 3200, 3500.
        ConditionEnvelope narrow = ConditionEnvelope.builder()
                .temperature(1200.0, 1800.0)
                .atmospheres(Atmosphere.REDUCING)
                .build();
        ConditionEnvelope full = ConditionEnvelope.builder()
                .temperature(1200.0, 3500.0)
                .atmospheres(Atmosphere.REDUCING, Atmosphere.OXIDISING, Atmosphere.INERT)
                .build();

        double[] expected = {1800.0, 2300.0, 2800.0, 3200.0, 3500.0};
        ChassisMark[] marks = ChassisMark.values();
        for (int i = 0; i < marks.length; i++) {
            double actual = marks[i].widen(narrow, full).maxTemperature();
            near(marks[i].displayName() + " reaches ~" + (int) expected[i] + " C",
                    expected[i], actual, 20.0);
        }

        eq("MK I is exactly the narrow ceiling", 1800.0,
                ChassisMark.MK_I.widen(narrow, full).maxTemperature());
        eq("MK V is exactly the full ceiling", 3500.0,
                ChassisMark.MK_V.widen(narrow, full).maxTemperature());
        yes("MK V gains the wider atmosphere set",
                ChassisMark.MK_V.widen(narrow, full).supportsAtmosphere(Atmosphere.OXIDISING));
        no("MK I does not",
                ChassisMark.MK_I.widen(narrow, full).supportsAtmosphere(Atmosphere.OXIDISING));
        yes("widening is monotonic",
                ChassisMark.MK_II.widen(narrow, full).maxTemperature()
                        < ChassisMark.MK_III.widen(narrow, full).maxTemperature());
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void near(String what, double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) <= tolerance) {
            pass(what);
        } else {
            fail(what + ": expected ~" + expected + " but got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            pass(what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            pass(what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void pass(String what) {
        System.out.println("  ok   " + what);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
