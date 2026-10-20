package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.FluidInsert;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.recipe.LegacyGuideAssembly;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/** Paid-owner selection with native vanilla inputs; registered Guide settlement belongs to WorkshopInterfaceChecks. */
public final class VerifyLegacyGuideMachine {

    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
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
            eq("the fallback retains the exact registered output ID", "grindless:field_guide",
                    legacy.itemOutputs().get(0).id());
            eq("the fallback declares one Guide, never a substitute item", 1, legacy.itemOutputs().get(0).count());

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
        System.out.println("ALL LEGACY GUIDE SELECTION CHECKS PASSED (" + checks
                + "); actual registered one-time settlement remains required in workshop-ports native probe.");
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
