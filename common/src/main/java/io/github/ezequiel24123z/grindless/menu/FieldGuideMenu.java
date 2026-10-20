package io.github.ezequiel24123z.grindless.menu;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.quest.QuestBookAccess;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Retained menu identity for old Guide stacks; its only action opens current quests.
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !(player instanceof ServerPlayer server) || !stillValid(player)) {
            return false;
        }
        QuestBookAccess.ensure(server);
        MenuRegistry.openMenu(server, new SimpleMenuProvider(
                (menuId, inventory, opener) -> new QuestBookMenu(menuId, inventory),
                Component.translatable("gui.grindless.quest.title")));
        return true;
    }
}
