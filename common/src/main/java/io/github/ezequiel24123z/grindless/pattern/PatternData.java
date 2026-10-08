package io.github.ezequiel24123z.grindless.pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Item ids this world has scanned.
 *
 * <p>World-scoped: a pattern is a factory fact, not a personal one. Storing an id
 * twice is a no-op. The Replicator (slice AH) reads this set; it is not built here (ADR-0101).
 */
public final class PatternData extends SavedData {

    public static final String KEY = "grindless_patterns";

    private static final String TAG_PATTERNS = "Patterns";

    private final LinkedHashSet<String> patterns = new LinkedHashSet<>();

    public static PatternData get(ServerLevel level) {
        return get(level.getServer());
    }

    /** The overworld copy, so every dimension shares the catalogue. */
    public static PatternData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(PatternData::load, PatternData::new, KEY);
    }

    public boolean has(String id) {
        return id != null && patterns.contains(id);
    }

    /**
     * Records {@code id}. Returns whether this call was the one that stored it.
     */
    public boolean store(String id) {
        if (id == null || id.isBlank() || !patterns.add(id)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Set<String> patterns() {
        return Collections.unmodifiableSet(patterns);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String id : patterns) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(TAG_PATTERNS, list);
        return tag;
    }

    public static PatternData load(CompoundTag tag) {
        PatternData data = new PatternData();
        ListTag list = tag.getList(TAG_PATTERNS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (id != null && !id.isBlank()) {
                data.patterns.add(id);
            }
        }
        return data;
    }
}
