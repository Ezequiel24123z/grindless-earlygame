package io.github.ezequiel24123z.grindless.material;

import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Chooses which item stands for a material and form when several mods provide one.
 *
 * <p>Grindless's own machines <em>output</em> a single item per material, and it must be the one
 * the rest of the pack would also settle on, or a base ends up with two kinds of tin that do not
 * stack. Inputs never need this: they accept the whole tag.
 *
 * <p>The order is: vanilla first, then namespaces the pack author named, then every other mod
 * alphabetically, and Grindless's own fallback last. Alphabetical is arbitrary but stable, which
 * is the property that matters — the choice must not change between reloads or between server and
 * client.
 */
public final class Unifier {

    private static final int VANILLA = 0;
    private static final int NAMED = 1;
    private static final int OTHER_MOD = 1_000_000;
    private static final int GRINDLESS = Integer.MAX_VALUE;

    private final List<String> priority;
    private final Comparator<ResourceLocation> order;

    /** @param priority namespaces to prefer after vanilla, most preferred first */
    public Unifier(List<String> priority) {
        this.priority = List.copyOf(priority);
        this.order = Comparator
                .comparingInt(this::rank)
                .thenComparing(ResourceLocation::getNamespace)
                .thenComparing(ResourceLocation::getPath);
    }

    private int rank(ResourceLocation id) {
        String namespace = id.getNamespace();
        if (namespace.equals("minecraft")) {
            return VANILLA;
        }
        if (namespace.equals(Grindless.MOD_ID)) {
            return GRINDLESS;
        }
        int named = priority.indexOf(namespace);
        return named >= 0 ? NAMED + named : OTHER_MOD;
    }

    /** The preferred item, or empty if there are none. */
    public Optional<ResourceLocation> pick(Collection<ResourceLocation> candidates) {
        return candidates.stream().min(order);
    }

    /** The candidates, most preferred first. */
    public List<ResourceLocation> sorted(Collection<ResourceLocation> candidates) {
        return candidates.stream().sorted(order).toList();
    }
}
