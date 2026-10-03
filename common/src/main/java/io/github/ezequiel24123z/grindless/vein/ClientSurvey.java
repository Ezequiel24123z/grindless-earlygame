package io.github.ezequiel24123z.grindless.vein;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client cache of recently surveyed chunks, for the scanner overlay.
 */
public final class ClientSurvey {

    public record Entry(int x, int z, String material, double richness, long reserve) {
        public long packed() {
            return net.minecraft.world.level.ChunkPos.asLong(x, z);
        }
    }

    private static final Map<Long, Entry> CHUNKS = new HashMap<>();

    private ClientSurvey() {
    }

    public static void replace(List<Entry> next) {
        CHUNKS.clear();
        for (Entry entry : next) {
            CHUNKS.put(entry.packed(), entry);
        }
    }

    public static void merge(List<Entry> next) {
        for (Entry entry : next) {
            CHUNKS.put(entry.packed(), entry);
        }
    }

    public static Entry at(int x, int z) {
        return CHUNKS.get(net.minecraft.world.level.ChunkPos.asLong(x, z));
    }

    public static Map<Long, Entry> all() {
        return Collections.unmodifiableMap(CHUNKS);
    }
}
