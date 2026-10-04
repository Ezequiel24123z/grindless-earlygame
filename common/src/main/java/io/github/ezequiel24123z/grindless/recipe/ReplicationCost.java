package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * What one item costs to replicate, walked from the recipe graph (ADR-0010, ADR-0088).
 *
 * <p>No Minecraft types. A line matches an item id, never a tag: expanding a tag to its members
 * is the Replicator's job (slice AH). An item with no line, and an item already on the stack,
 * costs {@link #FLOOR}. One output costs at least that floor and at least its inputs shared
 * across the line's output count, so a later replicator cannot undercut the craft.
 */
public final class ReplicationCost {

    /** The cost of a primitive, a cycle, or an item the graph does not name. */
    public static final int FLOOR = 1;

    /** One bucket, in millibuckets. A fluid input costs this many buckets, rounded up. */
    public static final int BUCKET_MB = 1000;

    private ReplicationCost() {
    }

    /**
     * One way to make {@code outputId}. Item inputs are item ids. Fluids are millibuckets and
     * are not themselves items.
     */
    public record Line(String outputId, int outputCount, List<ItemPart> items, int fluidMb) {

        public Line {
            items = List.copyOf(items);
            if (outputId == null || outputId.isBlank()) {
                throw new IllegalArgumentException("output id is required");
            }
            if (outputCount <= 0) {
                throw new IllegalArgumentException("output count must be positive");
            }
            if (fluidMb < 0) {
                throw new IllegalArgumentException("fluid cannot be negative");
            }
        }
    }

    /** One item input of a line. */
    public record ItemPart(String id, int count) {

        public ItemPart {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("item id is required");
            }
            if (count <= 0) {
                throw new IllegalArgumentException("count must be positive");
            }
        }
    }

    /** The cheapest cost of one {@code itemId} across {@code lines}. */
    public static int of(String itemId, List<Line> lines) {
        if (itemId == null || itemId.isBlank()) {
            return FLOOR;
        }
        return walk(itemId, lines == null ? List.of() : lines, new ArrayDeque<>());
    }

    /**
     * The same walk over live process recipes. Tag outputs are skipped. Tag inputs are not
     * expanded, so they add nothing until AH resolves them.
     */
    public static int ofRecipes(String itemId, List<ProcessRecipe> recipes) {
        return of(itemId, linesOf(recipes));
    }

    /** Item-id outputs of {@code recipes}, as lines. */
    public static List<Line> linesOf(List<ProcessRecipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }
        List<Line> lines = new ArrayList<>();
        for (ProcessRecipe recipe : recipes) {
            List<ItemPart> items = new ArrayList<>();
            int fluid = 0;
            for (IngredientSpec input : recipe.itemInputs()) {
                if (IngredientSpec.ITEM.equals(input.kind())) {
                    items.add(new ItemPart(input.id(), input.count()));
                }
            }
            for (IngredientSpec input : recipe.fluidInputs()) {
                fluid += input.count();
            }
            for (OutputSpec output : recipe.itemOutputs()) {
                if (!IngredientSpec.ITEM.equals(output.kind()) || output.vented()) {
                    continue;
                }
                lines.add(new Line(output.id(), output.count(), items, fluid));
            }
        }
        return List.copyOf(lines);
    }

    private static int walk(String itemId, List<Line> lines, ArrayDeque<String> stack) {
        if (stack.contains(itemId)) {
            return FLOOR;
        }
        int best = Integer.MAX_VALUE;
        boolean found = false;
        stack.push(itemId);
        for (Line line : lines) {
            if (!itemId.equals(line.outputId())) {
                continue;
            }
            found = true;
            long sum = fluidCost(line.fluidMb());
            for (ItemPart part : line.items()) {
                sum += (long) walk(part.id(), lines, stack) * part.count();
            }
            int share = share(sum, line.outputCount());
            if (share < best) {
                best = share;
            }
        }
        stack.pop();
        return found ? best : FLOOR;
    }

    /** Buckets of fluid, at least one when the line names any, otherwise zero. */
    public static int fluidCost(int fluidMb) {
        if (fluidMb <= 0) {
            return 0;
        }
        int buckets = (fluidMb + BUCKET_MB - 1) / BUCKET_MB;
        return Math.max(1, buckets);
    }

    /** One output's share of {@code inputSum}, never below the floor. */
    static int share(long inputSum, int outputCount) {
        if (outputCount <= 0) {
            return FLOOR;
        }
        long rounded = (Math.max(0L, inputSum) + outputCount - 1L) / outputCount;
        if (rounded < FLOOR) {
            return FLOOR;
        }
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }
}
