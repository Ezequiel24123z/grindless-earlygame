package io.github.ezequiel24123z.grindless.fluid;

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
import java.util.List;

/** Actual native tank storage, persistence, exact container settlement and lifecycle checks. */
public final class VerifyBasicTank {

    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        sidesAndLimits();
        persistence();
        containerSettlement();
        removedEndpoints();
        System.out.println("ALL BASIC TANK CHECKS PASSED (" + checks + ")");
    }

    private static void sidesAndLimits() throws Exception {
        for (Direction face : Direction.values()) {
            BasicTankBlockEntity tank = tank();
            FluidState water = FluidState.of(FluidLogic.WATER, 20_000);
            eq("all six faces simulate the actual 16 B store", 16_000, tank.acceptedAmount(face, water));
            eq("simulation does not fill", FluidState.EMPTY, tank.contents());
            eq("overflow is returned exactly", water.withAmount(4000), tank.insert(face, water));
            eq("the actual store never exceeds capacity", 16_000, tank.contents().millibuckets());
            eq("a full store accepts zero", 0, tank.acceptedAmount(face, water));
            eq("a full store returns the whole incoming stack", water, tank.insert(face, water));
            eq("a negative extraction cannot change stock", FluidState.EMPTY, tank.extract(face, -1));
            eq("zero extraction cannot change stock", FluidState.EMPTY, tank.extract(face, 0));
            eq("the actual drain is bounded by the stored amount", 16_000,
                    tank.extract(face, Integer.MAX_VALUE).millibuckets());
            eq("draining twice cannot duplicate", FluidState.EMPTY, tank.extract(face, 1000));
        }
        BasicTankBlockEntity tank = tank();
        for (FluidState refused : List.of(
                FluidState.of("minecraft:lava", 1000, 1000, 0.1),
                FluidState.of(FluidLogic.WATER, 1000, 41, 0.1),
                FluidState.of(FluidLogic.WATER, 1000, 20, 0.2),
                FluidState.of(FluidLogic.WATER, 1000, Double.NaN, 0.1),
                FluidState.of(FluidLogic.WATER, 1000, 20, Double.NaN),
                FluidState.of(FluidLogic.WATER, 1000, 20, -0.1),
                FluidState.of(FluidLogic.WATER, 1000, -274, 0.1))) {
            eq("invalid state is refused by simulation", 0, tank.acceptedAmount(null, refused));
            eq("invalid state is returned by execution", refused, tank.insert(null, refused));
            eq("invalid insertion cannot alter stock", FluidState.EMPTY, tank.contents());
        }
        eq("null insertion is explicitly empty", FluidState.EMPTY, tank.insert(null, null));
        FluidState gas = FluidState.of(FluidLogic.CARBON_MONOXIDE, 1500, 28, 0.1);
        eq("existing ambient native gas remains supported", FluidState.EMPTY, tank.insert(null, gas));
        eq("different substances are not mixed", 0,
                tank.acceptedAmount(null, FluidState.of(FluidLogic.WATER, 1000)));
        eq("native extraction preserves temperature and pressure", gas.withAmount(500), tank.extract(null, 500));
    }

    private static void persistence() throws Exception {
        BasicTankBlockEntity original = tank();
        FluidState stock = FluidState.of(FluidLogic.CARBON_MONOXIDE, 12_345, 31.5, 0.1);
        original.insert(null, stock);
        CompoundTag saved = roundTrip(original.saveWithoutMetadata());
        eq("the existing Fluid key is retained", stock, FluidNbt.load(saved.getCompound("Fluid")));
        for (int restart = 0; restart < 4; restart++) {
            BasicTankBlockEntity next = tank();
            next.load(saved);
            eq("save/load preserves all fluid metadata", stock, next.contents());
            saved = roundTrip(next.saveWithoutMetadata());
        }
        BasicTankBlockEntity legacy = tank();
        CompoundTag oldFluid = new CompoundTag();
        oldFluid.putString("Id", FluidLogic.WATER);
        oldFluid.putInt("Amount", 1000);
        CompoundTag old = new CompoundTag();
        old.put("Fluid", oldFluid);
        legacy.load(old);
        eq("legacy volume without thermal fields remains ambient water",
                FluidState.of(FluidLogic.WATER, 1000), legacy.contents());
        old.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, 17_000)));
        legacy.load(old);
        eq("legacy overcapacity stock is not silently deleted", 17_000, legacy.contents().millibuckets());
        eq("overcapacity stock cannot accept another drop", 0,
                legacy.acceptedAmount(null, FluidState.of(FluidLogic.WATER, 1)));
        eq("overcapacity stock remains recoverable", 17_000, legacy.extract(null, 17_000).millibuckets());
    }

    private static void containerSettlement() throws Exception {
        BasicTankBlockEntity tank = tank();
        ItemStack[] hand = {new ItemStack(Items.WATER_BUCKET)};
        NativeFluidStore.Settlement emptyBucket = replace(hand, new ItemStack(Items.BUCKET));
        check("one whole container really fills the native store", tank.exchangeContainer(Direction.WEST,
                FluidState.of(FluidLogic.WATER, 1000), true, () -> true, emptyBucket));
        eq("successful input owns exactly one bucket", 1000, tank.contents().millibuckets());
        check("the input container is actually replaced", hand[0].is(Items.BUCKET));
        check("one whole extraction really pays for the filled item", tank.exchangeContainer(Direction.NORTH,
                tank.contents(), false, () -> true, replace(hand, new ItemStack(Items.WATER_BUCKET))));
        eq("the paid native store is empty", FluidState.EMPTY, tank.contents());
        check("the filled output container is delivered", hand[0].is(Items.WATER_BUCKET));
        check("a repeated extraction cannot create a second bucket", !tank.exchangeContainer(Direction.NORTH,
                FluidState.of(FluidLogic.WATER, 1000), false, () -> true, emptyBucket));

        tank.insert(null, FluidState.of(FluidLogic.WATER, 15_500));
        check("a whole bucket cannot disappear into 500 mB of space", !tank.exchangeContainer(Direction.UP,
                FluidState.of(FluidLogic.WATER, 1000), true, () -> true, emptyBucket));
        eq("refused input retains all existing fluid", 15_500, tank.contents().millibuckets());
        check("a partial-capable container may commit exactly 500 mB", tank.exchangeContainer(Direction.UP,
                FluidState.of(FluidLogic.WATER, 500), true, () -> true, emptyBucket));
        eq("a real partial exchange fills only the actual free space", 16_000, tank.contents().millibuckets());
        check("changed inventory aborts before withdrawal", !tank.exchangeContainer(Direction.DOWN,
                FluidState.of(FluidLogic.WATER, 1000), false, () -> false, emptyBucket));
        eq("a stale hand cannot consume fluid", 16_000, tank.contents().millibuckets());

        for (boolean into : List.of(false, true)) {
            BasicTankBlockEntity failing = tank();
            FluidState original = FluidState.of(FluidLogic.WATER, 1234, 29, 0.1);
            failing.insert(null, original);
            ItemStack prior = new ItemStack(Items.BUCKET, 3);
            hand[0] = prior.copy();
            RuntimeException injected = new IllegalStateException("container settlement fixture");
            try {
                failing.exchangeContainer(Direction.SOUTH, FluidState.of(FluidLogic.WATER, 234), into, () -> true,
                        new NativeFluidStore.Settlement() {
                            @Override
                            public void apply() {
                                hand[0] = new ItemStack(Items.WATER_BUCKET);
                                throw injected;
                            }

                            @Override
                            public void restore() {
                                hand[0] = prior.copy();
                            }
                        });
                throw new AssertionError("container exception must propagate");
            } catch (RuntimeException actual) {
                check("the original container failure is not hidden", actual == injected);
            }
            eq("exception restores full native metadata in both directions", original, failing.contents());
            check("exception restores the original container stack", ItemStack.matches(prior, hand[0]));
        }
    }

    private static void removedEndpoints() throws Exception {
        BasicTankBlockEntity tank = tank();
        FluidState stock = FluidState.of(FluidLogic.WATER, 1000);
        tank.insert(null, stock);
        NativeFluidStore.Batch pending = NativeFluidStore.plan(List.of(new NativeFluidStore.Request(
                FluidLogic.WATER, 500, List.of(new NativeFluidStore.Access(tank.nativeStore(),
                        () -> tank.canExtract(Direction.NORTH)))))).orElseThrow();
        tank.setRemoved();
        eq("a retained removed view cannot expose fluid", FluidState.EMPTY, tank.contents());
        eq("a removed view cannot reserve fluid", 0, tank.acceptedAmount(Direction.EAST, stock));
        eq("a removed view cannot insert", stock, tank.insert(Direction.EAST, stock));
        eq("a removed view cannot extract", FluidState.EMPTY, tank.extract(Direction.EAST, 1000));
        check("a removed view exposes no native owner", tank.nativeStore() == null);
        check("a plan from before removal must abort", !pending.commit());
        eq("removal keeps the saved stock for the one break/relocation carrier", stock,
                FluidNbt.load(tank.saveWithoutMetadata().getCompound("Fluid")));
    }

    private static NativeFluidStore.Settlement replace(ItemStack[] hand, ItemStack result) {
        ItemStack before = hand[0].copy();
        return new NativeFluidStore.Settlement() {
            @Override
            public void apply() {
                hand[0] = result.copy();
            }

            @Override
            public void restore() {
                hand[0] = before.copy();
            }
        };
    }

    private static BasicTankBlockEntity tank() throws Exception {
        Constructor<BasicTankBlockEntity> constructor = BasicTankBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static CompoundTag roundTrip(CompoundTag tag) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.write(tag, new DataOutputStream(bytes));
        return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private static void check(String label, boolean passed) {
        checks++;
        if (!passed) {
            throw new AssertionError(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        check(label + ": expected " + expected + ", got " + actual, expected.equals(actual));
    }
}
