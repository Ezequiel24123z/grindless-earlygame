package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.machine.TickSubscription;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** Throwaway check of the container contract. Not part of the mod. */
public final class VerifyContainer {

    private static int failures = 0;

    public static void main(String[] args) {
        listeners();
        sides();
        autoVoid();
        persistence();

        System.out.println(failures == 0
                ? "ALL CONTAINER CHECKS PASSED"
                : failures + " CONTAINER CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void listeners() {
        ChangeListeners listeners = new ChangeListeners();
        yes("starts empty", listeners.isEmpty());

        int[] runs = {0};
        TickSubscription handle = listeners.add(() -> runs[0]++);
        no("not empty once registered", listeners.isEmpty());
        listeners.notifyChanged();
        listeners.notifyChanged();
        eq("fired twice", 2, runs[0]);

        handle.unsubscribe();
        listeners.notifyChanged();
        eq("stopped after unsubscribe", 2, runs[0]);
        yes("empty again", listeners.isEmpty());

        // A listener that cancels itself mid-notify: the owner being unloaded by the very
        // change it was told about is a real sequence, not a hypothetical one.
        ChangeListeners selfCancel = new ChangeListeners();
        int[] cancelRuns = {0};
        TickSubscription[] holder = new TickSubscription[1];
        holder[0] = selfCancel.add(() -> {
            cancelRuns[0]++;
            holder[0].unsubscribe();
        });
        selfCancel.notifyChanged();
        selfCancel.notifyChanged();
        eq("self-cancelling listener fired once", 1, cancelRuns[0]);
        yes("empty after self-cancel", selfCancel.isEmpty());

        ChangeListeners cleared = new ChangeListeners();
        cleared.add(() -> { });
        cleared.clear();
        yes("clear empties", cleared.isEmpty());
    }

    private static void sides() {
        ContainerConfig config = new ContainerConfig();
        yes("defaults to both ways", config.allowsInsert(Direction.NORTH)
                && config.allowsExtract(Direction.NORTH));

        config.setSideMode(Direction.NORTH, SideMode.INSERT);
        yes("insert face accepts", config.allowsInsert(Direction.NORTH));
        no("insert face does not give", config.allowsExtract(Direction.NORTH));
        yes("other faces unaffected", config.allowsExtract(Direction.SOUTH));

        config.setSideMode(Direction.SOUTH, SideMode.NONE);
        no("closed face blocks insert", config.allowsInsert(Direction.SOUTH));
        no("closed face blocks extract", config.allowsExtract(Direction.SOUTH));

        // The machine's own logic is not restricted by face configuration.
        yes("internal access still inserts", config.allowsInsert(null));
        yes("internal access still extracts", config.allowsExtract(null));

        yes("cycling returns to start",
                SideMode.BOTH.cycle().cycle().cycle().cycle() == SideMode.BOTH);

        // Configuration changes must notify: they are subscription inputs too.
        int[] notified = {0};
        config.listeners().add(() -> notified[0]++);
        config.setSideMode(Direction.UP, SideMode.EXTRACT);
        eq("side change notified", 1, notified[0]);
        config.setSideMode(Direction.UP, SideMode.EXTRACT);
        eq("no-op change did not notify", 1, notified[0]);
        config.setBufferTarget(64L);
        eq("buffer target change notified", 2, notified[0]);
    }

    private static void autoVoid() {
        ContainerConfig config = new ContainerConfig();
        no("off by default", config.isAutoVoid());
        eq("voids nothing while off", 0L, config.voidAmount(1000L, true, false));

        config.setAutoVoid(true);
        config.setVoidThreshold(100L);
        yes("defaults to overflow mode", config.voidMode() == VoidMode.OVERFLOW);

        // It trims, it never empties.
        eq("voids only the surplus", 50L, config.voidAmount(150L, true, false));
        eq("nothing at the threshold", 0L, config.voidAmount(100L, true, false));
        eq("nothing below it", 0L, config.voidAmount(40L, true, false));
        yes("reports that it is voiding", config.isVoiding(150L, true, false));
        no("not voiding at the threshold", config.isVoiding(100L, true, false));

        // Never discards the irreplaceable, whatever the settings.
        eq("protected material is never voided", 0L, config.voidAmount(100000L, true, true));
        config.setVoidMode(VoidMode.EVERYTHING);
        eq("not even in EVERYTHING mode", 0L, config.voidAmount(100000L, false, true));

        config.setVoidMode(VoidMode.FILTERED);
        eq("filtered mode voids matches", 50L, config.voidAmount(150L, true, false));
        eq("filtered mode spares non-matches", 0L, config.voidAmount(150L, false, false));

        config.setVoidMode(VoidMode.EVERYTHING);
        eq("everything mode voids non-matches", 50L, config.voidAmount(150L, false, false));

        // A zero threshold still trims rather than emptying: it voids down to zero, which is
        // what was asked for, but only what is above zero.
        config.setVoidThreshold(0L);
        eq("zero threshold voids all surplus", 10L, config.voidAmount(10L, true, false));
        eq("empty container voids nothing", 0L, config.voidAmount(0L, true, false));
    }

    private static void persistence() {
        ContainerConfig config = new ContainerConfig();
        config.setSideMode(Direction.NORTH, SideMode.INSERT);
        config.setSideMode(Direction.DOWN, SideMode.NONE);
        config.setBufferTarget(256L);
        config.setCapacityLimit(1000L);
        config.setAutoVoid(true);
        config.setVoidMode(VoidMode.FILTERED);
        config.setVoidThreshold(64L);
        config.setInsertPriority(5);
        config.setExtractPriority(-3);

        CompoundTag tag = config.save(new CompoundTag());
        ContainerConfig loaded = new ContainerConfig();
        loaded.load(tag);

        yes("side modes survive", loaded.sideMode(Direction.NORTH) == SideMode.INSERT
                && loaded.sideMode(Direction.DOWN) == SideMode.NONE
                && loaded.sideMode(Direction.EAST) == SideMode.BOTH);
        eq("buffer target survives", 256L, loaded.bufferTarget());
        eq("capacity limit survives", 1000L, loaded.capacityLimit());
        yes("auto-void survives", loaded.isAutoVoid());
        yes("void mode survives", loaded.voidMode() == VoidMode.FILTERED);
        eq("void threshold survives", 64L, loaded.voidThreshold());
        eq("insert priority survives", 5, loaded.insertPriority());
        eq("negative extract priority survives", -3, loaded.extractPriority());

        // An unlimited container must not come back limited to zero.
        ContainerConfig unlimited = new ContainerConfig();
        eq("unlimited by default", -1L, unlimited.capacityLimit());
        ContainerConfig reloaded = new ContainerConfig();
        reloaded.load(unlimited.save(new CompoundTag()));
        eq("still unlimited after a round trip", -1L, reloaded.capacityLimit());
        eq("effective capacity is physical", 64L, reloaded.effectiveCapacity(64L));
        reloaded.setCapacityLimit(16L);
        eq("limit caps physical", 16L, reloaded.effectiveCapacity(64L));
        eq("limit never raises physical", 8L, reloaded.effectiveCapacity(8L));

        // A corrupt or future ordinal must fall back, not throw during world load.
        CompoundTag corrupt = config.save(new CompoundTag());
        corrupt.putByte("VoidMode", (byte) 99);
        corrupt.putByteArray("Sides", new byte[]{99, 99, 99, 99, 99, 99});
        ContainerConfig survived = new ContainerConfig();
        try {
            survived.load(corrupt);
            yes("corrupt void mode falls back", survived.voidMode() == VoidMode.OVERFLOW);
            yes("corrupt side mode falls back",
                    survived.sideMode(Direction.NORTH) == SideMode.BOTH);
        } catch (RuntimeException e) {
            fail("corrupt NBT threw " + e.getClass().getSimpleName());
        }

        // A short array must not throw either.
        CompoundTag truncated = config.save(new CompoundTag());
        truncated.putByteArray("Sides", new byte[]{1});
        try {
            new ContainerConfig().load(truncated);
            pass("truncated side array falls back");
        } catch (RuntimeException e) {
            fail("truncated NBT threw " + e.getClass().getSimpleName());
        }
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
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
