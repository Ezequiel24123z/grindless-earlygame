package io.github.ezequiel24123z.grindless.research;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Client copy of the world's unlocked blueprints.
 *
 * <p>Forge recipe conditions are load-time, not world-scoped. Gating therefore lives in
 * {@code Recipe#matches}, and the client needs this cache so the crafting grid and the recipe
 * book agree with the server (ADR-0057, ADR-0059).
 */
public final class ClientResearch {

    private static Set<Blueprint> unlocked = EnumSet.noneOf(Blueprint.class);

    private ClientResearch() {
    }

    public static boolean isUnlocked(Blueprint blueprint) {
        return unlocked.contains(blueprint);
    }

    public static void replace(Set<Blueprint> next) {
        unlocked = next.isEmpty() ? EnumSet.noneOf(Blueprint.class) : EnumSet.copyOf(next);
    }

    public static Set<Blueprint> unlocked() {
        return Collections.unmodifiableSet(unlocked);
    }
}
