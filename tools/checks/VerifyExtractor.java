package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.network.PylonStructure;
import io.github.ezequiel24123z.grindless.vein.VeinData;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

/** Behaviour checks for the Crude Extractor and the pylon tower. Not part of the mod. */
public final class VerifyExtractor {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            cycle();
            status();
            outputForm();
            tower();
            pendingPersistence();
            recoverySourceContract();
        } else if (args.length == 2 && args[0].equals("--a01-vein-data")) {
            savedVeinAccounting(Path.of(args[1]));
        } else {
            throw new IllegalArgumentException("Usage: VerifyExtractor [--a01-vein-data <Luna grindless_veins.dat>]");
        }

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

    private static void recoverySourceContract() throws Exception {
        String source = Files.readString(Path.of("common", "src", "main", "java", "io", "github",
                "ezequiel24123z", "grindless", "machine", "CrudeExtractorBlockEntity.java"));
        String subscriptions = method(source, "protected void updateSubscriptions()");
        yes("owned output subscribes independently of work and vein",
                subscriptions.contains("if (!output.isEmpty())")
                && subscriptions.contains("subscribe(exporting, this::exportWaitingOutput)"));
        yes("empty output releases its exporter",
                subscriptions.contains("exporting.unsubscribe()") && subscriptions.contains("exporting = null"));
        String exporting = method(source, "private void exportWaitingOutput()");
        yes("pending export uses the existing eight-tick retry policy",
                exporting.contains("isDue(8)") && exporting.contains("pushOutput()")
                && exporting.contains("updateSubscriptions()"));
        String work = method(source, "private void work()");
        int guard = work.indexOf("lastWorkTick == tick");
        int power = work.indexOf("requestPower(");
        yes("the world-time guard precedes callbacks and power draw",
                work.contains("getLevel().getGameTime()") && guard >= 0 && power > guard
                && work.indexOf("lastWorkTick = tick") > guard
                && work.indexOf("lastWorkTick = tick") < power);
        int paid = work.indexOf("if (progress >= ExtractorLogic.CYCLE_TICKS)");
        yes("completed paid work is settled before requesting additional power",
                paid > guard && paid < power
                && work.substring(paid, power).contains("complete(product)")
                && work.substring(paid, power).contains("return;"));
        String completion = method(source, "private void complete(ItemStack product)");
        yes("one successful eject debits one cycle and records one extraction",
                completion.contains("if (eject(product))")
                && completion.contains("progress -= ExtractorLogic.CYCLE_TICKS")
                && occurrences(source, "recordExtraction();") == 1
                && occurrences(completion, "recordExtraction();") == 1);
        String pushing = method(source, "private void pushOutput()");
        yes("external insertion receives a copy of owned output",
                pushing.contains("output.getItem(0).copy()"));
        yes("only a changed confirmed remainder notifies the inventory",
                pushing.contains("ItemStack.matches(output.getItem(0), stack)"));
        no("export does not buy work or alter extraction accounting",
                (exporting + pushing).contains("requestPower(")
                        || (exporting + pushing).contains("progress ")
                        || (exporting + pushing).contains("recordExtraction("));
        yes("subscription refresh leaves runnable status observations to work",
                subscriptions.contains("if (vein == null || stuck)"));
    }

    private static void pendingPersistence() throws Exception {
        for (double paid : new double[] {0.0, 123.5, 400.0, 400.5}) {
            CrudeExtractorBlockEntity first = extractor();
            ItemStack owned = new ItemStack(Items.RAW_IRON, 64);
            owned.getOrCreateTag().putString("a01_probe", "owned-output");
            SimpleContainer inventory = new SimpleContainer(1);
            inventory.setItem(0, owned.copy());
            CompoundTag saved = first.saveWithoutMetadata();
            saved.putDouble("Progress", paid);
            saved.put("Output", inventory.createTag());
            saved.getCompound("Energy").putLong("FluxStored", 16L);
            first.load(binaryRoundTrip(saved));

            CompoundTag waiting = first.saveWithoutMetadata();
            CrudeExtractorBlockEntity reconstructed = extractor();
            reconstructed.load(binaryRoundTrip(waiting));
            CompoundTag after = reconstructed.saveWithoutMetadata();
            yes(paid + " paid work retains the existing persistence keys",
                    saved.getAllKeys().equals(after.getAllKeys()));
            eq(paid + " paid work survives binary NBT and reconstruction", paid,
                    after.getDouble("Progress"));
            eq(paid + " pending output preserves stored Flux", 16L, reconstructed.energy().getStored());
            yes(paid + " owned stack and tags survive reconstruction",
                    ItemStack.matches(owned, reconstructed.getItem(0)));
            eq(paid + " pending output remains one slot", 1, reconstructed.getContainerSize());
            no(paid + " output remains extract-only",
                    reconstructed.canPlaceItem(0, new ItemStack(Items.RAW_IRON)));
        }
    }

    private static CrudeExtractorBlockEntity extractor() throws Exception {
        Constructor<CrudeExtractorBlockEntity> constructor =
                CrudeExtractorBlockEntity.class.getDeclaredConstructor(
                        BlockEntityType.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static CompoundTag binaryRoundTrip(CompoundTag tag) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return NbtIo.read(input);
        }
    }

    private static void savedVeinAccounting(Path path) throws Exception {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("The cleanly stopped A01 world's Luna vein data is required: " + path);
        }
        CompoundTag root = NbtIo.readCompressed(path.toFile());
        if (root == null || !root.contains("data", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Expected a vanilla SavedData compound: " + path);
        }
        VeinData saved = VeinData.load(root.getCompound("data"));
        eq("recovered paid cycle records exactly one extraction", 1L,
                saved.extractedFrom(new ChunkPos(0, 0)));
        eq("same-tick guard case records exactly one extraction", 1L,
                saved.extractedFrom(new ChunkPos(1, 0)));
        eq("no other diagnostic chunk was charged an extraction", 2, saved.workedChunkCount());
        eq("completed extraction accounting totals exactly two", 2L,
                saved.worked().values().stream().mapToLong(Long::longValue).sum());
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            return "";
        }
        int body = source.indexOf('{', start);
        int depth = 1;
        for (int end = body + 1; end < source.length(); end++) {
            char next = source.charAt(end);
            if (next == '{') {
                depth++;
            } else if (next == '}' && --depth == 0) {
                return source.substring(body + 1, end);
            }
        }
        throw new IllegalStateException("Unclosed method: " + signature);
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
