package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** T2 chassis. Two slots. Does not generate (ADR-0103). */
public final class ExosuitItem extends ArmorItem {

    public ExosuitItem(Type type, Properties properties) {
        super(ExosuitArmorMaterial.EXOSUIT, type, properties);
    }

    public static ExosuitLogic.Piece piece(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return ExosuitLogic.empty();
        }
        return new ExosuitLogic.Piece(
                tag.getString(ExosuitLogic.FIRST), tag.getInt(ExosuitLogic.FIRST_CHARGE),
                tag.getString(ExosuitLogic.SECOND), tag.getInt(ExosuitLogic.SECOND_CHARGE));
    }

    public static void write(ItemStack stack, ExosuitLogic.Piece piece) {
        CompoundTag tag = stack.getOrCreateTag();
        put(tag, ExosuitLogic.FIRST, ExosuitLogic.FIRST_CHARGE, piece.first(), piece.firstCharge());
        put(tag, ExosuitLogic.SECOND, ExosuitLogic.SECOND_CHARGE, piece.second(), piece.secondCharge());
    }

    /** Installs {@code module}. Returns whether the slot accepted it. */
    public static boolean install(ItemStack stack, String module, int charge) {
        ExosuitLogic.Piece piece = piece(stack);
        ExosuitLogic.Piece next = ExosuitLogic.install(piece, module, charge);
        if (next == piece) {
            return false;
        }
        write(stack, next);
        return true;
    }

    private static void put(CompoundTag tag, String moduleKey, String chargeKey, String module, int charge) {
        if (module == null || module.isEmpty()) {
            tag.remove(moduleKey);
            tag.remove(chargeKey);
            return;
        }
        tag.putString(moduleKey, module);
        tag.putInt(chargeKey, charge);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ExosuitLogic.Removed removed = ExosuitLogic.removeLast(piece(stack));
            if (removed != null) {
                if (!level.isClientSide()) {
                    write(stack, removed.piece());
                    give(player, removed.module(), removed.charge());
                    player.displayClientMessage(
                            Component.translatable("chat.grindless.exosuit.removed"), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        return super.use(level, player, hand);
    }

    private static void give(Player player, String module, int charge) {
        Item item = itemFor(module);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (HarnessLogic.FLUX_CELL.equals(module)) {
            stack.getOrCreateTag().putInt(HarnessLogic.CHARGE, charge);
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static Item itemFor(String module) {
        if (HarnessLogic.FLUX_CELL.equals(module)) {
            return ModItems.FLUX_CELL.get();
        }
        if (ExosuitLogic.NETWORK_TAP.equals(module)) {
            return ModItems.NETWORK_TAP.get();
        }
        if (ExosuitLogic.EXOSKELETON.equals(module)) {
            return ModItems.EXOSKELETON_LEGS.get();
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ExosuitLogic.Piece piece = piece(stack);
        tooltip.add(Component.translatable("tooltip.grindless.exosuit.slots",
                ExosuitLogic.SLOTS - piece.freeSlots(), ExosuitLogic.SLOTS));
        if (piece.has(ExosuitLogic.NETWORK_TAP)) {
            tooltip.add(Component.translatable("tooltip.grindless.exosuit.tap"));
        }
        if (piece.has(ExosuitLogic.EXOSKELETON)) {
            tooltip.add(Component.translatable("tooltip.grindless.exosuit.legs"));
        }
    }
}
