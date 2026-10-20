package io.github.ezequiel24123z.grindless.shop;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.quest.QuestClaimService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.concurrent.ThreadLocalRandom;

public final class ShopAccess {
    private ShopAccess() {
    }

    /** Packet entry always verifies the real held book on the server, never a client projection. */
    public static void openFromBook(ServerPlayer player) {
        requireThread(player);
        if (!QuestClaimService.holdingBook(player)) {
            player.displayClientMessage(Component.translatable("chat.grindless.shop.book_required"), false);
            return;
        }
        open(player);
    }

    /** The explicit utility command may open recovery without a held book. It never grants goods. */
    public static void open(ServerPlayer player) {
        requireThread(player);
        if (player.containerMenu instanceof ShopMenu) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.isRemoved()) {
            player.displayClientMessage(Component.translatable(ShopResult.CLOSED.translationKey()), false);
            return;
        }
        long token = ThreadLocalRandom.current().nextLong();
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf data) {
                data.writeLong(token);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.grindless.shop.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                return new ShopMenu(id, inventory, token);
            }
        });
    }

    private static void requireThread(ServerPlayer player) {
        if (!player.server.isSameThread()) {
            throw new IllegalStateException("Recovery shop access must run on the server thread");
        }
    }
}
