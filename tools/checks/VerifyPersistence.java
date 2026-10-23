package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.SlottedContainer;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestInventoryEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.LegacyGuideAssembly;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost.SelectionIssue;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost.SelectionResult;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Executable vanilla NBT and inventory contracts; real registered Matrices also need a Forge smoke. */
public final class VerifyPersistence {

    private static final String BLUEPRINT = "assemble/blueprint_tool";
    private static final String PLANNER = "assemble/deconstruction_planner";
    private static final List<String> RETIRED_ASSEMBLER_TARGETS = List.of(
            "assemble/chemical_washer", "assemble/press", "assemble/bootstrap/furnace");
    private static int failures;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        outputRoundTrip();
        sparseInputs();
        legacyInventory();
        matrixCodec();
        itemPacket();
        questInventory();
        questProgress();
        assemblerWorkNbt();
        assemblerMachineWork();
        benchRetiredWork();
        benchWorkNonRegression();
        benchCacheAndGeneration();
        System.out.println(failures == 0 ? "ALL PERSISTENCE CHECKS PASSED"
                : failures + " PERSISTENCE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void outputRoundTrip() throws Exception {
        SimpleContainer original = new SlottedContainer(5);
        ItemStack carrier = new ItemStack(Items.PAPER, 2);
        carrier.getOrCreateTag().putInt("Rating", 2);
        original.setItem(4, carrier);
        CompoundTag saved = new CompoundTag();
        saved.put("Items", original.createTag());
        ListTag items = saved.getList("Items", 10);
        check("output serialization records slot four",
                items.getCompound(0).contains("Slot") && items.getCompound(0).getInt("Slot") == 4);
        SimpleContainer restored = new SlottedContainer(5);
        restored.fromTag(roundTrip(saved).getList("Items", 10));
        check("reconstruction keeps the output out of input zero", restored.getItem(0).isEmpty());
        check("reconstruction preserves the output slot, count and rating",
                ItemStack.isSameItemSameTags(carrier, restored.getItem(4))
                        && restored.getItem(4).getCount() == 2);
        for (int restart = 0; restart < 3; restart++) {
            CompoundTag checkpoint = new CompoundTag();
            checkpoint.put("Items", restored.createTag());
            SimpleContainer next = new SlottedContainer(5);
            next.fromTag(roundTrip(checkpoint).getList("Items", 10));
            restored = next;
        }
        check("repeated reconstruction neither moves nor duplicates the rated output",
                restored.getItem(0).isEmpty() && restored.getItem(4).getCount() == 2
                        && ItemStack.isSameItemSameTags(carrier, restored.getItem(4)));
    }

    private static void sparseInputs() {
        SimpleContainer original = new SlottedContainer(5);
        original.setItem(1, new ItemStack(Items.GLASS, 4));
        original.setItem(3, new ItemStack(Items.REDSTONE, 1));
        SimpleContainer restored = new SlottedContainer(5);
        restored.fromTag(original.createTag());
        check("empty inputs stay empty after loading",
                restored.getItem(0).isEmpty() && restored.getItem(2).isEmpty());
        check("non-primary inputs keep their positions",
                restored.getItem(1).is(Items.GLASS) && restored.getItem(1).getCount() == 4
                        && restored.getItem(3).is(Items.REDSTONE));
    }

    private static void legacyInventory() {
        SimpleContainer compact = new SimpleContainer(5);
        compact.setItem(0, new ItemStack(Items.GLASS, 4));
        compact.setItem(1, new ItemStack(Items.REDSTONE, 2));
        ListTag saved = compact.createTag();
        ListTag before = saved.copy();
        SlottedContainer restored = new SlottedContainer(5);
        restored.fromTag(saved);
        check("legacy compact input order and quantities are retained",
                restored.getItem(0).is(Items.GLASS) && restored.getItem(0).getCount() == 4
                        && restored.getItem(1).is(Items.REDSTONE) && restored.getItem(1).getCount() == 2);
        check("migration does not mutate the caller's saved tag", saved.equals(before));
        restored.fromTag(new ListTag());
        check("missing inventory data restores an empty container", restored.isEmpty());
    }

    private static void assemblerWorkNbt() throws Exception {
        String blueprint = BLUEPRINT;
        String planner = PLANNER;
        AssemblerWork work = new AssemblerWork();
        work.activate("assemble/press", 3.375, blueprint);
        work.activate(blueprint, 12.03125, planner);
        work.select("removed/selected");
        CompoundTag saved = new CompoundTag();
        saved.putString("Recipe", planner);
        saved.putString("WorkRecipe", planner);
        saved.putDouble("Progress", 400.1);
        work.save(saved);
        CompoundTag original = saved.copy();
        for (int reopen = 0; reopen < 24; reopen++) {
            saved = roundTrip(saved);
            AssemblerWork restored = AssemblerWork.load(saved, saved.getString("WorkRecipe"), saved.getDouble("Progress"));
            check("binary reopen " + reopen + " preserves unknown selected identity without AUTO",
                    restored.valid() && restored.selected().equals("removed/selected"));
            check("binary reopen " + reopen + " preserves exact known and unavailable paid balances",
                    restored.suspended().equals(java.util.Map.of("assemble/press", 3.375, blueprint, 12.03125)));
            restored.save(saved);
            check("binary reopen " + reopen + " cannot duplicate, normalize or erase work", saved.equals(original));
        }
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Recipe", blueprint);
        legacy.putDouble("Progress", 0.34375);
        AssemblerWork automatic = AssemblerWork.load(legacy, legacy.getString("Recipe"), legacy.getDouble("Progress"));
        automatic.save(legacy);
        check("a legacy AUTO save gains no optional selector or work keys",
                !legacy.contains(AssemblerWork.TARGET) && !legacy.contains(AssemblerWork.SUSPENDED)
                        && legacy.getDouble("Progress") == 0.34375 && !legacy.contains("WorkRecipe"));
        List<Consumer<CompoundTag>> invalid = List.of(
                tag -> tag.putString("Progress", "0.5"),
                tag -> tag.putDouble("Progress", Double.NaN),
                tag -> tag.putDouble("Progress", Double.POSITIVE_INFINITY),
                tag -> tag.putDouble("Progress", -0.5),
                tag -> tag.putInt("WorkRecipe", 1),
                tag -> tag.putInt(AssemblerWork.TARGET, 1),
                tag -> tag.putString(AssemblerWork.TARGET, " ".repeat(2)),
                tag -> tag.putString(AssemblerWork.TARGET, "x".repeat(257)),
                tag -> tag.put(AssemblerWork.SUSPENDED, new CompoundTag()),
                tag -> {
                    ListTag rows = new ListTag();
                    rows.add(StringTag.valueOf("not paid work"));
                    tag.put(AssemblerWork.SUSPENDED, rows);
                },
                tag -> {
                    ListTag rows = tag.getList(AssemblerWork.SUSPENDED, Tag.TAG_COMPOUND);
                    rows.add(rows.getCompound(0).copy());
                },
                tag -> tag.getList(AssemblerWork.SUSPENDED, Tag.TAG_COMPOUND).getCompound(0)
                        .putString("Recipe", planner),
                tag -> tag.getList(AssemblerWork.SUSPENDED, Tag.TAG_COMPOUND).getCompound(0)
                        .putDouble("Progress", 0.0),
                tag -> tag.getList(AssemblerWork.SUSPENDED, Tag.TAG_COMPOUND).getCompound(0)
                        .putDouble("Progress", Double.NEGATIVE_INFINITY),
                tag -> tag.getList(AssemblerWork.SUSPENDED, Tag.TAG_COMPOUND).getCompound(0)
                        .putString("Progress", "3.375"),
                tag -> tag.getList(AssemblerWork.SUSPENDED, Tag.TAG_COMPOUND).getCompound(0)
                        .putString("Unexpected", "retain rather than ignore"));
        for (int index = 0; index < invalid.size(); index++) {
            CompoundTag damaged = original.copy();
            invalid.get(index).accept(damaged);
            damaged = roundTrip(damaged);
            CompoundTag before = damaged.copy();
            AssemblerWork refused = AssemblerWork.load(damaged, damaged.getString("WorkRecipe"),
                    damaged.getDouble("Progress"));
            check("malformed paid state " + index + " is explicit and unavailable",
                    !refused.valid() && !refused.problem().isEmpty());
            try {
                refused.select(blueprint);
                check("malformed paid state cannot change configuration", false);
            } catch (IllegalStateException expected) {
                check("malformed paid state refuses selection " + index, true);
            }
            try {
                refused.activate(damaged.getString("WorkRecipe"), damaged.getDouble("Progress"), blueprint);
                check("malformed paid state cannot execute or transfer work", false);
            } catch (IllegalStateException expected) {
                check("malformed paid state refuses execution " + index, true);
            }
            check("malformed paid state remains unavailable after refused operations " + index, !refused.valid());
            refused.save(damaged);
            // Vanilla SNBT retains typed values (including NaN) and sorts compound keys.
            check("malformed paid state " + index + " is retained exactly through binary reopen",
                    roundTrip(damaged).toString().equals(before.toString()));
        }
    }

