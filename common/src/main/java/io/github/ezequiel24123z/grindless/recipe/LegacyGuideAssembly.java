package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.util.List;
import java.util.Optional;

/** One pending old Guide batch may finish; these recipes never enter the discoverable graph. */
public final class LegacyGuideAssembly {

    private LegacyGuideAssembly() {
    }

    public static boolean retired(MachineFamily family, String recipeId) {
        return family == MachineFamily.ASSEMBLY_BENCH && "bench/field_guide".equals(recipeId)
                || family == MachineFamily.ASSEMBLER && "assemble/bootstrap/field_guide".equals(recipeId);
    }

    /** The caller also verifies current inputs and clears this owner/progress after its one successful settlement. */
    public static Optional<ProcessRecipe> resume(MachineFamily family, String workRecipeId, double paidWork) {
        if (!Double.isFinite(paidWork) || paidWork <= 0.0 || !retired(family, workRecipeId)) {
            return Optional.empty();
        }
        boolean bench = family == MachineFamily.ASSEMBLY_BENCH;
        return Optional.of(new ProcessRecipe(workRecipeId, family,
                List.of(IngredientSpec.item("minecraft:paper", 2), IngredientSpec.item("minecraft:stick", 1)),
                List.of(OutputSpec.item("grindless:field_guide", 1)), Double.NaN, null,
                bench ? SourceLogic.BENCH_TICKS : SourceLogic.INDUSTRIAL_ASSEMBLY_TICKS,
                bench ? FluxTier.F0.nominal() : FluxTier.F1.nominal(), List.of(), bench ? "bootstrap" : null));
    }
}
