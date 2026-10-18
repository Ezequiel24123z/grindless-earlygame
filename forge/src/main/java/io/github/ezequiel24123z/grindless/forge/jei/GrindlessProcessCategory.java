package io.github.ezequiel24123z.grindless.forge.jei;

import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.MachineMenuLayout;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One JEI page per live machine family, with tag alternatives and process conditions intact. */
final class GrindlessProcessCategory implements IRecipeCategory<ProcessRecipe> {

    private static final int WIDTH = 180;
    private static final int FLUID_Y = 76;
    private static final int FLUID_HEIGHT = 28;

    private final MachineFamily family;
    private final MachineMenuKind menuKind;
    private final RecipeType<ProcessRecipe> type;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable slotBackground;

    GrindlessProcessCategory(MachineFamily family, IGuiHelper gui) {
        this.family = family;
        this.menuKind = MachineMenuKind.valueOf(family.name());
        this.type = GrindlessJeiPlugin.type(family);
        this.icon = gui.createDrawableItemLike(GrindlessJeiPlugin.catalyst(family));
        this.arrow = gui.getRecipeArrow();
        this.slotBackground = gui.getSlotDrawable();
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
        var font = Minecraft.getInstance().font;
        int lines = recipes().stream().mapToInt(recipe -> details(recipe).stream()
                .mapToInt(detail -> font.split(detail, WIDTH - 8).size()).sum()).max().orElse(1);
        return detailsY(fluidInputs(), fluidOutputs()) + lines * font.lineHeight + 4;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessRecipe recipe, IFocusGroup focuses) {
        int fluidInputs = Math.max(fluidInputs(), recipe.fluidInputs().size());
        int fluidOutputs = Math.max(fluidOutputs(), storedOutputs(recipe).size());
        if (fluidInputs > 2 || fluidOutputs > 1) {
            throw new IllegalStateException(family + " exceeds the authored primary/adjacent fluid layout");
        }
        List<IngredientSpec> inputs = new ArrayList<>(recipe.itemInputs());
        inputs.addAll(recipe.catalysts());
        List<OutputSpec> outputs = recipe.itemOutputs();
        if (inputs.size() > menuKind.inputs() || outputs.size() > menuKind.outputs()) {
            throw new IllegalArgumentException(recipe.id() + " exceeds its physical machine item inventory");
        }
        for (MachineMenuLayout.ItemSlot position : MachineMenuLayout.itemSlots(menuKind)) {
            if (position.output()) {
                int index = position.index() - menuKind.inputs();
                var slot = builder.addOutputSlot(position.x(), position.y()).setSlotName("item_output_" + index);
                if (index < outputs.size()) {
                    ProcessLookup.resolve(outputs.get(index)).ifPresent(slot::addItemStack);
                }
            } else {
                int index = position.index();
                RecipeIngredientRole role = index < inputs.size() && index >= recipe.itemInputs().size()
                        ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT;
                var slot = builder.addSlot(role, position.x(), position.y()).setSlotName("item_input_" + index);
                if (role == RecipeIngredientRole.CATALYST) {
                    slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(
                            Component.translatable("jei.grindless.not_consumed").withStyle(ChatFormatting.RED)));
                }
                if (index < inputs.size()) {
                    addInput(slot, inputs.get(index));
                }
            }
        }
        List<IngredientSpec> incoming = recipe.fluidInputs();
        for (int index = 0; index < fluidInputs; index++) {
            var slot = builder.addInputSlot(fluidX(false, index, fluidInputs), FLUID_Y)
                    .setSlotName("fluid_input_" + index);
            if (index < incoming.size()) {
                IngredientSpec spec = incoming.get(index);
                addFluid(slot, spec.id(), spec.count(), index == 0 ? FluidLogic.MACHINE_CAPACITY : spec.count(),
                        index == 0);
                if (index > 0) {
                    slot.addRichTooltipCallback((view, tooltip) ->
                            tooltip.add(Component.translatable("jei.grindless.adjacent_fluid")));
                }
                if (family == MachineFamily.SETTLING_TROUGH) {
                    slot.addRichTooltipCallback((view, tooltip) ->
                            tooltip.add(Component.translatable("jei.grindless.shared_tank")));
                }
            }
        }
        List<OutputSpec> outgoing = storedOutputs(recipe);
        for (int index = 0; index < fluidOutputs; index++) {
            var slot = builder.addOutputSlot(fluidX(true, index, fluidOutputs), FLUID_Y)
                    .setSlotName("fluid_output_" + index);
            if (index < outgoing.size()) {
                OutputSpec spec = outgoing.get(index);
                addFluid(slot, spec.id(), spec.count(), FluidLogic.MACHINE_CAPACITY, true);
                if (family == MachineFamily.SETTLING_TROUGH) {
                    slot.addRichTooltipCallback((view, tooltip) ->
                            tooltip.add(Component.translatable("jei.grindless.shared_tank")));
                }
            }
        }
    }

    @Override
    public void draw(ProcessRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        int fluidInputs = Math.max(fluidInputs(), recipe.fluidInputs().size());
        int fluidOutputs = Math.max(fluidOutputs(), storedOutputs(recipe).size());
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.grindless.inputs"), 28, 8, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.grindless.outputs"), 107, 8, 0x404040, false);
        for (MachineMenuLayout.ItemSlot slot : MachineMenuLayout.itemSlots(menuKind)) {
            slotBackground.draw(graphics, slot.x() - 1, slot.y() - 1);
        }
        arrow.draw(graphics, MachineMenuLayout.progressX(menuKind), 35);
        List<IngredientSpec> incoming = recipe.fluidInputs();
        for (int index = 0; index < fluidInputs; index++) {
            IngredientSpec spec = index < incoming.size() ? incoming.get(index) : null;
            drawFluidCell(graphics, fluidX(false, index, fluidInputs),
                    spec == null ? null : spec.id(), spec == null ? 0 : spec.count());
        }
        List<OutputSpec> outgoing = storedOutputs(recipe);
        for (int index = 0; index < fluidOutputs; index++) {
            OutputSpec spec = index < outgoing.size() ? outgoing.get(index) : null;
            drawFluidCell(graphics, fluidX(true, index, fluidOutputs),
                    spec == null ? null : spec.id(), spec == null ? 0 : spec.count());
        }
        int y = detailsY(fluidInputs, fluidOutputs);
        for (Component detail : details(recipe)) {
            for (var line : font.split(detail, WIDTH - 8)) {
                graphics.drawString(font, line, 4, y, 0x404040, false);
                y += font.lineHeight;
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ProcessRecipe recipe, IRecipeSlotsView slots,
                           double mouseX, double mouseY) {
        List<IngredientSpec> incoming = recipe.fluidInputs();
        int inputCount = Math.max(fluidInputs(), incoming.size());
        for (int index = 0; index < incoming.size(); index++) {
            IngredientSpec spec = incoming.get(index);
            addFluidDiagnostic(tooltip, spec.id(), spec.count(), false, index, inputCount, mouseX, mouseY);
        }
        List<OutputSpec> outgoing = storedOutputs(recipe);
        int outputCount = Math.max(fluidOutputs(), outgoing.size());
        for (int index = 0; index < outgoing.size(); index++) {
            OutputSpec spec = outgoing.get(index);
            addFluidDiagnostic(tooltip, spec.id(), spec.count(), true, index, outputCount, mouseX, mouseY);
        }
    }

    private static void addFluidDiagnostic(ITooltipBuilder tooltip, String id, int amount,
                                           boolean output, int index, int count,
                                           double mouseX, double mouseY) {
        int x = fluidX(output, index, count);
        if (mouseX < x - 1 || mouseX >= x + 17 || mouseY < FLUID_Y - 1
                || mouseY >= FLUID_Y + FLUID_HEIGHT + 1 || fluid(id, amount).isPresent()) {
            return;
        }
        tooltip.add(Component.translatable(output ? "jei.grindless.state_fluid_output"
                : "jei.grindless.state_fluid_input", id, amount));
        tooltip.add(Component.translatable("jei.grindless.fluid_unavailable").withStyle(ChatFormatting.RED));
        if (!output && index > 0) {
            tooltip.add(Component.translatable("jei.grindless.adjacent_fluid"));
        }
    }

    private static void addInput(IRecipeSlotBuilder slot, IngredientSpec spec) {
        if (IngredientSpec.ITEM.equals(spec.kind())) {
            ProcessLookup.resolve(new OutputSpec(spec.kind(), spec.id(), spec.count(), false,
                    spec.matrixRating())).ifPresent(slot::addItemStack);
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
                slot.addItemStacks(stacks);
            }
        }
    }

    private static Optional<FluidStack> fluid(String id, int amount) {
        Fluid registered = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(id));
        return registered == null || registered == Fluids.EMPTY
                ? Optional.empty() : Optional.of(new FluidStack(registered, amount));
    }

    private static void addFluid(IRecipeSlotBuilder slot, String id, int amount, int capacity,
                                 boolean showCapacity) {
        fluid(id, amount).ifPresent(stack -> slot.setFluidRenderer(capacity, showCapacity, 16, FLUID_HEIGHT)
                .addIngredient(ForgeTypes.FLUID_STACK, stack));
    }

    private static List<OutputSpec> storedOutputs(ProcessRecipe recipe) {
        return recipe.fluidOutputs().stream().filter(output -> !output.vented()).toList();
    }

    private static int fluidX(boolean output, int index, int count) {
        return output ? 116 + index * 40 : 44 + (2 * index - count + 1) * 20;
    }

    private List<ProcessRecipe> recipes() {
        return ProcessLookup.recipes().stream().filter(recipe -> recipe.family() == family).toList();
    }

    private int fluidInputs() {
        return recipes().stream().mapToInt(recipe -> recipe.fluidInputs().size()).max().orElse(0);
    }

    private int fluidOutputs() {
        return recipes().stream().mapToInt(recipe -> storedOutputs(recipe).size()).max().orElse(0);
    }

    private static int detailsY(int fluidInputs, int fluidOutputs) {
        return fluidInputs > 0 || fluidOutputs > 0 ? 122 : 68;
    }

    private static void drawFluidCell(GuiGraphics graphics, int x, String id, int amount) {
        graphics.fill(x - 1, FLUID_Y - 1, x + 17, FLUID_Y + FLUID_HEIGHT + 1, 0xFF373737);
        graphics.fill(x, FLUID_Y, x + 17, FLUID_Y + FLUID_HEIGHT + 1, 0xFFFFFFFF);
        graphics.fill(x, FLUID_Y, x + 16, FLUID_Y + FLUID_HEIGHT, 0xFF8B8B8B);
        if (id != null && fluid(id, amount).isEmpty()) {
            graphics.drawString(Minecraft.getInstance().font, "?", x + 5, FLUID_Y + 9, 0xAA0000, false);
        }
        if (amount > 0) {
            var font = Minecraft.getInstance().font;
            Component label = Component.translatable("jei.grindless.amount", amount);
            graphics.drawString(font, label, x + 8 - font.width(label) / 2,
                    FLUID_Y + FLUID_HEIGHT + 3, 0x404040, false);
        }
    }

    private static List<Component> details(ProcessRecipe recipe) {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("jei.grindless.energy_time", recipe.fuPerTick(),
                recipe.durationTicks() / 20.0));
        for (IngredientSpec spec : recipe.fluidInputs()) {
            if (fluid(spec.id(), spec.count()).isEmpty()) {
                details.add(Component.translatable("jei.grindless.state_fluid_input", spec.id(), spec.count()));
            }
        }
        for (OutputSpec spec : recipe.fluidOutputs()) {
            if (spec.vented()) {
                details.add(Component.translatable("jei.grindless.fluid_vent", spec.id(), spec.count()));
            } else if (fluid(spec.id(), spec.count()).isEmpty()) {
                details.add(Component.translatable("jei.grindless.state_fluid_output", spec.id(), spec.count()));
            }
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
        switch (recipe.family()) {
            case FIRING_CHAMBER -> details.add(Component.translatable(
                    WorkshopStructure.Fault.MISSING_BRICKS.translationKey()));
            case SETTLING_TROUGH -> details.add(Component.translatable("jei.grindless.settling_structure"));
            case GEOLOGICAL_COLLECTOR -> {
                details.add(Component.translatable(SourceSite.Fault.WRONG_DIMENSION.translationKey()));
                details.add(Component.translatable(SourceSite.Fault.NEEDS_ROCK_BED.translationKey()));
            }
            case BIOCULTIVATOR -> {
                details.add(Component.translatable(SourceSite.Fault.WRONG_DIMENSION.translationKey()));
                details.add(Component.translatable(SourceSite.Fault.NEEDS_OPEN_SKY.translationKey()));
                details.add(Component.translatable(SourceSite.Fault.NEEDS_DAYLIGHT.translationKey()));
            }
            default -> {
            }
        }
        return details;
    }
}
