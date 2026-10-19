package io.github.ezequiel24123z.grindless.energy;

import io.github.ezequiel24123z.grindless.fluid.FluidBuffer;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.machine.GeneratorKind;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ThermalGeneratorBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Behaviour checks for the Flux energy layer. Not part of the mod. */
public final class VerifyEnergy {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        storage();
        rateLimits();
        conversion();
        tiers();
        itemFuel();
        gasFuel();
        gasReturns();

        System.out.println(failures == 0
                ? "ALL ENERGY CHECKS PASSED"
                : failures + " ENERGY CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void itemFuel() throws Exception {
        for (GeneratorKind kind : GeneratorKind.values()) {
            ThermalGeneratorBlockEntity generator = generator(kind);
            eq(kind + " keeps its rated energy capacity", kind.tier().nominal() * 20 * 10,
                    generator.energy().getCapacity());
            no(kind + " still refuses external energy insertion", generator.energy().canReceive());
            generator.energy().setStored(1000);
            eq(kind + " still exports at its rated rate", kind.tier().nominal(),
                    generator.energy().extract(1000, false));
            for (int ticks : new int[] {200, 1600}) {
                generator.menuContainer().setItem(0, new ItemStack(Items.CHARCOAL, 2));
                yes(kind + " admits the furnace-provided item burn time",
                        (boolean) invoke(generator, "tryIgniteItem", new Class<?>[] {int.class}, ticks));
                eq(kind + " consumes one item, not the whole stack", 1,
                        generator.menuContainer().getItem(0).getCount());
                eq(kind + " keeps the supplied burn duration", ticks, integer(generator, "burnDuration"));
                eq(kind + " starts with all paid burn ticks remaining", ticks, integer(generator, "burnRemaining"));
                no(kind + " rejects non-fuel without consumption",
                        (boolean) invoke(generator, "tryIgniteItem", new Class<?>[] {int.class}, 0));
                eq(kind + " keeps the rejected item", 1, generator.menuContainer().getItem(0).getCount());
                eq(kind + " keeps an existing burn on rejection", ticks, integer(generator, "burnRemaining"));
            }
            eq(kind + " retains the one item slot", 1, generator.menuContainer().getContainerSize());
        }
        yes("F1 accepts gas", GeneratorKind.THERMAL.acceptsGas());
        no("F0 remains item-only", GeneratorKind.BIOMASS.acceptsGas());
        no("the generator exposes no fluid endpoint",
                FluidEndpoint.class.isAssignableFrom(ThermalGeneratorBlockEntity.class));
        yes("gas storage is private", Modifier.isPrivate(
                ThermalGeneratorBlockEntity.class.getDeclaredField("gas").getModifiers()));
    }

