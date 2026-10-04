package io.github.ezequiel24123z.grindless.planet;

import io.github.ezequiel24123z.grindless.machine.ExtractorLogic;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.vein.ChunkVein;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The item an extractor emits for a vein. Luna's helium-3 does not go through the pack
 * scan: it has no {@code forge:raw_materials} tag, so the scan would not find it.
 */
public final class PlanetProduct {

    private PlanetProduct() {
    }

    public static ItemStack resolve(String dimension, ChunkVein vein) {
        if (vein == null) {
            return ItemStack.EMPTY;
        }
        String direct = PlanetCatalogue.extractable(dimension, vein.material());
        if (direct != null) {
            return stack(direct);
        }
        Material material = MaterialRegistry.snapshot().material(vein.material()).orElse(null);
        if (material == null) {
            return ItemStack.EMPTY;
        }
        MaterialForm form = ExtractorLogic.outputForm(
                material.has(MaterialForm.RAW), material.has(MaterialForm.ORE));
        ResourceLocation id = MaterialRegistry.output(vein.material(), form).orElse(null);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return stack(id.toString());
    }

    private static ItemStack stack(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }
        ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(parsed));
        return item.isEmpty() ? ItemStack.EMPTY : item;
    }
}
