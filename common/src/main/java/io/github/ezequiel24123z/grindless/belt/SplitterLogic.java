package io.github.ezequiel24123z.grindless.belt;

import java.util.ArrayList;
import java.util.List;

/**
 * How a splitter picks an outlet, independent of a world.
 *
 * <p>Filter plus priority, copied from Factorio because that pair is the whole interesting
 * belt language (README System 4). An empty filter accepts everything. A matching filter
 * beats an unfiltered outlet — that is the implicit priority a player sets by putting an
 * item on one face. Equal priority rotates from the last choice so a mixed line actually
 * splits rather than sticking to the first open face.
 */
public final class SplitterLogic {

    /** Front, left, right — the three faces a 1-block splitter can feed. */
    public static final int OUTLETS = 3;

    public static final int FRONT = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private SplitterLogic() {
    }

    /**
     * One possible output.
     *
     * @param index    {@link #FRONT}, {@link #LEFT} or {@link #RIGHT}
     * @param filtered whether this outlet names a specific item
     * @param matches  whether the travelling item is accepted
     * @param backedUp whether the downstream lane cannot take another item
     * @param priority higher wins; equal priority round-robins
     */
    public record Outlet(int index, boolean filtered, boolean matches, boolean backedUp, int priority) {
    }

    /**
     * Which outlet should take the item, or {@code -1} if every matching outlet is backed up.
     */
    public static int route(List<Outlet> outlets, int lastIndex) {
        List<Outlet> open = new ArrayList<>();
        for (Outlet outlet : outlets) {
            if (!outlet.backedUp() && outlet.matches()) {
                open.add(outlet);
            }
        }
        if (open.isEmpty()) {
            return -1;
        }
        boolean anyFilter = false;
        for (Outlet outlet : open) {
            if (outlet.filtered()) {
                anyFilter = true;
                break;
            }
        }
        if (anyFilter) {
            List<Outlet> filtered = new ArrayList<>();
            for (Outlet outlet : open) {
                if (outlet.filtered()) {
                    filtered.add(outlet);
                }
            }
            open = filtered;
        }
        int best = Integer.MIN_VALUE;
        for (Outlet outlet : open) {
            if (outlet.priority() > best) {
                best = outlet.priority();
            }
        }
        List<Outlet> preferred = new ArrayList<>();
        for (Outlet outlet : open) {
            if (outlet.priority() == best) {
                preferred.add(outlet);
            }
        }
        if (preferred.size() == 1) {
            return preferred.get(0).index();
        }
        preferred.sort((a, b) -> Integer.compare(a.index(), b.index()));
        for (Outlet outlet : preferred) {
            if (outlet.index() > lastIndex) {
                return outlet.index();
            }
        }
        return preferred.get(0).index();
    }

    /** Empty filter accepts every item. */
    public static boolean matches(String itemId, String filterId) {
        return filterId == null || filterId.isEmpty() || filterId.equals(itemId);
    }
}
