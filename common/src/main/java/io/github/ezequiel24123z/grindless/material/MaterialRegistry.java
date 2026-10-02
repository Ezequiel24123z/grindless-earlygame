package io.github.ezequiel24123z.grindless.material;

import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The runtime, tag-driven view of which materials exist and who provides them.
 *
 * <p>Grindless registers a finite supply set up front ({@link SupplyCatalogue}) because items must
 * exist before tags are read. This class is the other half: after every datapack load it scans the
 * tags the pack actually has and records, for each material and form, which items fill it. That
 * answers the two questions compatibility depends on — what can a recipe accept, and is
 * Grindless's own item even needed here (ADR-0050).
 *
 * <p>The snapshot is JVM-global and replaced atomically. In an integrated server the client and
 * server see the same synced tags, so one scan serves both.
 */
public final class MaterialRegistry {

    private static volatile MaterialSnapshot snapshot = MaterialSnapshot.EMPTY;
    private static volatile Unifier unifier = new Unifier(List.of());

    private MaterialRegistry() {
    }

    public static void bootstrap() {
        Grindless.LOG.debug("material registry bootstrapped (tag scan runs when tags are loaded)");
    }

    /** Re-reads the pack. Called whenever tags are loaded or reloaded. */
    public static void rebuild(TagView tags) {
        MaterialSnapshot next = MaterialSnapshot.scan(tags, unifier);
        snapshot = next;
        Grindless.LOG.info("[{}] {} materials found; Grindless supplies {} of its {} items",
                Grindless.MOD_NAME, next.materials().size(),
                next.activeFallbacks().size(), SupplyCatalogue.entries().size());
    }

    public static MaterialSnapshot snapshot() {
        return snapshot;
    }

    /** Sets the namespaces preferred after vanilla when several mods provide the same thing. */
    public static void setPreferredNamespaces(List<String> namespaces) {
        unifier = new Unifier(namespaces);
    }

    /**
     * Whether a Grindless item is a supply item that the pack already makes redundant.
     *
     * <p>Items that are not supply items (machines, the multitool) are never redundant.
     */
    public static boolean isRedundantFallback(ResourceLocation itemId) {
        if (!itemId.getNamespace().equals(Grindless.MOD_ID)) {
            return false;
        }
        Optional<SupplyCatalogue.Entry> entry = SupplyCatalogue.byItemName(itemId.getPath());
        return entry.isPresent()
                && !snapshot.fallbackActive(entry.get().material(), entry.get().form());
    }

    /** The item to output for a material and form, honouring the pack's preference. */
    public static Optional<ResourceLocation> output(String material, MaterialForm form) {
        return snapshot.preferred(material, form);
    }
}
