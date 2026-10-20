package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.menu.QuestBookMenu;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.shop.ShopNetwork;
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
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The quest lines, drawn with fills (ADR-0100).
 *
 * <p>T0 stays readable ahead. T1 has a separate server-gated tab; teaching and live recipes
 * are paged inside the selected quest rather than in a second required book.
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
    private static final int LIST_TOP = 40;
    private static final int DETAIL_HEIGHT = 90;
    private static final int REWARD_STEP = 20;

    private final List<Integer> rows = new ArrayList<>();
    private int scroll;
    private int selected;
    private Button claim;
    private Button help;
    private Button shop;
    private Button t1;
    private Button previous;
    private Button next;
    private Button back;
    private QuestCatalogue.Chapter chapter = QuestCatalogue.Chapter.T0;
    private boolean reading;
    private int helpPage;
    private List<List<FormattedCharSequence>> helpPages = List.of();

    public QuestBookScreen(QuestBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 222;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.chapter.t0"),
                button -> selectChapter(QuestCatalogue.Chapter.T0))
                .bounds(leftPos + 8, topPos + 18, 140, 16).build());
        t1 = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.chapter.t1"),
                button -> selectChapter(QuestCatalogue.Chapter.T1))
                .bounds(leftPos + 152, topPos + 18, 140, 16).build());
        t1.active = menu.chapterUnlocked(QuestCatalogue.Chapter.T1);
        t1.setTooltip(Tooltip.create(Component.translatable("gui.grindless.quest.chapter_gate")));
        claim = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.claim"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null && selected >= 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
                    }
                }).bounds(leftPos + 8, topPos + imageHeight - 22, 96, 16).build());
        help = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.help"),
                button -> showHelp()).bounds(leftPos + 108, topPos + imageHeight - 22, 88, 16).build());
        shop = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.shop"),
                button -> ShopNetwork.requestOpen())
                .bounds(leftPos + 200, topPos + imageHeight - 22, 92, 16).build());
        back = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.quest.back"),
                button -> setReading(false)).bounds(leftPos + 8, topPos + imageHeight - 22, 92, 16).build());
        previous = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.guide.prev"),
                button -> turn(-1)).bounds(leftPos + 104, topPos + imageHeight - 22, 92, 16).build());
        next = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.guide.next"),
                button -> turn(1)).bounds(leftPos + 200, topPos + imageHeight - 22, 92, 16).build());
        selectChapter(menu.chapterUnlocked(chapter) ? chapter : QuestCatalogue.Chapter.T0);
    }

    private void selectChapter(QuestCatalogue.Chapter selectedChapter) {
        if (!menu.chapterUnlocked(selectedChapter)) {
            return;
        }
        chapter = selectedChapter;
        rows.clear();
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        for (String line : QuestCatalogue.lines(chapter)) {
            rows.add(-1);
            for (int i = 0; i < tasks.size(); i++) {
                QuestCatalogue.Task task = tasks.get(i);
                if (QuestCatalogue.chapter(task) == chapter && task.line().equals(line)) {
                    rows.add(i);
                }
            }
        }
        selected = firstTask();
        scroll = 0;
        setReading(false);
        selectTask(selected);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        t1.active = menu.chapterUnlocked(QuestCatalogue.Chapter.T1);
        if (!menu.chapterUnlocked(chapter)) {
            selectChapter(QuestCatalogue.Chapter.T0);
        }
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
            case CLAIMABLE -> task.type() == QuestCatalogue.Type.LESSON
                    ? "gui.grindless.quest.record" : task.evidence() == QuestCatalogue.Evidence.FREE
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
            claim.setTooltip(Tooltip.create(Component.translatable(task.type() == QuestCatalogue.Type.LESSON
                    ? "gui.grindless.quest.lesson_policy" : task.evidence() == QuestCatalogue.Evidence.FREE
                    ? "gui.grindless.quest.starter_policy" : "gui.grindless.quest.claim_policy")));
        }
        updateClaim();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (reading) {
            return;
        }
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
                if (task.id().equals(QuestCatalogue.STARTER_SUPPLIES)) {
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
        graphics.fill(x + 4, reading ? y + LIST_TOP : detailTop,
                x + imageWidth - 4, y + imageHeight - 24, PANEL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, HEADER, false);
        if (reading) {
            drawHelp(graphics);
            return;
        }
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
                String mark = status == QuestLogic.Status.CLAIMED ? "+ "
                        : status == QuestLogic.Status.CLAIMABLE ? "> " : "- ";
                String optional = QuestTeaching.path(entry) == QuestTeaching.Path.OPTIONAL
                        ? Component.translatable("gui.grindless.quest.optional").getString() : "";
                String text = font.plainSubstrByWidth(mark + Component.translatable(
                        "gui.grindless.quest.task." + entry.id()).getString() + optional, imageWidth - 24);
                graphics.drawString(font, text, 14, rowY, color, false);
            }
        }
        drawDetail(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (reading) {
            turn(delta < 0 ? 1 : -1);
            return true;
        }
        int max = Math.max(0, rows.size() - visibleRows());
        scroll = (int) Math.max(0, Math.min(max, scroll - Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !reading) {
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
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                        "gui.grindless.quest.task." + task.id()).getString(), imageWidth - 16),
                8, y, HEADER, false);
        String needs = task.id().equals(QuestCatalogue.STARTER_SUPPLIES)
                ? Component.translatable("gui.grindless.quest.starter_once").getString() : needs(task);
        if (!needs.isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(needs, imageWidth - 16), 8, y + 12, MUTED, false);
        }
        graphics.drawString(font, font.plainSubstrByWidth(objective(task), imageWidth - 16), 8, y + 22, ROW, false);
        graphics.drawString(font, task.type() == QuestCatalogue.Type.LESSON
                        ? Component.translatable("gui.grindless.quest.no_material_reward")
                        : Component.translatable("gui.grindless.quest.reward_stacks", task.rewards().size()),
                8, y + 34, MUTED, false);
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
        List<String> required = QuestCatalogue.requirements(task);
        if (required.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder(Component.translatable("gui.grindless.quest.needs").getString());
        for (int i = 0; i < required.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(Component.translatable("gui.grindless.quest.task." + required.get(i)).getString());
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
            return Component.translatable(task.type() == QuestCatalogue.Type.LESSON
                    ? "gui.grindless.quest.objective.read" : "gui.grindless.quest.objective.free").getString();
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
        List<String> lines = QuestCatalogue.lines(chapter);
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

    private void showHelp() {
        if (selected < 0) {
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.translatable("gui.grindless.quest.task."
                + QuestCatalogue.tasks().get(selected).id()), imageWidth - 24));
        lines.add(FormattedCharSequence.EMPTY);
        for (Component paragraph : QuestTeachingClient.content(QuestCatalogue.tasks().get(selected))) {
            lines.addAll(font.split(paragraph, imageWidth - 24));
            lines.add(FormattedCharSequence.EMPTY);
        }
        int perPage = Math.max(1, (imageHeight - LIST_TOP - 42) / font.lineHeight);
        helpPages = QuestTeaching.paginate(lines, perPage);
        helpPage = 0;
        setReading(true);
    }

    private void setReading(boolean value) {
        reading = value;
        claim.visible = !value;
        help.visible = !value;
        shop.visible = !value;
        back.visible = value;
        previous.visible = value;
        next.visible = value;
        updatePages();
    }

    private void updatePages() {
        previous.active = reading && helpPage > 0;
        next.active = reading && helpPage + 1 < helpPages.size();
    }

    private void turn(int delta) {
        helpPage = Math.max(0, Math.min(Math.max(0, helpPages.size() - 1), helpPage + delta));
        updatePages();
    }

    private void drawHelp(GuiGraphics graphics) {
        if (helpPages.isEmpty()) {
            return;
        }
        int y = LIST_TOP + 4;
        for (FormattedCharSequence line : helpPages.get(helpPage)) {
            graphics.drawString(font, line, 12, y, ROW, false);
            y += font.lineHeight;
        }
        graphics.drawString(font, Component.translatable("gui.grindless.guide.page", helpPage + 1, helpPages.size()),
                12, imageHeight - 36, MUTED, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (reading && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setReading(false);
            return true;
        }
        if (reading && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            turn(keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            return true;
        }
        if (!reading && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
            int at = rows.indexOf(selected);
            int step = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
            for (int index = at + step; index >= 0 && index < rows.size(); index += step) {
                if (rows.get(index) >= 0) {
                    selectTask(rows.get(index));
                    scroll = Math.max(0, Math.min(index, Math.max(scroll, index - visibleRows() + 1)));
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
