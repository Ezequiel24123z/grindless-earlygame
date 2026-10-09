package io.github.ezequiel24123z.grindless.quest;

/**
 * The three facts a task is allowed to watch (ADR-0100).
 *
 * <p>Holding an item, a physical rated Matrix, or standing in a dimension. Nothing
 * else counts, and nothing here is consumed.
 */
public final class QuestEvidence {

    private QuestEvidence() {
    }

    public static boolean met(QuestCatalogue.Task task, int held, String dimension) {
        return switch (task.evidence()) {
            case ITEM, MATRIX -> held >= task.count();
            case DIMENSION -> task.subject().equals(dimension);
        };
    }
}