    private static void gasFuel() throws Exception {
        ThermalGeneratorBlockEntity generator = generator(GeneratorKind.THERMAL);
        Object gas = field(generator, "gas");
        Endpoint source = new Endpoint(FluidLogic.CARBON_MONOXIDE, 1000, 250);
        int ignitions = 0;
        for (int attempt = 1; attempt <= 4; attempt++) {
            collect(gas, source);
            boolean ignited = (boolean) invoke(generator, "tryIgniteGas", new Class<?>[0]);
            if (ignited) {
                ignitions++;
            }
            eq("only attempt four ignites", attempt == 4, ignited);
            eq("partial extraction conserves every mB", 1000,
                    source.buffer.state().millibuckets() + state(gas, "stored").millibuckets()
                            + ignitions * FluidLogic.BUCKET);
            eq("each request is bounded by missing gas", 1000 - (attempt - 1) * 250, source.lastRequested);
        }
        eq("four 250 mB deliveries buy exactly one ignition", 1, ignitions);
        eq("one bucket buys the existing CO burn time", FluidLogic.CO_BURN_TICKS,
                integer(generator, "burnRemaining"));
        yes("the ignited private buffer is empty", state(gas, "stored").isEmpty());

        ThermalGeneratorBlockEntity partial = generator(GeneratorKind.THERMAL);
        Object held = field(partial, "gas");
        Endpoint dry = new Endpoint(FluidLogic.CARBON_MONOXIDE, 250, 250);
        yes("less than a bucket is eligible for collection",
                (boolean) invoke(held, "canCollect", new Class<?>[] {FluidState.class}, dry.contents()));
        collect(held, dry);
        for (int attempt = 0; attempt < 4; attempt++) {
            collect(held, dry);
            no("a dry source never ignites partial gas",
                    (boolean) invoke(partial, "tryIgniteGas", new Class<?>[0]));
        }
        eq("dry-source backpressure retains the partial bucket", 250, state(held, "stored").millibuckets());
        eq("an empty source is not drained again", 1, dry.extractions);
        no("partial gas alone is not ignition-ready", (boolean) invoke(held, "ready", new Class<?>[0]));
        no("no available gas means no collection eligibility",
                (boolean) invoke(held, "canCollect", new Class<?>[] {FluidState.class}, dry.contents()));
        invoke(partial, "observeStatus", new Class<?>[] {boolean.class}, true);
        eq("a previous burning observation is visible", MachineStatus.RUNNING, partial.menuStatus());
        invoke(partial, "observeStatus", new Class<?>[] {boolean.class}, false);
        eq("unsubscribed partial-gas backpressure settles idle", MachineStatus.IDLE, partial.menuStatus());
        Endpoint hydrogen = new Endpoint(FluidLogic.HYDROGEN, 1000, 1000);
        collect(held, hydrogen);
        eq("a partial CO bucket cannot mix hydrogen", 0, hydrogen.extractions);
        eq("refused mixing preserves the source", 1000, hydrogen.buffer.state().millibuckets());

        CompoundTag saved = roundTrip(partial.saveWithoutMetadata());
        yes("the gas save is additive", saved.contains("GasFuel", 10) && saved.contains("Fuel", 9));
        ThermalGeneratorBlockEntity restored = generator(GeneratorKind.THERMAL);
        restored.load(saved);
        eq("binary NBT retains the exact partial gas state", state(held, "stored"),
                state(field(restored, "gas"), "stored"));
        CompoundTag legacy = saved.copy();
        legacy.remove("GasFuel");
        restored.load(roundTrip(legacy));
        yes("an old save loads an empty gas buffer", state(field(restored, "gas"), "stored").isEmpty());
        yes("an old save also clears any gas return", state(field(restored, "gas"), "pendingReturn").isEmpty());

        Endpoint remaining = new Endpoint(FluidLogic.CARBON_MONOXIDE, 750, 750);
        collect(held, remaining);
        yes("compatible gas from another endpoint completes the bucket",
                (boolean) invoke(partial, "tryIgniteGas", new Class<?>[0]));
        eq("the completed bucket was consumed exactly once", 0, state(held, "stored").millibuckets());

        ThermalGeneratorBlockEntity hydrogenGenerator = generator(GeneratorKind.THERMAL);
        collect(field(hydrogenGenerator, "gas"), hydrogen);
        yes("hydrogen still ignites", (boolean) invoke(hydrogenGenerator, "tryIgniteGas", new Class<?>[0]));
        eq("hydrogen keeps its existing burn duration", FluidLogic.HYDROGEN_BURN_TICKS,
                integer(hydrogenGenerator, "burnDuration"));
    }

