package io.github.ezequiel24123z.grindless.registry;

import java.util.List;

/**
 * Which model each Grindless block uses, and which hand-made items need a placeholder sprite.
 *
 * <p>Like {@code SupplyCatalogue}, this has no Minecraft imports so the asset generator can read
 * it: blockstates, models, loot tables and tags are written from this one list and so cannot drift
 * from it. {@code VerifyAssets} checks the list against {@link ModBlocks}, which is where the
 * blocks are actually registered.
 *
 * <p>These are deliberately plain: a casing with one decorated face. Hero models are a named gap
 * in ADR-0048; this is the floor that makes every block render, drop itself and be mineable.
 */
public final class BlockCatalogue {

    /** How a block is drawn. */
    public enum Shape {
        /** One decorated face and plain casing elsewhere. Without a facing property, always north. */
        ORIENTABLE,
        /** The same decorated face on all four sides. */
        COLUMN
    }

    /**
     * @param name the registry path
     * @param shape how the model is built
     * @param face the decorated-face texture, under {@code textures/block}
     */
    public record Entry(String name, Shape shape, String face) {
    }

    private static final List<Entry> BLOCKS = List.of(
            new Entry("hand_crank_dynamo", Shape.ORIENTABLE, "face_coil"),
            new Entry("crude_extractor", Shape.ORIENTABLE, "face_aperture"),
            new Entry("research_terminal", Shape.ORIENTABLE, "face_gauge"),
            new Entry("flux_pylon_mk1", Shape.COLUMN, "face_vent"),
            new Entry("flux_pylon_mk2", Shape.COLUMN, "face_coil"),
            new Entry("flux_pylon_mk3", Shape.COLUMN, "face_aperture"));

    /** Items that are not blocks and not material forms, which need a hand-drawn sprite. */
    private static final List<String> PLACEHOLDER_SPRITES = List.of("multitool", "data_core");

    /** The item that renders as the bare casing cube. */
    public static final String CASING_ITEM = "machine_casing";

    private BlockCatalogue() {
    }

    public static List<Entry> blocks() {
        return BLOCKS;
    }

    public static List<String> placeholderSprites() {
        return PLACEHOLDER_SPRITES;
    }
}
