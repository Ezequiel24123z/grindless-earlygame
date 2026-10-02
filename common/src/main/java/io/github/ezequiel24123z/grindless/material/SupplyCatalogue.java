package io.github.ezequiel24123z.grindless.material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * What Grindless is willing to supply itself, for the case where nothing else in the pack does.
 *
 * <p>The runtime scan (ADR-0004) discovers every material in the pack, but an item has to exist
 * in the registry before any tag is read. So Grindless registers a finite <em>supply set</em> up
 * front, and decides at runtime, per material and form, whether each of those items is needed
 * (ADR-0050). With only Grindless installed, platinum exists because this catalogue lists it. With
 * another mod providing platinum, the same items stay registered — IDs must never depend on the
 * installed mod list, or removing a mod would turn every Grindless item in a save into a missing
 * mapping — but they are hidden, never output and never preferred.
 *
 * <p>This class has no Minecraft imports on purpose: the asset generator reads it too, so the list
 * of materials is written exactly once.
 *
 * <p>Vanilla is excluded structurally. Grindless never registers an iron ingot, a gold nugget or a
 * copper ingot, because vanilla already provides them and a duplicate would be a rival item under
 * the same tag. It does supply an iron <em>plate</em>, because vanilla has none.
 */
public final class SupplyCatalogue {

    /** The forms Grindless can supply, in creative-tab order. */
    private static final List<MaterialForm> FORMS = List.of(
            MaterialForm.RAW, MaterialForm.CRUSHED, MaterialForm.DUST, MaterialForm.NUGGET,
            MaterialForm.INGOT, MaterialForm.PLATE, MaterialForm.ROD, MaterialForm.BOLT,
            MaterialForm.GEAR, MaterialForm.RING);

    /**
     * One material Grindless can supply.
     *
     * @param name     the tag path name, such as {@code tin}
     * @param mineable whether it occurs in the ground; alloys do not, so they get neither raw nor crushed forms
     * @param vanilla  the forms vanilla already provides, which are never registered here
     */
    public record Supplied(String name, boolean mineable, Set<MaterialForm> vanilla) {
        public Supplied {
            vanilla = Set.copyOf(vanilla);
        }
    }

    /** One item Grindless registers: a material in a form. */
    public record Entry(String material, MaterialForm form) {
        /** The registry path, such as {@code tin_ingot} or {@code raw_tin}. */
        public String itemName() {
            return SupplyCatalogue.itemName(material, form);
        }
    }

    private static final List<Supplied> MATERIALS = List.of(
            vanillaBacked("iron", MaterialForm.RAW, MaterialForm.INGOT, MaterialForm.NUGGET),
            vanillaBacked("copper", MaterialForm.RAW, MaterialForm.INGOT),
            vanillaBacked("gold", MaterialForm.RAW, MaterialForm.INGOT, MaterialForm.NUGGET),
            ground("tin"),
            ground("lead"),
            ground("silver"),
            ground("nickel"),
            ground("zinc"),
            ground("aluminium"),
            ground("titanium"),
            ground("tungsten"),
            ground("platinum"),
            new Supplied("steel", false, Set.of()));

    private static final List<Entry> ENTRIES = buildEntries();

    private SupplyCatalogue() {
    }

    private static Supplied ground(String name) {
        return new Supplied(name, true, Set.of());
    }

    private static Supplied vanillaBacked(String name, MaterialForm first, MaterialForm... rest) {
        Set<MaterialForm> forms = EnumSet.of(first, rest);
        return new Supplied(name, true, forms);
    }

    private static List<Entry> buildEntries() {
        List<Entry> entries = new ArrayList<>();
        for (Supplied material : MATERIALS) {
            for (MaterialForm form : FORMS) {
                if (supplies(material, form)) {
                    entries.add(new Entry(material.name(), form));
                }
            }
        }
        return Collections.unmodifiableList(entries);
    }

    private static boolean supplies(Supplied material, MaterialForm form) {
        if (material.vanilla().contains(form)) {
            return false;
        }
        // Raw and crushed are steps of the ore line; an alloy has no ore to be either of.
        boolean oreLine = form == MaterialForm.RAW || form == MaterialForm.CRUSHED;
        return !oreLine || material.mineable();
    }

    /** The registry path of a material in a form: {@code raw_tin}, {@code tin_ingot}. */
    public static String itemName(String material, MaterialForm form) {
        return form == MaterialForm.RAW
                ? "raw_" + material
                : material + "_" + form.name().toLowerCase(Locale.ROOT);
    }

    public static List<Supplied> materials() {
        return MATERIALS;
    }

    /** Every item Grindless registers for materials, in a stable order. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** The forms Grindless can supply for some material, in a stable order. */
    public static List<MaterialForm> forms() {
        return FORMS;
    }

    /** Whether Grindless registers an item for this material and form. */
    public static boolean isSupplied(String material, MaterialForm form) {
        return find(material, form).isPresent();
    }

    public static Optional<Entry> find(String material, MaterialForm form) {
        for (Entry entry : ENTRIES) {
            if (entry.form() == form && entry.material().equals(material)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /** The entry registered under a Grindless item path, if the path is one of ours. */
    public static Optional<Entry> byItemName(String path) {
        for (Entry entry : ENTRIES) {
            if (entry.itemName().equals(path)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
}
