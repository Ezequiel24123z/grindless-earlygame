package io.github.ezequiel24123z.grindless.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blueprint numbers, independent of a world (ADR-0085).
 *
 * <p>A capture is a list of pieces relative to the minimum corner. Only a facing is kept.
 * Status and block entities are not. A stamp is all or nothing.
 */
public final class BlueprintLogic {

    public static final int MAX_EDGE = 32;

    public static final int MAX_PIECES = 512;

    /** One placed block: offsets from the minimum corner, and the item that pays for it. */
    public record Piece(int x, int y, int z, String blockId, String itemId, String facing) {
    }

    private BlueprintLogic() {
    }

    public static boolean edgeOk(int min, int max) {
        return max >= min && (max - min + 1) <= MAX_EDGE;
    }

    public static boolean countOk(int pieces) {
        return pieces > 0 && pieces <= MAX_PIECES;
    }

    /** Facing is structure. Status is a live machine and is not copied. */
    public static String keptProperty(String name, String value) {
        if (!"facing".equals(name) || value == null || value.isBlank()) {
            return "";
        }
        return value;
    }

    public static Map<String, Integer> cost(List<Piece> pieces) {
        Map<String, Integer> need = new LinkedHashMap<>();
        for (Piece piece : pieces) {
            if (piece.itemId() == null || piece.itemId().isBlank()) {
                continue;
            }
            need.merge(piece.itemId(), 1, Integer::sum);
        }
        return need;
    }

    /** Items still required. An empty map means the stamp can be paid for. */
    public static Map<String, Integer> shortfall(Map<String, Integer> need, Map<String, Integer> have) {
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : need.entrySet()) {
            int got = have.getOrDefault(entry.getKey(), 0);
            if (got < entry.getValue()) {
                missing.put(entry.getKey(), entry.getValue() - got);
            }
        }
        return missing;
    }
}
