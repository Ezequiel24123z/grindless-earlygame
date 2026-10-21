package io.github.ezequiel24123z.grindless.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Blueprint numbers, independent of a world (ADR-0085).
 *
 * <p>A capture is a list of pieces relative to the minimum corner. Only a facing is kept.
 * Status and block entities are not. Preflight does not place or spend anything.
 */
public final class BlueprintLogic {

    public static final int MAX_EDGE = 32;

    public static final int MAX_PIECES = 512;

    /** One placed block: offsets from the minimum corner, and the item that pays for it. */
    public record Piece(int x, int y, int z, String blockId, String itemId, String facing) {
    }

    public enum Failure {
        NONE, EMPTY, TOO_LARGE, MALFORMED, DUPLICATE, UNKNOWN_BLOCK, INVALID_ITEM,
        INVALID_FACING, OUTSIDE_WORLD, UNLOADED, FORBIDDEN, BLOCKED, MATERIALS_CHANGED, PLACEMENT_FAILED
    }

    public record Problem(Failure failure, Piece piece) {
    }

    public record Plan(List<Piece> pieces, Map<String, Integer> required,
                       Map<String, Integer> missing, List<Problem> problems) {
        public Plan {
            pieces = List.copyOf(pieces);
            required = Collections.unmodifiableMap(new LinkedHashMap<>(required));
            missing = Collections.unmodifiableMap(new LinkedHashMap<>(missing));
            problems = List.copyOf(problems);
        }

        public boolean ready() {
            return countOk(pieces.size()) && problems.isEmpty() && missing.isEmpty();
        }
    }

    private record Offset(int x, int y, int z) {
    }

    private BlueprintLogic() {
    }

    public static boolean edgeOk(int min, int max) {
        return max >= min && ((long) max - min + 1) <= MAX_EDGE;
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

    /** The inspector supplies read-only registry/site facts; it must never load a chunk. */
    public static Plan preflight(List<Piece> pieces, Map<String, Integer> have, boolean creative,
                                 Function<Piece, Failure> inspector) {
        if (pieces == null) {
            return rejected(Failure.MALFORMED, null);
        }
        if (!countOk(pieces.size())) {
            return rejected(pieces.isEmpty() ? Failure.EMPTY : Failure.TOO_LARGE, null);
        }
        Set<Offset> occupied = new HashSet<>();
        for (Piece piece : pieces) {
            if (piece == null || !offsetOk(piece.x()) || !offsetOk(piece.y()) || !offsetOk(piece.z())
                    || piece.blockId() == null || piece.blockId().isBlank()
                    || piece.itemId() == null || piece.itemId().isBlank()) {
                return rejected(Failure.MALFORMED, piece);
            }
            if (!occupied.add(new Offset(piece.x(), piece.y(), piece.z()))) {
                return rejected(Failure.DUPLICATE, piece);
            }
        }
        List<Piece> snapshot = List.copyOf(pieces);
        Map<String, Integer> required = cost(snapshot);
        Map<String, Integer> missing = creative ? Map.of() : shortfall(required, have);
        List<Problem> problems = new ArrayList<>();
        for (Piece piece : snapshot) {
            Failure failure = inspector.apply(piece);
            if (failure != Failure.NONE) {
                problems.add(new Problem(failure, piece));
            }
        }
        return new Plan(snapshot, required, missing, problems);
    }

    public static Plan rejected(Failure failure, Piece piece) {
        return new Plan(List.of(), Map.of(), Map.of(), List.of(new Problem(failure, piece)));
    }

    private static boolean offsetOk(int value) {
        return value >= 0 && value < MAX_EDGE;
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
            int got = Math.max(0, have.getOrDefault(entry.getKey(), 0));
            if (got < entry.getValue()) {
                missing.put(entry.getKey(), entry.getValue() - got);
            }
        }
        return missing;
    }
}
