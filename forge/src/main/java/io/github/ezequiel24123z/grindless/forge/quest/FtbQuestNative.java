package io.github.ezequiel24123z.grindless.forge.quest;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.DimensionTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Compatibility import pays nothing; all subsequent rewards belong exclusively to FTB Quests. */
public final class FtbQuestNative {

    private FtbQuestNative() {
    }

    public static void verify(BaseQuestFile file) {
        for (CompoundTag document : FtbQuestDefinitions.chapters().values()) {
            Chapter chapter = file.getChapter(parseId(document));
            if (chapter == null) {
                throw new IllegalStateException("FTB Quests did not load the Grindless chapter " + document.getString("id"));
            }
            Chapter expectedChapter = new Chapter(chapter.id, file, chapter.getGroup());
            expectedChapter.readData(document);
            sameData(expectedChapter, chapter);
            ListTag quests = document.getList("quests", Tag.TAG_COMPOUND);
            if (chapter.getQuests().size() != quests.size()) {
                throw new IllegalStateException("Unexpected native Grindless chapter contents: " + chapter.id);
            }
            for (int index = 0; index < quests.size(); index++) {
                CompoundTag definition = quests.getCompound(index);
                Quest quest = file.getQuest(parseId(definition));
                if (quest == null || quest.getChapter() != chapter) {
                    throw new IllegalStateException("Missing or displaced native Grindless quest: " + definition.getString("id"));
                }
                Quest expected = new Quest(quest.id, chapter);
                expected.readData(definition);
                sameData(expected, quest);
                ListTag tasks = definition.getList("tasks", Tag.TAG_COMPOUND);
                if (quest.getTasks().size() != 1 || tasks.size() != 1) {
                    throw new IllegalStateException("Unexpected native Grindless task count: " + quest.id);
                }
                CompoundTag taskDefinition = tasks.getCompound(0);
                Task task = file.getTask(parseId(taskDefinition));
                Task expectedTask = switch (taskDefinition.getString("type")) {
                    case "ftbquests:item" -> new ItemTask(parseId(taskDefinition), expected);
                    case "ftbquests:checkmark" -> new CheckmarkTask(parseId(taskDefinition), expected);
                    case "ftbquests:dimension" -> new DimensionTask(parseId(taskDefinition), expected);
                    default -> throw new IllegalStateException("Unsupported generated native task");
                };
                expectedTask.readData(taskDefinition);
                if (task == null || task.getQuest() != quest || task.getClass() != expectedTask.getClass()) {
                    throw new IllegalStateException("Missing or replaced native Grindless task: " + taskDefinition.getString("id"));
                }
                sameData(expectedTask, task);
                ListTag rewards = definition.getList("rewards", Tag.TAG_COMPOUND);
                if (quest.getRewards().size() != rewards.size()) {
                    throw new IllegalStateException("Unexpected native Grindless reward count: " + quest.id);
                }
                for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
                    CompoundTag rewardDefinition = rewards.getCompound(rewardIndex);
                    Reward reward = file.getReward(parseId(rewardDefinition));
                    ItemReward expectedReward = new ItemReward(parseId(rewardDefinition), expected);
                    expectedReward.readData(rewardDefinition);
                    if (reward == null || reward.getQuest() != quest || reward.getClass() != ItemReward.class) {
                        throw new IllegalStateException("Missing or replaced native Grindless item reward");
                    }
                    sameData(expectedReward, reward);
                }
            }
        }
    }

    public static void migrate(QuestProgress legacy, UUID player, TeamData nativeProgress) {
        if (player == null || nativeProgress == null) {
            throw new IllegalArgumentException("Native quest import requires a player and team progress");
        }
        Set<String> paid = legacy.legacyClaims(player);
        legacy.useNativeClaims(player);
        long now = System.currentTimeMillis();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            if (!paid.contains(task.id())) {
                continue;
            }
            for (long rewardId : FtbQuestDefinitions.rewardIds(task)) {
                Reward reward = nativeProgress.getFile().getReward(rewardId);
                if (reward == null || reward.isTeamReward()
                        || !nativeProgress.isRewardClaimed(player, reward)
                        && !nativeProgress.markRewardAsClaimed(player, reward, now)) {
                    throw new IllegalStateException("Cannot import paid native reward receipt: " + rewardId);
                }
            }
        }
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            if (!paid.contains(task.id()) || !QuestCatalogue.unlocked(QuestCatalogue.chapter(task), paid)) {
                continue;
            }
            Task nativeTask = nativeProgress.getFile().getTask(FtbQuestDefinitions.taskId(task.id()));
            Quest nativeQuest = nativeProgress.getFile().getQuest(FtbQuestDefinitions.questId(task.id()));
            if (nativeTask == null || nativeQuest == null || nativeProgress.isLocked()) {
                throw new IllegalStateException("Cannot import native quest completion: " + task.id());
            }
            if (nativeProgress.getProgress(nativeTask) < nativeTask.getMaxProgress()) {
                nativeProgress.setProgress(nativeTask, nativeTask.getMaxProgress());
            }
            if (!nativeProgress.isCompleted(nativeTask)) {
                nativeProgress.setCompleted(nativeTask.id, new Date(now));
            }
            if (!nativeProgress.isCompleted(nativeQuest)) {
                nativeProgress.setCompleted(nativeQuest.id, new Date(now));
            }
        }
        refresh(legacy, player, nativeProgress);
    }

    public static void refresh(QuestProgress legacy, UUID player, TeamData nativeProgress) {
        Set<String> completed = new LinkedHashSet<>();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            Quest quest = nativeProgress.getFile().getQuest(FtbQuestDefinitions.questId(task.id()));
            if (quest == null) {
                throw new IllegalStateException("Missing native quest during compatibility observation: " + task.id());
            }
            if (nativeProgress.isCompleted(quest)) {
                completed.add(task.id());
            }
        }
        legacy.updateNativeCompleted(player, completed);
    }

    private static void sameData(QuestObjectBase expected, QuestObjectBase actual) {
        CompoundTag expectedData = new CompoundTag();
        CompoundTag actualData = new CompoundTag();
        expected.writeData(expectedData);
        actual.writeData(actualData);
        if (!expectedData.equals(actualData)) {
            throw new IllegalStateException("Modified or colliding native Grindless definition: " + expected.getCodeString());
        }
    }

    private static long parseId(CompoundTag tag) {
        return Long.parseLong(tag.getString("id"), 16);
    }
}
