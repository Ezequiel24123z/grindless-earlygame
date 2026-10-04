package io.github.ezequiel24123z.grindless.item;

/**
 * Deconstruction marks, independent of a world (ADR-0086).
 *
 * <p>The edge matches a blueprint so a marked region is one the tool could also capture.
 * The planner does not break anything; the volume is only a number the player can read.
 */
public final class PlannerLogic {

    public static final int MAX_EDGE = BlueprintLogic.MAX_EDGE;

    private PlannerLogic() {
    }

    public static boolean edgeOk(int min, int max) {
        return BlueprintLogic.edgeOk(min, max);
    }

    /** Inclusive box. Zero when the edge is illegal, so a refused mark reports nothing. */
    public static long volume(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        if (!edgeOk(minX, maxX) || !edgeOk(minY, maxY) || !edgeOk(minZ, maxZ)) {
            return 0L;
        }
        long dx = (long) maxX - minX + 1;
        long dy = (long) maxY - minY + 1;
        long dz = (long) maxZ - minZ + 1;
        return dx * dy * dz;
    }
}
