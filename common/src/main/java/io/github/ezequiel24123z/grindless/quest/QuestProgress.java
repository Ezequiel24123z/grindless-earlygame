package io.github.ezequiel24123z.grindless.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which tasks each player has claimed (ADR-0100).
 *
 * <p>Overworld {@code SavedData}, keyed by player. The book is the player's own route, so one
 * claim does not finish it for the server. Stored here, not on a Forge player tag, so
 * {@code common/} does not take a loader type.
 */
public final class QuestProgress extends SavedData {

    public static final String KEY = "grindless_quest_progress";

    private static final String TAG_PLAYERS = "Players";
    private static final String TAG_PLAYER = "Player";
    private static final String TAG_TASKS = "Tasks";

    private final Map<UUID, Set<String>> claimed = new HashMap<>();

    public static QuestProgress get(ServerPlayer player) {
        return player.server.overworld().getDataStorage()
                .computeIfAbsent(QuestProgress::load, QuestProgress::new, KEY);
    }

    /** A copy. Callers cannot mark a task claimed by editing the returned set. */
    public Set<String> claimed(UUID player) {
        return Set.copyOf(claimed.getOrDefault(player, Set.of()));
    }

    /**
     * Records one claim. Returns whether this call was the one that recorded it.
     * Unknown ids are refused so a stale button cannot invent a task.
     */
    public boolean claim(UUID player, String task) {
        if (player == null || QuestCatalogue.byId(task) == null) {
            return false;
        }
        Set<String> tasks = claimed.computeIfAbsent(player, id -> new HashSet<>());
        if (!tasks.add(task)) {
            return false;
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : claimed.entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putString(TAG_PLAYER, entry.getKey().toString());
            ListTag tasks = new ListTag();
            for (String id : entry.getValue()) {
                tasks.add(StringTag.valueOf(id));
            }
            one.put(TAG_TASKS, tasks);
            players.add(one);
        }
        tag.put(TAG_PLAYERS, players);
        return tag;
    }

    public static QuestProgress load(CompoundTag tag) {
        QuestProgress progress = new QuestProgress();
        ListTag players = tag.getList(TAG_PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag one = players.getCompound(i);
            UUID player;
            try {
                player = UUID.fromString(one.getString(TAG_PLAYER));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Set<String> tasks = new HashSet<>();
            ListTag list = one.getList(TAG_TASKS, Tag.TAG_STRING);
            for (int t = 0; t < list.size(); t++) {
                String id = list.getString(t);
                if (QuestCatalogue.byId(id) != null) {
                    tasks.add(id);
                }
            }
            if (!tasks.isEmpty()) {
                progress.claimed.put(player, tasks);
            }
        }
        return progress;
    }
}
