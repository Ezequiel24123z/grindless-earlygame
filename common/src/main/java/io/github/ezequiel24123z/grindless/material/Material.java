package io.github.ezequiel24123z.grindless.material;

import java.util.Collections;
import java.util.Set;

/**
 * One material discovered in the loaded pack: copper, iron, cobalt, whatever is installed.
 *
 * <p>Materials are never authored. They are found by scanning the pack's tags at runtime
 * (ADR-0004), so a mod Grindless has never heard of is supported with no patch and no config.
 *
 * <p>A material carries only what the scan can honestly know: its name, which forms the pack
 * actually provides, and a vein weight. Everything else — how it processes, what it alloys with —
 * comes from the recipe graph rather than from a property here.
 *
 * @param name   the material's path name, such as {@code copper}
 * @param forms  the forms the pack provides for it
 * @param weight relative likelihood of a chunk owning this material; higher is commoner
 */
public record Material(String name, Set<MaterialForm> forms, int weight) {

    /** Weight given to a material whose rarity cannot be inferred from the pack. */
    public static final int DEFAULT_WEIGHT = 100;

    public Material {
        forms = Collections.unmodifiableSet(forms);
        weight = Math.max(1, weight);
    }

    /** Whether the pack provides {@code form} for this material. */
    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }

    /**
     * Whether this material can be extracted from a chunk vein at all.
     *
     * <p>A material needs an ore or a raw form to come out of the ground. One that exists only as
     * an ingot — many alloys — is made, not mined, and must never be assigned to a vein or the
     * extractor would produce something with no processing chain behind it.
     */
    public boolean isMineable() {
        return has(MaterialForm.ORE) || has(MaterialForm.RAW);
    }

    /**
     * Whether this material degrades gracefully through the ore line.
     *
     * <p>A material with an ore but no dust simply skips the pulverizing step rather than
     * breaking the chain, which is what makes an unknown mod's material usable on day one.
     */
    public boolean hasOreLine() {
        return isMineable() && (has(MaterialForm.DUST) || has(MaterialForm.INGOT));
    }
}
