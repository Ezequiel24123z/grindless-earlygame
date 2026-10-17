package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Behaviour checks for T1 fluids. Not part of the mod. */
public final class VerifyFluid {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        units();
        stacks();
        gravity();
        buffers();
        capture();
        burn();
        molten();
        pressure();
        processReservations();

        System.out.println(failures == 0
                ? "ALL FLUID CHECKS PASSED"
                : failures + " FLUID CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void processReservations() throws Exception {
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        ProcessMachineBlockEntity collector = machine(ProcessMachineKind.GEOLOGICAL_COLLECTOR);
        FluidState water = FluidState.of(FluidLogic.WATER, 400);
        eq("batch-sized insertion reserves only 250 mB", 250, collector.acceptedAmount(null, water));
        eq("insert returns exactly the unreserved volume", 150, collector.insert(null, water).millibuckets());
        eq("reserved inserted fluid fits without a throw", 250, collector.contents().millibuckets());
        eq("a full input batch accepts no more", 400, collector.insert(null, water).millibuckets());

        CompoundTag overfull = collector.saveWithoutMetadata();
        overfull.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, 3000)));
        overfull.remove("FluidOutputQuota");
        collector.load(overfull);
        eq("overfull NBT is retained rather than silently clipped", 3000, collector.contents().millibuckets());
        eq("overfull NBT cannot reserve an insertion", 0, collector.acceptedAmount(null, water));
        eq("overfull NBT returns an insert intact", water, collector.insert(null, water));
        eq("overfull NBT remains recoverable by extraction", 1000, collector.extract(null, 1000).millibuckets());
        eq("extraction debits the preserved stock exactly", 2000, collector.contents().millibuckets());

        for (int amount : new int[] {0, 1950, 2000, 3000}) {
            FluidBuffer destination = FluidBuffer.ambient(2000);
            destination.set(FluidState.of(FluidLogic.WATER, amount));
            FluidBuffer source = FluidBuffer.ambient(1000);
            source.set(FluidState.of(FluidLogic.WATER, 1000));
            int reserved = Math.min(FluidLogic.CONDUIT_MB_PER_TICK, destination.accepted(source.state()));
            FluidState taken = source.extract(null, Math.min(reserved, 25));
            yes("a rate-limited primary pull fits its reservation at " + amount + " mB",
                    destination.offer(taken).isEmpty());
            eq("a partial primary pull conserves source plus buffer", amount + 1000,
                    source.state().millibuckets() + destination.state().millibuckets());
            yes("an overfull buffer reserves no additional primary fluid",
                    amount <= destination.capacity() || reserved == 0);
        }

        ProcessMachineBlockEntity trough = machine(ProcessMachineKind.SETTLING_TROUGH);
        Field field = ProcessMachineBlockEntity.class.getDeclaredField("fluid");
        field.setAccessible(true);
        FluidBuffer buffer = (FluidBuffer) field.get(trough);
        ProcessRecipe batch = new ProcessRecipe("check/fluid_output", MachineFamily.SETTLING_TROUGH,
                List.of(IngredientSpec.fluid(FluidLogic.WATER, 1000)),
                List.of(OutputSpec.fluid(FluidLogic.WATER, 750)), Double.NaN, null, 160, 4);
        Method canOutput = ProcessMachineBlockEntity.class.getDeclaredMethod("canOutput", ProcessRecipe.class);
        canOutput.setAccessible(true);
        buffer.set(FluidState.of(FluidLogic.WATER, 3000));
        no("an overfull remainder blocks before output commit", (boolean) canOutput.invoke(trough, batch));
        eq("failed output reservation preserves all input", 3000, buffer.state().millibuckets());
        buffer.set(FluidState.of(FluidLogic.WATER, 2250));
        yes("output reserves against the exact post-consumption state", (boolean) canOutput.invoke(trough, batch));
        Method consume = ProcessMachineBlockEntity.class.getDeclaredMethod("consumeFluid", ProcessRecipe.class);
        consume.setAccessible(true);
        consume.invoke(trough, batch);
        yes("reserved non-vented output fits after consumption",
                buffer.offer(FluidLogic.emitted(FluidLogic.WATER, 750)).isEmpty());
        eq("output commit preserves the exact reserved volume", 2000, buffer.state().millibuckets());

        FluidBuffer target = FluidBuffer.ambient(1000);
        target.set(FluidState.of(FluidLogic.WATER, 900));
        FluidState offered = FluidState.of(FluidLogic.WATER, 250);
        FluidState remainder = target.offer(offered);
        int moved = offered.millibuckets() - remainder.millibuckets();
        yes("an in-mod partial insert returns a bounded same-id remainder",
                remainder.is(offered.id()) && moved >= 0 && moved <= offered.millibuckets());
        eq("partial output acceptance leaves every unaccepted mB", 150, remainder.millibuckets());
    }

    private static ProcessMachineBlockEntity machine(ProcessMachineKind kind) throws Exception {
        Constructor<ProcessMachineBlockEntity> constructor = ProcessMachineBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, ProcessMachineKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, kind, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static void units() {
        eq("a bucket is 1000 mB", 1000, FluidLogic.BUCKET);
        eq("ambient is 20 C", 20.0, FluidLogic.AMBIENT_C);
        eq("ambient is 0.1 MPa", 0.1, FluidLogic.AMBIENT_MPA);
        eq("the basic tank holds 16 B", 16_000, FluidLogic.TANK_CAPACITY);
        eq("wet mill takes half a bucket", 500, FluidLogic.WET_WATER_MB);
        eq("the well is five times the hand pump", 100, FluidLogic.WELL_MB_PER_TICK);
        eq("the hand pump stays at 20 mB per tick", 20, FluidLogic.PUMP_MB_PER_TICK);
        eq("R1 names 1 B of CO", 1000, FluidLogic.CO_MB);
        eq("R2 names 1 B of SO2", 1000, FluidLogic.SO2_MB);
        eq("CO id matches the process graph", ProcessLogic.CARBON_MONOXIDE, FluidLogic.CARBON_MONOXIDE);
        eq("SO2 id matches the process graph", ProcessLogic.SULFUR_DIOXIDE, FluidLogic.SULFUR_DIOXIDE);
        eq("SO3 id matches the process graph", ProcessLogic.SULFUR_TRIOXIDE, FluidLogic.SULFUR_TRIOXIDE);
        eq("acid id matches the process graph", ProcessLogic.SULFURIC_ACID, FluidLogic.SULFURIC_ACID);
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

        FluidState so3 = FluidState.of(FluidLogic.SULFUR_TRIOXIDE, 1000);
        yes("ambient SO3 is a gas", FluidLogic.isGas(so3));
        yes("a tank at ambient will hold SO3",
                FluidLogic.accepted(FluidState.EMPTY, so3, FluidLogic.TANK_CAPACITY,
                        FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA) == 1000);
        FluidState acid = FluidState.of(FluidLogic.SULFURIC_ACID, 1000);
        yes("ambient acid is liquid", FluidLogic.isLiquid(acid));
        no("ambient acid is not gas", FluidLogic.isGas(acid));

        FluidState hydrogen = FluidState.of(FluidLogic.HYDROGEN, 1000);
        FluidState oxygen = FluidState.of(FluidLogic.OXYGEN, 1000);
        FluidState nitrogen = FluidState.of(FluidLogic.NITROGEN, 1000);
        yes("ambient hydrogen is a gas", FluidLogic.isGas(hydrogen));
        yes("ambient oxygen is a gas", FluidLogic.isGas(oxygen));
        yes("ambient nitrogen is a gas", FluidLogic.isGas(nitrogen));
        no("hydrogen does not gravity-flow", FluidLogic.canGravityFlow(hydrogen, 70, 69));
        no("oxygen does not gravity-flow", FluidLogic.canGravityFlow(oxygen, 70, 69));

        List<FluidState> neighbours = List.of(FluidState.of(FluidLogic.WATER, 150), so3);
        eq("available sums matching neighbours", 150, FluidLogic.available(neighbours, FluidLogic.WATER));
        no("150 mB is not 0.2 B", FluidLogic.hasAtLeast(neighbours, FluidLogic.WATER, 200));
        yes("150 mB covers 0.1 B", FluidLogic.hasAtLeast(neighbours, FluidLogic.WATER, 100));
        no("SO2 is not in those neighbours", FluidLogic.hasAtLeast(neighbours, FluidLogic.SULFUR_DIOXIDE, 1));

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
        eq("hydrogen burns as long as CO", FluidLogic.CO_BURN_TICKS, FluidLogic.burnTicks(FluidLogic.HYDROGEN));
        eq("CO still burns 400 ticks", FluidLogic.CO_BURN_TICKS, FluidLogic.burnTicks(FluidLogic.CARBON_MONOXIDE));
        eq("oxygen is not a generator fuel", 0, FluidLogic.burnTicks(FluidLogic.OXYGEN));
        eq("nitrogen is not a generator fuel", 0, FluidLogic.burnTicks(FluidLogic.NITROGEN));
        eq("SO2 is not a generator fuel", 0, FluidLogic.burnTicks(FluidLogic.SULFUR_DIOXIDE));
        eq("water is not a generator fuel", 0, FluidLogic.burnTicks(FluidLogic.WATER));
    }

    private static void molten() {
        FluidState melt = FluidLogic.emitted(ProcessLogic.moltenId("iron"), ProcessLogic.MOLTEN_MB);
        eq("one unit of melt is 144 mB", 144, melt.millibuckets());
        eq("melt is emitted at 1000 C", 1000.0, melt.temperatureC());
        no("melt is not ambient", FluidLogic.isAmbient(melt));
        eq("a tank at ambient refuses melt", 0,
                FluidLogic.accepted(FluidState.EMPTY, melt, FluidLogic.TANK_CAPACITY,
                        FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA));
        eq("an ambient buffer refuses melt", 0, FluidBuffer.ambient(2000).accepted(melt));
        FluidBuffer hot = new FluidBuffer(2000, ProcessLogic.MOLTEN_MAX_C, FluidLogic.AMBIENT_MPA);
        eq("a hot buffer accepts one unit of melt", 144, hot.accepted(melt));
        FluidState water = FluidLogic.emitted(FluidLogic.WATER, 1000);
        yes("emitted water stays ambient", FluidLogic.isAmbient(water));
        FluidState co = FluidLogic.emitted(FluidLogic.CARBON_MONOXIDE, 1000);
        yes("emitted CO stays ambient", FluidLogic.isAmbient(co));
        FluidState surfactant = FluidState.of(ProcessLogic.SURFACTANT, 500);
        yes("surfactant is ambient", FluidLogic.isAmbient(surfactant));
        no("surfactant is not a gas", FluidLogic.isGas(surfactant));
        eq("surfactant is not a generator fuel", 0, FluidLogic.burnTicks(ProcessLogic.SURFACTANT));
        FluidState steam = FluidLogic.emitted(ProcessLogic.STEAM, 1000);
        eq("steam is 150 C", 150.0, steam.temperatureC());
        eq("steam is 0.5 MPa", 0.5, steam.pressureMPa());
        yes("steam is a gas", FluidLogic.isGas(steam));
        no("steam is not ambient", FluidLogic.isAmbient(steam));
        eq("a tank refuses steam", 0,
                FluidLogic.accepted(FluidState.EMPTY, steam, FluidLogic.TANK_CAPACITY,
                        FluidLogic.AMBIENT_MAX_C, FluidLogic.AMBIENT_MPA));
        FluidBuffer steamBuffer = new FluidBuffer(2000, ProcessLogic.STEAM_MAX_C, ProcessLogic.STEAM_MAX_MPA);
        eq("a boiler buffer accepts one bucket of steam", 1000, steamBuffer.accepted(steam));
        eq("steam is not a generator fuel", 0, FluidLogic.burnTicks(ProcessLogic.STEAM));
    }

    private static void pressure() {
        eq("the pipe holds 2 B", 2000, PressureLogic.PIPE_CAPACITY);
        eq("the pipe moves 200 mB a tick", 200, PressureLogic.PIPE_MB_PER_TICK);
        eq("the electric pump moves 100 mB a tick", 100, PressureLogic.PUMP_MB_PER_TICK);
        eq("the industrial tank holds 64 B", 64_000, PressureLogic.TANK_CAPACITY);
        eq("the fluid arm moves one bucket", 1000, PressureLogic.MANIPULATOR_MB);
        eq("the T2 ceiling is 1200 C", 1200.0, PressureLogic.MAX_C);
        eq("the T2 ceiling is 1.0 MPa", 1.0, PressureLogic.MAX_MPA);
        FluidState steam = FluidLogic.emitted(ProcessLogic.STEAM, 1000);
        FluidState melt = FluidLogic.emitted(ProcessLogic.moltenId("iron"), ProcessLogic.MOLTEN_MB);
        yes("steam fits the T2 rating", PressureLogic.accepts(steam));
        yes("melt fits the T2 rating", PressureLogic.accepts(melt));
        FluidState superheated = FluidState.of(ProcessLogic.STEAM, 1000, 450.0, 6.0);
        no("superheated steam does not fit", PressureLogic.accepts(superheated));
        FluidBuffer pipe = PressureLogic.buffer(PressureLogic.PIPE_CAPACITY);
        eq("a pressure pipe accepts steam", 1000, pipe.accepted(steam));
        eq("a pressure pipe accepts melt", 144, pipe.accepted(melt));
        eq("a pressure pipe refuses superheated steam", 0, pipe.accepted(superheated));
        eq("clay still refuses steam", 0, FluidBuffer.ambient(FluidLogic.CONDUIT_CAPACITY).accepted(steam));
        eq("the basic tank still refuses steam", 0,
                FluidBuffer.ambient(FluidLogic.TANK_CAPACITY).accepted(steam));
        FluidBuffer tank = PressureLogic.buffer(PressureLogic.TANK_CAPACITY);
        eq("the industrial tank accepts steam", 1000, tank.accepted(steam));
        eq("the industrial tank accepts melt", 144, tank.accepted(melt));
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
