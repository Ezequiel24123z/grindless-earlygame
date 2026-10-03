package io.github.ezequiel24123z.grindless.research;

import dev.architectury.networking.NetworkManager;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pushes the world's unlocked blueprints to every client so gated recipes can match locally.
 */
public final class ResearchSync {

    public static final ResourceLocation ID = Grindless.id("research");

    private ResearchSync() {
    }

    /**
     * Client only. Architectury's S2C receiver is a client method; calling it from common
     * init crashes a dedicated server.
     */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ID, (buf, context) -> {
            Set<Blueprint> unlocked = EnumSet.noneOf(Blueprint.class);
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                Blueprint blueprint = Blueprint.byId(buf.readUtf());
                if (blueprint != null) {
                    unlocked.add(blueprint);
                }
            }
            context.queue(() -> ClientResearch.replace(unlocked));
        });
    }

    public static void sendTo(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, ID, write(ResearchData.get(player.serverLevel())));
    }

    public static void broadcast(MinecraftServer server) {
        FriendlyByteBuf buf = write(ResearchData.get(server));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkManager.sendToPlayer(player, ID, copy(buf));
        }
    }

    private static FriendlyByteBuf write(ResearchData data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        Set<Blueprint> unlocked = data.unlocked();
        buf.writeVarInt(unlocked.size());
        for (Blueprint blueprint : unlocked) {
            buf.writeUtf(blueprint.id());
        }
        return buf;
    }

    private static FriendlyByteBuf copy(FriendlyByteBuf source) {
        FriendlyByteBuf copy = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        copy.writeBytes(source.copy());
        return copy;
    }
}