    private static void gasReturns() throws Exception {
        ThermalGeneratorBlockEntity generator = generator(GeneratorKind.THERMAL);
        Object gas = field(generator, "gas");
        Endpoint wrong = new Endpoint(FluidLogic.WATER, 1000, 250);
        wrong.advertisedId = FluidLogic.CARBON_MONOXIDE;
        collect(gas, wrong);
        eq("wrong-id delivery returns to its source without loss", 1000, wrong.buffer.state().millibuckets());
        eq("wrong-id refund uses the same extraction face", Direction.WEST, wrong.lastReturnFace);
        yes("wrong-id gas never enters the fuel buffer", state(gas, "stored").isEmpty());
        yes("a completed wrong-id refund leaves no escrow", state(gas, "pendingReturn").isEmpty());
        no("a wrong-id delivery never ignites", (boolean) invoke(generator, "tryIgniteGas", new Class<?>[0]));

        wrong.refundLimit = 0;
        collect(gas, wrong);
        eq("a refused wrong-id refund is kept privately", 250, state(gas, "pendingReturn").millibuckets());
        eq("the source plus escrow conserves mass", 1000,
                wrong.buffer.state().millibuckets() + state(gas, "pendingReturn").millibuckets());
        yes("a blocked gas refund has a visible fault", !generator.menuFault().getString().isEmpty());
        invoke(generator, "observeStatus", new Class<?>[] {boolean.class}, false);
        eq("a blocked refund publishes BLOCKED", MachineStatus.BLOCKED, generator.menuStatus());
        Endpoint fresh = new Endpoint(FluidLogic.CARBON_MONOXIDE, 1000, 1000);
        collect(gas, fresh);
        eq("pending return prevents further extraction", 0, fresh.extractions);
        ThermalGeneratorBlockEntity restored = generator(GeneratorKind.THERMAL);
        restored.load(roundTrip(generator.saveWithoutMetadata()));
        Object restoredGas = field(restored, "gas");
        eq("NBT retains blocked-return identity and volume", state(gas, "pendingReturn"),
                state(restoredGas, "pendingReturn"));
        eq("NBT retains the source face", Direction.WEST, field(restoredGas, "returnFace"));
        invoke(restored, "observeStatus", new Class<?>[] {boolean.class}, false);
        eq("a restored blocked refund remains BLOCKED", MachineStatus.BLOCKED, restored.menuStatus());
        wrong.refundLimit = 100;
        invoke(restoredGas, "returnTo", new Class<?>[] {FluidEndpoint.class}, wrong);
        eq("a partial refund retains its exact remainder", 150, state(restoredGas, "pendingReturn").millibuckets());
        eq("partial refund still conserves mass", 1000,
                wrong.buffer.state().millibuckets() + state(restoredGas, "pendingReturn").millibuckets());
        wrong.refundLimit = Integer.MAX_VALUE;
        invoke(restoredGas, "returnTo", new Class<?>[] {FluidEndpoint.class}, wrong);
        eq("unblocking returns every last mB", 1000, wrong.buffer.state().millibuckets());
        yes("a successful refund clears its visible fault", restored.menuFault().getString().isEmpty());
        invoke(restored, "observeStatus", new Class<?>[] {boolean.class}, false);
        eq("a settled refund cannot leave an unsubscribed BLOCKED display", MachineStatus.IDLE, restored.menuStatus());

        ThermalGeneratorBlockEntity excessGenerator = generator(GeneratorKind.THERMAL);
        Object excessGas = field(excessGenerator, "gas");
        Endpoint excess = new Endpoint(FluidLogic.CARBON_MONOXIDE, 2000, Integer.MAX_VALUE);
        excess.excess = 250;
        collect(excessGas, excess);
        eq("over-delivery never increases the private fuel capacity", 1000, state(excessGas, "stored").millibuckets());
        eq("over-delivery is refunded to the same source", 1000, excess.buffer.state().millibuckets());
        yes("the retained exact bucket can ignite", (boolean) invoke(excessGenerator, "tryIgniteGas", new Class<?>[0]));
        eq("over-delivery preserves source plus burned mass", 2000,
                excess.buffer.state().millibuckets() + FluidLogic.BUCKET);

        excess.refundLimit = 0;
        excess.buffer.set(FluidState.of(FluidLogic.CARBON_MONOXIDE, 2000));
        collect(excessGas, excess);
        eq("a refused excess refund keeps a one-bucket fuel buffer", 1000,
                state(excessGas, "stored").millibuckets());
        eq("the excess remains non-burnable escrow", 250, state(excessGas, "pendingReturn").millibuckets());
        no("unsettled extraction cannot ignite", (boolean) invoke(excessGenerator, "tryIgniteGas", new Class<?>[0]));
        eq("source, fuel and refused excess remain conserved", 2000, excess.buffer.state().millibuckets()
                + state(excessGas, "stored").millibuckets() + state(excessGas, "pendingReturn").millibuckets());
        restored.load(roundTrip(excessGenerator.saveWithoutMetadata()));
        eq("NBT preserves fuel beside an unsettled refund", 1000,
                state(field(restored, "gas"), "stored").millibuckets());
        eq("NBT preserves excess beside the fuel buffer", 250,
                state(field(restored, "gas"), "pendingReturn").millibuckets());
    }

