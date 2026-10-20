package io.github.ezequiel24123z.grindless.shop;

import dev.architectury.networking.NetworkManager;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S intent only. Native opening/data synchronization owns all S2C menu state. */
public final class ShopNetwork {
    private static final ResourceLocation OPEN = Grindless.id("shop_open");
    private static final ResourceLocation EXCHANGE = Grindless.id("shop_exchange");
    private static final int REQUEST_BYTES = Integer.BYTES * 3 + Long.BYTES;
    private static boolean registered;

    public record Request(int container, long token, int sequence, int offer) {
    }

    private ShopNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OPEN, (data, context) -> {
            if (data.readableBytes() != 0) {
                Grindless.LOG.warn("Rejected malformed recovery-shop opening packet");
                return;
            }
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    ShopAccess.openFromBook(player);
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, EXCHANGE, (data, context) -> {
            Request request = decode(data);
            if (request == null) {
                Grindless.LOG.warn("Rejected malformed recovery-shop exchange packet");
                return;
            }
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    if (player.containerMenu instanceof ShopMenu shop) {
                        shop.exchange(player, request);
                    } else {
                        player.displayClientMessage(Component.translatable(ShopResult.CLOSED.translationKey()), false);
                    }
                }
            });
        });
    }

    /** The one client action used by both quest interfaces. No claim or exchange is implicit. */
    public static void requestOpen() {
        NetworkManager.sendToServer(OPEN, buffer());
    }

    public static void requestExchange(ShopMenu menu, int offer) {
        NetworkManager.sendToServer(EXCHANGE, encode(new Request(menu.containerId, menu.token(), menu.sequence(), offer)));
    }

    static Request decode(FriendlyByteBuf data) {
        if (data.readableBytes() != REQUEST_BYTES) {
            return null;
        }
        return new Request(data.readInt(), data.readLong(), data.readInt(), data.readInt());
    }

    static FriendlyByteBuf encode(Request request) {
        FriendlyByteBuf data = buffer();
        data.writeInt(request.container());
        data.writeLong(request.token());
        data.writeInt(request.sequence());
        data.writeInt(request.offer());
        return data;
    }

    private static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }
}
