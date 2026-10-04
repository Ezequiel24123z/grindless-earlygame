package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.flight.SurveyRocket;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Places a survey rocket on a launch pad (ADR-0097).
 *
 * <p>The item is the rocket. It is manufactured, not crafted on a table. Right-click a
 * pad to set it down and sit. The climb starts once the pad has drawn its toll.
 */
public final class SurveyRocketItem extends Item {

    public SurveyRocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(ModBlocks.LAUNCH_PAD.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        AABB near = new AABB(pos).inflate(1.0);
        if (!server.getEntitiesOfClass(SurveyRocket.class, near).isEmpty()) {
            return InteractionResult.FAIL;
        }
        SurveyRocket rocket = SurveyRocket.create(server, pos);
        if (rocket == null || !server.addFreshEntity(rocket)) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player != null) {
            player.startRiding(rocket);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.survey_rocket"));
    }
}