    private static ThermalGeneratorBlockEntity generator(GeneratorKind kind) throws Exception {
        Constructor<ThermalGeneratorBlockEntity> constructor = ThermalGeneratorBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, GeneratorKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, kind, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static void collect(Object gas, Endpoint endpoint) throws Exception {
        invoke(gas, "collect", new Class<?>[] {FluidEndpoint.class, Direction.class}, endpoint, Direction.WEST);
    }

    private static Object invoke(Object owner, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(owner, arguments);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static FluidState state(Object owner, String name) throws Exception {
        return (FluidState) field(owner, name);
    }

    private static int integer(Object owner, String name) throws Exception {
        return (int) field(owner, name);
    }

    private static CompoundTag roundTrip(CompoundTag tag) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return NbtIo.read(input);
        }
    }

    private static final class Endpoint implements FluidEndpoint {
        private final FluidBuffer buffer = FluidBuffer.ambient(16_000);
        private final int rate;
        private String advertisedId;
        private int refundLimit = Integer.MAX_VALUE;
        private int excess;
        private int extractions;
        private int lastRequested;
        private Direction lastReturnFace;

        private Endpoint(String id, int amount, int rate) {
            buffer.set(FluidState.of(id, amount));
            this.rate = rate;
        }

        @Override
        public boolean canInsert(Direction from, FluidState state) {
            return refundLimit > 0 && buffer.accepted(state) > 0;
        }

        @Override
        public FluidState insert(Direction from, FluidState state) {
            lastReturnFace = from;
            int amount = Math.min(refundLimit, buffer.accepted(state));
            buffer.offer(state.withAmount(amount));
            return state.withAmount(state.millibuckets() - amount);
        }

        @Override
        public boolean canExtract(Direction from) {
            return !buffer.isEmpty();
        }

        @Override
        public FluidState extract(Direction from, int millibuckets) {
            extractions++;
            lastRequested = millibuckets;
            return buffer.extract(null, Math.min(rate, millibuckets + excess));
        }

        @Override
        public FluidState contents() {
            return advertisedId == null || buffer.isEmpty() ? buffer.state()
                    : FluidState.of(advertisedId, buffer.state().millibuckets());
        }

        @Override
        public int capacity() {
            return buffer.capacity();
        }
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
        SimpleFluxStorage fullProcess = new SimpleFluxStorage(1280L, FluxTier.F1.nominal(), 0L,
                () -> changes[0]++);
        fullProcess.setStored(1280L);
        int fullNotifications = changes[0];
        eq("the reload fixture's full F1 consumer accepts no donor energy", 0L,
                fullProcess.receive(FluxTier.F1.nominal(), false));
        eq("a positive donor offer to a full consumer cannot wake subscriptions",
                fullNotifications, changes[0]);
        fullProcess.setStored(1280L - FluxTier.F1.nominal());
        eq("only after paid work may the donor refill one F1 tick", FluxTier.F1.nominal(),
                fullProcess.receive(FluxTier.F1.nominal(), false));
        eq("the real withdrawal and refill each notify once", fullNotifications + 2, changes[0]);
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
