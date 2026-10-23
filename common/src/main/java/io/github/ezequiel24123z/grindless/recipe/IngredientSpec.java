package io.github.ezequiel24123z.grindless.recipe;

import com.google.gson.JsonObject;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;

import java.util.Set;

/**
 * One process input, as data.
 *
 * <p>Materials are always tags (ADR-0050). Fluids use the same record so the type is one union
 * (ADR-0058). Wet B1 is the first generated fluid input (ADR-0062).
 *
 * @param kind  {@code tag}, {@code item} or {@code fluid}
 * @param id    {@code namespace:path}, such as {@code forge:raw_materials/iron}
 * @param count units required
 * @param matrixRating exact or minimum Flux rating for a physical Control Matrix, or zero for ordinary items
 * @param matrixRole exact physical ingredient identity, or explicitly sufficient control capacity
 */
public record IngredientSpec(String kind, String id, int count, int matrixRating, MatrixRole matrixRole) {

    public static final String TAG = "tag";
    public static final String ITEM = "item";
    public static final String FLUID = "fluid";
    public static final String MINIMUM_CONTROL_TYPE = "grindless:minimum_control";
    public static final String MINIMUM_CONTROL_PREFIX = "minimum_control:";

    public enum MatrixRole {
        EXACT, MINIMUM_CONTROL
    }

    /** Existing rated ingredients retain exact item identity and rating. */
    public IngredientSpec(String kind, String id, int count, int matrixRating) {
        this(kind, id, count, matrixRating, MatrixRole.EXACT);
    }

    public IngredientSpec(String kind, String id, int count) {
        this(kind, id, count, 0);
    }

    public IngredientSpec {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (!TAG.equals(kind) && !ITEM.equals(kind) && !FLUID.equals(kind)) {
            throw new IllegalArgumentException("unknown ingredient kind: " + kind);
        }
        if (matrixRating < 0 || matrixRating >= FluxTier.values().length
                || (matrixRating > 0 && !ITEM.equals(kind))) {
            throw new IllegalArgumentException("invalid Control Matrix rating");
        }
        if (matrixRole == null || matrixRole == MatrixRole.MINIMUM_CONTROL && matrixRating == 0) {
            throw new IllegalArgumentException("minimum control requires an explicit physical Matrix rating");
        }
    }

    public static IngredientSpec tag(String id, int count) {
        return new IngredientSpec(TAG, id, count);
    }

    public static IngredientSpec item(String id, int count) {
        return new IngredientSpec(ITEM, id, count);
    }

    /** An item whose physical Control Matrix rating must match exactly. */
    public static IngredientSpec matrix(String id, FluxTier rating, int count) {
        if (rating == null || rating == FluxTier.F0) {
            throw new IllegalArgumentException("a Control Matrix must be F1 or higher");
        }
        return new IngredientSpec(ITEM, id, count, rating.ordinal());
    }

    /**
     * Sufficient physical control, regardless of architecture. The ID retains one canonical
     * supply option for projections; it is not an exact-architecture restriction (ADR-0176).
     */
    public static IngredientSpec minimumControl(String id, FluxTier rating, int count) {
        if (rating == null || rating == FluxTier.F0) {
            throw new IllegalArgumentException("a Control Matrix must be F1 or higher");
        }
        return new IngredientSpec(ITEM, id, count, rating.ordinal(), MatrixRole.MINIMUM_CONTROL);
    }

    /**
     * Pure manual metadata decoder, shared by the catalogue, audit and Forge bridge.
     * Only the actual registered Relay architecture supplies a canonical option today.
     * Unsupported frontiers fail visibly instead of inventing another architecture's item.
     */
    public static IngredientSpec manualControl(String tier) {
        FluxTier rating;
        try {
            rating = FluxTier.valueOf(tier);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IllegalArgumentException("Minimum control tier must name a supported Flux tier", invalid);
        }
        if (rating == FluxTier.F0 || !MatrixArchitecture.RELAY.supports(rating)) {
            throw new IllegalArgumentException("No registered canonical Matrix supply for minimum control " + tier);
        }
        return minimumControl(FabricationLogic.RELAY_MATRIX, rating, 1);
    }

    /** Strict custom ingredient schema; item/tag/NBT extras must never become ignored constraints. */
    public static IngredientSpec manualControl(JsonObject json) {
        if (json == null || !json.keySet().equals(Set.of("type", "tier"))
                || !json.get("type").isJsonPrimitive() || !json.getAsJsonPrimitive("type").isString()
                || !MINIMUM_CONTROL_TYPE.equals(json.get("type").getAsString())
                || !json.get("tier").isJsonPrimitive() || !json.getAsJsonPrimitive("tier").isString()) {
            throw new IllegalArgumentException("Minimum control requires exactly string type and tier fields");
        }
        return manualControl(json.get("tier").getAsString());
    }

    /** One cell of the existing shaped catalogue, not a new recipe engine. */
    public static IngredientSpec crafting(String declaration) {
        if (declaration != null && declaration.startsWith(MINIMUM_CONTROL_PREFIX)) {
            return manualControl(declaration.substring(MINIMUM_CONTROL_PREFIX.length()));
        }
        if (declaration != null && declaration.startsWith("item:")) {
            return item(declaration.substring(5), 1);
        }
        if (declaration != null && declaration.startsWith("tag:")) {
            return tag(declaration.substring(4), 1);
        }
        throw new IllegalArgumentException("Unsupported shaped ingredient: " + declaration);
    }

    public boolean isMinimumControl() {
        return matrixRole == MatrixRole.MINIMUM_CONTROL;
    }

    /** Call only after validating the actual ControlMatrixItem and its physical NBT. */
    public boolean acceptsMatrix(String itemId, ControlMatrixSpec physical) {
        if (matrixRating == 0 || itemId == null || physical == null) {
            return false;
        }
        return isMinimumControl() ? physical.canControl(FluxTier.values()[matrixRating])
                : id.equals(itemId) && physical.rating().ordinal() == matrixRating;
    }

    public static IngredientSpec fluid(String id, int count) {
        return new IngredientSpec(FLUID, id, count);
    }

    public boolean isFluid() {
        return FLUID.equals(kind);
    }

    public String qualified() {
        return kind + ":" + id;
    }
}
