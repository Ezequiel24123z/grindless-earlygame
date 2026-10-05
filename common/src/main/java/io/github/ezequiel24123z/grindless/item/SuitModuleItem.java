package io.github.ezequiel24123z.grindless.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A module that fits an exosuit slot and does not fit the harness (ADR-0103). */
public final class SuitModuleItem extends Item {

    private final String moduleId;

    public SuitModuleItem(String moduleId, Properties properties) {
        super(properties);
        this.moduleId = moduleId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack module = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(other(hand));
        if (!(other.getItem() instanceof ExosuitItem)) {
            return InteractionResultHolder.pass(module);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(module, true);
        }
        if (!ExosuitItem.install(other, moduleId, 0)) {
            player.displayClientMessage(Component.translatable("chat.grindless.exosuit.full"), true);
            return InteractionResultHolder.fail(module);
        }
        module.shrink(1);
        player.displayClientMessage(Component.translatable("chat.grindless.exosuit.installed"), true);
        return InteractionResultHolder.sidedSuccess(module, false);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }
}