    /** Runs the actual BE without Forge registration, using explicitly labelled vanilla carrier rows. */
    private static void assemblerMachineWork() throws Exception {
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        ProcessRecipe declaredBlueprint = ProcessLookup.byId(MachineFamily.ASSEMBLER, BLUEPRINT).orElseThrow();
        ProcessRecipe declaredPlanner = ProcessLookup.byId(MachineFamily.ASSEMBLER, PLANNER).orElseThrow();
        check("the BE fixture derives both permitted tool colliders from the actual identical declared bills",
                declaredBlueprint.inputs().equals(declaredPlanner.inputs()));
        for (String retired : RETIRED_ASSEMBLER_TARGETS) {
            check("the actual current catalogue has retired normal-machine target " + retired,
                    ProcessLookup.byId(MachineFamily.ASSEMBLER, retired).isEmpty());
        }
        List<String> carrierIds = List.of("minecraft:quartz", "minecraft:copper_ingot",
                "minecraft:iron_ingot", "minecraft:redstone");
        List<IngredientSpec> bill = new ArrayList<>();
        for (int slot = 0; slot < declaredBlueprint.itemInputs().size(); slot++) {
            bill.add(IngredientSpec.item(carrierIds.get(slot), declaredBlueprint.itemInputs().get(slot).count()));
        }
        ProcessRecipe blueprint = new ProcessRecipe(declaredBlueprint.id(), MachineFamily.ASSEMBLER, bill,
                List.of(OutputSpec.item("minecraft:clay_ball", declaredBlueprint.itemOutputs().get(0).count())),
                Double.NaN, null, declaredBlueprint.durationTicks(), declaredBlueprint.fuPerTick());
        ProcessRecipe planner = new ProcessRecipe(declaredPlanner.id(), MachineFamily.ASSEMBLER, bill,
                List.of(OutputSpec.item("minecraft:brick", declaredPlanner.itemOutputs().get(0).count())),
                Double.NaN, null, declaredPlanner.durationTicks(), declaredPlanner.fuPerTick());
        Field graph = ProcessLookup.class.getDeclaredField("GRAPH");
        graph.setAccessible(true);
        Object originalGraph = graph.get(null);
        Method index = originalGraph.getClass().getDeclaredMethod("index", List.class);
        index.setAccessible(true);
        try {
            graph.set(null, index.invoke(null, List.of(blueprint, planner)));
            assemblerRetiredWork(blueprint, planner);
            ProcessMachineBlockEntity machine = assembler();
            supplyAssembler(machine);
            ProcessRecipe expectedAuto = automaticTarget(machine);
            check("legacy unselected AUTO retains the actual live lookup authority (carriers)",
                    resolve(machine).id().equals(expectedAuto.id()));
            check("the real selection transition chooses the exact Blueprint tool independently (carriers)",
                    change(machine, blueprint.id()) == SelectionResult.SELECTED && resolve(machine) == blueprint);
            Object inventory = machine.menuContainer();
            Object energy = machine.energy();
            Object fluids = machine.nativeStore();
            Object config = machine.containerConfig();
            machine.energy().setStored(512);
            machine.containerConfig().setSideMode(Direction.NORTH,
                    io.github.ezequiel24123z.grindless.container.SideMode.EXTRACT);
            CompoundTag state = machine.saveWithoutMetadata();
            state.putDouble("Progress", blueprint.durationTicks() - 1.75);
            CompoundTag upgrades = new CompoundTag();
            upgrades.putInt("SPEED", 1);
            state.put("Upgrades", upgrades);
            state.put("Fluid", FluidNbt.save(FluidState.of("minecraft:water", 325, 37.0, 0.2)));
            machine.load(roundTrip(state));
            tick(machine, 1);
            exact("actual upgraded completion preserves fractional carry instead of resetting it", 0.25,
                    machine.inspectionProgress());
            check("the real transaction writes the selected output, not the last dry output (carriers)",
                    machine.getItem(4).is(Items.CLAY_BALL) && machine.getItem(4).getCount() == 1);
            check("actual completion debits one upgraded tick once",
                    machine.energy().getStored() == 512L - ProcessMachineBlockEntity.upgradedTickCost(
                            blueprint.fuPerTick(), machine.upgrades()));
            check("actual completion consumes the existing exact grouped bill once",
                    java.util.stream.IntStream.range(0, bill.size()).allMatch(slot -> machine.getItem(slot).isEmpty()));
            change(machine, planner.id());
            state = machine.saveWithoutMetadata();
            state.putDouble("Progress", 12.875);
            machine.load(roundTrip(state));
            CompoundTag stores = processStores(machine);
            for (int reopen = 0; reopen < 40; reopen++) {
                check("switching back to a fractional owner remains available " + reopen,
                        change(machine, blueprint.id()) == SelectionResult.SELECTED);
                exact("the real BE restores Blueprint tool carry exactly " + reopen, 0.25, machine.inspectionProgress());
                check("fractional carry never becomes another recipe's work " + reopen,
                        change(machine, planner.id()) == SelectionResult.SELECTED);
                exact("the real BE restores Planner partial work exactly " + reopen, 12.875, machine.inspectionProgress());
                machine.load(roundTrip(machine.saveWithoutMetadata()));
                check("switch/reopen keeps inventory, fluids, energy, configuration and layout " + reopen,
                        processStores(machine).equals(stores));
                check("switch/reopen preserves the actual exposed store objects " + reopen,
                        machine.menuContainer() == inventory && machine.energy() == energy
                                && machine.nativeStore() == fluids && machine.containerConfig() == config);
            }
            change(machine, blueprint.id());
            supplyAssembler(machine);
            machine.setItem(4, new ItemStack(Items.STONE, 64));
            state = machine.saveWithoutMetadata();
            state.putDouble("Progress", blueprint.durationTicks() + 0.375);
            machine.load(roundTrip(state));
            long fuel = machine.energy().getStored();
            for (long tick = 2; tick <= 5; tick++) {
                tick(machine, tick);
            }
            exact("completed blocked work is never redrawn or discarded",
                    blueprint.durationTicks() + 0.375, machine.inspectionProgress());
            check("completed blocked work never spends another FU", machine.energy().getStored() == fuel);
            check("the server-owned selected diagnosis names blocked completion",
                    machine.recipeSelection().orElseThrow().issue() == SelectionIssue.OUTPUT_BLOCKED);
            change(machine, planner.id());
            change(machine, blueprint.id());
            exact("blocked completion also survives switching",
                    blueprint.durationTicks() + 0.375, machine.inspectionProgress());
            machine.setItem(4, ItemStack.EMPTY);
            tick(machine, 6);
            exact("unblocking settles exactly once and retains the fractional remainder", 0.375,
                    machine.inspectionProgress());
            check("unblocking does not redraw completed power or create the dry-last output",
                    machine.energy().getStored() == fuel && machine.getItem(4).is(Items.CLAY_BALL)
                            && machine.getItem(4).getCount() == 1);
            tick(machine, 7);
            check("a settled batch cannot repeat without new physical supplies",
                    machine.energy().getStored() == fuel && machine.getItem(4).getCount() == 1);

            supplyAssembler(machine);
            for (String lock : List.of("loadingInventory", "processing", "settling")) {
                Field field = ProcessMachineBlockEntity.class.getDeclaredField(lock);
                field.setAccessible(true);
                CompoundTag before = machine.saveWithoutMetadata();
                field.setBoolean(machine, true);
                try {
                    check(lock + " refuses a reentrant selector", change(machine, planner.id()) == SelectionResult.BUSY);
                    check(lock + " refusal preserves the full saved machine", machine.saveWithoutMetadata().equals(before));
                } finally {
                    field.setBoolean(machine, false);
                }
            }
            CompoundTag before = machine.saveWithoutMetadata();
            check("an unknown requested ID refuses without clearing the real configuration",
                    change(machine, "missing/request") == SelectionResult.INVALID_RECIPE
                            && machine.saveWithoutMetadata().equals(before));
            check("a foreign-family requested ID refuses without mutation",
                    change(machine, "press/plate/iron") == SelectionResult.INVALID_RECIPE
                            && machine.saveWithoutMetadata().equals(before));
            ProcessLookup.Generation generation = ProcessLookup.generation();
            graph.set(null, index.invoke(null, List.of(planner)));
            check("reload actually changes the indexed publication", generation != ProcessLookup.generation());
            check("removed explicit IDs wait rather than executing the remaining matching bill",
                    resolve(machine) == null && machine.recipeSelection().orElseThrow().issue() == SelectionIssue.UNAVAILABLE);
            tick(machine, 8);
            check("an unavailable explicit ID survives actual binary save/reopen", machine.saveWithoutMetadata()
                    .getString(AssemblerWork.TARGET).equals(blueprint.id()));
            exact("an unavailable explicit ID retains its exact owned work", 0.375, machine.inspectionProgress());
            check("unavailable targeting never spends power", machine.energy().getStored() == fuel);
            graph.set(null, index.invoke(null, List.of(blueprint, planner)));
            check("returning IDs re-resolve to the current recipe, not the missing-generation cache",
                    resolve(machine) == blueprint);
            check("AUTO can be restored without discarding fractional work", change(machine, "") == SelectionResult.SELECTED);
            expectedAuto = automaticTarget(machine);
            check("AUTO keeps live lookup behavior when only partial old work exists", resolve(machine) == expectedAuto);
            change(machine, blueprint.id());
            exact("AUTO did not transfer or reset the suspended fractional balance", 0.375, machine.inspectionProgress());

            state = machine.saveWithoutMetadata();
            state.putString("WorkRecipe", "removed/paid_owner");
            state.putDouble("Progress", 5.125);
            state.remove(AssemblerWork.SUSPENDED);
            machine.load(roundTrip(state));
            check("unknown paid work cannot pay the selected known target", resolve(machine) == blueprint);
            exact("a known target starts with no borrowed unknown work", 0.0, machine.inspectionProgress());
            check("unknown paid work remains unavailable but recoverably stored",
                    machine.recipeSelection().orElseThrow().suspended().get("removed/paid_owner") == 5.125);

            List<IngredientSpec> wetBill = new ArrayList<>(bill);
            wetBill.add(IngredientSpec.fluid("minecraft:water", 250));
            ProcessRecipe wet = new ProcessRecipe("check/selected/wet", MachineFamily.ASSEMBLER, wetBill,
                    blueprint.outputs(), Double.NaN, null, 20, 32);
            ProcessRecipe hot = new ProcessRecipe("check/selected/hot", MachineFamily.ASSEMBLER, bill,
                    planner.outputs(), 1500, null, 20, 32);
            // Private vanilla unit rows exercise generic fluid/condition guards, not live production recipes.
            graph.set(null, index.invoke(null, List.of(blueprint, planner, wet, hot)));
            change(machine, wet.id());
            state = machine.saveWithoutMetadata();
            state.put("Fluid", FluidNbt.save(FluidState.of("minecraft:water", 249)));
            machine.load(roundTrip(state));
            check("the actual selected BE rejects insufficient fluid without a dry fallback", resolve(machine) == null
                    && machine.recipeSelection().orElseThrow().issue() == SelectionIssue.MISSING_FLUID);
            tick(machine, 9);
            check("short selected fluid cannot draw another FU", machine.energy().getStored() == fuel);
            check("selected intake reserves only its own remaining batch, not a family's other recipe",
                    machine.acceptedAmount(null, FluidState.of("minecraft:water", 2000)) == 1
                            && machine.acceptedAmount(null, FluidState.of("minecraft:lava", 250)) == 0);
            machine.insert(null, FluidState.of("minecraft:water", 1));
            check("the current selected wet row resolves after the exact real store is replenished", resolve(machine) == wet);
            machine.extract(null, 1);
            check("an already cached explicit row revalidates actual fluid before paying more work", resolve(machine) == null);
            change(machine, hot.id());
            tick(machine, 10);
            check("the selected row still obeys actual machine conditions before drawing power",
                    machine.energy().getStored() == fuel
                            && machine.recipeSelection().orElseThrow().issue() == SelectionIssue.CONDITIONS);

            change(machine, blueprint.id());
            supplyAssembler(machine);
            machine.setItem(4, ItemStack.EMPTY);
            state = machine.saveWithoutMetadata();
            state.putDouble("Progress", blueprint.durationTicks() + 0.625);
            machine.load(roundTrip(state));
            resolve(machine);
            CompoundTag settlementBefore = machine.saveWithoutMetadata();
            boolean[] thrown = {false};
            ((SimpleContainer) machine.menuContainer()).addListener(container -> {
                if (!thrown[0] && machine.getItem(0).isEmpty()) {
                    thrown[0] = true;
                    throw new IllegalStateException("authored Assembler settlement observer");
                }
            });
            Method complete = ProcessMachineBlockEntity.class.getDeclaredMethod("complete", ProcessRecipe.class, List.class);
            complete.setAccessible(true);
            try {
                complete.invoke(machine, blueprint, List.of());
                check("the authored observer must abort actual settlement", false);
            } catch (InvocationTargetException expected) {
                check("actual completion exposes its original settlement failure",
                        expected.getCause() instanceof IllegalStateException
                                && expected.getCause().getMessage().equals("authored Assembler settlement observer"));
            }
            check("rollback preserves exact active/suspended paid work, target and every saved store",
                    machine.saveWithoutMetadata().equals(settlementBefore));
            check("a refused/throwing settlement releases its selector guard",
                    change(machine, planner.id()) == SelectionResult.SELECTED);
            graph.set(null, index.invoke(null, List.of(blueprint, planner)));
            assemblerLegacyInventory(blueprint, planner);
        } finally {
            graph.set(null, originalGraph);
        }
    }

