package io.github.ezequiel24123z.grindless.forge.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.RegisterEvent;

/**
 * A shaped-recipe cell delegating physical admission to the existing runtime Matrix matcher.
 * Own bridge code over the pinned Forge 47 public ingredient API; no foreign implementation.
 */
public final class MinimumControlIngredient extends AbstractIngredient {
    public static final ResourceLocation ID = new ResourceLocation(IngredientSpec.MINIMUM_CONTROL_TYPE);
    public static final Serializer SERIALIZER = new Serializer();
    private final IngredientSpec requirement;

    public MinimumControlIngredient(FluxTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException("Minimum control requires a Flux tier");
        }
        requirement = IngredientSpec.manualControl(tier.name());
    }

    /** Invoked on the MOD bus, not the Forge command/gameplay bus. */
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            CraftingHelper.register(ID, SERIALIZER);
        }
    }

    public FluxTier tier() {
        return FluxTier.values()[requirement.matrixRating()];
    }

    @Override
    public boolean test(ItemStack stack) {
        // ProcessLookup checks the real registry identity/key roundtrip before spec/acceptsMatrix.
        return stack != null && ProcessLookup.matches(requirement, stack);
    }

    @Override
    public ItemStack[] getItems() {
        // Actual registered items, supported enum ratings, canonical stackFor, then the same matcher.
        return ProcessLookup.matrixInputs(requirement).toArray(ItemStack[]::new);
    }

    @Override
    public boolean isEmpty() {
        return getItems().length == 0;
    }

    @Override
    public boolean isSimple() {
        // NBT is significant even for otherwise identical item ids.
        return false;
    }

    @Override
    public IIngredientSerializer<MinimumControlIngredient> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", IngredientSpec.MINIMUM_CONTROL_TYPE);
        json.addProperty("tier", tier().name());
        return json;
    }

    public static final class Serializer implements IIngredientSerializer<MinimumControlIngredient> {
        @Override
        public MinimumControlIngredient parse(JsonObject json) {
            try {
                IngredientSpec spec = IngredientSpec.manualControl(json);
                return new MinimumControlIngredient(FluxTier.values()[spec.matrixRating()]);
            } catch (IllegalArgumentException invalid) {
                throw new JsonSyntaxException("Invalid Grindless minimum-control ingredient", invalid);
            }
        }

        @Override
        public MinimumControlIngredient parse(FriendlyByteBuf buffer) {
            // F1..F15 fit three characters; the shared decoder also refuses unsupported supply.
            IngredientSpec spec = IngredientSpec.manualControl(buffer.readUtf(3));
            return new MinimumControlIngredient(FluxTier.values()[spec.matrixRating()]);
        }

        @Override
        public void write(FriendlyByteBuf buffer, MinimumControlIngredient ingredient) {
            buffer.writeUtf(ingredient.tier().name(), 3);
        }
    }
}
