package io.github.ezequiel24123z.grindless.material;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

/** {@link TagView} over the live item registry, so it sees whatever the loaded pack tagged. */
public final class RegistryTagView implements TagView {

    private final Registry<Item> registry;

    public RegistryTagView(Registry<Item> registry) {
        this.registry = registry;
    }

    public static RegistryTagView ofItems() {
        return new RegistryTagView(BuiltInRegistries.ITEM);
    }

    @Override
    public Set<String> materials(MaterialForm form) {
        String prefix = form.tagPath() + "/";
        Set<String> names = new HashSet<>();
        registry.getTagNames().forEach(tag -> {
            ResourceLocation id = tag.location();
            if (id.getNamespace().equals(form.tagNamespace()) && id.getPath().startsWith(prefix)) {
                String name = id.getPath().substring(prefix.length());
                // A further slash would be a sub-category, not a material.
                if (!name.isEmpty() && name.indexOf('/') < 0) {
                    names.add(name);
                }
            }
        });
        return names;
    }

    @Override
    public Set<ResourceLocation> items(MaterialForm form, String material) {
        Set<ResourceLocation> items = new HashSet<>();
        registry.getTag(MaterialTags.of(form, material)).ifPresent(named -> {
            for (Holder<Item> holder : named) {
                holder.unwrapKey().ifPresent(key -> items.add(key.location()));
            }
        });
        return items;
    }
}
