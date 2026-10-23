package io.github.ezequiel24123z.grindless.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/** A captured layout. Using it stamps that layout from the inventory (ADR-0085). */
public final class BlueprintItem extends Item {

    private static final String KEY_BLOCKS = "Blocks";

    interface WorldView {
        boolean inBounds(BlockPos position);

        boolean loaded(BlockPos position);

        boolean mayBuild(BlockPos position);

        BlockState state(BlockPos position);
    }

    record Decoded(List<BlueprintLogic.Piece> pieces, BlueprintLogic.Failure failure) {
        Decoded {
            pieces = List.copyOf(pieces);
        }
    }

    record Placement(BlueprintLogic.Piece piece, BlockPos position, BlockState state) {
        Placement {
            position = position.immutable();
        }
    }

    record StampPlan(BlueprintLogic.Plan diagnosis, List<Placement> placements) {
        StampPlan {
            placements = List.copyOf(placements);
        }
    }

    record StampResult(int placed, BlueprintLogic.Plan diagnosis) {
    }

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
        Decoded decoded = decode(stack);
        if (decoded.failure() != BlueprintLogic.Failure.NONE) {
            throw new IllegalArgumentException("Invalid blueprint payload: " + decoded.failure());
        }
        return decoded.pieces();
    }

    static Decoded decode(ItemStack stack) {
        List<BlueprintLogic.Piece> pieces = new ArrayList<>();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(KEY_BLOCKS)) {
            return new Decoded(pieces, BlueprintLogic.Failure.NONE);
        }
        if (!(root.get(KEY_BLOCKS) instanceof ListTag list)) {
            return new Decoded(List.of(), BlueprintLogic.Failure.MALFORMED);
        }
        if (list.size() > BlueprintLogic.MAX_PIECES) {
            return new Decoded(List.of(), BlueprintLogic.Failure.TOO_LARGE);
        }
        if (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) {
            return new Decoded(List.of(), BlueprintLogic.Failure.MALFORMED);
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.contains("X", Tag.TAG_INT) || !tag.contains("Y", Tag.TAG_INT)
                    || !tag.contains("Z", Tag.TAG_INT) || !tag.contains("Id", Tag.TAG_STRING)
                    || !tag.contains("Item", Tag.TAG_STRING)
                    || (tag.contains("Facing") && !tag.contains("Facing", Tag.TAG_STRING))) {
                return new Decoded(List.of(), BlueprintLogic.Failure.MALFORMED);
            }
            pieces.add(new BlueprintLogic.Piece(
                    tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"),
                    tag.getString("Id"), tag.getString("Item"), tag.getString("Facing")));
        }
        return new Decoded(pieces, BlueprintLogic.Failure.NONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Decoded decoded = decode(stack);
        if (decoded.failure() != BlueprintLogic.Failure.NONE) {
            tooltip.add(Component.translatable("chat.grindless.blueprint.invalid_plan"));
        } else {
            tooltip.add(Component.translatable("tooltip.grindless.blueprint", decoded.pieces().size()));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos anchor = context.getClickedPos().relative(context.getClickedFace());
        WorldView view = view(context);
        boolean creative = player.getAbilities().instabuild;
        StampPlan plan = preflight(decode(context.getItemInHand()), anchor, view,
                contents(player.getInventory()), creative);
        if (!plan.diagnosis().ready()) {
            report(player, plan.diagnosis(), anchor, false);
            return InteractionResult.FAIL;
        }
        StampResult result;
        boolean returned = false;
        try {
            result = stamp(plan, view, player.getInventory(), creative,
                    (position, state) -> level.setBlock(position, state, Block.UPDATE_ALL),
                    unused -> {
                        if (!player.getInventory().add(unused)) {
                            player.drop(unused, false);
                        }
                    });
            returned = true;
        } finally {
            if (!returned) {
                player.displayClientMessage(Component.translatable(
                        "chat.grindless.blueprint.interrupted_unknown"), true);
            }
        }
        if (!result.diagnosis().ready()) {
            report(player, result.diagnosis(), anchor, false);
            if (result.placed() > 0) {
                player.displayClientMessage(Component.translatable("chat.grindless.blueprint.interrupted",
                        result.placed(), plan.placements().size()), false);
            }
            return InteractionResult.FAIL;
        }
        player.displayClientMessage(Component.translatable("chat.grindless.blueprint.stamped",
                result.placed()), true);
        return InteractionResult.CONSUME;
    }

    static StampPlan preflight(Decoded decoded, BlockPos anchor, WorldView view,
                               Map<String, Integer> available, boolean creative) {
        if (decoded.failure() != BlueprintLogic.Failure.NONE) {
            return new StampPlan(BlueprintLogic.rejected(decoded.failure(), null), List.of());
        }
        BlockPos origin = anchor.immutable();
        List<Placement> placements = new ArrayList<>();
        BlueprintLogic.Plan diagnosis = BlueprintLogic.preflight(decoded.pieces(), available, creative, piece -> {
            BlueprintLogic.Failure failure = pieceProblem(piece);
            if (failure != BlueprintLogic.Failure.NONE) {
                return failure;
            }
            if (!targetFits(origin, piece)) {
                return BlueprintLogic.Failure.OUTSIDE_WORLD;
            }
            BlockPos position = origin.offset(piece.x(), piece.y(), piece.z());
            failure = destinationProblem(view, position);
            if (failure == BlueprintLogic.Failure.NONE) {
                BlockState state = withFacing(blockOf(piece.blockId()).defaultBlockState(), piece.facing());
                placements.add(new Placement(piece, position, state));
            }
            return failure;
        });
        return new StampPlan(diagnosis, placements);
    }

    static BlueprintLogic.Failure pieceProblem(BlueprintLogic.Piece piece) {
        Block block = blockOf(piece.blockId());
        if (block == null) {
            return BlueprintLogic.Failure.UNKNOWN_BLOCK;
        }
        ResourceLocation itemKey = ResourceLocation.tryParse(piece.itemId());
        if (itemKey == null || !BuiltInRegistries.ITEM.containsKey(itemKey)
                || block.asItem() == Items.AIR || BuiltInRegistries.ITEM.get(itemKey) != block.asItem()
                || !piece.itemId().equals(itemKey.toString())) {
            return BlueprintLogic.Failure.INVALID_ITEM;
        }
        return withFacing(block.defaultBlockState(), piece.facing()) == null
                ? BlueprintLogic.Failure.INVALID_FACING : BlueprintLogic.Failure.NONE;
    }

    static BlueprintLogic.Failure readable(WorldView view, BlockPos position) {
        if (!view.inBounds(position)) {
            return BlueprintLogic.Failure.OUTSIDE_WORLD;
        }
        if (!view.loaded(position)) {
            return BlueprintLogic.Failure.UNLOADED;
        }
        return BlueprintLogic.Failure.NONE;
    }

    private static BlueprintLogic.Failure destinationProblem(WorldView view, BlockPos position) {
        BlueprintLogic.Failure failure = readable(view, position);
        if (failure != BlueprintLogic.Failure.NONE) {
            return failure;
        }
        if (!view.mayBuild(position)) {
            return BlueprintLogic.Failure.FORBIDDEN;
        }
        failure = readable(view, position);
        if (failure != BlueprintLogic.Failure.NONE) {
            return failure;
        }
        return view.state(position).canBeReplaced()
                ? BlueprintLogic.Failure.NONE : BlueprintLogic.Failure.BLOCKED;
    }

    private static boolean targetFits(BlockPos anchor, BlueprintLogic.Piece piece) {
        return (long) anchor.getX() + piece.x() <= Integer.MAX_VALUE
                && (long) anchor.getY() + piece.y() <= Integer.MAX_VALUE
                && (long) anchor.getZ() + piece.z() <= Integer.MAX_VALUE;
    }

    static WorldView view(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        return new WorldView() {
            @Override
            public boolean inBounds(BlockPos position) {
                return level.isInWorldBounds(position) && level.getWorldBorder().isWithinBounds(position);
            }

            @Override
            public boolean loaded(BlockPos position) {
                return level.hasChunkAt(position);
            }

            @Override
            public boolean mayBuild(BlockPos position) {
                return player != null && level.mayInteract(player, position)
                        && player.mayInteract(level, position)
                        && player.mayUseItemAt(position, context.getClickedFace(), context.getItemInHand());
            }

            @Override
            public BlockState state(BlockPos position) {
                return level.getBlockState(position);
            }
        };
    }

    static StampResult stamp(StampPlan plan, WorldView view, Inventory inventory, boolean creative,
                              BiPredicate<BlockPos, BlockState> place, Consumer<ItemStack> refund) {
        BlueprintLogic.Plan diagnosis = plan.diagnosis();
        if (!diagnosis.ready()) {
            return new StampResult(0, diagnosis);
        }
        Optional<Payment> payment = creative ? Optional.empty() : Payment.reserve(inventory, diagnosis.required());
        if (!creative && payment.isEmpty()) {
            Map<String, Integer> missing = BlueprintLogic.shortfall(diagnosis.required(), contents(inventory));
            return new StampResult(0, new BlueprintLogic.Plan(diagnosis.pieces(),
                    diagnosis.required(), missing, List.of(
                            new BlueprintLogic.Problem(BlueprintLogic.Failure.MATERIALS_CHANGED, null))));
        }
        int placed = 0;
        try {
            for (Placement placement : plan.placements()) {
                BlueprintLogic.Failure failure = destinationProblem(view, placement.position());
                if (failure != BlueprintLogic.Failure.NONE) {
                    return stopped(placed, diagnosis, failure, placement.piece());
                }
                ItemStack paid = creative ? ItemStack.EMPTY : payment.orElseThrow().take(placement.piece().itemId());
                // A throwing callback may already have changed the cell or emitted drops.
                // Keep that attempt paid; only an explicit false result can safely refund it.
                if (!place.test(placement.position(), placement.state())) {
                    if (!creative) {
                        payment.orElseThrow().restore(paid);
                    }
                    return stopped(placed, diagnosis, BlueprintLogic.Failure.PLACEMENT_FAILED, placement.piece());
                }
                placed++;
            }
            return new StampResult(placed, diagnosis);
        } finally {
            payment.ifPresent(reserved -> reserved.refund(refund));
        }
    }

    private static StampResult stopped(int placed, BlueprintLogic.Plan diagnosis,
                                        BlueprintLogic.Failure failure, BlueprintLogic.Piece piece) {
        return new StampResult(placed, new BlueprintLogic.Plan(diagnosis.pieces(), diagnosis.required(),
                Map.of(), List.of(new BlueprintLogic.Problem(failure, piece))));
    }

    private static BlockState withFacing(BlockState state, String facing) {
        if (facing == null || facing.isBlank()) {
            return state;
        }
        Direction direction = Direction.byName(facing);
        if (direction == null) {
            return null;
        }
        Property<?> property = state.getBlock().getStateDefinition().getProperty("facing");
        if (property == null || property.getValueClass() != Direction.class) {
            return null;
        }
        if (!property.getPossibleValues().contains(direction)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Property<Direction> facingProperty = (Property<Direction>) property;
        return state.setValue(facingProperty, direction);
    }

    private static Block blockOf(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(key);
    }

    static Map<String, Integer> contents(Inventory inventory) {
        Map<String, Integer> have = new LinkedHashMap<>();
        for (ItemStack stack : pockets(inventory)) {
            if (eligibleMaterial(stack)) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                have.merge(itemId, Math.min(BlueprintLogic.MAX_PIECES, stack.getCount()),
                        (first, second) -> Math.min(BlueprintLogic.MAX_PIECES, first + second));
            }
        }
        return have;
    }

    /** Relocated machines retain their stores and upgrades; a stamp only applies id/facing. */
    static boolean eligibleMaterial(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getTag() == null || !stack.getTag().contains(BlockItem.BLOCK_ENTITY_TAG));
    }

    private static final class Payment {
        private final List<ItemStack> reserved = new ArrayList<>();

        static Optional<Payment> reserve(Inventory inventory, Map<String, Integer> required) {
            List<ItemStack> live = pockets(inventory);
            List<ItemStack> before = new ArrayList<>();
            List<ItemStack> after = new ArrayList<>();
            for (ItemStack stack : live) {
                ItemStack snapshot = stack.copy();
                before.add(snapshot);
                after.add(snapshot.copy());
            }
            Payment payment = new Payment();
            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                int left = entry.getValue();
                for (ItemStack stack : after) {
                    if (left == 0) {
                        break;
                    }
                    if (eligibleMaterial(stack)
                            && entry.getKey().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                        int take = Math.min(left, stack.getCount());
                        payment.reserved.add(stack.split(take));
                        left -= take;
                    }
                }
                if (left != 0) {
                    return Optional.empty();
                }
            }
            List<ItemStack> current = pockets(inventory);
            for (int slot = 0; slot < before.size(); slot++) {
                if (current.get(slot) != live.get(slot) || !ItemStack.matches(current.get(slot), before.get(slot))) {
                    return Optional.empty();
                }
            }
            for (int slot = 0; slot < after.size(); slot++) {
                if (!ItemStack.matches(before.get(slot), after.get(slot))) {
                    int inventorySlot = slot < inventory.items.size() ? slot : slot + inventory.armor.size();
                    inventory.setItem(inventorySlot, after.get(slot));
                }
            }
            inventory.setChanged();
            return Optional.of(payment);
        }

        ItemStack take(String itemId) {
            for (ItemStack stack : reserved) {
                if (!stack.isEmpty() && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                    return stack.split(1);
                }
            }
            throw new IllegalStateException("Blueprint reservation lost material " + itemId);
        }

        void restore(ItemStack stack) {
            reserved.add(stack);
        }

        void refund(Consumer<ItemStack> refund) {
            for (ItemStack stack : reserved) {
                if (!stack.isEmpty()) {
                    refund.accept(stack);
                }
            }
        }
    }

    /** Main inventory and the offhand. Worn armour is not building material. */
    private static List<ItemStack> pockets(Inventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.items);
        stacks.addAll(inventory.offhand);
        return stacks;
    }

    static void report(Player player, BlueprintLogic.Plan diagnosis, BlockPos anchor, boolean capture) {
        MutableComponent message = Component.empty();
        if (!diagnosis.problems().isEmpty()) {
            BlueprintLogic.Problem problem = diagnosis.problems().get(0);
            BlueprintLogic.Piece piece = problem.piece();
            long x = piece == null ? anchor.getX() : (long) anchor.getX() + piece.x();
            long y = piece == null ? anchor.getY() : (long) anchor.getY() + piece.y();
            long z = piece == null ? anchor.getZ() : (long) anchor.getZ() + piece.z();
            message.append(switch (problem.failure()) {
                case EMPTY -> Component.translatable(capture
                        ? "chat.grindless.blueprint.empty" : "chat.grindless.blueprint.empty_plan");
                case TOO_LARGE -> Component.translatable("chat.grindless.blueprint.too_big");
                case DUPLICATE -> Component.translatable("chat.grindless.blueprint.duplicate", x, y, z);
                case UNKNOWN_BLOCK -> Component.translatable("chat.grindless.blueprint.unknown_block", piece.blockId());
                case INVALID_ITEM -> Component.translatable("chat.grindless.blueprint.invalid_item",
                        piece.itemId(), piece.blockId());
                case INVALID_FACING -> Component.translatable("chat.grindless.blueprint.invalid_facing",
                        piece.facing(), piece.blockId());
                case OUTSIDE_WORLD -> Component.translatable("chat.grindless.blueprint.outside_world", x, y, z);
                case UNLOADED -> Component.translatable("chat.grindless.blueprint.unloaded", x, y, z);
                case FORBIDDEN -> Component.translatable("chat.grindless.blueprint.forbidden", x, y, z);
                case BLOCKED -> Component.translatable("chat.grindless.blueprint.blocked_at", x, y, z);
                case MATERIALS_CHANGED -> Component.translatable("chat.grindless.blueprint.materials_changed");
                case PLACEMENT_FAILED -> Component.translatable("chat.grindless.blueprint.placement_failed", x, y, z);
                default -> Component.translatable("chat.grindless.blueprint.invalid_plan");
            });
        }
        if (!diagnosis.missing().isEmpty()) {
            MutableComponent materials = Component.empty();
            for (Map.Entry<String, Integer> entry : diagnosis.missing().entrySet()) {
                if (!materials.getSiblings().isEmpty()) {
                    materials.append(", ");
                }
                ResourceLocation key = ResourceLocation.tryParse(entry.getKey());
                Component name = key != null && BuiltInRegistries.ITEM.containsKey(key)
                        ? BuiltInRegistries.ITEM.get(key).getDescription() : Component.literal(entry.getKey());
                materials.append(Component.literal(entry.getValue() + " ")).append(name);
            }
            if (!diagnosis.problems().isEmpty()) {
                message.append(". ");
            }
            message.append(Component.translatable("chat.grindless.blueprint.missing_all", materials));
        }
        player.displayClientMessage(message, true);
    }
}
