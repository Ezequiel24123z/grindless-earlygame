package io.github.ezequiel24123z.grindless.vein;

import java.util.ArrayList;
import java.util.List;

/**
 * How the Prospector's Scanner picks chunks, independent of a world.
 *
 * <p>The standing chunk and its neighbours: a 3×3. Wide enough that walking finds a vein
 * without turning the first session into a grid-search, small enough that a rich chunk is
 * still a find.
 */
public final class SurveyLogic {

    /** Chebyshev radius in chunks. 1 is a 3×3. */
    public static final int RADIUS = 1;

    private SurveyLogic() {
    }

    public record ChunkRef(int x, int z) {
    }

    /** Every chunk in the square centred on {@code (cx, cz)}. */
    public static List<ChunkRef> around(int cx, int cz) {
        List<ChunkRef> chunks = new ArrayList<>();
        for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
            for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
                chunks.add(new ChunkRef(x, z));
            }
        }
        return chunks;
    }

    public static int area() {
        int span = RADIUS * 2 + 1;
        return span * span;
    }
}
