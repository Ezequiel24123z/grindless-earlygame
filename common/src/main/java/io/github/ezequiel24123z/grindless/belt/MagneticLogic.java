package io.github.ezequiel24123z.grindless.belt;

import java.util.Set;

/**
 * How a magnetic separator splits a mixed line, independent of a world (ADR-0080).
 *
 * <p>Left takes an item whose id has a ferromagnetic token. Everything else continues
 * forward. A backed-up magnet holds; it does not dump the metal onto the front. The
 * item itself does not change, so a single-material vein is not a grade upgrade.
 */
public final class MagneticLogic {

    public static final int FRONT = 0;
    public static final int LEFT = 1;

    private static final Set<String> FERROMAGNETIC = Set.of("iron", "nickel", "steel");

    private MagneticLogic() {
    }

    /** A path token, so {@code raw_iron} matches and {@code copper_ingot} does not. */
    public static boolean magnetic(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        int colon = itemId.indexOf(':');
        String path = colon >= 0 ? itemId.substring(colon + 1) : itemId;
        for (String token : path.split("_")) {
            if (FERROMAGNETIC.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Which outlet should take the item, or {@code -1} when that outlet is backed up.
     */
    public static int route(String itemId, boolean frontOpen, boolean leftOpen) {
        if (magnetic(itemId)) {
            return leftOpen ? LEFT : -1;
        }
        return frontOpen ? FRONT : -1;
    }
}
