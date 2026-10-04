package io.github.ezequiel24123z.grindless.quest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a task can be claimed (ADR-0100).
 *
 * <p>Pure. The menu supplies the evidence and the saved claims. This class does not see a
 * player, a world, or a registry.
 */
public final class QuestLogic {

    /**
     * Stored in a menu data slot, so the order is the wire format.
     * Claimed first, then locked, then unmet, then ready.
     */
    public enum Status {
        CLAIMED,
        LOCKED,
        UNMET,
        CLAIMABLE
    }

    private QuestLogic() {
    }

    public static Status consider(QuestCatalogue.Task task, Set<String> claimed, boolean evidenceMet) {
        if (claimed.contains(task.id())) {
            return Status.CLAIMED;
        }
        for (String required : task.requires()) {
            if (!claimed.contains(required)) {
                return Status.LOCKED;
            }
        }
        return evidenceMet ? Status.CLAIMABLE : Status.UNMET;
    }

    public static Status byCode(int code) {
        Status[] values = Status.values();
        if (code < 0 || code >= values.length) {
            return Status.UNMET;
        }
        return values[code];
    }

    /**
     * Structural problems in a catalogue. Empty means the graph is a book: unique ids,
     * dependencies that exist and come earlier, and a reward that fits one stack.
     */
    public static List<String> problems(List<QuestCatalogue.Task> tasks) {
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < tasks.size(); i++) {
            QuestCatalogue.Task task = tasks.get(i);
            if (task.id().isBlank() || !ids.add(task.id())) {
                problems.add("duplicate or blank id at " + i);
            }
            if (task.line().isBlank()) {
                problems.add(task.id() + " has no line");
            }
            if (task.subject().isBlank()) {
                problems.add(task.id() + " has no subject");
            }
            if (task.evidence() == QuestCatalogue.Evidence.ITEM && task.count() < 1) {
                problems.add(task.id() + " asks for no items");
            }
            QuestCatalogue.Reward reward = task.reward();
            if (reward == null || reward.count() < 1 || reward.count() > 64 || reward.itemId().isBlank()) {
                problems.add(task.id() + " reward is not one stack of an item");
            }
            for (String required : task.requires()) {
                int at = indexOf(tasks, required);
                if (at < 0) {
                    problems.add(task.id() + " depends on a missing task " + required);
                } else if (at >= i) {
                    problems.add(task.id() + " depends on " + required + ", which is not earlier");
                }
            }
        }
        return problems;
    }

    private static int indexOf(List<QuestCatalogue.Task> tasks, String id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
