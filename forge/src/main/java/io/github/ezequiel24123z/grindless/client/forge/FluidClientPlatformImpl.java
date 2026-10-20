package io.github.ezequiel24123z.grindless.client.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge still sprites and tint, independent of the optional recipe viewer. */
public final class FluidClientPlatformImpl {

    private FluidClientPlatformImpl() {
    }

    public static boolean draw(GuiGraphics graphics, String id, int x, int y, int width, int height) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(id));
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        FluidStack stack = new FluidStack(fluid, 1);
        IClientFluidTypeExtensions appearance = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation still = appearance.getStillTexture(stack);
        if (still == null) {
            return false;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int tint = appearance.getTintColor(stack);
        float red = (tint >> 16 & 255) / 255.0F;
        float green = (tint >> 8 & 255) / 255.0F;
        float blue = (tint & 255) / 255.0F;
        float alpha = (tint >>> 24) / 255.0F;
        for (int row = 0; row < height; row += 16) {
            for (int col = 0; col < width; col += 16) {
                graphics.blit(x + col, y + row, 0, Math.min(16, width - col), Math.min(16, height - row),
                        sprite, red, green, blue, alpha);
            }
        }
        return true;
    }

    public static Component name(String id) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(id));
        return fluid == null || fluid == Fluids.EMPTY
                ? Component.literal(id) : new FluidStack(fluid, 1).getDisplayName();
    }
}
