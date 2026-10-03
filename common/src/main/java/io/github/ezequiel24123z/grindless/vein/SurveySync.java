package io.github.ezequiel24123z.grindless.vein;

import dev.architectury.networking.NetworkManager;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Pushes a survey result to the client that ran the scanner.
 *
 * <p>Register only from client init — Architectury's S2C receiver is a client method.
 */
public final class SurveySync {

    public static final ResourceLocation ID = Grindless.id("survey");

    private SurveySync() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ID, (buf, context) -> {
            int count = buf.readVarInt();
            List<ClientSurvey.Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(new ClientSurvey.Entry(
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readUtf(),
                        buf.readDouble(),
                        buf.readVarLong()));
            }
            context.queue(() -> ClientSurvey.merge(entries));
        });
    }

    public static void sendTo(ServerPlayer player, List<ClientSurvey.Entry> entries) {
        NetworkManager.sendToPlayer(player, ID, write(entries));
    }

    private static FriendlyByteBuf write(List<ClientSurvey.Entry> entries) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(entries.size());
        for (ClientSurvey.Entry entry : entries) {
            buf.writeVarInt(entry.x());
            buf.writeVarInt(entry.z());
            buf.writeUtf(entry.material());
            buf.writeDouble(entry.richness());
            buf.writeVarLong(entry.reserve());
        }
        return buf;
    }
}
