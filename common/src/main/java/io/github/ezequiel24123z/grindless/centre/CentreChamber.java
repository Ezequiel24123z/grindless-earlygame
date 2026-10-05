package io.github.ezequiel24123z.grindless.centre;

import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The only air in the galactic centre (ADR-0099).
 *
 * <p>Everything this class does not name stays horizon shell. The list is finite, and
 * applying it again puts the room back, so the chamber cannot be kept as a base.
 */
public final class CentreChamber {

    /** What a carved cell holds. Shell is the absence of a cell. */
    public enum Kind {
        AIR,
        BERTH,
        MARK
    }

    public record Cell(int x, int y, int z, Kind kind) {
    }

    private CentreChamber() {
    }

    /**
     * The block a carved cell should be, or {@code null} when that position stays shell.
     */
    public static Kind kindAt(int x, int y, int z) {
        if (x == CentreCatalogue.BERTH_X && y == CentreCatalogue.BERTH_Y && z == CentreCatalogue.BERTH_Z) {
            return Kind.BERTH;
        }
        if (x == CentreCatalogue.MARK_X && y == CentreCatalogue.MARK_Y && z == CentreCatalogue.MARK_Z) {
            return Kind.MARK;
        }
        if (CentreCatalogue.inRoom(x, y, z) || CentreCatalogue.inShaft(x, y, z)) {
            return Kind.AIR;
        }
        return null;
    }

    /** Every carved cell, room first and then the shaft. No duplicates. */
    public static List<Cell> cells() {
        List<Cell> out = new ArrayList<>();
        for (int y = CentreCatalogue.AIR_BOTTOM; y < CentreCatalogue.AIR_TOP; y++) {
            for (int x = -CentreCatalogue.RADIUS; x <= CentreCatalogue.RADIUS; x++) {
                for (int z = -CentreCatalogue.RADIUS; z <= CentreCatalogue.RADIUS; z++) {
                    out.add(new Cell(x, y, z, kindAt(x, y, z)));
                }
            }
        }
        for (int y = CentreCatalogue.AIR_TOP; y < CentreCatalogue.HEIGHT; y++) {
            for (int x = -CentreCatalogue.SHAFT; x <= CentreCatalogue.SHAFT; x++) {
                for (int z = -CentreCatalogue.SHAFT; z <= CentreCatalogue.SHAFT; z++) {
                    out.add(new Cell(x, y, z, kindAt(x, y, z)));
                }
            }
        }
        return out;
    }

    /** Writes the chamber into {@code level}. Shell outside the list is left as it generated. */
    public static void carve(ServerLevel level) {
        int radius = CentreCatalogue.RADIUS;
        level.getChunk(radius >> 4, radius >> 4);
        level.getChunk((-radius) >> 4, (-radius) >> 4);
        for (Cell cell : cells()) {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            BlockState state = state(cell.kind());
            if (!level.getBlockState(pos).equals(state)) {
                level.setBlockAndUpdate(pos, state);
            }
        }
    }

    private static BlockState state(Kind kind) {
        return switch (kind) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case BERTH -> ModBlocks.STATION_BERTH.get().defaultBlockState();
            case MARK -> ModBlocks.ARRIVAL_MARK.get().defaultBlockState();
        };
    }
}
