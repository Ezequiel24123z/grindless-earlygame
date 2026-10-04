package io.github.ezequiel24123z.grindless.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A captured layout. Using it stamps that layout from the inventory (ADR-0085). */
public final class BlueprintItem extends Item {

    private static final String KEY_BLOCKS = "Blocks";

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    public static void write(ItemStack stack, List<BlueprintLogic.Piece> pieces) {
        ListTag list = new ListTag();
        for (BlueprintLogic.Piece piece : pieces) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", piece.x());
            tag.putInt("Y", piece.y());
            tag.putInt("Z", piece.z());
            tag.putString("Id", piece.blockId());
            tag.putString("Item", piece.itemId());
            if (piece.facing() != null && !piece.facing().isBlank()) {
                tag.putString("Facing", piece.facing());
            }
            list.add(tag);
        }
        stack.getOrCreateTag().put(KEY_BLOCKS, list);
    }

    public static List<BlueprintLogic.Piece> read(ItemStack stack) {
        List<BlueprintLogic.Piece> pieces = new ArrayList<>();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(KEY_BLOCKS, Tag.TAG_LIST)) {
            return pieces;
        }
        ListTag list = root.getList(KEY_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            pieces.add(new BlueprintLogic.Piece(
                    tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"),
                    tag.getString("Id"), tag.getString("Item"), tag.getString("Facing")));
        }
        return pieces;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.blueprint", read(stack).size()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        List<BlueprintLogic.Piece> pieces = read(context.getItemInHand());
        if (!BlueprintLogic.countOk(pieces.size())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat.grindless.blueprint.empty_plan"), true);
            }
            return InteractionResult.FAIL;
        }
        BlockPos anchor = context.getClickedPos().relative(context.getClickedFace());
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        List<BlockPos> targets = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (BlueprintLogic.Piece piece : pieces) {
            Block block = blockOf(piece.blockId());
            if (block == null) {
                player.displayClientMessage(Component.translatable("chat.grindless.blueprint.missing",
                        piece.blockId()), true);
                return InteractionResult.FAIL;
            }
            BlockPos at = anchor.offset(piece.x(), piece.y(), piece.z());
            if (!level.getBlockState(at).canBeReplaced()) {
                player.displayClientMessage(Component.translatable("chat.grindless.blueprint.blocked"), true);
                return InteractionResult.FAIL;
            }
            targets.add(at);
            states.add(withFacing(block.defaultBlockState(), piece.facing()));
        }
        boolean creative = player.getAbilities().instabuild;
        Map<String, Integer> need = BlueprintLogic.cost(pieces);
        if (!creative) {
            Map<String, Integer> have = new LinkedHashMap<>();
            for (String itemId : need.keySet()) {
                have.put(itemId, count(player, itemId));
            }
            Map<String, Integer> missing = BlueprintLogic.shortfall(need, have);
            if (!missing.isEmpty()) {
                Map.Entry<String, Integer> first = missing.entrySet().iterator().next();
                player.displayClientMessage(Component.translatable("chat.grindless.blueprint.missing",
                        first.getValue() + " " + first.getKey()), true);
                return InteractionResult.FAIL;
            }
        }
        for (int i = 0; i < targets.size(); i++) {
            level.setBlock(targets.get(i), states.get(i), Block.UPDATE_ALL);
        }
        if (!creative) {
            for (Map.Entry<String, Integer> entry : need.entrySet()) {
                shrink(player, entry.getKey(), entry.getValue());
            }
        }
        player.displayClientMessage(Component.translatable("chat.grindless.blueprint.stamped",
                pieces.size()), true);
        return InteractionResult.CONSUME;
    }

    private static BlockState withFacing(BlockState state, String facing) {
        if (facing == null || facing.isBlank()) {
            return state;
        }
        Direction direction = Direction.byName(facing);
        if (direction == null) {
            return state;
        }
        Property<?> property = state.getBlock().getStateDefinition().getProperty("facing");
        if (property == null || property.getValueClass() != Direction.class) {
            return state;
        }
        if (!property.getPossibleValues().contains(direction)) {
            return state;
        }
        @SuppressWarnings("unchecked")
        Property<Direction> facingProperty = (Property<Direction>) property;
        return state.setValue(facingProperty, direction);
    }

    private static Block blockOf(String id) {
        ResourceLocation key;
        try {
            key = new ResourceLocation(id);
        } catch (RuntimeException ex) {
            return null;
        }
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(key);
    }

    private static int count(Player player, String itemId) {
        int total = 0;
        for (ItemStack stack : pockets(player)) {
            if (!stack.isEmpty() && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void shrink(Player player, String itemId, int amount) {
        int left = amount;
        for (ItemStack stack : pockets(player)) {
            if (left <= 0) {
                break;
            }
            if (stack.isEmpty() || !itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                continue;
            }
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
    }

    /** Main inventory and the offhand. Worn armour is not building material. */
    private static List<ItemStack> pockets(Player player) {
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().items);
        stacks.add(player.getOffhandItem());
        return stacks;
    }
}
