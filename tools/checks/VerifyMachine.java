package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.process.Agitation;
import io.github.ezequiel24123z.grindless.process.Atmosphere;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ConditionDimension;
import io.github.ezequiel24123z.grindless.process.ConditionFault;
import io.github.ezequiel24123z.grindless.process.ConditionReport;
import io.github.ezequiel24123z.grindless.process.ConditionState;
import io.github.ezequiel24123z.grindless.process.ProcessField;
import io.github.ezequiel24123z.grindless.process.ProcessConditions;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.material.RegistryTagView;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Throwaway check of the machine layer. Not part of the mod. */
public final class VerifyMachine {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        subscriptions();
        upgrades();
        chassis();
        pushBackoff();
        persistence();
        statuses();
        regulation();
        pressureMatching();
        recipeGenerations();

        System.out.println(failures == 0
                ? "ALL MACHINE CHECKS PASSED"
                : failures + " MACHINE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void recipeGenerations() throws Exception {
        generationIdentity();
        Map<TagKey<Item>, List<Holder<Item>>> originalTags = new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(tag ->
                originalTags.put(tag.getFirst(), tag.getSecond().stream().toList()));
        try {
            rebuildCacheTags(true, Items.SAND);
            ProcessMachineBlockEntity machine = machine(ProcessMachineKind.PULVERIZER);
            machine.setItem(0, new ItemStack(Items.GRAVEL, 2));
            ProcessRecipe original = selected(machine);
            yes("the cache probe uses a real tag-generated recipe",
                    original != null && original.id().equals("b1/cache_probe"));
            if (original == null) {
                return;
            }
            machine.energy().setStored(original.fuPerTick() * 2);
            CompoundTag paid = machine.saveWithoutMetadata();
            paid.putDouble("Progress", original.durationTicks() - 1.0);
            machine.load(paid);
            original = selected(machine);

            rebuildCacheTags(true, Items.SAND);
            ProcessRecipe current = selected(machine);
            yes("an unchanged rebuild rebinds the cache to the published recipe object",
                    current != original && ProcessLookup.recipes().stream().anyMatch(row -> row == current));
            eq("an unchanged rebuild retains already paid work", paid.getDouble("Progress"),
                    machine.saveWithoutMetadata().getDouble("Progress"));
            yes("an unchanged rebuild retains the existing work owner",
                    paid.getString("WorkRecipe").equals(machine.saveWithoutMetadata().getString("WorkRecipe")));
            yes("generation invalidation does not add persistence keys",
                    paid.getAllKeys().equals(machine.saveWithoutMetadata().getAllKeys()));
            Field cached = ProcessMachineBlockEntity.class.getDeclaredField("cached");
            cached.setAccessible(true);
            machine.serverTick();
            yes("the empty subscription path retains a cache from the unchanged generation",
                    machine.subscriptions().isIdle() && cached.get(machine) == current);

            rebuildCacheTags(false, Items.SAND);
            no("removing only the ingot form retires the generated recipe",
                    ProcessLookup.recipes().stream().anyMatch(row -> row.id().equals("b1/cache_probe")));
            yes("the retired recipe's input and output still resolve",
                    ProcessLookup.matches(current, new ItemStack[] {machine.getItem(0)})
                            && ProcessLookup.resolve(current.itemOutputs().get(0)).isPresent());
            machine.serverTick();
            yes("a generation change invalidates the cache even with no subscribed work",
                    machine.subscriptions().isIdle() && cached.get(machine) == null);
            Method work = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
            work.setAccessible(true);
            work.invoke(machine, 1L, List.of());
            eq("a retired cached recipe cannot spend additional Flux", original.fuPerTick() * 2,
                    machine.energy().getStored());
            eq("a retired cached recipe cannot consume its input", 2, machine.getItem(0).getCount());
            yes("a retired cached recipe cannot emit its still-resolvable output",
                    machine.getItem(machine.menuKind().inputs()).isEmpty());
            eq("a missing generation suspends rather than discards paid work", paid.getDouble("Progress"),
                    machine.saveWithoutMetadata().getDouble("Progress"));
            yes("a missing generation retains the persisted work owner",
                    paid.getString("WorkRecipe").equals(machine.saveWithoutMetadata().getString("WorkRecipe")));

            ProcessMachineBlockEntity restored = machine(ProcessMachineKind.PULVERIZER);
            restored.load(machine.saveWithoutMetadata().copy());
            rebuildCacheTags(true, Items.RED_SAND);
            work.invoke(restored, 2L, List.of());
            eq("restored work pays only its one remaining tick", original.fuPerTick(),
                    restored.energy().getStored());
            eq("restored work consumes its input exactly once", 1, restored.getItem(0).getCount());
            ItemStack output = restored.getItem(restored.menuKind().inputs());
            yes("restored work uses the current material provider and exact yield",
                    output.is(Items.RED_SAND) && output.getCount() == 2);
            eq("restored completion debits exactly one paid cycle", 0.0,
                    restored.saveWithoutMetadata().getDouble("Progress"));
            rebuildCacheTags(true, Items.RED_SAND);
            restored.serverTick();
            work.invoke(restored, 2L, List.of());
            eq("a generation refresh cannot charge the same world tick twice", original.fuPerTick(),
                    restored.energy().getStored());
            eq("a generation refresh cannot advance the next queued batch in the same tick", 0.0,
                    restored.saveWithoutMetadata().getDouble("Progress"));
            eq("a generation refresh leaves the next queued input untouched", 1,
                    restored.getItem(0).getCount());
        } finally {
            BuiltInRegistries.ITEM.bindTags(originalTags);
            MaterialRegistry.rebuild(RegistryTagView.ofItems());
        }
    }

