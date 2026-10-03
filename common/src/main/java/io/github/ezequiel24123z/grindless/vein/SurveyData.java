package io.github.ezequiel24123z.grindless.vein;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Which chunks a player has surveyed with the Prospector's Scanner.
 *
 * <p>The vein itself is still derived (ADR-0009). This set is only the "I have looked"
 * flag the Terrestrial Extractor reads.
 */
public final class SurveyData extends SavedData {

    public static final String KEY = "grindless_surveys";

    private static final String TAG_CHUNKS = "Chunks";

    private final Set<Long> surveyed = new HashSet<>();

    public static SurveyData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SurveyData::load, SurveyData::new, KEY);
    }

    public boolean isSurveyed(ChunkPos chunk) {
        return surveyed.contains(chunk.toLong());
    }

    public void mark(ChunkPos chunk) {
        if (surveyed.add(chunk.toLong())) {
            setDirty();
        }
    }

    public Set<Long> surveyed() {
        return Collections.unmodifiableSet(surveyed);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] packed = new long[surveyed.size()];
        int i = 0;
        for (long chunk : surveyed) {
            packed[i++] = chunk;
        }
        tag.put(TAG_CHUNKS, new LongArrayTag(packed));
        return tag;
    }

    public static SurveyData load(CompoundTag tag) {
        SurveyData data = new SurveyData();
        for (long chunk : tag.getLongArray(TAG_CHUNKS)) {
            data.surveyed.add(chunk);
        }
        return data;
    }
}
