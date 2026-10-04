package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.network.PylonStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

/**
 * T0 wrench. Rotates and relocates Grindless blocks; does not mine (ADR-0055, ADR-0069).
 */
public final class MultitoolItem extends Item {

    public MultitoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !player.mayInteract(level, clicked)) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = resolve(level, clicked);
        BlockState state = level.getBlockState(pos);
        if (!ours(state)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && MultitoolLogic.sneakMeansRelocate()) {
            return relocate(level, pos, state, player);
        }
        return rotate(level, pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.multitool"));
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return false;
    }

    private static InteractionResult rotate(Level level, BlockPos pos, BlockState state) {
        BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
        if (rotated == state) {
            return InteractionResult.CONSUME;
        }
        level.setBlock(pos, rotated, 3);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult relocate(Level level, BlockPos pos, BlockState state,
                                              Player player) {
        ItemStack dropped = new ItemStack(state.getBlock());
        CompoundTag states = new CompoundTag();
        for (Property<?> property : state.getProperties()) {
            states.putString(property.getName(), name(state, property));
        }
        dropped.getOrCreateTag().put("BlockStateTag", states);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            dropped.getOrCreateTag().put(BlockItem.BLOCK_ENTITY_TAG, blockEntity.saveWithoutMetadata());
        }
        Relocation.run(() -> level.removeBlock(pos, false));
        if (!player.getInventory().add(dropped)) {
            player.drop(dropped, false);
        }
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    private static BlockPos resolve(Level level, BlockPos pos) {
        BlockPos base = PylonStructure.resolveBase(level, pos);
        return base != null ? base : pos;
    }

    private static boolean ours(BlockState state) {
        return Grindless.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
