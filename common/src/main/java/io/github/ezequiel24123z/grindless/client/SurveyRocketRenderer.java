package io.github.ezequiel24123z.grindless.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.flight.SurveyRocket;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the rocket as its item, upright. Original art, not another mod's model (ADR-0097).
 */
public final class SurveyRocketRenderer extends EntityRenderer<SurveyRocket> {

    private static final ResourceLocation TEXTURE = Grindless.id("textures/item/survey_rocket.png");

    private final ItemRenderer items;

    public SurveyRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(SurveyRocket entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0, 0.35, 0.0);
        pose.scale(1.4F, 1.4F, 1.4F);
        items.renderStatic(new ItemStack(ModItems.SURVEY_ROCKET.get()), ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SurveyRocket entity) {
        return TEXTURE;
    }
}
