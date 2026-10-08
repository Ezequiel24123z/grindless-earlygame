package io.github.ezequiel24123z.grindless.forge.jei;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.List;

/**
 * Optional JEI bridge for the live generated process graph.
 *
 * <p>The graph is the single recipe source: no JSON copy or hand-maintained JEI list is allowed.
 * This class lives in the Forge source set and is discovered only when JEI is installed, so a
 * standalone Grindless jar keeps no runtime dependency on a recipe viewer.
 */
@JeiPlugin
public final class GrindlessJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Grindless.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(Arrays.stream(MachineFamily.values())
                .map(family -> new GrindlessProcessCategory(family,
                        registration.getJeiHelpers().getGuiHelper()))
                .toArray(GrindlessProcessCategory[]::new));
        registration.addRecipeCategories(new GrindlessCalibrationCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (MachineFamily family : MachineFamily.values()) {
            List<ProcessRecipe> recipes = ProcessLookup.recipes().stream()
                    .filter(recipe -> recipe.family() == family)
                    .toList();
            registration.addRecipes(type(family), recipes);
        }
        registration.addRecipes(calibrationType(), List.of(new CalibrationJeiRecipe()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineFamily family : MachineFamily.values()) {
            registration.addRecipeCatalyst(catalyst(family), type(family));
        }
        registration.addRecipeCatalyst(ModBlocks.RESEARCH_TERMINAL.get(), calibrationType());
    }

    static RecipeType<ProcessRecipe> type(MachineFamily family) {
        return RecipeType.create(Grindless.MOD_ID, "process/" + family.name().toLowerCase(),
                ProcessRecipe.class);
    }

    static RecipeType<CalibrationJeiRecipe> calibrationType() {
        return RecipeType.create(Grindless.MOD_ID, "calibration", CalibrationJeiRecipe.class);
    }

    static ItemLike catalyst(MachineFamily family) {
        return switch (family) {
            case PULVERIZER -> ModBlocks.PULVERIZER.get();
            case ARC_FURNACE -> ModBlocks.ARC_FURNACE.get();
            case PRESS -> ModBlocks.PRESS.get();
            case ASSEMBLER -> ModBlocks.ASSEMBLER.get();
            case KILN -> ModBlocks.KILN.get();
            case WIRE_MILL -> ModBlocks.WIRE_MILL.get();
            case CHEMICAL_REACTOR -> ModBlocks.CHEMICAL_REACTOR.get();
            case CHEMICAL_WASHER -> ModBlocks.CHEMICAL_WASHER.get();
            case ELECTROLYSIS_CELL -> ModBlocks.ELECTROLYSIS_CELL.get();
            case ATMOSPHERIC_INTAKE -> ModBlocks.ATMOSPHERIC_INTAKE.get();
            case INDUCTION_FURNACE -> ModBlocks.INDUCTION_FURNACE.get();
            case CASTER -> ModBlocks.CASTER.get();
            case FLOTATION -> ModBlocks.FLOTATION_CELL.get();
            case BOILER -> ModBlocks.BOILER.get();
            case CONDENSER -> ModBlocks.CONDENSER.get();
        };
    }
}
