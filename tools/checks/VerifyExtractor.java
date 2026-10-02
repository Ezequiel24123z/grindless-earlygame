package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.network.PylonStructure;

/** Behaviour checks for the Crude Extractor and the pylon tower. Not part of the mod. */
public final class VerifyExtractor {

    private static int failures = 0;

    public static void main(String[] args) {
        cycle();
        status();
        outputForm();
        tower();

        System.out.println(failures == 0
                ? "ALL EXTRACTOR CHECKS PASSED"
                : failures + " EXTRACTOR CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void cycle() {
        eq("a cycle is twenty seconds", 20 * 20, ExtractorLogic.CYCLE_TICKS);
        eq("it draws F0", 8L, ExtractorLogic.FU_PER_TICK);

        eq("full power at rate 1 is one tick of work", 1.0,
                ExtractorLogic.work(1.0, 8L, 8L));
        eq("a rich vein at rate 2 is two ticks of work", 2.0,
                ExtractorLogic.work(2.0, 8L, 8L));
        eq("a poor vein at rate 0.5 is half a tick of work", 0.5,
                ExtractorLogic.work(0.5, 8L, 8L));
        eq("zero draw is zero work", 0.0, ExtractorLogic.work(1.0, 0L, 8L));
        eq("a brownout at half power is half work", 0.5,
                ExtractorLogic.work(1.0, 4L, 8L));

        double progress = 0.0;
        int ticks = 0;
        while (progress < ExtractorLogic.CYCLE_TICKS) {
            progress += ExtractorLogic.work(1.0, 8L, 8L);
            ticks++;
            if (ticks > 10_000) {
                fail("cycle never finished");
                return;
            }
        }
        eq("rate 1 and full power take twenty seconds", ExtractorLogic.CYCLE_TICKS, ticks);

        progress = 0.0;
        ticks = 0;
        while (progress < ExtractorLogic.CYCLE_TICKS) {
            progress += ExtractorLogic.work(2.0, 8L, 8L);
            ticks++;
        }
        eq("rate 2 takes ten seconds", ExtractorLogic.CYCLE_TICKS / 2, ticks);

        progress = 0.0;
        ticks = 0;
        while (progress < ExtractorLogic.CYCLE_TICKS) {
            progress += ExtractorLogic.work(0.5, 8L, 8L);
            ticks++;
        }
        eq("rate 0.5 takes forty seconds", ExtractorLogic.CYCLE_TICKS * 2, ticks);
    }

    private static void status() {
        eq("no vein is idle", MachineStatus.IDLE,
                ExtractorLogic.status(false, false, false, false));
        eq("product stuck is blocked, even with no power", MachineStatus.BLOCKED,
                ExtractorLogic.status(true, true, false, false));
        eq("a vein and no power is starved", MachineStatus.STARVED,
                ExtractorLogic.status(true, false, false, false));
        eq("powered and working is running", MachineStatus.RUNNING,
                ExtractorLogic.status(true, false, true, true));
        eq("powered but not yet working is idle", MachineStatus.IDLE,
                ExtractorLogic.status(true, false, true, false));
        no("blocked wins over starved",
                ExtractorLogic.status(true, true, false, false) == MachineStatus.STARVED);
    }

    private static void outputForm() {
        yes("raw beats ore", ExtractorLogic.outputForm(true, true) == MaterialForm.RAW);
        yes("ore is the fallback", ExtractorLogic.outputForm(false, true) == MaterialForm.ORE);
        yes("raw is preferred when there is no ore",
                ExtractorLogic.outputForm(true, false) == MaterialForm.RAW);
    }

    private static void tower() {
        eq("a pylon occupies three blocks", 3, PylonStructure.HEIGHT);
        eq("there are two shafts", 2, PylonStructure.shafts(new net.minecraft.core.BlockPos(0, 64, 0)).length);
        eq("the first shaft is one above the base", 65,
                PylonStructure.shafts(new net.minecraft.core.BlockPos(0, 64, 0))[0].getY());
        eq("the second shaft is two above the base", 66,
                PylonStructure.shafts(new net.minecraft.core.BlockPos(0, 64, 0))[1].getY());
        eq("index of the middle shaft", 1,
                PylonStructure.indexOf(new net.minecraft.core.BlockPos(0, 64, 0),
                        new net.minecraft.core.BlockPos(0, 65, 0)));
        eq("the base under a top shaft", 64,
                PylonStructure.baseOf(new net.minecraft.core.BlockPos(8, 66, 8), 2).getY());
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, MachineStatus expected, MachineStatus actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
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
