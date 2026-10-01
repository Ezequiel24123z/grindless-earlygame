package io.github.ezequiel24123z.grindless.process;

/** Throwaway check of the ConditionBand maths. Not part of the mod. */
public final class VerifyBand {

    private static int failures = 0;

    public static void main(String[] args) {
        ConditionBand t = ConditionBand.relative(1500.0);

        // ±15% of 1500 = 225 -> optimal [1275,1725]; tolerance x2 -> [1050,1950]
        eq("optimalMin", 1275.0, t.optimalMin());
        eq("optimalMax", 1725.0, t.optimalMax());
        eq("toleranceMin", 1050.0, t.toleranceMin());
        eq("toleranceMax", 1950.0, t.toleranceMax());

        eq("at optimum", 1.0, t.efficiencyAt(1500.0));
        eq("at optimal edge", 1.0, t.efficiencyAt(1725.0));
        eq("at tolerance edge", 0.25, t.efficiencyAt(1950.0));
        eq("halfway into tolerance", 0.625, t.efficiencyAt(1837.5));
        eq("below tolerance", 0.0, t.efficiencyAt(1000.0));
        eq("low tolerance edge", 0.25, t.efficiencyAt(1050.0));

        yes("admits optimum", t.admits(1500.0));
        no("rejects far too hot", t.admits(3000.0));
        yes("too cold is below", t.isBelow(900.0));
        no("too hot is not below", t.isBelow(3000.0));

        // Cryogenic: the band must not invert on a negative optimum.
        ConditionBand cryo = ConditionBand.relative(-196.0);
        yes("cryo band ordered", cryo.toleranceMin() < cryo.toleranceMax());
        yes("cryo admits optimum", cryo.admits(-196.0));
        eq("cryo optimal half-width", 29.4, cryo.optimalMax() - (-196.0));

        // Zone refining: an explicit tight band.
        ConditionBand zone = ConditionBand.absolute(1420.0, 5.0);
        yes("zone admits 1424", zone.admits(1424.0));
        no("zone rejects 1440", zone.admits(1440.0));

        // A relative band around zero has no width; it must be refused, not silently useless.
        try {
            ConditionBand.relative(0.0);
            fail("relative(0) should throw");
        } catch (IllegalArgumentException expected) {
            pass("relative(0) refused");
        }

        // Best reachable efficiency is the optimum clamped into the machine's range.
        eq("arc furnace reaches optimum", 1.0, t.bestEfficiencyWithin(1200.0, 3500.0));
        eq("kiln cannot reach at all", 0.0, t.bestEfficiencyWithin(100.0, 900.0));
        // A range that still contains the optimum is optimal, however wide or narrow: the
        // machine simply sets 1500. Degradation needs a range that excludes the optimum.
        eq("range containing optimum is optimal", 1.0, t.bestEfficiencyWithin(100.0, 1800.0));
        eq("floor above optimum degrades", 0.75, t.bestEfficiencyWithin(1800.0, 1900.0));
        eq("ceiling below optimum degrades", 0.75, t.bestEfficiencyWithin(1000.0, 1200.0));
        yes("degraded value is between floor and 1",
                t.bestEfficiencyWithin(1800.0, 1900.0) > 0.25
                        && t.bestEfficiencyWithin(1800.0, 1900.0) < 1.0);

        yes("arc furnace can reach", t.reachableWithin(1200.0, 3500.0));
        no("kiln cannot reach", t.reachableWithin(100.0, 900.0));

        System.out.println(failures == 0
                ? "ALL BAND CHECKS PASSED"
                : failures + " BAND CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
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
