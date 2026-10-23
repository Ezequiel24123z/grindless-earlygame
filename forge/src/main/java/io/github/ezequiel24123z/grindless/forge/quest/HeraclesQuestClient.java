package io.github.ezequiel24123z.grindless.forge.quest;

import com.teamresourceful.resourcefullib.client.scissor.ScissorBoxStack;
import earth.terrarium.heracles.api.client.DisplayWidget;
import earth.terrarium.heracles.api.client.WidgetUtils;
import earth.terrarium.heracles.api.tasks.client.QuestTaskWidgetFactory;
import earth.terrarium.heracles.api.tasks.client.QuestTaskWidgets;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.client.ResourceGuidanceClient;
import io.github.ezequiel24123z.grindless.client.QuestTeachingClient;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.shop.ShopNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NumericTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** One public native task widget, with Minecraft controls and live server-confirmed claimability. */
public final class HeraclesQuestClient {

    private static HeraclesQuestNetwork.Snapshot snapshot;
    private static long revision;

    private HeraclesQuestClient() {
    }

    public static void init() {
        QuestTaskWidgetFactory<ServerPlayer, NumericTag, GrindlessClaimTask> factory =
                (questId, task, progress, status) -> new ClaimWidget(task);
        QuestTaskWidgets.register(GrindlessClaimTask.TYPE, factory);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            snapshot = null;
            revision++;
        });
    }

    public static void receive(HeraclesQuestNetwork.Snapshot update) {
        snapshot = update;
        revision++;
    }

    private static final class ClaimWidget implements DisplayWidget {
        private final QuestCatalogue.Task task;
        private final Button button;
        private final Button shop;
        private final Button previous;
        private final Button next;
        private final List<Component> lesson;
        private List<List<FormattedCharSequence>> lessonPages = List.of();
        private int lessonWidth = -1;
        private int page;
        private int originX;
        private int originY;
        private long pendingRevision = -1;
        private long pendingUntil;

        private ClaimWidget(GrindlessClaimTask adapter) {
            task = QuestCatalogue.byId(adapter.catalogueId());
            lesson = QuestTeachingClient.content(task);
            button = Button.builder(Component.translatable(task.type() == QuestCatalogue.Type.LESSON
                    ? "gui.grindless.quest.record" : task.evidence() == QuestCatalogue.Evidence.FREE
                    ? "gui.grindless.quest.claim_free" : "gui.grindless.quest.claim"), ignored -> {
                HeraclesQuestNetwork.claim(task.id());
                pendingRevision = revision;
                pendingUntil = net.minecraft.Util.getMillis() + 2000;
            }).bounds(0, 0, 100, 20).build();
            shop = Button.builder(Component.translatable("gui.grindless.quest.shop"),
                    ignored -> ShopNetwork.requestOpen()).bounds(0, 0, 100, 20).build();
            previous = Button.builder(Component.translatable("gui.grindless.guide.prev"),
                    ignored -> page = Math.max(0, page - 1)).bounds(0, 0, 100, 20).build();
            next = Button.builder(Component.translatable("gui.grindless.guide.next"),
                    ignored -> page = Math.min(lessonPages.size() - 1, page + 1)).bounds(0, 0, 100, 20).build();
        }

        private HeraclesQuestNetwork.State state() {
            return snapshot == null ? null : snapshot.tasks().stream()
                    .filter(value -> value.taskId().equals(task.id())).findFirst().orElse(null);
        }

        private Component action() {
            HeraclesQuestNetwork.State state = state();
            if (snapshot == null) {
                return Component.translatable("gui.grindless.quest.waiting");
            }
            if (!snapshot.available()) {
                return Component.translatable("gui.grindless.quest.backend_unavailable");
            }
            if (state != null && state.status() == QuestLogic.Status.CLAIMED) {
                return Component.translatable("gui.grindless.quest.claimed");
            }
            if (!snapshot.holdingBook()) {
                return Component.translatable("gui.grindless.quest.book_required");
            }
            return Component.translatable(state == null ? "gui.grindless.quest.waiting" : switch (state.status()) {
                case CLAIMABLE -> task.type() == QuestCatalogue.Type.LESSON
                        ? "gui.grindless.quest.record" : task.evidence() == QuestCatalogue.Evidence.FREE
                        ? "gui.grindless.quest.claim_free" : "gui.grindless.quest.claim";
                case CLAIMED -> "gui.grindless.quest.claimed";
                case LOCKED -> "gui.grindless.quest.locked";
                case UNMET -> "gui.grindless.quest.unmet";
            });
        }

        private boolean claimable() {
            HeraclesQuestNetwork.State state = state();
            return snapshot != null && snapshot.available() && snapshot.holdingBook() && state != null
                    && state.status() == QuestLogic.Status.CLAIMABLE
                    && (pendingRevision != revision || net.minecraft.Util.getMillis() >= pendingUntil);
        }

        private List<FormattedCharSequence> lines(int width) {
            var font = Minecraft.getInstance().font;
            List<Component> text = new ArrayList<>();
            HeraclesQuestNetwork.State state = state();
            int held = state == null ? 0 : state.held();
            if (task.evidence() == QuestCatalogue.Evidence.FREE) {
                text.add(Component.translatable(task.type() == QuestCatalogue.Type.LESSON
                        ? "gui.grindless.quest.objective.read" : "gui.grindless.quest.objective.free"));
            } else {
                Component subject = task.evidence() == QuestCatalogue.Evidence.DIMENSION
                        ? Component.literal(task.subject()) : itemName(task.subject());
                text.add(task.evidence() == QuestCatalogue.Evidence.DIMENSION
                        ? Component.translatable("gui.grindless.quest.objective.dimension", subject)
                        : task.evidence() == QuestCatalogue.Evidence.MATRIX
                        ? Component.translatable("gui.grindless.quest.objective.matrix", subject,
                                task.matrixRating().name(), held, task.count())
                        : Component.translatable("gui.grindless.quest.objective.item", subject, held, task.count()));
            }
            for (QuestCatalogue.Reward reward : task.rewards()) {
                text.add(Component.translatable("gui.grindless.quest.reward", itemName(reward.itemId()), reward.count()));
            }
            text.add(Component.translatable(task.type() == QuestCatalogue.Type.LESSON
                    ? "gui.grindless.quest.lesson_policy" : task.evidence() == QuestCatalogue.Evidence.FREE
                    ? "gui.grindless.quest.starter_policy" : "gui.grindless.quest.claim_policy"));
            ResourceGuidanceContext.questSubject(task).ifPresent(resource ->
                    text.addAll(ResourceGuidanceClient.playerContext(resource)));
            List<FormattedCharSequence> result = new ArrayList<>();
            result.addAll(font.split(Component.translatable("gui.grindless.quest.task." + task.id()),
                    Math.max(1, width - 16)));
            if (lessonWidth != width) {
                List<FormattedCharSequence> wrapped = new ArrayList<>();
                lesson.forEach(paragraph -> wrapped.addAll(font.split(paragraph, Math.max(1, width - 16))));
                lessonPages = QuestTeaching.paginate(wrapped, 8);
                lessonWidth = width;
                page = Math.min(page, lessonPages.size() - 1);
            }
            result.addAll(lessonPages.get(page));
            result.addAll(font.split(Component.translatable("gui.grindless.guide.page", page + 1, lessonPages.size()),
                    Math.max(1, width - 16)));
            for (Component line : text) {
                result.addAll(font.split(line, Math.max(1, width - 16)));
            }
            return result;
        }

        @Override
        public int getHeight(int width) {
            return 16 + lines(width).size() * (Minecraft.getInstance().font.lineHeight + 2) + 76;
        }

        @Override
        public void render(GuiGraphics graphics, ScissorBoxStack scissor, int x, int y, int width,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            var font = Minecraft.getInstance().font;
            originX = x;
            originY = y;
            WidgetUtils.drawBackground(graphics, x, y, width, getHeight(width));
            int textY = y + 8;
            for (FormattedCharSequence line : lines(width)) {
                graphics.drawString(font, line, x + 8, textY, 0xFFFFFF, false);
                textY += font.lineHeight + 2;
            }
            previous.setPosition(x + 8, y + getHeight(width) - 76);
            previous.setWidth(Math.max(1, (width - 20) / 2));
            previous.active = page > 0;
            previous.render(graphics, mouseX, mouseY, partialTick);
            next.setPosition(x + 12 + (width - 20) / 2, y + getHeight(width) - 76);
            next.setWidth(Math.max(1, (width - 20) / 2));
            next.active = page + 1 < lessonPages.size();
            next.render(graphics, mouseX, mouseY, partialTick);
            button.setPosition(x + 8, y + getHeight(width) - 52);
            button.setWidth(Math.max(1, width - 16));
            button.active = claimable();
            button.setMessage(action());
            button.render(graphics, mouseX, mouseY, partialTick);
            shop.setPosition(x + 8, y + getHeight(width) - 28);
            shop.setWidth(Math.max(1, width - 16));
            shop.active = snapshot != null && snapshot.holdingBook();
            shop.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton, int width) {
            button.active = claimable();
            return previous.mouseClicked(originX + mouseX, originY + mouseY, mouseButton)
                    || next.mouseClicked(originX + mouseX, originY + mouseY, mouseButton)
                    || button.mouseClicked(originX + mouseX, originY + mouseY, mouseButton)
                    || shop.mouseClicked(originX + mouseX, originY + mouseY, mouseButton);
        }
    }

    private static Component itemName(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
                ? new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName() : Component.literal(name);
    }
}
