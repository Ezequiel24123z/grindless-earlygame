package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mines with charge. Sneak-use cycles the mode. A Drill Cell in the other hand fills it
 * (ADR-0084).
 */
public final class FluxDrillItem extends DiggerItem {

    private static final String KEY_CHARGE = "Charge";
    private static final String KEY_MODE = "Mode";

    private static final Tier DRILL = new Tier() {
        @Override
        public int getUses() {
            return 0;
        }

        @Override
        public float getSpeed() {
            return DrillLogic.SPEED;
        }

        @Override
        public float getAttackDamageBonus() {
            return 1.0F;
        }

        @Override
        public int getLevel() {
            return DrillLogic.HARVEST_LEVEL;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public FluxDrillItem(Properties properties) {
        super(1.0F, -2.8F, DRILL, BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }

    public static int charge(ItemStack stack) {
        return stack.getOrCreateTag().getInt(KEY_CHARGE);
    }

    public static void setCharge(ItemStack stack, int stored) {
        stack.getOrCreateTag().putInt(KEY_CHARGE, Math.max(0, stored));
    }

    public static DrillLogic.Mode mode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return DrillLogic.parse(tag == null ? "" : tag.getString(KEY_MODE));
    }

    public static void setMode(ItemStack stack, DrillLogic.Mode mode) {
        stack.getOrCreateTag().putString(KEY_MODE, mode.name());
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!DrillLogic.canStart(charge(stack))) {
            return 0.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charge(stack) / DrillLogic.CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00E5FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.flux_drill.charge",
                charge(stack), DrillLogic.CAPACITY));
        tooltip.add(Component.translatable("tooltip.grindless.flux_drill.mode",
                Component.translatable(modeKey(mode(stack)))));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            DrillLogic.Mode next = mode(stack).next();
            setMode(stack, next);
            player.displayClientMessage(Component.translatable("chat.grindless.flux_drill.mode",
                    Component.translatable(modeKey(next))), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
                             LivingEntity miner) {
        if (level.isClientSide() || !(miner instanceof Player player)) {
            return true;
        }
        int before = charge(stack);
        int charge = DrillLogic.afterBlock(before);
        setCharge(stack, charge);
        DrillLogic.Mode mode = mode(stack);
        List<DrillLogic.Cube> extras = switch (mode) {
            case SINGLE -> List.of();
            case AREA -> DrillLogic.area(look(player));
            case VEIN -> DrillLogic.vein(pos.getX(), pos.getY(), pos.getZ(),
                    Math.min(DrillLogic.VEIN_CAP, before / DrillLogic.BLOCK_FU),
                    (x, y, z) -> sameVein(level, state, stack, x, y, z));
            case TUNNEL -> DrillLogic.tunnel(player.getDirection());
        };
        for (DrillLogic.Cube cube : extras) {
            if (!DrillLogic.canStart(charge)) {
                break;
            }
            if (cube.x() == 0 && cube.y() == 0 && cube.z() == 0) {
                continue;
            }
            BlockPos at = pos.offset(cube.x(), cube.y(), cube.z());
            BlockState there = level.getBlockState(at);
            if (!harvestable(level, there, stack, at)) {
                continue;
            }
            if (level.destroyBlock(at, true, player)) {
                charge = DrillLogic.afterBlock(charge);
                setCharge(stack, charge);
            }
        }
        return true;
    }

    private static boolean sameVein(Level level, BlockState origin, ItemStack stack,
                                    int x, int y, int z) {
        BlockPos at = new BlockPos(x, y, z);
        BlockState there = level.getBlockState(at);
        return there.is(origin.getBlock()) && harvestable(level, there, stack, at);
    }

    private static boolean harvestable(Level level, BlockState state, ItemStack stack, BlockPos pos) {
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        boolean own = Grindless.MOD_ID.equals(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
        return DrillLogic.breaks(
                state.is(BlockTags.MINEABLE_WITH_PICKAXE),
                stack.getItem() instanceof FluxDrillItem drill && drill.isCorrectToolForDrops(state),
                own);
    }

    private static Direction look(Player player) {
        Vec3 look = player.getLookAngle();
        return Direction.getNearest(look.x, look.y, look.z);
    }

    private static String modeKey(DrillLogic.Mode mode) {
        return "chat.grindless.flux_drill.mode." + mode.name().toLowerCase(java.util.Locale.ROOT);
    }
}
