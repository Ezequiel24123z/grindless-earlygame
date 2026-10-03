package io.github.ezequiel24123z.grindless.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializers. Processing recipes are not vanilla {@code Recipe}s; they live in
 * {@link ProcessLookup}. This register is the Voltaic-gated crafting table.
 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Grindless.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<GatedShapedRecipe>> GATED_SHAPED =
            SERIALIZERS.register("gated_shaped", GatedShapedRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register() {
        SERIALIZERS.register();
    }
}
