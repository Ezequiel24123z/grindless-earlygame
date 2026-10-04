package io.github.ezequiel24123z.grindless.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Marks a box to tear down later. It does not break blocks and it is not a wrench
 * (ADR-0086).
 */
public final class DeconstructionPlannerItem extends Item {

    private static final String KEY_SET = "MarkSet";
    private static final String KEY_X0 = "X0";
    private static final String KEY_Y0 = "Y0";
    private static final String KEY_Z0 = "Z0";
    private static final String KEY_X1 = "X1";
    private static final String KEY_Y1 = "Y1";
    private static final String KEY_Z1 = "Z1";
    private static final String KEY_DIM = "Dim";
    private static final String KEY_PENDING = "Pending";

    public DeconstructionPlannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (markComplete(stack)) {
            tooltip.add(Component.translatable("tooltip.grindless.planner.marked", volume(stack)));
        } else {
            tooltip.add(Component.translatable("tooltip.grindless.planner"));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            clear(stack);
            player.displayClientMessage(Component.translatable("chat.grindless.planner.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        String dimension = level.dimension().location().toString();
        if (!pending(stack) || !dimension.equals(dim(stack))) {
            if (pending(stack) && !dimension.equals(dim(stack))) {
                player.displayClientMessage(Component.translatable("chat.grindless.planner.dimension"), true);
            }
            setPending(stack, clicked, dimension);
            player.displayClientMessage(Component.translatable("chat.grindless.planner.corner",
                    clicked.getX(), clicked.getY(), clicked.getZ()), true);
            return InteractionResult.CONSUME;
        }
        BlockPos first = pendingPos(stack);
        int minX = Math.min(first.getX(), clicked.getX());
        int minY = Math.min(first.getY(), clicked.getY());
        int minZ = Math.min(first.getZ(), clicked.getZ());
        int maxX = Math.max(first.getX(), clicked.getX());
        int maxY = Math.max(first.getY(), clicked.getY());
        int maxZ = Math.max(first.getZ(), clicked.getZ());
        long volume = PlannerLogic.volume(minX, maxX, minY, maxY, minZ, maxZ);
        if (volume <= 0L) {
            clear(stack);
            player.displayClientMessage(Component.translatable("chat.grindless.planner.too_big"), true);
            return InteractionResult.FAIL;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(KEY_SET, true);
        tag.putBoolean(KEY_PENDING, false);
        tag.putInt(KEY_X0, minX);
        tag.putInt(KEY_Y0, minY);
        tag.putInt(KEY_Z0, minZ);
        tag.putInt(KEY_X1, maxX);
        tag.putInt(KEY_Y1, maxY);
        tag.putInt(KEY_Z1, maxZ);
        tag.putString(KEY_DIM, dimension);
        player.displayClientMessage(Component.translatable("chat.grindless.planner.marked", volume), true);
        return InteractionResult.CONSUME;
    }

    private static boolean pending(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(KEY_PENDING);
    }

    private static boolean markComplete(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(KEY_SET);
    }

    private static long volume(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0L;
        }
        return PlannerLogic.volume(
                tag.getInt(KEY_X0), tag.getInt(KEY_X1),
                tag.getInt(KEY_Y0), tag.getInt(KEY_Y1),
                tag.getInt(KEY_Z0), tag.getInt(KEY_Z1));
    }

    private static void setPending(ItemStack stack, BlockPos pos, String dimension) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(KEY_PENDING, true);
        tag.putBoolean(KEY_SET, false);
        tag.putInt(KEY_X0, pos.getX());
        tag.putInt(KEY_Y0, pos.getY());
        tag.putInt(KEY_Z0, pos.getZ());
        tag.putString(KEY_DIM, dimension);
    }

    private static BlockPos pendingPos(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return new BlockPos(tag.getInt(KEY_X0), tag.getInt(KEY_Y0), tag.getInt(KEY_Z0));
    }

    private static String dim(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(KEY_DIM);
    }

    private static void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(KEY_SET);
        tag.remove(KEY_PENDING);
        tag.remove(KEY_X0);
        tag.remove(KEY_Y0);
        tag.remove(KEY_Z0);
        tag.remove(KEY_X1);
        tag.remove(KEY_Y1);
        tag.remove(KEY_Z1);
        tag.remove(KEY_DIM);
    }
}
