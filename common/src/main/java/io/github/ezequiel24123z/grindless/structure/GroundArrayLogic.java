package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.network.PylonTier;
import net.minecraft.core.BlockPos;

/**
 * Numbers and footprint for the Ground Array (ADR-0094). Independent of a world.
 *
 * <p>The ring is the eight blocks of the horizontal Moore neighbourhood. Seven is not a
 * structure. The capacity is ten seconds of an MK3 pylon, the same buffer the capacitor
 * bank uses for an MK1, and it is storage rather than generation.
 */
public final class GroundArrayLogic {

    /** Casings around the controller. */
    public static final int RING = 8;

    /**
     * Ticks of throughput the structure holds. Ten seconds, matching the capacitor bank's
     * buffer (ADR-0064) applied to MK3 instead of MK1.
     */
    public static final int BUFFER_TICKS = 200;

    /** Extra FU a complete, covered array adds. 200 × 32,768. */
    public static final long CAPACITY = PylonTier.MK3.throughput() * BUFFER_TICKS;

    /** Refractory bricks in one casing. The lining of one part, not the whole ring. */
    public static final int CASING_BRICKS = 4;

    /** Steel ingots in one casing. */
    public static final int CASING_STEEL = 1;

    /** Machine casings in the controller. */
    public static final int CONTROLLER_CASINGS = 1;

    /** Steel plates in the controller. */
    public static final int CONTROLLER_PLATES = 4;

    /** Refractory bricks in the controller. */
    public static final int CONTROLLER_BRICKS = 4;

    private GroundArrayLogic() {
    }

    /** Whether {@code casings} is a complete ring. */
    public static boolean formed(int casings) {
        return casings == RING;
    }

    /** Extra FU contributed while {@code complete}. An open ring adds nothing. */
    public static long contribution(boolean complete) {
        return complete ? CAPACITY : 0L;
    }

    /**
     * The eight positions around {@code controller}, same Y, not including the centre.
     *
     * <p>Order is x then z, each from −1 to 1, skipping (0, 0). A corner is in this list:
     * Minecraft only tells a block about its six orthogonal neighbours, so the casing has
     * to walk this same neighbourhood to find the controller.
     */
    public static BlockPos[] ring(BlockPos controller) {
        BlockPos[] positions = new BlockPos[RING];
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                positions[n++] = controller.offset(dx, 0, dz);
            }
        }
        return positions;
    }
}
