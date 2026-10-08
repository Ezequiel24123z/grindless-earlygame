package io.github.ezequiel24123z.grindless.forge.jei;

import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/** One JEI page per live machine family, with tag alternatives and process conditions intact. */
final class GrindlessProcessCategory implements IRecipeCategory<ProcessRecipe> {

    private static final int WIDTH = 162;
    private static final int HEIGHT = 76;

    private final MachineFamily family;
    private final RecipeType<ProcessRecipe> type;
    private final IDrawable icon;
    private final IDrawable arrow;

    GrindlessProcessCategory(MachineFamily family, IGuiHelper gui) {
        this.family = family;
        this.type = GrindlessJeiPlugin.type(family);
        this.icon = gui.createDrawableItemLike(GrindlessJeiPlugin.catalyst(family));
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<ProcessRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.grindless.process",
                GrindlessJeiPlugin.catalyst(family).asItem().getDescription());
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
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessRecipe recipe, IFocusGroup focuses) {
        int input = 0;
        for (IngredientSpec spec : recipe.itemInputs()) {
            addInput(builder, spec, input++);
        }
        for (IngredientSpec catalyst : recipe.catalysts()) {
            addInput(builder, catalyst, input++);
        }
        int output = 0;
        for (OutputSpec spec : recipe.itemOutputs()) {
            java.util.Optional<ItemStack> resolved = ProcessLookup.resolve(spec);
            if (resolved.isPresent()) {
                builder.addOutputSlot(118 + (output++ * 20), 20)
                        .setOutputSlotBackground().addItemStack(resolved.get());
            }
        }
    }

    @Override
    public void draw(ProcessRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 84, 22);
        List<Component> details = details(recipe);
        for (int index = 0; index < details.size(); index++) {
            graphics.drawString(Minecraft.getInstance().font, details.get(index), 4, 43 + (index * 10),
                    0x404040, false);
        }
    }

    private static void addInput(IRecipeLayoutBuilder builder, IngredientSpec spec, int index) {
        int x = 4 + ((index % 4) * 20);
        int y = 4 + ((index / 4) * 20);
        if (IngredientSpec.ITEM.equals(spec.kind())) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.id()));
            if (item != null) {
                builder.addInputSlot(x, y).setStandardSlotBackground()
                        .addItemStack(new ItemStack(item, spec.count()));
            }
            return;
        }
        if (IngredientSpec.TAG.equals(spec.kind())) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(spec.id()));
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : Ingredient.of(tag).getItems()) {
                ItemStack counted = stack.copy();
                counted.setCount(spec.count());
                stacks.add(counted);
            }
            if (!stacks.isEmpty()) {
                builder.addInputSlot(x, y).setStandardSlotBackground().addItemStacks(stacks);
            }
        }
    }

    private static List<Component> details(ProcessRecipe recipe) {
        List<Component> details = new ArrayList<>();
        for (IngredientSpec fluid : recipe.fluidInputs()) {
            details.add(Component.translatable("jei.grindless.fluid_input", fluid.id(), fluid.count()));
        }
        for (OutputSpec fluid : recipe.fluidOutputs()) {
            String key = fluid.vented() ? "jei.grindless.fluid_vent" : "jei.grindless.fluid_output";
            details.add(Component.translatable(key, fluid.id(), fluid.count()));
        }
        if (recipe.namesTemperature()) {
            details.add(Component.translatable("jei.grindless.temperature", Math.round(recipe.temperatureC())));
        }
        if (recipe.namesAtmosphere()) {
            details.add(Component.translatable("jei.grindless.atmosphere", recipe.atmosphere()));
        }
        if (recipe.namesAgitation()) {
            details.add(Component.translatable("jei.grindless.agitation", recipe.agitation()));
        }
        details.add(Component.translatable("jei.grindless.energy_time", recipe.fuPerTick(),
                recipe.durationTicks() / 20.0));
        return details.subList(0, Math.min(details.size(), 3));
    }
}
