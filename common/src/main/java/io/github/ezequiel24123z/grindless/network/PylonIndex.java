package io.github.ezequiel24123z.grindless.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the pylons covering a position without looking at every pylon in the world.
 *
 * <h2>Why an index at all</h2>
 *
 * <p>A machine needs to know which network powers it, and the naive answer — scan every pylon and
 * test its supply cube — is O(pylons) per machine. In a base with hundreds of pylons and thousands
 * of machines that is the per-tick graph walk the design explicitly rules out (ADR-0007).
 *
 * <h2>How it avoids it</h2>
 *
 * <p>Each pylon is filed under <em>every chunk its supply cube touches</em>. A lookup then hashes
 * the query position's chunk once and tests only the handful of pylons filed there — in practice
 * zero to three, and never more than a base actually overlaps in one place. Cost tracks local
 * density rather than world size, which is the property that matters.
 *
 * <p>The buckets stay small because supply cubes are small: an MK3's 64-block cube spans at most
 * five chunks on a side, so one pylon occupies at most 25 buckets and most occupy four.
 *
 * <p>Not thread-safe; this is server-thread state held by {@link FluxNetworkData}.
 */
public final class PylonIndex {

    private final Map<Long, List<BlockPos>> byChunk = new HashMap<>();
    private final Map<BlockPos, PylonTier> pylons = new HashMap<>();

    /** Every pylon known to this index, with its tier. */
    public Map<BlockPos, PylonTier> pylons() {
        return Collections.unmodifiableMap(pylons);
    }

    /** How many pylons are indexed. */
    public int size() {
        return pylons.size();
    }

    /** The tier of the pylon at {@code pos}, or {@code null} if there is none. */
    public PylonTier tierAt(BlockPos pos) {
        return pylons.get(pos);
    }

    public boolean contains(BlockPos pos) {
        return pylons.containsKey(pos);
    }

    /**
     * Files a pylon, replacing any entry already at that position.
     *
     * <p>Positions are copied with {@code immutable()} before being stored. Minecraft hands out
     * mutable {@code BlockPos} instances in iteration loops, and storing one as a map key means
     * the key mutates underneath the map — a genuinely baffling bug, since the entry is still
     * present but can no longer be found.
     */
    public void add(BlockPos pos, PylonTier tier) {
        BlockPos key = pos.immutable();
        if (pylons.containsKey(key)) {
            remove(key);
        }
        pylons.put(key, tier);
        forEachCoveredChunk(key, tier, chunk ->
                byChunk.computeIfAbsent(chunk, c -> new ArrayList<>(1)).add(key));
    }

    /** Removes the pylon at {@code pos}, if any. */
    public void remove(BlockPos pos) {
        BlockPos key = pos.immutable();
        PylonTier tier = pylons.remove(key);
        if (tier == null) {
            return;
        }
        forEachCoveredChunk(key, tier, chunk -> {
            List<BlockPos> bucket = byChunk.get(chunk);
            if (bucket != null) {
                bucket.remove(key);
                // Empty buckets are dropped rather than left behind. A base that is built and
                // demolished repeatedly would otherwise accumulate them for the world's lifetime.
                if (bucket.isEmpty()) {
                    byChunk.remove(chunk);
                }
            }
        });
    }

    /** Every pylon whose supply cube contains {@code pos}. */
    public List<BlockPos> covering(BlockPos pos) {
        List<BlockPos> bucket = byChunk.get(ChunkPos.asLong(pos));
        if (bucket == null || bucket.isEmpty()) {
            return List.of();
        }
        List<BlockPos> covering = new ArrayList<>(1);
        for (BlockPos pylon : bucket) {
            PylonTier tier = pylons.get(pylon);
            // The bucket is filed by chunk, so a pylon can be in it without covering this
            // particular block: the cube clips the chunk's corner. The exact test is still needed.
            if (tier != null && tier.covers(pylon, pos)) {
                covering.add(pylon);
            }
        }
        return covering;
    }

    /**
     * Every pylon that links automatically to one of {@code tier} at {@code pos}, excluding
     * itself.
     *
     * <p>Link range exceeds supply range at every tier, so the coverage buckets cannot answer
     * this — a pylon can be in range to link while covering nothing in common. Candidates are
     * gathered from the chunks within link range instead.
     */
    public List<BlockPos> linkedTo(BlockPos pos, PylonTier tier) {
        BlockPos key = pos.immutable();
        Set<BlockPos> candidates = new HashSet<>();
        int chunkReach = (tier.linkRange() >> 4) + 1;
        int centreX = key.getX() >> 4;
        int centreZ = key.getZ() >> 4;
        for (int dx = -chunkReach; dx <= chunkReach; dx++) {
            for (int dz = -chunkReach; dz <= chunkReach; dz++) {
                List<BlockPos> bucket = byChunk.get(ChunkPos.asLong(centreX + dx, centreZ + dz));
                if (bucket != null) {
                    candidates.addAll(bucket);
                }
            }
        }
        List<BlockPos> linked = new ArrayList<>(1);
        for (BlockPos other : candidates) {
            if (other.equals(key)) {
                continue;
            }
            PylonTier otherTier = pylons.get(other);
            if (otherTier != null && PylonTier.linksAutomatically(tier, key, otherTier, other)) {
                linked.add(other);
            }
        }
        return linked;
    }

    /** Forgets every pylon. */
    public void clear() {
        byChunk.clear();
        pylons.clear();
    }

    private void forEachCoveredChunk(BlockPos pos, PylonTier tier, java.util.function.LongConsumer action) {
        int r = tier.radius();
        int minChunkX = (pos.getX() - r) >> 4;
        int maxChunkX = (pos.getX() + r) >> 4;
        int minChunkZ = (pos.getZ() - r) >> 4;
        int maxChunkZ = (pos.getZ() + r) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                action.accept(ChunkPos.asLong(x, z));
            }
        }
    }
}