    private static void assemblerRetiredWork(ProcessRecipe blueprint, ProcessRecipe planner) throws Exception {
        for (String retired : RETIRED_ASSEMBLER_TARGETS) {
            ProcessMachineBlockEntity machine = assembler();
            supplyAssembler(machine);
            machine.energy().setStored(512);
            CompoundTag state = machine.saveWithoutMetadata();
            state.putString("Recipe", retired);
            state.putString("WorkRecipe", retired);
            state.putString(AssemblerWork.TARGET, retired);
            double paid = 1600.375;
            state.putDouble("Progress", paid);
            machine.load(roundTrip(state));
            CompoundTag stores = processStores(machine);
            for (int reopen = 0; reopen < 3; reopen++) {
                check(retired + " explicitly refuses without falling back to a permitted same-bill tool",
                        resolve(machine) == null
                                && machine.recipeSelection().orElseThrow().issue() == SelectionIssue.UNAVAILABLE);
                CompoundTag raw = machine.saveWithoutMetadata();
                check(retired + " retains its raw target, owner and complete/fractional paid amount",
                        raw.getString(AssemblerWork.TARGET).equals(retired)
                                && raw.getString("WorkRecipe").equals(retired)
                                && Double.doubleToRawLongBits(raw.getDouble("Progress"))
                                == Double.doubleToRawLongBits(paid));
                check(retired + " cannot be requested again as a new current target",
                        change(machine, retired) == SelectionResult.INVALID_RECIPE
                                && machine.saveWithoutMetadata().equals(raw));
                tick(machine, 100 + reopen);
                exact(retired + " cannot spend, refund or complete old paid work", paid, machine.inspectionProgress());
                check(retired + " does not draw Flux or change physical stores",
                        processStores(machine).equals(stores) && machine.getItem(4).isEmpty());
                machine.load(roundTrip(machine.saveWithoutMetadata()));
            }
            for (ProcessRecipe permitted : List.of(blueprint, planner)) {
                check("a permitted target can be selected without deleting retired work: " + permitted.id(),
                        change(machine, permitted.id()) == SelectionResult.SELECTED);
                exact("retired work never pays permitted target " + permitted.id(), 0.0, machine.inspectionProgress());
                check("the exact retired paid balance stays suspended and unspendable",
                        machine.recipeSelection().orElseThrow().suspended().get(retired) == paid);
                tick(machine, permitted == blueprint ? 200 : 201);
                exact("the permitted target must buy its own upgraded-independent work", 1.0,
                        machine.inspectionProgress());
                check("only the permitted working tick draws its declared Flux",
                        machine.energy().getStored() == 512 - blueprint.fuPerTick()
                                - (permitted == planner ? planner.fuPerTick() : 0)
                                && machine.getItem(4).isEmpty());
            }
            machine.load(roundTrip(machine.saveWithoutMetadata()));
            check("binary reopen retains, rather than transfers or erases, the old normal-machine job",
                    machine.recipeSelection().orElseThrow().suspended().get(retired) == paid);
        }
    }

