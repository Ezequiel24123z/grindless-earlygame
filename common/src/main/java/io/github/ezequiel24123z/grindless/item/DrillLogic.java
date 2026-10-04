package io.github.ezequiel24123z.grindless.item;

import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Drill numbers, independent of a world (ADR-0084).
 *
 * <p>Charge is an integer. A cell fills it. A broken block spends it. The shape of an area or a
 * tunnel is a list of offsets; the vein is a face-connected flood with a cap.
 */
public final class DrillLogic {

    /** One F1 tick, so a block costs the same unit the factory already uses. */
    public static final int BLOCK_FU = 32;

    /** One hundred blocks. A full tunnel is 72, so one cell finishes it. */
    public static final int CELL_FU = 3200;

    /** Two cells. A third is refused whole, never clipped. */
    public static final int CAPACITY = 6400;

    public static final int VEIN_CAP = 32;

    public static final int TUNNEL_DEPTH = 8;

    public static final int HARVEST_LEVEL = 3;

    public static final float SPEED = 8.0F;

    public enum Mode {
        SINGLE,
        AREA,
        VEIN,
        TUNNEL;

        public Mode next() {
            return switch (this) {
                case SINGLE -> AREA;
                case AREA -> VEIN;
                case VEIN -> TUNNEL;
                case TUNNEL -> SINGLE;
            };
        }
    }

    /** A cube offset from the block that was mined. */
    public record Cube(int x, int y, int z) {
    }

    /** True when this cube is the same block the vein is following. */
    public interface Same {
        boolean same(int x, int y, int z);
    }

    private static final Cube[] FACES = {
            new Cube(1, 0, 0), new Cube(-1, 0, 0),
            new Cube(0, 1, 0), new Cube(0, -1, 0),
            new Cube(0, 0, 1), new Cube(0, 0, -1)
    };

    private DrillLogic() {
    }

    public static Mode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Mode.SINGLE;
        }
        try {
            return Mode.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return Mode.SINGLE;
        }
    }

    public static boolean canStart(int stored) {
        return stored >= BLOCK_FU;
    }

    public static int afterBlock(int stored) {
        return Math.max(0, stored - BLOCK_FU);
    }

    /** Adds one cell, or leaves the charge alone when the whole cell would not fit. */
    public static int storedAfterCell(int stored) {
        int current = Math.max(0, stored);
        if (current + CELL_FU > CAPACITY) {
            return current;
        }
        return current + CELL_FU;
    }

    /** Terrain the drill may remove. A Grindless block is the factory, not the ore. */
    public static boolean breaks(boolean pickaxe, boolean tierOk, boolean ownMachine) {
        return pickaxe && tierOk && !ownMachine;
    }

    /** The eight neighbours in the plane perpendicular to {@code facing}. */
    public static List<Cube> area(Direction facing) {
        List<Cube> out = new ArrayList<>();
        Direction.Axis axis = facing.getAxis();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                int x = 0;
                int y = 0;
                int z = 0;
                if (axis == Direction.Axis.Y) {
                    x = a;
                    z = b;
                } else if (axis == Direction.Axis.X) {
                    y = a;
                    z = b;
                } else {
                    x = a;
                    y = b;
                }
                out.add(new Cube(x, y, z));
            }
        }
        return out;
    }

    /**
     * A horizontal 3×3, eight blocks deep, not counting the block already mined.
     * A vertical look becomes north: the tunnel is the chore the README names, not a shaft.
     */
    public static List<Cube> tunnel(Direction facing) {
        Direction horizontal = facing.getAxis().isVertical() ? Direction.NORTH : facing;
        int stepX = horizontal.getStepX();
        int stepZ = horizontal.getStepZ();
        List<Cube> out = new ArrayList<>();
        for (int depth = 0; depth < TUNNEL_DEPTH; depth++) {
            for (int across = -1; across <= 1; across++) {
                for (int rise = -1; rise <= 1; rise++) {
                    if (depth == 0 && across == 0 && rise == 0) {
                        continue;
                    }
                    int x = stepX * depth + (stepX == 0 ? across : 0);
                    int z = stepZ * depth + (stepZ == 0 ? across : 0);
                    out.add(new Cube(x, rise, z));
                }
            }
        }
        return out;
    }

    /** Face-connected cubes of the same block, including the origin, stopped at {@code cap}. */
    public static List<Cube> vein(int x, int y, int z, int cap, Same same) {
        List<Cube> found = new ArrayList<>();
        if (cap <= 0) {
            return found;
        }
        ArrayDeque<Cube> queue = new ArrayDeque<>();
        HashSet<Long> seen = new HashSet<>();
        Cube origin = new Cube(x, y, z);
        queue.add(origin);
        seen.add(key(origin));
        while (!queue.isEmpty() && found.size() < cap) {
            Cube at = queue.removeFirst();
            if (!same.same(at.x, at.y, at.z)) {
                continue;
            }
            found.add(at);
            if (found.size() >= cap) {
                break;
            }
            for (Cube face : FACES) {
                Cube next = new Cube(at.x + face.x, at.y + face.y, at.z + face.z);
                if (seen.add(key(next))) {
                    queue.add(next);
                }
            }
        }
        return found;
    }

    private static long key(Cube cube) {
        return ((long) (cube.x & 0x3FFFFFF) << 38)
                | ((long) (cube.y & 0xFFF) << 26)
                | (cube.z & 0x3FFFFFFL);
    }
}
