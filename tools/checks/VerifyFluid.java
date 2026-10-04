package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;

/** Behaviour checks for T1 fluids. Not part of the mod. */
public final class VerifyFluid {

    private static int failures = 0;

    public static void main(String[] args) {
        units();
        stacks();
        gravity();
        buffers();
        capture();
        burn();

        System.out.println(failures == 0
                ? "ALL FLUID CHECKS PASSED"
                : failures + " FLUID CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void units() {
        eq("a bucket is 1000 mB", 1000, FluidLogic.BUCKET);
        eq("ambient is 20 C", 20.0, FluidLogic.AMBIENT_C);
        eq("ambient is 0.1 MPa", 0.1, FluidLogic.AMBIENT_MPA);
        eq("the basic tank holds 16 B", 16_000, FluidLogic.TANK_CAPACITY);
        eq("wet mill takes half a bucket", 500, FluidLogic.WET_WATER_MB);
        eq("R1 names 1 B of CO", 1000, FluidLogic.CO_MB);
        eq("R2 names 1 B of SO2", 1000, FluidLogic.SO2_MB);
        eq("CO id matches the process graph", ProcessLogic.CARBON_MONOXIDE, FluidLogic.CARBON_MONOXIDE);
        eq("SO2 id matches the process graph", ProcessLogic.SULFUR_DIOXIDE, FluidLogic.SULFUR_DIOXIDE);
    }

    private static void stacks() {
        FluidState empty = FluidState.EMPTY;
        yes("empty is empty", empty.isEmpty());
        FluidState water = FluidState.of(FluidLogic.WATER, 500);
        eq("ambient water is 20 C", 20.0, water.temperatureC());
        yes("ambient water is liquid", FluidLogic.isLiquid(water));
        no("ambient water is not gas", FluidLogic.isGas(water));
        yes("ambient water is ambient", FluidLogic.isAmbient(water));

        FluidState steam = FluidState.of(FluidLogic.WATER, 500, 120.0, 0.1);
        yes("steam is a gas", FluidLogic.isGas(steam));
        no("steam is not ambient-cool", FluidLogic.isAmbient(steam));

        FluidState co = FluidState.of(FluidLogic.CARBON_MONOXIDE, 1000);
        yes("ambient CO is a gas", FluidLogic.isGas(co));
        yes("ambient CO is still ambient pressure", FluidLogic.isAmbient(co));
        FluidState so2 = FluidState.of(FluidLogic.SULFUR_DIOXIDE, 1000);
        yes("ambient SO2 is a gas", FluidLogic.isGas(so2));
        yes("a tank at ambient will hold SO2",
                FluidLogic.accepted(FluidState.EMPTY, so2, FluidLogic.TANK_CAPACITY,
                        FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA) == 1000);
        no("SO2 is not CO", so2.is(FluidLogic.CARBON_MONOXIDE));

        FluidState mixed = water.merge(FluidState.of(FluidLogic.WATER, 500, 40.0, 0.1));
        eq("mix adds volumes", 1000, mixed.millibuckets());
        eq("mix weights temperature", 30.0, mixed.temperatureC(), 1e-9);
    }

    private static void gravity() {
        FluidState water = FluidState.of(FluidLogic.WATER, 200);
        yes("level flow is allowed", FluidLogic.canGravityFlow(water, 70, 70));
        yes("downhill flow is allowed", FluidLogic.canGravityFlow(water, 70, 69));
        no("uphill flow is refused", FluidLogic.canGravityFlow(water, 70, 71));
        FluidState co = FluidState.of(FluidLogic.CARBON_MONOXIDE, 200);
        no("gas does not gravity-flow", FluidLogic.canGravityFlow(co, 70, 69));
        FluidState hot = FluidState.of(FluidLogic.WATER, 200, 80.0, 0.1);
        no("hot water does not use clay", FluidLogic.canGravityFlow(hot, 70, 69));
    }

    private static void buffers() {
        FluidState none = FluidState.EMPTY;
        FluidState water = FluidState.of(FluidLogic.WATER, 2000);
        eq("an empty tank takes what fits", 2000,
                FluidLogic.accepted(none, water, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                        FluidLogic.AMBIENT_MPA));
        FluidState filled = FluidLogic.insert(none, water, 1500, FluidLogic.AMBIENT_MAX_C,
                FluidLogic.AMBIENT_MPA);
        eq("capacity clips the insert", 1500, filled.millibuckets());
        eq("the leftover is the rest", 500,
                FluidLogic.leftover(none, water, 1500, FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA)
                        .millibuckets());

        FluidState hot = FluidState.of(FluidLogic.WATER, 100, 90.0, 0.1);
        eq("a basic tank refuses hot fluid", 0,
                FluidLogic.accepted(none, hot, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                        FluidLogic.AMBIENT_MPA));

        FluidState co = FluidState.of(FluidLogic.CARBON_MONOXIDE, 1000);
        eq("a tank at ambient will hold CO", 1000,
                FluidLogic.accepted(none, co, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                        FluidLogic.AMBIENT_MPA));
        eq("it will not mix CO into water", 0,
                FluidLogic.accepted(filled, co, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                        FluidLogic.AMBIENT_MPA));

        eq("pump status with no water is starved", MachineStatus.STARVED,
                FluidLogic.pumpStatus(false, false));
        eq("pumping is running", MachineStatus.RUNNING, FluidLogic.pumpStatus(true, true));
        eq("an empty tank is idle", MachineStatus.IDLE, FluidLogic.tankStatus(false));
        eq("a holding tank is running", MachineStatus.RUNNING, FluidLogic.tankStatus(true));

        FluidBuffer tank = FluidBuffer.ambient(FluidLogic.TANK_CAPACITY);
        eq("an empty buffer has its full space", FluidLogic.TANK_CAPACITY, tank.space());
        tank.offer(FluidState.of(FluidLogic.WATER, 2500));
        eq("space shrinks by what was offered", FluidLogic.TANK_CAPACITY - 2500, tank.space());
        FluidState leftoverHot = tank.offer(FluidState.of(FluidLogic.WATER, 100, 90.0, 0.1));
        eq("hot fluid is refused whole", 100, leftoverHot.millibuckets());
        eq("and does not occupy space", FluidLogic.TANK_CAPACITY - 2500, tank.space());
    }

    private static void capture() {
        FluidState tank = FluidState.EMPTY;
        FluidState co = FluidState.of(FluidLogic.CARBON_MONOXIDE, FluidLogic.CO_MB);
        int take = FluidLogic.accepted(tank, co, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                FluidLogic.AMBIENT_MPA);
        yes("an empty tank captures the whole B of CO", take == FluidLogic.CO_MB);
        FluidState leftover = FluidLogic.leftover(tank, co, FluidLogic.TANK_CAPACITY,
                FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA);
        yes("and none is left to vent", leftover.isEmpty());

        FluidState almost = FluidState.of(FluidLogic.CARBON_MONOXIDE, FluidLogic.TANK_CAPACITY - 100);
        int partial = FluidLogic.accepted(almost, co, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                FluidLogic.AMBIENT_MPA);
        eq("a nearly full tank captures what fits", 100, partial);
        eq("the rest would vent", 900,
                FluidLogic.leftover(almost, co, FluidLogic.TANK_CAPACITY, FluidLogic.AMBIENT_MAX_C,
                        FluidLogic.AMBIENT_MPA).millibuckets());
    }

    private static void burn() {
        eq("captured CO burns 400 ticks per bucket", 400, FluidLogic.CO_BURN_TICKS);
        eq("that is 20 s of F1", 20 * 20, FluidLogic.CO_BURN_TICKS);
        yes("a bucket of CO is extractable",
                FluidLogic.extractable(FluidState.of(FluidLogic.CARBON_MONOXIDE, 1000),
                        FluidLogic.CARBON_MONOXIDE, 1000) == 1000);
        no("water is not CO",
                FluidLogic.extractable(FluidState.of(FluidLogic.WATER, 1000),
                        FluidLogic.CARBON_MONOXIDE, 1000) > 0);
        no("SO2 is not burnt as CO",
                FluidLogic.extractable(FluidState.of(FluidLogic.SULFUR_DIOXIDE, 1000),
                        FluidLogic.CARBON_MONOXIDE, 1000) > 0);
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, double expected, double actual) {
        eq(what, expected, actual, 0.0);
    }

    private static void eq(String what, double expected, double actual, double eps) {
        if (Math.abs(expected - actual) <= eps) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
