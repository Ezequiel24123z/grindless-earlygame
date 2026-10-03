package io.github.ezequiel24123z.grindless.recipe;

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
 */
public record OutputSpec(String kind, String id, int count, boolean vented) {

    public OutputSpec {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (!IngredientSpec.TAG.equals(kind)
                && !IngredientSpec.ITEM.equals(kind)
                && !IngredientSpec.FLUID.equals(kind)) {
            throw new IllegalArgumentException("unknown output kind: " + kind);
        }
    }

    public static OutputSpec tag(String id, int count) {
        return new OutputSpec(IngredientSpec.TAG, id, count, false);
    }

    public static OutputSpec item(String id, int count) {
        return new OutputSpec(IngredientSpec.ITEM, id, count, false);
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
