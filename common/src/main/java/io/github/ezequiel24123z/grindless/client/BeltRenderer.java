package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.belt.BeltBlockEntity;
import io.github.ezequiel24123z.grindless.belt.BeltStacks;
import io.github.ezequiel24123z.grindless.belt.Lane;
import io.github.ezequiel24123z.grindless.belt.LaneItem;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Draws belt contents from lane data. Items never exist as entities (ADR-0008).
 */
public final class BeltRenderer implements BlockEntityRenderer<BeltBlockEntity> {

    public BeltRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BeltBlockEntity belt, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = belt.getBlockState().getValue(MachineProperties.FACING);
        Direction left = facing.getCounterClockWise();
        renderLane(belt.left(), facing, left, -0.2, pose, buffers, packedLight, packedOverlay);
        renderLane(belt.right(), facing, left, 0.2, pose, buffers, packedLight, packedOverlay);
    }

    private static void renderLane(Lane lane, Direction facing, Direction left, double side,
                                   PoseStack pose, MultiBufferSource buffers, int light,
                                   int overlay) {
        for (LaneItem item : lane.items()) {
            ItemStack stack = BeltStacks.toStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            double along = item.position() - 0.5;
            pose.pushPose();
            pose.translate(
                    0.5 + facing.getStepX() * along + left.getStepX() * side,
                    0.28,
                    0.5 + facing.getStepZ() * along + left.getStepZ() * side);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.scale(0.4F, 0.4F, 0.4F);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.GROUND, light, overlay, pose, buffers, null, 0);
            pose.popPose();
        }
    }
}
