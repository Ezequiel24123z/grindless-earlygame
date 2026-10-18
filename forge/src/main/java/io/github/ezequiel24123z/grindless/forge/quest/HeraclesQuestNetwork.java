package io.github.ezequiel24123z.grindless.forge.quest;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

/** Sender-owned claim requests and read-only live evidence, not client-supplied progress. */
public final class HeraclesQuestNetwork {

    private static final String PROTOCOL = "1";
    private static final class Channel {
        private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(Grindless.id("quest_projection"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    }

    public record ClaimRequest(String taskId) {
    }

    public record State(String taskId, int held, QuestLogic.Status status) {
    }

    public record Snapshot(boolean available, boolean holdingBook, List<State> tasks) {
        public Snapshot {
            tasks = List.copyOf(tasks);
        }
    }

    private HeraclesQuestNetwork() {
    }

    public static void init() {
        Channel.INSTANCE.messageBuilder(ClaimRequest.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeUtf(packet.taskId(), 64))
                .decoder(buffer -> new ClaimRequest(buffer.readUtf(64)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null) {
                        HeraclesQuestIntegration.claim(sender, packet.taskId());
                    }
                }).add();
        Channel.INSTANCE.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeraclesQuestNetwork::encode)
                .decoder(HeraclesQuestNetwork::decode)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> HeraclesQuestClient.receive(packet))).add();
    }

    public static void claim(String taskId) {
        Channel.INSTANCE.sendToServer(new ClaimRequest(taskId));
    }

    public static void send(ServerPlayer player, Snapshot snapshot) {
        Channel.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }

    static void encode(Snapshot packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.available());
        buffer.writeBoolean(packet.holdingBook());
        buffer.writeVarInt(packet.tasks().size());
        for (State task : packet.tasks()) {
            buffer.writeUtf(task.taskId(), 64);
            buffer.writeVarInt(task.held());
            buffer.writeEnum(task.status());
        }
    }

    static Snapshot decode(FriendlyByteBuf buffer) {
        boolean available = buffer.readBoolean();
        boolean holdingBook = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > QuestCatalogue.tasks().size()) {
            throw new IllegalArgumentException("Invalid quest projection row count: " + count);
        }
        List<State> tasks = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String id = buffer.readUtf(64);
            int held = buffer.readVarInt();
            if (QuestCatalogue.byId(id) == null || held < 0 || tasks.stream().anyMatch(task -> task.taskId().equals(id))) {
                throw new IllegalArgumentException("Invalid quest projection row: " + id);
            }
            tasks.add(new State(id, held, buffer.readEnum(QuestLogic.Status.class)));
        }
        if (available && count != QuestCatalogue.tasks().size()) {
            throw new IllegalArgumentException("Incomplete available quest projection");
        }
        return new Snapshot(available, holdingBook, tasks);
    }
}