    private static void assemblerLegacyInventory(ProcessRecipe blueprint, ProcessRecipe planner) throws Exception {
        ProcessMachineBlockEntity legacy = assembler();
        supplyAssembler(legacy);
        ProcessRecipe expectedAuto = automaticTarget(legacy);
        ProcessRecipe paidOwner = expectedAuto.id().equals(blueprint.id()) ? planner : blueprint;
        legacy.setItem(3, ItemStack.EMPTY);
        CompoundTag tag = legacy.saveWithoutMetadata();
        tag.putString("Recipe", paidOwner.id());
        tag.remove("WorkRecipe");
        tag.putDouble("Progress", 11.34375);
        tag.putInt("ItemLayout", 1);
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        CompoundTag output = new ItemStack(Items.BRICK, 2).save(new CompoundTag());
        output.putByte("Slot", (byte) 3);
        items.add(output);
        legacy.load(roundTrip(tag));
        check("legacy Assembler output migration keeps slot four and clears the old fourth input",
                legacy.getItem(3).isEmpty() && legacy.getItem(4).is(Items.BRICK) && legacy.getItem(4).getCount() == 2);
        check("legacy Recipe fallback retains the exact old paid owner",
                legacy.saveWithoutMetadata().getString("WorkRecipe").equals(paidOwner.id())
                        && legacy.inspectionProgress() == 11.34375);
        if (blueprint.itemInputs().size() > 3) {
            check("old inventory layout cannot invent a newly required fourth ingredient", resolve(legacy) == null);
            IngredientSpec required = blueprint.itemInputs().get(3);
            legacy.setItem(3, ProcessLookup.resolve(
                    new OutputSpec(required.kind(), required.id(), required.count(), false)).orElseThrow());
        }
        expectedAuto = automaticTarget(legacy);
        check("legacy AUTO fixture starts with incomplete paid work",
                legacy.inspectionProgress() < paidOwner.durationTicks());
        check("legacy AUTO fixture has a different current target than its paid owner: " + expectedAuto.id(),
                !expectedAuto.id().equals(paidOwner.id()));
        ProcessRecipe actualAuto = resolve(legacy);
        check("legacy AUTO preserves current lookup authority: expected=" + expectedAuto.id()
                        + ", actual=" + (actualAuto == null ? "<none>" : actualAuto.id()),
                actualAuto != null && actualAuto.id().equals(expectedAuto.id()));
        exact("legacy AUTO grants zero borrowed paid work: owner="
                        + legacy.saveWithoutMetadata().getString("WorkRecipe")
                        + ", actual=" + Double.toString(legacy.inspectionProgress()),
                0.0, legacy.inspectionProgress());
        change(legacy, paidOwner.id());
        exact("the legacy partial balance remains reachable after AUTO chooses another row", 11.34375,
                legacy.inspectionProgress());
        check("migration and selection never mutate the old output's count or slot",
                (blueprint.itemInputs().size() > 3
                        ? ProcessLookup.matches(blueprint.itemInputs().get(3), legacy.getItem(3))
                        : legacy.getItem(3).isEmpty())
                        && legacy.getItem(4).is(Items.BRICK) && legacy.getItem(4).getCount() == 2);
    }

