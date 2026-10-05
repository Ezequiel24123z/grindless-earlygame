package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The suit's buffer. Not the drill's fuel, and not a generator (ADR-0084, ADR-0102).
 */
public final class FluxCellItem extends Item {

    public FluxCellItem(Properties properties) {
        super(properties);
    }

    public static int charge(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return HarnessLogic.clamp(tag == null ? 0 : tag.getInt(HarnessLogic.CHARGE));
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt(HarnessLogic.CHARGE, HarnessLogic.clamp(charge));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!chargesFrom(state.getBlock())) {
            return InteractionResult.PASS;
        }
        ItemStack cell = context.getItemInHand();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        int stored = charge(cell);
        int room = HarnessLogic.accepted(stored, HarnessLogic.CELL_CAPACITY);
        if (room <= 0) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("chat.grindless.flux_cell.full"), true);
            }
            return InteractionResult.CONSUME;
        }
        int drawn = draw(level, context.getClickedPos(), room);
        if (drawn <= 0) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("chat.grindless.flux_cell.empty"), true);
            }
            return InteractionResult.CONSUME;
        }
        setCharge(cell, HarnessLogic.afterCharge(stored, drawn));
        if (player != null) {
            player.displayClientMessage(Component.translatable("chat.grindless.flux_cell.charged",
                    charge(cell), HarnessLogic.CELL_CAPACITY), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cell = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(other(hand));
        if (other.getItem() instanceof ExosuitItem) {
            return installExosuit(level, player, cell, other);
        }
        if (!(other.getItem() instanceof HarnessItem)) {
            return InteractionResultHolder.pass(cell);
        }
        HarnessLogic.Piece piece = HarnessItem.piece(other);
        HarnessLogic.Piece next = HarnessLogic.install(piece, charge(cell));
        if (next == piece) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat.grindless.harness.full"), true);
            }
            return InteractionResultHolder.fail(cell);
        }
        if (!level.isClientSide()) {
            HarnessItem.write(other, next);
            cell.shrink(1);
            player.displayClientMessage(Component.translatable("chat.grindless.harness.installed",
                    next.charge(), HarnessLogic.CELL_CAPACITY), true);
        }
        return InteractionResultHolder.sidedSuccess(cell, level.isClientSide());
    }

    private static InteractionResultHolder<ItemStack> installExosuit(Level level, Player player,
                                                                     ItemStack cell, ItemStack suit) {
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(cell, true);
        }
        if (!ExosuitItem.install(suit, HarnessLogic.FLUX_CELL, charge(cell))) {
            player.displayClientMessage(Component.translatable("chat.grindless.exosuit.full"), true);
            return InteractionResultHolder.fail(cell);
        }
        cell.shrink(1);
        player.displayClientMessage(Component.translatable("chat.grindless.exosuit.installed"), true);
        return InteractionResultHolder.sidedSuccess(cell, false);
    }

    private static int draw(Level level, BlockPos pos, int room) {
        if (!(level instanceof ServerLevel server) || room <= 0) {
            return 0;
        }
        FluxNetwork network = FluxNetworkData.get(server).networkCovering(pos);
        if (network == null) {
            return 0;
        }
        long drawn = network.extract(room, false);
        if (drawn > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) drawn;
    }

    private static boolean chargesFrom(Block block) {
        return block == ModBlocks.CAPACITOR_BANK.get() || block == ModBlocks.FLUX_TRANSFORMER.get();
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charge(stack) / HarnessLogic.CELL_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFB300;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.flux_cell",
                charge(stack), HarnessLogic.CELL_CAPACITY));
    }
}
