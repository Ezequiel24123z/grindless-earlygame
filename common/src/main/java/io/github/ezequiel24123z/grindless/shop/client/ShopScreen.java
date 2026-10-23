package io.github.ezequiel24123z.grindless.shop.client;

import io.github.ezequiel24123z.grindless.shop.ShopCatalogue;
import io.github.ezequiel24123z.grindless.shop.ShopInventory;
import io.github.ezequiel24123z.grindless.shop.ShopMenu;
import io.github.ezequiel24123z.grindless.shop.ShopNetwork;
import io.github.ezequiel24123z.grindless.shop.ShopResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Extends the existing native quest-book visual language; every action is keyboard-focusable. */
public final class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PANEL = 0xFF1A1A22;
    private static final int INNER = 0xFF252532;
    private static final int HEADER = 0xFF18FFFF;
    private static final int BODY = 0xFFECEFF1;
    private static final int MUTED = 0xFFB0BEC5;
    private static final int PAGE_SIZE = 8;
    private static final int DETAIL_X = 168;
    private final List<Button> offers = new ArrayList<>();
    private final int[] describedOffer = new int[PAGE_SIZE];
    private final int[] describedStatus = new int[PAGE_SIZE];
    private int selected;
    private int page;
    private int sentSequence = -1;
    private Button trade;
    private Button previous;
    private Button next;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 238;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        offers.clear();
        java.util.Arrays.fill(describedOffer, -1);
        java.util.Arrays.fill(describedStatus, -1);
        for (int row = 0; row < PAGE_SIZE; row++) {
            int offset = row;
            offers.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                selected = page * PAGE_SIZE + offset;
                update();
            }).bounds(leftPos + 8, topPos + 30 + row * 18, 150, 18).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.shop.previous"),
                button -> changePage(-1)).bounds(leftPos + 8, topPos + 180, 72, 18).build());
        next = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.shop.next"),
                button -> changePage(1)).bounds(leftPos + 86, topPos + 180, 72, 18).build());
        trade = addRenderableWidget(Button.builder(Component.translatable("gui.grindless.shop.trade"), button -> {
            if (sentSequence < 0 && menu.availability(selected) == ShopResult.READY) {
                sentSequence = menu.sequence();
                ShopNetwork.requestExchange(menu, selected);
                update();
            }
        }).bounds(leftPos + DETAIL_X, topPos + 206, 124, 20).build());
        trade.setTooltip(Tooltip.create(Component.translatable("gui.grindless.shop.atomic_policy")));
        update();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (sentSequence >= 0 && menu.sequence() != sentSequence) {
            sentSequence = -1;
        }
        update();
    }

    private void update() {
        if (trade == null) {
            return;
        }
        for (int row = 0; row < offers.size(); row++) {
            int index = page * PAGE_SIZE + row;
            Button button = offers.get(row);
            button.visible = index < ShopCatalogue.offers().size();
            if (button.visible) {
                ShopCatalogue.Offer offer = ShopCatalogue.offers().get(index);
                Component label = Component.translatable("gui.grindless.shop.offer." + offer.id());
                button.setMessage(Component.literal((index == selected ? "> " : "")
                        + font.plainSubstrByWidth(label.getString(), 132)));
                if (describedOffer[row] != index || describedStatus[row] != menu.availability(index).ordinal()) {
                    button.setTooltip(Tooltip.create(Component.translatable("gui.grindless.shop.offer_tooltip",
                            label, Component.translatable(menu.availability(index).translationKey()),
                            Component.translatable("gui.grindless.shop.detail." + offer.id()))));
                    describedOffer[row] = index;
                    describedStatus[row] = menu.availability(index).ordinal();
                }
            }
        }
        previous.active = page > 0;
        next.active = (page + 1) * PAGE_SIZE < ShopCatalogue.offers().size();
        trade.active = sentSequence < 0 && menu.availability(selected) == ShopResult.READY;
        trade.setMessage(Component.translatable(sentSequence < 0 ? "gui.grindless.shop.trade" : "gui.grindless.shop.waiting"));
    }

    private void changePage(int delta) {
        int last = Math.max(0, (ShopCatalogue.offers().size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(last, page + delta));
        selected = page * PAGE_SIZE;
        update();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 158 && delta != 0) {
            changePage(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, INNER);
        graphics.fill(leftPos + 164, topPos + 28, leftPos + imageWidth - 4, topPos + 202, PANEL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, HEADER, false);
        graphics.drawString(font, Component.translatable("gui.grindless.shop.subtitle"), 8, 18, MUTED, false);
        graphics.drawString(font, Component.translatable("gui.grindless.shop.page", page + 1,
                (ShopCatalogue.offers().size() + PAGE_SIZE - 1) / PAGE_SIZE), 8, 208, MUTED, false);
        ShopCatalogue.Offer offer = ShopCatalogue.offers().get(selected);
        graphics.drawString(font, Component.translatable("gui.grindless.shop.receive"), DETAIL_X, 32, HEADER, false);
        goods(graphics, offer.goods().item(), offer.goods().count(), 46);
        graphics.drawString(font, Component.translatable("gui.grindless.shop.pay"), DETAIL_X, 72, HEADER, false);
        int paymentY = 86;
        for (var payment : offer.payment().entrySet()) {
            goods(graphics, payment.getKey(), payment.getValue(), paymentY);
            paymentY += 20;
        }
        graphics.drawString(font, Component.translatable("gui.grindless.shop.requires"), DETAIL_X, 130, MUTED, false);
        int requirementY = 142;
        for (String quest : new java.util.TreeSet<>(offer.requires())) {
            Component name = Component.translatable("gui.grindless.quest.task." + quest);
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), 120), DETAIL_X, requirementY, BODY, false);
            requirementY += 10;
        }
        Component status = Component.translatable(menu.availability(selected).translationKey());
        int statusY = 164;
        for (var line : font.split(status, 120)) {
            if (statusY >= 194) {
                break;
            }
            graphics.drawString(font, line, DETAIL_X, statusY, BODY, false);
            statusY += 10;
        }
        if (menu.lastOffer() == selected && menu.result() == ShopResult.TRADED) {
            graphics.drawString(font, Component.translatable(ShopResult.TRADED.translationKey()), DETAIL_X, 190, HEADER, false);
        }
        graphics.drawString(font, Component.translatable("gui.grindless.shop.footer"), 8, 226, MUTED, false);
    }

    private void goods(GuiGraphics graphics, String id, int count, int y) {
        ItemStack stack = new ItemStack(ShopInventory.resolve(id), count);
        graphics.renderItem(stack, DETAIL_X, y);
        graphics.renderItemDecorations(font, stack, DETAIL_X, y);
        graphics.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 100),
                DETAIL_X + 22, y + 4, BODY, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mouseX < leftPos + DETAIL_X || mouseX >= leftPos + imageWidth - 4) {
            return;
        }
        ShopCatalogue.Offer offer = ShopCatalogue.offers().get(selected);
        if (mouseY >= topPos + 46 && mouseY < topPos + 64) {
            graphics.renderTooltip(font, new ItemStack(ShopInventory.resolve(offer.goods().item()), offer.goods().count()),
                    mouseX, mouseY);
        }
        int y = topPos + 86;
        for (var payment : offer.payment().entrySet()) {
            if (mouseY >= y && mouseY < y + 18) {
                graphics.renderTooltip(font, new ItemStack(ShopInventory.resolve(payment.getKey()), payment.getValue()),
                        mouseX, mouseY);
                return;
            }
            y += 20;
        }
    }
}
