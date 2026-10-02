package io.github.ezequiel24123z.grindless.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A read-only view of the item tags in the loaded pack.
 *
 * <p>The scan reads through this rather than the registry directly, so the same logic runs
 * against the real tags in game and against a hand-built set in the behaviour checks, with no
 * Minecraft bootstrap.
 */
public interface TagView {

    /** The material names that have at least one tag under {@code form}, such as {@code tin}. */
    Set<String> materials(MaterialForm form);

    /** The items in the tag for this form and material; empty if the tag does not exist. */
    Set<ResourceLocation> items(MaterialForm form, String material);
}
