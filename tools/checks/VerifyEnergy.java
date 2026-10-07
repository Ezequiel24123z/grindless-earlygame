package io.github.ezequiel24123z.grindless.energy;

/** Behaviour checks for the Flux energy layer. Not part of the mod. */
public final class VerifyEnergy {

    private static int failures = 0;

    public static void main(String[] args) {
        storage();
        rateLimits();
        conversion();
        tiers();

        System.out.println(failures == 0
                ? "ALL ENERGY CHECKS PASSED"
                : failures + " ENERGY CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void storage() {
        SimpleFluxStorage buffer = new SimpleFluxStorage(1000L, 100L);
        eq("starts empty", 0L, buffer.getStored());
        eq("space is full capacity", 1000L, buffer.getSpace());
        eq("empty reads as zero fill", 0.0, buffer.getFillFraction());

        eq("accepts up to the rate limit", 100L, buffer.receive(500L, false));
        eq("stored what it accepted", 100L, buffer.getStored());

        eq("simulation reports without taking", 100L, buffer.receive(500L, true));
        eq("simulation changed nothing", 100L, buffer.getStored());

        eq("extracts up to the rate limit", 100L, buffer.extract(500L, false));
        eq("back to empty", 0L, buffer.getStored());
        eq("cannot extract from empty", 0L, buffer.extract(100L, false));

        buffer.setStored(1000L);
        eq("fills to capacity", 1000L, buffer.getStored());
        eq("full accepts nothing", 0L, buffer.receive(100L, false));
        eq("full reads as one", 1.0, buffer.getFillFraction());
        buffer.setStored(5000L);
        eq("setStored clamps to capacity", 1000L, buffer.getStored());
        buffer.setStored(-50L);
        eq("setStored clamps at zero", 0L, buffer.getStored());

        eq("negative receive is refused", 0L, buffer.receive(-10L, false));
        eq("negative extract is refused", 0L, buffer.extract(-10L, false));

        // A change callback is how a machine knows to re-evaluate its subscriptions.
        int[] changes = {0};
        SimpleFluxStorage watched = new SimpleFluxStorage(100L, 100L, 100L, () -> changes[0]++);
        watched.receive(50L, true);
        eq("simulation does not notify", 0, changes[0]);
        watched.receive(50L, false);
        eq("a real change notifies", 1, changes[0]);
        watched.receive(0L, false);
        eq("a no-op does not notify", 1, changes[0]);
    }

    private static void rateLimits() {
        // A generator buffer: nothing in from outside, rated output out. This is the shape the
        // Hand Crank Dynamo uses so the capability it exposes holds the power it generated.
        SimpleFluxStorage generator = new SimpleFluxStorage(1000L, 0L, 8L, null);
        no("generator refuses insertion", generator.canReceive());
        yes("generator allows extraction", generator.canExtract());
        eq("insertion really is refused", 0L, generator.receive(500L, false));
        generator.setStored(500L);
        eq("external pull is rate limited", 8L, generator.extract(500L, false));

        // A machine buffer is the opposite: a load, not a battery.
        SimpleFluxStorage load = new SimpleFluxStorage(1000L, 8L, 0L, null);
        yes("load accepts", load.canReceive());
        no("load does not give power back", load.canExtract());
        load.setStored(500L);
        eq("extraction really is refused", 0L, load.extract(500L, false));

        // The bug this check exists for. A generator's own push logic must not route through
        // the rate limit: a throttled push covering five ticks would be capped at one tick's
        // worth, silently running the generator at a fifth of its rating.
        SimpleFluxStorage dynamo = new SimpleFluxStorage(1000L, 0L, 8L, null);
        dynamo.setStored(1000L);
        eq("rate limit caps a multi-tick extract", 8L, dynamo.extract(8L * 5L, true));
        long allowance = Math.min(dynamo.getStored(), 8L * 5L);
        eq("reading the buffer directly does not", 40L, allowance);
        dynamo.setStored(dynamo.getStored() - allowance);
        eq("a period's worth really left the buffer", 960L, dynamo.getStored());
    }

    private static void conversion() {
        // FU and FE are 1:1 and lossless (ADR-0006).
        eq("FU to FE is one to one", 1000, FluxConversion.toFe(1000L));
        eq("FE to FU is one to one", 1000L, FluxConversion.fromFe(1000));
        eq("zero stays zero", 0, FluxConversion.toFe(0L));
        eq("negative FU clamps to zero", 0, FluxConversion.toFe(-5L));
        eq("negative FE clamps to zero", 0L, FluxConversion.fromFe(-5));

        // FU is long and FE is int, so the boundary saturates rather than wrapping. Wrapping
        // would turn a full buffer into a debt, which is the failure ADR-0037 exists to stop.
        eq("over-large FU saturates", Integer.MAX_VALUE, FluxConversion.toFe(Long.MAX_VALUE));
        eq("exactly MAX_VALUE is exact", Integer.MAX_VALUE,
                FluxConversion.toFe((long) Integer.MAX_VALUE));
        yes("saturation is never negative", FluxConversion.toFe(Long.MAX_VALUE) > 0);

        // EU conversion rounds down, because rounding up would let a conversion chain create
        // energy out of nothing.
        eq("EU to FU is exact", 400L, FluxConversion.fromEu(100L, 4));
        eq("FU to EU rounds down", 25L, FluxConversion.toEu(100L, 4));
        eq("FU to EU really rounds down", 25L, FluxConversion.toEu(103L, 4));
        eq("a divide by zero is refused", 0L, FluxConversion.toEu(100L, 0));
    }

    private static void tiers() {
        eq("F0 is the hand crank", 8L, FluxTier.F0.nominal());
        eq("F15 is the ceiling", FluxTier.MAX_NOMINAL, FluxTier.F15.nominal());
        eq("there are sixteen aligned tiers", 16L, FluxTier.values().length);
        for (int i = 1; i < FluxTier.values().length; i++) {
            FluxTier previous = FluxTier.values()[i - 1];
            FluxTier current = FluxTier.values()[i];
            eq(current.name() + " is four times " + previous.name(),
                    previous.nominal() * 4L, current.nominal());
        }
        yes("F15 is the top", FluxTier.F15.next() == FluxTier.F15);
        yes("tiers step up", FluxTier.F0.next() == FluxTier.F1);
        eq("F0 has the aligned name", "Bootstrap", FluxTier.F0.tierName());
        eq("F4 has the aligned name", "Precision", FluxTier.F4.tierName());
        eq("F15 has the aligned name", "Event Horizon", FluxTier.F15.tierName());

        // Voltage names are aliases on one ladder, not a second scale (ADR-0038).
        eq("LV is F1", "LV", FluxTier.F1.voltageAlias());
        eq("IV is F5", "IV", FluxTier.F5.voltageAlias());
        yes("F0 has no voltage name", FluxTier.F0.voltageAlias() == null);
        yes("nor does F6", FluxTier.F6.voltageAlias() == null);

        // Under-volting degrades smoothly rather than stalling: a power shortfall is never a wall.
        eq("matched supply is full speed", 1.0, FluxTier.F3.throughputFactor(FluxTier.F3));
        eq("over-volting gives no bonus", 1.0, FluxTier.F3.throughputFactor(FluxTier.F5));
        eq("one tier down is half speed", 0.5, FluxTier.F3.throughputFactor(FluxTier.F2));
        eq("two tiers down is a quarter", 0.25, FluxTier.F3.throughputFactor(FluxTier.F1));
        yes("far under-volting still runs",
                FluxTier.F15.throughputFactor(FluxTier.F0) > 0.0);

        eq("exact fit picks that tier", FluxTier.F2, FluxTier.forThroughput(128L));
        eq("just over steps up", FluxTier.F3, FluxTier.forThroughput(129L));
        eq("F14 crosses the FE int boundary", 2_147_483_648L, FluxTier.F14.nominal());
        eq("beyond the ladder saturates", FluxTier.F15, FluxTier.forThroughput(Long.MAX_VALUE));
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
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
