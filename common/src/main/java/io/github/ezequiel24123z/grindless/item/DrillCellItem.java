package io.github.ezequiel24123z.grindless.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One charge of the Flux Drill. Not the armour Flux Cell (ADR-0084). */
public final class DrillCellItem extends Item {

    public DrillCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cell = player.getItemInHand(hand);
        ItemStack drill = player.getItemInHand(other(hand));
        if (!(drill.getItem() instanceof FluxDrillItem)) {
            return InteractionResultHolder.pass(cell);
        }
        int stored = FluxDrillItem.charge(drill);
        int next = DrillLogic.storedAfterCell(stored);
        if (next == stored) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("chat.grindless.drill_cell.full"), true);
            }
            return InteractionResultHolder.fail(cell);
        }
        if (!level.isClientSide()) {
            FluxDrillItem.setCharge(drill, next);
            cell.shrink(1);
            player.displayClientMessage(Component.translatable("chat.grindless.drill_cell.charged",
                    next), true);
        }
        return InteractionResultHolder.sidedSuccess(cell, level.isClientSide());
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }
}
