package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

/** Marks two corners and writes a Blueprint (ADR-0085). */
public final class BlueprintToolItem extends Item {

    private static final String KEY_SET = "AnchorSet";
    private static final String KEY_X = "AnchorX";
    private static final String KEY_Y = "AnchorY";
    private static final String KEY_Z = "AnchorZ";
    private static final String KEY_DIM = "AnchorDim";

    public BlueprintToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.blueprint_tool"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            clear(stack);
            player.displayClientMessage(Component.translatable("chat.grindless.blueprint.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        String dimension = level.dimension().location().toString();
        if (!anchorSet(stack)) {
            setAnchor(stack, clicked, dimension);
            player.displayClientMessage(Component.translatable("chat.grindless.blueprint.corner",
                    clicked.getX(), clicked.getY(), clicked.getZ()), true);
            return InteractionResult.CONSUME;
        }
        if (!dimension.equals(anchorDim(stack))) {
            clear(stack);
            player.displayClientMessage(Component.translatable("chat.grindless.blueprint.dimension"), true);
            return InteractionResult.FAIL;
        }
        BlockPos first = anchor(stack);
        int minX = Math.min(first.getX(), clicked.getX());
        int minY = Math.min(first.getY(), clicked.getY());
        int minZ = Math.min(first.getZ(), clicked.getZ());
        int maxX = Math.max(first.getX(), clicked.getX());
        int maxY = Math.max(first.getY(), clicked.getY());
        int maxZ = Math.max(first.getZ(), clicked.getZ());
        clear(stack);
        if (!BlueprintLogic.edgeOk(minX, maxX) || !BlueprintLogic.edgeOk(minY, maxY)
                || !BlueprintLogic.edgeOk(minZ, maxZ)) {
            player.displayClientMessage(Component.translatable("chat.grindless.blueprint.too_big"), true);
            return InteractionResult.FAIL;
        }
        List<BlueprintLogic.Piece> pieces = capture(level, minX, minY, minZ, maxX, maxY, maxZ);
        if (!BlueprintLogic.countOk(pieces.size())) {
            player.displayClientMessage(Component.translatable(pieces.isEmpty()
                    ? "chat.grindless.blueprint.empty"
                    : "chat.grindless.blueprint.too_big"), true);
            return InteractionResult.FAIL;
        }
        ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT.get());
        BlueprintItem.write(blueprint, pieces);
        if (!player.getInventory().add(blueprint)) {
            player.drop(blueprint, false);
        }
        player.displayClientMessage(Component.translatable("chat.grindless.blueprint.captured",
                pieces.size()), true);
        return InteractionResult.CONSUME;
    }

    private static List<BlueprintLogic.Piece> capture(Level level, int minX, int minY, int minZ,
                                                      int maxX, int maxY, int maxZ) {
        List<BlueprintLogic.Piece> pieces = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    Item item = state.getBlock().asItem();
                    if (item == Items.AIR) {
                        continue;
                    }
                    String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                    pieces.add(new BlueprintLogic.Piece(
                            x - minX, y - minY, z - minZ, blockId, itemId, facingOf(state)));
                    if (pieces.size() > BlueprintLogic.MAX_PIECES) {
                        return pieces;
                    }
                }
            }
        }
        return pieces;
    }

    private static String facingOf(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("facing");
        if (property == null) {
            return "";
        }
        return BlueprintLogic.keptProperty(property.getName(), state.getValue(property).toString());
    }

    private static boolean anchorSet(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(KEY_SET);
    }

    private static void setAnchor(ItemStack stack, BlockPos pos, String dimension) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(KEY_SET, true);
        tag.putInt(KEY_X, pos.getX());
        tag.putInt(KEY_Y, pos.getY());
        tag.putInt(KEY_Z, pos.getZ());
        tag.putString(KEY_DIM, dimension);
    }

    private static BlockPos anchor(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return new BlockPos(tag.getInt(KEY_X), tag.getInt(KEY_Y), tag.getInt(KEY_Z));
    }

    private static String anchorDim(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(KEY_DIM);
    }

    private static void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(KEY_SET);
        tag.remove(KEY_X);
        tag.remove(KEY_Y);
        tag.remove(KEY_Z);
        tag.remove(KEY_DIM);
    }
}
