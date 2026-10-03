package io.github.ezequiel24123z.grindless.vein;

import io.github.ezequiel24123z.grindless.machine.ChassisMark;
import io.github.ezequiel24123z.grindless.machine.MachineUpgrade;
import io.github.ezequiel24123z.grindless.machine.UpgradeSet;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Behaviour checks for Resource Genesis. Not part of the mod. */
public final class VerifyVein {

    private static int failures = 0;

    public static void main(String[] args) {
        determinism();
        distribution();
        depletion();
        deepBore();
        lifetime();

        System.out.println(failures == 0
                ? "ALL VEIN CHECKS PASSED"
                : failures + " VEIN CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<Material> materials() {
        List<Material> list = new ArrayList<>();
        list.add(new Material("copper", EnumSet.of(MaterialForm.ORE, MaterialForm.INGOT), 200));
        list.add(new Material("iron", EnumSet.of(MaterialForm.ORE, MaterialForm.INGOT), 200));
        list.add(new Material("tin", EnumSet.of(MaterialForm.ORE, MaterialForm.INGOT), 100));
        list.add(new Material("diamond", EnumSet.of(MaterialForm.ORE, MaterialForm.GEM), 5));
        return list;
    }

    private static void determinism() {
        List<Material> materials = materials();
        ChunkVein first = VeinGenerator.generate(12345L, 10, 20, materials);
        ChunkVein again = VeinGenerator.generate(12345L, 10, 20, materials);
        yes("the same seed and chunk give the same vein", first.equals(again));
        eq("and the same material", first.material(), again.material());

        ChunkVein otherChunk = VeinGenerator.generate(12345L, 11, 20, materials);
        ChunkVein otherSeed = VeinGenerator.generate(99999L, 10, 20, materials);
        yes("a different chunk differs", !first.equals(otherChunk));
        yes("a different seed differs", !first.equals(otherSeed));

        // The version is part of the hash, which is what lets a future change be made safely.
        yes("the vein records its version", first.version() == VeinGenerator.VERSION);

        yes("an empty pack yields no vein",
                VeinGenerator.generate(1L, 0, 0, List.of()) == null);

        // Richness must stay inside its stated band for every chunk, not just on average.
        for (int x = -50; x < 50; x++) {
            ChunkVein vein = VeinGenerator.generate(7L, x, x * 3, materials);
            if (vein.richness() < VeinGenerator.MIN_RICHNESS
                    || vein.richness() > VeinGenerator.MAX_RICHNESS) {
                fail("richness out of band at x=" + x + ": " + vein.richness());
                return;
            }
        }
        pass("richness stays inside its band across 100 chunks");
    }

    private static void distribution() {
        List<Material> materials = materials();
        Map<String, Integer> counts = new HashMap<>();
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            ChunkVein vein = VeinGenerator.generate(42L, i % 200, i / 200, materials);
            counts.merge(vein.material(), 1, Integer::sum);
        }

        yes("every material appears somewhere", counts.size() == 4);

        // Weights are 200/200/100/5. Rare materials must actually be rare, or finding one stops
        // being an event; common ones must actually be common, or the early game stalls.
        int copper = counts.getOrDefault("copper", 0);
        int diamond = counts.getOrDefault("diamond", 0);
        yes("common materials are common (copper " + copper + "/" + samples + ")",
                copper > samples / 6);
        yes("rare materials are rare (diamond " + diamond + "/" + samples + ")",
                diamond < samples / 20);
        yes("but rare materials do exist", diamond > 0);

        // A weak hash would stripe the map: long runs of one material along an axis. That is the
        // pattern a player notices immediately, and it kills the "every chunk is a find" feel.
        int longestRun = 1;
        int run = 1;
        String previous = null;
        for (int x = 0; x < 500; x++) {
            String material = VeinGenerator.generate(99L, x, 0, materials).material();
            if (material.equals(previous)) {
                run++;
                longestRun = Math.max(longestRun, run);
            } else {
                run = 1;
            }
            previous = material;
        }
        yes("no striping along an axis (longest run " + longestRun + ")", longestRun <= 8);
    }

