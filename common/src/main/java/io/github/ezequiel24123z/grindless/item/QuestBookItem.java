package io.github.ezequiel24123z.grindless.item;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.menu.QuestBookMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Handheld quest book (ADR-0100).
 *
 * <p>Right-click opens the route: lines, tasks, dependencies and rewards. It does not
 * unlock a blueprint and it does not move the player.
 */
public final class QuestBookItem extends Item {

    public QuestBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            MenuRegistry.openMenu(server, new SimpleMenuProvider(
                    (id, inventory, opener) -> new QuestBookMenu(id, inventory),
                    Component.translatable("gui.grindless.quest.title")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.quest_book"));
    }
}
