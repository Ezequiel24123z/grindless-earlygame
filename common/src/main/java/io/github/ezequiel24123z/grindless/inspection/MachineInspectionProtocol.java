package io.github.ezequiel24123z.grindless.inspection;

import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Small request/reply and lifecycle contracts; neither side retains a world or executes work. */
public final class MachineInspectionProtocol {

    public static final int MAX_REQUEST_BYTES = 13;
    public static final int MAX_REPLY_BYTES = 32 * 1024;
    public static final int REQUEST_INTERVAL_TICKS = 10;
    public static final int EXPIRY_TICKS = 40;
    public static final long SERVER_INTERVAL_NANOS = 100_000_000L;
    public static final double MAX_DISTANCE = 6.0;

    private MachineInspectionProtocol() {
    }

    public enum ReplyKind {
        AVAILABLE(0), UNAVAILABLE(1), ERROR(2), MALFORMED(3);

        private final int code;

        ReplyKind(int code) {
            this.code = code;
        }

        private static ReplyKind fromCode(int code) {
            return switch (code) {
                case 0 -> AVAILABLE;
                case 1 -> UNAVAILABLE;
                case 2 -> ERROR;
                case 3 -> MALFORMED;
                default -> throw new IllegalArgumentException("unknown machine inspection reply kind");
            };
        }
    }

    public record Request(int sequence, Position position) {
        public Request {
            if (sequence < 0) {
                throw new IllegalArgumentException("negative machine inspection request identity");
            }
            Objects.requireNonNull(position, "position");
        }
    }

    public record Reply(int sequence, ReplyKind kind, Optional<MachineInspectionSnapshot> snapshot) {
        public Reply {
            if (sequence < 0) {
                throw new IllegalArgumentException("negative machine inspection reply identity");
            }
            Objects.requireNonNull(kind, "kind");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            if ((kind == ReplyKind.AVAILABLE) != snapshot.isPresent()) {
                throw new IllegalArgumentException("incomplete machine inspection reply");
            }
        }
    }

    public record Target(String dimension, Position position, String blockId) {
        public Target {
            dimension = MachineInspectionSnapshot.id(dimension);
            Objects.requireNonNull(position, "position");
            blockId = MachineInspectionSnapshot.id(blockId);
        }

        public boolean matches(MachineInspectionSnapshot snapshot) {
            return snapshot.describes(dimension, position, blockId);
        }
    }

    public static void encodeRequest(Request request, FriendlyByteBuf buffer) {
        buffer.writeVarInt(request.sequence());
        buffer.writeBlockPos(new BlockPos(request.position().x(), request.position().y(), request.position().z()));
    }

