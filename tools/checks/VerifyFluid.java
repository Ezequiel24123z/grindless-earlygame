package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.container.SideMode;
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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BooleanSupplier;

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
        nativeBatches();
        completionRevalidation();
        completedWorkRecovery();

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
        yes("the real completion commits its reserved non-vented output", complete(trough, batch, List.of()));
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

    private static void nativeBatches() {
        NativeEndpoint primary = new NativeEndpoint(FluidState.of(ProcessLogic.HYDROGEN, 100), 70);
        NativeEndpoint first = new NativeEndpoint(FluidState.of(FluidLogic.WATER, 80, 47.0, 0.3), 60);
        NativeEndpoint second = new NativeEndpoint(FluidState.of(FluidLogic.WATER, 70, 29.0, 0.2), 40);
        List<IngredientSpec> demand = List.of(IngredientSpec.fluid(ProcessLogic.HYDROGEN, 100),
                IngredientSpec.fluid(FluidLogic.WATER, 150));
        List<FluidInsert.Source> split = List.of(source(first, Direction.NORTH), source(second, Direction.NORTH));
        boolean[] settled = {false};
        primary.afterChanged = () -> yes("primary notification observes settled consumption", settled[0]);
        first.afterChanged = () -> yes("split-source notification observes all withdrawals",
                settled[0] && primary.contents().isEmpty() && second.contents().isEmpty());
        no("extract-only sources reject ordinary insertion", first.canInsert(Direction.NORTH, first.contents()));
        NativeFluidStore.Batch batch = FluidInsert.plan(primary.nativeStore(), demand, split).orElseThrow();
        eq("planning does not withdraw from the primary", 100, primary.contents().millibuckets());
        eq("planning does not withdraw from a secondary", 80, first.contents().millibuckets());
        yes("split extract-only sources pay one exact native batch", batch.commit(() -> settled[0] = true));
        eq("primary withdrawal debits its quota", 0, primary.quota);
        eq("first split receipt debits its quota", 0, first.quota);
        eq("second split receipt debits its quota", 0, second.quota);
        eq("a backing owner notifies once after commit", 1, first.notifications);
        eq("native withdrawal never calls external extract", 0, first.extractCalls + second.extractCalls);
        eq("native withdrawal never attempts rollback by insert", 0, first.insertCalls + second.insertCalls);

        NativeEndpoint shared = new NativeEndpoint(FluidState.of(FluidLogic.WATER, 100), 80);
        NativeEndpoint alias = new NativeEndpoint(shared);
        primary.buffer.set(FluidState.of(ProcessLogic.HYDROGEN, 1));
        List<FluidInsert.Source> aliases = List.of(source(shared, Direction.NORTH), source(alias, Direction.NORTH));
        eq("selection counts backing aliases only once", 1, FluidInsert.available(aliases).size());
        no("backing aliases cannot manufacture a second payment",
                FluidInsert.plan(primary.nativeStore(), List.of(IngredientSpec.fluid(ProcessLogic.HYDROGEN, 1),
                        IngredientSpec.fluid(FluidLogic.WATER, 150)), aliases).isPresent());
        no("the owner planner also rejects duplicate aliases across requests", NativeFluidStore.plan(List.of(
                new NativeFluidStore.Request(FluidLogic.WATER, 80,
                        List.of(new NativeFluidStore.Access(shared.nativeStore(), () -> true))),
                new NativeFluidStore.Request(FluidLogic.WATER, 80,
                        List.of(new NativeFluidStore.Access(alias.nativeStore(), () -> true))))).isPresent());
        eq("a refused alias plan preserves all backing stock", 100, shared.contents().millibuckets());

        NativeEndpoint unsupported = new NativeEndpoint(FluidState.of(FluidLogic.WATER, 1000), 0);
        unsupported.nativeEnabled = false;
        List<FluidInsert.Source> filtered = List.of(source(shared, Direction.SOUTH),
                source(unsupported, Direction.NORTH), source(shared, Direction.NORTH));
        eq("selection applies the same native and face filter", List.of(shared.contents()),
                FluidInsert.available(filtered));
        List<IngredientSpec> filteredDemand = List.of(IngredientSpec.fluid(ProcessLogic.HYDROGEN, 1),
                IngredientSpec.fluid(FluidLogic.WATER, 50));
        no("an unsupported handler cannot enter the native withdrawal plan",
                FluidInsert.plan(primary.nativeStore(), filteredDemand,
                        List.of(source(unsupported, Direction.NORTH))).isPresent());
        no("planning rejects the same face that selection rejects",
                FluidInsert.plan(primary.nativeStore(), filteredDemand,
                        List.of(source(shared, Direction.SOUTH))).isPresent());
        eq("unsupported source handlers remain untouched", 0, unsupported.extractCalls + unsupported.insertCalls);

        FluidEndpoint[] selected = {shared};
        FluidInsert.Source replaceable = new FluidInsert.Source(Direction.NORTH, () -> selected[0]);
        NativeFluidStore.Batch staleIdentity = FluidInsert.plan(shared.nativeStore(),
                List.of(IngredientSpec.fluid(FluidLogic.WATER, 20),
                        IngredientSpec.fluid(FluidLogic.WATER, 20)), List.of(replaceable)).orElseThrow();
        selected[0] = alias;
        no("a replaced endpoint identity aborts even with the same backing",
                staleIdentity.commit(() -> fail("stale identity must not settle")));
        eq("identity failure preserves source quota", 80, shared.quota);
        eq("identity failure sends no tentative notification", 0, shared.notifications);

        NativeFluidStore.Batch staleState = NativeFluidStore.plan(List.of(new NativeFluidStore.Request(
                FluidLogic.WATER, 50, List.of(new NativeFluidStore.Access(shared.nativeStore(), () -> true)))))
                .orElseThrow();
        FluidState replacement = FluidState.of(FluidLogic.WATER, 100, 66.0, 0.4);
        shared.buffer.set(replacement);
        no("a changed temperature/pressure snapshot aborts before mutation",
                staleState.commit(() -> fail("stale state must not settle")));
        eq("a stale plan does not overwrite a newer full fluid state", replacement, shared.contents());
        NativeFluidStore.Batch staleQuota = NativeFluidStore.plan(List.of(new NativeFluidStore.Request(
                FluidLogic.WATER, 50, List.of(new NativeFluidStore.Access(shared.nativeStore(), () -> true)))))
                .orElseThrow();
        shared.quota = 31;
        no("a changed owner quota also invalidates the snapshot",
                staleQuota.commit(() -> fail("stale quota must not settle")));
        eq("a stale plan preserves the newer owner quota", 31, shared.quota);
        NativeFluidStore.Batch closedFace = FluidInsert.plan(shared.nativeStore(),
                List.of(IngredientSpec.fluid(FluidLogic.WATER, 20),
                        IngredientSpec.fluid(FluidLogic.WATER, 20)), List.of(source(shared, Direction.NORTH)))
                .orElseThrow();
        shared.allowed = () -> false;
        no("a face closed after planning aborts before withdrawal",
                closedFace.commit(() -> fail("closed face must not settle")));
        eq("permission failure preserves full state", replacement, shared.contents());
    }

    private static void completionRevalidation() throws Exception {
        ProcessRecipe fixture = new ProcessRecipe("check/native_completion", MachineFamily.CHEMICAL_REACTOR,
                List.of(IngredientSpec.item("minecraft:dirt", 1),
                        IngredientSpec.fluid(ProcessLogic.HYDROGEN, 100),
                        IngredientSpec.fluid(FluidLogic.WATER, 100),
                        IngredientSpec.fluid(ProcessLogic.OXYGEN, 50)),
                List.of(OutputSpec.item("minecraft:clay_ball", 2)), Double.NaN, null, 20, 4);
        FluidState primary = FluidState.of(ProcessLogic.HYDROGEN, 100, 53.0, 0.4);
        FluidState water = FluidState.of(FluidLogic.WATER, 500, 41.0, 0.3);
        ProcessMachineBlockEntity consumer = machine(ProcessMachineKind.CHEMICAL_REACTOR);
        loadFluid(consumer, primary, 70);
        consumer.setItem(0, new ItemStack(Items.DIRT));
        cacheCompleted(consumer, fixture);
        ProcessMachineBlockEntity producer = machine(ProcessMachineKind.CHEMICAL_REACTOR);
        loadFluid(producer, water, 350);
        producer.containerConfig().setSideMode(Direction.NORTH, SideMode.EXTRACT);
        no("a process output source need not permit return insertion",
                producer.canInsert(Direction.NORTH, water.withAmount(100)));
        NativeEndpoint late = new NativeEndpoint(FluidState.of(ProcessLogic.OXYGEN, 80, 37.0, 0.2), 60);
        List<FluidInsert.Source> sources = List.of(source(producer, Direction.NORTH), source(late, Direction.NORTH));
        int[] probes = {0};
        late.allowed = () -> ++probes[0] < 3;
        no("a late participant contract change aborts actual completion", complete(consumer, fixture, sources));
        eq("late abort restores the consumer's complete primary state", primary, consumer.contents());
        eq("late abort restores the consumer's output quota", 70,
                consumer.saveWithoutMetadata().getInt("FluidOutputQuota"));
        eq("late abort restores an earlier process source's complete state", water, producer.contents());
        eq("late abort restores the earlier process output quota", 350,
                producer.saveWithoutMetadata().getInt("FluidOutputQuota"));
        eq("late abort preserves the untouched participant state",
                FluidState.of(ProcessLogic.OXYGEN, 80, 37.0, 0.2), late.contents());
        eq("late abort leaves paid progress unchanged", 20.0,
                consumer.saveWithoutMetadata().getDouble("Progress"));
        eq("late abort does not consume items", 1, consumer.getItem(0).getCount());
        yes("late abort does not create an item output",
                consumer.getItem(consumer.menuKind().inputs()).isEmpty());
        eq("late abort sends no source notification", 0, late.notifications);
        eq("late abort never calls an external extraction", 0, late.extractCalls);
        eq("late abort never tries insertion-based restoration", 0, late.insertCalls);

        late.allowed = () -> true;
        consumer.removeItem(0, 1);
        no("completion rechecks an item removed from a cached recipe", complete(consumer, fixture, sources));
        eq("missing items preserve primary fluid", primary, consumer.contents());
        eq("missing items preserve every secondary", water, producer.contents());
        consumer.setItem(0, new ItemStack(Items.DIRT));
        for (int slot = consumer.menuKind().inputs(); slot < consumer.getContainerSize(); slot++) {
            consumer.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        no("full item outputs reject completion before any withdrawal", complete(consumer, fixture, sources));
        eq("full output preserves primary fluid", primary, consumer.contents());
        eq("full output preserves secondary quota", 350,
                producer.saveWithoutMetadata().getInt("FluidOutputQuota"));
        for (int slot = consumer.menuKind().inputs(); slot < consumer.getContainerSize(); slot++) {
            consumer.setItem(slot, ItemStack.EMPTY);
        }
        late.afterChanged = () -> {
            eq("notification sees the consumer's settled item output", 2,
                    consumer.getItem(consumer.menuKind().inputs()).getCount());
            eq("notification sees the consumer's settled progress", 0.0,
                    consumer.saveWithoutMetadata().getDouble("Progress"));
            eq("notification sees the earlier producer's settled quota", 250,
                    producer.saveWithoutMetadata().getInt("FluidOutputQuota"));
        };
        yes("completion can retry exactly after a participant recovers", complete(consumer, fixture, sources));
        yes("successful completion consumes the primary exactly once", consumer.contents().isEmpty());
        eq("successful completion retains exact source metadata", water.withAmount(400), producer.contents());
        eq("successful completion takes only its late-source receipt", 30, late.contents().millibuckets());
        eq("successful completion consumes the required item once", 0, consumer.getItem(0).getCount());

        for (int amount : new int[] {0, 99}) {
            ProcessMachineBlockEntity missingPrimary = machine(ProcessMachineKind.CHEMICAL_REACTOR);
            loadFluid(missingPrimary, primary.withAmount(amount), 0);
            missingPrimary.setItem(0, new ItemStack(Items.DIRT));
            cacheCompleted(missingPrimary, fixture);
            NativeEndpoint fullWater = new NativeEndpoint(water, 0);
            NativeEndpoint fullOxygen = new NativeEndpoint(FluidState.of(ProcessLogic.OXYGEN, 80), 0);
            no("missing/short primary rejects actual completion at " + amount, complete(missingPrimary, fixture,
                    List.of(source(fullWater, Direction.NORTH), source(fullOxygen, Direction.NORTH))));
            eq("missing/short primary preserves the item at " + amount, 1,
                    missingPrimary.getItem(0).getCount());
            eq("missing/short primary preserves water at " + amount, water, fullWater.contents());
            eq("missing/short primary preserves oxygen at " + amount, 80, fullOxygen.contents().millibuckets());
        }
    }

    private static void completedWorkRecovery() throws Exception {
        ProcessRecipe recipe = ProcessLookup.recipes().stream().filter(row -> row.id().equals("recombine/water"))
                .findFirst().orElseThrow();
        IngredientSpec primary = recipe.fluidInputs().get(0);
        IngredientSpec secondary = recipe.fluidInputs().get(1);
        ProcessMachineBlockEntity consumer = machine(ProcessMachineKind.CHEMICAL_REACTOR);
        loadFluid(consumer, FluidState.of(primary.id(), primary.count()), 0);
        cacheCompleted(consumer, recipe);
        consumer.energy().setStored(Math.min(512L, consumer.energy().getCapacity()));
        NativeEndpoint neighbour = new NativeEndpoint(FluidState.of(secondary.id(), secondary.count()), 0);
        List<FluidInsert.Source> sources = List.of(source(neighbour, Direction.NORTH));
        neighbour.buffer.set(FluidState.EMPTY);
        long energy = consumer.energy().getStored();
        for (long tick = 1; tick <= 4; tick++) {
            work(consumer, tick, sources);
        }
        eq("a drained cached secondary freezes completed progress", (double) recipe.durationTicks(),
                consumer.saveWithoutMetadata().getDouble("Progress"));
        eq("a drained cached secondary consumes no additional energy", energy, consumer.energy().getStored());
        eq("a drained cached secondary reports missing input, not full output", MachineStatus.STARVED,
                consumer.menuStatus());
        eq("a drained cached secondary retains its selected recipe", recipe.id(),
                consumer.saveWithoutMetadata().getString("Recipe"));
        eq("a drained cached secondary leaves the primary untouched", primary.count(),
                consumer.contents().millibuckets());

        CompoundTag waiting = consumer.saveWithoutMetadata();
        ProcessMachineBlockEntity reloaded = machine(ProcessMachineKind.CHEMICAL_REACTOR);
        reloaded.load(waiting);
        eq("waiting work round-trips without adding persistence keys", waiting.getAllKeys(),
                reloaded.saveWithoutMetadata().getAllKeys());
        work(reloaded, 5, sources);
        eq("loaded completed work remains selected with no secondary", recipe.id(),
                reloaded.saveWithoutMetadata().getString("WorkRecipe"));
        eq("loaded completed work keeps its paid progress", (double) recipe.durationTicks(),
                reloaded.saveWithoutMetadata().getDouble("Progress"));
        neighbour.buffer.set(FluidState.of(secondary.id(), secondary.count()));
        neighbour.afterChanged = () -> {
            yes("source notification sees the consumer's final fluid output",
                    reloaded.contents().is(recipe.fluidOutputs().get(0).id()));
            eq("source notification sees settled output accounting", recipe.fluidOutputs().get(0).count(),
                    reloaded.saveWithoutMetadata().getInt("FluidOutputQuota"));
            eq("source notification sees the completed progress debit", 0.0,
                    reloaded.saveWithoutMetadata().getDouble("Progress"));
        };
        work(reloaded, 6, sources);
        eq("a replenished secondary completes without additional power", energy, reloaded.energy().getStored());
        eq("a replenished secondary consumes the exact required volume", FluidState.EMPTY, neighbour.contents());
        eq("a replenished secondary produces the original recipe output", recipe.fluidOutputs().get(0).count(),
                reloaded.contents().millibuckets());
        eq("completion never called the extract-only handler", 0, neighbour.extractCalls);

        for (int amount : new int[] {0, primary.count() - 1}) {
            ProcessMachineBlockEntity starved = machine(ProcessMachineKind.CHEMICAL_REACTOR);
            loadFluid(starved, FluidState.of(primary.id(), amount), 0);
            cacheCompleted(starved, recipe);
            starved.energy().setStored(Math.min(512L, starved.energy().getCapacity()));
            NativeEndpoint available = new NativeEndpoint(FluidState.of(secondary.id(), secondary.count()), 0);
            List<FluidInsert.Source> availableSources = List.of(source(available, Direction.NORTH));
            long before = starved.energy().getStored();
            work(starved, 10, availableSources);
            eq("missing/short primary reports starvation at " + amount, MachineStatus.STARVED,
                    starved.menuStatus());
            eq("missing/short primary freezes energy at " + amount, before, starved.energy().getStored());
            eq("missing/short primary freezes progress at " + amount, (double) recipe.durationTicks(),
                    starved.saveWithoutMetadata().getDouble("Progress"));
            eq("missing/short primary leaves the secondary at " + amount, secondary.count(),
                    available.contents().millibuckets());
            yes("primary replenishment fits its exact reservation at " + amount, starved.insert(null,
                    FluidState.of(primary.id(), primary.count() - amount)).isEmpty());
            work(starved, 11, availableSources);
            eq("primary replenishment completes the retained recipe at " + amount,
                    recipe.fluidOutputs().get(0).id(), starved.contents().id());
            eq("primary recovery spends no additional energy at " + amount, before, starved.energy().getStored());
        }

        ProcessMachineBlockEntity blocked = machine(ProcessMachineKind.CHEMICAL_REACTOR);
        loadFluid(blocked, FluidState.of(primary.id(), primary.count() + 100), 0);
        cacheCompleted(blocked, recipe);
        NativeEndpoint untouched = new NativeEndpoint(FluidState.of(secondary.id(), secondary.count()), 0);
        work(blocked, 20, List.of(source(untouched, Direction.NORTH)));
        eq("a non-empty incompatible remainder is an output blockage", MachineStatus.BLOCKED, blocked.menuStatus());
        eq("output blockage preserves every secondary", secondary.count(), untouched.contents().millibuckets());
        eq("output blockage preserves completed work", (double) recipe.durationTicks(),
                blocked.saveWithoutMetadata().getDouble("Progress"));
    }

    private static void loadFluid(ProcessMachineBlockEntity machine, FluidState state, int quota) {
        CompoundTag saved = machine.saveWithoutMetadata();
        saved.put("Fluid", FluidNbt.save(state));
        saved.putInt("FluidOutputQuota", quota);
        machine.load(saved);
    }

    private static void cacheCompleted(ProcessMachineBlockEntity machine, ProcessRecipe recipe) throws Exception {
        for (String name : List.of("cachedRecipeId", "workRecipeId")) {
            Field field = ProcessMachineBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(machine, recipe.id());
        }
        Field cached = ProcessMachineBlockEntity.class.getDeclaredField("cached");
        cached.setAccessible(true);
        cached.set(machine, recipe);
        Field progress = ProcessMachineBlockEntity.class.getDeclaredField("progress");
        progress.setAccessible(true);
        progress.setDouble(machine, recipe.durationTicks());
    }

    private static boolean complete(ProcessMachineBlockEntity machine, ProcessRecipe recipe,
                                    List<FluidInsert.Source> sources) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("complete", ProcessRecipe.class, List.class);
        method.setAccessible(true);
        return (boolean) method.invoke(machine, recipe, sources);
    }

    private static void work(ProcessMachineBlockEntity machine, long tick,
                             List<FluidInsert.Source> sources) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
        method.setAccessible(true);
        method.invoke(machine, tick, sources);
    }

    private static FluidInsert.Source source(FluidEndpoint endpoint, Direction face) {
        return new FluidInsert.Source(face, () -> endpoint);
    }

    private static final class NativeEndpoint implements FluidEndpoint {
        private final FluidBuffer buffer;
        private final NativeFluidStore store;
        private int quota;
        private int notifications;
        private int extractCalls;
        private int insertCalls;
        private boolean nativeEnabled = true;
        private BooleanSupplier allowed = () -> true;
        private Runnable afterChanged = () -> { };

        private NativeEndpoint(FluidState contents, int quota) {
            buffer = new FluidBuffer(16_000, 2000.0, 5.0);
            buffer.set(contents);
            this.quota = quota;
            store = new NativeFluidStore(buffer, () -> this.quota, value -> this.quota = value, () -> {
                notifications++;
                afterChanged.run();
            });
        }

        private NativeEndpoint(NativeEndpoint backing) {
            buffer = backing.buffer;
            store = backing.store;
        }

        @Override
        public boolean canInsert(Direction from, FluidState state) {
            return false;
        }

        @Override
        public FluidState insert(Direction from, FluidState state) {
            insertCalls++;
            throw new AssertionError("Native completion must not use insertion-based rollback");
        }

        @Override
        public boolean canExtract(Direction from) {
            return from == Direction.NORTH && allowed.getAsBoolean() && !buffer.isEmpty();
        }

        @Override
        public FluidState extract(Direction from, int millibuckets) {
            extractCalls++;
            throw new AssertionError("Native completion must not call an external extraction handler");
        }

        @Override
        public NativeFluidStore nativeStore() {
            return nativeEnabled ? store : null;
        }

        @Override
        public FluidState contents() {
            return buffer.state();
        }

        @Override
        public int capacity() {
            return buffer.capacity();
        }
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
