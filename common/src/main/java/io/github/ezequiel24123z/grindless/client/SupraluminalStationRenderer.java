package io.github.ezequiel24123z.grindless.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.station.SupraluminalStation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the station as its item, wider than the survey rocket. Original art, not
 * another mod's hull (ADR-0098).
 */
public final class SupraluminalStationRenderer extends EntityRenderer<SupraluminalStation> {

    private static final ResourceLocation TEXTURE =
            Grindless.id("textures/item/supraluminal_station.png");

    private final ItemRenderer items;

    public SupraluminalStationRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(SupraluminalStation entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0, 0.45, 0.0);
        pose.scale(1.8F, 1.8F, 1.8F);
        items.renderStatic(new ItemStack(ModItems.SUPRALUMINAL_STATION.get()), ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SupraluminalStation entity) {
        return TEXTURE;
    }
}
