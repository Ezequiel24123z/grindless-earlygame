package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Reusable emergency/manual work for F0 machinery; never an automated energy source. */
public final class CrankHandleItem extends Item {
    public CrankHandleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ProcessMachineBlockEntity machine)
                || machine.kind().tier() != FluxTier.F0 || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide()
                && !context.getPlayer().getCooldowns().isOnCooldown(this)) {
            machine.crankByHand();
            context.getPlayer().getCooldowns().addCooldown(this, SourceLogic.MANUAL_COOLDOWN);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.crank_handle"));
    }
}