    private static void depletion() {
        ChunkVein vein = new ChunkVein("copper", 1.0, 10_000L, 1);

        eq("a fresh vein is at full rate", 1.0, vein.rateAfter(0L));
        eq("a fresh vein is undepleted", 0.0, vein.depletion(0L));
        no("and is not exhausted", vein.isExhausted(0L));

        // The curve is gentle early: this is the number that decides whether a player feels
        // permanently on a clock.
        double half = vein.rateAfter(5_000L);
        yes("half the reserve still yields ~83% (got " + round(half) + ")",
                half > 0.80 && half < 0.85);

        double quarter = vein.rateAfter(2_500L);
        yes("a quarter gone is barely felt (got " + round(quarter) + ")", quarter > 0.95);

        // And steep late, so depletion is eventually a real signal.
        double threeQuarters = vein.rateAfter(7_500L);
        yes("three quarters gone is noticeable (got " + round(threeQuarters) + ")",
                threeQuarters < 0.75 && threeQuarters > 0.55);

        eq("a worked-out vein sits at the floor", ChunkVein.FLOOR_FRACTION,
                vein.rateAfter(10_000L));
        eq("and stays there however much more is taken", ChunkVein.FLOOR_FRACTION,
                vein.rateAfter(10_000_000L));
        yes("which is a real contribution, not a token", ChunkVein.FLOOR_FRACTION >= 0.25);
        yes("the vein reports itself exhausted", vein.isExhausted(10_000L));

        // Monotonic: a vein must never yield more after being worked.
        double previous = Double.MAX_VALUE;
        for (long taken = 0; taken <= 12_000L; taken += 250L) {
            double rate = vein.rateAfter(taken);
            if (rate > previous + 1e-9) {
                fail("rate increased at " + taken);
                return;
            }
            previous = rate;
        }
        pass("the rate never increases as the vein is worked");

        // Richness multiplies throughput.
        ChunkVein rich = new ChunkVein("copper", 2.0, 20_000L, 1);
        eq("a rich vein yields double", 2.0, rich.rateAfter(0L));
    }

    private static void deepBore() {
        ChunkVein vein = new ChunkVein("copper", 1.0, 10_000L, 1);
        UpgradeSet none = UpgradeSet.EMPTY;
        eq("no upgrade leaves the natural floor", ChunkVein.FLOOR_FRACTION,
                none.veinFloor(ChunkVein.FLOOR_FRACTION));

        UpgradeSet one = none.install(MachineUpgrade.DEEP_BORE, ChassisMark.MK_II);
        eq("one Deep Bore reaches 45%", 0.45, one.veinFloor(ChunkVein.FLOOR_FRACTION));
        UpgradeSet two = one.install(MachineUpgrade.DEEP_BORE, ChassisMark.MK_II);
        eq("two reach 60%", 0.60, two.veinFloor(ChunkVein.FLOOR_FRACTION));

        // The cap is what keeps expansion necessary: a chunk must never become infinite.
        UpgradeSet many = two;
        for (int i = 0; i < 4; i++) {
            many = many.install(MachineUpgrade.DEEP_BORE, ChassisMark.MK_V);
        }
        eq("the floor is capped", UpgradeSet.MAX_BORE_FLOOR,
                many.veinFloor(ChunkVein.FLOOR_FRACTION));
        yes("and the cap leaves a real reason to expand", UpgradeSet.MAX_BORE_FLOOR < 1.0);

        // It buys a worked vein a great deal...
        double wornNatural = vein.rateAfter(10_000L, ChunkVein.FLOOR_FRACTION);
        double wornBored = vein.rateAfter(10_000L, two.veinFloor(ChunkVein.FLOOR_FRACTION));
        yes("a worked vein gains a lot from Deep Bore", wornBored > wornNatural * 1.9);

        // ...and a fresh one nothing at all, which is what makes it a decision.
        double freshNatural = vein.rateAfter(0L, ChunkVein.FLOOR_FRACTION);
        double freshBored = vein.rateAfter(0L, two.veinFloor(ChunkVein.FLOOR_FRACTION));
        eq("a fresh vein gains nothing from Deep Bore", freshNatural, freshBored);
    }

    private static void lifetime() {
        List<Material> materials = materials();

        // Richness scales reserve as well as rate, so lifetime is independent of richness and a
        // rich chunk is a find rather than a countdown.
        ChunkVein poor = new ChunkVein("copper", 0.5, (long) (VeinGenerator.BASE_RESERVE * 0.5), 1);
        ChunkVein rich = new ChunkVein("copper", 2.0, (long) (VeinGenerator.BASE_RESERVE * 2.0), 1);
        eq("a poor and a rich vein deplete at the same point",
                poor.depletion(poor.reserve()), rich.depletion(rich.reserve()));
        yes("but the rich one yields four times as much",
                Math.abs(rich.rateAfter(0L) / poor.rateAfter(0L) - 4.0) < 1e-9);

        // The headline number: a vein must outlast a player's interest in watching it.
        // At the T1 reference rate of one unit per five seconds.
        double unitsPerHour = 3600.0 / 5.0;
        double hours = VeinGenerator.BASE_RESERVE / unitsPerHour;
        yes("a vein holds tens of hours of T1 extraction (got " + Math.round(hours) + "h)",
                hours >= 35.0);

        // And every generated vein really is that size, not just the base constant.
        for (int x = 0; x < 100; x++) {
            ChunkVein vein = VeinGenerator.generate(5L, x, -x, materials);
            double veinHours = vein.reserve() / (unitsPerHour * vein.richness());
            if (veinHours < 35.0) {
                fail("vein at x=" + x + " lasts only " + Math.round(veinHours) + "h");
                return;
            }
        }
        pass("every generated vein lasts at least 35 hours at its own rate");
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
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
