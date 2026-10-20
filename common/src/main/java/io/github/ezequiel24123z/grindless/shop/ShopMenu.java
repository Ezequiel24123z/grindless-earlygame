package io.github.ezequiel24123z.grindless.shop;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import io.github.ezequiel24123z.grindless.telemetry.dev.DevTelemetry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** No movable slots: every exchange is one server-owned payment and delivery batch. */
public final class ShopMenu extends AbstractContainerMenu {
    private static final int SEQUENCE = 0;
    private static final int RESULT = 1;
    private static final int LAST_OFFER = 2;
    private static final int OFFER_START = 3;

    private final Player owner;
    private final long token;
    private final ShopSession session;
    private final SimpleContainerData state = new SimpleContainerData(OFFER_START + ShopCatalogue.offers().size());

    public ShopMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readLong());
    }

    public ShopMenu(int id, Inventory inventory, long token) {
        super(ModMenus.RECOVERY_SHOP.get(), id);
        this.owner = inventory.player;
        this.token = token;
        this.session = owner instanceof ServerPlayer ? new ShopSession(id, token) : null;
        for (int offer = 0; offer < ShopCatalogue.offers().size(); offer++) {
            state.set(OFFER_START + offer, ShopResult.CLOSED.ordinal());
        }
        addDataSlots(state);
    }

    public long token() {
        return token;
    }

    public int sequence() {
        return state.get(SEQUENCE);
    }

    public ShopResult result() {
        return result(state.get(RESULT));
    }

    public int lastOffer() {
        return state.get(LAST_OFFER) - 1;
    }

    public ShopResult availability(int offer) {
        return offer < 0 || offer >= ShopCatalogue.offers().size()
                ? ShopResult.INVALID_OFFER : result(state.get(OFFER_START + offer));
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator() && !player.isRemoved();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        // The vanilla byte-sized button channel cannot carry the opening token and sequence.
        return false;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        if (session != null) {
            session.close();
        }
        super.removed(player);
    }

    public void exchange(ServerPlayer player, ShopNetwork.Request request) {
        if (!player.server.isSameThread()) {
            throw new IllegalStateException("Recovery exchanges must run on the server thread");
        }
        if (player.containerMenu != this || !stillValid(player) || session == null) {
            player.displayClientMessage(Component.translatable(ShopResult.CLOSED.translationKey()), false);
            return;
        }
        ShopCatalogue.Offer offer = request.offer() >= 0 && request.offer() < ShopCatalogue.offers().size()
                ? ShopCatalogue.offers().get(request.offer()) : null;
        ShopResult result = session.exchange(request.container(), request.token(), request.sequence(),
                request.offer(), player.server.overworld().getGameTime(), () -> {
                    Set<String> claimed = QuestProgress.get(player).claimed(player.getUUID());
                    ShopInventory.Prepared plan = ShopInventory.prepare(player.getInventory(), offer, claimed);
                    return plan.result() == ShopResult.READY ? plan.transaction().commit() : plan.result();
                });
        if (result == ShopResult.INVALID_OFFER || result == ShopResult.STALE_INVENTORY
                || result == ShopResult.REENTRANT) {
            Grindless.LOG.warn("Recovery exchange rejected: {}", result);
        }
        state.set(RESULT, result.ordinal());
        state.set(LAST_OFFER, offer == null ? 0 : request.offer() + 1);
        DevTelemetry.shop(player.level(), offer, result);
        if (result == ShopResult.TRADED) {
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("chat.grindless.shop.traded",
                    offer.goods().count(), new ItemStack(ShopInventory.resolve(offer.goods().item())).getHoverName()), false);
        } else {
            player.displayClientMessage(Component.translatable(result.translationKey()), false);
        }
        broadcastChanges();
        if (result == ShopResult.CLOSED) {
            player.closeContainer();
        }
    }

    private void refresh() {
        if (!(owner instanceof ServerPlayer player) || session == null) {
            return;
        }
        Set<String> claimed = QuestProgress.get(player).claimed(player.getUUID());
        state.set(SEQUENCE, session.sequence());
        for (int i = 0; i < ShopCatalogue.offers().size(); i++) {
            state.set(OFFER_START + i,
                    ShopInventory.prepare(player.getInventory(), ShopCatalogue.offers().get(i), claimed).result().ordinal());
        }
    }

    private static ShopResult result(int code) {
        return code < 0 || code >= ShopResult.values().length ? ShopResult.INVALID_OFFER : ShopResult.values()[code];
    }
}
