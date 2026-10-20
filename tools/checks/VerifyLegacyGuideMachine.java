package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.recipe.LegacyGuideAssembly;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/** Real process completion using an isolated item-ID fixture; not a claim about the Guide's client behavior. */
public final class VerifyLegacyGuideMachine {

    private static int checks;
    private static Item guide;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        ResourceLocation id = new ResourceLocation("grindless", "field_guide");
        guide = BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id)
                : Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties()));
        // Register the isolated native fixture before vanilla freezes intrusive holders.
        Bootstrap.bootStrap();
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        for (ProcessMachineKind kind : List.of(ProcessMachineKind.ASSEMBLY_BENCH, ProcessMachineKind.ASSEMBLER)) {
            String owner = kind == ProcessMachineKind.ASSEMBLY_BENCH
                    ? "bench/field_guide" : "assemble/bootstrap/field_guide";
            ProcessRecipe legacy = LegacyGuideAssembly.resume(kind.family(), owner, 1).orElseThrow();
            ProcessMachineBlockEntity partial = machine(kind, owner, legacy.durationTicks() / 2.0);
            supply(partial, 2);
            eq("partial paid legacy work still resolves its exact old row", owner, recipe(partial).id());
            eq("partial paid work retains its existing progress", legacy.durationTicks() / 2.0,
                    partial.inspectionProgress());

            ProcessMachineBlockEntity completed = machine(kind, owner, legacy.durationTicks() * 3.0 + 0.5);
            supply(completed, 4);
            ProcessRecipe resumed = recipe(completed);
            int output = kind.menuKind().inputs();
            for (int slot = output; slot < completed.getContainerSize(); slot++) {
                completed.setItem(slot, new ItemStack(Items.STONE, 64));
            }
            check("full output blocks the real old batch", !complete(completed, resumed));
            eq("blocked output does not consume paper", 4, completed.getItem(0).getCount());
            eq("blocked output retains all paid work", legacy.durationTicks() * 3.0 + 0.5,
                    completed.inspectionProgress());
            for (int slot = output; slot < completed.getContainerSize(); slot++) {
                completed.setItem(slot, ItemStack.EMPTY);
            }
            check("the repaired old job settles exactly once", complete(completed, resumed));
            eq("exactly one actual legacy item is delivered", 1, completed.getItem(output).getCount());
            check("the delivered item resolves the old stable ID", completed.getItem(output).is(guide));
            eq("one batch spends exactly two paper", 2, completed.getItem(0).getCount());
            eq("one batch spends exactly one stick", 1, completed.getItem(1).getCount());
            eq("successful migration clears even oversized paid progress", 0.0, completed.inspectionProgress());
            CompoundTag saved = completed.saveWithoutMetadata();
            eq("successful migration clears the saved work owner", "", saved.getString("WorkRecipe"));
            eq("successful migration clears the cached saved selection", "", saved.getString("Recipe"));
            ProcessRecipe next = recipe(completed);
            check("remaining ingredients cannot start another retired Guide",
                    next == null || !LegacyGuideAssembly.retired(kind.family(), next.id()));

            ProcessMachineBlockEntity fresh = machine(kind, owner, 0);
            supply(fresh, 2);
            ProcessRecipe unpaid = recipe(fresh);
            check("a nonpaid saved ID grants no new legacy start",
                    unpaid == null || !LegacyGuideAssembly.retired(kind.family(), unpaid.id()));
            ProcessMachineBlockEntity wrong = machine(kind, "not-the-old-owner", legacy.durationTicks());
            supply(wrong, 2);
            ProcessRecipe changedOwner = recipe(wrong);
            check("a different saved owner grants no legacy start",
                    changedOwner == null || !LegacyGuideAssembly.retired(kind.family(), changedOwner.id()));
            ProcessMachineBlockEntity missing = machine(kind, owner, legacy.durationTicks());
            missing.setItem(0, new ItemStack(Items.PAPER, 2));
            check("paid work never substitutes for missing physical inputs", recipe(missing) == null);
        }
        System.out.println("ALL LEGACY GUIDE MACHINE CHECKS PASSED (" + checks + ")");
    }

    private static void supply(ProcessMachineBlockEntity machine, int paper) {
        machine.setItem(0, new ItemStack(Items.PAPER, paper));
        machine.setItem(1, new ItemStack(Items.STICK, 2));
    }

    private static ProcessMachineBlockEntity machine(ProcessMachineKind kind, String owner, double progress)
            throws Exception {
        Constructor<ProcessMachineBlockEntity> constructor = ProcessMachineBlockEntity.class.getDeclaredConstructor(
                BlockEntityType.class, ProcessMachineKind.class, BlockPos.class, BlockState.class);
        constructor.setAccessible(true);
        ProcessMachineBlockEntity machine = constructor.newInstance(null, kind, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        CompoundTag tag = machine.saveWithoutMetadata();
        tag.putString("Recipe", owner);
        tag.putString("WorkRecipe", owner);
        tag.putDouble("Progress", progress);
        machine.load(tag);
        return machine;
    }

    private static ProcessRecipe recipe(ProcessMachineBlockEntity machine) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("recipe", List.class);
        method.setAccessible(true);
        return (ProcessRecipe) method.invoke(machine, List.<FluidInsert.Source>of());
    }

    private static boolean complete(ProcessMachineBlockEntity machine, ProcessRecipe recipe) throws Exception {
        Method method = ProcessMachineBlockEntity.class.getDeclaredMethod("complete", ProcessRecipe.class, List.class);
        method.setAccessible(true);
        return (boolean) method.invoke(machine, recipe, List.<FluidInsert.Source>of());
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
