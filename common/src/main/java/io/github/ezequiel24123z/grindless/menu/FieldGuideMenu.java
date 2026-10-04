package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The field guide. No slots and no claims: it is the route, read in game (ADR-0100).
 */
public final class FieldGuideMenu extends AbstractContainerMenu {

    public FieldGuideMenu(int id, Inventory inventory) {
        super(ModMenus.FIELD_GUIDE.get(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(ModItems.FIELD_GUIDE.get())
                || player.getOffhandItem().is(ModItems.FIELD_GUIDE.get());
    }
}
