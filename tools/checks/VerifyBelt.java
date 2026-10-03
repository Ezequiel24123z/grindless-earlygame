package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.TerrestrialLogic;
import io.github.ezequiel24123z.grindless.vein.SurveyLogic;

import java.util.List;

/** Behaviour checks for belts, the splitter, the manipulator and the T1 extractor. Not part of the mod. */
public final class VerifyBelt {

    private static int failures = 0;

    public static void main(String[] args) {
        numbers();
        lane();
        splitter();
        manipulator();
        survey();
        terrestrial();

        System.out.println(failures == 0
                ? "ALL BELT CHECKS PASSED"
                : failures + " BELT CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void numbers() {
        eq("eight items a second", 8, BeltLogic.ITEMS_PER_SECOND);
        eq("two lanes", 2, BeltLogic.LANES);
        eq("four slots per lane", 4, BeltLogic.SLOTS_PER_LANE);
        eq("an item occupies a quarter tile", 0.25, BeltLogic.ITEM_LENGTH);
        eq("a conveyor moves one tile per second", 1.0, BeltLogic.SPEED);
        eq("one second of travel is one tile", 1.0, BeltLogic.travel(20));
        eq("a tick of travel is a twentieth of a tile", 0.05, BeltLogic.travel(1));
        yes("an empty tile accepts", BeltLogic.canEnter(0.25));
        no("a tile with an item at the mouth refuses", BeltLogic.canEnter(0.1));
        yes("an item at 1 is ready to leave", BeltLogic.readyToLeave(1.0));
        no("an item at 0.9 is not ready", BeltLogic.readyToLeave(0.9));
    }

    private static void lane() {
        Lane lane = new Lane();
        yes("a new lane accepts", lane.canAccept());
        yes("the first item enters", lane.insert(new LaneItem("minecraft:cobblestone", 1, 0)));
        eq("one occupant", 1, lane.size());
        eq("it sits at the mouth", 0.0, lane.items().get(0).position());
        no("a second item does not overlap it", lane.canAccept());

        lane.advance(BeltLogic.travel(5));
        eq("five ticks move a quarter tile", 0.25, lane.items().get(0).position(), 1e-9);
        yes("the mouth is free after a quarter tile", lane.canAccept());
        yes("a second item enters behind", lane.insert(new LaneItem("minecraft:raw_iron", 1, 0)));

        for (int i = 0; i < 20; i++) {
            lane.advance(BeltLogic.travel(1));
        }
        LaneItem front = lane.peekFront();
        yes("the lead item reached the end", front != null && BeltLogic.readyToLeave(front.position()));
        eq("the lead item is still cobble", "minecraft:cobblestone", front.id());
        LaneItem taken = lane.takeReady();
        eq("takeReady hands cobble over", "minecraft:cobblestone", taken.id());
        eq("one item remains", 1, lane.size());
        no("the trailer is not yet ready", BeltLogic.readyToLeave(lane.peekFront().position()));

        Lane packed = new Lane();
        for (int i = 0; i < 4; i++) {
            yes("slot " + i + " of a packed lane fills", packed.insert(new LaneItem("a", 1, 0)));
            packed.advance(BeltLogic.ITEM_LENGTH);
        }
        no("a fifth item does not fit", packed.canAccept());

        Lane blocked = new Lane();
        blocked.insert(new LaneItem("stuck", 1, 0));
        blocked.advance(10.0);
        eq("a blocked tile still stops at 1", 1.0, blocked.peekFront().position());
        LaneItem held = blocked.takeReady();
        // put it back at 1 to simulate a backed-up downstream
        held.setPosition(1.0);
        blocked.items(); // keep the reference used
        // re-insert at the end by advancing an empty then placing
        Lane backed = new Lane();
        backed.insert(new LaneItem("head", 1, 0));
        backed.advance(10.0);
        backed.insert(new LaneItem("tail", 1, 0));
        backed.advance(10.0);
        eq("the trailer stops a quarter behind the head", 0.75, backed.items().get(1).position(), 1e-9);
    }

    private static void splitter() {
        List<SplitterLogic.Outlet> even = List.of(
                new SplitterLogic.Outlet(0, false, true, false, 0),
                new SplitterLogic.Outlet(1, false, true, false, 0),
                new SplitterLogic.Outlet(2, false, true, false, 0));
        eq("first item goes front", 0, SplitterLogic.route(even, -1));
        eq("second item goes left", 1, SplitterLogic.route(even, 0));
        eq("third item goes right", 2, SplitterLogic.route(even, 1));
        eq("fourth item wraps to front", 0, SplitterLogic.route(even, 2));

        List<SplitterLogic.Outlet> filtered = List.of(
                new SplitterLogic.Outlet(0, true, true, false, 0),
                new SplitterLogic.Outlet(1, false, true, false, 0),
                new SplitterLogic.Outlet(2, false, true, false, 0));
        eq("a matching filter beats an open face", 0, SplitterLogic.route(filtered, -1));
        eq("and keeps winning", 0, SplitterLogic.route(filtered, 0));

        List<SplitterLogic.Outlet> backed = List.of(
                new SplitterLogic.Outlet(0, true, true, true, 0),
                new SplitterLogic.Outlet(1, false, true, false, 0),
                new SplitterLogic.Outlet(2, false, true, false, 0));
        eq("a backed-up filter yields to the open face", 1, SplitterLogic.route(backed, -1));

        List<SplitterLogic.Outlet> priority = List.of(
                new SplitterLogic.Outlet(0, false, true, false, 0),
                new SplitterLogic.Outlet(1, false, true, false, 2),
                new SplitterLogic.Outlet(2, false, true, false, 1));
        eq("the highest priority wins", 1, SplitterLogic.route(priority, -1));

        List<SplitterLogic.Outlet> none = List.of(
                new SplitterLogic.Outlet(0, false, true, true, 0),
                new SplitterLogic.Outlet(1, true, false, false, 0),
                new SplitterLogic.Outlet(2, false, true, true, 0));
        eq("no open matching outlet is -1", -1, SplitterLogic.route(none, 0));

        yes("an empty filter accepts cobble", SplitterLogic.matches("minecraft:cobblestone", ""));
        yes("the same id matches", SplitterLogic.matches("minecraft:raw_iron", "minecraft:raw_iron"));
        no("a different id does not", SplitterLogic.matches("minecraft:raw_iron", "minecraft:coal"));
    }

    private static void manipulator() {
        eq("crude moves once a second", 20, ManipulatorLogic.CYCLE_TICKS);
        eq("crude moves one item", 1, ManipulatorLogic.STACK);
        yes("a full cycle is ready", ManipulatorLogic.ready(20));
        no("nineteen ticks are not", ManipulatorLogic.ready(19));
        eq("nothing in reach is idle", MachineStatus.IDLE, ManipulatorLogic.status(false, false));
        eq("work in reach is running", MachineStatus.RUNNING, ManipulatorLogic.status(true, false));
        eq("a transfer is running", MachineStatus.RUNNING, ManipulatorLogic.status(false, true));
    }

    private static void survey() {
        eq("the scanner covers a 3x3", 9, SurveyLogic.area());
        List<SurveyLogic.ChunkRef> around = SurveyLogic.around(4, 7);
        eq("nine chunks", 9, around.size());
        yes("it includes the standing chunk", around.contains(new SurveyLogic.ChunkRef(4, 7)));
        yes("it includes a corner", around.contains(new SurveyLogic.ChunkRef(3, 6)));
        yes("it includes the opposite corner", around.contains(new SurveyLogic.ChunkRef(5, 8)));
        no("it does not reach two chunks away", around.contains(new SurveyLogic.ChunkRef(2, 7)));
    }

    private static void terrestrial() {
        eq("five seconds per unit", 100, TerrestrialLogic.CYCLE_TICKS);
        eq("it draws F1", 32L, TerrestrialLogic.FU_PER_TICK);
        eq("unsurveyed is out of band", MachineStatus.OUT_OF_BAND,
                TerrestrialLogic.status(false, true, false, true, false));
        eq("surveyed and stuck is blocked", MachineStatus.BLOCKED,
                TerrestrialLogic.status(true, true, true, true, false));
        eq("surveyed and no power is starved", MachineStatus.STARVED,
                TerrestrialLogic.status(true, true, false, false, false));
        eq("surveyed and working is running", MachineStatus.RUNNING,
                TerrestrialLogic.status(true, true, false, true, true));
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
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

    private static void eq(String what, double expected, double actual) {
        eq(what, expected, actual, 1e-9);
    }

    private static void eq(String what, double expected, double actual, double epsilon) {
        if (Math.abs(expected - actual) < epsilon) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, String expected, String actual) {
        if (expected.equals(actual)) {
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
