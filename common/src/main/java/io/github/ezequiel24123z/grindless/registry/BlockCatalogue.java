package io.github.ezequiel24123z.grindless.registry;

import java.util.List;
import java.util.Locale;

/**
 * Every Grindless block, what shape it is, and which states it can show.
 *
 * <p>Like {@code SupplyCatalogue}, this has no Minecraft imports so the asset generator can read
 * it: blockstates, models, textures, loot tables, tags and the server-side state scenario are all
 * written from this one list and so cannot drift from it. The block classes build their
 * {@code status} property from the same lists. {@code VerifyAssets} checks the list against
 * {@link ModBlocks}, which is where the blocks are actually registered.
 *
 * <p>Each machine has its own geometry, front panel, top and per-status textures, so two machines
 * are never a recolour of each other (ADR-0052).
 */
public final class BlockCatalogue {

    /** The silhouette a block is built as. */
    public enum Geometry {
        /** A flat base with a flywheel and a hand crank on the front. */
        DYNAMO,
        /** A heavy hopper-and-stack machine with a drill aperture on the front. */
        EXTRACTOR,
        /** A desk with a raked screen. */
        TERMINAL,
        /** A tall tower; taller and more ringed with each tier. */
        PYLON
    }

    /** A generator: it can have charge it cannot give away, but is never starved. */
    public static final List<String> GENERATOR = List.of("idle", "running", "blocked");

    /** The grid: it carries power, or is overdrawn. */
    public static final List<String> GRID = List.of("idle", "running", "starved");

    /** A consumer: it can be stuck on output or on input. */
    public static final List<String> CONSUMER = List.of("idle", "running", "blocked", "starved");

    /**
     * @param name     the registry path
     * @param geometry the silhouette
     * @param tier     1 to 3 for pylons, whose height and detail grow with it; 1 otherwise
     * @param facing   whether the block has a front and so a {@code facing} property
     * @param statuses the values of its {@code status} property, in {@code MachineStatus} names
     */
    public record Entry(String name, Geometry geometry, int tier, boolean facing, List<String> statuses) {

        /** The blockstate variant key for one state, such as {@code facing=north,status=idle}. */
        public String variantKey(String facingValue, String status) {
            return facing ? "facing=" + facingValue + ",status=" + status : "status=" + status;
        }

        /** The model name for one status, shared by every facing. */
        public String modelName(String status) {
            return name + "_" + status;
        }

        public String lower() {
            return geometry.name().toLowerCase(Locale.ROOT);
        }
    }

    /** The four horizontal facings and the Y rotation that turns a north-facing model to each. */
    public static final List<String> FACINGS = List.of("north", "east", "south", "west");
    public static final List<Integer> FACING_ROTATIONS = List.of(0, 90, 180, 270);

    private static final List<Entry> BLOCKS = List.of(
            new Entry("hand_crank_dynamo", Geometry.DYNAMO, 1, true, GENERATOR),
            new Entry("crude_extractor", Geometry.EXTRACTOR, 1, true, CONSUMER),
            new Entry("research_terminal", Geometry.TERMINAL, 1, true, CONSUMER),
            new Entry("flux_pylon_mk1", Geometry.PYLON, 1, false, GRID),
            new Entry("flux_pylon_mk2", Geometry.PYLON, 2, false, GRID),
            new Entry("flux_pylon_mk3", Geometry.PYLON, 3, false, GRID));

    /** Items that are not blocks and not material forms, which need a hand-drawn sprite. */
    private static final List<String> PLACEHOLDER_SPRITES = List.of("multitool", "data_core");

    /** The item that renders as the bare casing cube. */
    public static final String CASING_ITEM = "machine_casing";

    private BlockCatalogue() {
    }

    public static List<Entry> blocks() {
        return BLOCKS;
    }

    public static Entry block(String name) {
        return BLOCKS.stream().filter(b -> b.name().equals(name)).findFirst().orElseThrow();
    }

    public static List<String> placeholderSprites() {
        return PLACEHOLDER_SPRITES;
    }
}
