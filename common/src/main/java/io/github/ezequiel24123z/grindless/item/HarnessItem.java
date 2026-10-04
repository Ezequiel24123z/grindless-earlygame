package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** One piece of the Voltaic Harness. One slot. No generator (ADR-0089). */
public final class HarnessItem extends ArmorItem {

    public HarnessItem(Type type, Properties properties) {
        super(VoltaicArmorMaterial.VOLTAIC, type, properties);
    }

    public static HarnessLogic.Piece piece(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(HarnessLogic.MODULE)) {
            return HarnessLogic.empty();
        }
        return new HarnessLogic.Piece(tag.getString(HarnessLogic.MODULE), tag.getInt(HarnessLogic.CHARGE));
    }

    public static void write(ItemStack stack, HarnessLogic.Piece piece) {
        CompoundTag tag = stack.getOrCreateTag();
        if (piece == null || !piece.filled()) {
            tag.remove(HarnessLogic.MODULE);
            tag.remove(HarnessLogic.CHARGE);
            return;
        }
        tag.putString(HarnessLogic.MODULE, piece.module());
        tag.putInt(HarnessLogic.CHARGE, piece.charge());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && piece(stack).filled()) {
            if (!level.isClientSide()) {
                eject(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    private static void eject(Player player, ItemStack stack) {
        HarnessLogic.Piece piece = piece(stack);
        Integer charge = HarnessLogic.remove(piece);
        write(stack, HarnessLogic.empty());
        ItemStack cell = new ItemStack(ModItems.FLUX_CELL.get());
        cell.getOrCreateTag().putInt(HarnessLogic.CHARGE, charge == null ? 0 : charge);
        if (!player.getInventory().add(cell)) {
            player.drop(cell, false);
        }
        player.displayClientMessage(Component.translatable("chat.grindless.harness.removed"), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        HarnessLogic.Piece piece = piece(stack);
        if (piece.filled()) {
            tooltip.add(Component.translatable("tooltip.grindless.harness.cell",
                    piece.charge(), HarnessLogic.CELL_CAPACITY));
        } else {
            tooltip.add(Component.translatable("tooltip.grindless.harness.empty"));
        }
    }
}
