package io.github.ezequiel24123z.grindless.fluid;

import io.github.ezequiel24123z.grindless.machine.MachineEffects;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.item.MultitoolItem;
import io.github.ezequiel24123z.grindless.item.Relocation;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** T0 tank. Unpressurised, ambient only; storage survives both ordinary breaking and relocation. */
public class BasicTankBlock extends BaseEntityBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.BELT);

    public BasicTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MachineProperties.FACING, Direction.NORTH)
                .setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineProperties.FACING, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(MachineProperties.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(MachineProperties.FACING,
                rotation.rotate(state.getValue(MachineProperties.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(MachineProperties.FACING)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof MultitoolItem) {
            return InteractionResult.PASS;
        }
        if (!player.mayInteract(level, pos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof BasicTankBlockEntity tank)) {
            return InteractionResult.FAIL;
        }
        InteractionResult interaction = FluidContainerPlatform.interact(player, hand, tank, hit.getDirection());
        if (interaction != InteractionResult.PASS) {
            return interaction;
        }
        player.displayClientMessage(tank.fillFeedback(), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(BasicTankBlockEntity.feedback(FluidNbt.load(
                stack.getTagElement(BlockItem.BLOCK_ENTITY_TAG) == null ? null
                        : stack.getTagElement(BlockItem.BLOCK_ENTITY_TAG).getCompound("Fluid"))));
        tooltip.add(Component.translatable("tooltip.grindless.tank.interaction"));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder loot) {
        if (loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BasicTankBlockEntity tank
                && tank.hasStoredFluid()) {
            return List.of();
        }
        return super.getDrops(state, loot);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide() && !Relocation.active()
                && level.getBlockEntity(pos) instanceof BasicTankBlockEntity tank && tank.hasStoredFluid()) {
            tank.setRemoved();
            Block.popResource(level, pos, tank.preservedDrop());
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MachineEffects.animate(BlockCatalogue.Geometry.TANK, 1, state.getValue(STATUS),
                state.getValue(MachineProperties.FACING), level, pos, random);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasicTankBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.BASIC_TANK.get(), BasicTankBlockEntity::tick);
    }
}
