package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Viewer container. No slots: the Atlas is a list, not a machine (ADR-0066).
 */
public final class ProcessAtlasMenu extends AbstractContainerMenu {

    public ProcessAtlasMenu(int id, Inventory inventory) {
        super(ModMenus.PROCESS_ATLAS.get(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(ModItems.PROCESS_ATLAS.get())
                || player.getOffhandItem().is(ModItems.PROCESS_ATLAS.get());
    }
}
