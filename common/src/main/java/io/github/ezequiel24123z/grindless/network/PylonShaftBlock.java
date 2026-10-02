package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.machine.MachineEffects;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The middle or top third of a Flux Pylon. Not in the creative tab and not dropped: breaking it
 * breaks the base, which drops the pylon the player placed.
 */
public class PylonShaftBlock extends Block {

    public static final IntegerProperty INDEX = IntegerProperty.create("index", 1, 2);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 3);
    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.GRID);

    public static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 16.0 - 4.0);

    public PylonShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(INDEX, 1)
                .setValue(TIER, 1)
                .setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX, TIER, STATUS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(INDEX) == PylonStructure.HEIGHT - 1) {
            MachineEffects.animate(BlockCatalogue.Geometry.PYLON, state.getValue(TIER),
                    state.getValue(STATUS), Direction.NORTH, level, pos, random);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(pylonItem(state.getValue(TIER)));
    }

    private static Block pylonItem(int tier) {
        return switch (tier) {
            case 2 -> ModBlocks.FLUX_PYLON_MK2.get();
            case 3 -> ModBlocks.FLUX_PYLON_MK3.get();
            default -> ModBlocks.FLUX_PYLON_MK1.get();
        };
    }

    /**
     * If the base is gone, this shaft is debris and pops. The base's own removal already deletes
     * shafts, so this is the path where something else (an explosion, a piston) took the base.
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN) {
            BlockPos base = PylonStructure.baseOf(pos, state.getValue(INDEX));
            if (!(level.getBlockState(base).getBlock() instanceof PylonBlock)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /**
     * Breaking a shaft breaks the whole tower. The base is still a pylon when this runs, because
     * the world has only replaced <em>this</em> block; the base's {@code onRemove} then deletes
     * the remaining shaft. The other way round — base already air — is a no-op, which is how the
     * base can delete shafts without those shafts deleting the base.
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos base = PylonStructure.baseOf(pos, state.getValue(INDEX));
            if (level.getBlockState(base).getBlock() instanceof PylonBlock) {
                level.destroyBlock(base, !player.isCreative());
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
