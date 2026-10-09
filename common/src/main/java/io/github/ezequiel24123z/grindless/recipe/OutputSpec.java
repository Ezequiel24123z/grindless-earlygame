package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/**
 * One process output, as data.
 *
 * <p>Item outputs resolve through the material registry so a pack's preferred ingot is what
 * leaves the machine (ADR-0050). Fluid outputs exist on the type from day one; a vented fluid
 * is the named T1 sink (ADR-0036, ADR-0058) and is not produced as an item.
 *
 * @param kind   {@code tag}, {@code item} or {@code fluid}
 * @param id     {@code namespace:path}
 * @param count  units produced
 * @param vented whether this output is discarded by a named sink rather than stored
 * @param matrixRating physical Flux rating written to a Control Matrix, or zero for ordinary items
 */
public record OutputSpec(String kind, String id, int count, boolean vented, int matrixRating) {

    public OutputSpec(String kind, String id, int count, boolean vented) {
        this(kind, id, count, vented, 0);
    }

    public OutputSpec {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (!IngredientSpec.TAG.equals(kind)
                && !IngredientSpec.ITEM.equals(kind)
                && !IngredientSpec.FLUID.equals(kind)) {
            throw new IllegalArgumentException("unknown output kind: " + kind);
        }
        if (matrixRating < 0 || matrixRating >= FluxTier.values().length
                || (matrixRating > 0 && (!IngredientSpec.ITEM.equals(kind) || vented))) {
            throw new IllegalArgumentException("invalid Control Matrix rating");
        }
    }

    public static OutputSpec tag(String id, int count) {
        return new OutputSpec(IngredientSpec.TAG, id, count, false);
    }

    public static OutputSpec item(String id, int count) {
        return new OutputSpec(IngredientSpec.ITEM, id, count, false);
    }

    /** An item result carrying a physical Control Matrix rating. */
    public static OutputSpec matrix(String id, FluxTier rating, int count) {
        if (rating == null || rating == FluxTier.F0) {
            throw new IllegalArgumentException("a Control Matrix must be F1 or higher");
        }
        return new OutputSpec(IngredientSpec.ITEM, id, count, false, rating.ordinal());
    }

    public static OutputSpec fluid(String id, int count) {
        return new OutputSpec(IngredientSpec.FLUID, id, count, false);
    }

    public static OutputSpec ventedFluid(String id, int count) {
        return new OutputSpec(IngredientSpec.FLUID, id, count, true);
    }

    public boolean isFluid() {
        return IngredientSpec.FLUID.equals(kind);
    }

    public boolean isItem() {
        return !isFluid();
    }

    public String qualified() {
        return kind + ":" + id;
    }
}
