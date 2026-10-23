package io.github.ezequiel24123z.grindless.fluid.forge;

import io.github.ezequiel24123z.grindless.fluid.BasicTankBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.TankContainerDelivery;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

/** Stages a real item capability on a copy before settling native tank and vanilla inventory. */
public final class FluidContainerPlatformImpl {

    private FluidContainerPlatformImpl() {
    }

    public static InteractionResult interact(Player player, InteractionHand hand,
                                             BasicTankBlockEntity tank, Direction face) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack originalHand = held.copy();
        Optional<IFluidHandlerItem> capability = FluidUtil.getFluidHandler(held.copyWithCount(1)).resolve();
        if (capability.isEmpty()) {
            return InteractionResult.PASS;
        }
        Optional<Exchange> exchange = prepare(tank, face, capability.get());
        if (exchange.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.grindless.tank.container_refused"), true);
            return InteractionResult.CONSUME;
        }
        if (!ItemStack.matches(player.getItemInHand(hand), originalHand)) {
            player.displayClientMessage(Component.translatable("message.grindless.tank.exchange_changed"), true);
            return InteractionResult.CONSUME;
        }
        Optional<TankContainerDelivery.Prepared> delivery = TankContainerDelivery.prepare(player.getInventory(), hand,
                originalHand, player.getAbilities().instabuild, exchange.get().intoTank(), exchange.get().container());
        if (delivery.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.grindless.tank.inventory_full"), true);
            return InteractionResult.CONSUME;
        }
        boolean committed = tank.exchangeContainer(face, exchange.get().fluid(), exchange.get().intoTank(),
                delivery.get()::unchanged, delivery.get());
        player.displayClientMessage(committed ? tank.fillFeedback()
                : Component.translatable("message.grindless.tank.exchange_changed"), true);
        return InteractionResult.CONSUME;
    }

    record Exchange(FluidState fluid, boolean intoTank, ItemStack container) {
    }

    static Optional<Exchange> prepare(BasicTankBlockEntity tank, Direction face, IFluidHandlerItem item) {
        FluidStack offered = MachineFluidCapability.toForge(tank.contents());
        if (!offered.isEmpty() && tank.canExtract(face)) {
            int accepted = Math.min(offered.getAmount(), item.fill(offered.copy(), IFluidHandler.FluidAction.SIMULATE));
            if (accepted > 0) {
                FluidStack transfer = offered.copy();
                transfer.setAmount(accepted);
                int filled = item.fill(transfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled != accepted) {
                    throw new IllegalStateException("Fluid container violated its simulated fill amount");
                }
                return Optional.of(new Exchange(tank.contents().withAmount(filled), false, item.getContainer().copy()));
            }
        }
        FluidStack simulated = item.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        FluidState incoming = MachineFluidCapability.fromForge(simulated);
        int accepted = tank.acceptedAmount(face, incoming);
        if (accepted <= 0) {
            return Optional.empty();
        }
        FluidStack requested = simulated.copy();
        requested.setAmount(accepted);
        FluidStack exact = item.drain(requested, IFluidHandler.FluidAction.SIMULATE);
        if (exact.isEmpty()) {
            return Optional.empty();
        }
        if (!requested.isFluidEqual(exact) || exact.getAmount() > accepted) {
            throw new IllegalStateException("Fluid container simulated an invalid drain");
        }
        requested = exact;
        FluidStack drained = item.drain(requested, IFluidHandler.FluidAction.EXECUTE);
        if (!requested.isFluidEqual(drained) || drained.getAmount() != requested.getAmount()) {
            throw new IllegalStateException("Fluid container violated its simulated drain amount");
        }
        return Optional.of(new Exchange(MachineFluidCapability.fromForge(drained), true, item.getContainer().copy()));
    }

}
