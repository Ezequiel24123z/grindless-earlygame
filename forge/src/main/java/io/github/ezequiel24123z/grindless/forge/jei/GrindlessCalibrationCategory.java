package io.github.ezequiel24123z.grindless.forge.jei;

import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.research.ResearchLogic;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** JEI page for the non-JSON calibration cycle that starts the technology route. */
final class GrindlessCalibrationCategory implements IRecipeCategory<CalibrationJeiRecipe> {

    private static final int WIDTH = 132;
    private static final int HEIGHT = 48;

    private final IDrawable icon;
    private final IDrawable arrow;

    GrindlessCalibrationCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(ModBlocks.RESEARCH_TERMINAL.get());
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<CalibrationJeiRecipe> getRecipeType() {
        return GrindlessJeiPlugin.calibrationType();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.grindless.calibration");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CalibrationJeiRecipe recipe,
                          IFocusGroup focuses) {
        builder.addInputSlot(4, 4).setStandardSlotBackground()
                .addItemStack(ModItems.DATA_CORE.get().getDefaultInstance());
        builder.addOutputSlot(96, 4).setOutputSlotBackground()
                .addItemStack(ModItems.CALIBRATED_DATA_CORE.get().getDefaultInstance());
    }

    @Override
    public void draw(CalibrationJeiRecipe recipe,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        arrow.draw(graphics, 48, 4);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.grindless.energy_time", ResearchLogic.FU_PER_TICK,
                        ResearchLogic.CYCLE_TICKS / 20.0), 4, 30, 0x404040, false);
    }

}