    private static void generationIdentity() throws Exception {
        ProcessLookup.Generation live = ProcessLookup.generation();
        yes("reading the generation identity is stable", live == ProcessLookup.generation());
        Field graph = ProcessLookup.class.getDeclaredField("GRAPH");
        graph.setAccessible(true);
        Class<?> graphType = graph.get(null).getClass();
        Method index = graphType.getDeclaredMethod("index", List.class);
        index.setAccessible(true);
        Method identity = graphType.getDeclaredMethod("generation");
        identity.setAccessible(true);
        Object first = index.invoke(null, List.of());
        Object second = index.invoke(null, List.of());
        yes("equivalent empty indexes still receive distinct generation identities",
                identity.invoke(first) != identity.invoke(second));
        yes("preparing an index never publishes a partial generation", live == ProcessLookup.generation());
    }

    private static void rebuildCacheTags(boolean ingotPresent, Item crushed) {
        Map<TagKey<Item>, List<Holder<Item>>> tags = Map.of(
                TagKey.create(Registries.ITEM, new ResourceLocation("forge:raw_materials/cache_probe")),
                List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.GRAVEL)),
                TagKey.create(Registries.ITEM, new ResourceLocation("grindless:crushed_materials/cache_probe")),
                List.of(BuiltInRegistries.ITEM.wrapAsHolder(crushed)),
                TagKey.create(Registries.ITEM, new ResourceLocation("forge:ingots/cache_probe")),
                ingotPresent ? List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.IRON_INGOT)) : List.of());
        BuiltInRegistries.ITEM.bindTags(tags);
        MaterialRegistry.rebuild(RegistryTagView.ofItems());
    }

    private static ProcessRecipe selected(ProcessMachineBlockEntity machine) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("recipe", List.class);
        method.setAccessible(true);
        return (ProcessRecipe) method.invoke(machine, List.of());
    }

    private static void pressureMatching() throws Exception {
        ProcessRecipe absent = pressureRecipe(Double.NaN, Double.NaN);
        ProcessConditions unnamed = ProcessLookup.conditionsOf(absent);
        yes("conversion preserves an unnamed pressure, not ambient pressure", unnamed.pressure() == null);
        yes("an unnamed pressure runs at zero", unnamed.evaluate(ConditionState.AMBIENT.withPressure(0)).isOptimal());
        yes("an unnamed pressure imposes no envelope restriction",
                unnamed.checkEnvelope(ConditionEnvelope.builder().pressure(20, 25).build()).canRun());

        ProcessRecipe named = pressureRecipe(1.0, 0.125);
        ProcessConditions conditions = ProcessLookup.conditionsOf(named);
        yes("conversion names exactly the physical pressure dimension",
                conditions.namedDimensions().equals(List.of(ConditionDimension.PRESSURE)));
        eq("conversion preserves the absolute MPa optimum", 1.0, conditions.pressure().optimum());
        eq("conversion preserves the explicit optimal half-width", 1.125, conditions.pressure().optimalMax());
        eq("conversion reuses the shared tolerance multiplier", 1.25, conditions.pressure().toleranceMax());
        yes("in-band pressure runs at full speed",
                conditions.evaluate(ConditionState.AMBIENT.withPressure(1.0)).isOptimal());
        ConditionReport slower = conditions.evaluate(ConditionState.AMBIENT.withPressure(1.1875));
        yes("a pressure tolerance mismatch names the existing limiting dimension",
                slower.canRun() && slower.limiting() == ConditionDimension.PRESSURE);
        eq("pressure tolerance changes speed rather than outputs", 0.625, slower.efficiency());
        eq("the exact low pressure tolerance edge still runs", 0.25,
                conditions.evaluate(ConditionState.AMBIENT.withPressure(0.75)).efficiency());
        eq("the exact high pressure tolerance edge still runs", 0.25,
                conditions.evaluate(ConditionState.AMBIENT.withPressure(1.25)).efficiency());
        yes("the next lower representable pressure reports too low",
                conditions.evaluate(ConditionState.AMBIENT.withPressure(Math.nextDown(0.75))).fault()
                        == ConditionFault.PRESSURE_TOO_LOW);
        yes("the next higher representable pressure reports too high",
                conditions.evaluate(ConditionState.AMBIENT.withPressure(Math.nextUp(1.25))).fault()
                        == ConditionFault.PRESSURE_TOO_HIGH);
        ConditionReport unreachable = conditions.checkEnvelope(ConditionEnvelope.builder().pressure(2, 3).build());
        yes("an unreachable pressure has the existing named envelope fault",
                unreachable.fault() == ConditionFault.OUTSIDE_ENVELOPE
                        && unreachable.limiting() == ConditionDimension.PRESSURE);
        yes("an envelope reaching only pressure tolerance is honestly degraded",
                conditions.checkEnvelope(ConditionEnvelope.builder().pressure(1.1875, 1.1875).build()).equals(slower));

        ProcessConditions vacuum = ProcessLookup.conditionsOf(pressureRecipe(0.0, 0.001));
        yes("zero absolute pressure survives conversion as a real requirement",
                vacuum.pressure() != null && vacuum.evaluate(ConditionState.AMBIENT.withPressure(0)).isOptimal());
        yes("ambient pressure is not silently substituted for zero",
                vacuum.evaluate(ConditionState.AMBIENT).fault() == ConditionFault.PRESSURE_TOO_HIGH);
        yes("an ambient-only envelope cannot reach the zero-pressure band",
                vacuum.checkEnvelope(ConditionEnvelope.builder().build()).fault() == ConditionFault.OUTSIDE_ENVELOPE);

        Field graph = ProcessLookup.class.getDeclaredField("GRAPH");
        graph.setAccessible(true);
        Object original = graph.get(null);
        Method index = original.getClass().getDeclaredMethod("index", List.class);
        index.setAccessible(true);
        try {
            graph.set(null, index.invoke(null, List.of(named)));
            ItemStack[] inputs = {new ItemStack(Items.CLAY_BALL, 2)};
            ProcessRecipe found = ProcessLookup.find(named.family(), inputs).orElseThrow();
            yes("indexed item selection retains the same pressured recipe object", found == named);
            yes("selection and direct conversion retain identical pressure bounds",
                    ProcessLookup.conditionsOf(found).pressure().toleranceMax()
                            == conditions.pressure().toleranceMax());
            no("pressure never bypasses the original item quantities",
                    ProcessLookup.find(named.family(), new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1)}).isPresent());
            ProcessMachineBlockEntity machine = machine(ProcessMachineKind.PULVERIZER);
            machine.setItem(0, inputs[0].copy());
            selected(machine);
            machine.energy().setStored(32);
            CompoundTag paid = machine.saveWithoutMetadata();
            paid.putDouble("Progress", 12.5);
            paid.put("Conditions", ConditionState.AMBIENT.withPressure(1.0).save(new CompoundTag()));
            machine.load(paid.copy());
            Method work = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
            work.setAccessible(true);
            work.invoke(machine, 1L, List.of());
            yes("a fixed-pressure machine cannot invent a regulator from saved state",
                    machine.inspectionConditionFault() == ConditionFault.PRESSURE_TOO_LOW);
            eq("the actual fixed pressure is retained", ProcessMachineKind.PULVERIZER.heldConditions().pressure(),
                    machine.conditions().pressure());
            eq("blocked pressure spends no Flux", 32L, machine.energy().getStored());
            eq("blocked pressure consumes no input", 2, machine.getItem(0).getCount());
            yes("blocked pressure emits no output", machine.getItem(machine.menuKind().inputs()).isEmpty());
            CompoundTag blocked = machine.saveWithoutMetadata();
            eq("blocked pressure preserves paid work", 12.5, blocked.getDouble("Progress"));
            yes("blocked pressure preserves the existing WorkRecipe owner",
                    paid.getString("WorkRecipe").equals(blocked.getString("WorkRecipe")));
            yes("pressure adds no active-work or inventory persistence keys",
                    paid.getAllKeys().equals(blocked.getAllKeys()));
            eq("pressure keeps ItemLayout unchanged", paid.getInt("ItemLayout"), blocked.getInt("ItemLayout"));
            eq("pressure keeps FluidOutputQuota unchanged", paid.getInt("FluidOutputQuota"),
                    blocked.getInt("FluidOutputQuota"));
            ProcessMachineBlockEntity restored = machine(ProcessMachineKind.PULVERIZER);
            restored.load(blocked.copy());
            work.invoke(restored, 2L, List.of());
            eq("reload retains paid work while the same pressure fault persists", 12.5,
                    restored.inspectionProgress());
            eq("reload cannot charge the blocked batch", 32L, restored.energy().getStored());
            graph.set(null, index.invoke(null, List.of(pressureRecipe(0.1, 0.01))));
            work.invoke(restored, 3L, List.of());
            eq("a reachable same-ID definition resumes existing paid work", 13.5, restored.inspectionProgress());
            eq("resumed work pays only one new tick", 28L, restored.energy().getStored());
            yes("resuming pressure does not change the saved owner",
                    restored.saveWithoutMetadata().getString("WorkRecipe").equals(named.id()));
        } finally {
            graph.set(null, original);
        }
    }

    private static ProcessRecipe pressureRecipe(double optimum, double halfWidth) {
        return new ProcessRecipe("check/pressure", ProcessMachineKind.PULVERIZER.family(),
                List.of(IngredientSpec.item("minecraft:clay_ball", 2)),
                List.of(OutputSpec.item("minecraft:brick", 1)), Double.NaN, optimum, halfWidth, null, null,
                80, 4, List.of(), null);
    }

    private static void regulation() throws Exception {
        ProcessMachineKind chamber = ProcessMachineKind.FIRING_CHAMBER;
        yes("the firing chamber declares regulation", chamber.regulatesConditions());
        for (ProcessMachineKind kind : ProcessMachineKind.values()) {
            if (kind == chamber) {
                continue;
            }
            no(kind + " does not regulate recipe bands", kind.regulatesConditions());
            ProcessRecipe unnamed = new ProcessRecipe("check/fixed", kind.family(),
                    List.of(), List.of(), Double.NaN, null, 80, 4);
            yes(kind + " retains its fixed held state", kind.heldConditions().equals(kind.heldConditions(unnamed)));
            ProcessMachineBlockEntity fixed = machine(kind);
            regulate(fixed, unnamed);
            ConditionState first = fixed.conditions();
            regulate(fixed, unnamed);
            yes(kind + " does not replace unchanged conditions", first == fixed.conditions());
        }

        ProcessMachineBlockEntity machine = machine(chamber);
        for (double temperature : new double[] {20, 500, 850, 1400, 1450}) {
            ProcessRecipe recipe = firing(temperature, "REDUCING");
            ConditionReport report = regulate(machine, recipe);
            yes(temperature + " C is admitted", report.canRun());
            eq(temperature + " C is held exactly", temperature, machine.conditions().temperature());
            yes("the requested reducing atmosphere is held",
                    machine.conditions().atmosphere() == Atmosphere.REDUCING);
            ConditionState held = machine.conditions();
            regulate(machine, recipe);
            yes("unchanged regulated conditions are not replaced", held == machine.conditions());
            yes("valid conditions clear the visible fault", machine.menuFault().getString().isEmpty());
        }
        ConditionReport unnamed = regulate(machine, firing(Double.NaN, "OXIDISING"));
        yes("an unnamed temperature does not fault", unnamed.canRun());
        eq("NaN keeps the default firing temperature", BootstrapProcesses.FIRING_C,
                machine.conditions().temperature());

        for (double temperature : new double[] {1450.01, 19.99, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            firingFault(machine, firing(temperature, "REDUCING"), ConditionDimension.TEMPERATURE);
        }
        for (String atmosphere : new String[] {"INERT", "VACUUM", null, "", " ", "UNKNOWN", "reducing"}) {
            firingFault(machine, firing(1000, atmosphere), ConditionDimension.ATMOSPHERE);
        }

        CompoundTag legacy = new CompoundTag();
        ConditionState old = ConditionState.AMBIENT.withTemperature(1900).withAtmosphere(Atmosphere.INERT);
        legacy.put("Conditions", old.save(new CompoundTag()));
        legacy.putDouble("Progress", 12);
        legacy.putString("WorkRecipe", "check/firing");
        machine.load(legacy);
        yes("load retains the old saved conditions until regulation", old.equals(machine.conditions()));
        Method firstTick = ProcessMachineBlockEntity.class.getDeclaredMethod("onFirstTick");
        firstTick.setAccessible(true);
        firstTick.invoke(machine);
        yes("the first tick restores a safe default", chamber.heldConditions().equals(machine.conditions()));
        ConditionReport restored = regulate(machine, firing(1100, "REDUCING"));
        yes("the next work regulation admits the selected saved-world recipe", restored.canRun());
        eq("old-save regulation uses the recipe, not saved 1900 C", 1100, machine.conditions().temperature());
        eq("regulation preserves paid work", 12, machine.saveWithoutMetadata().getDouble("Progress"));
    }

    private static void firingFault(ProcessMachineBlockEntity machine, ProcessRecipe recipe,
                                    ConditionDimension dimension) throws Exception {
        ConditionReport report = regulate(machine, recipe);
        no(recipe.temperatureC() + "/" + recipe.atmosphere() + " cannot run", report.canRun());
        yes("the fault names the unreachable dimension", report.fault() == ConditionFault.OUTSIDE_ENVELOPE
                && report.limiting() == dimension);
        yes("invalid conditions are never held or clamped",
                machine.conditions().equals(ProcessMachineKind.FIRING_CHAMBER.heldConditions()));
        yes("the existing menu exposes the condition fault", !machine.menuFault().getString().isEmpty());
        yes("the published report selects OUT_OF_BAND even with blocked output",
                ProcessLogic.status(true, false, true, report.canRun(), true, false)
                        == MachineStatus.OUT_OF_BAND);
    }

    private static ProcessRecipe firing(double temperature, String atmosphere) {
        return new ProcessRecipe("check/firing", ProcessMachineKind.FIRING_CHAMBER.family(),
                List.of(), List.of(), temperature, atmosphere, 80, 4);
    }

    private static ConditionReport regulate(ProcessMachineBlockEntity machine, ProcessRecipe recipe)
            throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("regulateConditions", ProcessRecipe.class);
        method.setAccessible(true);
        return (ConditionReport) method.invoke(machine, recipe);
    }

    private static ProcessMachineBlockEntity machine(ProcessMachineKind kind) throws Exception {
        Constructor<ProcessMachineBlockEntity> constructor = ProcessMachineBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, ProcessMachineKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, kind, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static void subscriptions() {
        TickSubscriptions subs = new TickSubscriptions();
        yes("starts idle", subs.isIdle());

        int[] runs = {0};
        TickSubscription a = subs.subscribe(() -> runs[0]++);
        no("not idle once subscribed", subs.isIdle());
        subs.tick();
        subs.tick();
        eq("ran twice", 2, runs[0]);

        a.unsubscribe();
        subs.tick();
        eq("stopped after unsubscribe", 2, runs[0]);
        yes("idle again", subs.isIdle());

        // Passing the handle back must not subscribe the same work twice.
        TickSubscriptions dedupe = new TickSubscriptions();
        int[] once = {0};
        TickSubscription handle = dedupe.subscribe(null, () -> once[0]++);
        handle = dedupe.subscribe(handle, () -> once[0]++);
        handle = dedupe.subscribe(handle, () -> once[0]++);
        eq("deduped to one subscription", 1, dedupe.activeCount());
        dedupe.tick();
        eq("deduped work ran once", 1, once[0]);

        // The important case: work that cancels itself mid-tick. This is how a machine
        // goes idle, so it must not corrupt the iteration.
        TickSubscriptions selfCancel = new TickSubscriptions();
        int[] cancelRuns = {0};
        TickSubscription[] holder = new TickSubscription[1];
        holder[0] = selfCancel.subscribe(() -> {
            cancelRuns[0]++;
            holder[0].unsubscribe();
        });
        selfCancel.tick();
        selfCancel.tick();
        eq("self-cancelling work ran exactly once", 1, cancelRuns[0]);
        yes("idle after self-cancel", selfCancel.isIdle());

        // Work that subscribes more work during the pass.
        TickSubscriptions growing = new TickSubscriptions();
        int[] added = {0};
        TickSubscription[] seed = new TickSubscription[1];
        seed[0] = growing.subscribe(() -> {
            growing.subscribe(() -> added[0]++);
            seed[0].unsubscribe();
        });
        growing.tick();
        eq("added work ran in the same pass", 1, added[0]);
        eq("one subscription remains", 1, growing.activeCount());

        // One cancelling another mid-pass: the cancelled one must not run.
        TickSubscriptions crossCancel = new TickSubscriptions();
        int[] victimRuns = {0};
        TickSubscription[] victim = new TickSubscription[1];
        crossCancel.subscribe(() -> victim[0].unsubscribe());
        victim[0] = crossCancel.subscribe(() -> victimRuns[0]++);
        crossCancel.tick();
        eq("cancelled peer did not run", 0, victimRuns[0]);

        TickSubscriptions cleared = new TickSubscriptions();
        cleared.subscribe(() -> { });
        cleared.clear();
        yes("clear leaves it idle", cleared.isIdle());

        // Offsets must actually spread work out rather than agreeing.
        int offA = TickOffset.forPosition(100, 64, 100);
        int offB = TickOffset.forPosition(101, 64, 100);
        yes("offsets are non-negative", offA >= 0 && offB >= 0);
        yes("adjacent blocks get different offsets", offA % 20 != offB % 20);
        yes("offset is stable", offA == TickOffset.forPosition(100, 64, 100));

        // The case that matters: a row of machines, as players actually build. A weak hash
        // would give these the same offset and resynchronise everything.
        boolean[] seen = new boolean[20];
        int distinct = 0;
        for (int i = 0; i < 20; i++) {
            int bucket = TickOffset.forPosition(100 + i, 64, 100) % 20;
            if (!seen[bucket]) {
                seen[bucket] = true;
                distinct++;
            }
        }
        yes("a row of 20 spreads over many ticks (got " + distinct + ")", distinct >= 10);

        int due = 0;
        for (long t = 0; t < 20; t++) {
            if (TickOffset.isDue(t, offA, 20)) {
                due++;
            }
        }
        eq("period 20 fires once per 20 ticks", 1, due);
        yes("period 1 always fires", TickOffset.isDue(7L, offA, 1));
    }

    private static void persistence() {
        UpgradeSet set = UpgradeSet.EMPTY
                .install(MachineUpgrade.SPEED, ChassisMark.MK_V)
                .install(MachineUpgrade.SPEED, ChassisMark.MK_V)
                .install(MachineUpgrade.YIELD, ChassisMark.MK_V);
        UpgradeSet back = UpgradeSet.load(set.save(new CompoundTag()));
        eq("upgrade count survives a save", set.size(), back.size());
        eq("stacked upgrades keep their count", 2, back.count(MachineUpgrade.SPEED));
        eq("a single upgrade keeps its count", 1, back.count(MachineUpgrade.YIELD));
        eq("derived multipliers survive too", set.timeMultiplier(), back.timeMultiplier());
        yes("an empty set loads as the shared empty set",
                UpgradeSet.load(UpgradeSet.EMPTY.save(new CompoundTag())) == UpgradeSet.EMPTY);
        yes("a machine saved before upgrades were persisted loads empty",
                UpgradeSet.load(new CompoundTag()) == UpgradeSet.EMPTY);

        CompoundTag foreign = new CompoundTag();
        foreign.putInt("SPEED", 1);
        foreign.putInt("REMOVED_IN_A_LATER_VERSION", 3);
        foreign.putInt("YIELD", 0);
        foreign.putInt("PRECISION", -2);
        UpgradeSet lenient = UpgradeSet.load(foreign);
        eq("unknown, zero and negative entries are skipped", 1, lenient.size());
        eq("the valid entry is kept", 1, lenient.count(MachineUpgrade.SPEED));

        ConditionState hot = ConditionState.AMBIENT
                .withTemperature(850.5)
                .withPressure(12.25)
                .withAtmosphere(Atmosphere.INERT)
                .withField(ProcessField.MAGNETIC)
                .withAgitation(Agitation.FLUIDISED)
                .withCatalyst(new ResourceLocation("grindless", "platinum_mesh"));
        yes("conditions survive a save",
                hot.equals(ConditionState.load(hot.save(new CompoundTag()))));
        yes("no catalyst stays no catalyst", ConditionState.AMBIENT.equals(
                ConditionState.load(ConditionState.AMBIENT.save(new CompoundTag()))));
        yes("a machine saved before conditions were persisted loads ambient",
                ConditionState.AMBIENT.equals(ConditionState.load(new CompoundTag())));

        CompoundTag damaged = hot.save(new CompoundTag());
        damaged.putString("Atmosphere", "PLASMA_FROM_THE_FUTURE");
        damaged.putDouble("Temperature", Double.NaN);
        damaged.putString("Catalyst", "not a valid location!");
        ConditionState degraded = ConditionState.load(damaged);
        eq("an unknown atmosphere degrades to air", Atmosphere.AIR.ordinal(),
                degraded.atmosphere().ordinal());
        eq("a non-finite temperature degrades to ambient",
                ConditionState.AMBIENT.temperature(), degraded.temperature());
        yes("an unparseable catalyst is dropped", degraded.catalyst() == null);
        eq("the rest of the damaged state is kept", hot.pressure(), degraded.pressure());
    }

    private static void statuses() {
        eq("an empty dynamo is idle", MachineStatus.IDLE.ordinal(),
                DynamoStatus.of(0L, 0).ordinal());
        eq("a charged dynamo that is giving power is running", MachineStatus.RUNNING.ordinal(),
                DynamoStatus.of(100L, 0).ordinal());
        eq("one fruitless push is a blip, not a fault", MachineStatus.RUNNING.ordinal(),
                DynamoStatus.of(100L, 1).ordinal());
        eq("two are a fact: charge with nowhere to go is blocked", MachineStatus.BLOCKED.ordinal(),
                DynamoStatus.of(100L, DynamoStatus.BLOCKED_AFTER).ordinal());
        eq("running dry is idle whatever the push history", MachineStatus.IDLE.ordinal(),
                DynamoStatus.of(0L, 9).ordinal());

        StatusDebounce flicker = new StatusDebounce();
        int changes = 0;
        for (int tick = 0; tick < 200; tick++) {
            // Power moves on one tick in five, as a throttled push does.
            if (flicker.observe(tick % 5 == 0 ? MachineStatus.RUNNING : MachineStatus.IDLE)) {
                changes++;
            }
        }
        eq("a status that flaps every few ticks changes once, not forty times", 1, changes);
        eq("and settles on running", MachineStatus.RUNNING.ordinal(), flicker.shown().ordinal());

        StatusDebounce quiet = new StatusDebounce();
        quiet.observe(MachineStatus.RUNNING);
        for (int i = 0; i < StatusDebounce.IDLE_HOLD - 1; i++) {
            quiet.observe(MachineStatus.IDLE);
        }
        eq("going quiet is not shown before the hold", MachineStatus.RUNNING.ordinal(), quiet.shown().ordinal());
        quiet.observe(MachineStatus.IDLE);
        eq("but is shown once it has held", MachineStatus.IDLE.ordinal(), quiet.shown().ordinal());

        StatusDebounce fault = new StatusDebounce();
        fault.observe(MachineStatus.RUNNING);
        yes("a fault is shown at once", fault.observe(MachineStatus.STARVED));
        for (int i = 0; i < StatusDebounce.RECOVER_HOLD - 1; i++) {
            fault.observe(MachineStatus.RUNNING);
        }
        eq("and is not cleared by a brief recovery", MachineStatus.STARVED.ordinal(), fault.shown().ordinal());
        fault.observe(MachineStatus.RUNNING);
        eq("only by a sustained one", MachineStatus.RUNNING.ordinal(), fault.shown().ordinal());

        StatusDebounce broken = new StatusDebounce();
        broken.observe(MachineStatus.RUNNING);
        broken.observe(MachineStatus.STARVED);
        for (int i = 0; i < StatusDebounce.RECOVER_HOLD - 1; i++) {
            broken.observe(MachineStatus.RUNNING);
        }
        broken.observe(MachineStatus.STARVED);
        for (int i = 0; i < StatusDebounce.RECOVER_HOLD - 1; i++) {
            broken.observe(MachineStatus.RUNNING);
        }
        eq("a recovery interrupted by the fault returning starts over", MachineStatus.STARVED.ordinal(),
                broken.shown().ordinal());

        StatusDebounce burst = new StatusDebounce();
        burst.observe(MachineStatus.STARVED);
        int shownRunning = 0;
        for (int tick = 0; tick < 200; tick++) {
            // Empty-network ticks between a five-tick 40 FU burst feeding two 8 FU/t loads.
            MachineStatus observed = (tick % 5) < 3
                    ? MachineStatus.RUNNING : MachineStatus.STARVED;
            burst.observe(observed);
            if (burst.shown() == MachineStatus.RUNNING) {
                shownRunning++;
            }
        }
        eq("starve blips every five ticks never recover to running", 0, shownRunning);

        eq("statuses parse from their serialised names", MachineStatus.OUT_OF_BAND.ordinal(),
                MachineStatus.parse("out_of_band").ordinal());
        eq("an idle machine emits no light", 0, MachineStatus.IDLE.lightLevel());
        yes("a running one does", MachineStatus.RUNNING.lightLevel() > MachineStatus.STARVED.lightLevel());
    }

    private static void pushBackoff() {
        eq("full speed while power flows", 5, PushBackoff.period(5, 0));
        eq("doubles after one fruitless push", 10, PushBackoff.period(5, 1));
        eq("doubles again", 20, PushBackoff.period(5, 2));
        eq("stops at the ceiling", PushBackoff.MAX_PERIOD, PushBackoff.period(5, 3));
        eq("and stays there", PushBackoff.MAX_PERIOD, PushBackoff.period(5, 1_000_000));
        yes("never faster than the base", PushBackoff.period(5, 0) >= 5);
        eq("a base above the ceiling is left alone", 40, PushBackoff.period(40, 7));
    }

    private static void upgrades() {
        UpgradeSet set = UpgradeSet.EMPTY;
        eq("starts empty", 0, set.size());
        eq("no parallel by default", 1, set.parallelCount());
        eq("no time change by default", 1.0, set.timeMultiplier());

        // MK I has one slot.
        set = set.install(MachineUpgrade.SPEED, ChassisMark.MK_I);
        eq("one installed", 1, set.size());
        no("second does not fit in MK I", set.canInstall(MachineUpgrade.YIELD, ChassisMark.MK_I));
        yes("but fits in MK II", set.canInstall(MachineUpgrade.YIELD, ChassisMark.MK_II));

        // Exclusions must hold in both directions.
        no("speed excludes efficiency",
                set.canInstall(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V));
        no("speed excludes precision",
                set.canInstall(MachineUpgrade.PRECISION, ChassisMark.MK_V));
        UpgradeSet eff = UpgradeSet.EMPTY.install(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V);
        no("efficiency excludes speed", eff.canInstall(MachineUpgrade.SPEED, ChassisMark.MK_V));
        yes("exclusion is symmetric",
                MachineUpgrade.SPEED.conflictsWith(MachineUpgrade.EFFICIENCY)
                        && MachineUpgrade.EFFICIENCY.conflictsWith(MachineUpgrade.SPEED));
        UpgradeSet par = UpgradeSet.EMPTY.install(MachineUpgrade.PARALLEL, ChassisMark.MK_V);
        no("parallel excludes insulation",
                par.canInstall(MachineUpgrade.INSULATION, ChassisMark.MK_V));

        yes("rejection explains itself",
                set.rejectionReason(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V) != null
                        && set.rejectionReason(MachineUpgrade.EFFICIENCY, ChassisMark.MK_V)
                                .contains("Speed"));

        // Speed: double speed for roughly triple power, the one documented figure.
        eq("one speed halves the time", 0.5, set.timeMultiplier());
        eq("one speed triples the energy", 3.0, set.energyMultiplier());
        UpgradeSet twoSpeed = set.install(MachineUpgrade.SPEED, ChassisMark.MK_V);
        eq("speed stacks", 2, twoSpeed.count(MachineUpgrade.SPEED));
        eq("two speeds quarter the time", 0.25, twoSpeed.timeMultiplier());
        eq("two speeds are ninefold energy", 9.0, twoSpeed.energyMultiplier());

        // Parallel scales power linearly: the honest, boring axis.
        UpgradeSet twoPar = UpgradeSet.EMPTY
                .install(MachineUpgrade.PARALLEL, ChassisMark.MK_V)
                .install(MachineUpgrade.PARALLEL, ChassisMark.MK_V);
        eq("three recipes per cycle", 3, twoPar.parallelCount());
        eq("power scales linearly with parallel", 3.0, twoPar.energyMultiplier());
        eq("parallel does not change cycle time", 1.0, twoPar.timeMultiplier());

        // Efficiency: cheaper, slower. The opposite trade.
        yes("efficiency is slower", eff.timeMultiplier() > 1.0);
        yes("efficiency is cheaper", eff.energyMultiplier() < 1.0);

        UpgradeSet removed = twoSpeed.remove(MachineUpgrade.SPEED);
        eq("removing leaves one", 1, removed.count(MachineUpgrade.SPEED));
        eq("removing again empties", 0,
                removed.remove(MachineUpgrade.SPEED).count(MachineUpgrade.SPEED));
        eq("removing absent is harmless", 0,
                UpgradeSet.EMPTY.remove(MachineUpgrade.YIELD).size());
        eq("sets are immutable", 1, set.size());
    }

    private static void chassis() {
        eq("MK I has one slot", 1, ChassisMark.MK_I.upgradeSlots());
        eq("MK V has six", 6, ChassisMark.MK_V.upgradeSlots());
        yes("MK V is the top", ChassisMark.MK_V.next() == ChassisMark.MK_V);
        yes("marks step up", ChassisMark.MK_I.next() == ChassisMark.MK_II);

        // The Arc Furnace ladder from MACHINES.md: 1800, 2300, 2800, 3200, 3500.
        ConditionEnvelope narrow = ConditionEnvelope.builder()
                .temperature(1200.0, 1800.0)
                .atmospheres(Atmosphere.REDUCING)
                .build();
        ConditionEnvelope full = ConditionEnvelope.builder()
                .temperature(1200.0, 3500.0)
                .atmospheres(Atmosphere.REDUCING, Atmosphere.OXIDISING, Atmosphere.INERT)
                .build();

        double[] expected = {1800.0, 2300.0, 2800.0, 3200.0, 3500.0};
        ChassisMark[] marks = ChassisMark.values();
        for (int i = 0; i < marks.length; i++) {
            double actual = marks[i].widen(narrow, full).maxTemperature();
            near(marks[i].displayName() + " reaches ~" + (int) expected[i] + " C",
                    expected[i], actual, 20.0);
        }

        eq("MK I is exactly the narrow ceiling", 1800.0,
                ChassisMark.MK_I.widen(narrow, full).maxTemperature());
        eq("MK V is exactly the full ceiling", 3500.0,
                ChassisMark.MK_V.widen(narrow, full).maxTemperature());
        yes("MK V gains the wider atmosphere set",
                ChassisMark.MK_V.widen(narrow, full).supportsAtmosphere(Atmosphere.OXIDISING));
        no("MK I does not",
                ChassisMark.MK_I.widen(narrow, full).supportsAtmosphere(Atmosphere.OXIDISING));
        yes("widening is monotonic",
                ChassisMark.MK_II.widen(narrow, full).maxTemperature()
                        < ChassisMark.MK_III.widen(narrow, full).maxTemperature());
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void near(String what, double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) <= tolerance) {
            pass(what);
        } else {
            fail(what + ": expected ~" + expected + " but got " + actual);
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
