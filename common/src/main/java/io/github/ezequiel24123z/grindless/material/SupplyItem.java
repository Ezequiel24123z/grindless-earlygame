package io.github.ezequiel24123z.grindless.material;

import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * A material in a form, supplied by Grindless because the pack may not provide it.
 *
 * <p>Named from two lang keys, one for the form and one for the material, so the whole matrix
 * needs one entry per axis value rather than one per item — adding a material is one line of
 * translation, the same economy the textures have.
 */
public class SupplyItem extends Item {

    private final SupplyCatalogue.Entry entry;

    public SupplyItem(SupplyCatalogue.Entry entry, Properties properties) {
        super(properties);
        this.entry = entry;
    }

    public SupplyCatalogue.Entry entry() {
        return entry;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                "form." + Grindless.MOD_ID + "." + entry.form().name().toLowerCase(Locale.ROOT),
                Component.translatable("material." + Grindless.MOD_ID + "." + entry.material()));
    }
}