    /**
     * Actual Bench BE/NBT plus the real retained brick-block row, not a fabricated production fixture.
     * Positive orphaned work must remain held across matching, inspection, cache adoption and ticks.
     */
    private static void benchRetiredWork() throws Exception {
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        ProcessRecipe permitted = ProcessLookup.byId(MachineFamily.ASSEMBLY_BENCH, "bench/brick_block").orElseThrow();
        check("the Bench fallback witness is the actual retained vanilla brick-block basic",
                permitted.itemOutputs().equals(List.of(OutputSpec.item("minecraft:bricks", 1)))
                        && permitted.fluidInputs().isEmpty() && permitted.catalysts().isEmpty());
        check("the existing retired Field Guide identity remains a separate compatibility exception",
                LegacyGuideAssembly.retired(MachineFamily.ASSEMBLY_BENCH, "bench/field_guide"));
        // Real retired processors plus unknown/unattributed ownership counterexamples, not extra live recipes.
        for (String retired : List.of("bench/furnace", "bench/quern", "removed/bench/paid", "")) {
            check("the paid Bench owner is unavailable in the actual current catalogue: " + retired,
                    ProcessLookup.byId(MachineFamily.ASSEMBLY_BENCH, retired).isEmpty());
            for (boolean legacyRecipeOnly : List.of(false, true)) {
                for (double paid : List.of(0.34375, SourceLogic.BENCH_TICKS + 0.375)) {
                    String context = retired + " paid=" + paid + " legacyRecipeOnly=" + legacyRecipeOnly;
                    check(context + " cannot invent old-normal completion through the Guide exception",
                            LegacyGuideAssembly.resume(MachineFamily.ASSEMBLY_BENCH, retired, paid).isEmpty());
                    ProcessMachineBlockEntity bench = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
                    bench.energy().setStored(bench.energy().getCapacity());
                    CompoundTag saved = bench.saveWithoutMetadata();
                    saved.putString("Recipe", retired);
                    saved.putDouble("Progress", paid);
                    if (legacyRecipeOnly) {
                        saved.remove("WorkRecipe");
                    } else {
                        saved.putString("WorkRecipe", retired);
                    }
                    bench.load(roundTrip(saved));
                    retainedBenchWork(bench, retired, paid, context + " load");
                    CompoundTag stores = processStores(bench);
                    for (int reopen = 0; reopen < 3; reopen++) {
                        check(context + " has no executable batch with missing materials", resolve(bench) == null);
                        tick(bench, 10 + reopen);
                        retainedBenchWork(bench, retired, paid, context + " empty/reopen=" + reopen);
                        check(context + " held work is an explicit fault even with no remaining inputs",
                                bench.menuStatus() == MachineStatus.OUT_OF_BAND);
                        check(context + " cannot draw, refund or alter stores while unavailable",
                                processStores(bench).equals(stores));
                        bench.load(roundTrip(bench.saveWithoutMetadata()));
                    }

                    // Supply exactly one genuine CURRENT basic bill, derived from the actual row.
                    // The input-only lookup must match; the paid Bench must nevertheless refuse it.
                    supplyBench(bench, permitted);
                    ProcessRecipe wouldMatch = automaticTarget(bench);
                    check(context + " really offers a permitted competing batch, not an empty-lookup false positive",
                            wouldMatch.id().equals(permitted.id()));
                    stores = processStores(bench);
                    for (int reopen = 0; reopen < 3; reopen++) {
                        check(context + " unavailable paid Bench has no permitted-recipe fallback " + reopen,
                                resolve(bench) == null);
                        retainedBenchWork(bench, retired, paid, context + " after competing lookup=" + reopen);
                        tick(bench, 100 + reopen);
                        retainedBenchWork(bench, retired, paid, context + " after competing tick=" + reopen);
                        check(context + " a competing bill cannot clear the held-work fault",
                                bench.menuStatus() == MachineStatus.OUT_OF_BAND);
                        check(context + " keeps inputs/output, Flux, fluids and configuration unchanged",
                                processStores(bench).equals(stores));
                        bench.load(roundTrip(bench.saveWithoutMetadata()));
                        retainedBenchWork(bench, retired, paid, context + " competing binary reopen=" + reopen);
                    }
                }
            }
        }
    }

    private static void retainedBenchWork(ProcessMachineBlockEntity bench, String retired, double paid, String context)
            throws ReflectiveOperationException {
        CompoundTag saved = bench.saveWithoutMetadata();
        exact(context + " retains the exact paid balance", paid, bench.inspectionProgress());
        exact(context + " retains the raw Progress double", paid, saved.getDouble("Progress"));
        check(context + " retains the actual old WorkRecipe owner", saved.getString("WorkRecipe").equals(retired));
        check(context + " is actual Bench state, not an invented Assembler selection/work queue",
                bench.kind() == ProcessMachineKind.ASSEMBLY_BENCH && bench.recipeSelection().isEmpty()
                        && !saved.contains(AssemblerWork.TARGET) && !saved.contains(AssemblerWork.SUSPENDED));
        check(context + " read-only inspection cannot expose a cached competitor", bench.inspectionRecipe().isEmpty());
        check(context + " read-only matching cannot select another row", matching(bench).isEmpty());
        check(context + " the existing fault presentation names retained paid work",
                bench.menuFault().equals(Component.translatable("gui.grindless.bench.retained_work",
                        retired, Double.toString(paid))));
        check(context + " inspection/matching/fault reads cannot change saved ownership or stores",
                bench.saveWithoutMetadata().equals(saved));
    }

