package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.menu.MachineMenuLayout;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.source.SourceSite;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * One screen for every process machine.
 *
 * <p>Machine I/O, a wrapped status region and player inventory have separate space. Slot
 * backgrounds follow the actual menu slots, including empty machine and player slots.
 */
public final class ProcessMachineScreen extends AbstractContainerScreen<ProcessMachineMenu> {

    private static final ResourceLocation TEXTURE = Grindless.id("textures/gui/process_machine.png");
    private boolean fluidAppearanceUnavailable;
    private final List<Button> recipeButtons = new ArrayList<>();
    private List<PickerEntry> filteredRecipes = List.of();
    private ProcessMachineMenu.SelectionView shownSelection;
    private EditBox recipeSearch;
    private Button auto;
    private Button previous;
    private Button next;
    private String recipeQuery = "";
    private int recipePage;

    private record PickerEntry(String id, ItemStack output, boolean available, double paid) {
        Component name() {
            if (!available) {
                return Component.translatable("gui.grindless.assembler.unavailable",
                        id.isEmpty() ? Component.translatable("gui.grindless.assembler.unattributed") : id);
            }
            return output.isEmpty() ? Component.literal(id) : output.getHoverName();
        }
    }

    public ProcessMachineScreen(ProcessMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MachineMenuLayout.WIDTH;
        this.imageHeight = MachineMenuLayout.HEIGHT;
        this.inventoryLabelY = MachineMenuLayout.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.kind() != MachineMenuKind.ASSEMBLER) {
            return;
        }
        leftPos = (width - MachineMenuLayout.screenWidth(menu.kind())) / 2;
        int x = leftPos + MachineMenuLayout.ASSEMBLER_PICKER_X;
        recipeButtons.clear();
        recipeSearch = addRenderableWidget(new EditBox(font, x + 6, topPos + 20, 120, 18,
                Component.translatable("gui.grindless.assembler.search")));
        recipeSearch.setMaxLength(128);
        recipeSearch.setHint(Component.translatable("gui.grindless.assembler.search"));
        recipeSearch.setValue(recipeQuery);
        recipeSearch.setResponder(query -> {
            recipeQuery = query;
            recipePage = 0;
            updatePicker();
        });
        auto = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.assembler.auto"),
                button -> choose("")).bounds(x + 6, topPos + 42, 120, 20).build());
        auto.setTooltip(Tooltip.create(Component.translatable("gui.grindless.assembler.auto_help")));
        for (int row = 0; row < MachineMenuLayout.ASSEMBLER_PICKER_ROWS; row++) {
            int offset = row;
            recipeButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                int index = recipePage * MachineMenuLayout.ASSEMBLER_PICKER_ROWS + offset;
                if (index < filteredRecipes.size()) {
                    choose(filteredRecipes.get(index).id());
                }
            }).bounds(x + 26, topPos + MachineMenuLayout.ASSEMBLER_PICKER_ROW_Y
                    + row * MachineMenuLayout.ASSEMBLER_PICKER_ROW_HEIGHT, 100, 18).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.assembler.previous"),
                button -> changeRecipePage(-1)).bounds(x + 6, topPos + 158, 58, 20).build());
        next = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.assembler.next"),
                button -> changeRecipePage(1)).bounds(x + 68, topPos + 158, 58, 20).build());
        setInitialFocus(recipeSearch);
        updatePicker();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (recipeSearch != null) {
            recipeSearch.tick();
            updatePicker();
        }
    }

    private void choose(String id) {
        if (shownSelection != null) {
            menu.requestSelection(shownSelection, id);
            updatePicker();
        }
    }

    private void updatePicker() {
        if (auto == null) {
            return;
        }
        shownSelection = menu.selection().orElse(null);
        List<PickerEntry> entries = new ArrayList<>();
        if (shownSelection != null) {
            MachineMenuHost.RecipeSelection state = shownSelection.state();
            HashSet<String> ids = new HashSet<>();
            for (ProcessMachineMenu.RecipeChoice choice : shownSelection.choices()) {
                ids.add(choice.id());
                entries.add(new PickerEntry(choice.id(), choice.output(), true,
                        state.activeRecipe().equals(choice.id()) ? state.activeWork()
                                : state.suspended().getOrDefault(choice.id(), 0.0)));
            }
            if (!state.selected().isEmpty() && ids.add(state.selected())) {
                entries.add(new PickerEntry(state.selected(), ItemStack.EMPTY, false,
                        state.activeRecipe().equals(state.selected()) ? state.activeWork()
                                : state.suspended().getOrDefault(state.selected(), 0.0)));
            }
            if (state.activeWork() > 0.0 && ids.add(state.activeRecipe())) {
                entries.add(new PickerEntry(state.activeRecipe(), ItemStack.EMPTY, false, state.activeWork()));
            }
            state.suspended().forEach((id, paid) -> {
                if (ids.add(id)) {
                    entries.add(new PickerEntry(id, ItemStack.EMPTY, false, paid));
                }
            });
        }
        String query = recipeQuery.strip().toLowerCase(Locale.ROOT);
        filteredRecipes = entries.stream().filter(entry -> query.isEmpty()
                || entry.id().toLowerCase(Locale.ROOT).contains(query)
                || entry.name().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        int rows = MachineMenuLayout.ASSEMBLER_PICKER_ROWS;
        recipePage = Math.min(recipePage, Math.max(0, (filteredRecipes.size() - 1) / rows));
        boolean enabled = shownSelection != null && !menu.selectionPending()
                && shownSelection.state().issue() != MachineMenuHost.SelectionIssue.INVALID_WORK;
        auto.active = enabled;
        auto.setMessage(Component.translatable(menu.selectionPending() ? "gui.grindless.assembler.waiting"
                : shownSelection != null && shownSelection.state().selected().isEmpty()
                        ? "gui.grindless.assembler.auto_selected" : "gui.grindless.assembler.auto"));
        for (int row = 0; row < recipeButtons.size(); row++) {
            Button button = recipeButtons.get(row);
            int index = recipePage * rows + row;
            button.visible = index < filteredRecipes.size();
            button.active = enabled && button.visible && filteredRecipes.get(index).available();
            if (button.visible) {
                PickerEntry entry = filteredRecipes.get(index);
                boolean selected = shownSelection != null && shownSelection.state().selected().equals(entry.id());
                button.setMessage(Component.literal(selected ? "> " : "").append(entry.name()));
                button.setTooltip(Tooltip.create(Component.empty().append(entry.name()).append("\n")
                        .append(entry.id().isEmpty()
                                ? Component.translatable("gui.grindless.assembler.unattributed")
                                : Component.literal(entry.id()))
                        .append("\n").append(Component.translatable("gui.grindless.assembler.held_work",
                                Double.toString(entry.paid())))));
            } else if (button.isFocused()) {
                button.setFocused(false);
                setFocused(recipeSearch);
                recipeSearch.setFocused(true);
            }
        }
        previous.active = recipePage > 0;
        next.active = (recipePage + 1) * rows < filteredRecipes.size();
    }

    private void changeRecipePage(int delta) {
        int last = Math.max(0, (filteredRecipes.size() - 1) / MachineMenuLayout.ASSEMBLER_PICKER_ROWS);
        recipePage = Math.max(0, Math.min(last, recipePage + delta));
        updatePicker();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (recipeSearch != null) {
            if (!recipeSearch.isFocused() && (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN)) {
                changeRecipePage(key == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1);
                return true;
            }
            if (recipeSearch.isFocused() && key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_TAB) {
                recipeSearch.keyPressed(key, scanCode, modifiers);
                if (key == GLFW.GLFW_KEY_ENTER && !recipeButtons.isEmpty() && recipeButtons.get(0).visible) {
                    recipeSearch.setFocused(false);
                    setFocused(recipeButtons.get(0));
                    recipeButtons.get(0).setFocused(true);
                }
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return recipeSearch != null && recipeSearch.isFocused() && recipeSearch.charTyped(character, modifiers)
                || super.charTyped(character, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (recipeSearch != null && mouseX >= leftPos + MachineMenuLayout.ASSEMBLER_PICKER_X
                && mouseX < leftPos + MachineMenuLayout.screenWidth(menu.kind())
                && mouseY >= topPos + MachineMenuLayout.ASSEMBLER_PICKER_ROW_Y
                && mouseY < topPos + 178 && delta != 0) {
            changeRecipePage(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        if (menu.kind() == MachineMenuKind.ASSEMBLER) {
            return mouseX < guiLeft || mouseX >= guiLeft + MachineMenuLayout.screenWidth(menu.kind())
                    || mouseY < guiTop || mouseY >= guiTop + imageHeight;
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        fluidAppearanceUnavailable = false;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }
        MachineMenuLayout.waterTank(menu.kind()).ifPresent(tank -> {
            drawRecessed(graphics, x + tank.x(), y + tank.y(), tank.width(), tank.height());
            menu.primaryFluidView().ifPresent(fluid -> {
                int fill = fluid.fillHeight(tank.height() - 2);
                if (fill > 0 && !fluid.id().map(id -> FluidClientPlatform.draw(graphics, id,
                        x + tank.x() + 1, y + tank.y() + tank.height() - 1 - fill,
                        tank.width() - 2, fill)).orElse(false)) {
                    fluidAppearanceUnavailable = true;
                    graphics.drawString(font, "?", x + tank.x() + 6, y + tank.y() + 13, 0xAA0000, false);
                }
            });
        });
        graphics.fill(x + 8, y + 16, x + 20, y + 70, 0xFF373737);
        graphics.fill(x + 9, y + 17, x + 19, y + 69, 0xFF8B8B8B);
        int arrowX = MachineMenuLayout.progressX(menu.kind());
        graphics.fill(x + arrowX, y + 35, x + arrowX + 24, y + 52, 0xFF8B8B8B);
        int energy = menu.energy();
        int capacity = menu.capacity();
        int energyHeight = (int) (50L * energy / capacity);
        if (energyHeight > 0) {
            graphics.fill(x + 10, y + 18 + (50 - energyHeight), x + 18, y + 68, 0xFF00E5FF);
        }
        int progress = menu.progress();
        int cycle = menu.cycle();
        int arrow = (int) (24L * progress / cycle);
        if (arrow > 0) {
            graphics.blit(TEXTURE, x + arrowX, y + 35, 176, 0, arrow, 17);
        }
        if (menu.kind() == MachineMenuKind.ASSEMBLER) {
            int panel = x + MachineMenuLayout.ASSEMBLER_PICKER_X;
            graphics.fill(panel, y, panel + MachineMenuLayout.ASSEMBLER_PICKER_WIDTH, y + imageHeight, 0xFF373737);
            graphics.fill(panel + 1, y + 1, panel + MachineMenuLayout.ASSEMBLER_PICKER_WIDTH - 1,
                    y + imageHeight - 1, 0xFFC6C6C6);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        drawRecessed(graphics, x, y, 18, 18);
    }

    private static void drawRecessed(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        var lines = font.split(statusLine(), imageWidth - 16);
        int visible = MachineMenuLayout.visibleStatusLines(lines.size(), font.lineHeight);
        for (int index = 0; index < visible; index++) {
            int y = MachineMenuLayout.STATUS_Y + index * font.lineHeight;
            if (index == visible - 1 && lines.size() > visible) {
                graphics.drawString(font, Component.translatable("gui.grindless.status_more"),
                        MachineMenuLayout.STATUS_X, y, 0x404040, false);
            } else {
                graphics.drawString(font, lines.get(index), MachineMenuLayout.STATUS_X, y, 0x404040, false);
            }
        }
        menu.primaryFluidView().ifPresent(fluid -> graphics.drawString(font,
                Component.translatable("gui.grindless.fluid_amount", fluid.amount(), fluid.capacity()),
                32, 16, 0x404040, false));
        if (recipeSearch != null) {
            int x = MachineMenuLayout.ASSEMBLER_PICKER_X + 6;
            graphics.drawString(font, Component.translatable("gui.grindless.assembler.title"), x, 6, 0x404040, false);
            if (shownSelection == null) {
                graphics.drawString(font, Component.translatable("gui.grindless.assembler.loading"),
                        x, 184, 0x404040, false);
            } else {
                graphics.drawString(font, font.plainSubstrByWidth(targetLine().getString(), 120), x, 182,
                        shownSelection.state().issue() == MachineMenuHost.SelectionIssue.UNAVAILABLE
                                ? 0xAA0000 : 0x404040, false);
                graphics.drawString(font, font.plainSubstrByWidth(workLine().getString(), 120), x, 192, 0x404040, false);
            }
            if (shownSelection != null && filteredRecipes.isEmpty()) {
                int y = 72;
                for (var line : font.split(Component.translatable("gui.grindless.assembler.no_results"), 120)) {
                    graphics.drawString(font, line, x, y, 0x404040, false);
                    y += font.lineHeight;
                }
            }
        }
    }

    private Component statusLine() {
        Component base = machineStatusLine();
        if (menu.kind() == MachineMenuKind.ASSEMBLER) {
            return Component.empty().append(base).append("\n").append(shownSelection == null
                    ? Component.translatable("gui.grindless.assembler.loading")
                    : Component.translatable(shownSelection.state().issue().translationKey()));
        }
        return base;
    }

    private Component targetLine() {
        String id = shownSelection.state().selected();
        Component name = id.isEmpty() ? Component.translatable("gui.grindless.assembler.auto")
                : shownSelection.choices().stream().filter(choice -> choice.id().equals(id)).findFirst()
                        .map(choice -> choice.output().isEmpty() ? Component.literal(id) : choice.output().getHoverName())
                        .orElseGet(() -> Component.translatable("gui.grindless.assembler.unavailable", id));
        return Component.translatable("gui.grindless.assembler.target", name);
    }

    private Component workLine() {
        return Component.translatable("gui.grindless.assembler.work", Double.toString(shownSelection.state().activeWork()),
                shownSelection.state().suspended().size());
    }

    private Component machineStatusLine() {
        if (menu.siteFault() != SourceSite.Fault.NONE) {
            return Component.translatable(menu.siteFault().translationKey());
        }
        if (menu.structureFault() != WorkshopStructure.Fault.NONE) {
            return Component.translatable(menu.structureFault().translationKey());
        }
        MachineStatus[] statuses = MachineStatus.values();
        int ordinal = menu.statusOrdinal();
        MachineStatus status = ordinal >= 0 && ordinal < statuses.length ? statuses[ordinal] : MachineStatus.IDLE;
        Component line = Component.translatable("gui.grindless.status." + status.getSerializedName());
        if (menu.host() != null && menu.host().menuFault() != null
                && status == MachineStatus.OUT_OF_BAND) {
            line = menu.host().menuFault();
        }
        return line;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (recipeSearch != null) {
            int rows = MachineMenuLayout.ASSEMBLER_PICKER_ROWS;
            for (int row = 0; row < rows && recipePage * rows + row < filteredRecipes.size(); row++) {
                ItemStack output = filteredRecipes.get(recipePage * rows + row).output();
                int x = leftPos + MachineMenuLayout.ASSEMBLER_PICKER_X + 7;
                int y = topPos + MachineMenuLayout.ASSEMBLER_PICKER_ROW_Y
                        + row * MachineMenuLayout.ASSEMBLER_PICKER_ROW_HEIGHT + 1;
                graphics.renderItem(output, x, y);
                graphics.renderItemDecorations(font, output, x, y);
            }
            if (shownSelection != null && mouseX >= leftPos + MachineMenuLayout.ASSEMBLER_PICKER_X
                    && mouseX < leftPos + MachineMenuLayout.screenWidth(menu.kind())
                    && mouseY >= topPos + 180 && mouseY < topPos + imageHeight) {
                var tooltip = new ArrayList<Component>();
                tooltip.add(targetLine());
                tooltip.add(Component.literal(shownSelection.state().selected()));
                tooltip.add(Component.translatable(shownSelection.state().issue().translationKey()));
                tooltip.add(workLine());
                tooltip.add(Component.translatable("gui.grindless.assembler.active_work",
                        shownSelection.state().activeRecipe().isEmpty()
                                ? Component.translatable("gui.grindless.assembler.unattributed")
                                : shownSelection.state().activeRecipe(),
                        Double.toString(shownSelection.state().activeWork())));
                tooltip.add(Component.translatable("gui.grindless.assembler.work_help"));
                ResourceGuidanceClient.renderTooltip(graphics, font, tooltip, mouseX, mouseY);
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + MachineMenuLayout.STATUS_X && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + MachineMenuLayout.STATUS_Y
                && mouseY < topPos + MachineMenuLayout.INVENTORY_LABEL_Y - 4) {
            graphics.renderTooltip(font, statusLine(), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 20
                && mouseY >= topPos + 16 && mouseY < topPos + 70) {
            graphics.renderTooltip(font, Component.translatable("gui.grindless.energy",
                    menu.energy(), menu.capacity()), mouseX, mouseY);
        }
        MachineMenuLayout.waterTank(menu.kind()).ifPresent(tank -> {
            if (mouseX >= leftPos + tank.x() && mouseX < leftPos + tank.x() + tank.width()
                    && mouseY >= topPos + tank.y() && mouseY < topPos + tank.y() + tank.height()) {
                menu.primaryFluidView().ifPresent(fluid -> {
                    var tooltip = new ArrayList<Component>();
                    tooltip.add(fluid.id().map(FluidClientPlatform::name).orElseGet(() ->
                            Component.translatable(fluid.amount() == 0
                                    ? "gui.grindless.fluid_empty" : "gui.grindless.fluid_unknown")));
                    tooltip.add(Component.translatable("gui.grindless.fluid_amount",
                            fluid.amount(), fluid.capacity()));
                    if (menu.kind() == MachineMenuKind.SETTLING_TROUGH) {
                        tooltip.add(Component.translatable("gui.grindless.fluid_output_quota", fluid.outputQuota()));
                    }
                    if (fluid.amount() > 0 && fluid.id().isEmpty()) {
                        tooltip.add(Component.translatable("gui.grindless.fluid_unknown_details")
                                .withStyle(ChatFormatting.RED));
                    } else if (fluidAppearanceUnavailable) {
                        fluid.id().ifPresent(id -> tooltip.add(
                                Component.translatable("gui.grindless.fluid_unavailable", id)
                                        .withStyle(ChatFormatting.RED)));
                    }
                    if (fluid.amount() > 0) {
                        ResourceGuidanceClient.append(tooltip, fluid.id()
                                .map(id -> new Resource(Kind.FLUID, id)).orElse(null), true, hasShiftDown());
                    }
                    ResourceGuidanceClient.renderTooltip(graphics, font, tooltip, mouseX, mouseY);
                });
            }
        });
    }
}
