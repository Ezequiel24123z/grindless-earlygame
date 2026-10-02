package io.github.ezequiel24123z.grindless.material;

import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * An immutable picture of the pack's materials at one moment: the result of one tag scan.
 *
 * <p>Replaced wholesale on every datapack reload rather than mutated, so a thread reading it while
 * a reload runs sees either the old pack or the new one and never a half-built mixture.
 */
public final class MaterialSnapshot {

    public static final MaterialSnapshot EMPTY =
            new MaterialSnapshot(Map.of(), Map.of(), Set.of());

    private record Key(String material, MaterialForm form) {
    }

    private final Map<String, Material> materials;
    private final Map<Key, List<ResourceLocation>> items;
    private final Set<Key> foreign;

    private MaterialSnapshot(Map<String, Material> materials,
                             Map<Key, List<ResourceLocation>> items, Set<Key> foreign) {
        this.materials = materials;
        this.items = items;
        this.foreign = foreign;
    }

    /** Reads every material and form out of {@code tags}. */
    public static MaterialSnapshot scan(TagView tags, Unifier unifier) {
        Map<String, Set<MaterialForm>> forms = new TreeMap<>();
        Map<Key, List<ResourceLocation>> items = new HashMap<>();
        Set<Key> foreign = new HashSet<>();

        for (MaterialForm form : MaterialForm.values()) {
            for (String name : tags.materials(form)) {
                Set<ResourceLocation> found = tags.items(form, name);
                if (found.isEmpty()) {
                    continue;
                }
                Key key = new Key(name, form);
                items.put(key, Collections.unmodifiableList(unifier.sorted(found)));
                forms.computeIfAbsent(name, n -> EnumSet.noneOf(MaterialForm.class)).add(form);
                if (found.stream().anyMatch(id -> !id.getNamespace().equals(Grindless.MOD_ID))) {
                    foreign.add(key);
                }
            }
        }

        Map<String, Material> materials = new TreeMap<>();
        forms.forEach((name, present) ->
                materials.put(name, new Material(name, present, Material.DEFAULT_WEIGHT)));
        return new MaterialSnapshot(
                Collections.unmodifiableMap(materials),
                Collections.unmodifiableMap(items),
                Collections.unmodifiableSet(foreign));
    }

    public Optional<Material> material(String name) {
        return Optional.ofNullable(materials.get(name));
    }

    /** Every material the pack provides, in name order. */
    public Collection<Material> materials() {
        return materials.values();
    }

    /** Every item filling this slot, most preferred first; empty if nothing provides it. */
    public List<ResourceLocation> providers(String material, MaterialForm form) {
        return items.getOrDefault(new Key(material, form), List.of());
    }

    /** The item Grindless should output for this slot, or empty if nothing provides it. */
    public Optional<ResourceLocation> preferred(String material, MaterialForm form) {
        List<ResourceLocation> providers = providers(material, form);
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    /**
     * Whether Grindless's own item is the only thing filling this slot, and so is needed.
     *
     * <p>True also when nothing is loaded yet, so that before the first scan nothing is hidden by
     * mistake. False means another mod or vanilla already provides it, and Grindless's item should
     * stay out of creative tabs, recipe output and worldgen.
     */
    public boolean fallbackActive(String material, MaterialForm form) {
        return !foreign.contains(new Key(material, form));
    }

    /** The names of every slot Grindless's fallback currently fills. */
    public List<String> activeFallbacks() {
        List<String> active = new ArrayList<>();
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            if (fallbackActive(entry.material(), entry.form())) {
                active.add(entry.itemName());
            }
        }
        return active;
    }
}
