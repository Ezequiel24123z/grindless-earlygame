package io.github.ezequiel24123z.grindless.recipe;

/**
 * One process input, as data.
 *
 * <p>Materials are always tags (ADR-0050). Fluids use the same record so the type is one union
 * (ADR-0058); slice A never generates a fluid input.
 *
 * @param kind  {@code tag}, {@code item} or {@code fluid}
 * @param id    {@code namespace:path}, such as {@code forge:raw_materials/iron}
 * @param count units required
 */
public record IngredientSpec(String kind, String id, int count) {

    public static final String TAG = "tag";
    public static final String ITEM = "item";
    public static final String FLUID = "fluid";

    public IngredientSpec {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (!TAG.equals(kind) && !ITEM.equals(kind) && !FLUID.equals(kind)) {
            throw new IllegalArgumentException("unknown ingredient kind: " + kind);
        }
    }

    public static IngredientSpec tag(String id, int count) {
        return new IngredientSpec(TAG, id, count);
    }

    public static IngredientSpec item(String id, int count) {
        return new IngredientSpec(ITEM, id, count);
    }

    public boolean isFluid() {
        return FLUID.equals(kind);
    }

    public String qualified() {
        return kind + ":" + id;
    }
}
