package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import io.github.ezequiel24123z.grindless.network.ManualLink;
import io.github.ezequiel24123z.grindless.network.PylonStructure;
import io.github.ezequiel24123z.grindless.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
 * Handheld pylon linker. Right-click two pylons to join them across any distance (ADR-0064).
 *
 * <p>This is a tool, not a block and not a cable. The interesting decision is the trunk between
 * two bases; connecting adjacent machines is what pylons already did.
 */
public final class FluxConduitItem extends Item {

    private static final String KEY_POS = "LinkPos";
    private static final String KEY_DIM = "LinkDim";

    public FluxConduitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos base = PylonStructure.resolveBase(level, context.getClickedPos());
        if (base == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server) || player == null) {
            return InteractionResult.CONSUME;
        }
        if (!hasMark(stack)) {
            mark(stack, server, base);
            player.displayClientMessage(Component.translatable("chat.grindless.conduit.marked",
                    base.getX(), base.getY(), base.getZ()), true);
            return InteractionResult.CONSUME;
        }
        ResourceLocation dim = dimension(stack);
        if (dim != null && !dim.equals(server.dimension().location())) {
            player.displayClientMessage(Component.translatable("chat.grindless.conduit.dimension"),
                    true);
            return InteractionResult.FAIL;
        }
        BlockPos first = markedPos(stack);
        if (first == null) {
            clearMark(stack);
            return InteractionResult.FAIL;
        }
        if (first.equals(base)) {
            clearMark(stack);
            player.displayClientMessage(Component.translatable("chat.grindless.conduit.cleared"), true);
            return InteractionResult.CONSUME;
        }
        FluxNetworkData data = FluxNetworkData.get(server);
        if (player.isShiftKeyDown()) {
            if (data.removeManualLink(first, base)) {
                clearMark(stack);
                player.displayClientMessage(Component.translatable("chat.grindless.conduit.unlinked"),
                        true);
                server.playSound(null, base, ModSounds.RELAY_CLICK.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("chat.grindless.conduit.missing"), true);
            return InteractionResult.FAIL;
        }
        if (!data.addManualLink(first, base)) {
            player.displayClientMessage(Component.translatable("chat.grindless.conduit.missing"), true);
            return InteractionResult.FAIL;
        }
        long upkeep = ManualLink.of(first, base).upkeep();
        clearMark(stack);
        player.displayClientMessage(Component.translatable("chat.grindless.conduit.linked", upkeep),
                true);
        server.playSound(null, base, ModSounds.PYLON_LINK.get(), SoundSource.PLAYERS, 0.8F, 1.1F);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && hasMark(stack)) {
            if (!level.isClientSide()) {
                clearMark(stack);
                player.displayClientMessage(Component.translatable("chat.grindless.conduit.cleared"),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        if (hasMark(stack)) {
            BlockPos pos = markedPos(stack);
            if (pos != null) {
                tooltip.add(Component.translatable("tooltip.grindless.conduit.marked",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.grindless.conduit.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean hasMark(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(KEY_POS);
    }

    private static BlockPos markedPos(ItemStack stack) {
        return hasMark(stack) ? BlockPos.of(stack.getTag().getLong(KEY_POS)) : null;
    }

    private static ResourceLocation dimension(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(KEY_DIM)) {
            return null;
        }
        return ResourceLocation.tryParse(stack.getTag().getString(KEY_DIM));
    }

    private static void mark(ItemStack stack, ServerLevel level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(KEY_POS, pos.asLong());
        tag.putString(KEY_DIM, level.dimension().location().toString());
    }

    private static void clearMark(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(KEY_POS);
        tag.remove(KEY_DIM);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }
}
