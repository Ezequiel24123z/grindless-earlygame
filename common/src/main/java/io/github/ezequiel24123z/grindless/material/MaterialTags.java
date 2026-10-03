package io.github.ezequiel24123z.grindless.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The only way recipes name a material.
 *
 * <p>An ingredient that is a material in a form is always a tag, never an item ID. That single
 * rule is what makes every Grindless recipe accept another mod's tin: the recipe asks for
 * {@code forge:ingots/tin}, and whichever mod's ingot is in that tag satisfies it. Naming
 * {@code othermod:tin_ingot} instead would hard-wire the recipe to one mod and make it unusable
 * without it (ADR-0050).
 */
public final class MaterialTags {

    private MaterialTags() {
    }

    /** The tag for a material in a form, in the namespace that form is shared under. */
    public static TagKey<Item> of(MaterialForm form, String material) {
        return TagKey.create(Registries.ITEM,
                new ResourceLocation(form.tagNamespace(), form.tagPath(material)));
    }

    /** An ingredient accepting any item in the material's tag, from any mod. */
    public static Ingredient ingredient(MaterialForm form, String material) {
        return Ingredient.of(of(form, material));
    }
}
