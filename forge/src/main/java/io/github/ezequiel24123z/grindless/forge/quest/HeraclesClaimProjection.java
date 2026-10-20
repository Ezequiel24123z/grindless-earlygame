package io.github.ezequiel24123z.grindless.forge.quest;

import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.handlers.progress.QuestsProgress;
import io.github.ezequiel24123z.grindless.quest.QuestProjection;
import net.minecraft.nbt.ByteTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rebuilds only owned native state, with no player, reward API or write access to the legacy ledger. */
public final class HeraclesClaimProjection {

    private HeraclesClaimProjection() {
    }

    public static List<String> reconcile(QuestsProgress nativeProgress, Set<String> legacyClaims,
                                         Map<String, Quest> definitions) {
        List<String> changed = new ArrayList<>();
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            Quest quest = definitions.get(entry.id());
            if (quest == null || !(quest.tasks().get(QuestProjection.CLAIM_TASK) instanceof GrindlessClaimTask task)
                    || !task.catalogueId().equals(entry.task().id()) || !quest.settings().individualProgress()
                    || !quest.rewards().isEmpty() || quest.settings().autoClaimRewards() || quest.settings().repeatable()) {
                throw new IllegalStateException("Unsafe native Grindless definition: " + entry.id());
            }
            boolean claimed = legacyClaims.contains(entry.task().id());
            QuestProgress current = nativeProgress.progress().get(entry.id());
            var currentTask = current == null ? null : current.tasks().get(QuestProjection.CLAIM_TASK);
            if (current != null && current.isComplete() == claimed && current.claimedRewards().isEmpty()
                    && current.tasks().size() == 1 && currentTask != null && currentTask.isComplete() == claimed
                    && task.storage().readBoolean(currentTask.progress()) == claimed) {
                continue;
            }
            QuestProgress projection = new QuestProgress();
            var progress = projection.getTask(task);
            progress.setProgress(ByteTag.valueOf(claimed));
            progress.setComplete(claimed);
            projection.setComplete(claimed);
            nativeProgress.progress().put(entry.id(), projection);
            changed.add(entry.id());
        }
        if (!changed.isEmpty()) {
            nativeProgress.completableQuests().updateCompleteQuests(nativeProgress);
        }
        return List.copyOf(changed);
    }
}