    private static void benchWorkNonRegression() throws Exception {
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        ProcessRecipe permitted = ProcessLookup.byId(MachineFamily.ASSEMBLY_BENCH, "bench/brick_block").orElseThrow();
        for (String unpaidOwner : List.of("", "removed/bench/unpaid")) {
            ProcessMachineBlockEntity bench = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
            supplyBench(bench, permitted);
            bench.energy().setStored(bench.energy().getCapacity());
            CompoundTag saved = bench.saveWithoutMetadata();
            saved.putString("Recipe", unpaidOwner);
            saved.putString("WorkRecipe", unpaidOwner);
            saved.putDouble("Progress", 0.0);
            bench.load(roundTrip(saved));
            CompoundTag before = bench.saveWithoutMetadata();
            check("zero/unpaid ordinary Bench matching remains functional: " + unpaidOwner,
                    matching(bench).orElseThrow() == permitted && bench.saveWithoutMetadata().equals(before));
            check("zero/unpaid Bench has no orphaned-paid-work fault", bench.menuFault().getString().isEmpty());
            check("zero/unpaid Bench can adopt the actual current row", resolve(bench) == permitted);
            exact("ordinary adoption cannot invent credit", 0.0, bench.inspectionProgress());
            long fuel = bench.energy().getStored();
            tick(bench, 1);
            exact("ordinary Bench work still buys one base tick", 1.0, bench.inspectionProgress());
            check("ordinary Bench work pays only its real declared Flux",
                    bench.energy().getStored() == fuel - permitted.fuPerTick()
                            && bench.saveWithoutMetadata().getString("WorkRecipe").equals(permitted.id()));
        }

        ProcessMachineBlockEntity known = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
        supplyBench(known, permitted);
        known.energy().setStored(known.energy().getCapacity());
        CompoundTag saved = known.saveWithoutMetadata();
        saved.putString("Recipe", permitted.id());
        saved.putString("WorkRecipe", permitted.id());
        saved.putDouble("Progress", permitted.durationTicks() - 0.625);
        known.load(roundTrip(saved));
        check("a live known paid Bench recipe still resolves and is inspectable",
                resolve(known) == permitted && known.inspectionRecipe().orElseThrow() == permitted);
        long fuel = known.energy().getStored();
        tick(known, 1);
        exact("a live known Bench settles once with fractional carry", 0.375, known.inspectionProgress());
        check("the actual current basic still consumes its bill and outputs once",
                known.getItem(0).isEmpty() && known.getItem(known.menuKind().inputs()).is(Items.BRICKS)
                        && known.getItem(known.menuKind().inputs()).getCount() == permitted.itemOutputs().get(0).count()
                        && known.energy().getStored() == fuel - permitted.fuPerTick());

        for (double paid : List.of(0.34375, SourceLogic.BENCH_TICKS + 0.375)) {
            ProcessRecipe guide = LegacyGuideAssembly.resume(
                    MachineFamily.ASSEMBLY_BENCH, "bench/field_guide", paid).orElseThrow();
            ProcessMachineBlockEntity bench = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
            supplyBench(bench, guide);
            bench.energy().setStored(bench.energy().getCapacity());
            saved = bench.saveWithoutMetadata();
            saved.putString("Recipe", guide.id());
            saved.remove("WorkRecipe");
            saved.putDouble("Progress", paid);
            bench.load(roundTrip(saved));
            CompoundTag before = bench.saveWithoutMetadata();
            check("valid paid legacy Guide remains an authorized read-only compatibility row",
                    matching(bench).orElseThrow().id().equals(guide.id())
                            && bench.saveWithoutMetadata().equals(before));
            check("legacy Guide cache adoption and inspection preserve its actual paid owner",
                    resolve(bench).id().equals(guide.id())
                            && bench.inspectionRecipe().orElseThrow().id().equals(guide.id())
                            && bench.menuFault().getString().isEmpty());
            exact("legacy Guide exact paid credit survives resolution", paid, bench.inspectionProgress());
            if (paid < guide.durationTicks()) {
                fuel = bench.energy().getStored();
                tick(bench, 1);
                exact("the actual partial Guide path can still buy its next tick", paid + 1.0, bench.inspectionProgress());
                check("the partial legacy Guide tick draws its own Flux once",
                        bench.energy().getStored() == fuel - guide.fuPerTick());
            }
            // Completed Guide resolution is checked, not completion with an unregistered vanilla output carrier.
            bench.load(roundTrip(bench.saveWithoutMetadata()));
            check("legacy Recipe-only Guide ownership remains recoverable after binary reopen",
                    bench.saveWithoutMetadata().getString("WorkRecipe").equals(guide.id()));
        }

        ProcessMachineBlockEntity invalidGuide = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
        supplyBench(invalidGuide, permitted);
        saved = invalidGuide.saveWithoutMetadata();
        saved.putString("Recipe", "bench/field_guide");
        saved.putString("WorkRecipe", "bench/field_guide");
        saved.putDouble("Progress", Double.POSITIVE_INFINITY);
        invalidGuide.load(roundTrip(saved));
        retainedBenchWork(invalidGuide, "bench/field_guide", Double.POSITIVE_INFINITY,
                "nonfinite Guide credit cannot borrow the valid paid-Guide exception");
    }

    private static void benchCacheAndGeneration() throws Exception {
        ProcessRecipe permitted = ProcessLookup.byId(MachineFamily.ASSEMBLY_BENCH, "bench/brick_block").orElseThrow();
        ProcessMachineBlockEntity stale = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
        supplyBench(stale, permitted);
        CompoundTag saved = stale.saveWithoutMetadata();
        saved.putString("Recipe", "removed/bench/cached_owner");
        saved.putString("WorkRecipe", "removed/bench/cached_owner");
        saved.putDouble("Progress", 0.34375);
        stale.load(roundTrip(saved));
        // Hostile/stale private cache counterexample over a real current row, not a fake production registration.
        Field cache = ProcessMachineBlockEntity.class.getDeclaredField("cached");
        cache.setAccessible(true);
        cache.set(stale, permitted);
        retainedBenchWork(stale, "removed/bench/cached_owner", 0.34375, "same-generation stale cache");
        check("the actual cache fast path cannot adopt a competitor for orphaned paid Bench work",
                resolve(stale) == null && cache.get(stale) == null);
        retainedBenchWork(stale, "removed/bench/cached_owner", 0.34375, "refused cache adoption");

        ProcessMachineBlockEntity bench = processMachine(ProcessMachineKind.ASSEMBLY_BENCH);
        supplyBench(bench, permitted);
        saved = bench.saveWithoutMetadata();
        saved.putString("Recipe", permitted.id());
        saved.putString("WorkRecipe", permitted.id());
        saved.putDouble("Progress", 0.34375);
        bench.load(roundTrip(saved));
        check("the generation fixture begins with an actual live cached paid row", resolve(bench) == permitted);
        Field graph = ProcessLookup.class.getDeclaredField("GRAPH");
        graph.setAccessible(true);
        Object original = graph.get(null);
        Method index = original.getClass().getDeclaredMethod("index", List.class);
        index.setAccessible(true);
        ProcessLookup.Generation generation = ProcessLookup.generation();
        try {
            // Unit-only catalogue withdrawal of one real basic; never register or return a retired normal recipe.
            graph.set(null, index.invoke(null, ProcessLookup.recipes().stream()
                    .filter(row -> !row.id().equals(permitted.id())).toList()));
            check("the actual indexed publication changes generation", generation != ProcessLookup.generation());
            retainedBenchWork(bench, permitted.id(), 0.34375, "generation withdraws a cached paid owner");
            check("generation invalidation cannot resurrect the old cached row", resolve(bench) == null);
            tick(bench, 1);
            retainedBenchWork(bench, permitted.id(), 0.34375, "withdrawn owner after ordinary work");
            bench.load(roundTrip(bench.saveWithoutMetadata()));
            retainedBenchWork(bench, permitted.id(), 0.34375, "withdrawn owner binary reopen");
        } finally {
            graph.set(null, original);
        }
        check("returning the actual permitted basic resumes its own exact paid credit",
                resolve(bench) == permitted && bench.inspectionProgress() == 0.34375);
    }

    private static void supplyBench(ProcessMachineBlockEntity bench, ProcessRecipe recipe) {
        for (int slot = 0; slot < bench.menuKind().inputs(); slot++) {
            bench.setItem(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < recipe.itemInputs().size(); slot++) {
            IngredientSpec input = recipe.itemInputs().get(slot);
            ItemStack physical = ProcessLookup.resolve(
                    new OutputSpec(input.kind(), input.id(), input.count(), false)).orElseThrow();
            check("actual Bench fixture material group matches " + recipe.id() + "/" + slot,
                    ProcessLookup.matches(input, physical));
            bench.setItem(slot, physical);
        }
    }

    private static CompoundTag processStores(ProcessMachineBlockEntity machine) {
        CompoundTag stores = machine.saveWithoutMetadata();
        for (String key : List.of("Progress", "Recipe", "WorkRecipe", AssemblerWork.TARGET, AssemblerWork.SUSPENDED)) {
            stores.remove(key);
        }
        return stores;
    }

    private static ProcessMachineBlockEntity assembler() throws Exception {
        return processMachine(ProcessMachineKind.ASSEMBLER);
    }

    private static ProcessMachineBlockEntity processMachine(ProcessMachineKind kind) throws Exception {
        Constructor<ProcessMachineBlockEntity> constructor = ProcessMachineBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, ProcessMachineKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, kind, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static void supplyAssembler(ProcessMachineBlockEntity machine) {
        ProcessRecipe recipe = ProcessLookup.byId(MachineFamily.ASSEMBLER, BLUEPRINT).orElseThrow();
        for (int slot = 0; slot < machine.menuKind().inputs(); slot++) {
            machine.setItem(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < recipe.itemInputs().size(); slot++) {
            IngredientSpec input = recipe.itemInputs().get(slot);
            machine.setItem(slot, ProcessLookup.resolve(
                    new OutputSpec(input.kind(), input.id(), input.count(), false)).orElseThrow());
        }
    }

    private static ProcessRecipe automaticTarget(ProcessMachineBlockEntity machine) {
        ItemStack[] inputs = new ItemStack[machine.menuKind().inputs()];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = machine.getItem(slot).copy();
        }
        return ProcessLookup.find(machine.kind().family(), inputs, machine.contents())
                .orElseThrow(() -> new IllegalStateException("AUTO fixture has no current matching target"));
    }

    private static SelectionResult change(ProcessMachineBlockEntity machine, String id) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("changeRecipeSelection", String.class);
        method.setAccessible(true);
        return (SelectionResult) method.invoke(machine, id);
    }

    private static ProcessRecipe resolve(ProcessMachineBlockEntity machine) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("recipe", List.class);
        method.setAccessible(true);
        return (ProcessRecipe) method.invoke(machine, List.<FluidInsert.Source>of());
    }

