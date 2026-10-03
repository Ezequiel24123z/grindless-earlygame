package io.github.ezequiel24123z.grindless.vein;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * How much has been taken out of each worked chunk.
 *
 * <p>This is the <em>only</em> vein state that reaches disk (ADR-0009). Composition is derived
 * from the seed, so a chunk nobody has extracted from has no entry here at all — an unexplored
 * world costs nothing, however large.
 *
 * <h2>The derivation version is pinned per world</h2>
 *
 * <p>The version in use is recorded the first time this data is created and never changed
 * afterwards. If a later release alters the derivation, existing worlds keep deriving under the
 * version they were made with, and only new worlds see the new map. Without this, a mod update
 * would silently rewrite every surveyed-but-unbuilt chunk a player had planned around.
 */
public final class VeinData extends SavedData {

    /** The storage key. Per dimension, since chunk coordinates mean different places in each. */
    public static final String KEY = "grindless_veins";

    private static final String TAG_ENTRIES = "Extracted";
    private static final String TAG_CHUNK = "Chunk";
    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_VERSION = "DerivationVersion";

    private final Map<Long, Long> extracted = new HashMap<>();

    private int derivationVersion = VeinGenerator.VERSION;

    /** The vein data for {@code level}, creating it if this dimension has none yet. */
    public static VeinData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(VeinData::load, VeinData::new, KEY);
    }

    /**
     * The derivation version this world was created under.
     *
     * <p>Callers deriving a vein must pass this rather than {@link VeinGenerator#VERSION}, or a
     * future version bump would change the map underneath an existing save.
     */
    public int derivationVersion() {
        return derivationVersion;
    }

    /** How much has been taken from {@code chunk}. */
    public long extractedFrom(ChunkPos chunk) {
        return extracted.getOrDefault(chunk.toLong(), 0L);
    }

    /**
     * Records {@code amount} taken from {@code chunk}.
     *
     * <p>Only called when an extractor actually produces something, which is what keeps the saved
     * set to chunks a player has genuinely worked rather than every chunk they have walked
     * through.
     */
    public void recordExtraction(ChunkPos chunk, long amount) {
        if (amount <= 0L) {
            return;
        }
        extracted.merge(chunk.toLong(), amount, Long::sum);
        setDirty();
    }

    /** Every worked chunk, keyed by packed chunk position. */
    public Map<Long, Long> worked() {
        return Collections.unmodifiableMap(extracted);
    }

    /** How many chunks have been worked at all. */
    public int workedChunkCount() {
        return extracted.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<Long, Long> entry : extracted.entrySet()) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong(TAG_CHUNK, entry.getKey());
            chunkTag.putLong(TAG_AMOUNT, entry.getValue());
            entries.add(chunkTag);
        }
        tag.put(TAG_ENTRIES, entries);
        tag.putInt(TAG_VERSION, derivationVersion);
        return tag;
    }

    /**
     * Rebuilds from saved data.
     *
     * <p>A save written before the version field existed reads as version 1, which is correct:
     * that is the version those worlds were in fact generated under.
     */
    public static VeinData load(CompoundTag tag) {
        VeinData data = new VeinData();
        ListTag entries = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag chunkTag = entries.getCompound(i);
            data.extracted.put(chunkTag.getLong(TAG_CHUNK), chunkTag.getLong(TAG_AMOUNT));
        }
        int saved = tag.getInt(TAG_VERSION);
        data.derivationVersion = saved > 0 ? saved : 1;
        return data;
    }
}
