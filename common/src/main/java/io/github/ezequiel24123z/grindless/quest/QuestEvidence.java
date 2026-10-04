package io.github.ezequiel24123z.grindless.quest;

/**
 * The three facts a task is allowed to watch (ADR-0100).
 *
 * <p>Holding an item, a blueprint already unlocked, or standing in a dimension. Nothing
 * else counts, and nothing here is consumed.
 */
public final class QuestEvidence {

    private QuestEvidence() {
    }

    public static boolean met(QuestCatalogue.Task task, int held, boolean researched, String dimension) {
        return switch (task.evidence()) {
            case ITEM -> held >= task.count();
            case RESEARCH -> researched;
            case DIMENSION -> task.subject().equals(dimension);
        };
    }
}