    @SuppressWarnings("unchecked")
    private static Optional<ProcessRecipe> matching(ProcessMachineBlockEntity machine) throws ReflectiveOperationException {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("matchingRecipe", List.class);
        method.setAccessible(true);
        return (Optional<ProcessRecipe>) method.invoke(machine, List.<FluidInsert.Source>of());
    }

    private static void tick(ProcessMachineBlockEntity machine, long tick) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
        method.setAccessible(true);
        method.invoke(machine, tick, List.<FluidInsert.Source>of());
    }

    private static void exact(String label, double expected, double actual) {
        check(label, Double.doubleToRawLongBits(expected) == Double.doubleToRawLongBits(actual));
    }

    private static void matrixCodec() throws Exception {
        ControlMatrixSpec f2 = new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F2);
        CompoundTag saved = ControlMatrixNbt.save(f2);
        check("Matrix serialization keeps the original integer Rating key",
                saved.contains("Rating", 3) && saved.getInt("Rating") == 2 && saved.getAllKeys().size() == 1);
        ControlMatrixSpec decoded = ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(saved)).orElseThrow();
        check("binary NBT save/load reconstructs the exact F2 specification", decoded.equals(f2));
        check("untagged Relay remains F1",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating() == FluxTier.F1);
        // NBT/data evidence, not a registered-Matrix positive matching or Forge qualification.
        IngredientSpec control = IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1);
        check("valid decoded F2 supplies the minimum control contract (NBT/data, not registered item proof)",
                control.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decoded));
        check("untagged legacy F1 agrees with the explicit minimum rating (NBT/data contract)",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating().ordinal()
                        == control.matrixRating());
        CompoundTag explicitF1 = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F1));
        check("explicit F1 agrees with the minimum rating (NBT/data contract)",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(explicitF1))
                        .orElseThrow().rating().ordinal() == control.matrixRating());
        CompoundTag explicitF3 = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F3));
        ControlMatrixSpec decodedF3 = ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(explicitF3))
                .orElseThrow();
        check("valid decoded F3 supplies the minimum control contract (NBT/data, not registered item proof)",
                decodedF3.rating() == FluxTier.F3
                        && control.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decodedF3));
        IngredientSpec predecessor = IngredientSpec.matrix(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1);
        check("the same valid F2 cannot serve as its own exact predecessor",
                !predecessor.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decoded));
        check("a missing Matrix stack fails the real ingredient matcher",
                !ProcessLookup.matches(control, ItemStack.EMPTY));
        ItemStack wrongKind = new ItemStack(Items.PAPER);
        wrongKind.setTag(explicitF1.copy());
        check("an F1 tag on a non-Matrix item fails the real ingredient matcher",
                !ProcessLookup.matches(control, wrongKind));
        Path common = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
        String matrixItem = Files.readString(common.resolve("item/ControlMatrixItem.java"));
        String lookup = Files.readString(common.resolve("recipe/ProcessLookup.java"));
        check("registered Matrix reads reuse this NBT decoder (source contract)",
                matrixItem.contains("return ControlMatrixNbt.load(architecture, stack.getTag());"));
        check("native input matching validates the actual Matrix before dispatching its typed role (source contract)",
                lookup.contains("stack.getItem() instanceof ControlMatrixItem matrix")
                        && lookup.contains("matrix.spec(stack)")
                        && lookup.contains("spec.acceptsMatrix("));
        check("missing Rating on a nonempty tag remains the architecture's first frontier",
                ControlMatrixNbt.load(MatrixArchitecture.INTEGRATED, new CompoundTag())
                        .orElseThrow().rating() == FluxTier.F4);
        List<Consumer<CompoundTag>> numeric = List.of(
                tag -> tag.putByte("Rating", (byte) 2), tag -> tag.putShort("Rating", (short) 2),
                tag -> tag.putInt("Rating", 2), tag -> tag.putLong("Rating", 2L),
                tag -> tag.putFloat("Rating", 2.0f), tag -> tag.putDouble("Rating", 2.0));
        for (int index = 0; index < numeric.size(); index++) {
            CompoundTag tag = new CompoundTag();
            numeric.get(index).accept(tag);
            CompoundTag before = tag.copy();
            check("lossless legacy numeric rating type " + index + " remains valid",
                    ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).orElseThrow().equals(f2));
            check("reading numeric rating type " + index + " does not rewrite it", tag.equals(before));
        }
        rejectRating("F0 is not a Matrix", tag -> tag.putInt("Rating", 0));
        rejectRating("negative ratings are invalid", tag -> tag.putInt("Rating", -1));
        rejectRating("Relay does not carry F4", tag -> tag.putInt("Rating", 4));
        rejectRating("out-of-ladder ratings are invalid", tag -> tag.putInt("Rating", 16));
        rejectRating("a numeric-looking string is invalid", tag -> tag.putString("Rating", "2"));
        rejectRating("a compound is not a rating", tag -> tag.put("Rating", new CompoundTag()));
        rejectRating("fractional doubles cannot become F2", tag -> tag.putDouble("Rating", 2.75));
        rejectRating("fractional floats cannot become F1", tag -> tag.putFloat("Rating", 1.5f));
        rejectRating("overflowing longs cannot wrap into F2", tag -> tag.putLong("Rating", 4_294_967_298L));
        rejectRating("NaN is invalid", tag -> tag.putDouble("Rating", Double.NaN));
        rejectRating("infinite ratings are invalid", tag -> tag.putDouble("Rating", Double.POSITIVE_INFINITY));
    }

    private static void rejectRating(String label, Consumer<CompoundTag> write) {
        CompoundTag tag = new CompoundTag();
        write.accept(tag);
        check(label, ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).isEmpty());
    }

    private static void itemPacket() {
        ItemStack carrier = new ItemStack(Items.PAPER, 3);
        carrier.getOrCreateTag().putInt("Rating", 2);
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.writeItem(carrier);
            ItemStack received = packet.readItem();
            check("vanilla item synchronization preserves rating payload and count",
                    ItemStack.isSameItemSameTags(carrier, received) && received.getCount() == 3);
        } finally {
            packet.release();
        }
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

    private static void questInventory() {
        Inventory inventory = new Inventory(null);
        inventory.setItem(40, new ItemStack(Items.PAPER, 3));
        QuestCatalogue.Task paper = new QuestCatalogue.Task("paper", "check", QuestCatalogue.Evidence.ITEM,
                "minecraft:paper", 4, null, List.of(), new QuestCatalogue.Reward("minecraft:paper", 1));
        check("the actual player inventory includes its offhand slot", inventory.getContainerSize() == 41);
        check("an offhand stack is counted exactly once", QuestInventoryEvidence.count(inventory, paper) == 3);
        inventory.setItem(0, new ItemStack(Items.PAPER, 2));
        check("main and offhand stacks add without duplication", QuestInventoryEvidence.count(inventory, paper) == 5);
        check("evidence counting never consumes the inventory",
                inventory.getItem(0).getCount() == 2 && inventory.getItem(40).getCount() == 3);
        inventory.getItem(0).getOrCreateTag().putInt("Rating", 2);
        QuestCatalogue.Task fake = new QuestCatalogue.Task("fake", "check", QuestCatalogue.Evidence.MATRIX,
                "minecraft:paper", 1, FluxTier.F2, List.of(), new QuestCatalogue.Reward("minecraft:paper", 1));
        check("a rating tag on a non-Matrix item cannot satisfy Matrix evidence",
                QuestInventoryEvidence.count(inventory, fake) == 0);
    }

    private static void questProgress() throws Exception {
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        CompoundTag oldPlayer = new CompoundTag();
        oldPlayer.putString("Player", player.toString());
        ListTag oldTasks = new ListTag();
        for (String id : List.of("multitool", "quern", "ceramics", "calibrate", "industrial_matrix")) {
            oldTasks.add(StringTag.valueOf(id));
        }
        oldPlayer.put("Tasks", oldTasks);
        ListTag players = new ListTag();
        players.add(oldPlayer);
        CompoundTag legacy = new CompoundTag();
        legacy.put("Players", players);
        QuestProgress restored = QuestProgress.load(roundTrip(legacy));
        Set<String> previous = Set.of("multitool", "quern", "ceramics", "calibrate", "industrial_matrix");
        check("binary loading of a pre-starter save preserves every old stable quest id",
                restored.claimed(player).equals(previous));
        check("Players-only binary saves remain compatible without inventing native authority",
                legacy.getAllKeys().equals(Set.of("Players"))
                        && !restored.usesNativeClaims(player) && !restored.usesNativeClaims(otherPlayer));
        check("upgrading an old save does not automatically claim the new starter",
                !restored.claimed(player).contains(QuestCatalogue.STARTER_SUPPLIES));
        check("the new explicit starter claim fits the existing ledger",
                restored.claim(player, QuestCatalogue.STARTER_SUPPLIES));
        Set<String> paid = new HashSet<>(previous);
        paid.add(QuestCatalogue.STARTER_SUPPLIES);
        for (int reload = 0; reload < 3; reload++) {
            CompoundTag saved = restored.save(new CompoundTag());
            check("quest progress adds only NativeClaims and keeps the original player/task schema",
                    saved.getAllKeys().equals(Set.of("Players", "NativeClaims"))
                            && saved.get("Players") instanceof ListTag savedPlayers
                            && savedPlayers.getElementType() == Tag.TAG_COMPOUND && savedPlayers.size() == 1
                            && savedPlayers.getCompound(0).getAllKeys().equals(Set.of("Player", "Tasks"))
                            && savedPlayers.getCompound(0).getString("Player").equals(player.toString())
                            && savedPlayers.getCompound(0).get("Tasks") instanceof ListTag savedTasks
                            && savedTasks.getElementType() == Tag.TAG_STRING && savedTasks.size() == paid.size());
            check("legacy saves persist an exactly empty native authority list",
                    new ListTag().equals(saved.get("NativeClaims")));
            restored = QuestProgress.load(roundTrip(saved));
            check("binary save/reload retains old claims alongside starter supplies",
                    restored.claimed(player).containsAll(previous)
                            && restored.claimed(player).contains(QuestCatalogue.STARTER_SUPPLIES));
            check("binary save/reload never resets the one-time starter claim",
                    !restored.claim(player, QuestCatalogue.STARTER_SUPPLIES));
            check("binary reconstruction never gives another player the starter claim",
                    restored.claimed(UUID.randomUUID()).isEmpty());
            check("binary legacy reload retains exactly the paid receipts without native authority",
                    restored.legacyClaims(player).equals(paid)
                            && !restored.usesNativeClaims(player) && !restored.usesNativeClaims(otherPlayer));
        }
        CompoundTag paidSave = restored.save(new CompoundTag());
        restored.useNativeClaims(player);
        restored.useNativeClaims(player);
        ListTag nativePlayers = new ListTag();
        nativePlayers.add(StringTag.valueOf(player.toString()));
        CompoundTag nativeSave = restored.save(new CompoundTag());
        for (int reload = 0; reload < 3; reload++) {
            check("native authority refuses a conflicting unpaid known legacy claim",
                    QuestCatalogue.byId("crank") != null && !restored.legacyClaims(player).contains("crank")
                            && !restored.claim(player, "crank"));
            restored.updateNativeCompleted(player, Set.of("crank"));
            check("native completion opens compatibility gates without becoming a paid receipt",
                    restored.claimed(player).contains("crank") && restored.legacyClaims(player).equals(paid));
            CompoundTag saved = restored.save(new CompoundTag());
            CompoundTag persisted = roundTrip(saved);
            check("binary native saves keep exactly the two ledger roots and the migrated UUID marker",
                    persisted.getAllKeys().equals(Set.of("Players", "NativeClaims"))
                            && nativePlayers.equals(persisted.get("NativeClaims")));
            check("migration retains the original paid player/task NBT unchanged",
                    paidSave.get("Players").equals(persisted.get("Players")));
            check("native observations never change the full persisted reward ledger",
                    saved.equals(nativeSave) && persisted.equals(saved));
            restored = QuestProgress.load(persisted);
            check("binary reload preserves native authority for only the migrated UUID",
                    restored.usesNativeClaims(player) && !restored.usesNativeClaims(otherPlayer));
            check("binary reload retains old receipts and starter but discards native observations",
                    restored.legacyClaims(player).equals(paid) && restored.claimed(player).equals(paid)
                            && !restored.claim(player, QuestCatalogue.STARTER_SUPPLIES));
            check("binary native reconstruction never gives another UUID receipts or completion",
                    restored.legacyClaims(otherPlayer).isEmpty() && restored.claimed(otherPlayer).isEmpty());
        }
        ListTag wrongElement = new ListTag();
        wrongElement.add(IntTag.valueOf(1));
        ListTag wrongPlayer = nativePlayers.copy();
        wrongPlayer.add(StringTag.valueOf("not-a-player"));
        List<Tag> malformedMarkers = List.of(StringTag.valueOf(player.toString()), new CompoundTag(),
                wrongElement, wrongPlayer);
        for (int corruption = 0; corruption < malformedMarkers.size(); corruption++) {
            CompoundTag corrupt = nativeSave.copy();
            corrupt.put("NativeClaims", malformedMarkers.get(corruption).copy());
            CompoundTag persisted = roundTrip(corrupt);
            boolean rejected = false;
            try {
                QuestProgress.load(persisted);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            check("malformed NativeClaims case " + corruption + " fails closed on binary reload",
                    rejected && persisted.equals(corrupt));
        }
        check("the quest SavedData key has not migrated", QuestProgress.KEY.equals("grindless_quest_progress"));
    }

    private static void check(String label, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + label);
        if (!passed) {
            failures++;
        }
    }
}
