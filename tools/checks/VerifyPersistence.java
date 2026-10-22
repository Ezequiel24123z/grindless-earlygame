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
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Executable vanilla NBT and inventory contracts; real registered Matrices also need a Forge smoke. */
public final class VerifyPersistence {

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
        String washer = "assemble/chemical_washer";
        String reactor = "assemble/chemical_reactor";
        AssemblerWork work = new AssemblerWork();
        work.activate("removed/paid", 3.375, washer);
        work.activate(washer, 12.03125, reactor);
        work.select("removed/selected");
        CompoundTag saved = new CompoundTag();
        saved.putString("Recipe", reactor);
        saved.putString("WorkRecipe", reactor);
        saved.putDouble("Progress", 400.1);
        work.save(saved);
        CompoundTag original = saved.copy();
        for (int reopen = 0; reopen < 24; reopen++) {
            saved = roundTrip(saved);
            AssemblerWork restored = AssemblerWork.load(saved, saved.getString("WorkRecipe"), saved.getDouble("Progress"));
            check("binary reopen " + reopen + " preserves unknown selected identity without AUTO",
                    restored.valid() && restored.selected().equals("removed/selected"));
            check("binary reopen " + reopen + " preserves exact known and unavailable paid balances",
                    restored.suspended().equals(java.util.Map.of("removed/paid", 3.375, washer, 12.03125)));
            restored.save(saved);
            check("binary reopen " + reopen + " cannot duplicate, normalize or erase work", saved.equals(original));
        }
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Recipe", washer);
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
                        .putString("Recipe", reactor),
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
                refused.select(washer);
                check("malformed paid state cannot change configuration", false);
            } catch (IllegalStateException expected) {
                check("malformed paid state refuses selection " + index, true);
            }
            refused.save(damaged);
            check("malformed paid state " + index + " is retained exactly through binary reopen",
                    roundTrip(damaged).equals(before));
        }
    }

    /** Runs the actual BE without Forge registration, using explicitly labelled vanilla carrier rows. */
    private static void assemblerMachineWork() throws Exception {
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        List<IngredientSpec> bill = List.of(IngredientSpec.item("minecraft:quartz", 1),
                IngredientSpec.item("minecraft:copper_ingot", 2), IngredientSpec.item("minecraft:iron_ingot", 4));
        ProcessRecipe washer = new ProcessRecipe("assemble/chemical_washer", MachineFamily.ASSEMBLER, bill,
                List.of(OutputSpec.item("minecraft:clay_ball", 1)), Double.NaN, null, 160, 32);
        ProcessRecipe reactor = new ProcessRecipe("assemble/chemical_reactor", MachineFamily.ASSEMBLER, bill,
                List.of(OutputSpec.item("minecraft:brick", 1)), Double.NaN, null, 160, 32);
        Field graph = ProcessLookup.class.getDeclaredField("GRAPH");
        graph.setAccessible(true);
        Object originalGraph = graph.get(null);
        Method index = originalGraph.getClass().getDeclaredMethod("index", List.class);
        index.setAccessible(true);
        try {
            graph.set(null, index.invoke(null, List.of(washer, reactor)));
            ProcessMachineBlockEntity machine = assembler();
            supplyAssembler(machine);
            check("legacy unselected AUTO retains the actual last-dry resolver (carriers)",
                    resolve(machine).id().equals(reactor.id()));
            check("the real selection transition chooses the exact Washer independently (carriers)",
                    change(machine, washer.id()) == SelectionResult.SELECTED && resolve(machine) == washer);
            Object inventory = machine.menuContainer();
            Object energy = machine.energy();
            Object fluids = machine.nativeStore();
            Object config = machine.containerConfig();
            machine.energy().setStored(512);
            machine.containerConfig().setSideMode(Direction.NORTH,
                    io.github.ezequiel24123z.grindless.container.SideMode.EXTRACT);
            CompoundTag state = machine.saveWithoutMetadata();
            state.putDouble("Progress", 158.25);
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
            check("actual completion debits one paid tick once", machine.energy().getStored() == 480);
            check("actual completion consumes the existing exact grouped bill once",
                    machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty() && machine.getItem(2).isEmpty());
            change(machine, reactor.id());
            state = machine.saveWithoutMetadata();
            state.putDouble("Progress", 12.875);
            machine.load(roundTrip(state));
            CompoundTag stores = assemblerStores(machine);
            for (int reopen = 0; reopen < 40; reopen++) {
                check("switching back to a fractional owner remains available " + reopen,
                        change(machine, washer.id()) == SelectionResult.SELECTED);
                exact("the real BE restores Washer carry exactly " + reopen, 0.25, machine.inspectionProgress());
                check("fractional carry never becomes another recipe's work " + reopen,
                        change(machine, reactor.id()) == SelectionResult.SELECTED);
                exact("the real BE restores Reactor partial work exactly " + reopen, 12.875, machine.inspectionProgress());
                machine.load(roundTrip(machine.saveWithoutMetadata()));
                check("switch/reopen keeps inventory, fluids, energy, configuration and layout " + reopen,
                        assemblerStores(machine).equals(stores));
                check("switch/reopen preserves the actual exposed store objects " + reopen,
                        machine.menuContainer() == inventory && machine.energy() == energy
                                && machine.nativeStore() == fluids && machine.containerConfig() == config);
            }
            change(machine, washer.id());
            supplyAssembler(machine);
            machine.setItem(4, new ItemStack(Items.STONE, 64));
            state = machine.saveWithoutMetadata();
            state.putDouble("Progress", 160.375);
            machine.load(roundTrip(state));
            long fuel = machine.energy().getStored();
            for (long tick = 2; tick <= 5; tick++) {
                tick(machine, tick);
            }
            exact("completed blocked work is never redrawn or discarded", 160.375, machine.inspectionProgress());
            check("completed blocked work never spends another FU", machine.energy().getStored() == fuel);
            check("the server-owned selected diagnosis names blocked completion",
                    machine.recipeSelection().orElseThrow().issue() == SelectionIssue.OUTPUT_BLOCKED);
            change(machine, reactor.id());
            change(machine, washer.id());
            exact("blocked completion also survives switching", 160.375, machine.inspectionProgress());
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
                    check(lock + " refuses a reentrant selector", change(machine, reactor.id()) == SelectionResult.BUSY);
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
            graph.set(null, index.invoke(null, List.of(reactor)));
            check("reload actually changes the indexed publication", generation != ProcessLookup.generation());
            check("removed explicit IDs wait rather than executing the remaining matching bill",
                    resolve(machine) == null && machine.recipeSelection().orElseThrow().issue() == SelectionIssue.UNAVAILABLE);
            tick(machine, 8);
            check("an unavailable explicit ID survives actual binary save/reopen", machine.saveWithoutMetadata()
                    .getString(AssemblerWork.TARGET).equals(washer.id()));
            exact("an unavailable explicit ID retains its exact owned work", 0.375, machine.inspectionProgress());
            check("unavailable targeting never spends power", machine.energy().getStored() == fuel);
            graph.set(null, index.invoke(null, List.of(washer, reactor)));
            check("returning IDs re-resolve to the current recipe, not the missing-generation cache",
                    resolve(machine) == washer);
            check("AUTO can be restored without discarding fractional work", change(machine, "") == SelectionResult.SELECTED);
            check("AUTO keeps last-dry behavior when only partial old work exists", resolve(machine) == reactor);
            change(machine, washer.id());
            exact("AUTO did not transfer or reset the suspended fractional balance", 0.375, machine.inspectionProgress());

            state = machine.saveWithoutMetadata();
            state.putString("WorkRecipe", "removed/paid_owner");
            state.putDouble("Progress", 5.125);
            state.remove(AssemblerWork.SUSPENDED);
            machine.load(roundTrip(state));
            check("unknown paid work cannot pay the selected known target", resolve(machine) == washer);
            exact("a known target starts with no borrowed unknown work", 0.0, machine.inspectionProgress());
            check("unknown paid work remains unavailable but recoverably stored",
                    machine.recipeSelection().orElseThrow().suspended().get("removed/paid_owner") == 5.125);

            List<IngredientSpec> wetBill = new ArrayList<>(bill);
            wetBill.add(IngredientSpec.fluid("minecraft:water", 250));
            ProcessRecipe wet = new ProcessRecipe("check/selected/wet", MachineFamily.ASSEMBLER, wetBill,
                    washer.outputs(), Double.NaN, null, 20, 32);
            ProcessRecipe hot = new ProcessRecipe("check/selected/hot", MachineFamily.ASSEMBLER, bill,
                    reactor.outputs(), 1500, null, 20, 32);
            graph.set(null, index.invoke(null, List.of(washer, reactor, wet, hot)));
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

            change(machine, washer.id());
            supplyAssembler(machine);
            machine.setItem(4, ItemStack.EMPTY);
            state = machine.saveWithoutMetadata();
            state.putDouble("Progress", 160.625);
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
                complete.invoke(machine, washer, List.of());
                check("the authored observer must abort actual settlement", false);
            } catch (InvocationTargetException expected) {
                check("actual completion exposes its original settlement failure",
                        expected.getCause() instanceof IllegalStateException
                                && expected.getCause().getMessage().equals("authored Assembler settlement observer"));
            }
            check("rollback preserves exact active/suspended paid work, target and every saved store",
                    machine.saveWithoutMetadata().equals(settlementBefore));
            check("a refused/throwing settlement releases its selector guard",
                    change(machine, reactor.id()) == SelectionResult.SELECTED);
            assemblerLegacyInventory(washer, reactor);
        } finally {
            graph.set(null, originalGraph);
        }
    }

    private static void assemblerLegacyInventory(ProcessRecipe washer, ProcessRecipe reactor) throws Exception {
        ProcessMachineBlockEntity legacy = assembler();
        supplyAssembler(legacy);
        CompoundTag tag = legacy.saveWithoutMetadata();
        tag.putString("Recipe", washer.id());
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
                legacy.saveWithoutMetadata().getString("WorkRecipe").equals(washer.id())
                        && legacy.inspectionProgress() == 11.34375);
        check("legacy AUTO preserves last-dry selection without giving it someone else's work",
                resolve(legacy) == reactor && legacy.inspectionProgress() == 0.0);
        change(legacy, washer.id());
        exact("the legacy partial balance remains reachable after AUTO chooses another row", 11.34375,
                legacy.inspectionProgress());
        check("migration and selection never mutate the old output's count or slot",
                legacy.getItem(3).isEmpty() && legacy.getItem(4).is(Items.BRICK) && legacy.getItem(4).getCount() == 2);
    }

    private static CompoundTag assemblerStores(ProcessMachineBlockEntity machine) {
        CompoundTag stores = machine.saveWithoutMetadata();
        for (String key : List.of("Progress", "Recipe", "WorkRecipe", AssemblerWork.TARGET, AssemblerWork.SUSPENDED)) {
            stores.remove(key);
        }
        return stores;
    }

    private static ProcessMachineBlockEntity assembler() throws Exception {
        Constructor<ProcessMachineBlockEntity> constructor = ProcessMachineBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, ProcessMachineKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, ProcessMachineKind.ASSEMBLER, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    }

    private static void supplyAssembler(ProcessMachineBlockEntity machine) {
        machine.setItem(0, new ItemStack(Items.QUARTZ));
        machine.setItem(1, new ItemStack(Items.COPPER_INGOT, 2));
        machine.setItem(2, new ItemStack(Items.IRON_INGOT, 4));
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
        IngredientSpec millMatrix = ProcessGraph.generate(List.of()).stream()
                .filter(recipe -> recipe.id().equals("assemble/wire_mill")).findFirst().orElseThrow()
                .itemInputs().get(3);
        check("the live base mill's fourth group declares minimum-F1 control",
                millMatrix.equals(IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1)));
        check("valid decoded F2 supplies the minimum control contract (NBT/data, not registered item proof)",
                millMatrix.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decoded));
        check("untagged legacy F1 agrees with the base mill's declared rating (NBT/data contract)",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating().ordinal()
                        == millMatrix.matrixRating());
        CompoundTag explicitF1 = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F1));
        check("explicit F1 agrees with the base mill's declared rating (NBT/data contract)",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(explicitF1))
                        .orElseThrow().rating().ordinal() == millMatrix.matrixRating());
        CompoundTag explicitF3 = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F3));
        ControlMatrixSpec decodedF3 = ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(explicitF3))
                .orElseThrow();
        check("valid decoded F3 supplies the minimum control contract (NBT/data, not registered item proof)",
                decodedF3.rating() == FluxTier.F3
                        && millMatrix.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decodedF3));
        IngredientSpec predecessor = IngredientSpec.matrix(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1);
        check("the same valid F2 cannot serve as its own exact predecessor",
                !predecessor.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decoded));
        check("a missing Matrix stack fails the real ingredient matcher",
                !ProcessLookup.matches(millMatrix, ItemStack.EMPTY));
        ItemStack wrongKind = new ItemStack(Items.PAPER);
        wrongKind.setTag(explicitF1.copy());
        check("an F1 tag on a non-Matrix item fails the real ingredient matcher",
                !ProcessLookup.matches(millMatrix, wrongKind));
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
