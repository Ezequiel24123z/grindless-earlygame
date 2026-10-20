package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.menu.QuestBookMenu;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The quest lines, drawn with fills (ADR-0100).
 *
 * <p>A row is a line header or a task. Selecting a task and pressing Claim asks the server
 * to pay the complete reward batch once. Locked rows stay visible so the route can be read ahead.
 */
public final class QuestBookScreen extends AbstractContainerScreen<QuestBookMenu> {

    private static final int PANEL = 0xFF1A1A22;
    private static final int PANEL_INNER = 0xFF252532;
    private static final int HEADER = 0xFF18FFFF;
    private static final int ROW = 0xFFECEFF1;
    private static final int SELECTED = 0xFF80CBC4;
    private static final int MUTED = 0xFF90A4AE;
    private static final int DONE = 0xFF546E7A;
    private static final int ROW_HEIGHT = 10;
    private static final int LIST_TOP = 18;
    private static final int DETAIL_HEIGHT = 90;
    private static final int REWARD_STEP = 20;

    private final List<Integer> rows = new ArrayList<>();
    private int scroll;
    private int selected;
    private Button claim;

    public QuestBookScreen(QuestBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 222;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        String line = "";
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        for (int i = 0; i < tasks.size(); i++) {
            QuestCatalogue.Task task = tasks.get(i);
            if (!task.line().equals(line)) {
                rows.add(-1);
                line = task.line();
            }
            rows.add(i);
        }
        selected = firstTask();
        scroll = 0;
        claim = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.claim"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null && selected >= 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    }
                }).bounds(leftPos + 8, topPos + imageHeight - 22, 120, 16).build());
        selectTask(selected);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateClaim();
    }

    private void updateClaim() {
        if (claim == null || selected < 0 || selected >= QuestCatalogue.tasks().size()) {
            return;
        }
        QuestCatalogue.Task task = QuestCatalogue.tasks().get(selected);
        QuestLogic.Status status = menu.status(selected);
        claim.active = status == QuestLogic.Status.CLAIMABLE;
        claim.setMessage(Component.translatable(switch (status) {
            case CLAIMABLE -> task.evidence() == QuestCatalogue.Evidence.FREE
                    ? "gui.grindless.quest.claim_free" : "gui.grindless.quest.claim";
            case CLAIMED -> "gui.grindless.quest.claimed";
            case LOCKED -> "gui.grindless.quest.locked";
            case UNMET -> "gui.grindless.quest.unmet";
        }));
    }

    private void selectTask(int index) {
        selected = index;
        if (selected >= 0 && selected < QuestCatalogue.tasks().size()) {
            QuestCatalogue.Task task = QuestCatalogue.tasks().get(selected);
            claim.setTooltip(Tooltip.create(Component.translatable(task.evidence() == QuestCatalogue.Evidence.FREE
                    ? "gui.grindless.quest.starter_policy" : "gui.grindless.quest.claim_policy")));
        }
        updateClaim();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (selected >= 0 && selected < QuestCatalogue.tasks().size()
                && mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8) {
            QuestCatalogue.Task task = QuestCatalogue.tasks().get(selected);
            for (int i = 0; i < task.rewards().size(); i++) {
                int rewardX = leftPos + 8 + i * REWARD_STEP;
                int rewardY = topPos + imageHeight - 42;
                if (mouseX >= rewardX - 1 && mouseX < rewardX + 17
                        && mouseY >= rewardY - 1 && mouseY < rewardY + 17) {
                    QuestCatalogue.Reward reward = task.rewards().get(i);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(reward(reward));
                    ResourceGuidanceClient.append(tooltip, new Resource(Kind.ITEM, reward.itemId()), true, hasShiftDown());
                    ResourceGuidanceClient.renderTooltip(graphics, font, tooltip, mouseX, mouseY);
                    return;
                }
            }
            int objectiveY = topPos + imageHeight - DETAIL_HEIGHT + 26;
            if (mouseY >= objectiveY && mouseY < objectiveY + 10) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(objective(task)));
                if (task.evidence() == QuestCatalogue.Evidence.FREE) {
                    tooltip.add(Component.translatable("gui.grindless.quest.starter_policy"));
                }
                ResourceGuidanceContext.questSubject(task).ifPresent(resource ->
                        ResourceGuidanceClient.append(tooltip, resource, true, hasShiftDown()));
                ResourceGuidanceClient.renderTooltip(graphics, font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL_INNER);
        int detailTop = y + imageHeight - DETAIL_HEIGHT;
        graphics.fill(x + 4, detailTop, x + imageWidth - 4, y + imageHeight - 24, PANEL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, HEADER, false);
        int visible = visibleRows();
        int maxScroll = Math.max(0, rows.size() - visible);
        scroll = Math.min(scroll, maxScroll);
        int listBottom = imageHeight - DETAIL_HEIGHT - 4;
        int lineOrdinal = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) < 0) {
                lineOrdinal++;
            }
            if (i < scroll || i >= scroll + visible) {
                continue;
            }
            int rowY = LIST_TOP + (i - scroll) * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT > listBottom) {
                break;
            }
            int task = rows.get(i);
            if (task < 0) {
                graphics.drawString(font, lineName(lineOrdinal), 8, rowY, HEADER, false);
            } else {
                QuestCatalogue.Task entry = QuestCatalogue.tasks().get(task);
                QuestLogic.Status status = menu.status(task);
                int color = task == selected ? SELECTED : color(status);
                String text = font.plainSubstrByWidth(Component.translatable(
                        "gui.grindless.quest.task." + entry.id()).getString(), imageWidth - 24);
                graphics.drawString(font, text, 14, rowY, color, false);
            }
        }
        drawDetail(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, rows.size() - visibleRows());
        scroll = (int) Math.max(0, Math.min(max, scroll - Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int relY = (int) mouseY - topPos;
            int relX = (int) mouseX - leftPos;
            int listBottom = imageHeight - DETAIL_HEIGHT - 4;
            if (relX >= 8 && relX < imageWidth - 8 && relY >= LIST_TOP && relY < listBottom) {
                int index = scroll + (relY - LIST_TOP) / ROW_HEIGHT;
                if (index >= 0 && index < rows.size() && rows.get(index) >= 0) {
                    selectTask(rows.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawDetail(GuiGraphics graphics) {
        if (selected < 0 || selected >= QuestCatalogue.tasks().size()) {
            return;
        }
        QuestCatalogue.Task task = QuestCatalogue.tasks().get(selected);
        int y = imageHeight - DETAIL_HEIGHT + 4;
        graphics.drawString(font, Component.translatable("gui.grindless.quest.task." + task.id()),
                8, y, HEADER, false);
        String needs = task.evidence() == QuestCatalogue.Evidence.FREE
                ? Component.translatable("gui.grindless.quest.starter_once").getString() : needs(task);
        if (!needs.isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(needs, imageWidth - 16), 8, y + 12, MUTED, false);
        }
        graphics.drawString(font, font.plainSubstrByWidth(objective(task), imageWidth - 16), 8, y + 22, ROW, false);
        graphics.drawString(font, Component.translatable("gui.grindless.quest.reward_stacks", task.rewards().size()),
                8, y + 34, MUTED, false);
        // All eleven legal starter stacks fit without aggregation, clipping or a hidden second row.
        for (int i = 0; i < task.rewards().size(); i++) {
            int rewardX = 8 + i * REWARD_STEP;
            int rewardY = imageHeight - 42;
            ItemStack stack = rewardStack(task.rewards().get(i));
            graphics.fill(rewardX - 1, rewardY - 1, rewardX + 17, rewardY + 17, PANEL_INNER);
            graphics.renderItem(stack, rewardX, rewardY);
            graphics.renderItemDecorations(font, stack, rewardX, rewardY);
        }
    }

    private String needs(QuestCatalogue.Task task) {
        if (task.requires().isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder(Component.translatable("gui.grindless.quest.needs").getString());
        for (int i = 0; i < task.requires().size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(Component.translatable("gui.grindless.quest.task." + task.requires().get(i)).getString());
        }
        return text.toString();
    }

    private static ItemStack rewardStack(QuestCatalogue.Reward reward) {
        ResourceLocation id = ResourceLocation.tryParse(reward.itemId());
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item == null ? Items.AIR : item, reward.count());
    }

    private static Component reward(QuestCatalogue.Reward reward) {
        ItemStack stack = rewardStack(reward);
        Component name = stack.isEmpty() ? Component.literal(reward.itemId()) : stack.getHoverName();
        return Component.translatable("gui.grindless.quest.reward", name, reward.count());
    }

    private String objective(QuestCatalogue.Task task) {
        if (task.evidence() == QuestCatalogue.Evidence.FREE) {
            return Component.translatable("gui.grindless.quest.objective.free").getString();
        }
        if (task.evidence() == QuestCatalogue.Evidence.DIMENSION) {
            return Component.translatable("gui.grindless.quest.objective.dimension", task.subject()).getString();
        }
        ResourceLocation id = ResourceLocation.tryParse(task.subject());
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        String name = item == null || item == Items.AIR ? task.subject() : item.getDescription().getString();
        if (task.evidence() == QuestCatalogue.Evidence.MATRIX && item instanceof ControlMatrixItem matrix) {
            name = matrix.stackFor(task.matrixRating(), 1).getHoverName().getString();
        }
        return Component.translatable("gui.grindless.quest.objective.item", name,
                menu.held(selected), task.count()).getString();
    }

    private static int color(QuestLogic.Status status) {
        return switch (status) {
            case CLAIMABLE -> SELECTED;
            case CLAIMED -> DONE;
            case LOCKED, UNMET -> MUTED;
        };
    }

    private String lineName(int ordinal) {
        List<String> lines = QuestCatalogue.lines();
        if (ordinal < 0 || ordinal >= lines.size()) {
            return "";
        }
        return Component.translatable("gui.grindless.quest.line." + lines.get(ordinal)).getString();
    }

    private int firstTask() {
        for (int row : rows) {
            if (row >= 0) {
                return row;
            }
        }
        return -1;
    }

    private int visibleRows() {
        int listHeight = imageHeight - DETAIL_HEIGHT - LIST_TOP - 4;
        return Math.max(1, listHeight / ROW_HEIGHT);
    }
}
