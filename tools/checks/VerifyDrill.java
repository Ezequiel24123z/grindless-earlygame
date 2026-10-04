package io.github.ezequiel24123z.grindless.item;

import net.minecraft.core.Direction;

import java.util.List;

/** Behaviour checks for the Flux Drill (ADR-0084). Not part of the mod. */
public final class VerifyDrill {

    private static int failures = 0;

    public static void main(String[] args) {
        charge();
        modes();
        shapes();
        vein();
        System.out.println(failures == 0
                ? "ALL DRILL CHECKS PASSED"
                : failures + " DRILL CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void charge() {
        eq("a block costs one F1 tick", 32, DrillLogic.BLOCK_FU);
        eq("a cell is one hundred blocks", 3200, DrillLogic.CELL_FU);
        eq("the drill holds two cells", 6400, DrillLogic.CAPACITY);
        eq("diamond harvest", 3, DrillLogic.HARVEST_LEVEL);
        no("thirty-one cannot start", DrillLogic.canStart(31));
        yes("thirty-two can start", DrillLogic.canStart(32));
        eq("a block spends thirty-two", 0, DrillLogic.afterBlock(32));
        eq("an empty cell fills to one cell", 3200, DrillLogic.storedAfterCell(0));
        eq("a second cell fills the drill", 6400, DrillLogic.storedAfterCell(3200));
        eq("a third cell is refused whole", 6400, DrillLogic.storedAfterCell(6400));
        eq("a partial cell is refused whole", 3201, DrillLogic.storedAfterCell(3201));
        yes("terrain breaks", DrillLogic.breaks(true, true, false));
        no("the factory does not", DrillLogic.breaks(true, true, true));
        no("the wrong tier does not", DrillLogic.breaks(true, false, false));
        yes("one cell pays for a full tunnel",
                DrillLogic.CELL_FU >= DrillLogic.BLOCK_FU * DrillLogic.TUNNEL_DEPTH * 9);
    }

    private static void modes() {
        eq("single is next area", DrillLogic.Mode.AREA, DrillLogic.Mode.SINGLE.next());
        eq("area is next vein", DrillLogic.Mode.VEIN, DrillLogic.Mode.AREA.next());
        eq("vein is next tunnel", DrillLogic.Mode.TUNNEL, DrillLogic.Mode.VEIN.next());
        eq("tunnel wraps to single", DrillLogic.Mode.SINGLE, DrillLogic.Mode.TUNNEL.next());
        eq("a blank mode is single", DrillLogic.Mode.SINGLE, DrillLogic.parse(""));
        eq("an unknown mode is single", DrillLogic.Mode.SINGLE, DrillLogic.parse("silk"));
        eq("vein parses", DrillLogic.Mode.VEIN, DrillLogic.parse("VEIN"));
    }

    private static void shapes() {
        List<DrillLogic.Cube> flat = DrillLogic.area(Direction.UP);
        eq("a 3×3 drops the centre", 8, flat.size());
        yes("looking up stays horizontal", flat.stream().allMatch(cube -> cube.y() == 0));
        yes("the area stays inside one block", flat.stream().allMatch(cube ->
                Math.abs(cube.x()) <= 1 && Math.abs(cube.z()) <= 1));
        List<DrillLogic.Cube> wall = DrillLogic.area(Direction.NORTH);
        yes("looking north is a vertical plane", wall.stream().allMatch(cube -> cube.z() == 0));
        List<DrillLogic.Cube> tunnel = DrillLogic.tunnel(Direction.NORTH);
        eq("eight slices of 3×3, minus the origin", 71, tunnel.size());
        no("the tunnel does not repeat the origin", tunnel.stream().anyMatch(cube ->
                cube.x() == 0 && cube.y() == 0 && cube.z() == 0));
        yes("a north tunnel steps north", tunnel.stream().allMatch(cube ->
                cube.z() <= 0 && cube.z() >= 1 - DrillLogic.TUNNEL_DEPTH));
        yes("a vertical look still digs north", DrillLogic.tunnel(Direction.UP).stream()
                .anyMatch(cube -> cube.z() == -7));
    }

    private static void vein() {
        List<DrillLogic.Cube> line = DrillLogic.vein(0, 0, 0, 32,
                (x, y, z) -> (x == 0 && y == 0 && z == 0) || (x == 1 && y == 0 && z == 0));
        eq("a face neighbour joins the vein", 2, line.size());
        List<DrillLogic.Cube> gap = DrillLogic.vein(0, 0, 0, 32,
                (x, y, z) -> (x == 0 && y == 0 && z == 0) || (x == 2 && y == 0 && z == 0));
        eq("a gap stops the vein", 1, gap.size());
        List<DrillLogic.Cube> corner = DrillLogic.vein(0, 0, 0, 32,
                (x, y, z) -> (x == 0 && y == 0 && z == 0) || (x == 1 && y == 1 && z == 0));
        eq("a corner is not a face", 1, corner.size());
        eq("the cap stops the flood", 1, DrillLogic.vein(0, 0, 0, 1, (x, y, z) -> true).size());
        eq("the named cap is thirty-two", 32, DrillLogic.VEIN_CAP);
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
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
