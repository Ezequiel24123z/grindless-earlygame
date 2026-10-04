package io.github.ezequiel24123z.grindless.pattern;

import io.github.ezequiel24123z.grindless.machine.FacingLogisticsBlock;
import io.github.ezequiel24123z.grindless.machine.MachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Turns one item into one Matter. The blacklist does not apply (ADR-0088). */
public class DeconstructorBlock extends FacingLogisticsBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.CONSUMER);

    public DeconstructorBlock(Properties properties) {
        super(properties, STATUS, BlockCatalogue.Geometry.DECONSTRUCTOR,
                Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeconstructorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DeconstructorBlockEntity deconstructor)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        deconstructor.smash(player, held);
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.DECONSTRUCTOR.get(),
                MachineBlockEntity::tick);
    }
}
