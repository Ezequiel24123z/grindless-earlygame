package io.github.ezequiel24123z.grindless.client.audio;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Quern voice ownership and admission/recovery policy, independent of the native sound engine. */
final class QuernLoopTracker<S, V extends QuernLoopTracker.Voice> {

    static final int RANGE = 16;
    static final int ADMISSION_TICKS = 5;
    static final int RETRY_TICKS = 40;

    interface Voice {
        long heartbeat();

        boolean isStopped();
    }

    interface Playback<S, V> {
        V start(long position, S source);

        void stop(V voice);

        boolean isActive(V voice);

        void notAdmitted(long position);
    }

    private final Playback<S, V> playback;
    private final Map<Long, Entry<S, V>> entries = new HashMap<>();
    private WeakReference<Object> world = new WeakReference<>(null);
    private WeakReference<Object> player = new WeakReference<>(null);
    private long tick;

    QuernLoopTracker(Playback<S, V> playback) {
        this.playback = Objects.requireNonNull(playback);
    }

    void tick(Object currentWorld, Object currentPlayer, Map<Long, S> eligible,
              boolean paused, boolean enabled) {
        if (world.get() != currentWorld || player.get() != currentPlayer) {
            clear();
            world = new WeakReference<>(currentWorld);
            player = new WeakReference<>(currentPlayer);
        }
        if (currentWorld == null || currentPlayer == null || !enabled) {
            clear();
            return;
        }
        if (paused) {
            return;
        }
        tick++;
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var owned = iterator.next();
            Entry<S, V> entry = owned.getValue();
            S source = eligible.get(owned.getKey());
            if (source == null || entry.source.get() != source) {
                stop(entry);
                iterator.remove();
            }
        }
        eligible.forEach((position, source) -> {
            Entry<S, V> entry = entries.computeIfAbsent(position, key -> new Entry<>(source));
            if (entry.voice != null) {
                long heartbeat = entry.voice.heartbeat();
                if (heartbeat != entry.heartbeat) {
                    entry.heartbeat = heartbeat;
                    entry.lastHeartbeat = tick;
                    entry.admitted = true;
                }
                // Neither a tick nor isActive proves audibility. Together they detect lost ownership.
                if (entry.voice.isStopped()
                        || (entry.admitted && !playback.isActive(entry.voice))
                        || tick - entry.lastHeartbeat >= ADMISSION_TICKS) {
                    stop(entry);
                    if (entry.admitted) {
                        entry.retryAt = tick + RETRY_TICKS;
                    } else {
                        // A cancelled/replaced play must not spawn repeated, unowned replacement voices.
                        entry.suppressed = true;
                        playback.notAdmitted(position);
                    }
                }
            } else if (!entry.suppressed && tick >= entry.retryAt) {
                entry.voice = Objects.requireNonNull(playback.start(position, source));
                entry.heartbeat = entry.voice.heartbeat();
                entry.lastHeartbeat = tick;
                entry.admitted = false;
            }
        });
    }

    void clear() {
        entries.values().forEach(this::stop);
        entries.clear();
        world.clear();
        player.clear();
        tick = 0;
    }

    static boolean inRange(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz < RANGE * RANGE;
    }

    static int minChunk(double coordinate) {
        return (int) Math.floor((coordinate - RANGE) / 16.0);
    }

    static int maxChunk(double coordinate) {
        return (int) Math.floor((coordinate + RANGE) / 16.0);
    }

    private void stop(Entry<S, V> entry) {
        if (entry.voice != null) {
            playback.stop(entry.voice);
            entry.voice = null;
        }
    }

    private static final class Entry<S, V> {
        private final WeakReference<S> source;
        private V voice;
        private long heartbeat;
        private long lastHeartbeat;
        private long retryAt;
        private boolean admitted;
        private boolean suppressed;

        private Entry(S source) {
            this.source = new WeakReference<>(source);
        }
    }
}
