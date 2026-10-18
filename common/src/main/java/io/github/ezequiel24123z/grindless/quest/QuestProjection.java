package io.github.ezequiel24123z.grindless.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable renderer identities derived from the catalogue, never a second quest or reward ledger. */
public final class QuestProjection {

    public static final String PREFIX = "grindless_";
    public static final String CLAIM_TASK = "claim";

    public record Entry(String id, QuestCatalogue.Task task, List<String> dependencies) {
    }

    private QuestProjection() {
    }

    public static String id(String catalogueId) {
        if (QuestCatalogue.byId(catalogueId) == null) {
            throw new IllegalArgumentException("Unknown quest catalogue id: " + catalogueId);
        }
        return PREFIX + catalogueId;
    }

    public static List<Entry> entries() {
        List<String> problems = QuestLogic.problems(QuestCatalogue.tasks());
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Cannot project an invalid quest catalogue: " + problems);
        }
        return QuestCatalogue.tasks().stream()
                .map(task -> new Entry(id(task.id()), task, task.requires().stream().map(QuestProjection::id).toList()))
                .toList();
    }

    /** Only owned ids are projected, and only legacy claims may become native completion. */
    public static Map<String, Boolean> claimed(Set<String> legacyClaims) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Entry entry : entries()) {
            result.put(entry.id(), legacyClaims.contains(entry.task().id()));
        }
        return java.util.Collections.unmodifiableMap(result);
    }
}
