package io.github.ezequiel24123z.grindless.item;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.menu.FieldGuideMenu;
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
 * Handheld field guide (ADR-0100).
 *
 * <p>Right-click reads the route from the hand crank to the sealed chamber. Pages stay
 * open whether or not the matching quest is claimed.
 */
public final class FieldGuideItem extends Item {

    public FieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            MenuRegistry.openMenu(server, new SimpleMenuProvider(
                    (id, inventory, opener) -> new FieldGuideMenu(id, inventory),
                    Component.translatable("gui.grindless.guide.title")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.field_guide"));
    }
}
