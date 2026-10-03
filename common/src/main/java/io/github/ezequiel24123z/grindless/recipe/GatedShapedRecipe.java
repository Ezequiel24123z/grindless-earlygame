package io.github.ezequiel24123z.grindless.recipe;

import com.google.gson.JsonObject;
import io.github.ezequiel24123z.grindless.research.Blueprint;
import io.github.ezequiel24123z.grindless.research.ResearchAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A shaped crafting recipe that also requires a researched blueprint.
 *
 * <p>Forge {@code ICondition} is evaluated when recipes load, not against a world, so it cannot
 * gate on {@code ResearchData}. Matching here — and a client unlock cache — is the gate
 * (ADR-0057, ADR-0059).
 */
public final class GatedShapedRecipe extends ShapedRecipe {

    private final Blueprint blueprint;

    public GatedShapedRecipe(ResourceLocation id, String group, CraftingBookCategory category,
                             int width, int height, NonNullList<Ingredient> items, ItemStack result,
                             boolean showNotification, Blueprint blueprint) {
        super(id, group, category, width, height, items, result, showNotification);
        this.blueprint = blueprint;
    }

    public Blueprint blueprint() {
        return blueprint;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return ResearchAccess.isUnlocked(level, blueprint) && super.matches(container, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GATED_SHAPED.get();
    }

    public static final class Serializer implements RecipeSerializer<GatedShapedRecipe> {

        @Override
        public GatedShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe inner = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            Blueprint blueprint = Blueprint.byId(GsonHelper.getAsString(json, "blueprint"));
            if (blueprint == null) {
                throw new IllegalArgumentException("unknown blueprint in " + id);
            }
            return wrap(inner, blueprint);
        }

        @Override
        public GatedShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe inner = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            Blueprint blueprint = Blueprint.byId(buf.readUtf());
            if (blueprint == null) {
                throw new IllegalArgumentException("unknown blueprint in " + id);
            }
            return wrap(inner, blueprint);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, GatedShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
            buf.writeUtf(recipe.blueprint.id());
        }

        private static GatedShapedRecipe wrap(ShapedRecipe inner, Blueprint blueprint) {
            return new GatedShapedRecipe(
                    inner.getId(),
                    inner.getGroup(),
                    inner.category(),
                    inner.getWidth(),
                    inner.getHeight(),
                    inner.getIngredients(),
                    inner.getResultItem(null),
                    inner.showNotification(),
                    blueprint);
        }
    }
}
