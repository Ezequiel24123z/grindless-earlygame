package io.github.ezequiel24123z.grindless.fluid.forge;

import io.github.ezequiel24123z.grindless.fluid.BasicTankBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.NativeFluidStore;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
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
        Optional<IFluidHandlerItem> capability = FluidUtil.getFluidHandler(held.copyWithCount(1)).resolve();
        if (capability.isEmpty()) {
            return InteractionResult.PASS;
        }
        Optional<Exchange> exchange = prepare(tank, face, capability.get());
        if (exchange.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.grindless.tank.container_refused"), true);
            return InteractionResult.CONSUME;
        }
        Inventory inventory = player.getInventory();
        ItemStack originalHand = held.copy();
        ItemStack[] before = new ItemStack[inventory.items.size()];
        ItemStack[] after = new ItemStack[before.length];
        for (int slot = 0; slot < before.length; slot++) {
            before[slot] = inventory.getItem(slot).copy();
            after[slot] = before[slot].copy();
        }
        ItemStack resultingHand = originalHand.copy();
        if (!player.getAbilities().instabuild) {
            if (originalHand.getCount() == 1) {
                resultingHand = exchange.get().container().copy();
            } else {
                resultingHand.shrink(1);
                if (hand == InteractionHand.MAIN_HAND) {
                    after[inventory.selected] = resultingHand.copy();
                }
                SimpleContainer staged = new SimpleContainer(after);
                if (!staged.addItem(exchange.get().container().copy()).isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.grindless.tank.inventory_full"), true);
                    return InteractionResult.CONSUME;
                }
                for (int slot = 0; slot < after.length; slot++) {
                    after[slot] = staged.getItem(slot).copy();
                }
                if (hand == InteractionHand.MAIN_HAND) {
                    resultingHand = after[inventory.selected].copy();
                }
            }
        }
        ItemStack finalHand = resultingHand;
        boolean committed = tank.exchangeContainer(face, exchange.get().fluid(), exchange.get().intoTank(),
                () -> ItemStack.matches(player.getItemInHand(hand), originalHand) && matches(inventory, before),
                new NativeFluidStore.Settlement() {
                    @Override
                    public void apply() {
                        assign(inventory, after);
                        player.setItemInHand(hand, finalHand.copy());
                        inventory.setChanged();
                    }

                    @Override
                    public void restore() {
                        assign(inventory, before);
                        player.setItemInHand(hand, originalHand.copy());
                        inventory.setChanged();
                    }
                });
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

    private static boolean matches(Inventory inventory, ItemStack[] snapshot) {
        for (int slot = 0; slot < snapshot.length; slot++) {
            if (!ItemStack.matches(inventory.getItem(slot), snapshot[slot])) {
                return false;
            }
        }
        return true;
    }

    private static void assign(Inventory inventory, ItemStack[] snapshot) {
        for (int slot = 0; slot < snapshot.length; slot++) {
            if (!ItemStack.matches(inventory.getItem(slot), snapshot[slot])) {
                inventory.setItem(slot, snapshot[slot].copy());
            }
        }
    }
}
