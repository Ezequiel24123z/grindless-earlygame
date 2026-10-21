package io.github.ezequiel24123z.grindless.forge.quest;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProjection;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Native FTB Quests chapter data; the framework owns screens, tasks, rewards and progress. */
public final class FtbQuestDefinitions {

    private FtbQuestDefinitions() {
    }

    public static long id(String kind, String key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("grindless/ftb/" + kind + "/" + key).getBytes(StandardCharsets.UTF_8));
            long result = ByteBuffer.wrap(hash).getLong() & Long.MAX_VALUE;
            if (result <= 1) {
                throw new IllegalStateException("Reserved native quest id for " + kind + "/" + key);
            }
            return result;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java must provide SHA-256", exception);
        }
    }

    public static long questId(String catalogueId) {
        if (QuestCatalogue.byId(catalogueId) == null) {
            throw new IllegalArgumentException("Unknown quest catalogue id: " + catalogueId);
        }
        return id("quest", catalogueId);
    }

    public static long taskId(String catalogueId) {
        questId(catalogueId);
        return id("task", catalogueId);
    }

    public static List<Long> rewardIds(QuestCatalogue.Task task) {
        return java.util.stream.IntStream.range(0, task.rewards().size())
                .mapToObj(index -> id("reward", task.id() + "/" + index)).toList();
    }

    public static String code(long id) {
        return String.format(Locale.ROOT, "%016X", id);
    }

    public static Map<String, CompoundTag> chapters() {
        Map<String, CompoundTag> chapters = new LinkedHashMap<>();
        Set<Long> ids = new HashSet<>();
        Map<String, Integer> depths = new LinkedHashMap<>();
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            depths.put(entry.task().id(), entry.task().requires().stream()
                    .filter(parent -> QuestCatalogue.chapter(QuestCatalogue.byId(parent))
                            == QuestCatalogue.chapter(entry.task()))
                    .mapToInt(parent -> depths.get(parent) + 1).max().orElse(0));
        }
        for (QuestCatalogue.Chapter chapter : QuestCatalogue.Chapter.values()) {
            String name = "grindless_" + chapter.name().toLowerCase(Locale.ROOT);
            CompoundTag document = new CompoundTag();
            document.putString("id", unique(ids, id("chapter", chapter.name())));
            document.putString("filename", name);
            document.putString("title", json(Component.literal("Grindless " + chapter.name())));
            document.putInt("order_index", chapter.ordinal());
            document.putString("progression_mode", "linear");
            document.putBoolean("consume_items", false);
            document.putBoolean("default_repeatable_quest", false);
            ListTag quests = new ListTag();
            int lineTop = 0;
            for (String line : QuestCatalogue.lines(chapter)) {
                Map<Integer, Integer> rows = new LinkedHashMap<>();
                for (QuestProjection.Entry entry : QuestProjection.entries()) {
                    QuestCatalogue.Task task = entry.task();
                    if (QuestCatalogue.chapter(task) != chapter || !task.line().equals(line)) {
                        continue;
                    }
                    int depth = depths.get(task.id());
                    int row = rows.getOrDefault(depth, 0);
                    rows.put(depth, row + 1);
                    quests.add(quest(entry, depth * 2.5, lineTop + row * 1.5, ids));
                }
                lineTop += rows.values().stream().mapToInt(Integer::intValue).max().orElse(1) * 2 + 1;
            }
            document.put("quests", quests);
            chapters.put(name + ".snbt", document);
        }
        return Collections.unmodifiableMap(chapters);
    }

    private static CompoundTag quest(QuestProjection.Entry entry, double x, double y, Set<Long> ids) {
        QuestCatalogue.Task task = entry.task();
        CompoundTag quest = new CompoundTag();
        quest.putString("id", unique(ids, questId(task.id())));
        quest.putString("title", json(Component.translatable("gui.grindless.quest.task." + task.id())));
        quest.putDouble("x", x);
        quest.putDouble("y", y);
        quest.put("tags", strings(List.of(entry.id())));
        quest.put("dependencies", strings(QuestCatalogue.requirements(task).stream()
                .map(parent -> code(questId(parent))).toList()));
        quest.putBoolean("can_repeat", false);
        quest.putString("progression_mode", "linear");
        quest.putBoolean("hide_until_deps_complete", QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1);
        quest.putBoolean("hide_details_until_startable", QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1);
        ListTag description = new ListTag();
        description.add(StringTag.valueOf(json(body(task))));
        if (task.id().equals("fluid_logistics")) {
            description.add(StringTag.valueOf(json(Component.translatable("gui.grindless.quest.tank_containers",
                    FluidLogic.TANK_CAPACITY))));
        }
        quest.put("description", description);
        ListTag tasks = new ListTag();
        CompoundTag evidence = new CompoundTag();
        evidence.putString("id", unique(ids, taskId(task.id())));
        evidence.putString("type", switch (task.evidence()) {
            case ITEM -> "ftbquests:item";
            case MATRIX -> "grindless:matrix";
            case FREE -> "ftbquests:checkmark";
            case DIMENSION -> "ftbquests:dimension";
        });
        if (task.evidence() == QuestCatalogue.Evidence.ITEM || task.evidence() == QuestCatalogue.Evidence.MATRIX) {
            evidence.putString("item", task.subject());
            evidence.putLong("count", task.count());
            evidence.putBoolean("consume_items", false);
            evidence.putBoolean("only_from_crafting", false);
            evidence.putBoolean("match_nbt", false);
        } else if (task.evidence() == QuestCatalogue.Evidence.DIMENSION) {
            evidence.putString("dimension", task.subject());
        }
        if (task.evidence() == QuestCatalogue.Evidence.MATRIX) {
            evidence.putString("grindless_task", task.id());
        }
        tasks.add(evidence);
        quest.put("tasks", tasks);
        ListTag rewards = new ListTag();
        List<Long> rewardIds = rewardIds(task);
        for (int index = 0; index < task.rewards().size(); index++) {
            QuestCatalogue.Reward declared = task.rewards().get(index);
            CompoundTag reward = new CompoundTag();
            reward.putString("id", unique(ids, rewardIds.get(index)));
            reward.putString("type", "ftbquests:item");
            reward.putString("item", declared.itemId());
            reward.putInt("count", declared.count());
            reward.putBoolean("team_reward", false);
            reward.putString("auto", "disabled");
            rewards.add(reward);
        }
        quest.put("rewards", rewards);
        return quest;
    }

    private static Component body(QuestCatalogue.Task task) {
        return switch (task.id()) {
            case "manual_work" -> Component.translatable(QuestTeaching.bodyKey(task),
                    SourceLogic.MANUAL_CHARGE, SourceLogic.MANUAL_COOLDOWN);
            case "biomass_dynamo" -> Component.translatable(QuestTeaching.bodyKey(task),
                    FluxTier.F0.nominal(), SourceLogic.BIOMASS_BURN_TICKS);
            default -> Component.translatable(QuestTeaching.bodyKey(task));
        };
    }

    private static String json(Component component) {
        return Component.Serializer.toJson(component);
    }

    private static ListTag strings(List<String> values) {
        ListTag result = new ListTag();
        values.forEach(value -> result.add(StringTag.valueOf(value)));
        return result;
    }

    private static String unique(Set<Long> ids, long id) {
        if (!ids.add(id)) {
            throw new IllegalStateException("Duplicate native quest object id: " + code(id));
        }
        return code(id);
    }
}
