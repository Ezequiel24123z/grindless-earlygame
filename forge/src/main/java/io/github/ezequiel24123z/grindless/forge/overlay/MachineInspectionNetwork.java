package io.github.ezequiel24123z.grindless.forge.overlay;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.inspection.MachineInspection;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Reply;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.ReplyKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Request;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.RequestLimiter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Sender-owned targeted requests. Invalid requests never inspect remote or unloaded machines. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class MachineInspectionNetwork {

    private static final String PROTOCOL = "1";
    private static final Map<ServerPlayer, RequestLimiter> LIMITERS = new WeakHashMap<>();

    private static final class Channel {
        private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
                Grindless.id("machine_inspection"), () -> PROTOCOL,
                MachineInspectionNetwork::compatible, MachineInspectionNetwork::compatible);
    }

    private MachineInspectionNetwork() {
    }

    private static boolean compatible(String version) {
        return PROTOCOL.equals(version) || NetworkRegistry.ABSENT.equals(version)
                || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }

    public static void init() {
        Channel.INSTANCE.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MachineInspectionProtocol::encodeRequest)
                .decoder(MachineInspectionProtocol::decodeRequest)
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null) {
                        inspect(sender, packet);
                    }
                }).add();
        Channel.INSTANCE.messageBuilder(Reply.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MachineInspectionProtocol::encodeReply)
                .decoder(MachineInspectionProtocol::decodeReply)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> MachineInspectionOverlayClient.receive(packet))).add();
    }

    public static boolean available(Connection connection) {
        return Channel.INSTANCE.isRemotePresent(connection);
    }

    public static void request(Request request) {
        Channel.INSTANCE.sendToServer(request);
    }

    private static void inspect(ServerPlayer sender, Request request) {
        if (!LIMITERS.computeIfAbsent(sender, ignored -> new RequestLimiter()).admit(System.nanoTime())) {
            return;
        }
        ServerLevel level = sender.serverLevel();
        BlockPos position = new BlockPos(request.position().x(), request.position().y(), request.position().z());
        Vec3 eye = sender.getEyePosition();
        Vec3 end = eye.add(sender.getViewVector(1.0F).scale(MachineInspectionProtocol.MAX_DISTANCE));
        if (!level.hasChunkAt(position)
                || !MachineInspectionProtocol.withinDistance(request.position(), eye.x, eye.y, eye.z)
                || !rayChunksLoaded(level, eye, end)) {
            send(sender, new Reply(request.sequence(), ReplyKind.UNAVAILABLE, Optional.empty()));
            return;
        }
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, sender));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(position)
                || !MachineInspectionProtocol.withinHitDistance(hit.getLocation().distanceToSqr(eye))) {
            send(sender, new Reply(request.sequence(), ReplyKind.UNAVAILABLE, Optional.empty()));
            return;
        }
        try {
            var snapshot = MachineInspection.query(level, position);
            send(sender, new Reply(request.sequence(),
                    snapshot.isPresent() ? ReplyKind.AVAILABLE : ReplyKind.UNAVAILABLE, snapshot));
        } catch (IllegalArgumentException invalid) {
            Grindless.LOG.warn("Machine inspection rejected invalid native data: {}", invalid.getMessage());
            send(sender, new Reply(request.sequence(), ReplyKind.ERROR, Optional.empty()));
        }
    }

    /** Four load predicates cover this six-block ray; no chunk is retrieved, scanned or requested. */
    private static boolean rayChunksLoaded(ServerLevel level, Vec3 start, Vec3 end) {
        return level.hasChunkAt(BlockPos.containing(start.x, start.y, start.z))
                && level.hasChunkAt(BlockPos.containing(start.x, start.y, end.z))
                && level.hasChunkAt(BlockPos.containing(end.x, start.y, start.z))
                && level.hasChunkAt(BlockPos.containing(end.x, end.y, end.z));
    }

    private static void send(ServerPlayer player, Reply reply) {
        Channel.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), reply);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LIMITERS.clear();
    }
}
