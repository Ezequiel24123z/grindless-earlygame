package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** An oriented working-channel component with no inventory, artificial status or block entity. */
public final class WorkshopPartBlock extends Block {

    private static final List<Direction> FACINGS = List.of(
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final Map<Geometry, Map<Direction, VoxelShape>> SHAPES = shapes();
    private final Geometry geometry;

    public WorkshopPartBlock(Geometry geometry, Properties properties) {
        super(properties);
        if (geometry != Geometry.CHANNEL && geometry != Geometry.SCREEN) {
            throw new IllegalArgumentException("a static workshop part must be a channel or screen");
        }
        this.geometry = geometry;
        registerDefaultState(stateDefinition.any().setValue(MachineProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineProperties.FACING);
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(geometry, state.getValue(MachineProperties.FACING));
    }

    /** Shares the authoritative collision geometry with the active controller as well as its parts. */
    public static VoxelShape shape(Geometry geometry, Direction facing) {
        VoxelShape shape = SHAPES.getOrDefault(geometry, Map.of()).get(facing);
        if (shape == null) {
            throw new IllegalArgumentException("unsupported settling geometry or facing");
        }
        return shape;
    }

    private static Map<Geometry, Map<Direction, VoxelShape>> shapes() {
        Map<Geometry, Map<Direction, VoxelShape>> result = new EnumMap<>(Geometry.class);
        for (Geometry geometry : List.of(Geometry.SETTLER, Geometry.CHANNEL, Geometry.SCREEN)) {
            Map<Direction, VoxelShape> directions = new EnumMap<>(Direction.class);
            for (int turn = 0; turn < FACINGS.size(); turn++) {
                VoxelShape combined = Shapes.empty();
                for (WorkshopGeometry.Box box : WorkshopGeometry.boxes(geometry)) {
                    double x0 = box.x0();
                    double z0 = box.z0();
                    double x1 = box.x1();
                    double z1 = box.z1();
                    for (int step = 0; step < turn; step++) {
                        double nextX0 = 16 - z1;
                        double nextX1 = 16 - z0;
                        z0 = x0;
                        z1 = x1;
                        x0 = nextX0;
                        x1 = nextX1;
                    }
                    combined = Shapes.or(combined, Block.box(x0, box.y0(), z0, x1, box.y1(), z1));
                }
                directions.put(FACINGS.get(turn), combined.optimize());
            }
            result.put(geometry, Map.copyOf(directions));
        }
        return Map.copyOf(result);
    }
}
