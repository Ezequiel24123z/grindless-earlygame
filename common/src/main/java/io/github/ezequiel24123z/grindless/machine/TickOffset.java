package io.github.ezequiel24123z.grindless.machine;

import net.minecraft.core.BlockPos;

/**
 * Spreads periodic machine work across ticks instead of letting it land on all of them at once.
 *
 * <h2>The problem this solves</h2>
 *
 * <p>Throttling expensive work to every N ticks is the obvious optimisation, and written the
 * obvious way — {@code gameTime % 20 == 0} — it makes things worse in a specific and nasty way.
 * Every machine in the world agrees on {@code gameTime}, so all of them do their expensive work on
 * the <em>same</em> tick. Average load falls by twenty times; the worst tick does not fall at all.
 * Players experience the worst tick, which is why this shows up as a once-a-second stutter rather
 * than as a lower tick rate.
 *
 * <p>Giving each machine a stable offset turns that spike into a flat line. GregTech CEu does
 * exactly this with its own offset timer, and it is why their throttles read
 * {@code getOffsetTimer() % 5 == 0} rather than using the raw game time.
 */
public final class TickOffset {

    private TickOffset() {
    }

    /** A stable offset for a machine at {@code pos}. */
    public static int forPosition(BlockPos pos) {
        return forPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * A stable offset for a machine at the given coordinates.
     *
     * <p>Derived from the position rather than randomly assigned, so it survives a reload without
     * being saved and two machines in the same place always agree.
     *
     * <p>The mixing matters. Block coordinates are extremely regular, and using them directly —
     * or with a weak hash — puts every machine in a row on the same tick. That is the case that
     * counts, because players build in rows, so a naive offset would spread work across the one
     * axis nobody builds along and synchronise it along the ones everybody does.
     */
    public static int forPosition(int x, int y, int z) {
        int hash = x * 0x9E3779B9 ^ y * 0x85EBCA6B ^ z * 0xC2B2AE35;
        hash ^= hash >>> 15;
        hash *= 0x2545F491;
        hash ^= hash >>> 13;
        return hash & 0x7FFFFFFF;
    }

    /**
     * Whether periodic work with this offset is due on this tick.
     *
     * @param gameTime the level's game time
     * @param offset   from {@link #forPosition}
     * @param period   how often the work should run, in ticks; must be positive
     */
    public static boolean isDue(long gameTime, int offset, int period) {
        if (period <= 1) {
            return true;
        }
        return Math.floorMod(gameTime + offset, period) == 0;
    }
}
