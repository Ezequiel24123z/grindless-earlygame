package io.github.ezequiel24123z.grindless.quest;

/**
 * Explicit free claims and the physical facts a task is allowed to watch (ADR-0100).
 *
 * <p>Holding an item, a physical rated Matrix, or standing in a dimension. Nothing
 * else counts. A FREE goal needs none of those facts, and nothing here is consumed.
 */
public final class QuestEvidence {

    private QuestEvidence() {
    }

    public static boolean met(QuestCatalogue.Task task, int held, String dimension) {
        return switch (task.evidence()) {
            case ITEM, MATRIX -> held >= task.count();
            case DIMENSION -> task.subject().equals(dimension);
            case FREE -> true;
        };
    }
}
