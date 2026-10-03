package io.github.ezequiel24123z.grindless.vein;

import io.github.ezequiel24123z.grindless.material.Material;

import java.util.List;

/**
 * Derives a chunk's Resource Vein from the world seed and the chunk's coordinates.
 *
 * <p>Nothing is stored until a vein is actually worked (ADR-0009). An unexplored chunk costs
 * exactly zero bytes, the same seed always produces the same map, and a world where the player
 * never builds an extractor generates no vein data at all.
 *
 * <h2>Versioning, which is not optional</h2>
 *
 * <p>Changing the derivation would silently rewrite every unexploited chunk in every existing
 * world — a player's surveyed-but-unbuilt copper chunk quietly becoming tin. So the version is
 * part of the hash and is saved alongside extraction data: a world generated under one version
 * keeps deriving under it, and a changed algorithm applies to new worlds rather than rewriting old
 * ones. ADR-0009 calls this out specifically.
 *
 * <p><b>Never change {@link #VERSION} and the mixing together.</b> Bumping the version is how a
 * deliberate change is made safe; editing the mix without bumping it is how a world silently
 * breaks.
 */
public final class VeinGenerator {

    /**
     * The derivation version.
     *
     * <p>Bump this when the mixing, the weighting or the richness curve changes. Worlds saved
     * under an older version keep using it.
     */
    public static final int VERSION = 1;

    /** Lowest richness a vein can roll: half rate. */
    public static final double MIN_RICHNESS = 0.5;

    /** Highest richness a vein can roll: double rate. */
    public static final double MAX_RICHNESS = 2.0;

    /**
     * Base reserve before richness, in units.
     *
     * <p>Sized in <em>hours</em> rather than picked as a round number. At the T1 Terrestrial
     * Extractor's reference rate of one unit every five seconds, this is roughly forty hours of
     * continuous extraction before a vein reaches its floor — and the gentle depletion curve means
     * most of that is spent near full rate.
     *
     * <p>Long on purpose. An outpost that needs relocating every session turns expansion into a
     * chore, which is the grind this mod exists to delete; the player should build a line, watch
     * it work, and expand because they want <em>more</em> rather than because the old one is
     * dying.
     */
    public static final long BASE_RESERVE = 30_000L;

    private VeinGenerator() {
    }

    /**
     * The vein owned by one chunk, or {@code null} if the pack offers nothing mineable.
     *
     * @param materials the mineable materials, in a stable order — the caller must guarantee the
     *                  order, since a set iterated differently between sessions would derive a
     *                  different map from the same seed
     */
    public static ChunkVein generate(long seed, int chunkX, int chunkZ, List<Material> materials) {
        if (materials.isEmpty()) {
            return null;
        }
        long hash = mix(seed, chunkX, chunkZ, VERSION);

        Material material = pickWeighted(materials, hash);
        // A second, independently mixed value, so richness is not correlated with the material
        // choice. Reusing one hash for both would make every copper chunk equally rich.
        double roll = unitFraction(mix(hash, chunkZ, chunkX, VERSION + 0x9E37));
        double richness = MIN_RICHNESS + (MAX_RICHNESS - MIN_RICHNESS) * roll;

        // Reserve scales with richness, which makes a vein's *lifetime* independent of how rich
        // it is: a rich chunk yields faster and holds proportionally more, so it runs out after
        // the same number of hours. Richness is therefore purely throughput — a find, not a
        // countdown — and a player never has to weigh "rich but short" against "poor but long",
        // which is a false choice nobody enjoys making.
        return new ChunkVein(material.name(), richness, (long) (BASE_RESERVE * richness), VERSION);
    }

    /**
     * Picks a material in proportion to its weight.
     *
     * <p>Rarer materials are weighted into fewer chunks, so finding a rich vein of something
     * scarce is a genuine event rather than a formality — which is the whole reason exploration
     * has a purpose in this design.
     */
    private static Material pickWeighted(List<Material> materials, long hash) {
        long total = 0L;
        for (Material material : materials) {
            total += material.weight();
        }
        if (total <= 0L) {
            return materials.get(0);
        }
        // Unsigned remainder: a negative hash would otherwise index backwards off the start.
        long pick = Long.remainderUnsigned(hash, total);
        for (Material material : materials) {
            pick -= material.weight();
            if (pick < 0L) {
                return material;
            }
        }
        return materials.get(materials.size() - 1);
    }

    /**
     * Mixes the inputs into a well-distributed hash.
     *
     * <p>The mixing matters as much here as it does for tick offsets. Chunk coordinates are small
     * and highly regular, and a weak hash would stripe the map — long runs of the same material
     * along an axis, which is exactly the pattern a player notices and the opposite of the
     * "every chunk is a find" feeling the design wants.
     */
    private static long mix(long seed, int x, int z, int version) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (long) version * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** Maps a hash into {@code [0, 1)}. */
    private static double unitFraction(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
