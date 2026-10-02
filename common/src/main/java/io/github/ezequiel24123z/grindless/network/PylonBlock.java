package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.machine.MachineEffects;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Flux Pylon block. One class covers all three tiers; the tier is a property of the block
 * instance, which is what lets a single block entity type serve MK1 through MK3.
 */
public class PylonBlock extends BaseEntityBlock {

    public static final EnumProperty<MachineStatus> STATUS =
            MachineProperties.status(BlockCatalogue.GRID);

    private final PylonTier tier;

    public PylonBlock(PylonTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MachineEffects.animate(BlockCatalogue.Geometry.PYLON, tier.ordinal() + 1,
                state.getValue(STATUS), Direction.NORTH, level, pos, random);
    }

    public PylonTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PylonBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE, which suits a block drawn entirely by a renderer
        // and not one with an ordinary model. Forgetting this is the usual cause of an invisible
        // block entity.
        return RenderShape.MODEL;
    }

    /**
     * Registers the pylon once it is in the world.
     *
     * <p>Registration happens here rather than in the block entity's constructor because joining a
     * network needs the level, and during construction the block entity is not attached to one.
     *
     * <p>A block merely changing state is not a placement: re-adding a pylon that is already a
     * member removes it first, which re-runs the split flood fill.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide() && !oldState.is(state.getBlock())
                && level.getBlockEntity(pos) instanceof PylonBlockEntity pylon) {
            pylon.register();
        }
    }

    /**
     * Deregisters the pylon before the block entity is gone.
     *
     * <p>Order matters: {@code super.onRemove} destroys the block entity, so the network has to be
     * told first. Doing it afterwards leaves a network holding a pylon that no longer exists,
     * which keeps its capacity and its topology — a player would break a pylon and see nothing
     * change.
     *
     * <p>The {@code newState} guard means a block merely changing state, rather than being
     * removed, does not drop out of its network.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PylonBlockEntity pylon) {
            pylon.deregister();
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