    public static Request decodeRequest(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("machine inspection request exceeds its bound");
        }
        int sequence = buffer.readVarInt();
        BlockPos position = buffer.readBlockPos();
        consumed(buffer);
        return new Request(sequence, new Position(position.getX(), position.getY(), position.getZ()));
    }

    public static void encodeReply(Reply reply, FriendlyByteBuf buffer) {
        int start = buffer.writerIndex();
        buffer.writeVarInt(reply.sequence());
        buffer.writeByte(reply.kind().code);
        reply.snapshot().ifPresent(snapshot -> buffer.writeNbt(MachineInspectionCodec.encode(snapshot)));
        if (buffer.writerIndex() - start > MAX_REPLY_BYTES) {
            throw new IllegalArgumentException("machine inspection reply exceeds its bound");
        }
    }

    public static Reply decodeReply(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_REPLY_BYTES) {
            throw new IllegalArgumentException("machine inspection reply exceeds its bound");
        }
        int sequence = buffer.readVarInt();
        ReplyKind kind = ReplyKind.fromCode(buffer.readUnsignedByte());
        Optional<MachineInspectionSnapshot> snapshot = Optional.empty();
        if (kind == ReplyKind.AVAILABLE) {
            CompoundTag tag = buffer.readNbt();
            if (tag == null) {
                kind = ReplyKind.MALFORMED;
            } else {
                try {
                    snapshot = Optional.of(MachineInspectionCodec.decode(tag));
                } catch (IllegalArgumentException invalid) {
                    // The caller must show/log MALFORMED, never retain an older successful reply.
                    kind = ReplyKind.MALFORMED;
                }
            }
        }
        consumed(buffer);
        return new Reply(sequence, kind, snapshot);
    }

    /** Nearest point of the target block; a face within reach must not fail a centre-distance proxy. */
    public static boolean withinDistance(Position target, double eyeX, double eyeY, double eyeZ) {
        if (!Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)) {
            return false;
        }
        double dx = Math.max(0.0, Math.max(target.x() - eyeX, eyeX - target.x() - 1.0));
        double dy = Math.max(0.0, Math.max(target.y() - eyeY, eyeY - target.y() - 1.0));
        double dz = Math.max(0.0, Math.max(target.z() - eyeZ, eyeZ - target.z() - 1.0));
        return dx * dx + dy * dy + dz * dz <= MAX_DISTANCE * MAX_DISTANCE;
    }

    private static void consumed(FriendlyByteBuf buffer) {
        if (buffer.isReadable()) {
            throw new IllegalArgumentException("unexpected machine inspection packet bytes");
        }
    }

    /** One per live sender, invoked on the server thread before any world query. */
    public static final class RequestLimiter {
        private long lastAccepted;
        private boolean accepted;

        public boolean admit(long nowNanos) {
            if (accepted && nowNanos - lastAccepted < SERVER_INTERVAL_NANOS) {
                return false;
            }
            accepted = true;
            lastAccepted = nowNanos;
            return true;
        }
    }

    /** A single target/reply, not a history, cache of worlds or prediction of progress. */
    public static final class ClientState {
        private Target target;
        private MachineInspectionSnapshot snapshot;
        private ReplyKind kind = ReplyKind.UNAVAILABLE;
        private int sequence;
        private int pending = -1;
        private long requestedAt = -REQUEST_INTERVAL_TICKS;
        private long receivedAt;

        public OptionalInt request(Target aimed, long tick) {
            if (!Objects.equals(target, aimed)) {
                target = aimed;
                snapshot = null;
                pending = -1;
                kind = ReplyKind.UNAVAILABLE;
            }
            expire(tick);
            if (aimed == null || tick - requestedAt < REQUEST_INTERVAL_TICKS
                    || pending >= 0 && tick - requestedAt <= EXPIRY_TICKS) {
                return OptionalInt.empty();
            }
            requestedAt = tick;
            sequence = sequence == Integer.MAX_VALUE ? 0 : sequence + 1;
            pending = sequence;
            return OptionalInt.of(sequence);
        }

        public boolean receive(Reply reply, long tick) {
            if (target == null || reply.sequence() != pending) {
                return false;
            }
            if (tick - requestedAt > EXPIRY_TICKS) {
                pending = -1;
                snapshot = null;
                kind = ReplyKind.UNAVAILABLE;
                return false;
            }
            pending = -1;
            snapshot = null;
            receivedAt = tick;
            kind = reply.kind();
            if (reply.snapshot().isPresent()) {
                if (!target.matches(reply.snapshot().get())) {
                    kind = ReplyKind.UNAVAILABLE;
                    return false;
                }
                snapshot = reply.snapshot().get();
            }
            return true;
        }

        public Optional<MachineInspectionSnapshot> snapshot(long tick) {
            expire(tick);
            return Optional.ofNullable(snapshot);
        }

        public ReplyKind kind() {
            return kind;
        }

        public Target target() {
            return target;
        }

        public void reset() {
            target = null;
            snapshot = null;
            pending = -1;
            requestedAt = -REQUEST_INTERVAL_TICKS;
            kind = ReplyKind.UNAVAILABLE;
        }

        private void expire(long tick) {
            if (snapshot != null && tick - receivedAt > EXPIRY_TICKS) {
                snapshot = null;
                kind = ReplyKind.UNAVAILABLE;
            }
        }
    }
}
